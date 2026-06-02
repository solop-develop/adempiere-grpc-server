/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retención de IVA (ReteIVA)                          *
 * Adaptado de org.erpya.lve.util.APInvoiceIVA (Localización Venezuela)              *
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

import org.adempiere.core.domains.models.I_C_Invoice;
import org.spin.model.I_WH_Withholding;
import org.adempiere.core.domains.models.X_LCO_WH_IVARate;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * Retención de IVA (ReteIVA) para Colombia, sobre facturas de compra (AP).
 *
 * Tarifa general 15% del IVA; 100% en casos especiales (p. ej. proveedor del
 * exterior). La tarifa se resuelve por prioridad factura → tercero → default del
 * setup. Solo se retiene a terceros Responsables de IVA (LCO_TaxRegime = RIVA) o
 * del Exterior (EXTR). Se respeta la base mínima en UVT (bienes/servicios).
 *
 * @author Solop, www.solopsoftware.com
 */
public class APInvoiceIVA extends AbstractWithholdingSetting implements LCOUtil {

	public APInvoiceIVA(MWHSetting setting) {
		super(setting);
	}

	private MInvoice invoice;
	private MBPartner businessPartner;
	private List<MInvoiceTax> taxes;
	private X_LCO_WH_IVARate ivaRate;
	private BigDecimal tributeUnitAmount = Env.ZERO;

	@Override
	public boolean isValid() {
		boolean isValid = true;
		if (getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("No es una factura (C_Invoice)");
			return false;
		}
		invoice = (MInvoice) getDocument();
		businessPartner = (MBPartner) invoice.getC_BPartner();
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());

		//	ReteIVA aplica sobre compras
		if (invoice.isSOTrx()) {
			addLog("ReteIVA no aplica en documentos de venta");
			return false;
		}
		if (invoice.isReversal()) {
			addLog("La factura está reversada");
			return false;
		}
		//	Solo facturas/NC de compra
		MDocType documentType = MDocType.get(getContext(), invoice.getC_DocTypeTarget_ID());
		if (documentType == null
				|| (!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo))) {
			addLog("Se requiere una factura o nota de crédito de compra");
			return false;
		}
		//	Configuración de retenciones
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), invoice.getAD_Org_ID(), getTransactionName());
		if (setup == null) {
			addLog("No existe Configuración de Retenciones (LCO_WithholdingSetup)");
			return false;
		}
		if (setup.isClientExcluded() || setup.isExcludeIVAWithholding()) {
			addLog("ReteIVA excluida por configuración (LCO_WithholdingSetup)");
			return false;
		}
		//	Exención del tercero: no se calcula ReteIVA
		if (businessPartner.get_ValueAsBoolean(COLUMNNAME_LCO_IsIVAExempt)) {
			addLog("El tercero está exento de ReteIVA");
			return false;
		}
		//	Exención por documento
		if (invoice.get_ValueAsBoolean(COLUMNNAME_LCO_IsWithholdingTaxExempt)) {
			addLog("La factura está marcada como exenta de ReteIVA");
			return false;
		}
		//	Régimen del tercero: solo responsable de IVA o exterior
		String regime = businessPartner.get_ValueAsString(COLUMNNAME_LCO_TaxRegime);
		if (!TAXREGIME_VATResponsible.equals(regime) && !TAXREGIME_Foreign.equals(regime)) {
			addLog("El tercero no es responsable de IVA: no aplica ReteIVA");
			return false;
		}
		//	Tarifa de ReteIVA: factura → tercero (sin default de setup)
		int ivaRateId = invoice.get_ValueAsInt(COLUMNNAME_LCO_WH_IVARate_ID);
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
		//	UVT vigente
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), invoice.getDateInvoiced(), getTransactionName());
		if (tributeUnitAmount.signum() == 0) {
			addLog("No se encontró el valor de la UVT (LCO_TributeUnit) vigente");
			return false;
		}
		//	Base mínima en UVT (bienes/servicios)
		BigDecimal minUVT = hasServiceLine() ? ivaRate.getMinUVTService() : ivaRate.getMinUVTProduct();
		if (minUVT != null && minUVT.signum() > 0) {
			BigDecimal minimum = minUVT.multiply(tributeUnitAmount);
			if (invoice.getTotalLines().abs().compareTo(minimum) < 0) {
				addLog("Base inferior a la base mínima de ReteIVA en UVT (mínimo = " + minimum
						+ ", base = " + invoice.getTotalLines().abs() + ")");
				return false;
			}
		}
		//	Impuestos sobre los que se retiene (IVA = C_InvoiceTax con monto > 0)
		//	NOTA: sin marcador a nivel C_Tax (ver informe). Se toma todo impuesto con monto.
		taxes = Arrays.asList(invoice.getTaxes(false))
				.stream()
				.filter(it -> it.getTaxAmt() != null && it.getTaxAmt().signum() != 0)
				.collect(Collectors.toList());
		if (taxes.isEmpty()) {
			addLog("La factura no tiene impuestos sujetos a ReteIVA");
			return false;
		}
		if (isGenerated())
			return false;
		return isValid;
	}

	@Override
	public String run() {
		BigDecimal rate = ivaRate.getWithholdingRate();
		taxes.forEach(invoiceTax -> {
			setWithholdingRate(rate);
			addBaseAmount(invoiceTax.getTaxAmt());
			addWithholdingAmount(invoiceTax.getTaxAmt().multiply(getWithholdingRate(true)));
			addDescription(ivaRate.getName() + " procesado");
			setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());
			setReturnValue(MWHWithholding.COLUMNNAME_IsManual, false);
			setReturnValue(MWHWithholding.COLUMNNAME_C_Tax_ID, invoiceTax.getC_Tax_ID());
			setReturnValue(COLUMNNAME_LCO_WH_IVARate_ID, ivaRate.getLCO_WH_IVARate_ID());
			setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
			saveResult();
		});
		return null;
	}

	/** ¿Alguna línea es de tipo servicio? Para elegir el mínimo en UVT. */
	private boolean hasServiceLine() {
		for (MInvoiceLine line : invoice.getLines()) {
			if (line.getM_Product_ID() != 0) {
				MProduct product = MProduct.get(getContext(), line.getM_Product_ID());
				if (product != null && MProduct.PRODUCTTYPE_Service.equals(product.getProductType()))
					return true;
			} else if (line.getC_Charge_ID() != 0) {
				//	Los cargos se asimilan a servicios
				return true;
			}
		}
		return false;
	}

	/** Anti-duplicado: ¿ya existe retención definitiva para esta factura/definición/setting? */
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
