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
import org.spin.service.grpc.util.value.StringManager;

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
		DictionaryConvertUtil.translateEntity(context, window);

		//	
		Window.Builder builder = Window.newBuilder()
			.setId(
				StringManager.getValidString(
					window.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					window.getUUID()
				)
			)
			.setInternalId(
				window.getAD_Window_ID()
			)
			.setName(window.getName())
			.setDescription(
				StringManager.getValidString(
					window.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					window.getHelp()
				)
			)
			.setWindowType(
				StringManager.getValidString(
					window.getWindowType()
				)
			)
			.setIsSalesTransaction(window.isSOTrx())
			.setIsActive(
				window.isActive()
			)
			.setIsBetaFunctionality(
				window.isBetaFunctionality()
			)
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
		builder.setId(
				StringManager.getValidString(
					table.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					table.getUUID()
				)
			)
			.setInternalId(
				table.getAD_Table_ID()
			)
			.setTableName(
				StringManager.getValidString(
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

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, tab);

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
			.setId(
				StringManager.getValidString(
					tab.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					tab.getUUID()
				)
			)
			.setInternalId(
				tab.getAD_Tab_ID()
			)
			.setName(
				StringManager.getValidString(
					tab.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					tab.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					tab.getHelp()
				)
			)
			.setIsInsertRecord(
				!isReadOnly && tab.isInsertRecord()
			)
			.setCommitWarning(
				StringManager.getValidString(
					tab.getCommitWarning()
				)
			)
			.setTableName(
				StringManager.getValidString(
					table.getTableName()
				)
			)
			.setTable(
				convertTable(table)
			)
			.setSequence(tab.getSeqNo())
			.setDisplayLogic(
				StringManager.getValidString(
					tab.getDisplayLogic()
				)
			)
			.setReadOnlyLogic(
				StringManager.getValidString(
					tab.getReadOnlyLogic()
				)
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
						StringManager.getValidString(
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
				MProcess process = MProcess.get(context, tab.getAD_Process_ID());

				Process.Builder processAssociated = ProcessConvertUtil.convertProcess(
					context,
					process,
					false
				);
				builder.setProcessId(
						process.getAD_Process_ID()
					)
					.setProcessUuid(
						StringManager.getValidString(
							process.getUUID()
						)
					)
					.setProcess(processAssociated)
				;
			}
		}

		List<MProcess> processList = WindowUtil.getProcessActionFromTab(context, tab);
		if(processList != null && processList.size() > 0) {
			processList = processList.stream()
				.sorted(Comparator.comparing(MProcess::getName))
				.collect(Collectors.toList())
			;
			for(MProcess process : processList) {
				// get process associated without parameters
				Process.Builder processBuilder = ProcessConvertUtil.convertProcess(
					context,
					process,
					false
				);
				builder.addProcesses(processBuilder.build());
				builder.addProcessesUuid(
					StringManager.getValidString(
						process.getUUID()
					)
				);
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

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, field);

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
			.setId(
				StringManager.getValidString(
					field.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					field.getUUID()
				)
			)
			.setInternalId(
				field.getAD_Field_ID()
			)
			.setName(
				StringManager.getValidString(
					field.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					field.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					field.getHelp()
				)
			)
			.setCallout(
				StringManager.getValidString(
					column.getCallout()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					column.getColumnName()
				)
			)
			// .setElementName(
			// 	StringManager.getValidString(
			// 		element.getColumnName()
			// 	)
			// )
			.setColumnSql(
				StringManager.getValidString(
					column.getColumnSQL()
				)
			)
			.setDefaultValue(
				StringManager.getValidString(defaultValue)
			)
			.setDisplayLogic(
				StringManager.getValidString(
					field.getDisplayLogic()
				)
			)
			.setDisplayType(displayTypeId)
			.setFormatPattern(
				StringManager.getValidString(
					column.getFormatPattern()
				)
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
				StringManager.getValidString(
					column.getMandatoryLogic()
				)
			)
			.setReadOnlyLogic(
				StringManager.getValidString(
					column.getReadOnlyLogic()
				)
			)
			.setSequence(field.getSeqNo())
			.setSeqNoGrid(field.getSeqNoGrid())
			.setValueMax(
				StringManager.getValidString(
					column.getValueMax()
				)
			)
			.setValueMin(
				StringManager.getValidString(
					column.getValueMin()
				)
			)
			.setFieldLength(
				column.getFieldLength()
			)
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

		//	Reference Value
		int referenceValueId = field.getAD_Reference_Value_ID();
		if(referenceValueId <= 0) {
			referenceValueId = column.getAD_Reference_Value_ID();
		}

		// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
		displayTypeId = ReferenceUtil.overwriteDisplayType(
			displayTypeId,
			referenceValueId
		);
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Validation Code
			int validationRuleId = field.getAD_Val_Rule_ID();
			if(validationRuleId <= 0) {
				validationRuleId = column.getAD_Val_Rule_ID();
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
					StringManager.getValidString(
						fieldCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
			// ASP default displayed field as table
			if (fieldCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsTable(
					StringManager.getValidString(
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
						final String defaultValue = currentField.getDefaultValue();
						if (ContextManager.isUseParentColumnOnContext(parentColumnName, defaultValue)) {
							return true;
						}
						// Dynamic Validation
						final int dynamicValidationId = currentField.getAD_Val_Rule_ID();
						if (dynamicValidationId > 0) {
							MValRule validationRule = MValRule.get(
								currentField.getCtx(),
								dynamicValidationId
							);
							if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
								return true;
							}
						}

						MColumn currentColumn = MColumn.get(currentField.getCtx(), currentField.getAD_Column_ID());
						// Default Value of Column
						if (Util.isEmpty(defaultValue, true) && ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getDefaultValue())) {
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
						if (dynamicValidationId <= 0 && currentColumn.getAD_Val_Rule_ID() > 0) {
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
								StringManager.getValidString(
									currentField.getUUID()
								)
							)
							.setUuid(
								StringManager.getValidString(
									currentField.getUUID()
								)
							)
							.setInternalId(
								currentField.getAD_Field_ID()
							)
							.setColumnName(
								StringManager.getValidString(
									currentColumnName
								)
							)
							.setParentId(
								tab.getAD_Tab_ID()
							)
							.setParentUuid(
								StringManager.getValidString(
									tab.getUUID()
								)
							)
							.setParentName(
								StringManager.getValidString(
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
				StringManager.getValidString(
					fieldGroup.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(name))
			.setFieldGroupType(
				StringManager.getValidString(
					fieldGroup.getFieldGroupType()
				)
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
				StringManager.getValidString(
					fieldDefinition.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					fieldDefinition.getValue()
				)
			)
			.setName(
				StringManager.getValidString(
					fieldDefinition.getName()
				)
			)
		;
		//	Get conditions
		for(MADFieldCondition condition : fieldDefinition.getConditions()) {
			if(!condition.isActive()) {
				continue;
			}
			FieldCondition.Builder fieldConditionBuilder = FieldCondition.newBuilder()
				.setId(
					condition.getAD_FieldCondition_ID()
				)
				.setUuid(
					StringManager.getValidString(
						condition.getUUID()
					)
				)
				.setCondition(
					StringManager.getValidString(
						condition.getCondition()
					)
				)
				.setStylesheet(
					StringManager.getValidString(
						condition.getStylesheet()
					)
				)
			;
			//	Add to parent
			builder.addConditions(fieldConditionBuilder);
		}
		return builder;
	}

}
