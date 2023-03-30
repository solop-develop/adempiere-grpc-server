/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.BankStatementMatchGrpc.BankStatementMatchImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Bank Statement Match form
 */
public class BankStatementMatchServiceImplementation extends BankStatementMatchImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(BankStatementMatchServiceImplementation.class);

	@Override
	public void listImportedBankMovements(ListImportedBankMovementsRequest request, StreamObserver<ListImportedBankMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListImportedBankMovementsResponse.Builder builder = listImportedBankMovements(request);
			responseObserver.onNext(builder.build());
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

	private ListImportedBankMovementsResponse.Builder listImportedBankMovements(ListImportedBankMovementsRequest request) {
		// validate key values
		if (request.getBankAccountId() == 0 && Util.isEmpty(request.getBankAccountUuid(), true)) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}

		ListImportedBankMovementsResponse.Builder builderList = ListImportedBankMovementsResponse.newBuilder();

		return builderList;
	}

	@Override
	public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPaymentsResponse.Builder builder = listPayments(request);
			responseObserver.onNext(builder.build());
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

	private ListPaymentsResponse.Builder listPayments(ListPaymentsRequest request) {
		// validate key values
		if (request.getBankAccountId() == 0 && Util.isEmpty(request.getBankAccountUuid(), true)) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}

		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder();

		return builderList;
	}


	@Override
	public void listMatchingMovements(ListMatchingMovementsRequest request, StreamObserver<ListMatchingMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListMatchingMovementsResponse.Builder builder = listMatchingMovements(request);
			responseObserver.onNext(builder.build());
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

	private ListMatchingMovementsResponse.Builder listMatchingMovements(ListMatchingMovementsRequest request) {
		// validate key values
		if (request.getBankAccountId() == 0 && Util.isEmpty(request.getBankAccountUuid(), true)) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}

		ListMatchingMovementsResponse.Builder builderList = ListMatchingMovementsResponse.newBuilder();

		return builderList;
	}


	@Override
	public void processMovements(ProcessMovementsRequest request, StreamObserver<ProcessMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessMovementsResponse.Builder builder = processMovements(request);
			responseObserver.onNext(builder.build());
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

	private ProcessMovementsResponse.Builder processMovements(ProcessMovementsRequest request) {
		ProcessMovementsResponse.Builder builderList = ProcessMovementsResponse.newBuilder();

		return builderList;
	}

}
