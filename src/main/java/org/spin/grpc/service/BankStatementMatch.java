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
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.BankStatementMatchGrpc.BankStatementMatchImplBase;
import org.spin.backend.grpc.form.bank_statement_match.GetBankStatementRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankAccountsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListBusinessPartnersRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListResultMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListResultMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListSearchModesRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsResponse;
import org.spin.form.bank_statement_match.BankStatementMatchServiceLogic;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Bank Statement Match form
 */
public class BankStatementMatch extends BankStatementMatchImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(BankStatementMatch.class);



	@Override
	public void getBankStatement(GetBankStatementRequest request, StreamObserver<BankStatement> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			BankStatement.Builder builderList = BankStatementMatchServiceLogic.getBankStatement(request);
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
	public void listBankStatements(ListBankStatementsRequest request, StreamObserver<ListBankStatementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListBankStatementsResponse.Builder builder = BankStatementMatchServiceLogic.listBankStatements(request);
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
	public void listBankAccounts(ListBankAccountsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = BankStatementMatchServiceLogic.listBankAccounts(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = BankStatementMatchServiceLogic.listBusinessPartners(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void listSearchModes(ListSearchModesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = BankStatementMatchServiceLogic.listSearchModes(request);
			responseObserver.onNext(builderList.build());
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



	@Override
	public void listImportedBankMovements(ListImportedBankMovementsRequest request, StreamObserver<ListImportedBankMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListImportedBankMovementsResponse.Builder builder = BankStatementMatchServiceLogic.listImportedBankMovements(request);
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



	@Override
	public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPaymentsResponse.Builder builder = BankStatementMatchServiceLogic.listPayments(request);
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



	@Override
	public void listMatchingMovements(ListMatchingMovementsRequest request, StreamObserver<ListMatchingMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListMatchingMovementsResponse.Builder builder = BankStatementMatchServiceLogic.listMatchingMovements(request);
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
	public void listResultMovements(ListResultMovementsRequest request, StreamObserver<ListResultMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListResultMovementsResponse.Builder builder = BankStatementMatchServiceLogic.listResultMovements(request);
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
	public void matchPayments(MatchPaymentsRequest request, StreamObserver<MatchPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			MatchPaymentsResponse.Builder builder = BankStatementMatchServiceLogic.matchPayments(request);
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
	public void unmatchPayments(UnmatchPaymentsRequest request, StreamObserver<UnmatchPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			UnmatchPaymentsResponse.Builder builder = BankStatementMatchServiceLogic.unmatchPayments(request);
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
	public void processMovements(ProcessMovementsRequest request, StreamObserver<ProcessMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessMovementsResponse.Builder builder = BankStatementMatchServiceLogic.processMovements(request);
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

}
