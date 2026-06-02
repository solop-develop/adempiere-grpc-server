/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retención de Impuesto Municipal (ReteICA)           *
 * Adaptado de org.erpya.lve.util.APInvoiceIM (Localización Venezuela)               *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.spin.model.I_WH_Withholding;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * Retención de Impuesto Municipal de Industria y Comercio (ReteICA) para Colombia,
 * sobre facturas de compra (AP).
 *
 * El ICA es municipal: requiere que la organización sea <b>agente retenedor de Impuesto
 * Municipal</b> ({@code LCO_WithholdingSetup.IsMunicipalWithholdingAgent}) y aplica la
 * <i>tarifa por mil</i> de la actividad (concepto) en el <b>municipio del agente</b>
 * (territorialidad). El concepto se toma de la cabecera ({@code C_Invoice.LCO_ICAConcept_ID})
 * o del tercero ({@code C_BPartner.LCO_ICAConcept_ID}); ya no hay default de setup ni
 * configuración por producto/cargo. El cálculo lo centraliza {@link LCOMunicipalWithholding}.
 *
 * @author Solop, www.solopsoftware.com
 */
public class APInvoiceICA extends AbstractWithholdingSetting implements LCOUtil {

	public APInvoiceICA(MWHSetting setting) {
		super(setting);
	}

	private MInvoice invoice;
	private MBPartner businessPartner;
	private LCOMunicipalWithholding calc;
	private BigDecimal tributeUnitAmount = Env.ZERO;

	@Override
	public boolean isValid() {
		if (getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("No es una factura (C_Invoice)");
			return false;
		}
		invoice = (MInvoice) getDocument();
		businessPartner = (MBPartner) invoice.getC_BPartner();
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());

		if (invoice.isSOTrx()) {
			addLog("ReteICA no aplica en documentos de venta");
			return false;
		}
		if (invoice.isReversal()) {
			addLog("La factura está reversada");
			return false;
		}
		MDocType documentType = MDocType.get(getContext(), invoice.getC_DocTypeTarget_ID());
		if (documentType == null
				|| (!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo))) {
			addLog("Se requiere una factura o nota de crédito de compra");
			return false;
		}
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), invoice.getAD_Org_ID(), getTransactionName());
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
		int conceptId = invoice.get_ValueAsInt(COLUMNNAME_LCO_ICAConcept_ID);
		if (conceptId == 0)
			conceptId = businessPartner.get_ValueAsInt(COLUMNNAME_LCO_ICAConcept_ID);
		//	UVT vigente (para base mínima) y municipio del agente (territorialidad)
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), invoice.getDateInvoiced(), getTransactionName());
		int agentCityId = LCOMunicipalWithholding.getAgentCityId(getContext(), invoice.getAD_Org_ID(), getTransactionName());
		BigDecimal base = invoice.getTotalLines();
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
		addDescription(calc.conceptName);
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());
		setReturnValue(COLUMNNAME_LCO_MunicipalConcept_PK, calc.municipalConceptId);
		setReturnValue(COLUMNNAME_LCO_MunicipalRate_ID, calc.municipalRateId);
		setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
		setReturnValue(MWHWithholding.COLUMNNAME_IsManual, false);
		saveResult();
		return null;
	}

	private boolean isGenerated() {
		if (invoice == null)
			return false;
		return new Query(getContext(), MWHWithholding.Table_Name,
					"SourceInvoice_ID = ? AND WH_Definition_ID = ? AND WH_Setting_ID = ? "
					+ "AND Processed = 'Y' AND IsSimulation = 'N' AND DocStatus IN (?,?)", getTransactionName())
				.setParameters(invoice.get_ID(), getDefinition().get_ID(), getSetting().get_ID(),
						MWHWithholding.DOCSTATUS_Completed, MWHWithholding.DOCSTATUS_Closed)
				.match();
	}
}
