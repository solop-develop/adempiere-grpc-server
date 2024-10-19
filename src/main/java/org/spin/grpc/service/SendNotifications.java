/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Elsio Sanchez elsiosanches@gmail.com                             *
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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.X_AD_AppRegistration;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.send_notifications.ListNotificationsTypesRequest;
import org.spin.backend.grpc.send_notifications.ListNotificationsTypesResponse;
import org.spin.backend.grpc.send_notifications.ListUsersRequest;
import org.spin.backend.grpc.send_notifications.NotifcationType;
import org.spin.backend.grpc.send_notifications.SendNotificationRequest;
import org.spin.backend.grpc.send_notifications.SendNotificationResponse;
import org.spin.backend.grpc.send_notifications.SendNotificationsGrpc.SendNotificationsImplBase;
import org.spin.base.util.LookupUtil;
import org.spin.eca62.support.IS3;
import org.spin.eca62.support.ResourceMetadata;
import org.spin.model.MADAppRegistration;
import org.spin.queue.notification.DefaultNotifier;
import org.spin.queue.util.QueueLoader;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Elsio Sanchez elsiosanches@gmail.com https://github.com/elsiosanchez
 * Service for Send Notifications
 */
public class SendNotifications extends SendNotificationsImplBase{
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(SendNotifications.class);


	@Override
	public void listUsers(ListUsersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = listUsers(request);
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

	private ListLookupItemsResponse.Builder listUsers(ListUsersRequest request) {
		//	Add DocStatus for validation
		final String validationCode = "NotificationType <> 'X' ";
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
			.setRecordCount(count)
		;

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
		final String whereClause = "AD_Reference_ID = ? "
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
			.setParameters(X_AD_AppRegistration.APPLICATIONTYPE_AD_Reference_ID, clientId);
		;

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


	public void sendNotification(SendNotificationRequest request, StreamObserver<SendNotificationResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			SendNotificationResponse.Builder builder = sendNotification(request);
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

	private SendNotificationResponse.Builder sendNotification(SendNotificationRequest request) {
		int userId = request.getUserId();
		//	Validate user
		if(userId <= 0 && request.getUserId() <= 0) {
			userId = Env.getAD_User_ID(
				Env.getCtx()
			);
		}

		if(Util.isEmpty(request.getNotificationType(), true)) {
			throw new AdempiereException("@NotifcationType@ @NotFound@");
		}

		// Validate the Title
		if(Util.isEmpty(request.getTitle(), true)) {
			throw new AdempiereException("@Title@ @Mandatory@");
		}

		// Validate the list of recipients
		if (request.getRecipientsList() == null || request.getRecipientsList().isEmpty()) {
			throw new AdempiereException("@Recipinets@ @Mandatory@");
		}
		StringBuffer error = new StringBuffer();
		request.getRecipientsList().forEach(recipient -> {
			if (recipient.getContactId() <= 0 && Util.isEmpty(recipient.getAccountName(), true)) {
				error.append("Recipient does not have a valid Contact ID (" + recipient.getContactId() + ") Account Name (" + recipient.getAccountName() + ") ");
			}
		});

		if (error.length() > 0) {
			throw new AdempiereException("Errors in the recipient list" + error.toString());
		}

		//	Get instance for notifier
		DefaultNotifier notifier = (DefaultNotifier) QueueLoader.getInstance()
			.getQueueManager(DefaultNotifier.QUEUETYPE_DefaultNotifier)
			.withContext(Env.getCtx())
		;

		//	Send notification to queue
		notifier
			.clearMessage()
			.withApplicationType(
				request.getNotificationType()
			)
			.withUserId(userId)
			.withText(
				request.getBody()
			)
			.withDescription(
				request.getTitle()
			)
		;

		// Add Recipient to Notification
		request.getRecipientsList().forEach(recipients -> {
			int contactId = -1;
			String accountName = null;
			if (recipients.getContactId() > 0) {
				MUser user = MUser.get(Env.getCtx(), recipients.getContactId());
				if (user != null) {
					contactId = user.getAD_User_ID();
					if (!Util.isEmpty(user.getEMail(), true)) {
						accountName = user.getEMail();
					}
				}
			}
			if (Util.isEmpty(accountName, true)) {
				accountName = recipients.getAccountName();
			}
			notifier.addRecipient(
				contactId,
				accountName
			);
		});

		// Add attachments
		List<File> files = getAttachmentsFiles(
			request.getAttachmentsList()
		);
		files.forEach(file -> {
			notifier.addAttachment(file);
		});

		//	Add to queue
		notifier.addToQueue();

		return SendNotificationResponse.newBuilder();
	}

	private List<File> getAttachmentsFiles(List<String> fileNames) {
		if(fileNames == null || fileNames.size() == 0) {
			return List.of();
		}
		List<File> files = new ArrayList<File>();
		try {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
			if(clientInfo.getFileHandler_ID() <= 0) {
				throw new AdempiereException("@FileHandler_ID@ @NotFound@");
			}

			MADAppRegistration genericConnector = MADAppRegistration.getById(
				Env.getCtx(),
				clientInfo.getFileHandler_ID(),
				null
			);
			if(genericConnector == null || genericConnector.getAD_AppRegistration_ID() <= 0) {
				throw new AdempiereException("@AD_AppRegistration_ID@ @NotFound@");
			}
			//	Load
			IAppSupport supportedApi = AppSupportHandler.getInstance().getAppSupport(genericConnector);
			if(supportedApi == null) {
				throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
			}
			if(!IS3.class.isAssignableFrom(supportedApi.getClass())) {
				throw new AdempiereException("@AD_AppSupport_ID@ @Unsupported@");
			}
			//	Get it
			IS3 fileHandler = (IS3) supportedApi;

			fileNames.forEach(fileName -> {
				if (Util.isEmpty(fileName, true)) {
					// item without file name
					return;
				}
				ResourceMetadata resourceMetadata = ResourceMetadata.newInstance()
					.withResourceName(fileName)
				;
				try {
					int lastFolder = fileName.lastIndexOf("/") + 1;
					String tempFolder = System.getProperty("java.io.tmpdir");
					File tmpFile = new File(tempFolder + File.separator + fileName.substring(lastFolder));
					InputStream inputStream = fileHandler.getResource(resourceMetadata);
					Files.copy(inputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					files.add(tmpFile);
				} catch (Exception e) {
					log.warning(e.getLocalizedMessage());
				}
			});
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
		}
		return files;
	}

}
