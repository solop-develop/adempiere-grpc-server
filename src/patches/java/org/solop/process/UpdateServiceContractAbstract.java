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

/** Generated Process for (Actualización de Contratos)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class UpdateServiceContractAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "UY_P_UpdateServiceContract";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Actualización de Contratos";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 2000099;
	/**	Parameter Name for Action	*/
	public static final String ACTION = "Action";
	/**	Parameter Name for Document Date	*/
	public static final String DATEDOC = "DateDoc";
	/**	Parameter Name for Account Date	*/
	public static final String DATEACCT = "DateAcct";
	/**	Parameter Name for Date Start	*/
	public static final String DATESTART = "DateStart";
	/**	Parameter Name for Finish Schedule	*/
	public static final String DATEFINISHSCHEDULE = "DateFinishSchedule";
	/**	Parameter Name for Service Type	*/
	public static final String S_SERVICETYPE_ID = "S_ServiceType_ID";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Name for S_ContractReason	*/
	public static final String S_CONTRACTREASON_ID = "S_ContractReason_ID";
	/**	Parameter Value for Action	*/
	private String action;
	/**	Parameter Value for Document Date	*/
	private Timestamp dateDoc;
	/**	Parameter Value for Account Date	*/
	private Timestamp dateAcct;
	/**	Parameter Value for Date Start	*/
	private Timestamp dateStart;
	/**	Parameter Value for Finish Schedule	*/
	private Timestamp dateFinishSchedule;
	/**	Parameter Value for Service Type	*/
	private int serviceTypeId;
	/**	Parameter Value for Document Action	*/
	private String docAction;
	/**	Parameter Value for S_ContractReason	*/
	private int contractReasonId;

	@Override
	protected void prepare() {
		action = getParameterAsString(ACTION);
		dateDoc = getParameterAsTimestamp(DATEDOC);
		dateAcct = getParameterAsTimestamp(DATEACCT);
		dateStart = getParameterAsTimestamp(DATESTART);
		dateFinishSchedule = getParameterAsTimestamp(DATEFINISHSCHEDULE);
		serviceTypeId = getParameterAsInt(S_SERVICETYPE_ID);
		docAction = getParameterAsString(DOCACTION);
		contractReasonId = getParameterAsInt(S_CONTRACTREASON_ID);
	}

	/**	 Getter Parameter Value for Action	*/
	protected String getAction() {
		return action;
	}

	/**	 Setter Parameter Value for Action	*/
	protected void setAction(String action) {
		this.action = action;
	}

	/**	 Getter Parameter Value for Document Date	*/
	protected Timestamp getDateDoc() {
		return dateDoc;
	}

	/**	 Setter Parameter Value for Document Date	*/
	protected void setDateDoc(Timestamp dateDoc) {
		this.dateDoc = dateDoc;
	}

	/**	 Getter Parameter Value for Account Date	*/
	protected Timestamp getDateAcct() {
		return dateAcct;
	}

	/**	 Setter Parameter Value for Account Date	*/
	protected void setDateAcct(Timestamp dateAcct) {
		this.dateAcct = dateAcct;
	}

	/**	 Getter Parameter Value for Date Start	*/
	protected Timestamp getDateStart() {
		return dateStart;
	}

	/**	 Setter Parameter Value for Date Start	*/
	protected void setDateStart(Timestamp dateStart) {
		this.dateStart = dateStart;
	}

	/**	 Getter Parameter Value for Finish Schedule	*/
	protected Timestamp getDateFinishSchedule() {
		return dateFinishSchedule;
	}

	/**	 Setter Parameter Value for Finish Schedule	*/
	protected void setDateFinishSchedule(Timestamp dateFinishSchedule) {
		this.dateFinishSchedule = dateFinishSchedule;
	}

	/**	 Getter Parameter Value for Service Type	*/
	protected int getServiceTypeId() {
		return serviceTypeId;
	}

	/**	 Setter Parameter Value for Service Type	*/
	protected void setServiceTypeId(int serviceTypeId) {
		this.serviceTypeId = serviceTypeId;
	}

	/**	 Getter Parameter Value for Document Action	*/
	protected String getDocAction() {
		return docAction;
	}

	/**	 Setter Parameter Value for Document Action	*/
	protected void setDocAction(String docAction) {
		this.docAction = docAction;
	}

	/**	 Getter Parameter Value for S_ContractReason	*/
	protected int getContractReasonId() {
		return contractReasonId;
	}

	/**	 Setter Parameter Value for S_ContractReason	*/
	protected void setContractReasonId(int contractReasonId) {
		this.contractReasonId = contractReasonId;
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