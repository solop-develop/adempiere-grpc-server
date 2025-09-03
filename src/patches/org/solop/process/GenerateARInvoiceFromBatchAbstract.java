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

package org.solop.process;

import org.compiere.process.SvrProcess;

import java.sql.Timestamp;

/** Generated Process for (Generate AP Invoice)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class GenerateARInvoiceFromBatchAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "PPBGenerateAPInvoice";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Generate AP Invoice";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54886;
	/**	Parameter Name for Target Document Type	*/
	public static final String C_DOCTYPETARGET_ID = "C_DocTypeTarget_ID";
	/**	Parameter Name for Document Date	*/
	public static final String DATEDOC = "DateDoc";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Value for Target Document Type	*/
	private int docTypeTargetId;
	/**	Parameter Value for Document Date	*/
	private Timestamp dateDoc;
	/**	Parameter Value for Document Action	*/
	private String docAction;

	@Override
	protected void prepare() {
		docTypeTargetId = getParameterAsInt(C_DOCTYPETARGET_ID);
		dateDoc = getParameterAsTimestamp(DATEDOC);
		docAction = getParameterAsString(DOCACTION);
	}

	/**	 Getter Parameter Value for Target Document Type	*/
	protected int getDocTypeTargetId() {
		return docTypeTargetId;
	}

	/**	 Setter Parameter Value for Target Document Type	*/
	protected void setDocTypeTargetId(int docTypeTargetId) {
		this.docTypeTargetId = docTypeTargetId;
	}

	/**	 Getter Parameter Value for Document Date	*/
	protected Timestamp getDateDoc() {
		return dateDoc;
	}

	/**	 Setter Parameter Value for Document Date	*/
	protected void setDateDoc(Timestamp dateDoc) {
		this.dateDoc = dateDoc;
	}

	/**	 Getter Parameter Value for Document Action	*/
	protected String getDocAction() {
		return docAction;
	}

	/**	 Setter Parameter Value for Document Action	*/
	protected void setDocAction(String docAction) {
		this.docAction = docAction;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}