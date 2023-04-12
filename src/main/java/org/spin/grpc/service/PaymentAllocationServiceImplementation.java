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

import org.adempiere.core.domains.models.I_AD_Column;
import org.compiere.model.MBPartner;
import org.compiere.model.MLookupInfo;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.payment_allocation.BusinessPartner;
import org.spin.backend.grpc.form.payment_allocation.GetDifferenceRequest;
import org.spin.backend.grpc.form.payment_allocation.GetDifferenceResponse;
import org.spin.backend.grpc.form.payment_allocation.ListBusinessPartnersRequest;
import org.spin.backend.grpc.form.payment_allocation.ListChargesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListCurrenciesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListInvoicesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListInvoicesResponse;
import org.spin.backend.grpc.form.payment_allocation.ListOrganizationsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListPaymentsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListPaymentsResponse;
import org.spin.backend.grpc.form.payment_allocation.ListTransactionOrganizationsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListTransactionTypesRequest;
import org.spin.backend.grpc.form.payment_allocation.ProcessRequest;
import org.spin.backend.grpc.form.payment_allocation.ProcessResponse;
import org.spin.backend.grpc.form.payment_allocation.PaymentAllocationGrpc.PaymentAllocationImplBase;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Payment Allocation form
 */
public class PaymentAllocationServiceImplementation extends PaymentAllocationImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PaymentAllocationServiceImplementation.class);



	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listBusinessPartners(request);
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

	ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		// BPartner
		int columnId = 3499; // C_Invoice.C_BPartner_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}

	BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (builder == null) {
			return builder;
		}

		builder.setId(businessPartner.getC_BPartner_ID())
			.setUuid(
				ValueUtil.validateNull(businessPartner.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(businessPartner.getName())
			)
			// .setTaxId(
			// 	ValueUtil.validateNull(businessPartner.getTaxID())
			// )
			.setName(
				ValueUtil.validateNull(businessPartner.getName())
			)
			// .setDescription(
			// 	ValueUtil.validateNull(businessPartner.getDescription())
			// )
		;

		return builder;
	}



	@Override
	public void listOrganizations(ListOrganizationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listOrganizations(request);
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

	/**
	 * @param request
	 * @return
	 */
	private ListLookupItemsResponse.Builder listOrganizations(ListOrganizationsRequest request) {
		// Organization filter selection
		int columnId = 839; // C_Period.AD_Org_ID (needed to allow org 0)
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listCurrencies(ListCurrenciesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listCurrencies(request);
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

	private ListLookupItemsResponse.Builder listCurrencies(ListCurrenciesRequest request) {
		// Currency
		int columnId = 3505; // C_Invoice.C_Currency_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listTransactionTypes(ListTransactionTypesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listTransactionTypes(request);
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

	private ListLookupItemsResponse.Builder listTransactionTypes(ListTransactionTypesRequest request) {
		// APAR
		int columnId = 14082; // T_InvoiceGL.APAR
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListPaymentsResponse.Builder builder = ListPaymentsResponse.newBuilder();
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
	public void listInvoices(ListInvoicesRequest request, StreamObserver<ListInvoicesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListInvoicesResponse.Builder builder = ListInvoicesResponse.newBuilder();
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
	public void listCharges(ListChargesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listCharges(request);
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

	private ListLookupItemsResponse.Builder listCharges(ListChargesRequest request) {
		// Charge
		int columnId = 61804; // C_AllocationLine.C_Charge_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listTransactionOrganizations(ListTransactionOrganizationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listTransactionOrganizations(request);
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

	private ListLookupItemsResponse.Builder listTransactionOrganizations(ListTransactionOrganizationsRequest request) {
		// Organization to overwrite
		int columnId = 3863; // C_Period.AD_Org_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void getDifference(GetDifferenceRequest request, StreamObserver<GetDifferenceResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			GetDifferenceResponse.Builder builder = GetDifferenceResponse.newBuilder();
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
	public void process(ProcessRequest request, StreamObserver<ProcessResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessResponse.Builder builder = ProcessResponse.newBuilder();
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


}
