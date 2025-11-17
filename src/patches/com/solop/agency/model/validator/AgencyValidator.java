/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package com.solop.agency.model.validator;

import org.adempiere.core.domains.models.I_AD_Image;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Commission;
import org.adempiere.core.domains.models.I_C_CommissionLine;
import org.adempiere.core.domains.models.I_C_CommissionRun;
import org.adempiere.core.domains.models.I_C_CommissionSalesRep;
import org.adempiere.core.domains.models.I_C_CommissionType;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_C_ProjectPhase;
import org.adempiere.core.domains.models.I_C_ProjectTask;
import org.adempiere.core.domains.models.I_C_RfQ;
import org.adempiere.core.domains.models.I_C_RfQResponse;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_S_Contract;
import org.adempiere.core.domains.models.I_S_TimeExpense;
import org.adempiere.core.domains.models.X_C_CommissionSalesRep;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionLine;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MCommissionType;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MImage;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPeriod;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MProject;
import org.compiere.model.MProjectPhase;
import org.compiere.model.MProjectTask;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestType;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MRfQLine;
import org.compiere.model.MRfQLineQty;
import org.compiere.model.MRfQResponse;
import org.compiere.model.MStatus;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.MTree;
import org.compiere.model.MTree_NodeBP;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.context.service.infrastructure.domain.entities.MSContract;
import org.eevolution.services.dsl.ProcessBuilder;
import org.javatuples.Pair;
import org.openup.core.process.CommissionOrderCreateAbstract;
import org.spin.model.MRNoticeTemplateEvent;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * Model validator for agency particularities
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class AgencyValidator implements ModelValidator
{
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(getClass());
	/** Client			*/
	private int		clientId = -1;

	//Openup. Nicolas Sarlabos. 04/08/2020. #14418.
	private int DOCTYPE_SO_MEDIACOM_INVENTARIO_ID = 1000182;
	private int DOCTYPE_SO_MEDIACOM_XAXIS_ID = 1000183;
	private int USER1_GROUPM_INVENTARIO_ID = 1000269;
	private int USER1_XAXIS_ID = 1000226;
	//Fin #14418.

	public void initialize (ModelValidationEngine engine, MClient client) {
		if (client != null) {	
			clientId = client.getAD_Client_ID();
		}
		engine.addModelChange(MProject.Table_Name, this);
		engine.addModelChange(MOrder.Table_Name, this);
		engine.addModelChange(MInvoice.Table_Name, this);
		engine.addModelChange(MOrderLine.Table_Name, this);
		engine.addModelChange(MCommissionLine.Table_Name, this);
		engine.addModelChange(MProjectTask.Table_Name, this);
		engine.addModelChange(MBPartner.Table_Name, this);
		engine.addModelChange(MRequest.Table_Name, this);
		engine.addModelChange(MRfQLineQty.Table_Name, this);
		engine.addModelChange(MRfQResponse.Table_Name, this);
		engine.addModelChange(MAttachment.Table_Name, this);
		engine.addModelChange(I_R_Request.Table_Name, this);
		engine.addDocValidate(MOrder.Table_Name, this);
		engine.addDocValidate(I_S_TimeExpense.Table_Name, this);
		engine.addDocValidate(MTimeExpense.Table_Name, this);
		engine.addDocValidate(MSContract.Table_Name, this);
		engine.addDocValidate(MInvoice.Table_Name, this);
		engine.addDocValidate(MCommissionRun.Table_Name, this);
	}	//	initialize

	public String modelChange (PO po, int type) throws Exception {
		log.info(po.get_TableName() + " Type: "+type);
		if (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE) {
			if(po instanceof MRequest) {
				MRequest request = (MRequest) po;
				if(request.getR_RequestType_ID() != 0) {
					MRequestType requestType = new MRequestType(request.getCtx(), request.getR_RequestType_ID(), request.get_TrxName());
					// Validates Approved on Request Type					
					if(requestType.get_ValueAsBoolean("IsApproved")) {
						// Validates Status be Completed
						MStatus status = new MStatus(request.getCtx(), request.get_ValueAsInt("R_Status_ID"), request.get_TrxName());
						if (status.isClosed() && !status.isFinalClose()) {
							// Validates Approved 1 on Request						
							if(request.get_ValueAsBoolean("IsApproved1")) {
								// Validates Approved 2 on Request
								if(!request.get_ValueAsBoolean("IsApproved2")) {
									throw new AdempiereException(Msg.getMsg(Env.getCtx(), "NotApproved2"));
								}
							}else {
								throw new AdempiereException(Msg.getMsg(Env.getCtx(), "NotApproved1"));
							}
						}
					}		
				}				
			} else if(po instanceof MRfQLineQty) {
				MRfQLineQty lineQuantity = (MRfQLineQty) po;
				MRfQLine line = MRfQLine.get(lineQuantity.getCtx(), lineQuantity.getC_RfQLine_ID(), lineQuantity.get_TrxName());
				if(line.get_ValueAsInt(I_C_Project.COLUMNNAME_C_Project_ID) > 0
						|| line.get_ValueAsInt(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID) > 0
						|| line.get_ValueAsInt(I_C_ProjectPhase.COLUMNNAME_C_ProjectPhase_ID) > 0) {
					lineQuantity.setIsOfferQty(true);
					lineQuantity.setIsPurchaseQty(true);
				}

				if(lineQuantity.is_ValueChanged("BestResponseAmt")){

					String sql = "select coalesce(q.margin,0)" +
							" from c_rfqresponselineqty q" +
							" join c_rfqresponseline l on q.c_rfqresponseline_id = l.c_rfqresponseline_id" +
							" join c_rfqresponse h on l.c_rfqresponse_id = h.c_rfqresponse_id" +
							" where h.isselectedwinner = 'Y' and c_rfqlineqty_id = " + lineQuantity.get_ID();

					BigDecimal margin = DB.getSQLValueBDEx(lineQuantity.get_TrxName(), sql);

					if(margin != null
							&& margin.compareTo(Env.ZERO) > 0) {
						lineQuantity.setMargin(margin);
					}

				}

			} else if(po instanceof MRfQResponse) {
				MRfQResponse response = (MRfQResponse) po;
				if(response.is_ValueChanged(I_C_RfQResponse.COLUMNNAME_IsComplete)) {
					//	Validate PDF File
					if(response.isComplete()) {
						if(response.getPdfAttachment() == null) {
							throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AttachmentNotFound"));
						}
					}
				}
			} else if(po instanceof MAttachment) {
				ArrayList<String> messageList = new ArrayList<>();
				StringBuffer messageToSend = new StringBuffer();
				MAttachment attachment = (MAttachment) po;
				if(attachment.getAD_Table_ID() == I_R_Request.Table_ID) {
					MAttachmentEntry[] entries = attachment.getEntries();
					MRequest request = new MRequest(attachment.getCtx(), attachment.getRecord_ID(), attachment.get_TrxName());
					List<MAttachmentEntry> entryToAdd = new ArrayList<>();
					if(entries != null) {
						MAttachment requestAttachment = request.getAttachment(true);
						MAttachmentEntry[] requestEntries = null;
						if(requestAttachment != null
								&& requestAttachment.getAD_Attachment_ID() > 0) {
							requestEntries = requestAttachment.getEntries();
						}
						//	Add or match
						for(MAttachmentEntry entry : entries) {
							if(requestEntries == null) {
								entryToAdd.add(entry);
							} else {
								boolean existsEntry = false;
								for(MAttachmentEntry attachmentEntry : requestEntries) {
									if(attachmentEntry.getName().equals(entry.getName())) {
										existsEntry = true;
										break;
									}
								}
								if(!existsEntry) {
									entryToAdd.add(entry);
								}
							}
						}
						//	Add Request Update
						for(MAttachmentEntry entry : entryToAdd) {
							MRequestUpdate update = new MRequestUpdate(request);
							String message = "@File@ @Added@: ";
							if(entry.isGraphic()) {
								MImage image = new MImage(attachment.getCtx(), 0, attachment.get_TrxName());
								image.setBinaryData(entry.getData());
								image.setName(entry.getName());
								image.saveEx();
								update.set_ValueOfColumn(I_AD_Image.COLUMNNAME_AD_Image_ID, image.getAD_Image_ID());
								message = "@AD_Image_ID@ @Added@: ";
							}
							String translatedMessage = Msg.parseTranslation(attachment.getCtx(), message) + entry.getName();
							update.setResult(translatedMessage);
							//	
							update.saveEx();
							//	Add attachment to update
							MAttachment updateAttachment = update.createAttachment();
							updateAttachment.addEntry(entry);
							updateAttachment.addTextMsg(translatedMessage);
							updateAttachment.saveEx();
							if(messageToSend.length() > 0) {
								messageToSend.append(Env.NL);
							}
							messageToSend.append(translatedMessage);
						}
						//	Notify
						if(messageToSend.length() > 0) {
							messageList.add(I_R_Request.COLUMNNAME_Result);
							request.setResult(messageToSend.toString());
							request.sendNotices(messageList, MRNoticeTemplateEvent.EVENTTYPE_AutomaticTaskNewActivityNotice);
						}
					}
				}
			} else if(po instanceof MOrder) {
				MOrder order = (MOrder) po;
				MBPartner bPartner = (MBPartner) order.getC_BPartner();
				if (bPartner.get_ValueAsBoolean("IsMandatoryOrderReference")) {
					if (order.get_ValueAsBoolean("IsAllowToInvoice")) {
						if (order.isSOTrx() && (order.getLink_Order_ID() <= 0  
								|| (Util.isEmpty(order.getPOReference())))) {
							throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LinkOrderNotFound"));
						}
					}
				}
			} else if(po instanceof MSContract) {
				MSContract contract = (MSContract) po;
				if(contract.is_ValueChanged(I_S_Contract.COLUMNNAME_M_PriceList_ID)) {
					if(contract.getM_PriceList_ID() > 0) {
						MPriceList priceList = MPriceList.get(contract.getCtx(), contract.getM_PriceList_ID(), contract.get_TrxName());
						contract.setC_Currency_ID(priceList.getC_Currency_ID());
					}
				}
			}
			if (type == TYPE_BEFORE_CHANGE) {
				if (po instanceof MProject) {
					MProject project = (MProject) po;
					if(project.get_ValueAsBoolean("IsApprovedAttachment")) {
						MAttachment projectAttachment = project.getAttachment(true);
						if (projectAttachment == null 
								|| projectAttachment.getAD_Attachment_ID() <= 0) {
							throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AttachmentNotFound"));
						}
					}
				} else if(po instanceof MOrderLine) {
					if(po.is_ValueChanged(I_C_OrderLine.COLUMNNAME_Link_OrderLine_ID)) {
						MOrderLine orderLine = (MOrderLine) po;
						int projectPhaseId = orderLine.getC_ProjectPhase_ID();
						int projectTaskId = orderLine.getC_ProjectTask_ID();
						if(orderLine.getLink_OrderLine_ID() > 0) {
							MOrder order = orderLine.getParent();
							if(order.isSOTrx()) {
								MOrderLine generatedOrderLine = (MOrderLine) orderLine.getLink_OrderLine();
								MOrder generatedOrder = generatedOrderLine.getParent();
								if(!generatedOrder.isProcessed()) {
									MDocType sourceDocumentType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
									//Openup. Nicolas Sarlabos. #2752.
									if(sourceDocumentType.get_ValueAsBoolean("IsSetPOPriceFromSO")) {
										//si la linea de OV tiene definido PriceList entonces lo asigno, si no le asigno
										//si no le asigno el PriceEntered
										if(orderLine.getPriceList().compareTo(Env.ZERO) != 0){
											generatedOrderLine.setPriceEntered(orderLine.getPriceList());
											generatedOrderLine.setPriceActual(orderLine.getPriceList());
										} else if(orderLine.getPriceList().compareTo(Env.ZERO) == 0){
											generatedOrderLine.setPriceEntered(orderLine.getPriceEntered());
											generatedOrderLine.setPriceActual(orderLine.getPriceEntered());
										}
									}//Fin #2752.

									if(projectPhaseId > 0) {
										generatedOrderLine.setC_ProjectPhase_ID(projectPhaseId);
									} else if(projectTaskId > 0) {
										generatedOrderLine.setC_ProjectTask_ID(projectTaskId);
									}								
									if(orderLine.getC_Campaign_ID() != 0)
										generatedOrderLine.set_ValueOfColumn("C_Campaign_ID", orderLine.getC_Campaign_ID());
									if(orderLine.getUser1_ID() != 0)
										generatedOrderLine.set_ValueOfColumn("User1_ID", orderLine.getUser1_ID());
									if(orderLine.getC_Project_ID() != 0)
										generatedOrderLine.set_ValueOfColumn("C_Project_ID", orderLine.getC_Project_ID());
									if(orderLine.get_ValueAsInt("CUST_MediaType_ID") != 0)
										generatedOrderLine.set_ValueOfColumn("CUST_MediaType_ID", orderLine.get_ValueAsInt("CUST_MediaType_ID"));
									generatedOrderLine.saveEx();
								}
							}
						}
					}
				} else if(po instanceof MOrder) {
					MOrder order = (MOrder) po;
					if(po.is_ValueChanged(I_C_Order.COLUMNNAME_Link_Order_ID)) {
						//					MOrder order = (MOrder) po;
						if(order.getLink_Order_ID() > 0
								&& order.isSOTrx()) {
							MOrder linkSourceOrder = (MOrder) order.getLink_Order();
							linkSourceOrder.setDateOrdered(order.getDateOrdered());
							linkSourceOrder.setDatePromised(order.getDatePromised());
							if(order.isDropShip()) {
								linkSourceOrder.set_ValueOfColumn("IsDirectInvoice", order.get_ValueAsBoolean("IsDirectInvoice"));
							}
							linkSourceOrder.saveEx();
							//	Copy commission from RFQ Response
							copyCommissionFromFRQResponse(order.get_ValueAsInt(I_C_RfQ.COLUMNNAME_C_RfQ_ID), linkSourceOrder);
						}
					}
					//	For counter document
					if(order.getRef_Order_ID() > 0
							&& !order.isProcessed()) {
						MOrder sourceOrder = (MOrder) order.getRef_Order();
						if(sourceOrder.getC_Currency_ID() != order.getC_Currency_ID()) {
							MCurrency currency = MCurrency.get(sourceOrder.getCtx(), sourceOrder.getC_Currency_ID());
							MPriceList defaultPriceList = MPriceList.getDefault(sourceOrder.getCtx(), order.isSOTrx(), currency.getISO_Code());
							if(defaultPriceList != null) {
								order.setM_PriceList_ID(defaultPriceList.getM_PriceList_ID());
							} else {
								throw new IllegalArgumentException("@DefaultPriceListCurrencyNotFound@ (@C_Currency_ID@: " + currency.getISO_Code() + ")");
							}
						}
					}
				} else if(po instanceof MProjectTask) {
					MProjectTask projectTask = (MProjectTask) po;
					if(projectTask.get_ValueAsBoolean("IsCustomerApproved")){
						if(projectTask.get_ValueAsBoolean("IsApprovedAttachment")) {
							MAttachment projectTaskAttachment = projectTask.getAttachment(true);
							if (projectTaskAttachment == null 
									|| projectTaskAttachment.getAD_Attachment_ID() <= 0) {
								throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AttachmentNotFound"));
							}
						}
					}
				}
			} else if(type == TYPE_BEFORE_NEW) {
				if (po instanceof MCommissionLine) {
					MCommissionLine commissionLine = (MCommissionLine) po;
					MCommission commission = (MCommission) commissionLine.getC_Commission();
					if(commission != null
							&& commissionLine.get_ValueAsInt("Vendor_ID") <= 0) {
						if(commissionLine.get_ValueAsInt("C_Order_ID") > 0) {
							MOrder order = new MOrder(commission.getCtx(), commissionLine.get_ValueAsInt("C_Order_ID"), commission.get_TrxName());
							commissionLine.set_ValueOfColumn("Vendor_ID", order.getC_BPartner_ID());
						}
					}
				} else if(po instanceof MOrderLine) {
					MOrderLine orderLine = (MOrderLine) po;
					MOrder order = new MOrder(orderLine.getCtx(), orderLine.getC_Order_ID(), orderLine.get_TrxName());
					MDocType docType = (MDocType) order.getC_DocTypeTarget();
					int projectPhaseId = orderLine.getC_ProjectPhase_ID();
					int projectTaskId = orderLine.getC_ProjectTask_ID();
					if(projectTaskId > 0) {
						MProjectTask projectTask = new MProjectTask(orderLine.getCtx(), projectTaskId,orderLine.get_TrxName());
						if(projectTask.getC_Campaign_ID() != 0)
							orderLine.set_ValueOfColumn("C_Campaign_ID", projectTask.getC_Campaign_ID());

						//Openup. Nicolas Sarlabos. 21/10/2020. #14916
						if (order.getRef_Order_ID() > 0) {
							if (projectTask.getUser1_ID() != 0)
								orderLine.set_ValueOfColumn("User1_ID", projectTask.getUser3_ID());
							if (projectTask.getUser3_ID() != 0)
								orderLine.set_ValueOfColumn("User3_ID", projectTask.getUser1_ID());
						} else {
							if (projectTask.getUser1_ID() != 0)
								orderLine.set_ValueOfColumn("User1_ID", projectTask.getUser1_ID());
							if (projectTask.getUser3_ID() != 0)
								orderLine.set_ValueOfColumn("User3_ID", projectTask.getUser3_ID());
						}//Fin #14916.

						if(projectTask.get_ValueAsInt("CUST_MediaType_ID") != 0)
							orderLine.set_ValueOfColumn("CUST_MediaType_ID", projectTask.get_ValueAsInt("CUST_MediaType_ID"));
						MProjectPhase projectPhasefromTask = new MProjectPhase(orderLine.getCtx(), projectTask.getC_ProjectPhase_ID(),orderLine.get_TrxName());
						if(projectPhasefromTask.getC_Project_ID() != 0)
							orderLine.set_ValueOfColumn("C_Project_ID", projectPhasefromTask.getC_Project_ID());						
					} else if(projectPhaseId > 0) {
						MProjectPhase projectPhase = new MProjectPhase(orderLine.getCtx(), projectPhaseId, orderLine.get_TrxName());						
						if(projectPhase.getC_Campaign_ID() != 0)
							orderLine.set_ValueOfColumn("C_Campaign_ID", projectPhase.getC_Campaign_ID());

						//Openup. Nicolas Sarlabos. 21/10/2020. #14916
						if (order.getRef_Order_ID() > 0) {
							if (projectPhase.getUser1_ID() != 0)
								orderLine.set_ValueOfColumn("User1_ID", projectPhase.getUser3_ID());
							if (projectPhase.getUser3_ID() != 0)
								orderLine.set_ValueOfColumn("User3_ID", projectPhase.getUser1_ID());
						} else {
							if (projectPhase.getUser1_ID() != 0)
								orderLine.set_ValueOfColumn("User1_ID", projectPhase.getUser1_ID());
							if (projectPhase.getUser3_ID() != 0)
								orderLine.set_ValueOfColumn("User3_ID", projectPhase.getUser3_ID());
						}//Fin #14916.

						if(projectPhase.getC_Project_ID() != 0)
							orderLine.set_ValueOfColumn("C_Project_ID", projectPhase.getC_Project_ID());
						if(projectPhase.get_ValueAsInt("CUST_MediaType_ID") != 0)
							orderLine.set_ValueOfColumn("CUST_MediaType_ID", projectPhase.get_ValueAsInt("CUST_MediaType_ID"));
					}

					//Openup. Nicolas Sarlabos. 04/08/2020. #14418.
					if(docType.get_ID() == DOCTYPE_SO_MEDIACOM_INVENTARIO_ID){
						orderLine.set_ValueOfColumn("User1_ID", USER1_GROUPM_INVENTARIO_ID);
					} else if(docType.get_ID() == DOCTYPE_SO_MEDIACOM_XAXIS_ID){
						orderLine.set_ValueOfColumn("User1_ID", USER1_XAXIS_ID);
					}//Fin #14418.

					//Openup. Nicolas Sarlabos. 22/10/2020. #14912.
					if (orderLine.getLink_OrderLine_ID() > 0) {
						MOrderLine linkOrderLine = (MOrderLine) orderLine.getLink_OrderLine();
						MOrder linkOrder = (MOrder) linkOrderLine.getC_Order();

						if (linkOrder.getC_DocTypeTarget_ID() == DOCTYPE_SO_MEDIACOM_XAXIS_ID
								|| linkOrder.getC_DocTypeTarget_ID() == DOCTYPE_SO_MEDIACOM_INVENTARIO_ID) {
							orderLine.setUser1_ID(linkOrderLine.getUser1_ID());
						}
					}//Fin #14912.

				} else if(po instanceof MOrder) {
					MOrder order = (MOrder) po;
					MDocType docType = (MDocType) order.getC_DocTypeTarget();
					int orderprojectId = order.getC_Project_ID();
					if(orderprojectId > 0) {
						if(order.getC_ConversionType_ID() <= 0) {
							order.setC_ConversionType_ID(MConversionType.TYPE_SPOT);
						}
					}
					// Validates Order Has ProjectPorcentaje
					//Openup. Nicolas Sarlabos. 05/08/2020. #14419.
					if(!docType.get_ValueAsBoolean("IsSplitDocuments")){
						int serviceContractId = order.get_ValueAsInt("S_Contract_ID");
						if(serviceContractId > 0) {
							MSContract serviceContract = new MSContract(order.getCtx(), serviceContractId, order.get_TrxName());
							if(serviceContract.getUser1_ID() > 0) {
								order.setUser1_ID(serviceContract.getUser1_ID());
							}
						}
					}//Fin #14419.

					if(order.getRef_Order_ID() > 0){

						MOrder refOrder = (MOrder) order.getRef_Order();

						if(refOrder.getUser3_ID() > 0){
							order.setUser1_ID(refOrder.getUser3_ID());
							order.setUser3_ID(refOrder.getUser1_ID());
						}
					}
				} else if (po instanceof MRequest) {
					MRequest mRequest = (MRequest) po;

					// Openup Solutions - #14703 - Raul Capecce - 15/09/2020
					MProject mProject = null;
					try {
						if (mRequest.getC_Invoice_ID() > 0) {
							mProject = (MProject) mRequest.getC_Invoice().getC_Project();
						} else if (mRequest.getC_Order_ID() > 0) {
							mProject = (MProject) mRequest.getC_Order().getC_Project();
						} else if (mRequest.getC_InvoiceLine_ID() > 0) {
							mProject = (MProject) mRequest.getC_InvoiceLine().getC_Invoice().getC_Project();
						} else if (mRequest.getC_OrderLine_ID() > 0) {
							mProject = (MProject) mRequest.getC_OrderLine().getC_Order().getC_Project();
						}
						if (mProject != null) {
							mRequest.setC_Project_ID(mProject.get_ID());
						}
					} catch (Exception ignored) {
					}
					// Openup Solutions - #14703 - End
				}
			} else if(type == TYPE_AFTER_CHANGE) {
				if (po instanceof MBPartner
						&& po.is_ValueChanged(I_C_BPartner.COLUMNNAME_BPartner_Parent_ID)) {
					MBPartner bPartner = (MBPartner) po;
					int treeId = MTree.getDefaultTreeIdFromTableId(bPartner.getAD_Client_ID(), I_C_BPartner.Table_ID);
					if(treeId > 0) {
						MTree tree = MTree.get(bPartner.getCtx(), treeId, null);
						MTree_NodeBP node = MTree_NodeBP.get(tree, bPartner.getC_BPartner_ID());
						if(node != null) {
							int parentId = bPartner.getBPartner_Parent_ID();
							if(parentId < 0) {
								parentId = 0;
							}
							node.setParent_ID(parentId);
							node.saveEx();
						}
					}
				}
			}
		}
			//
			return null;
		}	//	modelChange

		/**
		 * Copy commission from RFQ Response to Purchase Order	
		 * @param RFQ reference
		 * @param linkSourceOrder
		 */
		private void copyCommissionFromFRQResponse(int rFQId, MOrder linkSourceOrder) {
			new Query(linkSourceOrder.getCtx(), I_C_CommissionLine.Table_Name, 
					"EXISTS(SELECT 1 FROM C_RfQResponse rr "
					+ "		WHERE rr.C_RfQResponse_ID = C_CommissionLine.C_RfQResponse_ID "
					+ "		AND rr.C_RfQ_ID = ? "
					+ "		AND rr.IsSelectedWinner = 'Y' "
					+ "		AND rr.IsComplete = 'Y')", linkSourceOrder.get_TrxName())
				.setParameters(rFQId)
				.setOnlyActiveRecords(true)
				.<MCommissionLine>list()
				.forEach(commissionLine -> {
					MCommissionLine lineToAdd = new MCommissionLine(commissionLine.getCtx(),0, commissionLine.get_TrxName());
					PO.copyValues(commissionLine, lineToAdd);
					lineToAdd.set_ValueOfColumn(I_C_RfQResponse.COLUMNNAME_C_RfQResponse_ID, null);
					lineToAdd.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_Order_ID, linkSourceOrder.getC_Order_ID());
					lineToAdd.saveEx();
				});
		}

	/**
	 * Openup Solutions - Redmine #13452 - 02/04/2020
	 * Cierra cálculos de comisiones anteriores en estado completo sin factura asociada y realiza un nuevo cálculo de comisión con los montos actualies de la orden
	 * @author Raul Capecce
	 * @param mOrder
	 */
		public static void updateCommissionRunForOrder(MOrder mOrder) {
			MCommissionType mCommissionType = null;
			try {
				mCommissionType = new MCommissionType(mOrder.getCtx(), ((MDocType) mOrder.getC_DocTypeTarget()).get_ValueAsInt("C_CommissionType_ID"), mOrder.get_TrxName());
			} catch (Exception ignored) {}

			if (mCommissionType != null && mCommissionType.get_ID() > 0) {
				cancelingPreviousOrderCommissionRun(mOrder);
				createCommissionForOrder(mOrder, mCommissionType.get_ID(), true);
			}
		}

	/**
	 * Openup Solutions - Redmine #13452 - 02/04/2020
	 * Cierra los cálculos de comisión asociados a la orden que no tengan factura asociada y estén en estado completo
	 * Dichos cálculos de comisión los pasa a estado Cerrado
	 * @author Raul Capecce
	 * @param mOrder
	 */
		public static void cancelingPreviousOrderCommissionRun(MOrder mOrder) {
			String whereClause = "C_Order_ID=? AND C_Invoice_ID IS NULL AND " + I_C_CommissionRun.COLUMNNAME_DocStatus + "=?";
			List<MCommissionRun> mCommissionRuns = new Query(mOrder.getCtx(), I_C_CommissionRun.Table_Name, whereClause, mOrder.get_TrxName())
					.setParameters(mOrder.get_ID(), DocAction.STATUS_Completed)
					.list();
			for (MCommissionRun mCommissionRun : mCommissionRuns) {
				String whereClause2 = "C_CommissionRun_ID=? AND " + I_C_Order.COLUMNNAME_GrandTotal + ">0";
				MOrder commissionMOrder = new Query(mOrder.getCtx(), I_C_Order.Table_Name, whereClause2, mOrder.get_TrxName())
						.setParameters(mCommissionRun.get_ID())
						.first();
				if (commissionMOrder != null && commissionMOrder.get_ID() > 0) {
					reverseCommissionOrder(commissionMOrder, commissionMOrder.getTotalLines().negate());
				}
				if (!mCommissionRun.processIt(DocAction.ACTION_Close)) {
//					log.warning("MCommissionRun complete - failed: " + mCommissionRun);
				}
				mCommissionRun.saveEx();
			}



		}

	/**
	 * Get the current timestamp, date part only
	 * @return Timestamp, Date part only
	 */
	private static Timestamp getCurrentDate() {
			Timestamp currentTimestamp =  new Timestamp(System.currentTimeMillis());
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentTimestamp);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			currentTimestamp = new Timestamp(cal.getTimeInMillis());

			return currentTimestamp;
		}

		public static void reverseCommissionOrder(MOrder mOrder, BigDecimal reverseAmount) {
			Timestamp currentTimestamp = getCurrentDate();

			MOrder reverseOrder = new MOrder(mOrder.getCtx(), 0, mOrder.get_TrxName());
			PO.copyValues(mOrder, reverseOrder);
			reverseOrder.setAD_Org_ID(mOrder.getAD_Org_ID());
			reverseOrder.setDocumentNo(null);
			reverseOrder.setDateOrdered(currentTimestamp);
			reverseOrder.setDatePromised(currentTimestamp);
			reverseOrder.setDateAcct(currentTimestamp);
			reverseOrder.setPOReference(mOrder.getDocumentNo());
			reverseOrder.addDescription(Msg.parseTranslation(mOrder.getCtx(), "@Generated@ [@C_Order_ID@ " + mOrder.getDocumentNo()) + "]");
			reverseOrder.setDocStatus(MOrder.DOCSTATUS_Drafted);
			reverseOrder.setDocAction(MOrder.DOCACTION_Complete);
			reverseOrder.setTotalLines(Env.ZERO);
			reverseOrder.setGrandTotal(Env.ZERO);
			reverseOrder.setIsSOTrx(mOrder.isSOTrx());
			reverseOrder.setRef_Order_ID(-1);
			reverseOrder.setIsDropShip(false);
			reverseOrder.setDropShip_BPartner_ID(0);
			reverseOrder.setDropShip_Location_ID(0);
			reverseOrder.setDropShip_User_ID(0);
			reverseOrder.set_ValueOfColumn("ConsumptionOrder_ID", null);
			reverseOrder.set_ValueOfColumn("PreOrder_ID", null);
			reverseOrder.saveEx();
			//	Add Line
			MOrderLine commissionOrderLine = mOrder.getLines(true, null)[0];
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
			reverseOrderLine.setPrice(reverseAmount);
			reverseOrderLine.setProcessed(true);
			reverseOrderLine.saveEx();
			reverseOrder.setDocStatus(MOrder.DOCSTATUS_Closed);
			reverseOrder.setDocAction(MOrder.DOCACTION_None);
			reverseOrder.setProcessed(true);
			reverseOrder.calculateTaxTotal();
			reverseOrder.saveEx();
			reverseOrder.processIt(MOrder.ACTION_Post);
			mOrder.setDocStatus(MOrder.DOCSTATUS_Closed);
			mOrder.saveEx();
		}

		@Override
		public String docValidate (PO po, int timing) {
			log.info(po.get_TableName() + " Timing: "+timing);
			//	Validate table
			if(po instanceof MOrder) {
				MOrder order = (MOrder) po;
				//	Validate
				MDocType  documentType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
				if(timing == TIMING_BEFORE_PREPARE) {
					if(order.get_ValueAsInt("S_Contract_ID") <= 0) {
						if(order.getC_Project_ID() > 0) {
							MProject parentProject = MProject.getById(order.getCtx(), order.getC_Project_ID(), order.get_TrxName());
							if(parentProject.get_ValueAsInt("S_Contract_ID") > 0) {
								order.set_ValueOfColumn("S_Contract_ID", parentProject.get_ValueAsInt("S_Contract_ID"));
								order.saveEx();
							}
						}
					}
					//	Validate User1_ID
					if(order.get_ValueAsInt("S_Contract_ID") > 0 && order.getRef_Order_ID() <= 0) {
						//	For Split Order
						if(order.get_ValueAsInt(I_C_CommissionRun.COLUMNNAME_C_CommissionRun_ID) > 0) {
							MCommissionRun commissionRun = new MCommissionRun(order.getCtx(), order.get_ValueAsInt(I_C_CommissionRun.COLUMNNAME_C_CommissionRun_ID), order.get_TrxName());
							if(commissionRun.get_ValueAsInt(I_S_Contract.COLUMNNAME_S_Contract_ID) > 0) {
								MSContract contract = new MSContract(order.getCtx(), commissionRun.get_ValueAsInt(I_S_Contract.COLUMNNAME_S_Contract_ID), order.get_TrxName());
								order.setAD_Org_ID(contract.getAD_Org_ID());
								if(contract.getUser1_ID() > 0) {
									order.setUser1_ID(contract.getUser1_ID());
								}
								//	Get brand from business partner
								int user1Id = DB.getSQLValue(order.get_TrxName(), "SELECT p.User1_ID "
										+ "FROM S_ContractParties p "
										+ "WHERE S_Contract_ID = ? "
										+ "AND EXISTS(SELECT 1 FROM AD_User u WHERE u.AD_User_ID = p.AD_User_ID AND u.C_BPartner_ID = ?)", contract.getS_Contract_ID(), order.getC_BPartner_ID());
								//
								if(user1Id > 0) {
									order.setUser3_ID(user1Id);
								}
								order.saveEx();//Openup. Nicolas Sarlabos. 28/07/2020.
							}
							//	Set allows to invoice
							if(commissionRun.get_ValueAsInt("C_Invoice_ID") != 0) {
								order.set_ValueOfColumn("IsAllowToInvoice", true);
								if(Util.isEmpty(order.getPOReference())) {
									order.setPOReference("-");
								}
								order.saveEx();
							}
						} else {

							//Openup. Nicolas Sarlabos. 05/08/2020. #14419.
							if(!documentType.get_ValueAsBoolean("IsSplitDocuments")){
								if (order.get_ValueAsInt("S_Contract_ID") > 0) {

									MSContract contract = new MSContract(order.getCtx(), order.get_ValueAsInt("S_Contract_ID"), order.get_TrxName());

									if (order.getAD_Org_ID() == contract.getAD_Org_ID()) {
										int user1Id = DB.getSQLValue(order.get_TrxName(), "SELECT p.User1_ID "
												+ "FROM S_ContractParties p "
												+ "WHERE S_Contract_ID = ? "
												+ "AND EXISTS(SELECT 1 FROM AD_User u WHERE u.AD_User_ID = p.AD_User_ID AND u.C_BPartner_ID = ?)", order.get_ValueAsInt("S_Contract_ID"), order.getC_BPartner_ID());

										if (user1Id > 0) {
											order.setUser1_ID(user1Id);
											order.saveEx();
										}
									}
								}
							}
						}
					}
					// Document type IsCustomerApproved = Y and order IsCustomerApproved = N
					if (documentType.get_ValueAsBoolean("IsApprovedRequired")) {
						if(!order.get_ValueAsBoolean("IsCustomerApproved")) {
							throw new AdempiereException(Msg.parseTranslation(Env.getCtx(), "@CustomerApprovedRequired@"));
						}
						MAttachment orderAttachment = order.getAttachment(true);
						if(orderAttachment == null
								|| orderAttachment.getAD_Attachment_ID() <= 0) {
							throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AttachmentNotFound"));
						}
						//	Validate project reference
						if(!documentType.get_ValueAsBoolean("NoProjectRequired") && order.getC_Project_ID() <= 0) {
							throw new AdempiereException(Msg.parseTranslation(Env.getCtx(), "@C_Project_ID@ @NotFound@"));
						}
					}
					//	Validate Document Type for commission
					if(documentType.get_ValueAsInt("C_CommissionType_ID") > 0) {
						createCommissionForOrder(order, documentType.get_ValueAsInt("C_CommissionType_ID"), false);
					}
				} else if (timing == TIMING_AFTER_COMPLETE) {
					//	Validate Document Type for commission
					if(documentType.get_ValueAsInt("C_CommissionType_ID") > 0) {
						createCommissionForOrder(order, documentType.get_ValueAsInt("C_CommissionType_ID"), true);
					}
					//	Generate Pre-Purchase reverse
					generateReverseAmount(order);
				} else if(timing == TIMING_AFTER_VOID) {
					//	For commissions
					new Query(order.getCtx(), I_C_CommissionRun.Table_Name, I_C_Order.COLUMNNAME_C_Order_ID + " = ? "
							+ "AND " + I_C_Invoice.COLUMNNAME_C_Invoice_ID + " IS NULL "
							+ "AND DocStatus = 'CO'", order.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(order.getC_Order_ID())
					.<MCommissionRun>list().forEach(commissionRun -> {
						if(!commissionRun.processIt(MCommissionRun.DOCACTION_Void)) {
							throw new AdempiereException(commissionRun.getProcessMsg());
						}
						commissionRun.saveEx();
					});
					//	For reverses
					new Query(order.getCtx(), I_C_Order.Table_Name, "ConsumptionOrder_ID = ? "
							+ "AND DocStatus = 'CO'", order.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(order.getC_Order_ID())
					.<MOrder>list().forEach(reverseOrder -> {
						if(!reverseOrder.processIt(MOrder.DOCACTION_Void)) {
							throw new AdempiereException(reverseOrder.getProcessMsg());
						}
						reverseOrder.saveEx();
					});
					//	For Purchases (Drop Ship)
					if(order.isSOTrx()) {
						new Query(order.getCtx(), I_C_Order.Table_Name, "C_Order_ID = ? "
								+ "AND DocStatus = 'CO'", order.get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(order.getLink_Order_ID())
						.<MOrder>list().forEach(reverseOrder -> {
							if(!reverseOrder.processIt(MOrder.DOCACTION_Void)) {
								throw new AdempiereException(reverseOrder.getProcessMsg());
							}
							reverseOrder.saveEx();
						});
					}
				} else if(timing == TIMING_BEFORE_COMPLETE) {
					//	
					if(order.isSOTrx()) {
						for(MOrderLine orderLine : order.getLines()) {
							if(orderLine.getM_Product_ID() != 0) {
								MPriceList  priceList = new MPriceList(order.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
								MPriceListVersion priceListVersion = new MPriceListVersion(priceList.getCtx(), priceList.getM_PriceList_ID(), priceList.get_TrxName());
								MProduct product = new MProduct (orderLine.getCtx(), orderLine.getM_Product_ID(), orderLine.get_TrxName());
								MProductPrice productPrice = new MProductPrice(orderLine.getCtx(), priceListVersion.getM_PriceList_Version_ID(), product.getM_Product_ID(), orderLine.get_TrxName());
								if (!productPrice.get_ValueAsBoolean("IsIncludedContract")
										&& !orderLine.get_ValueAsBoolean("IsBonusProduct")) {
									BigDecimal priceEntered = orderLine.getPriceEntered();						
									if( priceEntered.compareTo(BigDecimal.ZERO) == 0) {
										throw new AdempiereException(Msg.parseTranslation(Env.getCtx(), "@PriceEntereddontBeZero@"));
									}
								}	
							}
						}
					}
				} else if (timing == TIMING_AFTER_CLOSE) {
					updateCommissionRunForOrder(order);
				}
			} else if(po instanceof MCommissionRun) {
				MCommissionRun commissionRun = (MCommissionRun) po;
				if(timing == TIMING_AFTER_VOID) {
					new Query(commissionRun.getCtx(), I_C_Order.Table_Name, I_C_CommissionRun.COLUMNNAME_C_CommissionRun_ID + " = ? "
							+ "AND DocStatus = 'CO'", commissionRun.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(commissionRun.getC_CommissionRun_ID())
					.<MOrder>list().forEach(order -> {
						if(!order.processIt(MOrder.DOCACTION_Void)) {
							throw new AdempiereException(order.getProcessMsg());
						}
						order.saveEx();
					});
				}
			} else if(po instanceof MTimeExpense) {
				MTimeExpense expenseReport = (MTimeExpense) po;
				if(timing == TIMING_BEFORE_COMPLETE) {
					//				for(MTimeExpenseLine expenseReportLine : expenseReport.getLines()) {
					//					if(expenseReportLine.getC_Project_ID() <= 0) {
					//						continue;
					//					}
					//					StringBuffer whereClause = new StringBuffer();
					//					whereClause.append("C_Project_ID = ? "
					//							+ "AND EXISTS(SELECT 1 FROM S_TimeExpense te ")
					//						.append("WHERE S_TimeExpenseLine.S_TimeExpense_ID = te.S_TimeExpense_ID AND (te.DocStatus = ? OR te.S_TimeExpense_ID = ?))");
					//					BigDecimal expenseAmt = new Query(po.getCtx(), I_S_TimeExpenseLine.Table_Name, whereClause.toString(), po.get_TrxName())
					//							.setClient_ID()
					//							.setParameters(expenseReportLine.getC_Project_ID(), MTimeExpense.DOCSTATUS_Completed, expenseReportLine.getS_TimeExpense_ID())
					//							.sum(MTimeExpenseLine.COLUMNNAME_ExpenseAmt);
					//					//	
					//					MProject project = MProject.getById(expenseReport.getCtx(), expenseReportLine.getC_Project_ID(), expenseReport.get_TrxName());
					//					BigDecimal plannedAmt = project.getPlannedAmt();
					//					if(plannedAmt != null
					//							&& plannedAmt.compareTo(expenseAmt) < 0) {
					//						DecimalFormat format = DisplayType.getNumberFormat(DisplayType.Amount);
					//						throw new AdempiereException(Msg.parseTranslation(Env.getCtx(), 
					//								"@S_TimeExpenseLine_ID@ > @PlannedAmt@ @C_Project_ID@ [" + project.getName() + ", " + format.format(plannedAmt) + "] @ExpenseAmt@ " + format.format(expenseAmt)));
					//					}
					//				}
				} else if(timing == TIMING_AFTER_COMPLETE) {
					Hashtable<Integer, Hashtable<Integer, BigDecimal>> orders = new Hashtable<Integer, Hashtable<Integer, BigDecimal>>();
					for(MTimeExpenseLine line : expenseReport.getLines()) {
						//	Validate Orders
						int salesOrderLineId = line.getC_OrderLine_ID();
						int linkOrderLineId = line.get_ValueAsInt("Link_OrderLine_ID");
						if(salesOrderLineId <= 0
								&& linkOrderLineId <= 0
								|| line.getQty() == null
								|| line.getQty().compareTo(Env.ZERO) <= 0) {
							continue;
						}
						//	For sales
						if(!expenseReport.get_ValueAsBoolean("IsNotGenerateDelivery")) {//Openup. Nicolas Sarlabos. 21/10/2020. #14905.
							if(salesOrderLineId > 0) {
								MOrderLine salesOrderLine = new MOrderLine(expenseReport.getCtx(), salesOrderLineId, expenseReport.get_TrxName());
								Hashtable<Integer, BigDecimal> salesOrderLines = orders.get(salesOrderLine.getC_Order_ID());
								if(salesOrderLines == null) {
									salesOrderLines = new Hashtable<Integer, BigDecimal>();
								}
								//	Add
								salesOrderLines.put(salesOrderLine.getC_OrderLine_ID(), line.getQty());
								orders.put(salesOrderLine.getC_Order_ID(), salesOrderLines);
							}
						}//Fin #14905.
						//	For purchases
						if(!expenseReport.get_ValueAsBoolean("IsNotGenerateReceipt")){//Openup. Nicolas Sarlabos. 16/09/2020. #14415.
							if(linkOrderLineId > 0) {
								MOrderLine salesOrderLine = new MOrderLine(expenseReport.getCtx(), linkOrderLineId, expenseReport.get_TrxName());
								Hashtable<Integer, BigDecimal> purchaseOrderLines = orders.get(salesOrderLine.getC_Order_ID());
								if(purchaseOrderLines == null) {
									purchaseOrderLines = new Hashtable<Integer, BigDecimal>();
								}
								//	Add
								purchaseOrderLines.put(salesOrderLine.getC_OrderLine_ID(), line.getQty());
								orders.put(salesOrderLine.getC_Order_ID(), purchaseOrderLines);
							}
						}
					}
					//	Generate from orders
					//Openup. Nicolas Sarlabos. 07/01/2020. #13498.
					if(!expenseReport.get_ValueAsBoolean("IsFromReturn")){
						orders.entrySet().stream().forEach(orderSet -> {
							generateInOutFromOrder(expenseReport, orderSet.getKey(), orderSet.getValue());
						});
					}//Fin #13498.

				}
			} else if(po instanceof MInvoice) {
				MInvoice invoice = (MInvoice) po;
				if(timing == TIMING_BEFORE_PREPARE) {
					if(invoice.get_ValueAsInt("S_Contract_ID") <= 0) {
						if(invoice.getC_Project_ID() > 0) {
							MProject parentProject = MProject.getById(invoice.getCtx(), invoice.getC_Project_ID(), invoice.get_TrxName());
							if(parentProject.get_ValueAsInt("S_Contract_ID") > 0) {
								invoice.set_ValueOfColumn("S_Contract_ID", parentProject.get_ValueAsInt("S_Contract_ID"));
								invoice.saveEx();
							}
						}
					}
					//	Validate IsAllowToInvoice
					if(invoice.getC_Order_ID() > 0) {
						MOrder order = (MOrder) invoice.getC_Order();
						//	Set Project Phase and Task
						if(order.get_ValueAsInt(I_C_ProjectPhase.COLUMNNAME_C_ProjectPhase_ID) > 0) {
							invoice.set_ValueOfColumn(I_C_ProjectPhase.COLUMNNAME_C_ProjectPhase_ID, order.get_ValueAsInt(I_C_ProjectPhase.COLUMNNAME_C_ProjectPhase_ID));
						}
						if(order.get_ValueAsInt(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID) > 0) {
							invoice.set_ValueOfColumn(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID, order.get_ValueAsInt(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID));
						}
						invoice.saveEx();
						if(invoice.isSOTrx()) {
							//Openup. Nicolas Sarlabos. 05/10/2020. #14819.
							MDocType docType = (MDocType) invoice.getC_DocTypeTarget();
							if(!order.get_ValueAsBoolean("IsAllowToInvoice")
									&& !docType.getDocBaseType().equalsIgnoreCase(MDocType.DOCBASETYPE_ARCreditMemo)) {
								throw new AdempiereException("@C_Order_ID@ " + order.getDocumentNo() + " @IsAllowToInvoiceRequired@");
							}//Fin #14819.
						}
					}
				} else if(timing == TIMING_AFTER_COMPLETE) {
					if(!invoice.isReversal()) {
						//	Validate
						MDocType  documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
						//	Validate Document Type for commission
						if(documentType.get_ValueAsInt("C_CommissionType_ID") > 0) {
							createCommissionForInvoice(invoice, documentType.get_ValueAsInt("C_CommissionType_ID"), true);
						}
					}
				} else if(timing == TIMING_AFTER_REVERSECORRECT
						|| timing == TIMING_AFTER_REVERSEACCRUAL
						|| timing == TIMING_AFTER_VOID) {
					new Query(invoice.getCtx(), I_C_CommissionRun.Table_Name, I_C_Invoice.COLUMNNAME_C_Invoice_ID + " = ? "
							+ "AND DocStatus = 'CO'", invoice.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(invoice.getC_Invoice_ID())
					.<MCommissionRun>list().forEach(commissionRun -> {
						if(!commissionRun.processIt(MCommissionRun.DOCACTION_Void)) {
							throw new AdempiereException(commissionRun.getProcessMsg());
						}
					});
				}
			} else if(po instanceof MSContract) {
				if(timing == TIMING_BEFORE_COMPLETE) {
					MSContract serviceContract = (MSContract) po;
					String whereClause = "S_Contract_ID = ?";
					X_C_CommissionSalesRep salesRep = new Query(po.getCtx(), I_C_CommissionSalesRep.Table_Name, whereClause, po.get_TrxName())
							.setParameters(serviceContract.getS_Contract_ID())
							.setOnlyActiveRecords(true)
							.<X_C_CommissionSalesRep>first();
							if(salesRep != null
									&& salesRep.getC_CommissionSalesRep_ID() > 0) {
								BigDecimal sumPercent = new Query(po.getCtx(), I_C_CommissionSalesRep.Table_Name, whereClause, po.get_TrxName())
										.setParameters(serviceContract.getS_Contract_ID())
										.sum("AmtMultiplier");
								if(sumPercent.compareTo(Env.ONEHUNDRED) != 0){					
									throw new AdempiereException(Msg.getMsg(Env.getCtx(), "TotalPercentageIsNot100"));
								}
							}
				}
			}
			return null;
		}	//	docValidate

		/**
		 * Generate In/Out from Sales or Purchase Orders
		 * @param orderId
		 * @param lines
		 */
		private void generateInOutFromOrder(MTimeExpense expenseReport, int orderId, Hashtable<Integer, BigDecimal> lines) {
			MOrder order = new MOrder(expenseReport.getCtx(), orderId, expenseReport.get_TrxName());
			MInOut inOut = new MInOut(order, 0, expenseReport.getDateReport());

			//Solop. Nicolas Sarlabos. 01/02/2021. #15491.
			if (!MPeriod.isOpen(inOut.getCtx(), expenseReport.getDateReport(), inOut.getC_DocType().getDocBaseType(), inOut.getAD_Org_ID()))
				throw new AdempiereException("ERROR: Período cerrado para tipo de documento '" + inOut.getC_DocType().getName() + "'");
			//#15491.

			inOut.setM_Warehouse_ID(order.getM_Warehouse_ID());
			inOut.set_ValueOfColumn("S_TimeExpense_ID", expenseReport.get_ID());//Openup. Nicolas Sarlabos. 24/12/2019. #13331.
			inOut.saveEx();
			AtomicReference<BigDecimal> totalOrdered = new AtomicReference<BigDecimal>(Env.ZERO);
			AtomicReference<BigDecimal> totalDelivered = new AtomicReference<BigDecimal>(Env.ZERO);
			lines.entrySet().stream().forEach(linesSet -> {
				MOrderLine orderLine = new MOrderLine(expenseReport.getCtx(), linesSet.getKey(), expenseReport.get_TrxName());
				BigDecimal toDeliver = linesSet.getValue();

				createInoutLine(inOut, orderLine, toDeliver);

				totalOrdered.updateAndGet(amount -> amount.add(orderLine.getLineNetAmt()));
				totalDelivered.updateAndGet(amount -> amount.add(orderLine.getPriceActual().multiply(toDeliver)));
			});
			//	Complete In/Out
			inOut.setDocStatus(MInOut.DOCSTATUS_Drafted);
			inOut.processIt(MInOut.ACTION_Complete);
			inOut.saveEx();
			//	Generate Delivery for Commission
//			if(totalOrdered.get() != null
//					&& totalOrdered.get().compareTo(Env.ZERO) > 0
//					&& totalDelivered.get() != null
//					&& totalDelivered.get().compareTo(Env.ZERO) > 0) {
//				//	Calculate Percentage
//				BigDecimal multiplier = Env.ONE.divide(totalOrdered.get(), MathContext.DECIMAL128).multiply(totalDelivered.get());
//				generateInOutFromCommissionOrder(order, multiplier);
//			}

			//Openup. Nicolas Sarlabos. 26/12/2019. #12701.
            if (expenseReport.getAD_Org_ID() == 2000004) {//si es checkin MANEREL
                if (inOut.isSOTrx() && inOut.getAD_Org_ID() == 2000004) {

                	MBPartner partner = inOut.getBPartner();

                	if(partner.getAD_OrgBP_ID() != null){

						MOrder sOrder = (MOrder) inOut.getC_Order();

						String sql = "select c_order_id" +
								" from c_order" +
								" where ref_order_id = " + sOrder.get_ID() +
								" and issotrx = 'N'" +
								" and docstatus in ('CO','CL')";

						int orderID = DB.getSQLValueEx(inOut.get_TrxName(), sql);

						if (orderID > 0) {

							MOrder pOrder = new MOrder(expenseReport.getCtx(), orderID, expenseReport.get_TrxName());

							sql = "select c_order_id" +
									" from c_order" +
									" where link_order_id = " + orderID +
									" and docstatus in ('CO','CL')";

							orderID = DB.getSQLValueEx(inOut.get_TrxName(), sql);

							if(orderID > 0){

								createDeliveryFromSO(inOut, orderID, inOut.get_ValueAsInt("S_TimeExpense_ID"));

							} else throw new AdempiereException("No se obtuvo orden de venta, completa o cerrada, para la orden de compra Nro. " + pOrder.getDocumentNo());

						} else throw new AdempiereException("No se obtuvo orden de compra, completa o cerrada, para la orden de venta Nro. " + sOrder.getDocumentNo());
					}
                }
            }//Fin #12701.
		}

	private List<Pair<Integer, BigDecimal>> getInvoiceLines(MInOut inOut, MOrderLine orderLine) {

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String whereDocBaseType = "";
		List<Pair<Integer, BigDecimal>> invoiceLines = new ArrayList<>();

		try{

			MDocType inOutDocType = (MDocType) inOut.getC_DocType();

			//si es Entrega o Recepcion
			if ((inOutDocType.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialReceipt) && !inOutDocType.isSOTrx())
					|| (inOutDocType.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialDelivery) && inOutDocType.isSOTrx())) {
				whereDocBaseType = " AND d.docbasetype in ('ARI','API')";

			//si es Devolucion Cliente o Proveedor
			} else if((inOutDocType.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialReceipt) && inOutDocType.isSOTrx())
					|| (inOutDocType.getDocBaseType().equals(MDocType.DOCBASETYPE_MaterialDelivery) && !inOutDocType.isSOTrx())){
				whereDocBaseType = " AND d.docbasetype in ('ARC','APC')";

			}

			String sql = "SELECT il.c_invoiceline_id, (SELECT coalesce(SUM(qty),0)" +
					" FROM m_matchinv WHERE c_invoiceline_id = il.c_invoiceline_id) as qtyUsed" +
					" FROM c_invoiceline il" +
					" JOIN c_invoice i ON il.c_invoice_id = i.c_invoice_id" +
					" JOIN c_doctype d ON i.c_doctypetarget_id = d.c_doctype_id" +
					" WHERE i.docstatus IN ('CO','CL')" +
					" AND il.c_orderline_id = " + orderLine.get_ID() + whereDocBaseType;

			pstmt = DB.prepareStatement(sql, orderLine.get_TrxName());
			rs = pstmt.executeQuery();

			while (rs.next()){

				MInvoiceLine iLine = new MInvoiceLine(orderLine.getCtx(), rs.getInt("C_InvoiceLine_ID"), orderLine.get_TrxName());
				BigDecimal qtyInvoiced = iLine.getQtyInvoiced();
				BigDecimal qtyUsed = rs.getBigDecimal("qtyUsed");
				BigDecimal qtyInvAvailable = qtyInvoiced.subtract(qtyUsed);

				if(qtyInvAvailable.compareTo(Env.ZERO) > 0)
					invoiceLines.add(new Pair<>(rs.getInt("C_InvoiceLine_ID"),qtyInvAvailable));

			}

		} catch (Exception e) {
			throw new AdempiereException(e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		return invoiceLines;
	}

	private void createInoutLine(MInOut inOut, MOrderLine orderLine, BigDecimal toDeliver) {

		try {

			List<Pair<Integer, BigDecimal>> invoiceLines = getInvoiceLines(inOut, orderLine);

			BigDecimal restShipQtyEntered = toDeliver;
			BigDecimal qtyShip = toDeliver;

			if(invoiceLines.isEmpty()){

				MInOutLine inOutLine = new MInOutLine (inOut);
				inOutLine.setOrderLine(orderLine, 0, qtyShip);
				inOutLine.setQty(qtyShip);
				inOutLine.saveEx();

			} else {

				for (Pair<Integer, BigDecimal> invoiceLine : invoiceLines) {

					if(restShipQtyEntered.compareTo(Env.ZERO) <= 0)
						break;

					qtyShip = restShipQtyEntered;

					int invoiceLineID = invoiceLine.getValue0();
					BigDecimal qtyInvAvailable = invoiceLine.getValue1();

					if(qtyShip.compareTo(qtyInvAvailable) > 0)
						qtyShip = qtyInvAvailable;

					MInOutLine inOutLine = new MInOutLine (inOut);
					inOutLine.setOrderLine(orderLine, 0, qtyShip);
					inOutLine.setQty(qtyShip);
					inOutLine.set_ValueOfColumn("C_InvoiceLine_ID", invoiceLineID);
					inOutLine.saveEx();

					restShipQtyEntered = restShipQtyEntered.subtract(qtyShip);
				}

				if(restShipQtyEntered.compareTo(Env.ZERO) > 0){
					MInOutLine inOutLine = new MInOutLine (inOut);
					inOutLine.setOrderLine(orderLine, 0, restShipQtyEntered);
					inOutLine.setQty(restShipQtyEntered);
					inOutLine.saveEx();
				}
			}

		} catch (Exception e) {
			throw new AdempiereException(e.getMessage());
		}
	}

    /**
     * OpenUp. Nicolas Sarlabos. 04/11/2019. #12701.
     * Metodo que crea la entrega de cliente para la OV asociada a la OC de la recepcion (solo XAXIS).
     */
    private void createDeliveryFromSO(MInOut inout, int orderID, int expenseID) {

        MInOut inoutHdr = null;
        MOrder order = new MOrder (inout.getCtx(), orderID, inout.get_TrxName());
        MOrderLine[] lines = order.getLines ();

        for (int i = 0; i < lines.length; i++){

            MOrderLine oLine = lines [i];

            if(inoutHdr == null){

                //creo cabezal de entrega
                inoutHdr = new MInOut(order, 0, inout.getMovementDate());
                inoutHdr.setAD_Org_ID(order.getAD_Org_ID());
                inoutHdr.setC_BPartner_ID(order.getC_BPartner_ID());
                inoutHdr.set_ValueOfColumn("S_TimeExpense_ID", expenseID);
                inoutHdr.saveEx();

            }

            int link_line_id = oLine.getLink_OrderLine_ID();

            String sql = "select l.m_inoutline_id" +
                    " from m_inoutline l" +
                    " inner join m_inout h on l.m_inout_id = h.m_inout_id" +
                    " where h.docstatus in ('CO','CL')" +
                    " and l.c_orderline_id = " + link_line_id +
                    " and h.issotrx = 'N'" +
                    " and s_timeexpense_id = " + expenseID;

            int ioLine_id = DB.getSQLValueEx(inout.get_TrxName(), sql);

            if(ioLine_id > 0){

                MInOutLine ref_line = new MInOutLine(inout.getCtx(), ioLine_id, inout.get_TrxName());
                MInOutLine inOutLine;
				List<Pair<Integer, BigDecimal>> invoiceLines = getInvoiceLines(inoutHdr, oLine);

				BigDecimal restShipQtyEntered = ref_line.getQtyEntered();
				BigDecimal qtyShip;

				if(!invoiceLines.isEmpty()){

					for (Pair<Integer, BigDecimal> invoiceLine : invoiceLines) {

						if(restShipQtyEntered.compareTo(Env.ZERO) <= 0)
							break;

						qtyShip = restShipQtyEntered;

						int invoiceLineID = invoiceLine.getValue0();
						BigDecimal qtyInvAvailable = invoiceLine.getValue1();

						if(qtyShip.compareTo(qtyInvAvailable) > 0)
							qtyShip = qtyInvAvailable;

						inOutLine = new MInOutLine (inoutHdr);
						inOutLine.setOrderLine(oLine, 0, qtyShip);
						inOutLine.setM_Warehouse_ID(oLine.getM_Warehouse_ID());
						inOutLine.setQty(qtyShip);
						inOutLine.set_ValueOfColumn("C_InvoiceLine_ID", invoiceLineID);
						inOutLine.saveEx();

						restShipQtyEntered = restShipQtyEntered.subtract(qtyShip);
					}

					if(restShipQtyEntered.compareTo(Env.ZERO) > 0){
						inOutLine = new MInOutLine (inoutHdr);
						inOutLine.setOrderLine(oLine, 0, restShipQtyEntered);
						inOutLine.setM_Warehouse_ID(oLine.getM_Warehouse_ID());
						inOutLine.setQty(restShipQtyEntered);
						inOutLine.saveEx();
					}

				} else {

					// Genero linea de entrega
					MInOutLine inoutLine = new MInOutLine(inoutHdr);
					inoutLine.setC_OrderLine_ID(oLine.get_ID());
					inoutLine.setM_Product_ID(oLine.getM_Product_ID());
					inoutLine.setC_UOM_ID(oLine.getC_UOM_ID());
					inoutLine.setM_AttributeSetInstance_ID(oLine.getM_AttributeSetInstance_ID());
					inoutLine.setM_Warehouse_ID(oLine.getM_Warehouse_ID());
					inoutLine.setUser1_ID(oLine.getUser1_ID());//Solop. Nicolas Sarlabos. 5/7/2021. #16333.
					inoutLine.setUser4_ID(oLine.getUser4_ID());//Solop. Nicolas Sarlabos. 5/7/2021. #16333.
					MWarehouse warehouse = (MWarehouse)oLine.getM_Warehouse();
					inoutLine.setM_Locator_ID(MLocator.getDefault(warehouse).get_ID());
					inoutLine.setQtyEntered(ref_line.getQtyEntered());
					inoutLine.setMovementQty(ref_line.getMovementQty());
					inoutLine.saveEx();
				}
            }
        }

        //completo la entrega si tiene lineas, si no elimino el cabezal
        if(inoutHdr.getLines(true).length > 0){
            if (!inoutHdr.processIt(DocumentEngine.ACTION_Complete)){
                throw new AdempiereException (inoutHdr.getProcessMsg());
            }
        } else inoutHdr.deleteEx(true);
    }

    /**
		 * Reverse Previous commission
		 * @param sourceInvoice
		 * @param reverseAmount
		 */
		private static void reversePreviousCommissionOrders(MInvoice sourceInvoice, BigDecimal reverseAmount) {
			new Query(sourceInvoice.getCtx(), I_C_Order.Table_Name, 
					"DocStatus IN ('CO', 'CL') "
							+ "AND grandTotal > 0 "
							+ "AND EXISTS(SELECT 1 FROM C_CommissionRun cr "
							+ "WHERE cr.C_CommissionRun_ID = C_Order.C_CommissionRun_ID "
							+ "AND cr.C_Invoice_ID IS NULL "
							+ "AND cr.docStatus='CO'"
							+ "AND EXISTS(SELECT 1 FROM C_InvoiceLine il "
							+ "	INNER JOIN C_OrderLine ol ON(ol.C_OrderLine_ID = il.C_OrderLine_ID) "
							+ "	WHERE il.C_Invoice_ID = ? "
							+ "	AND ol.C_Order_ID = cr.C_Order_ID))", sourceInvoice.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(sourceInvoice.getC_Invoice_ID())
			.<MOrder>list()
			.stream().forEach(commissionOrder -> {
				MOrder reverseOrder = new MOrder(commissionOrder.getCtx(), 0, commissionOrder.get_TrxName());
				PO.copyValues(commissionOrder, reverseOrder);
				reverseOrder.setDocumentNo(null);
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
				reverseOrder.set_ValueOfColumn("ConsumptionOrder_ID", null);
				reverseOrder.set_ValueOfColumn("PreOrder_ID", null);
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
				reverseOrderLine.setPrice(reverseAmount);
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
		
		/**
		 * Reverse amount of pre-purchase order from a purchase order
		 * @param sourceOrder
		 */
		private void generateReverseAmount(MOrder sourceOrder) {
			int projectPhaseId = sourceOrder.get_ValueAsInt("C_ProjectPhase_ID");
			if(projectPhaseId <= 0) {
				return;
			}
			//	
			MDocType documentType = MDocType.get(sourceOrder.getCtx(), sourceOrder.getC_DocTypeTarget_ID());
			if(documentType.get_ValueAsBoolean("IsConsumePreOrder")) {
				int reverseDocumentTypeId = documentType.get_ValueAsInt("C_DocTypeReversal_ID");
				//	find all purchase order of pre-purchase
				MOrder preOrder = new Query(sourceOrder.getCtx(), I_C_Order.Table_Name, "DocStatus = 'CO' "
						+ "AND C_ProjectPhase_ID = ? "
						+ "AND AD_Org_ID = ? "
						+ "AND IsSOTrx = '" + (sourceOrder.isSOTrx()? "Y": "N") + "' "
						+ "AND EXISTS(SELECT 1 FROM C_DocType dt WHERE dt.C_DocType_ID = C_Order.C_DocType_ID AND dt.IsPreOrder = 'Y')", sourceOrder.get_TrxName())
						.setParameters(projectPhaseId, sourceOrder.getAD_Org_ID())
						.first();
				//	Validate
				if(preOrder != null
						&& preOrder.getC_Order_ID() > 0) {
					BigDecimal consumeAmount = DB.getSQLValueBD(sourceOrder.get_TrxName(), "SELECT SUM(TotalLines) "
							+ "FROM C_Order o "
							+ "WHERE o.DocStatus IN('CO') "
							+ "AND o.PreOrder_ID = ? "
							+ "AND o.IsSOTrx = '" + (sourceOrder.isSOTrx()? "Y": "N") + "' "
							+ "AND EXISTS(SELECT 1 FROM C_DocType dt WHERE dt.C_DocType_ID = o.C_DocType_ID AND dt.IsConsumePreOrder = 'Y')", preOrder.getC_Order_ID());
					//	Validate
					if(consumeAmount == null) {
						consumeAmount = Env.ZERO;
					}
					BigDecimal releasedAmount = DB.getSQLValueBD(sourceOrder.get_TrxName(), "SELECT SUM(ol.ReleasedQty * ol.PriceActual) "
							+ "FROM C_Order o "
							+ "INNER JOIN C_OrderLine ol ON(ol.C_Order_ID = o.C_Order_ID) "
							+ "WHERE o.DocStatus IN('CO') "
							+ "AND COALESCE(ol.ReleasedQty, 0) > 0 "
							+ "AND o.PreOrder_ID = ? "
							+ "AND o.IsSOTrx = '" + (sourceOrder.isSOTrx()? "Y": "N") + "' "
							+ "AND EXISTS(SELECT 1 FROM C_DocType dt WHERE dt.C_DocType_ID = o.C_DocType_ID AND dt.IsConsumePreOrder = 'Y')", preOrder.getC_Order_ID());
					//	Validate
					if(releasedAmount == null) {
						releasedAmount = Env.ZERO;
					}
					//	Subtract Released Amount
					consumeAmount = consumeAmount.subtract(releasedAmount);
					consumeAmount = consumeAmount.add(sourceOrder.getTotalLines());
					if(consumeAmount.compareTo(preOrder.getTotalLines()) > 0) {
						DecimalFormat format = DisplayType.getNumberFormat(DisplayType.Amount);
						throw new AdempiereException("[@ConsumedAmt@] > @PreOrderAmt@ (@PreOrderAmt@ = " 
								+ format.format(preOrder.getTotalLines()) + ", @ConsumedAmt@ = " + format.format(consumeAmount) 
								+ Env.NL + "@amount.difference@ = " + format.format(preOrder.getTotalLines().subtract(consumeAmount)) + ")");
					}
					//	Generate document
					if(reverseDocumentTypeId > 0) {
						MOrder reverseOrder = new MOrder(sourceOrder.getCtx(), 0, sourceOrder.get_TrxName());
						PO.copyValues(preOrder, reverseOrder);
						reverseOrder.setDocumentNo(null);
						reverseOrder.setC_DocTypeTarget_ID(reverseDocumentTypeId);
						reverseOrder.setDateOrdered(sourceOrder.getDateOrdered());
						reverseOrder.setDatePromised(sourceOrder.getDatePromised());
						reverseOrder.setPOReference(sourceOrder.getDocumentNo());
						reverseOrder.addDescription(Msg.parseTranslation(sourceOrder.getCtx(), "@Generated@ [@C_Order_ID@ " + sourceOrder.getDocumentNo()) + "]");
						reverseOrder.setDocStatus(MOrder.DOCSTATUS_Drafted);
						reverseOrder.setDocAction(MOrder.DOCACTION_Complete);
						reverseOrder.setTotalLines(Env.ZERO);
						reverseOrder.setGrandTotal(Env.ZERO);
						reverseOrder.setIsSOTrx(sourceOrder.isSOTrx());
						reverseOrder.setRef_Order_ID(-1);
						reverseOrder.setIsDropShip(false);
						reverseOrder.setDropShip_BPartner_ID(0);
						reverseOrder.setDropShip_Location_ID(0);
						reverseOrder.setDropShip_User_ID(0);
						reverseOrder.set_ValueOfColumn("ConsumptionOrder_ID", sourceOrder.getC_Order_ID());
						reverseOrder.set_ValueOfColumn("PreOrder_ID", preOrder.getC_Order_ID());
						reverseOrder.set_ValueOfColumn("IsAllowToInvoice", false);//Openup. Nicolas Sarlabos. 29/05/2020. #14134.
						reverseOrder.saveEx();

						DB.executeUpdateEx("update c_order set ad_org_id = " + sourceOrder.getAD_Org_ID() + " where c_order_id = " + reverseOrder.get_ID(), sourceOrder.get_TrxName());

						if(sourceOrder.getUser1_ID() > 0){
							DB.executeUpdateEx("update c_order set user1_id = " + sourceOrder.getUser1_ID() + " where c_order_id = " + reverseOrder.get_ID(), sourceOrder.get_TrxName());
						}

						if(sourceOrder.getUser3_ID() > 0){
							DB.executeUpdateEx("update c_order set user3_id = " + sourceOrder.getUser3_ID() + " where c_order_id = " + reverseOrder.get_ID(), sourceOrder.get_TrxName());
						}

						DB.executeUpdateEx("update c_order set link_order_id = null where c_order_id = " + reverseOrder.get_ID(), sourceOrder.get_TrxName());//Openup. Nicolas Sarlabos. 28/09/2020. #14716.
						//	Add Line
						MOrderLine preOrderLine = preOrder.getLines(true, null)[0];

						MOrderLine reverseOrderLine = new MOrderLine(reverseOrder);
						PO.copyValues(reverseOrderLine, preOrderLine);
						reverseOrderLine.setOrder(reverseOrder);
						reverseOrderLine.setProduct(preOrderLine.getProduct());
						reverseOrderLine.setLineNetAmt(Env.ZERO);
						reverseOrderLine.setQty(Env.ONE);
						reverseOrderLine.setPrice(sourceOrder.getTotalLines().negate());
						reverseOrderLine.setTax();
						reverseOrderLine.saveEx();
						//	Complete
						if(!reverseOrder.processIt(MOrder.DOCACTION_Complete)) {
							throw new AdempiereException(reverseOrder.getProcessMsg());
						}
						reverseOrder.saveEx();
						//	Set pre Order
						if(sourceOrder.get_ValueAsInt("PreOrder_ID") <= 0) {
							sourceOrder.set_ValueOfColumn("PreOrder_ID", preOrder.getC_Order_ID());
						}
						sourceOrder.saveEx();
					}
				}
			}
		}

		/**
		 * Generate Delivery from commission Order that are generated from order
		 * @param order
		 * @param multiplier
		 */
//		private void generateInOutFromCommissionOrder(MOrder order, BigDecimal multiplier) {
//			new Query(order.getCtx(), I_C_Order.Table_Name, 
//					"DocStatus = 'CO' "
//							+ "AND EXISTS(SELECT 1 FROM C_CommissionRun cr "
//							+ "WHERE cr.C_CommissionRun_ID = C_Order.C_CommissionRun_ID "
//							+ "AND cr.C_Order_ID = ?) "
//							+ "AND EXISTS(SELECT 1 FROM C_OrderLine ol "
//							+ "WHERE ol.C_Order_ID = C_Order.C_Order_ID "
//							+ "AND ol.QtyOrdered > COALESCE(QtyDelivered, 0))", order.get_TrxName())
//			.setOnlyActiveRecords(true)
//			.setParameters(order.getC_Order_ID())
//			.<MOrder>list()
//			.stream().forEach(commissionOrder -> {
//				for(MOrderLine line : commissionOrder.getLines()) {
//					MInOut shipment = new MInOut(commissionOrder, 0, commissionOrder.getDateOrdered());
//					shipment.setM_Warehouse_ID(line.getM_Warehouse_ID());
//					shipment.setMovementDate(line.getDatePromised());
//					shipment.saveEx();
//					//	Add Line
//					BigDecimal qtyToDeliver = line.getQtyOrdered().multiply(multiplier);
//					MInOutLine sline = new MInOutLine(shipment);
//					sline.setOrderLine(line, 0, qtyToDeliver);
//					sline.setQtyEntered(qtyToDeliver);
//					sline.setC_UOM_ID(line.getC_UOM_ID());
//					sline.setQty(qtyToDeliver);
//					sline.setM_Warehouse_ID(line.getM_Warehouse_ID());
//					sline.saveEx();
//					//	Process It
//					if (!shipment.processIt(DocAction.ACTION_Complete)) {
//						log.warning("Failed: " + shipment);
//					}
//					shipment.saveEx();
//					//					ProcessBuilder.create(order.getCtx())
//					//						.process(OrderLineCreateShipmentAbstract.getProcessId())
//					//						.withRecordId(I_C_OrderLine.Table_ID, line.getC_OrderLine_ID())
//					//						.withParameter(OrderLineCreateShipmentAbstract.DOCACTION, DocAction.ACTION_Complete)
//					//						.withoutTransactionClose()
//					//					.execute(order.get_TrxName());
//				}
//			});
//		}

		/**
		 * Create a commission based on rules defined and get result inside order line 
		 * it is only running if exists a flag for document type named (Calculate commission for Order)
		 * @param order
		 * @param
		 */
		private static void createCommissionForOrder(MOrder order, int commissionTypeId, boolean splitDocuments) {
			if(MOrgInfo.get(order.getCtx(), order.getAD_Org_ID(), order.get_TrxName()).get_ValueAsBoolean("IsExcludeOfCommission")) {
				return;
			}
			removeLineFromCommission(order, commissionTypeId);
			new Query(order.getCtx(), I_C_Commission.Table_Name, I_C_CommissionType.COLUMNNAME_C_CommissionType_ID + " = ? "
					+ "AND IsSplitDocuments = ?", order.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(commissionTypeId, (splitDocuments? "Y": "N"))
			.<MCommission>list().forEach(commissionDefinition -> {
				int documentTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_SalesCommission, order.getAD_Org_ID());
				MCommissionRun commissionRun = new MCommissionRun(commissionDefinition);
				commissionRun.setDateDoc(getCurrentDate()); // Redmine #13452
				commissionRun.setC_DocType_ID(documentTypeId);
				commissionRun.setDescription(Msg.parseTranslation(order.getCtx(), "@Generate@: @C_Order_ID@ - " + order.getDocumentNo()));
				commissionRun.set_ValueOfColumn("C_Order_ID", order.getC_Order_ID());
				commissionRun.set_ValueOfColumn("C_Currency_ID", order.getC_Currency_ID());
				commissionRun.setAD_Org_ID(order.getAD_Org_ID());
				commissionRun.saveEx();
				//	Process commission
				// TODO: Implement It
//				commissionRun.addFilterValues("C_Order_ID", order.getC_Order_ID());
				commissionRun.setDocStatus(MCommissionRun.DOCSTATUS_Drafted);
				//	Complete
				if(commissionRun.processIt(MCommissionRun.DOCACTION_Complete)) {
					commissionRun.updateFromAmt();
					commissionRun.saveEx();
					if(commissionRun.getGrandTotal() != null
							&& commissionRun.getGrandTotal().compareTo(Env.ZERO) > 0) {
						if(commissionDefinition.get_ValueAsBoolean("IsSplitDocuments")) {

							int docType_ID =  commissionDefinition.get_ValueAsInt("C_DocTypeOrder_ID");

							if(!order.isSOTrx() && order.get_ValueAsBoolean("IsDirectInvoice")){
								int docTypeSystem_ID = Integer.valueOf(MSysConfig.getValue("UY_DIRECT_COMMISSION_DOCTYPE_OV", "0", order.getAD_Client_ID()));

								if(docTypeSystem_ID > 0)
									docType_ID = docTypeSystem_ID;
							}

							ProcessBuilder.create(order.getCtx())
							.process(CommissionOrderCreateAbstract.getProcessId())
							.withRecordId(I_C_CommissionRun.Table_ID, commissionRun.getC_CommissionRun_ID())
							.withParameter(CommissionOrderCreateAbstract.ISSOTRX, true)
							.withParameter(CommissionOrderCreateAbstract.DATEORDERED, commissionRun.getDateDoc()) // Redmine #13452
							.withParameter(CommissionOrderCreateAbstract.DOCACTION, DocAction.ACTION_Complete)
							.withParameter(CommissionOrderCreateAbstract.C_BPARTNER_ID, order.getC_BPartner_ID())
							.withParameter(CommissionOrderCreateAbstract.C_DOCTYPE_ID, docType_ID)
							.withoutTransactionClose()
							.execute(order.get_TrxName());
						} else {
							commissionRun.getCommissionAmtList().stream()
							.filter(commissionAmt -> commissionAmt.getCommissionAmt() != null 
							&& commissionAmt.getCommissionAmt().compareTo(Env.ZERO) > 0).forEach(commissionAmt -> {
								MOrderLine orderLine = new MOrderLine(order);
								orderLine.setC_Charge_ID(commissionDefinition.getC_Charge_ID());
								orderLine.setQty(Env.ONE);
								orderLine.setPrice(commissionAmt.getCommissionAmt());
								orderLine.setTax();
								orderLine.saveEx(order.get_TrxName());
							});
						}
					}
				} else {
					throw new AdempiereException(commissionRun.getProcessMsg());
				}
			});
		}

		/**
		 * Create a commission based on rules defined and get result inside order line 
		 * it is only running if exists a flag for document type named (Calculate commission for Order)
		 * @param invoice
		 * @param
		 */
		public static void createCommissionForInvoice(MInvoice invoice, int commissionTypeId, boolean splitDocuments) {
			if(MOrgInfo.get(invoice.getCtx(), invoice.getAD_Org_ID(), invoice.get_TrxName()).get_ValueAsBoolean("IsExcludeOfCommission")) {
				return;
			}
			removeLineFromCommission(invoice, commissionTypeId);

			List<MCommissionRun> currentCRuns = new ArrayList<>();

			AtomicReference<BigDecimal> reverseAmount = new AtomicReference<BigDecimal>(Env.ZERO);
			new Query(invoice.getCtx(), I_C_Commission.Table_Name, I_C_CommissionType.COLUMNNAME_C_CommissionType_ID + " = ? "
					+ "AND IsSplitDocuments = ?", invoice.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(commissionTypeId, (splitDocuments? "Y": "N"))
			.<MCommission>list().forEach(commissionDefinition -> {
				int documentTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_SalesCommission, invoice.getAD_Org_ID());
				MCommissionRun commissionRun = new MCommissionRun(commissionDefinition);
				commissionRun.setDateDoc(invoice.getDateInvoiced());
				commissionRun.setC_DocType_ID(documentTypeId);
				commissionRun.setDescription(Msg.parseTranslation(invoice.getCtx(), "@Generate@: @C_Order_ID@ - " + invoice.getDocumentNo()));
				if(invoice.getC_Order_ID() > 0) {
					commissionRun.set_ValueOfColumn("C_Order_ID", invoice.getC_Order_ID());
				}
				commissionRun.set_ValueOfColumn("C_Invoice_ID", invoice.getC_Invoice_ID());
				commissionRun.set_ValueOfColumn("C_Currency_ID", invoice.getC_Currency_ID());
				commissionRun.setAD_Org_ID(invoice.getAD_Org_ID());
				commissionRun.saveEx();
				//	Process commission
				//	TODO: Implement It
//				commissionRun.addFilterValues("C_Invoice_ID", invoice.getC_Invoice_ID());

				// Openup Solutions - #14737 - Raul Capecce - 18/09/2020
				// Además de referenciar la factura, se revisa si hay Notas de Corrección para agregar al Calculo de Comision
				if (!invoice.isSOTrx()) {
					List<MInvoice> refInvoices = getReferencedCorrectionNotes(invoice);
					for (MInvoice refInvoice : refInvoices) {
						//	TODO: Implement It
//						commissionRun.addArrayFilterValues("C_Invoice_ID", refInvoice.get_ID());
					}
				}

				commissionRun.setDocStatus(MCommissionRun.DOCSTATUS_Drafted);
				//	Complete
				if(commissionRun.processIt(MCommissionRun.DOCACTION_Complete)) {
					commissionRun.updateFromAmt();
					commissionRun.saveEx();
					if(commissionRun.getGrandTotal() != null
							&& commissionRun.getGrandTotal().compareTo(Env.ZERO) > 0) {
						if(commissionDefinition.get_ValueAsBoolean("IsSplitDocuments")) {
							ProcessBuilder.create(invoice.getCtx())
							.process(CommissionOrderCreateAbstract.getProcessId())
							.withRecordId(I_C_CommissionRun.Table_ID, commissionRun.getC_CommissionRun_ID())
							.withParameter(CommissionOrderCreateAbstract.ISSOTRX, true)
							.withParameter(CommissionOrderCreateAbstract.DATEORDERED, invoice.getDateInvoiced())
							.withParameter(CommissionOrderCreateAbstract.DOCACTION, DocAction.ACTION_Complete)
							.withParameter(CommissionOrderCreateAbstract.C_BPARTNER_ID, invoice.getC_BPartner_ID())
							.withParameter(CommissionOrderCreateAbstract.C_DOCTYPE_ID, commissionDefinition.get_ValueAsInt("C_DocTypeOrder_ID"))
							.withoutTransactionClose()
							.execute(invoice.get_TrxName());
							reverseAmount.updateAndGet(reverse -> reverse.add(commissionRun.getGrandTotal()));
						} else {
							commissionRun.getCommissionAmtList().stream()
							.filter(commissionAmt -> commissionAmt.getCommissionAmt() != null 
							&& commissionAmt.getCommissionAmt().compareTo(Env.ZERO) > 0).forEach(commissionAmt -> {
								MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
								invoiceLine.setC_Charge_ID(commissionDefinition.getC_Charge_ID());
								invoiceLine.setQty(Env.ONE);
								invoiceLine.setPrice(commissionAmt.getCommissionAmt());
								invoiceLine.setTax();
								invoiceLine.saveEx(invoice.get_TrxName());
							});
						}
					}
				} else {
					throw new AdempiereException(commissionRun.getProcessMsg());
				}
				currentCRuns.add(commissionRun);
			});
			//	Reverse previous commission
			reversePreviousCommissionOrders(invoice, reverseAmount.get().negate());
			closePreviousInvoiceCRun(invoice, currentCRuns);
		}

		/**
		 * Openup Solutions - #14667
		 * Cierra los cálculos de comisión previos al que se está generando actualmente
		 * @author Raul Capecce
		 * @param invoice Factura que tiene los Cálculos de Comisión a cerrar
		 * @param currentCRuns Los CRun recién creados que no deben ser cerrados
		 */
		private static void closePreviousInvoiceCRun(MInvoice invoice, List<MCommissionRun> currentCRuns) {
			try {
				String sqlPrevCRunInv = "docStatus<>'CL' AND C_Invoice_ID=? AND C_Order_ID=?";
				List<Object> params = new ArrayList<>();
				params.add(invoice.get_ID());
				params.add(invoice.getC_Order_ID());

				if (!currentCRuns.isEmpty()) {
					sqlPrevCRunInv += " AND C_CommissionRun_ID NOT IN (";
					sqlPrevCRunInv += currentCRuns.stream().map(mCommissionRun -> String.valueOf(mCommissionRun.get_ID())).collect(Collectors.joining(","));
					sqlPrevCRunInv += " )";
				}

				List<MCommissionRun> prevCRuns = new Query(invoice.getCtx(), I_C_CommissionRun.Table_Name, sqlPrevCRunInv, invoice.get_TrxName())
					.setParameters(params)
					.list();
				for (MCommissionRun prevCRun : prevCRuns) {
					prevCRun.processIt(DocAction.ACTION_Close);
					prevCRun.saveEx();
				}
			} catch (Exception e) {

			}
		}

		/**
		 * Remove Line From Commission
		 * @param order
		 */
		private static void removeLineFromCommission(MOrder order, int commissionTypeId) {
			String whereClause = " AND EXISTS(SELECT 1 FROM C_Commission c WHERE c.C_CommissionType_ID = " + commissionTypeId 
					+ " AND c.C_Charge_ID = C_OrderLine.C_Charge_ID)";
			for(MOrderLine line : order.getLines(whereClause, "")) {
				line.deleteEx(true);
			}
		}

		/**
		 * Remove Line From Commission
		 * @param invoice
		 */
		private static void removeLineFromCommission(MInvoice invoice, int commissionTypeId) {
			String whereClause = "EXISTS(SELECT 1 FROM C_Commission c WHERE c.C_CommissionType_ID = " + commissionTypeId 
					+ " AND c.C_Charge_ID = C_InvoiceLine.C_Charge_ID)";
			List<MInvoiceLine> invoiceLineList = new Query(invoice.getCtx(), MInvoiceLine.Table_Name, whereClause, invoice.get_TrxName())
					.<MInvoiceLine>list();
			for(MInvoiceLine line : invoiceLineList) {
				line.deleteEx(true);
			}
		}

	/**
	 * Obtiene todas las notas de Corrección referentes a la factura o ticket mediante Asignaciones Completas
	 * Solo obtendrá registros que sean docBaseType="__C", partiendo de una factura o ticket
	 * @param mInvoice Factura o Ticket (debe ser docBaseType="__I")
	 * @return Notas de Corrección referenciadas
	 */
	public static List<MInvoice> getReferencedCorrectionNotes(MInvoice mInvoice) {
		List<MInvoice> ret = new ArrayList<>();

		try {
			if (mInvoice.getC_DocType().getDocBaseType().endsWith("I")) {
				List<MAllocationHdr> mAllocationHdrs = Arrays
					.stream(MAllocationHdr.getOfInvoice(mInvoice.getCtx(), mInvoice.get_ID(), mInvoice.get_TrxName()))
					.filter(mAllocationHdr -> mAllocationHdr.getDocStatus().equalsIgnoreCase("CO"))
					.collect(Collectors.toList());

				for (MAllocationHdr mAllocationHdr : mAllocationHdrs) {
					MAllocationLine[] mAllocationLines = mAllocationHdr.getLines(true);
					for (MAllocationLine mAllocationLine : mAllocationLines) {
						MInvoice refInv = mAllocationLine.getInvoice();
						if (refInv.get_ID() != mInvoice.get_ID() && refInv.getC_DocType().getDocBaseType().endsWith("C")) {
							ret.add(refInv);
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return ret;
	}


		@Override
		public int getAD_Client_ID() {
			return clientId;
		}

		@Override
		public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
			return null;
		}
	}	//	AgencyValidator
