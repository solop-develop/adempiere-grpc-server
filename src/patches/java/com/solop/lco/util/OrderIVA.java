/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — ReteIVA simulada desde Orden de Compra              *
 * Adaptado de org.erpya.lve.util.OrderIVA (Localización Venezuela)                  *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_Order;
import org.spin.model.I_WH_Withholding;
import com.solop.lco.model.X_LCO_WH_IVARate;
import com.solop.lco.model.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderTax;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * ReteIVA estimada (IsSimulation=Y) sobre una Orden de Compra completada, para
 * prever cuánto retener al prepagar al proveedor. El documento definitivo se genera
 * desde la factura ({@link APInvoiceIVA}); las simulaciones son ignoradas por
 * WithholdingGenerate.
 *
 * @author Solop, www.solopsoftware.com
 */
public class OrderIVA extends AbstractWithholdingSetting implements LCOUtil {

	public OrderIVA(MWHSetting setting) {
		super(setting);
	}

	private MOrder order;
	private MBPartner businessPartner;
	private List<MOrderTax> taxes;
	private X_LCO_WH_IVARate ivaRate;
	private BigDecimal tributeUnitAmount = Env.ZERO;

	@Override
	public boolean isValid() {
		if (getDocument().get_Table_ID() != I_C_Order.Table_ID) {
			addLog("No es una orden (C_Order)");
			return false;
		}
		order = (MOrder) getDocument();
		businessPartner = (MBPartner) order.getC_BPartner();
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceOrder_ID, order.getC_Order_ID());
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());

		if (order.isSOTrx()) {
			addLog("ReteIVA no aplica en órdenes de venta");
			return false;
		}
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), order.getAD_Org_ID(), getTransactionName());
		if (setup == null) {
			addLog("No existe Configuración de Retenciones (LCO_WithholdingSetup)");
			return false;
		}
		if (setup.isClientExcluded() || setup.isExcludeIVAWithholding()) {
			addLog("ReteIVA excluida por configuración (LCO_WithholdingSetup)");
			return false;
		}
		if (businessPartner.get_ValueAsBoolean(COLUMNNAME_LCO_IsIVAExempt)) {
			addLog("El tercero está exento de ReteIVA");
			return false;
		}
		if (order.get_ValueAsBoolean(COLUMNNAME_LCO_IsWithholdingTaxExempt)) {
			addLog("La orden está marcada como exenta de ReteIVA");
			return false;
		}
		String regime = businessPartner.get_ValueAsString(COLUMNNAME_LCO_TaxRegime);
		if (!TAXREGIME_VATResponsible.equals(regime) && !TAXREGIME_Foreign.equals(regime)) {
			addLog("El tercero no es responsable de IVA: no aplica ReteIVA");
			return false;
		}
		int ivaRateId = order.get_ValueAsInt(COLUMNNAME_LCO_WH_IVARate_ID);
		if (ivaRateId == 0)
			ivaRateId = businessPartner.get_ValueAsInt(COLUMNNAME_LCO_WH_IVARate_ID);
		if (ivaRateId <= 0) {
			addLog("No se encontró Tarifa de ReteIVA (LCO_WH_IVARate)");
			return false;
		}
		ivaRate = new X_LCO_WH_IVARate(getContext(), ivaRateId, getTransactionName());
		if (ivaRate.getWithholdingRate() == null || ivaRate.getWithholdingRate().signum() == 0) {
			addLog("La Tarifa de ReteIVA es cero");
			return false;
		}
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), order.getDateOrdered(), getTransactionName());
		taxes = Arrays.asList(order.getTaxes(false))
				.stream()
				.filter(ot -> ot.getTaxAmt() != null && ot.getTaxAmt().signum() != 0)
				.collect(Collectors.toList());
		if (taxes.isEmpty()) {
			addLog("La orden no tiene impuestos sujetos a ReteIVA");
			return false;
		}
		if (isGenerated())
			return false;
		return true;
	}

	@Override
	public String run() {
		BigDecimal rate = ivaRate.getWithholdingRate();
		taxes.forEach(orderTax -> {
			setWithholdingRate(rate);
			addBaseAmount(orderTax.getTaxAmt());
			addWithholdingAmount(orderTax.getTaxAmt().multiply(getWithholdingRate(true)));
			addDescription(ivaRate.getName() + " (simulación)");
			setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());
			setReturnValue(MWHWithholding.COLUMNNAME_C_Tax_ID, orderTax.getC_Tax_ID());
			setReturnValue(COLUMNNAME_LCO_WH_IVARate_ID, ivaRate.getLCO_WH_IVARate_ID());
			setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
			setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, true);
			saveResult();
		});
		return null;
	}

	private boolean isGenerated() {
		if (order == null)
			return false;
		return new Query(getContext(), MWHWithholding.Table_Name,
					"SourceOrder_ID = ? AND WH_Definition_ID = ? AND WH_Setting_ID = ? "
					+ "AND Processed = 'Y' AND IsSimulation = 'Y' AND DocStatus IN (?,?)", getTransactionName())
				.setParameters(order.get_ID(), getDefinition().get_ID(), getSetting().get_ID(),
						MWHWithholding.DOCSTATUS_Completed, MWHWithholding.DOCSTATUS_Closed)
				.match();
	}
}
