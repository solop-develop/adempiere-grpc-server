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

package org.spin.grpc.service.form.issue_management;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_R_RequestAction;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.core.domains.models.X_R_Request;
import org.adempiere.core.domains.models.X_R_RequestUpdate;
import org.compiere.model.MBPartner;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MGroup;
import org.compiere.model.MProject;
import org.compiere.model.MRefList;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestAction;
import org.compiere.model.MRequestCategory;
import org.compiere.model.MRequestType;
import org.compiere.model.MRole;
import org.compiere.model.MStatus;
import org.compiere.model.MStatusCategory;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.issue_management.BusinessPartner;
import org.spin.backend.grpc.issue_management.Category;
import org.spin.backend.grpc.issue_management.DueType;
import org.spin.backend.grpc.issue_management.Group;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.IssueComment;
import org.spin.backend.grpc.issue_management.IssueCommentLog;
import org.spin.backend.grpc.issue_management.IssueCommentType;
import org.spin.backend.grpc.issue_management.Priority;
import org.spin.backend.grpc.issue_management.Project;
import org.spin.backend.grpc.issue_management.RequestType;
import org.spin.backend.grpc.issue_management.Status;
import org.spin.backend.grpc.issue_management.StatusCategory;
import org.spin.backend.grpc.issue_management.TaskStatus;
import org.spin.backend.grpc.issue_management.User;
import org.spin.model.MADAttachmentReference;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.Value;

