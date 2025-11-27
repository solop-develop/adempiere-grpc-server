/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.luy.util;

/**
 * Add here all changes for core and static methods
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">...</a>
 */
public interface LUYChanges {
	/** Withholding Amount Base Type*/
	String COLUMNNAME_SPLUY_WithholdingAmtBase = "SPLUY_WithholdingAmtBase";
	/** Withholding Amount Base Options*/
	String WithholdingAmtBase_Total_Amount ="A";
	String WithholdingAmtBase_Net_Amount ="S";
	String WithholdingAmtBase_Tax_Amount ="T";
	/** Withholding Percentage*/
	String COLUMNNAME_SPLUY_WithholdingPercentage = "SPLUY_WithholdingPercentage";
}
