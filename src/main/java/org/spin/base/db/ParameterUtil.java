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
package org.spin.base.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Value;

public class ParameterUtil {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(ParameterUtil.class);


	/**
	 * Set DB Parameter from Objects list
	 * @param pstmt
	 * @param parametersList
	 */
	public static void setParametersFromObjectsList(PreparedStatement pstmt, List<Object> parametersList) {
		try {
			AtomicInteger parameterIndex = new AtomicInteger(1);
			for(Object parameter : parametersList) {
				ParameterUtil.setParameterFromObject(pstmt, parameter, parameterIndex.getAndIncrement());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			throw new AdempiereException(e);
		}
	}

	/**
	 * Set Parameter for Statement from object
	 * @param pstmt
	 * @param value
	 * @param index
	 * @throws SQLException
	 */
	public static void setParameterFromObject(PreparedStatement pstmt, Object value, int index) throws SQLException {
		if(value instanceof Integer) {
			pstmt.setInt(index, (Integer) value);
		} else if(value instanceof Double) {
			pstmt.setDouble(index, (Double) value);
		} else if(value instanceof Long) {
			pstmt.setLong(index, (Long) value);
		} else if(value instanceof BigDecimal) {
			pstmt.setBigDecimal(index, (BigDecimal) value);
		} else if(value instanceof String) {
			pstmt.setString(index, (String) value);
		} else if(value instanceof Timestamp) {
			pstmt.setTimestamp(index, (Timestamp) value);
		} else if(value instanceof Date) {
			pstmt.setDate(index, (Date) value);
		} else if(value instanceof Boolean) {
			// pstmt.setString(index, ((Boolean) value) ? "Y" : "N");
			String boolValue = BooleanManager.getBooleanToString((Boolean) value);
			pstmt.setString(index, boolValue);
		} else {
			pstmt.setObject(index, null);
		}
	}



	/**
	 * Set DB Parameter from Objects list
	 * @param pstmt
	 * @param parameters
	 */
	public static void setParametersFromValuesList(PreparedStatement pstmt, List<Value> parametersList) {
		try {
			AtomicInteger parameterIndex = new AtomicInteger(1);
			for(Value parameter : parametersList) {
				ParameterUtil.setParameterFromValue(pstmt, parameter, parameterIndex.getAndIncrement());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			throw new AdempiereException(e);
		}
	}

	/**
	 * Set Parameter for Statement from value
	 * @param pstmt
	 * @param value
	 * @param index
	 * @throws SQLException
	 */
	public static void setParameterFromValue(PreparedStatement pstmt, Value grpcValue, int index) throws SQLException {
		Object value = ValueManager.getObjectFromValue(grpcValue);
		if(value instanceof Integer) {
			pstmt.setInt(index, ValueManager.getIntegerFromValue(grpcValue));
		} else if(value instanceof BigDecimal) {
			pstmt.setBigDecimal(index, ValueManager.getBigDecimalFromValue(grpcValue));
		} else if(value instanceof Boolean) {
			// pstmt.setBoolean(index, ValueManager.getBooleanFromValue(grpcValue));
			String boolValue = BooleanManager.getBooleanToString((Boolean) value);
			pstmt.setString(index, boolValue);
		} else if(value instanceof String) {
			pstmt.setString(index, ValueManager.getStringFromValue(grpcValue));
		} else if(value instanceof Timestamp) {
			pstmt.setTimestamp(index, TimeManager.getTimestampFromObject(grpcValue));
		} else {
			pstmt.setObject(index, value);
		}
	}


	/**
	 * Get DB Value to match
	 * @param value
	 * @param displayType
	 * @return
	 */
	public static String getDBValue(Object value, int displayType) {
		String sqlValue = "";
		if(value instanceof BigDecimal) {
			sqlValue = DB.TO_NUMBER((BigDecimal) value, displayType);
		} else if (value instanceof Integer) {
			sqlValue = value.toString();
		} else if (value instanceof String) {
			sqlValue = value.toString();
		} else if (value instanceof Boolean) {
			sqlValue = " '" + BooleanManager.getBooleanToString((Boolean) value, false) + "' ";
		} else if(value instanceof Timestamp) {
			sqlValue = DB.TO_DATE((Timestamp) value, displayType == DisplayType.Date);
		}
		return sqlValue;
	}

	public static ArrayList<Object> getParametersFromKeyColumns(String[] keyColumns, Map<String, Value> attributes) {
		ArrayList<Object> parametersList = new ArrayList<Object>();

		if (keyColumns != null && keyColumns.length > 0) {
			for (String columnName: keyColumns) {
				Value value = attributes.get(columnName);
				if (value != null) {
					parametersList.add(
						ValueManager.getObjectFromValue(value)
					);
					attributes.remove(columnName);
				}
			}
		}

		return parametersList;
	}


	/**
	 * Get value based on filter url (all is instanceof `String` data type)
	 * @param displayTypeId
	 * @param value
	 * @return
	 */
	public static Object getValueToFilterRestriction(int displayTypeId, Object value) {
		Object transformValue = value;
		if (value == null) {
			return transformValue;
		}
		if (DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			transformValue = NumberManager.getIntegerFromObject(
				value
			);
		} else if (DisplayType.isNumeric(displayTypeId)) {
			transformValue = NumberManager.getBigDecimalFromObject(
				value
			);
		} else if (DisplayType.YesNo == displayTypeId) {
			if (value instanceof String) {
				transformValue = BooleanManager.getBooleanFromString(
					(String) value
				);
			}
		} else if (DisplayType.isDate(displayTypeId)) {
			transformValue = TimeManager.getTimestampFromObject(
				value
			);
		} else if (DisplayType.isText(displayTypeId) || DisplayType.List == displayTypeId) {
			transformValue = (String) value;
		} else {
			transformValue = (String) value;
		}

		return transformValue;
	}

}
