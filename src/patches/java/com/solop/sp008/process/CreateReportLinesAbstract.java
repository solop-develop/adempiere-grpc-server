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

import org.compiere.process.SvrProcess;

/** Generated Process for (Create Report Lines)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class CreateReportLinesAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SP008_Create_PA_ReportLines";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Create Report Lines";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54755;
	/**	Parameter Name for Report Type	*/
	public static final String SP008_REPORTTYPE = "SP008_ReportType";
	/**	Parameter Name for Element	*/
	public static final String C_ELEMENT_ID = "C_Element_ID";
	/**	Parameter Name for Recreate Lines	*/
	public static final String SP008_RECREATELINES = "SP008_RecreateLines";
	/**	Parameter Value for Report Type	*/
	private String reportType;
	/**	Parameter Value for Element	*/
	private int elementId;
	/**	Parameter Value for Recreate Lines	*/
	private boolean isRecreateLines;

	@Override
	protected void prepare() {
		reportType = getParameterAsString(SP008_REPORTTYPE);
		elementId = getParameterAsInt(C_ELEMENT_ID);
		isRecreateLines = getParameterAsBoolean(SP008_RECREATELINES);
	}

	/**	 Getter Parameter Value for Report Type	*/
	protected String getReportType() {
		return reportType;
	}

	/**	 Setter Parameter Value for Report Type	*/
	protected void setReportType(String reportType) {
		this.reportType = reportType;
	}

	/**	 Getter Parameter Value for Element	*/
	protected int getElementId() {
		return elementId;
	}

	/**	 Setter Parameter Value for Element	*/
	protected void setElementId(int elementId) {
		this.elementId = elementId;
	}

	/**	 Getter Parameter Value for Recreate Lines	*/
	protected boolean isRecreateLines() {
		return isRecreateLines;
	}

	/**	 Setter Parameter Value for Recreate Lines	*/
	protected void setRecreateLines(boolean isRecreateLines) {
		this.isRecreateLines = isRecreateLines;
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