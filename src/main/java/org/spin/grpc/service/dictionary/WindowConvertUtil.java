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
package org.spin.grpc.service.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_FieldGroup;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_FieldGroup;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MFieldCustom;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
// import org.compiere.model.M_Element;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
// import org.spin.backend.grpc.dictionary.ContextInfo;
import org.spin.backend.grpc.dictionary.DependentField;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.FieldCondition;
import org.spin.backend.grpc.dictionary.FieldDefinition;
import org.spin.backend.grpc.dictionary.FieldGroup;
import org.spin.backend.grpc.dictionary.Process;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.Tab;
import org.spin.backend.grpc.dictionary.Table;
import org.spin.backend.grpc.dictionary.Window;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.AccessUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceUtil;
import org.spin.dictionary.custom.FieldCustomUtil;
import org.spin.dictionary.util.WindowUtil;
import org.spin.model.MADFieldCondition;
import org.spin.model.MADFieldDefinition;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

public class WindowConvertUtil {

	/**
	 * Convert Window from Window Model
	 * @param window
	 * @param withTabs
	 * @return
	 */
	public static Window.Builder convertWindow(Properties context, MWindow window, boolean withTabs) {
		if (window == null) {
			return Window.newBuilder();
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(window);

		//	
		Window.Builder builder = Window.newBuilder()
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
		;
		//	With Tabs
		if(withTabs) {
			boolean isShowAcct = MRole.getDefault(context, false).isShowAcct();
			// List<Tab.Builder> tabListForGroup = new ArrayList<>();
			List<MTab> tabs = Arrays.asList(
				window.getTabs(false, null)
			);
			if (tabs != null) {
				for(MTab tab : tabs) {
					if(tab == null || !tab.isActive()) {
						continue;
					}
					// role without permission to accounting
					if (tab.isInfoTab() && !isShowAcct) {
						continue;
					}
					Tab.Builder tabBuilder = WindowConvertUtil.convertTab(
						context,
						tab,
						tabs,
						withTabs
					);
					builder.addTabs(tabBuilder.build());
					//	Get field group
					// int [] fieldGroupIdArray = getFieldGroupIdsFromTab(tab.getAD_Tab_ID());
					// if(fieldGroupIdArray != null) {
					// 	for(int fieldGroupId : fieldGroupIdArray) {
					// 		Tab.Builder tabFieldGroup = convertTab(context, tab, false);
					// 		FieldGroup.Builder fieldGroup = convertFieldGroup(context, fieldGroupId);
					// 		tabFieldGroup.setFieldGroup(fieldGroup);
					// 		tabFieldGroup.setName(fieldGroup.getName());
					// 		tabFieldGroup.setDescription("");
					// 		tabFieldGroup.setUuid(tabFieldGroup.getUuid() + "---");
					// 		//	Add to list
					// 		tabListForGroup.add(tabFieldGroup);
					// 	}
					// }
				}
				//	Add Field Group Tabs
				// for(Tab.Builder tabFieldGroup : tabListForGroup) {
				// 	builder.addTabs(tabFieldGroup.build());
				// }
			}
		}
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Window,
			window.getAD_Window_ID()
		);
		//	return
		return builder;
	}


	public static Table.Builder convertTable(MTable table) {
		Table.Builder builder = Table.newBuilder();
		if (table == null || table.getAD_Table_ID() <= 0) {
			return builder;
		}
		List<String> selectionColums = table.getColumnsAsList(true).stream()
			.filter(column -> {
				return column.isSelectionColumn();
			})
			.map(column -> {
				return column.getColumnName();
			})
			.collect(Collectors.toList())
		;
		List<String> identifierColumns = table.getColumnsAsList(false).stream()
			.filter(column -> {
				return column.isIdentifier();
			})
			.sorted(Comparator.comparing(MColumn::getSeqNo))
			.map(column -> {
				return column.getColumnName();
			})
			.collect(Collectors.toList())
		;
		builder.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
			.setAccessLevel(
				NumberManager.getIntFromString(
					table.getAccessLevel()
				)
			)
			.addAllKeyColumns(
				Arrays.asList(
					table.getKeyColumns()
				)
			)
			.setIsView(
				table.isView()
			)
			.setIsDocument(
				table.isDocument()
			)
			.setIsDeleteable(
				table.isDeleteable()
			)
			.setIsChangeLog(
				table.isChangeLog()
			)
			.addAllIdentifierColumns(identifierColumns)
			.addAllSelectionColumns(selectionColums)
		;

		return builder;
	}

