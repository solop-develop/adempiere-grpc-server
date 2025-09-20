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

import org.compiere.model.MProject;
import org.compiere.model.MRequest;
import org.compiere.model.MResourceAssignment;
import org.compiere.util.Env;
import org.spin.backend.grpc.form.task_management.Project;
import org.spin.backend.grpc.form.task_management.Request;
import org.spin.backend.grpc.form.task_management.ResourceAssignment;
import org.spin.backend.grpc.form.task_management.Task;
import org.spin.backend.grpc.form.task_management.TaskType;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class TaskManagementConvertUtil {


	public static Project.Builder convertProject(int projectId) {
		Project.Builder builder = Project.newBuilder();
		if (projectId <= 0) {
			return builder;
		}
		MProject project = MProject.getById(Env.getCtx(), projectId, null);
		return convertProject(project);
	}

	public static Project.Builder convertProject(MProject project) {
		Project.Builder builder = Project.newBuilder();
		if (project == null || project.getC_Project_ID() <= 0) {
			return builder;
		}

		builder.setId(
				project.getC_Project_ID()
			)
			.setUuid(
				StringManager.getValidString(
					project.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					project.getValue()
				)
			)
			.setDateStartSchedule(
				ValueManager.getProtoTimestampFromTimestamp(
					project.getDateStartSchedule()
				)
			)
			.setDateFinishSchedule(
				ValueManager.getProtoTimestampFromTimestamp(
					project.getDateFinishSchedule()
				)
			)
		;

		return builder;
	}

	public static Task.Builder convertTaskByProject(int projectId) {
		Task.Builder builder = Task.newBuilder();
		if (projectId <= 0) {
			return builder;
		}
		MProject project = MProject.getById(Env.getCtx(), projectId, null);
		return convertTaskByProject(project);
	}
	public static Task.Builder convertTaskByProject(MProject project) {
		Task.Builder builder = Task.newBuilder();
		if (project == null || project.getC_Project_ID() <= 0) {
			return builder;
		}
		Project.Builder projectBuilder = convertProject(project);

		builder.setTaskType(
				TaskType.PROJECT
			)
			.setName(
				project.getValue() + " - " + project.getName()
			)
			.setDescription(
				StringManager.getValidString(
					project.getDescription()
				)
			)
			.setStartDate(
				ValueManager.getProtoTimestampFromTimestamp(
					project.getDateStartSchedule()
				)
			)
			.setEndDate(
				ValueManager.getProtoTimestampFromTimestamp(
					project.getDateFinishSchedule()
				)
			)
			.setProject(projectBuilder)
		;

		return builder;
	}



	public static Request.Builder convertRequest(int requestId) {
		Request.Builder builder = Request.newBuilder();
		if (requestId <= 0) {
			return builder;
		}
		MRequest request = new MRequest(Env.getCtx(), requestId, null);
		return convertRequest(request);
	}

	public static Request.Builder convertRequest(MRequest request) {
		Request.Builder builder = Request.newBuilder();
		if (request == null || request.getR_Request_ID() <= 0) {
			return builder;
		}

		builder.setId(
			request.getR_Request_ID()
			)
			.setUuid(
				StringManager.getValidString(
					request.getUUID()
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					request.getDocumentNo()
				)
			)
			.setDateStartPlan(
				ValueManager.getProtoTimestampFromTimestamp(
					request.getDateStartPlan()
				)
			)
			.setDateCompletePlan(
				ValueManager.getProtoTimestampFromTimestamp(
					request.getDateCompletePlan()
				)
			)
		;

		return builder;
	}

	public static Task.Builder convertTaskByRequest(int requestId) {
		Task.Builder builder = Task.newBuilder();
		if (requestId <= 0) {
			return builder;
		}
		MRequest request = new MRequest(Env.getCtx(), requestId, null);
		return convertTaskByRequest(request);
	}
	public static Task.Builder convertTaskByRequest(MRequest request) {
		Task.Builder builder = Task.newBuilder();
		if (request == null || request.getR_Request_ID() <= 0) {
			return builder;
		}
		Request.Builder requestBuilder = convertRequest(request);

		builder.setTaskType(
				TaskType.REQUEST
			)
			.setName(
				request.getDisplayValue()
			)
			.setDescription(
				StringManager.getValidString(
					request.getSummary()
				)
			)
			.setStartDate(
				ValueManager.getProtoTimestampFromTimestamp(
					request.getDateStartPlan()
				)
			)
			.setEndDate(
				ValueManager.getProtoTimestampFromTimestamp(
					request.getDateCompletePlan()
				)
			)
			.setRequest(requestBuilder)
		;

		return builder;
	}



	public static ResourceAssignment.Builder convertResourceAssignment(int resourceAssignmentId) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		if (resourceAssignmentId <= 0) {
			return builder;
		}
		MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), resourceAssignmentId, null);
		return convertResourceAssignment(resourceAssignment);
	}

	public static ResourceAssignment.Builder convertResourceAssignment(MResourceAssignment resourceAssignment) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		if (resourceAssignment == null || resourceAssignment.getS_ResourceAssignment_ID() <= 0) {
			return builder;
		}

		builder.setId(
				resourceAssignment.getS_ResourceAssignment_ID()
			)
			.setUuid(
				StringManager.getValidString(
					resourceAssignment.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(
					resourceAssignment.getName()
				)
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					resourceAssignment.getQty()
				)
			)
			.setAssignDateForm(
				ValueManager.getProtoTimestampFromTimestamp(
					resourceAssignment.getAssignDateFrom()
				)
			)
			.setAssignDateTo(
				ValueManager.getProtoTimestampFromTimestamp(
					resourceAssignment.getAssignDateTo()
				)
			)
		;

		return builder;
	}

	public static Task.Builder convertTaskByResourceAssignment(int resourceAssignmentId) {
		Task.Builder builder = Task.newBuilder();
		if (resourceAssignmentId <= 0) {
			return builder;
		}
		MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), resourceAssignmentId, null);
		return convertTaskByResourceAssignment(resourceAssignment);
	}
	public static Task.Builder convertTaskByResourceAssignment(MResourceAssignment resourceAssignment) {
		Task.Builder builder = Task.newBuilder();
		if (resourceAssignment == null || resourceAssignment.getS_ResourceAssignment_ID() <= 0) {
			return builder;
		}
		ResourceAssignment.Builder resourceAssignmentBuilder = convertResourceAssignment(resourceAssignment);

		builder.setTaskType(
				TaskType.RESOURCE_ASSIGNMENT
			)
			.setName(
				StringManager.getValidString(
					resourceAssignment.getDisplayValue()
				)
			)
			.setDescription(
				StringManager.getValidString(
					resourceAssignment.getDescription()
				)
			)
			.setStartDate(
				ValueManager.getProtoTimestampFromTimestamp(
					resourceAssignment.getAssignDateFrom()
				)
			)
			.setEndDate(
				ValueManager.getProtoTimestampFromTimestamp(
					resourceAssignment.getAssignDateTo()
				)
			)
			.setResourceAssignment(resourceAssignmentBuilder)
		;

		return builder;
	}


}