/**
 * This class was created for add all convert methods for Issue Management form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class IssueManagementConvertUtil {

	public static Priority.Builder convertPriority(String value) {
		Priority.Builder builder = Priority.newBuilder();
		if (Util.isEmpty(value, true)) {
			return builder;
		}
		MRefList priority = MRefList.get(Env.getCtx(), MRequest.PRIORITY_AD_Reference_ID, value, null);
		return convertPriority(priority);
	}
	public static Priority.Builder convertPriority(MRefList priority) {
		Priority.Builder builder = Priority.newBuilder();
		if (priority == null || priority.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = priority.getName();
		String description = priority.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = priority.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = priority.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(priority.getAD_Ref_List_ID())
			.setValue(
				ValueManager.validateNull(priority.getValue())
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setDescription(
				ValueManager.validateNull(description)
			)
		;

		return builder;
	}



	public static TaskStatus.Builder convertTaskStatus(String value) {
		TaskStatus.Builder builder = TaskStatus.newBuilder();
		if (Util.isEmpty(value, true)) {
			return builder;
		}
		MRefList taskStatus = MRefList.get(Env.getCtx(), X_R_Request.TASKSTATUS_AD_Reference_ID, value, null);
		return convertTaskStatus(taskStatus);
	}
	public static TaskStatus.Builder convertTaskStatus(MRefList taskStatus) {
		TaskStatus.Builder builder = TaskStatus.newBuilder();
		if (taskStatus == null || taskStatus.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = taskStatus.getName();
		String description = taskStatus.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = taskStatus.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = taskStatus.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(taskStatus.getAD_Ref_List_ID())
			.setValue(
				ValueManager.validateNull(taskStatus.getValue())
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setDescription(
				ValueManager.validateNull(description)
			)
		;

		return builder;
	}



	public static User.Builder convertUser(int userId) {
		if (userId <= 0) {
			return User.newBuilder();
		}
		MUser user = MUser.get(Env.getCtx(), userId);
		return convertUser(user);
	}
	public static User.Builder convertUser(MUser user) {
		User.Builder builder = User.newBuilder();
		if (user == null || user.getAD_User_ID() <= 0) {
			return builder;
		}
		builder.setId(user.getAD_User_ID())
			.setName(
				ValueManager.validateNull(user.getName())
			)
			.setDescription(
				ValueManager.validateNull(user.getDescription())
			)
		;
		if (user.getLogo_ID() > 0) {
			int clientId = Env.getAD_Client_ID(Env.getCtx());
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), clientId);
			if (clientInfo != null && AttachmentUtil.getInstance().isValidForClient(clientId)) {
				MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					clientInfo.getLogo_ID(),
					null
				);
				if (attachmentReference != null && attachmentReference.getAD_AttachmentReference_ID() > 0) {
					builder.setAvatar(
						ValueManager.validateNull(
							attachmentReference.getValidFileName()
						)
					);
				}
			}
		}
		return builder;
	}



	public static DueType.Builder convertDueType(String value) {
		DueType.Builder builder = DueType.newBuilder();
		if (Util.isEmpty(value, true)) {
			return builder;
		}
		MRefList priority = MRefList.get(Env.getCtx(), MRequest.DUETYPE_AD_Reference_ID, value, null);
		return convertDueType(priority);
	}
	public static DueType.Builder convertDueType(MRefList dueType) {
		DueType.Builder builder = DueType.newBuilder();
		if (dueType == null || dueType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = dueType.getName();
		String description = dueType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = dueType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = dueType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(dueType.getAD_Ref_List_ID())
			.setValue(
				ValueManager.validateNull(dueType.getValue())
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setDescription(
				ValueManager.validateNull(description)
			)
		;

		return builder;
	}



	public static RequestType.Builder convertRequestType(int requestTypeId) {
		RequestType.Builder builder = RequestType.newBuilder();
		if (requestTypeId <= 0) {
			return builder;
		}

		MRequestType requestType = MRequestType.get(Env.getCtx(), requestTypeId);
		return convertRequestType(requestType);
	}
	public static RequestType.Builder convertRequestType(MRequestType requestType) {
		RequestType.Builder builder = RequestType.newBuilder();
		if (requestType == null) {
			return builder;
		}

		builder.setId(requestType.getR_RequestType_ID())
			.setName(
				ValueManager.validateNull(requestType.getName())
			)
			.setDescription(
				ValueManager.validateNull(requestType.getDescription())
			)
			.setDueDateTolerance(
				requestType.getDueDateTolerance()
			)
			.setIsDefault(
				requestType.isDefault()
			)
		;

		final String whereClause = "R_StatusCategory_ID = ? AND IsDefault = 'Y' ";
		MStatus defaultStatus = new Query(
			Env.getCtx(),
			I_R_Status.Table_Name,
			whereClause,
			null
		)
			.setParameters(requestType.getR_StatusCategory_ID())
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.first()
		;
		builder.setDefaultStatus(
			convertStatus(
				defaultStatus
			)
		);

		return builder;
	}



	public static StatusCategory.Builder convertStatusCategory(int statusCategoryId) {
		StatusCategory.Builder builder = StatusCategory.newBuilder();
		if (statusCategoryId <= 0) {
			return builder;
		}

		MStatusCategory statusCategory = MStatusCategory.get(Env.getCtx(), statusCategoryId);
		return convertStatusCategory(statusCategory);
	}
	public static StatusCategory.Builder convertStatusCategory(MStatusCategory statusCategory) {
		StatusCategory.Builder builder = StatusCategory.newBuilder();
		if (statusCategory == null || statusCategory.getR_StatusCategory_ID() <= 0) {
			return builder;
		}

		builder.setId(statusCategory.getR_StatusCategory_ID())
			.setName(
				ValueManager.validateNull(statusCategory.getName())
			)
			.setDescription(
				ValueManager.validateNull(statusCategory.getDescription())
			)
			.setIsDefault(
				statusCategory.isDefault()
			)
		;

		return builder;
	}



	public static Status.Builder convertStatus(int statusId) {
		Status.Builder builder = Status.newBuilder();
		if (statusId <= 0) {
			return builder;
		}

		MStatus status = MStatus.get(Env.getCtx(), statusId);
		return convertStatus(status);
	}
	public static Status.Builder convertStatus(MStatus status) {
		Status.Builder builder = Status.newBuilder();
		if (status == null || status.getR_Status_ID() <= 0) {
			return builder;
		}

		builder.setId(status.getR_Status_ID())
			.setName(
				ValueManager.validateNull(status.getName())
			)
			.setValue(
				ValueManager.validateNull(
					status.getValue()
				)
			)
			.setDescription(
				ValueManager.validateNull(status.getDescription())
			)
			.setSequence(
				status.getSeqNo()
			)
			.setIsDefault(
				status.isDefault()
			)
		;

		return builder;
	}



	public static Category.Builder convertCategory(int requestCategoryId) {
		Category.Builder builder = Category.newBuilder();
		if (requestCategoryId <= 0) {
			return builder;
		}

		MRequestCategory status = MRequestCategory.get(Env.getCtx(), requestCategoryId);
		return convertCategory(status);
	}
	public static Category.Builder convertCategory(MRequestCategory category) {
		Category.Builder builder = Category.newBuilder();
		if (category == null || category.getR_Category_ID() <= 0) {
			return builder;
		}

		builder.setId(category.getR_Category_ID())
			.setName(
				ValueManager.validateNull(
					category.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					category.getDescription()
				)
			)
		;

		return builder;
	}



	public static Group.Builder convertGroup(int groupId) {
		Group.Builder builder = Group.newBuilder();
		if (groupId <= 0) {
			return builder;
		}

		MGroup group = MGroup.get(Env.getCtx(), groupId);
		return convertGroup(group);
	}
	public static Group.Builder convertGroup(MGroup group) {
		Group.Builder builder = Group.newBuilder();
		if (group == null || group.getR_Group_ID() <= 0) {
			return builder;
		}

		builder.setId(group.getR_Group_ID())
			.setName(
				ValueManager.validateNull(
					group.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					group.getDescription()
				)
			)
		;

		return builder;
	}



	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartnerId <= 0) {
			return builder;
		}

		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(businessPartner);
	}
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}

		builder.setId(businessPartner.getC_BPartner_ID())
			.setValue(
				ValueManager.validateNull(
					businessPartner.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(
					businessPartner.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					businessPartner.getDescription()
				)
			)
		;

		return builder;
	}



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

		builder.setId(project.getC_Project_ID())
			.setValue(
				ValueManager.validateNull(
					project.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(
					project.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					project.getDescription()
				)
			)
		;

		return builder;
	}



	public static Issue.Builder convertRequest(int requestId) {
		if (requestId <= 0) {
			return Issue.newBuilder();
		}
		MRequest request = new MRequest(Env.getCtx(), requestId, null);
		return convertRequest(request);
	}
	public static Issue.Builder convertRequest(MRequest request) {
		Issue.Builder builder = Issue.newBuilder();
		if (request == null) {
			return builder;
		}

		builder.setId(request.getR_Request_ID())
			.setDocumentNo(
				ValueManager.validateNull(
					request.getDocumentNo()
				)
			)
			.setSubject(
				ValueManager.validateNull(
					request.getSubject()
				)
			)
			.setSummary(
				ValueManager.validateNull(
					request.getSummary()
				)
			)
			.setCreated(
				ValueManager.getTimestampFromDate(
					request.getUpdated()
				)
			)
			.setLastUpdated(
				ValueManager.getTimestampFromDate(
					request.getUpdated()
				)
			)
			.setDateNextAction(
				ValueManager.getTimestampFromDate(
					request.getDateNextAction()
				)
			)
			.setDueType(
				convertDueType(
					request.getDueType()
				)
			)
			.setRequestType(
				convertRequestType(
					request.getR_RequestType_ID()
				)
			)
			.setSalesRepresentative(
				IssueManagementConvertUtil.convertUser(
					request.getSalesRep_ID()
				)
			)
			.setStatus(
				convertStatus(request.getR_Status_ID())
			)
			.setUser(
				IssueManagementConvertUtil.convertUser(
					request.getCreatedBy()
				)
			)
			.setTaskStatus(
				IssueManagementConvertUtil.convertTaskStatus(
					request.getTaskStatus()
				)
			)
			.setCategory(
				IssueManagementConvertUtil.convertCategory(
					request.getR_Category_ID()
				)
			)
			.setGroup(
				IssueManagementConvertUtil.convertGroup(
					request.getR_Group_ID()
				)
			)
			.setBusinessPartner(
				IssueManagementConvertUtil.convertBusinessPartner(
					request.getC_BPartner_ID()
				)
			)
			.setProject(
				IssueManagementConvertUtil.convertProject(
					request.getC_Project_ID()
				)
			)
			.setParentIssue(
				IssueManagementConvertUtil.convertRequest(
					request.getR_RequestRelated_ID()
				)
			)
		;
		if (!Util.isEmpty(request.getPriority(), true)) {
			builder.setPriority(
				convertPriority(request.getPriority())
			);
		}

		return builder;
	}



	public static IssueComment.Builder convertRequestUpdate(int requestUpdateId) {
		if (requestUpdateId <= 0) {
			return IssueComment.newBuilder();
		}
		X_R_RequestUpdate requestUpdate = new X_R_RequestUpdate(Env.getCtx(), requestUpdateId, null);
		return convertRequestUpdate(requestUpdate);
	}
	public static IssueComment.Builder convertRequestUpdate(X_R_RequestUpdate requestUpdate) {
		IssueComment.Builder builder = IssueComment.newBuilder();
		if (requestUpdate == null || requestUpdate.getR_RequestUpdate_ID() <= 0) {
			return builder;
		}
		builder.setId(requestUpdate.getR_RequestUpdate_ID())
			.setCreated(
				ValueManager.getTimestampFromDate(
					requestUpdate.getCreated()
				)
			)
			.setResult(
				ValueManager.validateNull(requestUpdate.getResult())
			)
			.setIssueCommentType(IssueCommentType.COMMENT)
			.setUser(
				IssueManagementConvertUtil.convertUser(requestUpdate.getCreatedBy())
			)
		;

		return builder;
	}



	public static IssueComment.Builder convertRequestAction(int requestActionId) {
		if (requestActionId <= 0) {
			return IssueComment.newBuilder();
		}
		MRequestAction requestAction = new MRequestAction(Env.getCtx(), requestActionId, null);
		return convertRequestAction(requestAction);
	}
	public static IssueComment.Builder convertRequestAction(MRequestAction requestAction) {
		IssueComment.Builder builder = IssueComment.newBuilder();
		if (requestAction == null || requestAction.getR_RequestAction_ID() <= 0) {
			return builder;
		}
		builder.setId(requestAction.getR_RequestAction_ID())
			.setCreated(
				ValueManager.getTimestampFromDate(
					requestAction.getCreated()
				)
			)
			.setIssueCommentType(IssueCommentType.LOG)
			.setUser(
				IssueManagementConvertUtil.convertUser(requestAction.getCreatedBy())
			)
		;

		if (!Util.isEmpty(requestAction.getNullColumns(), true)) {
			for (String columnName : requestAction.getNullColumns().split(";")) {
				IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
					columnName,
					null
				);
				builder.addChangeLogs(columnBuilder);
			}
		}

		if (requestAction.getR_RequestType_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_R_RequestType_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getR_Group_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_R_Group_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getR_Category_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_R_Category_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getR_Status_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_R_Status_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getR_Resolution_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_R_Resolution_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getTaskStatus(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_TaskStatus;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getPriority(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_Priority;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getPriorityUser(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_PriorityUser;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getSummary(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_Summary;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getConfidentialType(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_ConfidentialType;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getIsInvoiced(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_IsInvoiced;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getIsEscalated(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_IsEscalated;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (!Util.isEmpty(requestAction.getIsSelfService(), true)) {
			String columnName = I_R_RequestAction.COLUMNNAME_IsSelfService;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getSalesRep_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_SalesRep_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getAD_Role_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_AD_Role_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_Activity_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_Activity_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_BPartner_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_BPartner_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getAD_User_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_AD_User_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_Project_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_Project_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getA_Asset_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_A_Asset_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_Order_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_Order_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_Invoice_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_Invoice_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getM_Product_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_M_Product_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getC_Payment_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_C_Payment_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getM_InOut_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_M_InOut_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getM_RMA_ID() > 0) {
			String columnName = I_R_RequestAction.COLUMNNAME_M_RMA_ID;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}
		if (requestAction.getDateNextAction() != null) {
			String columnName = I_R_RequestAction.COLUMNNAME_DateNextAction;
			IssueCommentLog.Builder columnBuilder = convertIssueCommentLog(
				columnName,
				requestAction.get_Value(columnName)
			);
			builder.addChangeLogs(columnBuilder);
		}

		return builder;
	}


	public static IssueCommentLog.Builder convertIssueCommentLog(String columnName, Object value) {
		IssueCommentLog.Builder builder = IssueCommentLog.newBuilder();
		if (Util.isEmpty(columnName, true)) {
			return builder;
		}

		MColumn column = new Query(
			Env.getCtx(),
			I_AD_Column.Table_Name,
			"AD_Table_ID = ? AND ColumnName = ?",
			null
		)
			.setParameters(I_R_RequestAction.Table_ID, columnName)
			.first();
		if (column == null) {
			return builder;
		}

		String label = column.getName();
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			label = column.get_Translation(I_AD_Column.COLUMNNAME_Name);
		}

		Value.Builder valueBuilder = ValueManager.getValueFromReference(
			value,
			column.getAD_Reference_ID()
		);
		builder.setNewValue(
			valueBuilder
		);
		String displayedValue = ValueManager.getDisplayedValueFromReference(
			value,
			column.getColumnName(),
			column.getAD_Reference_ID(),
			column.getAD_Reference_Value_ID()
		);
		builder.setDisplayedValue(
			ValueManager.validateNull(displayedValue)
		);

		builder.setColumnName(columnName)
			.setLabel(
				ValueManager.validateNull(
					label
				)
			)
		;

		return builder;
	}

}
