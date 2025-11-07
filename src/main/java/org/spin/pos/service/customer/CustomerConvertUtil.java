package org.spin.pos.service.customer;

import java.util.Arrays;
import java.util.Optional;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_POS;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MRegion;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Address;
import org.spin.backend.grpc.pos.City;
import org.spin.backend.grpc.pos.Customer;
import org.spin.backend.grpc.pos.CustomerTemplate;
import org.spin.backend.grpc.pos.Region;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.store.util.VueStoreFrontUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class CustomerConvertUtil {

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
				TextManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setName(
				TextManager.getValidString(
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


	public static Customer.Builder convertCustomer(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			return Customer.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertCustomer(businessPartner);
	}

	public static Customer.Builder convertCustomer(I_C_BPartner businessPartner) {
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return Customer.newBuilder();
		}
		return convertCustomer(
			(MBPartner) businessPartner
		);
	}
	/**
	 * Convert customer
	 * @param businessPartner
	 * @return
	 */
	public static Customer.Builder convertCustomer(MBPartner businessPartner) {
		if(businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return Customer.newBuilder();
		}
		Customer.Builder customer = Customer.newBuilder()
			.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setValue(
				TextManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setTaxId(
				TextManager.getValidString(
					businessPartner.getTaxID()
				)
			)
			.setDuns(
				TextManager.getValidString(
					businessPartner.getDUNS()
				)
			)
			.setNaics(
				TextManager.getValidString(
					businessPartner.getNAICS()
				)
			)
			.setName(
				TextManager.getValidString(
					businessPartner.getName()
				)
			)
			.setLastName(
				TextManager.getValidString(
					businessPartner.getName2()
				)
			)
			.setDescription(
				TextManager.getValidString(
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
			Value value = ValueManager.getProtoValueFromObject(
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
		Arrays.asList(businessPartner.getLocations(true))
			.stream()
			.filter(customerLocation -> customerLocation.isActive())
			.forEach(address -> {
				customer.addAddresses(
					convertCustomerAddress(address)
				);
			})
		;
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
				TextManager.getValidString(
					location.toString()
				)
			)
			.setPostalCode(
				TextManager.getValidString(
					location.getPostal()
				)
			)
			.setPostalCodeAdditional(
				TextManager.getValidString(
					location.getPostal_Add()
				)
			)
			.setAddress1(
				TextManager.getValidString(
					location.getAddress1()
				)
			)
			.setAddress2(
				TextManager.getValidString(
					location.getAddress2()
				)
			)
			.setAddress3(
				TextManager.getValidString(
					location.getAddress3()
				)
			)
			.setAddress4(
				TextManager.getValidString(
					location.getAddress4()
				)
			)
			.setPostalCode(
				TextManager.getValidString(
					location.getPostal()
				)
			)
			.setDescription(
				TextManager.getValidString(
					businessPartnerLocation.getDescription()
				)
			)
			.setLocationName(
				TextManager.getValidString(
					businessPartnerLocation.getName()
				)
			)
			.setContactName(
				TextManager.getValidString(
					businessPartnerLocation.getContactPerson()
				)
			)
			.setEmail(
				TextManager.getValidString(
					businessPartnerLocation.getEMail()
				)
			)
			.setPhone(
				TextManager.getValidString(
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
			TextManager.getValidString(
				Optional.ofNullable(businessPartnerLocation.getPhone()).orElse(Optional.ofNullable(phone).orElse(""))
			)
		);
		MCountry country = MCountry.get(Env.getCtx(), location.getC_Country_ID());
		builder.setCountryCode(
				TextManager.getValidString(
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
						TextManager.getValidString(
							city.getName()
						)
					)
			);
		} else {
			builder.setCity(
				City.newBuilder()
					.setName(
						TextManager.getValidString(
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
						TextManager.getValidString(
							region.getName()
						)
					)
			);
		}
		//	Additional Attributes
		MTable.get(Env.getCtx(), businessPartnerLocation.get_Table_ID()).getColumnsAsList()
			.stream()
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
			})
		;
		//	
		return builder;
	}

}
