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
package org.spin.grpc.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Note;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MNote;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
import org.spin.backend.grpc.notice_management.AcknowledgeNoticeRequest;
import org.spin.backend.grpc.notice_management.AcknowledgeNoticeResponse;
import org.spin.backend.grpc.notice_management.DeleteNoticesRequest;
import org.spin.backend.grpc.notice_management.DeleteNoticesResponse;
import org.spin.backend.grpc.notice_management.ListNoticesRequest;
import org.spin.backend.grpc.notice_management.ListNoticesResponse;
import org.spin.backend.grpc.notice_management.ListUsersRequest;
import org.spin.backend.grpc.notice_management.ListUsersResponse;
import org.spin.backend.grpc.notice_management.Notice;
import org.spin.backend.grpc.notice_management.User;
import org.spin.backend.grpc.notice_management.NoticeManagementGrpc.NoticeManagementImplBase;
import org.spin.base.db.LimitUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Notice Management
 */
public class NoticeManagement extends NoticeManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(NoticeManagement.class);


	public static User.Builder convertUser(int userId) {
		User.Builder builder = User.newBuilder();
		if (userId <= 0) {
			return builder;
		}
		MUser user = MUser.get(Env.getCtx(), userId);
		return convertUser(user);
	}
	public static User.Builder convertUser(MUser user) {
		User.Builder builder = User.newBuilder();
		if (user == null || user.getAD_User_ID() <= 0) {
			return builder;
		}
		
		builder.setId(
				user.getAD_User_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					user.getUUID()
				)
			)
			.setValue(
				ValueManager.validateNull(
					user.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(
					user.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					user.getDescription()
				)
			)
		;
		return builder;
	}



	public static Notice.Builder convertNotice(int noticeId) {
		Notice.Builder builder = Notice.newBuilder();
		if (noticeId <= 0) {
			return builder;
		}
		MNote note = new MNote(Env.getCtx(), noticeId, null);
		return convertNotice(
			note
		);
	}
	public static Notice.Builder convertNotice(MNote notice) {
		Notice.Builder builder = Notice.newBuilder();
		if (notice == null || notice.getAD_Note_ID() <= 0) {
			return builder;
		}
		builder.setId(notice.getAD_Note_ID())
			.setUuid(
				ValueManager.validateNull(
					notice.getUUID()
				)
			)
			.setCreated(
				ValueManager.getTimestampFromDate(
					notice.getCreated()
				)
			)
			.setMessage(
				ValueManager.validateNull(
					notice.getMessage()
				)
			)
			.setUser(
				convertUser(
					notice.getAD_User_ID()
				)
			)
			.setTableName(
				MTable.getTableName(
					Env.getCtx(),
					notice.getAD_Table_ID()
				)
			)
			.setRecordId(
				notice.getRecord_ID()
			)
			.setReference(
				ValueManager.validateNull(
					notice.getReference()
				)
			)
			.setTextMessage(
				ValueManager.validateNull(
					notice.getTextMsg()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					notice.getDescription()
				)
			)
			.setIsAcknowledge(
				notice.isProcessed()
			)
		;
		return builder;
	}


	@Override
	public void listNotices(ListNoticesRequest request, StreamObserver<ListNoticesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListNoticesResponse.Builder builderList = listNotices(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	public ListNoticesResponse.Builder listNotices(ListNoticesRequest request) {
		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			Env.getAD_User_ID(Env.getCtx())
		);
		filtersList.add(false);

		final String whereClause = "AD_User_ID = ? AND Processed = ?";
		Query queryNotices = new Query(
			Env.getCtx(),
			I_AD_Note.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED)
			.setOnlyActiveRecords(true)
		;

		int recordCount = queryNotices.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListNoticesResponse.Builder builderList = ListNoticesResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(nexPageToken)
			)
		;

		queryNotices
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(noticeId -> {
				Notice.Builder builder = convertNotice(noticeId);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}



	@Override
	public void acknowledgeNotice(AcknowledgeNoticeRequest request, StreamObserver<AcknowledgeNoticeResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			AcknowledgeNoticeResponse.Builder builder = acknowledgeNotice(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	public AcknowledgeNoticeResponse.Builder acknowledgeNotice(AcknowledgeNoticeRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Notice_ID@");
		}
		MNote notice = new MNote(Env.getCtx(), request.getId(), null);
		if (notice == null || notice.getAD_Note_ID() <= 0) {
			throw new AdempiereException("@AD_Notice_ID@ @NotFound@");
		}
		notice.setProcessed(true);
		notice.saveEx();

		AcknowledgeNoticeResponse.Builder builder = AcknowledgeNoticeResponse.newBuilder();
		return builder;
	}



	@Override
	public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListUsersResponse.Builder builderList = listUsers(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	public ListUsersResponse.Builder listUsers(ListUsersRequest request) {
		final String whereClause = "EXISTS("
			+ "SELECT * FROM C_BPartner bp "
			+ "WHERE AD_User.C_BPartner_ID=bp.C_BPartner_ID "
			+ "AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y')"
			+ ")"
		;		Query queryNotices = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED)
			.setOnlyActiveRecords(true)
		;

		int recordCount = queryNotices.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(
			SessionManager.getSessionUuid(),
			request.getPageToken()
		);
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListUsersResponse.Builder builderList = ListUsersResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(nexPageToken)
			)
		;

		queryNotices
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(userId -> {
				User.Builder builder = convertUser(userId);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}



	@Override
	public void deleteNotices(DeleteNoticesRequest request, StreamObserver<DeleteNoticesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			DeleteNoticesResponse.Builder builder = deleteNotices(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	public DeleteNoticesResponse.Builder deleteNotices(DeleteNoticesRequest request) throws FileNotFoundException, IOException {
		// AD_NoteDelete
		final int processId = 241;
		Struct.Builder parameters = Struct.newBuilder();
		if (request.getUserId() > 0) {
			Value.Builder value = ValueManager.getValueFromInt(
				request.getUserId()
			);
			parameters.putFields("AD_User_ID", value.build());
		}
		if (request.getKeepLogDays() > 0) {
			Value.Builder value = ValueManager.getValueFromInt(
				request.getKeepLogDays()
			);
			parameters.putFields("KeepLogDays", value.build());
		}
		RunBusinessProcessRequest.Builder processRequest = RunBusinessProcessRequest.newBuilder()
			.setId(processId)
			.setParameters(parameters)
		;

		ProcessLog.Builder processLog = BusinessData.runBusinessProcess(
			processRequest.build()
		);

		// Response
		DeleteNoticesResponse.Builder builder = DeleteNoticesResponse.newBuilder()
			.setSummary(
				processLog.getSummary()
			)
			.addAllLogs(
				processLog.getLogsList()
			)
		;
		return builder;
	}

}
