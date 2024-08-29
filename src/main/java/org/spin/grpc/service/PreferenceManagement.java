package org.spin.grpc.service;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Preference;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPreference;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.preference_management.DeletePreferenceRequest;
import org.spin.backend.grpc.preference_management.GetPreferenceRequest;
import org.spin.backend.grpc.preference_management.Preference;
import org.spin.backend.grpc.preference_management.PreferenceManagementGrpc.PreferenceManagementImplBase;
import org.spin.backend.grpc.preference_management.PreferenceType;
import org.spin.backend.grpc.preference_management.SetPreferenceRequest;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class PreferenceManagement extends PreferenceManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PreferenceManagement.class);


	@Override
	public void getPreference(GetPreferenceRequest request, StreamObserver<Preference> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object GetPreferenceRequest Null");
			}
			//	Save preference
			Preference.Builder preferenceBuilder = getPreference(request);
			responseObserver.onNext(preferenceBuilder.build());
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
	Preference.Builder getPreference(GetPreferenceRequest request) {
		MPreference preference = getPreference(
			request.getTypeValue(),
			request.getColumnName(),
			request.getIsForCurrentClient(),
			request.getIsForCurrentOrganization(),
			request.getIsForCurrentUser(),
			request.getIsForCurrentContainer(),
			request.getContainerId()
		);

		Preference.Builder preferenceBuilder = convertPreference(
			preference
		);
		return preferenceBuilder;
	}



	@Override
	public void setPreference(SetPreferenceRequest request, StreamObserver<Preference> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			//	Save preference
			Preference.Builder preferenceBuilder = setPreference(request);
			responseObserver.onNext(preferenceBuilder.build());
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
	Preference.Builder setPreference(SetPreferenceRequest request) {
		MPreference preference = getPreference(
			request.getTypeValue(),
			request.getColumnName(),
			request.getIsForCurrentClient(),
			request.getIsForCurrentOrganization(),
			request.getIsForCurrentUser(),
			request.getIsForCurrentContainer(),
			request.getContainerId()
		);

		// is new record
		if (preference == null || preference.getAD_Preference_ID() <= 0) {
			preference = new MPreference(Env.getCtx(), 0, null);
			preference.setAttribute(
				request.getColumnName()
			);
			
			// For client
			int clientId = Env.getAD_Client_ID(Env.getCtx());
			if(!request.getIsForCurrentClient()) {
				clientId = 0;
			}
			preference.set_ValueOfColumn(
				I_AD_Preference.COLUMNNAME_AD_Client_ID,
				clientId
			);

			// For Organization
			int orgId = Env.getAD_Org_ID(Env.getCtx());
			if(!request.getIsForCurrentOrganization()) {
				orgId = 0;
			}
			preference.setAD_Org_ID(orgId);

			// For User
			int userId = Env.getAD_User_ID(Env.getCtx());
			if(!request.getIsForCurrentUser()) {
				userId = -1;
			}
			preference.setAD_User_ID(userId);

			// For Window
			if (request.getTypeValue() == PreferenceType.WINDOW_VALUE) {
				int windowId = request.getContainerId();
				if(!request.getIsForCurrentContainer()) {
					windowId = -1;
				} else {
					if (windowId <= 0) {
						throw new AdempiereException("@FillMandatory@ @AD_Window_ID@");
					}
				}
				preference.setAD_Window_ID(windowId);
			}
		}

		preference.setValue(
			request.getValue()
		);
		preference.saveEx();

		// builder convert
		Preference.Builder builder = convertPreference(preference);
		return builder;
	}

	Preference.Builder convertPreference(MPreference preference) {
		Preference.Builder builder = Preference.newBuilder();
		if (preference == null || preference.getAD_Preference_ID() <= 0) {
			return builder;
		}
		builder
			.setClientId(
				preference.getAD_Client_ID()
			)
			.setOrganizationId(
				preference.getAD_Org_ID()
			)
			.setUserId(
				preference.getAD_User_ID()
			)
			.setContainerId(
				preference.getAD_Window_ID()
			)
			.setColumnName(
				ValueManager.validateNull(
					preference.getAttribute()
				)
			)
			.setValue(
				ValueManager.validateNull(
					preference.getValue()
				)
			)
		;
		//	
		return builder;
	}


	@Override
	public void deletePreference(DeletePreferenceRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			deletePreference(request);
			Empty.Builder empty = Empty.newBuilder();
			responseObserver.onNext(empty.build());
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
	private void deletePreference(DeletePreferenceRequest request) {
		MPreference preference = getPreference(
			request.getTypeValue(),
			request.getColumnName(),
			request.getIsForCurrentClient(),
			request.getIsForCurrentOrganization(),
			request.getIsForCurrentUser(),
			request.getIsForCurrentContainer(),
			request.getContainerId()
		);
		if (preference != null && preference.getAD_Preference_ID() > 0) {
			preference.deleteEx(true);
		}
	}

	
	/**
	 * Get preference
	 * @param preferenceType
	 * @param attribute
	 * @param isCurrentClient
	 * @param isCurrentOrganization
	 * @param isCurrentUser
	 * @param isCurrentContainer
	 * @param id
	 * @return
	 */
	private MPreference getPreference(int preferenceType, String attributeName, boolean isCurrentClient, boolean isCurrentOrganization, boolean isCurrentUser, boolean isCurrentContainer, int containerId) {
		if (Util.isEmpty(attributeName, true)) {
			throw new AdempiereException("@FillMandatory@ @Attribute@");
		}

		List<Object> parameters = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer("Attribute = ? ");
		parameters.add(attributeName);

		//	For client
		whereClause.append("AND AD_Client_ID = ? ");
		if(isCurrentClient) {
			parameters.add(Env.getAD_Client_ID(Env.getCtx()));
		} else {
			parameters.add(0);
		}

		//	For Organization
		whereClause.append("AND AD_Org_ID = ? ");
		if(isCurrentOrganization) {
			parameters.add(Env.getAD_Org_ID(Env.getCtx()));
		} else {
			parameters.add(0);
		}

		// For User
		if(isCurrentUser) {
			parameters.add(Env.getAD_User_ID(Env.getCtx()));
			whereClause.append("AND AD_User_ID = ? ");
		} else {
			whereClause.append("AND AD_User_ID IS NULL ");
		}

		if(preferenceType == PreferenceType.WINDOW_VALUE) {
			//	For Window
			if (isCurrentContainer) {
				if (containerId <= 0) {
					throw new AdempiereException("@FillMandatory@ @AD_Window_ID@");
				}
				parameters.add(containerId);
				whereClause.append(" AND AD_Window_ID = ?");
			}
		} else {
			whereClause.append("AND AD_Window_ID IS NULL ");
		}

		MPreference preference = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.first();

		return preference;
	}

}
