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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_FieldGroup;
import org.adempiere.core.domains.models.I_AD_Form;
import org.adempiere.core.domains.models.I_AD_Message;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_FieldGroup;
import org.adempiere.core.domains.models.X_AD_Reference;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.MBrowseFieldCustom;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MFieldCustom;
import org.compiere.model.MForm;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MMessage;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProcessParaCustom;
import org.compiere.model.MReportView;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.MWindow;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Browser;
import org.spin.backend.grpc.dictionary.ContextInfo;
import org.spin.backend.grpc.dictionary.DependentField;
import org.spin.backend.grpc.dictionary.DictionaryGrpc.DictionaryImplBase;
import org.spin.backend.grpc.dictionary.EntityRequest;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.FieldCondition;
import org.spin.backend.grpc.dictionary.FieldDefinition;
import org.spin.backend.grpc.dictionary.FieldGroup;
import org.spin.backend.grpc.dictionary.FieldRequest;
import org.spin.backend.grpc.dictionary.Form;
import org.spin.backend.grpc.dictionary.ListFieldsRequest;
import org.spin.backend.grpc.dictionary.ListFieldsResponse;
import org.spin.backend.grpc.dictionary.MessageText;
import org.spin.backend.grpc.dictionary.Process;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.ReferenceRequest;
import org.spin.backend.grpc.dictionary.ReportExportType;
import org.spin.backend.grpc.dictionary.Tab;
import org.spin.backend.grpc.dictionary.Window;
import org.spin.base.db.OrderByUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.dictionary.DictionaryConvertUtil;
import org.spin.base.dictionary.WindowUtil;
import org.spin.base.dictionary.custom.BrowseFieldCustomUtil;
import org.spin.base.dictionary.custom.FieldCustomUtil;
import org.spin.base.dictionary.custom.ProcessParaCustomUtil;
import org.spin.base.util.DictionaryUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.logic.DictionaryServiceLogic;
import org.spin.model.MADContextInfo;
import org.spin.model.MADFieldCondition;
import org.spin.model.MADFieldDefinition;
import org.spin.service.grpc.util.ValueManager;
import org.spin.util.ASPUtil;
import org.spin.util.AbstractExportFormat;
import org.spin.util.ReportExportHandler;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Dictionary service
 * Get all dictionary meta-data
 */
