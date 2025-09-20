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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.core.domains.models.I_C_PaymentMethod;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MBank;
import org.compiere.model.MCampaign;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocation;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRegion;
import org.compiere.model.MTable;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Address;
import org.spin.backend.grpc.pos.AvailableOrderLine;
import org.spin.backend.grpc.pos.AvailablePaymentMethod;
import org.spin.backend.grpc.pos.Bank;
import org.spin.backend.grpc.pos.Campaign;
import org.spin.backend.grpc.pos.City;
import org.spin.backend.grpc.pos.CommandShortcut;
import org.spin.backend.grpc.pos.Customer;
import org.spin.backend.grpc.pos.CustomerTemplate;
import org.spin.backend.grpc.pos.GiftCard;
import org.spin.backend.grpc.pos.GiftCardLine;
import org.spin.backend.grpc.pos.PaymentMethod;
import org.spin.backend.grpc.pos.Region;
import org.spin.backend.grpc.pos.ShipmentLine;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.service.payment.PaymentConvertUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.store.model.MCPaymentMethod;
import org.spin.store.util.VueStoreFrontUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

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
		builder.setId(
				bank.getC_Bank_ID()
			)
			.setName(
				StringManager.getValidString(
					bank.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					bank.getDescription()
				)
			)
			.setRoutingNo(
				StringManager.getValidString(
					bank.getRoutingNo()
				)
			)
			.setSwiftCode(
				StringManager.getValidString(
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
		builder.setId(
				campaign.getC_Campaign_ID()
			)
			.setName(
				StringManager.getValidString(
					campaign.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					campaign.getDescription()
				)
			)
			.setStartDate(
				ValueManager.getProtoTimestampFromTimestamp(
					campaign.getStartDate()
				)
			)
			.setEndDate(
				ValueManager.getProtoTimestampFromTimestamp(
					campaign.getEndDate()
				)
			)
		;
		return builder;
	}


	public static AvailablePaymentMethod.Builder convertPaymentMethod(PO availablePaymentMethod) {
		AvailablePaymentMethod.Builder tenderTypeValue = AvailablePaymentMethod.newBuilder();
		if (availablePaymentMethod == null || availablePaymentMethod.get_ID() <= 0) {
			return tenderTypeValue;
		}

		MTable paymentTypeTable = MTable.get(Env.getCtx(), I_C_PaymentMethod.Table_Name);

		MCPaymentMethod paymentMethod = (MCPaymentMethod) paymentTypeTable.getPO(
			availablePaymentMethod.get_ValueAsInt(I_C_PaymentMethod.COLUMNNAME_C_PaymentMethod_ID), null
		);
		PaymentMethod.Builder paymentMethodBuilder = PaymentConvertUtil.convertPaymentMethod(
			paymentMethod
		);

		final String paymentMethodName = Util.isEmpty(availablePaymentMethod.get_ValueAsString(I_AD_Ref_List.COLUMNNAME_Name), true) ?
			paymentMethod.getName() :
			availablePaymentMethod.get_ValueAsString(I_AD_Ref_List.COLUMNNAME_Name)
		;
		tenderTypeValue
			.setId(
				availablePaymentMethod.get_ID()
			)
			.setName(
				StringManager.getValidString(
					paymentMethodName
				)
			)
			.setPosId(
				availablePaymentMethod.get_ValueAsInt(
					I_C_POS.COLUMNNAME_C_POS_ID
				)
			)
			.setIsPosRequiredPin(
				availablePaymentMethod.get_ValueAsBoolean(I_C_POS.COLUMNNAME_IsPOSRequiredPIN)
			)
			.setIsAllowedToRefund(
				availablePaymentMethod.get_ValueAsBoolean("IsAllowedToRefund")
			)
			.setIsAllowedToRefundOpen(
				availablePaymentMethod.get_ValueAsBoolean("IsAllowedToRefundOpen")
			)
			.setMaximumRefundAllowed(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromObject(
						availablePaymentMethod.get_Value("MaximumRefundAllowed")
					)
				)
			)
			.setMaximumDailyRefundAllowed(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromObject(
						availablePaymentMethod.get_Value("MaximumDailyRefundAllowed")
					)
				)
			)
			.setIsPaymentReference(
				availablePaymentMethod.get_ValueAsBoolean("IsPaymentReference")
			)
			.setDocumentTypeId(
				availablePaymentMethod.get_ValueAsInt("C_DocTypeCreditMemo_ID")
			)
			.setPaymentMethod(
				paymentMethodBuilder
			)
			.setIsAllowsApplyDiscount(
				availablePaymentMethod.get_ValueAsBoolean(
					ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount
				)
			)
			.setMaximumDiscountAllowed(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromObject(
						availablePaymentMethod.get_Value(
							ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed
						)
					)
				)
			)
			.setIsOnline(
				availablePaymentMethod.get_ValueAsBoolean("IsOnline")
			)
		;
		if(availablePaymentMethod.get_ValueAsInt("RefundReferenceCurrency_ID") > 0) {
			tenderTypeValue.setRefundReferenceCurrency(
				CoreFunctionalityConvert.convertCurrency(
					availablePaymentMethod.get_ValueAsInt("RefundReferenceCurrency_ID")
				)
			);
		}
		if(availablePaymentMethod.get_ValueAsInt("ReferenceCurrency_ID") > 0) {
			tenderTypeValue.setReferenceCurrency(
				CoreFunctionalityConvert.convertCurrency(
					availablePaymentMethod.get_ValueAsInt("ReferenceCurrency_ID")
				)
			);
		}
		return tenderTypeValue;
	}


	/**
	 * Convert giftCard from entity
	 * @param giftCard
	 * @return
	 */
	public static GiftCard.Builder convertGiftCard(PO giftCard) {
		GiftCard.Builder builder = GiftCard.newBuilder();
		if (giftCard == null || giftCard.get_ID() <= 0) {
			return builder;
		}
		//	Convert
		builder
			.setId(
				giftCard.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					giftCard.get_ValueAsString(
						I_C_Order.COLUMNNAME_UUID
					)
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					giftCard.get_ValueAsString(
						I_C_Order.COLUMNNAME_DocumentNo
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					giftCard.get_ValueAsString(
						I_C_Order.COLUMNNAME_Description
					)
				)
			)
			.setDateDoc(
				ValueManager.getProtoTimestampFromTimestamp(
					TimeManager.getTimestampFromObject(
						giftCard.get_Value("DateDoc")
					)
				)
			)
			.setValidTo(
				ValueManager.getProtoTimestampFromTimestamp(
					TimeManager.getTimestampFromObject(
						giftCard.get_Value("ValidTo")
					)
				)
			)
			.setOrderId(
				giftCard.get_ValueAsInt(
					I_C_Order.COLUMNNAME_C_Order_ID
				)
			)
			.setIsProcessed(
				giftCard.get_ValueAsBoolean(
					I_C_Order.COLUMNNAME_Processed
				)
			)
			.setIsProcessing(
				giftCard.get_ValueAsBoolean(
					I_C_Order.COLUMNNAME_Processing
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromString(
						giftCard.get_ValueAsString("Amount")
					)
				)
			)
			.setCurrency(
				CoreFunctionalityConvert.convertCurrency(
					giftCard.get_ValueAsInt(
						I_C_Order.COLUMNNAME_C_Currency_ID
					)
				)
			)
			.setConversionTypeId(
				giftCard.get_ValueAsInt(
					I_C_Order.COLUMNNAME_C_ConversionType_ID
				)
			)
			.setIsPrepayment(
				giftCard.get_ValueAsBoolean("IsPrepayment")
			)
			.setBusinessPartner(
				CoreFunctionalityConvert.convertBusinessPartner(
					giftCard.get_ValueAsInt(
						I_C_Order.COLUMNNAME_C_BPartner_ID
					)
				)
			)
		;

		String whereClause = "ECA14_GiftCard_ID = ?";
		List<PO> giftCardLines = new Query(
			giftCard.getCtx(),
			"ECA14_GiftCardLine",
			whereClause,
			giftCard.get_TrxName()
		)
			.setParameters(giftCard.get_ID())
			.list()
		;
		giftCardLines.forEach( line -> {
			builder.addGiftCardLines(
				convertGiftCardLine(line)
			);
		});
		return builder;
	}

	/**
	 * Convert giftCard from entity
	 * @param giftCardLine
	 * @return
	 */
	public static GiftCardLine.Builder convertGiftCardLine(PO giftCardLine) {
		GiftCardLine.Builder builder = GiftCardLine.newBuilder();
		if(giftCardLine == null || giftCardLine.get_ID() <= 0) {
			return builder;
		}
		int orderId = giftCardLine.get_ValueAsInt("C_OrderLine_ID");
		MOrderLine orderLine = new MOrderLine(Env.getCtx(), orderId, null);
		MProduct giftProduct = MProduct.get(Env.getCtx(), giftCardLine.get_ValueAsInt("M_Product_ID"));
		MUOMConversion uom = null;
		MUOMConversion productUom = null;
		if (orderLine.getM_Product_ID() > 0) {
			MProduct product = MProduct.get(Env.getCtx(), orderLine.getM_Product_ID());
			List<MUOMConversion> productsConversion = Arrays.asList(
				MUOMConversion.getProductConversions(Env.getCtx(), product.getM_Product_ID())
			);
			Optional<MUOMConversion> maybeUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == orderLine.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeUom.isPresent()) {
				uom = maybeUom.get();
			}

			Optional<MUOMConversion> maybeProductUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == product.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeProductUom.isPresent()) {
				productUom = maybeProductUom.get();
			}
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
			.setId(
				giftCardLine.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					giftCardLine.get_ValueAsString("UUID")
				)
			).setProduct(
				CoreFunctionalityConvert.convertProduct(
					giftProduct
				)
			)
			.setDescription(
				StringManager.getValidString(
					giftCardLine.get_ValueAsString("Description")
				)
			)
			.setOrderLineId(
				giftCardLine.get_ValueAsInt(
					I_C_OrderLine.COLUMNNAME_C_OrderLine_ID
				)
			)
			.setIsProcessed(
				giftCardLine.get_ValueAsBoolean(
					I_C_OrderLine.COLUMNNAME_Processed
				)
			)
			.setQuantityEntered(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromString(
						giftCardLine.get_ValueAsString(
							I_C_OrderLine.COLUMNNAME_QtyEntered
						)
					)
				)
			)
			.setQuantityOrdered(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromString(
						giftCardLine.get_ValueAsString(
							I_C_OrderLine.COLUMNNAME_QtyOrdered
						)
					)
				)
			).setUom(
				CoreFunctionalityConvert.convertProductConversion(uom)
			)
			.setProductUom(
				CoreFunctionalityConvert.convertProductConversion(productUom)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromString(
						giftCardLine.get_ValueAsString("Amount")
					)
				)
			)
			.setGiftCardId(
				giftCardLine.get_ValueAsInt("ECA14_GiftCard_ID")
			)
		;
	}
	/**
	 * Convert Available Order Line
	 * @param orderLine
	 * @param availableQty
	 * @return
	 */
	public static AvailableOrderLine.Builder convertAvailableOrderLine(MOrderLine orderLine, BigDecimal availableQty) {
		AvailableOrderLine.Builder builder = AvailableOrderLine.newBuilder();
		if(orderLine == null || orderLine.get_ID() <= 0) {
			return builder;
		}
		MProduct product =orderLine.getProduct();
		MUOMConversion uom = null;
		MUOMConversion productUom = null;
		if (orderLine.getM_Product_ID() > 0) {
			List<MUOMConversion> productsConversion = Arrays.asList(
				MUOMConversion.getProductConversions(Env.getCtx(), product.getM_Product_ID())
			);
			Optional<MUOMConversion> maybeUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == orderLine.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeUom.isPresent()) {
				uom = maybeUom.get();
			}

			Optional<MUOMConversion> maybeProductUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == product.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeProductUom.isPresent()) {
				productUom = maybeProductUom.get();
			}
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
			.setId(
				orderLine.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					orderLine.getUUID()
				)
			).setProduct(
				CoreFunctionalityConvert.convertProduct(
					product
				)
			)
			.setDescription(
				StringManager.getValidString(
					orderLine.getDescription()
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					availableQty
				)
			)
			.setPrice(
				NumberManager.getBigDecimalToString(
					orderLine.getPriceActual()
				)
			)
			.setUom(
				CoreFunctionalityConvert.convertProductConversion(uom)
			)
			.setProductUom(
				CoreFunctionalityConvert.convertProductConversion(productUom)
			)
		;
	}



	public static CommandShortcut.Builder convertCommandShorcut(PO commandShortcut) {
		CommandShortcut.Builder builder = CommandShortcut.newBuilder();
		if (commandShortcut == null) {
			return builder;
		}
		builder.setId(
				commandShortcut.get_ID()
			)
			.setPosId(
				commandShortcut.get_ValueAsInt(I_C_POS.COLUMNNAME_C_POS_ID)
			)
			.setCommand(
				StringManager.getValidString(
					commandShortcut.get_ValueAsString("ECA14_Command")
				)
			)
			.setShortcut(
				StringManager.getValidString(
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
			Optional<MUOMConversion> maybeUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == orderLine.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeUom.isPresent()) {
				uom = maybeUom.get();
			}

			Optional<MUOMConversion> maybeProductUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == product.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeProductUom.isPresent()) {
				productUom = maybeProductUom.get();
			}
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
			.setOrderLineId(
				orderLine.getC_OrderLine_ID()
			)
			.setId(
				shipmentLine.getM_InOutLine_ID()
			)
			.setLine(
				shipmentLine.getLine()
			)
			.setDescription(
				StringManager.getValidString(
					shipmentLine.getDescription()
				)
			)
			.setProduct(
				CoreFunctionalityConvert.convertProduct(
					shipmentLine.getM_Product_ID()
				)
			)
			.setCharge(
				CoreFunctionalityConvert.convertCharge(
					shipmentLine.getC_Charge_ID()
				)
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					shipmentLine.getQtyEntered()
				)
			)
			.setMovementQuantity(
				NumberManager.getBigDecimalToString(
					shipmentLine.getMovementQty()
				)
			)
			.setUom(
				CoreFunctionalityConvert.convertProductConversion(uom)
			)
			.setProductUom(
				CoreFunctionalityConvert.convertProductConversion(productUom)
			)
		;
	}



	public static Customer.Builder convertCustomer(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			return Customer.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertCustomer(businessPartner);
	}
	/**
	 * Convert customer
	 * @param businessPartner
	 * @return
	 */
	public static Customer.Builder convertCustomer(MBPartner businessPartner) {
		if(businessPartner == null) {
			return Customer.newBuilder();
		}
		Customer.Builder customer = Customer.newBuilder()
			.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setValue(
				StringManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setTaxId(
				StringManager.getValidString(
					businessPartner.getTaxID()
				)
			)
			.setDuns(
				StringManager.getValidString(
					businessPartner.getDUNS()
				)
			)
			.setNaics(
				StringManager.getValidString(
					businessPartner.getNAICS()
				)
			)
			.setName(
				StringManager.getValidString(
					businessPartner.getName()
				)
			)
			.setLastName(
				StringManager.getValidString(
					businessPartner.getName2()
				)
			)
			.setDescription(
				StringManager.getValidString(
					businessPartner.getDescription()
				)
			)
		;
		//	Additional Attributes
		Struct.Builder customerAdditionalAttributes = Struct.newBuilder();
		MTable.get(Env.getCtx(), businessPartner.get_Table_ID()).getColumnsAsList().stream()
		.filter(column -> {
			String columnName = column.getColumnName();
			return !columnName.equals(MBPartner.COLUMNNAME_UUID)
				&& !columnName.equals(MBPartner.COLUMNNAME_Value)
				&& !columnName.equals(MBPartner.COLUMNNAME_TaxID)
				&& !columnName.equals(MBPartner.COLUMNNAME_DUNS)
				&& !columnName.equals(MBPartner.COLUMNNAME_NAICS)
				&& !columnName.equals(MBPartner.COLUMNNAME_Name)
				&& !columnName.equals(MBPartner.COLUMNNAME_Name2)
				&& !columnName.equals(MBPartner.COLUMNNAME_Description)
			;
		}).forEach(column -> {
			String columnName = column.getColumnName();
			Value value = ValueManager.getValueFromReference(
					businessPartner.get_Value(columnName),
					column.getAD_Reference_ID()
				).build();
			customerAdditionalAttributes.putFields(
				columnName,
				value
			);
		});
		customer.setAdditionalAttributes(customerAdditionalAttributes);
		//	Add Address
		Arrays.asList(businessPartner.getLocations(true)).stream()
			.filter(customerLocation -> customerLocation.isActive())
			.forEach(address -> {
				customer.addAddresses(
					convertCustomerAddress(address)
				);
			});
		return customer;
	}
	
	/**
	 * Convert Address
	 * @param businessPartnerLocation
	 * @return
	 * @return Address.Builder
	 */
	public static Address.Builder convertCustomerAddress(MBPartnerLocation businessPartnerLocation) {
		if(businessPartnerLocation == null) {
			return Address.newBuilder();
		}
		MLocation location = businessPartnerLocation.getLocation(true);
		Address.Builder builder = Address.newBuilder()
			.setId(
				businessPartnerLocation.getC_BPartner_Location_ID()
			)
			.setDisplayValue(
				StringManager.getValidString(
					location.toString()
				)
			)
			.setPostalCode(
				StringManager.getValidString(
					location.getPostal()
				)
			)
			.setPostalCodeAdditional(
				StringManager.getValidString(
					location.getPostal_Add()
				)
			)
			.setAddress1(
				StringManager.getValidString(
					location.getAddress1()
				)
			)
			.setAddress2(
				StringManager.getValidString(
					location.getAddress2()
				)
			)
			.setAddress3(
				StringManager.getValidString(
					location.getAddress3()
				)
			)
			.setAddress4(
				StringManager.getValidString(
					location.getAddress4()
				)
			)
			.setPostalCode(
				StringManager.getValidString(
					location.getPostal()
				)
			)
			.setDescription(
				StringManager.getValidString(
					businessPartnerLocation.getDescription()
				)
			)
			.setLocationName(
				StringManager.getValidString(
					businessPartnerLocation.getName()
				)
			)
			.setContactName(
				StringManager.getValidString(
					businessPartnerLocation.getContactPerson()
				)
			)
			.setEmail(
				StringManager.getValidString(
					businessPartnerLocation.getEMail()
				)
			)
			.setPhone(
				StringManager.getValidString(
					businessPartnerLocation.getPhone()
				)
			)
			.setIsDefaultShipping(
				businessPartnerLocation.get_ValueAsBoolean(
					VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping
				)
			)
			.setIsDefaultBilling(
				businessPartnerLocation.get_ValueAsBoolean(
					VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling
				)
			)
		;
		//	Get user from location
		MUser user = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			I_AD_User.COLUMNNAME_C_BPartner_Location_ID + " = ?",
			businessPartnerLocation.get_TrxName()
		)
			.setParameters(businessPartnerLocation.getC_BPartner_Location_ID())
			.setOnlyActiveRecords(true)
			.first();
		String phone = null;
		if(user != null && user.getAD_User_ID() > 0) {
			if(!Util.isEmpty(user.getPhone())) {
				phone = user.getPhone();
			}
			if(!Util.isEmpty(user.getName()) && Util.isEmpty(builder.getContactName())) {
				builder.setContactName(user.getName());
			}
		}
		//	
		builder.setPhone(
			StringManager.getValidString(
				Optional.ofNullable(businessPartnerLocation.getPhone()).orElse(Optional.ofNullable(phone).orElse(""))
			)
		);
		MCountry country = MCountry.get(Env.getCtx(), location.getC_Country_ID());
		builder.setCountryCode(
				StringManager.getValidString(
					country.getCountryCode()
				)
			)
			.setCountryId(
				country.getC_Country_ID()
			)
		;
		//	City
		if(location.getC_City_ID() > 0) {
			MCity city = MCity.get(Env.getCtx(), location.getC_City_ID());
			builder.setCity(
				City.newBuilder()
					.setId(
						city.getC_City_ID()
					)
					.setName(
						StringManager.getValidString(
							city.getName()
						)
					)
			);
		} else {
			builder.setCity(
				City.newBuilder()
					.setName(
						StringManager.getValidString(
							location.getCity()
						)
					)
				)
			;
		}
		//	Region
		if(location.getC_Region_ID() > 0) {
			MRegion region = MRegion.get(Env.getCtx(), location.getC_Region_ID());
			builder.setRegion(
				Region.newBuilder()
					.setId(
						region.getC_Region_ID()
					)
					.setName(
						StringManager.getValidString(
							region.getName()
						)
					)
			);
		}
		//	Additional Attributes
		MTable.get(Env.getCtx(), businessPartnerLocation.get_Table_ID()).getColumnsAsList().stream()
		.map(column -> column.getColumnName())
		.filter(columnName -> {
			return !columnName.equals(MBPartnerLocation.COLUMNNAME_UUID)
				&& !columnName.equals(MBPartnerLocation.COLUMNNAME_Phone)
				&& !columnName.equals(MBPartnerLocation.COLUMNNAME_Name)
			;
		}).forEach(columnName -> {
			Struct.Builder values = Struct.newBuilder()
				.putFields(
					columnName,
					ValueManager.getProtoValueFromObject(
						businessPartnerLocation.get_Value(columnName)
					).build()
				)
			;
			builder.setAdditionalAttributes(values);
		});
		//	
		return builder;
	}


	/**
	 * Convert customer
	 * @param posCustomerTemplate
	 * @return
	 */
	public static CustomerTemplate.Builder convertCustomerTemplate(PO posCustomerTemplate) {
		CustomerTemplate.Builder builder = CustomerTemplate.newBuilder();
		if(posCustomerTemplate == null || posCustomerTemplate.get_ID() <= 0) {
			return builder;
		}
		int businessPartnerId = posCustomerTemplate.get_ValueAsInt(I_C_BPartner.COLUMNNAME_C_BPartner_ID);
		if(businessPartnerId <= 0) {
			return builder;
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}
		builder
			.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setKey(
				StringManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setName(
				StringManager.getValidString(
					businessPartner.getDisplayValue()
				)
			)
			.setIsPosRequiredPin(
				posCustomerTemplate.get_ValueAsBoolean(
					I_C_POS.COLUMNNAME_IsPOSRequiredPIN
				)
			)
		;

		return builder;
	}

}
