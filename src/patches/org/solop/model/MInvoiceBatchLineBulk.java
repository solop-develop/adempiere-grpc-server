package org.solop.model;

import org.compiere.model.MInvoiceBatchLine;

import java.util.Properties;

/**
 * {@link MInvoiceBatchLine} variant used for bulk updates in
 * {@code GenerateInvoiceFromBatch}.
 *
 * Skips the parent {@code afterSave}, which recomputes
 * {@code C_InvoiceBatch.DocumentAmt} with a {@code SUM(LineTotalAmt)} over
 * every line of the batch — expensive and accumulative due to MVCC bloat
 * when 100k+ rows are updated inside the same transaction.
 *
 * In this flow only {@code C_Invoice_ID} and {@code C_InvoiceLine_ID} are
 * set on the batch line: {@code LineTotalAmt} does not change, so
 * {@code DocumentAmt} does not either. No recomputation is needed.
 */
public class MInvoiceBatchLineBulk extends MInvoiceBatchLine {

	public MInvoiceBatchLineBulk(Properties ctx, int C_InvoiceBatchLine_ID, String trxName) {
		super(ctx, C_InvoiceBatchLine_ID, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		return success;
	}
}
