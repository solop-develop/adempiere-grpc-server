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
/** Generated Model - DO NOT CHANGE */
package org.adempiere.core.domains.models;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for AD_ClientInfo
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_ClientInfo extends PO implements I_AD_ClientInfo, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260306L;

    /** Standard Constructor */
    public X_AD_ClientInfo (Properties ctx, int AD_ClientInfo_ID, String trxName)
    {
      super (ctx, AD_ClientInfo_ID, trxName);
      /** if (AD_ClientInfo_ID == 0)
        {
			setAllowsAttendanceOutOfTolerance (false);
// N
			setAttendanceRangeTolerance (Env.ZERO);
// 0
			setAttendanceTimeTolerance (Env.ZERO);
// 0
			setIsDiscountLineAmt (false);
			setIsRequiresToleranceApproval (false);
// N
        } */
    }

    /** Load Constructor */
    public X_AD_ClientInfo (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_AD_ClientInfo[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_Tree getAD_Tree_Activity() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Activity_ID(), get_TrxName());	}

	/** Set Activity Tree.
		@param AD_Tree_Activity_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_Activity_ID (int AD_Tree_Activity_ID)
	{
		if (AD_Tree_Activity_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Activity_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Activity_ID, Integer.valueOf(AD_Tree_Activity_ID));
	}

	/** Get Activity Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_Activity_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Activity_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_BPartner() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_BPartner_ID(), get_TrxName());	}

	/** Set BPartner Tree.
		@param AD_Tree_BPartner_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_BPartner_ID (int AD_Tree_BPartner_ID)
	{
		if (AD_Tree_BPartner_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_BPartner_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_BPartner_ID, Integer.valueOf(AD_Tree_BPartner_ID));
	}

	/** Get BPartner Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_Campaign() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Campaign_ID(), get_TrxName());	}

	/** Set Campaign Tree.
		@param AD_Tree_Campaign_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_Campaign_ID (int AD_Tree_Campaign_ID)
	{
		if (AD_Tree_Campaign_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Campaign_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Campaign_ID, Integer.valueOf(AD_Tree_Campaign_ID));
	}

	/** Get Campaign Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_Campaign_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_Menu() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Menu_ID(), get_TrxName());	}

	/** Set Menu Tree.
		@param AD_Tree_Menu_ID 
		Tree of the menu
	  */
	public void setAD_Tree_Menu_ID (int AD_Tree_Menu_ID)
	{
		if (AD_Tree_Menu_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Menu_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Menu_ID, Integer.valueOf(AD_Tree_Menu_ID));
	}

	/** Get Menu Tree.
		@return Tree of the menu
	  */
	public int getAD_Tree_Menu_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Menu_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_Org() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Org_ID(), get_TrxName());	}

	/** Set Organization Tree.
		@param AD_Tree_Org_ID 
		Trees are used for (financial) reporting and security access (via role)
	  */
	public void setAD_Tree_Org_ID (int AD_Tree_Org_ID)
	{
		if (AD_Tree_Org_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Org_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Org_ID, Integer.valueOf(AD_Tree_Org_ID));
	}

	/** Get Organization Tree.
		@return Trees are used for (financial) reporting and security access (via role)
	  */
	public int getAD_Tree_Org_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Org_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_Product() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Product_ID(), get_TrxName());	}

	/** Set Product Tree.
		@param AD_Tree_Product_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_Product_ID (int AD_Tree_Product_ID)
	{
		if (AD_Tree_Product_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Product_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Product_ID, Integer.valueOf(AD_Tree_Product_ID));
	}

	/** Get Product Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_Project() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_Project_ID(), get_TrxName());	}

	/** Set Project Tree.
		@param AD_Tree_Project_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_Project_ID (int AD_Tree_Project_ID)
	{
		if (AD_Tree_Project_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Project_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_Project_ID, Integer.valueOf(AD_Tree_Project_ID));
	}

	/** Get Project Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_Project_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_Project_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Tree getAD_Tree_SalesRegion() throws RuntimeException
    {
		return (I_AD_Tree)MTable.get(getCtx(), I_AD_Tree.Table_Name)
			.getPO(getAD_Tree_SalesRegion_ID(), get_TrxName());	}

	/** Set Sales Region Tree.
		@param AD_Tree_SalesRegion_ID 
		Trees are used for (financial) reporting
	  */
	public void setAD_Tree_SalesRegion_ID (int AD_Tree_SalesRegion_ID)
	{
		if (AD_Tree_SalesRegion_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_SalesRegion_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Tree_SalesRegion_ID, Integer.valueOf(AD_Tree_SalesRegion_ID));
	}

	/** Get Sales Region Tree.
		@return Trees are used for (financial) reporting
	  */
	public int getAD_Tree_SalesRegion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tree_SalesRegion_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Allows Attendance Out Of Tolerance.
		@param AllowsAttendanceOutOfTolerance Allows Attendance Out Of Tolerance	  */
	public void setAllowsAttendanceOutOfTolerance (boolean AllowsAttendanceOutOfTolerance)
	{
		set_Value (COLUMNNAME_AllowsAttendanceOutOfTolerance, Boolean.valueOf(AllowsAttendanceOutOfTolerance));
	}

	/** Get Allows Attendance Out Of Tolerance.
		@return Allows Attendance Out Of Tolerance	  */
	public boolean isAllowsAttendanceOutOfTolerance () 
	{
		Object oo = get_Value(COLUMNNAME_AllowsAttendanceOutOfTolerance);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_AD_User getApprovalSupervisor() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getApprovalSupervisor_ID(), get_TrxName());	}

	/** Set Approval Supervisor.
		@param ApprovalSupervisor_ID Approval Supervisor	  */
	public void setApprovalSupervisor_ID (int ApprovalSupervisor_ID)
	{
		if (ApprovalSupervisor_ID < 1) 
			set_Value (COLUMNNAME_ApprovalSupervisor_ID, null);
		else 
			set_Value (COLUMNNAME_ApprovalSupervisor_ID, Integer.valueOf(ApprovalSupervisor_ID));
	}

	/** Get Approval Supervisor.
		@return Approval Supervisor	  */
	public int getApprovalSupervisor_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ApprovalSupervisor_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Attendance Range Tolerance.
		@param AttendanceRangeTolerance Attendance Range Tolerance	  */
	public void setAttendanceRangeTolerance (BigDecimal AttendanceRangeTolerance)
	{
		set_Value (COLUMNNAME_AttendanceRangeTolerance, AttendanceRangeTolerance);
	}

	/** Get Attendance Range Tolerance.
		@return Attendance Range Tolerance	  */
	public BigDecimal getAttendanceRangeTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AttendanceRangeTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Attendance Time Tolerance.
		@param AttendanceTimeTolerance Attendance Time Tolerance	  */
	public void setAttendanceTimeTolerance (BigDecimal AttendanceTimeTolerance)
	{
		set_Value (COLUMNNAME_AttendanceTimeTolerance, AttendanceTimeTolerance);
	}

	/** Get Attendance Time Tolerance.
		@return Attendance Time Tolerance	  */
	public BigDecimal getAttendanceTimeTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AttendanceTimeTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_C_AcctSchema getC_AcctSchema1() throws RuntimeException
    {
		return (I_C_AcctSchema)MTable.get(getCtx(), I_C_AcctSchema.Table_Name)
			.getPO(getC_AcctSchema1_ID(), get_TrxName());	}

	/** Set Primary Accounting Schema.
		@param C_AcctSchema1_ID 
		Primary rules for accounting
	  */
	public void setC_AcctSchema1_ID (int C_AcctSchema1_ID)
	{
		if (C_AcctSchema1_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema1_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_AcctSchema1_ID, Integer.valueOf(C_AcctSchema1_ID));
	}

	/** Get Primary Accounting Schema.
		@return Primary rules for accounting
	  */
	public int getC_AcctSchema1_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_AcctSchema1_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BPartner getC_BPartnerCashTrx() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getC_BPartnerCashTrx_ID(), get_TrxName());	}

	/** Set Template B.Partner.
		@param C_BPartnerCashTrx_ID 
		Business Partner used for creating new Business Partners on the fly
	  */
	public void setC_BPartnerCashTrx_ID (int C_BPartnerCashTrx_ID)
	{
		if (C_BPartnerCashTrx_ID < 1) 
			set_Value (COLUMNNAME_C_BPartnerCashTrx_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartnerCashTrx_ID, Integer.valueOf(C_BPartnerCashTrx_ID));
	}

	/** Get Template B.Partner.
		@return Business Partner used for creating new Business Partners on the fly
	  */
	public int getC_BPartnerCashTrx_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartnerCashTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Calendar getC_Calendar() throws RuntimeException
    {
		return (I_C_Calendar)MTable.get(getCtx(), I_C_Calendar.Table_Name)
			.getPO(getC_Calendar_ID(), get_TrxName());	}

	/** Set Calendar.
		@param C_Calendar_ID 
		Accounting Calendar Name
	  */
	public void setC_Calendar_ID (int C_Calendar_ID)
	{
		if (C_Calendar_ID < 1) 
			set_Value (COLUMNNAME_C_Calendar_ID, null);
		else 
			set_Value (COLUMNNAME_C_Calendar_ID, Integer.valueOf(C_Calendar_ID));
	}

	/** Get Calendar.
		@return Accounting Calendar Name
	  */
	public int getC_Calendar_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Calendar_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** ComparisonSource AD_Reference_ID=54589 */
	public static final int COMPARISONSOURCE_AD_Reference_ID=54589;
	/** Order = OR */
	public static final String COMPARISONSOURCE_Order = "OR";
	/** Shipment = SH */
	public static final String COMPARISONSOURCE_Shipment = "SH";
	/** Invoice = IN */
	public static final String COMPARISONSOURCE_Invoice = "IN";
	/** Set Comparison Source.
		@param ComparisonSource 
		Document source used for actual sales comparison
	  */
	public void setComparisonSource (String ComparisonSource)
	{

		set_Value (COLUMNNAME_ComparisonSource, ComparisonSource);
	}

	/** Get Comparison Source.
		@return Document source used for actual sales comparison
	  */
	public String getComparisonSource () 
	{
		return (String)get_Value(COLUMNNAME_ComparisonSource);
	}

	public I_C_UOM getC_UOM_Length() throws RuntimeException
    {
		return (I_C_UOM)MTable.get(getCtx(), I_C_UOM.Table_Name)
			.getPO(getC_UOM_Length_ID(), get_TrxName());	}

	/** Set UOM for Length.
		@param C_UOM_Length_ID 
		Standard Unit of Measure for Length
	  */
	public void setC_UOM_Length_ID (int C_UOM_Length_ID)
	{
		if (C_UOM_Length_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_Length_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_Length_ID, Integer.valueOf(C_UOM_Length_ID));
	}

	/** Get UOM for Length.
		@return Standard Unit of Measure for Length
	  */
	public int getC_UOM_Length_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_Length_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_UOM getC_UOM_Time() throws RuntimeException
    {
		return (I_C_UOM)MTable.get(getCtx(), I_C_UOM.Table_Name)
			.getPO(getC_UOM_Time_ID(), get_TrxName());	}

	/** Set UOM for Time.
		@param C_UOM_Time_ID 
		Standard Unit of Measure for Time
	  */
	public void setC_UOM_Time_ID (int C_UOM_Time_ID)
	{
		if (C_UOM_Time_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_Time_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_Time_ID, Integer.valueOf(C_UOM_Time_ID));
	}

	/** Get UOM for Time.
		@return Standard Unit of Measure for Time
	  */
	public int getC_UOM_Time_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_Time_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_UOM getC_UOM_Volume() throws RuntimeException
    {
		return (I_C_UOM)MTable.get(getCtx(), I_C_UOM.Table_Name)
			.getPO(getC_UOM_Volume_ID(), get_TrxName());	}

	/** Set UOM for Volume.
		@param C_UOM_Volume_ID 
		Standard Unit of Measure for Volume
	  */
	public void setC_UOM_Volume_ID (int C_UOM_Volume_ID)
	{
		if (C_UOM_Volume_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_Volume_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_Volume_ID, Integer.valueOf(C_UOM_Volume_ID));
	}

	/** Get UOM for Volume.
		@return Standard Unit of Measure for Volume
	  */
	public int getC_UOM_Volume_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_Volume_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_UOM getC_UOM_Weight() throws RuntimeException
    {
		return (I_C_UOM)MTable.get(getCtx(), I_C_UOM.Table_Name)
			.getPO(getC_UOM_Weight_ID(), get_TrxName());	}

	/** Set UOM for Weight.
		@param C_UOM_Weight_ID 
		Standard Unit of Measure for Weight
	  */
	public void setC_UOM_Weight_ID (int C_UOM_Weight_ID)
	{
		if (C_UOM_Weight_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_Weight_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_Weight_ID, Integer.valueOf(C_UOM_Weight_ID));
	}

	/** Get UOM for Weight.
		@return Standard Unit of Measure for Weight
	  */
	public int getC_UOM_Weight_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_Weight_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_AppRegistration getFileHandler() throws RuntimeException
    {
		return (I_AD_AppRegistration)MTable.get(getCtx(), I_AD_AppRegistration.Table_Name)
			.getPO(getFileHandler_ID(), get_TrxName());	}

	/** Set File Handler.
		@param FileHandler_ID 
		File Handler Registered as App Registration for handle all file system
	  */
	public void setFileHandler_ID (int FileHandler_ID)
	{
		if (FileHandler_ID < 1) 
			set_Value (COLUMNNAME_FileHandler_ID, null);
		else 
			set_Value (COLUMNNAME_FileHandler_ID, Integer.valueOf(FileHandler_ID));
	}

	/** Get File Handler.
		@return File Handler Registered as App Registration for handle all file system
	  */
	public int getFileHandler_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FileHandler_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** ForecastEngine AD_Reference_ID=54603 */
	public static final int FORECASTENGINE_AD_Reference_ID=54603;
	/** Default Forecast Engine = org.solop.forecast.engine.DefaultForecastEngine */
	public static final String FORECASTENGINE_DefaultForecastEngine = "org.solop.forecast.engine.DefaultForecastEngine";
	/** Set Forecast Engine.
		@param ForecastEngine Forecast Engine	  */
	public void setForecastEngine (String ForecastEngine)
	{

		set_Value (COLUMNNAME_ForecastEngine, ForecastEngine);
	}

	/** Get Forecast Engine.
		@return Forecast Engine	  */
	public String getForecastEngine () 
	{
		return (String)get_Value(COLUMNNAME_ForecastEngine);
	}

	/** ForecastLevel AD_Reference_ID=54593 */
	public static final int FORECASTLEVEL_AD_Reference_ID=54593;
	/** Financial = FI */
	public static final String FORECASTLEVEL_Financial = "FI";
	/** Classification = CL */
	public static final String FORECASTLEVEL_Classification = "CL";
	/** Operational = OP */
	public static final String FORECASTLEVEL_Operational = "OP";
	/** Everything = EV */
	public static final String FORECASTLEVEL_Everything = "EV";
	/** Set Forecast Level.
		@param ForecastLevel 
		Financial, Commercial, or Operational forecast level
	  */
	public void setForecastLevel (String ForecastLevel)
	{

		set_Value (COLUMNNAME_ForecastLevel, ForecastLevel);
	}

	/** Get Forecast Level.
		@return Financial, Commercial, or Operational forecast level
	  */
	public String getForecastLevel () 
	{
		return (String)get_Value(COLUMNNAME_ForecastLevel);
	}

	/** Set Calculates Forecast.
		@param IsCalculateForecast Calculates Forecast	  */
	public void setIsCalculateForecast (boolean IsCalculateForecast)
	{
		set_Value (COLUMNNAME_IsCalculateForecast, Boolean.valueOf(IsCalculateForecast));
	}

	/** Get Calculates Forecast.
		@return Calculates Forecast	  */
	public boolean isCalculateForecast () 
	{
		Object oo = get_Value(COLUMNNAME_IsCalculateForecast);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Discount calculated from Line Amounts.
		@param IsDiscountLineAmt 
		Payment Discount calculation does not include Taxes and Charges
	  */
	public void setIsDiscountLineAmt (boolean IsDiscountLineAmt)
	{
		set_Value (COLUMNNAME_IsDiscountLineAmt, Boolean.valueOf(IsDiscountLineAmt));
	}

	/** Get Discount calculated from Line Amounts.
		@return Payment Discount calculation does not include Taxes and Charges
	  */
	public boolean isDiscountLineAmt () 
	{
		Object oo = get_Value(COLUMNNAME_IsDiscountLineAmt);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Requires Out Of Tolerance Approval.
		@param IsRequiresToleranceApproval Requires Out Of Tolerance Approval	  */
	public void setIsRequiresToleranceApproval (boolean IsRequiresToleranceApproval)
	{
		set_Value (COLUMNNAME_IsRequiresToleranceApproval, Boolean.valueOf(IsRequiresToleranceApproval));
	}

	/** Get Requires Out Of Tolerance Approval.
		@return Requires Out Of Tolerance Approval	  */
	public boolean isRequiresToleranceApproval () 
	{
		Object oo = get_Value(COLUMNNAME_IsRequiresToleranceApproval);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Days to keep Log.
		@param KeepLogDays 
		Number of days to keep the log entries
	  */
	public void setKeepLogDays (int KeepLogDays)
	{
		set_Value (COLUMNNAME_KeepLogDays, Integer.valueOf(KeepLogDays));
	}

	/** Get Days to keep Log.
		@return Number of days to keep the log entries
	  */
	public int getKeepLogDays () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_KeepLogDays);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Logo.
		@param Logo_ID Logo	  */
	public void setLogo_ID (int Logo_ID)
	{
		if (Logo_ID < 1) 
			set_Value (COLUMNNAME_Logo_ID, null);
		else 
			set_Value (COLUMNNAME_Logo_ID, Integer.valueOf(Logo_ID));
	}

	/** Get Logo.
		@return Logo	  */
	public int getLogo_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Logo_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Logo Report.
		@param LogoReport_ID Logo Report	  */
	public void setLogoReport_ID (int LogoReport_ID)
	{
		if (LogoReport_ID < 1) 
			set_Value (COLUMNNAME_LogoReport_ID, null);
		else 
			set_Value (COLUMNNAME_LogoReport_ID, Integer.valueOf(LogoReport_ID));
	}

	/** Get Logo Report.
		@return Logo Report	  */
	public int getLogoReport_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LogoReport_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Logo Web.
		@param LogoWeb_ID Logo Web	  */
	public void setLogoWeb_ID (int LogoWeb_ID)
	{
		if (LogoWeb_ID < 1) 
			set_Value (COLUMNNAME_LogoWeb_ID, null);
		else 
			set_Value (COLUMNNAME_LogoWeb_ID, Integer.valueOf(LogoWeb_ID));
	}

	/** Get Logo Web.
		@return Logo Web	  */
	public int getLogoWeb_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LogoWeb_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Product getM_ProductFreight() throws RuntimeException
    {
		return (I_M_Product)MTable.get(getCtx(), I_M_Product.Table_Name)
			.getPO(getM_ProductFreight_ID(), get_TrxName());	}

	/** Set Product for Freight.
		@param M_ProductFreight_ID Product for Freight	  */
	public void setM_ProductFreight_ID (int M_ProductFreight_ID)
	{
		if (M_ProductFreight_ID < 1) 
			set_Value (COLUMNNAME_M_ProductFreight_ID, null);
		else 
			set_Value (COLUMNNAME_M_ProductFreight_ID, Integer.valueOf(M_ProductFreight_ID));
	}

	/** Get Product for Freight.
		@return Product for Freight	  */
	public int getM_ProductFreight_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ProductFreight_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_R_MailText getRestorePassword_MailText() throws RuntimeException
    {
		return (I_R_MailText)MTable.get(getCtx(), I_R_MailText.Table_Name)
			.getPO(getRestorePassword_MailText_ID(), get_TrxName());	}

	/** Set Restore Password Mail Text.
		@param RestorePassword_MailText_ID 
		Used for Restore Password Mail Text
	  */
	public void setRestorePassword_MailText_ID (int RestorePassword_MailText_ID)
	{
		if (RestorePassword_MailText_ID < 1) 
			set_Value (COLUMNNAME_RestorePassword_MailText_ID, null);
		else 
			set_Value (COLUMNNAME_RestorePassword_MailText_ID, Integer.valueOf(RestorePassword_MailText_ID));
	}

	/** Get Restore Password Mail Text.
		@return Used for Restore Password Mail Text
	  */
	public int getRestorePassword_MailText_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_RestorePassword_MailText_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}