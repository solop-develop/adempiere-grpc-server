/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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
package org.spin.grpc.service;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MMenu;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Login;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.authentication.services.OpenIDUtil;
import org.spin.backend.grpc.security.ChangeRoleRequest;
import org.spin.backend.grpc.security.Client;
import org.spin.backend.grpc.security.ListRolesRequest;
import org.spin.backend.grpc.security.ListRolesResponse;
import org.spin.backend.grpc.security.ListServicesRequest;
import org.spin.backend.grpc.security.ListServicesResponse;
import org.spin.backend.grpc.security.LoginOpenIDRequest;
import org.spin.backend.grpc.security.LoginRequest;
import org.spin.backend.grpc.security.LogoutRequest;
import org.spin.backend.grpc.security.Menu;
import org.spin.backend.grpc.security.MenuRequest;
import org.spin.backend.grpc.security.Role;
import org.spin.backend.grpc.security.SecurityGrpc.SecurityImplBase;
import org.spin.backend.grpc.security.Service;
import org.spin.backend.grpc.security.Session;
import org.spin.backend.grpc.security.SessionInfo;
import org.spin.backend.grpc.security.SessionInfoRequest;
import org.spin.backend.grpc.security.SetSessionAttributeRequest;
import org.spin.backend.grpc.security.UserInfo;
import org.spin.backend.grpc.security.UserInfoRequest;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.SessionManager;
import org.spin.model.MADAttachmentReference;
import org.spin.model.MADToken;
import org.spin.service.grpc.util.ValueManager;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Security service
 */
public class Security extends SecurityImplBase {
	
	/**
	 * Load Validators
	 */
	public Security() {
		super();
		DB.validateSupportedUUIDFromDB();
		MCountry.getCountries(Env.getCtx());
	}
	
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(Security.class);
	/**	Menu */
	private static CCache<String, Menu.Builder> menuCache = new CCache<String, Menu.Builder>("Menu_for_User", 30, 0);


	boolean isSessionContext(String contextKey) {
		return contextKey.startsWith("#") || contextKey.startsWith("$");
	}


