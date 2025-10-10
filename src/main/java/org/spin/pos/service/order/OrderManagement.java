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
package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_PaymentMethod;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MPriceList;
import org.compiere.model.MProductPrice;
import org.compiere.model.MTable;
import org.compiere.model.MTax;
import org.compiere.model.MUser;
import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.service.cash.CashUtil;
import org.spin.pos.service.payment.GiftCardManagement;
import org.spin.pos.service.payment.PaymentManagement;
import org.spin.pos.service.pos.AccessManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.value.TimeManager;

import static org.spin.pos.service.payment.GiftCardManagement.createGiftCard;
import static org.spin.pos.service.payment.GiftCardManagement.createGiftCardReference;

/**
 * A util class for change values for documents
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class OrderManagement {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(OrderManagement.class);


	/**
	 * Configure Warehouse after change
	 * @param order
	 */
	public static void configureWarehouse(MOrder order) {
		Arrays.asList(order.getLines())
			.forEach(orderLine -> {
				orderLine.setM_Warehouse_ID(order.getM_Warehouse_ID());
				orderLine.saveEx();
			})
		;
	}


	/**
	 * Configure Price List after change
	 * @param order
	 */
	public static void configurePriceList(MOrder order) {
		Arrays.asList(order.getLines())
			.stream()
			.filter(orderLine -> {
				// omit charges
				return orderLine.getM_Product_ID() > 0;
			})
			.forEach(orderLine -> {
				MProductPrice productPrice = MProductPrice.get(
					orderLine.getCtx(),
					order.getM_PriceList_ID(),
					orderLine.getM_Product_ID(),
					order.get_TrxName()
				);
				if (productPrice == null || productPrice.getM_ProductPrice_ID() <= 0) {
					log.warning(
						"Sales Order: " + order.getDocumentNo()
						+ " Line " + orderLine.getLine() + " - " + orderLine.getM_Product().getName()
						+ " Product " + orderLine.getProduct().getValue() + " - " + orderLine.getM_Product().getName()
						+ " No in Price List " + order.getM_PriceList().getName()
					);
					orderLine.deleteEx(true);
				} else {
					orderLine.setPrice();
					orderLine.setTax();
					orderLine.saveEx();
					if(Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO).signum() == 0) {
						/*
						// Set with price list reference
						orderLine.deleteEx(true);
						*/
					}
				}
			})
		;
	}


	/**
	 * 	Set BPartner, update price list and locations
	 *  Configuration of Business Partner has priority over POS configuration
	 *	@param order
	 *	@param businessPartnerId id
	 */
	public static void configureBPartner(MOrder order, int businessPartnerId, String transactionName) {
		//	Valid if has a Order
		if(DocumentUtil.isCompleted(order)
				|| DocumentUtil.isVoided(order))
			return;
		log.fine( "CPOS.setC_BPartner_ID=" + businessPartnerId);
		boolean isSamePOSPartner = false;
		// TODO: Validate order.getC_POS_ID > 0 and pos != null
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), null);
		//	Validate BPartner
		if (businessPartnerId == 0) {
			isSamePOSPartner = true;
			businessPartnerId = pos.getC_BPartnerCashTrx_ID();
		}
		//	Get BPartner
		MBPartner partner = MBPartner.get(Env.getCtx(), businessPartnerId);
		partner.set_TrxName(null);
		if (partner == null || partner.get_ID() == 0) {
			throw new AdempiereException("POS.NoBPartnerForOrder");
		} else {
			log.info("CPOS.SetC_BPartner_ID -" + partner);
			order.setBPartner(partner);
			AtomicBoolean priceListIsChanged = new AtomicBoolean(false);
			if(partner.getM_PriceList_ID() > 0 && pos.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsKeepPriceFromCustomer)) {
				MPriceList businesPartnerPriceList = MPriceList.get(Env.getCtx(), partner.getM_PriceList_ID(), null);
				MPriceList currentPriceList = MPriceList.get(Env.getCtx(), pos.getM_PriceList_ID(), null);
				if(currentPriceList.getC_Currency_ID() != businesPartnerPriceList.getC_Currency_ID()) {
					order.setM_PriceList_ID(currentPriceList.getM_PriceList_ID());
				} else if(currentPriceList.getM_PriceList_ID() != partner.getM_PriceList_ID()) {
					priceListIsChanged.set(true);
				}
			} else {
				order.setM_PriceList_ID(pos.getM_PriceList_ID());
			}
			//	
			MBPartnerLocation [] partnerLocations = partner.getLocations(true);
			if(partnerLocations.length > 0) {
				for(MBPartnerLocation partnerLocation : partnerLocations) {
					if(partnerLocation.isBillTo())
						order.setBill_Location_ID(partnerLocation.getC_BPartner_Location_ID());
					if(partnerLocation.isShipTo())
						order.setShip_Location_ID(partnerLocation.getC_BPartner_Location_ID());
				}				
			}
			//	Validate Same BPartner
			if(isSamePOSPartner) {
				if(order.getPaymentRule() == null)
					order.setPaymentRule(MOrder.PAYMENTRULE_Cash);
			}
			//	Set Sales Representative
			if (order.getC_BPartner().getSalesRep_ID() != 0) {
				order.setSalesRep_ID(order.getC_BPartner().getSalesRep_ID());
			} else {
				order.setSalesRep_ID(Env.getAD_User_ID(Env.getCtx()));
			}
			//	Save Header
			order.saveEx(transactionName);
			//	Load Price List Version
			Arrays.asList(order.getLines(true, "Line"))
			.forEach(orderLine -> {
				orderLine.setC_BPartner_ID(partner.getC_BPartner_ID());
				orderLine.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
				orderLine.setPrice();
				orderLine.setTax();
				orderLine.saveEx(transactionName);
				if(Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO).signum() == 0
						&& priceListIsChanged.get()) {
					orderLine.saveEx(transactionName);
				}
			});
		}
		//	Change for payments
		MPayment.getOfOrder(order).forEach(payment -> {
			if(DocumentUtil.isCompleted(payment)
					|| DocumentUtil.isVoided(payment)) {
				throw new AdempiereException("@C_Payment_ID@ @Processed@ " + payment.getDocumentNo());
			}
			//	Change Business Partner
			payment.setC_BPartner_ID(order.getC_BPartner_ID());
			payment.saveEx(transactionName);
		});
	}


	public static MOrder processOrder(MPOS pos, int orderId, boolean isRefundOpen) {
		if(orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder salesOrder = OrderUtil.validateAndGetOrder(orderId, transactionName);
			final int userId = Env.getAD_User_ID(pos.getCtx());
			if (salesOrder.get_ValueAsBoolean("IsManualDocument")) {
				boolean isAllowsManualDocument = AccessManagement.getBooleanValueFromPOS(pos, userId, "IsAllowsCreateManualDocuments");
				if (!isAllowsManualDocument) {
					throw new AdempiereException("@ActionNotAllowedHere@: @IsAllowsCreateManualDocuments@");
				}
			}

			// Verify online payments
			int onlinePaymentsWithoutApproved = PaymentManagement.isOrderWithoutOnlinePaymentApproved(
				salesOrder.getC_Order_ID()
			);
			if (onlinePaymentsWithoutApproved > 0) {
				throw new AdempiereException("@PaymentFormController: PaymentNotProcessed@");
			}

			CashManagement.validatePreviousCashClosing(pos, salesOrder.getDateOrdered(), transactionName);
			CashManagement.getCurrentCashClosing(pos, salesOrder.getDateOrdered(), true, transactionName);

			List<PO> paymentReferences = getPaymentReferences(salesOrder);
			if(!OrderUtil.isValidOrder(salesOrder)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			boolean isOpenToRefund = isRefundOpen;
			BigDecimal paymentReferenceAmount = getPaymentReferenceAmount(salesOrder, paymentReferences);
			if(paymentReferenceAmount.compareTo(Env.ZERO) != 0) {
				isOpenToRefund = true;
			}
			if(DocumentUtil.isDrafted(salesOrder)) {
				// In case the Order is Invalid, set to In Progress; otherwise it will not be completed
				if (salesOrder.getDocStatus().equalsIgnoreCase(MOrder.STATUS_Invalid))  {
					salesOrder.setDocStatus(MOrder.STATUS_InProgress);
				}
				//	Set default values
				salesOrder.setDocAction(DocAction.ACTION_Complete);
				OrderUtil.setCurrentDate(salesOrder);
				salesOrder.saveEx();
				//	Update Process if exists
				if (!salesOrder.processIt(MOrder.DOCACTION_Complete)) {
					throw new AdempiereException("@ProcessFailed@ :" + salesOrder.getProcessMsg());
				}
				//	Release Order
				int invoiceId = salesOrder.getC_Invoice_ID();
				if(invoiceId > 0) {
					salesOrder.setIsInvoiced(true);
				}
				salesOrder.set_ValueOfColumn("AssignedSalesRep_ID", null);
				salesOrder.saveEx();
			}

			processPayments(salesOrder, pos, isOpenToRefund, transactionName);
			//	Process all references
			processPaymentReferences(salesOrder, pos, paymentReferences, transactionName);
			//	Create
			orderReference.set(salesOrder);
		});
		return orderReference.get();
	}
	
	public static MOrder createOrderFromOther(int posId, int salesRepresentativeId, int sourceOrderId) {
		if(posId <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if(sourceOrderId <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		MPOS pos = new MPOS(Env.getCtx(), posId, null);
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder sourceOrder = new MOrder(Env.getCtx(), sourceOrderId, transactionName);
			if(DocumentUtil.isClosed(sourceOrder)
					|| DocumentUtil.isVoided(sourceOrder)
					|| sourceOrder.isReturnOrder()) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			MOrder targetOrder = OrderUtil.copyOrder(pos, sourceOrder, transactionName);
			MBPartner businessPartner = (MBPartner) targetOrder.getC_BPartner();
			OrderUtil.setCurrentDate(targetOrder);
			int salesRepId = salesRepresentativeId;
			MUser currentUser = MUser.get(Env.getCtx());
			if (pos.get_ValueAsBoolean("IsSharedPOS")) {
				salesRepId = currentUser.getAD_User_ID();
			} else if (businessPartner.getSalesRep_ID() != 0) {
				salesRepId = businessPartner.getSalesRep_ID();
			} else {
				salesRepId = pos.getSalesRep_ID();
			}
			if(salesRepId > 0) {
				targetOrder.setSalesRep_ID(salesRepId);
			}
			targetOrder.set_ValueOfColumn("AssignedSalesRep_ID", currentUser.getAD_User_ID());
			targetOrder.saveEx();
			OrderUtil.copyOrderLinesFromOrder(sourceOrder, targetOrder, transactionName);
			orderReference.set(targetOrder);
		});
		return orderReference.get();
	}
	
	public static MOrder createOrderFromRMA(int posId, int salesRepresentativeId, int sourceOrderId) {
		if(posId <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if(sourceOrderId <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		MPOS pos = new MPOS(Env.getCtx(), posId, null);
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		MOrder returnOrder = new Query(Env.getCtx(), I_C_Order.Table_Name, 
				"ECA14_Source_RMA_ID = ? ", null)
				.setParameters(sourceOrderId)
				.first();
		if(returnOrder != null) {
			return returnOrder;
		}
		Trx.run(transactionName -> {
			MOrder sourceReturnOrder = new MOrder(Env.getCtx(), sourceOrderId, transactionName);
			if(!DocumentUtil.isClosed(sourceReturnOrder)
					|| DocumentUtil.isVoided(sourceReturnOrder)
					|| !sourceReturnOrder.isReturnOrder()) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			MOrder targetOrder = OrderUtil.copyOrder(pos, sourceReturnOrder, transactionName);
			setDefaultValuesFromPOS(pos, targetOrder);
			MBPartner businessPartner = (MBPartner) targetOrder.getC_BPartner();
			OrderUtil.setCurrentDate(targetOrder);
			int salesRepId = salesRepresentativeId;
			MUser currentUser = MUser.get(Env.getCtx());
			if (pos.get_ValueAsBoolean("IsSharedPOS")) {
				salesRepId = currentUser.getAD_User_ID();
			} else if (businessPartner.getSalesRep_ID() != 0) {
				salesRepId = businessPartner.getSalesRep_ID();
			} else {
				salesRepId = pos.getSalesRep_ID();
			}
			if(salesRepId > 0) {
				targetOrder.setSalesRep_ID(salesRepId);
			}
			targetOrder.set_ValueOfColumn("AssignedSalesRep_ID", currentUser.getAD_User_ID());
			//	Set reference
			targetOrder.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_Source_RMA_ID, sourceReturnOrder.getC_Order_ID());
			targetOrder.saveEx();
			OrderUtil.copyOrderLinesFromRMA(sourceReturnOrder, targetOrder, transactionName);
			//	Add Credit Memo as Payment
			int creditMemoId = sourceReturnOrder.getC_Invoice_ID();
			if(creditMemoId > 0) {
				createPaymentFromCreditMemo(targetOrder, pos, creditMemoId, transactionName);
			}
			orderReference.set(targetOrder);
		});
		return orderReference.get();
	}

	private static void createPaymentFromCreditMemo(MOrder salesOrder, MPOS pos, int creditMemoId, String transactionName) {
		MInvoice creditMemo = new MInvoice(salesOrder.getCtx(), creditMemoId, transactionName);
		BigDecimal paymentAmount = salesOrder.getGrandTotal().min(salesOrder.getGrandTotal());
		int currencyId = salesOrder.getC_Currency_ID();
		MPayment payment = new MPayment(Env.getCtx(), 0, transactionName);
		payment.setC_BankAccount_ID(pos.getC_BankAccount_ID());
		PO paymentTypeAllocation = POS.getPaymentMethodAllocationFromTenderType(pos.getC_POS_ID(), MPayment.TENDERTYPE_CreditMemo);
		if (paymentTypeAllocation == null) {
			throw new AdempiereException("@C_POSPaymentTypeAllocation_ID@ @NotFound@");
		}
		//	Payment Method
		int paymentMethodId = paymentTypeAllocation.get_ValueAsInt(I_C_PaymentMethod.COLUMNNAME_C_PaymentMethod_ID);
		payment.setC_PaymentMethod_ID(paymentMethodId);
		//	Document Type
		String documentTypeColumnName = "POSCollectingDocumentType_ID";
		int documentTypeId = pos.get_ValueAsInt(documentTypeColumnName);
		if(paymentTypeAllocation.get_ValueAsInt("C_DocTypeTarget_ID") > 0) {
			documentTypeId = paymentTypeAllocation.get_ValueAsInt("C_DocTypeTarget_ID");
		}
		if(documentTypeId > 0) {
			payment.setC_DocType_ID(documentTypeId);
		} else {
			payment.setC_DocType_ID(true);
		}
		payment.setAD_Org_ID(salesOrder.getAD_Org_ID());
		payment.setDateTrx(salesOrder.getDateOrdered());
		payment.setDateAcct(salesOrder.getDateOrdered());
		payment.setDateAcct(salesOrder.getDateAcct());
		payment.setTenderType(MPayment.TENDERTYPE_CreditMemo);
		payment.setDescription(salesOrder.getDescription());
		payment.setC_BPartner_ID (salesOrder.getC_BPartner_ID());
		payment.setC_Currency_ID(currencyId);
		payment.setC_POS_ID(pos.getC_POS_ID());
		if(salesOrder.getSalesRep_ID() > 0) {
			payment.set_ValueOfColumn("CollectingAgent_ID", salesOrder.getSalesRep_ID());
		}
		if(pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID) > 0) {
			payment.setC_ConversionType_ID(pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID));
		}
		payment.setPayAmt(paymentAmount);
		//	Order Reference
		payment.setC_Order_ID(salesOrder.getC_Order_ID());
		payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		payment.setR_PnRef(creditMemo.getDocumentNo());
		payment.setDocumentNo(creditMemo.getDocumentNo());
		payment.setCheckNo(creditMemo.getDocumentNo());
		payment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_Invoice_Reference_ID, creditMemoId);
		CashUtil.setCurrentDate(payment);
		payment.saveEx(transactionName);
	}
	
	private static void setDefaultValuesFromPOS(MPOS pos, MOrder salesOrder) {
		salesOrder.setM_PriceList_ID(pos.getM_PriceList_ID());
		salesOrder.setM_Warehouse_ID(pos.getM_Warehouse_ID());
		//	Document Type
		int documentTypeId = pos.getC_DocType_ID();
		//	Validate
		if(documentTypeId > 0) {
			salesOrder.setC_DocTypeTarget_ID(documentTypeId);
		} else {
			salesOrder.setC_DocTypeTarget_ID(MOrder.DocSubTypeSO_POS);
		}
		//	Delivery Rules
		if (pos.getDeliveryRule() != null) {
			salesOrder.setDeliveryRule(pos.getDeliveryRule());
		}
		//	Invoice Rule
		if (pos.getInvoiceRule() != null) {
			salesOrder.setInvoiceRule(pos.getInvoiceRule());
		}
		//	Conversion Type
		if(pos.get_ValueAsInt(MOrder.COLUMNNAME_C_ConversionType_ID) > 0) {
			salesOrder.setC_ConversionType_ID(pos.get_ValueAsInt(MOrder.COLUMNNAME_C_ConversionType_ID));
		}
	}
	
	/**
	 * Process Payment references
	 * @param salesOrder
	 * @param pos
	 * @param paymentReferences
	 * @param transactionName
	 */
	private static void processPaymentReferences(MOrder salesOrder, MPOS pos, List<PO> paymentReferences, String transactionName) {
		paymentReferences.stream().filter(paymentReference -> {
			PO paymentMethodAlocation = POS.getPaymentMethodAllocation(paymentReference.get_ValueAsInt("C_PaymentMethod_ID"), paymentReference.get_ValueAsInt("C_POS_ID"), paymentReference.get_TrxName());
			if(paymentMethodAlocation == null) {
				return false;
			}
			return paymentMethodAlocation.get_ValueAsBoolean("IsPaymentReference") && !paymentReference.get_ValueAsBoolean("IsAutoCreatedReference");
		}).forEach(paymentReference -> {
			paymentReference.set_ValueOfColumn("Processed", true);
			if ("G".equals(paymentReference.get_ValueAsString("TenderType"))) {
				if (!paymentReference.get_ValueAsBoolean("IsReceipt")) {
					PO giftCard = GiftCardManagement.createGiftCardFromPaymentReference(
						salesOrder,
						paymentReference,
						transactionName
					);
					if (giftCard != null && giftCard.get_ID() > 0) {
						paymentReference.set_ValueOfColumn(
							ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID,
							giftCard.get_ID()
						);
					}
				} else {
					final int giftCardId = paymentReference.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID);
					GiftCardManagement.processGiftCard(
						giftCardId,
						null,
						paymentReference,
						transactionName
					);
				}
			}
			paymentReference.saveEx();
		});
	}


	/**
	 * Get Refund references from order
	 * @param order
	 * @return
	 * @return List<PO>
	 */
	private static List<PO> getPaymentReferences(MOrder order) {
		if(MTable.get(Env.getCtx(), "C_POSPaymentReference") == null) {
			return new ArrayList<PO>();
		}
		//	
		return new Query(order.getCtx(), "C_POSPaymentReference", "C_Order_ID = ? AND IsPaid = 'N' AND Processed = 'N'", order.get_TrxName()).setParameters(order.getC_Order_ID()).list();
	}
	
	/**
	 * Get Refund Reference Amount
	 * @param order
	 * @return
	 * @return BigDecimal
	 */
	private static BigDecimal getPaymentReferenceAmount(MOrder order, List<PO> paymentReferences) {
		Optional<BigDecimal> paymentReferenceAmount = paymentReferences.stream().map(refundReference -> {
			return OrderUtil.getConvertedAmount(order, refundReference, ((BigDecimal) refundReference.get_Value("Amount")));
		}).reduce(BigDecimal::add);
        return paymentReferenceAmount.orElse(Env.ZERO);
    }
	
	private static void createCreditMemoReference(MOrder salesOrder, MPayment payment, String transactionName) {
		if(payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Invoice_Reference_ID) <= 0) {
			return;
		}
		MInvoice creditMemo = new MInvoice(salesOrder.getCtx(), payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Invoice_Reference_ID), transactionName);
		payment.setC_Invoice_ID(-1);
		payment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_Reference_Amount, payment.getPayAmt());
		payment.setPayAmt(Env.ZERO);
		CashUtil.setCurrentDate(payment, true);
		payment.saveEx(transactionName);
		creditMemo.setC_Payment_ID(payment.getC_Payment_ID());
		creditMemo.saveEx(transactionName);
	}
	
	/**
	 * Create Credit Memo from payment
	 * @param salesOrder
	 * @param payment
	 * @param transactionName
	 * @return void
	 */
	private static void createCreditMemo(MOrder salesOrder, MPayment payment, String transactionName) {
		MInvoice creditMemo = new MInvoice(salesOrder.getCtx(), 0, transactionName);
		creditMemo.setBPartner((MBPartner) payment.getC_BPartner());
		creditMemo.setDescription(payment.getDescription());
		creditMemo.setIsSOTrx(true);
		creditMemo.setSalesRep_ID(payment.get_ValueAsInt("CollectingAgent_ID"));
		creditMemo.setAD_Org_ID(payment.getAD_Org_ID());
		creditMemo.setC_POS_ID(payment.getC_POS_ID());
		if(creditMemo.getM_PriceList_ID() <= 0
				|| creditMemo.getC_Currency_ID() != payment.getC_Currency_ID()) {
			String isoCode = MCurrency.getISO_Code(salesOrder.getCtx(), payment.getC_Currency_ID());
			MPriceList priceList = MPriceList.getDefault(salesOrder.getCtx(), true, isoCode);
			if(priceList == null) {
				throw new AdempiereException("@M_PriceList_ID@ @NotFound@ (@C_Currency_ID@ " + isoCode + ")");
			}
			creditMemo.setM_PriceList_ID(priceList.getM_PriceList_ID());
		}
		PO paymentTypeAllocation = POS.getPaymentMethodAllocation(payment.get_ValueAsInt("C_PaymentMethod_ID"), payment.getC_POS_ID(), payment.get_TrxName());
		int chargeId = 0;
		if(paymentTypeAllocation != null) {
			chargeId = paymentTypeAllocation.get_ValueAsInt("C_Charge_ID");
			if(paymentTypeAllocation.get_ValueAsInt("C_DocTypeCreditMemo_ID") > 0) {
				creditMemo.setC_DocTypeTarget_ID(paymentTypeAllocation.get_ValueAsInt("C_DocTypeCreditMemo_ID"));
			}
		}
		//	Set if not exist
		if(creditMemo.getC_DocTypeTarget_ID() <= 0) {
			creditMemo.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ARCreditMemo);
		}
		creditMemo.setDocumentNo(payment.getDocumentNo());
		creditMemo.setC_ConversionType_ID(payment.getC_ConversionType_ID());
		creditMemo.setC_Order_ID(salesOrder.getC_Order_ID());
		creditMemo.setC_Payment_ID(payment.getC_Payment_ID());
		creditMemo.setDateInvoiced(payment.getDateTrx());
		creditMemo.setDateAcct(payment.getDateAcct());
		OrderUtil.copyAccountDimensions(salesOrder, creditMemo);
		creditMemo.saveEx(transactionName);
		//	Add line
		MInvoiceLine creditMemoLine = new MInvoiceLine(creditMemo);
		if(chargeId <= 0) {
			chargeId = new Query(salesOrder.getCtx(), MCharge.Table_Name, null, transactionName).setClient_ID().firstId();
		}
		MCharge charge = MCharge.get(payment.getCtx(), chargeId);
		Optional<MTax> optionalTax = Arrays.asList(MTax.getAll(Env.getCtx()))
			.parallelStream()
			.filter(tax -> tax.getC_TaxCategory_ID() == charge.getC_TaxCategory_ID() 
									&& (tax.isSalesTax() 
											|| (!Util.isEmpty(tax.getSOPOType()) 
													&& (tax.getSOPOType().equals(MTax.SOPOTYPE_Both) 
															|| tax.getSOPOType().equals(MTax.SOPOTYPE_SalesTax)))))
			.findFirst()
		;
		creditMemoLine.setC_Charge_ID(chargeId);
		creditMemoLine.setC_Tax_ID(optionalTax.get().getC_Tax_ID());
		creditMemoLine.setQty(Env.ONE);
		creditMemoLine.setPrice(payment.getPayAmt());
		creditMemoLine.setDescription(payment.getDescription());
		creditMemoLine.saveEx(transactionName);
		//	change payment
		payment.setC_Invoice_ID(-1);
		payment.setPayAmt(Env.ZERO);
		CashUtil.setCurrentDate(payment, true);
		payment.saveEx(transactionName);
		//	Process credit Memo
		if (!creditMemo.processIt(MInvoice.DOCACTION_Complete)) {
			throw new AdempiereException("@ProcessFailed@ :" + creditMemo.getProcessMsg());
		}
		creditMemo.setDocumentNo(payment.getDocumentNo());
		creditMemo.saveEx(transactionName);
	}


	/**
	 * Validate if a order is released
	 * @param salesOrder
	 * @return void
	 */
	public static void validateOrderReleased(MOrder salesOrder) {
		if(salesOrder.get_ValueAsInt("AssignedSalesRep_ID") > 0
				&& salesOrder.get_ValueAsInt("AssignedSalesRep_ID") != Env.getAD_User_ID(Env.getCtx())) {
			throw new AdempiereException("@POS.SalesRepAssigned@");
		}
	}
	
	/**
	 * Process payment of Order
	 * @param salesOrder
	 * @param pos
	 * @param isOpenRefund
	 * @param transactionName
	 * @return void
	 */
	public static void processPayments(MOrder salesOrder, MPOS pos, boolean isOpenRefund, String transactionName) {
		//	Get invoice if exists
		int invoiceId = salesOrder.getC_Invoice_ID();
		AtomicReference<BigDecimal> openAmount = new AtomicReference<BigDecimal>(OrderUtil.getTotalOpenAmount(salesOrder));
		AtomicReference<BigDecimal> currentAmount = new AtomicReference<BigDecimal>(salesOrder.getGrandTotal());
		List<Integer> paymentsIds = new ArrayList<Integer>();
		//	Complete Payments
		List<MPayment> payments = MPayment.getOfOrder(salesOrder);
		payments.stream().sorted(Comparator.comparing(MPayment::getCreated)).forEach(payment -> {
			if (payment.isOnline() && (Util.isEmpty(payment.get_ValueAsString("ResponseStatus"), true)
				|| !payment.get_ValueAsString("ResponseStatus").equals("A"))) {
				String errorMessage = "<" + payment.getDocumentNo() + "> @IsPaymentVerificationRequired@ .";
				if (!Util.isEmpty(payment.get_ValueAsString("ResponseMessage"))) {
					errorMessage += "@ResponseMessage@ : " + payment.get_ValueAsString("ResponseMessage") + ". ";
				}
				if (!Util.isEmpty(payment.get_ValueAsString("ResponseCode"))) {
					errorMessage += "@ResponseCode@ : " + payment.get_ValueAsString("ResponseCode") + ". ";
				}
				throw new AdempiereException(errorMessage);
			}
			BigDecimal convertedAmount = OrderUtil.getConvertedAmount(salesOrder, payment, payment.getPayAmt());
			//	Get current open amount
			AtomicReference<BigDecimal> multiplier = new AtomicReference<BigDecimal>(!salesOrder.isReturnOrder()? Env.ONE: Env.ONE.negate());
			if(!payment.isReceipt()) {
				multiplier.updateAndGet(BigDecimal::negate);
			}
			openAmount.updateAndGet(amount -> amount.subtract(convertedAmount.multiply(multiplier.get())));
			if(payment.isAllocated()) {
				currentAmount.updateAndGet(amount -> amount.subtract(convertedAmount.multiply(multiplier.get())));
			}
			if(DocumentUtil.isDrafted(payment)) {
				payment.setIsPrepayment(true);
				payment.setOverUnderAmt(Env.ZERO);
				if(payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
					if(payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Invoice_Reference_ID) <= 0) {
						createCreditMemo(salesOrder, payment, transactionName);
					} else {
						createCreditMemoReference(salesOrder, payment, transactionName);
					}
				} else if (payment.getTenderType().equals("G")) {
					if(!payment.isReceipt()) {
						createGiftCard(salesOrder, payment, transactionName);
					} else {
						createGiftCardReference(salesOrder, payment, transactionName);
					}
				}
				payment.setDocAction(MPayment.DOCACTION_Complete);
				CashUtil.setCurrentDate(payment);
				payment.saveEx(transactionName);
				if (!payment.processIt(MPayment.DOCACTION_Complete)) {
					throw new AdempiereException("@ProcessFailed@ :" + payment.getProcessMsg());
				}
				payment.saveEx(transactionName);
				paymentsIds.add(payment.getC_Payment_ID());
				salesOrder.saveEx(transactionName);
			}
			CashManagement.addPaymentToCash(pos, payment);
		});

		List<PO> paymentReferencesList = getPaymentReferences(salesOrder);
		//	Allocate all payments
		if(!paymentsIds.isEmpty() || !paymentReferencesList.isEmpty()) {
			String description = Msg.parseTranslation(Env.getCtx(), "@C_POS_ID@: " + pos.getName() + " - " + salesOrder.getDocumentNo());
			//	
			MAllocationHdr paymentAllocation = new MAllocationHdr (Env.getCtx(), true, TimeManager.getDate(), salesOrder.getC_Currency_ID(), description, transactionName);
			paymentAllocation.setAD_Org_ID(salesOrder.getAD_Org_ID());
			//	Set Description
			paymentAllocation.saveEx();
			//	Add lines
			paymentsIds.stream()
				.map(paymentId -> {
					return new MPayment(Env.getCtx(), paymentId, transactionName);
				})
				.filter(payment -> {
					return payment.getC_POS_ID() != 0;
				})
				.forEach(payment -> {
					createAllocationLine(pos, salesOrder, invoiceId, paymentAllocation, payment);
				})
			;

			// Create RMA if payment with gift card
			GiftCardManagement.createReturnSalesOrder(
				salesOrder,
				paymentsIds,
				transactionName
			);

			//	Add write off
			if(!isOpenRefund
					|| OrderUtil.isAutoWriteOff(salesOrder, openAmount.get())) {
				if(openAmount.get().compareTo(Env.ZERO) != 0) {
					MAllocationLine paymentAllocationLine = new MAllocationLine (paymentAllocation, Env.ZERO, Env.ZERO, openAmount.get(), Env.ZERO);
					paymentAllocationLine.setDocInfo(salesOrder.getC_BPartner_ID(), salesOrder.getC_Order_ID(), invoiceId);
					paymentAllocationLine.saveEx();
				}
			}

			//	Complete
			if (!paymentAllocation.processIt(MAllocationHdr.DOCACTION_Complete)) {
				throw new AdempiereException(paymentAllocation.getProcessMsg());
			}
			paymentAllocation.saveEx();
			//	Test allocation
			paymentsIds.stream()
				.map(paymentId -> {
					return new MPayment(Env.getCtx(), paymentId, transactionName);
				})
				.forEach(payment -> {
					payment.setIsAllocated(true);
					payment.setC_Invoice_ID(invoiceId);
					payment.saveEx();
				})
			;
		} else {
			//	Add write off
			if(!isOpenRefund) {
				if(openAmount.get().compareTo(Env.ZERO) != 0) {
					String description = Msg.parseTranslation(Env.getCtx(), "@C_POS_ID@: " + pos.getName() + " - " + salesOrder.getDocumentNo());
					MAllocationHdr paymentAllocation = new MAllocationHdr (Env.getCtx(), true, TimeManager.getDate(), salesOrder.getC_Currency_ID(), description, transactionName);
					paymentAllocation.setAD_Org_ID(salesOrder.getAD_Org_ID());
					//	Set Description
					paymentAllocation.saveEx();
					MAllocationLine paymentAllocationLine = new MAllocationLine (paymentAllocation, Env.ZERO, Env.ZERO, openAmount.get(), Env.ZERO);
					paymentAllocationLine.setDocInfo(salesOrder.getC_BPartner_ID(), salesOrder.getC_Order_ID(), invoiceId);
					paymentAllocationLine.saveEx();
					//	Complete
					if (!paymentAllocation.processIt(MAllocationHdr.DOCACTION_Complete)) {
						throw new AdempiereException(paymentAllocation.getProcessMsg());
					}
					paymentAllocation.saveEx();
				}
			}
		}
	}

	private static boolean isOnlyReference(MPayment payment) {
		return payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo) || payment.getTenderType().equals("G");
	}

	private static BigDecimal getPaymentAmount(MOrder salesOrder, MPayment payment) {
		if(isOnlyReference(payment)) {
			return Optional.ofNullable((BigDecimal) payment.get_Value(ColumnsAdded.COLUMNNAME_ECA14_Reference_Amount)).orElse(Env.ZERO);
		}
		return OrderUtil.getConvertedAmount(salesOrder, payment, payment.getPayAmt());
	}

	private static BigDecimal getDiscountAmount(MOrder salesOrder, MPayment payment) {
		if(isOnlyReference(payment)) {
			return Env.ZERO;
		}
		return OrderUtil.getConvertedAmount(salesOrder, payment, payment.getDiscountAmt());
	}

	private static BigDecimal getOverUnderAmount(MOrder salesOrder, MPayment payment) {
		if(isOnlyReference(payment)) {
			return Env.ZERO;
		}
		return OrderUtil.getConvertedAmount(salesOrder, payment, payment.getOverUnderAmt());
	}

	private static BigDecimal getWriteOffAmount(MOrder salesOrder, MPayment payment) {
		if(isOnlyReference(payment)) {
			return Env.ZERO;
		}
		return OrderUtil.getConvertedAmount(salesOrder, payment, payment.getWriteOffAmt());
	}

	private static void createAllocationLine(MPOS pos, MOrder salesOrder, int invoiceId, MAllocationHdr paymentAllocation, MPayment payment) {
		BigDecimal multiplier = Env.ONE;
		if(!payment.isReceipt()) {
			multiplier = Env.ONE.negate();
		}
		BigDecimal paymentAmount = getPaymentAmount(salesOrder, payment);
		BigDecimal discountAmount = getDiscountAmount(salesOrder, payment);
		BigDecimal overUnderAmount = getOverUnderAmount(salesOrder, payment);
		BigDecimal writeOffAmount = getWriteOffAmount(salesOrder, payment);
		if (overUnderAmount.signum() < 0 && paymentAmount.signum() > 0) {
			paymentAmount = paymentAmount.add(overUnderAmount);
		}
		MAllocationLine paymentAllocationLine = new MAllocationLine (paymentAllocation, paymentAmount.multiply(multiplier), discountAmount.multiply(multiplier), writeOffAmount.multiply(multiplier), overUnderAmount.multiply(multiplier));
		paymentAllocationLine.setDocInfo(salesOrder.getC_BPartner_ID(), salesOrder.getC_Order_ID(), invoiceId);
		if(!isOnlyReference(payment)) {
			paymentAllocationLine.setPaymentInfo(payment.getC_Payment_ID(), 0);
		}
		paymentAllocationLine.saveEx();
		//	Credit Memo / Gift Card
		if(payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
			multiplier = multiplier.negate();
			paymentAllocationLine = new MAllocationLine (paymentAllocation, paymentAmount.multiply(multiplier), discountAmount.multiply(multiplier), writeOffAmount.multiply(multiplier), overUnderAmount.multiply(multiplier));
			paymentAllocationLine.setDocInfo(salesOrder.getC_BPartner_ID(), salesOrder.getC_Order_ID(), payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Invoice_Reference_ID));
			paymentAllocationLine.saveEx();
		} else if(payment.getTenderType().equals("G")) {
			final int defaultGiftCardChargeId = pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_DefaultGiftCardCharge_ID);
			paymentAllocationLine = new MAllocationLine (paymentAllocation, paymentAmount.multiply(multiplier), Env.ZERO, Env.ZERO, Env.ZERO);
			paymentAllocationLine.setDocInfo(salesOrder.getC_BPartner_ID(), salesOrder.getC_Order_ID(), 0);
			paymentAllocationLine.setC_Charge_ID(defaultGiftCardChargeId);
			paymentAllocationLine.saveEx();
		}
	}

	/***
	 * Update order line
	 * @param transactionName
	 * @param orderLineId
	 * @param quantity
	 * @param price
	 * @param discountRate
	 * @param isAddQuantity
	 * @param warehouseId
	 * @return
	 */
	public static MOrderLine updateOrderLine(String transactionName, MPOS pos, int orderLineId, BigDecimal quantity, BigDecimal price, BigDecimal discountRate, boolean isAddQuantity, int warehouseId, int unitOfMeasureId) {
		if(orderLineId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
		}
		MOrderLine orderLine = new MOrderLine(Env.getCtx(), orderLineId, transactionName);
		if (orderLine == null || orderLine.getC_OrderLine_ID() <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ (" + orderLineId + ") @NotFound@");
		}
		if(orderLine.isProcessed()) {
			throw new AdempiereException("@C_OrderLine_ID@ (#" + orderLine.getLine() + ") @Processed@");
		}

		MOrder order = orderLine.getParent();
		order.set_TrxName(transactionName);
		//	Valid Complete
		if (!DocumentUtil.isDrafted(order)) {
			throw new AdempiereException("@C_Order_ID@ (#" + order.getDocumentNo() + ") @Processed@");
		}
		OrderManagement.validateOrderReleased(order);

		OrderUtil.setCurrentDate(order);
		orderLine.setHeaderInfo(order);
		int precision = MPriceList.getPricePrecision(Env.getCtx(), order.getM_PriceList_ID());
		BigDecimal quantityToChange = quantity;
		//	Get if is null
		if(quantity != null) {
			if(isAddQuantity) {
				BigDecimal currentQuantity = orderLine.getQtyEntered();
				if(currentQuantity == null) {
					currentQuantity = Env.ZERO;
				}
				quantityToChange = currentQuantity.add(quantityToChange);
			}
			if(orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID) > 0) {
				// if(warehouseId != orderLine.get_ValueAsInt("Ref_WarehouseSource_ID")
				// 		|| unitOfMeasureId != orderLine.getC_UOM_ID()) {
				// 	throw new AdempiereException("@ActionNotAllowedHere@");
				// }
				MOrderLine sourceRmaLine = new MOrderLine(Env.getCtx(), orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID), transactionName);
				BigDecimal availableQuantity = OrderUtil.getAvailableQuantityForSell(orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID), orderLineId, sourceRmaLine.getQtyEntered(), quantityToChange);
				if(availableQuantity.compareTo(Env.ZERO) > 0) {
					//	Update order quantity
					quantityToChange = availableQuantity;
				} else {
					throw new AdempiereException("@QtyInsufficient@");
				}
			}
			orderLine.setQty(quantityToChange);
		}
		//	Calculate discount from final price
		if(price != null || discountRate != null) {
			if(orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID) > 0) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			// TODO: Verify with Price Entered/Actual
			BigDecimal priceToOrder = orderLine.getPriceActual();
			BigDecimal discountRateToOrder = Env.ZERO;
			if (price != null) {
				priceToOrder = price;
				discountRateToOrder = DiscountManagement.getDiscount(
					orderLine.getPriceList(),
					price,
					precision
				);
			}
			//	Calculate final price from discount
			if (discountRate != null) {
				discountRateToOrder = discountRate;
				priceToOrder = DiscountManagement.getFinalPrice(orderLine.getPriceList(), discountRate, precision);
				priceToOrder = MUOMConversion.convertProductFrom(orderLine.getCtx(), orderLine.getM_Product_ID(), orderLine.getC_UOM_ID(), priceToOrder);
			}
			orderLine.setDiscount(discountRateToOrder);
			orderLine.setPrice(priceToOrder);
		}
		//	
		if(warehouseId > 0) {
			// orderLine.setM_Warehouse_ID(warehouseId);
			orderLine.set_ValueOfColumn("Ref_WarehouseSource_ID", warehouseId);
		}
		//	Validate UOM
		OrderUtil.updateUomAndQuantity(orderLine, unitOfMeasureId, quantityToChange);
		//	Set values
		orderLine.setTax();
		orderLine.saveEx(transactionName);
		//	Apply Discount from order
		DiscountManagement.configureDiscountRateOff(
			order,
			(BigDecimal) order.get_Value("FlatDiscount"),
			transactionName
		);

		return orderLine;
	} // UpdateLine

}
