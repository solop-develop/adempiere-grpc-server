/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_EntityType;
import org.adempiere.core.domains.models.I_AD_Migration;
import org.adempiere.core.domains.models.I_AD_MigrationData;
import org.adempiere.core.domains.models.I_AD_MigrationStep;
import org.adempiere.core.domains.models.I_AD_Modification;
import org.adempiere.core.domains.models.X_AD_Modification;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MEntityType;
import org.compiere.model.MMigration;
import org.compiere.model.MMigrationData;
import org.compiere.model.MMigrationStep;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.update.ListPackagesRequest;
import org.spin.backend.grpc.update.ListPackagesResponse;
import org.spin.backend.grpc.update.ListStepsRequest;
import org.spin.backend.grpc.update.ListStepsResponse;
import org.spin.backend.grpc.update.ListUpdatesRequest;
import org.spin.backend.grpc.update.ListUpdatesResponse;
import org.spin.backend.grpc.update.Package;
import org.spin.backend.grpc.update.PackageVersion;
import org.spin.backend.grpc.update.Step;
import org.spin.backend.grpc.update.StepValue;
import org.spin.backend.grpc.update.Update;
import org.spin.backend.grpc.update.UpdateCenterGrpc.UpdateCenterImplBase;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Service for backend of Update Center
 */