	@Override
	public void runLogin(LoginRequest request, StreamObserver<Session> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Session Requested = " + request.getUserName());
			Session.Builder sessionBuilder = runLogin(request, true);
			responseObserver.onNext(sessionBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void runLoginOpenID(LoginOpenIDRequest request, StreamObserver<Session> responseObserver) {
		try {
			log.fine("Run Login Open ID");
			Session.Builder sessionBuilder = createSessionFromOpenID(request);
			responseObserver.onNext(sessionBuilder.build());
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
	public void listServices(ListServicesRequest request, StreamObserver<ListServicesResponse> responseObserver) {
		try {
			log.fine("List Services");
			ListServicesResponse.Builder serviceBuilder = ListServicesResponse.newBuilder();
			Hashtable<Integer, Map<String, String>> services = OpenIDUtil.getAuthenticationServices();
			services.entrySet().forEach(service -> {
				Service.Builder availableService = Service.newBuilder();
				availableService.setId(service.getKey())
					.setDisplayName(
						ValueManager.validateNull(
							service.getValue().get(OpenIDUtil.DISPLAYNAME)
						)
					)
					.setAuthorizationUri(
						ValueManager.validateNull(
							service.getValue().get(OpenIDUtil.ENDPOINT_Authorization_URI)
						)
					)
				;
				serviceBuilder.addServices(availableService);
			});
			responseObserver.onNext(serviceBuilder.build());
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
	public void runLogout(LogoutRequest request, StreamObserver<Session> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Session.Builder sessionBuilder = logoutSession(request);
			responseObserver.onNext(sessionBuilder.build());
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
	public void getSessionInfo(SessionInfoRequest request, StreamObserver<SessionInfo> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			SessionInfo.Builder sessionBuilder = getSessionInfo(request);
			responseObserver.onNext(sessionBuilder.build());
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
	public void setSessionAttribute(SetSessionAttributeRequest request, StreamObserver<Session> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Session.Builder sessionBuilder = setSessionAttribute(request);
			responseObserver.onNext(sessionBuilder.build());
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

	private Session.Builder setSessionAttribute(SetSessionAttributeRequest request) {
		Properties context = Env.getCtx();
		//	Language
		String language = Env.getAD_Language(context);
		int warehouseId = Env.getContextAsInt(context, "#M_Warehouse_ID");
		if (!Util.isEmpty(request.getLanguage())) {
			language = ContextManager.getDefaultLanguage(request.getLanguage());
		}
		MSession currentSession = MSession.get(context, false);
		// Session values
		Session.Builder builder = Session.newBuilder();
		final String bearerToken = SessionManager.createSession(currentSession.getWebSession(), language, currentSession.getAD_Role_ID(), currentSession.getCreatedBy(), currentSession.getAD_Org_ID(), warehouseId);
		builder.setToken(bearerToken);
		return builder;
	}


	@Override
	public void getUserInfo(UserInfoRequest request, StreamObserver<UserInfo> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			UserInfo.Builder UserInfo = getUserInfo(request);
			responseObserver.onNext(UserInfo.build());
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
	public void getMenu(MenuRequest request, StreamObserver<Menu> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Menu.Builder menuBuilder = convertMenu();
			responseObserver.onNext(menuBuilder.build());
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
	public void listRoles(ListRolesRequest request, StreamObserver<ListRolesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListRolesResponse.Builder rolesList = listRoles(request);
			responseObserver.onNext(rolesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	/**
	 * Convert languages to gRPC
	 * @param context
	 * @param request
	 * @return
	 */
	private ListRolesResponse.Builder listRoles(ListRolesRequest request) {
		int userId = Env.getAD_User_ID(Env.getCtx());

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		final String whereClause = "EXISTS("
				+ "SELECT 1 FROM AD_User_Roles ur "
				+ "WHERE ur.AD_Role_ID = AD_Role.AD_Role_ID AND ur.AD_User_ID = ?"
			+ ")"
			+ "AND ("
				+ "(IsAccessAllOrgs = 'Y' AND EXISTS(SELECT 1 FROM AD_Org AS o "
				+ "WHERE (o.AD_Client_ID = AD_Client_ID OR o.AD_Org_ID = 0) "
				+ "AND o.IsActive = 'Y' AND o.IsSummary = 'N'))"
			+ "OR ("
				+ "IsUseUserOrgAccess = 'N' AND EXISTS(SELECT 1 FROM AD_Role_OrgAccess AS ro "
				+ "INNER JOIN AD_Org AS o ON o.AD_Org_ID = ro.AD_Org_ID AND o.IsSummary = 'N' "
				+ "WHERE ro.AD_Role_ID = AD_Role.AD_Role_ID AND ro.IsActive = 'Y')) "
			+ "OR ("
				+ "IsUseUserOrgAccess = 'Y' AND EXISTS(SELECT 1 FROM AD_User_OrgAccess AS uo "
				+ "INNER JOIN AD_Org AS o ON o.AD_Org_ID = uo.AD_Org_ID AND o.IsSummary = 'N' "
				+ "WHERE uo.AD_User_ID = ? AND uo.IsActive = 'Y')) "
			+ ")"
		;
		Query query = new Query(
			Env.getCtx(),
			I_AD_Role.Table_Name,
			whereClause,
			null
		)
			.setParameters(userId, userId)
			.setOnlyActiveRecords(true)
		;
		int count = query.count();

		ListRolesResponse.Builder builder = ListRolesResponse.newBuilder();
		query.setLimit(limit, offset)
			.<MRole>list()
			.forEach(role -> {
				builder.addRoles(
					convertRole(role)
				);
			});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(count > offset && count > limit) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		//	Return
		return builder;
	}
	
	/**
	 * Get User ID
	 * @param userName
	 * @param userPass
	 * @return
	 */
	private int getUserId(String userName, String userPass) {
		Login login = new Login(Env.getCtx());
		return login.getAuthenticatedUserId(userName, userPass);
	}
	
	/**
	 * Get and convert session
	 * @param request
	 * @return
	 */
	private Session.Builder runLogin(LoginRequest request, boolean isDefaultRole) {
		//	Validate if is token based
		int userId = -1;
		int roleId = -1;
		int organizationId = -1;
		int warehouseId = -1;
		if(!Util.isEmpty(request.getToken())) {
			MADToken token = SessionManager.createSessionFromToken(request.getToken());
			if(Optional.ofNullable(token).isPresent()) {
				userId = token.getAD_User_ID();
				roleId = token.getAD_Role_ID();
				organizationId = token.getAD_Org_ID();
			}
		} else {
			userId = getUserId(request.getUserName(), request.getUserPass());
			//	Get Values from role
			if(userId < 0) {
				throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
			}
			MUser user = MUser.get(Env.getCtx(), userId);
			if (user == null) {
				throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
			}

			// TODO: Validate values
			if (request.getRoleId() >= 0) {
				roleId = request.getRoleId();
			}
			if(request.getOrganizationId() >= 0) {
				organizationId = request.getOrganizationId();
			}
			if(request.getWarehouseId() >= 0) {
				warehouseId = request.getWarehouseId();
			}
		}
		return createValidSession(isDefaultRole, request.getClientVersion(), request.getLanguage(), roleId, userId, organizationId, warehouseId);
	}
	
	/**
	 * Create Valid Session After Login
	 * @param isDefaultRole
	 * @param clientVersion
	 * @param language
	 * @param roleId
	 * @param userId
	 * @param organizationId
	 * @param warehouseId
	 * @return
	 */
	private Session.Builder createValidSession(boolean isDefaultRole, String clientVersion, String language, int roleId, int userId, int organizationId, int warehouseId) {
		Session.Builder builder = Session.newBuilder();
			if(isDefaultRole
					|| roleId <= 0) {
				roleId = SessionManager.getDefaultRoleId(userId);
			}
			//	Get Values from role
			if(roleId < 0) {
				throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
			}

			//	Organization
			if(organizationId <= 0) {
				organizationId = SessionManager.getDefaultOrganizationId(roleId, userId);
			}
			if(organizationId < 0) {
				throw new AdempiereException("@AD_User_ID@: @AD_Org_ID@ @NotFound@");
			}

			if (organizationId == 0) {
				warehouseId = 0;
			} else if (warehouseId <= 0) {
				warehouseId = SessionManager.getDefaultWarehouseId(organizationId);
			}

			//	Session values
			final String bearerToken = SessionManager.createSession(clientVersion, language, roleId, userId, organizationId, warehouseId);
			builder.setToken(bearerToken);
			//	Return session
			return builder;
	}
	
	/**
	 * Get and convert session
	 * @param request
	 * @return
	 */
	private Session.Builder createSessionFromOpenID(LoginOpenIDRequest request) {
		MUser validUser = OpenIDUtil.getUserAuthenticated(request.getCodeParameter(), request.getStateParameter());
		if(validUser == null) {
			throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
		}
		return createValidSession(true, request.getClientVersion(), request.getLanguage(), -1, validUser.getAD_User_ID(), -1, -1); 
	}


	/**
	 * Convert Values from Context
	 * @param value
	 * @return
	 */
//	private Value.Builder convertObjectFromContext(String value) {
//		Value.Builder builder = Value.newBuilder();
//		if (Util.isEmpty(value)) {
//			return builder;
//		}
//		if (ValueUtil.isNumeric(value)) {
//			builder.setIntValue(ValueUtil.getIntegerFromString(value));
//		} else if (ValueUtil.isBoolean(value)) {
//			boolean booleanValue = ValueUtil.stringToBoolean(value.trim());
//			builder.setBooleanValue(booleanValue);
//		} else if (ValueUtil.isDate(value)) {
//			return ValueUtil.getValueFromDate(ValueUtil.getDateFromString(value));
//		} else {
//			builder.setStringValue(ValueUtil.validateNull(value));
//		}
//		//	
//		return builder;
//	}
	
	@Override
	public void runChangeRole(ChangeRoleRequest request, StreamObserver<Session> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Session.Builder sessionBuilder = runChangeRole(request);
			responseObserver.onNext(sessionBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Change current role
	 * @param request
	 * @return
	 */
	private Session.Builder runChangeRole(ChangeRoleRequest request) {
		DB.validateSupportedUUIDFromDB();
		Properties context = Env.getCtx();
		//	Get / Validate Session
		MSession currentSession = MSession.get(context, false, false);
		int userId = currentSession.getCreatedBy();
		//	Get Values from role
		int roleId = request.getRoleId();
		if (roleId < 0) {
			throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
		}
		MRole role = MRole.get(context, roleId);

		// Get organization
		int organizationId = -1;
		if (request.getOrganizationId() >= 0) {
			organizationId = request.getOrganizationId();
			if (!role.isOrgAccess(organizationId, true)) {
				// invlaid organization from role
				organizationId = -1;
			}
		}
		if (organizationId < 0) {
			organizationId = SessionManager.getDefaultOrganizationId(roleId, userId);
			if (organizationId < 0) {
				// TODO: Verify it access
				organizationId = 0;
			}
		}

		// Get warehouse
		int warehouseId = -1;
		if (request.getWarehouseId() >= 0) {
			warehouseId = request.getWarehouseId();
		}
		if (warehouseId < 0) {
			warehouseId = 0;
		}

		// Default preference values
		String language = request.getLanguage();
		if (Util.isEmpty(language, true)) {
			// set language with current session
			language = Env.getContext(currentSession.getCtx(), Env.LANGUAGE);
		}

		// Session values
		Session.Builder builder = Session.newBuilder();
		final String bearerToken = SessionManager.createSession(currentSession.getWebSession(), language, roleId, userId, organizationId, warehouseId);
		builder.setToken(bearerToken);
		// Logout
		logoutSession(LogoutRequest.newBuilder().build());

		// Return session
		return builder;
	}


	/**
	 * Populate default values and preferences for session
	 * @param session
	 */
	private void populateDefaultPreferences(SessionInfo.Builder session) {
		MCountry country = MCountry.get(Env.getCtx(), Env.getContextAsInt(Env.getCtx(), "#C_Country_ID"));
		MCurrency currency = MCurrency.get(Env.getCtx(), country.getC_Currency_ID());
		//	Set values for currency
		session.setCountryId(country.getC_Country_ID());
		session.setCountryCode(
			ValueManager.validateNull(
				country.getCountryCode()
			)
		);
		session.setCountryName(
			ValueManager.validateNull(
				country.getName()
			)
		);
		session.setDisplaySequence(
			ValueManager.validateNull(
				country.getDisplaySequence()
			)
		);
		session.setCurrencyIsoCode(
			ValueManager.validateNull(currency.getISO_Code()));
		session.setCurrencyName(
			ValueManager.validateNull(currency.getDescription()));
		session.setCurrencySymbol(
			ValueManager.validateNull(currency.getCurSymbol()));
		session.setStandardPrecision(currency.getStdPrecision());
		session.setCostingPrecision(currency.getCostingPrecision());
		session.setLanguage(
			ValueManager.validateNull(ContextManager.getDefaultLanguage(Env.getAD_Language(Env.getCtx()))));
		//	Set default context
		Struct.Builder contextValues = Struct.newBuilder();
		Env.getCtx().entrySet().stream()
			.filter(keyValue -> isSessionContext(String.valueOf(keyValue.getKey())))
			.forEach(contextKeyValue -> {
				contextValues.putFields(
					contextKeyValue.getKey().toString(),
					convertObjectFromContext(
						(String) contextKeyValue.getValue()
					).build()
				);
			});
		session.setDefaultContext(contextValues);
	}
	
	private Value.Builder convertObjectFromContext(String value) {
		Value.Builder builder = Value.newBuilder();
		if (Util.isEmpty(value)) {
			return builder;
		}
		if (ValueManager.isNumeric(value)) {
			builder.setNumberValue(ValueManager.getIntegerFromString(value));
		} else if (ValueManager.isBoolean(value)) {
			boolean booleanValue = ValueManager.stringToBoolean(value.trim());
			builder.setBoolValue(booleanValue);
		} else if(ValueManager.isDate(value)) {
			return ValueManager.getValueFromDate(
				ValueManager.convertStringToDate(value)
			);
		} else {
			builder.setStringValue(
				ValueManager.validateNull(value)
			);
		}
		//	
		return builder;
	}

	
	
	/**
	 * Logout session
	 * @param request
	 * @return
	 */
	private Session.Builder logoutSession(LogoutRequest request) {
		MSession session = MSession.get(Env.getCtx(), false);
		//	Logout
		session.logout();
		//	Session values
		Session.Builder builder = Session.newBuilder();
		//	Return session
		return builder;
	}

	/**
	 * Logout session
	 * @param request
	 * @return
	 */
	private SessionInfo.Builder getSessionInfo(SessionInfoRequest request) {
		Properties context = Env.getCtx();
		MSession session = MSession.get(context, false);
		//	Load default preference values
		SessionManager.loadDefaultSessionValues(context, Env.getAD_Language(context));
		//	Session values
		SessionInfo.Builder builder = SessionInfo.newBuilder();
		builder.setId(session.getAD_Session_ID())
			.setName(
				ValueManager.validateNull(
					session.getDescription()
				)
			)
			.setUserInfo(
				convertUserInfo(
					MUser.get(context, session.getCreatedBy())
				).build()
			)
		;
		//	Set role
		Role.Builder roleBuilder = convertRole(
			MRole.get(context, session.getAD_Role_ID())
		);
		builder.setRole(roleBuilder.build());
		//	Set default context
		populateDefaultPreferences(builder);
		//	Return session
		return builder;
	}
	
	/**
	 * Convert User entity
	 * @param user
	 * @return
	 */
	private UserInfo.Builder convertUserInfo(MUser user) {
		UserInfo.Builder userInfo = UserInfo.newBuilder()
			.setId(user.getAD_User_ID())
			.setName(
				ValueManager.validateNull(
					user.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					user.getDescription()
				)
			)
			.setComments(
				ValueManager.validateNull(
					user.getComments()
				)
			)
		;
		if(user.getLogo_ID() > 0 && AttachmentUtil.getInstance().isValidForClient(user.getAD_Client_ID())) {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), user.getAD_Client_ID());
			MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(user.getCtx(), clientInfo.getFileHandler_ID(), user.getLogo_ID(), null);
			if(attachmentReference != null
					&& attachmentReference.getAD_AttachmentReference_ID() > 0) {
				userInfo.setImage(
					ValueManager.validateNull(
						attachmentReference.getValidFileName()
					)
				);
			}
		}
		userInfo.setConnectionTimeout(SessionManager.getSessionTimeout(user));
		return userInfo;
	}
	
	/**
	 * Get User Info
	 * @param request
	 * @return
	 */
	private UserInfo.Builder getUserInfo(UserInfoRequest request) {
		MSession session = MSession.get(Env.getCtx(), false);
		List<MRole> roleList = new Query(Env.getCtx(), I_AD_Role.Table_Name, 
				"EXISTS(SELECT 1 FROM AD_User_Roles ur "
				+ "WHERE ur.AD_Role_ID = AD_Role.AD_Role_ID "
				+ "AND ur.AD_User_ID = ?)", null)
				.setParameters(session.getCreatedBy())
				.setOnlyActiveRecords(true)
				.<MRole>list();
		//	Validate
		if(roleList == null
				|| roleList.size() == 0) {
			return null;
		}
		//	Get it
		MUser user = MUser.get(Env.getCtx(), session.getCreatedBy());
		if(user == null
				|| user.getAD_User_ID() <= 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	Return
		return convertUserInfo(user);
	}


	private Client.Builder convertClient(int clientId) {
		Client.Builder builder = Client.newBuilder();
		if (clientId < 0) {
			return builder;
		}
		MClient client = MClient.get(Env.getCtx(), clientId);
		if (client == null) {
			return builder;
		}
		builder.setId(client.getAD_Client_ID())
			.setName(
				ValueManager.validateNull(
					client.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					client.getDescription()
				)
			)
		;

		// Add client logo
		if (AttachmentUtil.getInstance().isValidForClient(client.getAD_Client_ID())) {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), client.getAD_Client_ID());
			if (clientInfo.getLogo_ID() > 0) {
				MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					clientInfo.getLogo_ID(),
					null
				);
				if (attachmentReference != null && attachmentReference.getAD_AttachmentReference_ID() > 0) {
					builder.setLogo(
						ValueManager.validateNull(
							attachmentReference.getValidFileName()
						)
					);
				}
			}
			if (clientInfo.getLogoReport_ID() > 0) {
				MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					clientInfo.getLogoReport_ID(),
					null
				);
				if (attachmentReference != null && attachmentReference.getAD_AttachmentReference_ID() > 0) {
					builder.setLogoReport(
						ValueManager.validateNull(
							attachmentReference.getValidFileName()
						)
					);
				}
			}
			if (clientInfo.getLogoWeb_ID() > 0) {
				MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					clientInfo.getLogoWeb_ID(),
					null
				);
				if (attachmentReference != null && attachmentReference.getAD_AttachmentReference_ID() > 0) {
					builder.setLogoWeb(
						ValueManager.validateNull(
							attachmentReference.getValidFileName()
						)
					);
				}
			}
		}

		return builder;
	}


	/**
	 * Convert role from model class
	 * @param role
	 * @param withAccess
	 * @return
	 */
	private Role.Builder convertRole(MRole role) {
		Role.Builder builder = Role.newBuilder();
		//	Validate
		if(role == null) {
			return builder;
		}
		Client.Builder clientBuilder = convertClient(role.getAD_Client_ID());
		builder = Role.newBuilder()
			.setId(role.getAD_Role_ID())
			.setName(
				ValueManager.validateNull(
					role.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					role.getDescription()
				)
			)
			.setClient(
				clientBuilder
			)
			.setIsCanExport(role.isCanExport())
			.setIsCanReport(role.isCanReport())
			.setIsPersonalAccess(role.isPersonalAccess())
			.setIsPersonalLock(role.isPersonalLock())
			.setIsAllowHtmlView(role.isAllow_HTML_View())
			.setIsAllowInfoAccount(role.isAllow_Info_Account())
			.setIsAllowInfoAsset(role.isAllow_Info_Asset())
			.setIsAllowInfoBusinessPartner(role.isAllow_Info_BPartner())
			.setIsAllowInfoCashJournal(role.isAllow_Info_CashJournal())
			.setIsAllowInfoCrp(role.isAllow_Info_CRP())
			.setIsAllowInfoInOut(role.isAllow_Info_InOut())
			.setIsAllowInfoInvoice(role.isAllow_Info_Invoice())
			.setIsAllowInfoMrp(role.isAllow_Info_MRP())
			.setIsAllowInfoOrder(role.isAllow_Info_Order())
			.setIsAllowInfoPayment(role.isAllow_Info_Payment())
			.setIsAllowInfoProduct(role.isAllow_Info_Product())
			.setIsAllowInfoResource(role.isAllow_Info_Resource())
			.setIsAllowInfoSchedule(role.isAllow_Info_Schedule())
			.setIsAllowXlsView(role.isAllow_XLS_View())
		;

		//	return
		return builder;
	}


	/**
	 * Convert Menu
	 * @return
	 */
	private Menu.Builder convertMenu() {
		int roleId = Env.getAD_Role_ID(Env.getCtx());
		int userId = Env.getAD_User_ID(Env.getCtx());
		String menuKey = roleId + "|" + userId + "|" + Env.getAD_Language(Env.getCtx());
		Menu.Builder builder = menuCache.get(menuKey);
		if(builder != null) {
			return builder;
		}
		builder = Menu.newBuilder();
		MMenu menu = new MMenu(Env.getCtx(), 0, null);
		menu.setName(Msg.getMsg(Env.getCtx(), "Menu"));
		//	Get Reference
		int treeId = DB.getSQLValue(null,
			"SELECT COALESCE(r.AD_Tree_Menu_ID, ci.AD_Tree_Menu_ID)" 
			+ "FROM AD_ClientInfo ci" 
			+ " INNER JOIN AD_Role r ON (ci.AD_Client_ID=r.AD_Client_ID) "
			+ "WHERE AD_Role_ID=?", roleId);
		if (treeId <= 0) {
			treeId = MTree.getDefaultTreeIdFromTableId(menu.getAD_Client_ID(), I_AD_Menu.Table_ID);
		}
		if(treeId != 0) {
			MTree tree = new MTree(Env.getCtx(), treeId, false, false, null, null);
			//	
			builder = convertMenu(Env.getCtx(), menu, 0, Env.getAD_Language(Env.getCtx()));
			//	Get main node
			MTreeNode rootNode = tree.getRoot();
			Enumeration<?> childrens = rootNode.children();
			while (childrens.hasMoreElements()) {
				MTreeNode child = (MTreeNode)childrens.nextElement();
				Menu.Builder childBuilder = convertMenu(Env.getCtx(), MMenu.getFromId(Env.getCtx(), child.getNode_ID()), child.getParent_ID(), Env.getAD_Language(Env.getCtx()));
				//	Explode child
				addChildren(Env.getCtx(), childBuilder, child, Env.getAD_Language(Env.getCtx()));
				builder.addChildren(childBuilder.build());
			}
		}
		//	Set from DB
		menuCache.put(menuKey, builder);
		return builder;
	}
	
	/**
	 * Convert Menu to builder
	 * @param context
	 * @param menu
	 * @param parentId
	 * @param language
	 * @param withChild
	 * @return
	 */
	private Menu.Builder convertMenu(Properties context, MMenu menu, int parentId, String language) {
		String name = null;
		String description = null;
		if(!Util.isEmpty(language)) {
			name = menu.get_Translation(I_AD_Menu.COLUMNNAME_Name, language);
			description = menu.get_Translation(I_AD_Menu.COLUMNNAME_Description, language);
		}
		//	Validate for default
		if(Util.isEmpty(name)) {
			name = menu.getName();
		}
		if(Util.isEmpty(description)) {
			description = menu.getDescription();
		}
		Menu.Builder builder = Menu.newBuilder()
				.setId(menu.getAD_Menu_ID())
				.setName(
					ValueManager.validateNull(name)
				)
				.setDescription(
					ValueManager.validateNull(description))
				.setAction(
					ValueManager.validateNull(
						menu.getAction()
					)
				)
				.setIsSOTrx(menu.isSOTrx())
				.setIsSummary(menu.isSummary())
				.setIsReadOnly(menu.isReadOnly())
				.setIsActive(menu.isActive());
		//	Supported actions
		if(!Util.isEmpty(menu.getAction())) {
			int referenceId = 0;
			if(menu.getAction().equals(MMenu.ACTION_Form)) {
				if(menu.getAD_Form_ID() > 0) {
					referenceId = menu.getAD_Form_ID();
				}
			} else if(menu.getAction().equals(MMenu.ACTION_Window)) {
				if(menu.getAD_Window_ID() > 0) {
					referenceId = menu.getAD_Window_ID();
				}
			} else if(menu.getAction().equals(MMenu.ACTION_Process)
				|| menu.getAction().equals(MMenu.ACTION_Report)) {
				if(menu.getAD_Process_ID() > 0) {
					referenceId = menu.getAD_Process_ID();
				}
			} else if(menu.getAction().equals(MMenu.ACTION_SmartBrowse)) {
				if(menu.getAD_Browse_ID() > 0) {
					referenceId = menu.getAD_Browse_ID();
				}
			} else if(menu.getAction().equals(MMenu.ACTION_WorkFlow)) {
				if(menu.getAD_Workflow_ID() > 0) {
					referenceId = menu.getAD_Workflow_ID();
				}
			}
			builder.setReferenceId(referenceId)
			;
		}
		return builder;
	}
	
	/**
	 * Add children to menu
	 * @param context
	 * @param builder
	 * @param node
	 * @param language
	 */
	private void addChildren(Properties context, Menu.Builder builder, MTreeNode node, String language) {
		Enumeration<?> childrens = node.children();
		while (childrens.hasMoreElements()) {
			MTreeNode child = (MTreeNode)childrens.nextElement();
			Menu.Builder childBuilder = convertMenu(context, MMenu.getFromId(context, child.getNode_ID()), child.getParent_ID(), language);
			addChildren(context, childBuilder, child, language);
			builder.addChildren(childBuilder.build());
		}
	}
}
