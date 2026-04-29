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

import java.util.List;
import java.util.Optional;

import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.util.Env;

/**
 * 	The Stub class for reference
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReferenceValues {

	private String tableName;
	private String accessLevel;
	private int displayTypeId;
	private int referenceValueId;
	private String embeddedContextColumn;
	private List<String> contextColumns;



	private ReferenceValues(String tableName, String accessLevel, int displayTypeId, int referenceValueId, String embeddedContextColumn, List<String> contextColumns) {
		this.displayTypeId = displayTypeId;
		this.tableName = tableName;
		this.accessLevel = accessLevel;
		this.referenceValueId = referenceValueId;
		this.embeddedContextColumn = embeddedContextColumn;
		this.contextColumns = contextColumns;
	}



	public static ReferenceValues newInstance(int displayTypeId, String tableName, String embeddedContextColumn) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		List<String> contextColumnsList = ReferenceUtil.getContextColumnNames(
			Optional.ofNullable(embeddedContextColumn).orElse("")
		);
		return new ReferenceValues(
			tableName,
			table.getAccessLevel(),
			displayTypeId,
			0,
			embeddedContextColumn,
			contextColumnsList
		);
	}

	public static ReferenceValues newInstance(MLookupInfo lookupInfo) {
		MTable table = MTable.get(Env.getCtx(), lookupInfo.TableName);
		String embeddedContextColumn = Optional.ofNullable(lookupInfo.QueryDirect).orElse("")
			+ Optional.ofNullable(lookupInfo.Query).orElse("")
			+ Optional.ofNullable(lookupInfo.ValidationCode).orElse("")
		;
		List<String> contextColumnsList = ReferenceUtil.getContextColumnNames(
			Optional.ofNullable(embeddedContextColumn).orElse("")
		);
		return new ReferenceValues(
			table.getTableName(),
			table.getAccessLevel(),
			lookupInfo.DisplayType,
			lookupInfo.AD_Reference_Value_ID,
			embeddedContextColumn,
			contextColumnsList
		);
	}



	public String getTableName() {
		return tableName;
	}

	public String getAccessLevel() {
		return accessLevel;
	}

	public int getDisplayTypeId() {
		return displayTypeId;
	}

	public int getReferenceValueId() {
		return referenceValueId;
	}

	public String getEmbeddedContextColumn() {
		return embeddedContextColumn;
	}

	public List<String> getContextColumns() {
		return contextColumns;
	}

}
