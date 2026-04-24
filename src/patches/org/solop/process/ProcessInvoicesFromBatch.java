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

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceBatch;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import java.sql.Timestamp;
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
 * Process: complete invoices of a C_InvoiceBatch in parallel.
 * <p>
 * Mirrors phase 3 of {@link GenerateInvoiceFromBatch}:
 * <ul>
 *   <li>validates the batch is fully generated before processing</li>
 *   <li>completes each invoice inside its own transaction, in parallel,
 *       using a dedicated ForkJoinPool sized to 2 x available cores</li>
 *   <li>uses {@code isBulkComplete} on MInvoice to skip per-invoice
 *       BPartner / AD_User updates during {@code completeIt()}</li>
 *   <li>recomputes the BPartner totals once per affected BP after all
 *       invoices are completed (replaces the skipped per-invoice deltas)</li>
 *   <li>marks the batch as processed when the action is Complete and
 *       every invoice succeeded</li>
 * </ul>
 */
public class ProcessInvoicesFromBatch extends ProcessInvoicesFromBatchAbstract
{
	private static final String PERF_TAG = "[ProcessInvBatch-PERF]";

	private static void perf(String msg) {
		System.out.println(PERF_TAG + " " + msg);
	}

	/** Per-DocType / DocStatus counters, aggregated into addLog() at the end. */
	private final ConcurrentMap<String, LongAdder> resultMsgMap = new ConcurrentHashMap<>();
	/** Error-message counters (same message is collapsed into a single log row). */
	private final ConcurrentMap<String, LongAdder> errorMsgMap = new ConcurrentHashMap<>();

	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		try {
			if (getRecord_ID() <= 0) {
				throw new AdempiereException("@C_InvoiceBatch_ID@ @NotFound@");
			}

			MInvoiceBatch batch = new MInvoiceBatch(getCtx(), getRecord_ID(), get_TrxName());
			if (batch.get_ID() <= 0) {
				throw new AdempiereException("@C_InvoiceBatch_ID@ @NotFound@");
			}

			String documentAction = getDocAction();
			if (documentAction == null || documentAction.isEmpty()) {
				throw new AdempiereException("@DocAction@ @NotFound@");
			}

			// Fail fast if the batch is not fully generated.
			validateBatchIntegrity();

			// Candidate invoices: every invoice referenced by this batch that is
			// not already in a final state (VO/RE/CO/CL). Matches the query used
			// by the original processInvoices.groovy, which is more permissive
			// than the status filter used inside GenerateInvoiceFromBatch
			// (it also covers AP, NA, WC, ??, etc.).
			List<Integer> allInvoiceIds = new Query(getCtx(), I_C_Invoice.Table_Name,
					"DocStatus NOT IN ('VO','RE','CO','CL')"
					+ " AND EXISTS (SELECT 1 FROM C_InvoiceBatchLine ibl"
					+ "             WHERE ibl.C_InvoiceBatch_ID = ?"
					+ "               AND ibl.C_Invoice_ID = C_Invoice.C_Invoice_ID)",
					get_TrxName())
					.setParameters(getRecord_ID())
					.getIDsAsList();


			if (allInvoiceIds.isEmpty()) {
				addLog(0, null, null, "@NotFound@ @C_Invoice_ID@");
				return "@Processed@: 0";
			}

			// Parallel completion
			final AtomicInteger processOk = new AtomicInteger();
			final AtomicInteger processErr = new AtomicInteger();
			final AtomicBoolean hasError = new AtomicBoolean();
			final String docAction = documentAction;
			// Capture the gRPC context of the caller thread; workers re-attach it
			// so Env.getCtx() returns the real Properties inside each task.
			final io.grpc.Context grpcCtx = io.grpc.Context.current();

			int parallelism = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
			ForkJoinPool pool = new ForkJoinPool(parallelism,
					p -> {
						ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
						t.setName("ProcessInvBatch-Worker-" + t.getPoolIndex());
						return t;
					},
					null, false);
			perf(String.format("PROCESS parallelism=%d  cores=%d",
					parallelism, Runtime.getRuntime().availableProcessors()));

			try {
				pool.submit(() -> allInvoiceIds.parallelStream().forEach(invId -> {
					try {
						grpcCtx.wrap(() -> Trx.run(trx -> {
							MInvoice invoice = new MInvoice(getCtx(), invId, trx);
							// Recompute BEFORE enabling the bulk flag: if the recompute
							// throws, the invoice stays consistent and the afterSave
							// bypass is never activated.
							recomputeInvoiceHeader(invoice, trx);
							invoice.setIsBulkComplete(true);
							invoice.setDocAction(docAction);
							invoice.processIt(docAction);
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
							MInvoice invoice = new MInvoice(getCtx(), invId, get_TrxName());
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
				throw new AdempiereException("@ProcessFailed@: " + e.getLocalizedMessage(), e);
			} finally {
				pool.shutdown();
			}

			int created = processOk.get();

			if (processErr.get() > 0) {
				addLog(0, null, null, "@Error@ @C_Invoice_ID@ @Qty@: " + processErr.get());
			}

			// Canonical BPartner recompute for every BP touched by this batch.
			// Replaces the per-invoice deltas that MInvoice.completeIt() skips
			// when isBulkComplete == true. setTotalOpenBalance / setActualLifeTimeValue
			// rebuild the values from C_Invoice_v and are the source of truth.
			int[] affectedBPs = DB.getIDsEx(get_TrxName(),
					"SELECT DISTINCT i.C_BPartner_ID FROM C_Invoice i "
					+ "WHERE i.C_Invoice_ID IN (SELECT DISTINCT C_Invoice_ID FROM C_InvoiceBatchLine "
					+ "                         WHERE C_InvoiceBatch_ID = ? AND C_Invoice_ID IS NOT NULL)",
					getRecord_ID());
			for (int bpId : affectedBPs) {
				MBPartner bp = new MBPartner(getCtx(), bpId, get_TrxName());
				bp.setTotalOpenBalance();
				bp.setActualLifeTimeValue();
				if (bp.getFirstSale() == null) {
					Timestamp firstSale = DB.getSQLValueTSEx(get_TrxName(),
							"SELECT MIN(DateInvoiced) FROM C_Invoice "
							+ "WHERE C_BPartner_ID = ? AND IsSOTrx='Y' AND DocStatus IN ('CO','CL')",
							bpId);
					if (firstSale != null) bp.setFirstSale(firstSale);
				}
				bp.saveEx();
			}

			// Close the batch only when every invoice succeeded and the action
			// was a full Complete.
			if (!hasError.get() && DocumentEngine.ACTION_Complete.equals(documentAction)) {
				batch.load(get_TrxName());
				batch.setProcessed(true);
				batch.saveEx();
			}

			for (Map.Entry<String, LongAdder> e : resultMsgMap.entrySet()) {
				addLog(0, null, null, e.getKey() + e.getValue().sum());
			}
			for (Map.Entry<String, LongAdder> e : errorMsgMap.entrySet()) {
				addLog(0, null, null, e.getKey() + e.getValue().sum());
			}

			return "@Processed@: " + created;
		} catch (Exception e) {
			addLog(0, null, null, "@Error@ " + e.getLocalizedMessage());
			return "@Error@ " + e.getLocalizedMessage();
		}
	}

	/**
	 * Guards against an incomplete batch. Aborts when any batch line is missing
	 * its invoice or invoice-line reference, or when a referenced invoice no
	 * longer exists in the database.
	 */
	private void validateBatchIntegrity() {
		int pendingLines = DB.getSQLValueEx(get_TrxName(),
				"SELECT COUNT(*) FROM C_InvoiceBatchLine"
				+ " WHERE C_InvoiceBatch_ID = ?"
				+ "   AND (C_Invoice_ID IS NULL OR C_InvoiceLine_ID IS NULL)",
				getRecord_ID());
		if (pendingLines > 0) {
			throw new AdempiereException(
					"@C_InvoiceBatchLine_ID@: @C_Invoice_ID@ @NotFound@ @Qty@: " + pendingLines);
		}

		int missingInvoices = DB.getSQLValueEx(get_TrxName(),
				"SELECT COUNT(DISTINCT ibl.C_Invoice_ID) FROM C_InvoiceBatchLine ibl"
				+ " WHERE ibl.C_InvoiceBatch_ID = ?"
				+ "   AND ibl.C_Invoice_ID IS NOT NULL"
				+ "   AND NOT EXISTS (SELECT 1 FROM C_Invoice i WHERE i.C_Invoice_ID = ibl.C_Invoice_ID)",
				getRecord_ID());
		if (missingInvoices > 0) {
			throw new AdempiereException(
					"@C_Invoice_ID@ @NotFound@ @Qty@: " + missingInvoices);
		}
	}

	/**
	 * Rebuilds the invoice header totals (C_InvoiceTax, TotalLines, GrandTotal)
	 * in a single pass, avoiding the per-line updateHeaderTax() cost.
	 * Same helper as in {@link GenerateInvoiceFromBatch}.
	 */
	private void recomputeInvoiceHeader(MInvoice invoice, String trxName) {
		invoice.calculateTaxTotal();
		DB.executeUpdateEx(
				"UPDATE C_Invoice i SET TotalLines="
				+ "(SELECT COALESCE(SUM(LineNetAmt),0) FROM C_InvoiceLine il WHERE i.C_Invoice_ID=il.C_Invoice_ID) "
				+ "WHERE C_Invoice_ID=?",
				new Object[] { invoice.getC_Invoice_ID() }, trxName);
		String grandSql = invoice.isTaxIncluded()
				? "UPDATE C_Invoice i SET GrandTotal=TotalLines WHERE C_Invoice_ID=?"
				: "UPDATE C_Invoice i SET GrandTotal=TotalLines+"
						+ "(SELECT COALESCE(SUM(TaxAmt),0) FROM C_InvoiceTax it WHERE i.C_Invoice_ID=it.C_Invoice_ID) "
						+ "WHERE C_Invoice_ID=?";
		DB.executeUpdateEx(grandSql, new Object[] { invoice.getC_Invoice_ID() }, trxName);
		invoice.load(trxName);
	}
}
