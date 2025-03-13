/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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
package org.spin.grpc.service.display_definition;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_FieldGroup;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_S_ResourceAssignment;
import org.adempiere.core.domains.models.X_AD_FieldGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCity;
import org.compiere.model.MColumn;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRefTable;
import org.compiere.model.MRegion;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MValRule;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.backend.grpc.display_definition.Address;
import org.spin.backend.grpc.display_definition.BusinessPartner;
import org.spin.backend.grpc.display_definition.CalendarEntry;
import org.spin.backend.grpc.display_definition.City;
import org.spin.backend.grpc.display_definition.DataEntry;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.DefinitionType;
import org.spin.backend.grpc.display_definition.DependentFieldDefinition;
import org.spin.backend.grpc.display_definition.ExpandCollapseEntry;
import org.spin.backend.grpc.display_definition.ExpandCollapseGroup;
import org.spin.backend.grpc.display_definition.FieldDefinition;
import org.spin.backend.grpc.display_definition.FieldGroup;
import org.spin.backend.grpc.display_definition.GeneralEntry;
import org.spin.backend.grpc.display_definition.HierarchyChild;
import org.spin.backend.grpc.display_definition.HierarchyParent;
import org.spin.backend.grpc.display_definition.KanbanEntry;
import org.spin.backend.grpc.display_definition.KanbanStep;
import org.spin.backend.grpc.display_definition.MosaicEntry;
import org.spin.backend.grpc.display_definition.Reference;
import org.spin.backend.grpc.display_definition.Region;
import org.spin.backend.grpc.display_definition.ResourceEntry;
import org.spin.backend.grpc.display_definition.TimelineEntry;
import org.spin.backend.grpc.display_definition.WorkflowEntry;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.store.util.VueStoreFrontUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.solop.sp010.data.BaseFieldItem;
import com.solop.sp010.data.calendar.CalendarItem;
import com.solop.sp010.data.expand_collapse.ExpandCollapseItem;
import com.solop.sp010.data.general.GeneralItem;
import com.solop.sp010.data.generic.GenericItem;
import com.solop.sp010.data.hierarchy.HierarchyChildItem;
import com.solop.sp010.data.hierarchy.HierarchySummary;
import com.solop.sp010.data.kanban.KanbanColumn;
import com.solop.sp010.data.kanban.KanbanItem;
import com.solop.sp010.data.mosaic.MosaicItem;
import com.solop.sp010.data.resource.ResourceItem;
import com.solop.sp010.data.timeline.TimeLineItem;
import com.solop.sp010.data.workflow.WorkflowColumn;
import com.solop.sp010.data.workflow.WorkflowItem;
import com.solop.sp010.util.DisplayDefinitionChanges;

public class DisplayDefinitionConvertUtil {


