/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
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

package org.spin.form.issue_management;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_R_RequestAction;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.core.domains.models.X_R_RequestUpdate;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestAction;
import org.compiere.model.MRequestType;
import org.compiere.model.MRole;
import org.compiere.model.MStatus;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.issue_management.DueType;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.IssueComment;
import org.spin.backend.grpc.issue_management.IssueCommentType;
import org.spin.backend.grpc.issue_management.Priority;
import org.spin.backend.grpc.issue_management.RequestType;
import org.spin.backend.grpc.issue_management.User;
import org.spin.model.MADAttachmentReference;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.AttachmentUtil;
import static com.google.protobuf.util.Timestamps.fromMillis;

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
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()));
			if (clientInfo != null && AttachmentUtil.getInstance().isValidForClient(clientInfo.getAD_Client_ID())) {
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
			.setDueDateTolerance(requestType.getDueDateTolerance())
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



	public static org.spin.backend.grpc.issue_management.Status.Builder convertStatus(int statusId) {
		org.spin.backend.grpc.issue_management.Status.Builder builder = org.spin.backend.grpc.issue_management.Status.newBuilder();
		if (statusId <= 0) {
			return builder;
		}

		MStatus requestType = MStatus.get(Env.getCtx(), statusId);
		return convertStatus(requestType);
	}
	public static org.spin.backend.grpc.issue_management.Status.Builder convertStatus(MStatus status) {
		org.spin.backend.grpc.issue_management.Status.Builder builder = org.spin.backend.grpc.issue_management.Status.newBuilder();
		if (status == null || status.getR_Status_ID() <= 0) {
			return builder;
		}

		builder.setId(status.getR_Status_ID())
			.setName(
				ValueManager.validateNull(status.getName())
			)
			.setDescription(
				ValueManager.validateNull(status.getDescription())
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
				ValueManager.validateNull(request.getDocumentNo())
			)
			.setSubject(
				ValueManager.validateNull(request.getSubject())
			)
			.setSummary(
				ValueManager.validateNull(request.getSummary())
			)
			.setCreated(fromMillis(request.getUpdated().getTime()))
			.setLastUpdated(fromMillis(request.getUpdated().getTime())
			)
			.setDateNextAction(fromMillis(request.getDateNextAction().getTime())
			)
			.setDueType(
				convertDueType(request.getDueType())
			)
			.setRequestType(
				convertRequestType(request.getR_RequestType_ID())
			)
			.setSalesRepresentative(
				IssueManagementConvertUtil.convertUser(request.getSalesRep_ID())
			)
			.setStatus(
				convertStatus(request.getR_Status_ID())
			)
			.setUser(
				IssueManagementConvertUtil.convertUser(request.getCreatedBy())
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
				fromMillis(requestUpdate.getCreated().getTime())
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
			.setCreated(fromMillis(requestAction.getCreated().getTime()))
			.setIssueCommentType(IssueCommentType.LOG)
			.setUser(
				IssueManagementConvertUtil.convertUser(requestAction.getCreatedBy())
			)
		;

		String columnModified = null;
		if (!Util.isEmpty(requestAction.getNullColumns(), true)) {
			columnModified = requestAction.getNullColumns();
		} else {
			if (requestAction.getR_RequestType_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_RequestType_ID;
			} else if (requestAction.getR_Group_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Group_ID;
			} else if (requestAction.getR_Category_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Category_ID;
			} else if (requestAction.getR_Status_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Status_ID;
			} else if (requestAction.getR_Resolution_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Resolution_ID;
			} else if (!Util.isEmpty(requestAction.getPriority(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Priority;
			} else if (!Util.isEmpty(requestAction.getPriorityUser(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_PriorityUser;
			} else if (!Util.isEmpty(requestAction.getSummary(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Summary;
			} else if (!Util.isEmpty(requestAction.getConfidentialType(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Summary;
			} else if (!Util.isEmpty(requestAction.getIsInvoiced(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsInvoiced;
			} else if (!Util.isEmpty(requestAction.getIsEscalated(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsEscalated;
			} else if (!Util.isEmpty(requestAction.getIsSelfService(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsSelfService;
			} else if (requestAction.getSalesRep_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_SalesRep_ID;
			} else if (requestAction.getAD_Role_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_AD_Role_ID;
			} else if (requestAction.getDateNextAction() != null) {
				columnModified = I_R_RequestAction.COLUMNNAME_DateNextAction;
			} else if (requestAction.getC_Activity_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Activity_ID;
			} else if (requestAction.getC_BPartner_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_BPartner_ID;
			} else if (requestAction.getAD_User_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_AD_User_ID;
			} else if (requestAction.getC_Project_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Project_ID;
			} else if (requestAction.getA_Asset_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_A_Asset_ID;
			} else if (requestAction.getC_Order_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Order_ID;
			} else if (requestAction.getC_Invoice_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Invoice_ID;
			} else if (requestAction.getM_Product_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_Product_ID;
			} else if (requestAction.getC_Payment_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Payment_ID;
			} else if (requestAction.getM_InOut_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_InOut_ID;
			} else if (requestAction.getM_RMA_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_RMA_ID;
			}
		}

		if (!Util.isEmpty(columnModified, true)) {
			MColumn column = new Query(
				Env.getCtx(),
				I_AD_Column.Table_Name,
				"AD_Table_ID = ? AND ColumnName = ?",
				null
			)
				.setParameters(I_R_RequestAction.Table_ID, columnModified)
				.first();

			if (column != null) {
				String label = column.getName();
				if (!Env.isBaseLanguage(Env.getCtx(), "")) {
					label = column.get_Translation(I_AD_Column.COLUMNNAME_Name);
				}
				builder.setLabel(
					ValueManager.validateNull(label)
				);

				Object value = requestAction.get_Value(columnModified);
				builder.setNewValue(
					ValueManager.getValueFromReference(value, column.getAD_Reference_ID())
				);
				String displayedValue = ValueManager.getDisplayedValueFromReference(
					value,
					columnModified,
					column.getAD_Reference_ID(),
					column.getAD_Reference_Value_ID()
				);
				builder.setDisplayedValue(
					ValueManager.validateNull(displayedValue)
				);
			}
		}

		return builder;
	}

}
