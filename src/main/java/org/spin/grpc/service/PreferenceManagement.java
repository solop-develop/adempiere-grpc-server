package org.spin.grpc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.core.domains.models.I_AD_Preference;
import org.adempiere.core.domains.models.X_AD_Role;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPreference;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.preference_management.DeletePreferenceRequest;
import org.spin.backend.grpc.preference_management.GetPreferenceRequest;
import org.spin.backend.grpc.preference_management.ListPreferencesRequest;
import org.spin.backend.grpc.preference_management.ListPreferencesResponse;
import org.spin.backend.grpc.preference_management.Preference;
import org.spin.backend.grpc.preference_management.PreferenceManagementGrpc.PreferenceManagementImplBase;
import org.spin.backend.grpc.preference_management.PreferenceType;
import org.spin.backend.grpc.preference_management.SetMultiplePreferencesRequest;
import org.spin.backend.grpc.preference_management.SetPreferenceRequest;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class PreferenceManagement extends PreferenceManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PreferenceManagement.class);


	/** Allows change User preference */
	public static final List<String> ALLOW_USER_PREFERENCE = Arrays.asList(
		X_AD_Role.PREFERENCETYPE_Client,
		X_AD_Role.PREFERENCETYPE_Organization,
		X_AD_Role.PREFERENCETYPE_User
	);

	/** Allows change Organization preference */
	public static final List<String> ALLOW_ORGANIZATION_PREFERENCE = Arrays.asList(
		X_AD_Role.PREFERENCETYPE_Client,
		X_AD_Role.PREFERENCETYPE_Organization
	);

	/** Allows change Client preference */
	public static final List<String> ALLOW_CLIENT_PREFERENCE = Arrays.asList(
		X_AD_Role.PREFERENCETYPE_Client
	);

	/** Allows change Window preference */
	public static final List<String> ALLOW_WINDOW_PREFERENCE = Arrays.asList(
		X_AD_Role.PREFERENCETYPE_Client
	);


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
	private MPreference getMPreference(int preferenceType, String attributeName, boolean isCurrentClient, boolean isCurrentOrganization, boolean isCurrentUser, boolean isCurrentContainer, int containerId) {
		return getMPreference(
			preferenceType,
			attributeName,
			isCurrentClient,
			isCurrentOrganization,
			isCurrentUser,
			isCurrentContainer,
			containerId,
			null
		);
	}
	private MPreference getMPreference(int preferenceType, String attributeName, boolean isCurrentClient, boolean isCurrentOrganization, boolean isCurrentUser, boolean isCurrentContainer, int containerId, String transactionName) {
		if (Util.isEmpty(attributeName, true)) {
			throw new AdempiereException("@FillMandatory@ @Attribute@ / @AD_Column_ID@");
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
			transactionName
		)
			.setParameters(parameters)
			.first()
		;

		return preference;
	}


	private void validatePreferenceAccess(String currentLevel, MPreference existingPreference) {
		boolean isClientPreference = existingPreference.getAD_Client_ID() > 0;
		boolean isOrgPreference = existingPreference.getAD_Org_ID() > 0 && !isClientPreference;
		// boolean isUserPreference = existingPreference.getAD_User_ID() > 0 && !isOrgPreference && !isClientPreference;

		if (X_AD_Role.PREFERENCETYPE_None.equals(currentLevel)) {
			throw new AdempiereException("@CannotModifyPreference@");
		}
		if (X_AD_Role.PREFERENCETYPE_User.equals(currentLevel) && (isOrgPreference || isClientPreference)) {
			throw new AdempiereException("@CannotModifyHigherLevelPreference@");
		}
		if (X_AD_Role.PREFERENCETYPE_Organization.equals(currentLevel) && isClientPreference) {
			throw new AdempiereException("@CannotModifyClientLevelPreference@");
		}
	}


	Preference.Builder convertPreference(MPreference preference) {
		Preference.Builder builder = Preference.newBuilder();
		if (preference == null || preference.getAD_Preference_ID() <= 0) {
			return builder;
		}
		builder
			.setId(
				preference.getAD_Preference_ID()
			)
			.setUuid(
				StringManager.getValidString(
					preference.getUUID()
				)
			)
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
				StringManager.getValidString(
					preference.getAttribute()
				)
			)
			.setValue(
				StringManager.getValidString(
					preference.getValue()
				)
			)
		;
		//	
		return builder;
	}



	@Override
	public void getPreference(GetPreferenceRequest request, StreamObserver<Preference> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object GetPreferenceRequest Null");
			}
			//	Get Preference
			Preference.Builder preferenceBuilder = getPreference(request);
			responseObserver.onNext(preferenceBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
		MPreference preference = null;
		if (request.getId() > 0) {
			preference = new MPreference(Env.getCtx(), request.getId(), null);
		} else {
			preference = getMPreference(
				request.getTypeValue(),
				request.getColumnName(),
				request.getIsForCurrentClient(),
				request.getIsForCurrentOrganization(),
				request.getIsForCurrentUser(),
				request.getIsForCurrentContainer(),
				request.getContainerId()
			);
		}

		Preference.Builder preferenceBuilder = convertPreference(
			preference
		);
		return preferenceBuilder;
	}



	@Override
	public void listPreferences(ListPreferencesRequest request, StreamObserver<ListPreferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListPreferencesRequest Null");
			}
			ListPreferencesResponse.Builder preferenceBuilder = listPreferences(request);
			responseObserver.onNext(preferenceBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
				);
		}
	}
	ListPreferencesResponse.Builder listPreferences(ListPreferencesRequest request) {
		StringBuffer whereClause = new StringBuffer();
		List<Object> parameters = new ArrayList<>();

		//	For client
		whereClause.append("AD_Client_ID IN (0, ?) ");
		if(request.getIsForCurrentClient()) {
			parameters.add(Env.getAD_Client_ID(Env.getCtx()));
		} else {
			parameters.add(0);
		}

		//	For Organization
		whereClause.append("AND AD_Org_ID IN (0, ?) ");
		if(request.getIsForCurrentOrganization()) {
			parameters.add(Env.getAD_Org_ID(Env.getCtx()));
		} else {
			parameters.add(0);
		}

		// For User
		// if(request.getIsForCurrentUser()) {
			parameters.add(Env.getAD_User_ID(Env.getCtx()));
			whereClause.append("AND AD_User_ID = ? ");
		// } else {
		// 	whereClause.append("AND AD_User_ID IS NULL ");
		// }

		if(request.getTypeValue() == PreferenceType.WINDOW_VALUE) {
			//	For Window
			if (request.getIsForCurrentContainer()) {
				if (request.getContainerId() <= 0) {
					throw new AdempiereException("@FillMandatory@ @AD_Window_ID@");
				}
				parameters.add(request.getContainerId());
				whereClause.append(" AND AD_Window_ID = ?");
			}
		} else {
			whereClause.append("AND AD_Window_ID IS NULL ");
		}

		Query query = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setOrderBy("ORDER BY AD_Client_ID DESC")
		;
		
		ListPreferencesResponse.Builder builderList = ListPreferencesResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;
		query.getIDsAsList().forEach(preferenceId -> {
			MPreference preference = new MPreference(Env.getCtx(), preferenceId, null);
			Preference.Builder builder = convertPreference(preference);
			builderList.addRecords(builder);
		});

		return builderList;
	}



	@Override
	public void setPreference(SetPreferenceRequest request, StreamObserver<Preference> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object SetPreferenceRequest Null");
			}
			//	Save preference
			Preference.Builder preferenceBuilder = setPreference(request);
			responseObserver.onNext(preferenceBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
		if (Util.isEmpty(request.getValue(), true)) {
			throw new AdempiereException("@FillMandatory@ @Value@");
		}
		final String columnName = request.getColumnName();
		if (columnName.startsWith("$") || columnName.startsWith("#")) {
			throw new AdempiereException("@invalid@ @ColumnName@ / @Attribute@ " + columnName);
		}

		MRole role = MRole.getDefault();
		final String preferenceLevel = role.getPreferenceType();
		if (X_AD_Role.PREFERENCETYPE_None.equals(preferenceLevel)) {
			throw new AdempiereException("@PreferenceType@ @None@");
		}

		MPreference preference = null;
		if (request.getId() > 0) {
			preference = new MPreference(Env.getCtx(), request.getId(), null);
		} else {
			preference = getMPreference(
				request.getTypeValue(),
				columnName,
				request.getIsForCurrentClient(),
				request.getIsForCurrentOrganization(),
				request.getIsForCurrentUser(),
				request.getIsForCurrentContainer(),
				request.getContainerId()
			);
		}

		// is new record
		if (preference == null || preference.getAD_Preference_ID() <= 0) {
			preference = new MPreference(Env.getCtx(), 0, null);
			preference.setAttribute(columnName);
		} else {
			validatePreferenceAccess(preferenceLevel, preference);
		}
		preference.setValue(
			request.getValue()
		);

		// For User
		if (ALLOW_USER_PREFERENCE.contains(preferenceLevel)) {
			int userId = Env.getAD_User_ID(Env.getCtx());
			if(!request.getIsForCurrentUser()) {
				userId = -1;
			}
			preference.setAD_User_ID(userId);
		}

		// For Organization
		if (ALLOW_ORGANIZATION_PREFERENCE.contains(preferenceLevel)) {
			int orgId = Env.getAD_Org_ID(Env.getCtx());
			if(!request.getIsForCurrentOrganization()) {
				orgId = 0;
			}
			preference.setAD_Org_ID(orgId);
		}

		// For Client
		if (ALLOW_CLIENT_PREFERENCE.contains(preferenceLevel)) {
			int clientId = Env.getAD_Client_ID(Env.getCtx());
			if(!request.getIsForCurrentClient()) {
				// System as all clients
				clientId = 0;
			}
			preference.set_ValueOfColumn(
				I_AD_Preference.COLUMNNAME_AD_Client_ID,
				clientId
			);
		}

		// For Window ((System))
		if (ALLOW_WINDOW_PREFERENCE.contains(preferenceLevel)) {
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

		preference.saveEx();

		// builder convert
		Preference.Builder builder = convertPreference(preference);
		return builder;
	}



	@Override
	public void setMultiplePreferences(SetMultiplePreferencesRequest request, StreamObserver<ListPreferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object SetMultiplePreferencesRequest Null");
			}
			//	Save preference
			ListPreferencesResponse.Builder preferenceBuilder = setMultiplePreferences(request);
			responseObserver.onNext(preferenceBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
				);
		}
	}
	ListPreferencesResponse.Builder setMultiplePreferences(SetMultiplePreferencesRequest request) {
		MRole role = MRole.getDefault();
		final String preferenceLevel = role.getPreferenceType();
		if (X_AD_Role.PREFERENCETYPE_None.equals(preferenceLevel)) {
			throw new AdempiereException("@PreferenceType@ @None@");
		}

		ListPreferencesResponse.Builder builderList = ListPreferencesResponse.newBuilder();
		AtomicInteger recordCount = new AtomicInteger();

		Trx.run(transactionName -> {
			Map<String, Value> preferences = request.getPreferences().getFieldsMap();
			preferences.entrySet().forEach(preferenceItem -> {
				String columnName = preferenceItem.getKey();
				if (columnName.startsWith("$") || columnName.startsWith("#")) {
					throw new AdempiereException("@invalid@ @ColumnName@ / @Attribute@ " + columnName);
				}
				Value preferenceValue = preferenceItem.getValue();
				Object value = ValueManager.getObjectFromValue(preferenceValue);
				String valueString = StringManager.getStringFromObject(value);
				if (Util.isEmpty(valueString, true)) {
					throw new AdempiereException(columnName + " @FillMandatory@ @Value@");
				}

				MPreference preference = getMPreference(
					request.getTypeValue(),
					columnName,
					request.getIsForCurrentClient(),
					request.getIsForCurrentOrganization(),
					request.getIsForCurrentUser(),
					request.getIsForCurrentContainer(),
					request.getContainerId(),
					transactionName
				);

				// is new record
				if (preference == null || preference.getAD_Preference_ID() <= 0) {
					preference = new MPreference(Env.getCtx(), 0, transactionName);
					preference.setAttribute(columnName);
				} else {
					validatePreferenceAccess(preferenceLevel, preference);
				}
				preference.setValue(valueString);

				// For User
				if (ALLOW_USER_PREFERENCE.contains(preferenceLevel)) {
					int userId = Env.getAD_User_ID(Env.getCtx());
					if(!request.getIsForCurrentUser()) {
						userId = -1;
					}
					preference.setAD_User_ID(userId);
				}

				// For Organization
				if (ALLOW_ORGANIZATION_PREFERENCE.contains(preferenceLevel)) {
					int orgId = Env.getAD_Org_ID(Env.getCtx());
					if(!request.getIsForCurrentOrganization()) {
						orgId = 0;
					}
					preference.setAD_Org_ID(orgId);
				}

				// For Client
				if (ALLOW_CLIENT_PREFERENCE.contains(preferenceLevel)) {
					int clientId = Env.getAD_Client_ID(Env.getCtx());
					if(!request.getIsForCurrentClient()) {
						// System as all clients
						clientId = 0;
					}
					preference.set_ValueOfColumn(
						I_AD_Preference.COLUMNNAME_AD_Client_ID,
						clientId
					);
				}

				// For Window ((System))
				if (ALLOW_WINDOW_PREFERENCE.contains(preferenceLevel)) {
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

				preference.saveEx(transactionName);

				// builder convert
				recordCount.addAndGet(1);
				Preference.Builder builder = convertPreference(preference);
				builderList.addRecords(builder);
			});
		});

		builderList.setRecordCount(
			recordCount.get()
		);
		return builderList;
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
			log.warning(e.getLocalizedMessage());
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
		MRole role = MRole.getDefault();
		final String preferenceLevel = role.getPreferenceType();
		if (X_AD_Role.PREFERENCETYPE_None.equals(preferenceLevel)) {
			throw new AdempiereException("@PreferenceType@ @None@");
		}

		MPreference preference = null;
		if (request.getId() > 0) {
			preference = new MPreference(Env.getCtx(), request.getId(), null);
		} else {
			preference = getMPreference(
				request.getTypeValue(),
				request.getColumnName(),
				request.getIsForCurrentClient(),
				request.getIsForCurrentOrganization(),
				request.getIsForCurrentUser(),
				request.getIsForCurrentContainer(),
				request.getContainerId()
			);
		}
		if (preference != null && preference.getAD_Preference_ID() > 0) {
			validatePreferenceAccess(preferenceLevel, preference);

			preference.deleteEx(true);
		}
	}

}
