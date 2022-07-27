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

import java.util.List;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.I_AD_Browse_Field;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.I_AD_Column;
import org.compiere.model.I_AD_Field;
import org.compiere.model.I_AD_Process_Para;
import org.compiere.model.I_AD_Reference;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcessPara;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.util.ASPUtil;

/**
 * Class for store information about reference
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ReferenceInfo {
	
	public ReferenceInfo() {
		displayColumnValue = "";
		displayColumnAlias = "";
		joinColumnName = "";
		tableName = "";
		tableAlias = "";
		columnName = "";
		referenceId = 0;
		hasJoinValue = false;
		language = Language.AD_Language_en_US;
	}
	/**	Display column Value: Test.Name	*/
	private String displayColumnValue;
	/**	Display column Alias: TestName	*/
	private String displayColumnAlias;
	/**	Column Name	*/
	private String columnName;
	/**	Join Column Name	*/
	private String joinColumnName;
	/**	Table Name	*/
	private String tableName;
	/**	Table Alias	*/
	private String tableAlias;
	/**	Language	*/
	private String language;
	/**	Reference ID	*/
	private int referenceId;
	/**	Has Join value	*/
	private boolean hasJoinValue;
	/**	Default Column And Table Alias	*/
	private final String DISPLAY_COLUMN_ALIAS = "DisplayColumn";
	
	public boolean isHasJoinValue() {
		return hasJoinValue;
	}

	public void setHasJoinValue(boolean hasJoinValue) {
		this.hasJoinValue = hasJoinValue;
	}

	public int getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(int referenceId) {
		this.referenceId = referenceId;
	}
	
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public boolean isTranslated() {
		return !language.equals(Language.AD_Language_en_US);
	}

	public String getDisplayColumnValue() {
		return getDisplayColumnValue(false);
	}
	
	private String getDisplayColumnValue(boolean withTable) {
		if(withTable) {
			return getTableAlias(isTranslated()) + "." + displayColumnValue;
		}
		return displayColumnValue;
	}
	
	public void setDisplayColumnValue(String displayColumnValue) {
		this.displayColumnValue = displayColumnValue;
	}
	
	private String getDisplayColumnAlias() {
		return displayColumnAlias;
	}
	
	public void setDisplayColumnAlias(String displayColumnAlias) {
		this.displayColumnAlias = displayColumnAlias;
	}
	
	public String getJoinColumnName() {
		return getJoinColumnName(false);
	}
	
	private String getJoinColumnName(boolean withTable) {
		if(withTable) {
			return getTableAlias() + "." + joinColumnName;
		}
		return joinColumnName;
	}
	
	public void setJoinColumnName(String joinColumnName) {
		this.joinColumnName = joinColumnName;
		setHasJoinValue(true);
	}
	
	private String getTableName() {
		return getTableName(false);
	}
	
	private String getTableName(boolean translated) {
		if(translated) {
			return tableName + "_Trl";
		}
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	private String getTableAlias(boolean translated) {
		if(translated) {
			return tableAlias + "_Trl";
		}
		return tableAlias;
	}
	
	private String getTableAlias() {
		return getTableAlias(false);
	}
	
	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
	
	/**
	 * Without column name
	 */
	private void buildAlias() {
		buildAlias(null);
	}
	
	/**
	 * Create alias
	 */
	private void buildAlias(String columnName) {
		//	For table alias
		if(Util.isEmpty(tableAlias)) {
			setTableAlias(getColumnName() + "_" + getTableName());
		}
		//	For column alias
		if(Util.isEmpty(displayColumnAlias)) {
			if(Util.isEmpty(columnName)) {
				setDisplayColumnAlias(DISPLAY_COLUMN_ALIAS + "_" + getColumnName());
			} else {
				setDisplayColumnAlias(DISPLAY_COLUMN_ALIAS + "_" + columnName);
			}
		}
	}
	
	/**
	 * Get display value for Query
	 * @return
	 */
	public String getDisplayValue() {
		buildAlias();
		//	
		return getDisplayColumnValue(isHasJoinValue()) + " AS \"" + getDisplayColumnAlias() + "\"";
	}
	
	/**
	 * With custom column name
	 * @param columnName
	 * @return
	 */
	public String getDisplayValue(String columnName) {
		buildAlias(columnName);
		//	
		return getDisplayColumnValue(isHasJoinValue()) + " AS \"" + getDisplayColumnAlias() + "\"";
	}
	
	/**
	 * Get Join value for query
	 * @return
	 */
	public String getJoinValue(String baseColumnName, String baseTable) {
		buildAlias();
		if(!isHasJoinValue()) {
			return "";
		}
		StringBuffer join = new StringBuffer();
		join.append(" LEFT JOIN ")
			.append(getTableName()).append(" AS ").append(getTableAlias())
			.append(" ON(").append(getJoinColumnName(true)).append(" = ").append(baseTable).append(".").append(baseColumnName);
		//	Reference
		if(getReferenceId() > 0) {
			join.append(" AND ").append(getTableAlias() + ".AD_Reference_ID = ").append(getReferenceId());
		}
		join.append(")");
		//	Language
		if(isTranslated()) {
			join.append(" LEFT JOIN ")
				.append(getTableName(true)).append(" AS ").append(getTableAlias(true))
				.append(" ON(").append(getTableAlias(true) + "." + getTableName() + "_ID").append(" = ").append(getTableAlias() + "." + getTableName() + "_ID");
				join.append(" AND ").append(getTableAlias(true)).append(".").append("AD_Language = '").append(getLanguage()).append("'").append(")");
		}
		//	Return
		return join.toString();
	}

	/**
	 * Get reference Info from request
	 * @param request
	 * @return
	 */
	static public MLookupInfo getInfoFromRequest(String referenceUuid, String fieldUuid, String processParameterUuid, String browseFieldUuid, String columnUuid, String columnName, String tableName) {
		int referenceId = 0;
		int referenceValueId = 0;
		int validationRuleId = 0;
		if(!Util.isEmpty(referenceUuid)) {
			referenceId = RecordUtil.getIdFromUuid(I_AD_Reference.Table_Name, referenceUuid, null);
		} else if(!Util.isEmpty(fieldUuid)) {
			MField field = (MField) RecordUtil.getEntity(Env.getCtx(), I_AD_Field.Table_Name, fieldUuid, 0, null);
			int fieldId = field.getAD_Field_ID();
			List<MField> customFields = ASPUtil.getInstance(Env.getCtx()).getWindowFields(field.getAD_Tab_ID());
			if(customFields != null) {
				Optional<MField> maybeField = customFields.stream().filter(customField -> customField.getAD_Field_ID() == fieldId).findFirst();
				if(maybeField.isPresent()) {
					field = maybeField.get();
					MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
					//	Display Type
					referenceId = column.getAD_Reference_ID();
					referenceValueId = column.getAD_Reference_Value_ID();
					validationRuleId = column.getAD_Val_Rule_ID();
					columnName = column.getColumnName();
					if(field.getAD_Reference_ID() > 0) {
						referenceId = field.getAD_Reference_ID();
					}
					if(field.getAD_Reference_Value_ID() > 0) {
						referenceValueId = field.getAD_Reference_Value_ID();
					}
					if(field.getAD_Val_Rule_ID() > 0) {
						validationRuleId = field.getAD_Val_Rule_ID();
					}
				}
			}
		} else if(!Util.isEmpty(browseFieldUuid)) {
			MBrowseField browseField = (MBrowseField) RecordUtil.getEntity(Env.getCtx(), I_AD_Browse_Field.Table_Name, browseFieldUuid, 0, null);
			int browseFieldId = browseField.getAD_Browse_Field_ID();
			List<MBrowseField> customFields = ASPUtil.getInstance(Env.getCtx()).getBrowseFields(browseField.getAD_Browse_ID());
			if(customFields != null) {
				Optional<MBrowseField> maybeField = customFields.stream().filter(customField -> customField.getAD_Browse_Field_ID() == browseFieldId).findFirst();
				if(maybeField.isPresent()) {
					browseField = maybeField.get();
					referenceId = browseField.getAD_Reference_ID();
					referenceValueId = browseField.getAD_Reference_Value_ID();
					validationRuleId = browseField.getAD_Val_Rule_ID();
					MViewColumn viewColumn = browseField.getAD_View_Column();
					if(viewColumn.getAD_Column_ID() > 0) {
						columnName = MColumn.getColumnName(Env.getCtx(), viewColumn.getAD_Column_ID());
					} else {
						columnName = browseField.getAD_Element().getColumnName();
					}
				}
			}
		} else if(!Util.isEmpty(processParameterUuid)) {
			MProcessPara processParameter = (MProcessPara) RecordUtil.getEntity(Env.getCtx(), I_AD_Process_Para.Table_Name, processParameterUuid, 0, null);
			int processParameterId = processParameter.getAD_Process_Para_ID();
			List<MProcessPara> customParameters = ASPUtil.getInstance(Env.getCtx()).getProcessParameters(processParameter.getAD_Process_ID());
			if(customParameters != null) {
				Optional<MProcessPara> maybeParameter = customParameters.stream().filter(customField -> customField.getAD_Process_Para_ID() == processParameterId).findFirst();
				if(maybeParameter.isPresent()) {
					processParameter = maybeParameter.get();
					referenceId = processParameter.getAD_Reference_ID();
					referenceValueId = processParameter.getAD_Reference_Value_ID();
					validationRuleId = processParameter.getAD_Val_Rule_ID();
					columnName = processParameter.getColumnName();
				}
			}
		} else if(!Util.isEmpty(columnUuid)) {
			int columnId = RecordUtil.getIdFromUuid(I_AD_Column.Table_Name, columnUuid, null);
			if(columnId > 0) {
				MColumn column = MColumn.get(Env.getCtx(), columnId);
				referenceId = column.getAD_Reference_ID();
				referenceValueId = column.getAD_Reference_Value_ID();
				validationRuleId = column.getAD_Val_Rule_ID();
				columnName = column.getColumnName();
			}
		} else if(!Util.isEmpty(columnName)) {
			referenceId = DisplayType.TableDir;
		} else if(!Util.isEmpty(tableName)) {	//	Is a Table Direct
			referenceId = DisplayType.TableDir;
			columnName = tableName + "_ID";
		} else {
			throw new AdempiereException("@AD_Reference_ID@ / @AD_Column_ID@ / @AD_Table_ID@ / @AD_Process_Para_ID@ / @IsMandatory@");
		}
		return ReferenceUtil.getReferenceLookupInfo(referenceId, referenceValueId, columnName, validationRuleId);
	}
	
	@Override
	public String toString() {
		return "ReferenceInfo [displayColumnValue=" + displayColumnValue + ", displayColumnAlias=" + displayColumnAlias
				+ ", columnName=" + columnName + ", joinColumnName=" + joinColumnName + ", tableName=" + tableName
				+ ", tableAlias=" + tableAlias + ", language=" + language + ", referenceId=" + referenceId + "]";
	}
}
