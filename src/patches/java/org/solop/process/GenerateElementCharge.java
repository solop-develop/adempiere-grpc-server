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

import org.adempiere.core.domains.models.X_C_AcctSchema_Element;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MCharge;
import org.compiere.model.MClientInfo;
import org.compiere.model.MElementValue;
import org.compiere.util.DB;
import org.compiere.util.Env;

import java.util.ArrayList;
import java.util.List;

/** Generated Process for (Generate Element Charge)
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 *
 *  Generates a Charge from an Account Element (C_ElementValue). It mirrors the
 *  logic of the {@code VCharge}/{@code WCharge} form when creating charges from
 *  existing accounts, but here ONLY the Charge is created (no new account is
 *  created). From the parameters only Charge Type and Tax Category are used; the
 *  remaining ones (Value, Name, IsExpense) belonged to the form's "create new
 *  account" path and are not needed here.
 */
public class GenerateElementCharge extends GenerateElementChargeAbstract
{
	/** Primary Accounting Schema */
	private MAcctSchema acctSchema = null;
	/** C_Element_ID of the natural account element of the primary schema */
	private int accountElementId = 0;

	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (!isSelection() && getRecord_ID() <= 0) {
			throw new AdempiereException("@C_ElementValue_ID@ @NotFound@");
		}

		//	Resolve primary accounting schema and its natural account element
		MClientInfo clientInfo = MClientInfo.get(getCtx());
		acctSchema = clientInfo != null ? clientInfo.getMAcctSchema1() : null;
		if (acctSchema == null || acctSchema.getC_AcctSchema_ID() == 0) {
			throw new AdempiereException("@C_AcctSchema_ID@ @NotFound@");
		}
		MAcctSchemaElement accountElement =
			acctSchema.getAcctSchemaElement(X_C_AcctSchema_Element.ELEMENTTYPE_Account);
		if (accountElement == null) {
			throw new AdempiereException("@C_Element_ID@ @NotFound@");
		}
		accountElementId = accountElement.getC_Element_ID();

		//	Gather selected element values
		List<Integer> selectionKeys = new ArrayList<>();
		if (isSelection()) {
			selectionKeys = getSelectionKeys();
		} else {
			selectionKeys.add(getRecord_ID());
		}
		if (selectionKeys == null || selectionKeys.isEmpty()){
			throw new AdempiereException("@C_ElementValue_ID@ @NotFound@");
		}

		int created = 0;
		StringBuilder createdNames = new StringBuilder();
		for (Integer selectionKey : selectionKeys) {
			MElementValue elementValue = new MElementValue(getCtx(), selectionKey, get_TrxName());
			if (elementValue.get_ID() <= 0) {
				continue;
			}
			//	Only natural accounts belonging to the account element of the schema
			if (elementValue.getC_Element_ID() != accountElementId) {
				addLog("@Skipped@: " + elementValue.getValue() + " - @C_Element_ID@");
				continue;
			}

			MCharge charge = createCharge(elementValue);
			created++;
			if (createdNames.length() > 0) {
				createdNames.append(", ");
			}
			createdNames.append(charge.getName());
			addLog(charge.getC_Charge_ID(), null, null, charge.getName());
		}

		return "@Created@ #" + created
			+ (createdNames.length() > 0 ? " (" + createdNames + ")" : "");
	}

	/**
	 *	Create the Charge and link its expense/revenue accounts to the
	 *	valid combination of the given account element value.
	 *	@param elementValue natural account
	 *	@return the created charge
	 */
	private MCharge createCharge(MElementValue elementValue)
	{
		//	Charge
		MCharge charge = new MCharge(getCtx(), 0, get_TrxName());
		charge.setName(elementValue.getName());
		if (getTaxCategoryId() > 0) {
			charge.setC_TaxCategory_ID(getTaxCategoryId());
		}
		if (getChargeTypeId() > 0) {
			charge.setC_ChargeType_ID(getChargeTypeId());
		}
		charge.saveEx();

		//	Target account (valid combination) for the element value
		MAccount account = getAccount(elementValue.getC_ElementValue_ID());
		if (account == null) {
			throw new AdempiereException("@C_ValidCombination_ID@ @NotFound@ - " + elementValue.getName());
		}

		updateChargeAccount(charge, account);
		return charge;
	}

	/**
	 *	Get the account valid combination for the given element value, based on
	 *	the default account of the primary schema.
	 *	@param elementValueId natural account id
	 *	@return the account
	 */
	private MAccount getAccount(int elementValueId)
	{
		MAccount defaultAccount = MAccount.getDefault(acctSchema, true); //  optional null
		return MAccount.get(getCtx(),
			getAD_Client_ID(),
			Env.getAD_Org_ID(getCtx()),
			acctSchema.getC_AcctSchema_ID(),
			elementValueId,
			defaultAccount.getC_SubAcct_ID(),
			defaultAccount.getM_Product_ID(),
			defaultAccount.getC_BPartner_ID(),
			defaultAccount.getAD_OrgTrx_ID(),
			defaultAccount.getC_LocFrom_ID(),
			defaultAccount.getC_LocTo_ID(),
			defaultAccount.getC_SalesRegion_ID(),
			defaultAccount.getC_Project_ID(),
			defaultAccount.getC_Campaign_ID(),
			defaultAccount.getC_Activity_ID(),
			defaultAccount.getUser1_ID(),
			defaultAccount.getUser2_ID(),
			defaultAccount.getUser3_ID(),
			defaultAccount.getUser4_ID(),
			defaultAccount.getUserElement1_ID(),
			defaultAccount.getUserElement2_ID(),
			defaultAccount.getS_Contract_ID(),
			get_TrxName());
	}

	/**
	 *	Update the charge accounting (expense/revenue) for the primary schema
	 *	to point at the element value valid combination.
	 *	@param charge  charge
	 *	@param account valid combination account
	 */
	private void updateChargeAccount(MCharge charge, MAccount account)
	{
		StringBuilder sql = new StringBuilder("UPDATE C_Charge_Acct ")
			.append("SET CH_Expense_Acct=").append(account.getC_ValidCombination_ID())
			.append(", CH_Revenue_Acct=").append(account.getC_ValidCombination_ID())
			.append(" WHERE C_Charge_ID=").append(charge.getC_Charge_ID())
			.append(" AND C_AcctSchema_ID=").append(acctSchema.getC_AcctSchema_ID());
		int no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (no != 1) {
			throw new AdempiereException("Update C_Charge_Acct #" + no + " - " + charge.getName());
		}
	}
}
