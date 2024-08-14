/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.base.util;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;

/**
 * A helper class for Lookups
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class LookupUtil {

	/**	Key column constant	*/
	public final static String KEY_COLUMN_KEY = "KeyColumn";
	/**	Uuid column constant	*/
	public final static String UUID_COLUMN_KEY = "UUID";
	/**	Value column constant	*/
	public final static String VALUE_COLUMN_KEY = "ValueColumn";
	/**	Display column constant	*/
	public final static String DISPLAY_COLUMN_KEY = "DisplayColumn";
	/**	To column constant	*/
	public final static String To_COLUMN_KEY = "To";
	
	
	/**
	 * New instance for it
	 * @return
	 */
	public static LookupUtil newInstance() {
		return new LookupUtil();
	}


	/**
	 * Convert Values from result
	 * @param keyValue
	 * @param uuidValue
	 * @param value
	 * @param displayValue
	 * @param isActive
	 * @return
	 */
	public static LookupItem.Builder convertObjectFromResult(Object keyValue, String uuidValue, String value, String displayValue, boolean isActive) {
		Struct.Builder values = Struct.newBuilder();

		LookupItem.Builder builder = LookupItem.newBuilder()
			.setValues(values)
			.setIsActive(isActive)
		;
		if(keyValue == null) {
			return builder;
		}

		// Key Column
		if(keyValue instanceof Integer) {
			builder.setId((Integer) keyValue);
			values.putFields(
				KEY_COLUMN_KEY,
				ValueManager.getValueFromInteger((Integer) keyValue).build()
			);
		} else {
			values.putFields(
				KEY_COLUMN_KEY,
				ValueManager.getValueFromString((String) keyValue).build()
			);
		}
		//	Set Value
		if(!Util.isEmpty(value)) {
			values.putFields(
				VALUE_COLUMN_KEY,
				ValueManager.getValueFromString(value).build()
			);
		}
		//	Display column
		if(!Util.isEmpty(displayValue)) {
			values.putFields(
				DISPLAY_COLUMN_KEY,
				ValueManager.getValueFromString(displayValue).build()
			);
		}
		// UUID Value
		values.putFields(
			LookupUtil.UUID_COLUMN_KEY,
			ValueManager.getValueFromString(uuidValue).build()
		);

		builder.setValues(values);
		return builder;
	}


	/**
	 * Get Lookup Item from Ref List
	 * @param {MRefList} refList
	 * @return {LookupItem.Builder} builder
	 */
	public static LookupItem.Builder convertLookupItemFromReferenceList(MRefList refList) {
		LookupItem.Builder builder = LookupItem.newBuilder();
		Struct.Builder values = Struct.newBuilder();
		if (refList == null || refList.getAD_Ref_List_ID() <= 0) {
			builder.setValues(values);
			return builder;
		}

		builder.setId(refList.getAD_Ref_List_ID())
			.setTableName(I_AD_Ref_List.Table_Name)
		;

		// Key Column
		values.putFields(
			KEY_COLUMN_KEY,
			ValueManager.getValueFromString(refList.getValue()).build()
		);

		//	Value
		values.putFields(
			LookupUtil.VALUE_COLUMN_KEY,
			ValueManager.getValueFromString(refList.getValue()).build()
		);

		//	Display column
		String name = refList.getName();
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			// set translated values
			name = refList.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
		}
		values.putFields(
			LookupUtil.DISPLAY_COLUMN_KEY,
			ValueManager.getValueFromString(name).build()
		);

		// UUID Value
		values.putFields(
			LookupUtil.UUID_COLUMN_KEY,
			ValueManager.getValueFromString(refList.getUUID()).build()
		);

		builder.setValues(values);
		return builder;
	}


	/**
	 * Get display column name
	 * @param columnName
	 * @return example DisplayColumn_AD_Client_ID
	 */
	public static String getDisplayColumnName(String columnName) {
		return LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName;
	}

	/**
	 * Get uuid column name
	 * @param columnName
	 * @return example AD_Client_ID_UUID
	 */
	public static String getUuidColumnName(String columnName) {
		return columnName + "_" + LookupUtil.UUID_COLUMN_KEY;
	}

	/**
	 * Get to range column name
	 * @param columnName
	 * @return example AD_Client_ID_To
	 */
	public static String getToColumnName(String columnName) {
		return columnName + "_" + LookupUtil.To_COLUMN_KEY;
	}

}
