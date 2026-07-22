/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package com.solop.sp008.process;

import org.compiere.model.Query;
import org.compiere.report.MReportLine;
import org.compiere.report.MReportSource;
import org.compiere.util.DB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 	Generated Process for (Create Report Lines)
 * 	Create report lines from definitions
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 */
public class CreateReportLines extends CreateReportLinesAbstract {

	/**	Cashflow	*/
	private static final String Cashflow = "C";
	/**	General Balance	*/
	private static final String General_Balance = "G";
	/**	Profit and Lost	*/
	private static final String Profit_and_Lost = "P";
	/**	Trial Balance	*/
	private static final String Trial_Balance = "T";
	/**	Account Type	*/
	private static final String SP008_AccountType = "SP008_AccountType";
	/**	Column name not exposed by the generated model	*/
	private static final String COLUMNNAME_Parent_ID = "Parent_ID";
	/**	Canonical order for the AccountType sections	*/
	private static final List<String> ACCOUNT_TYPE_ORDER = Arrays.asList("A", "L", "O", "R", "E", "M");

	@Override
	protected String doIt() throws Exception {
		int reportLineSetId = getRecord_ID();
		if(reportLineSetId <= 0) {
			return "@NoRecordsFound@";
		}
		//
		List<String> validAccountTypes = getValidAccountTypes(getReportType());
		if(validAccountTypes == null) {
			return "@ActionNotSupported@";
		}
		if(isRecreateLines()) {
			//	Regenerate the whole definition from scratch: delete the child lines and their
			//	sources, then rebuild both from the account tree.
			deleteDefinition(reportLineSetId);
			int created = createStructure(reportLineSetId, validAccountTypes);
			return "@Created@ " + created;
		}
		//	Keep the existing lines; only delete and rebuild their sources (validating account type).
		int created = recreateSources(reportLineSetId, validAccountTypes);
		return "@Created@ " + created;
	}

