/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.core_functionality;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_UOM;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCharge;
import org.compiere.model.MClientInfo;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.util.Env;
import org.spin.backend.grpc.core_functionality.BankAccount;
import org.spin.backend.grpc.core_functionality.BankAccountType;
import org.spin.backend.grpc.core_functionality.BusinessPartner;
import org.spin.backend.grpc.core_functionality.Charge;
import org.spin.backend.grpc.core_functionality.ConversionRate;
import org.spin.backend.grpc.core_functionality.Country;
import org.spin.backend.grpc.core_functionality.Currency;
import org.spin.backend.grpc.core_functionality.DocumentType;
import org.spin.backend.grpc.core_functionality.Organization;
import org.spin.backend.grpc.core_functionality.PriceList;
import org.spin.backend.grpc.core_functionality.Product;
import org.spin.backend.grpc.core_functionality.ProductConversion;
import org.spin.backend.grpc.core_functionality.SalesRepresentative;
import org.spin.backend.grpc.core_functionality.TaxRate;
import org.spin.backend.grpc.core_functionality.UnitOfMeasure;
import org.spin.backend.grpc.core_functionality.Warehouse;
import org.spin.model.MADAttachmentReference;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.AttachmentUtil;

/**
 * This class was created for add all convert methods for Common proto definition
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class CoreFunctionalityConvert {
	
	/**
	 * Convert Currency
	 * @param currency
	 * @return
	 */
	public static Currency.Builder convertCurrency(int currencyId) {
		Currency.Builder builder = Currency.newBuilder();
		if(currencyId <= 0) {
			return builder;
		}
		//	Set values
		MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
		return convertCurrency(
			currency
		);
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
			builder.setCurrency(
				CoreFunctionalityConvert.convertCurrency(
					country.getC_Currency_ID()
				)
			);
		}
		//	
		return builder;
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
			.setValidFrom(
				ValueManager.getTimestampFromDate(
					conversionRate.getValidFrom()
				)
			)
			.setConversionTypeId(conversionRate.getC_ConversionType_ID())
			.setCurrencyFrom(
				CoreFunctionalityConvert.convertCurrency(
					conversionRate.getC_Currency_ID()
				)
			)
			.setCurrencyTo(
				CoreFunctionalityConvert.convertCurrency(
					conversionRate.getC_Currency_ID_To()
				)
			)
			.setMultiplyRate(
				NumberManager.getBigDecimalToString(
					conversionRate.getMultiplyRate()
				)
			)
			.setDivideRate(
				NumberManager.getBigDecimalToString(
					conversionRate.getDivideRate()
				)
			)
		;
		if(conversionRate.getValidTo() != null) {
			builder.setValidTo(
				ValueManager.getTimestampFromDate(
					conversionRate.getValidTo()
				)
			);
		}
		//	
		return builder;
	}


	/**
	 * Convert Document Type
	 * @param documentTypeId
	 * @return
	 */
	public static DocumentType.Builder convertDocumentType(int documentTypeId) {
		DocumentType.Builder builder = DocumentType.newBuilder();
		if (documentTypeId <= 0) {
			return builder;
		}
		MDocType documenType = MDocType.get(Env.getCtx(), documentTypeId);
		return convertDocumentType(documenType);
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
	 * Convert Bank Account to gRPC stub class
	 * @param bankAccountId
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
		return builder.setId(bankAccount.getC_BankAccount_ID())
			.setAccountNo(ValueManager.validateNull(bankAccount.getAccountNo()))
			.setName(ValueManager.validateNull(bankAccount.getName()))
			.setDescription(ValueManager.validateNull(bankAccount.getDescription()))
			.setIsDefault(bankAccount.isDefault())
			.setBban(ValueManager.validateNull(bankAccount.getBBAN()))
			.setIban(ValueManager.validateNull(bankAccount.getIBAN()))
			.setBankAccountType(
				bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Checking) ?
					BankAccountType.CHECKING : BankAccountType.SAVINGS
			)
			.setCreditLimit(
				NumberManager.getBigDecimalToString(
					bankAccount.getCreditLimit()
				)
			)
			.setCurrentBalance(
				NumberManager.getBigDecimalToString(
					bankAccount.getCurrentBalance()
				)
			)
			//	Foreign
			.setCurrency(
				convertCurrency(
					bankAccount.getC_Currency_ID())
				)
			.setBusinessPartner(
				convertBusinessPartner(
					bankAccount.getC_BPartner_ID()
				)
			)
			.setBankId(
				bankAccount.getC_Bank_ID()
			)
		;
	}


	/**
	 * Convert business partner
	 * @param businessPartnerId
	 * @return
	 */
	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		if(businessPartnerId <= 0) {
			return BusinessPartner.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(
			businessPartner
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
			.setMultiplyRate(
				NumberManager.getBigDecimalToString(
					productConversion.getMultiplyRate()
				)
			)
			.setDivideRate(
				NumberManager.getBigDecimalToString(
					productConversion.getDivideRate()
				)
			)
			.setUom(convertUnitOfMeasure(uomToConvert))
			.setProductUom(convertUnitOfMeasure(productUom))
		;
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
		return convertCharge(MCharge.get(Env.getCtx(), chargeId));
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
	 * Convert product
	 * @param productId
	 * @return
	 */
	public static Product.Builder convertProduct(int productId) {
		Product.Builder builder = Product.newBuilder();
		if(productId <= 0) {
			return builder;
		}
		return convertProduct(MProduct.get(Env.getCtx(), productId));
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
			.setWeight(
				NumberManager.getBigDecimalToString(
					product.getWeight()
				)
			)
			.setVolume(
				NumberManager.getBigDecimalToString(
					product.getVolume()
				)
			)
			.setShelfDepth(
				String.valueOf(
					// TODO: is Integer reference change to Quantity reference on database
					product.getShelfDepth()
				)
			)
			.setShelfHeight(
				NumberManager.getBigDecimalToString(
					product.getShelfHeight()
				)
			)
			.setShelfWidth(
				String.valueOf(
					// TODO: is Integer reference change to Quantity reference on database
					product.getShelfWidth()
				)
			)
			.setUnitsPerPallet(
				NumberManager.getBigDecimalToString(
					product.getUnitsPerPallet()
				)
			)
			.setUnitsPerPack(
				String.valueOf(
					// TODO: is Integer reference change to Quantity reference on database
					product.getUnitsPerPack()
				)
			)
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
			.setCurrency(
				CoreFunctionalityConvert.convertCurrency(
					priceList.getC_Currency_ID()
				)
			)
			.setIsDefault(priceList.isDefault())
			.setIsTaxIncluded(priceList.isTaxIncluded())
			.setIsEnforcePriceLimit(priceList.isEnforcePriceLimit())
			.setIsNetPrice(priceList.isNetPrice())
			.setPricePrecision(priceList.getPricePrecision()
		);
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
	 * convert warehouse from id
	 * @param warehouseId
	 * @return
	 */
	public static Warehouse.Builder convertWarehouse(int warehouseId) {
		Warehouse.Builder builder = Warehouse.newBuilder();
		if(warehouseId < 0) {
			return builder;
		}
		return convertWarehouse(MWarehouse.get(Env.getCtx(), warehouseId));
	}
	/**
	 * Convert warehouse
	 * @param warehouse
	 * @return
	 */
	public static Warehouse.Builder convertWarehouse(MWarehouse warehouse) {
		if (warehouse == null || warehouse.getM_Warehouse_ID() < 0) {
			return Warehouse.newBuilder();
		}
		return Warehouse.newBuilder()
			.setId(warehouse.getM_Warehouse_ID())
			.setName(ValueManager.validateNull(warehouse.getName()))
			.setDescription(ValueManager.validateNull(warehouse.getDescription()))
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
			.setRate(
				NumberManager.getBigDecimalToString(
					tax.getRate()
				)
			)
		;
	}

}
