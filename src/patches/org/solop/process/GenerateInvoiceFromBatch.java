/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.solop.process;

import org.adempiere.core.domains.models.I_C_InvoiceBatchLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceBatch;
import org.compiere.model.MInvoiceBatchLine;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MProductPricing;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.solop.model.MInvoiceBatchLineBulk;
import org.solop.model.MInvoiceLineBulk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Process: generate invoices from a C_InvoiceBatch.
 * <p>
 * The work is split into four phases so that every expensive step runs in
 * bulk instead of per-line:
 * <ol>
 *   <li><b>Phase 0</b> — load pre-existing draft invoices of the batch
 *       (so a re-run reuses them) and resolve the default price list.</li>
 *   <li><b>Phase 1</b> — create one invoice header per
 *       (BPartner, Location) in parallel. Each created header is linked back
 *       to every pending batch line of the same key via a single SQL
 *       {@code UPDATE}.</li>
 *   <li><b>Phase 2</b> — insert the invoice lines, again in parallel but
 *       grouped by invoice so each task builds a full invoice's lines.
 *       Uses {@link MInvoiceLineBulk} to skip {@code updateHeaderTax()} on
 *       every save, plus an in-memory price-list cache.</li>
 *   <li><b>Phase 3</b> — complete each invoice in parallel on a dedicated
 *       {@link ForkJoinPool}. Totals are rebuilt once per invoice
 *       (see {@link #recomputeInvoiceHeader}) and the BPartner balance /
 *       life-time value is recomputed once per affected BP at the end
 *       (replaces the per-invoice deltas skipped by
 *       {@code MInvoice.isBulkComplete}).</li>
 * </ol>
 */
public class GenerateInvoiceFromBatch extends GenerateInvoiceFromBatchAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		final ConcurrentMap<String, LongAdder> resultMsgMap = new ConcurrentHashMap<>();
		final ConcurrentMap<String, LongAdder> errorMsgMap = new ConcurrentHashMap<>();
		int created = 0;
		boolean processing = false;
		try {
			if (getRecord_ID() <= 0) {
				throw new AdempiereException("@C_InvoiceBatch_ID@ @NotFound@");
			}
			final MInvoiceBatch batch = new MInvoiceBatch(getCtx(), getRecord_ID(), null);
			final String documentAction = getDocAction();

			// Atomic claim against both action columns: free state is
			// ('G','C'); while either process runs both flip to ('P','P'),
			// so a single UPDATE with a conditional WHERE works as a CAS.
			int claimed = DB.executeUpdateEx(
					"UPDATE C_InvoiceBatch "
					+ "SET SP030C_PR_GenerateInvoices='P', SP030C_PR_ProcessInvoices='P' "
					+ "WHERE C_InvoiceBatch_ID = ? "
					+ "  AND SP030C_PR_GenerateInvoices = 'G' "
					+ "  AND SP030C_PR_ProcessInvoices = 'C' "
					+ "  AND Processed = 'N'",
					new Object[]{ getRecord_ID() }, null);
			if (claimed == 0) {
				batch.load(null);
				if (batch.isProcessed()) {
					throw new AdempiereException("@C_InvoiceBatch_ID@ @Processed@");
				}
				throw new AdempiereException("@Processing@");
			}
			processing = true;

			if (batch.getControlAmt().signum() != 0
					&& batch.getControlAmt().compareTo(batch.getDocumentAmt()) != 0) {
				throw new AdempiereException("@ControlAmt@ <> @DocumentAmt@");
			}
			// Phase 0: reuse any draft invoices already linked to this batch
			// (re-run safe) and resolve the default price list for fallbacks.
			final ConcurrentMap<String, Integer> invoiceIdByKey = new ConcurrentHashMap<>();
			// Read with null trx (autocommit / fresh connection) so the parent
			// process trx does not stay idle during the long parallel phases
			// (PostgreSQL idle_in_transaction_session_timeout would kill it).
			List<Integer> existingInvoiceIds = new Query(getCtx(), MInvoice.Table_Name,
					"C_Invoice_ID IN (SELECT DISTINCT C_Invoice_ID FROM C_InvoiceBatchLine"
					+ " WHERE C_InvoiceBatch_ID = ? AND C_Invoice_ID IS NOT NULL)"
					+ " AND DocStatus IN ('DR','IP','IN')", null)
					.setParameters(getRecord_ID())
					.getIDsAsList();
			for (Integer invId : existingInvoiceIds) {
				MInvoice inv = new MInvoice(getCtx(), invId, null);
				invoiceIdByKey.put(inv.getC_BPartner_ID() + "|" + inv.getC_BPartner_Location_ID(), invId);
			}

			final int defaultPriceListId = new Query(batch.getCtx(), MPriceList.Table_Name,
					"IsDefault = 'Y' AND IsSOPriceList = ? AND C_Currency_ID = ?", null)
					.setParameters(batch.isSOTrx(), batch.getC_Currency_ID())
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.setOrderBy("M_PriceList_ID")
					.firstId();

			// Phase 1: create one invoice header per (BPartner, Location) key
			// that still has no invoice. Each successful header is linked back
			// to every pending batch line of its key with a single SQL update.
			List<int[]> keysToCreate = loadDistinctPendingKeys(invoiceIdByKey);
			final AtomicInteger headersErr = new AtomicInteger();
			if (!keysToCreate.isEmpty()) {
				keysToCreate.parallelStream().forEach(row -> {
					int bpId = row[0];
					int locId = row[1];
					int sampleLineId = row[2];
					String key = bpId + "|" + locId;
					try {
						Trx.run(trx -> {
							MInvoiceBatchLine line = new MInvoiceBatchLineBulk(getCtx(), sampleLineId, trx);
							MInvoice invoice = new MInvoice(batch, line);
							int priceListId = 0;
							int expenseId = line.get_ValueAsInt("SP030C_TimeExpense_ID");
							if (expenseId > 0) {
								MTimeExpense expense = new MTimeExpense(getCtx(), expenseId, trx);
								boolean isRecurring = expense.get_ValueAsBoolean("SP030C_RecurringInvoice");
								priceListId = expense.getM_PriceList_ID();
								invoice.setIsSelfService(!isRecurring);
								invoice.set_ValueOfColumn("SP030C_RecurringInvoice", isRecurring);
								if (priceListId > 0) {
									invoice.setM_PriceList_ID(priceListId);
								}
							}
							if (priceListId <= 0) {
								invoice.setM_PriceList_ID(defaultPriceListId);
							}
							invoice.saveEx();
							DB.executeUpdateEx(
									"UPDATE C_InvoiceBatchLine SET C_Invoice_ID = ? "
									+ "WHERE C_InvoiceBatch_ID = ? AND C_BPartner_ID = ? AND C_BPartner_Location_ID = ? "
									+ "AND C_Invoice_ID IS NULL AND C_InvoiceLine_ID IS NULL",
									new Object[] { invoice.getC_Invoice_ID(), getRecord_ID(), bpId, locId },
									trx);
							invoiceIdByKey.put(key, invoice.getC_Invoice_ID());
						});
					} catch (Exception e) {
						headersErr.incrementAndGet();
						errorMsgMap.computeIfAbsent(
								"@Error@ @Header@ key=" + key + " " + e.getLocalizedMessage() + " @Qty@: ",
								k -> new LongAdder()).increment();
					}
				});
			}

			// Phase 2: insert the invoice lines, grouped by invoice so each
			// parallel task produces a complete set of lines for one invoice.
			Map<String, List<Integer>> linesByKey = loadPendingLinesGroupedByKey();
			AtomicInteger lineErr = new AtomicInteger();
			linesByKey.entrySet().parallelStream().forEach(entry -> {
				String key = entry.getKey();
				List<Integer> lineIds = entry.getValue();
				Integer invId = invoiceIdByKey.get(key);
				if (invId == null) {
					lineErr.addAndGet(lineIds.size());
					errorMsgMap.computeIfAbsent(
							"@Error@ @NoInvoice@ key=" + key + " @Qty@: ",
							k -> new LongAdder()).add(lineIds.size());
					return;
				}
				try {
					Trx.run(trx -> insertLinesForInvoice(batch, invId, lineIds, trx));
				} catch (Exception e) {
					lineErr.addAndGet(lineIds.size());
					errorMsgMap.computeIfAbsent(
							"@Error@ @Lines@ key=" + key + " " + e.getLocalizedMessage() + " @Qty@: ",
							k -> new LongAdder()).add(lineIds.size());
				}
			});

			// Recompute the batch DocumentAmt once (the per-line afterSave is
			// skipped by MInvoiceBatchLineBulk during phase 2).
			DB.executeUpdateEx(
					"UPDATE C_InvoiceBatch h SET DocumentAmt = COALESCE("
					+ "(SELECT SUM(LineTotalAmt) FROM C_InvoiceBatchLine l "
					+ " WHERE h.C_InvoiceBatch_ID=l.C_InvoiceBatch_ID AND l.IsActive='Y'), 0) "
					+ "WHERE C_InvoiceBatch_ID=?",
					new Object[] { getRecord_ID() }, null);

			// Phase 3: complete every invoice of the batch in parallel.
			// A batch line that still has no invoice or no invoice-line means
			// some earlier phase failed; in that case skip phase 3 and leave
			// the partial state for the user to inspect / re-run.
			int pendingLines = new Query(getCtx(), I_C_InvoiceBatchLine.Table_Name,
					"C_InvoiceBatch_ID = ? AND (C_Invoice_ID IS NULL OR C_InvoiceLine_ID IS NULL)", null)
					.setParameters(getRecord_ID())
					.count();

			if (pendingLines > 0) {
				addLog(0, null, null,
						"@Error@ @C_InvoiceBatchLine_ID@ @Qty@: " + pendingLines);
			} else {
				List<Integer> allInvoiceIds = new Query(getCtx(), MInvoice.Table_Name,
						"C_Invoice_ID IN (SELECT DISTINCT C_Invoice_ID FROM C_InvoiceBatchLine"
						+ " WHERE C_InvoiceBatch_ID = ? AND C_Invoice_ID IS NOT NULL)"
						+ " AND DocStatus IN ('DR','IP','IN')", null)
						.setParameters(getRecord_ID())
						.getIDsAsList();

				final AtomicInteger processOk = new AtomicInteger();
				final AtomicInteger processErr = new AtomicInteger();
				if (!allInvoiceIds.isEmpty()) {
					final AtomicBoolean hasError = new AtomicBoolean();
					// Capture the gRPC context of the caller thread; workers
					// re-attach it so Env.getCtx() returns the real Properties.
					final io.grpc.Context grpcCtx = io.grpc.Context.current();
					// Dedicated DB-bound pool: 2 x cores, named worker threads.
					int phase3Parallelism = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
					ForkJoinPool phase3Pool = new ForkJoinPool(phase3Parallelism,
							pool -> {
								ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
								t.setName("GenInvBatch-P3-Worker-" + t.getPoolIndex());
								return t;
							},
							null, false);
					try {
						phase3Pool.submit(() -> allInvoiceIds.parallelStream().forEach(invId -> {
							try {
								grpcCtx.wrap(() -> Trx.run(trx -> {
									MInvoice invoice = new MInvoice(getCtx(), invId, trx);
									// Recompute BEFORE enabling the bulk flag: if the
									// recompute throws, the invoice stays consistent
									// and the afterSave bypass is never activated.
									recomputeInvoiceHeader(invoice, trx);
									invoice.setIsBulkComplete(true);
									invoice.setDocAction(documentAction);
									invoice.processIt(documentAction);
									invoice.saveEx();
									invoice.load(trx);
									processOk.incrementAndGet();
									MDocType docType = (MDocType) invoice.getC_DocTypeTarget();
									String key = "OK | " + docType.getName() + " "
											+ invoice.getDocStatusName() + " @Qty@: ";
									resultMsgMap.computeIfAbsent(key, k -> new LongAdder()).increment();
								})).run();
							} catch (Exception e) {
								hasError.set(true);
								processErr.incrementAndGet();
								try {
									MInvoice invoice = new MInvoice(getCtx(), invId, null);
									MDocType docType = (MDocType) invoice.getC_DocTypeTarget();
									String key = "@Error@ " + docType.getName() + " "
											+ invoice.getDocStatusName() + " @Qty@: ";
									resultMsgMap.computeIfAbsent(key, k -> new LongAdder()).increment();
								} catch (Exception ignored) {
									// fall back to the raw exception message below
								}
								errorMsgMap.computeIfAbsent(
										e.getLocalizedMessage() + " @Qty@: ",
										k -> new LongAdder()).increment();
							}
						})).get();
					} catch (Exception e) {
						throw new AdempiereException("PHASE 3 failed: " + e.getLocalizedMessage(), e);
					} finally {
						phase3Pool.shutdown();
					}
					created = processOk.get();
					if (processErr.get() > 0) {
						addLog(0, null, null,
								"@Error@ @C_Invoice_ID@ @Qty@: " + processErr.get());
					}

					// Canonical BPartner recompute, one pass per affected BP.
					// setTotalOpenBalance / setActualLifeTimeValue rebuild the
					// values from C_Invoice_v (multi-currency, open-amount and
					// unallocated payments) and are the source of truth that
					// replaces the per-invoice deltas skipped when
					// MInvoice.isBulkComplete == true.
					int[] affectedBPs = DB.getIDsEx(null,
							"SELECT DISTINCT i.C_BPartner_ID FROM C_Invoice i "
							+ "WHERE i.C_Invoice_ID IN (SELECT DISTINCT C_Invoice_ID FROM C_InvoiceBatchLine "
							+ "                         WHERE C_InvoiceBatch_ID = ? AND C_Invoice_ID IS NOT NULL)",
							getRecord_ID());
					for (int bpId : affectedBPs) {
						// One transaction per BP so the parent process trx
						// stays free of long-held locks.
						Trx.run(trx -> {
							MBPartner bp = new MBPartner(getCtx(), bpId, trx);
							bp.setTotalOpenBalance();
							bp.setActualLifeTimeValue();
							if (bp.getFirstSale() == null) {
								Timestamp firstSale = DB.getSQLValueTSEx(trx,
										"SELECT MIN(DateInvoiced) FROM C_Invoice "
										+ "WHERE C_BPartner_ID = ? AND IsSOTrx='Y' AND DocStatus IN ('CO','CL')",
										bpId);
								if (firstSale != null) bp.setFirstSale(firstSale);
							}
							bp.saveEx();
						});
					}

					// Close the batch only on a clean Complete run.
					if (!hasError.get() && DocumentEngine.ACTION_Complete.equals(documentAction)) {
						batch.load(null);
						batch.setProcessed(true);
						batch.saveEx();
					}
				}
			}
			for (Map.Entry<String, LongAdder> entry : resultMsgMap.entrySet()) {
				addLog(0, null, null, entry.getKey() + entry.getValue().sum());
			}
			for (Map.Entry<String, LongAdder> entry : errorMsgMap.entrySet()) {
				addLog(0, null, null, entry.getKey() + entry.getValue().sum());
			}
			return "@Created@: " + created;
		} catch (Exception e) {
			addLog(0, null, null, "@Error@ " + e.getLocalizedMessage());
			return "@Error@ " + e.getLocalizedMessage();
		} finally {
			if (processing) {
				DB.executeUpdateEx(
						"UPDATE C_InvoiceBatch "
						+ "SET SP030C_PR_GenerateInvoices='G', SP030C_PR_ProcessInvoices='C' "
						+ "WHERE C_InvoiceBatch_ID=?",
						new Object[]{ getRecord_ID() }, null);
			}
		}
	}

	/**
	 * Returns the distinct (C_BPartner_ID, C_BPartner_Location_ID) keys of
	 * the batch that still have pending lines and no pre-existing invoice in
	 * {@code invoiceIdByKey}. A sample C_InvoiceBatchLine_ID is included so
	 * the header can be built from a real line.
	 */
	private List<int[]> loadDistinctPendingKeys(ConcurrentMap<String, Integer> invoiceIdByKey) {
		String sql = "SELECT C_BPartner_ID, C_BPartner_Location_ID, MIN(C_InvoiceBatchLine_ID) "
				+ "FROM C_InvoiceBatchLine "
				+ "WHERE C_InvoiceBatch_ID = ? "
				+ "  AND C_Invoice_ID IS NULL AND C_InvoiceLine_ID IS NULL "
				+ "GROUP BY C_BPartner_ID, C_BPartner_Location_ID";
		List<int[]> result = new ArrayList<>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// null trx: read on a fresh connection so the parent process trx
			// does not stay idle across the long parallel phases.
			ps = DB.prepareStatement(sql, (String) null);
			ps.setInt(1, getRecord_ID());
			rs = ps.executeQuery();
			while (rs.next()) {
				int bp = rs.getInt(1);
				int loc = rs.getInt(2);
				int sample = rs.getInt(3);
				String key = bp + "|" + loc;
				if (!invoiceIdByKey.containsKey(key)) {
					result.add(new int[] { bp, loc, sample });
				}
			}
		} catch (Exception e) {
			throw new AdempiereException("Error loading pending keys: " + e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, ps);
		}
		return result;
	}

	/**
	 * Loads every pending C_InvoiceBatchLine_ID grouped by (BPartner|Location)
	 * key and ordered inside the group for stable line numbering.
	 */
	private Map<String, List<Integer>> loadPendingLinesGroupedByKey() {
		String sql = "SELECT C_BPartner_ID, C_BPartner_Location_ID, C_InvoiceBatchLine_ID "
				+ "FROM C_InvoiceBatchLine "
				+ "WHERE C_InvoiceBatch_ID = ? "
				+ "  AND C_InvoiceLine_ID IS NULL "
				+ "ORDER BY C_BPartner_ID, C_BPartner_Location_ID, C_InvoiceBatchLine_ID";
		Map<String, List<Integer>> result = new HashMap<>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// null trx: see comment in loadDistinctPendingKeys.
			ps = DB.prepareStatement(sql, (String) null);
			ps.setInt(1, getRecord_ID());
			rs = ps.executeQuery();
			while (rs.next()) {
				String key = rs.getInt(1) + "|" + rs.getInt(2);
				result.computeIfAbsent(key, k -> new ArrayList<>()).add(rs.getInt(3));
			}
		} catch (Exception e) {
			throw new AdempiereException("Error loading pending lines: " + e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, ps);
		}
		return result;
	}

	/**
	 * Inserts every C_InvoiceLine belonging to one invoice inside the given
	 * transaction, updates the matching C_InvoiceBatchLine with the FK and,
	 * when applicable, links the originating C_TimeExpenseLine back to the
	 * new invoice line. Uses {@link MInvoiceLineBulk} to bypass the per-save
	 * header-tax recompute and an in-memory price-list cache to avoid
	 * recomputing MProductPricing for the same (PriceList, Product) pair.
	 */
	private void insertLinesForInvoice(MInvoiceBatch batch, int invoiceId, List<Integer> lineIds,
			String trxName) {
		MInvoice invoice = new MInvoice(getCtx(), invoiceId, trxName);
		int nextLineNo = DB.getSQLValue(trxName,
				"SELECT COALESCE(MAX(Line),0) FROM C_InvoiceLine WHERE C_Invoice_ID=?",
				invoiceId);
		Map<Long, BigDecimal> priceCache = new HashMap<>();

		for (int lineId : lineIds) {
			MInvoiceBatchLine line = new MInvoiceBatchLine(getCtx(), lineId, trxName);
			line.setIsDirectLoad(true);
			nextLineNo += 10;

			MInvoiceLine invoiceLine = new MInvoiceLineBulk(invoice);
			invoiceLine.setIsDirectLoad(true);
			invoiceLine.setLine(nextLineNo);
			invoiceLine.setDescription(line.getDescription());
			if (line.getC_Charge_ID() > 0) {
				invoiceLine.setC_Charge_ID(line.getC_Charge_ID());
			} else if (line.get_ValueAsInt("SP030C_Product_ID") > 0) {
				invoiceLine.setM_Product_ID(line.get_ValueAsInt("SP030C_Product_ID"));
			}
			invoiceLine.setQty(line.getQtyEntered());

			long cacheKey = ((long) invoice.getM_PriceList_ID() << 32)
					| (invoiceLine.getM_Product_ID() & 0xFFFFFFFFL);
			BigDecimal priceList = priceCache.get(cacheKey);
			if (priceList == null) {
				MProductPricing pp = new MProductPricing(invoiceLine.getM_Product_ID(),
						invoice.getC_BPartner_ID(), BigDecimal.ZERO, invoice.isSOTrx(), trxName);
				pp.setM_PriceList_ID(invoice.getM_PriceList_ID());
				pp.setPriceDate(batch.getDateDoc());
				priceList = pp.getPriceList();
				priceCache.put(cacheKey, priceList);
			}
			invoiceLine.setPriceList(priceList);

			invoiceLine.setPrice(line.getPriceEntered());
			invoiceLine.setC_Tax_ID(line.getC_Tax_ID());
			invoiceLine.setTaxAmt(line.getTaxAmt());
			invoiceLine.setLineNetAmt(line.getLineNetAmt());
			invoiceLine.setLineTotalAmt(line.getLineTotalAmt());
			if (Env.ZERO.compareTo(priceList) != 0) {
				int precision = MCurrency.getStdPrecision(getCtx(), batch.getC_Currency_ID());
				BigDecimal discount = priceList.subtract(line.getPriceEntered())
						.multiply(new BigDecimal(100))
						.divide(priceList, precision, RoundingMode.HALF_UP);
				invoiceLine.set_ValueOfColumn("Discount", discount);
			}
			int expenseLineId = line.get_ValueAsInt("SP030C_TimeExpenseLine_ID");
			MTimeExpenseLine expenseLine = null;
			if (expenseLineId > 0) {
				expenseLine = new MTimeExpenseLine(getCtx(), expenseLineId, trxName);
				invoiceLine.setC_Campaign_ID(expenseLine.getC_Campaign_ID());
				invoiceLine.setC_Project_ID(expenseLine.getC_Project_ID());
				invoiceLine.setC_ProjectPhase_ID(expenseLine.getC_ProjectPhase_ID());
				invoiceLine.setC_ProjectTask_ID(expenseLine.getC_ProjectTask_ID());
			}
			invoiceLine.setC_Activity_ID(line.getC_Activity_ID());
			invoiceLine.set_ValueOfColumn("SP030C_ContractLine_ID", line.get_ValueAsInt("SP030C_ContractLine_ID"));
			invoiceLine.set_ValueOfColumn("SP030C_TimeExpenseLine_ID", line.get_ValueAsInt("SP030C_TimeExpenseLine_ID"));

			invoiceLine.saveEx();
			if (expenseLine != null) {
				expenseLine.setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
				expenseLine.setIsInvoiced(true);
				expenseLine.saveEx();
			}
			line.setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
			line.saveEx();
		}
	}

	/**
	 * Rebuilds the invoice header totals (C_InvoiceTax, TotalLines, GrandTotal)
	 * in a single pass, avoiding the per-line updateHeaderTax() cost.
	 */
	private void recomputeInvoiceHeader(MInvoice invoice, String trxName) {
		invoice.calculateTaxTotal();
		// Tax-included invoices have taxes baked into LineNetAmt, so GrandTotal
		// equals TotalLines. Otherwise taxes come from C_InvoiceTax and are
		// added on top.
		String taxTerm = invoice.isTaxIncluded()
				? "0"
				: "COALESCE((SELECT SUM(TaxAmt) FROM C_InvoiceTax it WHERE it.C_Invoice_ID=i.C_Invoice_ID),0)";
		String sql = "UPDATE C_Invoice i SET"
				+ " TotalLines=COALESCE((SELECT SUM(LineNetAmt) FROM C_InvoiceLine il WHERE il.C_Invoice_ID=i.C_Invoice_ID),0),"
				+ " GrandTotal=COALESCE((SELECT SUM(LineNetAmt) FROM C_InvoiceLine il WHERE il.C_Invoice_ID=i.C_Invoice_ID),0)"
				+ "           +" + taxTerm
				+ " WHERE C_Invoice_ID=?";
		DB.executeUpdateEx(sql, new Object[] { invoice.getC_Invoice_ID() }, trxName);
		invoice.load(trxName);
	}
}
