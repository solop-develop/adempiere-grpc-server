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

/** Generated Process for (Generate Element Charge)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class GenerateElementChargeAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "GenerateElementCharge";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Generate Element Charge";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54965;
	/**	Parameter Name for Search Key	*/
	public static final String VALUE = "Value";
	/**	Parameter Name for Name	*/
	public static final String NAME = "Name";
	/**	Parameter Name for Is Expense	*/
	public static final String ISEXPENSE = "IsExpense";
	/**	Parameter Name for Charge Type	*/
	public static final String C_CHARGETYPE_ID = "C_ChargeType_ID";
	/**	Parameter Name for Tax Category	*/
	public static final String C_TAXCATEGORY_ID = "C_TaxCategory_ID";
	/**	Parameter Value for Search Key	*/
	private String value;
	/**	Parameter Value for Name	*/
	private String name;
	/**	Parameter Value for Is Expense	*/
	private boolean isExpense;
	/**	Parameter Value for Charge Type	*/
	private int chargeTypeId;
	/**	Parameter Value for Tax Category	*/
	private int taxCategoryId;

	@Override
	protected void prepare() {
		value = getParameterAsString(VALUE);
		name = getParameterAsString(NAME);
		isExpense = getParameterAsBoolean(ISEXPENSE);
		chargeTypeId = getParameterAsInt(C_CHARGETYPE_ID);
		taxCategoryId = getParameterAsInt(C_TAXCATEGORY_ID);
	}

	/**	 Getter Parameter Value for Search Key	*/
	protected String getValue() {
		return value;
	}

	/**	 Setter Parameter Value for Search Key	*/
	protected void setValue(String value) {
		this.value = value;
	}

	/**	 Getter Parameter Value for Name	*/
	protected String getName() {
		return name;
	}

	/**	 Setter Parameter Value for Name	*/
	protected void setName(String name) {
		this.name = name;
	}

	/**	 Getter Parameter Value for Is Expense	*/
	protected boolean isExpense() {
		return isExpense;
	}

	/**	 Setter Parameter Value for Is Expense	*/
	protected void setIsExpense(boolean isExpense) {
		this.isExpense = isExpense;
	}

	/**	 Getter Parameter Value for Charge Type	*/
	protected int getChargeTypeId() {
		return chargeTypeId;
	}

	/**	 Setter Parameter Value for Charge Type	*/
	protected void setChargeTypeId(int chargeTypeId) {
		this.chargeTypeId = chargeTypeId;
	}

	/**	 Getter Parameter Value for Tax Category	*/
	protected int getTaxCategoryId() {
		return taxCategoryId;
	}

	/**	 Setter Parameter Value for Tax Category	*/
	protected void setTaxCategoryId(int taxCategoryId) {
		this.taxCategoryId = taxCategoryId;
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