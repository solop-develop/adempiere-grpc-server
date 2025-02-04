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

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_FieldGroup;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_FieldGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.display_definition.CalendarEntry;
import org.spin.backend.grpc.display_definition.DataEntry;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.DefinitionType;
import org.spin.backend.grpc.display_definition.FieldDefinition;
import org.spin.backend.grpc.display_definition.FieldGroup;
import org.spin.backend.grpc.display_definition.KanbanEntry;
import org.spin.backend.grpc.display_definition.KanbanStep;
import org.spin.backend.grpc.display_definition.ResourceEntry;
import org.spin.backend.grpc.display_definition.TimelineEntry;
import org.spin.backend.grpc.display_definition.WorkflowEntry;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.solop.sp010.data.calendar.CalendarItem;
import com.solop.sp010.data.generic.GenericItem;
import com.solop.sp010.data.kanban.KanbanColumn;
import com.solop.sp010.data.kanban.KanbanItem;
import com.solop.sp010.data.resource.ResourceItem;
import com.solop.sp010.data.timeline.TimeLineItem;
import com.solop.sp010.data.workflow.WorkflowColumn;
import com.solop.sp010.data.workflow.WorkflowItem;
import com.solop.sp010.util.Changes;

public class DisplayDefinitionConvertUtil {
	

	public static DefinitionMetadata.Builder convertDefinitionMetadata(PO record) {
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
					Changes.SP010_IsResource
				)
			)
		;

		String displayType = record.get_ValueAsString(Changes.SP010_DisplayType);
		builder.setDisplayType(
			StringManager.getValidString(
				displayType
			)
		);
		if (!Util.isEmpty(displayType, true)) {
			if (displayType.equals(Changes.SP010_DisplayType_Calendar)) {
				builder.setType(
					DefinitionType.CALENDAR
				);
				int validFromColumnId = record.get_ValueAsInt(
					Changes.SP010_ValidFrom_ID
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
					Changes.SP010_ValidTo_ID
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
			} else if (displayType.equals(Changes.SP010_DisplayType_Kanban)) {
				builder.setType(
					DefinitionType.KANBAN
				);
				int groupColumnId = record.get_ValueAsInt(
					Changes.SP010_Group_ID
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
			} else if (displayType.equals(Changes.SP010_DisplayType_Resource)) {
				builder.setType(
					DefinitionType.RESOURCE
				);
				int validFromColumnId = record.get_ValueAsInt(
					Changes.SP010_ValidFrom_ID
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
					Changes.SP010_ValidTo_ID
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
			} else if (displayType.equals(Changes.SP010_DisplayType_Timeline)) {
				builder.setType(
					DefinitionType.TIMELINE
				);
				int dateColumnId = record.get_ValueAsInt(
					Changes.SP010_Date_ID
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
			} else if (displayType.equals(Changes.SP010_DisplayType_Workflow)) {
				builder.setType(
					DefinitionType.WORKFLOW
				);
				int groupColumnId = record.get_ValueAsInt(
					Changes.SP010_Group_ID
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
			}
		}
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
					Changes.SP010_DisplayDefinition_ID
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
			.setIsMandatory(
				isMandatory
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
			.setIsDisplayedGrid(
				fieldDefinitionItem.get_ValueAsBoolean(
					I_AD_Column.COLUMNNAME_SeqNo
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
		;

		final int fieldGroupId = fieldDefinitionItem.get_ValueAsInt(
			I_AD_Field.COLUMNNAME_AD_FieldGroup_ID
		);
		if (fieldGroupId > 0) {
			FieldGroup.Builder fieldGroupBuilder = convertFieldGroup(fieldGroupId);
			builder.setFieldGroup(fieldGroupBuilder);
		}

		return builder;
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
			Struct.Builder fieldValue = Struct.newBuilder();
			fieldValue.putFields("value", ValueManager.getValueFromObject(field.getValue().getValue()).build());
			if(!Util.isEmpty(field.getValue().getDisplayValue())) {
				fieldValue.putFields("display_value", ValueManager.getValueFromObject(field.getValue().getDisplayValue()).build());
				fieldValue.putFields("table_name", ValueManager.getValueFromObject(field.getValue().getTableName()).build());
			}
			fields.putFields(StringManager.getValidString(field.getValue().getColumnName()), Value.newBuilder().setStructValue(fieldValue.build()).build());
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
			Struct.Builder fieldValue = Struct.newBuilder();
			fieldValue.putFields("value", ValueManager.getValueFromObject(field.getValue().getValue()).build());
			if(!Util.isEmpty(field.getValue().getDisplayValue())) {
				fieldValue.putFields("display_value", ValueManager.getValueFromObject(field.getValue().getDisplayValue()).build());
				fieldValue.putFields("table_name", ValueManager.getValueFromObject(field.getValue().getTableName()).build());
			}
			fields.putFields(StringManager.getValidString(field.getValue().getColumnName()), Value.newBuilder().setStructValue(fieldValue.build()).build());
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
			Struct.Builder fieldValue = Struct.newBuilder();
			fieldValue.putFields("value", ValueManager.getValueFromObject(field.getValue().getValue()).build());
			if(!Util.isEmpty(field.getValue().getDisplayValue())) {
				fieldValue.putFields("display_value", ValueManager.getValueFromObject(field.getValue().getDisplayValue()).build());
				fieldValue.putFields("table_name", ValueManager.getValueFromObject(field.getValue().getTableName()).build());
			}
			fields.putFields(StringManager.getValidString(field.getValue().getColumnName()), Value.newBuilder().setStructValue(fieldValue.build()).build());
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
			Struct.Builder fieldValue = Struct.newBuilder();
			fieldValue.putFields("value", ValueManager.getValueFromObject(field.getValue().getValue()).build());
			if(!Util.isEmpty(field.getValue().getDisplayValue())) {
				fieldValue.putFields("display_value", ValueManager.getValueFromObject(field.getValue().getDisplayValue()).build());
				fieldValue.putFields("table_name", ValueManager.getValueFromObject(field.getValue().getTableName()).build());
			}
			fields.putFields(StringManager.getValidString(field.getValue().getColumnName()), Value.newBuilder().setStructValue(fieldValue.build()).build());
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
			Struct.Builder fieldValue = Struct.newBuilder();
			fieldValue.putFields("value", ValueManager.getValueFromObject(field.getValue().getValue()).build());
			if(!Util.isEmpty(field.getValue().getDisplayValue())) {
				fieldValue.putFields("display_value", ValueManager.getValueFromObject(field.getValue().getDisplayValue()).build());
				fieldValue.putFields("table_name", ValueManager.getValueFromObject(field.getValue().getTableName()).build());
			}
			fields.putFields(StringManager.getValidString(field.getValue().getColumnName()), Value.newBuilder().setStructValue(fieldValue.build()).build());
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
		MTable fieldTable = MTable.get(Env.getCtx(), Changes.SP010_Field);
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
		builder.setFields(
			additionalFields
		);

		return builder;
	}

}
