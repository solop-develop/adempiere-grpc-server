/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 *****************************************************************************/

package org.solop.process;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.X_C_DunningInterestRate;
import org.adempiere.core.domains.models.X_C_DunningInterestVersion;
import org.adempiere.core.domains.models.X_S_TimeExpense;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.solop.util.DunningInterestResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generated Process for (Generate Overdue Interest)
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 */
public class GenerateOverdueInterest extends GenerateOverdueInterestAbstract
{
	private int created = 0;
	private int bPartnerID = 0;
	private BigDecimal totalAmtPartner = Env.ZERO;
	private BigDecimal totalAmount = Env.ZERO;

	private Timestamp dateDoc = null;
	private String docAction = null;

	/** Cache of versions per dunning interest type. Loaded lazily — the
	 *  first allocation that needs a version for a given type triggers
	 *  a single query that returns all versions of that type; subsequent
	 *  allocations resolve the applicable version walking the list in
	 *  memory. Justified here because one invoice can have several
	 *  allocations and several invoices typically share the same type. */
	private final Map<Integer, List<X_C_DunningInterestVersion>> versionsByType = new HashMap<>();

	private final SimpleDateFormat dateFormat = DisplayType.getDateFormat(DisplayType.Date);

	@Override
	protected void prepare()
	{
		super.prepare();
		dateDoc = getDateDoc() != null ? getDateDoc() : new Timestamp(System.currentTimeMillis());
		docAction = getDocAction() != null ? getDocAction() : MTimeExpense.DOCACTION_Complete;
	}

	@Override
	protected String doIt() throws Exception
	{
		String where = I_C_Invoice.COLUMNNAME_IsSOTrx + " = 'Y'"
				+ " AND " + I_C_Invoice.COLUMNNAME_IsPaid + " = 'Y'"
				+ " AND DaysDue > 0"
				+ " AND PayDate <= ?"
				+ " AND C_Invoice_ID NOT IN ("
				+ "    SELECT tel.Source_Invoice_ID FROM S_TimeExpenseLine tel"
				+ "    JOIN S_TimeExpense te ON te.S_TimeExpense_ID = tel.S_TimeExpense_ID"
				+ "    WHERE tel.Source_Invoice_ID IS NOT NULL"
				+ "      AND te.DocStatus NOT IN ('VO','RE'))";

		if (getOrgId() > 0)
			where += " AND " + I_C_Invoice.COLUMNNAME_AD_Org_ID + " = " + getOrgId();

		if (getDunningInterestTypeId() > 0) {
			where += " AND EXISTS (SELECT 1 FROM C_BPartner bp WHERE bp.C_BPartner_ID = C_Invoice.C_BPartner_ID"
					+ "      AND bp.C_DunningInterestType_ID = " + getDunningInterestTypeId() + ")";
		} else {
			where += " AND EXISTS (SELECT 1 FROM C_BPartner bp WHERE bp.C_BPartner_ID = C_Invoice.C_BPartner_ID"
					+ "      AND bp.C_DunningInterestType_ID IS NOT NULL)";
		}

		if (getBPartnerId() > 0)
			where += " AND " + I_C_Invoice.COLUMNNAME_C_BPartner_ID + " = " + getBPartnerId();

		List<MInvoice> invoiceList = new Query(getCtx(), I_C_Invoice.Table_Name, where, get_TrxName())
				.setParameters(dateDoc)
				.setClient_ID()
				.setOrderBy(I_C_Invoice.COLUMNNAME_C_BPartner_ID)
				.list();

		for (MInvoice invoice : invoiceList) {
			processInvoice(invoice);
		}

		if (bPartnerID > 0 && totalAmtPartner.compareTo(Env.ZERO) > 0) {
			MBPartner bPartner = new MBPartner(getCtx(), bPartnerID, get_TrxName());
			addLog(0, null, totalAmtPartner, Msg.parseTranslation(getCtx(),
					"@C_BPartner_ID@: " + bPartner.getValue() + "_" + bPartner.getName()
					+ " | @TotalAmt@: " + totalAmtPartner));
		}

		return Msg.parseTranslation(getCtx(),
				"@DateDoc@: " + dateFormat.format(dateDoc)
				+ " | @TotalAmt@: " + totalAmount
				+ " | @Created@: " + created);
	}

