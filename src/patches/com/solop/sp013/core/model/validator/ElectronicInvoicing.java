/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2016 E.R.P. Consultores y Asociados.                    *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpcya.com                                 *
 *****************************************************************************/
package com.solop.sp013.core.model.validator;

import com.solop.sp013.core.model.X_SP013_ElectronicLineSummary;
import com.solop.sp013.core.process.SendInvoice;
import com.solop.sp013.core.process.SendShipment;
import com.solop.sp013.core.queue.ElectronicDocument;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.core.util.ElectronicInvoicingSummaryGrouping;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_M_InOut;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPOS;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.queue.util.QueueLoader;

/**
 * Document Builder
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class ElectronicInvoicing implements ModelValidator {

	public ElectronicInvoicing() {
		super();
	}

	/** Logger 			*/
	private static CLogger log = CLogger
			.getCLogger(ElectronicInvoicing.class);
	/**	Client			*/
	private int clientId = 0;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
		} else {
			log.info("Initializing global validator: " + toString());
		}
		//
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
		engine.addModelChange(I_C_Invoice.Table_Name, this);
		engine.addDocValidate(I_M_InOut.Table_Name, this);
		engine.addModelChange(I_M_InOut.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		MOrgInfo orgInfo = MOrgInfo.get(po.getCtx(), po.getAD_Org_ID(), po.get_TrxName());
		if(!orgInfo.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicInvoicing)){
			return null;
		}

		if(type == TYPE_BEFORE_NEW
				|| type == TYPE_BEFORE_CHANGE) {
			log.fine(" TYPE_BEFORE_NEW || TYPE_BEFORE_CHANGE");
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;

				MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
				if (documentType == null || !documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)){
					return null;
				}
				if(invoice.isSOTrx()) {
					if(type == TYPE_BEFORE_NEW || invoice.is_ValueChanged(I_C_Invoice.COLUMNNAME_C_BPartner_ID)) {
						MBPartner customer = MBPartner.get(invoice.getCtx(), invoice.getC_BPartner_ID());

						String fiscalDocumentType = documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType);

						if (!Util.isEmpty(fiscalDocumentType, true)){
							int documentTypeId = ElectronicInvoicingUtil.getDocumentTypeFromTaxGroup(invoice.getCtx(), fiscalDocumentType, customer.getC_TaxGroup_ID(), invoice.isManualDocument());
							if(documentTypeId > 0) {
								invoice.setC_DocTypeTarget_ID(documentTypeId);
							}
						}

						MBPartner mbPartner = (MBPartner) invoice.getC_BPartner();
						String electronicBillingCriteria = mbPartner.get_ValueAsString("SP013_BillingCriteria");
						if (!Util.isEmpty(electronicBillingCriteria, true)) {
							invoice.set_ValueOfColumn("SP013_BillingCriteria", mbPartner.get_ValueAsString("SP013_BillingCriteria"));
						}
					}

				}
				if(type == TYPE_BEFORE_NEW || invoice.is_ValueChanged(I_C_Invoice.COLUMNNAME_C_DocTypeTarget_ID)) {
					invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsElectronicDocument, documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument));
					invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsAllowsReverse, documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse));
				}

				//	Set Allocated Document
				if(type == TYPE_BEFORE_NEW) {
					if(invoice.getC_POS_ID() <= 0 || invoice.getC_Order_ID() <= 0) {
						return null;
					}
					MOrder returnOrder = new MOrder(Env.getCtx(), invoice.getC_Order_ID(), invoice.get_TrxName());
					if(returnOrder.get_ValueAsInt("ECA14_Source_Order_ID") <= 0) {
						return null;
					}
					MOrder sourceOrder = new MOrder(Env.getCtx(), returnOrder.get_ValueAsInt("ECA14_Source_Order_ID"), invoice.get_TrxName());
					int sourceInvoiceId = sourceOrder.getC_Invoice_ID();
					if(sourceInvoiceId <= 0) {
						return null;
					}
					invoice.set_ValueOfColumn(ElectronicInvoicingChanges.ReferenceDocument_ID, sourceInvoiceId);
				}
			} else if (po.get_TableName().equals(MInOut.Table_Name)) {
				MInOut inOut = (MInOut) po;
				if(inOut.isSOTrx()) {
					//	TODO: To Shipment not setup the fiscal document type
//					if(type == TYPE_BEFORE_NEW || inOut.is_ValueChanged(I_C_Invoice.COLUMNNAME_C_BPartner_ID)) {
//						MBPartner customer = MBPartner.get(inOut.getCtx(), inOut.getC_BPartner_ID());
//						MDocType documentType = MDocType.get(inOut.getCtx(), inOut.getC_DocType_ID());
//						String fiscalDocumentType = ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice;
//						if(documentType != null) {
//							fiscalDocumentType = documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType);
//							if(fiscalDocumentType == null) {
//								fiscalDocumentType = ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice;
//							}
//						}
//						int documentTypeId = ElectronicInvoicingUtil.getDocumentTypeFromTaxGroup(inOut.getCtx(), fiscalDocumentType, customer.getC_TaxGroup_ID());
//						if(documentTypeId > 0) {
//							inOut.setC_DocType_ID(documentTypeId);
//						}
//
//						MBPartner mbPartner = (MBPartner) inOut.getC_BPartner();
//						String electronicBillingCriteria = mbPartner.get_ValueAsString("SP013_BillingCriteria");
//						if (!Util.isEmpty(electronicBillingCriteria, true)) {
//							inOut.set_ValueOfColumn("SP013_BillingCriteria", mbPartner.get_ValueAsString("SP013_BillingCriteria"));
//						}
//					}
					if(type == TYPE_BEFORE_NEW || inOut.is_ValueChanged(I_C_Invoice.COLUMNNAME_C_DocTypeTarget_ID)) {
						MDocType documentType = MDocType.get(inOut.getCtx(), inOut.getC_DocType_ID());
						inOut.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsElectronicDocument, documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument));
						inOut.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsAllowsReverse, documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse));
					}
				}
			}
		} else if(type == TYPE_AFTER_NEW) {
			log.fine(" TYPE_BEFORE_NEW || TYPE_BEFORE_CHANGE");
			if (po.get_TableName().equals(MInvoice.Table_Name)) {

			}
		} else if (type == TYPE_AFTER_CHANGE) {
			if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
				if (documentType != null && documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)){
					if (invoice.is_ValueChanged("SP013_BillingCriteria")){
						ElectronicInvoicingSummaryGrouping grouping = ElectronicInvoicingSummaryGrouping.newInstance(invoice);
						grouping.process();
					}
				}

			}
		}
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		MOrgInfo orgInfo = MOrgInfo.get(po.getCtx(), po.getAD_Org_ID(), po.get_TrxName());
		if(!orgInfo.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicInvoicing)){
			return null;
		}

		if(timing == TIMING_BEFORE_PREPARE) {
			if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if (!new Query(invoice.getCtx(), X_SP013_ElectronicLineSummary.Table_Name, X_SP013_ElectronicLineSummary.COLUMNNAME_C_Invoice_ID + "=?", invoice.get_TrxName())
						.setParameters(invoice.get_ID())
						.match()) {
					ElectronicInvoicingSummaryGrouping grouping = ElectronicInvoicingSummaryGrouping.newInstance(invoice);
					grouping.process();
				}
			}
		}
		if(timing == TIMING_BEFORE_REVERSECORRECT
				|| timing == TIMING_BEFORE_VOID) {
			log.fine(" TIMING_BEFORE_REVERSECORRECT || TIMING_BEFORE_VOID");
			if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)
						&& !invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse)) {
					return Msg.parseTranslation(Env.getCtx(), "@SP013.ReverseNotAllowed@");
				}
			} else if(po.get_TableName().equals(I_M_InOut.Table_Name)) {
				MInOut inOut = (MInOut) po;
				if(inOut.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)
						&& !inOut.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse)) {
					return Msg.parseTranslation(Env.getCtx(), "@SP013.ReverseNotAllowed@");
				}
			}
		} else if(timing == TIMING_BEFORE_COMPLETE) {
			log.fine(" TIMING_BEFORE_COMPLETE");
			if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(!invoice.isReversal()
						&& invoice.isSOTrx()) {
					int posId = invoice.getC_POS_ID();
					if(posId == 0) {
						if(invoice.getC_Order_ID() != 0) {
							posId = invoice.getC_Order().getC_POS_ID();
						}
					}
					if(posId != 0) {
						MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
						if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
							MPOS posConfiguration = MPOS.get(invoice.getCtx(), posId);
							int fiscalSenderId = MOrgInfo.get(invoice.getCtx(), posConfiguration.getAD_Org_ID(), null).get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID);
							if(fiscalSenderId != 0) {
								invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalSender_ID, fiscalSenderId);
								invoice.setC_POS_ID(posId);
								invoice.saveEx();
							}
						}
					}
				}
			} else if(po.get_TableName().equals(I_M_InOut.Table_Name)) {
				MInOut inOut = (MInOut) po;
				if(!inOut.isReversal()
						&& inOut.isSOTrx()) {
					int posId = inOut.getC_POS_ID();
					if(posId == 0) {
						if(inOut.getC_Order_ID() != 0) {
							posId = inOut.getC_Order().getC_POS_ID();
						}
					}
					if(posId != 0) {
						MDocType documentType = MDocType.get(inOut.getCtx(), inOut.getC_DocType_ID());
						if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
							MPOS posConfiguration = MPOS.get(inOut.getCtx(), posId);
							int fiscalSenderId = MOrgInfo.get(inOut.getCtx(), posConfiguration.getAD_Org_ID(), null).get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID);
							if(fiscalSenderId != 0) {
								inOut.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalSender_ID, fiscalSenderId);
								inOut.setC_POS_ID(posId);
								inOut.saveEx();
							}
						}
					}
				}
			}
		} else if(timing == TIMING_AFTER_COMPLETE) {
			log.fine(" TIMING_AFTER_COMPLETE");
			if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {

				MInvoice invoice = (MInvoice) po;
				if (invoice.isReversal()) {
					invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsSent, false);
					invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_DownloadURL, null);
					invoice.saveEx();
				}
				MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
				if((!invoice.isReversal() || documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse))) {
					if(!invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSent)) {
						if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
							if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSendAfterComplete)) {
								ProcessInfo info = org.eevolution.services.dsl.ProcessBuilder.create(invoice.getCtx())
										.process(SendInvoice.getProcessId())
										.withRecordId(I_C_Invoice.Table_ID, invoice.getC_Invoice_ID())
										.withParameter(SendInvoice.C_INVOICE_ID, invoice.getC_Invoice_ID())
										.withoutTransactionClose()
										.execute(invoice.get_TrxName());
								if(info.isError()) {
									return info.getSummary();
								}
							} else {
								QueueLoader.getInstance().getQueueManager(ElectronicDocument.QueueType_ElectronicDocument)
										.withContext(invoice.getCtx())
										.withTransactionName(invoice.get_TrxName())
										.withEntity(invoice)
										.addToQueue();
							}
						}
					}
				}
			} else if(po.get_TableName().equals(I_M_InOut.Table_Name)) {
				MInOut inOut = (MInOut) po;
				MDocType documentType = MDocType.get(inOut.getCtx(), inOut.getC_DocType_ID());
				if((!inOut.isReversal() || documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse))
						&& (inOut.isSOTrx())) {

					if (inOut.isReversal()) {
						inOut.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsSent, false);
						inOut.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_DownloadURL, null);
						inOut.saveEx();
					}
					if(!inOut.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSent)) {
						if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
							if(documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSendAfterComplete)) {
								ProcessInfo info = org.eevolution.services.dsl.ProcessBuilder.create(inOut.getCtx())
										.process(SendShipment.getProcessId())
										.withRecordId(I_M_InOut.Table_ID, inOut.getM_InOut_ID())
										.withParameter(SendShipment.M_INOUT_ID, inOut.getM_InOut_ID())
										.withoutTransactionClose()
										.execute(inOut.get_TrxName());
								if(info.isError()) {
									return info.getSummary();
								}
							} else {
								QueueLoader.getInstance().getQueueManager(ElectronicDocument.QueueType_ElectronicDocument)
										.withContext(inOut.getCtx())
										.withTransactionName(inOut.get_TrxName())
										.withEntity(inOut)
										.addToQueue();
							}
						}
					}
				}
			}
		}
		return null;
	}
}