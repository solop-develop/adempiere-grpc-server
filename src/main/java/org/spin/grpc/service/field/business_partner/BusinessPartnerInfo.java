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
package org.spin.grpc.service.field.business_partner;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerInfoServiceGrpc.BusinessPartnerInfoServiceImplBase;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerAddressLocationsRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerAddressLocationsResponse;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerContactsRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerContactsResponse;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnersInfoRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnersInfoResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class BusinessPartnerInfo extends BusinessPartnerInfoServiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(BusinessPartnerInfo.class);


	@Override
	public void listBusinessPartnersInfo(ListBusinessPartnersInfoRequest request, StreamObserver<ListBusinessPartnersInfoResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListBusinessPartnersInfoResponse.Builder entityValueList = BusinessPartnerLogic.listBusinessPartnersInfo(request);
			responseObserver.onNext(entityValueList.build());
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
	public void listBusinessPartnerContacts(ListBusinessPartnerContactsRequest request, StreamObserver<ListBusinessPartnerContactsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListBusinessPartnerContactsResponse.Builder entityValueList = BusinessPartnerLogic.listBusinessPartnerContacts(request);
			responseObserver.onNext(entityValueList.build());
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
	public void listBusinessPartnerAddressLocations(ListBusinessPartnerAddressLocationsRequest request, StreamObserver<ListBusinessPartnerAddressLocationsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListBusinessPartnerAddressLocationsResponse.Builder entityValueList = BusinessPartnerLogic.listBusinessPartnerAddressLocations(request);
			responseObserver.onNext(entityValueList.build());
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
