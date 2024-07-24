package org.spin.grpc.service;

import java.util.List;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.field.order.ListOrdersInfoRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoResponse;
import org.spin.backend.grpc.payment_print_export.ListPaymentsRequest;
import org.spin.backend.grpc.payment_print_export.ListPaymentsResponse;
import org.spin.backend.grpc.send_notifications.ListAppSupportsRequest;
import org.spin.backend.grpc.send_notifications.ListNotificationsTypesRequest;
import org.spin.backend.grpc.send_notifications.ListNotificationsTypesResponse;
import org.spin.backend.grpc.send_notifications.ListUsersRequest;
import org.spin.backend.grpc.send_notifications.NotifcationType;
import org.spin.backend.grpc.send_notifications.SendNotificationsGrpc.SendNotificationsImplBase;
import org.spin.base.util.LookupUtil;
import org.spin.model.MADAppRegistration;
import org.spin.service.grpc.util.value.ValueManager;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class SendNotifications extends  SendNotificationsImplBase{
    /**	Logger			*/
	private CLogger log = CLogger.getCLogger(ImportFileLoader.class);

    @Override
	public void listUsers(ListUsersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = ListUsers(request);
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
	
	private ListLookupItemsResponse.Builder ListUsers(ListUsersRequest request) {
		//	Add DocStatus for validation
		final String validationCode = "Email IS NOT NULL ";
		Query query = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			validationCode,
			null
		)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int count = query.count();
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(count);

		List<MUser> userList = query.list();
		userList.stream().forEach(userSelection -> {
			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				userSelection.getAD_User_ID(),
				userSelection.getUUID(),
				userSelection.getEMail(),
				userSelection.getDisplayValue(),
				userSelection.isActive()
			);

			builderList.addRecords(
				builderItem.build()
			);
		});

		return builderList;
	}

    @Override
	public void listNotificationsTypes(ListNotificationsTypesRequest request, StreamObserver<ListNotificationsTypesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListNotificationsTypesResponse.Builder builder = listNotificationsTypes(request);
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
	
	private ListNotificationsTypesResponse.Builder listNotificationsTypes(ListNotificationsTypesRequest request) {
        final String whereClause = "AD_Reference_ID = 54081"
			+ "AND Value IN('STW', 'SFA', 'SYT', 'SIG', 'SSK', 'SIN', 'SSN', 'STG', 'SWH', 'SDC', 'EMA', 'NTE') "
			+ "AND EXISTS("
				+ "SELECT 1 FROM AD_AppRegistration AS a "
				+ "WHERE a.ApplicationType = AD_Ref_List.Value "
				+ "AND a.AD_Client_ID IN(0, ?) "
				+ "ORDER BY a.AD_Client_ID DESC "
			+ ")"
		;

		final int clientId = Env.getAD_Client_ID((Env.getCtx()));
		Query query = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		)
			.setParameters(clientId)
		;

		MRefList.getList(Env.getCtx(), 54081, false);

		int count = query.count();

		ListNotificationsTypesResponse.Builder builderList = ListNotificationsTypesResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;

		List<MRefList> appList = query.list();                         
		appList.stream().forEach(refList -> {
			String value = refList.getValue();
			String name = refList.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			String description = refList.get_Translation(I_AD_Ref_List.COLUMNNAME_Value);
			NotifcationType.Builder builder = NotifcationType.newBuilder()
				.setName(
					ValueManager.validateNull(
						name
					)
				)
				.setValue(
					ValueManager.validateNull(
						value
					)
				)
				.setDescription(
					ValueManager.validateNull(
						description
					)
				)
			;

			builderList.addRecords(
				builder.build()
			);
		});

		return builderList;
	}
}
