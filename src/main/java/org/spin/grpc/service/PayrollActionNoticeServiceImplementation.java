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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.MBPartner;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.eevolution.model.MHRConcept;
import org.eevolution.model.MHREmployee;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRPayroll;
import org.eevolution.model.MHRProcess;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.grpc.util.DeletePayrollMovementsRequest;
import org.spin.grpc.util.Empty;
import org.spin.grpc.util.Entity;
import org.spin.grpc.util.ListEmployeeValidRequest;
import org.spin.grpc.util.ListEntitiesResponse;
import org.spin.grpc.util.ListLookupItemsResponse;
import org.spin.grpc.util.ListPayrollConceptsRequest;
import org.spin.grpc.util.ListPayrollMovementsRequest;
import org.spin.grpc.util.ListPayrollProcessRequest;
import org.spin.grpc.util.LookupItem;
import org.spin.grpc.util.PayrollActionNoticeGrpc.PayrollActionNoticeImplBase;
import org.spin.grpc.util.SavePayrollMovementRequest;

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
			ListLookupItemsResponse.Builder lookupsList = convertPayrollConceptsList(context, request);
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

	private ListLookupItemsResponse.Builder convertPayrollConceptsList(Properties context, ListPayrollConceptsRequest request) {
		ListLookupItemsResponse.Builder lookupsList = ListLookupItemsResponse.newBuilder();

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (request.getContextAttributesList() == null || contextAttributesList.size() < 0) {
			return lookupsList;
		}

		int payrollProcessId = (int) contextAttributesList.get(MHRProcess.COLUMNNAME_HR_Process_ID);
		if (payrollProcessId <= 0) {
			return lookupsList;
		}
		int businessPartnerId = (int) contextAttributesList.get(MBPartner.COLUMNNAME_C_BPartner_ID);
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@BPartnerNotFound@");
		}
		MHREmployee employee = MHREmployee.getActiveEmployee(Env.getCtx(), businessPartnerId, null);
		if (employee == null || employee.getHR_Employee_ID() <= 0) {
			throw new AdempiereException("@BPartnerNotFound@ on ");
		}
		
		// Get Payroll Process
		MHRProcess payrollProcess = new MHRProcess(Env.getCtx(), payrollProcessId, null);

		List<Object> parameters = new ArrayList<>();
		String selectQuery = "SELECT hrpc.HR_Concept_ID, hrpc.Value || ' - ' || hrpc.Name AS DisplayColumn, hrpc.Value UUID "
			+ "FROM HR_Concept hrpc ";

		selectQuery = RecordUtil.addSearchValueAndGet(selectQuery, MHRProcess.Table_Name, "hrpc", request.getSearchValue(), parameters);
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
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ValueUtil.setParametersFromObjectsList(pstmt, parameters);
			
			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				//	1 = Key Column
				//	2 = UUID
				//	3 = Display Value
				int keyValue = rs.getInt(MHRConcept.COLUMNNAME_HR_Concept_ID);
				String uuid = rs.getString(MHRConcept.COLUMNNAME_UUID);
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
	public void listPayrollMovements(ListPayrollMovementsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			Properties context = ContextManager.getContext(request.getClientRequest());
			ListEntitiesResponse.Builder entitiesList = ListEntitiesResponse.newBuilder();
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

	@Override
	public void savePayrollMovement(SavePayrollMovementRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Properties context = ContextManager.getContext(request.getClientRequest());
			Entity.Builder entity = Entity.newBuilder();
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
		List<String> uuids = request.getUuidsList();

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
			else {
				String sqlParams = " ?,".repeat(uuids.size());
				sqlParams = sqlParams.substring(0, sqlParams.length() - 1);

				List<MHRMovement>payrollMovementsList = new Query(
						Env.getCtx(),
						tableName,
						MHRMovement.COLUMNNAME_UUID + " IN( " + sqlParams + " ) ",
						transactionName
					)
					.setParameters(uuids)
					.list(MHRMovement.class);

				payrollMovementsList.forEach(payrollMovement -> {
					if (payrollMovement != null && payrollMovement.get_ID() > 0) {
						payrollMovement.deleteEx(true);
					}
				});
			}
		});
		//	Return
		return Empty.newBuilder();
	}

}
