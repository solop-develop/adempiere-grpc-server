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
 * Copyright (C) 2012-2022 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.base.util;

import org.compiere.util.Util;
import org.spin.backend.grpc.common.LookupItem;

/**
 * A helper class for Lookups
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class LookupUtil {

	/**	Key column constant	*/
	public final static String KEY_COLUMN_KEY = "KeyColumn";
	/**	Key column constant	*/
	public final static String UUID_COLUMN_KEY = "UUID";
	/**	Key column constant	*/
	public final static String VALUE_COLUMN_KEY = "ValueColumn";
	/**	Key column constant	*/
	public final static String DISPLAY_COLUMN_KEY = "DisplayColumn";
	
	
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
	 * @return
	 */
	public static LookupItem.Builder convertObjectFromResult(Object keyValue, String uuidValue, String value, String displayValue) {
		LookupItem.Builder builder = LookupItem.newBuilder();
		if(keyValue == null) {
			return builder;
		}

		// Key Column
		if(keyValue instanceof Integer) {
			builder.setId((Integer) keyValue);
			builder.putValues(KEY_COLUMN_KEY, ValueUtil.getValueFromInteger((Integer) keyValue).build());
		} else {
			builder.putValues(KEY_COLUMN_KEY, ValueUtil.getValueFromString((String) keyValue).build());
		}
		//	Set Value
		if(!Util.isEmpty(value)) {
			builder.putValues(VALUE_COLUMN_KEY, ValueUtil.getValueFromString(value).build());
		}
		//	Display column
		if(!Util.isEmpty(displayValue)) {
			builder.putValues(DISPLAY_COLUMN_KEY, ValueUtil.getValueFromString(displayValue).build());
		}
		// UUID Value
		builder.setUuid(ValueUtil.validateNull(uuidValue));
		builder.putValues(LookupUtil.UUID_COLUMN_KEY, ValueUtil.getValueFromString(uuidValue).build());

		return builder;
	}

}
