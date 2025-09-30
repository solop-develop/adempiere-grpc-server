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

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.process.SvrProcess;

/** Generated Process for (Generate AP Invoice)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class GenerateAPInvoiceFromBatchAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "PPBGenerateAPInvoice";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Generate AP Invoice";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54886;
	/**	Parameter Name for Document Date	*/
	public static final String DATEDOC = "DateDoc";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Name for Vendor Document Type	*/
	public static final String VENDORDOCUMENTTYPE = "VendorDocumentType";
	/**	Parameter Name for Overwrite Amount and Charge	*/
	public static final String OVERWRITEAMTANDCHARGE = "OverwriteAmtAndCharge";
	/**	Parameter Name for Amount	*/
	public static final String AMOUNT = "Amount";
	/**	Parameter Name for Charge	*/
	public static final String C_CHARGE_ID = "C_Charge_ID";
	/**	Parameter Value for Document Date	*/
	private Timestamp dateDoc;
	/**	Parameter Value for Document Action	*/
	private String docAction;
	/**	Parameter Value for Vendor Document Type	*/
	private String vendorDocumentType;
	/**	Parameter Value for Overwrite Amount and Charge	*/
	private boolean isOverwriteAmtAndCharge;
	/**	Parameter Value for Amount	*/
	private BigDecimal amount;
	/**	Parameter Value for Charge	*/
	private int chargeId;

	@Override
	protected void prepare() {
		dateDoc = getParameterAsTimestamp(DATEDOC);
		docAction = getParameterAsString(DOCACTION);
		vendorDocumentType = getParameterAsString(VENDORDOCUMENTTYPE);
		isOverwriteAmtAndCharge = getParameterAsBoolean(OVERWRITEAMTANDCHARGE);
		amount = getParameterAsBigDecimal(AMOUNT);
		chargeId = getParameterAsInt(C_CHARGE_ID);
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

	/**	 Getter Parameter Value for Vendor Document Type	*/
	protected String getVendorDocumentType() {
		return vendorDocumentType;
	}

	/**	 Setter Parameter Value for Vendor Document Type	*/
	protected void setVendorDocumentType(String vendorDocumentType) {
		this.vendorDocumentType = vendorDocumentType;
	}

	/**	 Getter Parameter Value for Overwrite Amount and Charge	*/
	protected boolean isOverwriteAmtAndCharge() {
		return isOverwriteAmtAndCharge;
	}

	/**	 Setter Parameter Value for Overwrite Amount and Charge	*/
	protected void setOverwriteAmtAndCharge(boolean isOverwriteAmtAndCharge) {
		this.isOverwriteAmtAndCharge = isOverwriteAmtAndCharge;
	}

	/**	 Getter Parameter Value for Amount	*/
	protected BigDecimal getAmount() {
		return amount;
	}

	/**	 Setter Parameter Value for Amount	*/
	protected void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**	 Getter Parameter Value for Charge	*/
	protected int getChargeId() {
		return chargeId;
	}

	/**	 Setter Parameter Value for Charge	*/
	protected void setChargeId(int chargeId) {
		this.chargeId = chargeId;
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