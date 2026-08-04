/**
 * 
 */
package org.solop.util;

import org.adempiere.core.domains.models.*;
import org.adempiere.pipo.PackOut;
import org.adempiere.pipo.handler.GenericPOHandler;
import org.compiere.model.*;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.TransformerHandler;
import java.util.List;
import java.util.Properties;

/**
 *    @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/11">https://github.com/solop-develop/adempiere-base/issues/11</a>
 */
public class EntityTypeExport extends GenericPOHandler {
	public void create(Properties ctx, TransformerHandler document) throws SAXException {
		int entityTypeId = Env.getContextAsInt(ctx, X_AD_Package_Exp_Detail.COLUMNNAME_AD_EntityType_ID);
		PackOut packOut = (PackOut) ctx.get("PackOutProcess");
		if(packOut == null ) {
			packOut = new PackOut();
			packOut.setLocalContext(ctx);
		}
		MEntityType entityType = new MEntityType(Env.getCtx(), entityTypeId, null);
		//	Entity Type
		packOut.createGenericPO(document, I_AD_EntityType.Table_ID, entityTypeId, false, null);
		//	Messages
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Message.Table_Name, false, null);
		//	Windows
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Window.Table_Name, false, null);
		//	Tables
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Table.Table_Name, false, null);
		//	Forms
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Form.Table_Name, false, null);
		//	Report Views
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_ReportView.Table_Name, false, null);
		//	Validation Rules
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Val_Rule.Table_Name, false, null);
		//	Reference Header Only
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Reference.Table_Name, false, null);
		//	Elements
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Element.Table_Name, false, null);
		//	View
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_View.Table_Name, false, null);
		//	View Definition
		createViewDefinition(packOut, document, entityType.getEntityType());
		//	Columns
		createViewColumns(packOut, document, entityType.getEntityType());
		//	Process Without Browser
		createProcessWithoutBrowser(packOut, document, entityType.getEntityType());
		//	Smart Browser
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Browse.Table_Name, false, null);
		//	Browse Fields
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Browse_Field.Table_Name, false, null);
		//	Process
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Process.Table_Name, false, null);
		//	Table Process
		createReferencesNoId(packOut, document, entityType.getEntityType(),  I_AD_Table_Process.Table_Name, false, null);
		//	Parameters
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Process_Para.Table_Name, false, null);
		//	Columns
		createColumns(packOut, document, entityType.getEntityType());
		//	Reference
		createReferenceListAndTable(packOut, document, entityType.getEntityType());
		// Relation Type
		createReferences(packOut, document, entityType.getEntityType(), I_AD_RelationType.Table_Name, false, null);
		//	Tabs
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Tab.Table_Name, false, null);
		//	Fields
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Field.Table_Name, false, null);
		//	Rules
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Rule.Table_Name, false, null);
		//	Table Rules
		createScriptValidators(packOut, document, entityType.getEntityType());
		//	Modules
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Module.Table_Name, false, null);
		//	SubModules
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_SubModule.Table_Name, false, null);
		//	Workflows
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_Workflow.Table_Name, false, null);
		//	Workflow Nodes
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_WF_Node.Table_Name, false, null);
		//	Workflow Node Next
		createReferences(packOut, document, entityType.getEntityType(),  I_AD_WF_NodeNext.Table_Name, false, null);
		//	Workflow Node Parameters
		createWorkflowNodeParameters(packOut, document, entityType.getEntityType());

		//	Create Menu
		createMenu(packOut, document, entityType.getEntityType());
	}

	private void createMenu(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_Menu.Table_Name, "EntityType = ?", null)
				.setParameters(entityType)
				.getIDsAsList();
		for (int id : referenceIds) {
			createParentMenu(packOut, document, id, entityType);
			packOut.createGenericPO(document, I_AD_Menu.Table_ID, id, false, null);
		}
	}

	private void createParentMenu(PackOut packOut, TransformerHandler document, int menuId, String entityType) throws SAXException {
		MMenu menu = MMenu.getFromId(Env.getCtx(), menuId);
		//	Create reference if exists
		packOut.createGenericPO(document, menu, false, null);
		//	Get default tree
		int defaultTreeId = MTree.getDefaultTreeIdFromTableId(menu.getAD_Client_ID(), I_AD_Menu.Table_ID);
		//	Create Parent
		packOut.createGenericPO(document, menu);
		String childSQL = "SELECT m.AD_Menu_ID "
				+ "FROM AD_Menu m "
				+ "WHERE EXISTS(SELECT 1 FROM AD_TreeNodeMM tnm "
				+ "			WHERE tnm.Parent_ID = m.AD_Menu_ID "
				+ "			AND tnm.AD_Tree_ID = " + defaultTreeId + " "
				+ "			AND tnm.Node_ID = ?)"
				+ " AND m.EntityType = ?";
		int parentId = DB.getSQLValueEx(null, childSQL, menu.getAD_Menu_ID(), entityType);
		if(parentId > 0) {
			createParentMenu(packOut, document, parentId, entityType);
		}
	}

	private void createReferences(PackOut packOut, TransformerHandler document, String entityType, String tableName, boolean includeParents, List<String> excludedParentList) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), tableName, "EntityType = ?", null)
				.setParameters(entityType)
				.getIDsAsList();
		int tableId = MTable.getTable_ID(tableName);
		for (int id : referenceIds) {
			packOut.createGenericPO(document, tableId, id, includeParents, excludedParentList);
		}
	}

	private void createProcessWithoutBrowser(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_Process.Table_Name, "EntityType = ? AND AD_Browse_ID IS NULL", null)
				.setParameters(entityType)
				.getIDsAsList();
		int tableId = MTable.getTable_ID(I_AD_Process.Table_Name);
		for (int id : referenceIds) {
			packOut.createGenericPO(document, tableId, id, false, null);
		}
	}

	private void createReferencesNoId(PackOut packOut, TransformerHandler document, String entityType, String tableName, boolean includeParents, List<String> excludedParentList) throws SAXException {
		List<PO> records = new Query(Env.getCtx(), tableName, "EntityType = ?", null)
				.setParameters(entityType)
				.list();
		for (PO record : records) {
			packOut.createGenericPO(document, record, includeParents, excludedParentList);
		}
	}

	private void createScriptValidators(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<PO> records = new Query(Env.getCtx(), I_AD_Table_ScriptValidator.Table_Name, "EXISTS(SELECT 1 FROM AD_Rule r WHERE r.AD_Rule_ID = AD_Table_ScriptValidator.AD_Rule_ID AND r.EntityType = ?)", null)
				.setParameters(entityType)
				.list();
		for (PO record : records) {
			packOut.createGenericPO(document, record, false, null);
		}
	}

	private void createWorkflowNodeParameters(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<PO> records = new Query(Env.getCtx(), I_AD_WF_Node_Para.Table_Name, "EXISTS(SELECT 1 FROM AD_WF_Node n WHERE n.AD_WF_Node_ID = AD_WF_Node_Para.AD_WF_Node_ID AND n.EntityType = ?)", null)
				.setParameters(entityType)
				.list();
		for (PO record : records) {
			packOut.createGenericPO(document, record, false, null);
		}
	}

	private void createColumns(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_Column.Table_Name, "EntityType = ? ", null)
				.setParameters(entityType)
				.getIDsAsList();
		for (int id : referenceIds) {
			packOut.createGenericPO(document, I_AD_Column.Table_ID, id, false, null);
		}
	}

	private void createViewDefinition(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_View_Definition.Table_Name, "(EXISTS(SELECT 1 FROM AD_View v WHERE v.AD_View_ID = AD_View_Definition.AD_View_ID AND v.EntityType = ?))", null)
				.setParameters(entityType)
				.getIDsAsList();
		for (int id : referenceIds) {
			packOut.createGenericPO(document, I_AD_View_Definition.Table_ID, id, false, null);
		}
	}

	private void createViewColumns(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_View_Column.Table_Name, "(EntityType = ? OR EXISTS(SELECT 1 FROM AD_Browse_Field f WHERE f.AD_View_Column_ID = AD_View_Column.AD_View_Column_ID AND f.EntityType = ?))", null)
				.setParameters(entityType, entityType)
				.getIDsAsList();
		for (int id : referenceIds) {
			packOut.createGenericPO(document, I_AD_View_Column.Table_ID, id, false, null);
		}
	}

	private void createReferenceListAndTable(PackOut packOut, TransformerHandler document, String entityType) throws SAXException {
		List<Integer> referenceIds = new Query(Env.getCtx(), I_AD_Reference.Table_Name, "EntityType = ?", null)
			.setParameters(entityType)
			.getIDsAsList();
		for (int id : referenceIds) {
			X_AD_Reference reference = new X_AD_Reference(Env.getCtx(), id, null);
			packOut.createGenericPO(document, reference, false, null);

		}

		List<X_AD_Ref_List> referenceListAsList = new Query(Env.getCtx(), I_AD_Ref_List.Table_Name, "EntityType = ?", null)
			.setParameters(entityType)
			.setOnlyActiveRecords(true)
			.list();
		for(X_AD_Ref_List referenceList : referenceListAsList) {
			packOut.createGenericPO(document, referenceList, false, null);
		}

		List<X_AD_Ref_Table> referenceTableAsList = new Query(Env.getCtx(), I_AD_Ref_Table.Table_Name, "EntityType = ?", null)
			.setParameters(entityType)
			.setOnlyActiveRecords(true)
			.list();
		for(X_AD_Ref_Table referenceTable : referenceTableAsList) {
			packOut.createGenericPO(document, referenceTable, false, null);
		}
	}
}
