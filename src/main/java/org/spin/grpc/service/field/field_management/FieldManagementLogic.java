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
package org.spin.grpc.service.field.field_management;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Browse_Field;
import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_Reference;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsRequest;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.field.DefaultValue;
import org.spin.backend.grpc.field.GetDefaultValueRequest;
import org.spin.backend.grpc.field.GetZoomParentRecordRequest;
import org.spin.backend.grpc.field.GetZoomParentRecordResponse;
import org.spin.backend.grpc.field.ListGeneralSearchRecordsRequest;
import org.spin.backend.grpc.field.ListZoomWindowsRequest;
import org.spin.backend.grpc.field.ListZoomWindowsResponse;
import org.spin.backend.grpc.field.ZoomWindow;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ReferenceUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;

public class FieldManagementLogic {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(FieldManagementLogic.class);


	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static DefaultValue.Builder getDefaultValue(GetDefaultValueRequest request) {
		int referenceId = 0;
		int referenceValueId = 0;
		int validationRuleId = 0;
		String columnName = null;
		String defaultValue = null;

		if(request.getFieldId() > 0) {
			MField field = (MField) RecordUtil.getEntity(Env.getCtx(), I_AD_Field.Table_Name, request.getFieldId(), null);
			if(field == null || field.getAD_Field_ID() <= 0) {
				throw new AdempiereException("@AD_Field_ID@ @NotFound@");
			}
			defaultValue = field.getDefaultValue();
			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			//	Display Type
			referenceId = column.getAD_Reference_ID();
			referenceValueId = column.getAD_Reference_Value_ID();
			validationRuleId = column.getAD_Val_Rule_ID();
			columnName = column.getColumnName();
			if(field.getAD_Reference_ID() > 0) {
				referenceId = field.getAD_Reference_ID();
			}
			if(field.getAD_Reference_Value_ID() > 0) {
				referenceValueId = field.getAD_Reference_Value_ID();
			}
			if(field.getAD_Val_Rule_ID() > 0) {
				validationRuleId = field.getAD_Val_Rule_ID();
			}
			if(Util.isEmpty(defaultValue)
					&& !Util.isEmpty(column.getDefaultValue())) {
				defaultValue = column.getDefaultValue();
			}
		} else if(request.getBrowseFieldId() > 0) {
			MBrowseField browseField = (MBrowseField) RecordUtil.getEntity(
				Env.getCtx(),
				I_AD_Browse_Field.Table_Name,
				request.getBrowseFieldId(),
				null
			);
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				throw new AdempiereException("@AD_Browse_Field_ID@ @NotFound@");
			}
			defaultValue = browseField.getDefaultValue();
			referenceId = browseField.getAD_Reference_ID();
			referenceValueId = browseField.getAD_Reference_Value_ID();
			validationRuleId = browseField.getAD_Val_Rule_ID();
			MViewColumn viewColumn = browseField.getAD_View_Column();
			if(viewColumn.getAD_Column_ID() > 0) {
				MColumn column = MColumn.get(Env.getCtx(), viewColumn.getAD_Column_ID());
				columnName = column.getColumnName();
				if(Util.isEmpty(defaultValue)
						&& !Util.isEmpty(column.getDefaultValue())) {
					defaultValue = column.getDefaultValue();
				}
			} else {
				columnName = browseField.getAD_Element().getColumnName();
			}
		} else if(request.getBrowseFieldToId() > 0) {
			MBrowseField browseField = (MBrowseField) RecordUtil.getEntity(
				Env.getCtx(),
				I_AD_Browse_Field.Table_Name,
				request.getBrowseFieldToId(),
				null
			);
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				throw new AdempiereException("@AD_Browse_Field_ID@ @NotFound@");
			}
			defaultValue = browseField.getDefaultValue2(); // value to
			referenceId = browseField.getAD_Reference_ID();
			referenceValueId = browseField.getAD_Reference_Value_ID();
			validationRuleId = browseField.getAD_Val_Rule_ID();
			MViewColumn viewColumn = browseField.getAD_View_Column();
			if(viewColumn.getAD_Column_ID() > 0) {
				MColumn column = MColumn.get(Env.getCtx(), viewColumn.getAD_Column_ID());
				columnName = column.getColumnName();
				if(Util.isEmpty(defaultValue) && !Util.isEmpty(column.getDefaultValue())) {
					defaultValue = column.getDefaultValue();
				}
			} else {
				columnName = browseField.getAD_Element().getColumnName();
			}
		} else if(request.getProcessParameterId() > 0) {
			MProcessPara processParameter = (MProcessPara) RecordUtil.getEntity(Env.getCtx(), I_AD_Process_Para.Table_Name, request.getProcessParameterId(), null);
			if(processParameter == null || processParameter.getAD_Process_Para_ID() <= 0) {
				throw new AdempiereException("@AD_Process_Para_ID@ @NotFound@");
			}
			referenceId = processParameter.getAD_Reference_ID();
			referenceValueId = processParameter.getAD_Reference_Value_ID();
			validationRuleId = processParameter.getAD_Val_Rule_ID();
			columnName = processParameter.getColumnName();
			defaultValue = processParameter.getDefaultValue();
		} else if(request.getProcessParameterToId() > 0) {
			MProcessPara processParameter = (MProcessPara) RecordUtil.getEntity(Env.getCtx(), I_AD_Process_Para.Table_Name, request.getProcessParameterToId(), null);
			if(processParameter == null || processParameter.getAD_Process_Para_ID() <= 0) {
				throw new AdempiereException("@AD_Process_Para_ID@ @NotFound@");
			}
			referenceId = processParameter.getAD_Reference_ID();
			referenceValueId = processParameter.getAD_Reference_Value_ID();
			validationRuleId = processParameter.getAD_Val_Rule_ID();
			columnName = processParameter.getColumnName();
			defaultValue = processParameter.getDefaultValue2(); // value to
		} else if(request.getColumnId() > 0) {
			MColumn column = MColumn.get(Env.getCtx(), request.getColumnId());
			if(column == null || column.getAD_Column_ID() <= 0) {
				throw new AdempiereException("@AD_Column_ID@ @NotFound@");
			}
			referenceId = column.getAD_Reference_ID();
			referenceValueId = column.getAD_Reference_Value_ID();
			validationRuleId = column.getAD_Val_Rule_ID();
			columnName = column.getColumnName();
			defaultValue = column.getDefaultValue();
		} else if (!Util.isEmpty(request.getTableName(), true) && !Util.isEmpty(request.getColumnName(), true)) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if(table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@TableName@ @NotFound@");
			}
			MColumn column = table.getColumn(request.getColumnName());
			if (column == null || column.getAD_Column_ID() <= 0) {
				throw new AdempiereException("@ColumnName@ @NotFound@");
			}
			referenceId = column.getAD_Reference_ID();
			referenceValueId = column.getAD_Reference_Value_ID();
			validationRuleId = column.getAD_Val_Rule_ID();
			columnName = column.getColumnName();
			defaultValue = column.getDefaultValue();
		} else {
			throw new AdempiereException(
				"@AD_Reference_ID@ / @AD_Column_ID@ / @AD_Table_ID@ / @AD_Field_ID@ / @AD_Process_Para_ID@ / @AD_Browse_Field_ID@ / @IsMandatory@"
			);
		}

		// overwrite default value with user value request
		if (Optional.ofNullable(request.getValue()).isPresent()
			&& !Util.isEmpty(request.getValue().getStringValue())) {
			// URL decode to change characteres
			final String overwriteValue = request.getValue().getStringValue();
			defaultValue = overwriteValue;
		}

		//	Validate SQL
		DefaultValue.Builder builder = getDefaultKeyAndValue(
			request.getContextAttributes(),
			defaultValue,
			referenceId,
			referenceValueId,
			columnName,
			validationRuleId
		);
		return builder;
	}

	/**
	 * Get Default value, also convert it to lookup value if is necessary
	 * @param contextAttributes
	 * @param defaultValue
	 * @param referenceId
	 * @param referenceValueId
	 * @param columnName
	 * @param validationRuleId
	 * @return
	 */
	private static DefaultValue.Builder getDefaultKeyAndValue(String contextAttributes, String defaultValue, int displayTypeId, int referenceValueId, String columnName, int validationRuleId) {
		Struct.Builder values = Struct.newBuilder();
		DefaultValue.Builder builder = DefaultValue.newBuilder()
			.setValues(values)
		;
		if(Util.isEmpty(defaultValue, true)) {
			return builder;
		}
		Object defaultValueAsObject = null;

		// Fill context
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, context, contextAttributes, true
		);

		if(defaultValue.trim().startsWith("@SQL=")) {
			String sqlDefaultValue = defaultValue.replace("@SQL=", "");
			sqlDefaultValue = Env.parseContext(context, windowNo, sqlDefaultValue, false);
			if (Util.isEmpty(sqlDefaultValue, true)) {
				log.warning("@SQL@ @Unparseable@ " + sqlDefaultValue);
				return builder;
			}
			defaultValueAsObject = getDefaultValueFromSQL(sqlDefaultValue);
		} else {
			defaultValueAsObject = Env.parseContext(context, windowNo, defaultValue, false);
		}
		//	 For lookups
		if(defaultValueAsObject == null) {
			return builder;
		}

		//	Convert value from type
		if (DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			Integer integerValue = NumberManager.getIntegerFromObject(defaultValueAsObject);
			if (integerValue == null && defaultValueAsObject != null
				&& (DisplayType.Search == displayTypeId || DisplayType.Table == displayTypeId)) {
					// EntityType, AD_Language columns
				;
			} else {
				defaultValueAsObject = integerValue;
			}
		} else if (DisplayType.isNumeric(displayTypeId)) {
			defaultValueAsObject = NumberManager.getIntegerFromObject(defaultValueAsObject);
		}
		if (ReferenceUtil.validateReference(displayTypeId) || DisplayType.Button == displayTypeId) {
			if (displayTypeId == DisplayType.Button && referenceValueId > 0) {
				//	Reference Value
				X_AD_Reference reference = new X_AD_Reference(Env.getCtx(), referenceValueId, null);
				if (reference != null && reference.getAD_Reference_ID() > 0) {
					// overwrite display type to Table or List
					if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
						displayTypeId = DisplayType.Table;
					} else {
						displayTypeId = DisplayType.List;
					}
				}
			}

			if (DisplayType.List == displayTypeId) {
				// (') (text) (') or (") (text) (")
				String singleQuotesPattern = "('|\")(\\w+)('|\")";
				// columnName = value
				Pattern pattern = Pattern.compile(
					singleQuotesPattern,
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL
				);
				Matcher matcherSingleQuotes = pattern
					.matcher(String.valueOf(defaultValueAsObject));
				// remove single quotation mark 'DR' -> DR, "DR" -> DR
				String defaultValueList = matcherSingleQuotes.replaceAll("$2");

				MRefList referenceList = MRefList.get(context, referenceValueId, defaultValueList, null);
				if (referenceList == null) {
					log.fine(Msg.parseTranslation(context, "@AD_Ref_List_ID@ @NotFound@") + ": " + defaultValueList);
					return builder;
				}
				builder = FieldManagementConvert.convertDefaultValue(
					referenceList.getValue(),
					referenceList.getUUID(),
					referenceList.getValue(),
					referenceList.get_Translation(MRefList.COLUMNNAME_Name),
					referenceList.isActive()
				);
				builder.setId(referenceList.getAD_Ref_List_ID());
			} else {
				if (DisplayType.Button == displayTypeId) {
					if (columnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
						defaultValueAsObject = Integer.valueOf(defaultValueAsObject.toString());
						int tableId = Env.getContextAsInt(context, windowNo, I_AD_Table.COLUMNNAME_AD_Table_ID);
						MTable table = MTable.get(context, tableId);
						String tableKeyColumn = table.getTableName() + "_ID";
						columnName = tableKeyColumn;
						// overwrite display type to Table Direct
						displayTypeId = DisplayType.TableDir;
					} else {
						values.putFields(
							columnName,
							ValueManager.getValueFromObject(defaultValueAsObject).build()
						);
						builder.setValues(values);
						return builder;
					}
				}

				MLookupInfo lookupInfo = ReferenceUtil.getReferenceLookupInfo(
					displayTypeId,
					referenceValueId,
					columnName,
					validationRuleId
				);
				if(lookupInfo == null || Util.isEmpty(lookupInfo.QueryDirect, true)) {
					return builder;
				}
				final String sql = WhereClauseUtil.removeIsActiveRestriction(
					lookupInfo.TableName,
					lookupInfo.QueryDirect
				);
				// final String sql = MRole.getDefault(context, false).addAccessSQL(
				// 	lookupInfo.QueryDirect,
				// 	lookupInfo.TableName,
				// 	MRole.SQL_FULLYQUALIFIED,
				// 	MRole.SQL_RO
				// );
				PreparedStatement pstmt = null;
				ResultSet rs = null;
				try {
					//	SELECT Key, Value, Name FROM ...
					pstmt = DB.prepareStatement(sql.toString(), null);
					DB.setParameter(pstmt, 1, defaultValueAsObject);

					//	Get from Query
					rs = pstmt.executeQuery();
					if (rs.next()) {
						//	1 = Key Column
						//	2 = Optional Value
						//	3 = Display Value
						ResultSetMetaData metaData = rs.getMetaData();
						int keyValueType = metaData.getColumnType(1);
						Object keyValue = null;
						if(keyValueType == Types.CHAR
								|| keyValueType == Types.NCHAR
								|| keyValueType == Types.VARCHAR
								|| keyValueType == Types.LONGVARCHAR
								|| keyValueType == Types.NVARCHAR
								|| keyValueType == Types.LONGNVARCHAR) {
							keyValue = rs.getString(2);
						} else {
							keyValue = rs.getInt(1);
						}
						String uuid = null;
						//	Validate if exist UUID
						int uuidIndex = RecordUtil.getColumnIndex(
							metaData,
							I_AD_Element.COLUMNNAME_UUID
						);
						if(uuidIndex != -1) {
							uuid = rs.getString(uuidIndex);
						}
						//	
						builder = FieldManagementConvert.convertDefaultValue(
							keyValue,
							uuid,
							rs.getString(2),
							rs.getString(3),
							rs.getBoolean(
								I_AD_Element.COLUMNNAME_IsActive
							)
						);
					} else {
						log.severe("Direct Query default value without results");
					}
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
					e.printStackTrace();
					throw new AdempiereException(e);
				} finally {
					DB.close(rs, pstmt);
					rs = null;
					pstmt = null;
				}
			}
		} else {
			values.putFields(
				columnName,
				ValueManager.getValueFromObject(defaultValueAsObject).build()
			);
			builder.setValues(values);
		}

		return builder;
	}

	/**
	 * Get Default Value from sql direct query
	 * @param sql
	 * @return
	 */
	private static Object getDefaultValueFromSQL(String sql) {
		Object defaultValue = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			//	Get from Query
			rs = pstmt.executeQuery();
			if (rs.next()) {
				defaultValue = rs.getObject(1);
			} else {
				log.severe("SQL default value without results");
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//	Return values
		return defaultValue;
	}



	/**
	 * Convert Object Request to list
	 * @param request
	 * @return
	 */
	public static ListLookupItemsResponse.Builder listLookupItems(ListLookupItemsRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName(),
			request.getIsWithoutValidation()
		);
		if (reference == null) {
			throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		}

		return listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);
	}
	/**
	 * Convert Object to list
	 * @param MLookupInfo reference
	 * @param Map<String, Object> contextAttributes
	 * @param int pageSize
	 * @param String pageToken
	 * @param String searchValue
	 * @return
	 */
	public static ListLookupItemsResponse.Builder listLookupItems(MLookupInfo reference, String contextAttributes, int pageSize, String pageToken, String searchValue, boolean isOnlyActiveRecords) {
		if (reference == null) {
			throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		}

		//	Fill context
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, context, contextAttributes
		);

		String sql = reference.Query;
		sql = Env.parseContext(context, windowNo, sql, false);
		if(Util.isEmpty(sql, true)
				&& !Util.isEmpty(reference.Query, true)) {
			throw new AdempiereException("@AD_Reference_ID@ @WhereClause@ @Unparseable@");
		}

		// TODO: Fix with list document type
		// if (isOnlyActiveRecords) {
		// 	sql = WhereClauseUtil.addIsActiveRestriction(reference.TableName, sql);
		// }
		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				reference.TableName,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		;

		String sqlWithActiveRecords = sqlWithRoleAccess;
		if (isOnlyActiveRecords) {
			//	Order by
			String queryWithoutOrderBy = org.spin.service.grpc.util.db.OrderByUtil.removeOrderBy(sqlWithRoleAccess);
			String orderByClause = org.spin.service.grpc.util.db.OrderByUtil.getOnlyOrderBy(sqlWithRoleAccess);
	
			StringBuffer whereClause = new StringBuffer()
				.append(" AND ")
			;
			if (!Util.isEmpty(reference.TableName, true)) {
				whereClause.append(reference.TableName)
					.append(".")
				;
			}
			whereClause.append("IsActive = 'Y' ");

			sqlWithActiveRecords = queryWithoutOrderBy + whereClause.toString() + orderByClause;
		}

		List<Object> parameters = new ArrayList<>();
		String parsedSQL = RecordUtil.addSearchValueAndGet(
			sqlWithActiveRecords,
			reference.TableName,
			searchValue,
			parameters
		);

		//	Get page and count
		int count = CountUtil.countRecords(parsedSQL, reference.TableName, parameters);
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), pageToken);
		int limit = LimitUtil.getPageSize(pageSize);
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		ListLookupItemsResponse.Builder builder = ListLookupItemsResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, parameters);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				//	1 = Key Column
				//	2 = Optional Value
				//	3 = Display Value
				ResultSetMetaData metaData = rs.getMetaData();
				int keyValueType = metaData.getColumnType(1);
				Object keyValue = null;
				if(keyValueType == Types.VARCHAR
						|| keyValueType == Types.NVARCHAR
						|| keyValueType == Types.CHAR
						|| keyValueType == Types.NCHAR
						|| keyValueType == Types.OTHER) {
					keyValue = rs.getString(2);
				} else {
					keyValue = rs.getInt(1);
				}
				String uuid = null;
				//	Validate if exist UUID
				int uuidIndex = RecordUtil.getColumnIndex(
					metaData,
					I_AD_Element.COLUMNNAME_UUID
				);
				if(uuidIndex != -1) {
					uuid = rs.getString(uuidIndex);
				}
				//	
				LookupItem.Builder valueObject = LookupUtil.convertObjectFromResult(
					keyValue,
					uuid,
					rs.getString(2),
					rs.getString(3),
					rs.getBoolean(
						I_AD_Element.COLUMNNAME_IsActive
					)
				);
				valueObject.setTableName(
					ValueManager.validateNull(reference.TableName)
				);
				builder.addRecords(valueObject.build());
			}
		} catch (Exception e) {
			// log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
		//	
		builder.setRecordCount(count)
			.setNextPageToken(
				ValueManager.validateNull(nexPageToken)
			)
		;
		//	Return
		return builder;
	}



	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListEntitiesResponse.Builder listGeneralSearchRecords(ListGeneralSearchRecordsRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName(),
			request.getIsWithoutValidation()
		);

		final MTable table = RecordUtil.validateAndGetTable(
			reference.TableName
		);

		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, Env.getCtx(), request.getContextAttributes()
		);

		//
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

		// validate is active record
		if (request.getIsOnlyActiveRecords()) {
			if (table.getColumn("IsActive") != null) {
				String newSQL = WhereClauseUtil.addIsActiveRestriction(
					reference.TableName,
					sql.toString()
				);
				sql = new StringBuilder(newSQL);
			}
		}

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();

		// validation code of field
		if (!request.getIsWithoutValidation()) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				table.getTableName(),
				reference.ValidationCode
			);
			if (!Util.isEmpty(reference.ValidationCode, true)) {
				String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
				if (Util.isEmpty(parsedValidationCode, true)) {
					throw new AdempiereException(
						"@Reference@ " + reference.KeyColumn + ", @Code@/@WhereClause@ @Unparseable@"
					);
				}
				whereClause.append(" AND ").append(parsedValidationCode);
			}
		}

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(
			request.getFilters(),
			table.getTableName(),
			params
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")");
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(
			sqlWithRoleAccess,
			table.getTableName(),
			request.getSearchValue(),
			false,
			params
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//	Count records
		count = CountUtil.countRecords(
			parsedSQL,
			table.getTableName(),
			params
		);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(
			table,
			parsedSQL,
			params
		);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		return builder;
	}



	public static ListZoomWindowsResponse.Builder listZoomWindows(ListZoomWindowsRequest request) {
		Properties context = Env.getCtx();
		ListZoomWindowsResponse.Builder builderList = ListZoomWindowsResponse.newBuilder();

		MLookupInfo lookupInfo = ReferenceInfo.getInfoFromRequest(
			0,
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(), request.getTableName(),
			0, null
		);

		if (lookupInfo == null) {
			return builderList;
		}

		List<String> contextColumnsList = ContextManager.getContextColumnNames(
			lookupInfo.QueryDirect,
			lookupInfo.Query,
			lookupInfo.ValidationCode
		);

		MTable table = MTable.get(context, lookupInfo.TableName);
		List<String> keyColumnsList = Arrays.asList(
			table.getKeyColumns()
		);

		builderList.setTableName(
				ValueManager.validateNull(
					lookupInfo.TableName
				)
			)
			.setKeyColumnName(
				ValueManager.validateNull(
					lookupInfo.KeyColumn
				)
			)
			.addAllKeyColumns(
				keyColumnsList
			)
			.setDisplayColumnName(
				ValueManager.validateNull(
					lookupInfo.DisplayColumn
				)
			)
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		//	Window Reference
		if (lookupInfo.ZoomWindow > 0) {
			ZoomWindow.Builder windowSalesBuilder = FieldManagementConvert.convertZoomWindow(
				context,
				lookupInfo.ZoomWindow,
				lookupInfo.TableName
			);
			builderList.addZoomWindows(
				windowSalesBuilder.build()
			);
		}
		// window reference Purchase Order
		if (lookupInfo.ZoomWindowPO > 0) {
			ZoomWindow.Builder windowPurchaseBuilder = FieldManagementConvert.convertZoomWindow(
				context,
				lookupInfo.ZoomWindowPO,
				lookupInfo.TableName
			);
			builderList.addZoomWindows(
				windowPurchaseBuilder.build()
			);
		}

		return builderList;
	}


	public static GetZoomParentRecordResponse.Builder getZoomParentRecord(GetZoomParentRecordRequest request) {
		if (request.getWindowId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Window_ID@");
		}
		MWindow window = MWindow.get(
			Env.getCtx(),
			request.getWindowId()
		);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab currentTab = MTab.get(
			window.getCtx(),
			request.getTabId()
		);
		if (currentTab == null || currentTab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}
		MTable currentTable = MTable.get(Env.getCtx(), currentTab.getAD_Table_ID());
		String[] keys = currentTable.getKeyColumns();
		String currentKeyColumnName = currentTable.getTableName() + "_ID";
		if (keys != null && keys.length > 0) {
			currentKeyColumnName = keys[0];
		}
		MColumn currentKeycolumn = currentTable.getColumn(currentKeyColumnName);
		String currentLinkColumn = "";
		if (currentTab.getParent_Column_ID() > 0) {
			currentLinkColumn = MColumn.getColumnName(Env.getCtx(), currentTab.getParent_Column_ID());
		} else if (currentTab.getAD_Column_ID() > 0) {
			currentLinkColumn = MColumn.getColumnName(Env.getCtx(), currentTab.getParent_Column_ID());
		}

		List<MTab> tabsList = Arrays.asList(
				window.getTabs(false, null)
			)
			.stream()
			.filter(tabItem -> {
				return tabItem.isActive();
			})
			.collect(Collectors.toList())
		;
		MTab parentTab = tabsList.stream()
			.filter(tab -> {
				return tab.getTabLevel() == 0;
			})
			.sorted(
				Comparator.comparing(MTab::getSeqNo)
					.thenComparing(MTab::getTabLevel)
					.reversed()
			)
			.findFirst()
			.orElse(null);
		GetZoomParentRecordResponse.Builder builder = GetZoomParentRecordResponse.newBuilder();
		if (parentTab == null) {
			return builder;
		}

		MTable table = MTable.get(Env.getCtx(), parentTab.getAD_Table_ID());
		String parentKeyColum = table.getTableName() + "_ID";
		if (Util.isEmpty(currentLinkColumn, true)) {
			currentLinkColumn = parentKeyColum;
		}

		builder.setParentTabId(
				parentTab.getAD_Tab_ID()
			)
			.setParentTabUuid(
				ValueManager.validateNull(
					parentTab.getUUID()
				)
			)
			.setKeyColumn(
				ValueManager.validateNull(
					parentKeyColum
				)
			)
			.setName(
				ValueManager.validateNull(
					parentTab.get_Translation(
						I_AD_Tab.COLUMNNAME_Name
					)
				)
			)
		;

		final String sql = "SELECT parent." + parentKeyColum + " FROM " + table.getTableName() + " AS parent "
			+ "WHERE EXISTS(SELECT 1 "
				+ "FROM " + currentTable.getTableName() + " AS child "
				+ "WHERE child." + currentLinkColumn + " = parent." + parentKeyColum
				+ " AND child." + currentKeycolumn.getColumnName() + " = ?"
			+ ")"
		;
		Object currentValue = ValueManager.getObjectFromReference(
			request.getValue(),
			currentKeycolumn.getAD_Reference_ID()
		);
		int recordId = DB.getSQLValue(null, sql, currentValue);
		if (recordId >= 0) {
			builder.setRecordId(
				recordId
			);
		}

		return builder;
	}

}
