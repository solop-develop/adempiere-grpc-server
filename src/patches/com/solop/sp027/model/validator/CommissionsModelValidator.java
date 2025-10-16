/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package com.solop.sp027.model.validator;

import org.adempiere.core.domains.models.I_C_BP_Relation;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.X_C_BP_Relation;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gabriel Escalona
 *
 */
public class CommissionsModelValidator implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(CommissionsModelValidator.class);
	/** Client */
	private int clientId = -1;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		engine.addDocValidate(I_C_Order.Table_Name, this);
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}

	@Override
	public String modelChange(PO entity, int type) throws Exception {

		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		if(timing == TIMING_AFTER_COMPLETE) {
			if(entity.get_TableName().equals(I_C_Order.Table_Name)) {
				MOrder order = (MOrder) entity;

				MTable commissionDocTypeTable = MTable.get(order.getCtx(), "SP027_CommissionDocType");
				if (commissionDocTypeTable == null || commissionDocTypeTable.get_ID() <= 0){
					return null;
				}
				String whereClause = "EXISTS (SELECT 1 FROM SP027_CommissionDocType cdt WHERE cdt.C_Commission_ID = C_Commission.C_Commission_ID " +
						" AND cdt.C_DocType_ID = ?)";
				List<Integer> commissionIds = new Query(order.getCtx(), MCommission.Table_Name, whereClause, order.get_TrxName())
						.setParameters(order.getC_DocTypeTarget_ID())
						.getIDsAsList();

				commissionIds.forEach(commissionId -> {
					MCommission commissionDefinition = new MCommission(order.getCtx(), commissionId, order.get_TrxName());

					int documentTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_SalesCommission, order.getAD_Org_ID());
					MCommissionRun commissionRun = new MCommissionRun(commissionDefinition);
					commissionRun.setDateDoc(TimeUtil.getDay(new Timestamp(System.currentTimeMillis())));
					commissionRun.setC_DocType_ID(documentTypeId);
					commissionRun.setDescription(Msg.parseTranslation(order.getCtx(), "@Generate@: @C_Order_ID@ - " + order.getDocumentNo()));
					commissionRun.set_ValueOfColumn("SP027_Order_ID", order.getC_Order_ID());
					commissionRun.set_ValueOfColumn("C_Currency_ID", order.getC_Currency_ID());
					commissionRun.setAD_Org_ID(order.getAD_Org_ID());
					commissionRun.saveEx();
					//	Process commission
					commissionRun.setDocStatus(MCommissionRun.DOCSTATUS_Drafted);
					//	Complete
					if(commissionRun.processIt(MCommissionRun.DOCACTION_Complete)) {
						commissionRun.updateFromAmt();
						commissionRun.saveEx();
						createOrderFromCommission(commissionDefinition, commissionRun, order, null);
					}
				});
			}

			if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) entity;

				MTable commissionDocTypeTable = MTable.get(invoice.getCtx(), "SP027_CommissionDocType");
				if (commissionDocTypeTable == null || commissionDocTypeTable.get_ID() <= 0){
					return null;
				}
				String whereClause = "EXISTS (SELECT 1 FROM SP027_CommissionDocType cdt WHERE cdt.C_Commission_ID = C_Commission.C_Commission_ID " +
						" AND cdt.C_DocType_ID = ?)";
				List<Integer> commissionIds = new Query(invoice.getCtx(), MCommission.Table_Name, whereClause, invoice.get_TrxName())
						.setParameters(invoice.getC_DocTypeTarget_ID())
						.getIDsAsList();

				AtomicReference<BigDecimal> totalCommissionAmount = new AtomicReference<>();
				commissionIds.forEach(commissionId -> {
					MCommission commissionDefinition = new MCommission(invoice.getCtx(), commissionId, invoice.get_TrxName());

					int documentTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_SalesCommission, invoice.getAD_Org_ID());
					MCommissionRun commissionRun = new MCommissionRun(commissionDefinition);
					commissionRun.setDateDoc(TimeUtil.getDay(new Timestamp(System.currentTimeMillis())));
					commissionRun.setC_DocType_ID(documentTypeId);
					commissionRun.setDescription(Msg.parseTranslation(invoice.getCtx(), "@Generate@: @C_Invoice_ID@ - " + invoice.getDocumentNo()));
					commissionRun.set_ValueOfColumn("SP027_Invoice_ID", invoice.getC_Invoice_ID());
					commissionRun.set_ValueOfColumn("C_Currency_ID", invoice.getC_Currency_ID());
					if (invoice.getC_Order_ID() > 0) {
						commissionRun.set_ValueOfColumn("SP027_Order_ID", invoice.getC_Order_ID());
					}
					commissionRun.setAD_Org_ID(invoice.getAD_Org_ID());
					commissionRun.saveEx();
					//	Process commission
					commissionRun.setDocStatus(MCommissionRun.DOCSTATUS_Drafted);
					//	Complete
					if(commissionRun.processIt(MCommissionRun.DOCACTION_Complete)) {
						commissionRun.updateFromAmt();
						totalCommissionAmount.set((BigDecimal)commissionRun.get_Value("TotalCommissionAmt"));
						commissionRun.saveEx();
						createOrderFromCommission(commissionDefinition, commissionRun, (MOrder) invoice.getC_Order(), invoice);
					}
				});
				if (totalCommissionAmount.get() != null && totalCommissionAmount.get().signum() != 0) {
					reversePreviousCommissionOrders(invoice, totalCommissionAmount.get());
				}

			}
		}
		return null;
	}

	private void createOrderFromCommission(MCommission commissionDefinition, MCommissionRun commissionRun, MOrder originalOrder, MInvoice originalInvoice) {

		if (commissionRun.getGrandTotal().signum() == 0) {
			return;
		}
		MOrder order = new MOrder (commissionRun.getCtx(), 0, commissionRun.get_TrxName());
		order.setClientOrg(commissionRun.getAD_Client_ID(), commissionRun.getAD_Org_ID());
		order.setIsSOTrx(true);

		int docType_ID =  commissionDefinition.get_ValueAsInt("C_DocTypeOrder_ID");

		/* TODO: Validate if this will be used, haven't found an instance that has this variable configured in the System Configurator
		if(!originalOrder.isSOTrx() && originalOrder.get_ValueAsBoolean("IsDirectInvoice")){
			int docTypeSystem_ID = Integer.valueOf(MSysConfig.getValue("UY_DIRECT_COMMISSION_DOCTYPE_OV", "0", order.getAD_Client_ID()));

			if(docTypeSystem_ID > 0)
				docType_ID = docTypeSystem_ID;
		}*/
		if(docType_ID > 0) {
			order.setC_DocTypeTarget_ID(docType_ID);
		} else {
			order.setC_DocTypeTarget_ID();	//	POO
		}
		MDocType docType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
		order.setIsSOTrx(docType.isSOTrx());
		int partnerId = 0;
		int salesRepId = 0;
		Timestamp dateOrdered = null;
		if (originalOrder != null && originalInvoice == null) {
			partnerId = originalOrder.getC_BPartner_ID();
			salesRepId = originalOrder.getSalesRep_ID();
			dateOrdered = TimeUtil.getDay(new Timestamp(System.currentTimeMillis()));
		}
		if (originalInvoice != null) {
			partnerId = originalInvoice.getC_BPartner_ID();
			salesRepId = originalInvoice.getSalesRep_ID();
			dateOrdered = originalInvoice.getDateInvoiced();
		}
		MBPartner businessPartner = MBPartner.get(commissionRun.getCtx(), partnerId);
		order.setBPartner(businessPartner);
		//if (businessPartner.get_ValueAsBoolean("BillToRelationship")) {//TODO: BillToRelationship Column
		String billBPartnerWhere = I_C_BP_Relation.COLUMNNAME_C_BPartner_ID + "=? AND " + I_C_BP_Relation.COLUMNNAME_IsBillTo + "='Y'";
		X_C_BP_Relation bpRelation = new Query(order.getCtx(), I_C_BP_Relation.Table_Name, billBPartnerWhere, order.get_TrxName())
				.setParameters(order.getC_BPartner_ID())
				.setOnlyActiveRecords(true)
				.first();
		MBPartner billBPartner = null;
		if (bpRelation != null) {
			billBPartner = new MBPartner(order.getCtx(), bpRelation.getC_BPartnerRelation_ID(), order.get_TrxName());
		}
		if (billBPartner != null) {
			order.setBill_BPartner_ID(billBPartner.get_ID());
		}
		//}
		int currencyId = commissionRun.get_ValueAsInt("C_Currency_ID");
		if(currencyId == 0) {
			currencyId = commissionDefinition.getC_Currency_ID();
		}
		MCurrency currency = MCurrency.get(commissionRun.getCtx(), currencyId);
		MPriceList defaultPriceList = MPriceList.getDefault(commissionRun.getCtx(), order.isSOTrx(), currency.getISO_Code());
		//TODO: IsTaxIncluded for CommissionDefinition
		if(defaultPriceList != null) {
			order.setM_PriceList_ID(defaultPriceList.getM_PriceList_ID());
		} else {
			throw new IllegalArgumentException("@M_PriceList_ID@ @NotFound@");
		}
		order.setSalesRep_ID(salesRepId);
		order.setDateOrdered(dateOrdered);
		order.setDocStatus(MOrder.DOCSTATUS_Drafted);
		order.setDocAction(MOrder.DOCACTION_Complete);
		//
		order.setDescription(Msg.parseTranslation(commissionRun.getCtx(), "@Generate@: @C_CommissionRun_ID@ " + commissionRun.getDocumentNo()));
		//
		boolean isDirectInvoice = false;
		if(originalOrder != null) {
			order.addDescription(Msg.parseTranslation(commissionRun.getCtx(), "@C_Order_ID@ " + originalOrder.getDocumentNo()));
			//	Set Project
			if(originalOrder.getC_Project_ID() > 0) {
				order.setC_Project_ID(originalOrder.getC_Project_ID());
			} else if(originalOrder.getUser1_ID() > 0) {
				order.setUser1_ID(originalOrder.getUser1_ID());
				order.setUser4_ID(originalOrder.getUser4_ID());
			}
			if(originalOrder.get_ValueAsInt("S_Contract_ID") > 0) {
				order.set_ValueOfColumn("S_Contract_ID", originalOrder.get_ValueAsInt("S_Contract_ID"));
			}
			//TODO: Validate is this will be used
			/*if (!originalOrder.isSOTrx()) {
				isDirectInvoice = originalOrder.get_ValueAsBoolean("IsDirectInvoice");
			}*/
		}
		order.set_ValueOfColumn("SP027_CommissionRun_ID", commissionRun.get_ID());
		order.saveEx();
		int productId = commissionDefinition.get_ValueAsInt("M_Product_ID");
		int chargeId = commissionDefinition.get_ValueAsInt("C_Charge_ID");
		commissionRun.getCommissionAmtList().stream()
				.filter(commissionAmt -> commissionAmt.getCommissionAmt() != null
						&& commissionAmt.getCommissionAmt().compareTo(Env.ZERO) != 0).forEach(commissionAmt -> {
					MOrderLine orderLine = new MOrderLine(order);
					if(productId > 0) {
						MProduct product = MProduct.get(commissionRun.getCtx(), productId);
						orderLine.setProduct(product);

					} else {
						orderLine.setC_Charge_ID(chargeId);
					}
					orderLine.setQty(Env.ONE);
					orderLine.setPrice(commissionAmt.getCommissionAmt().abs());
					orderLine.setTax();
					//Openup. Nicolas Sarlabos. 16/07/2020. #13614.
					orderLine.setUser1_ID(order.getUser1_ID());
					//orderLine.setUser3_ID(comLine.get_ValueAsInt("User3_ID")); //TODO: this comes from contract Split
					orderLine.setUser4_ID(order.getUser4_ID());
					orderLine.saveEx();
				});


		/* TODO: Validate is this will be used
		if (isDirectInvoice) {
			order.set_ValueOfColumn("IsAllowToInvoice", true);
		}
		*/
		order.processIt(MOrder.DOCACTION_Complete);
		order.saveEx();
	}

	/**
	 *
	 * @param sourceInvoice
	 * @param amount
	 */
	private static void reversePreviousCommissionOrders(MInvoice sourceInvoice, BigDecimal amount) {
		String whereClause = "DocStatus IN ('CO', 'CL') "
			+ "AND GrandTotal > 0 "
			+ "AND EXISTS(SELECT 1 FROM C_CommissionRun cr "
			+ "WHERE cr.C_CommissionRun_ID = C_Order.SP027_CommissionRun_ID "
			+ "AND cr.SP027_Invoice_ID IS NULL "
			+ "AND cr.DocStatus='CO'"
			+ "AND EXISTS(SELECT 1 FROM C_InvoiceLine il "
			+ "	INNER JOIN C_OrderLine ol ON(ol.C_OrderLine_ID = il.C_OrderLine_ID) "
			+ "	WHERE il.C_Invoice_ID = ? "
			+ "	AND ol.C_Order_ID = cr.SP027_Order_ID))";
		new Query(sourceInvoice.getCtx(), I_C_Order.Table_Name, whereClause, sourceInvoice.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(sourceInvoice.getC_Invoice_ID())
			.<MOrder>getIDsAsList()
			.forEach(commissionOrderId -> {
				MOrder commissionOrder = new MOrder(sourceInvoice.getCtx(), commissionOrderId, sourceInvoice.get_TrxName());
				MOrder reverseOrder = new MOrder(commissionOrder.getCtx(), 0, commissionOrder.get_TrxName());
				PO.copyValues(commissionOrder, reverseOrder);
				reverseOrder.setDocumentNo(null);
				int returnOrderDocTypeId =  MDocType.getDocTypeBaseOnSubType(commissionOrder.getAD_Org_ID(), MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_ReturnMaterial);
				if (returnOrderDocTypeId <= 0) {
					throw new AdempiereException("@C_DocType_ID@ @NotFound@");
				}
				reverseOrder.setC_DocTypeTarget_ID(returnOrderDocTypeId);
				reverseOrder.setC_DocType_ID(returnOrderDocTypeId);
				reverseOrder.setDateOrdered(sourceInvoice.getDateInvoiced());
				reverseOrder.setDatePromised(sourceInvoice.getDateInvoiced());
				reverseOrder.setDateAcct(sourceInvoice.getDateAcct());
				reverseOrder.setDatePrinted(sourceInvoice.getDatePrinted());
				reverseOrder.setPOReference(commissionOrder.getDocumentNo());
				reverseOrder.addDescription(Msg.parseTranslation(commissionOrder.getCtx(), "@Generated@ [@C_Order_ID@ " + commissionOrder.getDocumentNo()) + "]");
				reverseOrder.setDocStatus(MOrder.DOCSTATUS_Drafted);
				reverseOrder.setDocAction(MOrder.DOCACTION_Complete);
				reverseOrder.setTotalLines(Env.ZERO);
				reverseOrder.setGrandTotal(Env.ZERO);
				reverseOrder.setIsSOTrx(commissionOrder.isSOTrx());
				reverseOrder.setRef_Order_ID(-1);
				reverseOrder.setIsDropShip(false);
				reverseOrder.setDropShip_BPartner_ID(0);
				reverseOrder.setDropShip_Location_ID(0);
				reverseOrder.setDropShip_User_ID(0);
				reverseOrder.saveEx();
				//	Add Line
				MOrderLine commissionOrderLine = commissionOrder.getLines(true, null)[0];
				MOrderLine reverseOrderLine = new MOrderLine(reverseOrder);
				PO.copyValues(commissionOrderLine, reverseOrderLine);
				reverseOrderLine.setOrder(reverseOrder);
				reverseOrderLine.setC_Order_ID(reverseOrder.getC_Order_ID());
				//	Set from reverse amount
				if(commissionOrderLine.getQtyOrdered().signum() < 0) {
					reverseOrderLine.setQty(Env.ONE.negate());
				} else {
					reverseOrderLine.setQty(Env.ONE);
				}
				//	Set amount
				reverseOrderLine.setPrice(amount);
				reverseOrderLine.setProcessed(true);
				reverseOrderLine.saveEx();
				reverseOrder.setDocStatus(MOrder.DOCSTATUS_Closed);
				reverseOrder.setDocAction(MOrder.DOCACTION_None);
				reverseOrder.setProcessed(true);
				reverseOrder.calculateTaxTotal();
				reverseOrder.saveEx();
				reverseOrder.processIt(MOrder.ACTION_Post);
				commissionOrder.setDocStatus(MOrder.DOCSTATUS_Closed);
				commissionOrder.saveEx();
			});
	}
}
