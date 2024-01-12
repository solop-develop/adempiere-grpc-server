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
package org.spin.log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Note;
import org.adempiere.core.domains.models.I_AD_PInstance;
import org.compiere.model.MChangeLog;
import org.compiere.model.MPInstance;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.logs.EntityLog;
import org.spin.backend.grpc.logs.ListUserActivitesRequest;
import org.spin.backend.grpc.logs.ListUserActivitesResponse;
import org.spin.backend.grpc.logs.UserActivity;
import org.spin.backend.grpc.logs.UserActivityType;
import org.spin.backend.grpc.notice_management.Notice;
import org.spin.grpc.service.NoticeManagement;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import static com.google.protobuf.util.Timestamps.toMillis;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Logs
 */
public class LogsServiceLogic {
	public static ListUserActivitesResponse.Builder listUserActivites(ListUserActivitesRequest request) {
		final int userId = Env.getAD_User_ID(Env.getCtx());
		Timestamp date = ValueManager.getDateFromTimestampDate(
			request.getDate()
		);
		if (date == null) {
			// set current date
			date = new Timestamp(System.currentTimeMillis());
		}
		List<UserActivity> userActivitiesList = new ArrayList<>();

		// Process Log
		final String whereClauseProcessLog = "AD_User_ID = ? AND TRUNC(Created, 'DD') = ?";
		Query queryProcessLogs = new Query(
			Env.getCtx(),
			I_AD_PInstance.Table_Name,
			whereClauseProcessLog,
			null
		)
			.setParameters(userId, date)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;
		int count = queryProcessLogs.count();

		//	Convert Process Instance
		queryProcessLogs
			// .setLimit(limit, offset)
			.setOrderBy(I_AD_PInstance.COLUMNNAME_Created + " DESC")
			.getIDsAsList()
			.forEach(processInstanceId -> {
				MPInstance processInstance = new MPInstance(Env.getCtx(), processInstanceId, null);
				ProcessLog.Builder processLogBuilder = LogsConvertUtil.convertProcessLog(processInstance);

				UserActivity.Builder userBuilder = UserActivity.newBuilder();
				userBuilder.setUserActivityType(UserActivityType.PROCESS_LOG);
				userBuilder.setProcessLog(processLogBuilder);
				userActivitiesList.add(userBuilder.build());
			})
		;


		// Record Log
		String whereClauseRecordsLog = "CreatedBy = ? AND TRUNC(Created, 'DD') = ?";
		Query queryRecordLogs = new Query(
			Env.getCtx(),
			I_AD_ChangeLog.Table_Name,
			whereClauseRecordsLog,
			null
		)
			.setParameters(userId, date)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;
		count += queryRecordLogs.count();
		List<MChangeLog> recordLogList = queryRecordLogs
			.setOrderBy(I_AD_PInstance.COLUMNNAME_Created + " DESC")
			.<MChangeLog>list();

		//	Convert Record Log
		List<EntityLog.Builder> recordsLogsBuilderList = LogsConvertUtil.convertRecordLog(recordLogList);

		ListUserActivitesResponse.Builder builderList = ListUserActivitesResponse.newBuilder();
		recordsLogsBuilderList.forEach(recordLog -> {
			UserActivity.Builder userBuilder = UserActivity.newBuilder();
			userBuilder.setUserActivityType(UserActivityType.ENTITY_LOG);
			userBuilder.setEntityLog(recordLog);
			userActivitiesList.add(userBuilder.build());
		});

		// Notice
		final String whereClause = "AD_User_ID = ? AND Processed = ? AND TRUNC(Created, 'DD') = ?";
		Query queryNotices = new Query(
			Env.getCtx(),
			I_AD_Note.Table_Name,
			whereClause,
			null
		)
			.setParameters(userId, false, date)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED)
			.setOnlyActiveRecords(true)
		;
		count += queryNotices.count();

		//	Convert Notice
		queryNotices
			.setOrderBy(I_AD_Note.COLUMNNAME_Created + " DESC")
			.getIDsAsList()
			.forEach(noticeId -> {
				Notice.Builder noticeBuilder = NoticeManagement.convertNotice(noticeId);

				UserActivity.Builder userBuilder = UserActivity.newBuilder();
				userBuilder.setUserActivityType(UserActivityType.NOTICE);
				userBuilder.setNotice(noticeBuilder);
				userActivitiesList.add(userBuilder.build());
			});
		;

		// All activities
		List<UserActivity> recordsList = userActivitiesList.stream().sorted((u1, u2) -> {
			Timestamp from = null;
			if (u1.getUserActivityType() == UserActivityType.ENTITY_LOG) {
				from = ValueManager.getDateFromTimestampDate(
					u1.getEntityLog().getLogDate()
				);
			} else if (u1.getUserActivityType() == UserActivityType.PROCESS_LOG) {
				from = TimeManager.getTimestampFromLong(
					toMillis(u1.getProcessLog().getLastRun())
				);
			} else if (u1.getUserActivityType() == UserActivityType.NOTICE) {
				from = TimeManager.getTimestampFromLong(
					toMillis(u1.getNotice().getCreated())
				);
			}

			Timestamp to = null;
			if (u2.getUserActivityType() == UserActivityType.ENTITY_LOG) {
				to = ValueManager.getDateFromTimestampDate(
					u2.getEntityLog().getLogDate()
				);
			} else if (u2.getUserActivityType() == UserActivityType.PROCESS_LOG) {
				to = TimeManager.getTimestampFromLong(
					toMillis((u2.getProcessLog().getLastRun())));
			} else if (u2.getUserActivityType() == UserActivityType.NOTICE) {
				to = TimeManager.getTimestampFromLong(
					toMillis(u2.getNotice().getCreated())
				);
			}

			if (from == null || to == null) {
				// prevent Null Pointer Exception
				return 0;
			}
			return to.compareTo(from);

		})
		.collect(Collectors.toList());

		builderList.setRecordCount(count)
			.addAllRecords(recordsList)
		;

		return builderList;
	}

}
