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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POAdapter;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.display_definition.CalendarEntry;
import org.spin.backend.grpc.display_definition.CreateDataEntryRequest;
import org.spin.backend.grpc.display_definition.DataEntry;
import org.spin.backend.grpc.display_definition.DefinitionMetadata;
import org.spin.backend.grpc.display_definition.DeleteDataEntryRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataResponse;
import org.spin.backend.grpc.display_definition.FieldDefinition;
import org.spin.backend.grpc.display_definition.KanbanEntry;
import org.spin.backend.grpc.display_definition.KanbanStep;
import org.spin.backend.grpc.display_definition.ListCalendarsDataRequest;
import org.spin.backend.grpc.display_definition.ListCalendarsDataResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionFieldsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionFieldsMetadataResponse;
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
import org.spin.backend.grpc.display_definition.ReadDataEntryRequest;
import org.spin.backend.grpc.display_definition.ResourceEntry;
import org.spin.backend.grpc.display_definition.ResourceGroup;
import org.spin.backend.grpc.display_definition.ResourceGroupChild;
import org.spin.backend.grpc.display_definition.TimelineEntry;
import org.spin.backend.grpc.display_definition.UpdateDataEntryRequest;
import org.spin.backend.grpc.display_definition.WorkflowEntry;
import org.spin.backend.grpc.display_definition.WorkflowStep;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;
import com.google.protobuf.Value;
import com.solop.sp010.controller.DisplayBuilder;
import com.solop.sp010.data.calendar.CalendarData;
import com.solop.sp010.data.generic.GenericItem;
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

	public static PO validateAndGetDisplayDefinition(int displayDefinitionId) {
			if (displayDefinitionId <= 0) {
				throw new AdempiereException("@FillMandatory@ @SP010_DisplayDefinition_ID@");
			}
			PO displayDefinition = new Query(
				Env.getCtx(),
				Changes.SP010_DisplayDefinition,
				"SP010_DisplayDefinition_ID = ?",
				null
			)
				.setParameters(displayDefinitionId)
				.first()
			;
			if (displayDefinition == null || displayDefinition.get_ID() <= 0) {
				throw new AdempiereException("@SP010_DisplayDefinition_ID@ @NotFound@");
			}
			return displayDefinition;
		}
	
	
	
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
		String whereclause = "AD_Table_ID = ? AND SP010_DisplayType NOT IN('T', 'W')";
		if (displayDefinitionTable != null && displayDefinitionTable.getAD_Table_ID() > 0) {
			if(request.getOnlyReferences()) {
				whereclause = "EXISTS(SELECT 1 FROM SP010_ReferenceTable r WHERE r.SP010_DisplayDefinition_ID = SP010_DisplayDefinition.SP010_DisplayDefinition_ID AND r.AD_Table_ID = ?)";
			}
			recordCount = new Query(
				Env.getCtx(),
				displayDefinitionTable,
				whereclause,
				null
			)
				.setParameters(table.getAD_Table_ID())
				.setClient_ID()
				.setOnlyActiveRecords(true)
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
		String displayTableName = Changes.SP010_DisplayDefinition;
		String whereClause = "AD_Table_ID = ? AND SP010_DisplayType NOT IN('T', 'W')";
		if(request.getOnlyReferences()) {
			displayTableName = "SP010_ReferenceTable";
			whereClause = "AD_Table_ID = ?";
		}
		List<Object> parametersList = new ArrayList<>();
		parametersList.add(
			table.getAD_Table_ID()
		);

		Query query = new Query(
			Env.getCtx(),
			displayTableName,
			whereClause,
			null
		)
			.setParameters(parametersList)
			.setClient_ID()
			.setOnlyActiveRecords(true)
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
		MTable referenceTable = MTable.get(Env.getCtx(), displayTableName);
		if(request.getOnlyReferences()) {
			query.setLimit(limit, offset)
				.getIDsAsList()
				.forEach(recordId -> {
					PO displayReference = referenceTable.getPO(recordId, null);
					PO display = displayDefinitionTable.getPO(displayReference.get_ValueAsInt(Changes.SP010_DisplayDefinition_ID), null);
					if(!Util.isEmpty(displayReference.get_ValueAsString("Name"))) {
						display.set_ValueOfColumn("Name", displayReference.get_ValueAsString("Name"));
					}
					if(!Util.isEmpty(displayReference.get_ValueAsString("Description"))) {
						display.set_ValueOfColumn("Description", displayReference.get_ValueAsString("Description"));
					}
					DefinitionMetadata.Builder builder = DisplayDefinitionConvertUtil.convertDefinitionMetadata(display);
					builderList.addRecords(builder);
				})
			;
		} else {
			query.setLimit(limit, offset)
				.getIDsAsList()
				.forEach(recordId -> {
					PO display = referenceTable.getPO(recordId, null);
					DefinitionMetadata.Builder builder = DisplayDefinitionConvertUtil.convertDefinitionMetadata(display);
					builderList.addRecords(builder);
				})
			;
		}
		return builderList;
	}


	public static ListDisplayDefinitionFieldsMetadataResponse.Builder listDisplayDefinitionFieldsMetadata(ListDisplayDefinitionFieldsMetadataRequest request) {
		if (request.getDisplayDefinitionId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @SP010_DisplayDefinition_ID@");
		}

		MTable table = RecordUtil.validateAndGetTable(
			"SP010_Field"
		);

		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getDisplayDefinitionId()
		);

		Query query = new Query(
			Env.getCtx(),
			"SP010_Field",
			"SP010_DisplayDefinition_ID = ?",
			null
		)
			.setParameters(displayDefinition.get_ID())
			.setOnlyActiveRecords(true)
		;

		ListDisplayDefinitionFieldsMetadataResponse.Builder builderList = ListDisplayDefinitionFieldsMetadataResponse.newBuilder()
			.setFieldDefinitionsCount1(
				query.count()
			)
		;
		query.setOrderBy(
				I_AD_Field.COLUMNNAME_SeqNo
			)
			.getIDsAsList()
			.forEach(fieldId -> {
				PO field = table.getPO(fieldId, null);
				FieldDefinition.Builder fieldBuilder = DisplayDefinitionConvertUtil.convertFieldDefinition(field);
				builderList.addFieldDefinitions2(
					fieldBuilder.build()
				);
			})
		;

		return builderList;
	}



	public static ListCalendarsDataResponse.Builder listCalendarsData(ListCalendarsDataRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		CalendarData displayData = (CalendarData) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
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
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		Kanban kanbanDefinition = new Kanban(displayDefinition.get_ID());
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
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		KanbanData displayData = (KanbanData) DisplayBuilder.newInstance(displayDefinition.get_ID())
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
	
	public static ListResourcesDataResponse.Builder listResourcesData(ListResourcesDataRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		ResourceData displayData = (ResourceData) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
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
			if(resourcesGroup.containsKey(validGroupName)) {
				resources = resourcesGroup.get(validGroupName);
			}
			resources.add(resourceItem);
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
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Filter> filtersList = FilterManager.newInstance(request.getFilters()).getConditions();
		TimeLineData displayData = (TimeLineData) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
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
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getId()
		);

		Workflow worflowDefinition = new Workflow(displayDefinition.get_ID());

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



	public static DataEntry.Builder createDataEntry(CreateDataEntryRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getDisplayDefinitionId()
		);

		//	Fill context
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, context, request.getContextAttributes()
		);

		MTable table = MTable.get(
			context,
			displayDefinition.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID)
		);
		PO currentEntity = table.getPO(0, null);
		if (currentEntity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		POAdapter adapter = new POAdapter(currentEntity);

		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		attributes.entrySet().forEach(attribute -> {
			final String columnName = attribute.getKey();
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			int referenceId = column.getAD_Reference_ID();
			Object value = null;
			if (!attribute.getValue().hasNullValue()) {
				if (referenceId > 0) {
					value = ValueManager.getObjectFromReference(
						attribute.getValue(),
						referenceId
					);
				} 
				if (value == null) {
					value = ValueManager.getObjectFromValue(
						attribute.getValue()
					);
				}
			}
			if (column.isMandatory() && value == null) {
				// fill value with context
				String currentValue = Env.getContext(context, windowNo, columnName, false);
				if (!Util.isEmpty(currentValue, true)) {
					value = currentValue;
				}
			}
			adapter.set_ValueNoCheck(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();

		GenericItem recordItem = (GenericItem) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
			.run(
				currentEntity.get_ID()
			)
		;
		DataEntry.Builder builder = DisplayDefinitionConvertUtil.convertDataEntry(
			displayDefinition,
			recordItem
		);

		return builder;
	}

	public static DataEntry.Builder readDataEntry(ReadDataEntryRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getDisplayDefinitionId()
		);

		MTable table = MTable.get(
			Env.getCtx(),
			displayDefinition.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID)
		);

		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}
		PO entity = table.getPO(request.getId(), null);
		if (entity == null || entity.get_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		GenericItem recordItem = (GenericItem) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
			.run(
				entity.get_ID()
			)
		;
		DataEntry.Builder builder = DisplayDefinitionConvertUtil.convertDataEntry(
			displayDefinition,
			recordItem
		);

		return builder;
	}

	public static DataEntry.Builder updateDataEntry(UpdateDataEntryRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getDisplayDefinitionId()
		);

		MTable table = MTable.get(
			Env.getCtx(),
			displayDefinition.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID)
		);
		String[] keyColumns = table.getKeyColumns();

		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}
		PO currentEntity = table.getPO(request.getId(), null);
		if (currentEntity == null || currentEntity.get_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		POAdapter adapter = new POAdapter(currentEntity);

		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		attributes.entrySet().forEach(attribute -> {
			final String columnName = attribute.getKey();
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			if (Arrays.stream(keyColumns).anyMatch(columnName::equals)) {
				// prevent warning `PO.set_Value: Column not updateable`
				return;
			}
			int referenceId = column.getAD_Reference_ID();
			Object value = null;
			if (!attribute.getValue().hasNullValue()) {
				if (referenceId > 0) {
					value = ValueManager.getObjectFromReference(
						attribute.getValue(),
						referenceId
					);
				} 
				if (value == null) {
					value = ValueManager.getObjectFromValue(
						attribute.getValue()
					);
				}
			}
			adapter.set_ValueNoCheck(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();

		GenericItem recordItem = (GenericItem) DisplayBuilder.newInstance(
				displayDefinition.get_ID()
			)
			.run(
				currentEntity.get_ID()
			)
		;
		DataEntry.Builder builder = DisplayDefinitionConvertUtil.convertDataEntry(
			displayDefinition,
			recordItem
		);

		return builder;
	}

	public static Empty.Builder deleteDataEntry(DeleteDataEntryRequest request) {
		PO displayDefinition = validateAndGetDisplayDefinition(
			request.getDisplayDefinitionId()
		);

		MTable table = MTable.get(
			Env.getCtx(),
			displayDefinition.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID)
		);

		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}
		PO entity = table.getPO(request.getId(), null);
		if (entity == null || entity.get_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		entity.saveEx();

		return Empty.newBuilder();
	}

}
