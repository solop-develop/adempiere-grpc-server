/************************************************************************************
 * Copyright (C) 2012-present E.R.P. Consultores y Asociados, C.A.                  *
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
package org.spin.grpc.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;

import org.adempiere.core.domains.models.I_AD_Browse_Field;
import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Client;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_EntityType;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Language;
import org.adempiere.core.domains.models.I_AD_Preference;
import org.adempiere.core.domains.models.I_AD_Private_Access;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Record_Access;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_CM_Chat;
import org.adempiere.core.domains.models.I_R_MailText;
import org.adempiere.core.domains.models.X_AD_Reference;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.GridTab;
import org.compiere.model.GridTabVO;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MChangeLog;
import org.compiere.model.MChat;
import org.compiere.model.MChatEntry;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMailText;
import org.compiere.model.MMessage;
import org.compiere.model.MPreference;
import org.compiere.model.MPrivateAccess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRecordAccess;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MRule;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsRequest;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.user_interface.ChatEntry;
import org.spin.backend.grpc.user_interface.ContextInfoValue;
import org.spin.backend.grpc.user_interface.CreateChatEntryRequest;
import org.spin.backend.grpc.user_interface.CreateTabEntityRequest;
import org.spin.backend.grpc.user_interface.DefaultValue;
import org.spin.backend.grpc.user_interface.DeletePreferenceRequest;
import org.spin.backend.grpc.user_interface.GetContextInfoValueRequest;
import org.spin.backend.grpc.user_interface.GetDefaultValueRequest;
import org.spin.backend.grpc.user_interface.GetPrivateAccessRequest;
import org.spin.backend.grpc.user_interface.GetRecordAccessRequest;
import org.spin.backend.grpc.user_interface.GetTabEntityRequest;
import org.spin.backend.grpc.user_interface.ListBrowserItemsRequest;
import org.spin.backend.grpc.user_interface.ListBrowserItemsResponse;
import org.spin.backend.grpc.user_interface.ListGeneralSearchRecordsRequest;
import org.spin.backend.grpc.user_interface.ListMailTemplatesRequest;
import org.spin.backend.grpc.user_interface.ListMailTemplatesResponse;
import org.spin.backend.grpc.user_interface.ListTabEntitiesRequest;
import org.spin.backend.grpc.user_interface.ListTabSequencesRequest;
import org.spin.backend.grpc.user_interface.ListTranslationsRequest;
import org.spin.backend.grpc.user_interface.ListTranslationsResponse;
import org.spin.backend.grpc.user_interface.ListTreeNodesRequest;
import org.spin.backend.grpc.user_interface.ListTreeNodesResponse;
import org.spin.backend.grpc.user_interface.LockPrivateAccessRequest;
import org.spin.backend.grpc.user_interface.MailTemplate;
import org.spin.backend.grpc.user_interface.Preference;
import org.spin.backend.grpc.user_interface.PreferenceType;
import org.spin.backend.grpc.user_interface.PrivateAccess;
import org.spin.backend.grpc.user_interface.RecordAccess;
import org.spin.backend.grpc.user_interface.RecordAccessRole;
import org.spin.backend.grpc.user_interface.RollbackEntityRequest;
import org.spin.backend.grpc.user_interface.RunCalloutRequest;
import org.spin.backend.grpc.user_interface.SaveTabSequencesRequest;
import org.spin.backend.grpc.user_interface.SetPreferenceRequest;
import org.spin.backend.grpc.user_interface.SetRecordAccessRequest;
import org.spin.backend.grpc.user_interface.Translation;
import org.spin.backend.grpc.user_interface.UnlockPrivateAccessRequest;
import org.spin.backend.grpc.user_interface.UpdateBrowserEntityRequest;
import org.spin.backend.grpc.user_interface.UpdateTabEntityRequest;
import org.spin.backend.grpc.user_interface.UserInterfaceGrpc.UserInterfaceImplBase;
import org.spin.base.db.OrderByUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.interim.ContextTemporaryWorkaround;
import org.spin.base.query.FilterManager;
import org.spin.base.query.SortingManager;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.service.ui.CalloutLogic;
import org.spin.grpc.service.ui.UserInterfaceLogic;
import org.spin.model.MADContextInfo;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Business data service
 */
