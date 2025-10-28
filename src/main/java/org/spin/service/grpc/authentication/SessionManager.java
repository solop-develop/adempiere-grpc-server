/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.service.grpc.authentication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import javax.crypto.SecretKey;

import org.adempiere.core.domains.models.I_AD_Language;
import org.adempiere.core.domains.models.I_AD_Session;
import org.adempiere.core.domains.models.I_AD_User_Authentication;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MUserAuthentication;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.model.MOrg;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Language;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.eca52.util.JWTUtil;
import org.spin.model.MADToken;
import org.spin.model.MADTokenDefinition;
import org.spin.service.grpc.util.base.PreferenceUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.util.IThirdPartyAccessGenerator;
import org.spin.util.ITokenGenerator;
import org.spin.util.TokenGeneratorHandler;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Class for handle Session for Third Party Access
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class SessionManager {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(SessionManager.class);

	private static int userId = -1;
	private static int roleId = -1;
	private static int organizationId = -1;
	private static int warehouseId = -1;
	private static String language = "en_US";

	/**	Language */
	private static CCache<String, String> languageCache = new CCache<String, String>(I_AD_Language.Table_Name, 30, 0);	//	no time-out

	/**	Open ID token with Session ID */
	private static CCache<String, Integer> openIdSessionCache = new CCache<String, Integer>(I_AD_Session.Table_Name, 30, 0);	//	no time-out
	private static CCache<Integer, String> sessionOpenIDCCache = new CCache<Integer, String>(I_AD_Session.Table_Name, 30, 0);	//	no time-out

	/**	Session Context	*/
	private static final Map<String, Properties> sessionsContext = Collections.synchronizedMap(new Hashtable<String, Properties>());

	public static void revokeSession(String token) {
		sessionsContext.remove(token);
	}

	/**
	 * Get Default Country
	 * @return
	 */
	public static MCountry getDefaultCountry() {
		MClient client = MClient.get (Env.getCtx());
		MLanguage language = MLanguage.get(Env.getCtx(), client.getAD_Language());
		MCountry country = MCountry.get(Env.getCtx(), language.getCountryCode());
		//	Verify
		if(country != null) {
			return country;
		}
		//	Default
		return MCountry.getDefault(Env.getCtx());
	}
	
	/**
	 * Get Default from language
	 * @param language
	 * @return
	 */
	public static String getDefaultLanguage(String language) {
		MClient client = MClient.get(Env.getCtx());
		String clientLanguage = client.getAD_Language();
		if(!Util.isEmpty(clientLanguage, true) && Util.isEmpty(language, true)) {
			return clientLanguage;
		}
		String defaultLanguage = language;
		if(Util.isEmpty(language, true)) {
			language = Language.AD_Language_en_US;
		}
		//	Using es / en instead es_VE / en_US
		//	get default
		if(language.length() == 2) {
			defaultLanguage = languageCache.get(language);
			if(!Util.isEmpty(defaultLanguage, true)) {
				return defaultLanguage;
			}
			final String sql = "SELECT AD_Language "
				+ "FROM AD_Language "
				+ "WHERE LanguageISO = ? "
					+ "AND (IsSystemLanguage = 'Y' OR IsBaseLanguage = 'Y') "
					// TODO: Add `IsActive` flag?
					+ "AND ROWNUM = 1 "
			;
			defaultLanguage = DB.getSQLValueString(null, sql, language);
			//	Set language
			languageCache.put(language, defaultLanguage);
		}
		if(Util.isEmpty(defaultLanguage, true)) {
			defaultLanguage = Language.AD_Language_en_US;
		}
		//	Default return
		return defaultLanguage;
	}

	public static void loadValuesWithClaims(Claims claimsPayload) {
		if (claimsPayload == null || claimsPayload.isEmpty()) {
			throw new AdempiereException("Claims.Body @NotFound@");
		}
		SessionManager.userId = claimsPayload.get("AD_User_ID", Integer.class);
		SessionManager.roleId = claimsPayload.get("AD_Role_ID", Integer.class);
		SessionManager.organizationId = claimsPayload.get("AD_Org_ID", Integer.class);
		SessionManager.warehouseId = claimsPayload.get("M_Warehouse_ID", Integer.class);
		SessionManager.language = claimsPayload.get("AD_Language", String.class);
	}

	public static void loadValuesWithMADToken(MADToken token) {
		if (token == null || token.getAD_Token_ID() <= 0) {
			throw new AdempiereException("@AD_Token_ID@ @NotFound@");
		}
		SessionManager.userId = token.getAD_User_ID();
		SessionManager.roleId = token.getAD_Role_ID();
		SessionManager.organizationId = token.getAD_Org_ID();
	}

	public static void loadValuesWithOpenID(MSession session) {
		if (session == null || session.getAD_Session_ID() <= 0) {
			throw new AdempiereException("@AD_Session_ID@ @NotFound@");
		}
		Properties context = session.getCtx();
		SessionManager.userId = session.getCreatedBy();
		SessionManager.roleId = session.getAD_Role_ID();
		SessionManager.organizationId = session.getAD_Org_ID();
		SessionManager.warehouseId = Env.getContextAsInt(context, "M_Warehouse_ID");
		SessionManager.language = Env.getAD_Language(context);
	}

	public static int getSessionIdByOpenID(String bearerToken) {
		int sessionId = -1;
		if (Util.isEmpty(bearerToken)) {
			return sessionId;
		}
		final MTable tableUserAuthentication = MTable.get(Env.getCtx(), I_AD_User_Authentication.Table_Name);
		if (tableUserAuthentication == null || tableUserAuthentication.getAD_Table_ID() <= 0) {
			return sessionId;
		}
		if (!openIdSessionCache.containsKey(bearerToken)) {
			MUserAuthentication userAuthentication = new Query(
				Env.getCtx(),
				tableUserAuthentication,
				"AccessToken = ?",
				null
			)
				.setParameters(bearerToken)
				.first()
			;
			if (userAuthentication == null || userAuthentication.get_ColumnIndex(I_AD_Session.COLUMNNAME_AD_Session_ID) < 0) {
				// add empty ID to avoid querying the database again
				openIdSessionCache.put(bearerToken, sessionId);
				sessionOpenIDCCache.put(sessionId, bearerToken);
				return sessionId;
			}
			sessionId = userAuthentication.get_ValueAsInt(
				I_AD_Session.COLUMNNAME_AD_Session_ID
			);
			openIdSessionCache.put(bearerToken, sessionId);
			sessionOpenIDCCache.put(sessionId, bearerToken);
			return sessionId;
		}
		sessionId = openIdSessionCache.get(bearerToken);
		return sessionId;
	}

	/**
	 * Load session from token
	 * @param tokenValue
	 */
	public static Properties getSessionFromToken(String tokenValue) {
		// Remove `Bearer` word from token
		tokenValue = TokenManager.getTokenWithoutType(tokenValue);

		boolean isNewSession = false;
		int sessionId = getSessionIdByOpenID(tokenValue);
		if (sessionId <= 0) {
			SecretKey secretKey = getJWT_SecretKey();
			//	Validate if is token based
			JwtParser parser = Jwts.parser()
				.verifyWith(secretKey)
				.build()
			;
			Jws<Claims> claims = parser.parseSignedClaims(tokenValue);
			sessionId = NumberManager.getIntFromString(
				claims.getPayload().getId()
			);
			if (sessionId > 0) {
				loadValuesWithClaims(
					claims.getPayload()
				);
			} else {
				MADToken token = createSessionFromToken(tokenValue);
				if(Optional.ofNullable(token).isPresent()) {
					loadValuesWithMADToken(token);
					isNewSession = true;
				}
			}
		} else {
			loadValuesWithOpenID(
				new MSession(Env.getCtx(), sessionId, null)
			);
		}
		//	Get Values from role
		if(SessionManager.roleId < 0) {
			throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ / @AD_Org_ID@ @NotFound@");
		}
		//	
		if(SessionManager.organizationId < 0) {
			SessionManager.organizationId = 0;
		}
		if(SessionManager.warehouseId < 0) {
			SessionManager.warehouseId = 0;
		}

		Properties context = (Properties) Env.getCtx().clone();
		DB.validateSupportedUUIDFromDB();

		Env.setContext (context, "#Date", TimeUtil.getDay(System.currentTimeMillis()));
		MRole role = MRole.get(context, SessionManager.roleId);
		//	Warehouse / Org
		Env.setContext(context, "#M_Warehouse_ID", SessionManager.warehouseId);
		Env.setContext(context, "#AD_Client_ID", role.getAD_Client_ID());
		Env.setContext(context, "#AD_Org_ID", SessionManager.organizationId);
		//	Role Info
		Env.setContext(context, "#AD_Role_ID", SessionManager.roleId);
		//	User Info
		Env.setContext(context, "#AD_User_ID", SessionManager.userId);
		//	
		if (!isNewSession) {
			Env.setContext (context, "#AD_Session_ID", sessionId);
		}
		MSession session = MSession.get(context, isNewSession);
		if(session == null || session.getAD_Session_ID() <= 0) {
			throw new AdempiereException("@AD_Session_ID@ @NotFound@");
		}
		//	Load preferences
		loadDefaultSessionValues(context, SessionManager.language);
		Env.setContext(context, "#AD_Session_ID", session.getAD_Session_ID());
		Env.setContext(context, "#Session_UUID", session.getUUID());
		Env.setContext(context, "#AD_User_ID", session.getCreatedBy());
		Env.setContext(context, "#AD_Role_ID", session.getAD_Role_ID());
		Env.setContext(context, "#AD_Client_ID", session.getAD_Client_ID());
		Env.setContext(context, "#Date", new Timestamp(System.currentTimeMillis()));
		setDefault(context, Env.getAD_Org_ID(context), SessionManager.organizationId, SessionManager.warehouseId);
		Env.setContext(context, Env.LANGUAGE, getDefaultLanguage(language));
		return context;
	}


	public static MSession createSession(String clientVersion, String language, int roleId, int userId, int organizationId, int warehouseId) {
		Properties context = (Properties) Env.getCtx().clone();
		MRole role = MRole.get(context, roleId);
		//	Warehouse / Org
		Env.setContext (context, "#M_Warehouse_ID", warehouseId);
		Env.setContext (context, "#AD_Session_ID", 0);
		//	Client Info
		MClient client = MClient.get(context, role.getAD_Client_ID());
		Env.setContext(context, "#AD_Client_ID", client.getAD_Client_ID());
		Env.setContext(context, "#AD_Org_ID", organizationId);
		//	Role Info
		Env.setContext(context, "#AD_Role_ID", roleId);
		//	User Info
		Env.setContext(context, "#AD_User_ID", userId);
		//	
		Env.setContext(context, "#Date", new Timestamp(System.currentTimeMillis()));
		MSession session = MSession.get(context, true);
		if (!Util.isEmpty(clientVersion, true)) {
			session.setWebSession(clientVersion);
			session.saveEx();
		}
		Env.setContext (context, "#AD_Session_ID", session.getAD_Session_ID());
		Env.setContext (context, "#Session_UUID", session.getUUID());
		//	Load preferences
		SessionManager.loadDefaultSessionValues(context, language);

		return session;
	}

	public static String createSessionAndGetToken(String clientVersion, String language, int roleId, int userId, int organizationId, int warehouseId, boolean isOpenID) {
		MSession session = createSession(clientVersion, language, roleId, userId, organizationId, warehouseId);

		String bearerToken = null;
		if (isOpenID) {
			bearerToken = getOpenIDToken(session);
		} else {
			bearerToken = createAndGetBearerToken(session, warehouseId, Env.getAD_Language(session.getCtx()));
		}

		// Update session preferences
		PreferenceUtil.saveSessionPreferences(
			userId, language, session.getAD_Role_ID(), session.getAD_Client_ID(), session.getAD_Org_ID(), warehouseId
		);

		return bearerToken;
	}


	public static String getOpenIDToken(MSession session) {
		String bearerToken = null;

		final MTable tableUserAuthentication = MTable.get(Env.getCtx(), I_AD_User_Authentication.Table_Name);
		if (tableUserAuthentication == null || tableUserAuthentication.getAD_Table_ID() <= 0) {
			return bearerToken;
		}
		Integer userAuthenticationId = new Query(
			session.getCtx(),
			tableUserAuthentication,
			"AD_User_ID = ?",
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(session.getCreatedBy())
			.aggregate(
				I_AD_User_Authentication.COLUMNNAME_AD_User_Authentication_ID,
				Query.AGGREGATE_MAX,
				Integer.class
			)
		;
		if (userAuthenticationId != null && userAuthenticationId > 0) {
			MUserAuthentication userAuthentication = new MUserAuthentication(session.getCtx(), userAuthenticationId, null);
			bearerToken = userAuthentication.getAccessToken();
			// Fill User Authentication
			if (userAuthentication.get_ColumnIndex(I_AD_Session.COLUMNNAME_AD_Session_ID) > 0) {
				userAuthentication.set_CustomColumn(
					I_AD_Session.COLUMNNAME_AD_Session_ID,
					session.getAD_Session_ID()
				);
				userAuthentication.saveEx();
				// add the empty ID so as not to query the database again
				openIdSessionCache.put(bearerToken, session.getAD_Session_ID());
				sessionOpenIDCCache.put(session.getAD_Session_ID(), bearerToken);
			}

			// Fill Session
			if (session.get_ColumnIndex(I_AD_User_Authentication.COLUMNNAME_AD_User_Authentication_ID) > 0) {
				session.set_CustomColumn(
					I_AD_User_Authentication.COLUMNNAME_AD_User_Authentication_ID,
					userAuthenticationId
				);
				session.saveEx();
				// add the empty ID so as not to query the database again
				openIdSessionCache.put(bearerToken, session.getAD_Session_ID());
				sessionOpenIDCCache.put(session.getAD_Session_ID(), bearerToken);
			}
		}
		//	Session values
		return bearerToken;
	}


	/**
	 * Get JWT Secrect Key generates with HMAC-SHA algorithms
	 * @return
	 */
	private static String getJWT_SecretKeyAsString() {
		// get by SysConfig client
		String secretKey = MSysConfig.getValue(
			JWTUtil.ECA52_JWT_SECRET_KEY,
			Env.getAD_Client_ID(
				Env.getCtx()
			)
		);
		if(Util.isEmpty(secretKey, true)) {
			// get by initialization statup setting
			secretKey = Ini.getProperty(
				JWTUtil.ECA52_JWT_SECRET_KEY
			);
		}
		if(Util.isEmpty(secretKey, true)) {
			throw new AdempiereException(
				"@" + JWTUtil.ECA52_JWT_SECRET_KEY + "@ @NotFound@"
			);
		}
		return secretKey;
	}
	private static SecretKey getJWT_SecretKey() {
		byte[] keyBytes = Base64.getDecoder().decode(
			getJWT_SecretKeyAsString()
		);
		SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
		return secretKey;
	}


	/**
	 * Create token as bearer
	 * @param session
	 * @param warehouseId
	 * @param language
	 * @return
	 */
	private static String createAndGetBearerToken(MSession session, int warehouseId, String language) {
		MUser user = MUser.get(session.getCtx(), session.getCreatedBy());
		long sessionTimeout = getSessionTimeout(user);

		SecretKey secretKey = getJWT_SecretKey();
		JwtBuilder jwtBuilder = Jwts.builder()
			.id(String.valueOf(session.getAD_Session_ID()))
			// .claims()
			.claim("AD_Client_ID", session.getAD_Client_ID())
			.claim("AD_Org_ID", session.getAD_Org_ID())
			.claim("AD_Role_ID", session.getAD_Role_ID())
			.claim("AD_User_ID", session.getCreatedBy())
			.claim("M_Warehouse_ID", warehouseId)
			.claim("AD_Language", language)
			.issuedAt(
				new Date()
			)
			.expiration(
				new Date(
					System.currentTimeMillis() + sessionTimeout
				)
			)
			.signWith(secretKey, Jwts.SIG.HS256)
		;

		return jwtBuilder.compact();
	}

	/**
	 * Get Session Timeout from user definition
	 * @param user
	 * @return
	 */
	public static long getSessionTimeout(MUser user) {
		long sessionTimeout = 0;
		Object value = null;
		// checks if the column exists in the database
		if (user.get_ColumnIndex("ConnectionTimeout") >= 0) {
			value = user.get_Value("ConnectionTimeout");
		}
		if(value == null) {
			String sessionTimeoutAsString = MSysConfig.getValue(
				"WEBUI_DEFAULT_TIMEOUT",
				Env.getAD_Client_ID(Env.getCtx()),
				0
			);
			sessionTimeout = NumberManager.getIntFromString(
				sessionTimeoutAsString
			);
		} else {
			sessionTimeout = NumberManager.getIntFromString(
				String.valueOf(value)
			);
		}

		if(sessionTimeout == 0) {
			// get by initialization statup setting
			String timeout = Ini.getProperty(
				"JWT_EXPIRATION_TIME"
			);
			sessionTimeout = NumberManager.getIntFromString(timeout);
		}

		//	Default 24 hours
		if (sessionTimeout == 0) {
			sessionTimeout = 86400000;
			log.info("Default Session Timeout");
		}
		return sessionTimeout;
	}
	
	/**
	 * Set Default warehouse and organization
	 * @param context
	 * @param defaultOrganizationId
	 * @param newOrganizationId
	 * @param warehouseId
	 */
	private static void setDefault(Properties context, int defaultOrganizationId, int newOrganizationId, int warehouseId) {
		int organizationId = defaultOrganizationId;
		if(newOrganizationId >= 0) {
			MOrg organization = MOrg.get(context, newOrganizationId);
			//	
			if(organization != null) {
				organizationId = organization.getAD_Org_ID();
			}
		}
		if (warehouseId >= 0) {
			MWarehouse warehouse = MWarehouse.get(context, warehouseId);
			if (warehouse != null) {
				Env.setContext(context, "#M_Warehouse_ID", warehouseId);
			}
		}
		Env.setContext(context, "#AD_Org_ID", organizationId);
	}
	
	/**
	 * Get id of current session
	 * @return
	 */
	public static int getSessionId() {
		return Env.getContextAsInt(Env.getCtx(), "#AD_Session_ID");
	}
	
	/**
	 * Get uuid of current session
	 * @return
	 */
	public static String getSessionUuid() {
		return Env.getContext(Env.getCtx(), "#Session_UUID");
	}
	
	/**
	 * Get token object: validate it
	 * @param tokenValue
	 * @return
	 */
	public static MADToken createSessionFromToken(String tokenValue) {
		if(Util.isEmpty(tokenValue, true)) {
			throw new AdempiereException("@AD_Token_ID@ @NotFound@");
		}
		// Remove `Bearer` word from token
		tokenValue = TokenManager.getTokenWithoutType(tokenValue);
		//	
		try {
			ITokenGenerator generator = TokenGeneratorHandler.getInstance()
				.getTokenGenerator(
					MADTokenDefinition.TOKENTYPE_ThirdPartyAccess
				);
			if(generator == null) {
				throw new AdempiereException("@AD_TokenDefinition_ID@ @NotFound@");
			}
			//	No child of definition
			if(!IThirdPartyAccessGenerator.class.isAssignableFrom(generator.getClass())) {
				throw new AdempiereException("@AD_TokenDefinition_ID@ @Invalid@");	
			}
			//	Validate
			IThirdPartyAccessGenerator thirdPartyAccessGenerator = ((IThirdPartyAccessGenerator) generator);
			if(!thirdPartyAccessGenerator.validateToken(tokenValue)) {
				throw new AdempiereException("@Invalid@ @AD_Token_ID@");
			}
			//	Default
			MADToken token = thirdPartyAccessGenerator.getToken();
			return token;
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}

	/**
	 * Load default values for session
	 * @param context
	 * @param language
	 */
	public static void loadDefaultSessionValues(Properties context, String language) {
		//	Client Info
		MClient client = MClient.get(context, Env.getContextAsInt(context, "#AD_Client_ID"));
		Env.setContext(context, "#AD_Client_Name", client.getName());
		Env.setContext(context, "#Date", new Timestamp(System.currentTimeMillis()));
		Env.setContext(context, Env.LANGUAGE, getDefaultLanguage(language));
		//	Role Info
		MRole role = MRole.get(context, Env.getContextAsInt(context, "#AD_Role_ID"));
		Env.setContext(context, "#AD_Role_Name", role.getName());
		Env.setContext(context, "#SysAdmin", role.getAD_Role_ID() == 0);

		//	User Info
		MUser user = MUser.get(context, Env.getContextAsInt(context, "#AD_User_ID"));
		Env.setContext(context, "#AD_User_Name", user.getName());
		Env.setContext(context, "#AD_User_Description", user.getDescription());
		Env.setContext(context, "#SalesRep_ID", user.getAD_User_ID());

		//	Load preferences
		loadPreferences(context);
	}

	/**
	 * Get Default role after login
	 * @param userId
	 * @return
	 */
	public static int getDefaultRoleId(int userId) {
		if (userId < 0) {
			return -1;
		}
		final String sql = "SELECT ur.AD_Role_ID "
			+ "FROM AD_User_Roles AS ur "
			+ "INNER JOIN AD_Role AS r ON ur.AD_Role_ID = r.AD_Role_ID "
			+ "WHERE ur.AD_User_ID = ? "
				+ "AND ur.IsActive = 'Y' "
				+ "AND r.IsActive = 'Y' "
				+ "AND ("
					+ "("
						+ "r.IsAccessAllOrgs = 'Y' AND EXISTS("
							+ "SELECT 1 FROM AD_Org AS o "
							+ "WHERE (o.AD_Client_ID = r.AD_Client_ID OR o.AD_Org_ID = 0) "
								+ "AND o.IsActive = 'Y' "
								+ "AND o.IsSummary = 'N' "
								// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
						+ ")"
					+ ") "
					+ "OR (r.IsUseUserOrgAccess = 'N' AND EXISTS("
						+ "SELECT 1 FROM AD_Role_OrgAccess AS ro "
							+ "WHERE ro.AD_Role_ID = ur.AD_Role_ID "
								+ "AND ro.IsActive = 'Y'"
								// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
						+ ")"
					+ ") "
					+ "OR ("
						+ "r.IsUseUserOrgAccess = 'Y' AND EXISTS("
							+ "SELECT 1 FROM AD_User_OrgAccess AS uo "
							+ "WHERE uo.AD_User_ID = ur.AD_User_ID "
							+ "AND uo.IsActive = 'Y' "
							// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
						+ ")"
					+ ")"
				+ ") "
				+ "AND ROWNUM = 1 "
			+ "ORDER BY COALESCE(ur.IsDefault, 'N') DESC";
		return DB.getSQLValue(null, sql, userId);
	}

	/**
	 * Get Default organization after login
	 * @param roleId
	 * @param userId
	 * @return
	 */
	public static int getDefaultOrganizationId(int roleId, int userId) {
		if (roleId < 0 && userId < 1) {
			return -1;
		}
		String organizationSQL = "SELECT o.AD_Org_ID "
			+ "FROM AD_Role AS r "
			+ "INNER JOIN AD_Client AS c ON(c.AD_Client_ID = r.AD_Client_ID) "
			+ "INNER JOIN AD_Org AS o ON(c.AD_Client_ID = o.AD_Client_ID OR o.AD_Org_ID = 0) "
			+ "WHERE r.AD_Role_ID = ? "
				+ "AND o.IsActive = 'Y' AND o.IsSummary = 'N' "
				+ "AND ("
					+ "r.IsAccessAllOrgs = 'Y' "
					+ "OR ("
						+ "r.IsUseUserOrgAccess = 'N' AND EXISTS("
							+ "SELECT 1 FROM AD_Role_OrgAccess AS ra "
							+ "WHERE ra.AD_Org_ID = o.AD_Org_ID "
								+ "AND ra.AD_Role_ID = r.AD_Role_ID "
								+ "AND ra.IsActive = 'Y' "
								// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
						+ ")"
					+ ") "
					+ "OR ("
						+ "r.IsUseUserOrgAccess = 'Y' AND EXISTS("
							+ "SELECT 1 FROM AD_User_OrgAccess AS ua "
								+ "WHERE ua.AD_Org_ID = o.AD_Org_ID "
									+ "AND ua.AD_User_ID = ? "
									+ "AND ua.IsActive = 'Y' "
								// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
						+ ")"
					+ ")"
				+ ") "
				+ "AND ROWNUM = 1 "
			+ "ORDER BY o.Name ";
		return DB.getSQLValue(null, organizationSQL, roleId, userId);
	}
	
	/**
	 * Get Default Warehouse after login
	 * @param organizationId
	 * @return
	 */
	public static int getDefaultWarehouseId(int organizationId) {
		if (organizationId < 0) {
			return -1;
		}
		final String sql = "SELECT M_Warehouse_ID "
			+ "FROM M_Warehouse "
			+ "WHERE IsActive = 'Y' "
				+ "AND AD_Org_ID = ? "
				+ "AND IsInTransit = 'N' "
				+ "AND ROWNUM = 1 "
			+ "ORDER BY Name "
		;
		return DB.getSQLValue(null, sql, organizationId);
	}

	/**
	 *	Load Preferences into Context for selected client.
	 *	<p>
	 *	Sets Org info in context and loads relevant field from
	 *	- AD_Client/Info,
	 *	- C_AcctSchema,
	 *	- C_AcctSchema_Elements
	 *	- AD_Preference
	 *	<p>
	 *	Assumes that the context is set for #AD_Client_ID, #AD_User_ID, #AD_Role_ID
	 *	@param context
	 *	@return AD_Message of error (NoValidAcctInfo) or ""
	 */
	private static void loadPreferences(Properties context) {
		if (context == null)
			throw new IllegalArgumentException("Required parameter missing");
		if (Env.getContext(context,"#AD_Client_ID").length() == 0)
			throw new UnsupportedOperationException("Missing Context #AD_Client_ID");
		if (Env.getContext(context,"#AD_User_ID").length() == 0)
			throw new UnsupportedOperationException("Missing Context #AD_User_ID");
		if (Env.getContext(context,"#AD_Role_ID").length() == 0)
			throw new UnsupportedOperationException("Missing Context #AD_Role_ID");
		//	Load Role Info
		MRole.getDefault(context, false);
		//	Other
		Env.setAutoCommit(context, Ini.isPropertyBool(Ini.P_A_COMMIT));
		Env.setAutoNew(context, Ini.isPropertyBool(Ini.P_A_NEW));

		String isShowAccounting = "N";
		if (MRole.getDefault(context, false).isShowAcct()) {
			isShowAccounting = "Y";
		}
		Env.setContext(context, "#ShowAcct", isShowAccounting);

		Env.setContext(context, "#ShowTrl", Ini.getProperty(Ini.P_SHOW_TRL));
		Env.setContext(context, "#ShowAdvanced", Ini.getProperty(Ini.P_SHOW_ADVANCED));

		//	Other Settings
		Env.setContext(context, "#YYYY", "Y");
		Env.setContext(context, "#StdPrecision", 2);
		int clientId = Env.getAD_Client_ID(context);
		int orgId = Env.getAD_Org_ID(context);
		//	AccountSchema Info (first)
		String sql = "SELECT * "
			+ "FROM C_AcctSchema AS a, AD_ClientInfo AS c "
			+ "WHERE a.C_AcctSchema_ID = c.C_AcctSchema1_ID "
				+ "AND c.AD_Client_ID = ? "
				+ "AND ROWNUM = 1 "
		;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			int acctSchemaId = 0;
			
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, clientId);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				//	Accounting Info
				acctSchemaId = rs.getInt("C_AcctSchema_ID");
				Env.setContext(context, "$C_AcctSchema_ID", acctSchemaId);
				Env.setContext(context, "$C_Currency_ID", rs.getInt("C_Currency_ID"));
				Env.setContext(context, "$HasAlias", rs.getString("HasAlias"));
			}
			rs.close();
			pstmt.close();
			/**Define AcctSchema , Currency, HasAlias for Multi AcctSchema**/
			MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(Env.getCtx(), clientId);
			if(ass != null && ass.length > 1) {
				for(MAcctSchema as : ass) {
					acctSchemaId = MClientInfo.get(Env.getCtx(), clientId).getC_AcctSchema1_ID();
					if (as.getAD_OrgOnly_ID() != 0) {
						if (as.isSkipOrg(orgId)) {
							continue;
						} else {
							acctSchemaId = as.getC_AcctSchema_ID();
							Env.setContext(context, "$C_AcctSchema_ID", acctSchemaId);
							Env.setContext(context, "$C_Currency_ID", as.getC_Currency_ID());
							Env.setContext(context, "$HasAlias", as.isHasAlias());
							break;
						}
					}
				}
			}

			//	Accounting Elements
			sql = "SELECT ElementType "
				+ "FROM C_AcctSchema_Element "
				+ "WHERE C_AcctSchema_ID = ? "
					+ "AND IsActive = 'Y' "
			;
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, acctSchemaId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Env.setContext(context, "$Element_" + rs.getString("ElementType"), "Y");
			}
			rs.close();
			pstmt.close();

			//	This reads all relevant window neutral defaults
			//	overwriting superseeded ones. Window specific is read in Mainain
			sql = "SELECT Attribute, Value, AD_Window_ID "
				+ "FROM AD_Preference "
				+ "WHERE AD_Client_ID IN (0, @#AD_Client_ID@) "
					+ "AND AD_Org_ID IN (0, @#AD_Org_ID@) "
					+ "AND (AD_User_ID IS NULL OR AD_User_ID = 0 OR AD_User_ID = @#AD_User_ID@) "
					+ "AND IsActive = 'Y' "
				+ "ORDER BY Attribute, AD_Client_ID, AD_User_ID DESC, AD_Org_ID"
			;
				//	the last one overwrites - System - Client - User - Org - Window
			sql = Env.parseContext(context, 0, sql, false);
			if (Util.isEmpty(sql, true)) {
				log.log(Level.SEVERE, "loadPreferences - Missing Environment");
			} else {
				pstmt = DB.prepareStatement(sql, null);
				rs = pstmt.executeQuery();
				while (rs.next()) {
					int AD_Window_ID = rs.getInt(3);
					String at = "";
					if (rs.wasNull())
						at = "P|" + rs.getString(1);
					else
						at = "P" + AD_Window_ID + "|" + rs.getString(1);
					String va = rs.getString(2);
					Env.setContext(context, at, va);
				}
				rs.close();
				pstmt.close();
			}

			// TODO: Improve query performance by adding cache, data does not change constantly unless dictionary changes
			//	Default Values
			log.info("Default Values ...");
			sql = "SELECT t.TableName, c.ColumnName "
				+ "FROM AD_Column AS c "
				+ "INNER JOIN AD_Table AS t "
					+ "ON (c.AD_Table_ID = t.AD_Table_ID) "
				+ "WHERE t.IsActive = 'Y'"
					+ "AND c.IsKey = 'Y' "
					+ "AND EXISTS ("
						+ "SELECT 1 "
						+ "FROM AD_Column AS cc "
						+ " WHERE cc.IsActive = 'Y' "
							+ "AND ColumnName = 'IsDefault' "
							+ "AND t.AD_Table_ID = cc.AD_Table_ID"
							// TODO: add `LIMIT 1` or `AND ROWNUM = 1` to best performance
					+ ")"
					// TODO: Only conversion type, and table dimensions
					+ "AND t.AD_Table_ID IN(" + I_C_ConversionType.Table_ID + ") "
			;
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				loadDefault (context, rs.getString(1), rs.getString(2));
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			log.log(Level.WARNING, "loadPreferences", e);
			e.printStackTrace();
		} finally {
			pstmt = null;
			rs = null;
			DB.close(rs, pstmt);
		}
		//	Country
		MCountry country = getDefaultCountry();
		if(country != null && country.getC_Country_ID() > 0) {
			Env.setContext(context, "#C_Country_ID", country.getC_Country_ID());
		}
		// Call ModelValidators afterLoadPreferences - teo_sarca FR [ 1670025 ]
		ModelValidationEngine.get().afterLoadPreferences(context);
	}	//	loadPreferences


	/**
	 *	Load Default Value for Table into Context.
	 *	@param tableName table name
	 *	@param columnName column name
	 */
	private static void loadDefault (Properties context, String tableName, String columnName) {
		if (tableName.startsWith("AD_Window")
			|| tableName.startsWith("AD_PrintFormat")
			|| tableName.startsWith("AD_Workflow") )
			return;
		String value = null;
		//
		String sql = "SELECT " + columnName + " "
			+ "FROM " + tableName + " "
			+ "WHERE IsActive = 'Y' "
				+ "AND IsDefault = 'Y' "
				+ "AND ROWNUM = 1 "
			+ "ORDER BY AD_Client_ID DESC, AD_Org_ID DESC"
		;
		sql = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				tableName,
				MRole.SQL_NOTQUALIFIED,
				MRole.SQL_RO
			);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				value = rs.getString(1);
			}
			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (SQLException e) {
			log.log(Level.WARNING, tableName + " (" + sql + ")", e);
			e.printStackTrace();
			return;
		} finally {
			DB.close(rs, pstmt);
		}
		//	Set Context Value
		if (!Util.isEmpty(value, true)) {
			if (tableName.equals("C_DocType")) {
				Env.setContext(context, "#C_DocTypeTarget_ID", value);
			}
			else {
				Env.setContext(context, "#" + columnName, value);
			}
		}
	}	//	loadDefault

}
