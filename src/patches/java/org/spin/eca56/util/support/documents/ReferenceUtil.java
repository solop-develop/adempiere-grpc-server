/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2023 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca56.util.support.documents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Chart;
import org.adempiere.core.domains.models.I_AD_Image;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_ElementValue;
import org.adempiere.core.domains.models.I_C_Location;
import org.adempiere.core.domains.models.I_C_ValidCombination;
import org.adempiere.core.domains.models.I_M_AttributeSetInstance;
import org.adempiere.core.domains.models.I_M_Locator;
import org.adempiere.core.domains.models.I_S_ResourceAssignment;
import org.adempiere.core.domains.models.X_AD_Reference;
import org.compiere.model.MCountry;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRefTable;
import org.compiere.model.MValRule;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

/**
 * 	The util class for all documents
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReferenceUtil {

	/**
	 * Validate reference
	 * TODO: Add support to Resource Assigment reference to get display column
	 * @param displayTypeId
	 * @return
	 */
	public static boolean isLookupReference(int displayTypeId) {
		if (DisplayType.isLookup(displayTypeId)
				|| DisplayType.Account == displayTypeId
				|| DisplayType.Assignment == displayTypeId
				|| DisplayType.ID == displayTypeId
				|| DisplayType.Location == displayTypeId
				|| DisplayType.Locator == displayTypeId
				|| DisplayType.Image == displayTypeId
				|| DisplayType.PAttribute == displayTypeId) {
			return true;
		}
		// if(DisplayType.Image == displayTypeId) {
		// 	return AttachmentUtil.getInstance()
		// 		.isValidForClient(
		// 			Env.getAD_Client_ID(Env.getCtx())
		// 		);
		// }
		return false;
	}

	public static int overwriteDisplayType(int displayTypeId, int referenceValueId) {
		int newDisplayType = displayTypeId;
		if (DisplayType.Button == displayTypeId) {
			//	Reference Value
			if (referenceValueId > 0) {
				X_AD_Reference reference = new X_AD_Reference(
					Env.getCtx(),
					referenceValueId,
					null
				);
				if (reference != null && reference.getAD_Reference_ID() > 0) {
					// overwrite display type to Table or List
					if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
						newDisplayType = DisplayType.Table;
					} else {
						newDisplayType = DisplayType.List;
					}
				}
			}
		} else if (DisplayType.TableDir == displayTypeId) {
			if (referenceValueId > 0) {
				// overwrite display type to Table (as C_DocTypeTarget_ID > C_DocType_ID)
				newDisplayType = DisplayType.Table;
			}
		}
		return newDisplayType;
	}

	/**
	 * Get Context column names from context
	 * @param context
	 * @return
	 * @return List<String>
	 */
	public static List<String> getContextColumnNames(String context) {
		if (Util.isEmpty(context, true)) {
			return new ArrayList<String>();
		}
		String START = "\\@";  // A literal "(" character in regex
		String END   = "\\@";  // A literal ")" character in regex

		// Captures the word(s) between the above two character(s)
		final String COLUMN_NAME_PATTERN = START + "(#|$|\\d\\|){0,1}(\\w+)" + END;

		Pattern pattern = Pattern.compile(
			COLUMN_NAME_PATTERN,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		);
		Matcher matcher = pattern.matcher(context);
		Map<String, Boolean> columnNamesMap = new HashMap<String, Boolean>();
		while(matcher.find()) {
			final String columnContext = matcher.group().replace("@", "").replace("@", "");
			columnNamesMap.put(columnContext, true);
		}
		return new ArrayList<String>(columnNamesMap.keySet());
	}

	public static ReferenceValues getReferenceDefinition(String columnName, int displayTypeId, int referenceValueId, int validationRuleId) {
		String embeddedContextColumn = null;

		if (DisplayType.Button == displayTypeId) {
			//	Reference Value
			if (referenceValueId > 0) {
				X_AD_Reference reference = new X_AD_Reference(Env.getCtx(), referenceValueId, null);
				if (reference != null && reference.getAD_Reference_ID() > 0) {
					// overwrite display type to Table or List
					if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
						displayTypeId = DisplayType.Table;
					} else {
						displayTypeId = DisplayType.List;
					}
				}
			}
		}

		if(displayTypeId > 0 && ReferenceUtil.isLookupReference(displayTypeId)) {
			String tableName = getTableNameFromReference(columnName, displayTypeId);
			// X_AD_Reference reference = new X_AD_Reference(Env.getCtx(), displayTypeId, null);
			// MLookupInfo lookupInformation = null;
			// //	Special references
			// if(Util.isEmpty(tableName)) {
			// 	lookupInformation = MLookupFactory.getLookupInfo(Env.getCtx(), 0, 0, referenceId, Language.getBaseLanguage(), columnName, referenceValueId, false, null, false);
			// 	if(lookupInformation != null) {
			// 		String validationRuleValue = null;
			// 		if(validationRuleId > 0) {
			// 			MValRule validationRule = MValRule.get(Env.getCtx(), validationRuleId);
			// 			validationRuleValue = validationRule.getCode();
			// 		}
			// 		tableName = lookupInformation.TableName;
			// 		embeddedContextColumn = Optional.ofNullable(lookupInformation.Query).orElse("") 
			// 			+ Optional.ofNullable(lookupInformation.QueryDirect).orElse("") 
			// 			+ Optional.ofNullable(lookupInformation.ValidationCode).orElse("")
			// 			+ Optional.ofNullable(validationRuleValue).orElse("")
			// 		;
			// 	}
			// }
			String whereClauseReference = null;
			String validationRuleValue = null;
			if(validationRuleId > 0) {
				MValRule validationRule = MValRule.get(Env.getCtx(), validationRuleId);
				validationRuleValue = validationRule.getCode();
			}
			if ((displayTypeId == DisplayType.Table || displayTypeId == DisplayType.Search) && referenceValueId > 0) {
				MRefTable tableReference = MRefTable.getById(Env.getCtx(), referenceValueId);
				if(tableReference != null) {
					whereClauseReference = tableReference.getWhereClause();
				}
			}
			embeddedContextColumn = Optional.ofNullable(whereClauseReference).orElse("")
				+ Optional.ofNullable(validationRuleValue).orElse("")
			;
			return ReferenceValues.newInstance(displayTypeId, tableName, embeddedContextColumn);
		}
		return null;
	}

	/**
	 * Get Table Name for special tables
	 * @param columnName
	 * @param referenceId
	 * @return
	 */
	public static String getTableNameFromReference(String columnName, int referenceId) {
		String tableName = null;
		if(DisplayType.ID == referenceId || DisplayType.Search == referenceId
			|| DisplayType.Table == referenceId || DisplayType.TableDir == referenceId) {
			tableName = columnName.replaceAll("(_ID_To|_To_ID|_ID)$", "");
			if (columnName.endsWith("_Acct")) {
				tableName = I_C_ElementValue.Table_Name;
			}
		} else if (DisplayType.List == referenceId) {
			tableName = I_AD_Ref_List.Table_Name;
		} else if (DisplayType.Location == referenceId) {
			tableName = I_C_Location.Table_Name;
		} else if (DisplayType.Locator == referenceId) {
			tableName = I_M_Locator.Table_Name;
		} else if (DisplayType.PAttribute == referenceId) {
			tableName = I_M_AttributeSetInstance.Table_Name;
		} else if(DisplayType.Image == referenceId) {
			tableName = I_AD_Image.Table_Name;
		} else if(DisplayType.Assignment == referenceId) {
			tableName = I_S_ResourceAssignment.Table_Name;
		} else if(DisplayType.Chart == referenceId) {
			tableName = I_AD_Chart.Table_Name;
		} else if(DisplayType.Account == referenceId) {
			tableName = I_C_ElementValue.Table_Name;
		}
		return tableName;
	}



	public static String getDisplayColumnSQLImage(String tableName, String columnName) {
		StringBuffer query = new StringBuffer()
			.append("SELECT ")
			.append("NVL(UUID, '')")
			.append(" || '-' || ")
			.append("NVL(FileName, '') ")
			.append("FROM AD_AttachmentReference ")
			.append("WHERE AD_Image_ID = ")
			.append(tableName + "." + columnName)
			// .append(" LIMIT 1")
			.append(" AND ROWNUM = 1 ")
		;
		return query.toString();
	}

	public static String getQueryColumnSQLImage() {
		StringBuffer query = new StringBuffer()
			.append("SELECT ")
			.append("AD_AttachmentReference.AD_AttachmentReference_ID, NULL, ")
			.append("NVL(AD_AttachmentReference.UUID, '')")
			.append(" || '-' || ")
			.append("NVL(AD_AttachmentReference.FileName, ''), ")
			.append("NVL(AD_AttachmentReference.UUID, '') AS UUID ")
			.append("FROM AD_AttachmentReference ")
			.append("INNER JOIN AD_Image AD_Image ")
			.append("ON AD_Image.AD_Image_ID = AD_AttachmentReference.AD_Image_ID ")
		;
		return query.toString();
	}

	public static String getDirectQueryColumnSQLImage() {
		String query = getQueryColumnSQLImage();
		StringBuffer directQuery = new StringBuffer()
			.append(query)
			.append("WHERE AD_AttachmentReference.AD_Image_ID = ? ")
			.append("AND ROWNUM = 1 ")
		;
		return directQuery.toString();
	}



	public static String getColumnsOrderLocation(String displaySequence, boolean isAddressReverse) {
		StringBuffer cityPostalRegion = new StringBuffer();

		String inStr = displaySequence.replace(",", "|| ', ' ");
		String token;
		int index = inStr.indexOf('@');
		while (index != -1) {
			cityPostalRegion.append(inStr.substring(0, index));          // up to @
			inStr = inStr.substring(index + 1, inStr.length());   // from first @

			int endIndex = inStr.indexOf('@');                     // next @
			if (endIndex < 0) {
				token = "";                                 // no second tag
				endIndex = index + 1;
			} else {
				token = inStr.substring(0, endIndex);
			}
			//  Tokens
			if (token.equals("C")) {
				cityPostalRegion.append("|| NVL(C_Location.City, ")
					.append("(SELECT NVL(C_City.Name, '') FROM C_City WHERE ")
					.append("C_City.C_City_ID = C_Location.C_City_ID")
					.append("), '') ");
			} else if (token.equals("R")) {
				// String regionName = "";
				//  local region name
				// if (!Util.isEmpty(country.getRegionName(), true)) {
				//     regionName = " || ' " + country.getRegionName() + "'";
				// }
				cityPostalRegion.append("|| NVL((SELECT NVL(C_Region.Name, '') FROM C_Region ")
					.append("WHERE C_Region.C_Region_ID = C_Location.C_Region_ID")
					.append("), '') ");
			} else if (token.equals("P")) {
				cityPostalRegion.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Postal + ", '') ");
			} else if (token.equals("A")) {
				cityPostalRegion.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Postal_Add + ", '') ");
			} else {
				cityPostalRegion.append("@").append(token).append("@");
			}

			inStr = inStr.substring(endIndex + 1, inStr.length());   // from second @
			index = inStr.indexOf('@');
		}
		cityPostalRegion.append(inStr); // add the rest of the string 

		StringBuffer query = new StringBuffer();
		if (isAddressReverse) {
			query.append("'' ")
				.append(cityPostalRegion)
				.append("|| ', ' ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address4 + " || ', ' , '') ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address3 + " || ', ' , '') ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address2 + " || ', ' , '') ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address1 + ", '')")
			;
		} else {
			query.append("'' ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address1 + " || ', ' , '') ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address2 + " || ', ' , '')  ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address3 + " || ', ' , '')  ")
				.append("|| NVL(" + I_C_Location.Table_Name + "." + I_C_Location.COLUMNNAME_Address4 + " || ', ' , '') ")
				.append(cityPostalRegion)
			;
		}

		return query.toString();
	}

	public static String getDisplayColumnSQLLocation(String tableName, String columnName) {
		MCountry country = MCountry.getDefault(Env.getCtx());
		boolean isAddressReverse = country.isAddressLinesLocalReverse() || country.isAddressLinesReverse();
		String displaySequence = country.getDisplaySequenceLocal();
		String columnsOrder = getColumnsOrderLocation(displaySequence, isAddressReverse);

		StringBuffer query = new StringBuffer()
			.append("SELECT ")
			.append("NVL(" + columnsOrder + ", '') ")
			.append("FROM C_Location ")
			.append("WHERE C_Location.C_Location_ID = ")
			.append(tableName + "." + columnName)
		;

		return query.toString();
	}

	public static String getQueryLocation() {
		MCountry country = MCountry.getDefault(Env.getCtx());
		boolean isAddressReverse = country.isAddressLinesLocalReverse() || country.isAddressLinesReverse();
		String displaySequence = country.getDisplaySequenceLocal();
		String columnsOrder = getColumnsOrderLocation(displaySequence, isAddressReverse);

		StringBuffer query = new StringBuffer()
			.append("SELECT ")
			.append("C_Location.C_Location_ID, NULL, ")
			.append("NVL(" + columnsOrder + ", '-1'), ")
			.append("C_Location.IsActive ")
			.append("FROM C_Location ")
		;

		return query.toString();
	}

	public static String getDirectQueryLocation() {
		String query = getQueryLocation();
		StringBuffer directQuery = new StringBuffer()
			.append(query)
			.append("WHERE C_Location.C_Location_ID = ? ")
		;

		return directQuery.toString();
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
		return getReferenceLookupInfo(
			referenceId,
			referenceValueId,
			columnName,
			validationRuleId,
			null
		);
	}
	/**
	 * Get Reference information, can return null if reference is invalid or not exists
	 * @param referenceId
	 * @param referenceValueId
	 * @param columnName
	 * @param validationRuleId
	 * @param customRestriction
	 * @return
	 */
	public static MLookupInfo getReferenceLookupInfo(int referenceId, int referenceValueId, String columnName, int validationRuleId, String customRestriction) {
		if(!isLookupReference(referenceId)) {
			return null;
		}
		MLookupInfo lookupInformation = null;
		if (DisplayType.Account == referenceId) {
			columnName = I_C_ValidCombination.COLUMNNAME_C_ValidCombination_ID;
		}

		if (DisplayType.Location == referenceId) {
			columnName = I_C_Location.COLUMNNAME_C_Location_ID;
			lookupInformation = getLookupInfoFromColumnName(columnName);
			lookupInformation.Query = getQueryLocation();
			lookupInformation.QueryDirect = getDirectQueryLocation();
		} else if (DisplayType.PAttribute == referenceId) {
			columnName = I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID;
			lookupInformation = getLookupInfoFromColumnName(columnName);
			lookupInformation.DisplayType = referenceId;
			lookupInformation.Query = "SELECT M_AttributeSetInstance.M_AttributeSetInstance_ID, "
				+ "NULL, M_AttributeSetInstance.Description, M_AttributeSetInstance.IsActive "
				+ "FROM M_AttributeSetInstance ";
			lookupInformation.QueryDirect = lookupInformation.Query
				+ "WHERE M_AttributeSetInstance.M_AttributeSetInstance_ID = ? ";
		} else if (DisplayType.Locator == referenceId) {
			columnName = I_M_Locator.COLUMNNAME_M_Locator_ID;
			lookupInformation = getLookupInfoFromColumnName(columnName);
			lookupInformation.DisplayType = referenceId;
			lookupInformation.Query = "SELECTM_Locator.M_Locator_ID, "
				+ "NULL, M_Locator.Value, M_Locatore.IsActive "
				+ "FROM M_Locator ";
			lookupInformation.QueryDirect = lookupInformation.Query
				+ "WHERE M_Locator.M_Locator_ID = ? ";
		} else if(DisplayType.Image == referenceId) {
			columnName = I_AD_Image.COLUMNNAME_AD_Image_ID;
			lookupInformation = getLookupInfoFromColumnName(columnName);
			lookupInformation.DisplayType = referenceId;
			lookupInformation.Query = getQueryColumnSQLImage();
			lookupInformation.QueryDirect = getDirectQueryColumnSQLImage();
		} else if(DisplayType.TableDir == referenceId
				|| referenceValueId <= 0) {
			// TODO: Add support on MLookupInfo to reuse on Zk/Swing
			if (AccountingUtils.USER_ELEMENT_COLUMNS.contains(columnName)) {
				String newColumnName = AccountingUtils.overwriteColumnName(columnName);
				if (!Util.isEmpty(newColumnName, true)) {
					columnName = newColumnName;
				}
			}
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
		lookupInformation.DisplayType = referenceId;
		lookupInformation.AD_Reference_Value_ID = referenceValueId;

		MValRule validationRule = null;
		//	For validation rule
		if (validationRuleId > 0) {
			validationRule = MValRule.get(Env.getCtx(), validationRuleId);
			if (validationRule != null) {
				if (!Util.isEmpty(validationRule.getCode(), true)) {
					String dynamicValidation = validationRule.getCode();
					if (!validationRule.getCode().startsWith("(")) {
						dynamicValidation = "(" + validationRule.getCode() + ")";
					}
					// // table validation
					// if (!Util.isEmpty(lookupInformation.ValidationCode, true)) {
					// 	dynamicValidation += " AND (" + lookupInformation.ValidationCode + ")";
					// }
					// overwrite ValidationCode with table validation on reference and dynamic validation
					lookupInformation.ValidationCode = dynamicValidation;
				}
			}
		}
		if (!Util.isEmpty(customRestriction, true)) {
			if (!Util.isEmpty(lookupInformation.ValidationCode)
				&& !(customRestriction.trim().startsWith("AND ") || customRestriction.trim().startsWith("OR "))
			 ) {
				lookupInformation.ValidationCode += " AND ";
			}
			lookupInformation.ValidationCode += " (" + customRestriction + ")";
		}

		// return only code without validation rule
		if (Util.isEmpty(lookupInformation.ValidationCode, true)) {
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
		X_AD_Reference reference = new X_AD_Reference(Env.getCtx(), referenceId, null);
		if (reference == null || reference.getAD_Reference_ID() <= 0) {
			return null;
		}
		if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_TableValidation)) {
			lookupInformation = MLookupFactory.getLookupInfo(
				Env.getCtx(),
				0,
				0,
				DisplayType.Search,
				Language.getLanguage(Env.getAD_Language(Env.getCtx())),
				null,
				reference.getAD_Reference_ID(),
				false,
				null,
				false
			);
		} else if(reference.getValidationType().equals(X_AD_Reference.VALIDATIONTYPE_ListValidation)) {
			lookupInformation = MLookupFactory.getLookup_List(
				Language.getLanguage(Env.getAD_Language(Env.getCtx())),
				reference.getAD_Reference_ID()
			);
		}
		return lookupInformation;
	}



	/**
	 * Get Query with UUID
	 * @param tableName
	 * @param query
	 * @return
	 */
	public static String getQueryWithUuid(String tableName, String query) {
		Matcher matcherFrom = Pattern.compile(
			"\\s+(FROM)\\s+(" + tableName + ")",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		).matcher(query);

		List<MatchResult> fromWhereParts = matcherFrom.results()
			.collect(Collectors.toList())
		;

		String queryWithUuid = query;
		if (fromWhereParts != null && fromWhereParts.size() > 0) {
			MatchResult lastFrom = fromWhereParts.get(fromWhereParts.size() - 1);
			queryWithUuid = query.substring(0, lastFrom.start());
			queryWithUuid += ", " + tableName + ".UUID";
			queryWithUuid += query.substring(lastFrom.start());
		}

		return queryWithUuid;
	}

	/**
	 * Get Query with UUID
	 * @param tableName
	 * @param query
	 * @return
	 */
	public static String getQueryWithActiveRestriction(String tableName, String query) {
		Matcher matcherFrom = Pattern.compile(
			"\\s+(FROM)\\s+(" + tableName + ")",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		).matcher(query);

		List<MatchResult> fromWhereParts = matcherFrom.results()
			.collect(Collectors.toList())
		;

		String queryWithUuid = query;
		if (fromWhereParts != null && fromWhereParts.size() > 0) {
			MatchResult lastFrom = fromWhereParts.get(fromWhereParts.size() - 1);
			queryWithUuid = query.substring(0, lastFrom.start());
			queryWithUuid += ", " + tableName + ".UUID";
			queryWithUuid += query.substring(lastFrom.start());
		}

		return queryWithUuid;
	}

	/**
	 * Get Lookup info from column name
	 * @param columnName
	 * @return
	 */
	private static MLookupInfo getLookupInfoFromColumnName(String columnName) {
		return MLookupFactory.getLookupInfo(
			Env.getCtx(),
			0,
			0,
			DisplayType.TableDir,
			Language.getLanguage(Env.getAD_Language(Env.getCtx())),
			columnName,
			0,
			false,
			null,
			false
		);
	}

}
