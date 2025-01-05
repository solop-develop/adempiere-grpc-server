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

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.display_definition.Calendar;
import org.spin.backend.grpc.display_definition.CalendarMetadata;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataResponse;
import org.spin.backend.grpc.display_definition.KanbanMetadata;
import org.spin.backend.grpc.display_definition.ListCalendarsRequest;
import org.spin.backend.grpc.display_definition.ListCalendarsResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionResponse;
import org.spin.backend.grpc.display_definition.ResourceMetadata;
import org.spin.backend.grpc.display_definition.TimelineMetadata;
import org.spin.backend.grpc.display_definition.WorkflowData;
import org.spin.backend.grpc.display_definition.WorkflowMetadata;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

import com.solop.sp010.data.CalendarData;
import com.solop.sp010.data.KanbanData;
import com.solop.sp010.query.CalendarQuery;
import com.solop.sp010.query.KanbanQuery;
import com.solop.sp010.util.Changes;

// import com.solop.sp010.util.Changes;

public class DisplayDefinitionServiceLogic {

	public static ExistsDisplayDefinitionMetadataResponse.Builder existsDisplayDefinitionsMetadata(ExistsDisplayDefinitionMetadataRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		MTable displayDefinitionTable = MTable.get(
			Env.getCtx(),
			Changes.SP010_DisplayDefinition
		);
		int recordCount = 0;
		if (displayDefinitionTable != null && displayDefinitionTable.getAD_Table_ID() > 0) {
			recordCount = new Query(
				Env.getCtx(),
				displayDefinitionTable,
				"AD_Table_ID = ?",
				null
			)
				.setParameters(table.getAD_Table_ID())
				.count()
			;
		}
		ExistsDisplayDefinitionMetadataResponse.Builder builder = ExistsDisplayDefinitionMetadataResponse.newBuilder()
			.setRecordCount(recordCount)
		;
		return builder;
	}



	public static ListDisplayDefinitionsMetadataResponse.Builder listDisplayDefinitionsMetadata(ListDisplayDefinitionsMetadataRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		MTable displayDefinitionTable = MTable.get(
			Env.getCtx(),
			Changes.SP010_DisplayDefinition
		);

		ListDisplayDefinitionsMetadataResponse.Builder builderList = ListDisplayDefinitionsMetadataResponse.newBuilder();
		if (displayDefinitionTable != null && displayDefinitionTable.getAD_Table_ID() > 0) {
			Query query = new Query(
				Env.getCtx(),
				displayDefinitionTable,
				"AD_Table_ID = ?",
				null
			)
				.setParameters(table.getAD_Table_ID())
			;

			//	Get page and count
			String nexPageToken = null;
			int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
			int limit = LimitUtil.getPageSize(request.getPageSize());
			int offset = (pageNumber - 1) * limit;
			int recordCount = query.count();

			builderList
				.setRecordCount(recordCount)
				.setNextPageToken(
					StringManager.getValidString(nexPageToken)
				)
			;

			//	Get List
			query.setLimit(limit, offset)
				.<PO>list()
				.forEach(record -> {
					DefinitionMetadata.Builder builder = DefinitionMetadata.newBuilder();
					String displayType = record.get_ValueAsString(Changes.SP010_DisplayType);
					if (Changes.SP010_DisplayType_Calendar.equals(displayType)) {
						CalendarMetadata.Builder calendarBuilder = DisplayDefinitionConvertUtil.convertCalendarMetadata(record);
						builder.setCalendarMetadata(calendarBuilder);
					} else if (Changes.SP010_DisplayType_Kanban.equals(displayType)) {
						KanbanMetadata.Builder kanbanBuilder = DisplayDefinitionConvertUtil.convertKanbanMetadata(record);
						builder.setKanbanMetadata(kanbanBuilder);
					} else if (Changes.SP010_DisplayType_Resource.equals(displayType)) {
						ResourceMetadata.Builder resourceBuilder = DisplayDefinitionConvertUtil.convertResourceMetadata(record);
						builder.setResourceMetadata(resourceBuilder);
					} else if (Changes.SP010_DisplayType_Timeline.equals(displayType)) {
						TimelineMetadata.Builder timelineMetadata = DisplayDefinitionConvertUtil.convertTimelineMetadata(record);
						builder.setTimelineMetadata(timelineMetadata);
					} else if (Changes.SP010_DisplayType_Workflow.equals(displayType)) {
						WorkflowMetadata.Builder workflowMetadata = DisplayDefinitionConvertUtil.convertWorkflowMetadata(record);
						builder.setWorkflowMetadata(workflowMetadata);
					}
					builderList.addRecords(builder);
				})
			;
		}

		return builderList;
	}



	public static ListCalendarsResponse.Builder listCalendars(ListCalendarsRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @SP010_DisplayDefinition_ID@");
		}

