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
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.base.util;

import java.util.Properties;
import java.util.Set;

import org.adempiere.core.domains.models.X_AD_Window;
import org.compiere.model.MColumn;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Evaluator;
import org.compiere.util.Util;

/**
 * Guards that protect record-write endpoints from mutating closed-document
 * headers.
 *
 * Centralizes the rule: once a document reaches a closed DocStatus, the
 * cabecera columns listed in {@link #DOC_CRITICAL_COLUMNS} must NEVER be
 * modified, regardless of AD_Column.IsAlwaysUpdateable. This protects against
 * accidental misconfiguration of the flag and against endpoints that bypass
 * the standard Processed guard.
 *
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public final class RecordWriteGuard {

	/** DocStatus values that close the document and lock its header. */
	public static final Set<String> DOC_CLOSED_STATUSES = Set.of(
		"CO", "CL", "VO", "RE"
	);

	/** Header columns that must never be modified on a closed document. */
	public static final Set<String> DOC_CRITICAL_COLUMNS = Set.of(
		"DocumentNo", "C_DocType_ID", "C_DocTypeTarget_ID",
		"DateAcct", "DateInvoiced", "C_BPartner_ID",
		"GrandTotal", "TotalLines", "C_Currency_ID",
		"AD_Org_ID", "M_Warehouse_ID", "C_PaymentTerm_ID",
		"Processed", "Processing", "DocStatus", "DocAction"
	);

	private RecordWriteGuard() {}

	/**
	 * @return true if {@code docStatus} is one of the values that close a
	 *         document header to further edits.
	 */
	public static boolean isClosedDocStatus(String docStatus) {
		return docStatus != null && DOC_CLOSED_STATUSES.contains(docStatus);
	}

	/**
	 * @return true if {@code entity}'s DocStatus is closed. Returns false for
	 *         null entities or for tables that do not expose a DocStatus column.
	 */
	public static boolean isDocumentClosed(PO entity) {
		if (entity == null) {
			return false;
		}
		if (entity.get_ColumnIndex("DocStatus") < 0) {
			return false;
		}
		return isClosedDocStatus(entity.get_ValueAsString("DocStatus"));
	}

	/**
	 * @return true if {@code columnName} is a header column whose value must
	 *         be preserved once the document is closed.
	 */
	public static boolean isCriticalDocumentColumn(String columnName) {
		return columnName != null && DOC_CRITICAL_COLUMNS.contains(columnName);
	}

	/**
	 * Decides whether a column must be skipped when writing to {@code entity}.
	 *
	 * Returns true when any of the following apply:
	 * <ul>
	 *   <li>{@code column} is null or not persisted.</li>
	 *   <li>The column is not {@code IsAlwaysUpdateable} AND either it is not
	 *       updateable, or {@code entity.Processed='Y'}, or {@code entity.Processing='Y'}.</li>
	 *   <li>The entity is in a closed DocStatus AND the column is in
	 *       {@link #DOC_CRITICAL_COLUMNS} — applies even if the column is
	 *       {@code IsAlwaysUpdateable}.</li>
	 * </ul>
	 *
	 * @param entity the persisted record being mutated; may be null
	 * @param column the column to be written
	 * @return true if the write must be skipped
	 */
	public static boolean shouldSkipColumn(PO entity, MColumn column) {
		if (column == null || column.getAD_Column_ID() <= 0) {
			return true;
		}
		if (!column.isAlwaysUpdateable()) {
			if (!column.isUpdateable()) {
				return true;
			}
			if (entity != null) {
				if (entity.get_ColumnIndex("Processed") >= 0 && entity.get_ValueAsBoolean("Processed")) {
					return true;
				}
				if (entity.get_ColumnIndex("Processing") >= 0 && entity.get_ValueAsBoolean("Processing")) {
					return true;
				}
			}
		}
		if (isDocumentClosed(entity) && isCriticalDocumentColumn(column.getColumnName())) {
			return true;
		}
		return false;
	}

	/**
	 * @return true if {@code table} cannot accept writes (e.g., it is a view
	 *         or is null).
	 */
	public static boolean isTableReadOnly(MTable table) {
		if (table == null) {
			return true;
		}
		return table.isView();
	}

	/**
	 * @return true if {@code tab} is read-only, either by its {@code IsReadOnly}
	 *         flag or because its {@code ReadOnlyLogic} evaluates to true
	 *         against {@code entity}.
	 *
	 * @param tab    the tab definition
	 * @param entity the record used to evaluate {@code ReadOnlyLogic}; if null,
	 *               only the static {@code IsReadOnly} flag is checked.
	 */
	public static boolean isTabReadOnly(MTab tab, PO entity) {
		if (tab == null) {
			return true;
		}
		if (tab.isReadOnly()) {
			return true;
		}
		String readOnlyLogic = tab.getReadOnlyLogic();
		if (Util.isEmpty(readOnlyLogic, true) || entity == null) {
			return false;
		}
		return Evaluator.evaluateLogic(entity, readOnlyLogic);
	}

	/**
	 * @return true if {@code window} cannot accept writes (currently: window
	 *         type is {@code QueryOnly} or window is null).
	 */
	public static boolean isWindowReadOnly(MWindow window) {
		if (window == null) {
			return true;
		}
		return X_AD_Window.WINDOWTYPE_QueryOnly.equals(window.getWindowType());
	}

	/**
	 * @return true if {@code entity} belongs to a different AD_Client than the
	 *         session resolved from {@code context}. Cross-client writes are a
	 *         tenancy violation and must be rejected.
	 */
	public static boolean isForeignClient(Properties context, PO entity) {
		if (context == null || entity == null) {
			return false;
		}
		return Env.getAD_Client_ID(context) != entity.getAD_Client_ID();
	}

}