	/**
	 * Convert Model tab to builder tab
	 * @param tab
	 * @return
	 */
	public static Tab.Builder convertTab(Properties context, MTab tab, List<MTab> tabs, boolean withFields) {
		if (tab == null) {
			return Tab.newBuilder();
		}

		int tabId = tab.getAD_Tab_ID();
		int parentTabId = 0;
		// root tab has no parent
		if (tab.getTabLevel() > 0) {
			parentTabId = WindowUtil.getDirectParentTabId(tab.getAD_Window_ID(), tabId);
		}

		//	Get table attributes
		MTable table = MTable.get(context, tab.getAD_Table_ID());
		boolean isReadOnly = tab.isReadOnly() || table.isView();

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
			.setIsInsertRecord(
				!isReadOnly && tab.isInsertRecord()
			)
			.setCommitWarning(
				ValueManager.validateNull(
					tab.getCommitWarning()
				)
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
			.setTable(
				convertTable(table)
			)
			.setSequence(tab.getSeqNo())
			.setDisplayLogic(
				ValueManager.validateNull(tab.getDisplayLogic())
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(tab.getReadOnlyLogic())
			)
			.setIsAdvancedTab(tab.isAdvancedTab())
			.setIsHasTree(tab.isHasTree())
			.setIsInfoTab(tab.isInfoTab())
			.setIsReadOnly(isReadOnly)
			.setIsSingleRow(tab.isSingleRow())
			.setIsSortTab(tab.isSortTab())
			.setIsTranslationTab(tab.isTranslationTab())
			.setTabLevel(tab.getTabLevel())
			.setParentTabId(parentTabId)
			.setWindowId(
				tab.getAD_Window_ID()
			)
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					Optional.ofNullable(whereClause).orElse("")
					+ Optional.ofNullable(tab.getOrderByClause()).orElse("")
				)
			)
		;

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

