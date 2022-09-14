/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package org.spin.base.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.I_AD_Reference;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.X_AD_Reference;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;


/**
 * Class for handle reference for reports, Smart Browsers and other values with Table, List or Table Direct
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ReferenceUtil {
	/**	Instance	*/
	private static ReferenceUtil instance = null;
	/**	Context	*/
	private Properties context;
	/**	Local cache	*/
	private Map<String, ReferenceInfo> referenceInfoMap;
	
	public static ReferenceUtil getInstance(Properties context) {
		if(instance == null) {
			instance = new ReferenceUtil(context);
		}
		return instance;
	}
	
	/**
	 * Private constructor
	 * @param context
	 */
	private ReferenceUtil(Properties context) {
		this.context = context;
		referenceInfoMap = new HashMap<String, ReferenceInfo>();
	}
	
	/**
	 * Validate reference
	 * @param referenceId
	 * @param referenceValueId
	 * @param columnName
	 * @return
	 */
	public static boolean validateReference(int referenceId) {
		if(!DisplayType.isLookup(referenceId)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Get Reference information, can return null if reference is invalid or not exists
	 * @param referenceId
	 * @param referenceValueId
	 * @param columnName
	 * @param language
	 * @return
	 */
	public ReferenceInfo getReferenceInfo(int referenceId, int referenceValueId, String columnName, String language, String tableName) {
		if(!validateReference(referenceId)) {
			return null;
		}
		String key = referenceId + "|" + referenceValueId + "|" + columnName + "|" + language;
		ReferenceInfo referenceInfo = referenceInfoMap.get(key);
		Language languageValue = Language.getLanguage(Env.getAD_Language(Env.getCtx()));
		if(referenceInfo == null) {
			if(DisplayType.TableDir == referenceId
					|| referenceValueId == 0) {
				//	Add Display
				referenceInfo = new ReferenceInfo();
				referenceInfo.setColumnName(columnName);
				referenceInfo.setDisplayColumnValue("(" + MLookupFactory.getLookup_TableDirEmbed(languageValue, columnName, tableName) + ")");
				referenceInfo.setHasJoinValue(false);
			} else {
				//	Get info
				MLookupInfo lookupInfo = MLookupFactory.getLookupInfo(context, 0, 0, referenceId, languageValue, columnName, referenceValueId, false, null, false);
				if(lookupInfo != null) {
					referenceInfo = new ReferenceInfo();
					referenceInfo.setColumnName(columnName);
					referenceInfo.setDisplayColumnValue((lookupInfo.DisplayColumn == null? "": lookupInfo.DisplayColumn).replace(lookupInfo.TableName + ".", ""));
					referenceInfo.setJoinColumnName((lookupInfo.KeyColumn == null? "": lookupInfo.KeyColumn).replace(lookupInfo.TableName + ".", ""));
					referenceInfo.setTableName(lookupInfo.TableName);
					if(DisplayType.List == referenceId
							&& referenceValueId != 0) {
						referenceInfo.setReferenceId(referenceValueId);
					}
					//	Translate
					if(MTable.hasTranslation(lookupInfo.TableName)) {
						referenceInfo.setLanguage(language);
					}
				}
			}
		}
		return referenceInfo;
	}
	
	/**
	 * Get Reference information, can return null if reference is invalid or not exists
	 * @param referenceId
	 * @param referenceValueId
	 * @param columnName
	 * @param validationRuleId
	 * @return
	 */
	public static MLookupInfo getReferenceLookupInfo(int referenceId, int referenceValueId, String columnName, int validationRuleId) {
		if(!validateReference(referenceId)) {
			return null;
		}
		MLookupInfo lookupInformation = null;
		if(DisplayType.TableDir == referenceId
				|| referenceValueId <= 0) {
			//	Add Display
			lookupInformation = getLookupInfoFromColumnName(columnName);
		} else {
			//	Get info
			lookupInformation = getLookupInfoFromReference(referenceValueId);	
		}

		// without lookup info
		if (lookupInformation == null) {
			return null;
		}

		//	For validation
		String queryForLookup = lookupInformation.Query;
		//	Add support to UUID
		queryForLookup = getQueryWithUuid(lookupInformation.TableName, queryForLookup);
		String directQuery = getQueryWithUuid(lookupInformation.TableName, lookupInformation.QueryDirect);

		// set new query
		lookupInformation.Query = queryForLookup;
		lookupInformation.QueryDirect = directQuery;

		MValRule validationRule = null;
		//	For validation rule
		if (validationRuleId > 0) {
			validationRule = MValRule.get(Env.getCtx(), validationRuleId);
			if (validationRule != null) {
				if (!Util.isEmpty(validationRule.getCode())) {
					String dynamicValidation = "(" + validationRule.getCode() + ")";
					// table validation
					if (!Util.isEmpty(lookupInformation.ValidationCode)) {
						dynamicValidation += " AND (" + lookupInformation.ValidationCode + ")";
					}
					// overwrite ValidationCode with table validation on reference and dynamic validation
					lookupInformation.ValidationCode = dynamicValidation;
				}
			}
		}

		// return only code without validation rule
		if (Util.isEmpty(lookupInformation.ValidationCode)) {
			return lookupInformation;
		}

		// remove order by
		String queryWithoutOrder = queryForLookup;
		int positionOrder = queryForLookup.lastIndexOf(" ORDER BY ");
		String orderByQuery = "";
		if (positionOrder != -1) {
			orderByQuery = queryForLookup.substring(positionOrder);
			queryWithoutOrder = queryForLookup.substring(0, positionOrder);
		}

		// add where clause with validation code
		int positionFrom = queryForLookup.lastIndexOf(" FROM ");
		boolean hasWhereClause = queryForLookup.indexOf(" WHERE ", positionFrom) != -1;
		if (hasWhereClause) {
			queryWithoutOrder += " AND ";
		} else {
			queryWithoutOrder += " WHERE ";
		}
		queryWithoutOrder += " " + lookupInformation.ValidationCode;
		
		// Add new query with where clause and order By
		queryForLookup = queryWithoutOrder + orderByQuery;

		// set new query
		lookupInformation.Query = queryForLookup;

		return lookupInformation;
	}
	
	/**
	 * Get Lookup info from reference
	 * @param referenceId
	 * @return
	 */
	private static MLookupInfo getLookupInfoFromReference(int referenceId) {
		MLookupInfo lookupInformation = null;
		X_AD_Reference reference = (X_AD_Reference) RecordUtil.getEntity(Env.getCtx(), I_AD_Reference.Table_Name, null, referenceId, null);
		if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_TableValidation)) {
			lookupInformation = MLookupFactory.getLookupInfo(Env.getCtx(), 0, 0, DisplayType.Search, Language.getLanguage(Env.getAD_Language(Env.getCtx())), null, reference.getAD_Reference_ID(), false, null, false);
		} else if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_ListValidation)) {
			lookupInformation = MLookupFactory.getLookup_List(Language.getLanguage(Env.getAD_Language(Env.getCtx())), reference.getAD_Reference_ID());
		}
		return lookupInformation;
	}
	
	/**
	 * Get Query with UUID
	 * @param tableName
	 * @param query
	 * @return
	 */
	private static String getQueryWithUuid(String tableName, String query) {
		Matcher matcher = Pattern.compile("\\s+(FROM)\\s+(" + tableName + ")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(query);
		int positionFrom = -1;
		if(matcher.find()) {
			positionFrom = matcher.start();
			query = query.substring(0, positionFrom) + ", " + tableName + ".UUID" + query.substring(positionFrom);
		}
		return query;
	}
	
	/**
	 * Get Lookup info from column name
	 * @param columnName
	 * @return
	 */
	private static MLookupInfo getLookupInfoFromColumnName(String columnName) {
		return MLookupFactory.getLookupInfo(Env.getCtx(), 0, 0, DisplayType.TableDir, Language.getLanguage(Env.getAD_Language(Env.getCtx())), columnName, 0, false, null, false);
	}
}