	/**
	 * Devuelve los AccountType válidos para el tipo de reporte, en el orden canónico.
	 * Devuelve null si el reporte no está soportado.
	 */
	private List<String> getValidAccountTypes(String reportType) {
		List<String> valid;
		if(Trial_Balance.equals(reportType)) {
			valid = Arrays.asList("A", "L", "O", "R", "E", "M");
		} else if(Profit_and_Lost.equals(reportType)) {
			valid = Arrays.asList("R", "E");
		} else if(General_Balance.equals(reportType)) {
			valid = Arrays.asList("A", "L", "O");
		} else {
			//	Cashflow requiere lógica diferente — ver sección §5 del .md.
			return null;
		}
		return ACCOUNT_TYPE_ORDER.stream()
				.filter(valid::contains)
				.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * Genera la estructura de PA_ReportLine replicando el árbol de cuentas del elemento:
	 * una línea por cada cuenta SUMARIA, con Name = código de cuenta y Description = nombre
	 * real de la cuenta (igual que las filas de detalle que arma el motor), anidada
	 * por Parent_ID según el árbol y en su mismo orden (SeqNo del árbol). Cada línea lleva
	 * su propia cuenta como source (totaliza su subárbol); las sumarias de último nivel
	 * listan además sus cuentas imputables. Solo se incluyen las ramas cuyo AccountType es
	 * válido para el tipo de reporte.
	 *
	 * @param reportLineSetId    PA_ReportLineSet_ID destino
	 * @param validAccountTypes  AccountTypes a incluir
	 * @return cantidad de líneas creadas
	 */
	private int createStructure(int reportLineSetId, List<String> validAccountTypes) {
		int treeId = DB.getSQLValueEx(get_TrxName(),
				"SELECT AD_Tree_ID FROM C_Element WHERE C_Element_ID = ?", getElementId());
		if(treeId <= 0) {
			log.warning("El elemento " + getElementId() + " no tiene árbol de cuentas asociado");
			return 0;
		}
		Map<Integer, List<AccountNode>> childrenByParent = loadTree(treeId);
		int[] seqNo = new int[]{0};
		int[] created = new int[]{0};
		//	Root accounts hang from node 0
		createLines(reportLineSetId, childrenByParent, 0, 0, validAccountTypes, seqNo, created);
		return created[0];
	}

	/**
	 * Recorre el árbol en profundidad (en el orden del árbol). Solo crea una línea por
	 * cuenta SUMARIA cuyo AccountType sea válido; las ramas de un tipo no válido se omiten.
	 * Las cuentas imputables no generan línea propia: las lista el motor (ListSources) al
	 * renderizar. Las líneas se anidan por Parent_ID a la línea de su sumaria padre.
	 */
	private void createLines(int reportLineSetId, Map<Integer, List<AccountNode>> childrenByParent,
			int parentNodeId, int parentReportLineId, List<String> validAccountTypes,
			int[] seqNo, int[] created) {
		List<AccountNode> children = childrenByParent.get(parentNodeId);
		if(children == null) {
			return;
		}
		for(AccountNode node : children) {
			if(!validAccountTypes.contains(node.accountType)) {
				continue; //	rama de un tipo no incluido en este reporte
			}
			if(!node.isSummary) {
				continue; //	las cuentas imputables no generan línea; las lista ListSources
			}
			seqNo[0] += 10;
			MReportLine reportLine = new MReportLine(getCtx(), 0, get_TrxName());
			reportLine.setPA_ReportLineSet_ID(reportLineSetId);
			//	Igual que las filas de detalle, que el motor completa con
			//	(Name, Description) = (C_ElementValue.Value, C_ElementValue.Name)
			reportLine.setName(node.value);
			reportLine.setDescription(node.name);
			reportLine.setSeqNo(seqNo[0]);
			reportLine.setLineType(MReportLine.LINETYPE_SegmentValue);
			reportLine.setIsPrinted(true);
			reportLine.set_ValueOfColumn(SP008_AccountType, node.accountType);
			reportLine.saveEx();
			int reportLineId = reportLine.getPA_ReportLine_ID();
			//	Parent_ID exists in PA_ReportLine but its AD_Column is inactive, so POInfo
			//	skips it and set_ValueOfColumn would drop the value. Write it directly.
			if(parentReportLineId > 0) {
				DB.executeUpdateEx(
						"UPDATE PA_ReportLine SET " + COLUMNNAME_Parent_ID + " = ? WHERE PA_ReportLine_ID = ?",
						new Object[]{parentReportLineId, reportLineId}, get_TrxName());
			}
			//	Single source: the account itself. Summary accounts total their subtree.
			//	Only the last-level summaries (no summary children) list their leaf accounts,
			//	so each posting account appears once instead of under every ancestor.
			MReportSource reportSource = new MReportSource(getCtx(), 0, get_TrxName());
			reportSource.setPA_ReportLine_ID(reportLineId);
			reportSource.setElementType(MReportSource.ELEMENTTYPE_Account);
			reportSource.setC_ElementValue_ID(node.id);
			reportSource.setListSources(!hasSummaryChild(childrenByParent, node.id));
			reportSource.saveEx();
			created[0]++;
			//	Recurse into children (tree order), nesting them under this line
			createLines(reportLineSetId, childrenByParent, node.id, reportLineId, validAccountTypes, seqNo, created);
		}
	}

	/**
	 * Reconstruye únicamente los PA_ReportSource de las líneas existentes del set, sin tocar
	 * las PA_ReportLine. Para cada línea busca en el árbol la cuenta cuyo código (Value)
	 * coincide con el Name de la línea; valida que su AccountType sea válido para el reporte
	 * y crea un source con ella (ListSources según tenga o no sumarias hijas). Las líneas sin
	 * cuenta correspondiente (ej. líneas de cálculo) o con tipo no válido se omiten.
	 *
	 * @param reportLineSetId    PA_ReportLineSet_ID destino
	 * @param validAccountTypes  AccountTypes a incluir
	 * @return cantidad de sources creados
	 */
	private int recreateSources(int reportLineSetId, List<String> validAccountTypes) {
		int treeId = DB.getSQLValueEx(get_TrxName(),
				"SELECT AD_Tree_ID FROM C_Element WHERE C_Element_ID = ?", getElementId());
		if(treeId <= 0) {
			log.warning("El elemento " + getElementId() + " no tiene árbol de cuentas asociado");
			return 0;
		}
		Map<Integer, List<AccountNode>> childrenByParent = loadTree(treeId);
		//	Index accounts by their code (Value) to match the line's Name against the tree.
		Map<String, AccountNode> nodesByValue = new HashMap<>();
		for(List<AccountNode> nodes : childrenByParent.values()) {
			for(AccountNode node : nodes) {
				nodesByValue.put(node.value, node);
			}
		}
		//	Sources are always deleted and recreated, regardless of the recreate-lines flag.
		deleteSources(reportLineSetId);
		int created = 0;
		List<MReportLine> reportLines = new Query(getCtx(), MReportLine.Table_Name,
				"PA_ReportLineSet_ID = ?", get_TrxName())
				.setParameters(reportLineSetId)
				.setOrderBy("SeqNo")
				.list();
		for(MReportLine reportLine : reportLines) {
			AccountNode node = nodesByValue.get(reportLine.getName());
			if(node == null) {
				//	Línea sin cuenta correspondiente en el árbol (ej. línea de cálculo): se ignora
				continue;
			}
			if(!validAccountTypes.contains(node.accountType)) {
				log.warning(String.format(
						"Línea %d (%s) con AccountType=%s no es válida para este tipo de reporte. Se omite.",
						reportLine.getPA_ReportLine_ID(), reportLine.getName(), node.accountType));
				continue;
			}
			MReportSource reportSource = new MReportSource(getCtx(), 0, get_TrxName());
			reportSource.setPA_ReportLine_ID(reportLine.getPA_ReportLine_ID());
			reportSource.setElementType(MReportSource.ELEMENTTYPE_Account);
			reportSource.setC_ElementValue_ID(node.id);
			reportSource.setListSources(!hasSummaryChild(childrenByParent, node.id));
			reportSource.saveEx();
			created++;
		}
		return created;
	}

	/**
	 * Indica si el nodo tiene al menos una cuenta hija que también es sumaria.
	 */
	private boolean hasSummaryChild(Map<Integer, List<AccountNode>> childrenByParent, int nodeId) {
		List<AccountNode> children = childrenByParent.get(nodeId);
		if(children == null) {
			return false;
		}
		return children.stream().anyMatch(child -> child.isSummary);
	}

	/**
	 * Carga el árbol de cuentas del elemento como un mapa Parent_ID -> hijos, ordenados
	 * por el SeqNo del árbol. La raíz virtual (Node_ID = 0) queda como clave 0.
	 */
	private Map<Integer, List<AccountNode>> loadTree(int treeId) {
		Map<Integer, List<AccountNode>> childrenByParent = new HashMap<>();
		String sql = "SELECT COALESCE(tn.Parent_ID, 0) AS Parent_ID, tn.Node_ID, "
				+ "ev.Value, ev.Name, ev.AccountType, ev.IsSummary "
				+ "FROM AD_TreeNode tn "
				+ "INNER JOIN C_ElementValue ev ON (ev.C_ElementValue_ID = tn.Node_ID) "
				+ "WHERE tn.AD_Tree_ID = ? AND tn.Node_ID > 0 AND ev.IsActive = 'Y' "
				+ "ORDER BY COALESCE(tn.Parent_ID, 0), tn.SeqNo";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, treeId);
			rs = pstmt.executeQuery();
			while(rs.next()) {
				AccountNode node = new AccountNode(
						rs.getInt("Node_ID"),
						rs.getString("Value"),
						rs.getString("Name"),
						rs.getString("AccountType"),
						"Y".equals(rs.getString("IsSummary")));
				childrenByParent
						.computeIfAbsent(rs.getInt("Parent_ID"), k -> new ArrayList<>())
						.add(node);
			}
		} catch(Exception e) {
			throw new IllegalStateException("No se pudo cargar el árbol de cuentas " + treeId, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return childrenByParent;
	}

	/**
	 * Borra la definición actual del set: primero los PA_ReportSource (por la FK) y
	 * luego las PA_ReportLine.
	 */
	private void deleteDefinition(int reportLineSetId) {
		deleteSources(reportLineSetId);
		int lines = DB.executeUpdateEx(
				"DELETE FROM PA_ReportLine WHERE PA_ReportLineSet_ID = ?",
				new Object[]{reportLineSetId}, get_TrxName());
		log.fine("Deleted Lines " + lines);
	}

	/**
	 * Borra los PA_ReportSource de todas las líneas del set, sin tocar las PA_ReportLine.
	 */
	private void deleteSources(int reportLineSetId) {
		int sources = DB.executeUpdateEx(
				"DELETE FROM PA_ReportSource WHERE PA_ReportLine_ID IN "
						+ "(SELECT PA_ReportLine_ID FROM PA_ReportLine WHERE PA_ReportLineSet_ID = ?)",
				new Object[]{reportLineSetId}, get_TrxName());
		log.fine("Deleted Sources " + sources);
	}

	/**
	 * Nodo de cuenta del árbol usado para armar la estructura.
	 */
	private static class AccountNode {
		final int id;
		final String value;
		final String name;
		final String accountType;
		final boolean isSummary;

		AccountNode(int id, String value, String name, String accountType, boolean isSummary) {
			this.id = id;
			this.value = value;
			this.name = name;
			this.accountType = accountType;
			this.isSummary = isSummary;
		}
	}
}