			//	Parent Column from parent tab
			MTab originTab = new Query(
				tab.getCtx(),
				I_AD_Tab.Table_Name,
				"AD_Window_ID = ? AND AD_Table_ID = ? AND IsSortTab = ?",
				null
			)
				.setParameters(tab.getAD_Window_ID(), table.getAD_Table_ID(), false)
				.first()
			;
			if (originTab != null && originTab.getAD_Tab_ID() > 0) {
				// is same table and columns
				List<MColumn> columnsList = table.getColumnsAsList();
				MColumn parentColumn = columnsList.parallelStream()
					.filter(column -> {
						return column.isParent();
					})
					.findFirst()
					.orElse(null)
				;
				if (parentColumn != null && parentColumn.getAD_Column_ID() > 0) {
					// filter_column_name
					builder.setFilterColumnName(
						ValueManager.validateNull(
							parentColumn.getColumnName()
						)
					);
				}
			}
		}

		//	Process
		if (tab.getAD_Process_ID() > 0) {
			// Record/Role access
			boolean isWithAccess = AccessUtil.isProcessAccess(MRole.getDefault(), tab.getAD_Process_ID());
			if (isWithAccess) {
				Process.Builder processAssociated = ProcessConvertUtil.convertProcess(
					context,
					tab.getAD_Process_ID(),
					false
				);
				builder.setProcess(processAssociated);
			}
		}

		List<MProcess> processList = WindowUtil.getProcessActionFromTab(context, tab);
		if(processList != null && processList.size() > 0) {
			for(MProcess process : processList) {
				// get process associated without parameters
				Process.Builder processBuilder = ProcessConvertUtil.convertProcess(
					context,
					process,
					false
				);
				builder.addProcesses(processBuilder.build());
			}
		}

		//	Fields
		if(withFields) {
			List<MField> fieldsList = Arrays.asList(
				tab.getFields(false, null)
			);
			for(MField field : fieldsList) {
				if (field == null) {
					continue;
				}
				Field.Builder fieldBuilder = WindowConvertUtil.convertField(
					context,
					field,
					false
				);
				builder.addFields(fieldBuilder.build());
			}
		}
		//	
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
		// M_Element element = new M_Element(context, column.getAD_Element_ID(), null);
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
			.setColumnName(
				ValueManager.validateNull(column.getColumnName())
			)
			// .setElementName(
			// 	ValueManager.validateNull(element.getColumnName())
			// )
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
			.setSeqNoGrid(field.getSeqNoGrid())
			.setValueMax(
				ValueManager.validateNull(column.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(column.getValueMin())
			)
			.setFieldLength(column.getFieldLength())
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					Optional.ofNullable(field.getDefaultValue()).orElse(
						Optional.ofNullable(column.getDefaultValue()).orElse("")
					)
				)
			)
		;

		//	Context Info
		// if(field.getAD_ContextInfo_ID() > 0) {
		// 	ContextInfo.Builder contextInfoBuilder = DictionaryConvertUtil.convertContextInfo(
		// 		context,
		// 		field.getAD_ContextInfo_ID()
		// 	);
		// 	builder.setContextInfo(contextInfoBuilder.build());
		// }
		//	Process
		if(column.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(context, column.getAD_Process_ID());
			Process.Builder processBuilder = ProcessConvertUtil.convertProcess(
				context,
				process,
				false
			);
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
			if (column.getColumnName().equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
				// To load default value
				builder.addContextColumnNames(I_AD_Table.COLUMNNAME_AD_Table_ID);
			}
		}

		// //	Field Definition
		// if(field.getAD_FieldDefinition_ID() > 0) {
		// 	FieldDefinition.Builder fieldDefinitionBuilder = WindowConvertUtil.convertFieldDefinition(
		// 		context,
		// 		field.getAD_FieldDefinition_ID()
		// 	);
		// 	builder.setFieldDefinition(fieldDefinitionBuilder);
		// }
		//	Field Group
		if(field.getAD_FieldGroup_ID() > 0) {
			FieldGroup.Builder fieldGroup = WindowConvertUtil.convertFieldGroup(
				context,
				field.getAD_FieldGroup_ID()
			);
			builder.setFieldGroup(fieldGroup.build());
		}

		MFieldCustom fieldCustom = FieldCustomUtil.getFieldCustom(field.getAD_Field_ID());
		if (fieldCustom != null && fieldCustom.isActive()) {
			// ASP default displayed field as panel
			if (fieldCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					ValueManager.validateNull(
						fieldCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
			// ASP default displayed field as table
			if (fieldCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsTable(
					ValueManager.validateNull(
						fieldCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME
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
		final String parentColumnName = MColumn.getColumnName(field.getCtx(), columnId);

		MTab parentTab = MTab.get(field.getCtx(), field.getAD_Tab_ID());
		MWindow window = MWindow.get(field.getCtx(), parentTab.getAD_Window_ID());
		List<MTab> tabsList = Arrays.asList(
			window.getTabs(false, null)
		);
		if (tabsList == null || tabsList.isEmpty()) {
			return depenentFieldsList;
		}
		tabsList.stream()
			.filter(currentTab -> {
				// transaltion tab is not rendering on client
				return currentTab.isActive() && !currentTab.isTranslationTab() && !currentTab.isSortTab();
			})
			.forEach(tab -> {
				List<MField> fieldsList = Arrays.asList(
					tab.getFields(false, null)
				);
				if (fieldsList == null || fieldsList.isEmpty()) {
					return;
				}

				fieldsList.stream()
					.filter(currentField -> {
						if (currentField == null || !currentField.isActive()) {
							return false;
						}
						// Display Logic
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentField.getDisplayLogic())) {
							return true;
						}
						// Default Value of Field
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentField.getDefaultValue())) {
							return true;
						}
						// Dynamic Validation
						if (currentField.getAD_Val_Rule_ID() > 0) {
							MValRule validationRule = MValRule.get(
								currentField.getCtx(),
								currentField.getAD_Val_Rule_ID()
							);
							if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
								return true;
							}
						}

						MColumn currentColumn = MColumn.get(currentField.getCtx(), currentField.getAD_Column_ID());
						// Default Value of Column
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getDefaultValue())) {
							return true;
						}
						// ReadOnly Logic
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getReadOnlyLogic())) {
							return true;
						}
						// Mandatory Logic
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getMandatoryLogic())) {
							return true;
						}
						// Dynamic Validation
						if (currentColumn.getAD_Val_Rule_ID() > 0) {
							MValRule validationRule = MValRule.get(
								currentField.getCtx(),
								currentColumn.getAD_Val_Rule_ID()
							);
							if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
								return true;
							}
						}
						return false;
					})
					.forEach(currentField -> {
						final String currentColumnName = MColumn.getColumnName(
							currentField.getCtx(),
							currentField.getAD_Column_ID()
						);
						DependentField.Builder builder = DependentField.newBuilder()
							.setId(
								currentField.getAD_Field_ID()
							)
							.setUuid(
								ValueManager.validateNull(
									currentField.getUUID()
								)
							)
							.setColumnName(
								ValueManager.validateNull(
									currentColumnName
								)
							)
							.setParentId(
								tab.getAD_Tab_ID()
							)
							.setParentUuid(
								ValueManager.validateNull(
									tab.getUUID()
								)
							)
							.setParentName(
								ValueManager.validateNull(
									tab.getName()
								)
							)
						;
						depenentFieldsList.add(builder.build());
					});
			});

		return depenentFieldsList;
	}


	/**
	 * Convert Field Group to builder
	 * @param fieldGroupId
	 * @return
	 */
	public static FieldGroup.Builder convertFieldGroup(Properties context, int fieldGroupId) {
		FieldGroup.Builder builder = FieldGroup.newBuilder();
		if(fieldGroupId <= 0) {
			return builder;
		}
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
		;
		return builder;
	}


	/**
	 * Convert Field Definition to builder
	 * @param fieldDefinitionId
	 * @return
	 */
	public static FieldDefinition.Builder convertFieldDefinition(Properties context, int fieldDefinitionId) {
		FieldDefinition.Builder builder = null;
		if(fieldDefinitionId <= 0) {
			return builder;
		}
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
			;
			//	Add to parent
			builder.addConditions(fieldConditionBuilder);
		}
		return builder;
	}

}
