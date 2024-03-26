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
package org.spin.grpc.service.field.product;

import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.field.product.GetLastPriceListVersionRequest;
import org.spin.backend.grpc.field.product.ListAttributeSetInstancesRequest;
import org.spin.backend.grpc.field.product.ListAttributeSetsRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesResponse;
import org.spin.backend.grpc.field.product.ListPricesListVersionsRequest;
import org.spin.backend.grpc.field.product.ListProductCategoriesRequest;
import org.spin.backend.grpc.field.product.ListProductClasessRequest;
import org.spin.backend.grpc.field.product.ListProductClassificationsRequest;
import org.spin.backend.grpc.field.product.ListProductGroupsRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoResponse;
import org.spin.backend.grpc.field.product.ListRelatedProductsRequest;
import org.spin.backend.grpc.field.product.ListRelatedProductsResponse;
import org.spin.backend.grpc.field.product.ListSubstituteProductsRequest;
import org.spin.backend.grpc.field.product.ListSubstituteProductsResponse;
import org.spin.backend.grpc.field.product.ListVendorPurchasesRequest;
import org.spin.backend.grpc.field.product.ListVendorPurchasesResponse;
import org.spin.backend.grpc.field.product.ListVendorsRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksResponse;
import org.spin.backend.grpc.field.product.ListWarehousesRequest;
import org.spin.backend.grpc.field.product.ProductInfoServiceGrpc.ProductInfoServiceImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Product Info field
 */
public class ProductInfo extends ProductInfoServiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ProductInfo.class);


	@Override
	public void listWarehouses(ListWarehousesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listWarehouses(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void getLastPriceListVersion(GetLastPriceListVersionRequest request, StreamObserver<LookupItem> responseObserver) {
		try {
			LookupItem.Builder buildersList = ProductInfoLogic.getLastPriceListVersion(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listPricesListVersions(ListPricesListVersionsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listPricesListVersions(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listAttributeSets(ListAttributeSetsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listAttributeSets(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listAttributeSetInstances(ListAttributeSetInstancesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listAttributeSetInstances(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listProductCategories(ListProductCategoriesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listProductCategories(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listProductGroups(ListProductGroupsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listProductGroups(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listProductClasses(ListProductClasessRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listProductClasses(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listProductClassifications(ListProductClassificationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listProductClassifications(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listVendors(ListVendorsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ProductInfoLogic.listVendors(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listProductsInfo(ListProductsInfoRequest request, StreamObserver<ListProductsInfoResponse> responseObserver) {
		try {
			ListProductsInfoResponse.Builder buildersList = ProductInfoLogic.listProductsInfo(request);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listWarehouseStocks(ListWarehouseStocksRequest request, StreamObserver<ListWarehouseStocksResponse> responseObserver) {
		try {
			ListWarehouseStocksResponse.Builder buildersList = ProductInfoLogic.listWarehouseStocks(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listSubstituteProducts(ListSubstituteProductsRequest request, StreamObserver<ListSubstituteProductsResponse> responseObserver) {
		try {
			ListSubstituteProductsResponse.Builder buildersList = ProductInfoLogic.listSubstituteProducts(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listRelatedProducts(ListRelatedProductsRequest request, StreamObserver<ListRelatedProductsResponse> responseObserver) {
		try {
			ListRelatedProductsResponse.Builder buildersList = ProductInfoLogic.listRelatedProducts(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listAvailableToPromises(ListAvailableToPromisesRequest request, StreamObserver<ListAvailableToPromisesResponse> responseObserver) {
		try {
			ListAvailableToPromisesResponse.Builder buildersList = ProductInfoLogic.listAvailableToPromises(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listVendorPurchases(ListVendorPurchasesRequest request, StreamObserver<ListVendorPurchasesResponse> responseObserver) {
		try {
			ListVendorPurchasesResponse.Builder buildersList = ProductInfoLogic.listVendorPurchases(
				request
			);
			responseObserver.onNext(
				buildersList.build()
			);
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
