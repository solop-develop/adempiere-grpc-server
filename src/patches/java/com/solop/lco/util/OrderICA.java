/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — ReteICA (Impuesto Municipal) simulada desde Orden   *
 * Adaptado de org.erpya.lve.util.OrderIM (Localización Venezuela)                   *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;

import org.adempiere.core.domains.models.I_C_Order;
import org.spin.model.I_WH_Withholding;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * ReteICA (Impuesto Municipal) estimada (IsSimulation=Y) sobre una Orden de Compra
 * completada, para prever cuánto retener al prepagar al proveedor. El documento
 * definitivo se genera desde la factura ({@link APInvoiceICA}); las simulaciones son
 * ignoradas por WithholdingGenerate. La lógica municipal (agente + territorialidad)
 * la centraliza {@link LCOMunicipalWithholding}.
 *
 * @author Solop, www.solopsoftware.com
 */
public class OrderICA extends AbstractWithholdingSetting implements LCOUtil {

	public OrderICA(MWHSetting setting) {
		super(setting);
	}

	private MOrder order;
	private MBPartner businessPartner;
	private LCOMunicipalWithholding calc;
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
			addLog("ReteICA no aplica en órdenes de venta");
			return false;
		}
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), order.getAD_Org_ID(), getTransactionName());
		if (setup == null) {
			addLog("No existe Configuración de Retenciones (LCO_WithholdingSetup)");
			return false;
		}
		if (setup.isClientExcluded() || setup.isExcludeICAWithholding()) {
			addLog("ReteICA excluida por configuración (LCO_WithholdingSetup)");
			return false;
		}
		//	La organización debe ser agente retenedor de Impuesto Municipal
		if (!setup.get_ValueAsBoolean(COLUMNNAME_IsMunicipalWithholdingAgent)) {
			addLog("La organización no es agente retenedor de Impuesto Municipal (ICA)");
			return false;
		}
		//	Exención del tercero
		if (businessPartner.get_ValueAsBoolean(COLUMNNAME_LCO_IsICAExempt)) {
			addLog("El tercero está exento de ReteICA");
			return false;
		}
		//	Concepto de ICA: cabecera → tercero (sin default ni por producto)
		int conceptId = order.get_ValueAsInt(COLUMNNAME_LCO_ICAConcept_ID);
		if (conceptId == 0)
			conceptId = businessPartner.get_ValueAsInt(COLUMNNAME_LCO_ICAConcept_ID);
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), order.getDateOrdered(), getTransactionName());
		int agentCityId = LCOMunicipalWithholding.getAgentCityId(getContext(), order.getAD_Org_ID(), getTransactionName());
		BigDecimal base = order.getTotalLines();
		calc = LCOMunicipalWithholding.evaluate(getContext(), conceptId, agentCityId, base, tributeUnitAmount, getTransactionName());
		if (!calc.valid) {
			addLog(calc.logMessage);
			return false;
		}
		if (isGenerated())
			return false;
		return true;
	}

	@Override
	public String run() {
		setWithholdingRate(calc.ratePercent);
		addBaseAmount(calc.subjectBase);
		addWithholdingAmount(calc.withholdingAmount);
		addDescription(calc.conceptName + " (simulación)");
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());
		setReturnValue(COLUMNNAME_LCO_MunicipalConcept_PK, calc.municipalConceptId);
		setReturnValue(COLUMNNAME_LCO_MunicipalRate_ID, calc.municipalRateId);
		setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
		setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, true);
		saveResult();
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
