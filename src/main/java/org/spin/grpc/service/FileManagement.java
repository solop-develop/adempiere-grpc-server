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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_AttachmentReference;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MArchive;
import org.compiere.model.MAttachment;
import org.compiere.model.MClientInfo;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.file_management.Attachment;
import org.spin.backend.grpc.file_management.DeleteResourceReferenceRequest;
import org.spin.backend.grpc.file_management.ExistsAttachmentRequest;
import org.spin.backend.grpc.file_management.ExistsAttachmentResponse;
import org.spin.backend.grpc.file_management.FileManagementGrpc.FileManagementImplBase;
import org.spin.backend.grpc.file_management.GetAttachmentRequest;
import org.spin.backend.grpc.file_management.GetResourceReferenceRequest;
import org.spin.backend.grpc.file_management.GetResourceRequest;
import org.spin.backend.grpc.file_management.LoadResourceRequest;
import org.spin.backend.grpc.file_management.Resource;
import org.spin.backend.grpc.file_management.ResourceReference;
import org.spin.backend.grpc.file_management.ResourceType;
import org.spin.backend.grpc.file_management.SetAttachmentDescriptionRequest;
import org.spin.backend.grpc.file_management.SetResourceReferenceDescriptionRequest;
import org.spin.backend.grpc.file_management.SetResourceReferenceRequest;
import org.spin.base.util.FileUtil;
import org.spin.base.util.RecordUtil;
import org.spin.model.MADAttachmentReference;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of File Management (Attanchment/Image/Archive)
 */
