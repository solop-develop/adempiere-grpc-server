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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.base.util.DocumentUtil;

/**
 * This class was created for return a order by product
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ReturnSalesOrder {
	
	/**
	 * Create a Return order and cancel all payments
	 * @param posId
	 * @param sourceOrderId
	 * @param salesRepresentativeId
	 * @param copyLines
	 * @param description
	 * @return
	 */
	public static MOrder createRMAFromOrder(int posId, int sourceOrderId, int salesRepresentativeId, boolean copyLines, String description) {
		if(posId <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if(sourceOrderId <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		MPOS pos = new MPOS(Env.getCtx(), posId, null);
		AtomicReference<MOrder> returnOrderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder sourceOrder = new MOrder(Env.getCtx(), sourceOrderId, transactionName);
			//	Validate source document
			if(DocumentUtil.isDrafted(sourceOrder) 
					|| DocumentUtil.isClosed(sourceOrder)
					|| sourceOrder.isReturnOrder()
					|| !OrderUtil.isValidOrder(sourceOrder)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			MOrder returnOrder = new Query(Env.getCtx(), I_C_Order.Table_Name, 
					"DocStatus = 'DR' "
					+ "AND Ref_Order_ID = ? ", transactionName)
					.setParameters(sourceOrderId)
					.first();
			if(returnOrder == null) {
				returnOrder = RMAUtil.copyRMAFromOrder(pos, sourceOrder, transactionName);
				if(salesRepresentativeId > 0) {
					returnOrder.setSalesRep_ID(salesRepresentativeId);
				}
			}
			if(!Util.isEmpty(description)) {
				returnOrder.setDescription(description);
			}
        	returnOrder.saveEx();
        	if(copyLines) {
        		RMAUtil.copyRMALinesFromOrder(sourceOrder, returnOrder, transactionName);
        	}
			returnOrderReference.set(returnOrder);
		});
		return returnOrderReference.get();
	}
    
	public static MOrderLine updateRMALine(int rmaLineId, BigDecimal quantity, String descrption) {
		AtomicReference<MOrderLine> returnOrderReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrderLine rmaLine = new MOrderLine(Env.getCtx(), rmaLineId, transactionName);
			if(rmaLine.getC_OrderLine_ID() <= 0) {
				throw new AdempiereException("@M_RMALine_ID@ @NotFound@");
			}
			MOrder rma = new MOrder(Env.getCtx(), rmaLine.getC_Order_ID(), transactionName);
			//	Validate source document
			if(DocumentUtil.isCompleted(rma) 
					|| DocumentUtil.isClosed(rma)
					|| !rma.isReturnOrder()
					|| !OrderUtil.isValidOrder(rma)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			MOrderLine sourcerOrderLine = new MOrderLine(Env.getCtx(), rmaLine.getRef_OrderLine_ID(), transactionName);
			if(sourcerOrderLine.getC_OrderLine_ID() <= 0) {
				throw new AdempiereException("@Ref_OrderLine_ID@ @NotFound@");
			}
			BigDecimal availableQuantity = RMAUtil.getAvailableQuantityForReturn(sourcerOrderLine, transactionName);
			if(availableQuantity.compareTo(Env.ZERO) > 0) {
				//	Update order quantity
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = rmaLine.getQtyEntered();
					quantityToOrder = quantityToOrder.add(Env.ONE);
				}
				if(availableQuantity.subtract(quantityToOrder).compareTo(Env.ZERO) >= 0) {
					OrderUtil.updateUomAndQuantity(rmaLine, rmaLine.getC_UOM_ID(), quantityToOrder);
				} else {
					throw new AdempiereException("@QtyInsufficient@");
				}
			} else {
				throw new AdempiereException("@QtyInsufficient@");
			}
			returnOrderReference.set(rmaLine);
		});
		return returnOrderReference.get();
	}
	
    /**
     * Create a RMA Line from Order
     * @param rmaId
     * @param sourceOrderLineId
     * @param quantity
     * @param descrption
     * @return
     */
    public static MOrderLine createRMALineFromOrder(int rmaId, int sourceOrderLineId, BigDecimal quantity, String descrption) {
    	if(rmaId <= 0) {
			throw new AdempiereException("@M_RMA_ID@ @NotFound@");
		}
		//	Validate Product and charge
		if(sourceOrderLineId <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
		}
		AtomicReference<MOrderLine> returnOrderReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrder rma = new MOrder(Env.getCtx(), rmaId, transactionName);
			//	Validate source document
			if(DocumentUtil.isCompleted(rma) 
					|| DocumentUtil.isClosed(rma)
					|| !rma.isReturnOrder()
					|| !OrderUtil.isValidOrder(rma)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			MOrderLine sourcerOrderLine = new MOrderLine(Env.getCtx(), sourceOrderLineId, transactionName);
			Optional<MOrderLine> maybeOrderLine = Arrays.asList(rma.getLines(true, null))
					.stream()
					.filter(rmaLineTofind -> rmaLineTofind.getRef_OrderLine_ID() == sourceOrderLineId)
					.findFirst();
			BigDecimal availableQuantity = RMAUtil.getAvailableQuantityForReturn(sourcerOrderLine, transactionName);
			if(maybeOrderLine.isPresent()) {
				MOrderLine rmaLine = maybeOrderLine.get();
				//	Set Quantity
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = rmaLine.getQtyEntered();
					quantityToOrder = quantityToOrder.add(Env.ONE);
				}
    			if(availableQuantity.subtract(quantityToOrder).compareTo(Env.ZERO) >= 0) {
    				//	Update order quantity
    				OrderUtil.updateUomAndQuantity(rmaLine, rmaLine.getC_UOM_ID(), quantityToOrder);
    				returnOrderReference.set(rmaLine);
    			} else {
    				throw new AdempiereException("@QtyInsufficient@");
    			}
			} else {
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = Env.ONE;
				}
		        if(availableQuantity.subtract(quantityToOrder).compareTo(Env.ZERO) >= 0) {
		        	MOrderLine rmaLine = RMAUtil.copyRMALineFromOrder(rma, sourcerOrderLine, transactionName);
		        	Optional.ofNullable(descrption).ifPresent(description -> rmaLine.setDescription(description));
		        	OrderUtil.updateUomAndQuantity(rmaLine, rmaLine.getC_UOM_ID(), quantityToOrder);
		        	returnOrderReference.set(rmaLine);
    			} else {
    				throw new AdempiereException("@QtyInsufficient@");
    			}
			}
		});
		return returnOrderReference.get();
    }
    
    /**
     * Process a RMA
     * @param rmaId
     * @param documentAction
     * @return
     */
    public static MOrder processRMA(int rmaId, int posId, String documentAction, String description) {
    	if(rmaId <= 0) {
			throw new AdempiereException("@M_RMA_ID@ @NotFound@");
		}
    	if(posId <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if(Util.isEmpty(documentAction)) {
			throw new AdempiereException("@DocAction@ @IsMandatory@");
		}
		if(!documentAction.equals(MInOut.ACTION_Complete)
				&& !documentAction.equals(MInOut.ACTION_Reverse_Accrual)
				&& !documentAction.equals(MInOut.ACTION_Reverse_Correct)) {
			throw new AdempiereException("@DocAction@ @Invalid@");
		}
		AtomicReference<MOrder> rmaReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder rma = new MOrder(Env.getCtx(), rmaId, transactionName);
			if(rma.isProcessed()) {
				throw new AdempiereException("@M_InOut_ID@ @Processed@");
			}
			Optional.ofNullable(description).ifPresent(descriptionValue -> rma.addDescription(descriptionValue));
			if (!rma.processIt(documentAction)) {
				throw new AdempiereException("@ProcessFailed@ :" + rma.getProcessMsg());
			}
			rma.saveEx(transactionName);
			//	Generate Return
			RMAUtil.generateReturnFromRMA(rma, transactionName);
	        //	Generate Credit Memo
			RMAUtil.generateCreditMemoFromRMA(rma, transactionName);
			if(!rma.processIt(MOrder.DOCACTION_Close)) {
				throw new AdempiereException("@ProcessFailed@ :" + rma.getProcessMsg());
	        }
			rma.saveEx(transactionName);
			rmaReference.set(rma);
		});
		return rmaReference.get();
    }
}
