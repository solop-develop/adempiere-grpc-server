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

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.MBrowseFieldCustom;
import org.compiere.model.MColumn;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.MWindow;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Browser;
import org.spin.backend.grpc.dictionary.DependentField;
import org.spin.backend.grpc.dictionary.DictionaryEntity;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.Table;
import org.spin.base.db.QueryUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceUtil;
import org.spin.dictionary.custom.BrowseFieldCustomUtil;
import org.spin.service.grpc.util.db.OrderByUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

public class BrowseConverUtil {

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
	 * Convert process to builder
	 * @param browser
	 * @param withFields
	 * @return
	 */
	public static Browser.Builder convertBrowser(Properties context, MBrowse browser, boolean withFields) {
		if (browser == null) {
			return Browser.newBuilder();
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, browser);

		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String orderByClause = OrderByUtil.getBrowseOrderBy(browser);
		Browser.Builder builder = Browser.newBuilder()
			.setId(
				StringManager.getValidString(
					browser.getUUID()
				))
			.setUuid(
				StringManager.getValidString(
					browser.getUUID()
				)
			)
			.setInternalId(
				browser.getAD_Browse_ID()
			)
			.setCode(
				StringManager.getValidString(
					browser.getValue()
				)
			)
			.setName(browser.getName())
			.setDescription(
				StringManager.getValidString(
					browser.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					browser.getHelp()
				)
			)
			.setIsActive(
				browser.isActive()
			)
			.setIsBetaFunctionality(
				browser.isBetaFunctionality()
			)
			.setAccessLevel(Integer.parseInt(browser.getAccessLevel()))
			.setIsCollapsibleByDefault(browser.isCollapsibleByDefault())
			.setIsDeleteable(browser.isDeleteable())
			.setIsExecutedQueryByDefault(browser.isExecutedQueryByDefault())
			.setIsSelectedByDefault(browser.isSelectedByDefault())
			.setIsShowTotal(browser.isShowTotal())
			.setIsUpdateable(browser.isUpdateable())
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
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
				StringManager.getValidString(
					viewColumn.getColumnName()
				)
			);
		}
		// set table name
		if (browser.getAD_Table_ID() > 0) {
			MTable table = MTable.get(browser.getCtx(), browser.getAD_Table_ID());
			builder.setTableName(
					StringManager.getValidString(
						table.getTableName()
					)
				)
				.setTable(
					convertTable(
						table
					)
				)
			;
		}

		//	Window Reference
		if(browser.getAD_Window_ID() > 0) {
			MWindow window = MWindow.get(
				context,
				browser.getAD_Window_ID()
			);
			DictionaryEntity.Builder windowBuilder = DictionaryConvertUtil.getDictionaryEntity(
				window
			);
			builder.setWindow(windowBuilder.build());
		}

		//	Process Reference
		if(browser.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(
				context,
				browser.getAD_Process_ID()
			);
			DictionaryEntity.Builder processBuilder = DictionaryConvertUtil.getDictionaryEntity(
				process
			);
			builder.setProcessId(
					process.getAD_Process_ID()
				)
				.setProcessUuid(
					StringManager.getValidString(
						process.getUUID()
					)
				)
				.setProcess(
					processBuilder.build()
				)
			;
		}

		//	Search process
		boolean isSearchProcess = browser.get_ColumnIndex("SearchProcess_ID") >= 0 && browser.get_ValueAsBoolean("SearchProcess_ID");
		builder.setIsSearchProcess(isSearchProcess);

		//	For parameters
		if(withFields) {
			List<MBrowseField> browseFields = browser.getFields();
			for(MBrowseField field : browseFields) {
				if (field == null) {
					continue;
				}
				Field.Builder fieldBuilder = BrowseConverUtil.convertBrowseField(
					context,
					field
				);
				builder.addFields(fieldBuilder.build());
			}
		}
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_SmartBrowse,
			browser.getAD_Browse_ID()
		);
		return builder;
	}


	/**
	 * Convert Browse Field
	 * @param browseField
	 * @return
	 */
	public static Field.Builder convertBrowseField(Properties context, MBrowseField browseField) {
		if (browseField == null) {
			return Field.newBuilder();
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, browseField);

		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(
				StringManager.getValidString(
					browseField.getUUID()
				))
			.setUuid(
				StringManager.getValidString(
					browseField.getUUID()
				)
			)
			.setInternalId(
				browseField.getAD_Browse_Field_ID()
			)
			.setName(
				StringManager.getValidString(
					browseField.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					browseField.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					browseField.getHelp()
				)
			)
			.setDefaultValue(
				StringManager.getValidString(
					browseField.getDefaultValue()
				)
			)
			.setDefaultValueTo(
				StringManager.getValidString(
					browseField.getDefaultValue2()
				)
			)
			.setDisplayLogic(
				StringManager.getValidString(
					browseField.getDisplayLogic()
				)
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
				StringManager.getValidString(
					browseField.getReadOnlyLogic()
				)
			)
			.setIsKey(browseField.isKey())
			.setIsIdentifier(browseField.isIdentifier())
			.setSeqNoGrid(browseField.getSeqNoGrid())
			.setSequence(browseField.getSeqNo())
			.setValueMax(
				StringManager.getValidString(
					browseField.getValueMax()
				)
			)
			.setValueMin(
				StringManager.getValidString(
					browseField.getValueMin()
				)
			)
			.setVFormat(
				StringManager.getValidString(
					browseField.getVFormat()
				)
			)
			.setCallout(
				StringManager.getValidString(
					browseField.getCallout()
				)
			)
			.setFieldLength(browseField.getFieldLength())
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					Optional.ofNullable(browseField.getDefaultValue()).orElse("")
					+ Optional.ofNullable(browseField.getDefaultValue2()).orElse("")
				)
			)
		;
		
		MViewColumn viewColumn = MViewColumn.getById(context, browseField.getAD_View_Column_ID(), null);
		builder.setColumnName(
			StringManager.getValidString(
				viewColumn.getColumnName()
			)
		);
		String elementName = null;
		if(viewColumn.getAD_Column_ID() != 0) {
			MColumn column = MColumn.get(context, viewColumn.getAD_Column_ID());
			elementName = column.getColumnName();
		}

		//	Default element
		if(Util.isEmpty(elementName)) {
			elementName = browseField.getAD_Element().getColumnName();
		}
		builder.setElementName(
			StringManager.getValidString(elementName))
		;

		// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
		int displayTypeId = ReferenceUtil.overwriteDisplayType(
			browseField.getAD_Reference_ID(),
			browseField.getAD_Reference_Value_ID()
		);
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
			if (browseFieldCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					StringManager.getValidString(
						browseFieldCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
			// ASP default displayed field as table
			if (browseFieldCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsTable(
					StringManager.getValidString(
						browseFieldCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_TABLE_COLUMN_NAME
						)
					)
				);
			}
		}

		List<DependentField> dependentBrowseFieldsList = BrowseConverUtil.generateDependentBrowseFields(browseField);
		builder.addAllDependentFields(dependentBrowseFieldsList);

		return builder;
	}



	public static List<DependentField> generateDependentBrowseFields(MBrowseField browseField) {
		List<DependentField> depenentFieldsList = new ArrayList<>();
		if (browseField == null) {
			return depenentFieldsList;
		}

		MViewColumn viewColumn = MViewColumn.getById(
			browseField.getCtx(),
			browseField.getAD_View_Column_ID(),
			null
		);
		final String parentColumnName = viewColumn.getColumnName();

		String elementName = null;
		if(viewColumn.getAD_Column_ID() != 0) {
			MColumn column = MColumn.get(browseField.getCtx(), viewColumn.getAD_Column_ID());
			elementName = column.getColumnName();
		}
		if(Util.isEmpty(elementName, true)) {
			elementName = browseField.getAD_Element().getColumnName();
		}
		final String parentElementName = elementName;

		MBrowse browse = MBrowse.get(
			browseField.getCtx(),
			browseField.getAD_Browse_ID()
		);
		List<MBrowseField> browseFieldsList = browse.getFields();
		if (browseFieldsList == null || browseFieldsList.isEmpty()) {
			return depenentFieldsList;
		}

		browseFieldsList.stream()
			.filter(currentBrowseField -> {
				if(currentBrowseField == null || !currentBrowseField.isActive()) {
					return false;
				}
				// Display Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDisplayLogic())
					|| ContextManager.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDisplayLogic())) {
					return true;
				}
				// Default Value
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDefaultValue())
					|| ContextManager.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDefaultValue())) {
					return true;
				}
				// Default Value 2
				// TODO: Validate range with `_To` suffix
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getDefaultValue2())
					|| ContextManager.isUseParentColumnOnContext(parentElementName, currentBrowseField.getDefaultValue2())) {
					return true;
				}
				// ReadOnly Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentBrowseField.getReadOnlyLogic())
					|| ContextManager.isUseParentColumnOnContext(parentElementName, currentBrowseField.getReadOnlyLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentBrowseField.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(
						currentBrowseField.getCtx(),
						currentBrowseField.getAD_Val_Rule_ID()
					);
					if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())
						|| ContextManager.isUseParentColumnOnContext(parentElementName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentBrowseField -> {
				MViewColumn currentViewColumn = MViewColumn.getById(
					currentBrowseField.getCtx(),
					currentBrowseField.getAD_View_Column_ID(),
					null
				);
				final String currentColumnName = currentViewColumn.getColumnName();
				DependentField.Builder builder = DependentField.newBuilder()
					.setId(
						StringManager.getValidString(
							currentBrowseField.getUUID()
						)
					)
					.setUuid(
						StringManager.getValidString(
							currentBrowseField.getUUID()
						)
					)
					.setInternalId(
						currentBrowseField.getAD_Browse_Field_ID()
					)
					.setColumnName(
						currentColumnName
					)
					.setParentId(
						browse.getAD_Browse_ID()
					)
					.setParentUuid(
						StringManager.getValidString(
							browse.getUUID()
						)
					)
					.setParentName(
						StringManager.getValidString(
							browse.getName()
						)
					)
				;
				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}

}
