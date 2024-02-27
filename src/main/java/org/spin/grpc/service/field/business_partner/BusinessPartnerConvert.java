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
package org.spin.grpc.service.field.business_partner;

import org.adempiere.core.domains.models.X_C_Greeting;
import org.compiere.model.MBPGroup;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerAddressLocation;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerContact;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerInfo;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

public class BusinessPartnerConvert {

	public static BusinessPartnerInfo.Builder convertBusinessPartner(int businessPartnerId) {
		BusinessPartnerInfo.Builder builder = BusinessPartnerInfo.newBuilder();
		if (businessPartnerId <= 0) {
			return builder;
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(
			businessPartner
		);
	}

	public static BusinessPartnerInfo.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartnerInfo.Builder builder = BusinessPartnerInfo.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}
		MBPGroup businessPartneGroup = MBPGroup.get(Env.getCtx(), businessPartner.getC_BP_Group_ID());

		builder.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					businessPartner.getUUID()
				)
			)
			.setDisplayValue(
				ValueManager.validateNull(
					businessPartner.getDisplayValue()
				)
			)
			.setValue(
				ValueManager.validateNull(
					businessPartner.getValue()
				)
			)
			.setTaxId(
				ValueManager.validateNull(
					businessPartner.getTaxID()
				)
			)
			.setName(
				ValueManager.validateNull(
					businessPartner.getName()
				)
			)
			.setName2(
				ValueManager.validateNull(
					businessPartner.getName2()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					businessPartner.getDescription()
				)
			)
			.setBusinessPartnerGroup(
				ValueManager.validateNull(
					businessPartneGroup.getName()
				)
			)
			.setOpenBalanceAmount(
				NumberManager.getBigDecimalToString(
					businessPartner.getTotalOpenBalance()
				)
			)
			.setCreditAvailableAmount(
				NumberManager.getBigDecimalToString(
					businessPartner.getSO_CreditLimit().subtract(
						businessPartner.getSO_CreditUsed()
					)
				)
			)
			.setCreditUsedAmount(
				NumberManager.getBigDecimalToString(
					businessPartner.getSO_CreditUsed()
				)
			)
			.setRevenueAmount(
				NumberManager.getBigDecimalToString(
					businessPartner.getActualLifeTimeValue()
				)
			)
			.setIsActive(
				businessPartner.isActive()
			)
		;
		return builder;
	}



	public static BusinessPartnerContact.Builder convertBusinessPartnerContact(int businessPartnerContactId) {
		BusinessPartnerContact.Builder builder = BusinessPartnerContact.newBuilder();
		if (businessPartnerContactId <= 0) {
			return builder;
		}

		MUser businessPartnerContact = MUser.get(Env.getCtx(), businessPartnerContactId);
		return convertBusinessPartnerContact(
			businessPartnerContact
		);
	}

	public static BusinessPartnerContact.Builder convertBusinessPartnerContact(MUser businessPartnerContact) {
		BusinessPartnerContact.Builder builder = BusinessPartnerContact.newBuilder();
		if (businessPartnerContact == null || businessPartnerContact.getAD_User_ID() <= 0) {
			return builder;
		}
		String greetingName = null;
		if (businessPartnerContact.getC_Greeting_ID() > 0) {
			X_C_Greeting greating = new X_C_Greeting(Env.getCtx(), businessPartnerContact.getC_Greeting_ID(), null);
			if (greating != null && greating.getC_Greeting_ID() > 0) {
				greetingName = greating.getName();
			}
		}
		String locationName = null;
		if (businessPartnerContact.getC_Location_ID() > 0) {
			MLocation location = MLocation.get(Env.getCtx(), businessPartnerContact.getC_Location_ID(), null);
			if (location != null && location.getC_Location_ID() > 0) {
				locationName = location.getDisplayValue();
			}
		}

		builder.setId(
				businessPartnerContact.getAD_User_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					businessPartnerContact.getUUID()
				)
			)
			.setGreeting(
				ValueManager.validateNull(
					greetingName
				)
			)
			.setName(
				ValueManager.validateNull(
					businessPartnerContact.getName()
				)
			)
			.setTitle(
				ValueManager.validateNull(
					businessPartnerContact.getTitle()
				)
			)
			.setAddress(
				ValueManager.validateNull(
					locationName
				)
			)
			.setPhone(
				ValueManager.validateNull(
					businessPartnerContact.getPhone()
				)
			)
			.setPhone2(
				ValueManager.validateNull(
					businessPartnerContact.getPhone2()
				)
			)
			.setFax(
				ValueManager.validateNull(
					businessPartnerContact.getFax()
				)
			)
			.setEmail(
				ValueManager.validateNull(
					businessPartnerContact.getEMail()
				)
			)
			.setLastContact(
				ValueManager.getTimestampFromDate(
					businessPartnerContact.getLastContact()
				)
			)
			.setLastResult(
				ValueManager.validateNull(
					businessPartnerContact.getLastResult()
				)
			)
			.setIsActive(
				businessPartnerContact.isActive()
			)
		;
		return builder;
	}



	public static BusinessPartnerAddressLocation.Builder convertBusinessPartnerLocationAddress(int businessPartnerLocationId) {
		BusinessPartnerAddressLocation.Builder builder = BusinessPartnerAddressLocation.newBuilder();
		if (businessPartnerLocationId <= 0) {
			return builder;
		}

		MBPartnerLocation businessPartnerLocation = new MBPartnerLocation(Env.getCtx(), businessPartnerLocationId, null);
		return convertBusinessPartnerLocationAddress(
			businessPartnerLocation
		);
	}

	public static BusinessPartnerAddressLocation.Builder convertBusinessPartnerLocationAddress(MBPartnerLocation businessPartnerLocation) {
		BusinessPartnerAddressLocation.Builder builder = BusinessPartnerAddressLocation.newBuilder();
		if (businessPartnerLocation == null || businessPartnerLocation.getC_BPartner_Location_ID() <= 0) {
			return builder;
		}
		String locationName = null;
		if (businessPartnerLocation.getC_Location_ID() > 0) {
			MLocation location = MLocation.get(Env.getCtx(), businessPartnerLocation.getC_Location_ID(), null);
			if (location != null && location.getC_Location_ID() > 0) {
				locationName = location.toString();
			}
		}

		builder.setId(
				businessPartnerLocation.getC_BPartner_Location_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					businessPartnerLocation.getUUID()
				)
			)
			.setPhone(
				ValueManager.validateNull(
					businessPartnerLocation.getPhone()
				)
			)
			.setPhone2(
				ValueManager.validateNull(
					businessPartnerLocation.getPhone2()
				)
			)
			.setFax(
				ValueManager.validateNull(
					businessPartnerLocation.getFax()
				)
			)
			.setAddress(
				ValueManager.validateNull(
					locationName
				)
			)
			.setIsShipToAddress(
				businessPartnerLocation.isShipTo()
			)
			.setIsBillToAddress(
				businessPartnerLocation.isBillTo()
			)
			.setIsRemitToAddress(
				businessPartnerLocation.isRemitTo()
			)
			.setIsPayFormAddress(
				businessPartnerLocation.isPayFrom()
			)
			.setIsActive(
				businessPartnerLocation.isActive()
			)
		;
		return builder;
	}

}
