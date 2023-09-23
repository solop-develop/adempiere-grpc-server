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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.spin.base.util.ValueUtil;

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
			pstmt.setString(index, ((Boolean) value) ? "Y" : "N");
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
	public static void setParameterFromValue(PreparedStatement pstmt, Value value, int index) throws SQLException {
		if(ValueUtil.getObjectFromValue(value) instanceof Integer) {
			pstmt.setInt(index, ValueUtil.getIntegerFromValue(value));
		} else if(ValueUtil.getObjectFromValue(value) instanceof BigDecimal) {
			pstmt.setBigDecimal(index, ValueUtil.getDecimalFromValue(value));
		} else if(ValueUtil.getObjectFromValue(value) instanceof Boolean) {
			pstmt.setBoolean(index, ValueUtil.getBooleanFromValue(value));
		} else if(ValueUtil.getObjectFromValue(value) instanceof String) {
			pstmt.setString(index, ValueUtil.getStringFromValue(value));
		} else if(ValueUtil.getObjectFromValue(value) instanceof Timestamp) {
			pstmt.setTimestamp(index, ValueUtil.getDateFromValue(value));
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
			sqlValue = " '" + ValueUtil.booleanToString((Boolean) value, false) + "' ";
		} else if(value instanceof Timestamp) {
			sqlValue = DB.TO_DATE((Timestamp) value, displayType == DisplayType.Date);
		}
		return sqlValue;
	}

}
