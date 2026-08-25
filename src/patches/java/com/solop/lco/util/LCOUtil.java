/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retenciones                                         *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

/**
 * Punto central de constantes de la localización Colombia.
 *
 * Solo declara los nombres de columnas <b>nuevas añadidas a tablas estándar</b>
 * (C_BPartner, AD_OrgInfo, C_Invoice, C_Order, C_OrderLine, WH_Withholding) y los
 * valores de las listas de referencia. Las columnas de las tablas {@code LCO_*}
 * se referencian con las constantes {@code COLUMNNAME_*} de las clases generadas
 * {@code I_LCO_*} / {@code X_LCO_*}.
 *
 * @author Solop, www.solopsoftware.com
 */
public interface LCOUtil {

	/* ----------------------------------------------------------------------
	 * C_BPartner (tercero)
	 * -------------------------------------------------------------------- */
	/** Tipo de persona del tercero (lista LCO_PersonType: PN / PJ). */
	String COLUMNNAME_LCO_PersonType            = "LCO_PersonType";
	/** Régimen tributario del tercero (lista LCO_TaxRegime). */
	String COLUMNNAME_LCO_TaxRegime             = "LCO_TaxRegime";
	/** Es Gran Contribuyente. */
	String COLUMNNAME_IsLargeTaxpayer           = "IsLargeTaxpayer";
	/** Es autorretenedor (no se le practica retención de renta). */
	String COLUMNNAME_IsSelfWithholding         = "IsSelfWithholding";
	/** Concepto de Retefuente (renta) por defecto del tercero. */
	String COLUMNNAME_LCO_RentaConcept_ID       = "LCO_RentaConcept_ID";
	/** Concepto de Impuesto Municipal (ICA) por defecto del tercero.
	 *  Referencia {@link #TABLENAME_LCO_MunicipalConcept}. */
	String COLUMNNAME_LCO_ICAConcept_ID         = "LCO_ICAConcept_ID";
	/** Tarifa de ReteIVA por defecto del tercero. */
	String COLUMNNAME_LCO_WH_IVARate_ID         = "LCO_WH_IVARate_ID";
	/** El tercero está exento de Retefuente (renta). */
	String COLUMNNAME_LCO_IsRentaExempt         = "LCO_IsRentaExempt";
	/** El tercero está exento de ReteIVA. */
	String COLUMNNAME_LCO_IsIVAExempt           = "LCO_IsIVAExempt";
	/** El tercero está exento de ReteICA. */
	String COLUMNNAME_LCO_IsICAExempt           = "LCO_IsICAExempt";

	/* ----------------------------------------------------------------------
	 * AD_OrgInfo (organización — autorretención en ventas)
	 * -------------------------------------------------------------------- */
	/** Concepto de autorretención de renta de la organización. */
	String COLUMNNAME_LCO_SelfRentaConcept_ID   = "LCO_SelfRentaConcept_ID";

	/* ----------------------------------------------------------------------
	 * Impuesto Municipal (ICA) — tablas y columnas propias
	 * -------------------------------------------------------------------- */
	/** Tabla maestro del concepto de Impuesto Municipal. */
	String TABLENAME_LCO_MunicipalConcept       = "LCO_MunicipalConcept";
	/** Tabla hija: tarifa por mil por ciudad. */
	String TABLENAME_LCO_MunicipalRate          = "LCO_MunicipalRate";
	/** La organización es agente retenedor de Impuesto Municipal (LCO_WithholdingSetup). */
	String COLUMNNAME_IsMunicipalWithholdingAgent = "IsMunicipalWithholdingAgent";
	/** PK del concepto municipal (también columna de salida en WH_Withholding). */
	String COLUMNNAME_LCO_MunicipalConcept_PK   = "LCO_MunicipalConcept_ID";
	/** PK de la tarifa municipal por ciudad (también columna de salida en WH_Withholding). */
	String COLUMNNAME_LCO_MunicipalRate_ID      = "LCO_MunicipalRate_ID";
	/** Tarifa "por mil" (concepto municipal y su tarifa por ciudad). */
	String COLUMNNAME_RatePerThousand           = "RatePerThousand";
	/** Tarifa de base sujeta (AIU u otra). */
	String COLUMNNAME_WithholdingBaseRate       = "WithholdingBaseRate";
	/** Base mínima en UVT. */
	String COLUMNNAME_MinimumUVT                = "MinimumUVT";
	/** Ciudad (C_City) de la tarifa municipal. */
	String COLUMNNAME_C_City_ID                 = "C_City_ID";

	/* ----------------------------------------------------------------------
	 * C_Invoice / C_Order (cabecera) y C_OrderLine
	 * -------------------------------------------------------------------- */
	/** Documento exento de retención de IVA. */
	String COLUMNNAME_LCO_IsWithholdingTaxExempt = "LCO_IsWithholdingTaxExempt";
	/** Tramo de retención (línea de orden). */
	String COLUMNNAME_LCO_WH_Combination_ID     = "LCO_WH_Combination_ID";

	/* ----------------------------------------------------------------------
	 * WH_Withholding (columnas LCO de salida)
	 * -------------------------------------------------------------------- */
	/** Concepto aplicado en la retención generada. */
	String COLUMNNAME_LCO_WH_Concept_ID         = "LCO_WH_Concept_ID";
	/** Valor de la UVT usada en el cálculo. */
	String COLUMNNAME_LCO_TributeUnitAmt        = "LCO_TributeUnitAmt";

	/* ----------------------------------------------------------------------
	 * Lista de referencia LCO_PersonType
	 * -------------------------------------------------------------------- */
	String PERSONTYPE_NaturalPerson             = "PN";
	String PERSONTYPE_LegalPerson               = "PJ";

	/* ----------------------------------------------------------------------
	 * Lista de referencia LCO_TaxRegime
	 * -------------------------------------------------------------------- */
	/** Responsable de IVA (Régimen Común). */
	String TAXREGIME_VATResponsible             = "RIVA";
	/** No Responsable de IVA. */
	String TAXREGIME_VATNotResponsible          = "NRIV";
	/** Régimen Simple de Tributación. */
	String TAXREGIME_Simple                     = "SIMP";
	/** No Contribuyente. */
	String TAXREGIME_NonContributor             = "NOCO";
	/** Exterior / No residente. */
	String TAXREGIME_Foreign                    = "EXTR";
}
