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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_AttachmentReference;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.import_file_loader.GetImportFromatRequest;
import org.spin.backend.grpc.form.import_file_loader.ImportFileLoaderGrpc.ImportFileLoaderImplBase;
import org.spin.backend.grpc.form.import_file_loader.ImportFormat;
import org.spin.grpc.logic.ImportFileLoaderServiceLogic;
import org.spin.model.MADAttachmentReference;
import org.spin.util.AttachmentUtil;
import org.spin.backend.grpc.form.import_file_loader.ListCharsetsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListFilePreviewRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.LoadImportFileRequest;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportRequest;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportResponse;
import org.spin.backend.grpc.form.import_file_loader.ResourceReference;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;


/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Import File Loader form
 */
public class ImportFileLoaderServiceImplementation extends ImportFileLoaderImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ImportFileLoaderServiceImplementation.class);


	@Override
	public void listCharsets(ListCharsetsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = ImportFileLoaderServiceLogic.listCharsets(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void listImportFormats(ListImportFormatsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = ImportFileLoaderServiceLogic.listImportFormats(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void getImportFromat(GetImportFromatRequest request, StreamObserver<ImportFormat> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ImportFormat.Builder builderList = ImportFileLoaderServiceLogic.getImportFromat(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public StreamObserver<LoadImportFileRequest> loadImportFile(StreamObserver<ResourceReference> responseObserver) {
		AtomicReference<String> resourceUuid = new AtomicReference<>();
		AtomicReference<ByteBuffer> buffer = new AtomicReference<>();

		return new StreamObserver<LoadImportFileRequest>() {
			@Override
			public void onNext(LoadImportFileRequest fileUploadRequest) {
				try {
					if(resourceUuid.get() == null) {
						resourceUuid.set(fileUploadRequest.getResourceUuid());
						BigDecimal size = ValueUtil.getBigDecimalFromDecimal(fileUploadRequest.getFileSize());
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
				} catch (Exception e){
					e.printStackTrace();
					this.onError(e);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				responseObserver.onError(throwable);
			}

			@Override
			public void onCompleted() {
				try {
					MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
					if (clientInfo == null || clientInfo.getFileHandler_ID() <= 0) {
						throw new AdempiereException("@FileHandler_ID@ @NotFound@");
					}
					if(resourceUuid.get() != null && buffer.get() != null) {
						MADAttachmentReference resourceReference = (MADAttachmentReference) RecordUtil.getEntity(
							Env.getCtx(),
							I_AD_AttachmentReference.Table_Name,
							resourceUuid.get(),
							-1,
							null
						);
						if (resourceReference != null) {
							byte[] data = buffer.get().array();
							AttachmentUtil.getInstance()
								.clear()
								.withAttachmentReferenceId(resourceReference.getAD_AttachmentReference_ID())
								.withFileName(resourceReference.getFileName())
								.withClientId(clientInfo.getAD_Client_ID())
								.withData(data)
								.saveAttachment()
							;
							MADAttachmentReference.resetAttachmentReferenceCache(clientInfo.getFileHandler_ID(), resourceReference);
						}
					}
					ResourceReference response = ResourceReference.newBuilder()
						// .setStatus(status)
						.build();
					responseObserver.onNext(response);
					responseObserver.onCompleted();
				} catch (Exception e) {
					e.printStackTrace();
					throw new AdempiereException(e);
				}
			}
		};
	}



	@Override
	public void listFilePreview(ListFilePreviewRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListEntitiesResponse.Builder builderList = ImportFileLoaderServiceLogic.listFilePreview(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void processImport(ProcessImportRequest request, StreamObserver<ProcessImportResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessImportResponse.Builder builderList = ImportFileLoaderServiceLogic.processImport(request);
			responseObserver.onNext(builderList.build());
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

}
