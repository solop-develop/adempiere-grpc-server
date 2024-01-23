/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.form.task_management;

import java.sql.Timestamp;
import java.text.ParseException;

import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_S_ResourceAssignment;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.backend.grpc.form.task_management.ListTasksRequest;
import org.spin.backend.grpc.form.task_management.ListTasksResponse;
import org.spin.backend.grpc.form.task_management.Task;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class TaskManagemetServiceLogic {

	public static ListTasksResponse.Builder listTasks(ListTasksRequest request) throws ParseException {
		Timestamp date = new Timestamp(System.currentTimeMillis());
		if(request.getDate() != null && (request.getDate().getSeconds() > 0 || request.getDate().getNanos() > 0)) {
			date = ValueManager.getDateFromTimestampDate(
				request.getDate()
			);
		}
		Timestamp startDate = TimeUtil.getMonthFirstDay(date);
		Timestamp endDate = TimeUtil.getMonthLastDay(date);
		endDate = RecordUtil.getDayLastTime(endDate);


		ListTasksResponse.Builder listBuilder = ListTasksResponse.newBuilder();
		int recordCount = 0;

		// Request
		if (request.getIsWithRequests()) {
			final String whereClause =
				"(DateStartPlan BETWEEN ? AND ? "
				+ "OR DateCompletePlan BETWEEN ? AND ?)"
			;
			Query requestQuery = new Query(
				Env.getCtx(),
				I_R_Request.Table_Name,
				whereClause,
				null
			)
				.setParameters(startDate, endDate, startDate, endDate)
				.setClient_ID()
			;
			recordCount += requestQuery.count();

			requestQuery.getIDsAsList().parallelStream().forEach(requestId -> {
				Task.Builder taskBuilder = TaskManagementConvertUtil.convertTaskByRequest(requestId);
				listBuilder.addTasks(taskBuilder);
			});
		}


		// Resource Assignment
		if (request.getIsWithResourceAssignments()) {
			final String whereClause =
				"(AssignDateFrom BETWEEN ? AND ? "
				+ "OR AssignDateTo BETWEEN ? AND ?)"
			;
			Query resourceAssignmentQuery = new Query(
				Env.getCtx(),
				I_S_ResourceAssignment.Table_Name,
				whereClause,
				null
			)
				.setParameters(startDate, endDate, startDate, endDate)
				.setClient_ID()
			;
			recordCount += resourceAssignmentQuery.count();

			resourceAssignmentQuery.getIDsAsList().parallelStream().forEach(resourceAssignmentId -> {
				Task.Builder taskBuilder = TaskManagementConvertUtil.convertTaskByResourceAssignment(resourceAssignmentId);
				listBuilder.addTasks(taskBuilder);
			});
		}

		// Project
		if (request.getIsWithProjects()) {
			final String whereClause = 
				"(DateStartSchedule BETWEEN ? AND ? "
				+ "OR DateFinishSchedule BETWEEN ? AND ?)"
			;
			Query projectQuery = new Query(
				Env.getCtx(),
				I_C_Project.Table_Name,
				whereClause,
				null
			)
				.setParameters(startDate, endDate, startDate, endDate)
				.setClient_ID()
			;
			recordCount += projectQuery.count();

			projectQuery.getIDsAsList().stream().forEach(projectId -> {
				Task.Builder taskBuilder = TaskManagementConvertUtil.convertTaskByProject(projectId);
				listBuilder.addTasks(taskBuilder);
			});
		}

		listBuilder.setRecordCount(recordCount);

		return listBuilder;
	} 
}
