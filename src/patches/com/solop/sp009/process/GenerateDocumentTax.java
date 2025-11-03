/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package com.solop.sp009.process;

import com.solop.sp009.model.MSP009Expedient;
import com.solop.sp009.model.MSP009ExpedientTax;
import com.solop.sp009.model.MSP009Tax;
import com.solop.sp009.util.ImportExportUtil;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPriceList;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Generate Document Tax)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class GenerateDocumentTax extends GenerateDocumentTaxAbstract
{
	private final static String EXPEDIENT_PREFIX = "EXP_";
	private HashMap<Integer, MInvoice> taxDocuments = new HashMap<Integer, MInvoice>();
	private HashMap<Integer, MSP009Tax> taxToGenerated = new HashMap<Integer, MSP009Tax>();
	private int Max_Sequence = 9999;
	BigDecimal taxAmount = Env.ZERO;
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		
		AtomicInteger seqNo = new AtomicInteger(0);
		
		if (isSelection()) {
			getSelectionKeys()
				.forEach(selectionId -> {
					MSP009Tax tax = new MSP009Tax(getCtx(), selectionId, get_TrxName());
					String baseForTax = Optional.ofNullable(tax.getSP009_BaseForTax()).orElse("");
					int sequence  = baseForTax.equals(MSP009Tax.SP009_BASEFORTAX_InvoiceBasePlusGeneratedTaxes) ? Max_Sequence : seqNo.incrementAndGet(); 
					taxToGenerated.put(sequence, tax);
				});
			
			taxToGenerated
				.entrySet()
				.stream()
				.sorted(Comparator.comparing(item -> item.getKey()))
				.forEach(taxItem -> {
					MSP009Tax tax = taxItem.getValue();
					BigDecimal baseForTax = Env.ZERO;
					BigDecimal baseAmount = getSelectionAsBigDecimal(tax.get_ID(), EXPEDIENT_PREFIX.concat(MSP009Expedient.COLUMNNAME_SP009_BaseAmtInvoices));
					baseForTax = (taxItem.getKey() == Max_Sequence) ? baseAmount.add(taxAmount) : baseAmount;
					int expedientId = getSelectionAsInt(tax.get_ID(), EXPEDIENT_PREFIX.concat(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID));
					generateTaxDocument(tax, baseForTax, expedientId);
				});
			
			taxDocuments.entrySet().forEach(taxDocumentIndex ->{
				MInvoice taxDocument = taxDocumentIndex.getValue();
				taxDocument.processIt(getDocAction());
				taxDocument.saveEx();
				addLog(taxDocument.get_ID(), taxDocument.getDateAcct(), taxDocument.getGrandTotal(), taxDocument.getDescription());
			});
			openResult(I_C_Invoice.Table_Name);
		}
		return "@OK@";
	}
	
	/**
	 * Generate Tax Document
	 * @param tax
	 * @param baseAmount
	 * @param expedientId
	 */
	private void generateTaxDocument(MSP009Tax tax, BigDecimal baseAmount,int expedientId) {
		
		MInvoice taxDocument = Optional.ofNullable(taxDocuments.get(tax.getC_BPartner_ID())).orElse(new MInvoice(getCtx(), 0, get_TrxName()));
		if (taxDocument.get_ID() == 0) { 
			taxDocument.setC_DocTypeTarget_ID(getDocTypeId());
			taxDocument.setC_BPartner_ID(tax.getC_BPartner_ID());
			taxDocument.setDateInvoiced(getDateDoc());
			taxDocument.setDateAcct(getDateDoc());
			taxDocument.setDocumentNo(getDocumentNo());
			taxDocument.set_ValueOfColumn(ImportExportUtil.COLUMNNAME_SP009_IsExpedientTax, true);
			if (getRefInvoiceId() > 0)
				taxDocument.set_ValueOfColumn(ImportExportUtil.COLUMNNAME_SP009_RefInvoice_ID, getRefInvoiceId());
			taxDocument.setIsSOTrx(false);
			MCurrency currency = MCurrency.get(getCtx(), getCurrencyId());
			Optional<MPriceList> maybePriceList = Optional.ofNullable(MPriceList.getDefault(getCtx(), false, currency.getISO_Code()));
			maybePriceList.ifPresent(priceList -> taxDocument.setM_PriceList_ID(priceList.get_ID()));
			if (expedientId > 0)
				taxDocument.set_ValueOfColumn(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID, expedientId);
			taxDocument.saveEx();
			
			if (getProjectId() == 0) {
				taxDocument.setC_Project_ID(0);
				taxDocument.save();
			}
				
			//Put to Array
			taxDocuments.put(tax.getC_BPartner_ID(), taxDocument);
		} 
		MInvoiceLine taxDocumentLine = new MInvoiceLine(taxDocument);
		taxDocumentLine.setC_Charge_ID(tax.getC_Charge_ID());
		taxDocumentLine.setQty(Env.ONE);
		taxDocumentLine.setPrice(baseAmount.multiply(tax.getRate().divide(Env.ONEHUNDRED)));
		taxDocumentLine.setDescription("(".concat(baseAmount.toString()).concat(" * ")
				.concat(tax.getRate().toString())
				.concat("% ) = ")
				.concat(taxDocumentLine.getPriceEntered().toString()));
		
		if (getRefInvoiceId() > 0)
			taxDocumentLine.set_ValueOfColumn(ImportExportUtil.COLUMNNAME_SP009_RefInvoice_ID, getRefInvoiceId());
		taxDocumentLine.saveEx();
		
		if (getProjectId() == 0) {
			taxDocumentLine.setC_Project_ID(0);
			taxDocumentLine.save();
		}
		//Create Expedient Tax Reference
		MSP009ExpedientTax expedientTax = new MSP009ExpedientTax(getCtx(), 0, get_TrxName());
		expedientTax.setC_Invoice_ID(taxDocument.get_ID());
		expedientTax.setSP009_Expedient_ID(expedientId);
		expedientTax.setSP009_Tax_ID(tax.get_ID());
		expedientTax.setTaxBaseAmt(baseAmount);
		expedientTax.setTaxAmt(taxDocumentLine.getPriceEntered());
		expedientTax.setProcessed(true);
		expedientTax.saveEx();
		taxAmount = taxAmount.add(expedientTax.getTaxAmt());
		
	}
}