	private void processInvoice(MInvoice invoice)
	{
		if (invoice.getC_BPartner_ID() != bPartnerID) {
			if (bPartnerID > 0 && totalAmtPartner.compareTo(Env.ZERO) > 0) {
				MBPartner prev = new MBPartner(getCtx(), bPartnerID, get_TrxName());
				addLog(0, null, totalAmtPartner, Msg.parseTranslation(getCtx(),
						"@C_BPartner_ID@: " + prev.getValue() + "_" + prev.getName()
						+ " | @TotalAmt@: " + totalAmtPartner));
			}
			bPartnerID = invoice.getC_BPartner_ID();
			totalAmtPartner = Env.ZERO;
		}

		MPaymentTerm payTerm = (MPaymentTerm) invoice.getC_PaymentTerm();
		BigDecimal daysGrace = new BigDecimal(payTerm != null ? payTerm.getGraceDays() : 0);

		MAllocationHdr[] allocations = MAllocationHdr.getOfInvoice(invoice.getCtx(),
				invoice.getC_Invoice_ID(),
				" AND h.DocStatus = 'CO' ",
				" Order By h.DateTrx", invoice.get_TrxName());

		if (allocations.length > 1) {
			for (MAllocationHdr alloc : allocations) {
				BigDecimal amt = getAllocatedAmt(invoice.get_ID(), alloc.get_ID());
				if (amt == null || amt.signum() <= 0)
					continue;

				String sql = "SELECT coalesce(DaysDue,0) FROM C_AllocationLine"
						+ " WHERE C_AllocationHdr_ID = " + alloc.get_ID()
						+ " AND C_Invoice_ID = " + invoice.get_ID();
				BigDecimal daysDue = DB.getSQLValueBDEx(get_TrxName(), sql);
				if (daysDue == null || daysDue.compareTo(daysGrace) <= 0)
					continue;

				DunningInterestResolver r = buildResolver(invoice, alloc.getDateTrx());
				if (!r.isApplicable())
					return;

				X_C_DunningInterestRate rate = r.getRate(invoice.getC_Currency_ID(), daysDue.intValue());
				BigDecimal percent = amt.multiply(Env.ONEHUNDRED)
						.divide(invoice.getGrandTotal(), 2, RoundingMode.HALF_UP);
				BigDecimal finalRate = DunningInterestResolver.computeFinalRate(daysDue, rate.getRate());

				createExpenseReport(invoice, r, alloc, percent, daysDue, rate, finalRate);
			}
		} else {
			BigDecimal daysDue = new BigDecimal(invoice.get_ValueAsInt("DaysDue"));
			if (daysDue.compareTo(daysGrace) <= 0)
				return;

			Timestamp refDate = allocations.length == 1 ? allocations[0].getDateTrx() : dateDoc;
			DunningInterestResolver interestResolver = buildResolver(invoice, refDate);
			if (!interestResolver.isApplicable())
				return;

			X_C_DunningInterestRate rate = interestResolver.getRate(invoice.getC_Currency_ID(), daysDue.intValue());
			BigDecimal finalRate = DunningInterestResolver.computeFinalRate(daysDue, rate.getRate());

			createExpenseReport(invoice, interestResolver, null, null, daysDue, rate, finalRate);
		}
	}

	private DunningInterestResolver buildResolver(MInvoice invoice, Timestamp date) {
		// Resolve type once; needed to look up cached version
		int typeId = getDunningInterestTypeId() > 0
				? getDunningInterestTypeId()
				: ((MBPartner) invoice.getC_BPartner()).get_ValueAsInt("C_DunningInterestType_ID");

		X_C_DunningInterestVersion cachedVersion = findCachedVersion(typeId, date);

		DunningInterestResolver.Builder resolverBuilder = DunningInterestResolver.newBuilder(getCtx(), get_TrxName())
				.setInvoice(invoice)
				.setDateDoc(date)
				.setOverrideTypeId(getDunningInterestTypeId())
				.setStrict(true);
		if (cachedVersion != null)
			resolverBuilder.setVersion(cachedVersion);

		DunningInterestResolver interestResolver = resolverBuilder.build();

		// Cache the version that the resolver ended up using (covers the
		// case where the resolver loaded it from DB on a cache miss).
		if (cachedVersion == null && interestResolver.isApplicable() && interestResolver.getVersion() != null)
			versionsByType.computeIfAbsent(typeId, k -> new ArrayList<>()).add(interestResolver.getVersion());

		return interestResolver;
	}