		PO displayDefinition = new Query(
			Env.getCtx(),
			Changes.SP010_DisplayDefinition,
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(request.getId())
			.first()
		;
		if (displayDefinition == null || displayDefinition.get_ID() <= 0) {
			throw new AdempiereException("@SP010_DisplayDefinition_ID@ @NotFound@");
		}
		String displayType = displayDefinition.get_ValueAsString(Changes.SP010_DisplayType);
		if (!Changes.SP010_DisplayType_Calendar.equals(displayType)) {
			throw new AdempiereException("@SP010_DisplayType@ @C@ @NotFound@");
		}

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> conditions = FilterManager.newInstance(request.getFilters()).getConditions();
		CalendarData displayData = (CalendarData) new CalendarQuery(request.getId())
			.withConditions(conditions)
			.withLimit(limit, offset)
			.run()
		;

		//	Set page token
		int count = NumberManager.getIntegerFromLong(
			displayData.getRecordCount()
		);
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCalendarsResponse.Builder builderList = ListCalendarsResponse.newBuilder()
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		displayData.getCalendars().forEach(calendarItem -> {
			Calendar.Builder builder = DisplayDefinitionConvertUtil.convertCalentar(calendarItem);
			builderList.addRecords(builder);
		});
		return builderList;
	}



	public static ListWorkflowsDefinitionResponse.Builder listWorkflowsDefinition(ListWorkflowsDefinitionRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @SP010_DisplayDefinition_ID@");
		}

		PO displayDefinition = new Query(
			Env.getCtx(),
			Changes.SP010_DisplayDefinition,
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(request.getId())
			.first()
		;
		if (displayDefinition == null || displayDefinition.get_ID() <= 0) {
			throw new AdempiereException("@SP010_DisplayDefinition_ID@ @NotFound@");
		}
		String displayType = displayDefinition.get_ValueAsString(Changes.SP010_DisplayType);
		if (!Changes.SP010_DisplayType_Kanban.equals(displayType) && !Changes.SP010_DisplayType_Workflow.equals(displayType)) {
			throw new AdempiereException("@SP010_DisplayType@ @K@/@W@ @NotFound@");
		}

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> conditions = FilterManager.newInstance(request.getFilters()).getConditions();
		KanbanData displayData = (KanbanData) new KanbanQuery(request.getId())
			.withConditions(conditions)
			.withLimit(limit, offset)
			.run()
		;

		ListWorkflowsDefinitionResponse.Builder builderList = ListWorkflowsDefinitionResponse.newBuilder()
			.setName(
				StringManager.getValidString(
					displayData.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					displayData.getDescription()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					displayData.getColumnName()
				)
			)
		;

		displayData.getColumns().forEach(kanbanColumn -> {
			WorkflowStep.Builder builder = DisplayDefinitionConvertUtil.convertWorkflowStep(kanbanColumn);
			builderList.addSteps(builder);
		});
		return builderList;
	}



	public static ListWorkflowsDataResponse.Builder listWorkflowsData(ListWorkflowsDataRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @SP010_DisplayDefinition_ID@");
		}

		PO displayDefinition = new Query(
			Env.getCtx(),
			Changes.SP010_DisplayDefinition,
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(request.getId())
			.first()
		;
		if (displayDefinition == null || displayDefinition.get_ID() <= 0) {
			throw new AdempiereException("@SP010_DisplayDefinition_ID@ @NotFound@");
		}
		String displayType = displayDefinition.get_ValueAsString(Changes.SP010_DisplayType);
		if (!Changes.SP010_DisplayType_Kanban.equals(displayType) && !Changes.SP010_DisplayType_Workflow.equals(displayType)) {
			throw new AdempiereException("@SP010_DisplayType@ @K@/@W@ @NotFound@");
		}

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> conditions = FilterManager.newInstance(request.getFilters()).getConditions();
		KanbanData displayData = (KanbanData) new KanbanQuery(request.getId())
			.withConditions(conditions)
			.withLimit(limit, offset)
			.run()
		;

		//	Set page token
		int count = NumberManager.getIntegerFromLong(
			displayData.getRecordCount()
		);
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListWorkflowsDataResponse.Builder builderList = ListWorkflowsDataResponse.newBuilder()
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		displayData.getColumns().forEach(kanbanColumn -> {
			WorkflowStep.Builder builder = DisplayDefinitionConvertUtil.convertWorkflowStep(kanbanColumn);
			builderList.addSteps(builder);
		});
		displayData.getKanbans().forEach(kanbanItem -> {
			WorkflowData.Builder builder = DisplayDefinitionConvertUtil.convertWorkflowData(kanbanItem);
			builderList.addRecords(builder);
		});
		return builderList;
	}

}
