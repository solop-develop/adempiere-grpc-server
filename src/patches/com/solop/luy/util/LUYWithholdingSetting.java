/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2014 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.luy.util;

import com.solop.luy.model.I_SPLUY_WithholdingTax;
import com.solop.luy.model.I_SPLUY_WithholdingVendor;
import org.adempiere.core.domains.models.I_C_DocType;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.spin.model.MWHDefinition;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Withholding Management Generic class
 *
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class LUYWithholdingSetting extends AbstractWithholdingSetting {

	private MInvoice mInvoice;
	private MBPartner mbPartner;
	private List<MWHWithholding> mWithholdings;


	public LUYWithholdingSetting(MWHSetting setting) {
		super(setting);
	}

	@Override
	public String run() {
		if (mWithholdings != null) {
			mWithholdings.forEach(mwhWithholding -> {
				mwhWithholding.saveEx();
				mwhWithholding.processIt(DocAction.ACTION_Complete);
				mwhWithholding.saveEx();
			});
		}
		return null;
	}

	@Override
	public boolean isValid() {
		AtomicBoolean isValid = new AtomicBoolean(true);

		if(getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("@C_Invoice_ID@ @NotFound@");
			isValid.set(false);
		}
		mInvoice = (MInvoice) getDocument();
		mbPartner = (MBPartner) mInvoice.getC_BPartner();
		mWithholdings = new ArrayList<>();

		MWHDefinition mwhDefinition = getDefinition();
		boolean hasVendor =  new Query(mInvoice.getCtx(), I_SPLUY_WithholdingVendor.Table_Name, I_SPLUY_WithholdingVendor.COLUMNNAME_WH_Definition_ID + "=? AND " + I_SPLUY_WithholdingVendor.COLUMNNAME_C_BPartner_ID + "=?", mInvoice.get_TrxName())
			.setParameters(mwhDefinition.get_ID(), mbPartner.get_ID())
			.match();

		if (!hasVendor) {
			isValid.set(false);
		} else {
			Arrays.asList(mInvoice.getLines()).forEach(mInvoiceLine -> {

				String whAmtType = mwhDefinition.get_ValueAsString(LUYChanges.COLUMNNAME_SPLUY_WithholdingAmtBase);

				if (isGenerated(mInvoiceLine)) {
					isValid.set(false);
				} else if (LUYChanges.WithholdingAmtBase_Tax_Amount.equalsIgnoreCase(whAmtType) && !(
					new Query(getContext(), I_SPLUY_WithholdingTax.Table_Name,
							I_SPLUY_WithholdingTax.COLUMNNAME_WH_Definition_ID + "=? AND " + I_SPLUY_WithholdingTax.COLUMNNAME_C_Tax_ID + "=?", getTransactionName()
					)

						.setParameters(mwhDefinition.get_ID(), mInvoiceLine.getC_Tax_ID())
						.match())) {
					isValid.set(false);
				} else {
					BigDecimal baseAmt = Env.ZERO;
					if (LUYChanges.WithholdingAmtBase_Tax_Amount.equalsIgnoreCase(whAmtType)) {
						baseAmt = mInvoiceLine.getTaxAmt();
					} else if (LUYChanges.WithholdingAmtBase_Total_Amount.equalsIgnoreCase(whAmtType)) {
						baseAmt = mInvoiceLine.getLineTotalAmt();
					} else if (LUYChanges.WithholdingAmtBase_Net_Amount.equalsIgnoreCase(whAmtType)) {
						baseAmt = mInvoiceLine.getLineNetAmt();
					}

					mInvoiceLine.getLineTotalAmt();
					BigDecimal rate = (BigDecimal) mwhDefinition.get_Value(LUYChanges.COLUMNNAME_SPLUY_WithholdingPercentage);

					if (rate == null) {
						throw new AdempiereException("@SPLUY_WithholdingPercentage@ @not.found@");
					}

					if (MDocType.DOCBASETYPE_APCreditMemo.equalsIgnoreCase(mInvoice.getC_DocType().getDocBaseType())) {
						baseAmt = baseAmt.negate();
					}

					BigDecimal withholdingAmount = baseAmt.multiply(rate).divide(Env.ONEHUNDRED, 4, RoundingMode.HALF_UP);


					MWHWithholding mwhWithholding = new MWHWithholding(getContext(), 0, getTransactionName());

					MDocType wdDocType = new Query(mInvoice.getCtx(), I_C_DocType.Table_Name, I_C_DocType.COLUMNNAME_DocBaseType + "=? AND " + I_C_DocType.COLUMNNAME_IsSOTrx + "=?", mInvoice.get_TrxName())
						.setParameters("WHH", mInvoice.isSOTrx() ? "Y" : "N")
						.first();

					if (wdDocType == null || wdDocType.get_ID() <= 0) {
						throw new AdempiereException("@C_DocType_ID@ (@DocBaseType@=WHH) @not.found@");
					}
					mwhWithholding.setAD_Org_ID(mInvoice.getAD_Org_ID());
					mwhWithholding.setC_DocType_ID(wdDocType.get_ID());
					mwhWithholding.setDateDoc(mInvoice.getDateAcct());
					mwhWithholding.setWH_Definition_ID(mwhDefinition.get_ID());
					mwhWithholding.setWH_Setting_ID(getSetting().getWH_Setting_ID());
					mwhWithholding.setDescription(mInvoiceLine.getDescription());
					mwhWithholding.setA_Base_Amount(baseAmt);
					mwhWithholding.setWithholdingRate(rate);
					mwhWithholding.setWithholdingAmt(withholdingAmount);
					mwhWithholding.setC_BPartner_ID(mInvoice.getC_BPartner_ID());
					mwhWithholding.setC_BPartner_Location_ID(mInvoice.getC_BPartner_Location_ID());
					mwhWithholding.setSourceInvoice_ID(mInvoice.get_ID());
					mwhWithholding.setSourceInvoiceLine_ID(mInvoiceLine.get_ID());
					mwhWithholding.set_ValueOfColumn("C_Project_ID", mInvoice.getC_Project_ID());
					mwhWithholding.setWH_Definition_ID(mwhDefinition.get_ID());
					mwhWithholding.setIsSOTrx(mInvoice.isSOTrx());

					mWithholdings.add(mwhWithholding);
				}
			});
		}



		return isValid.get();
	}

	private boolean isGenerated(MInvoiceLine mInvoiceLine) {
		if (mInvoice !=null)
			return new Query(getContext(), MWHWithholding.Table_Name, "SourceInvoice_ID = ? "
				+ "AND SourceInvoiceLine_ID = ? "
				+ "AND WH_Definition_ID = ? "
				+ "AND docStatus IN ('CO','CL')"
				+ "AND IsSimulation='N'", getTransactionName())
				.setParameters(
					mInvoice.get_ID(),
					mInvoiceLine.get_ID(),
					getDefinition().get_ID())
				.match();
		return false;
	}
}