	public static DefinitionMetadata.Builder convertDefinitionMetadata(PO record, boolean isWithFields) {
		DefinitionMetadata.Builder builder = DefinitionMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
		MTable table = MTable.get(Env.getCtx(), record.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID));
		builder.setId(
				record.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					record.get_UUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					record.get_ValueAsString("Value")
				)
			)
			.setName(
				StringManager.getValidString(
					record.get_ValueAsString(
						I_AD_Element.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					record.get_ValueAsString(
						I_AD_Element.COLUMNNAME_Description
					)
				)
			)
			.setTableId(
				table.getAD_Table_ID()
			)
			.setTableName(
				StringManager.getValidString(
					table.getTableName()
				)
			)
			.setIsResource(
				record.get_ValueAsBoolean(
					DisplayDefinitionChanges.SP010_IsResource
				)
			)
			.setIsInfoRecord(
				record.get_ValueAsBoolean(
					DisplayDefinitionChanges.SP010_IsInfoRecord
				)
			)
			.setIsInsertRecord(
				record.get_ValueAsBoolean(
					I_AD_Tab.COLUMNNAME_IsInsertRecord
				)
			)
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					record.get_ValueAsString(
						I_AD_Tab.COLUMNNAME_WhereClause
					)
				)
			)
		;

		String displayType = record.get_ValueAsString(DisplayDefinitionChanges.SP010_DisplayType);
		builder.setDisplayType(
			StringManager.getValidString(
				displayType
			)
		);
		if (!Util.isEmpty(displayType, true)) {
			if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Calendar)) {
				builder.setType(
					DefinitionType.CALENDAR
				);
				int validFromColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_ValidFrom_ID
				);
				if (validFromColumnId > 0 ) {
					MColumn validFromColumn = MColumn.get(Env.getCtx(), validFromColumnId);
					if (validFromColumn != null && validFromColumn.getAD_Column_ID() > 0) {
						builder.setValidFromColumn(
							StringManager.getValidString(
								validFromColumn.getColumnName()
							)
						);
					}
				}
				int validToColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_ValidTo_ID
				);
				if (validToColumnId > 0) {
					MColumn validToColumn = MColumn.get(Env.getCtx(), validToColumnId);
					if (validToColumn != null && validToColumn.getAD_Column_ID() > 0) {
						builder.setValidToColumn(
							StringManager.getValidString(
								validToColumn.getColumnName()
							)
						);
					}
				}
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_General)) {
				builder.setType(
					DefinitionType.GENERAL
				);
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Hierarchy)) {
				builder.setType(
					DefinitionType.HIERARCHY
				);

				final String whereClause = "SP010_ParentDD_ID = ? AND SP010_DisplayType = ?";
				PO childDisplayDefinition = new Query(
					Env.getCtx(),
					DisplayDefinitionChanges.SP010_DisplayDefinition,
					whereClause,
					null
				)
					.setParameters(
						record.get_ID(),
						DisplayDefinitionChanges.SP010_DisplayType_Hierarchy
					)
					.setOnlyActiveRecords(true)
					.setOrderBy("Created DESC")
					.first()
				;
				if (childDisplayDefinition != null) {
					DefinitionMetadata.Builder childBuilder = convertDefinitionMetadata(childDisplayDefinition, isWithFields);
					builder.setChildDisplayDefinition(childBuilder);
				}
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Kanban)
				|| displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_ExpandCollapse)
				|| displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Workflow)) {
				if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Kanban)) {
					builder.setType(
						DefinitionType.KANBAN
					);
				} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_ExpandCollapse)) {
					builder.setType(
						DefinitionType.EXPAND_COLLAPSE
					);
				} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Workflow)) {
					builder.setType(
						DefinitionType.WORKFLOW
					);
				}
				int groupColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_Group_ID
				);
				if (groupColumnId > 0 ) {
					MColumn groupColumn = MColumn.get(Env.getCtx(), groupColumnId);
					if (groupColumn != null && groupColumn.getAD_Column_ID() > 0) {
						builder.setGroupColumn(
							StringManager.getValidString(
								groupColumn.getColumnName()
							)
						);
					}
				}
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Mosaic)) {
				builder.setType(
					DefinitionType.MOSAIC
				);
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Resource)) {
				builder.setType(
					DefinitionType.RESOURCE
				);
				int validFromColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_ValidFrom_ID
				);
				if (validFromColumnId > 0 ) {
					MColumn validFromColumn = MColumn.get(Env.getCtx(), validFromColumnId);
					if (validFromColumn != null && validFromColumn.getAD_Column_ID() > 0) {
						builder.setValidFromColumn(
							StringManager.getValidString(
								validFromColumn.getColumnName()
							)
						);
					}
				}
				int validToColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_ValidTo_ID
				);
				if (validToColumnId > 0) {
					MColumn validToColumn = MColumn.get(Env.getCtx(), validToColumnId);
					if (validToColumn != null && validToColumn.getAD_Column_ID() > 0) {
						builder.setValidToColumn(
							StringManager.getValidString(
								validToColumn.getColumnName()
							)
						);
					}
				}
			} else if (displayType.equals(DisplayDefinitionChanges.SP010_DisplayType_Timeline)) {
				builder.setType(
					DefinitionType.TIMELINE
				);
				int dateColumnId = record.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_Date_ID
				);
				if (dateColumnId > 0 ) {
					MColumn dateColumn = MColumn.get(Env.getCtx(), dateColumnId);
					if (dateColumn != null && dateColumn.getAD_Column_ID() > 0) {
						builder.setDateColumn(
							StringManager.getValidString(
								dateColumn.getColumnName()
							)
						);
					}
				}
			}
		}

		// Fields
		HashMap<String, String> columnsMap = new HashMap<String, String>();
		if (record.get_ValueAsBoolean(DisplayDefinitionChanges.SP010_IsResource)) {
			MTable resourceAssignmentTable = MTable.get(Env.getCtx(), I_S_ResourceAssignment.Table_Name);

			// Assign Date From
			MColumn assignDateFromColumn = resourceAssignmentTable.getColumn(I_S_ResourceAssignment.COLUMNNAME_AssignDateFrom);
			FieldDefinition.Builder assignDateFromFieldBuilder = DisplayDefinitionConvertUtil.convertFieldDefinitionByColumn(assignDateFromColumn);
			assignDateFromFieldBuilder
				.setDisplayDefinitionId(
					record.get_ID()
				)
				.setSeqNoGrid(3)
				.setSequence(3)
			;
			builder.addFieldDefinitions(
				assignDateFromFieldBuilder.build()
			);
			columnsMap.put(
				assignDateFromColumn.getColumnName(),
				assignDateFromColumn.getColumnName()
			);
			// Assign Date To
			MColumn assignDateToColumn = resourceAssignmentTable.getColumn(I_S_ResourceAssignment.COLUMNNAME_AssignDateTo);
			FieldDefinition.Builder assignDateToFieldBuilder = DisplayDefinitionConvertUtil.convertFieldDefinitionByColumn(assignDateToColumn);
			assignDateToFieldBuilder
				.setDisplayDefinitionId(
					record.get_ID()
				)
				.setSeqNoGrid(4)
				.setSequence(4)
			;
			builder.addFieldDefinitions(
				assignDateToFieldBuilder.build()
			);
			columnsMap.put(
				assignDateToColumn.getColumnName(),
				assignDateToColumn.getColumnName()
			);
			// Resource
			MColumn resourceColumn = resourceAssignmentTable.getColumn(I_S_ResourceAssignment.COLUMNNAME_S_Resource_ID);
			FieldDefinition.Builder resourceFieldBuilder = DisplayDefinitionConvertUtil.convertFieldDefinitionByColumn(resourceColumn);
			resourceFieldBuilder
				.setDisplayDefinitionId(
					record.get_ID()
				)
				.setSeqNoGrid(5)
				.setSequence(5)
				.setIsUpdateRecord(false)
			;
			builder.addFieldDefinitions(
				resourceFieldBuilder.build()
			);
			columnsMap.put(
				resourceColumn.getColumnName(),
				resourceColumn.getColumnName()
			);
		}

		if (isWithFields) {
			MTable fieldTable = RecordUtil.validateAndGetTable(
				DisplayDefinitionChanges.SP010_Field
			);
			new Query(
				Env.getCtx(),
				DisplayDefinitionChanges.SP010_Field,
				"SP010_DisplayDefinition_ID = ?",
				null
			)
				.setParameters(record.get_ID())
				.setOnlyActiveRecords(true)
				.setOrderBy(
					I_AD_Field.COLUMNNAME_SeqNo
				)
				.getIDsAsList()
				.forEach(fieldId -> {
					PO field = fieldTable.getPO(fieldId, null);
					MColumn column = MColumn.get(
						field.getCtx(),
						field.get_ValueAsInt(
							I_AD_Column.COLUMNNAME_AD_Column_ID
						)
					);
					if (columnsMap.containsKey(column.getColumnName())) {
						// omit this column
						return;
					}
					FieldDefinition.Builder fieldBuilder = DisplayDefinitionConvertUtil.convertFieldDefinition(field);
					builder.addFieldDefinitions(
						fieldBuilder.build()
					);
				})
			;
		}

		return builder;
	}

	public static List<DependentFieldDefinition> generateDependentFieldDefinitions(PO field) {
		List<DependentFieldDefinition> depenentFieldsList = new ArrayList<DependentFieldDefinition>();
		if (field == null) {
			return depenentFieldsList;
		}

		final int columnId = field.get_ValueAsInt(
			I_AD_Field.COLUMNNAME_AD_Column_ID
		);
		final String parentColumnName = MColumn.getColumnName(field.getCtx(), columnId);

		PO displayDefinition = new Query(
			Env.getCtx(),
			DisplayDefinitionChanges.SP010_DisplayDefinition,
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(field.get_ValueAsInt(DisplayDefinitionChanges.SP010_DisplayDefinition_ID))
			.setOnlyActiveRecords(true)
			.first()
		;
		if (displayDefinition == null || displayDefinition.get_ID() <= 0) {
			return depenentFieldsList;
		}
		List<PO> fieldsList = new Query(
			Env.getCtx(),
			DisplayDefinitionChanges.SP010_Field,
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(displayDefinition.get_ID())
			.setOnlyActiveRecords(true)
			.setOrderBy(
				I_AD_Field.COLUMNNAME_SeqNo
			)
			.list()
		;

		if (fieldsList == null || fieldsList.isEmpty()) {
			return depenentFieldsList;
		}

		fieldsList.stream()
			.filter(currentField -> {
				if (currentField == null || !currentField.isActive()) {
					return false;
				}
				// Display Logic
				final String displayLogic = currentField.get_ValueAsString(
					I_AD_Field.COLUMNNAME_DisplayLogic
				);
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, displayLogic)) {
					return true;
				}
				// Default Value of Field
				final String defaultValue = currentField.get_ValueAsString(
					I_AD_Field.COLUMNNAME_DefaultValue
				);
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, defaultValue)) {
					return true;
				}
				// Dynamic Validation
				final int dynamicValidationId = currentField.get_ValueAsInt(
					I_AD_Field.COLUMNNAME_AD_Val_Rule_ID
				);
				if (dynamicValidationId > 0) {
					MValRule validationRule = MValRule.get(
						currentField.getCtx(),
						dynamicValidationId
					);
					if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}

				MColumn currentColumn = MColumn.get(
					currentField.getCtx(),
					currentField.get_ValueAsInt(
						I_AD_Field.COLUMNNAME_AD_Column_ID
					)
				);
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
					currentField.get_ValueAsInt(
						I_AD_Field.COLUMNNAME_AD_Column_ID
					)
				);
				DependentFieldDefinition.Builder builder = DependentFieldDefinition.newBuilder()
					.setId(
						StringManager.getValidString(
							currentField.get_UUID()
						)
					)
					.setUuid(
						StringManager.getValidString(
							currentField.get_UUID()
						)
					)
					.setInternalId(
						currentField.get_ID()
					)
					.setColumnName(
						StringManager.getValidString(
							currentColumnName
						)
					)
					.setParentId(
						displayDefinition.get_ID()
					)
					.setParentUuid(
						StringManager.getValidString(
							displayDefinition.get_UUID()
						)
					)
					.setParentName(
						StringManager.getValidString(
							displayDefinition.get_ValueAsString(
								I_AD_Field.COLUMNNAME_Name
							)
						)
					)
				;
				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}

	/**
	 * Convert Reference to builder
	 * @param info
	 * @return
	 */
	public static Reference.Builder convertReference(Properties context, MLookupInfo info) {
		Reference.Builder builder = Reference.newBuilder();
		if (info == null) {
			return builder;
		}

		List<String> contextColumnsList = ContextManager.getContextColumnNames(
			Optional.ofNullable(info.QueryDirect).orElse("")
			+ Optional.ofNullable(info.Query).orElse("")
			+ Optional.ofNullable(info.ValidationCode).orElse("")
		);
		builder.setTableName(
				StringManager.getValidString(
					info.TableName
				)
			)
			.setReferenceId(
				info.DisplayType
			)
			.setReferenceValueId(
				info.AD_Reference_Value_ID
			)
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		//	Return
		return builder;
	}

	/**
	 * Convert Field Group to builder
	 * @param fieldGroupId
	 * @return
	 */
	public static FieldGroup.Builder convertFieldGroup(int fieldGroupId) {
		FieldGroup.Builder builder = FieldGroup.newBuilder();
		if(fieldGroupId <= 0) {
			return builder;
		}
		X_AD_FieldGroup fieldGroup  = new X_AD_FieldGroup(Env.getCtx(), fieldGroupId, null);
		//	Get translation
		String name = null;
		String language = Env.getAD_Language(Env.getCtx());
		if(!Util.isEmpty(language)) {
			name = fieldGroup.get_Translation(
				I_AD_FieldGroup.COLUMNNAME_Name,
				language
			);
		}
		//	Validate for default
		if(Util.isEmpty(name)) {
			name = fieldGroup.getName();
		}
		//	Field Group
		builder = FieldGroup.newBuilder()
			.setId(
				fieldGroup.getAD_FieldGroup_ID()
			)
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

	public static FieldDefinition.Builder convertFieldDefinition(PO fieldDefinitionItem) {
		FieldDefinition.Builder builder = FieldDefinition.newBuilder();
		if (fieldDefinitionItem == null || fieldDefinitionItem.get_ID() <= 0) {
			return builder;
		}
		MColumn column = MColumn.get(
			fieldDefinitionItem.getCtx(),
			fieldDefinitionItem.get_ValueAsInt(
				I_AD_Column.COLUMNNAME_AD_Column_ID
			)
		);
		int displayTypeId = fieldDefinitionItem.get_ValueAsInt(I_AD_Column.COLUMNNAME_AD_Reference_ID);
		if (displayTypeId <= 0) {
			displayTypeId = column.getAD_Reference_ID();
		}
		String defaultValue = fieldDefinitionItem.get_ValueAsString(
			I_AD_Column.COLUMNNAME_DefaultValue
		);
		if (Util.isEmpty(defaultValue, true)) {
			defaultValue = column.getDefaultValue();
		}

		String isMandatoryString = fieldDefinitionItem.get_ValueAsString(
			I_AD_Field.COLUMNNAME_IsMandatory
		);
		boolean isMandatory = column.isMandatory();
		if (!Util.isEmpty(isMandatoryString, true)) {
			isMandatory = BooleanManager.getBooleanFromString(isMandatoryString);
		}

		boolean isAllowCopy = column.isAllowCopy();
		String isAllowCopyString = fieldDefinitionItem.get_ValueAsString(
			I_AD_Field.COLUMNNAME_IsAllowCopy
		);
		if (!Util.isEmpty(isAllowCopyString, true)) {
			isAllowCopy = BooleanManager.getBooleanFromString(isAllowCopyString);
		}

		boolean isSelectionColumn = column.isSelectionColumn();
		String isSelectionColumnString = fieldDefinitionItem.get_ValueAsString(
			I_AD_Column.COLUMNNAME_IsSelectionColumn
		);
		if (!Util.isEmpty(isSelectionColumnString, true)) {
			isSelectionColumn = BooleanManager.getBooleanFromString(isSelectionColumnString);
		}

		builder.setId(
				StringManager.getValidString(
					fieldDefinitionItem.get_UUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					fieldDefinitionItem.get_UUID()
				)
			)
			.setInternalId(
				fieldDefinitionItem.get_ID()
			)
			.setDisplayDefinitionId(
				fieldDefinitionItem.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_DisplayDefinition_ID
				)
			)
			.setColumnName(
				StringManager.getValidString(
					column.getColumnName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					fieldDefinitionItem.get_Translation(
						I_AD_Column.COLUMNNAME_Description
					)
				)
			)
			.setHelp(
				StringManager.getValidString(
					fieldDefinitionItem.get_Translation(
						I_AD_Column.COLUMNNAME_Help
					)
				)
			)
			.setName(
				StringManager.getValidString(
					fieldDefinitionItem.get_Translation(
						I_AD_Column.COLUMNNAME_Name
					)
				)
			)
			.setDisplayType(
				displayTypeId
			)
			.setSequence(
				fieldDefinitionItem.get_ValueAsInt(
					I_AD_Field.COLUMNNAME_SeqNo
				)
			)
			.setIsDisplayed(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsDisplayed
				)
			)
			.setDisplayLogic(
				StringManager.getValidString(
					fieldDefinitionItem.get_ValueAsString(
						I_AD_Field.COLUMNNAME_DisplayLogic
					)
				)
			)
			.setIsReadOnly(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsReadOnly
				)
			)
			.setReadOnlyLogic(
				StringManager.getValidString(
					column.getReadOnlyLogic()
				)
			)
			.setIsMandatory(
				isMandatory
			)
			.setMandatoryLogic(
				StringManager.getValidString(
					column.getMandatoryLogic()
				)
			)
			.setDefaultValue(
				StringManager.getValidString(
					defaultValue
				)
			)
			.setIsDisplayedGrid(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsDisplayedGrid
				)
			)
			.setSeqNoGrid(
				fieldDefinitionItem.get_ValueAsInt(
					I_AD_Field.COLUMNNAME_SeqNoGrid
				)
			)
			.setIsHeading(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsHeading
				)
			)
			.setIsFieldOnly(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsFieldOnly
				)
			)
			.setIsEncrypted(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsEncrypted
				)
			)
			.setIsQuickEntry(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Field.COLUMNNAME_IsQuickEntry
				)
			)
			.setIsInsertRecord(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Tab.COLUMNNAME_IsInsertRecord
				)
			)
			.setIsUpdateRecord(
				fieldDefinitionItem.get_ValueAsBoolean(
					"SP010_IsUpdateRecord"
				)
			)
			.setIsAllowCopy(
				isAllowCopy
			)
			.setIsSelectionColumn(
				isSelectionColumn
			)
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					defaultValue
				)
			)
		;

		final int fieldGroupId = fieldDefinitionItem.get_ValueAsInt(
			I_AD_Field.COLUMNNAME_AD_FieldGroup_ID
		);
		if (fieldGroupId > 0) {
			FieldGroup.Builder fieldGroupBuilder = convertFieldGroup(fieldGroupId);
			builder.setFieldGroup(fieldGroupBuilder);
		}

		//	Reference Value
		int referenceValueId = fieldDefinitionItem.get_ValueAsInt(
			I_AD_Field.COLUMNNAME_AD_Reference_Value_ID
		);
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
			int validationRuleId = fieldDefinitionItem.get_ValueAsInt(
				I_AD_Field.COLUMNNAME_AD_Val_Rule_ID
			);
			if(validationRuleId <= 0) {
				validationRuleId = column.getAD_Val_Rule_ID();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, column.getColumnName(), validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = convertReference(fieldDefinitionItem.getCtx(), info);
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

		return builder;
	}
	public static FieldDefinition.Builder convertFieldDefinitionByColumn(MColumn column) {
		FieldDefinition.Builder builder = FieldDefinition.newBuilder();
		if (column == null || column.getAD_Column_ID() <= 0) {
			return builder;
		}

		builder.setId(
				StringManager.getValidString(
					column.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					column.getUUID()
				)
			)
			.setInternalId(
				column.getAD_Column_ID()
			)
			.setColumnName(
				StringManager.getValidString(
					column.getColumnName()
				)
			)
			.setName(
				StringManager.getValidString(
					column.get_Translation(
						I_AD_Column.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					column.get_Translation(
						I_AD_Column.COLUMNNAME_Description
					)
				)
			)
			.setHelp(
				StringManager.getValidString(
					column.get_Translation(
						I_AD_Column.COLUMNNAME_Help
					)
				)
			)
			.setDisplayType(
				column.getAD_Reference_ID()
			)
			.setIsDisplayed(true)
			.setReadOnlyLogic(
				StringManager.getValidString(
					column.getReadOnlyLogic()
				)
			)
			.setIsMandatory(
				column.isMandatory()
			)
			.setMandatoryLogic(
				StringManager.getValidString(
					column.getMandatoryLogic()
				)
			)
			.setDefaultValue(
				StringManager.getValidString(
					column.getDefaultValue()
				)
			)
			.setIsDisplayedGrid(
				true
			)
			.setIsEncrypted(
				column.isEncrypted()
			)
			.setIsInsertRecord(true)
			.setIsUpdateRecord(true)
			.setIsSelectionColumn(
				column.isSelectionColumn()
			)
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					column.getDefaultValue()
				)
			)
		;

		// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
		int displayTypeId = ReferenceUtil.overwriteDisplayType(
			column.getAD_Reference_ID(),
			column.getAD_Reference_Value_ID()
		);
		if (ReferenceUtil.validateReference(displayTypeId)) {
			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, column.getAD_Reference_Value_ID(), column.getColumnName(), column.getAD_Val_Rule_ID()
			);
			if (info != null) {
				Reference.Builder referenceBuilder = convertReference(column.getCtx(), info);
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

		return builder;
	}

	public static Value convertFieldItem(BaseFieldItem fieldItem) {
		Struct.Builder fieldValue = Struct.newBuilder();

		fieldValue.putFields(
			"value",
			ValueManager.getValueFromObject(
				fieldItem.getValue()
			).build()
		);
		if(!Util.isEmpty(fieldItem.getDisplayValue())) {
			fieldValue.putFields(
				"display_value",
				ValueManager.getValueFromObject(
					fieldItem.getDisplayValue()
				).build()
			);
			fieldValue.putFields(
				"table_name",
				ValueManager.getValueFromObject(
					fieldItem.getTableName()
				).build()
			);
		}
		return Value.newBuilder()
			.setStructValue(
				fieldValue.build()
			).build()
		;
	}


	public static CalendarEntry.Builder convertCalentarEntry(CalendarItem calendarItem) {
		CalendarEntry.Builder builder = CalendarEntry.newBuilder();
		if (calendarItem == null) {
			return builder;
		}

		builder.setId(
				calendarItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					calendarItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					calendarItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					calendarItem.getDescription()
				)
			)
			.setValidFrom(
				ValueManager.getTimestampFromDate(
					calendarItem.getValidFrom()
				)
			)
			.setValidTo(
				ValueManager.getTimestampFromDate(
					calendarItem.getValidTo()
				)
			)
			.setIsConfirmed(
				calendarItem.isConfirmed()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		calendarItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static ExpandCollapseGroup.Builder convertExpandCollapseGroup(com.solop.sp010.data.expand_collapse.ExpandCollapseGroup group) {
		ExpandCollapseGroup.Builder builder = ExpandCollapseGroup.newBuilder();
		if (group == null) {
			return builder;
		}
		builder
			.setValue(
				StringManager.getValidString(
					group.getGroupCode()
				)
			)
			.setName(
				StringManager.getValidString(
					group.getName()
				)
			)
			.setSequence(
				group.getSequence()
			)
		;
		return builder;
	}

	public static ExpandCollapseEntry.Builder convertExpandCollapseEntry(ExpandCollapseItem expandCollapseItem) {
		ExpandCollapseEntry.Builder builder = ExpandCollapseEntry.newBuilder();
		if (expandCollapseItem == null) {
			return builder;
		}
		builder
			.setId(
				expandCollapseItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					expandCollapseItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					expandCollapseItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					expandCollapseItem.getDescription()
				)
			)
			.setIsActive(
				expandCollapseItem.isActive()
			)
			.setIsReadOnly(
				expandCollapseItem.isReadOnly()
			)
			.setGroupId(
				StringManager.getValidString(
					expandCollapseItem.getGroupCode()
				)
			)
			.setSequence(
				expandCollapseItem.getSequence()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		expandCollapseItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static GeneralEntry.Builder convertGeneralEntry(GeneralItem generalItem) {
		GeneralEntry.Builder builder = GeneralEntry.newBuilder();
		if (generalItem == null) {
			return builder;
		}
		builder
			.setId(
				generalItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					generalItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					generalItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					generalItem.getDescription()
				)
			)
			.setIsActive(
				generalItem.isActive()
			)
			.setIsReadOnly(
				generalItem.isReadOnly()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		generalItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static HierarchyParent.Builder convertHierarchyParent(HierarchySummary summaryItem) {
		HierarchyParent.Builder builder = HierarchyParent.newBuilder();
		if (summaryItem == null) {
			return builder;
		}
		builder
			.setTitle(
				StringManager.getValidString(
					summaryItem.getName()
				)
			)
			.setId(
				summaryItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					summaryItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					summaryItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					summaryItem.getDescription()
				)
			)
			.setIsActive(
				summaryItem.isActive()
			)
			.setIsReadOnly(
				summaryItem.isReadOnly()
			)
			.setLinkId(
				NumberManager.getIntFromString(
					summaryItem.getGroupCode()
				)
			)
		;

		// Additional fields
		Struct.Builder fields = Struct.newBuilder();
		summaryItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);

		// childs
		summaryItem.getChildItems().forEach(childItem -> {
			HierarchyChild.Builder builderChild = convertHierarchyChild(childItem);
			builder.addChilds(builderChild);
		});

		return builder;
	}

	public static HierarchyChild.Builder convertHierarchyChild(HierarchyChildItem childItem) {
		HierarchyChild.Builder builder = HierarchyChild.newBuilder();
		if (childItem == null) {
			return builder;
		}
		builder
			.setId(
				childItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					childItem.getUuid()
				)
			)
			// .setTitle(
			// 	StringManager.getValidString(
			// 		summaryItem.getName()
			// 	)
			// )
			.setTitle(
				StringManager.getValidString(
					childItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					childItem.getDescription()
				)
			)
			.setIsActive(
				childItem.isActive()
			)
			.setIsReadOnly(
				childItem.isReadOnly()
			)
			.setParentId(
				childItem.getGroupCode()
			)
		;

		// Additional fields
		Struct.Builder fields = Struct.newBuilder();
		childItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);

		return builder;
	}



	public static KanbanStep.Builder convertKanbanStep(KanbanColumn kanbanColumn) {
		KanbanStep.Builder builder = KanbanStep.newBuilder();
		if (kanbanColumn == null) {
			return builder;
		}
		builder
			.setValue(
				StringManager.getValidString(
					kanbanColumn.getGroupCode()
				)
			)
			.setName(
				StringManager.getValidString(
					kanbanColumn.getName()
				)
			)
			.setSequence(
				kanbanColumn.getSequence()
			)
		;
		return builder;
	}

	public static KanbanEntry.Builder convertKanbanEntry(KanbanItem kanbanItem) {
		KanbanEntry.Builder builder = KanbanEntry.newBuilder();
		if (kanbanItem == null) {
			return builder;
		}
		builder
			.setId(
				kanbanItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					kanbanItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					kanbanItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					kanbanItem.getDescription()
				)
			)
			.setIsActive(
				kanbanItem.isActive()
			)
			.setIsReadOnly(
				kanbanItem.isReadOnly()
			)
			.setGroupId(
				StringManager.getValidString(
					kanbanItem.getGroupCode()
				)
			)
			.setSequence(
				kanbanItem.getSequence()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		kanbanItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static MosaicEntry.Builder convertMosaicEntry(MosaicItem mosaicItem) {
		MosaicEntry.Builder builder = MosaicEntry.newBuilder();
		if (mosaicItem == null) {
			return builder;
		}
		builder
			.setId(
				mosaicItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					mosaicItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					mosaicItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					mosaicItem.getDescription()
				)
			)
			.setIsActive(
				mosaicItem.isActive()
			)
			.setIsReadOnly(
				mosaicItem.isReadOnly()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		mosaicItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static ResourceEntry.Builder convertResourceEntry(ResourceItem resourceItem) {
		ResourceEntry.Builder builder = ResourceEntry.newBuilder();
		if (resourceItem == null) {
			return builder;
		}

		builder.setId(
				resourceItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					resourceItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					resourceItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					resourceItem.getDescription()
				)
			)
			.setValidFrom(
				ValueManager.getTimestampFromDate(
					resourceItem.getValidFrom()
				)
			)
			.setValidTo(
				ValueManager.getTimestampFromDate(
					resourceItem.getValidTo()
				)
			)
			.setIsConfirmed(
				resourceItem.isConfirmed()
			)
			.setName(
				StringManager.getValidString(
					resourceItem.getName()
				)
			)
			.setGroupName(
				StringManager.getValidString(
					resourceItem.getGroupName()
				)
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		resourceItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		builder.setFields(fields);
		return builder;
	}



	public static TimelineEntry.Builder convertTimelineEntry(TimeLineItem timelineItem) {
		TimelineEntry.Builder builder = TimelineEntry.newBuilder();
		if (timelineItem == null) {
			return builder;
		}
		builder
			.setId(
				timelineItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					timelineItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					timelineItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					timelineItem.getDescription()
				)
			)
			.setIsActive(
				timelineItem.isActive()
			)
			.setIsReadOnly(
				timelineItem.isReadOnly()
			)
			.setDate(
				ValueManager.getTimestampFromDate(
					timelineItem.getDate()
				)
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		timelineItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		return builder;
	}



	public static WorkflowStep.Builder convertWorkflowStep(WorkflowColumn kanbanColumn) {
		WorkflowStep.Builder builder = WorkflowStep.newBuilder();
		if (kanbanColumn == null) {
			return builder;
		}
		builder
			.setValue(
				StringManager.getValidString(
					kanbanColumn.getGroupCode()
				)
			)
			.setName(
				StringManager.getValidString(
					kanbanColumn.getName()
				)
			)
			.setSequence(
				kanbanColumn.getSequence()
			)
		;
		return builder;
	}

	public static WorkflowEntry.Builder convertWorkflowEntry(WorkflowItem workflowItem) {
		WorkflowEntry.Builder builder = WorkflowEntry.newBuilder();
		if (workflowItem == null) {
			return builder;
		}
		builder
			.setId(
				workflowItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					workflowItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					workflowItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					workflowItem.getDescription()
				)
			)
			.setIsActive(
				workflowItem.isActive()
			)
			.setIsReadOnly(
				workflowItem.isReadOnly()
			)
			.setGroupId(
				StringManager.getValidString(
					workflowItem.getGroupCode()
				)
			)
			.setSequence(
				workflowItem.getSequence()
			)
		;
		Struct.Builder fields = Struct.newBuilder();
		workflowItem.getFields().entrySet().forEach(field -> {
			BaseFieldItem fieldItem = field.getValue();
			String columnName = StringManager.getValidString(
				fieldItem.getColumnName()
			);
			Value fieldValue = convertFieldItem(fieldItem);
			
			fields.putFields(
				columnName,
				fieldValue
			);
		});
		return builder;
	}


	public static DataEntry.Builder convertDataEntry(PO displayDefinition, GenericItem baseItem) {
		if (baseItem == null || baseItem.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		DataEntry.Builder builder = DataEntry.newBuilder()
			.setId(
				baseItem.getId()
			)
			.setUuid(
				StringManager.getValidString(
					baseItem.getUuid()
				)
			)
			.setTitle(
				StringManager.getValidString(
					baseItem.getTitle()
				)
			)
			.setDescription(
				StringManager.getValidString(
					baseItem.getDescription()
				)
			)
			.setIsActive(
				baseItem.isActive()
			)
			.setIsReadOnly(
				baseItem.isReadOnly()
			)
		;

		//	Additional fields
		MTable fieldTable = MTable.get(Env.getCtx(), DisplayDefinitionChanges.SP010_Field);
		if(fieldTable == null) {
			return builder;
		}

		Struct.Builder additionalFields = Struct.newBuilder();
		baseItem.getFields()
			.forEach((fieldId, fieldEntry) -> {
				PO field = fieldTable.getPO(fieldId, null);
				int referenceId = field.get_ValueAsInt(
					I_AD_Field.COLUMNNAME_AD_Reference_ID
				);
				MColumn column = MColumn.get(
					Env.getCtx(),
					field.get_ValueAsInt(
						I_AD_Column.COLUMNNAME_AD_Column_ID
					)
				);
				if(referenceId <= 0) {
					referenceId = column.getAD_Reference_ID();
				}
				Struct.Builder fieldItem = Struct.newBuilder();

				// value
				Value.Builder valueBuilder = ValueManager.getValueFromReference(
					fieldEntry.getValue(),
					referenceId
				);
				fieldItem.putFields(
					"value",
					valueBuilder.build()
				);
				// display value
				String displayValue = fieldEntry.getDisplayValue();
				if (fieldEntry.getValue() == null || Util.isEmpty(displayValue, true)) {
					displayValue = null;
				}
				Value.Builder displayValueBuilder = ValueManager.getValueFromString(
					displayValue
				);
				fieldItem.putFields(
					"display_value",
					displayValueBuilder.build()
				);

				Value.Builder structField = Value.newBuilder().setStructValue(
					fieldItem
				);
				additionalFields.putFields(
					column.getColumnName(),
					structField.build()
				);
			})
		;

		if (displayDefinition.get_ValueAsBoolean(DisplayDefinitionChanges.SP010_IsResource)) {
			MTable table = MTable.get(
				Env.getCtx(),
				displayDefinition.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID)
			);

			PO entity = table.getPO(baseItem.getId(), null);
			if (entity != null) {
				POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
				MTable tableResource = MTable.get(Env.getCtx(), I_S_ResourceAssignment.Table_Name);
				final List<String> RESOURCE_ASSIGMENT_COLUMNS = Arrays.asList(
					I_S_ResourceAssignment.COLUMNNAME_S_Resource_ID,
					I_S_ResourceAssignment.COLUMNNAME_Name,
					I_S_ResourceAssignment.COLUMNNAME_AssignDateFrom,
					I_S_ResourceAssignment.COLUMNNAME_AssignDateTo
				);
	
				int resourceAssignmentColumnId = displayDefinition.get_ValueAsInt(
					DisplayDefinitionChanges.SP010_Resource_ID
				);
				MColumn resourceAssignmentColumn = MColumn.get(Env.getCtx(), resourceAssignmentColumnId);
				MResourceAssignment resourceAssignment = new MResourceAssignment(
					displayDefinition.getCtx(),
					entity.get_ValueAsInt(
						resourceAssignmentColumn.getColumnName()
					),
					null
				);

				Language language = Language.getLoginLanguage();
				tableResource.getColumnsAsList()
					.stream()
					.filter(column -> {
						return RESOURCE_ASSIGMENT_COLUMNS.contains(column.getColumnName());
					})
					.forEach(column -> {
						String columnName = column.getColumnName();
						int displayTypeId = column.getAD_Reference_ID();
						Struct.Builder fieldItem = Struct.newBuilder();
						Object value = resourceAssignment.get_Value(
							columnName
						);
						// value
						Value.Builder valueBuilder = ValueManager.getValueFromReference(
							value,
							column.getAD_Reference_ID()
						);
						fieldItem.putFields(
							"value",
							valueBuilder.build()
						);
						// display value
						String displayValue = null;
						if (value != null) {
							if (columnName.equals(poInfo.getTableName() + "_ID")) {
								displayValue = entity.getDisplayValue();
							} else if (ReferenceUtil.validateReference(displayTypeId) || displayTypeId == DisplayType.Button) {
								int referenceValueId = column.getAD_Reference_Value_ID();
								displayTypeId = ReferenceUtil.overwriteDisplayType(
									displayTypeId,
									referenceValueId
								);
								String tableName = null;
								if(displayTypeId == DisplayType.TableDir) {
									tableName = columnName.replace("_ID", "");
								} else if(displayTypeId == DisplayType.Table || displayTypeId == DisplayType.Search) {
									if(referenceValueId <= 0) {
										tableName = columnName.replace("_ID", "");
									} else {
										MRefTable referenceTable = MRefTable.getById(Env.getCtx(), referenceValueId);
										tableName = MTable.getTableName(Env.getCtx(), referenceTable.getAD_Table_ID());
									}
								}
								if (!Util.isEmpty(tableName, true)) {
									int id = NumberManager.getIntegerFromObject(value);
									MTable referenceTable = MTable.get(Env.getCtx(), tableName);
									PO referenceEntity = referenceTable.getPO(id, null);
									if(referenceEntity != null) {
										displayValue = referenceEntity.getDisplayValue();
									}
								}
							} else if (DisplayType.isDate(column.getAD_Reference_ID())) {
								Timestamp date = (Timestamp) value;
								displayValue = DisplayType.getDateFormat(
									column.getAD_Reference_ID(),
									language,
									column.getFormatPattern()
								).format(date);
							} else if (DisplayType.isNumeric(column.getAD_Reference_ID())) {
								if (BigDecimal.class.isAssignableFrom(value.getClass())) {
									BigDecimal number = (BigDecimal) value;
									displayValue = DisplayType.getNumberFormat(
										column.getAD_Reference_ID(),
										language,
										column.getFormatPattern()
									).format(number);
								}
							}
						}
						if (value == null || Util.isEmpty(displayValue, true)) {
							displayValue = null;
						}
						Value.Builder displayValueBuilder = ValueManager.getValueFromString(
							displayValue
						);
						fieldItem.putFields(
							"display_value",
							displayValueBuilder.build()
						);
	
						Value.Builder structField = Value.newBuilder().setStructValue(
							fieldItem
						);
						additionalFields.putFields(
							column.getColumnName(),
							structField.build()
						);
					});
				;

			}
		}

		builder.setFields(
			additionalFields
		);
		

		return builder;
	}



	/**
	 * Convert customer
	 * @param businessPartner
	 * @return
	 */
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		if(businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return BusinessPartner.newBuilder();
		}
		BusinessPartner.Builder customer = BusinessPartner.newBuilder()
			.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setValue(
				StringManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setTaxId(
				StringManager.getValidString(
					businessPartner.getTaxID()
				)
			)
			.setDuns(
				StringManager.getValidString(
					businessPartner.getDUNS()
				)
			)
			.setNaics(
				StringManager.getValidString(
					businessPartner.getNAICS()
				)
			)
			.setName(
				StringManager.getValidString(
					businessPartner.getName()
				)
			)
			.setLastName(
				StringManager.getValidString(
					businessPartner.getName2()
				)
			)
			.setDescription(
				StringManager.getValidString(
					businessPartner.getDescription()
				)
			)
		;
		//	Additional Attributes
		Struct.Builder customerAdditionalAttributes = Struct.newBuilder();
		MTable.get(Env.getCtx(), businessPartner.get_Table_ID()).getColumnsAsList().stream()
		.filter(column -> {
			String columnName = column.getColumnName();
			return !columnName.equals(MBPartner.COLUMNNAME_UUID)
				&& !columnName.equals(MBPartner.COLUMNNAME_Value)
				&& !columnName.equals(MBPartner.COLUMNNAME_TaxID)
				&& !columnName.equals(MBPartner.COLUMNNAME_DUNS)
				&& !columnName.equals(MBPartner.COLUMNNAME_NAICS)
				&& !columnName.equals(MBPartner.COLUMNNAME_Name)
				&& !columnName.equals(MBPartner.COLUMNNAME_Name2)
				&& !columnName.equals(MBPartner.COLUMNNAME_Description)
			;
		}).forEach(column -> {
			String columnName = column.getColumnName();
			Value value = ValueManager.getValueFromReference(
					businessPartner.get_Value(columnName),
					column.getAD_Reference_ID()
				).build();
			customerAdditionalAttributes.putFields(
				columnName,
				value
			);
		});
		customer.setAdditionalAttributes(customerAdditionalAttributes);
		//	Add Address
		Arrays.asList(businessPartner.getLocations(true)).stream()
			.filter(customerLocation -> customerLocation.isActive())
			.forEach(address -> {
				customer.addAddresses(
					convertBusinessPartnerAddress(address)
				);
			});
		return customer;
	}
	/**
	 * Convert Address
	 * @param businessPartnerLocation
	 * @return
	 * @return Address.Builder
	 */
	public static Address.Builder convertBusinessPartnerAddress(MBPartnerLocation businessPartnerLocation) {
		if(businessPartnerLocation == null) {
			return Address.newBuilder();
		}
		MLocation location = businessPartnerLocation.getLocation(true);
		Address.Builder builder = Address.newBuilder()
			.setId(
				businessPartnerLocation.getC_BPartner_Location_ID()
			)
			.setDisplayValue(
				StringManager.getValidString(
					location.toString()
				)
			)
			.setPostalCode(
				StringManager.getValidString(
					location.getPostal()
				)
			)
			.setPostalCodeAdditional(
				StringManager.getValidString(
					location.getPostal_Add()
				)
			)
			.setAddress1(
				StringManager.getValidString(
					location.getAddress1()
				)
			)
			.setAddress2(
				StringManager.getValidString(
					location.getAddress2()
				)
			)
			.setAddress3(
				StringManager.getValidString(
					location.getAddress3()
				)
			)
			.setAddress4(
				StringManager.getValidString(
					location.getAddress4()
				)
			)
			.setPostalCode(
				StringManager.getValidString(
					location.getPostal()
				)
			)
			.setDescription(
				StringManager.getValidString(
					businessPartnerLocation.getDescription()
				)
			)
			.setLocationName(
				StringManager.getValidString(
					businessPartnerLocation.getName()
				)
			)
			.setContactName(
				StringManager.getValidString(
					businessPartnerLocation.getContactPerson()
				)
			)
			.setEmail(
				StringManager.getValidString(
					businessPartnerLocation.getEMail()
				)
			)
			.setPhone(
				StringManager.getValidString(
					businessPartnerLocation.getPhone()
				)
			)
			.setIsDefaultShipping(
				businessPartnerLocation.get_ValueAsBoolean(
					VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping
				)
			)
			.setIsDefaultBilling(
				businessPartnerLocation.get_ValueAsBoolean(
					VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling
				)
			)
		;
		//	Get user from location
		MUser user = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			I_AD_User.COLUMNNAME_C_BPartner_Location_ID + " = ?",
			businessPartnerLocation.get_TrxName()
		)
			.setParameters(businessPartnerLocation.getC_BPartner_Location_ID())
			.setOnlyActiveRecords(true)
			.first();
		String phone = null;
		if(user != null && user.getAD_User_ID() > 0) {
			if(!Util.isEmpty(user.getPhone())) {
				phone = user.getPhone();
			}
			if(!Util.isEmpty(user.getName()) && Util.isEmpty(builder.getContactName())) {
				builder.setContactName(user.getName());
			}
		}
		//	
		builder.setPhone(
			StringManager.getValidString(
				Optional.ofNullable(businessPartnerLocation.getPhone()).orElse(Optional.ofNullable(phone).orElse(""))
			)
		);
		MCountry country = MCountry.get(Env.getCtx(), location.getC_Country_ID());
		builder.setCountryCode(
				StringManager.getValidString(
					country.getCountryCode()
				)
			)
			.setCountryId(
				country.getC_Country_ID()
			)
		;
		//	City
		if(location.getC_City_ID() > 0) {
			MCity city = MCity.get(Env.getCtx(), location.getC_City_ID());
			builder.setCity(
				City.newBuilder()
					.setId(
						city.getC_City_ID()
					)
					.setName(
						StringManager.getValidString(
							city.getName()
						)
					)
			);
		} else {
			builder.setCity(
				City.newBuilder()
					.setName(
						StringManager.getValidString(
							location.getCity()
						)
					)
				)
			;
		}
		//	Region
		if(location.getC_Region_ID() > 0) {
			MRegion region = MRegion.get(Env.getCtx(), location.getC_Region_ID());
			builder.setRegion(
				Region.newBuilder()
					.setId(
						region.getC_Region_ID()
					)
					.setName(
						StringManager.getValidString(
							region.getName()
						)
					)
			);
		}
		//	Additional Attributes
		MTable.get(Env.getCtx(), businessPartnerLocation.get_Table_ID()).getColumnsAsList().stream()
		.map(column -> column.getColumnName())
		.filter(columnName -> {
			return !columnName.equals(MBPartnerLocation.COLUMNNAME_UUID)
				&& !columnName.equals(MBPartnerLocation.COLUMNNAME_Phone)
				&& !columnName.equals(MBPartnerLocation.COLUMNNAME_Name)
			;
		}).forEach(columnName -> {
			Struct.Builder values = Struct.newBuilder()
				.putFields(
					columnName,
					ValueManager.getValueFromObject(
						businessPartnerLocation.get_Value(columnName)
					).build()
				)
			;
			builder.setAdditionalAttributes(values);
		});
		//	
		return builder;
	}

}