	private X_C_DunningInterestVersion findCachedVersion(int typeId, Timestamp date) {
		List<X_C_DunningInterestVersion> versions = versionsByType.get(typeId);
		if (versions == null)
			return null;
		for (X_C_DunningInterestVersion version : versions) {
			Timestamp validFrom = version.getValidFrom();
			Timestamp validTo = version.getValidTo();
			if (validFrom != null && validFrom.after(date)) continue;
			if (validTo != null && validTo.before(date)) continue;
			return version;
		}
		return null;
	}

	private void createExpenseReport(MInvoice invoice, DunningInterestResolver r,
			MAllocationHdr allocation, BigDecimal percent, BigDecimal daysDue,
			X_C_DunningInterestRate rate, BigDecimal finalRate)
	{
		MTimeExpense expenseReport = new MTimeExpense(getCtx(), 0, get_TrxName());
		expenseReport.setAD_Org_ID(invoice.getAD_Org_ID());
		expenseReport.setDocStatus(X_S_TimeExpense.DOCSTATUS_Drafted);
		expenseReport.setDocAction(X_S_TimeExpense.DOCACTION_Complete);
		expenseReport.setC_BPartner_ID(invoice.getC_BPartner_ID());
		expenseReport.set_ValueOfColumn("Bill_BPartner_ID", invoice.getC_BPartner_ID());
		expenseReport.setDateReport(dateDoc);
		expenseReport.setM_PriceList_ID(invoice.getM_PriceList_ID());
		expenseReport.setDescription(Msg.parseTranslation(getCtx(),
				"@Generated@ @C_DunningInterestType_ID@: " + r.getType().getName()
				+ " | @C_Invoice_ID@ @DocumentNo@: " + invoice.getDocumentNo()));

		MOrgInfo orgInfo = MOrgInfo.get(getCtx(), invoice.getAD_Org_ID(), get_TrxName());
		if (orgInfo.getM_Warehouse_ID() > 0)
			expenseReport.setM_Warehouse_ID(orgInfo.getM_Warehouse_ID());

		if (invoice.get_ValueAsInt("S_Contract_ID") > 0)
			expenseReport.set_ValueOfColumn("S_Contract_ID", invoice.get_ValueAsInt("S_Contract_ID"));

		expenseReport.saveEx();

		BigDecimal totalReport = Env.ZERO;
		int chargeId = r.getType().getC_Charge_ID();
		int currencyPrecision = MCurrency.getStdPrecision(getCtx(), invoice.getC_Currency_ID());

		for (MInvoiceLine iLine : invoice.getLines(true)) {
			BigDecimal capital = (percent != null)
					? iLine.getLineTotalAmt().multiply(percent.divide(Env.ONEHUNDRED))
					: iLine.getLineTotalAmt();
			BigDecimal dunning = DunningInterestResolver.computeDunningAmount(capital, finalRate, currencyPrecision);

			MTimeExpenseLine expenseLine = new MTimeExpenseLine(getCtx(), 0, get_TrxName());
			expenseLine.setS_TimeExpense_ID(expenseReport.getS_TimeExpense_ID());
			expenseLine.setDateExpense(expenseReport.getDateReport());
			expenseLine.setC_BPartner_ID(invoice.getC_BPartner_ID());
			expenseLine.setIsTimeReport(false);
			expenseLine.setIsInvoiced(false);
			expenseLine.setC_Currency_ID(invoice.getC_Currency_ID());
			expenseLine.setQty(Env.ONE);
			expenseLine.setExpenseAmt(dunning);
			expenseLine.set_ValueOfColumn("C_Charge_ID", chargeId);
			expenseLine.setSource_Invoice_ID(invoice.getC_Invoice_ID());
			expenseLine.setC_InvoiceLine_ID(iLine.get_ID());
			expenseLine.setC_Activity_ID(iLine.getC_Activity_ID());
			expenseLine.set_ValueOfColumn("S_ContractLine_ID", iLine.get_ValueAsInt("S_ContractLine_ID"));

			if (invoice.getC_Project_ID() > 0)
				expenseLine.setC_Project_ID(invoice.getC_Project_ID());

			String allocationToken = (allocation != null)
					? " | @C_AllocationHdr_ID@ @DocumentNo@: " + allocation.getDocumentNo()
					+ " | @Percent@: " + percent + "%"
					: "";
			String descriptionTpl = "@C_Invoice_ID@ @DocumentNo@: " + invoice.getDocumentNo()
					+ allocationToken
					+ " | @C_DunningInterestType_ID@: " + r.getType().getName()
					+ " | @C_DunningInterestVersion_ID@: " + r.getVersion().getName()
					+ " | @DaysDue@: " + daysDue
					+ " | @C_DunningInterestRate_ID@ @Rate@: " + rate.getRate() + "%"
					+ " | @DaysFrom@-@DaysTo@: " + rate.getDaysFrom() + "-"
					+ (rate.getDaysTo() == 0 ? "*" : rate.getDaysTo())
					+ " | @Rate@ (@DaysDue@/30 * @Rate@): " + finalRate + "%"
					+ " | @LineNetAmt@: " + capital
					+ " | @ExpenseAmt@: " + dunning;
			expenseLine.setDescription(Msg.parseTranslation(getCtx(), descriptionTpl));
			expenseLine.saveEx();

			totalReport = totalReport.add(dunning);
		}

		if (!expenseReport.processIt(docAction))
			throw new AdempiereException("@Error@ " + expenseReport.getProcessMsg());

		expenseReport.saveEx();

		totalAmtPartner = totalAmtPartner.add(totalReport);
		totalAmount = totalAmount.add(totalReport);
		created++;
	}

