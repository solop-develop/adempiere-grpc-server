/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.grpc.service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.MAttachment;
import org.compiere.model.MClientInfo;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.file_management.Attachment;
import org.spin.backend.grpc.file_management.DeleteResourceReferenceRequest;
import org.spin.backend.grpc.file_management.ExistsAttachmentRequest;
import org.spin.backend.grpc.file_management.ExistsAttachmentResponse;
import org.spin.backend.grpc.file_management.FileManagementGrpc.FileManagementImplBase;
import org.spin.backend.grpc.file_management.GetAttachmentRequest;
import org.spin.backend.grpc.file_management.GetResourceReferenceRequest;
import org.spin.backend.grpc.file_management.GetResourceRequest;
import org.spin.backend.grpc.file_management.Resource;
import org.spin.backend.grpc.file_management.ResourceReference;
import org.spin.backend.grpc.file_management.SetResourceReferenceRequest;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.model.I_AD_AttachmentReference;
import org.spin.model.MADAttachmentReference;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.ByteString;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class FileManagementServiceImplementation extends FileManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(FileManagementServiceImplementation.class);
	
	public String tableName = I_C_Invoice.Table_Name;


	@Override
	public void getResource(GetResourceRequest request, StreamObserver<Resource> responseObserver) {
		try {
			if (request == null || (Util.isEmpty(request.getResourceUuid(), true) && Util.isEmpty(request.getResourceName(), true))) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Download Requested = " + request.getResourceUuid());
			ContextManager.getContext(request.getClientRequest());
			//	Get resource
			getResource(request.getResourceUuid(), request.getResourceName(), responseObserver);
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	public String getResourceUuidFromName(String resourceName) {
		if (Util.isEmpty(resourceName, true)) {
			return null;
		}

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
		MADAttachmentReference reference = new Query(
				Env.getCtx(),
				I_AD_AttachmentReference.Table_Name,
				"(UUID || '-' || FileName) = ? AND FileHandler_ID = ?",
				null
			)
			.setOrderBy(I_AD_AttachmentReference.COLUMNNAME_AD_Attachment_ID + " DESC")
			.setParameters(resourceName, clientInfo.getFileHandler_ID())
			.first();

		if (reference == null || reference.getAD_AttachmentReference_ID() <= 0) {
			return null;
		}
		return reference.getUUID();
	}
	
	/**
	 * Get File from fileName
	 * @param resourceUuid
	 * @param responseObserver
	 * @throws Exception 
	 */
	private void getResource(String resourceUuid, String resourceName, StreamObserver<Resource> responseObserver) throws Exception {
		if (!AttachmentUtil.getInstance().isValidForClient(Env.getAD_Client_ID(Env.getCtx()))) {
			responseObserver.onError(new AdempiereException("@NotFound@"));
			return;
		}

		//	Validate by name
		if (Util.isEmpty(resourceUuid, true)) {
			resourceUuid = getResourceUuidFromName(resourceName);
			if (Util.isEmpty(resourceUuid, true)) {
				responseObserver.onError(new AdempiereException("@NotFound@"));
				return;
			}
		}
		byte[] data = AttachmentUtil.getInstance()
			.withClientId(Env.getAD_Client_ID(Env.getCtx()))
			.withAttachmentReferenceId(RecordUtil.getIdFromUuid(I_AD_AttachmentReference.Table_Name, resourceUuid, null))
			.getAttachment();
		if (data == null) {
			responseObserver.onError(new AdempiereException("@NotFound@"));
			return;
		}
		//	For all
		int bufferSize = 256 * 1024; // 256k
        byte[] buffer = new byte[bufferSize];
        int length;
        InputStream is = new ByteArrayInputStream(data);
        while ((length = is.read(buffer, 0, bufferSize)) != -1) {
          responseObserver.onNext(
    		  Resource.newBuilder()
    		  	.setData(ByteString.copyFrom(buffer, 0, length))
    		  	.build()
          );
        }
        //	Completed
        responseObserver.onCompleted();
	}


	@Override
	public void getResourceReference(GetResourceReferenceRequest request, StreamObserver<ResourceReference> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ContextManager.getContext(request.getClientRequest());
			ResourceReference.Builder resourceReference = getResourceReferenceFromImageId(request.getImageId());
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Get resource from Image Id
	 * @param imageId
	 * @return
	 */
	private ResourceReference.Builder getResourceReferenceFromImageId(int imageId) {
		return convertResourceReference(RecordUtil.getResourceFromImageId(imageId));
	}


	@Override
	public void getAttachment(GetAttachmentRequest request, StreamObserver<Attachment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Attachment.Builder attachment = getAttachmentFromEntity(request);
			responseObserver.onNext(attachment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Get Attachment related to entity
	 * @param request
	 * @return
	 */
	private Attachment.Builder getAttachmentFromEntity(GetAttachmentRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		int tableId = 0;
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = MTable.get(context, request.getTableName());
			if (table != null && table.getAD_Table_ID() > 0) {
				tableId = table.getAD_Table_ID();
			}
		}
		int recordId = request.getId();
		if (recordId <= 0) {
			recordId = RecordUtil.getIdFromUuid(request.getTableName(), request.getUuid(), null);
		}
		if (tableId > 0 && recordId > 0) {
			return convertAttachment(MAttachment.get(context, tableId, recordId));
		}
		return Attachment.newBuilder();
	}


	/**
	 * Convert resource
	 * @param reference
	 * @return
	 */
	public static ResourceReference.Builder convertResourceReference(MADAttachmentReference reference) {
		if (reference == null) {
			return ResourceReference.newBuilder();
		}
		return ResourceReference.newBuilder()
			.setResourceUuid(ValueUtil.validateNull(reference.getUUID()))
			.setFileName(ValueUtil.validateNull(reference.getValidFileName()))
			.setDescription(ValueUtil.validateNull(reference.getDescription()))
			.setTextMsg(ValueUtil.validateNull(reference.getTextMsg()))
			.setContentType(ValueUtil.validateNull(MimeType.getMimeType(reference.getFileName())))
			.setFileSize(ValueUtil.getDecimalFromBigDecimal(reference.getFileSize()))
		;
	}

	/**
	 * Convert Attachment to gRPC
	 * @param attachment
	 * @return
	 */
	public static Attachment.Builder convertAttachment(MAttachment attachment) {
		if (attachment == null) {
			return Attachment.newBuilder();
		}
		Attachment.Builder builder = Attachment.newBuilder()
				.setAttachmentUuid(ValueUtil.validateNull(attachment.getUUID()))
				.setTitle(ValueUtil.validateNull(attachment.getTitle()))
				.setTextMsg(ValueUtil.validateNull(attachment.getTextMsg()));
		MClientInfo clientInfo = MClientInfo.get(attachment.getCtx());
		if (clientInfo == null || clientInfo.getFileHandler_ID() <= 0) {
			return builder;
		}

		MADAttachmentReference.resetAttachmentCacheFromId(clientInfo.getFileHandler_ID(), attachment.getAD_Attachment_ID());
		MADAttachmentReference.getListByAttachmentId(
			attachment.getCtx(),
			clientInfo.getFileHandler_ID(),
			attachment.getAD_Attachment_ID(),
			attachment.get_TrxName()
		)
		.forEach(attachmentReference -> {
			builder.addResourceReferences(convertResourceReference(attachmentReference));
		});
		return builder;
	}


	@Override
	public void setResourceReference(SetResourceReferenceRequest request, StreamObserver<ResourceReference> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceReference.Builder resourceReference = setResourceReference(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ResourceReference.Builder setResourceReference(SetResourceReferenceRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		MClientInfo clientInfo = MClientInfo.get(context);
		if (clientInfo == null || clientInfo.getFileHandler_ID() <= 0) {
			throw new AdempiereException("@FileHandler_ID@ @NotFound@");
		}

		String fileName = request.getFileName();
		if (Util.isEmpty(fileName, true)) {
			throw new AdempiereException("@Name@ @Attachment@ @NotFound@");
		}
		if (!MimeType.isValidMimeType(fileName)) {
			throw new AdempiereException("@Error@ @FileInvalidExtension@");
		}

		int tableId = 0;
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = MTable.get(context, request.getTableName());
			if (table != null && table.getAD_Table_ID() > 0) {
				tableId = table.getAD_Table_ID();
			}
		}
		if (tableId <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		int recordId = request.getRecordId();
		if (recordId <= 0) {
			if (Util.isEmpty(request.getRecordUuid(), true)) {
				recordId = RecordUtil.getIdFromUuid(request.getTableName(), request.getRecordUuid(), null);
			}
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}

		MAttachment attachment = new MAttachment(context, tableId, recordId, null);
		if (attachment.getAD_Attachment_ID() <= 0) {
			attachment.saveEx();
		}
		MADAttachmentReference attachmentReference = new MADAttachmentReference(context, 0, null);
		attachmentReference.setFileHandler_ID(clientInfo.getFileHandler_ID());
		attachmentReference.setAD_Attachment_ID(attachment.getAD_Attachment_ID());
		attachmentReference.setTextMsg(
			ValueUtil.validateNull(request.getTextMessage())
		);
		attachmentReference.setFileName(fileName);
		// attachmentReference.setFileSize(
		// 	BigDecimal.valueOf(request.getFileSize())
		// );
		attachmentReference.saveEx();
		//	Remove from cache
		MADAttachmentReference.resetAttachmentCacheFromId(clientInfo.getFileHandler_ID(), attachment.getAD_Attachment_ID());

		ResourceReference.Builder builder = convertResourceReference(attachmentReference);

		return builder;
	}

	@Override
	public void deleteResourceReference(DeleteResourceReferenceRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Empty.Builder resourceReference = deleteResourceReference(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	Empty.Builder deleteResourceReference(DeleteResourceReferenceRequest request) throws Exception {
		Properties context = ContextManager.getContext(request.getClientRequest());
		String resourceUuid = request.getResourceUuid();
		if (Util.isEmpty(resourceUuid, true)) {
			resourceUuid = getResourceUuidFromName(request.getResourceName());
		}
		if (Util.isEmpty(resourceUuid, true)) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}

		MADAttachmentReference resourceReference = (MADAttachmentReference) RecordUtil.getEntity(
			context,
			MADAttachmentReference.Table_Name, 
			resourceUuid,
			0,
			null
		);
		if (resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ Null");
		}

		MClientInfo clientInfo = MClientInfo.get(context);
		if (clientInfo == null || clientInfo.getFileHandler_ID() <= 0) {
			throw new AdempiereException("@FileHandler_ID@ @NotFound@");
		}
		AttachmentUtil.getInstance()
			.clear()
			.withAttachmentId(resourceReference.getAD_Attachment_ID())
			.withFileName(resourceReference.getFileName())
			.withClientId(clientInfo.getAD_Client_ID())
			.deleteAttachment();
		MADAttachmentReference.resetAttachmentCacheFromId(clientInfo.getFileHandler_ID(), resourceReference.getAD_Attachment_ID());

		return Empty.newBuilder();
	}

	@Override
	public void existsAttachment(ExistsAttachmentRequest request, StreamObserver<ExistsAttachmentResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ExistsAttachmentResponse.Builder resourceReference = existsAttachment(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ExistsAttachmentResponse.Builder existsAttachment(ExistsAttachmentRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		ExistsAttachmentResponse.Builder builder = ExistsAttachmentResponse.newBuilder();

		// validate table
		int tableId = 0;
		if (!Util.isEmpty(request.getTableName(), true)) {
			tableId = MTable.getTable_ID(request.getTableName());
		}
		if (tableId <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		// validate record
		int recordId = request.getRecordId();
		if (recordId <= 0) {
			if (Util.isEmpty(request.getRecordUuid(), true)) {
				recordId = RecordUtil.getIdFromUuid(request.getTableName(), request.getRecordUuid(), null);
			}
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}

		MAttachment attachment = MAttachment.get(context, tableId, recordId);
		if (attachment == null || attachment.getAD_Attachment_ID() <= 0) {
			// without attachment
			return builder;
		}

		int recordCount = new Query(
				context,
				I_AD_AttachmentReference.Table_Name,
				"AD_Attachment_ID = ?",
				null
			).setParameters(attachment.getAD_Attachment_ID())
			.count();

		return builder
			.setRecordCount(recordCount);
	}

}
