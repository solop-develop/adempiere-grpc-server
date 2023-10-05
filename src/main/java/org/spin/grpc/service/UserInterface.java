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
package org.spin.grpc.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import org.adempiere.core.domains.models.I_AD_Browse_Field;
import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Client;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Preference;
import org.adempiere.core.domains.models.I_AD_PrintFormat;
import org.adempiere.core.domains.models.I_AD_Private_Access;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Record_Access;
import org.adempiere.core.domains.models.I_AD_ReportView;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_CM_Chat;
import org.adempiere.core.domains.models.I_R_MailText;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.adempiere.model.MViewDefinition;
import org.adempiere.model.ZoomInfoFactory;
import org.compiere.model.Callout;
import org.compiere.model.CalloutOrder;
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
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MQuery;
import org.compiere.model.MRecordAccess;
import org.compiere.model.MRefList;
import org.compiere.model.MReportView;
import org.compiere.model.MRole;
import org.compiere.model.MRule;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.MUser;
import org.compiere.model.MWindow;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.PrintInfo;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ChatEntry;
import org.spin.backend.grpc.common.ContextInfoValue;
import org.spin.backend.grpc.common.CreateChatEntryRequest;
import org.spin.backend.grpc.common.CreateTabEntityRequest;
import org.spin.backend.grpc.common.DefaultValue;
import org.spin.backend.grpc.common.DeletePreferenceRequest;
import org.spin.backend.grpc.common.DrillTable;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ExistsReferencesRequest;
import org.spin.backend.grpc.common.ExistsReferencesResponse;
import org.spin.backend.grpc.common.GetContextInfoValueRequest;
import org.spin.backend.grpc.common.GetDefaultValueRequest;
import org.spin.backend.grpc.common.GetPrivateAccessRequest;
import org.spin.backend.grpc.common.GetRecordAccessRequest;
import org.spin.backend.grpc.common.GetReportOutputRequest;
import org.spin.backend.grpc.common.GetTabEntityRequest;
import org.spin.backend.grpc.common.ListBrowserItemsRequest;
import org.spin.backend.grpc.common.ListBrowserItemsResponse;
import org.spin.backend.grpc.common.ListDrillTablesRequest;
import org.spin.backend.grpc.common.ListDrillTablesResponse;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListGeneralInfoRequest;
import org.spin.backend.grpc.common.ListLookupItemsRequest;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.ListMailTemplatesRequest;
import org.spin.backend.grpc.common.ListMailTemplatesResponse;
import org.spin.backend.grpc.common.ListPrintFormatsRequest;
import org.spin.backend.grpc.common.ListPrintFormatsResponse;
import org.spin.backend.grpc.common.ListReferencesRequest;
import org.spin.backend.grpc.common.ListReferencesResponse;
import org.spin.backend.grpc.common.ListReportViewsRequest;
import org.spin.backend.grpc.common.ListReportViewsResponse;
import org.spin.backend.grpc.common.ListTabEntitiesRequest;
import org.spin.backend.grpc.common.ListTabSequencesRequest;
import org.spin.backend.grpc.common.ListTranslationsRequest;
import org.spin.backend.grpc.common.ListTranslationsResponse;
import org.spin.backend.grpc.common.ListTreeNodesRequest;
import org.spin.backend.grpc.common.ListTreeNodesResponse;
import org.spin.backend.grpc.common.LockPrivateAccessRequest;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.MailTemplate;
import org.spin.backend.grpc.common.Preference;
import org.spin.backend.grpc.common.PrintFormat;
import org.spin.backend.grpc.common.PrivateAccess;
import org.spin.backend.grpc.common.RecordAccess;
import org.spin.backend.grpc.common.RecordAccessRole;
import org.spin.backend.grpc.common.RecordReferenceInfo;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.common.ReportView;
import org.spin.backend.grpc.common.RollbackEntityRequest;
import org.spin.backend.grpc.common.RunCalloutRequest;
import org.spin.backend.grpc.common.SaveTabSequencesRequest;
import org.spin.backend.grpc.common.SetPreferenceRequest;
import org.spin.backend.grpc.common.SetRecordAccessRequest;
import org.spin.backend.grpc.common.Translation;
import org.spin.backend.grpc.common.TreeNode;
import org.spin.backend.grpc.common.TreeType;
import org.spin.backend.grpc.common.UnlockPrivateAccessRequest;
import org.spin.backend.grpc.common.UpdateBrowserEntityRequest;
import org.spin.backend.grpc.common.UpdateTabEntityRequest;
import org.spin.backend.grpc.common.UserInterfaceGrpc.UserInterfaceImplBase;
import org.spin.base.db.CountUtil;
import org.spin.base.db.LimitUtil;
import org.spin.base.db.OrderByUtil;
import org.spin.base.db.ParameterUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.dictionary.ReportUtil;
import org.spin.base.query.FilterManager;
import org.spin.base.query.SortingManager;
import org.spin.base.ui.UserInterfaceConvertUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.FileUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.SessionManager;
import org.spin.model.MADContextInfo;
import org.spin.service.grpc.util.ValueManager;
import org.spin.util.ASPUtil;

