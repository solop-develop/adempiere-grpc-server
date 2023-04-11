/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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
package org.spin.base.util;

/**
 * Services
 * @author Yamel Senih
 */
public enum Services {
	BUSINESS("business"),
	BUSINESS_PARTNER("business_partner"),
    CORE("core"),
	DASHBOARDING("dashboarding"),
	DICTIONARY("dictionary"),
	ENROLLMENT("enrollment"),
	EXPRESS_MOVEMENT("express_movement"),
	EXPRESS_RECEIPT("express_receipt"),
	EXPRESS_SHIPMENT("express_shipment"),
	FILE_MANAGEMENT("file_management"),
	GENERAL_LEDGER("general_ledger"),
	IN_OUT("in_out"),
	INVOICE("invoice"),
	ISSUE_MANAGEMENT("issue_management"),
	LOG("log"),
	MATERIAL_MANAGEMENT("material_management"),
    ORDER("order"),
    PAYMENT("payment"),
	PAYMENT_PTINT_EXPORT("payment_print_export"),
    PAYROLL_ACTION_NOTICE("payroll_action_notice"),
	POS("pos"),
	PRODUCT("product"),
	SECURITY("security"),
	STORE("store"),
	TIME_CONTROL("time_control"),
	TIME_RECORD("time_record"),
	USER_CUSTOMIZATION("user_customization"),
	UI("ui"),
	UPDATER("updater"),
	WORKFLOW("workflow");

	/**	Service Name	*/
    private final String serviceName;

    Services(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
