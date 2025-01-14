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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.display_definition.CalendarEntry;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataResponse;
import org.spin.backend.grpc.display_definition.KanbanEntry;
import org.spin.backend.grpc.display_definition.KanbanStep;
import org.spin.backend.grpc.display_definition.ListCalendarsDataRequest;
import org.spin.backend.grpc.display_definition.ListCalendarsDataResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataResponse;
import org.spin.backend.grpc.display_definition.ListKanbansDataRequest;
import org.spin.backend.grpc.display_definition.ListKanbansDataResponse;
import org.spin.backend.grpc.display_definition.ListKanbansDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListKanbansDefinitionResponse;
import org.spin.backend.grpc.display_definition.ListResourcesDataRequest;
import org.spin.backend.grpc.display_definition.ListResourcesDataResponse;
import org.spin.backend.grpc.display_definition.ListTimelinesDataRequest;
import org.spin.backend.grpc.display_definition.ListTimelinesDataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionResponse;
import org.spin.backend.grpc.display_definition.ResourceEntry;
import org.spin.backend.grpc.display_definition.ResourceGroup;
import org.spin.backend.grpc.display_definition.ResourceGroupChild;
import org.spin.backend.grpc.display_definition.TimelineEntry;
import org.spin.backend.grpc.display_definition.WorkflowEntry;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;import com.solop.sp010.controller.DisplayBuilder;
import com.solop.sp010.data.calendar.CalendarData;
import com.solop.sp010.data.kanban.KanbanData;
import com.solop.sp010.data.resource.ResourceData;
import com.solop.sp010.data.resource.ResourceItem;
import com.solop.sp010.data.timeline.TimeLineData;
import com.solop.sp010.data.workflow.WorkflowData;
import com.solop.sp010.query.Kanban;
import com.solop.sp010.query.Workflow;
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
		if (displayDefinitionTable == null || displayDefinitionTable.getAD_Table_ID() <= 0) {
			return builderList;
		}

		String whereClause = "AD_Table_ID = ?";
		List<Object> parametersList = new ArrayList<>();
		parametersList.add(
			table.getAD_Table_ID()
		);

		Query query = new Query(
			Env.getCtx(),
			displayDefinitionTable,
			whereClause,
			null
		)
			.setParameters(parametersList)
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
				DefinitionMetadata.Builder builder = DisplayDefinitionConvertUtil.convertDefinitionMetadata(record);

				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListCalendarsDataResponse.Builder listCalendarsData(ListCalendarsDataRequest request) {
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

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		CalendarData displayData = (CalendarData) DisplayBuilder.newInstance(request.getId())
			.withFilters(filtersList)
			.withLimit(limit)
			.withOffset(offset)
			.withOrderBy(
				request.getSortBy()
			)
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

		ListCalendarsDataResponse.Builder builderList = ListCalendarsDataResponse.newBuilder()
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		displayData.getCalendars().forEach(calendarItem -> {
			CalendarEntry.Builder builder = DisplayDefinitionConvertUtil.convertCalentarEntry(calendarItem);
			builderList.addRecords(builder);
		});
		return builderList;
	}



	public static ListKanbansDefinitionResponse.Builder listKanbansDefinition(ListKanbansDefinitionRequest request) {
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

		Kanban kanbanDefinition = new Kanban(request.getId());
		ListKanbansDefinitionResponse.Builder builderList = ListKanbansDefinitionResponse.newBuilder()
			.setName(
				StringManager.getValidString(
					kanbanDefinition.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					kanbanDefinition.getDescription()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					kanbanDefinition.getColumnName(
						Changes.SP010_Group_ID
					)
				)
			)
		;

		kanbanDefinition.getColumns().forEach(kanbanColumn -> {
			KanbanStep.Builder builder = DisplayDefinitionConvertUtil.convertKanbanStep(kanbanColumn);
			builderList.addSteps(builder);
		});
		return builderList;
	}

	public static ListKanbansDataResponse.Builder listKanbansData(ListKanbansDataRequest request) {
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

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		KanbanData displayData = (KanbanData) DisplayBuilder.newInstance(request.getId())
			.withFilters(filtersList)
			.withLimit(limit)
			.withOffset(offset)
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

		ListKanbansDataResponse.Builder builderList = ListKanbansDataResponse.newBuilder()
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
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		displayData.getColumns().forEach(kanbanColumn -> {
			KanbanStep.Builder builder = DisplayDefinitionConvertUtil.convertKanbanStep(kanbanColumn);
			builderList.addSteps(builder);
		});
		displayData.getKanbans().forEach(kanbanItem -> {
			KanbanEntry.Builder builder = DisplayDefinitionConvertUtil.convertKanbanEntry(kanbanItem);
			builderList.addRecords(builder);
		});
		return builderList;
	}



	@SuppressWarnings("unlikely-arg-type")
	public static ListResourcesDataResponse.Builder listResourcesData(ListResourcesDataRequest request) {
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

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		ResourceData displayData = (ResourceData) DisplayBuilder.newInstance(request.getId())
			.withFilters(filtersList)
			.withLimit(limit)
			.withOffset(offset)
			.withOrderBy(
				request.getSortBy()
			)
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

		ListResourcesDataResponse.Builder builderList = ListResourcesDataResponse.newBuilder()
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;
		Map<String, List<ResourceItem>> resourcesGroup = new HashMap<>();
		displayData.getResources().forEach(resourceItem -> {
			ResourceEntry.Builder builder = DisplayDefinitionConvertUtil.convertResourceEntry(resourceItem);
			builderList.addRecords(builder);
			String validGroupName = Optional.ofNullable(resourceItem.getGroupName()).orElse("");
			List<ResourceItem> resources = new ArrayList<>();
			if(resourcesGroup.containsValue(validGroupName)) {
				resources = resourcesGroup.get(validGroupName);
			} else {
				resources.add(resourceItem);
			}
			resourcesGroup.put(validGroupName, resources);
		});
		resourcesGroup.entrySet().forEach(entry -> {
			ResourceGroup.Builder group = ResourceGroup.newBuilder()
					.setName(StringManager.getValidString(entry.getKey()))
					;
			entry.getValue().forEach(resource -> {
				ResourceGroupChild.Builder child = ResourceGroupChild.newBuilder()
						.setId(resource.getId())
						.setUuid(StringManager.getValidString(resource.getUuid()))
						.setName(StringManager.getValidString(resource.getName()))
						;
				group.addResources(child);
			});
			builderList.addGroups(group);
		});
		return builderList;
	}

	public static ListTimelinesDataResponse.Builder listTimelinesData(ListTimelinesDataRequest request) {
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

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		TimeLineData displayData = (TimeLineData) DisplayBuilder.newInstance(request.getId())
			.withFilters(filtersList)
			.withLimit(limit)
			.withOffset(offset)
			.withOrderBy(
				request.getSortBy()
			)
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

		ListTimelinesDataResponse.Builder builderList = ListTimelinesDataResponse.newBuilder()
			.setRecordCount(
				count
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		displayData.getTimeLines().forEach(kanbanItem -> {
			TimelineEntry.Builder builder = DisplayDefinitionConvertUtil.convertTimelineEntry(kanbanItem);
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

		Workflow worflowDefinition = new Workflow(request.getId());

		ListWorkflowsDefinitionResponse.Builder builderList = ListWorkflowsDefinitionResponse.newBuilder()
			.setName(
				StringManager.getValidString(
					worflowDefinition.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					worflowDefinition.getDescription()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					worflowDefinition.getColumnName(
						Changes.SP010_Group_ID
					)
				)
			)
		;

		worflowDefinition.getColumns().forEach(kanbanColumn -> {
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

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		WorkflowData displayData = (WorkflowData) DisplayBuilder.newInstance(request.getId())
			.withFilters(filtersList)
			.withLimit(limit)
			.withOffset(offset)
			.withOrderBy(
				request.getSortBy()
			)
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
		displayData.getWorkflows().forEach(kanbanItem -> {
			WorkflowEntry.Builder builder = DisplayDefinitionConvertUtil.convertWorkflowEntry(kanbanItem);
			builderList.addRecords(builder);
		});
		return builderList;
	}

}