	/** Returns allocated amount for the given invoice and allocation header,
	 *  converted to the invoice currency. */
	private BigDecimal getAllocatedAmt(int invoiceID, int allocHdrID) {
		BigDecimal retValue = null;
		String sql = "SELECT coalesce(SUM(currencyConvert(al.Amount+al.DiscountAmt+al.WriteOffAmt,ah.C_Currency_ID, i.C_Currency_ID,ah.DateTrx,\n" +
				"COALESCE(p.C_ConversionType_ID, i.C_ConversionType_ID,0), al.AD_Client_ID,al.AD_Org_ID)),0) +\n" +
				"(SELECT coalesce(SUM(currencyconvert_receipt (al.Amount+al.DiscountAmt+al.WriteOffAmt,ah.currencyrate,ah.c_currency_id,i.c_currency_id, al.ad_client_id,al.ad_org_id)),0)\n" +
				"FROM C_AllocationLine al\n" +
				"INNER JOIN C_AllocationHdr ah ON (al.C_AllocationHdr_ID=ah.C_AllocationHdr_ID)\n" +
				"INNER JOIN C_Invoice i ON (al.C_Invoice_ID=i.C_Invoice_ID) \n" +
				"LEFT JOIN C_Payment p ON (al.C_Payment_ID=p.C_Payment_ID) \n" +
				"WHERE al.C_Invoice_ID=?\n" +
				"AND ah.C_AllocationHdr_ID = ?\n" +
				"AND ah.IsActive='Y' AND al.IsActive='Y'\n" +
				"AND ah.IsMultiCurrency = 'Y')\n" +
				"FROM C_AllocationLine al\n" +
				"INNER JOIN C_AllocationHdr ah ON (al.C_AllocationHdr_ID=ah.C_AllocationHdr_ID)\n" +
				"INNER JOIN C_Invoice i ON (al.C_Invoice_ID=i.C_Invoice_ID) \n" +
				"LEFT JOIN C_Payment p ON (al.C_Payment_ID=p.C_Payment_ID) \n" +
				"WHERE al.C_Invoice_ID=?\n" +
				"AND ah.C_AllocationHdr_ID = ?\n" +
				"AND ah.IsActive='Y' AND al.IsActive='Y'\n" +
				"AND ah.IsMultiCurrency = 'N'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, invoiceID);
			pstmt.setInt(2, allocHdrID);
			pstmt.setInt(3, invoiceID);
			pstmt.setInt(4, allocHdrID);
			rs = pstmt.executeQuery();
			if (rs.next())
				retValue = rs.getBigDecimal(1);
		} catch (SQLException e) {
			throw new DBException(e, sql);
		} finally {
			DB.close(rs, pstmt);
		}
		return retValue;
	}
}
