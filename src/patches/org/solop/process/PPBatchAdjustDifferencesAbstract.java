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

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Process for (Adjust Differences)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class PPBatchAdjustDifferencesAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "AdjustDifferences";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Adjust Differences";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54938;
	/**	Parameter Name for Payment	*/
	public static final String C_PAYMENT_ID = "C_Payment_ID";
	/**	Parameter Name for Correct Amount	*/
	public static final String CORRECTAMOUNT = "CorrectAmount";
	/**	Parameter Name for Charge	*/
	public static final String C_CHARGE_ID = "C_Charge_ID";
	/**	Parameter Name for Keep Original Date	*/
	public static final String KEEPORIGINALDATE = "KeepOriginalDate";
	/**	Parameter Name for Document Date	*/
	public static final String DATEDOC = "DateDoc";
	/**	Parameter Value for Payment	*/
	private int paymentId;
	/**	Parameter Value for Correct Amount	*/
	private BigDecimal correctAmount;
	/**	Parameter Value for Charge	*/
	private int chargeId;
	/**	Parameter Value for Keep Original Date	*/
	private boolean isKeepOriginalDate;
	/**	Parameter Value for Document Date	*/
	private Timestamp dateDoc;

	@Override
	protected void prepare() {
		paymentId = getParameterAsInt(C_PAYMENT_ID);
		correctAmount = getParameterAsBigDecimal(CORRECTAMOUNT);
		chargeId = getParameterAsInt(C_CHARGE_ID);
		isKeepOriginalDate = getParameterAsBoolean(KEEPORIGINALDATE);
		dateDoc = getParameterAsTimestamp(DATEDOC);
	}

	/**	 Getter Parameter Value for Payment	*/
	protected int getPaymentId() {
		return paymentId;
	}

	/**	 Setter Parameter Value for Payment	*/
	protected void setPaymentId(int paymentId) {
		this.paymentId = paymentId;
	}

	/**	 Getter Parameter Value for Correct Amount	*/
	protected BigDecimal getCorrectAmount() {
		return correctAmount;
	}

	/**	 Setter Parameter Value for Correct Amount	*/
	protected void setCorrectAmount(BigDecimal correctAmount) {
		this.correctAmount = correctAmount;
	}

	/**	 Getter Parameter Value for Charge	*/
	protected int getChargeId() {
		return chargeId;
	}

	/**	 Setter Parameter Value for Charge	*/
	protected void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}

	/**	 Getter Parameter Value for Keep Original Date	*/
	protected boolean isKeepOriginalDate() {
		return isKeepOriginalDate;
	}

	/**	 Setter Parameter Value for Keep Original Date	*/
	protected void setKeepOriginalDate(boolean isKeepOriginalDate) {
		this.isKeepOriginalDate = isKeepOriginalDate;
	}

	/**	 Getter Parameter Value for Document Date	*/
	protected Timestamp getDateDoc() {
		return dateDoc;
	}

	/**	 Setter Parameter Value for Document Date	*/
	protected void setDateDoc(Timestamp dateDoc) {
		this.dateDoc = dateDoc;
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