public class UserInterface extends UserInterfaceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(UserInterface.class);
	/**	Window emulation	*/
	private AtomicInteger windowNoEmulation = new AtomicInteger(1);
	
	@Override
	public void rollbackEntity(RollbackEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Rollback Requested = " + request.getId());
			Entity.Builder entityValue = rollbackLastEntityAction(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	@Override
	public void updateBrowserEntity(UpdateBrowserEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Requested is Null");
			}
			log.fine("UpdateBrowserEntityRequest = " + request);
			Entity.Builder entityValue = updateBrowserEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	/**
	 * Update Browser Entity
	 * @param request
	 * @return
	 */
	private Entity.Builder updateBrowserEntity(UpdateBrowserEntityRequest request) {
		Properties context = Env.getCtx();
		MBrowse browser = ASPUtil.getInstance(context).getBrowse(request.getId());

		if (!browser.isUpdateable()) {
			throw new AdempiereException("Smart Browser not updateable records");
		}

		if (browser.getAD_Table_ID() <= 0) {
			throw new AdempiereException("No Table defined in the Smart Browser");
		}
		final MTable table = MTable.get(context, browser.getAD_Table_ID());

		PO entity = RecordUtil.getEntity(context, table.getAD_Table_ID(), null, request.getRecordId(), null);
		if (entity == null || entity.get_ID() <= 0) {
			// Return
			return ConvertUtil.convertEntity(entity);
		}

		MView view = new MView(context, browser.getAD_View_ID());
		List<MViewColumn> viewColumnsList = view.getViewColumns();

		request.getAttributes().getFieldsMap().entrySet().parallelStream().forEach(attribute -> {
			// find view column definition
			MViewColumn viewColumn = viewColumnsList
				.parallelStream()
				.filter(currentViewColumn -> {
					return currentViewColumn.getColumnName().equals(attribute.getKey());
				})
				.findFirst()
				.orElse(null)
			;
			// if view aliases not exists, next element
			if (viewColumn == null || viewColumn.getAD_View_Column_ID() <= 0) {
				return;
			}

			MViewDefinition viewDefinition = MViewDefinition.get(context, viewColumn.getAD_View_Definition_ID());
			// not same table setting in smart browser and view definition
			if (browser.getAD_Table_ID() != viewDefinition.getAD_Table_ID()) {
				log.info("Browse Table " + browser.getAD_Table_ID() + " and View Definition Table " + viewDefinition.getAD_Table_ID() + " different ");
				return;
			}

			MBrowseField browseField = MBrowseField.get(browser, viewColumn);
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				log.warning("Browse Field no found");
				return;
			}
			if (!browseField.isActive() || browseField.isReadOnly()) {
				log.warning("Browse Field not updateable: " + browseField.getName());
				return;
			}

			MColumn column = MColumn.get(browser.getCtx(), viewColumn.getAD_Column_ID());
			if (column == null || column.getAD_Column_ID() <= 0) {
				// column is not present on current table
				return;
			}
			if (column.isVirtualColumn() || column.isKey() || !column.isUpdateable()) {
				// virtual column with columnSQL
				log.warning("Column is virtual column or not updateable: " + column.getColumnName());
				return;
			}
			String columnName = column.getColumnName();
			int referenceId = column.getAD_Reference_ID();

			Object value = null;
			if (referenceId > 0) {
				value = ValueManager.getObjectFromReference(
					attribute.getValue(),
					referenceId
				);
			}
			if (value == null) {
				value = ValueManager.getObjectFromValue(attribute.getValue());
			}
			entity.set_ValueOfColumn(columnName, value);
		});
		//	Save entity
		if (entity.is_Changed()) {
			entity.saveEx();
		} else {
			log.severe(
				Msg.parseTranslation(context, "@Ignored@")
			);
		}

		//	Return
		return ConvertUtil.convertEntity(entity);
	}



	@Override
	/**
	 * TODO: Replace LockPrivateAccessRequest with GetPrivateAccessRequest
	 * @param request
	 * @param responseObserver
	 */
	public void lockPrivateAccess(LockPrivateAccessRequest request, StreamObserver<PrivateAccess> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			int recordId = request.getId();
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
			MUser user = MUser.get(Env.getCtx());
			PrivateAccess.Builder privateaccess = lockUnlockPrivateAccess(Env.getCtx(), request.getTableName(), recordId, user.getAD_User_ID(), true, null);
			responseObserver.onNext(privateaccess.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	/**
	 * TODO: Replace UnlockPrivateAccessRequest with GetPrivateAccessRequest
	 * @param request
	 * @param responseObserver
	 */
	public void unlockPrivateAccess(UnlockPrivateAccessRequest request, StreamObserver<PrivateAccess> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			int recordId = request.getId();
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
			MUser user = MUser.get(Env.getCtx());
			PrivateAccess.Builder privateaccess = lockUnlockPrivateAccess(Env.getCtx(), request.getTableName(), recordId, user.getAD_User_ID(), false, null);
			responseObserver.onNext(privateaccess.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void getPrivateAccess(GetPrivateAccessRequest request, StreamObserver<PrivateAccess> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			int recordId = request.getId();
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
			MUser user = MUser.get(Env.getCtx());
			MPrivateAccess privateAccess = getPrivateAccess(Env.getCtx(), request.getTableName(), recordId, user.getAD_User_ID(), null);
			if(privateAccess == null
					|| privateAccess.getAD_Table_ID() == 0) {
				MTable table = MTable.get(Env.getCtx(), request.getTableName());
				//	Set values
				privateAccess = new MPrivateAccess(Env.getCtx(), user.getAD_User_ID(), table.getAD_Table_ID(), recordId);
				privateAccess.setIsActive(false);
			}
			PrivateAccess.Builder privateaccess = convertPrivateAccess(Env.getCtx(), privateAccess);
			responseObserver.onNext(privateaccess.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void getContextInfoValue(GetContextInfoValueRequest request, StreamObserver<ContextInfoValue> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ContextInfoValue.Builder contextInfoValue = convertContextInfoValue(Env.getCtx(), request);
			responseObserver.onNext(contextInfoValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listTranslations(ListTranslationsRequest request, StreamObserver<ListTranslationsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListTranslationsResponse.Builder translationsList = convertTranslationsList(Env.getCtx(), request);
			responseObserver.onNext(translationsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	@Override
	public void createChatEntry(CreateChatEntryRequest request, StreamObserver<ChatEntry> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ChatEntry.Builder chatEntryValue = addChatEntry(request);
			responseObserver.onNext(chatEntryValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	@Override
	public void setPreference(SetPreferenceRequest request, StreamObserver<Preference> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			MPreference preference = getPreference(request.getTypeValue(), request.getColumnName(), request.getIsForCurrentClient(), request.getIsForCurrentOrganization(), request.getIsForCurrentUser(), request.getIsForCurrentContainer(), request.getContainerId());
			if(preference == null
					|| preference.getAD_Preference_ID() <= 0) {
				preference = new MPreference(Env.getCtx(), 0, null);
			}
			//	Save preference
			Preference.Builder preferenceBuilder = savePreference(preference, request.getTypeValue(), request.getColumnName(), request.getIsForCurrentClient(), request.getIsForCurrentOrganization(), request.getIsForCurrentUser(), request.getIsForCurrentContainer(), request.getContainerId(), request.getValue());
			responseObserver.onNext(preferenceBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void deletePreference(DeletePreferenceRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Empty.Builder empty = Empty.newBuilder();
			MPreference preference = getPreference(request.getTypeValue(), request.getColumnName(), request.getIsForCurrentClient(), request.getIsForCurrentOrganization(), request.getIsForCurrentUser(), request.getIsForCurrentContainer(), request.getContainerId());
			if(preference != null
					&& preference.getAD_Preference_ID() > 0) {
				preference.deleteEx(true);
			}
			responseObserver.onNext(empty.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void getRecordAccess(GetRecordAccessRequest request, StreamObserver<RecordAccess> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			RecordAccess.Builder recordAccess = convertRecordAccess(request);
			responseObserver.onNext(recordAccess.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void setRecordAccess(SetRecordAccessRequest request, StreamObserver<RecordAccess> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			RecordAccess.Builder recordAccess = saveRecordAccess(request);
			responseObserver.onNext(recordAccess.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	/**
	 * TODO: Does not work for tables (Access, Acct, Translation) with multiple keys
	 */
	@Override
	public void getTabEntity(GetTabEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			Entity.Builder entityValue = getTabEntity(request, new ArrayList<Object>());
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	public static Entity.Builder getTabEntity(GetTabEntityRequest request, ArrayList<Object> multiKeys) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		String[] keyColumns = table.getKeyColumns();

		String sql = QueryUtil.getTabQueryWithReferences(tab);
		// add filter
		StringBuffer whereClause = new StringBuffer();
		List<Object> filtersList = new ArrayList<Object>();
		if (keyColumns.length == 1) {
			for (final String keyColumnName: table.getKeyColumns()) {
				MColumn column = table.getColumn(keyColumnName);
				if (DisplayType.isID(column.getAD_Reference_ID())) {
					if (whereClause.length() > 0) {
						whereClause.append(" OR ");
					}
					whereClause.append(
						table.getTableName() + "." + keyColumnName + " = ?"
					);
					filtersList.add(
						request.getId()
					);
				}
			}
		} else {
			String whereMultiKeys = WhereClauseUtil.getWhereClauseFromKeyColumns(keyColumns);
			whereMultiKeys = WhereClauseUtil.getWhereRestrictionsWithAlias(
				table.getTableName(),
				whereMultiKeys
			);
			whereClause.append(whereMultiKeys);
			filtersList = multiKeys;
		}
		sql += " WHERE " + whereClause.toString();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Entity.Builder valueObjectBuilder = Entity.newBuilder()
			.setTableName(
				table.getTableName()
			)
		;
		CLogger log = CLogger.getCLogger(UserInterface.class);

		try {
			LinkedHashMap<String, MColumn> columnsMap = new LinkedHashMap<>();
			//	Add field to map
			for (MColumn column: table.getColumnsAsList()) {
				columnsMap.put(
					column.getColumnName().toUpperCase(),
					column
				);
			}

			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);

			// add query parameters
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);

			//	Get from Query
			rs = pstmt.executeQuery();
			if (rs.next()) {
				Struct.Builder rowValues = Struct.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName(index);
						MColumn column = columnsMap.get(columnName.toUpperCase());
						//	Display Columns
						if(column == null) {
							String displayValue = rs.getString(index);
							Value.Builder displayValueBuilder = ValueManager.getValueFromString(displayValue);

							rowValues.putFields(
								columnName,
								displayValueBuilder.build()
							);
							continue;
						}
						if (column.isKey()) {
							valueObjectBuilder.setId(
								rs.getInt(index)
							);
						}
						//	From field
						String fieldColumnName = column.getColumnName();
						Object value = rs.getObject(index);
						Value.Builder valueBuilder = ValueManager.getValueFromReference(
							value,
							column.getAD_Reference_ID()
						);
						rowValues.putFields(
							fieldColumnName,
							valueBuilder.build()
						);
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
				valueObjectBuilder.setValues(rowValues);
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Return
		return valueObjectBuilder;
	}



	@Override
	public void listTabEntities(ListTabEntitiesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListEntitiesResponse.Builder entityValueList = listTabEntities(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	private ListEntitiesResponse.Builder listTabEntities(ListTabEntitiesRequest request) {
		int tabId = request.getTabId();
		if (tabId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		Properties context = Env.getCtx();

		MTab originalTab = MTab.get(context, tabId);
		if (originalTab == null || originalTab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}
		MTab tab = ASPUtil.getInstance(context)
			.getWindowTab(
				originalTab.getAD_Window_ID(),
				originalTab.getAD_Tab_ID()
			)
		;

		//	
		MTable table = MTable.get(context, tab.getAD_Table_ID());
		String tableName = table.getTableName();

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, context, request.getContextAttributes()
		);

		// get where clause including link column and parent column
		String where = WhereClauseUtil.getTabWhereClauseFromParentTabs(context, tab, null);
		String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
		if (Util.isEmpty(parsedWhereClause, true) && !Util.isEmpty(where, true)) {
			throw new AdempiereException("@AD_Tab_ID@ @WhereClause@ @Unparseable@");
		}
		StringBuffer whereClause = new StringBuffer(parsedWhereClause);
		List<Object> params = new ArrayList<>();

		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), tableName, params);
		if(!Util.isEmpty(dynamicWhere, true)) {
			if(!Util.isEmpty(whereClause.toString(), true)) {
				whereClause.append(" AND ");
			}
			//	Add
			whereClause.append(dynamicWhere);
		}

		//	Add from reference
		//	TODO: Add support to this functionality
		if(!Util.isEmpty(request.getRecordReferenceUuid(), true)) {
			String referenceWhereClause = RecordUtil.referenceWhereClauseCache.get(request.getRecordReferenceUuid());
			if(!Util.isEmpty(referenceWhereClause, true)) {
				String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(tableName, referenceWhereClause);
				if(whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				whereClause.append("(").append(validationCode).append(")");
			}
		}

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		//	
		StringBuilder sql = new StringBuilder(QueryUtil.getTabQueryWithReferences(tab));
		String sqlWithRoleAccess = MRole.getDefault()
			.addAccessSQL(
				sql.toString(),
				tableName,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += " AND " + whereClause;
		}
		//
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, request.getSearchValue(), false, params);

		String orderByClause = "";
		if (!Util.isEmpty(request.getSortBy(), true)) {
			orderByClause = " ORDER BY " + SortingManager.newInstance(request.getSortBy()).getSotingAsSQL();
		} else {
			String tabOrderBy = OrderByUtil.getTabOrderByClause(tab);
			if (!Util.isEmpty(tabOrderBy, true)) {
				String parsedTabOrderBy = Env.parseContext(context, windowNo, tabOrderBy, false);
				if (Util.isEmpty(parsedTabOrderBy, true)) {
					throw new AdempiereException("@AD_Tab_ID@ @OrderByClause@ @Unparseable@");
				}
				orderByClause = " ORDER BY " + parsedTabOrderBy;
			}
		}

		//	Count records
		count = CountUtil.countRecords(parsedSQL, tableName, params);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		//	Add Order By
		parsedSQL = parsedSQL + orderByClause;
		builder = RecordUtil.convertListEntitiesResult(table, parsedSQL, params);
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		//	Return
		return builder;
	}


	@Override
	public void createTabEntity(CreateTabEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entityValue = createTabEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	private Entity.Builder createTabEntity(CreateTabEntityRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		PO entity = table.getPO(0, null);
		if (entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		attributes.entrySet().parallelStream().forEach(attribute -> {
			int referenceId = org.spin.dictionary.util.DictionaryUtil.getReferenceId(entity.get_Table_ID(), attribute.getKey());
			Object value = null;
			if (referenceId > 0) {
				value = ValueManager.getObjectFromReference(
					attribute.getValue(),
					referenceId
				);
			} 
			if (value == null) {
				value = ValueManager.getObjectFromValue(attribute.getValue());
			}
			entity.set_ValueOfColumn(attribute.getKey(), value);
		});
		//	Save entity
		entity.saveEx();

		String[] keyColumns = table.getKeyColumns();
		ArrayList<Object> parametersList = new ArrayList<Object>();
		if (keyColumns.length > 1) {
			parametersList = ParameterUtil.getParametersFromKeyColumns(
				keyColumns,
				attributes
			);
		}

		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(entity.get_ID())
		;

		Entity.Builder builder = getTabEntity(
			getEntityBuilder.build(),
			parametersList
		);
		return builder;
	}


	@Override
	public void updateTabEntity(UpdateTabEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entityValue = updateTabEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	private Entity.Builder updateTabEntity(UpdateTabEntityRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}

		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		String[] keyColumns = table.getKeyColumns();
		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		ArrayList<Object> parametersList = new ArrayList<Object>();
		PO entity = null;
		if (keyColumns.length == 1) {
			entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getId(), null);
		} else {
			final String whereClause = WhereClauseUtil.getWhereClauseFromKeyColumns(keyColumns);
			parametersList = ParameterUtil.getParametersFromKeyColumns(
				keyColumns,
				attributes
			);
			entity = new Query(
				Env.getCtx(),
				table.getTableName(),
				whereClause,
				null
			).setParameters(parametersList)
			.first();
		}
		if (entity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}
		PO currentEntity = entity;
		attributes.entrySet().parallelStream().forEach(attribute -> {
			final String columnName = attribute.getKey();
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			if (Arrays.stream(keyColumns).anyMatch(columnName::equals)) {
				// prevent warning `PO.set_Value: Column not updateable`
				return;
			}
			int referenceId = column.getAD_Reference_ID();
			Object value = null;
			if (referenceId > 0) {
				value = ValueManager.getObjectFromReference(
					attribute.getValue(),
					referenceId
				);
			} 
			if (value == null) {
				value = ValueManager.getObjectFromValue(
					attribute.getValue()
				);
			}
			currentEntity.set_ValueOfColumn(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();


		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(currentEntity.get_ID())
		;

		Entity.Builder builder = getTabEntity(
			getEntityBuilder.build(),
			parametersList
		);

		return builder;
	}



	@Override
	public void listGeneralSearchRecords(ListGeneralSearchRecordsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("List General Search Records Request Null");
			}
			ListEntitiesResponse.Builder entityValueList = UserInterfaceLogic.listGeneralSearchRecords(
				request
			);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * Convert Record Access
	 * @param request
	 * @return
	 */
	private RecordAccess.Builder convertRecordAccess(GetRecordAccessRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		//	
		int tableId = table.getAD_Table_ID();
		int recordId = request.getId();
		RecordAccess.Builder builder = RecordAccess.newBuilder()
			.setTableName(
				ValueManager.validateNull(
					request.getTableName()
				)
			)
			.setId(recordId)
		;
		//	Populate access List
		getRecordAccess(tableId, recordId, null).parallelStream().forEach(recordAccess -> {
			MRole role = MRole.get(Env.getCtx(), recordAccess.getAD_Role_ID());
			builder.addCurrentRoles(RecordAccessRole.newBuilder()
				.setRoleId(role.getAD_Role_ID())
				.setRoleName(
					ValueManager.validateNull(
						role.getName()
					)
				)
				.setIsActive(recordAccess.isActive())
				.setIsDependentEntities(recordAccess.isDependentEntities())
				.setIsExclude(recordAccess.isExclude())
				.setIsReadOnly(recordAccess.isReadOnly()));
		});
		//	Populate roles list
		getRolesList(null).parallelStream().forEach(role -> {
			builder.addAvailableRoles(
				RecordAccessRole.newBuilder()
					.setRoleId(role.getAD_Role_ID())
					.setRoleName(
						ValueManager.validateNull(
							role.getName()
						)
					)
			);
		});
		return builder;
	}
	
	/**
	 * Get record access from client, role , table id and record id
	 * @param tableId
	 * @param recordId
	 * @param transactionName
	 * @return
	 */
	private List<MRecordAccess> getRecordAccess(int tableId, int recordId, String transactionName) {
		return new Query(Env.getCtx(), I_AD_Record_Access.Table_Name,"AD_Table_ID = ? "
				+ "AND Record_ID = ? "
				+ "AND AD_Client_ID = ?", transactionName)
			.setParameters(tableId, recordId, Env.getAD_Client_ID(Env.getCtx()))
			.list();
	}
	
	/**
	 * Get role for this client
	 * @param transactionName
	 * @return
	 */
	private List<MRole> getRolesList(String transactionName) {
		return new Query(Env.getCtx(), I_AD_Role.Table_Name, null, transactionName)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.list();
	}
	
	/**
	 * save record Access
	 * @param request
	 * @return
	 */
	private RecordAccess.Builder saveRecordAccess(SetRecordAccessRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		if(request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		//	
		RecordAccess.Builder builder = RecordAccess.newBuilder();
		Trx.run(transactionName -> {
			int tableId = table.getAD_Table_ID();
			AtomicInteger recordId = new AtomicInteger(request.getId());
			builder.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				)
				.setId(recordId.get())
			;
			//	Delete old
			DB.executeUpdateEx("DELETE FROM AD_Record_Access "
					+ "WHERE AD_Table_ID = ? "
					+ "AND Record_ID = ? "
					+ "AND AD_Client_ID = ?", new Object[]{tableId, recordId.get(), Env.getAD_Client_ID(Env.getCtx())}, transactionName);
			//	Add new record access
			request.getRecordAccessesList().parallelStream().forEach(recordAccessToSet -> {
				int roleId = recordAccessToSet.getRoleId();
				if(roleId <= 0) {
					throw new AdempiereException("@AD_Role_ID@ @NotFound@");
				}
				MRole role = MRole.get(Env.getCtx(), roleId);
				MRecordAccess recordAccess = new MRecordAccess(Env.getCtx(), role.getAD_Role_ID(), tableId, recordId.get(), transactionName);
				recordAccess.setIsActive(recordAccessToSet.getIsActive());
				recordAccess.setIsExclude(recordAccessToSet.getIsExclude());
				recordAccess.setIsDependentEntities(recordAccessToSet.getIsDependentEntities());
				recordAccess.setIsReadOnly(recordAccessToSet.getIsReadOnly());
				recordAccess.saveEx();
				//	Add current roles
				builder.addCurrentRoles(
					RecordAccessRole.newBuilder()
						.setRoleId(role.getAD_Role_ID())
						.setRoleName(
							ValueManager.validateNull(
								role.getName()
							)
						)
						.setIsActive(recordAccess.isActive())
						.setIsDependentEntities(recordAccess.isDependentEntities())
						.setIsExclude(recordAccess.isExclude())
						.setIsReadOnly(recordAccess.isReadOnly())
				);
			});
			//	Populate roles list
			getRolesList(transactionName).parallelStream().forEach(roleToGet -> {
				builder.addAvailableRoles(
					RecordAccessRole.newBuilder()
						.setRoleId(roleToGet.getAD_Role_ID())
				);
			});
		});
		//	
		return builder;
	}
	
	/**
	 * Save preference from values
	 * @param preference
	 * @param preferenceType
	 * @param attribute
	 * @param isCurrentClient
	 * @param isCurrentOrganization
	 * @param isCurrentUser
	 * @param isCurrentContainer
	 * @param id
	 * @param value
	 * @return
	 */
	private Preference.Builder savePreference(MPreference preference, int preferenceType, String attribute, boolean isCurrentClient, boolean isCurrentOrganization, boolean isCurrentUser, boolean isCurrentContainer, int id, String value) {
		Preference.Builder builder = Preference.newBuilder();
		if(preferenceType == PreferenceType.WINDOW_VALUE) {
			int windowId = id;
			int clientId = Env.getAD_Client_ID(Env.getCtx());
			int orgId = Env.getAD_Org_ID(Env.getCtx());
			int userId = Env.getAD_User_ID(Env.getCtx());
			//	For client
			if(!isCurrentClient) {
				clientId = 0;
			}
			//	For Organization
			if(!isCurrentOrganization) {
				orgId = 0;
			}
			//For User
			if(!isCurrentUser) {
				userId = -1;
			}
			//	For Window
			if(!isCurrentContainer) {
				windowId = -1;
			}
			//	Set values
			preference.set_ValueOfColumn(I_AD_Client.COLUMNNAME_AD_Client_ID, clientId);
			preference.setAD_Org_ID(orgId);
			preference.setAD_User_ID(userId);
			preference.setAD_Window_ID(windowId);
			preference.setAttribute(attribute);
			preference.setValue(value);
			//	
			preference.saveEx();
			builder.setClientId(preference.getAD_Client_ID())
				.setOrganizationId(preference.getAD_Org_ID())
				.setUserId(preference.getAD_User_ID())
				.setContainerId(id)
				.setColumnName(
					ValueManager.validateNull(
						preference.getAttribute()
					)
				)
				.setValue(preference.getValue())
			;
		}
		//	
		return builder;
	}
	
	/**
	 * Get preference
	 * @param preferenceType
	 * @param attribute
	 * @param isCurrentClient
	 * @param isCurrentOrganization
	 * @param isCurrentUser
	 * @param isCurrentContainer
	 * @param id
	 * @return
	 */
	private MPreference getPreference(int preferenceType, String attribute, boolean isCurrentClient, boolean isCurrentOrganization, boolean isCurrentUser, boolean isCurrentContainer, int id) {
		if(preferenceType == PreferenceType.WINDOW_VALUE) {
			StringBuffer whereClause = new StringBuffer("Attribute = ?");
			List<Object> parameters = new ArrayList<>();
			parameters.add(attribute);
			//	For client
			whereClause.append(" AND AD_Client_ID = ?");
			if(isCurrentClient) {
				parameters.add(Env.getAD_Client_ID(Env.getCtx()));
			} else {
				parameters.add(0);
			}
			//	For Organization
			whereClause.append(" AND AD_Org_ID = ?");
			if(isCurrentOrganization) {
				parameters.add(Env.getAD_Org_ID(Env.getCtx()));
			} else {
				parameters.add(0);
			}
			//For User
			if(isCurrentUser) {
				parameters.add(Env.getAD_User_ID(Env.getCtx()));
				whereClause.append(" AND AD_User_ID = ?");
			} else {
				whereClause.append(" AND AD_User_ID IS NULL");
			}
			//	For Window
			if(isCurrentContainer) {
				parameters.add(id);
				whereClause.append(" AND AD_Window_ID = ?");
			} else {
				whereClause.append(" AND AD_Window_ID IS NULL");
			}
			return new Query(Env.getCtx(), I_AD_Preference.Table_Name, whereClause.toString(), null)
					.setParameters(parameters)
					.first();
		}
		//	
		return null;
	}



	/**
	 * Get private access from table, record id and user id
	 * @param Env.getCtx()
	 * @param tableName
	 * @param recordId
	 * @param userUuid
	 * @param transactionName
	 * @return
	 */
	private MPrivateAccess getPrivateAccess(Properties context, String tableName, int recordId, int userId, String transactionName) {
		return new Query(Env.getCtx(), I_AD_Private_Access.Table_Name, "EXISTS(SELECT 1 FROM AD_Table t WHERE t.AD_Table_ID = AD_Private_Access.AD_Table_ID AND t.TableName = ?) "
				+ "AND Record_ID = ? "
				+ "AND AD_User_ID = ?", transactionName)
			.setParameters(tableName, recordId, userId)
			.first();
	}


	/**
	 * Lock and unlock private access
	 * @param Env.getCtx()
	 * @param request
	 * @param lock
	 * @param transactionName
	 * @return
	 */
	private PrivateAccess.Builder lockUnlockPrivateAccess(Properties context, String tableName, int recordId, int userId, boolean lock, String transactionName) {
		MPrivateAccess privateAccess = getPrivateAccess(Env.getCtx(), tableName, recordId, userId, transactionName);
		//	Create new
		if(privateAccess == null
				|| privateAccess.getAD_Table_ID() == 0) {
			MTable table = MTable.get(Env.getCtx(), tableName);
			//	Set values
			privateAccess = new MPrivateAccess(Env.getCtx(), userId, table.getAD_Table_ID(), recordId);
		}
		//	Set active
		privateAccess.setIsActive(lock);
		privateAccess.saveEx(transactionName);
		//	Convert Private Access
		return convertPrivateAccess(Env.getCtx(), privateAccess);
	}


	/**
	 * Convert languages to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ListTranslationsResponse.Builder convertTranslationsList(Properties context, ListTranslationsRequest request) {
		ListTranslationsResponse.Builder builder = ListTranslationsResponse.newBuilder();
		String tableName = request.getTableName();
		if(Util.isEmpty(tableName)) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		Trx.run(transactionName -> {
			MTable table = MTable.get(Env.getCtx(), tableName);
			PO entity = RecordUtil.getEntity(Env.getCtx(), tableName, request.getId(), transactionName);
			List<Object> parameters = new ArrayList<>();
			StringBuffer whereClause = new StringBuffer(entity.get_KeyColumns()[0] + " = ?");
			parameters.add(entity.get_ID());
			if(!Util.isEmpty(request.getLanguage())) {
				whereClause.append(" AND AD_Language = ?");
				parameters.add(request.getLanguage());
			}
			new Query(Env.getCtx(), tableName + "_Trl", whereClause.toString(), transactionName)
				.setParameters(parameters)
				.<PO>list()
				.parallelStream()
				.forEach(translation -> {
					Translation.Builder translationBuilder = Translation.newBuilder();
					Struct.Builder translationValues = Struct.newBuilder();
					table.getColumnsAsList().parallelStream()
						.filter(column -> {
							return column.isTranslated();
						})
						.forEach(column -> {
							Object value = translation.get_Value(column.getColumnName());
							if(value == null) {
								return;
							}
							Value.Builder builderValue = ValueManager.getValueFromObject(value);
							if(builderValue != null) {
								translationValues.putFields(
									column.getColumnName(),
									builderValue.build()
								);
							}
							//	Set Language
							if(Util.isEmpty(translationBuilder.getLanguage())) {
								translationBuilder.setLanguage(
									ValueManager.validateNull(
										translation.get_ValueAsString("AD_Language")
									)
								);
							}
						});
					translationBuilder.setValues(translationValues);
					builder.addTranslations(translationBuilder);
				});
		});
		//	Return
		return builder;
	}



	/**
	 * Rollback entity
	 * @param request
	 * @return
	 */
	private Entity.Builder rollbackLastEntityAction(RollbackEntityRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		AtomicReference<PO> entityWrapper = new AtomicReference<PO>();
		Trx.run(transactionName -> {
			int id = request.getId();
			//	get Table from table name
			int logId = request.getLogId();
			if(logId <= 0) {
				logId = getLastChangeLogId(table.getAD_Table_ID(), id, transactionName);
			}
			if(logId > 0) {
				List<MChangeLog> changeLogList = new Query(Env.getCtx(), I_AD_ChangeLog.Table_Name, I_AD_ChangeLog.COLUMNNAME_AD_ChangeLog_ID + " = ?", transactionName)
						.setParameters(logId)
						.<MChangeLog>list();
				String eventType = MChangeLog.EVENTCHANGELOG_Update;
				if(changeLogList.size() > 0) {
					MChangeLog log = changeLogList.get(0);
					eventType = log.getEventChangeLog();
					if(eventType.equals(MChangeLog.EVENTCHANGELOG_Insert)) {
						MChangeLog changeLog = new MChangeLog(Env.getCtx(), logId, transactionName);
						PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), changeLog.getRecord_ID(), transactionName);
						if(entity != null
								&& entity.get_ID() >= 0) {
							entity.delete(true);
						}
					} else if(eventType.equals(MChangeLog.EVENTCHANGELOG_Delete)
							|| eventType.equals(MChangeLog.EVENTCHANGELOG_Update)) {
						PO entity = table.getPO(id, transactionName);
						if(entity == null
								|| entity.get_ID() <= 0) {
							throw new AdempiereException("@Error@ @PO@ @NotFound@");
						}
						changeLogList.parallelStream().forEach(changeLog -> {
							setValueFromChangeLog(entity, changeLog);
						});
						entity.saveEx(transactionName);
						entityWrapper.set(entity);
					}
				}
			} else {
				throw new AdempiereException("@AD_ChangeLog_ID@ @NotFound@");
			}
		});
		//	Return
		if(entityWrapper.get() != null) {
			return ConvertUtil.convertEntity(entityWrapper.get());
		}
		//	Instead
		return Entity.newBuilder();
	}

	/**
	 * set value for PO from change log
	 * @param entity
	 * @param changeLog
	 */
	private void setValueFromChangeLog(PO entity, MChangeLog changeLog) {
		Object value = null;
		try {
			if(!changeLog.isOldNull()) {
				MColumn column = MColumn.get(Env.getCtx(), changeLog.getAD_Column_ID());
				value = stringToObject(column, changeLog.getOldValue());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
		//	Set value
		entity.set_ValueOfColumn(changeLog.getAD_Column_ID(), value);
	}


	/**
	 * Convert string representation to appropriate object type
	 * for column
	 * @param column
	 * @param value
	 * @return
	 */
	private Object stringToObject(MColumn column, String value) {
		if (value == null) {
			return null;
		}

		int displayTypeId = column.getAD_Reference_ID();
		if (DisplayType.isText(column.getAD_Reference_ID()) || displayTypeId == DisplayType.List
				|| column.getColumnName().equals(I_AD_EntityType.COLUMNNAME_EntityType)
				|| column.getColumnName().equals(I_AD_Language.COLUMNNAME_AD_Language)) {
			return value;
		}
		else if (DisplayType.isID(column.getAD_Reference_ID()) || DisplayType.Integer == displayTypeId) {
			Object valueObject = NumberManager.getIntegerFromString(value);
			if (valueObject == null && value != null
				&& (DisplayType.Search == displayTypeId || DisplayType.Table == displayTypeId)) {
				// EntityType, AD_Language
				return value;
			}
			return valueObject;
		}
		else if (DisplayType.isNumeric(displayTypeId)) {
			return NumberManager.getBigDecimalFromString(value);
		}
		else if (DisplayType.YesNo == displayTypeId) {
			return BooleanManager.getBooleanFromString(value);
		}
		else if (DisplayType.Button == displayTypeId && column.getAD_Reference_Value_ID() <= 0) {
			return "true".equalsIgnoreCase(value) ? "Y" : "N";
		}
		else if (DisplayType.Button == displayTypeId && column.getAD_Reference_Value_ID() > 0) {
			return value;
		}
		else if (DisplayType.isDate(displayTypeId)) {
			return TimeManager.getTimestampFromString(value);
		}
		// Binary, RowID, Image not supported
		else {
			return null;
		}
	}



	/**
	 * Create Chat Entry
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ChatEntry.Builder addChatEntry(CreateChatEntryRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		AtomicReference<MChatEntry> entryReference = new AtomicReference<>();
		Trx.run(transactionName -> {
			PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getId(), transactionName);
			//	
			StringBuffer whereClause = new StringBuffer();
			List<Object> parameters = new ArrayList<>();
			//	
			whereClause
				.append(I_CM_Chat.COLUMNNAME_AD_Table_ID).append(" = ?")
				.append(" AND ")
				.append(I_CM_Chat.COLUMNNAME_Record_ID).append(" = ?");
			//	Set parameters
			parameters.add(table.getAD_Table_ID());
			parameters.add(request.getId());
			MChat chat = new Query(Env.getCtx(), I_CM_Chat.Table_Name, whereClause.toString(), transactionName)
					.setParameters(parameters)
					.setClient_ID()
					.first();
			//	Add or create chat
			if (chat == null 
					|| chat.getCM_Chat_ID() == 0) {
				chat = new MChat (Env.getCtx(), table.getAD_Table_ID(), entity.get_ID(), entity.getDisplayValue(), transactionName);
				chat.saveEx();
			}
			//	Add entry PO
			MChatEntry entry = new MChatEntry(chat, request.getComment());
			entry.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
			entry.saveEx(transactionName);
			entryReference.set(entry);
		});
		//	Return
		return ConvertUtil.convertChatEntry(entryReference.get());
	}
	
	/**
	 * Get Last change Log
	 * @param tableId
	 * @param recordId
	 * @param transactionName
	 * @return
	 */
	private int getLastChangeLogId(int tableId, int recordId, String transactionName) {
		return DB.getSQLValue(null, "SELECT AD_ChangeLog_ID "
				+ "FROM AD_ChangeLog "
				+ "WHERE AD_Table_ID = ? "
				+ "AND Record_ID = ? "
				+ "AND ROWNUM <= 1 "
				+ "ORDER BY Updated DESC", tableId, recordId);
	}



	@Override
	public void getDefaultValue(GetDefaultValueRequest request, StreamObserver<DefaultValue> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			DefaultValue.Builder defaultValue = getDefaultValue(request);
			responseObserver.onNext(defaultValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException())
			;
		}
	}

	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	private DefaultValue.Builder getDefaultValue(GetDefaultValueRequest request) {
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
			int fieldId = field.getAD_Field_ID();
			List<MField> customFields = ASPUtil.getInstance(Env.getCtx()).getWindowFields(field.getAD_Tab_ID());
			if(customFields != null) {
				Optional<MField> maybeField = customFields.parallelStream()
					.filter(customField -> {
						return customField.getAD_Field_ID() == fieldId;
					})
					.findFirst();
				if(maybeField.isPresent()) {
					field = maybeField.get();
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
				}
			}
		} else if(request.getBrowseFieldId() > 0) {
			MBrowseField browseField = (MBrowseField) RecordUtil.getEntity(Env.getCtx(), I_AD_Browse_Field.Table_Name, request.getBrowseFieldId(), null);
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				throw new AdempiereException("@AD_Browse_Field_ID@ @NotFound@");
			}
			int browseFieldId = browseField.getAD_Browse_Field_ID();
			List<MBrowseField> customFields = ASPUtil.getInstance(Env.getCtx()).getBrowseFields(browseField.getAD_Browse_ID());
			if(customFields != null) {
				Optional<MBrowseField> maybeField = customFields.parallelStream()
					.filter(customField -> {
						return customField.getAD_Browse_Field_ID() == browseFieldId;
					})
					.findFirst();
				if(maybeField.isPresent()) {
					browseField = maybeField.get();
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
				}
			}
		} else if(request.getBrowseFieldToId() > 0) {
			MBrowseField browseField = (MBrowseField) RecordUtil.getEntity(Env.getCtx(), I_AD_Browse_Field.Table_Name, request.getBrowseFieldToId(), null);
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				throw new AdempiereException("@AD_Browse_Field_ID@ @NotFound@");
			}
			int browseFieldId = browseField.getAD_Browse_Field_ID();
			List<MBrowseField> customFields = ASPUtil.getInstance(Env.getCtx()).getBrowseFields(browseField.getAD_Browse_ID());
			if(customFields != null) {
				Optional<MBrowseField> maybeField = customFields.parallelStream()
					.filter(customField -> {
						return customField.getAD_Browse_Field_ID() == browseFieldId;
					})
					.findFirst();
				if(maybeField.isPresent()) {
					browseField = maybeField.get();
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
				}
			}
		} else if(request.getProcessParameterId() > 0) {
			MProcessPara processParameter = (MProcessPara) RecordUtil.getEntity(Env.getCtx(), I_AD_Process_Para.Table_Name, request.getProcessParameterId(), null);
			if(processParameter == null || processParameter.getAD_Process_Para_ID() <= 0) {
				throw new AdempiereException("@AD_Process_Para_ID@ @NotFound@");
			}
			int processParameterId = processParameter.getAD_Process_Para_ID();
			List<MProcessPara> customParameters = ASPUtil.getInstance(Env.getCtx()).getProcessParameters(processParameter.getAD_Process_ID());
			if(customParameters != null) {
				Optional<MProcessPara> maybeParameter = customParameters.parallelStream()
					.filter(customField -> {
						return customField.getAD_Process_Para_ID() == processParameterId;
					})
					.findFirst();
				if(maybeParameter.isPresent()) {
					processParameter = maybeParameter.get();
					referenceId = processParameter.getAD_Reference_ID();
					referenceValueId = processParameter.getAD_Reference_Value_ID();
					validationRuleId = processParameter.getAD_Val_Rule_ID();
					columnName = processParameter.getColumnName();
					defaultValue = processParameter.getDefaultValue();
				}
			}
		} else if(request.getProcessParameterToId() > 0) {
			MProcessPara processParameter = (MProcessPara) RecordUtil.getEntity(Env.getCtx(), I_AD_Process_Para.Table_Name, request.getProcessParameterToId(), null);
			if(processParameter == null || processParameter.getAD_Process_Para_ID() <= 0) {
				throw new AdempiereException("@AD_Process_Para_ID@ @NotFound@");
			}
			int processParameterId = processParameter.getAD_Process_Para_ID();
			List<MProcessPara> customParameters = ASPUtil.getInstance(Env.getCtx()).getProcessParameters(processParameter.getAD_Process_ID());
			if(customParameters != null) {
				Optional<MProcessPara> maybeParameter = customParameters.parallelStream()
					.filter(customField -> {
						return customField.getAD_Process_Para_ID() == processParameterId;
					})
					.findFirst();
				if(maybeParameter.isPresent()) {
					processParameter = maybeParameter.get();
					referenceId = processParameter.getAD_Reference_ID();
					referenceValueId = processParameter.getAD_Reference_Value_ID();
					validationRuleId = processParameter.getAD_Val_Rule_ID();
					columnName = processParameter.getColumnName();
					defaultValue = processParameter.getDefaultValue2(); // value to
				}
			}
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
			defaultValue = request.getValue().getStringValue();
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
	private DefaultValue.Builder getDefaultKeyAndValue(String contextAttributes, String defaultValue, int displayTypeId, int referenceValueId, String columnName, int validationRuleId) {
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
				builder = convertDefaultValue(
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
						builder = convertDefaultValue(
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
	private Object getDefaultValueFromSQL(String sql) {
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
	 * Convert Values from result
	 * @param keyValue
	 * @param uuidValue
	 * @param value
	 * @param displayValue
	 * @return
	 */
	private DefaultValue.Builder convertDefaultValue(Object keyValue, String uuidValue, String value, String displayValue, boolean isActive) {
		Struct.Builder values = Struct.newBuilder();
		DefaultValue.Builder builder = DefaultValue.newBuilder()
			.setValues(values)
			.setIsActive(isActive)
		;
		if(keyValue == null) {
			return builder;
		}

		// Key Column
		if(keyValue instanceof Integer) {
			Integer integerValue = NumberManager.getIntegerFromObject(
				keyValue
			);
			builder.setId(integerValue);
			values.putFields(
				LookupUtil.KEY_COLUMN_KEY,
				ValueManager.getValueFromInteger(integerValue).build()
			);
		} else {
			values.putFields(
				LookupUtil.KEY_COLUMN_KEY,
				ValueManager.getValueFromString((String) keyValue).build()
			);
		}
		//	Set Value
		if(!Util.isEmpty(value)) {
			values.putFields(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueManager.getValueFromString(value).build()
			);
		}
		//	Display column
		if(!Util.isEmpty(displayValue)) {
			values.putFields(
				LookupUtil.DISPLAY_COLUMN_KEY,
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
	 * Convert Context Info Value from query
	 * @param request
	 * @return
	 */
	private ContextInfoValue.Builder convertContextInfoValue(Properties context, GetContextInfoValueRequest request) {
		ContextInfoValue.Builder builder = ContextInfoValue.newBuilder();
		if(request == null) {
			throw new AdempiereException("Object Request Null");
		}
		if(request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		int id = request.getId();
		MADContextInfo contextInfo = MADContextInfo.getById(Env.getCtx(), id);
		if(contextInfo != null
				&& contextInfo.getAD_ContextInfo_ID() > 0) {
			try {
				//	Set value for parse no save
				contextInfo.setSQLStatement(request.getQuery());
				MMessage message = MMessage.get(Env.getCtx(), contextInfo.getAD_Message_ID());
				if(message != null) {
					//	Parse
					Object[] arguments = contextInfo.getArguments(0);
					if(arguments == null) {
						return builder;
					}
					//	
					String messageText = Msg.getMsg(Env.getAD_Language(Env.getCtx()), message.getValue(), arguments);
					//	Set result message
					builder.setMessageText(
						ValueManager.validateNull(messageText)
					);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, e.getLocalizedMessage());
			}
		}
		//	Return values
		return builder;
	}
	
	/**
	 * Convert Context Info Value from query
	 * @param request
	 * @return
	 */
	private PrivateAccess.Builder convertPrivateAccess(Properties context, MPrivateAccess privateAccess) {
		PrivateAccess.Builder builder = PrivateAccess.newBuilder();
		if(privateAccess == null) {
			return builder;
		}
		//	Table
		MTable table = MTable.get(Env.getCtx(), privateAccess.getAD_Table_ID());
		//	Set values
		builder.setTableName(table.getTableName());
		builder.setId(privateAccess.getRecord_ID());
		builder.setIsLocked(privateAccess.isActive());
		//	Return values
		return builder;
	}



	@Override
	public void listLookupItems(ListLookupItemsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Lookup Request Null");
			}

			ListLookupItemsResponse.Builder entityValueList = listLookupItems(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert Object Request to list
	 * @param request
	 * @return
	 */
	private ListLookupItemsResponse.Builder listLookupItems(ListLookupItemsRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName()
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



	@Override
	public void listBrowserItems(ListBrowserItemsRequest request, StreamObserver<ListBrowserItemsResponse> responseObserver) {
		try {
			log.fine("Object List Requested = " + request);
			ListBrowserItemsResponse.Builder entityValueList = listBrowserItems(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	private ListBrowserItemsResponse.Builder listBrowserItems(ListBrowserItemsRequest request) {
		ListBrowserItemsResponse.Builder builder = ListBrowserItemsResponse.newBuilder();
		Properties context = Env.getCtx();
		MBrowse browser = ASPUtil.getInstance(context).getBrowse(request.getId());
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return builder;
		}
		HashMap<String, Object> parameterMap = new HashMap<>();
		//	Populate map
		FilterManager.newInstance(request.getFilters()).getConditions()
			.parallelStream()
			.forEach(condition -> {
				parameterMap.put(condition.getColumnName(), condition.getValue());
			});

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());
		ContextManager.setContextWithAttributes(windowNo, context, parameterMap, false);

		//	get query columns
		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String sql = Env.parseContext(context, windowNo, query, false);
		if (Util.isEmpty(sql, true)) {
			throw new AdempiereException("@AD_Browse_ID@ @SQL@ @Unparseable@");
		}

		MView view = browser.getAD_View();
		MViewDefinition parentDefinition = view.getParentViewDefinition();
		String tableNameAlias = parentDefinition.getTableAlias();
		String tableName = parentDefinition.getAD_Table().getTableName();

		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				tableNameAlias,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();
		String where = browser.getWhereClause();
		if (!Util.isEmpty(where, true)) {
			String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
			if (Util.isEmpty(parsedWhereClause, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			whereClause
				.append(" AND ")
				.append(parsedWhereClause);
		}

		//	For dynamic condition
		List<Object> filterValues = new ArrayList<Object>();
		String dynamicWhere = WhereClauseUtil.getBrowserWhereClauseFromCriteria(
			browser,
			request.getFilters(),
			filterValues
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			String parsedDynamicWhere = Env.parseContext(context, windowNo, dynamicWhere, false);
			if (Util.isEmpty(parsedDynamicWhere, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			//	Add
			whereClause.append(" AND (")
				.append(parsedDynamicWhere)
				.append(") ")
			;
		}
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += whereClause;
		}

		String orderByClause = org.spin.service.grpc.util.db.OrderByUtil.getBrowseOrderBy(browser);
		if (!Util.isEmpty(orderByClause, true)) {
			orderByClause = " ORDER BY " + orderByClause;
		}

		//	Get page and count
		int count = CountUtil.countRecords(sqlWithRoleAccess, tableName, tableNameAlias, filterValues);
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Add Row Number
		String parsedSQL = LimitUtil.getQueryWithLimit(sqlWithRoleAccess, limit, offset);
		//	Add Order By
		parsedSQL = parsedSQL + orderByClause;
		//	Return
		builder = convertBrowserResult(browser, parsedSQL, filterValues);
		//	Validate page token
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		builder.setRecordCount(count);
		//	Return
		return builder;
	}
	
	/**
	 * Convert SQL to list values
	 * @param pagePrefix
	 * @param browser
	 * @param sql
	 * @param values
	 * @return
	 */
	private ListBrowserItemsResponse.Builder convertBrowserResult(MBrowse browser, String sql, List<Object> parameters) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListBrowserItemsResponse.Builder builder = ListBrowserItemsResponse.newBuilder();
		long recordCount = 0;
		try {
			LinkedHashMap<String, MBrowseField> fieldsMap = new LinkedHashMap<>();
			//	Add field to map
			for(MBrowseField field: ASPUtil.getInstance().getBrowseFields(browser.getAD_Browse_ID())) {
				fieldsMap.put(field.getAD_View_Column().getColumnName().toUpperCase(), field);
			}
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, parameters);

			MBrowseField fieldKey = browser.getFieldKey();
			String keyColumnName = null;
			if (fieldKey != null && fieldKey.get_ID() > 0) {
				keyColumnName = fieldKey.getAD_View_Column().getColumnName();
			}
			keyColumnName = ValueManager.validateNull(keyColumnName);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				Struct.Builder rowValues = Struct.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();

				Entity.Builder entityBuilder = Entity.newBuilder();
				if (!Util.isEmpty(keyColumnName, true) && rs.getObject(keyColumnName) != null) {
					entityBuilder.setId(
						rs.getInt(keyColumnName)
					);
				}
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName(index);
						MBrowseField field = fieldsMap.get(columnName.toUpperCase());
						//	Display Columns
						if(field == null) {
							String displayValue = rs.getString(index);
							Value.Builder displayValueBuilder = ValueManager.getValueFromString(displayValue);

							rowValues.putFields(
								columnName,
								displayValueBuilder.build()
							);
							continue;
						}
						//	From field
						String fieldColumnName = field.getAD_View_Column().getColumnName();
						Object value = rs.getObject(index);
						Value.Builder valueBuilder = ValueManager.getValueFromReference(
							value,
							field.getAD_Reference_ID()
						);
						rowValues.putFields(
							fieldColumnName,
							valueBuilder.build()
						);
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
					}
				}
				//	
				entityBuilder.setValues(rowValues);
				builder.addRecords(entityBuilder.build());
				recordCount++;
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//	Set record counts
		if (builder.getRecordCount() <= 0) {
			builder.setRecordCount(recordCount);
		}
		//	Return
		return builder;
	}



	@Override
	public void runCallout(RunCalloutRequest request, StreamObserver<org.spin.backend.grpc.user_interface.Callout> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			org.spin.backend.grpc.user_interface.Callout.Builder calloutResponse = runcallout(request);
			responseObserver.onNext(
				calloutResponse.build()
			);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Run callout with data from server
	 * @param request
	 * @return
	 */
	private org.spin.backend.grpc.user_interface.Callout.Builder runcallout(RunCalloutRequest request) {
		if (Util.isEmpty(request.getCallout(), true)) {
			throw new AdempiereException("@FillMandatory@ @Callout@");
		}
		if (Util.isEmpty(request.getColumnName(), true)) {
			throw new AdempiereException("@FillMandatory@ @ColumnName@");
		}
		org.spin.backend.grpc.user_interface.Callout.Builder calloutBuilder = org.spin.backend.grpc.user_interface.Callout.newBuilder();
		Trx.run(transactionName -> {
			if (request.getTabId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
			}
			MTab tab = MTab.get(Env.getCtx(), request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}

			MField field = null;
			Optional<MField> searchedValue = Arrays.asList(tab.getFields(false, null)).parallelStream()
				.filter(searchField -> {
					return searchField.getAD_Column().getColumnName().equals(request.getColumnName());
				})
				.findFirst();
			if(searchedValue.isPresent()) {
				field = searchedValue.get();
			}
			int tabNo = (tab.getSeqNo() / 10) - 1;
			if(tabNo < 0) {
				tabNo = 0;
			}
			//	window
			int windowNo = request.getWindowNo();
			if(windowNo <= 0) {
				windowNo = windowNoEmulation.getAndIncrement();
			}

			// set values on Env.getCtx()
			Map<String, Object> attributes = ValueManager.convertValuesMapToObjects(
				request.getContextAttributes().getFieldsMap()
			);
			ContextManager.setContextWithAttributesFromObjectMap(windowNo, Env.getCtx(), attributes);

			//
			Object oldValue = null;
			Object value = null;
			if (field != null && field.getAD_Field_ID() > 0) {
				MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
				int displayTypeId = column.getAD_Reference_ID();
				oldValue = ValueManager.getObjectFromReference(
					request.getOldValue(),
					displayTypeId
				);
				value = ValueManager.getObjectFromReference(
					request.getValue(),
					displayTypeId
				);
			} else {
				oldValue = ValueManager.getObjectFromValue(
					request.getOldValue()
				);
				value = ValueManager.getObjectFromValue(
					request.getValue()
				);
			}
			ContextManager.setTabContextByObject(Env.getCtx(), windowNo, tabNo, request.getColumnName(), value);

			//	Initial load for callout wrapper
			GridWindowVO gridWindowVo = GridWindowVO.create(Env.getCtx(), windowNo, tab.getAD_Window_ID());
			GridWindow gridWindow = new GridWindow(gridWindowVo, true);
			GridTabVO gridTabVo = GridTabVO.create(gridWindowVo, tabNo, tab, false, true);
			GridFieldVO gridFieldVo = GridFieldVO.create(Env.getCtx(), windowNo, tabNo, tab.getAD_Window_ID(), tab.getAD_Tab_ID(), false, field);
			GridField gridField = new GridField(gridFieldVo);
			GridTab gridTab = new GridTab(gridTabVo, gridWindow, true);
			//	Init tab
			gridTab.query(false);
			gridTab.clearSelection();
			gridTab.dataNew(false);

			//	load values
			for (Entry<String, Object> attribute : attributes.entrySet()) {
				gridTab.setValue(attribute.getKey(), attribute.getValue());
			}
			gridTab.setValue(request.getColumnName(), value);

			//	Load value for field
			gridField.setValue(oldValue, false);
			gridField.setValue(value, false);

			//	Run it
			String result = processCallout(windowNo, gridTab, gridField);
			Struct.Builder contextValues = Struct.newBuilder();
			Arrays.asList(gridTab.getFields())
				.parallelStream()
				.filter(fieldValue -> {
					return CalloutLogic.isValidChange(fieldValue);
				})
				.forEach(fieldValue -> {
					Value.Builder valueBuilder = ValueManager.getValueFromReference(
						fieldValue.getValue(),
						fieldValue.getDisplayType()
					);
					contextValues.putFields(
						fieldValue.getColumnName(),
						valueBuilder.build()
					);
				});

			// always add is sales transaction on context
			String isSalesTransaction = Env.getContext(Env.getCtx(), windowNo, "IsSOTrx", true);
			if (!Util.isEmpty(isSalesTransaction, true)) {
				Value.Builder valueBuilder = ValueManager.getValueFromStringBoolean(isSalesTransaction);
				contextValues.putFields(
					"IsSOTrx",
					valueBuilder.build()
				);
			}
			calloutBuilder.setResult(
					ValueManager.validateNull(result)
				)
				.setValues(contextValues)
			;

			// TODO: Temporary Workaround
			ContextTemporaryWorkaround.setAdditionalContext(
				request.getCallout(),
				windowNo,
				calloutBuilder
			);
		});
		return calloutBuilder;
	}

	/**
	 * Process Callout
	 * @param gridTab
	 * @param field
	 * @return
	 */
	private String processCallout (int windowNo, GridTab gridTab, GridField field) {
		String callout = field.getCallout();
		if (Util.isEmpty(callout, true)) {
			return "";
		}

		//
		Object value = field.getValue();
		Object oldValue = field.getOldValue();
		log.fine(field.getColumnName() + "=" + value
			+ " (" + callout + ") - old=" + oldValue);

		StringTokenizer st = new StringTokenizer(callout, ";,", false);
		while (st.hasMoreTokens()) {
			String cmd = st.nextToken().trim();
			String retValue = "";
			// FR [1877902]
			// CarlosRuiz - globalqss - implement beanshell callout
			// Victor Perez  - vpj-cd implement JSR 223 Scripting
			if (cmd.toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
				MRule rule = MRule.get(Env.getCtx(), cmd.substring(MRule.SCRIPT_PREFIX.length()));
				if (rule == null) {
					retValue = "Callout " + cmd + " not found"; 
					log.log(Level.SEVERE, retValue);
					return retValue;
				}
				if ( !  (rule.getEventType().equals(MRule.EVENTTYPE_Callout) 
					  && rule.getRuleType().equals(MRule.RULETYPE_JSR223ScriptingAPIs))) {
					retValue = "Callout " + cmd
						+ " must be of type JSR 223 and event Callout"; 
					log.log(Level.SEVERE, retValue);
					return retValue;
				}

				ScriptEngine engine = rule.getScriptEngine();

				// Window Env.getCtx() are    W_
				// Login Env.getCtx()  are    G_
				MRule.setContext(engine, Env.getCtx(), windowNo);
				// now add the callout parameters windowNo, tab, field, value, oldValue to the engine 
				// Method arguments Env.getCtx() are A_
				engine.put(MRule.ARGUMENTS_PREFIX + "WindowNo", windowNo);
				engine.put(MRule.ARGUMENTS_PREFIX + "Tab", this);
				engine.put(MRule.ARGUMENTS_PREFIX + "Field", field);
				engine.put(MRule.ARGUMENTS_PREFIX + "Value", value);
				engine.put(MRule.ARGUMENTS_PREFIX + "OldValue", oldValue);
				engine.put(MRule.ARGUMENTS_PREFIX + "Ctx", Env.getCtx());

				try {
					retValue = engine.eval(rule.getScript()).toString();
				} catch (Exception e) {
					log.log(Level.SEVERE, "", e);
					e.printStackTrace();
					retValue = 	"Callout Invalid: " + e.toString();
					return retValue;
				}
			} else {
				Callout call = null;
				String method = null;
				int methodStart = cmd.lastIndexOf('.');
				try {
					if (methodStart != -1) {
						Class<?> cClass = Class.forName(cmd.substring(0,methodStart));
						call = (Callout) cClass.getDeclaredConstructor().newInstance();
						method = cmd.substring(methodStart+1);
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "class", e);
					e.printStackTrace();
					return "Callout Invalid: " + cmd + " (" + e.toString() + ")";
				}

				if (call == null || Util.isEmpty(method, true)) {
					return "Callout Invalid: " + method;
				}

				try {
					retValue = call.start(Env.getCtx(), method, windowNo, gridTab, field, value, oldValue);
				} catch (Exception e) {
					log.log(Level.SEVERE, "start", e);
					e.printStackTrace();
					retValue = 	"Callout Invalid: " + e.toString();
					return retValue;
				}
			}
			if (!Util.isEmpty(retValue)) {	//	interrupt on first error
				log.severe (retValue);
				return retValue;
			}
		}   //  for each callout
		return "";
	}	//	processCallout



	@Override
	public void listTabSequences(ListTabSequencesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListEntitiesResponse.Builder recordsListBuilder = listTabSequences(request);
			responseObserver.onNext(recordsListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder listTabSequences(ListTabSequencesRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		// Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, Env.getCtx(), request.getContextAttributes()
		);

		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		;
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @No@ @Sequence@");
		}
		if (!tab.isSortTab()) {
			throw new AdempiereException("@AD_Tab_ID@ @No@ @Sequence@");
		}
		String sortColumnName = MColumn.getColumnName(Env.getCtx(), tab.getAD_ColumnSortOrder_ID());
		String includedColumnName = MColumn.getColumnName(Env.getCtx(), tab.getAD_ColumnSortYesNo_ID());

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		List<MColumn> columnsList = table.getColumnsAsList();
		MColumn keyColumn = columnsList.parallelStream()
			.filter(column -> {
				return column.isKey();
			})
			.findFirst()
			.orElse(null);

		MColumn parentColumn = columnsList.parallelStream()
			.filter(column -> {
				return column.isParent();
			})
			.findFirst()
			.orElse(null);

		int parentRecordId = Env.getContextAsInt(Env.getCtx(), windowNo, parentColumn.getColumnName());

		Query query = new Query(
				Env.getCtx(),
				table.getTableName(),
				parentColumn.getColumnName() + " = ?",
				null
			)
			.setParameters(parentRecordId)
			.setOrderBy(sortColumnName + " ASC")
		;

		int count = query.count();

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<PO> sequencesList = query.setLimit(limit, offset).list();
		ListEntitiesResponse.Builder builderList = ListEntitiesResponse.newBuilder()
			.setRecordCount(count);

		sequencesList.forEach(entity -> {
			Entity.Builder entityBuilder = Entity.newBuilder()
				.setTableName(table.getTableName())
				.setId(entity.get_ID())
			;

			// set attributes
			Struct.Builder values = Struct.newBuilder();
			values.putFields(
				keyColumn.getColumnName(),
				ValueManager.getValueFromInt(
					entity.get_ValueAsInt(keyColumn.getColumnName())
				).build()
			);
			values.putFields(
				LookupUtil.UUID_COLUMN_KEY,
				ValueManager.getValueFromString(
					entity.get_UUID()
				).build()
			);
			values.putFields(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueManager.getValueFromString(
					entity.getDisplayValue()
				).build()
			);
			values.putFields(
				sortColumnName,
				ValueManager.getValueFromInt(
					entity.get_ValueAsInt(sortColumnName)
				).build()
			);
			values.putFields(
				includedColumnName,
				ValueManager.getValueFromBoolean(
					entity.get_ValueAsBoolean(includedColumnName)
				).build()
			);
			entityBuilder.setValues(values);

			builderList.addRecords(entityBuilder);
		});

		// Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		return builderList;
	}


	@Override
	public void saveTabSequences(SaveTabSequencesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListEntitiesResponse.Builder recordsListBuilder = saveTabSequences(request);
			responseObserver.onNext(recordsListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder saveTabSequences(SaveTabSequencesRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		//  Fill Env.getCtx()
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		
		ContextManager.setContextWithAttributesFromStruct(windowNo, Env.getCtx(), request.getContextAttributes());
		
		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @No@ @Sequence@");
		}
		if (!tab.isSortTab()) {
			throw new AdempiereException("@AD_Tab_ID@ @No@ @Sequence@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		List<MColumn> columnsList = table.getColumnsAsList();
		MColumn keyColumn = columnsList.parallelStream()
			.filter(column -> {
				return column.isKey();
			})
			.findFirst()
			.orElse(null);
		String sortColumnName = MColumn.getColumnName(Env.getCtx(), tab.getAD_ColumnSortOrder_ID());
		String includedColumnName = MColumn.getColumnName(Env.getCtx(), tab.getAD_ColumnSortYesNo_ID());

		ListEntitiesResponse.Builder builderList = ListEntitiesResponse.newBuilder()
			.setRecordCount(request.getEntitiesList().size());

		Trx.run(transacctionName -> {
			request.getEntitiesList().parallelStream().forEach(entitySelection -> {
				PO entity = RecordUtil.getEntity(
					Env.getCtx(), table.getTableName(),
					entitySelection.getSelectionId(),
					transacctionName
				);
				if (entity == null || entity.get_ID() <= 0) {
					return;
				}
				// set new values
				entitySelection.getValues().getFieldsMap().entrySet()
					.parallelStream()
					.forEach(attribute -> {
						Object value = ValueManager.getObjectFromValue(attribute.getValue());
						entity.set_ValueOfColumn(attribute.getKey(), value);
					});
				entity.saveEx(transacctionName);

				Entity.Builder entityBuilder = Entity.newBuilder()
					.setTableName(table.getTableName())
					.setId(entity.get_ID())
				;

				Struct.Builder values = Struct.newBuilder();
				// set attributes
				values.putFields(
					keyColumn.getColumnName(),
					ValueManager.getValueFromInt(
						entity.get_ValueAsInt(keyColumn.getColumnName())
					).build()
				);
				values.putFields(
					LookupUtil.UUID_COLUMN_KEY,
					ValueManager.getValueFromString(
						entity.get_UUID()
					).build()
				);
				values.putFields(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueManager.getValueFromString(
						entity.getDisplayValue()
					).build()
				);
				values.putFields(
					sortColumnName,
					ValueManager.getValueFromInt(
						entity.get_ValueAsInt(sortColumnName)
					).build()
				);
				values.putFields(
					includedColumnName,
					ValueManager.getValueFromBoolean(
						entity.get_ValueAsBoolean(includedColumnName)
					).build()
				);

				entityBuilder.setValues(values);

				builderList.addRecords(entityBuilder);
			});
		});

		return builderList;
	}



	@Override
	public void listTreeNodes(ListTreeNodesRequest request, StreamObserver<ListTreeNodesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListTreeNodesResponse.Builder recordsListBuilder = UserInterfaceLogic.listTreeNodes(request);
			responseObserver.onNext(recordsListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	@Override
	public void listMailTemplates(ListMailTemplatesRequest request, StreamObserver<ListMailTemplatesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMailTemplatesResponse.Builder recordsListBuilder = listMailTemplates(request);
			responseObserver.onNext(recordsListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListMailTemplatesResponse.Builder listMailTemplates(ListMailTemplatesRequest request) {
		Query query = new Query(
				Env.getCtx(),
				I_R_MailText.Table_Name,
				null,
				null
			)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int recordCount = query.count();
		ListMailTemplatesResponse.Builder builderList = ListMailTemplatesResponse.newBuilder();
		builderList.setRecordCount(recordCount);
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		query
			.setLimit(limit, offset)
			.list(MMailText.class)
			.parallelStream()
			.forEach(requestRecord -> {
				MailTemplate.Builder builder = convertMailTemplate(requestRecord);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private MailTemplate.Builder convertMailTemplate(MMailText mailTemplate) {
		MailTemplate.Builder builder = MailTemplate.newBuilder();
		if (mailTemplate == null || mailTemplate.getR_MailText_ID() <= 0) {
			return builder;
		}

		String mailText = ValueManager.validateNull(mailTemplate.getMailText())
			+ ValueManager.validateNull(mailTemplate.getMailText2())
			+ ValueManager.validateNull(mailTemplate.getMailText3())
		;
		builder.setId(mailTemplate.getR_MailText_ID())
			.setName(
				ValueManager.validateNull(mailTemplate.getName())
			)
			.setSubject(
				ValueManager.validateNull(mailTemplate.getMailHeader())
			)
			.setMailText(
				ValueManager.validateNull(mailText)
			)
		;

		return builder;
	}

}
