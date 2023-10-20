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
package org.spin.base.util;

import static com.google.protobuf.util.Timestamps.fromMillis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_UOM;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCharge;
import org.compiere.model.MChatEntry;
import org.compiere.model.MClientInfo;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MLanguage;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPOS;
import org.compiere.model.MPOSKey;
import org.compiere.model.MPOSKeyLayout;
import org.compiere.model.MPayment;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MRefList;
import org.compiere.model.MStorage;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.BankAccount;
import org.spin.backend.grpc.common.BusinessPartner;
import org.spin.backend.grpc.common.Charge;
import org.spin.backend.grpc.common.ChatEntry;
import org.spin.backend.grpc.common.ChatEntry.ModeratorStatus;
import org.spin.backend.grpc.common.ConversionRate;
import org.spin.backend.grpc.common.Country;
import org.spin.backend.grpc.common.Currency;
import org.spin.backend.grpc.common.DocumentAction;
import org.spin.backend.grpc.common.DocumentStatus;
import org.spin.backend.grpc.common.DocumentType;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.Organization;
import org.spin.backend.grpc.common.PriceList;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.Product;
import org.spin.backend.grpc.common.ProductConversion;
import org.spin.backend.grpc.common.SalesRepresentative;
import org.spin.backend.grpc.common.TaxRate;
import org.spin.backend.grpc.common.UnitOfMeasure;
import org.spin.backend.grpc.common.Warehouse;
import org.spin.backend.grpc.pos.AvailableSeller;
import org.spin.backend.grpc.pos.CustomerBankAccount;
import org.spin.backend.grpc.pos.Key;
import org.spin.backend.grpc.pos.KeyLayout;
import org.spin.backend.grpc.pos.Order;
import org.spin.backend.grpc.pos.OrderLine;
import org.spin.backend.grpc.pos.Payment;
import org.spin.backend.grpc.pos.PaymentMethod;
import org.spin.backend.grpc.pos.RMA;
import org.spin.backend.grpc.pos.RMALine;
import org.spin.backend.grpc.pos.Shipment;
import org.spin.grpc.service.FileManagement;
import org.spin.grpc.service.TimeControl;
import org.spin.model.MADAttachmentReference;
import org.spin.pos.service.order.OrderUtil;
import org.spin.pos.util.ColumnsAdded;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.util.ValueManager;
import org.spin.store.model.MCPaymentMethod;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for convert any document
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ConvertUtil {
	
	/**
	 * Convert User entity
	 * @param user
	 * @return
	 */
	public static AvailableSeller.Builder convertSeller(MUser user) {
		AvailableSeller.Builder sellerInfo = AvailableSeller.newBuilder();
		if (user == null) {
			return sellerInfo;
		}
		sellerInfo.setId(user.getAD_User_ID());
		sellerInfo.setName(ValueManager.validateNull(user.getName()));
		sellerInfo.setDescription(ValueManager.validateNull(user.getDescription()));
		sellerInfo.setComments(ValueManager.validateNull(user.getComments()));
		if(user.getLogo_ID() > 0 && AttachmentUtil.getInstance().isValidForClient(user.getAD_Client_ID())) {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), user.getAD_Client_ID());
			MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(user.getCtx(), clientInfo.getFileHandler_ID(), user.getLogo_ID(), null);
			if(attachmentReference != null
					&& attachmentReference.getAD_AttachmentReference_ID() > 0) {
				sellerInfo.setImage(ValueManager.validateNull(attachmentReference.getValidFileName()));
			}
		}
		return sellerInfo;
	}

	/**
	 * Convert ProcessInfoLog to gRPC
	 * @param log
	 * @return
	 */
	public static ProcessInfoLog.Builder convertProcessInfoLog(org.compiere.process.ProcessInfoLog log) {
		ProcessInfoLog.Builder processLog = ProcessInfoLog.newBuilder();
		if (log == null) {
			return processLog;
		}
		processLog.setRecordId(log.getP_ID());
		processLog.setLog(ValueManager.validateNull(Msg.parseTranslation(Env.getCtx(), log.getP_Msg())));
		return processLog;
	}

	/**
	 * Convert PO class from Chat Entry process to builder
	 * @param chatEntry
	 * @return
	 */
	public static ChatEntry.Builder convertChatEntry(MChatEntry chatEntry) {
		ChatEntry.Builder builder = ChatEntry.newBuilder();
		if (chatEntry == null) {
			return builder;
		}
		builder.setId(chatEntry.getCM_ChatEntry_ID());
		builder.setChatId(chatEntry.getCM_Chat_ID());
		builder.setSubject(ValueManager.validateNull(chatEntry.getSubject()));
		builder.setCharacterData(ValueManager.validateNull(chatEntry.getCharacterData()));

		if (chatEntry.getAD_User_ID() > 0) {
			MUser user = MUser.get(chatEntry.getCtx(), chatEntry.getAD_User_ID());
			builder.setUserId(chatEntry.getAD_User_ID());
			builder.setUserName(ValueManager.validateNull(user.getName()));
		}
		
		builder.setLogDate(fromMillis(chatEntry.getCreated().getTime()));
		//	Confidential Type
		if(!Util.isEmpty(chatEntry.getConfidentialType())) {
			if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_PublicInformation)) {
				builder.setConfidentialType(org.spin.backend.grpc.common.ChatEntry.ConfidentialType.PUBLIC);
			} else if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_PartnerConfidential)) {
				builder.setConfidentialType(org.spin.backend.grpc.common.ChatEntry.ConfidentialType.PARTER);
			} else if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_Internal)) {
				builder.setConfidentialType(org.spin.backend.grpc.common.ChatEntry.ConfidentialType.INTERNAL);
			}
		}
		//	Moderator Status
		if(!Util.isEmpty(chatEntry.getModeratorStatus())) {
			if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_NotDisplayed)) {
				builder.setModeratorStatus(ModeratorStatus.NOT_DISPLAYED);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_Published)) {
				builder.setModeratorStatus(ModeratorStatus.PUBLISHED);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_Suspicious)) {
				builder.setModeratorStatus(ModeratorStatus.SUSPICIUS);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_ToBeReviewed)) {
				builder.setModeratorStatus(ModeratorStatus.TO_BE_REVIEWED);
			}
		}
		//	Chat entry type
		if(!Util.isEmpty(chatEntry.getChatEntryType())) {
			if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_NoteFlat)) {
				builder.setChatEntryType(org.spin.backend.grpc.common.ChatEntry.ChatEntryType.NOTE_FLAT);
			} else if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_ForumThreaded)) {
				builder.setChatEntryType(org.spin.backend.grpc.common.ChatEntry.ChatEntryType.NOTE_FLAT);
			} else if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_Wiki)) {
				builder.setChatEntryType(org.spin.backend.grpc.common.ChatEntry.ChatEntryType.NOTE_FLAT);
			}
		}
  		return builder;
	}
	
	/**
	 * Convert PO to Value Object
	 * @param entity
	 * @return
	 */
	public static Entity.Builder convertEntity(PO entity) {
		Entity.Builder builder = Entity.newBuilder();
		if(entity == null) {
			return builder;
		}
		builder.setId(entity.get_ID());
		//	Convert attributes
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), entity.get_Table_ID());
		builder.setTableName(ValueManager.validateNull(poInfo.getTableName()));

		Struct.Builder values = Struct.newBuilder();
		for(int index = 0; index < poInfo.getColumnCount(); index++) {
			String columnName = poInfo.getColumnName(index);
			int referenceId = poInfo.getColumnDisplayType(index);
			Object value = entity.get_Value(index);
			Value.Builder builderValue = ValueManager.getValueFromReference(value, referenceId);
			if(builderValue == null) {
				continue;
			}
			//	Add
			values.putFields(columnName, builderValue.build());
		}
		builder.setValues(values);
		//	
		return builder;
	}
	
	/**
	 * Convert Document Action
	 * @param value
	 * @param name
	 * @param description
	 * @return
	 */
	public static DocumentAction.Builder convertDocumentAction(String value, String name, String description) {
		return DocumentAction.newBuilder()
			.setValue(ValueManager.validateNull(value))
			.setName(ValueManager.validateNull(name))
			.setDescription(ValueManager.validateNull(description)
		);
	}
	
	/**
	 * Convert Document Status
	 * @param value
	 * @param name
	 * @param description
	 * @return
	 */
	public static DocumentStatus.Builder convertDocumentStatus(String value, String name, String description) {
		return DocumentStatus.newBuilder()
			.setValue(ValueManager.validateNull(value))
			.setName(ValueManager.validateNull(name))
			.setDescription(ValueManager.validateNull(description)
		);
	}
	
	/**
	 * Convert Document Type
	 * @param documentType
	 * @return
	 */
	public static DocumentType.Builder convertDocumentType(MDocType documentType) {
		if (documentType == null) {
			return DocumentType.newBuilder();
		}

		return DocumentType.newBuilder()
			.setId(documentType.getC_DocType_ID())
			.setName(ValueManager.validateNull(documentType.getName()))
			.setDescription(ValueManager.validateNull(documentType.getDescription()))
			.setPrintName(ValueManager.validateNull(documentType.getPrintName())
		);
	}
	
	/**
	 * Convert business partner
	 * @param businessPartner
	 * @return
	 */
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		if(businessPartner == null) {
			return BusinessPartner.newBuilder();
		}
		return BusinessPartner.newBuilder()
			.setId(businessPartner.getC_BPartner_ID())
			.setValue(ValueManager.validateNull(businessPartner.getValue()))
			.setTaxId(ValueManager.validateNull(businessPartner.getTaxID()))
			.setDuns(ValueManager.validateNull(businessPartner.getDUNS()))
			.setNaics(ValueManager.validateNull(businessPartner.getNAICS()))
			.setName(ValueManager.validateNull(businessPartner.getName()))
			.setLastName(ValueManager.validateNull(businessPartner.getName2()))
			.setDescription(ValueManager.validateNull(businessPartner.getDescription())
		);
	}
	
	/**
	 * Convert charge from 
	 * @param chargeId
	 * @return
	 */
	public static Charge.Builder convertCharge(MCharge charge) {
		Charge.Builder builder = Charge.newBuilder();
		if(charge == null) {
			return builder;
		}
		//	convert charge
		return builder
			.setId(charge.getC_Charge_ID())
			.setName(ValueManager.validateNull(charge.getName()))
			.setDescription(ValueManager.validateNull(charge.getDescription())
		);
	}
	
	/**
	 * Convert charge from 
	 * @param chargeId
	 * @return
	 */
	public static ConversionRate.Builder convertConversionRate(MConversionRate conversionRate) {
		ConversionRate.Builder builder = ConversionRate.newBuilder();
		if(conversionRate == null) {
			return builder;
		}
		//	convert charge
		builder
			.setId(conversionRate.getC_Conversion_Rate_ID())
			.setValidFrom(fromMillis(conversionRate.getValidFrom().getTime()))
			.setConversionTypeId(conversionRate.getC_ConversionType_ID())
			.setCurrencyFrom(convertCurrency(MCurrency.get(Env.getCtx(), conversionRate.getC_Currency_ID())))
			.setCurrencyTo(convertCurrency(MCurrency.get(Env.getCtx(), conversionRate.getC_Currency_ID_To())))
			.setMultiplyRate(ValueManager.getValueFromBigDecimal(conversionRate.getMultiplyRate()))
			.setDivideRate(ValueManager.getValueFromBigDecimal(conversionRate.getDivideRate()));
		if(conversionRate.getValidTo() != null) {
			builder.setValidTo(fromMillis(conversionRate.getValidTo().getTime()));
		}
		//	
		return builder;
	}
	
	/**
	 * Convert Product to 
	 * @param product
	 * @return
	 */
	public static Product.Builder convertProduct(MProduct product) {
		Product.Builder builder = Product.newBuilder();
		if (product == null) {
			return builder;
		}
		builder.setId(product.getM_Product_ID())
			.setValue(ValueManager.validateNull(product.getValue()))
			.setName(ValueManager.validateNull(product.getName()))
			.setDescription(ValueManager.validateNull(product.getDescription()))
			.setHelp(ValueManager.validateNull(product.getHelp()))
			.setDocumentNote(ValueManager.validateNull(product.getDocumentNote()))
			.setUomName(ValueManager.validateNull(MUOM.get(product.getCtx(), product.getC_UOM_ID()).getName()))
			.setDescriptionUrl(ValueManager.validateNull(product.getDescriptionURL()))
			//	Product Type
			.setIsStocked(product.isStocked())
			.setIsDropShip(product.isDropShip())
			.setIsPurchased(product.isPurchased())
			.setIsSold(product.isSold())
			.setImageUrl(ValueManager.validateNull(product.getImageURL()))
			.setUpc(ValueManager.validateNull(product.getUPC()))
			.setSku(ValueManager.validateNull(product.getSKU()))
			.setVersionNo(ValueManager.validateNull(product.getVersionNo()))
			.setGuaranteeDays(product.getGuaranteeDays())
			.setWeight(ValueManager.getValueFromBigDecimal(product.getWeight()))
			.setVolume(ValueManager.getValueFromBigDecimal(product.getVolume()))
			.setShelfDepth(product.getShelfDepth())
			.setShelfHeight(ValueManager.getValueFromBigDecimal(product.getShelfHeight()))
			.setShelfWidth(product.getShelfWidth())
			.setUnitsPerPallet(ValueManager.getValueFromBigDecimal(product.getUnitsPerPallet()))
			.setUnitsPerPack(product.getUnitsPerPack())
			.setTaxCategory(ValueManager.validateNull(product.getC_TaxCategory().getName()))
			.setProductCategoryName(
				ValueManager.validateNull(
					MProductCategory.get(product.getCtx(), product.getM_Product_Category_ID()).getName()
				)
			);
		//	Group
		if(product.getM_Product_Group_ID() != 0) {
			builder.setProductGroupName(ValueManager.validateNull(product.getM_Product_Group().getName()));
		}
		//	Class
		if(product.getM_Product_Class_ID() != 0) {
			builder.setProductClassName(ValueManager.validateNull(product.getM_Product_Class().getName()));
		}
		//	Classification
		if(product.getM_Product_Classification_ID() != 0) {
			builder.setProductClassificationName(ValueManager.validateNull(product.getM_Product_Classification().getName()));
		}
		return builder;
	}
	
	/**
	 * Convert Language to gRPC
	 * @param language
	 * @return
	 */
	public static org.spin.backend.grpc.common.Language.Builder convertLanguage(MLanguage language) {
		if (language == null) {
			return org.spin.backend.grpc.common.Language.newBuilder();
		}

		String datePattern = language.getDatePattern();
		String timePattern = language.getTimePattern();
		if(Util.isEmpty(datePattern)) {
			org.compiere.util.Language staticLanguage = org.compiere.util.Language.getLanguage(language.getAD_Language());
			if(staticLanguage != null) {
				datePattern = staticLanguage.getDateFormat().toPattern();
			}
			//	Validate
			if(Util.isEmpty(datePattern)) {
				datePattern = language.getDateFormat().toPattern();
			}
		}
		if(Util.isEmpty(timePattern)) {
			org.compiere.util.Language staticLanguage = org.compiere.util.Language.getLanguage(language.getAD_Language());
			if(staticLanguage != null) {
				timePattern = staticLanguage.getTimeFormat().toPattern();
			}
		}
		return org.spin.backend.grpc.common.Language.newBuilder()
			.setLanguage(ValueManager.validateNull(language.getAD_Language()))
			.setCountryCode(ValueManager.validateNull(language.getCountryCode()))
			.setLanguageIso(ValueManager.validateNull(language.getLanguageISO()))
			.setLanguageName(ValueManager.validateNull(language.getName()))
			.setDatePattern(ValueManager.validateNull(datePattern))
			.setTimePattern(ValueManager.validateNull(timePattern))
			.setIsBaseLanguage(language.isBaseLanguage())
			.setIsSystemLanguage(language.isSystemLanguage())
			.setIsDecimalPoint(language.isDecimalPoint()
		);
	}
	
	/**
	 * Convert Country
	 * @param context
	 * @param country
	 * @return
	 */
	public static Country.Builder convertCountry(Properties context, MCountry country) {
		Country.Builder builder = Country.newBuilder();
		if(country == null) {
			return builder;
		}
		builder.setId(country.getC_Country_ID())
			.setCountryCode(ValueManager.validateNull(country.getCountryCode()))
			.setName(ValueManager.validateNull(country.getName()))
			.setDescription(ValueManager.validateNull(country.getDescription()))
			.setHasRegion(country.isHasRegion())
			.setRegionName(ValueManager.validateNull(country.getRegionName()))
			.setDisplaySequence(ValueManager.validateNull(country.getDisplaySequence()))
			.setIsAddressLinesReverse(country.isAddressLinesReverse())
			.setCaptureSequence(ValueManager.validateNull(country.getCaptureSequence()))
			.setDisplaySequenceLocal(ValueManager.validateNull(country.getDisplaySequenceLocal()))
			.setIsAddressLinesLocalReverse(country.isAddressLinesLocalReverse())
			.setHasPostalAdd(country.isHasPostal_Add())
			.setExpressionPhone(ValueManager.validateNull(country.getExpressionPhone()))
			.setMediaSize(ValueManager.validateNull(country.getMediaSize()))
			.setExpressionBankRoutingNo(ValueManager.validateNull(country.getExpressionBankRoutingNo()))
			.setExpressionBankAccountNo(ValueManager.validateNull(country.getExpressionBankAccountNo()))
			.setAllowCitiesOutOfList(country.isAllowCitiesOutOfList())
			.setIsPostcodeLookup(country.isPostcodeLookup())
			.setLanguage(ValueManager.validateNull(country.getAD_Language())
		);
		//	Set Currency
		if(country.getC_Currency_ID() != 0) {
			builder.setCurrency(convertCurrency(MCurrency.get(context, country.getC_Currency_ID())));
		}
		//	
		return builder;
	}
	
	/**
	 * Convert Currency
	 * @param currency
	 * @return
	 */
	public static Currency.Builder convertCurrency(MCurrency currency) {
		Currency.Builder builder = Currency.newBuilder();
		if(currency == null) {
			return builder;
		}
		//	Set values
		return builder.setId(currency.getC_Currency_ID())
			.setIsoCode(ValueManager.validateNull(currency.getISO_Code()))
			.setCurSymbol(ValueManager.validateNull(currency.getCurSymbol()))
			.setDescription(ValueManager.validateNull(currency.getDescription()))
			.setStandardPrecision(currency.getStdPrecision())
			.setCostingPrecision(currency.getCostingPrecision()
		);
	}
	
	/**
	 * Convert Price List
	 * @param priceList
	 * @return
	 */
	public static PriceList.Builder convertPriceList(MPriceList priceList) {
		PriceList.Builder builder = PriceList.newBuilder();
		if(priceList == null) {
			return builder;
		}
		//	
		return builder.setId(priceList.getM_PriceList_ID())
			.setName(ValueManager.validateNull(priceList.getName()))
			.setDescription(ValueManager.validateNull(priceList.getDescription()))
			.setCurrency(convertCurrency(MCurrency.get(priceList.getCtx(), priceList.getC_Currency_ID())))
			.setIsDefault(priceList.isDefault())
			.setIsTaxIncluded(priceList.isTaxIncluded())
			.setIsEnforcePriceLimit(priceList.isEnforcePriceLimit())
			.setIsNetPrice(priceList.isNetPrice())
			.setPricePrecision(priceList.getPricePrecision()
		);
	}

	/**
	 * Convert Order from entity
	 * @param order
	 * @return
	 */
	public static Order.Builder convertOrder(MOrder order) {
		Order.Builder builder = Order.newBuilder();
		if(order == null) {
			return builder;
		}
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		int defaultDiscountChargeId = pos.get_ValueAsInt("DefaultDiscountCharge_ID");
		MRefList reference = MRefList.get(Env.getCtx(), MOrder.DOCSTATUS_AD_REFERENCE_ID, order.getDocStatus(), null);
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		List<MOrderLine> orderLines = Arrays.asList(order.getLines());
		BigDecimal totalLines = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() != defaultDiscountChargeId || defaultDiscountChargeId == 0)
				.map(orderLine -> Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal discountAmount = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() > 0 && orderLine.getC_Charge_ID() == defaultDiscountChargeId)
				.map(orderLine -> Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal lineDiscountAmount = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() != defaultDiscountChargeId || defaultDiscountChargeId == 0)
				.map(orderLine -> {
					BigDecimal priceActualAmount = Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO);
					BigDecimal priceListAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO);
					BigDecimal discountLine = priceListAmount.subtract(priceActualAmount)
						.multiply(Optional.ofNullable(orderLine.getQtyOrdered()).orElse(Env.ZERO));
					return discountLine;
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		//	
		BigDecimal totalDiscountAmount = discountAmount.add(lineDiscountAmount);
		
		//	
		Optional<BigDecimal> paidAmount = MPayment.getOfOrder(order).stream().map(payment -> {
			BigDecimal paymentAmount = payment.getPayAmt();
			if(paymentAmount.compareTo(Env.ZERO) == 0
					&& payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
				MInvoice creditMemo = new Query(payment.getCtx(), MInvoice.Table_Name, "C_Payment_ID = ?", payment.get_TrxName()).setParameters(payment.getC_Payment_ID()).first();
				if(creditMemo != null) {
					paymentAmount = creditMemo.getGrandTotal();
				}
			}
			if(!payment.isReceipt()) {
				paymentAmount = payment.getPayAmt().negate();
			}
			return getConvetedAmount(order, payment, paymentAmount);
		}).collect(Collectors.reducing(BigDecimal::add));

		BigDecimal grandTotal = order.getGrandTotal();
		BigDecimal grandTotalConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			grandTotal
		);

		BigDecimal paymentAmount = Env.ZERO;
		if(paidAmount.isPresent()) {
			paymentAmount = paidAmount.get();
		}

		BigDecimal creditAmt = OrderUtil.getCreditAmount(order);
		BigDecimal chargeAmt = OrderUtil.getChargeAmount(order);
		BigDecimal totalPaymentAmount = OrderUtil.getTotalPaymentAmount(order);

		BigDecimal openAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) < 0? Env.ZERO: grandTotal.subtract(totalPaymentAmount));
		BigDecimal refundAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) > 0? Env.ZERO: grandTotal.subtract(totalPaymentAmount).negate());
		BigDecimal displayCurrencyRate = getDisplayConversionRateFromOrder(order);
		//	Convert
		return builder
			.setId(order.getC_Order_ID())
			.setDocumentType(ConvertUtil.convertDocumentType(MDocType.get(Env.getCtx(), order.getC_DocTypeTarget_ID())))
			.setDocumentNo(ValueManager.validateNull(order.getDocumentNo()))
			.setSalesRepresentative(convertSalesRepresentative(MUser.get(Env.getCtx(), order.getSalesRep_ID())))
			.setDescription(ValueManager.validateNull(order.getDescription()))
			.setOrderReference(ValueManager.validateNull(order.getPOReference()))
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					ValueManager.validateNull(order.getDocStatus()), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Name)),
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Description))
				)
			)
			.setPriceList(ConvertUtil.convertPriceList(MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName())))
			.setWarehouse(convertWarehouse(order.getM_Warehouse_ID()))
			.setIsDelivered(order.isDelivered())
			.setDiscountAmount(ValueManager.getValueFromBigDecimal(Optional.ofNullable(totalDiscountAmount).orElse(Env.ZERO).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxAmount(ValueManager.getValueFromBigDecimal(grandTotal.subtract(totalLines.add(discountAmount)).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalLines(ValueManager.getValueFromBigDecimal(totalLines.add(totalDiscountAmount).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setGrandTotal(
				ValueManager.getValueFromBigDecimal(
					grandTotal.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setGrandTotalConverted(
				ValueManager.getValueFromBigDecimal(
					grandTotalConverted.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setDisplayCurrencyRate(ValueManager.getValueFromBigDecimal(displayCurrencyRate.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPaymentAmount(ValueManager.getValueFromBigDecimal(paymentAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setOpenAmount(ValueManager.getValueFromBigDecimal(openAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setRefundAmount(ValueManager.getValueFromBigDecimal(refundAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setDateOrdered(ValueManager.getTimestampFromDate(order.getDateOrdered()))
			.setCustomer(
				POSConvertUtil.convertCustomer(
					(MBPartner) order.getC_BPartner()
				)
			)
			.setCampaign(
				POSConvertUtil.convertCampaign(
					order.getC_Campaign_ID()
				)
			)
			.setChargeAmount(ValueManager.getValueFromBigDecimal(chargeAmt))
			.setCreditAmount(ValueManager.getValueFromBigDecimal(creditAmt))
			.setSourceRmaId(order.get_ValueAsInt("ECA14_Source_RMA_ID"))
			.setIsRma(order.isReturnOrder())
			.setIsOrder(!order.isReturnOrder())
			.setIsBindingOffer(OrderUtil.isBindingOffer(order))
		;
	}
	
	/**
	 * Convert RMA
	 * @param order
	 * @return
	 */
	public static RMA.Builder convertRMA(MOrder order) {
		RMA.Builder builder = RMA.newBuilder();
		if(order == null) {
			return builder;
		}
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		int defaultDiscountChargeId = pos.get_ValueAsInt("DefaultDiscountCharge_ID");
		MRefList reference = MRefList.get(Env.getCtx(), MOrder.DOCSTATUS_AD_REFERENCE_ID, order.getDocStatus(), null);
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		List<MOrderLine> orderLines = Arrays.asList(order.getLines());
		BigDecimal totalLines = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() != defaultDiscountChargeId || defaultDiscountChargeId == 0)
				.map(orderLine -> Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal discountAmount = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() > 0 && orderLine.getC_Charge_ID() == defaultDiscountChargeId)
				.map(orderLine -> Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal lineDiscountAmount = orderLines.stream()
				.filter(orderLine -> orderLine.getC_Charge_ID() != defaultDiscountChargeId || defaultDiscountChargeId == 0)
				.map(orderLine -> {
					BigDecimal priceActualAmount = Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO);
					BigDecimal priceListAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO);
					BigDecimal discountLine = priceListAmount.subtract(priceActualAmount)
						.multiply(Optional.ofNullable(orderLine.getQtyOrdered()).orElse(Env.ZERO));
					return discountLine;
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		//	
		BigDecimal totalDiscountAmount = discountAmount.add(lineDiscountAmount);
		
		//	
		Optional<BigDecimal> paidAmount = MPayment.getOfOrder(order).stream().map(payment -> {
			BigDecimal paymentAmount = payment.getPayAmt();
			if(paymentAmount.compareTo(Env.ZERO) == 0
					&& payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
				MInvoice creditMemo = new Query(payment.getCtx(), MInvoice.Table_Name, "C_Payment_ID = ?", payment.get_TrxName()).setParameters(payment.getC_Payment_ID()).first();
				if(creditMemo != null) {
					paymentAmount = creditMemo.getGrandTotal();
				}
			}
			if(!payment.isReceipt()) {
				paymentAmount = payment.getPayAmt().negate();
			}
			return getConvetedAmount(order, payment, paymentAmount);
		}).collect(Collectors.reducing(BigDecimal::add));

		List<PO> paymentReferencesList = OrderUtil.getPaymentReferencesList(order);
		Optional<BigDecimal> paymentReferenceAmount = paymentReferencesList.stream()
				.map(paymentReference -> {
			BigDecimal amount = ((BigDecimal) paymentReference.get_Value("Amount"));
			if(paymentReference.get_ValueAsBoolean("IsReceipt")) {
				amount = amount.negate();
			}
			return getConvetedAmount(order, paymentReference, amount);
		}).collect(Collectors.reducing(BigDecimal::add));
		BigDecimal grandTotal = order.getGrandTotal();
		BigDecimal paymentAmount = Env.ZERO;
		if(paidAmount.isPresent()) {
			paymentAmount = paidAmount.get();
		}

		int standardPrecision = priceList.getStandardPrecision();

		BigDecimal creditAmt = BigDecimal.ZERO.setScale(standardPrecision, RoundingMode.HALF_UP);
		Optional<BigDecimal> maybeCreditAmt = OrderUtil.getPaymentChageOrCredit(order, true);
		if (maybeCreditAmt.isPresent()) {
			creditAmt = maybeCreditAmt.get()
				.setScale(standardPrecision, RoundingMode.HALF_UP);
		}
		BigDecimal chargeAmt = BigDecimal.ZERO.setScale(standardPrecision, RoundingMode.HALF_UP);
		Optional<BigDecimal> maybeChargeAmt = OrderUtil.getPaymentChageOrCredit(order, false);
		if (maybeChargeAmt.isPresent()) {
			chargeAmt = maybeChargeAmt.get()
				.setScale(standardPrecision, RoundingMode.HALF_UP);
		}
		
		BigDecimal totalPaymentAmount = paymentAmount;
		if(paymentReferenceAmount.isPresent()) {
			totalPaymentAmount = totalPaymentAmount.subtract(paymentReferenceAmount.get());
		}

		BigDecimal openAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) < 0? Env.ZERO: grandTotal.subtract(totalPaymentAmount));
		BigDecimal refundAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) > 0? Env.ZERO: grandTotal.subtract(totalPaymentAmount).negate());
		BigDecimal displayCurrencyRate = getDisplayConversionRateFromOrder(order);
		//	Convert
		return builder
			.setId(order.getC_Order_ID())
			.setSourceOrderId(order.getRef_Order_ID())
			.setDocumentType(ConvertUtil.convertDocumentType(MDocType.get(Env.getCtx(), order.getC_DocTypeTarget_ID())))
			.setDocumentNo(ValueManager.validateNull(order.getDocumentNo()))
			.setSalesRepresentative(convertSalesRepresentative(MUser.get(Env.getCtx(), order.getSalesRep_ID())))
			.setDescription(ValueManager.validateNull(order.getDescription()))
			.setOrderReference(ValueManager.validateNull(order.getPOReference()))
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					ValueManager.validateNull(order.getDocStatus()), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Name)), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Description))
				)
			)
			.setPriceList(ConvertUtil.convertPriceList(MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName())))
			.setWarehouse(convertWarehouse(order.getM_Warehouse_ID()))
			.setIsDelivered(order.isDelivered())
			.setDiscountAmount(ValueManager.getValueFromBigDecimal(Optional.ofNullable(totalDiscountAmount).orElse(Env.ZERO).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxAmount(ValueManager.getValueFromBigDecimal(grandTotal.subtract(totalLines.add(discountAmount)).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalLines(ValueManager.getValueFromBigDecimal(totalLines.add(totalDiscountAmount).setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setGrandTotal(ValueManager.getValueFromBigDecimal(grandTotal.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setDisplayCurrencyRate(ValueManager.getValueFromBigDecimal(displayCurrencyRate.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPaymentAmount(ValueManager.getValueFromBigDecimal(paymentAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setOpenAmount(ValueManager.getValueFromBigDecimal(openAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setRefundAmount(ValueManager.getValueFromBigDecimal(refundAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setDateOrdered(ValueManager.getTimestampFromDate(order.getDateOrdered()))
			.setCustomer(
				POSConvertUtil.convertCustomer(
					(MBPartner) order.getC_BPartner()
				)
			)
			.setCampaign(
				POSConvertUtil.convertCampaign(
					order.getC_Campaign_ID()
				)
			)
			.setChargeAmount(ValueManager.getValueFromBigDecimal(chargeAmt))
			.setCreditAmount(ValueManager.getValueFromBigDecimal(creditAmt))
		;
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MOrder order, PO payment, BigDecimal amount) {
		if(payment.get_ValueAsInt("C_Currency_ID") == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.get_ValueAsInt("C_Currency_ID"), order.getC_Currency_ID(), order.getDateAcct(), payment.get_ValueAsInt("C_ConversionType_ID"), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param pos
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MPOS pos, MPayment payment, BigDecimal amount) {
		MPriceList priceList = MPriceList.get(pos.getCtx(), pos.getM_PriceList_ID(), null);
		if(payment.getC_Currency_ID() == priceList.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(pos.getCtx(), amount, payment.getC_Currency_ID(), priceList.getC_Currency_ID(), payment.getDateAcct(), payment.getC_ConversionType_ID(), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	private static BigDecimal getConvetedAmount(MOrder order, MPayment payment, BigDecimal amount) {
		if(payment.getC_Currency_ID() == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.getC_Currency_ID(), order.getC_Currency_ID(), payment.getDateAcct(), payment.getC_ConversionType_ID(), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Display Currency rate from Sales Order
	 * @param order
	 * @return
	 * @return BigDecimal
	 */
	private static BigDecimal getDisplayConversionRateFromOrder(MOrder order) {
		MPOS pos = MPOS.get(order.getCtx(), order.getC_POS_ID());
		if(order.getC_Currency_ID() == pos.get_ValueAsInt("DisplayCurrency_ID")
				|| pos.get_ValueAsInt("DisplayCurrency_ID") <= 0) {
			return Env.ONE;
		}
		BigDecimal conversionRate = MConversionRate.getRate(order.getC_Currency_ID(), pos.get_ValueAsInt("DisplayCurrency_ID"), order.getDateAcct(), order.getC_ConversionType_ID(), order.getAD_Client_ID(), order.getAD_Org_ID());
		//	
		return Optional.ofNullable(conversionRate).orElse(Env.ZERO);
	}
	
	public static PaymentMethod.Builder convertPaymentMethod(MCPaymentMethod paymentMethod) {
		PaymentMethod.Builder paymentMethodBuilder = PaymentMethod.newBuilder();
		if(paymentMethod == null) {
			return paymentMethodBuilder;
		}
		paymentMethodBuilder
			.setId(paymentMethod.getC_PaymentMethod_ID())
			.setName(ValueManager.validateNull(paymentMethod.getName()))
			.setValue(ValueManager.validateNull(paymentMethod.getValue()))
			.setDescription(ValueManager.validateNull(paymentMethod.getDescription()))
			.setTenderType(ValueManager.validateNull(paymentMethod.getTenderType()))
			.setIsActive(paymentMethod.isActive()
		);

		return paymentMethodBuilder;
	}
	
	/**
	 * Convert payment
	 * @param payment
	 * @return
	 */
	public static Payment.Builder convertPayment(MPayment payment) {
		Payment.Builder builder = Payment.newBuilder();
		if(payment == null) {
			return builder;
		}
		//	
		MRefList reference = MRefList.get(Env.getCtx(), MPayment.DOCSTATUS_AD_REFERENCE_ID, payment.getDocStatus(), payment.get_TrxName());
		int presicion = MCurrency.getStdPrecision(payment.getCtx(), payment.getC_Currency_ID());
		BigDecimal paymentAmount = payment.getPayAmt();
		if(payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)
				&& paymentAmount.compareTo(Env.ZERO) == 0) {
			MInvoice creditMemo = new Query(payment.getCtx(), MInvoice.Table_Name, "C_Payment_ID = ?", payment.get_TrxName()).setParameters(payment.getC_Payment_ID()).first();
			if(creditMemo != null) {
				paymentAmount = creditMemo.getGrandTotal();
			}
		}
		paymentAmount = paymentAmount.setScale(presicion, RoundingMode.HALF_UP);

		MCPaymentMethod paymentMethod = MCPaymentMethod.getById(Env.getCtx(), payment.get_ValueAsInt("C_PaymentMethod_ID"), null);
		PaymentMethod.Builder paymentMethodBuilder = convertPaymentMethod(paymentMethod);
		
		MCurrency currency = MCurrency.get(Env.getCtx(), payment.getC_Currency_ID());
		Currency.Builder currencyBuilder = convertCurrency(currency);
		MOrder order = new MOrder(payment.getCtx(), payment.getC_Order_ID(), null);
		BigDecimal convertedAmount = getConvetedAmount(order, payment, paymentAmount)
			.setScale(presicion, RoundingMode.HALF_UP);
		String invoiceNo = null;
		if(payment.getC_Invoice_ID() > 0) {
			invoiceNo = payment.getC_Invoice().getDocumentNo();
		}
		
		//	Convert
		builder
			.setId(payment.getC_Payment_ID())
			.setOrderId(payment.getC_Order_ID())
			.setDocumentNo(ValueManager.validateNull(payment.getDocumentNo()))
			.setOrderDocumentNo(ValueManager.validateNull(order.getDocumentNo()))
			.setInvoiceDocumentNo(ValueManager.validateNull(invoiceNo))
			.setTenderTypeCode(ValueManager.validateNull(payment.getTenderType()))
			.setReferenceNo(ValueManager.validateNull(Optional.ofNullable(payment.getCheckNo()).orElse(payment.getDocumentNo())))
			.setDescription(ValueManager.validateNull(payment.getDescription()))
			.setAmount(ValueManager.getValueFromBigDecimal(paymentAmount))
			.setConvertedAmount(ValueManager.getValueFromBigDecimal(convertedAmount))
			.setBankId(payment.getC_Bank_ID())
			.setCustomer(
				POSConvertUtil.convertCustomer(
					(MBPartner) payment.getC_BPartner()
				)
			)
			.setCurrency(currencyBuilder)
			.setPaymentDate(ValueManager.getTimestampFromDate(payment.getDateTrx()))
			.setIsRefund(!payment.isReceipt())
			.setPaymentAccountDate(ValueManager.getTimestampFromDate(payment.getDateAcct()))
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(ValueManager.validateNull(payment.getDocStatus()),
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Name)), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Description))
				)
			)
			.setPaymentMethod(paymentMethodBuilder)
			.setCharge(convertCharge(payment.getC_Charge_ID()))
			.setDocumentType(convertDocumentType(MDocType.get(Env.getCtx(), payment.getC_DocType_ID())))
			.setBankAccount(convertBankAccount(MBankAccount.get(Env.getCtx(), payment.getC_BankAccount_ID())))
			.setReferenceBankAccount(convertBankAccount(MBankAccount.get(Env.getCtx(), payment.get_ValueAsInt("POSReferenceBankAccount_ID"))))
			.setIsProcessed(payment.isProcessed())
		;
		if(payment.getCollectingAgent_ID() > 0) {
			builder.setCollectingAgent(ConvertUtil.convertSalesRepresentative(MUser.get(payment.getCtx(), payment.getCollectingAgent_ID())));
		}
		return builder;
	}
	
	/**
	 * Get Order conversion rate for payment
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getOrderConversionRateFromPaymentReference(PO paymentReference) {
		if(paymentReference.get_ValueAsInt("C_Order_ID") <= 0) {
			return Env.ONE;
		}
		MOrder order = new MOrder(Env.getCtx(), paymentReference.get_ValueAsInt("C_Order_ID"), null);
		if(paymentReference.get_ValueAsInt("C_Currency_ID") == order.getC_Currency_ID()) {
			return Env.ONE;
		}
		
		Timestamp conversionDate = Timestamp.valueOf(paymentReference.get_ValueAsString("PayDate"));
		BigDecimal conversionRate = MConversionRate.getRate(
			paymentReference.get_ValueAsInt("C_Currency_ID"),
			order.getC_Currency_ID(),
			conversionDate,
			paymentReference.get_ValueAsInt("C_ConversionType_ID"),
			paymentReference.getAD_Client_ID(),
			paymentReference.getAD_Org_ID()
		);
		//	
		return Optional.ofNullable(conversionRate).orElse(Env.ZERO);
	}
	
	/**
	 * Validate conversion
	 * @param order
	 * @param currencyId
	 * @param conversionTypeId
	 * @param transactionDate
	 */
	public static void validateConversion(MOrder order, int currencyId, int conversionTypeId, Timestamp transactionDate) {
		if(currencyId == order.getC_Currency_ID()) {
			return;
		}
		int convertionRateId = MConversionRate.getConversionRateId(currencyId, 
				order.getC_Currency_ID(), 
				transactionDate, 
				conversionTypeId, 
				order.getAD_Client_ID(), 
				order.getAD_Org_ID());
		if(convertionRateId == -1) {
			String error = MConversionRate.getErrorMessage(order.getCtx(), 
					"ErrorConvertingDocumentCurrencyToBaseCurrency", 
					currencyId, 
					order.getC_Currency_ID(), 
					conversionTypeId, 
					transactionDate, 
					null);
			throw new AdempiereException(error);
		}
	}
	
	/**
	 * Convert customer bank account
	 * @param customerBankAccount
	 * @return
	 * @return CustomerBankAccount.Builder
	 */
	public static CustomerBankAccount.Builder convertCustomerBankAccount(MBPBankAccount customerBankAccount) {
		CustomerBankAccount.Builder builder = CustomerBankAccount.newBuilder();
		if (customerBankAccount == null) {
			return builder;
		}
		builder.setId(customerBankAccount.getC_BP_BankAccount_ID())
			.setCity(ValueManager.validateNull(customerBankAccount.getA_City()))
			.setCountry(ValueManager.validateNull(customerBankAccount.getA_Country()))
			.setEmail(ValueManager.validateNull(customerBankAccount.getA_EMail()))
			.setDriverLicense(ValueManager.validateNull(customerBankAccount.getA_Ident_DL()))
			.setSocialSecurityNumber(ValueManager.validateNull(customerBankAccount.getA_Ident_SSN()))
			.setName(ValueManager.validateNull(customerBankAccount.getA_Name()))
			.setState(ValueManager.validateNull(customerBankAccount.getA_State()))
			.setStreet(ValueManager.validateNull(customerBankAccount.getA_Street()))
			.setZip(ValueManager.validateNull(customerBankAccount.getA_Zip()))
			.setBankAccountType(ValueManager.validateNull(customerBankAccount.getBankAccountType())
		);
		if(customerBankAccount.getC_Bank_ID() > 0) {
			builder.setBankId(customerBankAccount.getC_Bank_ID());
		}
		MBPartner customer = MBPartner.get(Env.getCtx(), customerBankAccount.getC_BPartner_ID());
		builder.setCustomerId(customer.getC_BPartner_ID());
		builder.setAddressVerified(ValueManager.validateNull(customerBankAccount.getR_AvsAddr()))
			.setZipVerified(ValueManager.validateNull(customerBankAccount.getR_AvsZip()))
			.setRoutingNo(ValueManager.validateNull(customerBankAccount.getRoutingNo()))
			.setAccountNo(ValueManager.validateNull(customerBankAccount.getAccountNo()))
			.setIban(ValueManager.validateNull(customerBankAccount.getIBAN())
		);
		return builder;
	}
	
	/**
	 * Convert Order from entity
	 * @param shipment
	 * @return
	 */
	public static  Shipment.Builder convertShipment(MInOut shipment) {
		Shipment.Builder builder = Shipment.newBuilder();
		if(shipment == null) {
			return builder;
		}
		MRefList reference = MRefList.get(Env.getCtx(), MOrder.DOCSTATUS_AD_REFERENCE_ID, shipment.getDocStatus(), null);
		MOrder order = (MOrder) shipment.getC_Order();
		//	Convert
		return builder
			.setOrderId(order.getC_Order_ID())
			.setId(shipment.getM_InOut_ID())
			.setDocumentType(ConvertUtil.convertDocumentType(MDocType.get(Env.getCtx(), shipment.getC_DocType_ID())))
			.setDocumentNo(ValueManager.validateNull(shipment.getDocumentNo()))
			.setSalesRepresentative(convertSalesRepresentative(MUser.get(Env.getCtx(), shipment.getSalesRep_ID())))
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					ValueManager.validateNull(shipment.getDocStatus()), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Name)), 
					ValueManager.validateNull(ValueManager.getTranslation(reference, I_AD_Ref_List.COLUMNNAME_Description))
				)
			)
			.setWarehouse(convertWarehouse(shipment.getM_Warehouse_ID()))
			.setMovementDate(ValueManager.getTimestampFromDate(shipment.getMovementDate())
		);
	}
	
	/**
	 * Convert order line to stub
	 * @param orderLine
	 * @return
	 */
	public static OrderLine.Builder convertOrderLine(MOrderLine orderLine) {
		OrderLine.Builder builder = OrderLine.newBuilder();
		if(orderLine == null) {
			return builder;
		}
		MTax tax = MTax.get(Env.getCtx(), orderLine.getC_Tax_ID());
		MOrder order = orderLine.getParent();
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		BigDecimal quantityEntered = orderLine.getQtyEntered();
		BigDecimal quantityOrdered = orderLine.getQtyOrdered();
		//	Units
		BigDecimal priceListAmount = orderLine.getPriceList();
		BigDecimal priceBaseAmount = orderLine.getPriceActual();
		BigDecimal priceAmount = orderLine.getPriceEntered();
		//	Discount
		BigDecimal discountRate = orderLine.getDiscount();
		BigDecimal discountAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO).subtract(Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO));
		//	Taxes
		BigDecimal priceTaxAmount = tax.calculateTax(priceAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceBaseTaxAmount = tax.calculateTax(priceBaseAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceListTaxAmount = tax.calculateTax(priceListAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		//	Prices with tax
		BigDecimal priceListWithTaxAmount = priceListAmount.add(priceListTaxAmount);
		BigDecimal priceBaseWithTaxAmount = priceBaseAmount.add(priceBaseTaxAmount);
		BigDecimal priceWithTaxAmount = priceAmount.add(priceTaxAmount);
		//	Totals
		BigDecimal totalDiscountAmount = discountAmount.multiply(quantityOrdered);
		BigDecimal totalAmount = orderLine.getLineNetAmt();
		BigDecimal totalAmountConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			totalAmount
		);
		BigDecimal totalBaseAmount = totalAmount.subtract(totalDiscountAmount);
		BigDecimal totalTaxAmount = tax.calculateTax(totalAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal totalBaseAmountWithTax = totalBaseAmount.add(totalTaxAmount);
		BigDecimal totalAmountWithTax = totalAmount.add(totalTaxAmount);
		BigDecimal totalAmountWithTaxConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			totalAmountWithTax
		);

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

		int standardPrecision = priceList.getStandardPrecision();
		BigDecimal availableQuantity = MStorage.getQtyAvailable(orderLine.getM_Warehouse_ID(), 0, orderLine.getM_Product_ID(), orderLine.getM_AttributeSetInstance_ID(), null);
		//	Convert
		return builder.setId(orderLine.getC_OrderLine_ID())
			.setOrderId(orderLine.getC_Order_ID())
			.setLine(orderLine.getLine())
			.setDescription(ValueManager.validateNull(orderLine.getDescription()))
			.setLineDescription(ValueManager.validateNull(orderLine.getName()))
			.setProduct(convertProduct(orderLine.getM_Product_ID()))
			.setCharge(convertCharge(orderLine.getC_Charge_ID()))
			.setWarehouse(convertWarehouse(orderLine.getM_Warehouse_ID()))
			.setQuantity(ValueManager.getValueFromBigDecimal(quantityEntered.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setQuantityOrdered(ValueManager.getValueFromBigDecimal(quantityOrdered.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setAvailableQuantity(ValueManager.getValueFromBigDecimal(availableQuantity.setScale(standardPrecision, RoundingMode.HALF_UP)))
			//	Prices
			.setPriceList(ValueManager.getValueFromBigDecimal(priceListAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPrice(ValueManager.getValueFromBigDecimal(priceAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceBase(ValueManager.getValueFromBigDecimal(priceBaseAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Taxes
			.setPriceListWithTax(ValueManager.getValueFromBigDecimal(priceListWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceBaseWithTax(ValueManager.getValueFromBigDecimal(priceBaseWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceWithTax(ValueManager.getValueFromBigDecimal(priceWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Prices with taxes
			.setListTaxAmount(ValueManager.getValueFromBigDecimal(priceListTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxAmount(ValueManager.getValueFromBigDecimal(priceTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setBaseTaxAmount(ValueManager.getValueFromBigDecimal(priceBaseTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Discounts
			.setDiscountAmount(ValueManager.getValueFromBigDecimal(discountAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setDiscountRate(ValueManager.getValueFromBigDecimal(discountRate.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxRate(ConvertUtil.convertTaxRate(tax))
			//	Totals
			.setTotalDiscountAmount(ValueManager.getValueFromBigDecimal(totalDiscountAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalTaxAmount(ValueManager.getValueFromBigDecimal(totalTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalBaseAmount(ValueManager.getValueFromBigDecimal(totalBaseAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalBaseAmountWithTax(ValueManager.getValueFromBigDecimal(totalBaseAmountWithTax.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalAmount(
				ValueManager.getValueFromBigDecimal(
					totalAmount.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountConverted(
				ValueManager.getValueFromBigDecimal(
					totalAmountConverted.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountWithTax(
				ValueManager.getValueFromBigDecimal(
					totalAmountWithTax.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountWithTaxConverted(
				ValueManager.getValueFromBigDecimal(
					totalAmountWithTaxConverted.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setUom(ConvertUtil.convertProductConversion(uom))
			.setProductUom(ConvertUtil.convertProductConversion(productUom))
			.setResourceAssignment(TimeControl.convertResourceAssignment(orderLine.getS_ResourceAssignment_ID()))
			.setSourceRmaLineId(orderLine.get_ValueAsInt("ECA14_Source_RMALine_ID"))
		;
	}
	
	public static RMALine.Builder convertRMALine(MOrderLine orderLine) {
		RMALine.Builder builder = RMALine.newBuilder();
		if(orderLine == null) {
			return builder;
		}
		MTax tax = MTax.get(Env.getCtx(), orderLine.getC_Tax_ID());
		MOrder order = orderLine.getParent();
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		BigDecimal quantityEntered = orderLine.getQtyEntered();
		BigDecimal quantityOrdered = orderLine.getQtyOrdered();
		//	Units
		BigDecimal priceListAmount = orderLine.getPriceList();
		BigDecimal priceBaseAmount = orderLine.getPriceActual();
		BigDecimal priceAmount = orderLine.getPriceEntered();
		//	Discount
		BigDecimal discountRate = orderLine.getDiscount();
		BigDecimal discountAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO).subtract(Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO));
		//	Taxes
		BigDecimal priceTaxAmount = tax.calculateTax(priceAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceBaseTaxAmount = tax.calculateTax(priceBaseAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceListTaxAmount = tax.calculateTax(priceListAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		//	Prices with tax
		BigDecimal priceListWithTaxAmount = priceListAmount.add(priceListTaxAmount);
		BigDecimal priceBaseWithTaxAmount = priceBaseAmount.add(priceBaseTaxAmount);
		BigDecimal priceWithTaxAmount = priceAmount.add(priceTaxAmount);
		//	Totals
		BigDecimal totalDiscountAmount = discountAmount.multiply(quantityOrdered);
		BigDecimal totalAmount = orderLine.getLineNetAmt();
		BigDecimal totalBaseAmount = totalAmount.subtract(totalDiscountAmount);
		BigDecimal totalTaxAmount = tax.calculateTax(totalAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal totalBaseAmountWithTax = totalBaseAmount.add(totalTaxAmount);
		BigDecimal totalAmountWithTax = totalAmount.add(totalTaxAmount);

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

		int standardPrecision = priceList.getStandardPrecision();
		BigDecimal availableQuantity = MStorage.getQtyAvailable(orderLine.getM_Warehouse_ID(), 0, orderLine.getM_Product_ID(), orderLine.getM_AttributeSetInstance_ID(), null);
		//	Convert
		return builder
			.setId(orderLine.getC_OrderLine_ID())
			.setSourceOrderLineId(orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_OrderLine_ID))
			.setLine(orderLine.getLine())
			.setDescription(ValueManager.validateNull(orderLine.getDescription()))
			.setLineDescription(ValueManager.validateNull(orderLine.getName()))
			.setProduct(convertProduct(orderLine.getM_Product_ID()))
			.setCharge(convertCharge(orderLine.getC_Charge_ID()))
			.setWarehouse(convertWarehouse(orderLine.getM_Warehouse_ID()))
			.setQuantity(ValueManager.getValueFromBigDecimal(quantityEntered.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setQuantityOrdered(ValueManager.getValueFromBigDecimal(quantityOrdered.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setAvailableQuantity(ValueManager.getValueFromBigDecimal(availableQuantity.setScale(standardPrecision, RoundingMode.HALF_UP)))
			//	Prices
			.setPriceList(
				ValueManager.getValueFromBigDecimal(
					priceListAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)
				)
			)
			.setPrice(ValueManager.getValueFromBigDecimal(priceAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceBase(ValueManager.getValueFromBigDecimal(priceBaseAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Taxes
			.setPriceListWithTax(ValueManager.getValueFromBigDecimal(priceListWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceBaseWithTax(ValueManager.getValueFromBigDecimal(priceBaseWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setPriceWithTax(ValueManager.getValueFromBigDecimal(priceWithTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Prices with taxes
			.setListTaxAmount(ValueManager.getValueFromBigDecimal(priceListTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxAmount(ValueManager.getValueFromBigDecimal(priceTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setBaseTaxAmount(ValueManager.getValueFromBigDecimal(priceBaseTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			//	Discounts
			.setDiscountAmount(ValueManager.getValueFromBigDecimal(discountAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setDiscountRate(ValueManager.getValueFromBigDecimal(discountRate.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTaxRate(ConvertUtil.convertTaxRate(tax))
			//	Totals
			.setTotalDiscountAmount(ValueManager.getValueFromBigDecimal(totalDiscountAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalTaxAmount(ValueManager.getValueFromBigDecimal(totalTaxAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalBaseAmount(ValueManager.getValueFromBigDecimal(totalBaseAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalBaseAmountWithTax(ValueManager.getValueFromBigDecimal(totalBaseAmountWithTax.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalAmount(ValueManager.getValueFromBigDecimal(totalAmount.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setTotalAmountWithTax(ValueManager.getValueFromBigDecimal(totalAmountWithTax.setScale(priceList.getStandardPrecision(), RoundingMode.HALF_UP)))
			.setUom(ConvertUtil.convertProductConversion(uom))
			.setProductUom(ConvertUtil.convertProductConversion(productUom))
		;
	}

	/**
	 * Convert product
	 * @param productId
	 * @return
	 */
	public static Product.Builder convertProduct(int productId) {
		Product.Builder builder = Product.newBuilder();
		if(productId <= 0) {
			return builder;
		}
		return ConvertUtil.convertProduct(MProduct.get(Env.getCtx(), productId));
	}
	
	/**
	 * Convert charge
	 * @param chargeId
	 * @return
	 */
	public static Charge.Builder convertCharge(int chargeId) {
		Charge.Builder builder = Charge.newBuilder();
		if(chargeId <= 0) {
			return builder;
		}
		return ConvertUtil.convertCharge(MCharge.get(Env.getCtx(), chargeId));
	}
	
	/**
	 * convert warehouse from id
	 * @param warehouseId
	 * @return
	 */
	public static Warehouse.Builder convertWarehouse(int warehouseId) {
		Warehouse.Builder builder = Warehouse.newBuilder();
		if(warehouseId <= 0) {
			return builder;
		}
		return ConvertUtil.convertWarehouse(MWarehouse.get(Env.getCtx(), warehouseId));
	}
	
	/**
	 * Convert key layout from id
	 * @param keyLayoutId
	 * @return
	 */
	public static KeyLayout.Builder convertKeyLayout(int keyLayoutId) {
		KeyLayout.Builder builder = KeyLayout.newBuilder();
		if(keyLayoutId <= 0) {
			return builder;
		}
		return convertKeyLayout(MPOSKeyLayout.get(Env.getCtx(), keyLayoutId));
	}
	
	/**
	 * Convert Key Layout from PO
	 * @param keyLayout
	 * @return
	 */
	public static KeyLayout.Builder convertKeyLayout(MPOSKeyLayout keyLayout) {
		KeyLayout.Builder builder = KeyLayout.newBuilder();
		if (keyLayout == null) {
			return builder;
		}
		builder
			.setId(keyLayout.getC_POSKeyLayout_ID())
			.setName(ValueManager.validateNull(keyLayout.getName()))
			.setDescription(ValueManager.validateNull(keyLayout.getDescription()))
			.setHelp(ValueManager.validateNull(keyLayout.getHelp()))
			.setLayoutType(ValueManager.validateNull(keyLayout.getPOSKeyLayoutType()))
			.setColumns(keyLayout.getColumns()
		);
		//	TODO: Color
		//	Add keys
		Arrays.asList(keyLayout.getKeys(false)).stream()
			.filter(key -> key.isActive())
			.forEach(key -> {
				builder.addKeys(
					convertKey(key)
				);
			});
		return builder;
	}
	
	/**
	 * Convet key for layout
	 * @param key
	 * @return
	 */
	public static Key.Builder convertKey(MPOSKey key) {
		if (key == null) {
			return Key.newBuilder();
		}
		String productValue = null;
		if(key.getM_Product_ID() > 0) {
			productValue = MProduct.get(Env.getCtx(), key.getM_Product_ID()).getValue();
		}
		return Key.newBuilder()
			.setId(key.getC_POSKeyLayout_ID())
			.setName(ValueManager.validateNull(key.getName()))
			//	TODO: Color
			.setSequence(key.getSeqNo())
			.setSpanX(key.getSpanX())
			.setSpanY(key.getSpanY())
			.setSubKeyLayoutId(key.getSubKeyLayout_ID())
			.setQuantity(ValueManager.getValueFromBigDecimal(Optional.ofNullable(key.getQty()).orElse(Env.ZERO)))
			.setProductValue(ValueManager.validateNull(productValue))
			.setResourceReference(
				FileManagement.convertResourceReference(
					FileUtil.getResourceFromImageId(
						key.getAD_Image_ID())
					)
			)
		;
	}


	/**
	 * Convert Sales Representative
	 * @param salesRepresentative
	 * @return
	 */
	public static SalesRepresentative.Builder convertSalesRepresentative(MUser salesRepresentative) {
		if (salesRepresentative == null) {
			return SalesRepresentative.newBuilder();
		}
		return SalesRepresentative.newBuilder()
			.setId(salesRepresentative.getAD_User_ID())
			.setName(ValueManager.validateNull(salesRepresentative.getName()))
			.setDescription(ValueManager.validateNull(salesRepresentative.getDescription()))
		;
	}

	/**
	 * Convert Bank Account to gRPC stub class
	 * @param bankAccount
	 * @return
	 */
	public static BankAccount.Builder convertBankAccount(int bankAccountId) {
		if(bankAccountId <= 0) {
			return BankAccount.newBuilder();
		}
		MBankAccount bankAccount = MBankAccount.get(Env.getCtx(), bankAccountId);
		return convertBankAccount(bankAccount);
	}
	/**
	 * Convert Bank Account to gRPC stub class
	 * @param bankAccount
	 * @return
	 */
	public static BankAccount.Builder convertBankAccount(MBankAccount bankAccount) {
		BankAccount.Builder builder = BankAccount.newBuilder();
		if(bankAccount == null) {
			return builder;
		}
		//	
		return builder.setId(bankAccount.getAD_Org_ID())
			.setAccountNo(ValueManager.validateNull(bankAccount.getAccountNo()))
			.setName(ValueManager.validateNull(bankAccount.getName()))
			.setDescription(ValueManager.validateNull(bankAccount.getDescription()))
			.setIsDefault(bankAccount.isDefault())
			.setBban(ValueManager.validateNull(bankAccount.getBBAN()))
			.setIban(ValueManager.validateNull(bankAccount.getIBAN()))
			.setBankAccountType(bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Checking)? BankAccount.BankAccountType.CHECKING: BankAccount.BankAccountType.SAVINGS)
			.setCreditLimit(ValueManager.getValueFromBigDecimal(bankAccount.getCreditLimit()))
			.setCurrentBalance(ValueManager.getValueFromBigDecimal(bankAccount.getCurrentBalance()))
			//	Foreign
			.setCurrency(convertCurrency(MCurrency.get(bankAccount.getCtx(), bankAccount.getC_Currency_ID())))
			.setBusinessPartner(ConvertUtil.convertBusinessPartner(MBPartner.get(Env.getCtx(), bankAccount.getC_BPartner_ID())))
		;
	}
	
	/**
	 * Convert organization
	 * @param organization
	 * @return
	 */
	public static Organization.Builder convertOrganization(MOrg organization) {
		if (organization == null) {
			return Organization.newBuilder();
		}
		MOrgInfo organizationInfo = MOrgInfo.get(Env.getCtx(), organization.getAD_Org_ID(), null);
		AtomicReference<String> corporateImageBranding = new AtomicReference<String>();
		if(organizationInfo.getCorporateBrandingImage_ID() > 0 && AttachmentUtil.getInstance().isValidForClient(organizationInfo.getAD_Client_ID())) {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), organizationInfo.getAD_Client_ID());
			MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(Env.getCtx(), clientInfo.getFileHandler_ID(), organizationInfo.getCorporateBrandingImage_ID(), null);
			if(attachmentReference != null
					&& attachmentReference.getAD_AttachmentReference_ID() > 0) {
				corporateImageBranding.set(attachmentReference.getValidFileName());
			}
		}
		return Organization.newBuilder()
				.setCorporateBrandingImage(ValueManager.validateNull(corporateImageBranding.get()))
				.setId(organization.getAD_Org_ID())
				.setName(ValueManager.validateNull(organization.getName()))
				.setDescription(ValueManager.validateNull(organization.getDescription()))
				.setDuns(ValueManager.validateNull(organizationInfo.getDUNS()))
				.setTaxId(ValueManager.validateNull(organizationInfo.getTaxID()))
				.setPhone(ValueManager.validateNull(organizationInfo.getPhone()))
				.setPhone2(ValueManager.validateNull(organizationInfo.getPhone2()))
				.setFax(ValueManager.validateNull(organizationInfo.getFax()))
				.setIsReadOnly(false)
			;
	}
	
	/**
	 * Convert warehouse
	 * @param warehouse
	 * @return
	 */
	public static Warehouse.Builder convertWarehouse(MWarehouse warehouse) {
		if (warehouse == null) {
			return Warehouse.newBuilder();
		}
		return Warehouse.newBuilder()
			.setId(warehouse.getM_Warehouse_ID())
			.setName(ValueManager.validateNull(warehouse.getName()))
			.setDescription(ValueManager.validateNull(warehouse.getDescription()))
		;
	}

	/**
	 * Convert Unit of Measure
	 * @param uom
	 * @return
	 */
	public static UnitOfMeasure.Builder convertUnitOfMeasure(MUOM unitOfMeasure) {
		UnitOfMeasure.Builder unitOfMeasureBuilder = UnitOfMeasure.newBuilder();
		if (unitOfMeasure == null) {
			return unitOfMeasureBuilder;
		}

		unitOfMeasureBuilder
			.setId(unitOfMeasure.getC_UOM_ID())
			.setName(ValueManager.validateNull(unitOfMeasure.get_Translation(I_C_UOM.COLUMNNAME_Name)))
			.setCode(ValueManager.validateNull(unitOfMeasure.getX12DE355()))
			.setSymbol(ValueManager.validateNull(unitOfMeasure.get_Translation(I_C_UOM.COLUMNNAME_UOMSymbol)))
			.setDescription(ValueManager.validateNull(unitOfMeasure.get_Translation(I_C_UOM.COLUMNNAME_Description)))
			.setCostingPrecision(unitOfMeasure.getCostingPrecision())
			.setStandardPrecision(unitOfMeasure.getStdPrecision())
		;
		return unitOfMeasureBuilder;
	}

	/**
	 * Convert Unit of Measure Product Conversion
	 * @param uom
	 * @return
	 */
	public static ProductConversion.Builder convertProductConversion(MUOMConversion productConversion) {
		if (productConversion == null) {
			return ProductConversion.newBuilder();
		}
		MUOM productUom = MUOM.get(Env.getCtx(), productConversion.getC_UOM_ID());
		MUOM uomToConvert = MUOM.get(Env.getCtx(), productConversion.getC_UOM_To_ID());
		
		return ProductConversion.newBuilder()
			.setId(productConversion.getC_UOM_Conversion_ID())
			.setMultiplyRate(ValueManager.getValueFromBigDecimal(productConversion.getMultiplyRate()))
			.setDivideRate(ValueManager.getValueFromBigDecimal(productConversion.getDivideRate()))
			.setUom(convertUnitOfMeasure(uomToConvert))
			.setProductUom(convertUnitOfMeasure(productUom))
		;
	}

	/**
	 * Convert tax to gRPC
	 * @param tax
	 * @return
	 */
	public static TaxRate.Builder convertTaxRate(MTax tax) {
		if (tax == null) {
			return TaxRate.newBuilder();
		}
		return TaxRate.newBuilder().setName(ValueManager.validateNull(tax.getName()))
			.setDescription(ValueManager.validateNull(tax.getDescription()))
			.setTaxIndicator(ValueManager.validateNull(tax.getTaxIndicator()))
			.setRate(ValueManager.getValueFromBigDecimal(tax.getRate()))
		;
	}
}
