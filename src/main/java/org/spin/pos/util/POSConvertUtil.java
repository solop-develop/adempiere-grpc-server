/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   	 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.pos.util;

import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_C_POS;
import org.compiere.model.MBank;
import org.compiere.model.MCampaign;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.Bank;
import org.spin.backend.grpc.pos.Campaign;
import org.spin.backend.grpc.pos.CommandShortcut;
import org.spin.backend.grpc.pos.ShipmentLine;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.ValueUtil;

/**
 * This class was created for add all convert methods for POS form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class POSConvertUtil {

	public static Bank.Builder convertBank(int bankId) {
		if (bankId <= 0) {
			return Bank.newBuilder();
		}
		MBank bank = MBank.get(Env.getCtx(), bankId);
		return convertBank(bank);
	}

	public static Bank.Builder convertBank(MBank bank) {
		Bank.Builder builder = Bank.newBuilder();
		if (bank == null) {
			return builder;
		}
		builder.setId(bank.getC_Bank_ID())
			.setName(
				ValueUtil.validateNull(
					bank.getName()
				)
			)
			.setDescription(
				ValueUtil.validateNull(
					bank.getDescription()
				)
			)
			.setRoutingNo(
				ValueUtil.validateNull(
					bank.getRoutingNo()
				)
			)
			.setSwiftCode(
				ValueUtil.validateNull(
					bank.getSwiftCode()
				)
			)
		;

		return builder;
	}


	public static Campaign.Builder convertCampaign(int campaignId) {
		Campaign.Builder builder = Campaign.newBuilder();
		if (campaignId <= 0) {
			return builder;
		}
		MCampaign campaign = MCampaign.getById(Env.getCtx(), campaignId, null);
		return convertCampaign(campaign);
	}

	public static Campaign.Builder convertCampaign(MCampaign campaign) {
		Campaign.Builder builder = Campaign.newBuilder();
		if (campaign == null || campaign.getC_Campaign_ID() <= 0) {
			return builder;
		}
		builder.setId(campaign.getC_Campaign_ID())
			.setName(
				ValueUtil.validateNull(
					campaign.getName()
				)
			)
			.setDescription(
				ValueUtil.validateNull(
					campaign.getDescription()
				)
			)
			.setStartDate(
				ValueUtil.getTimestampFromDate(
					campaign.getStartDate()
				)
			)
			.setEndDate(
				ValueUtil.getTimestampFromDate(
					campaign.getEndDate()
				)
			)
		;
		return builder;
	}


	public static CommandShortcut.Builder convertCommandShorcut(PO commandShortcut) {
		CommandShortcut.Builder builder = CommandShortcut.newBuilder();
		if (commandShortcut == null) {
			return builder;
		}
		builder.setId(commandShortcut.get_ID())
			.setPosId(
				commandShortcut.get_ValueAsInt(I_C_POS.COLUMNNAME_C_POS_ID)
			)
			.setCommand(
				ValueUtil.validateNull(
					commandShortcut.get_ValueAsString("ECA14_Command")
				)
			)
			.setShortcut(
				ValueUtil.validateNull(
					commandShortcut.get_ValueAsString("ECA14_Shortcut")
				)
			)
		;
		return builder;
	}
	
	/**
	 * Convert shipment line to stub
	 * @param shipmentLine
	 * @return
	 */
	public static ShipmentLine.Builder convertShipmentLine(MInOutLine shipmentLine) {
		ShipmentLine.Builder builder = ShipmentLine.newBuilder();
		if(shipmentLine == null) {
			return builder;
		}
		MOrderLine orderLine = (MOrderLine) shipmentLine.getC_OrderLine();

		MUOMConversion uom = null;
		MUOMConversion productUom = null;
		if (orderLine.getM_Product_ID() > 0) {
			MProduct product = MProduct.get(Env.getCtx(), orderLine.getM_Product_ID());
			List<MUOMConversion> productsConversion = Arrays.asList(MUOMConversion.getProductConversions(Env.getCtx(), product.getM_Product_ID()));
			uom = productsConversion.stream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == orderLine.getC_UOM_ID();
				})
				.findFirst()
				.get();
	
			productUom = productsConversion.stream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == product.getC_UOM_ID();
				})
				.findFirst()
				.get();
		} else {
			uom = new MUOMConversion(Env.getCtx(), 0, null);
			uom.setC_UOM_ID(orderLine.getC_UOM_ID());
			uom.setC_UOM_To_ID(orderLine.getC_UOM_ID());
			uom.setMultiplyRate(Env.ONE);
			uom.setDivideRate(Env.ONE);
			productUom = uom;
		}

		//	Convert
		return builder
			.setOrderLineId(orderLine.getC_OrderLine_ID())
			.setId(shipmentLine.getM_InOutLine_ID())
			.setLine(shipmentLine.getLine())
			.setDescription(ValueUtil.validateNull(shipmentLine.getDescription()))
			.setProduct(
				ConvertUtil.convertProduct(
					shipmentLine.getM_Product_ID()
				)
			)
			.setCharge(
				ConvertUtil.convertCharge(
					shipmentLine.getC_Charge_ID()
				)
			)
			.setQuantity(
				ValueUtil.getDecimalFromBigDecimal(
					shipmentLine.getQtyEntered()
				)
			)
			.setMovementQuantity(
				ValueUtil.getDecimalFromBigDecimal(
					shipmentLine.getMovementQty()
				)
			)
			.setUom(
				ConvertUtil.convertProductConversion(uom)
			)
			.setProductUom(
				ConvertUtil.convertProductConversion(productUom)
			)
		;
	}

}