public class Dictionary extends DictionaryImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(Dictionary.class);
	
	@Override
	public void getWindow(EntityRequest request, StreamObserver<Window> responseObserver) {
		requestWindow(request, responseObserver, true);
	}
	
	@Override
	public void getTab(EntityRequest request, StreamObserver<Tab> responseObserver) {
		requestTab(request, responseObserver, true);
	}
	
	@Override
	public void getField(FieldRequest request, StreamObserver<Field> responseObserver) {
		try {
			Field.Builder fieldBuilder = getField(request);
			responseObserver.onNext(fieldBuilder.build());
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
	
	@Override
	public void getReference(ReferenceRequest request, StreamObserver<Reference> responseObserver) {
		try {
			Reference.Builder fieldBuilder = convertReference(Env.getCtx(), request);
			responseObserver.onNext(fieldBuilder.build());
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
	public void getProcess(EntityRequest request, StreamObserver<Process> responseObserver) {
		try {
			Process.Builder processBuilder = convertProcess(Env.getCtx(), request.getId(), true);
			responseObserver.onNext(processBuilder.build());
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
	public void getBrowser(EntityRequest request, StreamObserver<Browser> responseObserver) {
		try {
			Browser.Builder browserBuilder = convertBrowser(Env.getCtx(), request.getId(), true);
			responseObserver.onNext(browserBuilder.build());
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
	public void getForm(EntityRequest request, StreamObserver<Form> responseObserver) {
		try {
			Form.Builder formBuilder = convertForm(Env.getCtx(), request.getId());
			responseObserver.onNext(formBuilder.build());
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
	 * Request with parameters
	 */
	public void requestWindow(EntityRequest request, StreamObserver<Window> responseObserver, boolean withTabs) {
		try {
			Window.Builder windowBuilder = convertWindow(Env.getCtx(), request.getId(), withTabs);
			responseObserver.onNext(windowBuilder.build());
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
	 * Request with parameter
	 * @param request
	 * @param responseObserver
	 * @param withFields
	 */
	public void requestTab(EntityRequest request, StreamObserver<Tab> responseObserver, boolean withFields) {
		try {
			Tab.Builder tabBuilder = convertTab(Env.getCtx(), request.getId(), withFields);
			responseObserver.onNext(tabBuilder.build());
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
	 * Request Window: can be only window or child
	 * @param request
	 * @param uuid
	 * @param id
	 * @param withTabs
	 */
	private Window.Builder convertWindow(Properties context, int id, boolean withTabs) {
		MWindow window = MWindow.get(context, id);
		if(window == null) {
			return Window.newBuilder();
		}
		return convertWindow(context, window, withTabs);
	}
	
	/**
	 * Request Form from uuid
	 * @param context
	 * @param uuid
	 * @param id
	 */
	private Form.Builder convertForm(Properties context, int id) {
		String whereClause = null;
		Object parameter = null;
		whereClause = I_AD_Form.COLUMNNAME_AD_Form_ID + " = ?";
		parameter = id;
		MForm form = new Query(context, I_AD_Form.Table_Name, whereClause, null)
				.setParameters(parameter)
				.setOnlyActiveRecords(true)
				.first();
		return convertForm(context, form);
	}
	
	/**
	 * Convert Window from Window Model
	 * @param form
	 * @return
	 */
	private Form.Builder convertForm(Properties context, MForm form) {
		Form.Builder builder = Form.newBuilder();
		if (form == null) {
			return builder;
		}
		//	
		builder.setId(form.getAD_Form_ID())
			.setUuid(
				ValueManager.validateNull(form.getUUID()))
			.setName(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Help)
				)
			)
			.setIsActive(form.isActive())
		;
		//	File Name
		String fileName = form.getClassname();
		if(!Util.isEmpty(fileName)) {
			int endIndex = fileName.lastIndexOf(".");
			int beginIndex = fileName.lastIndexOf("/");
			if(beginIndex == -1) {
				beginIndex = fileName.lastIndexOf(".");
				endIndex = -1;
			}
			if(beginIndex == -1) {
				beginIndex = 0;
			} else {
				beginIndex++;
			}
			if(endIndex == -1) {
				endIndex = fileName.length();
			}
			//	Set
			builder.setFileName(
				ValueManager.validateNull(
					fileName.substring(beginIndex, endIndex))
				)
			;
		}
		//	Add to recent Item
		org.spin.base.dictionary.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			form.getAD_Form_ID()
		);
		//	return
		return builder;
	}

	/**
	 * Convert Window from Window Model
	 * @param window
	 * @param withTabs
	 * @return
	 */
	private Window.Builder convertWindow(Properties context, MWindow window, boolean withTabs) {
		if (window == null) {
			return Window.newBuilder();
		}
		window = ASPUtil.getInstance(context).getWindow(window.getAD_Window_ID());
		Window.Builder builder = null;
		ContextInfo.Builder contextInfoBuilder = convertContextInfo(context, window.getAD_ContextInfo_ID());
		//	
		builder = Window.newBuilder()
			.setId(window.getAD_Window_ID())
			.setUuid(
				ValueManager.validateNull(window.getUUID())
			)
			.setName(window.getName())
			.setDescription(
				ValueManager.validateNull(window.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(window.getHelp())
			)
			.setWindowType(
				ValueManager.validateNull(window.getWindowType())
			)
			.setIsSalesTransaction(window.isSOTrx())
			.setIsActive(window.isActive())
		;
		if(contextInfoBuilder != null) {
			builder.setContextInfo(contextInfoBuilder.build());
		}
		//	With Tabs
		if(withTabs) {
			Boolean isShowAcct = MRole.getDefault(context, false).isShowAcct();
//			List<Tab.Builder> tabListForGroup = new ArrayList<>();
			List<MTab> tabs = ASPUtil.getInstance(context).getWindowTabs(window.getAD_Window_ID());
			for(MTab tab : tabs) {
				if(!tab.isActive()) {
					continue;
				}
				// role without permission to accounting
				if (tab.isInfoTab() && !isShowAcct) {
					continue;
				}
				Tab.Builder tabBuilder = convertTab(context, tab, tabs, withTabs);
				builder.addTabs(tabBuilder.build());
//				//	Get field group
//				int [] fieldGroupIdArray = getFieldGroupIdsFromTab(tab.getAD_Tab_ID());
//				if(fieldGroupIdArray != null) {
//					for(int fieldGroupId : fieldGroupIdArray) {
//						Tab.Builder tabFieldGroup = convertTab(context, tab, false);
//						FieldGroup.Builder fieldGroup = convertFieldGroup(context, fieldGroupId);
//						tabFieldGroup.setFieldGroup(fieldGroup);
//						tabFieldGroup.setName(fieldGroup.getName());
//						tabFieldGroup.setDescription("");
//						tabFieldGroup.setUuid(tabFieldGroup.getUuid() + "---");
//						//	Add to list
//						tabListForGroup.add(tabFieldGroup);
//					}
//				}
			}
			//	Add Field Group Tabs
//			for(Tab.Builder tabFieldGroup : tabListForGroup) {
//				builder.addTabs(tabFieldGroup.build());
//			}
		}
		//	Add to recent Item
		org.spin.base.dictionary.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Window,
			window.getAD_Window_ID()
		);
		//	return
		return builder;
	}



//	/**
//	 * Get Field group from Tab
//	 * @param tabId
//	 * @return
//	 */
//	private int[] getFieldGroupIdsFromTab(int tabId) {
//		return DB.getIDsEx(null, "SELECT f.AD_FieldGroup_ID "
//				+ "FROM AD_Field f "
//				+ "INNER JOIN AD_FieldGroup fg ON(fg.AD_FieldGroup_ID = f.AD_FieldGroup_ID) "
//				+ "WHERE f.AD_Tab_ID = ? "
//				+ "AND fg.FieldGroupType = ? "
//				+ "GROUP BY f.AD_FieldGroup_ID", tabId, X_AD_FieldGroup.FIELDGROUPTYPE_Tab);
//	}
	
	/**
	 * Convert Tabs from UUID
	 * @param uuid
	 * @param withFields
	 * @return
	 */
	private Tab.Builder convertTab(Properties context, int id, boolean withFields) {
		MTab tab = MTab.get(context, id);
		//	Convert
		return convertTab(context, tab, withFields);
	}
	
	/**
	 * Convert Process from UUID
	 * @param id
	 * @param withParameters
	 * @return
	 */
	private Process.Builder convertProcess(Properties context, int id, boolean withParameters) {
		MProcess process = MProcess.get(context, id);
		if(process == null) {
			return Process.newBuilder();
		}
		//	Convert
		return convertProcess(context, process, withParameters);
	}
	
	/**
	 * Convert Browser from UUID
	 * @param uuid
	 * @param withFields
	 * @return
	 */
	private Browser.Builder convertBrowser(Properties context, int id, boolean withFields) {
		MBrowse browser = ASPUtil.getInstance(context).getBrowse(id);
		//	Convert
		return convertBrowser(context, browser, withFields);
	}
	
	/**
	 * Convert Model tab to builder tab
	 * @param tab
	 * @return
	 */
	private Tab.Builder convertTab(Properties context, MTab tab, boolean withFields) {
		return convertTab(context, tab, null, withFields);
	}
	
	/**
	 * Convert Model tab to builder tab
	 * @param tab
	 * @return
	 */
	private Tab.Builder convertTab(Properties context, MTab tab, List<MTab> tabs, boolean withFields) {
		if (tab == null) {
			return Tab.newBuilder();
		}

		int tabId = tab.getAD_Tab_ID();
		tab = ASPUtil.getInstance(context).getWindowTab(tab.getAD_Window_ID(), tabId);

		int parentTabId = 0;
		// root tab has no parent
		if (tab.getTabLevel() > 0) {
			parentTabId = WindowUtil.getDirectParentTabId(tab.getAD_Window_ID(), tabId);
		}

		//	Get table attributes
		MTable table = MTable.get(context, tab.getAD_Table_ID());
		boolean isReadOnly = tab.isReadOnly() || table.isView();
		int contextInfoId = tab.getAD_ContextInfo_ID();
		if(contextInfoId <= 0) {
			contextInfoId = table.getAD_ContextInfo_ID();
		}

		// get where clause including link column and parent column
		String whereClause = WhereClauseUtil.getTabWhereClauseFromParentTabs(context, tab, tabs);

		//	create build
		Tab.Builder builder = Tab.newBuilder()
			.setId(tab.getAD_Tab_ID())
			.setUuid(
				ValueManager.validateNull(tab.getUUID())
			)
			.setName(
				ValueManager.validateNull(tab.getName())
			)
			.setDescription(
				ValueManager.validateNull(tab.getDescription())
			)
			.setHelp(ValueManager.validateNull(tab.getHelp()))
			.setAccessLevel(Integer.parseInt(table.getAccessLevel()))
			.setCommitWarning(
				ValueManager.validateNull(tab.getCommitWarning())
			)
			.setSequence(tab.getSeqNo())
			.setDisplayLogic(
				ValueManager.validateNull(tab.getDisplayLogic())
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(tab.getReadOnlyLogic())
			)
			.setIsAdvancedTab(tab.isAdvancedTab())
			.setIsDeleteable(table.isDeleteable())
			.setIsDocument(table.isDocument())
			.setIsHasTree(tab.isHasTree())
			.setIsInfoTab(tab.isInfoTab())
			.setIsInsertRecord(!isReadOnly && tab.isInsertRecord())
			.setIsReadOnly(isReadOnly)
			.setIsSingleRow(tab.isSingleRow())
			.setIsSortTab(tab.isSortTab())
			.setIsTranslationTab(tab.isTranslationTab())
			.setIsView(table.isView())
			.setTabLevel(tab.getTabLevel())
			.setTableName(
				ValueManager.validateNull(table.getTableName())
			)
			.setParentTabId(parentTabId)
			.setIsChangeLog(table.isChangeLog())
			.setIsActive(tab.isActive())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(whereClause).orElse("")
					+ Optional.ofNullable(tab.getOrderByClause()).orElse("")
				)
			)
			.addAllKeyColumns(
				Arrays.asList(
					table.getKeyColumns()
				)
			)
		;

		//	For link
		if(contextInfoId > 0) {
			ContextInfo.Builder contextInfoBuilder = convertContextInfo(context, contextInfoId);
			builder.setContextInfo(contextInfoBuilder.build());
		}
		//	Parent Link Column Name
		if(tab.getParent_Column_ID() > 0) {
			MColumn column = MColumn.get(context, tab.getParent_Column_ID());
			builder.setParentColumnName(column.getColumnName());
		}
		//	Link Column Name
		if(tab.getAD_Column_ID() > 0) {
			MColumn column = MColumn.get(context, tab.getAD_Column_ID());
			builder.setLinkColumnName(column.getColumnName());
		}
		if(tab.isSortTab()) {
			//	Sort Column
			if(tab.getAD_ColumnSortOrder_ID() > 0) {
				MColumn column = MColumn.get(context, tab.getAD_ColumnSortOrder_ID());
				builder.setSortOrderColumnName(column.getColumnName());
			}
			//	Sort Yes / No
			if(tab.getAD_ColumnSortYesNo_ID() > 0) {
				MColumn column = MColumn.get(context, tab.getAD_ColumnSortYesNo_ID());
				builder.setSortYesNoColumnName(column.getColumnName());
			}
		}
		//	Process
		List<MProcess> processList = WindowUtil.getProcessActionFromTab(context, tab);
		if(processList != null
				&& processList.size() > 0) {
			for(MProcess process : processList) {
				// get process associated without parameters
				Process.Builder processBuilder = convertProcess(context, process, false);
				builder.addProcesses(processBuilder.build());
			}
		}
		if(withFields) {
			for(MField field : ASPUtil.getInstance(context).getWindowFields(tab.getAD_Tab_ID())) {
				Field.Builder fieldBuilder = convertField(context, field, false);
				builder.addFields(fieldBuilder.build());
			}
		}
		//	
		return builder;
	}



	/**
	 * Convert Context Info to builder
	 * @param contextInfoId
	 * @return
	 */
	public static ContextInfo.Builder convertContextInfo(Properties context, int contextInfoId) {
		ContextInfo.Builder builder = ContextInfo.newBuilder();
		if(contextInfoId > 0) {
			MADContextInfo contextInfoValue = MADContextInfo.getById(context, contextInfoId);
			MMessage message = MMessage.get(context, contextInfoValue.getAD_Message_ID());
			//	Get translation
			String msgText = null;
			String msgTip = null;
			String language = Env.getAD_Language(context);
			if(!Util.isEmpty(language)) {
				msgText = message.get_Translation(I_AD_Message.COLUMNNAME_MsgText, language);
				msgTip = message.get_Translation(I_AD_Message.COLUMNNAME_MsgTip, language);
			}
			//	Validate for default
			if(Util.isEmpty(msgText)) {
				msgText = message.getMsgText();
			}
			if(Util.isEmpty(msgTip)) {
				msgTip = message.getMsgTip();
			}
			//	Add message text
			MessageText.Builder messageText = MessageText.newBuilder();
			if (message != null) {
				messageText
					.setId(message.getAD_Message_ID())
					.setValue(
						ValueManager.validateNull(message.getValue())
					)
					.setMessageText(
						ValueManager.validateNull(msgText)
					)
					.setMessageTip(
						ValueManager.validateNull(msgTip)
					)
				;
			}
			builder = ContextInfo.newBuilder()
				.setId(contextInfoValue.getAD_ContextInfo_ID())
				.setUuid(
					ValueManager.validateNull(contextInfoValue.getUUID())
				)
				.setName(
					ValueManager.validateNull(contextInfoValue.getName())
				)
				.setDescription(
					ValueManager.validateNull(contextInfoValue.getDescription())
				)
				.setMessageText(messageText.build())
				.setSqlStatement(
					ValueManager.validateNull(contextInfoValue.getSQLStatement())
				)
			;
		}
		return builder;
	}
	
	/**
	 * Convert process to builder
	 * @param process
	 * @return
	 */
	public static Process.Builder convertProcess(Properties context, MProcess process, boolean withParams) {
		if (process == null) {
			return Process.newBuilder();
		}
		process = ASPUtil.getInstance(context).getProcess(process.getAD_Process_ID());
		List<MProcessPara> parametersList = ASPUtil.getInstance(context).getProcessParameters(process.getAD_Process_ID());

		Process.Builder builder = Process.newBuilder()
			.setId(process.getAD_Process_ID())
			.setUuid(
				ValueManager.validateNull(process.getUUID())
			)
			.setValue(
				ValueManager.validateNull(process.getValue())
			)
			.setName(
				ValueManager.validateNull(process.getName())
			)
			.setDescription(
				ValueManager.validateNull(process.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(process.getHelp())
			)
			.setAccessLevel(Integer.parseInt(process.getAccessLevel()))
			.setIsDirectPrint(process.isDirectPrint())
			.setIsReport(process.isReport())
			.setIsActive(process.isActive())
			.setIsHaveParameres(
				parametersList != null && parametersList.size() > 0
			)
		;

		if (process.getAD_Browse_ID() > 0) {
			builder.setBrowserId(process.getAD_Browse_ID());
		}
		if (process.getAD_Form_ID() > 0) {
			builder.setFormId(process.getAD_Form_ID());
		}
		if (process.getAD_Workflow_ID() > 0) {
			builder.setWorkflowId(process.getAD_Workflow_ID());
		}
		//	Report Types
		if(process.isReport()) {
			MReportView reportView = null;
			if(process.getAD_ReportView_ID() > 0) {
				reportView = MReportView.get(context, process.getAD_ReportView_ID());
			}
			ReportExportHandler exportHandler = new ReportExportHandler(Env.getCtx(), reportView);
			for(AbstractExportFormat reportType : exportHandler.getExportFormatList()) {
				ReportExportType.Builder reportExportType = ReportExportType.newBuilder()
					.setName(
						ValueManager.validateNull(reportType.getName())
					)
					.setDescription(
						ValueManager.validateNull(reportType.getName())
					)
					.setType(
						ValueManager.validateNull(reportType.getExtension())
					)
				;
				builder.addReportExportTypes(reportExportType.build());
			}
		}
		//	For parameters
		if(withParams && parametersList != null && parametersList.size() > 0) {
			String language = context.getProperty(Env.LANGUAGE);
			for(MProcessPara parameter : parametersList) {
				// TODO: Remove conditional with fix the issue https://github.com/solop-develop/backend/issues/28
				if(!Language.isBaseLanguage(language)) {
					//	Name
					String value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Name, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Name, value);
					}
					//	Description
					value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Description, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Description, value);
					}
					//	Help
					value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Help, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Help, value);
					}
				}
				
				Field.Builder fieldBuilder = convertProcessParameter(context, parameter);
				builder.addParameters(fieldBuilder.build());
			}
		}

		//	Add to recent Item
		if (process.isReport()) {
			org.spin.base.dictionary.DictionaryUtil.addToRecentItem(
				MMenu.ACTION_Report,
				process.getAD_Process_ID()
			);
		} else {
			org.spin.base.dictionary.DictionaryUtil.addToRecentItem(
				MMenu.ACTION_Process,
				process.getAD_Process_ID()
			);
		}

		return builder;
	}
	
	/**
	 * Convert process to builder
	 * @param browser
	 * @param withFields
	 * @return
	 */
	private Browser.Builder convertBrowser(Properties context, MBrowse browser, boolean withFields) {
		if (browser == null) {
			return Browser.newBuilder();
		}
		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String orderByClause = OrderByUtil.getBrowseOrderBy(browser);
		Browser.Builder builder = Browser.newBuilder()
			.setId(browser.getAD_Browse_ID())
			.setUuid(
				ValueManager.validateNull(browser.getUUID())
			)
			.setValue(
				ValueManager.validateNull(browser.getValue())
			)
			.setName(browser.getName())
			.setDescription(
				ValueManager.validateNull(browser.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(browser.getHelp())
			)
			.setAccessLevel(Integer.parseInt(browser.getAccessLevel()))
			.setIsActive(browser.isActive())
			.setIsCollapsibleByDefault(browser.isCollapsibleByDefault())
			.setIsDeleteable(browser.isDeleteable())
			.setIsExecutedQueryByDefault(browser.isExecutedQueryByDefault())
			.setIsSelectedByDefault(browser.isSelectedByDefault())
			.setIsShowTotal(browser.isShowTotal())
			.setIsUpdateable(browser.isUpdateable())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(query).orElse("")
					+ Optional.ofNullable(browser.getWhereClause()).orElse("")
					+ Optional.ofNullable(orderByClause).orElse("")
				)
			)
		;
		//	Set fied key
		MBrowseField fieldKey = browser.getFieldKey();
		if (fieldKey != null && fieldKey.get_ID() > 0) {
			MViewColumn viewColumn = MViewColumn.getById(context, fieldKey.getAD_View_Column_ID(), null);
			builder.setFieldKey(
				ValueManager.validateNull(
					viewColumn.getColumnName()
				)
			);
		}
		//	Set View UUID
		if(browser.getAD_View_ID() > 0) {
			builder.setViewId(browser.getAD_View_ID());
		}
		// set table name
		if (browser.getAD_Table_ID() > 0) {
			MTable table = MTable.get(Env.getCtx(), browser.getAD_Table_ID());
			builder.setTableName(
				ValueManager.validateNull(table.getTableName())
			);
		}
		//	Window Reference
		if(browser.getAD_Window_ID() > 0) {
			MWindow window = ASPUtil.getInstance(context).getWindow(browser.getAD_Window_ID());
			Window.Builder windowBuilder = convertWindow(context, window, false);
			builder.setWindow(windowBuilder.build());
		}
		//	Process Reference
		if(browser.getAD_Process_ID() > 0) {
			Process.Builder processBuilder = convertProcess(context, MProcess.get(context, browser.getAD_Process_ID()), false);
			builder.setProcess(processBuilder.build());
		}
		//	For parameters
		if(withFields) {
			for(MBrowseField field : ASPUtil.getInstance(context).getBrowseFields(browser.getAD_Browse_ID())) {
				Field.Builder fieldBuilder = convertBrowseField(context, field);
				builder.addFields(fieldBuilder.build());
			}
		}
		//	Add to recent Item
		org.spin.base.dictionary.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_SmartBrowse,
			browser.getAD_Browse_ID()
		);
		return builder;
	}



	/**
	 * Convert Process Parameter
	 * @param processParameter
	 * @return
	 */
	public static Field.Builder convertProcessParameter(Properties context, MProcessPara processParameter) {
		if (processParameter == null) {
			return Field.newBuilder();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(processParameter.getAD_Process_Para_ID())
			.setUuid(
				ValueManager.validateNull(processParameter.getUUID())
			)
			.setName(
				ValueManager.validateNull(processParameter.getName())
			)
			.setDescription(
				ValueManager.validateNull(processParameter.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(processParameter.getHelp())
			)
			.setColumnName(
				ValueManager.validateNull(processParameter.getColumnName())
			)
			.setElementName(
				ValueManager.validateNull(processParameter.getColumnName())
			)
			.setDefaultValue(
				ValueManager.validateNull(processParameter.getDefaultValue())
			)
			.setDefaultValueTo(
				ValueManager.validateNull(processParameter.getDefaultValue2())
			)
			.setDisplayLogic(
				ValueManager.validateNull(processParameter.getDisplayLogic())
			)
			.setDisplayType(processParameter.getAD_Reference_ID())
			.setIsDisplayed(true)
			.setIsInfoOnly(processParameter.isInfoOnly())
			.setIsMandatory(processParameter.isMandatory())
			.setIsRange(processParameter.isRange())
			.setReadOnlyLogic(
				ValueManager.validateNull(processParameter.getReadOnlyLogic())
			)
			.setSequence(processParameter.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(processParameter.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(processParameter.getValueMin())
			)
			.setVFormat(
				ValueManager.validateNull(processParameter.getVFormat())
			)
			.setFieldLength(processParameter.getFieldLength())
			.setIsActive(processParameter.isActive())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(processParameter.getDefaultValue()).orElse("")
					+ Optional.ofNullable(processParameter.getDefaultValue2()).orElse("")
				)
			)
		;
		//	
		int displayTypeId = processParameter.getAD_Reference_ID();
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = processParameter.getAD_Reference_Value_ID();
			//	Validation Code
			int validationRuleId = processParameter.getAD_Val_Rule_ID();

			String columnName = processParameter.getColumnName();
			if (processParameter.getAD_Element_ID() > 0) {
				columnName = processParameter.getAD_Element().getColumnName();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, columnName, validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			}
		}

		MProcessParaCustom processParaCustom = ProcessParaCustomUtil.getProcessParaCustom(processParameter.getAD_Process_Para_ID());
		if (processParaCustom != null && processParaCustom.isActive()) {
			// ASP default displayed field as panel
			if (processParaCustom.get_ColumnIndex(org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					ValueManager.validateNull(
						processParaCustom.get_ValueAsString(
							org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
		}

		List<DependentField> dependentProcessParameters = generateDependentProcessParameters(processParameter);
		builder.addAllDependentFields(dependentProcessParameters);

		return builder;
	}

	public static List<DependentField> generateDependentProcessParameters(MProcessPara processParameter) {
		List<DependentField> depenentFieldsList = new ArrayList<>();

		String parentColumnName = processParameter.getColumnName();

		MProcess process = ASPUtil.getInstance().getProcess(processParameter.getAD_Process_ID());
		List<MProcessPara> parametersList = ASPUtil.getInstance().getProcessParameters(processParameter.getAD_Process_ID());

		parametersList.stream()
			.filter(currentParameter -> {
				if (!currentParameter.isActive()) {
					return false;
				}
				// Display Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentParameter.getDisplayLogic())) {
					return true;
				}
				// Default Value of Column
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentParameter.getDefaultValue())) {
					return true;
				}
				// ReadOnly Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentParameter.getReadOnlyLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentParameter.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(Env.getCtx(), currentParameter.getAD_Val_Rule_ID());
					if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentParameter -> {
				DependentField.Builder builder = DependentField.newBuilder();
				builder.setContainerId(process.getAD_Process_ID());
				builder.setContainerName(process.getName());

				builder.setId(currentParameter.getAD_Process_Para_ID());
				builder.setColumnName(currentParameter.getColumnName());

				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}

	/**
	 * Convert Browse Field
	 * @param browseField
	 * @return
	 */
	private Field.Builder convertBrowseField(Properties context, MBrowseField browseField) {
		if (browseField == null) {
			return Field.newBuilder();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(browseField.getAD_Browse_Field_ID())
			.setUuid(
				ValueManager.validateNull(browseField.getUUID())
			)
			.setName(
				ValueManager.validateNull(browseField.getName())
			)
			.setDescription(
				ValueManager.validateNull(browseField.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(browseField.getHelp())
			)
			.setDefaultValue(
				ValueManager.validateNull(browseField.getDefaultValue())
			)
			.setDefaultValueTo(
				ValueManager.validateNull(browseField.getDefaultValue2())
			)
			.setDisplayLogic(
				ValueManager.validateNull(browseField.getDisplayLogic())
			)
			.setDisplayType(browseField.getAD_Reference_ID())
			.setIsDisplayed(browseField.isDisplayed())
			.setIsQueryCriteria(browseField.isQueryCriteria())
			.setIsOrderBy(browseField.isOrderBy())
			.setIsInfoOnly(browseField.isInfoOnly())
			.setIsMandatory(browseField.isMandatory())
			.setIsRange(browseField.isRange())
			.setIsReadOnly(browseField.isReadOnly())
			.setReadOnlyLogic(
				ValueManager.validateNull(browseField.getReadOnlyLogic())
			)
			.setIsKey(browseField.isKey())
			.setIsIdentifier(browseField.isIdentifier())
			.setSeqNoGrid(browseField.getSeqNoGrid())
			.setSequence(browseField.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(browseField.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(browseField.getValueMin())
			)
			.setVFormat(
				ValueManager.validateNull(browseField.getVFormat())
			)
			.setIsActive(browseField.isActive())
			.setCallout(
				ValueManager.validateNull(browseField.getCallout())
			)
			.setFieldLength(browseField.getFieldLength())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(browseField.getDefaultValue()).orElse("")
					+ Optional.ofNullable(browseField.getDefaultValue2()).orElse("")
				)
			)
		;
		
		String elementName = null;
		MViewColumn viewColumn = MViewColumn.getById(context, browseField.getAD_View_Column_ID(), null);
		builder.setColumnName(
			ValueManager.validateNull(viewColumn.getColumnName())
		);
		if(viewColumn.getAD_Column_ID() != 0) {
			MColumn column = MColumn.get(context, viewColumn.getAD_Column_ID());
			elementName = column.getColumnName();
			builder.setColumnId(column.getAD_Column_ID());
		}

		//	Default element
		if(Util.isEmpty(elementName)) {
			elementName = browseField.getAD_Element().getColumnName();
		}
		builder.setElementName(ValueManager.validateNull(elementName))
			.setElementId(browseField.getAD_Element_ID())
		;

		//	
		int displayTypeId = browseField.getAD_Reference_ID();
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = browseField.getAD_Reference_Value_ID();
			//	Validation Code
			int validationRuleId = browseField.getAD_Val_Rule_ID();

			// TODO: Verify this conditional with "elementName" variable
			String columnName = browseField.getAD_Element().getColumnName();
			if (viewColumn.getAD_Column_ID() > 0) {
				MColumn column = MColumn.get(context, viewColumn.getAD_Column_ID());
				columnName = column.getColumnName();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, columnName, validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		}

		MBrowseFieldCustom browseFieldCustom = BrowseFieldCustomUtil.getBrowseFieldCustom(browseField.getAD_Browse_Field_ID());
		if (browseFieldCustom != null && browseFieldCustom.isActive()) {
			// ASP default displayed field as panel
			if (browseFieldCustom.get_ColumnIndex(org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					ValueManager.validateNull(
						browseFieldCustom.get_ValueAsString(
							org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
			// ASP default displayed field as table
			if (browseFieldCustom.get_ColumnIndex(org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsTable(
					ValueManager.validateNull(
						browseFieldCustom.get_ValueAsString(
							org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME
						)
					)
				);
			}
		}

		List<DependentField> dependentBrowseFieldsList = generateDependentBrowseFields(browseField);
		builder.addAllDependentFields(dependentBrowseFieldsList);

		return builder;
	}

	private List<DependentField> generateDependentBrowseFields(MBrowseField browseField) {
		List<DependentField> depenentFieldsList = new ArrayList<>();

		MViewColumn viewColumn = MViewColumn.getById(Env.getCtx(), browseField.getAD_View_Column_ID(), null);
		String parentColumnName = viewColumn.getColumnName();

		String elementName = null;
		if(viewColumn.getAD_Column_ID() != 0) {
			MColumn column = MColumn.get(Env.getCtx(), viewColumn.getAD_Column_ID());
			elementName = column.getColumnName();
		}
		if(Util.isEmpty(elementName, true)) {
			elementName = browseField.getAD_Element().getColumnName();
		}
		String parentElementName = elementName;

		MBrowse browse = ASPUtil.getInstance().getBrowse(browseField.getAD_Browse_ID());
		List<MBrowseField> browseFieldsList = ASPUtil.getInstance().getBrowseFields(browseField.getAD_Browse_ID());

		browseFieldsList.stream()
			.filter(currentBrowseField -> {
				if(!currentBrowseField.isActive()) {
					return false;
				}
				// Display Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDisplayLogic())
					|| DictionaryUtil.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDisplayLogic())) {
					return true;
				}
				// Default Value
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDefaultValue())
					|| DictionaryUtil.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDefaultValue())) {
					return true;
				}
				// Default Value 2
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDefaultValue2())
					|| DictionaryUtil.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDefaultValue2())) {
					return true;
				}
				// ReadOnly Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getReadOnlyLogic())
					|| DictionaryUtil.isUseParentColumnOnContext(parentElementName, currentBrowseField.getReadOnlyLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentBrowseField.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(Env.getCtx(), currentBrowseField.getAD_Val_Rule_ID());
					if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())
						|| DictionaryUtil.isUseParentColumnOnContext(parentElementName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentBrowseField -> {
				DependentField.Builder builder = DependentField.newBuilder();

				builder.setContainerId(browse.getAD_Browse_ID());
				builder.setContainerName(browse.getName());
				builder.setId(currentBrowseField.getAD_Browse_Field_ID());
				
				MViewColumn currentViewColumn = MViewColumn.getById(Env.getCtx(), currentBrowseField.getAD_View_Column_ID(), null);
				builder.setColumnName(currentViewColumn.getColumnName());

				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}

	/**
	 * Convert field from request
	 * @param context
	 * @param request
	 * @return
	 */
	private Field.Builder getField(FieldRequest request) {
		Field.Builder builder = Field.newBuilder();
		//	For UUID
		if(request.getId() > 0) {
			builder = convertField(Env.getCtx(), request.getId());
		} else if(request.getColumnId() > 0) {
			builder = convertField(Env.getCtx(), MColumn.get(Env.getCtx(), request.getColumnId()));
		} else if(request.getElementId() > 0) {
			builder = convertField(Env.getCtx(), new M_Element(Env.getCtx(), request.getElementId(), null));
		} else if(!Util.isEmpty(request.getElementColumnName())) {
			M_Element element = new Query(Env.getCtx(), I_AD_Element.Table_Name, I_AD_Element.COLUMNNAME_ColumnName+ " = ?", null)
					.setParameters(request.getElementColumnName())
					.setOnlyActiveRecords(true)
					.first();
			builder = convertField(Env.getCtx(), element);
		} else if(!Util.isEmpty(request.getTableName()) 
				&& !Util.isEmpty(request.getColumnName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if(table != null) {
				MColumn column = table.getColumn(request.getColumnName());
				builder = convertField(Env.getCtx(), column);
			}
		}
		return builder;
	}
	
	/**
	 * Convert Field from UUID
	 * @param id
	 * @return
	 */
	private Field.Builder convertField(Properties context, int id) {
		MField field = new Query(context, I_AD_Field.Table_Name, I_AD_Field.COLUMNNAME_AD_Field_ID + " = ?", null)
				.setParameters(id)
				.setOnlyActiveRecords(true)
				.first();
		int fieldId = field.getAD_Field_ID();
		List<MField> customFields = ASPUtil.getInstance(context).getWindowFields(field.getAD_Tab_ID());
		if(customFields != null) {
			Optional<MField> maybeField = customFields.stream().filter(customField -> customField.getAD_Field_ID() == fieldId).findFirst();
			if(maybeField.isPresent()) {
				field = maybeField.get();

				// TODO: Remove conditional with fix the issue https://github.com/solop-develop/backend/issues/28
				String language = context.getProperty(Env.LANGUAGE);
				if(!Language.isBaseLanguage(language)) {
					//	Name
					String value = field.get_Translation(I_AD_Field.COLUMNNAME_Name, language);
					if (!Util.isEmpty(value, true)) {
						field.set_ValueOfColumn(I_AD_Field.COLUMNNAME_Name, value);
					}
					//	Description
					value = field.get_Translation(I_AD_Field.COLUMNNAME_Description, language);
					if (!Util.isEmpty(value, true)) {
						field.set_ValueOfColumn(I_AD_Field.COLUMNNAME_Description, value);
					}
					//	Help
					value = field.get_Translation(I_AD_Tab.COLUMNNAME_Help, language);
					if (!Util.isEmpty(value, true)) {
						field.set_ValueOfColumn(I_AD_Field.COLUMNNAME_Help, value);
					}
				}
			}
		}
		//	Convert
		return convertField(context, field, true);
	}
	
	/**
	 * Convert field to builder
	 * @param column
	 * @param language
	 * @return
	 */
	public static Field.Builder convertField(Properties context, MColumn column) {
		if (column == null) {
			return Field.newBuilder();
		}
		String defaultValue = column.getDefaultValue();
		if(Util.isEmpty(defaultValue)) {
			defaultValue = column.getDefaultValue();
		}
		//	Display Type
		int displayTypeId = column.getAD_Reference_ID();
		// element
		M_Element element = new M_Element(context, column.getAD_Element_ID(), null);
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(column.getAD_Column_ID())
			.setUuid(
				ValueManager.validateNull(column.getUUID())
			)
			.setName(
				ValueManager.validateNull(column.getName())
			)
			.setDescription(
				ValueManager.validateNull(column.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(column.getHelp())
			)
			.setCallout(
				ValueManager.validateNull(column.getCallout())
			)
			.setColumnId(column.getAD_Column_ID())
			.setColumnName(
				ValueManager.validateNull(column.getColumnName())
			)
			.setElementId(element.getAD_Element_ID())
			.setElementName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setColumnSql(
				ValueManager.validateNull(column.getColumnSQL())
			)
			.setDefaultValue(
				ValueManager.validateNull(defaultValue)
			)
			.setDisplayType(displayTypeId)
			.setFormatPattern(
				ValueManager.validateNull(column.getFormatPattern())
			)
			.setIdentifierSequence(column.getSeqNo())
			.setIsAllowCopy(column.isAllowCopy())
			.setIsAllowLogging(column.isAllowLogging())
			.setIsAlwaysUpdateable(column.isAlwaysUpdateable())
			.setIsEncrypted(column.isEncrypted())
			.setIsIdentifier(column.isIdentifier())
			.setIsKey(column.isKey())
			.setIsMandatory(column.isMandatory())
			.setIsParent(column.isParent())
			.setIsRange(column.isRange())
			.setIsSelectionColumn(column.isSelectionColumn())
			.setIsTranslated(column.isTranslated())
			.setIsUpdateable(column.isUpdateable())
			.setMandatoryLogic(
				ValueManager.validateNull(column.getMandatoryLogic())
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(column.getReadOnlyLogic())
			)
			.setSequence(column.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(column.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(column.getValueMin())
			)
			.setFieldLength(column.getFieldLength())
			.setIsActive(column.isActive())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(column.getDefaultValue()).orElse("")
				)
			)
		;
		//	Process
		if(column.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(context, column.getAD_Process_ID());
			Process.Builder processBuilder = convertProcess(context, process, false);
			builder.setProcess(processBuilder.build());
		}
		//	
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = column.getAD_Reference_Value_ID();

			//	Validation Code
			int validationRuleId = column.getAD_Val_Rule_ID();

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, column.getColumnName(), validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		}

		List<DependentField> depenentFieldsList = generateDependentColumns(column);
		builder.addAllDependentFields(depenentFieldsList);

		return builder;
	}

	public static List<DependentField> generateDependentColumns(MColumn column) {
		List<DependentField> depenentFieldsList = new ArrayList<>();
		if (column == null) {
			return depenentFieldsList;
		}

		String parentColumnName = column.getColumnName();

		MTable table = MTable.get(Env.getCtx(), column.getAD_Table_ID());
		List<MColumn> columnsList = table.getColumnsAsList(false);

		columnsList.stream()
			.filter(currentColumn -> {
				if(!currentColumn.isActive()) {
					return false;
				}
				// Default Value
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getDefaultValue())) {
					return true;
				}
				// ReadOnly Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getReadOnlyLogic())) {
					return true;
				}
				// Mandatory Logic
				if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getMandatoryLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentColumn.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(Env.getCtx(), currentColumn.getAD_Val_Rule_ID());
					if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentColumn -> {
				DependentField.Builder builder = DependentField.newBuilder();

				builder.setContainerId(table.getAD_Table_ID());
				builder.setContainerName(table.getTableName());
	
				builder.setId(currentColumn.getAD_Column_ID());
				builder.setColumnName(currentColumn.getColumnName());

				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}

	/**
	 * Convert field to builder
	 * @param element
	 * @return
	 */
	private Field.Builder convertField(Properties context, M_Element element) {
		if (element == null) {
			return Field.newBuilder();
		}
		//	Display Type
		int displayTypeId = element.getAD_Reference_ID();
		if(element.getAD_Reference_ID() > 0) {
			displayTypeId = element.getAD_Reference_ID();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(element.getAD_Element_ID())
			.setUuid(
				ValueManager.validateNull(element.getUUID())
			)
			.setName(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Help)))
			.setColumnName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setElementName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setDisplayType(displayTypeId)
			.setFieldLength(element.getFieldLength())
			.setIsActive(element.isActive())
		;
		//	
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = element.getAD_Reference_Value_ID();
			if(element.getAD_Reference_Value_ID() > 0) {
				referenceValueId = element.getAD_Reference_Value_ID();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, element.getColumnName(), 0
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		}
		return builder;
	}
	
	/**
	 * Convert field to builder
	 * @param field
	 * @param translate
	 * @return
	 */
	public static Field.Builder convertField(Properties context, MField field, boolean translate) {
		if (field == null) {
			return Field.newBuilder();
		}
		// Column reference
		MColumn column = MColumn.get(context, field.getAD_Column_ID());
		M_Element element = new M_Element(context, column.getAD_Element_ID(), null);
		String defaultValue = field.getDefaultValue();
		if(Util.isEmpty(defaultValue)) {
			defaultValue = column.getDefaultValue();
		}
		//	Display Type
		int displayTypeId = column.getAD_Reference_ID();
		if(field.getAD_Reference_ID() > 0) {
			displayTypeId = field.getAD_Reference_ID();
		}
		//	Mandatory Property
		boolean isMandatory = column.isMandatory();
		if(!Util.isEmpty(field.getIsMandatory())) {
			isMandatory = !Util.isEmpty(field.getIsMandatory()) && field.getIsMandatory().equals("Y");
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(field.getAD_Field_ID())
			.setUuid(
				ValueManager.validateNull(field.getUUID())
			)
			.setName(
				ValueManager.validateNull(field.getName())
			)
			.setDescription(
				ValueManager.validateNull(field.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(field.getHelp())
			)
			.setCallout(
				ValueManager.validateNull(column.getCallout())
			)
			.setColumnId(column.getAD_Column_ID())
			.setColumnName(
				ValueManager.validateNull(column.getColumnName())
			)
			.setElementId(element.getAD_Element_ID())
			.setElementName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setColumnSql(
				ValueManager.validateNull(column.getColumnSQL())
			)
			.setDefaultValue(
				ValueManager.validateNull(defaultValue)
			)
			.setDisplayLogic(
				ValueManager.validateNull(field.getDisplayLogic())
			)
			.setDisplayType(displayTypeId)
			.setFormatPattern(
				ValueManager.validateNull(column.getFormatPattern())
			)
			.setIdentifierSequence(column.getSeqNo())
			.setIsAllowCopy(field.isAllowCopy())
			.setIsAllowLogging(column.isAllowLogging())
			.setIsDisplayed(field.isDisplayed())
			.setIsAlwaysUpdateable(column.isAlwaysUpdateable())
			.setIsDisplayedGrid(field.isDisplayedGrid())
			.setIsEncrypted(field.isEncrypted() || column.isEncrypted())
			.setIsFieldOnly(field.isFieldOnly())
			.setIsHeading(field.isHeading())
			.setIsIdentifier(column.isIdentifier())
			.setIsKey(column.isKey())
			.setIsMandatory(isMandatory)
			.setIsParent(column.isParent())
			.setIsQuickEntry(field.isQuickEntry())
			.setIsRange(column.isRange())
			.setIsReadOnly(field.isReadOnly())
			.setIsSameLine(field.isSameLine())
			.setIsSelectionColumn(column.isSelectionColumn())
			.setIsTranslated(column.isTranslated())
			.setIsUpdateable(column.isUpdateable())
			.setMandatoryLogic(
				ValueManager.validateNull(column.getMandatoryLogic())
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(column.getReadOnlyLogic())
			)
			.setSequence(field.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(column.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(column.getValueMin())
			)
			.setFieldLength(column.getFieldLength())
			.setIsActive(field.isActive())
			.addAllContextColumnNames(
				DictionaryUtil.getContextColumnNames(
					Optional.ofNullable(field.getDefaultValue()).orElse(
						Optional.ofNullable(column.getDefaultValue()).orElse("")
					)
				)
			)
		;
		//	Context Info
		if(field.getAD_ContextInfo_ID() > 0) {
			ContextInfo.Builder contextInfoBuilder = convertContextInfo(context, field.getAD_ContextInfo_ID());
			builder.setContextInfo(contextInfoBuilder.build());
		}
		//	Process
		if(column.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(context, column.getAD_Process_ID());
			Process.Builder processBuilder = convertProcess(context, process, false);
			builder.setProcess(processBuilder.build());
		}
		//
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = column.getAD_Reference_Value_ID();
			if(field.getAD_Reference_Value_ID() > 0) {
				referenceValueId = field.getAD_Reference_Value_ID();
			}
			//	Validation Code
			int validationRuleId = column.getAD_Val_Rule_ID();
			if(field.getAD_Val_Rule_ID() > 0) {
				validationRuleId = field.getAD_Val_Rule_ID();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, column.getColumnName(), validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		} else if (DisplayType.Button == displayTypeId) {
			if (column.getColumnName().equals("Record_ID")) {
				builder.addContextColumnNames(I_AD_Table.COLUMNNAME_AD_Table_ID);
			}
		}

		//	Field Definition
		if(field.getAD_FieldDefinition_ID() > 0) {
			FieldDefinition.Builder fieldDefinitionBuilder = convertFieldDefinition(context, field.getAD_FieldDefinition_ID());
			builder.setFieldDefinition(fieldDefinitionBuilder);
		}
		//	Field Group
		if(field.getAD_FieldGroup_ID() > 0) {
			FieldGroup.Builder fieldGroup = convertFieldGroup(context, field.getAD_FieldGroup_ID());
			builder.setFieldGroup(fieldGroup.build());
		}

		MFieldCustom fieldCustom = FieldCustomUtil.getFieldCustom(field.getAD_Field_ID());
		if (fieldCustom != null && fieldCustom.isActive()) {
			// ASP default displayed field as panel
			if (fieldCustom.get_ColumnIndex(org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					ValueManager.validateNull(
						fieldCustom.get_ValueAsString(
							org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
			// ASP default displayed field as table
			if (fieldCustom.get_ColumnIndex(org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsTable(
					ValueManager.validateNull(
						fieldCustom.get_ValueAsString(
							org.spin.base.dictionary.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME
						)
					)
				);
			}
		}

		List<DependentField> depenentFieldsList = generateDependentFields(field);
		builder.addAllDependentFields(depenentFieldsList);

		return builder;
	}

	public static List<DependentField> generateDependentFields(MField field) {
		List<DependentField> depenentFieldsList = new ArrayList<>();
		if (field == null) {
			return depenentFieldsList;
		}

		int columnId = field.getAD_Column_ID();
		String parentColumnName = MColumn.getColumnName(Env.getCtx(), columnId);

		MTab parentTab = MTab.get(Env.getCtx(), field.getAD_Tab_ID());
		List<MTab> tabsList = ASPUtil.getInstance(Env.getCtx()).getWindowTabs(parentTab.getAD_Window_ID());
		if (tabsList == null) {
			return depenentFieldsList;
		}
		tabsList.stream()
			.filter(currentTab -> {
				// transaltion tab is not rendering on client
				return currentTab.isActive() && !currentTab.isTranslationTab();
			})
			.forEach(tab -> {
				List<MField> fieldsList = ASPUtil.getInstance().getWindowFields(tab.getAD_Tab_ID());
				if (fieldsList == null) {
					return;
				}

				fieldsList.stream()
					.filter(currentField -> {
						if (!currentField.isActive()) {
							return false;
						}
						// Display Logic
						if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentField.getDisplayLogic())) {
							return true;
						}
						// Default Value of Field
						if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentField.getDisplayLogic())) {
							return true;
						}
						// Dynamic Validation
						if (currentField.getAD_Val_Rule_ID() > 0) {
							MValRule validationRule = MValRule.get(Env.getCtx(), currentField.getAD_Val_Rule_ID());
							if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
								return true;
							}
						}

						MColumn currentColumn = MColumn.get(Env.getCtx(), currentField.getAD_Column_ID());
						// Default Value of Column
						if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getDefaultValue())) {
							return true;
						}
						// ReadOnly Logic
						if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getReadOnlyLogic())) {
							return true;
						}
						// Mandatory Logic
						if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, currentColumn.getMandatoryLogic())) {
							return true;
						}
						// Dynamic Validation
						if (currentColumn.getAD_Val_Rule_ID() > 0) {
							MValRule validationRule = MValRule.get(Env.getCtx(), currentColumn.getAD_Val_Rule_ID());
							if (DictionaryUtil.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
								return true;
							}
						}
						return false;
					})
					.forEach(currentField -> {
						DependentField.Builder builder = DependentField.newBuilder();
						builder.setContainerId(tab.getAD_Tab_ID());
						builder.setContainerName(tab.getName());

						builder.setId(currentField.getAD_Field_ID());
						
						String currentColumnName = MColumn.getColumnName(Env.getCtx(), currentField.getAD_Column_ID());
						builder.setColumnName(currentColumnName);

						depenentFieldsList.add(builder.build());
					});
			});

		return depenentFieldsList;
	}


	/**
	 * Convert Field Definition to builder
	 * @param fieldDefinitionId
	 * @return
	 */
	public static FieldDefinition.Builder convertFieldDefinition(Properties context, int fieldDefinitionId) {
		FieldDefinition.Builder builder = null;
		if(fieldDefinitionId > 0) {
			MADFieldDefinition fieldDefinition  = new MADFieldDefinition(context, fieldDefinitionId, null);
			//	Reference
			builder = FieldDefinition.newBuilder()
				.setId(fieldDefinition.getAD_FieldDefinition_ID())
				.setUuid(
					ValueManager.validateNull(fieldDefinition.getUUID())
				)
				.setValue(
					ValueManager.validateNull(fieldDefinition.getValue())
				)
				.setName(
					ValueManager.validateNull(fieldDefinition.getName())
				)
			;
			//	Get conditions
			for(MADFieldCondition condition : fieldDefinition.getConditions()) {
				if(!condition.isActive()) {
					continue;
				}
				FieldCondition.Builder fieldConditionBuilder = FieldCondition.newBuilder()
					.setId(condition.getAD_FieldCondition_ID())
					.setUuid(
						ValueManager.validateNull(condition.getUUID())
					)
					.setCondition(
						ValueManager.validateNull(condition.getCondition())
					)
					.setStylesheet(
						ValueManager.validateNull(condition.getStylesheet())
					)
					.setIsActive(fieldDefinition.isActive())
				;
				//	Add to parent
				builder.addConditions(fieldConditionBuilder);
			}
		}
		return builder;
	}
	
	/**
	 * Convert Field Group to builder
	 * @param fieldGroupId
	 * @return
	 */
	public static FieldGroup.Builder convertFieldGroup(Properties context, int fieldGroupId) {
		FieldGroup.Builder builder = FieldGroup.newBuilder();
		if(fieldGroupId > 0) {
			X_AD_FieldGroup fieldGroup  = new X_AD_FieldGroup(context, fieldGroupId, null);
			//	Get translation
			String name = null;
			String language = Env.getAD_Language(context);
			if(!Util.isEmpty(language)) {
				name = fieldGroup.get_Translation(I_AD_FieldGroup.COLUMNNAME_Name, language);
			}
			//	Validate for default
			if(Util.isEmpty(name)) {
				name = fieldGroup.getName();
			}
			//	Field Group
			builder = FieldGroup.newBuilder()
				.setId(fieldGroup.getAD_FieldGroup_ID())
				.setUuid(
					ValueManager.validateNull(fieldGroup.getUUID())
				)
				.setName(
					ValueManager.validateNull(name))
				.setFieldGroupType(
					ValueManager.validateNull(fieldGroup.getFieldGroupType())
				)
				.setIsActive(fieldGroup.isActive())
			;
		}
		return builder;
	}
	
	/**
	 * Convert reference from a request
	 * @param context
	 * @param request
	 * @return
	 */
	private Reference.Builder convertReference(Properties context, ReferenceRequest request) {
		Reference.Builder builder = Reference.newBuilder();
		MLookupInfo info = null;
		if(request.getId() > 0) {
			X_AD_Reference reference = new X_AD_Reference(context, request.getId(), null);
			if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_TableValidation)) {
				info = MLookupFactory.getLookupInfo(context, 0, 0, DisplayType.Search, Language.getLanguage(Env.getAD_Language(context)), null, reference.getAD_Reference_ID(), false, null, false);
			} else if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_ListValidation)) {
				info = MLookupFactory.getLookup_List(Language.getLanguage(Env.getAD_Language(context)), reference.getAD_Reference_ID());
			}
		} else if(!Util.isEmpty(request.getColumnName())) {
			info = MLookupFactory.getLookupInfo(context, 0, 0, DisplayType.TableDir, Language.getLanguage(Env.getAD_Language(context)), request.getColumnName(), 0, false, null, false);
		}

		if (info != null) {
			builder = DictionaryConvertUtil.convertReference(context, info);
		}

		return builder;
	}



	@Override
	public void listIdentifiersFields(ListFieldsRequest request, StreamObserver<ListFieldsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListFieldsResponse.Builder fielsListBuilder = getIdentifierFields(request);
			responseObserver.onNext(fielsListBuilder.build());
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

	private ListFieldsResponse.Builder getIdentifierFields(ListFieldsRequest request) {
		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		ListFieldsResponse.Builder fieldsListBuilder = ListFieldsResponse.newBuilder();

		final String sql = "SELECT c.AD_Column_ID"
			// + ", c.ColumnName, t.AD_Table_ID, t.TableName, c.ColumnSql "
			+ " FROM AD_Table AS t "
			+ "	INNER JOIN AD_Column c ON (t.AD_Table_ID=c.AD_Table_ID) "
			+ "	WHERE c.AD_Reference_ID = 10 "
			+ " AND t.AD_Table_ID = ? "
			//	Displayed in Window
			+ "	AND EXISTS (SELECT * FROM AD_Field AS f "
			+ "	WHERE f.AD_Column_ID=c.AD_Column_ID "
			+ " AND f.IsDisplayed='Y' AND f.IsEncrypted='N' AND f.ObscureType IS NULL) "
			+ "	ORDER BY c.IsIdentifier DESC, c.SeqNo "
		;

		DB.runResultSet(null, sql, List.of(table.getAD_Table_ID()), resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				MColumn column = MColumn.get(context, resultSet.getInt(MColumn.COLUMNNAME_AD_Column_ID));
				if (column != null) {
					Field.Builder fieldBuilder = convertField(context, column);
					fieldsListBuilder.addFields(fieldBuilder.build());
				}
				recordCount++;
			}
			fieldsListBuilder.setRecordCount(recordCount);
		}).onFailure(throwable -> {
			log.log(Level.SEVERE, sql, throwable);
		});

		//	empty general info
		// if (fieldsListBuilder.getFieldsList().size() == 0) {
		// }

		return fieldsListBuilder;
	}



	@Override
	public void listTableSearchFields(ListFieldsRequest request, StreamObserver<ListFieldsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListFieldsResponse.Builder fielsListBuilder = getTableSearchFields(request);
			responseObserver.onNext(fielsListBuilder.build());
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

	private ListFieldsResponse.Builder getTableSearchFields(ListFieldsRequest request) {
		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		ListFieldsResponse.Builder fieldsListBuilder = ListFieldsResponse.newBuilder();

		final String sql = "SELECT f.AD_Field_ID "
			// + ", c.ColumnName, c.AD_Reference_ID, c.IsKey, f.IsDisplayed, c.AD_Reference_Value_ID, c.ColumnSql "
			+ " FROM AD_Column c "
			+ " INNER JOIN AD_Table t ON (c.AD_Table_ID=t.AD_Table_ID)"
			+ " INNER JOIN AD_Tab tab ON (t.AD_Window_ID=tab.AD_Window_ID)"
			+ " INNER JOIN AD_Field f ON (tab.AD_Tab_ID=f.AD_Tab_ID AND f.AD_Column_ID=c.AD_Column_ID) "
			+ " WHERE t.AD_Table_ID=? "
			+ " AND (c.IsKey='Y' OR "
				// + " (f.IsDisplayed='Y' AND f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
				+ " (f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
			+ "ORDER BY c.IsKey DESC, f.SeqNo"
		;

		DB.runResultSet(null, sql, List.of(table.getAD_Table_ID()), resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				MField field = new MField(context, resultSet.getInt(MField.COLUMNNAME_AD_Field_ID), null);
				if (field != null) {
					Field.Builder fieldBuilder = convertField(context, field, true);
					fieldsListBuilder.addFields(fieldBuilder.build());
				}
				recordCount++;
			}
			fieldsListBuilder.setRecordCount(recordCount);
		}).onFailure(throwable -> {
			log.log(Level.SEVERE, sql, throwable);
		});

		return fieldsListBuilder;
	}



	@Override
	public void listSearchInfoFields(ListFieldsRequest request, StreamObserver<ListFieldsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListFieldsResponse.Builder fielsListBuilder = DictionaryServiceLogic.listSearchInfoFields(request);
			responseObserver.onNext(fielsListBuilder.build());
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

}
