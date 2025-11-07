package org.spin.pos.service.customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.model.MPOS;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.AddressRequest;
import org.spin.backend.grpc.pos.CreateCustomerRequest;
import org.spin.backend.grpc.pos.Customer;
import org.spin.backend.grpc.pos.CustomerTemplate;
import org.spin.backend.grpc.pos.GetCustomerRequest;
import org.spin.backend.grpc.pos.ListCustomerTemplatesRequest;
import org.spin.backend.grpc.pos.ListCustomerTemplatesResponse;
import org.spin.backend.grpc.pos.ListCustomersRequest;
import org.spin.backend.grpc.pos.ListCustomersResponse;
import org.spin.backend.grpc.pos.UpdateCustomerRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.pos.service.pos.AccessManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.store.util.VueStoreFrontUtil;

public class CustomerServiceLogic {

	/**
	 * Create Customer
	 * @param request
	 * @return
	 */
	public static Customer.Builder createCustomer(Properties context, CreateCustomerRequest request) {
		//	Validate name
		if(Util.isEmpty(request.getName(), true)) {
			throw new AdempiereException("@Name@ @IsMandatory@");
		}
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
		//	POS Uuid
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		MBPartner businessPartner = MBPartner.getTemplate(context, clientId, pos.getC_POS_ID());

		//	Validate Template
		int customerTemplateId = request.getCustomerTemplateId();
		if (customerTemplateId <= 0) {
			customerTemplateId = pos.getC_BPartnerCashTrx_ID();
		}
		if(customerTemplateId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartnerCashTrx_ID@");
		}
		MBPartner template = MBPartner.get(context, customerTemplateId);
		if (template == null || template.getC_BPartner_ID() <= 0) {
			throw new AdempiereException("@C_BPartnerCashTrx_ID@ @NotFound@");
		}

		// copy and clear values by termplate
		PO.copyValues(template, businessPartner);
		businessPartner.setTaxID("");
		businessPartner.setValue("");
		businessPartner.setNAICS(null);
		businessPartner.setName("");
		businessPartner.setName2(null);
		businessPartner.setDUNS("");
		businessPartner.setIsActive(true);
		// Balances
		businessPartner.setTotalOpenBalance(null);
		businessPartner.setSO_CreditUsed(Env.ZERO);
		businessPartner.setActualLifeTimeValue(null);

		Optional<MBPartnerLocation> maybeTemplateLocation = Arrays.asList(template.getLocations(false))
			.stream()
			.findFirst()
		;
		if(!maybeTemplateLocation.isPresent()) {
			throw new AdempiereException("@C_BPartnerCashTrx_ID@ @C_BPartner_Location_ID@ @NotFound@");
		}
		//	Get location from template
		MLocation templateLocation = maybeTemplateLocation.get().getLocation(false);
		if(templateLocation == null || templateLocation.getC_Location_ID() <= 0) {
			throw new AdempiereException("@C_Location_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			//	Create it
			businessPartner.setAD_Org_ID(0);
			businessPartner.setIsCustomer (true);
			businessPartner.setIsVendor (false);
			businessPartner.set_TrxName(transactionName);

			//	Set Value
			String code = request.getValue();
			if(Util.isEmpty(code, true)) {
				code = DB.getDocumentNo(clientId, I_C_BPartner.Table_Name, transactionName, businessPartner);
			}
			businessPartner.setValue(code);
			//	Tax Id
			Optional.ofNullable(request.getTaxId()).ifPresent(value -> businessPartner.setTaxID(value));
			//	Duns
			Optional.ofNullable(request.getDuns()).ifPresent(value -> businessPartner.setDUNS(value));
			//	Naics
			Optional.ofNullable(request.getNaics()).ifPresent(value -> businessPartner.setNAICS(value));
			//	Name
			Optional.ofNullable(request.getName()).ifPresent(value -> businessPartner.setName(value));
			//	Last name
			Optional.ofNullable(request.getLastName()).ifPresent(value -> businessPartner.setName2(value));
			//	Description
			Optional.ofNullable(request.getDescription()).ifPresent(value -> businessPartner.setDescription(value));
			//	Business partner group
			if(request.getBusinessPartnerGroupId() > 0) {
				int businessPartnerGroupId = request.getBusinessPartnerGroupId();
				if(businessPartnerGroupId != 0) {
					businessPartner.setC_BP_Group_ID(businessPartnerGroupId);
				}
			}
			//	Additional attributes
			CustomerUtil.setAdditionalAttributes(
				businessPartner,
				request.getAdditionalAttributes().getFieldsMap()
			);
			//	Save it
			businessPartner.saveEx(transactionName);
			
			// clear price list from business partner group
			if (businessPartner.getM_PriceList_ID() > 0) {
				businessPartner.setM_PriceList_ID(0);
				businessPartner.saveEx(transactionName);
			}
			
			//	Location
			request.getAddressesList().forEach(address -> {
				createCustomerAddress(
					businessPartner,
					address,
					templateLocation,
					transactionName
				);
			});
		});
		//	Default return
		return CustomerConvertUtil.convertCustomer(
			businessPartner
		);
	}


