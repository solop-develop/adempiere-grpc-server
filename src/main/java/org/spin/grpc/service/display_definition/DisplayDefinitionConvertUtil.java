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

import org.adempiere.core.domains.models.I_AD_Element;
import org.compiere.model.MColumn;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.display_definition.Calendar;
import org.spin.backend.grpc.display_definition.CalendarMetadata;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.DefinitionType;
import org.spin.backend.grpc.display_definition.KanbanMetadata;
import org.spin.backend.grpc.display_definition.ResourceMetadata;
import org.spin.backend.grpc.display_definition.TimelineMetadata;
import org.spin.backend.grpc.display_definition.WorkflowData;
import org.spin.backend.grpc.display_definition.WorkflowMetadata;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.solop.sp010.data.CalendarItem;
import com.solop.sp010.data.KanbanColumn;
import com.solop.sp010.data.KanbanItem;
import com.solop.sp010.util.Changes;

public class DisplayDefinitionConvertUtil {
	

	public static DefinitionMetadata.Builder convertDefinitionMetadata(PO record) {
		DefinitionMetadata.Builder builder = DefinitionMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
			} else if (displayType.equals(Changes.SP010_DisplayType_Kanban)) {
				builder.setType(
					DefinitionType.KANBAN
				);
			} else if (displayType.equals(Changes.SP010_DisplayType_Resource)) {
				builder.setType(
					DefinitionType.RESOURCE
				);
			} else if (displayType.equals(Changes.SP010_DisplayType_Timeline)) {
				builder.setType(
					DefinitionType.TIMERLINE
				);
			} else if (displayType.equals(Changes.SP010_DisplayType_Workflow)) {
				builder.setType(
					DefinitionType.WORKFLOW
				);
			}
		}
		return builder;
	}

	public static CalendarMetadata.Builder convertCalendarMetadata(PO record) {
		CalendarMetadata.Builder builder = CalendarMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
		;
		// title column
		MColumn titleColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Title_ID
			)
		);
		if (titleColumn != null && titleColumn.get_ID() > 0) {
			builder.setTitleColumn(
				StringManager.getValidString(
					titleColumn.getColumnName()
				)
			);
		}
		// description column
		MColumn descriptionColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Description_ID
			)
		);
		if (descriptionColumn != null && descriptionColumn.get_ID() > 0) {
			builder.setDescriptionColumn(
				StringManager.getValidString(
					descriptionColumn.getColumnName()
				)
			);
		}
		// valid from column
		MColumn validFromColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_ValidFrom_ID
			)
		);
		if (validFromColumn != null && validFromColumn.get_ID() > 0) {
			builder.setValidFromColumn(
				StringManager.getValidString(
					validFromColumn.getColumnName()
				)
			);
		}
		// valid to column
		MColumn validToColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_ValidTo_ID
			)
		);
		if (validToColumn != null && validToColumn.get_ID() > 0) {
			builder.setValidToColumn(
				StringManager.getValidString(
					validToColumn.getColumnName()
				)
			);
		}
		return builder;
	}

	public static Calendar.Builder convertCalentar(CalendarItem calendarItem) {
		Calendar.Builder builder = Calendar.newBuilder();
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
			.setConfirmed(
				calendarItem.isConfirmed()
			)
		;

		return builder;
	}


	public static KanbanMetadata.Builder convertKanbanMetadata(PO record) {
		KanbanMetadata.Builder builder = KanbanMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
		;
		// title column
		MColumn titleColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Title_ID
			)
		);
		if (titleColumn != null && titleColumn.get_ID() > 0) {
			builder.setTitleColumn(
				StringManager.getValidString(
					titleColumn.getColumnName()
				)
			);
		}
		// description column
		MColumn descriptionColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Description_ID
			)
		);
		if (descriptionColumn != null && descriptionColumn.get_ID() > 0) {
			builder.setDescriptionColumn(
				StringManager.getValidString(
					descriptionColumn.getColumnName()
				)
			);
		}
		// group column
		MColumn groupColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Group_ID
			)
		);
		if (groupColumn != null && groupColumn.get_ID() > 0) {
			builder.setGroupColumn(
				StringManager.getValidString(
					groupColumn.getColumnName()
				)
			);
		}
		// group sequence column
		MColumn groupSequenceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_GroupSequence_ID
			)
		);
		if (groupSequenceColumn != null && groupSequenceColumn.get_ID() > 0) {
			builder.setGroupSequenceColumn(
				StringManager.getValidString(
					groupSequenceColumn.getColumnName()
				)
			);
		}
		// sequence column
		MColumn sequenceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Sequence_ID
			)
		);
		if (sequenceColumn != null && sequenceColumn.get_ID() > 0) {
			builder.setSequenceColumn(
				StringManager.getValidString(
					sequenceColumn.getColumnName()
				)
			);
		}
		return builder;
	}


	public static ResourceMetadata.Builder convertResourceMetadata(PO record) {
		ResourceMetadata.Builder builder = ResourceMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
		;
		// title column
		MColumn titleColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Title_ID
			)
		);
		if (titleColumn != null && titleColumn.get_ID() > 0) {
			builder.setTitleColumn(
				StringManager.getValidString(
					titleColumn.getColumnName()
				)
			);
		}
		// description column
		MColumn descriptionColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Description_ID
			)
		);
		if (descriptionColumn != null && descriptionColumn.get_ID() > 0) {
			builder.setDescriptionColumn(
				StringManager.getValidString(
					descriptionColumn.getColumnName()
				)
			);
		}
		// valid from column
		MColumn validFromColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_ValidFrom_ID
			)
		);
		if (validFromColumn != null && validFromColumn.get_ID() > 0) {
			builder.setValidFromColumn(
				StringManager.getValidString(
					validFromColumn.getColumnName()
				)
			);
		}
		// valid to column
		MColumn validToColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_ValidTo_ID
			)
		);
		if (validToColumn != null && validToColumn.get_ID() > 0) {
			builder.setValidToColumn(
				StringManager.getValidString(
					validToColumn.getColumnName()
				)
			);
		}
		// is resource column
		MColumn isResourceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_IsResource
			)
		);
		if (isResourceColumn != null && isResourceColumn.get_ID() > 0) {
			builder.setIsResourceReference(
				record.get_ValueAsBoolean(
					isResourceColumn.getColumnName()
				)
			);
		}
		// resource column
		MColumn resourceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Resource_ID
			)
		);
		if (resourceColumn != null && resourceColumn.get_ID() > 0) {
			builder.setResourceColumn(
				StringManager.getValidString(
					resourceColumn.getColumnName()
				)
			);
		}
		// resource item column
		MColumn resourceItemColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Item_ID
			)
		);
		if (resourceItemColumn != null && resourceItemColumn.get_ID() > 0) {
			builder.setResourceItemColumn(
				StringManager.getValidString(
					resourceItemColumn.getColumnName()
				)
			);
		}
		// resource item group column
		MColumn resourceItemGroupColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Item_ID
			)
		);
		if (resourceItemGroupColumn != null && resourceItemGroupColumn.get_ID() > 0) {
			builder.setResourceItemGroupColumn(
				StringManager.getValidString(
					resourceItemGroupColumn.getColumnName()
				)
			);
		}
		return builder;
	}


	public static TimelineMetadata.Builder convertTimelineMetadata(PO record) {
		TimelineMetadata.Builder builder = TimelineMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
		;
		// title column
		MColumn titleColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Title_ID
			)
		);
		if (titleColumn != null && titleColumn.get_ID() > 0) {
			builder.setTitleColumn(
				StringManager.getValidString(
					titleColumn.getColumnName()
				)
			);
		}
		// description column
		MColumn descriptionColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Description_ID
			)
		);
		if (descriptionColumn != null && descriptionColumn.get_ID() > 0) {
			builder.setDescriptionColumn(
				StringManager.getValidString(
					descriptionColumn.getColumnName()
				)
			);
		}
		// date column
		MColumn dateColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_ValidFrom_ID
			)
		);
		if (dateColumn != null && dateColumn.get_ID() > 0) {
			builder.setDateColumn(
				StringManager.getValidString(
					dateColumn.getColumnName()
				)
			);
		}
		return builder;
	}


	public static WorkflowStep.Builder convertWorkflowStep(KanbanColumn kanbanColumn) {
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


	public static WorkflowData.Builder convertWorkflowData(KanbanItem kanbanItem) {
		WorkflowData.Builder builder = WorkflowData.newBuilder();
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
			.setGroupId(
				StringManager.getValidString(
					kanbanItem.getGroupCode()
				)
			)
		;
		return builder;
	}


	public static WorkflowMetadata.Builder convertWorkflowMetadata(PO record) {
		WorkflowMetadata.Builder builder = WorkflowMetadata.newBuilder();
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}
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
		;
		// title column
		MColumn titleColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Title_ID
			)
		);
		if (titleColumn != null && titleColumn.get_ID() > 0) {
			builder.setTitleColumn(
				StringManager.getValidString(
					titleColumn.getColumnName()
				)
			);
		}
		// description column
		MColumn descriptionColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Description_ID
			)
		);
		if (descriptionColumn != null && descriptionColumn.get_ID() > 0) {
			builder.setDescriptionColumn(
				StringManager.getValidString(
					descriptionColumn.getColumnName()
				)
			);
		}
		// group column
		MColumn groupColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Group_ID
			)
		);
		if (groupColumn != null && groupColumn.get_ID() > 0) {
			builder.setGroupColumn(
				StringManager.getValidString(
					groupColumn.getColumnName()
				)
			);
		}
		// group sequence column
		MColumn groupSequenceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_GroupSequence_ID
			)
		);
		if (groupSequenceColumn != null && groupSequenceColumn.get_ID() > 0) {
			builder.setGroupSequenceColumn(
				StringManager.getValidString(
					groupSequenceColumn.getColumnName()
				)
			);
		}
		// sequence column
		MColumn sequenceColumn = MColumn.get(
			Env.getCtx(),
			record.get_ValueAsInt(
				Changes.SP010_Sequence_ID
			)
		);
		if (sequenceColumn != null && sequenceColumn.get_ID() > 0) {
			builder.setSequenceColumn(
				StringManager.getValidString(
					sequenceColumn.getColumnName()
				)
			);
		}
		return builder;
	}

}
