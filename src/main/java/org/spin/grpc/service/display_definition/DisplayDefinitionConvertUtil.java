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
import org.spin.backend.grpc.display_definition.CalendarEntry;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.DefinitionType;
import org.spin.backend.grpc.display_definition.KanbanEntry;
import org.spin.backend.grpc.display_definition.KanbanStep;
import org.spin.backend.grpc.display_definition.ResourceEntry;
import org.spin.backend.grpc.display_definition.TimelineEntry;
import org.spin.backend.grpc.display_definition.WorkflowEntry;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.solop.sp010.data.calendar.CalendarItem;
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
					DefinitionType.TIMERLINE
				);
				int dateColumnId = record.get_ValueAsInt(
					Changes.SP010_Group_ID
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

	public static WorkflowEntry.Builder convertWorkflowEntry(WorkflowItem kanbanItem) {
		WorkflowEntry.Builder builder = WorkflowEntry.newBuilder();
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
		return builder;
	}

}