	/**
	 * Create Address from customer and address request
	 * @param customer
	 * @param address
	 * @param templateLocation
	 * @param transactionName
	 * @return void
	 */
	private static void createCustomerAddress(MBPartner customer, AddressRequest address, MLocation templateLocation, String transactionName) {
		int countryId = address.getCountryId();
		//	Instance it
		MLocation location = new MLocation(Env.getCtx(), 0, transactionName);
		if(countryId > 0) {
			int regionId = address.getRegionId();
			int cityId = address.getCityId();
			String cityName = null;
			//	City Name
			if(!Util.isEmpty(address.getCityName())) {
				cityName = address.getCityName();
			}
			location.setC_Country_ID(countryId);
			location.setC_Region_ID(regionId);
			location.setCity(cityName);
			if(cityId > 0) {
				location.setC_City_ID(cityId);
			}
		} else {
			//	Copy
			PO.copyValues(templateLocation, location);
		}
		//	Postal Code
		if(!Util.isEmpty(address.getPostalCode())) {
			location.setPostal(address.getPostalCode());
		}
		//	Address
		Optional.ofNullable(address.getAddress1()).ifPresent(addressValue -> location.setAddress1(addressValue));
		Optional.ofNullable(address.getAddress2()).ifPresent(addressValue -> location.setAddress2(addressValue));
		Optional.ofNullable(address.getAddress3()).ifPresent(addressValue -> location.setAddress3(addressValue));
		Optional.ofNullable(address.getAddress4()).ifPresent(addressValue -> location.setAddress4(addressValue));
		Optional.ofNullable(address.getPostalCode()).ifPresent(postalCode -> location.setPostal(postalCode));
		//	
		location.saveEx(transactionName);
		//	Create BP location
		MBPartnerLocation businessPartnerLocation = new MBPartnerLocation(customer);
		businessPartnerLocation.setC_Location_ID(location.getC_Location_ID());
		//	Default
		businessPartnerLocation.setIsBillTo(address.getIsDefaultBilling());
		businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling, address.getIsDefaultBilling());
		businessPartnerLocation.setIsShipTo(address.getIsDefaultShipping());
		businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping, address.getIsDefaultShipping());
		Optional.ofNullable(address.getContactName()).ifPresent(contact -> businessPartnerLocation.setContactPerson(contact));
		Optional.ofNullable(address.getLocationName()).ifPresent(locationName -> businessPartnerLocation.setName(locationName));
		Optional.ofNullable(address.getEmail()).ifPresent(email -> businessPartnerLocation.setEMail(email));
		Optional.ofNullable(address.getPhone()).ifPresent(phome -> businessPartnerLocation.setPhone(phome));
		Optional.ofNullable(address.getDescription()).ifPresent(description -> businessPartnerLocation.setDescription(description));
		if(Util.isEmpty(businessPartnerLocation.getName())) {
			businessPartnerLocation.setName(".");
		}
		//	Additional attributes
		CustomerUtil.setAdditionalAttributes(
			businessPartnerLocation,
			address.getAdditionalAttributes().getFieldsMap()
		);
		businessPartnerLocation.saveEx(transactionName);
		//	Contact
		if(!Util.isEmpty(address.getContactName(), true) || !Util.isEmpty(address.getEmail(), true) || !Util.isEmpty(address.getPhone(), true)) {
			MUser contact = new MUser(customer);
			Optional.ofNullable(address.getEmail()).ifPresent(email -> contact.setEMail(email));
			Optional.ofNullable(address.getPhone()).ifPresent(phome -> contact.setPhone(phome));
			Optional.ofNullable(address.getDescription()).ifPresent(description -> contact.setDescription(description));
			String contactName = address.getContactName();
			if(Util.isEmpty(contactName, true)) {
				contactName = address.getEmail();
			}
			if(Util.isEmpty(contactName, true)) {
				contactName = address.getPhone();
			}
			contact.setName(contactName);
			//	Save
			contact.setC_BPartner_Location_ID(businessPartnerLocation.getC_BPartner_Location_ID());
			contact.saveEx(transactionName);
 		}
	}



	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static Customer.Builder getCustomer(GetCustomerRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if(!Util.isEmpty(searchValue, true)) {
			// TODO: Check if it is better with the `LIKE` operator
			whereClause.append(
				"(UPPER(Value) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getEmail());
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) = UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) = UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}

		//	Get business partner
		MBPartner businessPartner = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.first()
		;
		//	Default return
		return CustomerConvertUtil.convertCustomer(
			businessPartner
		);
	}

	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCustomersResponse.Builder listCustomers(ListCustomersRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append("("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(TaxID) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) LIKE UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) LIKE '%' || UPPER(?) || '%')"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) LIKE UPPER(?)))"
			);
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) LIKE UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) LIKE UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}
		//	
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), I_C_BPartner.Table_Name, parameters);
		if(whereClause.length() > 0
				&& !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCustomersResponse.Builder builderList = ListCustomersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(businessPartnerId -> {
				Customer.Builder customBuilder = CustomerConvertUtil.convertCustomer(
					businessPartnerId
				);
				builderList.addCustomers(customBuilder);
			});
	
		//	Default return
		return builderList;
	}



	/**
	 * update Customer
	 * @param request
	 * @return
	 */
	public static Customer.Builder updateCustomer(UpdateCustomerRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if(!AccessManagement.getBooleanValueFromPOS(pos, Env.getAD_User_ID(Env.getCtx()), ColumnsAdded.COLUMNNAME_IsAllowsModifyCustomer)) {
			throw new AdempiereException("@POS.ModifyCustomerNotAllowed@");
		}
		//	Customer Uuid
		if(request.getId() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @IsMandatory@");
		}
		//	
		AtomicReference<MBPartner> customer = new AtomicReference<MBPartner>();
		Trx.run(transactionName -> {
			//	Create it
			MBPartner businessPartner = MBPartner.get(Env.getCtx(), request.getId());
			if(businessPartner == null) {
				throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
			}
			if(businessPartner.getC_BPartner_ID() == pos.getC_BPartnerCashTrx_ID()) {
				throw new AdempiereException("@POS.ModifyTemplateCustomerNotAllowed@");
			}
			businessPartner.set_TrxName(transactionName);
			//	Set Value
			Optional.ofNullable(request.getValue()).ifPresent(value -> businessPartner.setValue(value));
			//	Tax Id
			Optional.ofNullable(request.getTaxId()).ifPresent(value -> businessPartner.setTaxID(value));
			//	Duns
			Optional.ofNullable(request.getDuns()).ifPresent(value -> businessPartner.setDUNS(value));
			//	Naics
			Optional.ofNullable(request.getNaics()).ifPresent(value -> businessPartner.setNAICS(value));
			//	Name
			Optional.ofNullable(request.getName()).ifPresent(value -> businessPartner.setName(value));
			//	Last name
			Optional.ofNullable(request.getLastName()).ifPresent(value -> businessPartner.setName2(value));
			//	Description
			Optional.ofNullable(request.getDescription()).ifPresent(value -> businessPartner.setDescription(value));
			//	Additional attributes
			CustomerUtil.setAdditionalAttributes(
				businessPartner,
				request.getAdditionalAttributes().getFieldsMap()
			);
			//	Save it
			businessPartner.saveEx(transactionName);
			//	Location
			request.getAddressesList().forEach(address -> {
				int countryId = address.getCountryId();
				//	
				int regionId = address.getRegionId();
				String cityName = null;
				int cityId = address.getCityId();
				//	City Name
				if(!Util.isEmpty(address.getCityName())) {
					cityName = address.getCityName();
				}
				//	Validate it
				if(countryId > 0 || regionId > 0 || cityId > 0 || !Util.isEmpty(cityName)) {
					//	Find it
					Optional<MBPartnerLocation> maybeCustomerLocation = Arrays.asList(businessPartner.getLocations(true))
						.parallelStream()
						.filter(customerLocation -> {
							return customerLocation.getC_BPartner_Location_ID() == address.getId();
						})
						.findFirst()
					;
					if(maybeCustomerLocation.isPresent()) {
						MBPartnerLocation businessPartnerLocation = maybeCustomerLocation.get();
						MLocation location = businessPartnerLocation.getLocation(true);
						location.set_TrxName(transactionName);
						if(countryId > 0) {
							location.setC_Country_ID(countryId);
						}
						if(regionId > 0) {
							location.setC_Region_ID(regionId);
						}
						if(cityId > 0) {
							location.setC_City_ID(cityId);
						}
						Optional.ofNullable(cityName).ifPresent(city -> location.setCity(city));
						//	Address
						Optional.ofNullable(address.getAddress1()).ifPresent(addressValue -> location.setAddress1(addressValue));
						Optional.ofNullable(address.getAddress2()).ifPresent(addressValue -> location.setAddress2(addressValue));
						Optional.ofNullable(address.getAddress3()).ifPresent(addressValue -> location.setAddress3(addressValue));
						Optional.ofNullable(address.getAddress4()).ifPresent(addressValue -> location.setAddress4(addressValue));
						Optional.ofNullable(address.getPostalCode()).ifPresent(postalCode -> location.setPostal(postalCode));
						//	Save
						location.saveEx(transactionName);
						//	Update business partner location
						businessPartnerLocation.setIsBillTo(address.getIsDefaultBilling());
						businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling, address.getIsDefaultBilling());
						businessPartnerLocation.setIsShipTo(address.getIsDefaultShipping());
						businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping, address.getIsDefaultShipping());
						Optional.ofNullable(address.getContactName()).ifPresent(contact -> businessPartnerLocation.setContactPerson(contact));
						Optional.ofNullable(address.getLocationName()).ifPresent(locationName -> businessPartnerLocation.setName(locationName));
						Optional.ofNullable(address.getEmail()).ifPresent(email -> businessPartnerLocation.setEMail(email));
						Optional.ofNullable(address.getPhone()).ifPresent(phome -> businessPartnerLocation.setPhone(phome));
						Optional.ofNullable(address.getDescription()).ifPresent(description -> businessPartnerLocation.setDescription(description));
						//	Additional attributes
						CustomerUtil.setAdditionalAttributes(
							businessPartnerLocation,
							address.getAdditionalAttributes().getFieldsMap()
						);
						businessPartnerLocation.saveEx(transactionName);
						//	Contact
						AtomicReference<MUser> contactReference = new AtomicReference<MUser>(
							CustomerUtil.getOfBusinessPartnerLocation(businessPartnerLocation, transactionName)
						);
						if(contactReference.get() == null
								|| contactReference.get().getAD_User_ID() <= 0) {
							contactReference.set(new MUser(businessPartner));
						}
						if(!Util.isEmpty(address.getContactName()) || !Util.isEmpty(address.getEmail()) || !Util.isEmpty(address.getPhone())) {
							MUser contact = contactReference.get();
							Optional.ofNullable(address.getEmail()).ifPresent(email -> contact.setEMail(email));
							Optional.ofNullable(address.getPhone()).ifPresent(phome -> contact.setPhone(phome));
							Optional.ofNullable(address.getDescription()).ifPresent(description -> contact.setDescription(description));
							String contactName = address.getContactName();
							if(Util.isEmpty(contactName)) {
								contactName = address.getEmail();
							}
							if(Util.isEmpty(contactName)) {
								contactName = address.getPhone();
							}
							contact.setName(contactName);
							//	Save
							contact.setC_BPartner_Location_ID(businessPartnerLocation.getC_BPartner_Location_ID());
							contact.saveEx(transactionName);
				 		}
					} else {
						//	Create new
						Optional<MBPartnerLocation> maybeTemplateLocation = Arrays.asList(businessPartner.getLocations(false)).stream().findFirst();
						if(!maybeTemplateLocation.isPresent()) {
							throw new AdempiereException("@C_BPartnerCashTrx_ID@ @C_BPartner_Location_ID@ @NotFound@");
						}
						//	Get location from template
						MLocation templateLocation = maybeTemplateLocation.get().getLocation(false);
						if(templateLocation == null || templateLocation.getC_Location_ID() <= 0) {
							throw new AdempiereException("@C_Location_ID@ @NotFound@");
						}
						createCustomerAddress(businessPartner, address, templateLocation, transactionName);
					}
					customer.set(businessPartner);
				}
			});
		});
		//	Default return
		return CustomerConvertUtil.convertCustomer(
			customer.get()
		);
	}



	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCustomerTemplatesResponse.Builder listCustomerTemplates(ListCustomerTemplatesRequest request) {		
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		ListCustomerTemplatesResponse.Builder builderList = ListCustomerTemplatesResponse.newBuilder();

		final String TABLE_NAME = "C_POSBPTemplate";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			// table not found
			return builderList;
		}

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		// Add pos filter
		whereClause.append("C_POS_ID = ? ");
		parameters.add(
			pos.getC_POS_ID()
		);

		// whereClause.append(
		// 	"AND EXISTS("
		// 	+ "SELECT 1 FROM C_BPartner AS bp "
		// 	+ "WHERE bp.C_BPartner_ID = C_POSBPTemplate.C_BPartner_ID "
		// 	+ "AND bp.IsActive = ? "
		// 	+ ")"
		// );
		// parameters.add(true);

		//	Get Customer Tempates list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		builderList
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list()
			.stream()
			.forEach(posCustomerTemplate -> {
				CustomerTemplate.Builder customerTemplateBuilder = CustomerConvertUtil.convertCustomerTemplate(
					posCustomerTemplate
				);
				builderList.addCustomerTemplates(customerTemplateBuilder);
			})
		;
	
		//	Default return
		return builderList;
	}

}
