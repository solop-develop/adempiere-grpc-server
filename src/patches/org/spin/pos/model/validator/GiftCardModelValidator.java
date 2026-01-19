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
 * Copyright (C) 2003-2016 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/

package org.spin.pos.model.validator;

import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MTable;
import org.compiere.model.MUOM;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.spin.pos.util.IGiftCard;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gift Card Model Validator
 *
 * @author Gabriel Escalona
 */
public class GiftCardModelValidator implements ModelValidator {
	private static CLogger log = CLogger.getCLogger(GiftCardModelValidator.class);

	/** Client			*/
	private int		clientId = -1;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		//
		if (client != null) {
			clientId = client.getAD_Client_ID();
		}
		engine.addModelChange(MProduct.Table_Name, this);
		engine.addModelChange(MOrderLine.Table_Name, this);

		engine.addDocValidate(MOrder.Table_Name, this);
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
		if (po instanceof MProduct) {
			MProduct product = (MProduct) po;
			if (type == TYPE_BEFORE_NEW) {
				MProductCategory productCategory = (MProductCategory) product.getM_Product_Category();
				product.set_ValueOfColumn(IGiftCard.IsGenerateGiftCard, productCategory.get_ValueAsBoolean(IGiftCard.IsGenerateGiftCard));
			}
		}else if (po instanceof MOrder) {
			MOrder order = (MOrder) po;
			if (type == TYPE_BEFORE_NEW
				|| (order.is_ValueChanged(MOrder.COLUMNNAME_C_DocTypeTarget_ID) && order.getC_DocType_ID() > 0)) {
				MDocType docType = order.getDocumentType();
				order.set_ValueOfColumn(IGiftCard.IsGenerateGiftCard, docType.get_ValueAsBoolean(IGiftCard.IsGenerateGiftCard));
			}
		} else if (po instanceof MOrderLine) {
			MOrderLine orderLine = (MOrderLine) po;
			if (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE) {
				if (orderLine.isSOTrx() && (orderLine.is_new()
					|| orderLine.is_ValueChanged(MOrderLine.COLUMNNAME_M_Product_ID))) {
					boolean isGenerateGiftCard = false;
					MOrder order = orderLine.getParent();
					if (!order.isReturnOrder() && orderLine.getM_Product_ID() > 0) {
						MProduct product = orderLine.getProduct();
						if (product != null) {
							isGenerateGiftCard = product.get_ValueAsBoolean(IGiftCard.IsGenerateGiftCard);
						}
					}
					orderLine.set_ValueOfColumn(IGiftCard.IsGenerateGiftCard, isGenerateGiftCard);
				}
			}
		}
		return null;
	}

	/**
	 * Document Validate for Standard Request Type
	 * @param entity
	 * @param timing see TIMING_ constants
	 * @return
	 */
	public String docValidate(PO entity, int timing) {
		if (entity instanceof MOrder) {
			if (timing == TIMING_AFTER_COMPLETE) {
				MOrder order = (MOrder) entity;
				if (!order.isSOTrx()) {
					return null;
				}
				if (!order.get_ValueAsBoolean(IGiftCard.IsGenerateGiftCard)) {
					return null;
				}
				MTable giftCardTable = MTable.get(order.getCtx(), IGiftCard.ECA14_GiftCard);
				MTable giftCardLineTable = MTable.get(order.getCtx(), IGiftCard.ECA14_GiftCardLine);
				if (giftCardTable == null || giftCardTable.get_ID() <= 0) {
					return null;
				}
				if (giftCardLineTable == null || giftCardLineTable.get_ID() <= 0) {
					return null;
				}

				final String whereClause = IGiftCard.IsGenerateGiftCard + " = 'Y' AND C_Order_ID = ?";
				List<Integer> lineIds = new Query(
					order.getCtx(),
					MOrderLine.Table_Name,
					whereClause,
					order.get_TrxName()
				)
					.setParameters(order.getC_Order_ID())
					.getIDsAsList()
				;
				lineIds.forEach(lineId -> {
					MOrderLine orderLine = new MOrderLine(order.getCtx(), lineId, order.get_TrxName());
					MUOM uom = (MUOM) orderLine.getC_UOM();
					int precision = uom.getStdPrecision();
					if (precision == 0) {
						iterateOrderLineQty(orderLine.getQtyEntered(), order, orderLine, giftCardTable, giftCardLineTable);
					} else {
						createGiftCard(order, orderLine, orderLine.getQtyEntered(), giftCardTable, giftCardLineTable);
					}
				});
			}
		}
		return null;
	}

	private void iterateOrderLineQty(BigDecimal lineQty, MOrder order, MOrderLine orderLine, MTable giftCardTable, MTable giftCardLineTable) {
		Stream.iterate(BigDecimal.ONE,
			current -> current.compareTo(lineQty) <= 0,
			current -> current.add(BigDecimal.ONE)
		)
		.forEach(unit -> {
			createGiftCard(order, orderLine, BigDecimal.ONE, giftCardTable, giftCardLineTable);
		});
	}

	private void createGiftCard(MOrder order, MOrderLine orderLine, BigDecimal qty, MTable giftCardTable, MTable giftCardLineTable) {

		PO giftCard = giftCardTable.getPO(0, order.get_TrxName());
		giftCard.set_ValueOfColumn(MOrder.COLUMNNAME_C_BPartner_ID, order.getC_BPartner_ID());
		giftCard.set_ValueOfColumn(MOrder.COLUMNNAME_C_ConversionType_ID, order.getC_ConversionType_ID());
		giftCard.set_ValueOfColumn(MOrder.COLUMNNAME_C_Currency_ID, order.getC_Currency_ID());
		giftCard.set_ValueOfColumn(MOrder.COLUMNNAME_C_Order_ID, order.getC_Order_ID());
		giftCard.set_ValueOfColumn(IGiftCard.DateDoc, order.getDateOrdered());
		giftCard.set_ValueOfColumn(MOrder.COLUMNNAME_Description, order.getDescription());
		giftCard.set_ValueOfColumn(IGiftCard.IsPrepayment, false);
		// set total amount on header
		giftCard.set_ValueOfColumn(IGiftCard.Amount, qty.multiply(orderLine.getPriceEntered()));
		giftCard.saveEx();

		PO giftCardLine = giftCardLineTable.getPO(0, order.get_TrxName());
		giftCardLine.set_ValueOfColumn(IGiftCard.Amount, qty.multiply(orderLine.getPriceEntered()));
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_C_OrderLine_ID, orderLine.getC_OrderLine_ID());
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_C_UOM_ID, orderLine.getC_UOM_ID());
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_Description, orderLine.getDescription());
		giftCardLine.set_ValueOfColumn(IGiftCard.ECA14_GiftCard_ID, giftCard.get_ValueAsInt(IGiftCard.ECA14_GiftCard_ID));
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_M_Product_ID, orderLine.getM_Product_ID());
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_QtyEntered, qty);
		giftCardLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_QtyOrdered, qty);
		giftCardLine.saveEx();
	}

}