public class UpdateManagement extends UpdateCenterImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(UpdateManagement.class);
	
	@Override
	public void listPackages(ListPackagesRequest request, StreamObserver<ListPackagesResponse> responseObserver) {
		try {
			ListPackagesResponse.Builder packagesList = convertPackagesList(request);
			responseObserver.onNext(packagesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listUpdates(ListUpdatesRequest request, StreamObserver<ListUpdatesResponse> responseObserver) {
		try {
			ListUpdatesResponse.Builder updatesList = convertUpdatesList(request);
			responseObserver.onNext(updatesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listSteps(ListStepsRequest request, StreamObserver<ListStepsResponse> responseObserver) {
		try {
			ListStepsResponse.Builder stepsList = convertStepsList(request);
			responseObserver.onNext(stepsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	/**
	 * Get steps from update
	 * @param request
	 * @return
	 */
	private ListStepsResponse.Builder convertStepsList(ListStepsRequest request) {
		ListStepsResponse.Builder builder = ListStepsResponse.newBuilder();
		//	Get Migration
		int migrationId = request.getUpdateId();
		//	Validate
		if(request.getUpdateId() < 0) {
			throw new AdempiereException("@AD_Migration_ID@ @NotFound@");
		}
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber("page-token", request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get POS List
		StringBuffer whereClause = new StringBuffer("AD_Migration_ID = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(migrationId);
		if(request.getFromStep() > 0) {
			parameters.add(request.getFromStep());
			whereClause.append(" AND ").append(I_AD_MigrationStep.COLUMNNAME_SeqNo).append(" >= ?");
		}
		//	Get
		Query query = new Query(Env.getCtx(), I_AD_MigrationStep.Table_Name , whereClause.toString(), null)
				.setOnlyActiveRecords(true)
				.setParameters(parameters)
				.setOrderBy(I_AD_MigrationStep.COLUMNNAME_SeqNo);
		int count = query.count();
		query
			.setLimit(limit, offset)
			.<MMigrationStep>list()
			.forEach(migration -> builder.addSteps(convertStep(migration)));
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix("page-token") + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}
	
	/**
	 * Convert step
	 * @param migrationStep
	 * @return
	 */
	private Step.Builder convertStep(MMigrationStep migrationStep) {
		Step.Builder builder = Step.newBuilder()
				.setId(migrationStep.getAD_MigrationStep_ID())
				.setAction(ValueUtil.validateNull(migrationStep.getAction()))
				.setComments(ValueUtil.validateNull(migrationStep.getComments()))
				.setStepType(ValueUtil.validateNull(migrationStep.getStepType()))
				.setTableId(migrationStep.getAD_Table_ID())
				.setRecordId(migrationStep.getRecord_ID())
				.setIsParsed(migrationStep.isParse())
				.setSequence(migrationStep.getSeqNo())
				.setSqlStatement(ValueUtil.validateNull(migrationStep.getSQLStatement()))
				.setRollbackStatement(ValueUtil.validateNull(migrationStep.getRollbackStatement()));
		new Query(Env.getCtx(), I_AD_MigrationData.Table_Name, I_AD_MigrationData.COLUMNNAME_AD_MigrationStep_ID + " = ?", null)
			.setParameters(migrationStep.getAD_MigrationStep_ID())
			.<MMigrationData>list()
			.forEach(modification -> builder.addStepValues(convertStepValue(modification)));
		return builder;
	}
	
	/**
	 * Convert step value
	 * @param migrationData
	 * @return
	 */
	private StepValue.Builder convertStepValue(MMigrationData migrationData) {
		StepValue.Builder builder = StepValue.newBuilder()
				.setId(migrationData.getAD_MigrationData_ID())
				.setColumnId(migrationData.getAD_Column_ID())
				.setOldValue(ValueUtil.validateNull(migrationData.getOldValue()))
				.setBackupValue(ValueUtil.validateNull(migrationData.getBackupValue()))
				.setNewValue(ValueUtil.validateNull(migrationData.getNewValue()))
				.setIsOldNull(migrationData.isOldNull())
				.setIsBackupNull(migrationData.isBackupNull())
				.setIsNewNull(migrationData.isNewNull());
		return builder;
	}
	
	/**
	 * Get available updates
	 * @param request
	 * @return
	 */
	private ListUpdatesResponse.Builder convertUpdatesList(ListUpdatesRequest request) {
		ListUpdatesResponse.Builder builder = ListUpdatesResponse.newBuilder();
		//	Validate Release
		if(Util.isEmpty(request.getEntityType())) {
			throw new AdempiereException("@EntityType@ @NotFound@");
		}
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber("page-token", request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get POS List
		StringBuffer whereClause = new StringBuffer("EntityType = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(request.getEntityType());
		if(!Util.isEmpty(request.getReleaseNo())) {
			parameters.add(request.getReleaseNo());
			whereClause.append(" AND ReleaseNo = ?");
		}
		//	Get
		Query query = new Query(Env.getCtx(), I_AD_Migration.Table_Name , whereClause.toString(), null)
				.setOnlyActiveRecords(true)
				.setParameters(parameters)
				.setOrderBy(I_AD_Migration.COLUMNNAME_SeqNo);
		int count = query.count();
		query
			.setLimit(limit, offset)
			.<MMigration>list()
			.forEach(migration -> builder.addUpdates(convertMigration(migration)));
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix("page-token") + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}
	
	/**
	 * Convert migration
	 * @param entityType
	 * @return
	 */
	private Update.Builder convertMigration(MMigration migration) {
		return Update.newBuilder()
				.setId(migration.getAD_Migration_ID())
				.setEntityType(ValueUtil.validateNull(migration.getEntityType()))
				.setSequence(migration.getSeqNo())
				.setName(ValueUtil.validateNull(migration.getName()))
				.setComments(ValueUtil.validateNull(migration.getComments()))
				.setReleaseNo(ValueUtil.validateNull(migration.getReleaseNo()))
				.setStepQuantity(getStepQuantity(migration));
	}
	
	/**
	 * Get Step quantity
	 * @param migration
	 * @return
	 */
	private int getStepQuantity(MMigration migration) {
		return new Query(Env.getCtx(), I_AD_MigrationStep.Table_Name, I_AD_MigrationStep.COLUMNNAME_AD_Migration_ID + " = ?", null)
				.setParameters(migration.getAD_Migration_ID())
				.count();
	}
	
	/**
	 * Get packages list
	 * @param request
	 * @return
	 */
	private ListPackagesResponse.Builder convertPackagesList(ListPackagesRequest request) {
		ListPackagesResponse.Builder builder = ListPackagesResponse.newBuilder();
		//	Validate Release
		if(Util.isEmpty(request.getReleaseNo())) {
			throw new AdempiereException("@ReleaseNo@ @NotFound@");
		}
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber("page-token", request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get POS List
		String whereClause = null;
		List<Object> parameters = new ArrayList<>();
		if(!Util.isEmpty(request.getVersion())) {
			whereClause = "Version = ? OR EXISTS(SELECT 1 FROM AD_Modification WHERE EntityType = AD_EntityType.EntityType AND Version = ?)";
			parameters.add(request.getVersion());
			parameters.add(request.getVersion());
		}
		//	Get
		Query query = new Query(Env.getCtx() , I_AD_EntityType.Table_Name , whereClause, null)
				.setOnlyActiveRecords(true)
				.setOrderBy(I_AD_EntityType.COLUMNNAME_Name);
		//	Set where clause
		if(whereClause != null) {
			query.setParameters(parameters);
		}
		int count = query.count();
		query
			.setLimit(limit, offset)
			.<MEntityType>list()
			.forEach(packageValue -> builder.addPackages(convertPackage(packageValue)));
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix("page-token") + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}
	
	/**
	 * Convert package
	 * @param entityType
	 * @return
	 */
	private Package.Builder convertPackage(MEntityType entityType) {
		Package.Builder builder = Package.newBuilder()
				.setId(entityType.getAD_EntityType_ID())
				.setEntityType(ValueUtil.validateNull(entityType.getEntityType()))
				.setName(ValueUtil.validateNull(entityType.getName()))
				.setDescription(ValueUtil.validateNull(entityType.getDescription()))
				.setHelp(ValueUtil.validateNull(entityType.getHelp()))
				.setVersion(ValueUtil.validateNull(entityType.getVersion()))
				.setModelPackage(ValueUtil.validateNull(entityType.getModelPackage()));
		new Query(Env.getCtx(), I_AD_Modification.Table_Name, I_AD_Modification.COLUMNNAME_EntityType + " = ?", null)
			.setParameters(entityType.getEntityType())
			.setOrderBy(I_AD_Modification.COLUMNNAME_SeqNo)
			.<X_AD_Modification>list()
			.forEach(modification -> builder.addVersions(convertPackageVersion(modification)));
		return builder;
	}
	
	/**
	 * Convert package version
	 * @param entityType
	 * @return
	 */
	private PackageVersion.Builder convertPackageVersion(X_AD_Modification modification) {
		return PackageVersion.newBuilder()
				.setId(modification.getAD_Modification_ID())
				.setSequence(modification.getSeqNo())
				.setName(ValueUtil.validateNull(modification.getName()))
				.setDescription(ValueUtil.validateNull(modification.getDescription()))
				.setHelp(ValueUtil.validateNull(modification.getHelp()))
				.setVersion(ValueUtil.validateNull(modification.getVersion()));
	}
}
