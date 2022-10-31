/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_InvoiceLine;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_AttributeSet;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_AttributeUse;
import org.compiere.model.I_M_AttributeValue;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Requisition;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeUse;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.model.X_M_Attribute;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.base.util.ContextManager;
import org.spin.base.util.DictionaryUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;

import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.material_management.GetProductAttributeSetRequest;
import org.spin.backend.grpc.material_management.ListProductAttributeSetInstancesRequest;
import org.spin.backend.grpc.material_management.ListProductAttributeSetInstancesResponse;
import org.spin.backend.grpc.material_management.ListProductStorageRequest;
import org.spin.backend.grpc.material_management.MaterialManagementGrpc.MaterialManagementImplBase;
import org.spin.backend.grpc.material_management.ProductAttribute;
import org.spin.backend.grpc.material_management.ProductAttributeSet;
import org.spin.backend.grpc.material_management.ProductAttributeSetInstance;
import org.spin.backend.grpc.material_management.ProductAttributeValue;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Material Management
 */
public class MaterialManagementServiceImplementation extends MaterialManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(MaterialManagementServiceImplementation.class);

	
	@Override
	public void listProductStorage(ListProductStorageRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListEntitiesResponse.Builder entitiesList = listProductStorage(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder listProductStorage(ListProductStorageRequest request) {
		//
		String tableName = "RV_Storage";
		Properties context = ContextManager.getContext(request.getClientRequest());

		MTable table = MTable.get(context, tableName);
		StringBuilder sql = new StringBuilder(DictionaryUtil.getQueryWithReferencesFromColumns(table));
		StringBuffer whereClause = new StringBuffer(" WHERE 1=1 ");

		//	For dynamic condition
		List<Object> parametersList = new ArrayList<>(); // includes on filters criteria
		if (!Util.isEmpty(request.getTableName(), true)) {
			int recordId = request.getRecordId();
			if (recordId <= 0) {
				recordId = RecordUtil.getIdFromUuid(request.getTableName(), request.getRecordUuid(), null);
			}
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
			String where = getWhereClause(request.getTableName(), recordId, parametersList);
			whereClause.append(where);
		}
		sql.append(whereClause); 

		// add where with access restriction
		String parsedSQL = MRole.getDefault(context, false)
			.addAccessSQL(sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//	Get page and count
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());
		int count = 0;
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//	Count records
		count = RecordUtil.countRecords(parsedSQL, tableName, parametersList);
		//	Add Row Number
		parsedSQL = RecordUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(context, tableName), parsedSQL, parametersList);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		return builder;
	}

	public static String getWhereClause(String tableName, int recordId, List<Object> parametersList) {
		String where = "";

		switch (tableName) {
			case I_M_Requisition.Table_Name:
			case I_M_RequisitionLine.Table_Name:
				where = getWhereClasuseByRequisition(tableName, recordId, parametersList);
				break;
			case I_C_Order.Table_Name:
			case I_C_OrderLine.Table_Name:
				where = getWhereClasuseByOrder(tableName, recordId, parametersList);
				break;
			case I_C_Invoice.Table_Name:
			case I_C_InvoiceLine.Table_Name:
			    where = getWhereClasuseByInvoice(tableName, recordId, parametersList);
				break;
			default:
			    where = " AND "
					+ " EXISTS(SELECT 1 FROM " + tableName
					+ " WHERE RV_Storage.M_Product_ID = "
					+ tableName + ".M_Product_ID"
					+ " AND "
					+ tableName + "." + tableName + "_ID = ?)"
				;
				parametersList.add(recordId);
				break;
		}

		return where;
	}


	public static String getWhereClasuseByRequisition(String tableName, int recordId, List<Object> parametersList) {
		int requisitionId = recordId;
		StringBuffer whereClause = new StringBuffer();
		if (tableName.equals(I_M_Requisition.Table_Name)) {
		} else if (tableName.equals(I_M_RequisitionLine.Table_Name)) {
			MRequisitionLine requisitionLine = new MRequisitionLine(Env.getCtx(), recordId, null);
			requisitionId = requisitionLine.getM_Requisition_ID();
		}
		whereClause.append(" AND ")
			.append("EXISTS(SELECT 1 FROM M_RequisitionLine WHERE ")
			.append("RV_Storage.M_Product_ID = M_RequisitionLine.M_Product_ID ")
			.append("AND M_RequisitionLine.M_Requisition_ID = ?) ")
		;
		parametersList.add(requisitionId);

		return whereClause.toString();
	}

	public static String getWhereClasuseByOrder(String tableName, int recordId, List<Object> parametersList) {
		int orderId = recordId;
		StringBuffer whereClause = new StringBuffer();
		if (tableName.equals(I_C_Order.Table_Name)) {
		} else if (tableName.equals(I_C_OrderLine.Table_Name)) {
			MOrderLine orderLine = new MOrderLine(Env.getCtx(), recordId, null);
			orderId = orderLine.getC_Order_ID();
		}
		whereClause.append(" AND ")
			.append("EXISTS(SELECT 1 FROM C_OrderLine WHERE ")
			.append("RV_Storage.M_Product_ID = C_OrderLine.M_Product_ID ")
			.append("AND C_OrderLine.C_Order_ID = ?) ")
		;
		parametersList.add(orderId);

		return whereClause.toString();
	}

	public static String getWhereClasuseByInvoice(String tableName, int recordId, List<Object> parametersList) {
		int invoiceId = recordId;
		StringBuffer whereClause = new StringBuffer();
		if (tableName.equals(I_C_Invoice.Table_Name)) {
		} else if (tableName.equals(I_C_InvoiceLine.Table_Name)) {
			MInvoiceLine invoiceLine = new MInvoiceLine(Env.getCtx(), recordId, null);
			invoiceId = invoiceLine.getC_Invoice_ID();
		}
		whereClause.append(" AND ")
			.append("EXISTS(SELECT 1 FROM C_InvoiceLine WHERE ")
			.append("RV_Storage.M_Product_ID = C_InvoiceLine.M_Product_ID ")
			.append("AND C_InvoiceLine.C_Invoice_ID = ?) ")
		;
		parametersList.add(invoiceId);

		return whereClause.toString();
	}

	@Override
	public void getProductAttributeSet(GetProductAttributeSetRequest request, StreamObserver<ProductAttributeSet> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProductAttributeSet.Builder productAttributeSetBuilder = getProductAttributeSet(request);
			responseObserver.onNext(productAttributeSetBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ProductAttributeSet.Builder getProductAttributeSet(GetProductAttributeSetRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		int productAttributeSetId = request.getId();
		if (productAttributeSetId <= 0) {
			if (!Util.isEmpty(request.getUuid())) {
				productAttributeSetId = RecordUtil.getIdFromUuid(I_M_AttributeSet.Table_Name, request.getUuid(), null);
			}
		}

		if (productAttributeSetId <= 0) {
			if (request.getProductId() > 0 || !Util.isEmpty(request.getProductUuid(), true)) {
				// get with product
				MProduct product = (MProduct) RecordUtil.getEntity(
					context,
					I_M_Product.Table_Name,
					request.getProductUuid(), 
					request.getProductId(),
					null
				);
				if (product == null || product.getM_Product_ID() <= 0) {
					throw new AdempiereException("@M_Product_ID@ @NotFound@");
				}
				if (product.getM_AttributeSet_ID() <= 0) {
					throw new AdempiereException("@PAttributeNoAttributeSet@");
				}
				productAttributeSetId = product.getM_AttributeSet_ID();
			} else if (request.getProductAttributeSetInstanceId() > 0 || !Util.isEmpty(request.getProductAttributeSetInstanceUuid(), true)) {
				// get with attribute set instance
				MAttributeSetInstance attributeSetInstance = (MAttributeSetInstance) RecordUtil.getEntity(
					context,
					I_M_AttributeSetInstance.Table_Name,
					request.getProductAttributeSetInstanceUuid(), 
					request.getProductAttributeSetInstanceId(),
					null
				);
				if (attributeSetInstance == null || attributeSetInstance.getM_AttributeSetInstance_ID() <= 0) {
					throw new AdempiereException("@M_AttributeSetInstance_ID@ @NotFound@");
				}
				if (attributeSetInstance.getM_AttributeSet_ID() <= 0) {
					throw new AdempiereException("@PAttributeNoAttributeSet@");
				}
				productAttributeSetId = attributeSetInstance.getM_AttributeSet_ID();
			}
		}
			
		if (productAttributeSetId <= 0) {
			throw new AdempiereException("@M_AttributeSet_ID@ @NotFound@");
		}
		
		ProductAttributeSet.Builder builder =convertProductAttributeSet(productAttributeSetId);
		
		return builder;
	}

	@Override
	public void listProductAttributeSetInstances(ListProductAttributeSetInstancesRequest request, StreamObserver<ListProductAttributeSetInstancesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListProductAttributeSetInstancesResponse.Builder recordsList = listProductAttributeSetInstances(request);
			responseObserver.onNext(recordsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	private ListProductAttributeSetInstancesResponse.Builder listProductAttributeSetInstances(ListProductAttributeSetInstancesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		int productAttributeSetId = request.getProductAttributeSetId();
		if (productAttributeSetId <= 0) {
			if (!Util.isEmpty(request.getProductAttributeSetUuid())) {
				productAttributeSetId = RecordUtil.getIdFromUuid(I_M_AttributeSet.Table_Name, request.getProductAttributeSetUuid(), null);
			}
		}

		// get with product
		if (productAttributeSetId <= 0 && (request.getProductId() > 0 || !Util.isEmpty(request.getProductUuid(), true))) {
			MProduct product = (MProduct) RecordUtil.getEntity(
				context,
				I_M_Product.Table_Name,
				request.getProductUuid(), 
				request.getProductId(),
				null
			);
			if (product == null || product.getM_Product_ID() <= 0) {
				throw new AdempiereException("@M_Product_ID@ @NotFound@");
			}
			if (product.getM_AttributeSet_ID() <= 0) {
				throw new AdempiereException("@PAttributeNoAttributeSet@");
			}
			productAttributeSetId = product.getM_AttributeSet_ID();
		}
			
		if (productAttributeSetId <= 0) {
			throw new AdempiereException("@M_AttributeSet_ID@ @NotFound@");
		}

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		Query query =  new Query(
			context,
			I_M_AttributeSetInstance.Table_Name,
			"M_AttributeSet_ID = ?",
			null
		)
			.setClient_ID()
			.setParameters(productAttributeSetId)
			.setOnlyActiveRecords(true);

		int count = query.count();
		Env.getAD_Client_ID(context);

		List<MAttributeSetInstance> productAttributeSetInstancesList = query.setLimit(limit, offset).list();
		ListProductAttributeSetInstancesResponse.Builder builderList = ListProductAttributeSetInstancesResponse.newBuilder()
			.setRecordCount(count);

		productAttributeSetInstancesList.forEach(attributeSetInstance -> {
			ProductAttributeSetInstance.Builder attributeSetInstanceBuilder = convertProductAttributeSetInstance(attributeSetInstance);
			builderList.addRecords(attributeSetInstanceBuilder);
		});

		//  Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		//  Set next page
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		return builderList;
	}

	private ProductAttributeSetInstance.Builder convertProductAttributeSetInstance(MAttributeSetInstance attributeSetInstance) {
		ProductAttributeSetInstance.Builder builder = ProductAttributeSetInstance.newBuilder();
		if (attributeSetInstance == null) {
			return builder;
		}
		builder.setId(attributeSetInstance.getM_AttributeSetInstance_ID())
			.setUuid(ValueUtil.validateNull(attributeSetInstance.getUUID()))
			.setDescription(ValueUtil.validateNull(attributeSetInstance.getDescription()))
			.setLot(ValueUtil.validateNull(attributeSetInstance.getLot()))
			.setLotId(attributeSetInstance.getM_Lot_ID())
			.setSerial(ValueUtil.validateNull(attributeSetInstance.getSerNo()))
			.setProductAttributeSet(
				convertProductAttributeSet(attributeSetInstance.getM_AttributeSet_ID())
			)
		;
		if (attributeSetInstance.getGuaranteeDate() != null) {
			builder.setGuaranteeDate(attributeSetInstance.getGuaranteeDate().getTime());
		}
		
		return builder;
	}


	private ProductAttributeSet.Builder convertProductAttributeSet(int attributeSetId) {
		MAttributeSet attributeSet = MAttributeSet.get(Env.getCtx(), attributeSetId);
		return convertProductAttributeSet(attributeSet);
	}
	private ProductAttributeSet.Builder convertProductAttributeSet(MAttributeSet attributeSet) {
		ProductAttributeSet.Builder builder = ProductAttributeSet.newBuilder();
		if (attributeSet == null) {
			return builder;
		}
		builder.setId(attributeSet.getM_AttributeSet_ID())
			.setUuid(ValueUtil.validateNull(attributeSet.getUUID()))
			.setName(ValueUtil.validateNull(attributeSet.getName()))
			.setDescription(ValueUtil.validateNull(attributeSet.getDescription()))
			.setIsInstanceAttribute(attributeSet.isInstanceAttribute())
			.setIsLot(attributeSet.isLot())
			.setIsLotMandatory(attributeSet.isLotMandatory())
			.setLotControlId(attributeSet.getM_LotCtl_ID())
			.setLotCharStartOverwrite(ValueUtil.validateNull(attributeSet.getLotCharSOverwrite()))
			.setLotCharEndOverwrite(ValueUtil.validateNull(attributeSet.getLotCharEOverwrite()))
			.setIsSerial(attributeSet.isSerNo())
			.setIsSerialMandatory(attributeSet.isSerNoMandatory())
			.setSerialControlId(attributeSet.getM_SerNoCtl_ID())
			.setSerialCharStartOverwrite(ValueUtil.validateNull(attributeSet.getSerNoCharSOverwrite()))
			.setSerialCharEndOverwrite(ValueUtil.validateNull(attributeSet.getSerNoCharEOverwrite()))
			.setIsGuaranteeDate(attributeSet.isGuaranteeDate())
			.setIsGuaranteeDateMandatory(attributeSet.isGuaranteeDateMandatory())
			.setGuaranteeDays(attributeSet.getGuaranteeDays())
			.setMandatoryType(ValueUtil.validateNull(attributeSet.getMandatoryType()))
		;
		
		new Query(
				Env.getCtx(),
				I_M_AttributeUse.Table_Name,
				"M_AttributeSet_ID = ?",
				null
			)
			.setParameters(attributeSet.getM_AttributeSet_ID())
			.<MAttributeUse>list()
			.forEach(attributeUse -> {
				ProductAttribute.Builder productAttributeBuilder = convertProductAttribute(attributeUse.getM_Attribute_ID());
				productAttributeBuilder.setSequence(attributeUse.getSeqNo());

				builder.addProductAttributes(productAttributeBuilder);
			});
		
		return builder;
	}

	private ProductAttribute.Builder convertProductAttribute(int attributeId) {
		MAttribute attributeSet = new MAttribute(Env.getCtx(), attributeId, null);
		return convertProductAttribute(attributeSet);
	}
	private ProductAttribute.Builder convertProductAttribute(MAttribute attribute) {
		ProductAttribute.Builder builder = ProductAttribute.newBuilder();
		if (attribute == null) {
			return builder;
		}
		builder.setId(attribute.getM_Attribute_ID())
			.setUuid(ValueUtil.validateNull(attribute.getUUID()))
			.setName(ValueUtil.validateNull(attribute.getName()))
			.setDescription(ValueUtil.validateNull(attribute.getDescription()))
			.setValueType(ValueUtil.validateNull(attribute.getAttributeValueType()))
			.setIsInstanceAttribute(attribute.isInstanceAttribute())
			.setIsMandatory(attribute.isMandatory())
		;
		
		if (X_M_Attribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType())) {
			new Query(
					Env.getCtx(),
					I_M_AttributeValue.Table_Name,
					"M_Attribute_ID = ?",
					null
				)
				.setParameters(attribute.getM_Attribute_ID())
				.<MAttributeValue>list()
				.forEach(attributeValue -> {
					ProductAttributeValue.Builder productAttributeValueBuilder = convertProductAttributeValue(attributeValue.getM_AttributeValue_ID());
					builder.addProductAttributeValues(productAttributeValueBuilder);
				});
		}
		
		return builder;
	}

	private ProductAttributeValue.Builder convertProductAttributeValue(int productAttributeValueId) {
		MAttributeValue productAttributeValue = new MAttributeValue(Env.getCtx(), productAttributeValueId, null);
		return convertProductAttributeValue(productAttributeValue);
	}
	private ProductAttributeValue.Builder convertProductAttributeValue(MAttributeValue productAttributeValue) {
		ProductAttributeValue.Builder builder = ProductAttributeValue.newBuilder();
		if (productAttributeValue == null) {
			return builder;
		}
		builder.setId(productAttributeValue.getM_AttributeValue_ID())
			.setUuid(ValueUtil.validateNull(productAttributeValue.getUUID()))
			.setName(ValueUtil.validateNull(productAttributeValue.getName()))
			.setDescription(ValueUtil.validateNull(productAttributeValue.getDescription()))
			.setValue(ValueUtil.validateNull(productAttributeValue.getValue()))
		;
		
		return builder;
	}
}
