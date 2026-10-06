/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.eevolution.context.service.infrastructure.domain.entities;

import org.adempiere.core.domains.models.X_S_ContractLine;
import org.compiere.model.MCurrency;
import org.compiere.model.MProduct;
import org.compiere.model.MUOMConversion;
import org.compiere.util.DB;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.Properties;

/**
 * Contract Line Entity
 */
public class MSContractLine extends X_S_ContractLine {

	public MSContractLine(Properties ctx, int S_ContractLine_ID, String trxName) {
		super(ctx, S_ContractLine_ID, trxName);
	}

	public MSContractLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/** Parent */
	private MSContract parent = null;

	/**
	 * Get Parent
	 * @return parent contract
	 */
	public MSContract getParent() {
		if (parent == null) {
			parent = new MSContract(getCtx(), getS_Contract_ID(), get_TrxName());
		}
		return parent;
	}

	/**
	 * Get Currency Precision from Currency
	 * @return precision
	 */
	public int getPrecision() {
		if (getC_Currency_ID() > 0) {
			return MCurrency.getStdPrecision(getCtx(), getC_Currency_ID());
		}
		return getParent().getPrecision();
	}

	/**
	 * Calculate Extended Amt.
	 * May or may not include tax
	 */
	public void setLineNetAmt() {
		BigDecimal priceEntered = Optional.ofNullable(getPriceEntered()).orElse(Env.ZERO);
		BigDecimal qtyEntered = Optional.ofNullable(getQtyEntered()).orElse(Env.ZERO);
		BigDecimal lineNetAmount = null;
		if (getM_Product_ID() > 0) {
			MProduct product = MProduct.get(getCtx(), getM_Product_ID(), get_TrxName());
			if (product.getC_UOM_ID() != getC_UOM_ID()
					&& priceEntered.signum() != 0
					&& qtyEntered.signum() != 0) {
				lineNetAmount = qtyEntered.multiply(priceEntered);
			}
		}
		//	Set default
		if (lineNetAmount == null) {
			lineNetAmount = Optional.ofNullable(getPriceActual())
				.orElse(Env.ZERO)
				.multiply(Optional.ofNullable(getQtyOrdered())
				.orElse(Env.ZERO))
			;
		}
		if (lineNetAmount.scale() > getPrecision()) {
			lineNetAmount = lineNetAmount.setScale(getPrecision(), RoundingMode.HALF_UP);
		}
		super.setLineNetAmt(lineNetAmount);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getParent().isProcessed()) {
			return true;
		}
		//	Currency from Contract
		if (getC_Currency_ID() <= 0) {
			setC_Currency_ID(getParent().getC_Currency_ID());
		}
		//	Quantity and Price from entered values
		setQtyOrderedAndPriceActual(newRecord);
		//	Line Net Amount
		setLineNetAmt();
		return true;
	}

	/**
	 * Sync Ordered Quantity and Actual Price from the entered values (UOM conversion),
	 * so the line does not depend on the callouts to be consistent
	 * @param newRecord new record
	 */
	private void setQtyOrderedAndPriceActual(boolean newRecord) {
		BigDecimal qtyEntered = Optional.ofNullable(getQtyEntered()).orElse(Env.ZERO);
		if (newRecord
				|| is_ValueChanged(COLUMNNAME_QtyEntered)
				|| is_ValueChanged(COLUMNNAME_C_UOM_ID)) {
			BigDecimal qtyOrdered = Optional.ofNullable(
				MUOMConversion.convertProductFrom(getCtx(), getM_Product_ID(), getC_UOM_ID(), qtyEntered)
			).orElse(qtyEntered);
			setQtyOrdered(qtyOrdered);
		}
		BigDecimal priceEntered = Optional.ofNullable(getPriceEntered()).orElse(Env.ZERO);
		BigDecimal priceActual = Optional.ofNullable(getPriceActual()).orElse(Env.ZERO);
		boolean isPriceEnteredChanged = (newRecord
				|| is_ValueChanged(COLUMNNAME_PriceEntered)
				|| is_ValueChanged(COLUMNNAME_C_UOM_ID))
			&& !is_ValueChanged(COLUMNNAME_PriceActual)
		;
		boolean isPriceActualMissing = priceActual.signum() == 0 && priceEntered.signum() != 0;
		if (isPriceEnteredChanged || isPriceActualMissing) {
			priceActual = Optional.ofNullable(
				MUOMConversion.convertProductTo(getCtx(), getM_Product_ID(), getC_UOM_ID(), priceEntered)
			).orElse(priceEntered);
			setPriceActual(priceActual);
		}
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success) {
			return success;
		}
		if (newRecord
				|| (is_ValueChanged(COLUMNNAME_C_Tax_ID) && !getParent().isProcessed())
				|| (is_ValueChanged(COLUMNNAME_LineNetAmt) && !getParent().isProcessed())
				|| (is_ValueChanged(COLUMNNAME_QtyEntered) && !getParent().isProcessed())
				|| (is_ValueChanged(COLUMNNAME_PriceActual) && !getParent().isProcessed())
				|| (is_ValueChanged(COLUMNNAME_IsActive) && !getParent().isProcessed())
		) {
			return updateHeaderTax();
		}
		return true;
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (!success) {
			return success;
		}
		return updateHeaderTax();
	}

	/**
	 * Update Tax & Header
	 * @return true if header updated
	 */
	private boolean updateHeaderTax() {
		//	Recalculate Tax for this Tax
		if (!getParent().isProcessed()) {
			getParent().calculateTaxTotal();
		}

		//	Update Contract Header
		String sql = "UPDATE S_Contract c"
			+ " SET TotalLines="
				+ "(SELECT COALESCE(SUM(LineNetAmt),0) FROM S_ContractLine cl WHERE c.S_Contract_ID=cl.S_Contract_ID) "
			+ "WHERE S_Contract_ID=? "
		;
		int no = DB.executeUpdateEx(sql, new Object[]{getS_Contract_ID()}, get_TrxName());
		if (no != 1) {
			log.warning("(1) #" + no);
		}

		if (getParent().isTaxIncluded()) {
			sql = "UPDATE S_Contract c "
				+ " SET GrandTotal=TotalLines "
				+ "WHERE S_Contract_ID=? "
			;
		}
		else {
			sql = "UPDATE S_Contract c "
				+ " SET GrandTotal=TotalLines+ "
					+ "(SELECT COALESCE(SUM(TaxAmt),0) FROM S_ContractTax ct WHERE c.S_Contract_ID=ct.S_Contract_ID) "
				+ "WHERE S_Contract_ID=? "
			;
		}
		no = DB.executeUpdateEx(sql, new Object[]{getS_Contract_ID()}, get_TrxName());
		if (no != 1) {
			log.warning("(2) #" + no);
		}
		parent = null;
		return no == 1;
	}

}	//	MSContractLine