import com.google.protobuf.ByteString;
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
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
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

	@Override
	public void updateBrowserEntity(UpdateBrowserEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Requested is Null");
			}
			log.fine("Object List Requested = " + request);
			Entity.Builder entityValue = updateBrowserEntity(Env.getCtx(), request);
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
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private Entity.Builder updateBrowserEntity(Properties context, UpdateBrowserEntityRequest request) {
		MBrowse browser = ASPUtil.getInstance(Env.getCtx()).getBrowse(request.getId());

		if (!browser.isUpdateable()) {
			throw new AdempiereException("Smart Browser not updateable record");
		}

		if (browser.getAD_Table_ID() <= 0) {
			throw new AdempiereException("No Table defined in the Smart Browser");
		}

		PO entity = RecordUtil.getEntity(Env.getCtx(), browser.getAD_Table_ID(), null, request.getRecordId(), null);
		if (entity == null || entity.get_ID() <= 0) {
			// Return
			return ConvertUtil.convertEntity(entity);
		}

		MView view = new MView(Env.getCtx(), browser.getAD_View_ID());
		List<MViewColumn> viewColumnsList = view.getViewColumns();

		request.getAttributes().getFieldsMap().entrySet().forEach(attribute -> {
			// find view column definition
			MViewColumn viewColumn = viewColumnsList
				.stream()
				.filter(column -> {
					return column.getColumnName().equals(attribute.getKey());
				})
				.findFirst()
				.get();

			// if view aliases not exists, next element
			if (viewColumn == null) {
				return;
			}
			MViewDefinition viewDefinition = MViewDefinition.get(Env.getCtx(), viewColumn.getAD_View_Definition_ID());

			// not same table setting in smart browser and view definition
			if (browser.getAD_Table_ID() != viewDefinition.getAD_Table_ID()) {
				return;
			}
			String columnName = MColumn.getColumnName(Env.getCtx(), viewColumn.getAD_Column_ID());

			int referenceId = org.spin.base.dictionary.DictionaryUtil.getReferenceId(entity.get_Table_ID(), columnName);

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
		entity.saveEx();

		//	Return
		return ConvertUtil.convertEntity(entity);
	}

	@Override
	public void listReferences(ListReferencesRequest request, StreamObserver<ListReferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Process Activity Requested is Null");
			}
			log.fine("References Info Requested = " + request);
			ListReferencesResponse.Builder entityValueList = listReferences(request);
			responseObserver.onNext(entityValueList.build());
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
	 * Convert references to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ListReferencesResponse.Builder listReferences(ListReferencesRequest request) {
		ListReferencesResponse.Builder builder = ListReferencesResponse.newBuilder();
		//	Get entity
		if (request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if (table == null || table.getAD_Table_ID() < 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		// validate multiple keys as accounting tables and translation tables
		if (!table.isSingleKey()) {
			return builder;
		}

		StringBuffer whereClause = new StringBuffer();
		List<Object> params = new ArrayList<>();
		whereClause.append(table.getTableName() + "_ID = ?");
		params.add(request.getId());
		PO entity = new Query(
			Env.getCtx(),
			table.getTableName(),
			whereClause.toString(),
			null
		)
			.setParameters(params)
			.first();
		if (entity == null || entity.get_ID() < 0) {
			return builder;
		}

		MWindow window = new Query(
			Env.getCtx(),
			I_AD_Window.Table_Name,
			I_AD_Window.COLUMNNAME_AD_Window_ID + " = ?",
			null
		)
			.setParameters(request.getWindowId())
			.setOnlyActiveRecords(true)
			.first();
		if (window != null && window.get_ID() > 0) {
			List<ZoomInfoFactory.ZoomInfo> zoomInfos = ZoomInfoFactory.retrieveZoomInfos(entity, window.getAD_Window_ID())
				.stream()
				.filter(zoomInfo -> {
					return zoomInfo.query.getRecordCount() > 0;
				})
				.collect(Collectors.toList());

			zoomInfos.stream().forEach(zoomInfo -> {
				MQuery zoomQuery = zoomInfo.query;
				//
				RecordReferenceInfo.Builder recordReferenceBuilder = RecordReferenceInfo.newBuilder();

				MWindow referenceWindow = MWindow.get(Env.getCtx(), zoomInfo.windowId);
				MTab tab = Arrays.stream(referenceWindow.getTabs(false, null))
					.filter(tabItem -> {
						return zoomQuery.getZoomTableName().equals(tabItem.getAD_Table().getTableName());
					})
					.findFirst()
					.orElse(null)
				;
				recordReferenceBuilder.setWindowId(referenceWindow.getAD_Window_ID());
				if (tab != null && tab.getAD_Tab_ID() > 0) {
					recordReferenceBuilder.setTabId(tab.getAD_Tab_ID());
				}
				recordReferenceBuilder.setTableName(
						ValueManager.validateNull(
							zoomQuery.getZoomTableName()
						)
					)
					.setWhereClause(
						ValueManager.validateNull(
							zoomQuery.getWhereClause()
						)
					)
					.setRecordCount(
						zoomQuery.getRecordCount()
					)
					.setDisplayName(
						zoomInfo.destinationDisplay + " (#" + zoomQuery.getRecordCount() + ")"
					)
					.setColumnName(
						ValueManager.validateNull(
							zoomQuery.getZoomColumnName()
						)
					)
					.setValue(
						ValueManager.getValueFromObject(
							zoomQuery.getZoomValue()
						).build()
					)
				;

				//	Add to list
				builder.addReferences(recordReferenceBuilder.build());
			});
			builder.setRecordCount(zoomInfos.size());
		}
		//	Return
		return builder;
	}


	@Override
	public void existsReferences(ExistsReferencesRequest request, StreamObserver<ExistsReferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Process Activity Requested is Null");
			}
			log.fine("References Info Requested = " + request);
			ExistsReferencesResponse.Builder entityValueList = existsReferences(request);
			responseObserver.onNext(entityValueList.build());
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

	private ExistsReferencesResponse.Builder existsReferences(ExistsReferencesRequest request) {
		

		// validate tab
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @Mandatory@");
		}
		MTab tab = (MTab) RecordUtil.getEntity(Env.getCtx(), I_AD_Tab.Table_Name, request.getTabId(), null);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		// builder
		ExistsReferencesResponse.Builder builder = ExistsReferencesResponse.newBuilder();

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		// validate multiple keys as accounting tables and translation tables
		if (!table.isSingleKey()) {
			return builder;
		}

		// validate record
		if(request.getRecordId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getRecordId(), null);
		// if (entity == null) {
		// 	throw new AdempiereException("@Record_ID@ @NotFound@");
		// }

		int recordCount = 0;
		if (entity != null && entity.get_ID() >= 0) {
			List<ZoomInfoFactory.ZoomInfo> zoomInfos = ZoomInfoFactory.retrieveZoomInfos(entity, tab.getAD_Window_ID())
				.stream()
				.filter(zoomInfo -> {
					return zoomInfo.query.getRecordCount() > 0;
				})
				.collect(Collectors.toList());
			if (zoomInfos != null && zoomInfos.size() > 0) {
				recordCount = zoomInfos.size();
			}
		}

		//	Return
		return builder.setRecordCount(recordCount);
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
	public void listPrintFormats(ListPrintFormatsRequest request, StreamObserver<ListPrintFormatsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPrintFormatsResponse.Builder printFormatsList = convertPrintFormatsList(Env.getCtx(), request);
			responseObserver.onNext(printFormatsList.build());
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
	public void listReportViews(ListReportViewsRequest request, StreamObserver<ListReportViewsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			ListReportViewsResponse.Builder reportViewsList = convertReportViewsList(Env.getCtx(), request);
			responseObserver.onNext(reportViewsList.build());
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
	public void listDrillTables(ListDrillTablesRequest request, StreamObserver<ListDrillTablesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListDrillTablesResponse.Builder drillTablesList = convertDrillTablesList(request);
			responseObserver.onNext(drillTablesList.build());
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
	public void getReportOutput(GetReportOutputRequest request, StreamObserver<ReportOutput> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ReportOutput.Builder reportOutput = getReportOutput(request);
			responseObserver.onNext(reportOutput.build());
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



	@Override
	public void getTabEntity(GetTabEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			Entity.Builder entityValue = getTabEntity(request);
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
	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	private Entity.Builder getTabEntity(GetTabEntityRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab tab = new Query(
			Env.getCtx(),
			MTab.Table_Name,
			MTab.COLUMNNAME_AD_Tab_ID + " = ? ",
			null
		)
		.setParameters(request.getTabId())
		.first();
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		String tableName = table.getTableName();

		String sql = QueryUtil.getTabQueryWithReferences(tab);
		// add filter
		StringBuffer whereClause = new StringBuffer()
			.append(" WHERE ")
			.append(tableName + "." + tableName + "_ID = ?");
		sql += whereClause.toString();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Entity.Builder valueObjectBuilder = Entity.newBuilder();
		valueObjectBuilder.setTableName(table.getTableName());
		try {
			LinkedHashMap<String, MColumn> columnsMap = new LinkedHashMap<>();
			//	Add field to map
			for (MColumn column: table.getColumnsAsList()) {
				columnsMap.put(column.getColumnName().toUpperCase(), column);
			}
			
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);

			// add query parameters
			pstmt.setInt(1, request.getId());

			//	Get from Query
			rs = pstmt.executeQuery();
			if (rs.next()) {
				Struct.Builder values = Struct.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName (index);
						MColumn field = columnsMap.get(columnName.toUpperCase());
						Value.Builder valueBuilder = Value.newBuilder();
						//	Display Columns
						if(field == null) {
							String value = rs.getString(index);
							if(!Util.isEmpty(value)) {
								valueBuilder = ValueManager.getValueFromString(value);
							}
							values.putFields(columnName, valueBuilder.build());
							continue;
						}
						if (field.isKey()) {
							valueObjectBuilder.setId(rs.getInt(index));
						}
						//	From field
						String fieldColumnName = field.getColumnName();
						Object value = rs.getObject(index);
						valueBuilder = ValueManager.getValueFromReference(
							value,
							field.getAD_Reference_ID()
						);
						values.putFields(fieldColumnName, valueBuilder.build());
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
				valueObjectBuilder.setValues(values);
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
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
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
		//	
		MTab tab = MTab.get(Env.getCtx(), tabId);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		String tableName = table.getTableName();

		//	Fill context
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromStruct(windowNo, context, request.getContextAttributes());

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
//		if(!Util.isEmpty(criteria.getReferenceUuid())) {
//			String referenceWhereClause = referenceWhereClauseCache.get(criteria.getReferenceUuid());
//			if(!Util.isEmpty(referenceWhereClause)) {
//				if(whereClause.length() > 0) {
//					whereClause.append(" AND ");
//				}
//				whereClause.append("(").append(referenceWhereClause).append(")");
//			}
//		}
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

		MTab tab = new Query(
			Env.getCtx(),
			I_AD_Tab.Table_Name,
			"AD_Tab_ID = ?",
			null
		).setParameters(request.getTabId())
		.first();
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		PO entity = table.getPO(0, null);
		if (entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		request.getAttributes().getFieldsMap().entrySet().forEach(attribute -> {
			int referenceId = org.spin.base.dictionary.DictionaryUtil.getReferenceId(entity.get_Table_ID(), attribute.getKey());
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
		
		
		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(entity.get_ID())
		;

		Entity.Builder builder = getTabEntity(getEntityBuilder.build());
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

		MTab tab = new Query(
			Env.getCtx(),
			I_AD_Tab.Table_Name,
			"AD_Tab_ID = ?",
			null
		).setParameters(request.getTabId())
		.first();
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getId(), null);
		if (entity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}
		if (entity.get_ID() >= 0) {
			request.getAttributes().getFieldsMap().entrySet().forEach(attribute -> {
				int referenceId = org.spin.base.dictionary.DictionaryUtil.getReferenceId(entity.get_Table_ID(), attribute.getKey());
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
		}

		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(entity.get_ID())
		;

		Entity.Builder builder = getTabEntity(getEntityBuilder.build());
		return builder;
	}


	@Override
	public void listGeneralInfo(ListGeneralInfoRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListEntitiesResponse.Builder entityValueList = listGeneralInfo(request);
			responseObserver.onNext(entityValueList.build());
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
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	private ListEntitiesResponse.Builder listGeneralInfo(ListGeneralInfoRequest request) {
		String tableName = request.getTableName();
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			tableName
		);

		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromStruct(windowNo, Env.getCtx(), request.getContextAttributes());

		//
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

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
		String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(tableName, reference.ValidationCode);
		String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
		if (!Util.isEmpty(reference.ValidationCode, true)) {
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException("@WhereClause@ @Unparseable@");
			}
			whereClause.append(" AND ").append(parsedValidationCode);
		}

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), tableName, params);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")");
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, request.getSearchValue(), false, params);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		
		//	Count records
		count = CountUtil.countRecords(parsedSQL, tableName, params);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(Env.getCtx(), tableName), parsedSQL, params);
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
	
	/**
	 * Convert Record Access
	 * @param request
	 * @return
	 */
	private RecordAccess.Builder convertRecordAccess(GetRecordAccessRequest request) {
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if(request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		//	
		int tableId = MTable.getTable_ID(request.getTableName());
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
		getRecordAccess(tableId, recordId, null).forEach(recordAccess -> {
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
		getRolesList(null).forEach(role -> {
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
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if(request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		//	
		RecordAccess.Builder builder = RecordAccess.newBuilder();
		Trx.run(transactionName -> {
			int tableId = MTable.getTable_ID(request.getTableName());
			AtomicInteger recordId = new AtomicInteger(request.getId());
			builder.setTableName(
					ValueManager.validateNull(
						request.getTableName()
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
			request.getRecordAccessesList().forEach(recordAccessToSet -> {
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
			getRolesList(transactionName).forEach(roleToGet -> {
				builder.addAvailableRoles(RecordAccessRole.newBuilder()
						.setRoleId(roleToGet.getAD_Role_ID()));
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
		if(preferenceType == SetPreferenceRequest.Type.WINDOW_VALUE) {
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
		if(preferenceType == SetPreferenceRequest.Type.WINDOW_VALUE) {
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
	 * Convert Object to list
	 * @param request
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private ReportOutput.Builder getReportOutput(GetReportOutputRequest request) throws FileNotFoundException, IOException {
		if (request.getProcessId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		int processId = request.getProcessId();
		MProcess process = ASPUtil.getInstance().getProcess(processId);
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}

		if(Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @TableName@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		//	
		if(!MRole.getDefault().isCanReport(table.getAD_Table_ID())) {
			throw new AdempiereException("@AccessCannotReport@");
		}

		//	Validate print format
		if(request.getPrintFormatId() <= 0 && request.getReportViewId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@ / @AD_ReportView_ID@");
		}

		//	
		PrintInfo printInformation = new PrintInfo(request.getReportName(), table.getAD_Table_ID(), 0, 0);
		//	Get Print Format
		MPrintFormat printFormat = null;
		MReportView reportView = null;
		if(request.getPrintFormatId() > 0) {
			printFormat = new Query(Env.getCtx(), I_AD_PrintFormat.Table_Name, I_AD_PrintFormat.COLUMNNAME_AD_PrintFormat_ID + " = ?", null)
					.setParameters(request.getPrintFormatId())
					.first();
		}
		//	Get Report View
		if(request.getReportViewId() > 0) {
			reportView = new Query(Env.getCtx(), I_AD_ReportView.Table_Name, I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ?", null)
					.setParameters(request.getReportViewId())
					.first();
		}
		//	Get Default
		if(printFormat == null) {
			int reportViewId = 0;
			if(reportView != null) {
				reportViewId = reportView.getAD_ReportView_ID();
			}
			printFormat = MPrintFormat.get(Env.getCtx(), reportViewId, table.getAD_Table_ID());
		}
		//	Validate print format
		if(printFormat == null || printFormat.getAD_PrintFont_ID() <= 0) {
			throw new AdempiereException("@AD_PrintFormat_ID@ @NotFound@");
		}
		if(table.getAD_Table_ID() != printFormat.getAD_Table_ID()) {
			table = MTable.get(Env.getCtx(), printFormat.getAD_Table_ID());
		}

		//	
		ReportOutput.Builder builder = ReportOutput.newBuilder();
		MQuery query = ReportUtil.getReportQueryFromCriteria(process.getAD_Process_ID(), request.getTableName(), request.getFilters());
//		if(!Util.isEmpty(criteria.getWhereClause(), true)) {
//			query.addRestriction(criteria.getWhereClause());
//		}

		//	Run report engine
		ReportEngine reportEngine = new ReportEngine(Env.getCtx(), printFormat, query, printInformation);
		//	Set report view
		if(reportView != null) {
			reportEngine.setAD_ReportView_ID(reportView.getAD_ReportView_ID());
		} else {
			reportView = MReportView.get(Env.getCtx(), reportEngine.getAD_ReportView_ID());
		}
		//	Set Summary
		reportEngine.setSummary(request.getIsSummary());
		//	
		File reportFile = ReportUtil.createOutput(reportEngine, request.getReportType());
		if(reportFile != null
				&& reportFile.exists()) {
			String validFileName = FileUtil.getValidFileName(reportFile.getName());
			builder.setFileName(
					ValueManager.validateNull(validFileName)
				)
				.setName(
					ValueManager.validateNull(
						reportEngine.getName()
					)
				)
				.setMimeType(
					ValueManager.validateNull(
						MimeType.getMimeType(validFileName)
					)
				)
			;
			String headerName = Msg.getMsg(Env.getCtx(), "Report") + ": " + reportEngine.getName() + "  " + Env.getHeader(Env.getCtx(), 0);
			builder.setHeaderName(
				ValueManager.validateNull(headerName)
			);
			StringBuffer footerName = new StringBuffer ();
			footerName.append(Msg.getMsg(Env.getCtx(), "DataCols")).append("=")
				.append(reportEngine.getColumnCount())
				.append(", ").append(Msg.getMsg(Env.getCtx(), "DataRows")).append("=")
				.append(reportEngine.getRowCount());
			builder.setFooterName(
				ValueManager.validateNull(
					footerName.toString()
				)
			);
			//	Type
			builder.setReportType(request.getReportType());
			ByteString resultFile = ByteString.readFrom(new FileInputStream(reportFile));
			if(request.getReportType().endsWith("html")
					|| request.getReportType().endsWith("txt")) {
				builder.setOutputBytes(resultFile);
			}
			if(reportView != null) {
				builder.setReportViewId(reportView.getAD_ReportView_ID());
			}
			builder.setPrintFormatId(printFormat.getAD_PrintFormat_ID())
				.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				)
				.setOutputStream(resultFile)
			;
		}
		//	Return
		return builder;
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
				.forEach(translation -> {
					Translation.Builder translationBuilder = Translation.newBuilder();
					Struct.Builder translationValues = Struct.newBuilder();
					table.getColumnsAsList().stream().filter(column -> column.isTranslated()).forEach(column -> {
						Object value = translation.get_Value(column.getColumnName());
						if(value != null) {
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
	 * Convert Report View to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ListReportViewsResponse.Builder convertReportViewsList(Properties context, ListReportViewsRequest request) {
		ListReportViewsResponse.Builder builder = ListReportViewsResponse.newBuilder();
		//	Get entity
		if(Util.isEmpty(request.getTableName())
				&& request.getProcessId() <= 0) {
			throw new AdempiereException("@TableName@ / @AD_Process_ID@ @NotFound@");
		}
		String whereClause = null;
		List<Object> parameters = new ArrayList<>();
		//	For Table Name
		if(!Util.isEmpty(request.getTableName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if(table == null) {
				throw new AdempiereException("@TableName@ @NotFound@");
			}
			whereClause = "AD_Table_ID = ?";
			parameters.add(table.getAD_Table_ID());
		} else if(request.getProcessId() > 0) {
			whereClause = "EXISTS(SELECT 1 FROM AD_Process p WHERE p.UUID = ? AND p.AD_ReportView_ID = AD_ReportView.AD_ReportView_ID)";
			parameters.add(request.getProcessId());
		}

		String language = Env.getCtx().getProperty(Env.LANGUAGE);
		//	Get List
		new Query(
			Env.getCtx(),
			I_AD_ReportView.Table_Name,
			whereClause,
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy(I_AD_ReportView.COLUMNNAME_PrintName + ", " + I_AD_ReportView.COLUMNNAME_Name)
			.<MReportView>list().forEach(reportViewReference -> {
				ReportView.Builder reportViewBuilder = ReportView.newBuilder();
				String name = reportViewReference.getName();
				String description = reportViewReference.getDescription();
				
				// add translation
				if(!Util.isEmpty(language) && !Env.isBaseLanguage(Env.getCtx(), "")) {
					/*
					String translation = reportViewReference.get_Translation("Name");
					if(!Util.isEmpty(translation)) {
						name = translation;
					}
					translation = reportViewReference.get_Translation("Description");
					if(!Util.isEmpty(translation)) {
						description = translation;
					}
					*/

					// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/31
					PO translation = new Query(
							Env.getCtx(), 
							I_AD_ReportView.Table_Name + "_Trl",
							I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ? AND " +
							"IsTranslated = ? AND AD_Language = ?",
							null
						)
						.setParameters(reportViewReference.get_ID(), "Y", language)
						.setOnlyActiveRecords(true)
						.first();

					if (translation != null) {
						String nameTranslated = translation.get_ValueAsString(I_AD_ReportView.COLUMNNAME_Name);
						if(!Util.isEmpty(nameTranslated)) {
							name = nameTranslated;
						}
						String desciptionTranslated = translation.get_ValueAsString(I_AD_ReportView.COLUMNNAME_Description);
						if(!Util.isEmpty(desciptionTranslated)) {
							description = desciptionTranslated;
						}
					}
				}

				reportViewBuilder.setId(reportViewReference.getAD_ReportView_ID())
					.setName(
						ValueManager.validateNull(name)
					)
					.setDescription(
						ValueManager.validateNull(description)
					)
				;
				MTable table = MTable.get(Env.getCtx(), reportViewReference.getAD_Table_ID());
				reportViewBuilder.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				);
				//	add
				builder.addReportViews(reportViewBuilder);
			});
		//	Return
		return builder;
	}
	
	/**
	 * Convert Report View to gRPC
	 * @param request
	 * @return
	 */
	private ListDrillTablesResponse.Builder convertDrillTablesList(ListDrillTablesRequest request) {
		ListDrillTablesResponse.Builder builder = ListDrillTablesResponse.newBuilder();
		//	Get entity
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		String sql = "SELECT t.TableName, e.ColumnName, NULLIF(e.PO_PrintName,e.PrintName) "
				+ "FROM AD_Column c "
				+ " INNER JOIN AD_Column used ON (c.ColumnName=used.ColumnName)"
				+ " INNER JOIN AD_Table t ON (used.AD_Table_ID=t.AD_Table_ID AND t.IsView='N' AND t.AD_Table_ID <> c.AD_Table_ID)"
				+ " INNER JOIN AD_Column cKey ON (t.AD_Table_ID=cKey.AD_Table_ID AND cKey.IsKey='Y')"
				+ " INNER JOIN AD_Element e ON (cKey.ColumnName=e.ColumnName) "
				+ "WHERE c.AD_Table_ID=? AND c.IsKey='Y' "
				+ "ORDER BY 3";
			PreparedStatement pstmt = null;
			ResultSet resultSet = null;
			try {
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, table.getAD_Table_ID());
				resultSet = pstmt.executeQuery();
				while (resultSet.next()) {
					String drillTableName = resultSet.getString("TableName");
					String columnName = resultSet.getString("ColumnName");
					M_Element element = M_Element.get(Env.getCtx(), columnName);
					//	Add here
					DrillTable.Builder drillTable = DrillTable.newBuilder();
					drillTable.setTableName(
						ValueManager.validateNull(drillTableName)
					);
					String name = element.getPrintName();
					String poName = element.getPO_PrintName();
					if(!Env.isBaseLanguage(Env.getCtx(), "")) {
						String translation = element.get_Translation("PrintName");
						if(!Util.isEmpty(translation)) {
							name = translation;
						}
						translation = element.get_Translation("PO_PrintName");
						if(!Util.isEmpty(translation)) {
							poName = translation;
						}
					}
					if(!Util.isEmpty(poName)) {
						name = name + "/" + poName;
					}
					//	Print Name
					drillTable.setPrintName(
						ValueManager.validateNull(name)
					);
					//	Add to list
					builder.addDrillTables(drillTable);
				}
				resultSet.close();
				pstmt.close();
			} catch (SQLException e) {
				log.log(Level.SEVERE, sql, e);
			} finally {
				DB.close(resultSet, pstmt);
			}
		//	Return
		return builder;
	}
	
	/**
	 * Convert print formats to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ListPrintFormatsResponse.Builder convertPrintFormatsList(Properties context, ListPrintFormatsRequest request) {
		ListPrintFormatsResponse.Builder builder = ListPrintFormatsResponse.newBuilder();
		//	Get entity
		if(Util.isEmpty(request.getTableName())
				&& request.getProcessId() <= 0
				&& request.getReportViewId() <= 0) {
			throw new AdempiereException("@TableName@ / @AD_Process_ID@ / @AD_ReportView_ID@ @NotFound@");
		}
		String whereClause = null;
		List<Object> parameters = new ArrayList<>();
		//	For Table Name
		if(!Util.isEmpty(request.getTableName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			whereClause = "AD_Table_ID = ?";
			parameters.add(table.getAD_Table_ID());
		} else if(request.getProcessId() > 0) {
			whereClause = "EXISTS(SELECT 1 FROM AD_Process p WHERE p.AD_Process_ID = ? AND (p.AD_PrintFormat_ID = AD_PrintFormat.AD_PrintFormat_ID OR p.AD_ReportView_ID = AD_PrintFormat.AD_ReportView_ID))";
			parameters.add(request.getProcessId());
		} else if(request.getReportViewId() > 0) {
			MReportView reportView = new Query(Env.getCtx(), I_AD_ReportView.Table_Name, I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ?", null)
				.setParameters(request.getReportViewId())
				.first();
			whereClause = "AD_ReportView_ID = ?";
			parameters.add(reportView.getUUID());
		}
		//	Get List
		new Query(
			Env.getCtx(),
			I_AD_PrintFormat.Table_Name,
			whereClause,
			null
		)
			.setParameters(parameters)
			.setOrderBy(I_AD_PrintFormat.COLUMNNAME_Name)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.<MPrintFormat>list().forEach(printFormatReference -> {
				PrintFormat.Builder printFormatBuilder = PrintFormat.newBuilder()
					.setId(printFormatReference.getAD_PrintFormat_ID())
					.setName(
						ValueManager.validateNull(
							printFormatReference.getName()
						)
					)
					.setDescription(
						ValueManager.validateNull(
							printFormatReference.getDescription()
						)
					)
					.setIsDefault(printFormatReference.isDefault())
				;
				MTable table = MTable.get(Env.getCtx(), printFormatReference.getAD_Table_ID());
				printFormatBuilder.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				);
				if(printFormatReference.getAD_ReportView_ID() != 0) {
					printFormatBuilder.setReportViewId(printFormatReference.getAD_ReportView_ID());
				}
				//	add
				builder.addPrintFormats(printFormatBuilder);
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
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if(table == null
				|| table.getAD_Table_ID() == 0) {
			throw new AdempiereException("@AD_Table_ID@ @Invalid@");
		}
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
						changeLogList.forEach(changeLog -> {
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
		if ( value == null )
			return null;
		
		if ( DisplayType.isText(column.getAD_Reference_ID()) 
				|| column.getAD_Reference_ID() == DisplayType.List  
				|| column.getColumnName().equals("EntityType") 
				|| column.getColumnName().equals("AD_Language")) {
			return value;
		}
		else if ( DisplayType.isNumeric(column.getAD_Reference_ID()) ){
			return new BigDecimal(value);
		}
		else if (DisplayType.isID(column.getAD_Reference_ID()) ) {
			return Integer.valueOf(value);
		}	
		else if (DisplayType.YesNo == column.getAD_Reference_ID() ) {
			return "true".equalsIgnoreCase(value);
		}
		else if (DisplayType.Button == column.getAD_Reference_ID() && column.getAD_Reference_Value_ID() == 0) {
			return "true".equalsIgnoreCase(value) ? "Y" : "N";
		}
		else if (DisplayType.Button == column.getAD_Reference_ID() && column.getAD_Reference_Value_ID() != 0) {
			return value;
		}
		else if (DisplayType.isDate(column.getAD_Reference_ID())) {
			return Timestamp.valueOf(value);
		}
	//Binary,  Radio, RowID, Image not supported
		else 
			return null;
	}
	
	/**
	 * Create Chat Entry
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ChatEntry.Builder addChatEntry(CreateChatEntryRequest request) {
		

		if (Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		AtomicReference<MChatEntry> entryReference = new AtomicReference<>();
		Trx.run(transactionName -> {
			String tableName = request.getTableName();
			MTable table = MTable.get(Env.getCtx(), tableName);
			if (table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}
			PO entity = RecordUtil.getEntity(Env.getCtx(), tableName, request.getId(), transactionName);
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
	
	/**
	 * Convert Default Value from query
	 * @param sql
	 * @return
	 */
	private Object convertDefaultValue(String sql) {
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
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		//	Return values
		return defaultValue;
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
				Optional<MField> maybeField = customFields.stream().filter(customField -> customField.getAD_Field_ID() == fieldId).findFirst();
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
				Optional<MBrowseField> maybeField = customFields.stream().filter(customField -> customField.getAD_Browse_Field_ID() == browseFieldId).findFirst();
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
				Optional<MBrowseField> maybeField = customFields.stream().filter(customField -> customField.getAD_Browse_Field_ID() == browseFieldId).findFirst();
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
				Optional<MProcessPara> maybeParameter = customParameters.stream().filter(customField -> customField.getAD_Process_Para_ID() == processParameterId).findFirst();
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
				Optional<MProcessPara> maybeParameter = customParameters.stream().filter(customField -> customField.getAD_Process_Para_ID() == processParameterId).findFirst();
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
			request.getContextAttributes().getFieldsMap(),
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
	private DefaultValue.Builder getDefaultKeyAndValue(Map<String, Value> contextAttributes, String defaultValue, int referenceId, int referenceValueId, String columnName, int validationRuleId) {
		DefaultValue.Builder builder = DefaultValue.newBuilder();
		Struct.Builder values = Struct.newBuilder();
		builder.setValues(values);
		if(Util.isEmpty(defaultValue, true)) {
			return builder;
		}
		Object defaultValueAsObject = null;

		// Fill contex
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromValuesMap(windowNo, Env.getCtx(), contextAttributes);

		if(defaultValue.trim().startsWith("@SQL=")) {
			defaultValue = defaultValue.replace("@SQL=", "");
			defaultValue = Env.parseContext(Env.getCtx(), windowNo, defaultValue, false);
			defaultValueAsObject = convertDefaultValue(defaultValue);
		} else {
			defaultValueAsObject = Env.parseContext(Env.getCtx(), windowNo, defaultValue, false);
		}
		//	 For lookups
		if(defaultValueAsObject == null) {
			return builder;
		}

		//	Convert value from type
		if (DisplayType.isID(referenceId) || referenceId == DisplayType.Integer) {
			try {
				defaultValueAsObject = Integer.parseInt(String.valueOf(defaultValueAsObject));
			} catch (Exception e) {
				// log.warning(e.getLocalizedMessage());
			}
		} else if (DisplayType.isNumeric(referenceId)) {
			try {
				defaultValueAsObject = new BigDecimal(String.valueOf(defaultValueAsObject));
			} catch (Exception e) {
				// log.warning(e.getLocalizedMessage());
			}
		}
		if (ReferenceUtil.validateReference(referenceId) || DisplayType.Button == referenceId) {
			if(referenceId == DisplayType.List || columnName.equals("DocAction")) {
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

				MRefList referenceList = MRefList.get(Env.getCtx(), referenceValueId, defaultValueList, null);
				if (referenceList == null) {
					log.fine(Msg.parseTranslation(Env.getCtx(), "@AD_Ref_List_ID@ @NotFound@") + ": " + defaultValueList);
					return builder;
				}
				builder = convertDefaultValueFromResult(
					referenceList.getValue(),
					referenceList.getUUID(),
					referenceList.getValue(),
					referenceList.get_Translation(MRefList.COLUMNNAME_Name)
				);
				builder.setId(referenceList.getAD_Ref_List_ID());
			} else {
				if (DisplayType.Button == referenceId) {
					if (columnName.equals("Record_ID")) {
						defaultValueAsObject = Integer.valueOf(defaultValueAsObject.toString());
						int tableId = Env.getContextAsInt(Env.getCtx(), windowNo, I_AD_Table.COLUMNNAME_AD_Table_ID);
						MTable table = MTable.get(Env.getCtx(), tableId);
						String tableKeyColumn = table.getTableName() + "_ID";
						referenceId = DisplayType.TableDir;
						columnName = tableKeyColumn;
					} else {
						values.putFields(
							columnName,
							ValueManager.getValueFromObject(defaultValueAsObject).build()
						);
						builder.setValues(values);
						return builder;
					}
				}

				MLookupInfo lookupInfo = ReferenceUtil.getReferenceLookupInfo(referenceId, referenceValueId, columnName, validationRuleId);
				if(lookupInfo == null || Util.isEmpty(lookupInfo.QueryDirect, true)) {
					return builder;
				}
				String sql = MRole.getDefault(Env.getCtx(), false).addAccessSQL(
					lookupInfo.QueryDirect,
					lookupInfo.TableName,
					MRole.SQL_FULLYQUALIFIED,
					MRole.SQL_RO
				);
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
						if(keyValueType == Types.VARCHAR
								|| keyValueType == Types.NVARCHAR
								|| keyValueType == Types.CHAR
								|| keyValueType == Types.NCHAR) {
							keyValue = rs.getString(2);
						} else {
							keyValue = rs.getInt(1);
						}
						String uuid = null;
						//	Validate if exist UUID
						int uuidIndex = getColumnIndex(metaData, I_AD_Element.COLUMNNAME_UUID);
						if(uuidIndex != -1) {
							uuid = rs.getString(uuidIndex);
						}
						//	
						builder = convertDefaultValueFromResult(keyValue, uuid, rs.getString(2), rs.getString(3));
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

		Map<String, Object> contextAttributes = ValueManager.convertValuesMapToObjects(
			request.getContextAttributes().getFieldsMap()
		);

		return listLookupItems(
			reference,
			contextAttributes,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
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
	public static ListLookupItemsResponse.Builder listLookupItems(MLookupInfo reference, Map<String, Object> contextAttributes, int pageSize, String pageToken, String searchValue) {
		if (reference == null) {
			throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		}

		//	Fill Env.getCtx()
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromObjectMap(windowNo, Env.getCtx(), contextAttributes);

		String sql = reference.Query;
		sql = Env.parseContext(Env.getCtx(), windowNo, sql, false);
		if(Util.isEmpty(sql)
				&& !Util.isEmpty(reference.Query)) {
			throw new AdempiereException("@AD_Tab_ID@ @WhereClause@ @Unparseable@");
		}
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql,
				reference.TableName,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);
		
		List<Object> parameters = new ArrayList<>();
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, reference.TableName, searchValue, parameters);

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
				int uuidIndex = getColumnIndex(metaData, I_AD_Element.COLUMNNAME_UUID);
				if(uuidIndex != -1) {
					uuid = rs.getString(uuidIndex);
				}
				//	
				LookupItem.Builder valueObject = LookupUtil.convertObjectFromResult(keyValue, uuid, rs.getString(2), rs.getString(3));
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
	 * Verify if exist a column
	 * @param metaData
	 * @param columnName
	 * @return
	 * @throws SQLException 
	 */
	public static int getColumnIndex(ResultSetMetaData metaData, String columnName) throws SQLException {
		for(int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
			if(metaData.getColumnName(columnIndex).toLowerCase().equals(columnName.toLowerCase())) {
				return columnIndex;
			}
		}
		return -1;
	}



	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	private ListBrowserItemsResponse.Builder listBrowserItems(ListBrowserItemsRequest request) {
		ListBrowserItemsResponse.Builder builder = ListBrowserItemsResponse.newBuilder();
		MBrowse browser = ASPUtil.getInstance(Env.getCtx()).getBrowse(request.getId());
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return builder;
		}
		HashMap<String, Object> parameterMap = new HashMap<>();
		//	Populate map
		FilterManager.newInstance(request.getFilters()).getConditions().forEach(condition -> {
			parameterMap.put(condition.getColumnName(), condition.getValue());
		});

		//	Fill Env.getCtx()
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromStruct(windowNo, context, request.getContextAttributes());
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

		String orderByClause = OrderByUtil.getBrowseOrderBy(browser);
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
				Struct.Builder values = Struct.newBuilder();
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
						Value.Builder valueBuilder = Value.newBuilder();;
						//	Display Columns
						if(field == null) {
							String value = rs.getString(index);
							if(!Util.isEmpty(value)) {
								valueBuilder = ValueManager.getValueFromString(value);
							}
							values.putFields(
								columnName,
								valueBuilder.build()
							);
							continue;
						}
						//	From field
						String fieldColumnName = field.getAD_View_Column().getColumnName();
						Object value = rs.getObject(index);
						valueBuilder = ValueManager.getValueFromReference(
							value,
							field.getAD_Reference_ID()
						);
						values.putFields(
							fieldColumnName,
							valueBuilder.build()
						);
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
					}
				}
				//	
				entityBuilder.setValues(values);
				builder.addRecords(entityBuilder.build());
				recordCount++;
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		//	Set record counts
		if (builder.getRecordCount() <= 0) {
			builder.setRecordCount(recordCount);
		}
		//	Return
		return builder;
	}

	@Override
	public void runCallout(RunCalloutRequest request, StreamObserver<org.spin.backend.grpc.common.Callout> responseObserver) {
		try {
			if(request == null
					|| Util.isEmpty(request.getCallout())) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Callout Requested = " + request.getCallout());
			org.spin.backend.grpc.common.Callout.Builder calloutResponse = runcallout(request);
			responseObserver.onNext(calloutResponse.build());
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
	private org.spin.backend.grpc.common.Callout.Builder runcallout(RunCalloutRequest request) {
		org.spin.backend.grpc.common.Callout.Builder calloutBuilder = org.spin.backend.grpc.common.Callout.newBuilder();
		Trx.run(transactionName -> {
			if (request.getTabId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
			}
			MTab tab = MTab.get(Env.getCtx(), request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}

			MField field = null;
			Optional<MField> searchedValue = Arrays.asList(tab.getFields(false, null)).stream()
				.filter(searchField -> searchField.getAD_Column().getColumnName().equals(request.getColumnName()))
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
			Object oldValue = ValueManager.getObjectFromValue(
				request.getOldValue()
			);
			Object value = ValueManager.getObjectFromValue(
				request.getValue()
			);
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
			Arrays.asList(gridTab.getFields()).stream()
				.filter(fieldValue -> isValidChange(fieldValue))
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
			String isSalesTransaction = Env.getContext(tab.getCtx(), windowNo, "IsSOTrx", true);
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

			setAdditionalContext(request.getCallout(), windowNo, calloutBuilder);
		});
		return calloutBuilder;
	}

	/**
	 * Set additonal Env.getCtx() used by callouts
	 * TODO: Remove this method on future
	 * @param calloutClass
	 * @param windowNo
	 * @param calloutBuilder
	 * @return
	 */
	private org.spin.backend.grpc.common.Callout.Builder setAdditionalContext(String calloutClass, int windowNo,
		org.spin.backend.grpc.common.Callout.Builder calloutBuilder) {
		Class<CalloutOrder> clazz = org.compiere.model.CalloutOrder.class;
		String className = clazz.getName();

		if (calloutClass.startsWith(className)) {
			Struct.Builder contextValues = calloutBuilder.getValuesBuilder();
			if (calloutClass.equals("org.compiere.model.CalloutOrder.docType")) {
				// - OrderType
				String docSubTypeSO = Env.getContext(Env.getCtx(), windowNo, "OrderType");
				contextValues.putFields(
					"OrderType",
					ValueManager.getValueFromString(docSubTypeSO).build()
				);

				// - HasCharges
				String hasCharges = Env.getContext(Env.getCtx(), windowNo, "HasCharges");
				contextValues.putFields(
					"HasCharges",
					ValueManager.getValueFromStringBoolean(hasCharges).build()
				);
			}
			else if (calloutClass.equals("org.compiere.model.CalloutOrder.priceList")) {
				// - M_PriceList_Version_ID
				int priceListVersionId = Env.getContextAsInt(Env.getCtx(), windowNo, "M_PriceList_Version_ID");
				contextValues.putFields(
					"M_PriceList_Version_ID",
					ValueManager.getValueFromInteger(priceListVersionId).build()
				);
			}
			else if (calloutClass.equals("org.compiere.model.CalloutOrder.product")) {
				// - M_PriceList_Version_ID
				int priceListVersionId = Env.getContextAsInt(Env.getCtx(), windowNo, "M_PriceList_Version_ID");
				contextValues.putFields(
					"M_PriceList_Version_ID",
					ValueManager.getValueFromInteger(priceListVersionId).build()
				);
				
				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClass.equals("org.compiere.model.CalloutOrder.charge")) {
				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClass.equals("org.compiere.model.CalloutOrder.amt")) {
				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClass.equals("org.compiere.model.CalloutOrder.qty")) {
				// - UOMConversion
				String isConversion = Env.getContext(Env.getCtx(), "UOMConversion");
				contextValues.putFields(
					"UOMConversion",
					ValueManager.getValueFromStringBoolean(isConversion).build()
				);
			}
			calloutBuilder.setValues(contextValues);
		}

		return calloutBuilder;
	}
	
	/**
	 * Verify if a value has been changed
	 * @param gridField
	 * @return
	 */
	private boolean isValidChange(GridField gridField) {
		//	Standard columns
		if(gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Created) 
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_CreatedBy) 
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Updated) 
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UpdatedBy) 
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UUID)) {
			return false;
		}
		//	Oly Displayed
		if(!gridField.isDisplayed()) {
			return false;
		}
		//	Key
		if(gridField.isKey()) {
			return false;
		}

		//	validate with old value
		if(gridField.getOldValue() != null
				&& gridField.getValue() != null
				&& gridField.getValue().equals(gridField.getOldValue())) {
			return false;
		}
		//	Default
		return true;
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
	
	
	/**
	 * Convert Values from result
	 * @param keyValue
	 * @param uuidValue
	 * @param value
	 * @param displayValue
	 * @return
	 */
	private DefaultValue.Builder convertDefaultValueFromResult(Object keyValue, String uuidValue, String value, String displayValue) {
		DefaultValue.Builder builder = DefaultValue.newBuilder();
		Struct.Builder values = Struct.newBuilder();
		if(keyValue == null) {
			builder.setValues(values);
			return builder;
		}

		// Key Column
		if(keyValue instanceof Integer) {
			builder.setId((Integer) keyValue);
			values.putFields(
				LookupUtil.KEY_COLUMN_KEY,
				ValueManager.getValueFromInteger((Integer) keyValue).build()
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

		//  Fill Env.getCtx()
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		
		ContextManager.setContextWithAttributesFromStruct(windowNo, Env.getCtx(), request.getContextAttributes());
		
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
		MColumn keyColumn = columnsList.stream()
			.filter(column -> {
				return column.isKey();
			})
			.findFirst()
			.orElse(null);

		MColumn parentColumn = columnsList.stream()
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
		MColumn keyColumn = columnsList.stream()
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
			request.getEntitiesList().stream().forEach(entitySelection -> {
				PO entity = RecordUtil.getEntity(
					Env.getCtx(), table.getTableName(),
					entitySelection.getSelectionId(),
					transacctionName
				);
				if (entity == null || entity.get_ID() <= 0) {
					return;
				}
				// set new values
				entitySelection.getValues().getFieldsMap().entrySet().forEach(attribute -> {
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

			ListTreeNodesResponse.Builder recordsListBuilder = listTreeNodes(request);
			responseObserver.onNext(recordsListBuilder.build());
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

	private ListTreeNodesResponse.Builder listTreeNodes(ListTreeNodesRequest request) {
		if (Util.isEmpty(request.getTableName(), true) && request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();

		// get element id
		int elementId = request.getElementId();
		MTable table = null;
		// tab where clause
		String whereClause = null;
		if (request.getTabId() > 0) {
			MTab tab = MTab.get(context, request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}

			table = MTable.get(context, tab.getAD_Table_ID());
			final String whereTab = WhereClauseUtil.getWhereClauseFromTab(tab.getAD_Tab_ID());
			//	Fill context
			int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
			ContextManager.setContextWithAttributesFromStruct(windowNo, context, null);
			String parsedWhereClause = Env.parseContext(context, windowNo, whereTab, false);
			if (Util.isEmpty(parsedWhereClause, true) && !Util.isEmpty(whereTab, true)) {
				throw new AdempiereException("@AD_Tab_ID@ @WhereClause@ @Unparseable@");
			}
			whereClause = parsedWhereClause;
		} else {
			table = MTable.get(context, request.getTableName());
		}
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if (!MTree.hasTree(table.getAD_Table_ID())) {
			throw new AdempiereException("@AD_Table_ID@ + @AD_Tree_ID@ @NotFound@");
		}

		final int clientId = Env.getAD_Client_ID(context);
		int treeId = getDefaultTreeIdFromTableName(clientId, table.getTableName(), elementId);
		MTree tree = new MTree(context, treeId, false, true, whereClause, null);

		MTreeNode treeNode = tree.getRoot();

		int treeNodeId = request.getId();
		ListTreeNodesResponse.Builder builder = ListTreeNodesResponse.newBuilder();

		TreeType.Builder treeTypeBuilder = UserInterfaceConvertUtil.convertTreeType(tree.getTreeType());
		builder.setTreeType(treeTypeBuilder);

		// list child nodes
		Enumeration<?> childrens = Collections.emptyEnumeration();
		if (treeNodeId <= 0) {
			// get root children's
			childrens = treeNode.children();
			builder.setRecordCount(treeNode.getChildCount());
		} else {
			// get current node
			MTreeNode currentNode = treeNode.findNode(treeNodeId);
			if (currentNode == null) {
				throw new AdempiereException("@Node_ID@ @NotFound@");
			}
			childrens = currentNode.children();
			builder.setRecordCount(currentNode.getChildCount());
		}

		final boolean isWhitChilds = true;
		while (childrens.hasMoreElements()) {
			MTreeNode child = (MTreeNode) childrens.nextElement();
			TreeNode.Builder childBuilder = convertTreeNode(table, child, isWhitChilds);
			builder.addRecords(childBuilder.build());
		}

		return builder;
	}

	public TreeNode.Builder convertTreeNode(MTable table, MTreeNode treeNode, boolean isWithChildrens) {
		TreeNode.Builder builder = TreeNode.newBuilder();
		builder.setId(treeNode.getNode_ID())
			.setRecordId(treeNode.getNode_ID())
			.setSequence(treeNode.getSeqNo())
			.setName(
				ValueManager.validateNull(
					treeNode.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					treeNode.getDescription()
				)
			)
			.setParentId(treeNode.getParent_ID())
			.setIsSummary(treeNode.isSummary())
			.setIsActive(true)
		;

		if (isWithChildrens) {
			Enumeration<?> childrens = treeNode.children();
			while (childrens.hasMoreElements()) {
				MTreeNode child = (MTreeNode) childrens.nextElement();
				TreeNode.Builder childBuilder = convertTreeNode(table, child, isWithChildrens);
				builder.addChilds(childBuilder.build());
			}
		}

		return builder;
	}


	public int getDefaultTreeIdFromTableName(int clientId, String tableName, int elementId) {
		if(Util.isEmpty(tableName)) {
			return -1;
		}
		//
		Integer treeId = null;
		String whereClause = new String();
		//	Valid Accouting Element
		if (elementId > 0) {
			whereClause = " AND EXISTS ("
				+ "SELECT 1 FROM C_Element ae "
				+ "WHERE ae.C_Element_ID=" + elementId
				+ " AND tr.AD_Tree_ID=ae.AD_Tree_ID) "
			;
		}
		if(treeId == null || treeId == 0) {
			String sql = "SELECT tr.AD_Tree_ID "
				+ "FROM AD_Tree tr "
				+ "INNER JOIN AD_Table tb ON (tr.AD_Table_ID=tb.AD_Table_ID) "
				+ "WHERE tr.AD_Client_ID IN(0, ?) "
				+ "AND tb.TableName=? "
				+ "AND tr.IsActive='Y' "
				+ "AND tr.IsAllNodes='Y' "
				+ whereClause
				+ "ORDER BY tr.AD_Client_ID DESC, tr.IsDefault DESC, tr.AD_Tree_ID"
			;
			//	Get Tree
			treeId = DB.getSQLValue(null, sql, clientId, tableName);
		}
		//	Default Return
		return treeId;
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
