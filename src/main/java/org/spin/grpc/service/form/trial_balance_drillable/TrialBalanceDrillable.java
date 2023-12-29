/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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

package org.spin.grpc.service.form.trial_balance_drillable;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ExportRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ExportResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ListAccoutingKeysRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListBudgetsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ListOrganizationsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListPeriodsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListReportCubesRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.TrialBalanceDrillableGrpc.TrialBalanceDrillableImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Trial Balance Drillable Report
 */
public class TrialBalanceDrillable extends TrialBalanceDrillableImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(TrialBalanceDrillable.class);



	@Override
	public void listOrganizations(ListOrganizationsRequest request,
			StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Organizations Request Null");
			}
			responseObserver.onNext(ListLookupItemsResponse.newBuilder().build());
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



	@Override
	public void listBudgets(ListBudgetsRequest request,
			StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Budgets Request Null");
			}
			responseObserver.onNext(ListLookupItemsResponse.newBuilder().build());
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



	@Override
	public void listPeriods(ListPeriodsRequest request,
			StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Periods Request Null");
			}
			responseObserver.onNext(ListLookupItemsResponse.newBuilder().build());
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



	@Override
	public void listAccoutingKeys(ListAccoutingKeysRequest request,
			StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Accoutings Request Null");
			}
			responseObserver.onNext(ListLookupItemsResponse.newBuilder().build());
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



	@Override
	public void listReportCubes(ListReportCubesRequest request,
			StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Report Cube Request Null");
			}
			responseObserver.onNext(ListLookupItemsResponse.newBuilder().build());
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



	@Override
	public void listFactAcctSummary(ListFactAcctSummaryRequest request,
			StreamObserver<ListFactAcctSummaryResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Fact Acct Summary Request Null");
			}
			responseObserver.onNext(ListFactAcctSummaryResponse.newBuilder().build());
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



	@Override
	public void export(ExportRequest request,
			StreamObserver<ExportResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Export Request Null");
			}
			responseObserver.onNext(ExportResponse.newBuilder().build());
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

}