public class FileManagement extends FileManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(FileManagement.class);
	
	public String tableName = I_C_Invoice.Table_Name;


	/**
	 * Validate client info exists and with configured file handler.
	 * @return clientInfo
	 */
	private static MClientInfo validateAndGetClientInfo() {
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
		if (clientInfo == null || clientInfo.getAD_Client_ID() < 0 || clientInfo.getFileHandler_ID() <= 0) {
			throw new AdempiereException("@FileHandler_ID@ @NotFound@");
		}
		return clientInfo;
	}



	/**
	 * Validate table exists.
	 * @return table
	 */
	private static MTable validateAndGetTable(String tableName) {
		// validate table
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		return table;
	}



	/**
	 * Validate attachment reference exists.
	 * @return attachment reference
	 */
	public static MADAttachmentReference validateAttachmentReferenceByUuid(String uuid) {
		if(Util.isEmpty(uuid, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_AttachmentReference_ID@");
		}
		MADAttachmentReference resourceReference = MADAttachmentReference.getByUuid(
			Env.getCtx(),
			uuid,
			null
		);
		if(resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}
		return resourceReference;
	}

	/**
	 * Validate attachment reference exists.
	 * @return attachment reference
	 */
	public static MADAttachmentReference validateAttachmentReferenceById(int id) {
		if(id <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_AttachmentReference_ID@");
		}
		MADAttachmentReference resourceReference = MADAttachmentReference.getById(
			Env.getCtx(),
			id,
			null
		);
		if(resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}
		return resourceReference;
	}

	@Override
	public void getResource(GetResourceRequest request, StreamObserver<Resource> responseObserver) {
		try {
			getResource(request.getId(), request.getResourceName(), responseObserver);
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
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
	private void getResource(int resourceId, String resourceName, StreamObserver<Resource> responseObserver) throws Exception {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		if (!AttachmentUtil.getInstance().isValidForClient(clientId)) {
			responseObserver.onError(new AdempiereException("@NotFound@"));
			return;
		}

		//	Validate by name
		int attachmentReferenceId = resourceId;
		byte[] data = AttachmentUtil.getInstance()
			.withClientId(clientId)
			.withAttachmentReferenceId(attachmentReferenceId)
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
			Resource builder = Resource.newBuilder()
				.setData(ByteString.copyFrom(buffer, 0, length))
				.build()
			;
			responseObserver.onNext(
				builder
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
			ResourceReference.Builder resourceReference = getResourceReference(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
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
	private ResourceReference.Builder getResourceReference(GetResourceReferenceRequest request) {
		// validate and get client info with configured file handler
		MClientInfo clientInfo = validateAndGetClientInfo();

		MADAttachmentReference resourceReference = null;
		if (request.getId() > 0) {
			resourceReference = MADAttachmentReference.getById(Env.getCtx(), request.getId(), null);
		}
		if (resourceReference == null) {
			resourceReference = MADAttachmentReference.getByAttachmentId(
				Env.getCtx(),
				clientInfo.getFileHandler_ID(),
				request.getId(),
				request.getResourceName(),
				null
			);
		}
		if (resourceReference == null && !Util.isEmpty(request.getResourceName(), true)) {
			String resourceUuid = getResourceUuidFromName(request.getResourceName());
			resourceReference = MADAttachmentReference.getByUuid(Env.getCtx(), resourceUuid, null);
		}

		int imageId = request.getImageId();
		if (resourceReference == null && imageId > 0) {
			resourceReference = MADAttachmentReference.getByImageId(
				Env.getCtx(),
				clientInfo.getFileHandler_ID(),
				imageId,
				null
			);
		}

		int archiveId = request.getArchiveId();
		if (resourceReference == null && archiveId > 0) {
			resourceReference = MADAttachmentReference.getByArchiveId(
				Env.getCtx(),
				clientInfo.getFileHandler_ID(),
				archiveId,
				null
			);
		}

		if (resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}

		return convertResourceReference(
			resourceReference
		);
	}


	@Override
	public StreamObserver<LoadResourceRequest> loadResource(StreamObserver<ResourceReference> responseObserver) {
		AtomicReference<String> resourceUuid = new AtomicReference<>();
		AtomicReference<ByteBuffer> buffer = new AtomicReference<>();
		return new StreamObserver<LoadResourceRequest>() {
			@Override
			public void onNext(LoadResourceRequest fileUploadRequest) {
				try {
					if(resourceUuid.get() == null) {
						// validate and get client info with configured file handler
						validateAndGetClientInfo();

						// validate and get attachment reference by uuid
						MADAttachmentReference resourceReference = validateAttachmentReferenceById(fileUploadRequest.getId());

						resourceUuid.set(resourceReference.getUUID());
						BigDecimal size = ValueManager.getBigDecimalFromValue(
							fileUploadRequest.getFileSize()
						);
						if (size != null && fileUploadRequest.getData() != null) {
							byte[] initByte = new byte[size.intValue()];
							buffer.set(ByteBuffer.wrap(initByte));
							byte[] bytes = fileUploadRequest.getData().toByteArray();
							buffer.set(buffer.get().put(bytes));
						}
					} else if (buffer.get() != null){
						byte[] bytes = fileUploadRequest.getData().toByteArray();
						buffer.set(buffer.get().put(bytes));
					}
				} catch (Exception e) {
					this.onError(e);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				log.severe(throwable.getLocalizedMessage());
				throwable.printStackTrace();
				responseObserver.onError(throwable);
			}

			@Override
			public void onCompleted() {
				try {
					// validate and get client info with configured file handler
					MClientInfo clientInfo = validateAndGetClientInfo();

					// validate and get attachment reference by uuid
					MADAttachmentReference resourceReference = validateAttachmentReferenceByUuid(resourceUuid.get());

					byte[] data = buffer.get().array();
					AttachmentUtil.getInstance()
						.clear()
						.withAttachmentReferenceId(resourceReference.getAD_AttachmentReference_ID())
						.withFileName(resourceReference.getFileName())
						.withClientId(clientInfo.getAD_Client_ID())
						.withData(data)
						.saveAttachment();

					MADAttachmentReference.resetAttachmentReferenceCache(clientInfo.getFileHandler_ID(), resourceReference);
					ResourceReference.Builder response = convertResourceReference(resourceReference);

					responseObserver.onNext(response.build());
					responseObserver.onCompleted();
				} catch (Exception e) {
					this.onError(e);
				}
			}
		};
	}



	@Override
	public void setAttachmentDescription(SetAttachmentDescriptionRequest request, StreamObserver<Attachment> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Attachment.Builder attachment = setAttachmentDescription(request);
			responseObserver.onNext(attachment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	Attachment.Builder setAttachmentDescription(SetAttachmentDescriptionRequest request) {
		if(request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Attachment_ID@");
		}
		int attachmentId = request.getId();
		MAttachment attachment = new MAttachment(Env.getCtx(), attachmentId, null);
		if(attachment == null || attachment.getAD_Attachment_ID() <= 0) {
			throw new AdempiereException("@AD_Attachment_ID@ @NotFound@");
		}

		attachment.setTextMsg(request.getTextMessage());
		attachment.saveEx();

		return convertAttachment(attachment);
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
			e.printStackTrace();
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
		// validate and get table
		MTable table = validateAndGetTable(request.getTableName());

		int recordId = request.getRecordId();
		if (!RecordUtil.isValidId(recordId, table.getAccessLevel())) {
			return Attachment.newBuilder();
		}

		MAttachment attachment = MAttachment.get(Env.getCtx(), table.getAD_Table_ID(), recordId);
		return convertAttachment(attachment);
	}


	/**
	 * Convert resource
	 * @param reference
	 * @return
	 */
	public static ResourceReference.Builder convertResourceReference(MADAttachmentReference reference) {
		ResourceReference.Builder builder = ResourceReference.newBuilder();
		if (reference == null) {
			return builder;
		}
		builder
			.setId(reference.getAD_AttachmentReference_ID())
			.setName(
				ValueManager.validateNull(reference.getFileName())
			)
			.setFileName(
				ValueManager.validateNull(reference.getValidFileName())
			)
			.setDescription(
				ValueManager.validateNull(reference.getDescription())
			)
			.setTextMessage(
				ValueManager.validateNull(reference.getTextMsg())
			)
			.setContentType(
				ValueManager.validateNull(
					MimeType.getMimeType(reference.getFileName())
				)
			)
			.setFileSize(
				ValueManager.getValueFromBigDecimal(
					reference.getFileSize()
				)
			)
			.setCreated(
				ValueManager.getTimestampFromDate(reference.getCreated())
			)
			.setUpdated(
				ValueManager.getTimestampFromDate(reference.getUpdated())
			)
		;

		if(reference.getAD_Image_ID() > 0) {
			builder.setResourceType(ResourceType.IMAGE)
				.setResourceId(reference.getAD_Image_ID())
			;
		} else if(reference.getAD_Archive_ID() > 0) {
			builder.setResourceType(ResourceType.ARCHIVE)
				.setResourceId(reference.getAD_Archive_ID())
			;
		} else {
			builder.setResourceType(ResourceType.ATTACHMENT)
				.setResourceId(reference.getAD_Attachment_ID())
			;
		}

		return builder;
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
			.setId(attachment.getAD_Attachment_ID())
			.setTitle(ValueManager.validateNull(attachment.getTitle()))
			.setTextMessage(
				ValueManager.validateNull(attachment.getTextMsg())
			)
		;

		// validate client info with configured file handler
		MClientInfo clientInfo = MClientInfo.get(attachment.getCtx());
		if (clientInfo == null || clientInfo.getAD_Client_ID() < 0 || clientInfo.getFileHandler_ID() <= 0) {
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
			ResourceReference.Builder builderReference = convertResourceReference(attachmentReference);
			builder.addResourceReferences(builderReference);
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ResourceReference.Builder setResourceReference(SetResourceReferenceRequest request) {
		// validate and get client info with configured file handler
		MClientInfo clientInfo = validateAndGetClientInfo();

		// validate file name
		final String fileName = request.getFileName();
		if (Util.isEmpty(fileName, true)) {
			throw new AdempiereException("@Name@ @Attachment@ @NotFound@");
		}
		if (!MimeType.isValidMimeType(fileName)) {
			throw new AdempiereException("@Error@ @FileInvalidExtension@");
		}

		AtomicReference<MADAttachmentReference> attachmentReferenceAtomic = new AtomicReference<MADAttachmentReference>();
		Trx.run(transactionName -> {
			MADAttachmentReference attachmentReference = new MADAttachmentReference(Env.getCtx(), 0, transactionName);
			attachmentReference.setFileHandler_ID(clientInfo.getFileHandler_ID());
			attachmentReference.setDescription(request.getDescription());
			attachmentReference.setTextMsg(request.getTextMessage());
			attachmentReference.setFileName(fileName);
			// attachmentReference.setFileSize(
			// 	BigDecimal.valueOf(request.getFileSize())
			// );

			if(request.getResourceType() == ResourceType.IMAGE) {
				if (!FileUtil.isValidImage(fileName)) {
					throw new AdempiereException("@Error@ @FileInvalidExtension@. @attach.image@");	
				}
				MImage image = MImage.get(Env.getCtx(), request.getId(), transactionName);
				if (image == null) {
					image = new MImage(Env.getCtx(), 0, transactionName);
				}
				image.setName(fileName);
				image.setDescription(request.getDescription());
				image.saveEx();
				MADAttachmentReference attachmentReferenceByImage = MADAttachmentReference.getByImageId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					image.getAD_Image_ID(),
					transactionName
				);
				if (attachmentReferenceByImage != null) {
					attachmentReference = attachmentReferenceByImage;
				} else {
					attachmentReference.setAD_Image_ID(image.getAD_Image_ID());
				}
			} else if(request.getResourceType() == ResourceType.ARCHIVE) {
				MArchive archive = new MArchive(Env.getCtx(), request.getId(), transactionName);
				archive.setName(fileName);
				archive.setDescription(request.getDescription());
				archive.saveEx();
				MADAttachmentReference attachmentReferenceByArchive = MADAttachmentReference.getByArchiveId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					archive.getAD_Archive_ID(),
					transactionName
				);
				if (attachmentReferenceByArchive != null) {
					attachmentReference = attachmentReferenceByArchive;
				} else {
					attachmentReference.setAD_Archive_ID(archive.getAD_Archive_ID());
				}
			} else {
				// validate and get table
				MTable table = validateAndGetTable(request.getTableName());

				// validate record
				int recordId = request.getRecordId();
				if (!RecordUtil.isValidId(recordId, table.getAccessLevel())) {
					throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
				}
				final int recordIdentifier = recordId;

				MAttachment attachment = new MAttachment(Env.getCtx(), request.getId(), transactionName);
				if (attachment.getAD_Attachment_ID() <= 0) {
					attachment = new MAttachment(Env.getCtx(), table.getAD_Table_ID(), recordIdentifier, transactionName);
				}
				if (attachment.getAD_Attachment_ID() <= 0) {
					/**
					 * TODO: `IsDirectLoad` disables `ModelValidator`, `beforeSave` and the `MAttachment.afterSave`
					 * which calls the `MAttachment.saveLOBData` method but generates an error
					 * (Null Pointer Exception) since `items` is initialized to null, when it should
					 * be initialized with a `new ArrayList<MAttachmentEntry>()`.
					 */
					attachment.setIsDirectLoad(true);
					attachment.saveEx();
				}
				attachmentReference.setAD_Attachment_ID(attachment.getAD_Attachment_ID());
			}

			attachmentReference.saveEx();
			//	Remove from cache
			MADAttachmentReference.resetAttachmentReferenceCache(clientInfo.getFileHandler_ID(), attachmentReference);
			attachmentReferenceAtomic.set(attachmentReference);
		});

		ResourceReference.Builder builder = convertResourceReference(attachmentReferenceAtomic.get());

		return builder;
	}



	@Override
	public void setResourceReferenceDescription(SetResourceReferenceDescriptionRequest request, StreamObserver<ResourceReference> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceReference.Builder resourceReference = setResourceReferenceDescription(request);
			responseObserver.onNext(resourceReference.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	ResourceReference.Builder setResourceReferenceDescription(SetResourceReferenceDescriptionRequest request) {
		// validate and get client info with configured file handler
		MClientInfo clientInfo = validateAndGetClientInfo();

		MADAttachmentReference resourceReference = null;
		if (request.getId() > 0) {
			resourceReference = MADAttachmentReference.getById(Env.getCtx(), request.getId(), null);
		}
		if (resourceReference == null && !Util.isEmpty(request.getFileName(), true)) {
			resourceReference = MADAttachmentReference.getByUuid(Env.getCtx(), request.getFileName(), null);
		}

		if (resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}

		resourceReference.setDescription(request.getDescription());
		resourceReference.setTextMsg(request.getTextMessage());
		resourceReference.saveEx();

		// Update parent description
		if (resourceReference.getAD_Image_ID() > 0) {
			MImage image = MImage.get(Env.getCtx(), resourceReference.getAD_Image_ID(), null);
			image.setDescription(request.getDescription());
			image.saveEx();
		} else if (resourceReference.getAD_Archive_ID() > 0) {
			MArchive archive = new MArchive(Env.getCtx(), resourceReference.getAD_Archive_ID(), null);
			archive.setDescription(request.getDescription());
			archive.setHelp(request.getTextMessage());
			archive.saveEx();
		}

		// reset cache
		MADAttachmentReference.resetAttachmentReferenceCache(clientInfo.getFileHandler_ID(), resourceReference);

		ResourceReference.Builder builder = convertResourceReference(resourceReference);

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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	Empty.Builder deleteResourceReference(DeleteResourceReferenceRequest request) throws Exception {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_AttachmentReference_ID@");
		}

		MADAttachmentReference resourceReference = null;
		if (request.getId() > 0) {
			resourceReference = MADAttachmentReference.getById(Env.getCtx(), request.getId(), null);
		}
		if (resourceReference == null || resourceReference.getAD_AttachmentReference_ID() <= 0) {
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
		}

		// validate and get client info with configured file handler
		MClientInfo clientInfo = validateAndGetClientInfo();

		// when delete attachmet reference, the `resourceReference` is clean values
		final int imageId = resourceReference.getAD_Image_ID();
		final int archiveId = resourceReference.getAD_Archive_ID();

		// delete file on cloud (s3, nexcloud)
		AttachmentUtil.getInstance()
			.clear()
			.withAttachmentReferenceId(resourceReference.getAD_AttachmentReference_ID())
			.withFileName(resourceReference.getFileName())
			.withClientId(clientInfo.getAD_Client_ID())
			.deleteAttachment();

		if (imageId > 0) {
			MImage image = MImage.get(Env.getCtx(), imageId, null);
			if (image != null && image.getAD_Image_ID() > 0) {
				image.deleteEx(false);
			}
		} else if (archiveId > 0) {
			MArchive archive = new MArchive(Env.getCtx(), archiveId, null);
			if (archive != null && archive.getAD_Archive_ID() > 0) {
				archive.deleteEx(false);
			}
		}

		// reset cache
		MADAttachmentReference.resetAttachmentReferenceCache(clientInfo.getFileHandler_ID(), resourceReference);

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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ExistsAttachmentResponse.Builder existsAttachment(ExistsAttachmentRequest request) {
		ExistsAttachmentResponse.Builder builder = ExistsAttachmentResponse.newBuilder();

		// validate client info with configured file handler
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
		if (clientInfo == null || clientInfo.getAD_Client_ID() < 0 || clientInfo.getFileHandler_ID() <= 0) {
			return builder;
		}

		// validate and get table
		MTable table = validateAndGetTable(request.getTableName());

		// validate record
		int recordId = request.getRecordId();
		if (!RecordUtil.isValidId(recordId, table.getAccessLevel())) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}

		MAttachment attachment = MAttachment.get(Env.getCtx(), table.getAD_Table_ID(), recordId);
		if (attachment == null || attachment.getAD_Attachment_ID() <= 0) {
			// without attachment
			return builder;
		}

		int recordCount = new Query(
				Env.getCtx(),
				I_AD_AttachmentReference.Table_Name,
				"AD_Attachment_ID = ?",
				null
			).setParameters(attachment.getAD_Attachment_ID())
			.setClient_ID()
			.count();

		return builder
			.setRecordCount(recordCount);
	}

}
