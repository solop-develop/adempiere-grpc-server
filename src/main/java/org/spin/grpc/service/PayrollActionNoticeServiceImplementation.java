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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.MBPartner;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Reference;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eevolution.model.I_HR_Period;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.MHREmployee;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRPayroll;
import org.eevolution.model.MHRProcess;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.Value;
import org.spin.backend.grpc.form.DeletePayrollMovementsRequest;
import org.spin.backend.grpc.form.GetPayrollConceptDefinitionRequest;
import org.spin.backend.grpc.form.ListEmployeeValidRequest;
import org.spin.backend.grpc.form.ListPayrollConceptsRequest;
import org.spin.backend.grpc.form.ListPayrollMovementsRequest;
import org.spin.backend.grpc.form.ListPayrollProcessRequest;
import org.spin.backend.grpc.form.PayrollActionNoticeGrpc.PayrollActionNoticeImplBase;
import org.spin.backend.grpc.form.SavePayrollMovementRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Paryroll Action Notice Form
 */
public class PayrollActionNoticeServiceImplementation extends PayrollActionNoticeImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PayrollActionNoticeServiceImplementation.class);
	
	@Override
	public void listPayrollProcess(ListPayrollProcessRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			ListLookupItemsResponse.Builder lookupsList = convertPayrollProcessList(context, request);
			responseObserver.onNext(lookupsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	private ListLookupItemsResponse.Builder convertPayrollProcessList(Properties context, ListPayrollProcessRequest request) {
		String sql = "SELECT HR_Process_ID, DocumentNo ||'-'|| Name AS DisplayColumn, DocumentNo, Name, UUID "
			+ "FROM HR_Process "
			+ "WHERE IsActive = 'Y' "
			+ "AND DocStatus IN('DR', 'IP') ";

		List<Object> parameters = new ArrayList<>();
		sql = RecordUtil.addSearchValueAndGet(sql, MHRProcess.Table_Name, request.getSearchValue(), parameters);
		sql = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				MHRProcess.Table_Name,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		sql += " ORDER BY DocumentNo, Name";

		//	Get page and count
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//	Add Row Number
		sql = RecordUtil.getQueryWithLimit(sql, limit, offset);

		ListLookupItemsResponse.Builder lookupsList = ListLookupItemsResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ValueUtil.setParametersFromObjectsList(pstmt, parameters);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				//	1 = Key Column
				//	2 = UUID
				//	3 = Display Value
				int keyValue = rs.getInt(MHRProcess.COLUMNNAME_HR_Process_ID);
				String uuid = rs.getString(MHRProcess.COLUMNNAME_UUID);
				String displayColumn = rs.getString(LookupUtil.DISPLAY_COLUMN_KEY);
				//
				LookupItem.Builder valueObject = LookupUtil.convertObjectFromResult(keyValue, uuid, null, displayColumn);
				valueObject.setTableName(MHRProcess.Table_Name);
				lookupsList.addRecords(valueObject.build());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Count records
		int count = RecordUtil.countRecords(sql, MHRProcess.Table_Name, parameters);
		lookupsList.setRecordCount(count);

		//	Set page token
		String nextPageToken = "";
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nextPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		lookupsList.setNextPageToken(nextPageToken);
		
		return lookupsList;
	}

	@Override
	public void listEmployeeValid(ListEmployeeValidRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			ListLookupItemsResponse.Builder lookupsList = convertEmployeeValidList(context, request);
			responseObserver.onNext(lookupsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListLookupItemsResponse.Builder convertEmployeeValidList(Properties context, ListEmployeeValidRequest request) {
		ListLookupItemsResponse.Builder lookupsList = ListLookupItemsResponse.newBuilder();

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		int payrollProcessId = (int) contextAttributesList.get(MHRProcess.COLUMNNAME_HR_Process_ID);
		if (request.getContextAttributesList() == null || payrollProcessId <= 0) {
			return lookupsList;
		}

		// Get Payroll Process
		MHRProcess payrollProcess = new MHRProcess(Env.getCtx(), payrollProcessId, null);

		//	Get Payroll attribute
		MHRPayroll payroll = MHRPayroll.getById(Env.getCtx(), payrollProcess.getHR_Payroll_ID(), payrollProcess.getName());

		List<Object> parameters = new ArrayList<>();
		String selectQuery = "SELECT bp.C_BPartner_ID, "
			+ "bp.Value || ' - ' || bp.Name || COALESCE(' ' || bp.Name2, '') AS DisplayColumn, UUID "
			+ "FROM C_BPartner bp";
		selectQuery = RecordUtil.addSearchValueAndGet(selectQuery, MHRProcess.Table_Name, "bp", request.getSearchValue(), parameters);
		selectQuery = MRole.getDefault(context, false)
			.addAccessSQL(
				selectQuery,
				"bp",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		String whereClause = "AND bp.IsActive = 'Y' "
				+ "AND EXISTS(SELECT 1 FROM HR_Employee hrpe "
				+ "		WHERE hrpe.C_BPartner_ID = bp.C_BPartner_ID "
				+ "		AND hrpe.IsActive = 'Y' ";
		if (payrollProcess.getHR_Payroll_ID() > 0 && !payroll.isIgnoreDefaultPayroll()) {
			whereClause += " AND (hrpe.HR_Payroll_ID = ? OR hrpe.HR_Payroll_ID is NULL) ";
			parameters.add(payrollProcess.getHR_Payroll_ID());
	
			if (payrollProcess.getHR_Department_ID() > 0) {
				whereClause += " AND (hrpe.HR_Department_ID = ? OR hrpe.HR_Department_ID is NULL) ";
 				parameters.add(payrollProcess.getHR_Department_ID());
			}
			if (payrollProcess.getHR_Job_ID() > 0) {
				whereClause += " AND (hrpe.HR_Job_ID = ? OR hrpe.HR_Job_ID is NULL) ";
 				parameters.add(payrollProcess.getHR_Job_ID());
			}
 			if (payrollProcess.getHR_Employee_ID() > 0) {
 				whereClause += " AND (hrpe.HR_Employee_ID = ? OR hrpe.HR_Employee_ID is NULL) ";
 				parameters.add(payrollProcess.getHR_Employee_ID());
 			}
		}
		whereClause += ")";

		String sql = selectQuery + whereClause + " ORDER BY " + LookupUtil.DISPLAY_COLUMN_KEY;

		//	Get page and count
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//	Add Row Number
		sql = RecordUtil.getQueryWithLimit(sql, limit, offset);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ValueUtil.setParametersFromObjectsList(pstmt, parameters);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				//	1 = Key Column
				//	2 = UUID
				//	3 = Display Value
				int keyValue = rs.getInt(I_C_BPartner.COLUMNNAME_C_BPartner_ID);
				String uuid = rs.getString(I_C_BPartner.COLUMNNAME_UUID);
				String displayColumn = rs.getString(LookupUtil.DISPLAY_COLUMN_KEY);
				//
				LookupItem.Builder valueObject = LookupUtil.convertObjectFromResult(keyValue, uuid, null, displayColumn);
				valueObject.setTableName(MHRProcess.Table_Name);
				lookupsList.addRecords(valueObject.build());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Count records
		int count = RecordUtil.countRecords(sql, MHRProcess.Table_Name, parameters);
		lookupsList.setRecordCount(count);

		//	Set page token
		String nextPageToken = "";
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nextPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		lookupsList.setNextPageToken(nextPageToken);

		return lookupsList;
	}


	@Override
	public void listPayrollConcepts(ListPayrollConceptsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			ListLookupItemsResponse.Builder listLookups = convertPayrollConceptsList(context, request);
			responseObserver.onNext(listLookups.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListLookupItemsResponse.Builder convertPayrollConceptsList(Properties context, ListPayrollConceptsRequest request) {
		ListLookupItemsResponse.Builder listLookups = ListLookupItemsResponse.newBuilder();

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (request.getContextAttributesList() == null || contextAttributesList.size() <= 0) {
			return listLookups;
		}

		int payrollProcessId = (int) contextAttributesList.get(MHRProcess.COLUMNNAME_HR_Process_ID);
		if (payrollProcessId <= 0) {
			return listLookups;
		}

		int businessPartnerId = 0;
		if (contextAttributesList.get(MBPartner.COLUMNNAME_C_BPartner_ID) != null) {
			businessPartnerId = (int) contextAttributesList.get(MBPartner.COLUMNNAME_C_BPartner_ID);
		}
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@BPartnerNotFound@");
		}
		MHREmployee employee = MHREmployee.getActiveEmployee(Env.getCtx(), businessPartnerId, null);
		if (employee == null || employee.getHR_Employee_ID() <= 0) {
			throw new AdempiereException("@BPartnerNotFound@ on Employee");
		}

		// Get Payroll Process
		MHRProcess payrollProcess = new MHRProcess(Env.getCtx(), payrollProcessId, null);

		List<Object> parameters = new ArrayList<>();
		String selectQuery = "SELECT hrpc.HR_Concept_ID, "
			+ "hrpc.Value || ' - ' || hrpc.Name AS DisplayColumn, "
			+ "hrpc.Value, "
			+ "hrpc.UUID "
			+ "FROM HR_Concept hrpc ";

		selectQuery = RecordUtil.addSearchValueAndGet(selectQuery, MHRConcept.Table_Name, "hrpc", request.getSearchValue(), parameters);
		selectQuery = MRole.getDefault(context, false)
			.addAccessSQL(
				selectQuery,
				"hrpc",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		String whereClause = " AND hrpc.AD_Client_ID = ? "
			+ "AND hrpc.IsActive = 'Y' "
			+ "AND hrpc.IsManual = 'Y' "
			+ "AND hrpc.Type != 'E' "
			+ "AND EXISTS(SELECT 1 FROM HR_Attribute a "
			+ "		WHERE a.HR_Concept_ID = hrpc.HR_Concept_ID ";	
		parameters.add(payrollProcess.getAD_Client_ID());
		
		// Process & Payroll
		if (payrollProcess.getHR_Payroll_ID() > 0) {
			whereClause += " AND (a.HR_Payroll_ID = ? OR a.HR_Payroll_ID is NULL) ";
			parameters.add(payrollProcess.getHR_Payroll_ID());
		}
		// Process & Department
		if (employee.getHR_Department_ID() > 0 ) {
			whereClause += " AND (a.HR_Department_ID = ? OR a.HR_Department_ID is NULL) ";
			parameters.add(employee.getHR_Department_ID());
		}
		// Process & Job
		if (employee.getHR_Job_ID() > 0 ) {
			whereClause += " AND (a.HR_Job_ID = ? OR a.HR_Job_ID is NULL)";
			parameters.add(employee.getHR_Job_ID());
		}
		// Process & Employee
		if (employee.getHR_Department_ID() > 0 ) {
			whereClause += " AND (a.HR_Employee_ID = ? OR a.HR_Employee_ID is NULL)";
			parameters.add(employee.getHR_Employee_ID());
		}
		whereClause += ")";

		String sql = selectQuery + whereClause + " ORDER BY " + LookupUtil.DISPLAY_COLUMN_KEY;

		//	Get page and count
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//	Add Row Number
		sql = RecordUtil.getQueryWithLimit(sql, limit, offset);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			ValueUtil.setParametersFromObjectsList(pstmt, parameters);
			
			//	Get from Query
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(MHRConcept.COLUMNNAME_HR_Concept_ID);
				String value = rs.getString(MHRConcept.COLUMNNAME_Value);
				String uuid = rs.getString(MHRConcept.COLUMNNAME_UUID);
				String displayColumn = rs.getString(LookupUtil.DISPLAY_COLUMN_KEY);
				//
				LookupItem.Builder lookupBuilder = LookupUtil.convertObjectFromResult(id, uuid, value, displayColumn);
				lookupBuilder.setTableName(MHRConcept.Table_Name);

				listLookups.addRecords(lookupBuilder.build());
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Count records
		int count = RecordUtil.countRecords(sql, MHRProcess.Table_Name, parameters);
		listLookups.setRecordCount(count);

		//	Set page token
		String nextPageToken = "";
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nextPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		listLookups.setNextPageToken(nextPageToken);

		return listLookups;
	}

	@Override
	public void getPayrollConceptDefinition(GetPayrollConceptDefinitionRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			Entity.Builder payrollConcept = convertPayrollConcept(context, request);
			responseObserver.onNext(payrollConcept.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder convertPayrollConcept(Properties context, GetPayrollConceptDefinitionRequest request) {
		String tableName = MHRConcept.Table_Name;

		MHRConcept conceptDefinition = null;
		if(request.getId() > 0) {
			conceptDefinition = MHRConcept.getById(context, request.getId(), null);
		} else if(!Util.isEmpty(request.getUuid(), true)) {
			conceptDefinition = new Query(
					context,
					tableName,
					MHRConcept.COLUMNNAME_UUID + " = ? ",
					null
				)
				.setParameters(request.getUuid())
				.first();
		}

		Entity.Builder entityBuilder = Entity.newBuilder();
		
		entityBuilder.setTableName(tableName);
		entityBuilder.setId(conceptDefinition.getHR_Concept_ID());
		entityBuilder.setUuid(conceptDefinition.getUUID());

		Value.Builder id = ValueUtil.getValueFromInteger(conceptDefinition.getHR_Concept_ID());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_HR_Concept_ID, id.build());

		Value.Builder uuid = ValueUtil.getValueFromString(conceptDefinition.getUUID());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_UUID, uuid.build());

		Value.Builder name = ValueUtil.getValueFromString(conceptDefinition.getName());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_Name, name.build());

		Value.Builder value = ValueUtil.getValueFromString(conceptDefinition.getValue());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_Name, value.build());

		Value.Builder type = ValueUtil.getValueFromString(conceptDefinition.getType());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_Type, type.build());

		Value.Builder columnType = ValueUtil.getValueFromString(conceptDefinition.getColumnType());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_ColumnType, columnType.build());

		int referenceId = conceptDefinition.getAD_Reference_ID();
		String referenceUuid = null;
		if (referenceId > 0) {
			X_AD_Reference reference = new X_AD_Reference(context, referenceId, null);			
			referenceUuid = reference.getUUID();
		}
		Value.Builder referenceIdValue = ValueUtil.getValueFromInteger(conceptDefinition.getAD_Reference_ID());
		entityBuilder.putValues(MHRConcept.COLUMNNAME_AD_Reference_ID, referenceIdValue.build());

		Value.Builder referenceUuidValue = ValueUtil.getValueFromString(referenceUuid);
		entityBuilder.putValues(MHRConcept.COLUMNNAME_AD_Reference_ID + "_UUID", referenceUuidValue.build());

		return entityBuilder;
	}
	
	
	@Override
	public void listPayrollMovements(ListPayrollMovementsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			Properties context = ContextManager.getContext(request.getClientRequest());
			ListEntitiesResponse.Builder entitiesList = convertListPayrollMovements(context, request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder convertListPayrollMovements(Properties context, ListPayrollMovementsRequest request) {
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (request.getContextAttributesList() == null || contextAttributesList.size() <= 0) {
			return builder;
		}

		int payrollProcessId = (int) contextAttributesList.get(MHRProcess.COLUMNNAME_HR_Process_ID);
		if (payrollProcessId <= 0) {
			return builder;
		}
		int businessPartnerId = (int) contextAttributesList.get(MBPartner.COLUMNNAME_C_BPartner_ID);
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@BPartnerNotFound@");
		}
		
		StringBuffer sqlQuery = new StringBuffer("SELECT o.Name as OrganizationName, hc.Name AS ConceptName, "		//	Organization Name, Concept Name
			+ "hm.ValidFrom, cr.ColumnType, "										//	Valid From, Column Type
			+ "hm.Qty, hm.Amount, hm.ServiceDate, hm.TextMsg, hm.Description, "		//	Quantity, Amount, Service Date, Text Message, Description
			+ "hm.HR_Movement_ID, hm.AD_Org_ID, hm.HR_Process_ID, hm.HR_Concept_ID, hm.UUID ");	//	References
		//	From Clause
		sqlQuery.append("FROM HR_Movement hm "
			+ "INNER JOIN AD_Org o ON(hm.AD_Org_ID = o.AD_Org_ID) "
			+ "INNER JOIN HR_Concept hc ON(hm.HR_Concept_ID = hc.HR_Concept_ID) "
			+ "LEFT JOIN (SELECT r.Value, COALESCE(rt.Name, r.Name) ColumnType "
			+ "				FROM AD_Ref_List r "
			+ "				LEFT JOIN AD_Ref_List_Trl rt ON(rt.AD_Ref_List_ID = r.AD_Ref_List_ID AND rt.AD_Language = ?) "
			+ "				WHERE r.AD_Reference_ID = ?) cr ON(cr.Value = hc.ColumnType) "
		);
		//	Where Clause
		sqlQuery.append("WHERE hm.Processed = 'N' "
			+ "AND hm.HR_Process_ID = ? "
			+ "AND hm.C_BPartner_ID = ? ");
		//	Order By
		sqlQuery.append("ORDER BY o.AD_Org_ID, hm.HR_Process_ID, hm.ValidFrom, hm.HR_Concept_ID");

		//	Get page and count
		String nexPageToken = null;
		int rowCount = 0;

		//  Execute
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlQuery.toString(), null);
			//	Language
			pstmt.setString(1, Env.getAD_Language(Env.getCtx()));
			//	Reference for Column Type
			pstmt.setInt(2, MHRConcept.COLUMNTYPE_AD_Reference_ID);
			//	HR Process
			pstmt.setInt(3, payrollProcessId);
			//	Business Partner
			pstmt.setInt(4, businessPartnerId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Entity.Builder entity = Entity.newBuilder();
				entity.setTableName(MHRMovement.Table_Name);
				entity.setId(rs.getInt(MHRMovement.COLUMNNAME_HR_Movement_ID));
				entity.setUuid(rs.getString(MHRMovement.COLUMNNAME_UUID));

				// AD_Org_ID
				Value.Builder organizationId = ValueUtil.getValueFromInteger(rs.getInt(MHRMovement.COLUMNNAME_AD_Org_ID));
				entity.putValues(MHRMovement.COLUMNNAME_AD_Org_ID, organizationId.build());

				// DisplayColumn_AD_Org_ID
				Value.Builder organizationName = ValueUtil.getValueFromString(rs.getString(1));
				entity.putValues(LookupUtil.DISPLAY_COLUMN_KEY + "_" + MHRMovement.COLUMNNAME_AD_Org_ID, organizationName.build());

				// HR_Movement_ID
				Value.Builder movementId = ValueUtil.getValueFromInteger(rs.getInt(MHRMovement.COLUMNNAME_HR_Movement_ID));
				entity.putValues(MHRMovement.COLUMNNAME_HR_Movement_ID, movementId.build());
				// movement UUID
				Value.Builder processUuid = ValueUtil.getValueFromString(rs.getString(MHRMovement.COLUMNNAME_UUID));
				entity.putValues(MHRMovement.COLUMNNAME_UUID, processUuid.build());

				// DisplayColumn_HR_Movement_ID
				Value.Builder movementName = ValueUtil.getValueFromString(rs.getString(2));
				entity.putValues(LookupUtil.DISPLAY_COLUMN_KEY + "_" + MHRMovement.COLUMNNAME_HR_Movement_ID, movementName.build());

				// ValidFrom
				Value.Builder validFrom = ValueUtil.getValueFromDate(rs.getTimestamp(MHRMovement.COLUMNNAME_ValidFrom));
				entity.putValues(MHRMovement.COLUMNNAME_ValidFrom, validFrom.build());

				// ColumnType
				Value.Builder columnType = ValueUtil.getValueFromString(rs.getString(4));
				entity.putValues("ColumnType", columnType.build());

				// Qty
				Value.Builder quantity = ValueUtil.getValueFromDecimal(rs.getBigDecimal(MHRMovement.COLUMNNAME_Qty));
				entity.putValues(MHRMovement.COLUMNNAME_Qty, quantity.build());

				// Amount
				Value.Builder amount = ValueUtil.getValueFromDecimal(rs.getBigDecimal(MHRMovement.COLUMNNAME_Amount));
				entity.putValues(MHRMovement.COLUMNNAME_Amount, amount.build());

				// ServiceDate
				Value.Builder serviceDate = ValueUtil.getValueFromDate(rs.getTimestamp(MHRMovement.COLUMNNAME_ServiceDate));
				entity.putValues(MHRMovement.COLUMNNAME_ServiceDate, serviceDate.build());

				// TextMsg
				Value.Builder textMessage = ValueUtil.getValueFromString(rs.getString(MHRMovement.COLUMNNAME_TextMsg));
				entity.putValues(MHRMovement.COLUMNNAME_TextMsg, textMessage.build());

				// Description
				Value.Builder description = ValueUtil.getValueFromString(rs.getString(MHRMovement.COLUMNNAME_Description));
				entity.putValues(MHRMovement.COLUMNNAME_Description, description.build());

				// Concept
				Value.Builder concept = ValueUtil.getValueFromString(rs.getString(MHRMovement.COLUMNNAME_HR_Concept_ID));
				entity.putValues(MHRMovement.COLUMNNAME_HR_Concept_ID, concept.build());				

				// Process ID
				Value.Builder processId = ValueUtil.getValueFromInteger(rs.getInt(MHRMovement.COLUMNNAME_HR_Process_ID));
				entity.putValues(MHRMovement.COLUMNNAME_HR_Process_ID, processId.build());

				//  prepare next
				builder.addRecords(entity);
				rowCount++;
			}
		}
		catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builder.setRecordCount(rowCount);

		//	Set page token
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		
		return builder;
	}

	@Override
	public void savePayrollMovement(SavePayrollMovementRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			Entity.Builder entity = convertSaveMovement(context, request);
			responseObserver.onNext(entity.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder convertSaveMovement(Properties context, SavePayrollMovementRequest request) {
		Entity.Builder builder = Entity.newBuilder();
		builder.setTableName(MHRMovement.Table_Name);

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (request.getContextAttributesList() == null || contextAttributesList.size() <= 0) {
			return builder;
		}

		int payrollProcessId = (int) contextAttributesList.get(MHRMovement.COLUMNNAME_HR_Process_ID);
		if (payrollProcessId <= 0) {
			throw new AdempiereException("Payroll Process ID Not Found");
		}
		MHRProcess payrollProcess = new MHRProcess(context, payrollProcessId, null);
		
		int businessPartnerId = (int) contextAttributesList.get(MHRMovement.COLUMNNAME_C_BPartner_ID);
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@BPartnerNotFound@");
		}

		Map<String, Object> attributesList = ValueUtil.convertValuesToObjects(request.getAttributesList());

		// Concept
		int conceptId = 0;
		if (attributesList.get(MHRConcept.COLUMNNAME_HR_Concept_ID) != null) {
			Object id = attributesList.get(MHRConcept.COLUMNNAME_HR_Concept_ID);
			if (id instanceof String) {
				conceptId = Integer.parseInt((String) id);
			} else {
				conceptId = (int) id;
			}
		}
		if (conceptId <= 0) {
			throw new AdempiereException("Concept ID Not Found");
		}

		int movementId = 0;
		if (contextAttributesList.get(MHRMovement.COLUMNNAME_HR_Movement_ID) != null
			|| (int) contextAttributesList.get(MHRMovement.COLUMNNAME_HR_Movement_ID) > 0) {
			movementId = (int) contextAttributesList.get(MHRMovement.COLUMNNAME_HR_Movement_ID);
		}

		MHRMovement movement = new MHRMovement(Env.getCtx(), movementId, null);

		MHRConcept concept = MHRConcept.getById(Env.getCtx(), conceptId, null);
		movement.setSeqNo(concept.getSeqNo());
		movement.setHR_Concept_ID(conceptId);

		if (concept.getColumnType().equals(MHRConcept.COLUMNTYPE_Quantity)) {
			// Quantity
			movement.setQty(null);
			Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_Qty))
				.ifPresent(qty -> {
					if (qty instanceof Integer) {
						movement.setQty(BigDecimal.valueOf((int) qty));
					} else {
						movement.setQty((BigDecimal) qty);
					}
				});
			movement.setAmount(null);
			movement.setServiceDate(null);
			movement.setTextMsg(null);
		} else if (concept.getColumnType().equals(MHRConcept.COLUMNTYPE_Amount)) {
			// Amount
			movement.setAmount(null);
			Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_Amount))
				.ifPresent(amount -> {
					if (amount instanceof Integer) {
						movement.setAmount(BigDecimal.valueOf((int) amount));
					} else {
						movement.setAmount((BigDecimal) amount);
					}
				});
			movement.setQty(null);
			movement.setServiceDate(null);
			movement.setTextMsg(null);
		} else if (concept.getColumnType().equals(MHRConcept.COLUMNTYPE_Text)) {
			// Message Text
			movement.setTextMsg(null);
			Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_TextMsg))
				.ifPresent(textValue -> {	
					movement.setTextMsg(textValue.toString());
				});
			movement.setQty(null);
			movement.setAmount(null);
			movement.setServiceDate(null);
		} else if (concept.getColumnType().equals(MHRConcept.COLUMNTYPE_Date)) {
			// Service Date
			movement.setServiceDate(null);
			Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_ServiceDate))
				.ifPresent(serviceDate -> {	
					movement.setServiceDate((Timestamp) serviceDate);
				});
			movement.setQty(null);
			movement.setAmount(null);
			movement.setTextMsg(null);
		} else {
			// without column type
			movement.setQty(null);
			movement.setAmount(null);
			movement.setTextMsg(null);
			movement.setServiceDate(null);
		}

		movement.setHR_Concept_Category_ID(concept.getHR_Concept_Category_ID());

		// Description
		movement.setDescription(null);
		Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_Description))
			.ifPresent(descriptionValue -> {	
				movement.setDescription(descriptionValue.toString());
			});

		// payroll process id
		movement.setHR_Process_ID(payrollProcess.getHR_Process_ID());

		// payroll period
		movement.setPeriodNo(0);
		I_HR_Period payrollPeriod = payrollProcess.getHR_Period();
		Optional.ofNullable(payrollPeriod)
			.ifPresent(period -> {
				if (period.getHR_Period_ID() > 0) {
					movement.setPeriodNo(period.getPeriodNo());
				}
			});

		// Valid From
		movement.setValidFrom(null);
		Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_ValidFrom))
			.ifPresent(validFrom -> {	
				movement.setValidFrom((Timestamp) validFrom);
			});

		// Valid To
		movement.setValidTo(null);
		Optional.ofNullable(attributesList.get(MHRMovement.COLUMNNAME_ValidTo))
			.ifPresent(validTo -> {	
				movement.setValidTo((Timestamp) validTo);
			});

		movement.setC_BPartner_ID(businessPartnerId);
		MHREmployee employee = MHREmployee.getActiveEmployee(Env.getCtx(), businessPartnerId, null);
		if (employee != null && employee.get_ID() > 0) {
			MHRPayroll payroll = MHRPayroll.getById(Env.getCtx(), payrollProcess.getHR_Payroll_ID(),null);
			movement.setAD_Org_ID(employee.getAD_Org_ID());
			movement.setHR_Department_ID(employee.getHR_Department_ID());
			movement.setHR_Job_ID(employee.getHR_Job_ID());
			movement.setHR_SkillType_ID(employee.getHR_SkillType_ID());

			int activityId = employee.getC_Activity_ID() > 0 ? employee.getC_Activity_ID() : employee.getHR_Department().getC_Activity_ID();
			movement.setC_Activity_ID(activityId);

			movement.setHR_Payroll_ID(payroll.getHR_Payroll_ID());
			movement.setHR_Contract_ID(payroll.getHR_Contract_ID());
			movement.setHR_Employee_ID(employee.getHR_Employee_ID());
			movement.setHR_EmployeeType_ID(employee.getHR_EmployeeType_ID());
		}

		movement.setIsManual(true);
	
		movement.saveEx();
		
		builder = generateEntityBuilderFromMovement(builder, movement);

		return builder;
	}
	
	private Entity.Builder generateEntityBuilderFromMovement(Entity.Builder entityBulder, MHRMovement payrollMovement) {
		entityBulder.setId(payrollMovement.getHR_Movement_ID());
		entityBulder.setUuid(ValueUtil.validateNull(payrollMovement.getUUID()));

		entityBulder.putValues(
			MHRMovement.COLUMNNAME_HR_Movement_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getHR_Movement_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_UUID,
			(ValueUtil.getValueFromString(payrollMovement.getUUID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_IsManual,
			(ValueUtil.getValueFromBoolean(payrollMovement.isManual())).build()
		);

		entityBulder.putValues(
			MHRMovement.COLUMNNAME_SeqNo,
			(ValueUtil.getValueFromInteger(payrollMovement.getSeqNo())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_HR_Concept_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getHR_Concept_ID())).build()
		);

		entityBulder.putValues(
			MHRConcept.COLUMNNAME_HR_Concept_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getHR_Concept_ID())).build()
		);
		// Values
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_Qty,
			(ValueUtil.getValueFromDecimal(payrollMovement.getQty())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_Amount,
			(ValueUtil.getValueFromDecimal(payrollMovement.getAmount())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_TextMsg,
			(ValueUtil.getValueFromString(payrollMovement.getTextMsg())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_ServiceDate,
			(ValueUtil.getValueFromDate(payrollMovement.getServiceDate())).build()
		);

		entityBulder.putValues(
			MHRMovement.COLUMNNAME_HR_Concept_Category_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getHR_Concept_Category_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_Description,
			(ValueUtil.getValueFromString(payrollMovement.getDescription())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_HR_Process_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getHR_Process_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_PeriodNo,
			(ValueUtil.getValueFromInteger(payrollMovement.getPeriodNo())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_ValidFrom,
			(ValueUtil.getValueFromDate(payrollMovement.getValidFrom())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_ValidTo,
			(ValueUtil.getValueFromDate(payrollMovement.getValidTo())).build()
		);

		// employee attributes
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);
		entityBulder.putValues(
			MHRMovement.COLUMNNAME_AD_Org_ID,
			(ValueUtil.getValueFromInteger(payrollMovement.getAD_Org_ID())).build()
		);

		return entityBulder;
	}


	@Override
	public void deletePayrollMovements(DeletePayrollMovementsRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			Empty.Builder entity = convertDeleteEntity(context, request);
			responseObserver.onNext(entity.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	Empty.Builder convertDeleteEntity(Properties context, DeletePayrollMovementsRequest request) {
		List<Integer> ids = request.getIdsList();

		List<Object> uuids = new ArrayList<Object>();
		request.getUuidsList().forEach(uuid -> {
			uuids.add(uuid);
		});
		
		if (ids.size() <= 0 && uuids.size() <= 0) {
			throw new AdempiereException("@NoRecordID@");
		}

		Trx.run(transactionName -> {
			String tableName = MHRMovement.Table_Name;

			// delete with id's
			if (ids.size() > 0) {
				MTable table = MTable.get(context, tableName);
				ids.stream().forEach(id -> {
					PO entity = table.getPO(id, transactionName);
					if (entity != null && entity.get_ID() > 0) {
						entity.deleteEx(true);
					}
				});
			}
			// delete with uuid's
			/*
			else {
				String sqlParams = " ?,".repeat(uuids.size());
				sqlParams = sqlParams.substring(0, sqlParams.length() - 1);
				String whereClause = MHRMovement.COLUMNNAME_UUID + " IN( " + sqlParams + " ) ";

				List<MHRMovement> payrollMovementsList = new Query(
						Env.getCtx(),
						tableName,
						whereClause,
						transactionName
					)
					.setParameters(uuids)
					.list();

				payrollMovementsList.forEach(payrollMovement -> {
					if (payrollMovement != null && payrollMovement.get_ID() > 0) {
						payrollMovement.deleteEx(true);
					}
				});
			}
			*/
		});
		//	Return
		return Empty.newBuilder();
	}

}
