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
package org.spin.grpc.service;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.import_file_loader.GetImportFromatRequest;
import org.spin.backend.grpc.form.import_file_loader.ImportFileLoaderGrpc.ImportFileLoaderImplBase;
import org.spin.form.import_file_loader.ImportFileLoaderServiceLogic;
import org.spin.backend.grpc.form.import_file_loader.ImportFormat;
import org.spin.backend.grpc.form.import_file_loader.ListCharsetsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListFilePreviewRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportProcessesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesResponse;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsRequest;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;


/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Import File Loader form
 */
public class ImportFileLoader extends ImportFileLoaderImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ImportFileLoader.class);


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
	public void listImportTables(ListImportTablesRequest request, StreamObserver<ListImportTablesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListImportTablesResponse.Builder builderList = ImportFileLoaderServiceLogic.listImportTables(request);
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



//	@Override
//	public void listClientImportFormats(ListClientImportFormatsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
//		try {
//			if (request == null) {
//				throw new AdempiereException("Object Request Null");
//			}
//
//			ListLookupItemsResponse.Builder builderList = ImportFileLoaderServiceLogic.listClientImportFormats(request);
//			responseObserver.onNext(builderList.build());
//			responseObserver.onCompleted();
//		} catch (Exception e) {
//			log.severe(e.getLocalizedMessage());
//			e.printStackTrace();
//			responseObserver.onError(Status.INTERNAL
//				.withDescription(e.getLocalizedMessage())
//				.withCause(e)
//				.asRuntimeException()
//			);
//		}
//	}



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
	public void saveRecords(SaveRecordsRequest request, StreamObserver<SaveRecordsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Save Records Request Null");
			}

			SaveRecordsResponse.Builder builder = ImportFileLoaderServiceLogic.saveRecords(request);
			responseObserver.onNext(builder.build());
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
	public void listImportProcesses(ListImportProcessesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = ImportFileLoaderServiceLogic.listImportProcesses(request);
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
