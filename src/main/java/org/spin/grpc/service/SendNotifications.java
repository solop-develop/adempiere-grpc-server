package org.spin.grpc.service;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.send_notifications.ListAppSupportsRequest;
import org.spin.backend.grpc.send_notifications.ListUserRequest;
import org.spin.backend.grpc.send_notifications.SendNotificationsGrpc.SendNotificationsImplBase;
import org.spin.base.util.LookupUtil;
import org.spin.model.MADAppRegistration;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class SendNotifications extends  SendNotificationsImplBase{
    /**	Logger			*/
	private CLogger log = CLogger.getCLogger(ImportFileLoader.class);

    @Override
	public void listUser(ListUserRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = ListUser(request, "AD_User");
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
	
	private ListLookupItemsResponse.Builder ListUser(ListUserRequest request, String tableName) {
		//	Add DocStatus for validation
		final String validationCode = " Email IS NOT null ";
		Query query = new Query(
			Env.getCtx(),
			tableName,
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
			// BigDecimal totalAmount = Env.ZERO;
			// if (paymentSelection.getTotalAmt() != null) {
			// 	totalAmount = paymentSelection.getTotalAmt();
			// }

			//	Display column
			String displayedValue = new StringBuffer()
				.append(Util.isEmpty(userSelection.getName(), true) ? "-1" : userSelection.getName())
				.append("_")
                .append(Util.isEmpty(userSelection.getEMail(), true) ? "-1" : userSelection.getEMail())
				// .append(MCurrency.getISO_Code(Env.getCtx(), userSelection.getC_Currency_ID()))
				// .append("_")
				// .append(DisplayType.getNumberFormat(DisplayType.Amount).format(totalAmount))
				.toString();
	
			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				userSelection.getAD_User_ID(),
				userSelection.getUUID(),
				userSelection.getEMail(),
				displayedValue,
				userSelection.isActive()
			);

			builderItem.setTableName(tableName);
			// builderItem.setId(paymentSelection.getC_PaySelection_ID());
			
			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}

    @Override
	public void listAppSupports(ListAppSupportsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = ListAppSupports(request, "AD_AppRegistration");
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
	
	private ListLookupItemsResponse.Builder ListAppSupports(ListAppSupportsRequest request, String tableName) {
        final String validationCode = "EXISTS ( SELECT 1 FROM ad_appsupport AS sp where sp.applicationtype = 'EMA' AND sp.AD_AppSupport_ID = AD_AppRegistration.AD_AppSupport_ID )";
		Query query = new Query(
			Env.getCtx(),
			tableName,
			validationCode,
			null
		)
		;

		int count = query.count();
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(count);

		List<MADAppRegistration> appList = query.list();
		appList.stream().forEach(appSelection -> {

			//	Display column
			String displayedValue = new StringBuffer()
				.append(Util.isEmpty(appSelection.getName(), true) ? "-1" : appSelection.getName())
				.append("_")
                .append(Util.isEmpty(appSelection.getDescription(), true) ? "-1" : appSelection.getDescription())
				.toString();
	
			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				appSelection.getAD_User_ID(),
				appSelection.getUUID(),
				appSelection.getApplicationType(),
				// appSelection.getValue(),
				displayedValue,
				appSelection.isActive()
			);

			builderItem.setTableName(tableName);
			
			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}
}
