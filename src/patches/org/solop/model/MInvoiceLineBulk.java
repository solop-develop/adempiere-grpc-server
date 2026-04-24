package org.solop.model;

import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;

/**
 * {@link MInvoiceLine} variant used for bulk inserts in {@code GenerateInvoiceFromBatch}.
 *
 * Skips {@code updateHeaderTax()} in {@code afterSave}: it does not update
 * {@code C_InvoiceTax}, {@code TotalLines} or {@code GrandTotal} on every
 * line save. The caller is responsible for rebuilding the header once
 * (by calling {@code calculateTaxTotal()} plus the totals UPDATEs) after
 * every line has been inserted and before completing the invoice.
 *
 * The caller must also set {@code Line}, {@code PriceActual}/{@code PriceList}
 * and {@code C_Tax_ID} before {@code saveEx()} to bypass the auto-computations
 * done by the standard {@code beforeSave}
 * ({@code SELECT MAX(Line)}, {@code setPrice()}, {@code setTax()}).
 */
public class MInvoiceLineBulk extends MInvoiceLine {

	public MInvoiceLineBulk(MInvoice invoice) {
		super(invoice);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		return success;
	}
}
