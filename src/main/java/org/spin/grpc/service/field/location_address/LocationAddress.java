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
package org.spin.grpc.service.field.location_address;

import org.compiere.util.CLogger;
import org.spin.backend.grpc.field.location_address.Address;
import org.spin.backend.grpc.field.location_address.Country;
import org.spin.backend.grpc.field.location_address.CreateAddressRequest;
import org.spin.backend.grpc.field.location_address.GetAddressRequest;
import org.spin.backend.grpc.field.location_address.GetCountryRequest;
import org.spin.backend.grpc.field.location_address.ListCitiesRequest;
import org.spin.backend.grpc.field.location_address.ListCitiesResponse;
import org.spin.backend.grpc.field.location_address.ListCountriesRequest;
import org.spin.backend.grpc.field.location_address.ListCountriesResponse;
import org.spin.backend.grpc.field.location_address.ListRegionsRequest;
import org.spin.backend.grpc.field.location_address.ListRegionsResponse;
import org.spin.backend.grpc.field.location_address.UpdateAddressRequest;
import org.spin.backend.grpc.field.location_address.LocationAddressGrpc.LocationAddressImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Location Address field
 */
public class LocationAddress extends LocationAddressImplBase {
		/**	Logger			*/
	private CLogger log = CLogger.getCLogger(LocationAddress.class);



	@Override
	public void listCountries(ListCountriesRequest request, StreamObserver<ListCountriesResponse> responseObserver) {
		try {
			ListCountriesResponse.Builder entityValueList = LocationAddressLogic.listCountries(
				request
			);
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
	public void getCountry(GetCountryRequest request, StreamObserver<Country> responseObserver) {
		try {
			Country.Builder entityValueList = LocationAddressLogic.getCountry(
				request
			);
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
	public void listRegions(ListRegionsRequest request, StreamObserver<ListRegionsResponse> responseObserver) {
		try {
			ListRegionsResponse.Builder entityValueList = LocationAddressLogic.listRegions(
				request
			);
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
	public void listCities(ListCitiesRequest request, StreamObserver<ListCitiesResponse> responseObserver) {
		try {
			ListCitiesResponse.Builder entityValueList = LocationAddressLogic.listCities(
				request
			);
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
	public void getAddress(GetAddressRequest request, StreamObserver<Address> responseObserver) {
		try {
			Address.Builder entityValueList = LocationAddressLogic.getAddress(
				request
			);
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
	public void createAddress(CreateAddressRequest request, StreamObserver<Address> responseObserver) {
		try {
			Address.Builder entityValueList = LocationAddressLogic.createAddress(
				request
			);
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
	public void updateAddress(UpdateAddressRequest request, StreamObserver<Address> responseObserver) {
		try {
			Address.Builder entityValueList = LocationAddressLogic.updateAddress(
				request
			);
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
