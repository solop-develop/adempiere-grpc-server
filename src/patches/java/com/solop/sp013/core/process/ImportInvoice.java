/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package com.solop.sp013.core.process;

import org.adempiere.core.domains.models.I_I_Invoice;
import org.adempiere.core.domains.models.X_I_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MClientInfo;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocation;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPeriod;
import org.compiere.model.MPriceList;
import org.compiere.model.MTax;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 *	Import Invoice from I_Invoice
 *
 * 	@author 	Jorg Janke
 *  @author		victor.perez@e-evolution.com, www.e-evolution.com
 *  <li> https://adempiere.atlassian.net/browse/ADEMPIERE-84
 * 	@version 	$Id: ImportInvoice.java,v 1.1 2007/09/05 09:27:31 cruiz Exp $
 */
public class ImportInvoice extends SvrProcess
{
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Organization to be imported to		*/
	private int				m_AD_Org_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Document Action					*/
	private String			m_docAction = MInvoice.DOCACTION_Prepare;
	/** Effective						*/
	private Timestamp		m_DateValue = null;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AD_Org_ID"))
				m_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("DocAction"))
				m_docAction = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (m_DateValue == null)
			m_DateValue = new Timestamp (System.currentTimeMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return clear Message
	 *  @throws Exception
	 */
	protected String doIt() throws Exception
	{
		StringBuffer sql = null;
		int no = 0;
		String clientCheck = " AND AD_Client_ID=" + m_AD_Client_ID;
		MOrg org = new MOrg(getCtx(), m_AD_Org_ID, get_TrxName());
		MOrgInfo orgInfo = org.getInfo();

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuffer ("DELETE I_Invoice "
					+ "WHERE I_IsImported='Y'").append (clientCheck);
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			log.fine("Delete Old Imported =" + no);
		}

		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET AD_Client_ID = COALESCE (AD_Client_ID,").append (m_AD_Client_ID).append ("),"
				+ " AD_Org_ID = ").append (m_AD_Org_ID).append (","
				+ " IsActive = COALESCE (IsActive, 'Y'),"
				+ " Created = COALESCE (Created, SysDate),"
				+ " CreatedBy = COALESCE (CreatedBy, 0),"
				+ " Updated = COALESCE (Updated, SysDate),"
				+ " UpdatedBy = COALESCE (UpdatedBy, 0),"
				+ " I_ErrorMsg = ' ',"
				+ " I_IsImported = 'N' "
				+ "WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.info ("Reset=" + no);

		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '"
				+ "WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0"
				+ " OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_Org_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			log.warning ("Invalid Org=" + no);
			addLog("@Error@ @AD_Org_ID@ @Invalid@ " + no);
		}


		//	Document Type - PO - SO
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName"
				+ " AND d.DocBaseType IN ('API','APC') AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set PO DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName"
				+ " AND d.DocBaseType IN ('ARI','ARC') AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set SO DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName"
				+ " AND d.DocBaseType IN ('API','ARI','APC','ARC') AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Dpcument Type Name, ' "
				+ "WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @DocTypeName@ @Invalid@ " + no);
			log.warning ("Invalid DocTypeName=" + no);
		}

		//	DocType Default
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'"
				+ " AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set PO Default DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'"
				+ " AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set SO Default DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_DocType_ID=(SELECT MAX(C_DocType_ID) FROM C_DocType d WHERE d.IsDefault='Y'"
				+ " AND d.DocBaseType IN('ARI','API') AND o.AD_Client_ID=d.AD_Client_ID) "
				+ "WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			log.fine("Set Default DocType=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Document Type, ' "
				+ "WHERE C_DocType_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_DocType_ID@ @NotFound@ " + no);
			log.warning ("No DocType=" + no);
		}


		//	Set IsSOTrx
		sql = new StringBuffer ("UPDATE I_Invoice o SET IsSOTrx='Y' "
				+ "WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID)"
				+ " AND C_DocType_ID IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set IsSOTrx=Y=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o SET IsSOTrx='N' "
				+ "WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID)"
				+ " AND C_DocType_ID IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set IsSOTrx=N=" + no);

		//	Price List
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'"
				+ " AND p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Default Currency PriceList=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p WHERE p.IsDefault='Y'"
				+ " AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Default PriceList=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p "
				+ " WHERE p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Currency PriceList=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_PriceList_ID=(SELECT MAX(M_PriceList_ID) FROM M_PriceList p "
				+ " WHERE p.IsSOPriceList=o.IsSOTrx AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set PriceList=" + no);
		//
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Price List, ' "
				+ "WHERE M_PriceList_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @M_PriceList_ID@ @NotFound@ " + no);
			log.warning("No PriceList=" + no);
		}


		//	Payment Term
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p"
				+ " WHERE o.PaymentTermValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE C_PaymentTerm_ID IS NULL AND PaymentTermValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set PaymentTerm=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_PaymentTerm_ID=(SELECT MAX(C_PaymentTerm_ID) FROM C_PaymentTerm p"
				+ " WHERE p.IsDefault='Y' AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE C_PaymentTerm_ID IS NULL AND o.PaymentTermValue IS NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Default PaymentTerm=" + no);
		//
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Payment Term, ' "
				+ "WHERE C_PaymentTerm_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_PaymentTerm_ID@ @NotFound@ " + no);
			log.warning ("No PaymentTerm=" + no);
		}


		// globalqss - Add project and activity
		//	Project
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_Project_ID=(SELECT C_Project_ID FROM C_Project p"
				+ " WHERE o.ProjectValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Project=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Project, ' "
				+ "WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL "
				+ "AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @ProjectValue@ @Invalid@ " + no);
			log.warning ("Invalid Project=" + no);
		}

		//	Activity
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_Activity_ID=(SELECT C_Activity_ID FROM C_Activity p"
				+ " WHERE o.ActivityValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Activity=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Activity, ' "
				+ "WHERE C_Activity_ID IS NULL AND ActivityValue IS NOT NULL "
				+ "AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @ActivityValue@ @Invalid@ " + no);
			log.warning ("Invalid Activity=" + no);
		}

		// globalqss - add charge
		//	Charge
		sql = new StringBuffer("UPDATE I_Invoice o "
				+ "SET C_Charge_ID=(SELECT C_Charge_ID FROM C_Charge p"
				+ " WHERE o.ChargeName=p.Name AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE C_Charge_ID IS NULL AND ChargeName IS NOT NULL AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Charge=" + no);
		sql = new StringBuffer("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Charge, ' "
				+ "WHERE C_Charge_ID IS NULL AND ChargeName IS NOT NULL "
				+ "AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @ChargeName@ @Invalid@ " + no);
			log.warning("Invalid Charge=" + no);
		}


		//

		//	BP from EMail
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u"
				+ " WHERE o.EMail=u.EMail AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) "
				+ "WHERE C_BPartner_ID IS NULL AND EMail IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set BP from EMail=" + no);
		//	BP from ContactName
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u"
				+ " WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) "
				+ "WHERE C_BPartner_ID IS NULL AND ContactName IS NOT NULL"
				+ " AND EXISTS (SELECT Name FROM AD_User u WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL GROUP BY Name HAVING COUNT(*)=1)"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set BP from ContactName=" + no);
		//	BP from Value
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_BPartner_ID=(SELECT MAX(C_BPartner_ID) FROM C_BPartner bp"
				+ " WHERE o.BPartnerValue=bp.Value AND o.AD_Client_ID=bp.AD_Client_ID) "
				+ "WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set BP from Value=" + no);
		//	Default BP
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_BPartner_ID=(SELECT C_BPartnerCashTrx_ID FROM AD_ClientInfo c"
				+ " WHERE o.AD_Client_ID=c.AD_Client_ID) "
				+ "WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NULL AND Name IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Default BP=" + no);

		//	Existing Location ? Exact Match
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_BPartner_Location_ID=(SELECT C_BPartner_Location_ID"
				+ " FROM C_BPartner_Location bpl INNER JOIN C_Location l ON (bpl.C_Location_ID=l.C_Location_ID)"
				+ " WHERE o.C_BPartner_ID=bpl.C_BPartner_ID AND bpl.AD_Client_ID=o.AD_Client_ID"
				+ " AND DUMP(o.Address1)=DUMP(l.Address1) AND DUMP(o.Address2)=DUMP(l.Address2)"
				+ " AND DUMP(o.City)=DUMP(l.City) AND DUMP(o.Postal)=DUMP(l.Postal)"
				+ " AND o.C_Region_ID=l.C_Region_ID AND o.C_Country_ID=l.C_Country_ID) "
				+ "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL"
				+ " AND I_IsImported='N'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Found Location=" + no);
		//	Set Location from BPartner
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_BPartner_Location_ID=(SELECT MAX(C_BPartner_Location_ID) FROM C_BPartner_Location l"
				+ " WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID"
				+ " AND ((l.IsBillTo='Y' AND o.IsSOTrx='Y') OR o.IsSOTrx='N')"
				+ ") "
				+ "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set BP Location from BP=" + no);
		//
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No Partner Location, ' "
				+ "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_BPartner_Location_ID@ @NotFound@ " + no);
			log.warning ("No BP Location=" + no);
		}


		//	Set Country

		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_Country_ID=(SELECT C_Country_ID FROM C_Country c"
				+ " WHERE o.CountryCode=c.CountryCode AND c.AD_Client_ID IN (0, o.AD_Client_ID)) "
				+ "WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL AND CountryCode IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Country=" + no);
		//
		/*sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Country, ' "
				+ "WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());*/
		if (no != 0)
			log.warning ("Invalid Country=" + no);

		//	Set Region
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "Set RegionName=(SELECT MAX(Name) FROM C_Region r"
				+ " WHERE r.IsDefault='Y' AND r.C_Country_ID=o.C_Country_ID"
				+ " AND r.AD_Client_ID IN (0, o.AD_Client_ID)) "
				+ "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Region Default=" + no);
		//
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "Set C_Region_ID=(SELECT C_Region_ID FROM C_Region r"
				+ " WHERE upper(r.Name)=upper(o.RegionName) AND r.C_Country_ID=o.C_Country_ID"
				+ " AND r.AD_Client_ID IN (0, o.AD_Client_ID)) "
				+ "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Region=" + no);
		//
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Region, ' "
				+ "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL "
				+ " AND EXISTS (SELECT * FROM C_Country c"
				+ " WHERE c.C_Country_ID=o.C_Country_ID AND c.HasRegion='Y')"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_BPartner_Location_ID@ @NotFound@ " + no);
			log.warning ("Invalid Region=" + no);
		}


		//	Product
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p"
				+ " WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Product from Value=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p"
				+ " WHERE o.UPC=p.UPC AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_Product_ID IS NULL AND UPC IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Product from UPC=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET M_Product_ID=(SELECT MAX(M_Product_ID) FROM M_Product p"
				+ " WHERE o.SKU=p.SKU AND o.AD_Client_ID=p.AD_Client_ID) "
				+ "WHERE M_Product_ID IS NULL AND SKU IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Product fom SKU=" + no);
		sql = new StringBuffer("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' "
				+ "WHERE M_Product_ID IS NULL AND (ProductValue IS NOT NULL OR UPC IS NOT NULL OR SKU IS NOT NULL)"
				+ " AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @ProductValue@ @OR@ @UPC@ @OR@ @SKU@ @Invalid@" + no);
			log.warning("Invalid Product=" + no);
		}


		// globalqss - charge and product are exclusive
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR= No Product and Charge, ' "
				+ "WHERE M_Product_ID IS NOT NULL AND C_Charge_ID IS NOT NULL "
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @ChargeExclusively@ " + no);
			log.warning ("Invalid Product and Charge exclusive=" + no);
		}


		//	Tax
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET C_Tax_ID=(SELECT MAX(C_Tax_ID) FROM C_Tax t "
				+ " INNER JOIN SP013_TaxType tt ON (tt.SP013_TaxType_ID = t.SP013_TaxType_ID) "
				+ " WHERE o.TaxIndicator=tt.Value AND o.AD_Client_ID=t.AD_Client_ID) "
				+ " WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		log.fine("Set Tax=" + no);
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Tax, ' "
				+ "WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @TaxIndicator@ @Invalid@ " + no);
			log.warning ("Invalid Tax=" + no);
		}


		//Control de linea sin producto ni cargo
		sql = new StringBuffer ("UPDATE I_Invoice o "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR= No Product or Charge, ' "
				+ "WHERE o.M_Product_ID IS NULL AND o.C_Charge_ID IS NULL "
				+ "AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0){
			addLog("@Error@ @M_Product_ID@ @AND@ @C_Charge_ID@ @NotFound@: " + no);
			log.warning ("No charge and product=" + no);
			return "";
		}

		commitEx();

		//Control de periodo abierto para fecha contable
		sql = new StringBuffer ("SELECT distinct DateAcct, C_DocType_ID FROM I_Invoice "
				+ "WHERE I_IsImported='N' "
				+ "AND DateAcct IS NOT NULL AND C_DocType_ID IS NOT NULL").append (clientCheck)
				.append(" ORDER BY DateAcct");

		PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
		ResultSet rs = pstmt.executeQuery();

		try	{

			while (rs.next()) {
				int docTypeID = rs.getInt("C_DocType_ID");
				Timestamp dateAcct = rs.getTimestamp("DateAcct");
				MPeriod period = MPeriod.get(getCtx(), dateAcct, m_AD_Org_ID, get_TrxName());

				if (period == null) {
					sql = new StringBuffer ("UPDATE I_Invoice "
							+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR= No Period, ' "
							+ "WHERE C_DocType_ID = " + docTypeID + " AND DateAcct = '" + dateAcct + "' "
							+ "AND I_IsImported<>'Y'").append (clientCheck);
					no = DB.executeUpdate(sql.toString(), get_TrxName());
					if (no != 0) {
						addLog("@Error@ @PeriodNotValid@: " + DisplayType.getDateFormat(DisplayType.Date).format(dateAcct));
						log.warning ("No Period for " + dateAcct);
					}


				} else {

					MDocType docType = new MDocType(getCtx(), docTypeID, get_TrxName());
					boolean open = MPeriod.isOpen(getCtx(), dateAcct, docType.getDocBaseType(), m_AD_Org_ID, get_TrxName());

					if (!open) {
						sql = new StringBuffer ("UPDATE I_Invoice "
								+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Period Closed, ' "
								+ "WHERE C_DocType_ID = " + docTypeID + " AND DateAcct = '" + dateAcct + "' "
								+ "AND I_IsImported<>'Y'").append (clientCheck);
						no = DB.executeUpdate(sql.toString(), get_TrxName());
						if(no != 0) {
							addLog("@Error@ @PeriodClosed@: @DocBaseType@" + docType.getDocBaseType() + " (" + DisplayType.getDateFormat(DisplayType.Date).format(dateAcct) + ")");
							log.warning(period.getName() + ": Not open for " + docType.getDocBaseType() + " (" + dateAcct + ")");
						}

					}
				}
			}

			DB.close(rs);

		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		//	-- New BPartner ---------------------------------------------------
		String createBPartnerOrg = orgInfo.get_ValueAsString("SP013_CreateBPartnerImport");
		boolean createBPartner;
		if (!Util.isEmpty(createBPartnerOrg, true)) {
			createBPartner = "Y".equals(createBPartnerOrg);
		} else {
			createBPartner = MClientInfo.get(getCtx()).get_ValueAsBoolean("SP013_CreateBPartnerImport");
		}
		if (createBPartner) {
			//	Go through Invoice Records w/o C_BPartner_ID
			sql = new StringBuffer("I_IsImported='N' AND C_BPartner_ID IS NULL").append(clientCheck);
			List<Integer> importedInvoiceIds = new Query(getCtx(), I_I_Invoice.Table_Name, sql.toString(), get_TrxName())
					.getIDsAsList();
			try {
				for (Integer importInvoiceId : importedInvoiceIds) {
					Trx.run(transactionName -> {
						X_I_Invoice imp = new X_I_Invoice(getCtx(), importInvoiceId, transactionName);
						if (imp.get_ValueAsString("TaxID") != null
								&& !imp.get_ValueAsString("TaxID").equalsIgnoreCase(""))
							imp.setBPartnerValue(imp.get_ValueAsString("TaxID"));
						else if (imp.getEMail() != null)
							imp.setBPartnerValue(imp.getEMail());
						else if (imp.getName() != null)
							imp.setBPartnerValue(imp.getName());
						else
							return;

						if (imp.getName() == null) {
							if (imp.getContactName() != null)
								imp.setName(imp.getContactName());
							else
								imp.setName(imp.getBPartnerValue());
						}
						//	BPartner
						MBPartner bp = MBPartner.get(getCtx(), imp.getBPartnerValue());
						if (bp == null) {
							bp = MBPartner.getTemplate(getCtx(), m_AD_Client_ID);
							bp.set_TrxName(transactionName);
							bp.setClientOrg(imp.getAD_Client_ID(), imp.getAD_Org_ID());
							bp.setValue(imp.getBPartnerValue());
							bp.setName(imp.getName());
							bp.setIsVendor(!imp.isSOTrx());
							bp.setIsCustomer(imp.isSOTrx());
							if (imp.get_ValueAsString("TaxID") != null
									&& !imp.get_ValueAsString("TaxID").equalsIgnoreCase("")){
								bp.setTaxID(imp.get_ValueAsString("TaxID"));
							}
							if (!bp.save(transactionName))
								return;
						}
						imp.setC_BPartner_ID(bp.getC_BPartner_ID());

						//	BP Location
						MBPartnerLocation bpl = null;
						MBPartnerLocation[] bpls = bp.getLocations(true);
						for (int i = 0; bpl == null && i < bpls.length; i++) {
							if (imp.getC_BPartner_Location_ID() == bpls[i].getC_BPartner_Location_ID())
								bpl = bpls[i];
								//	Same Location ID
							else if (imp.getC_Location_ID() == bpls[i].getC_Location_ID())
								bpl = bpls[i];
								//	Same Location Info
							else if (imp.getC_Location_ID() == 0) {
								MLocation loc = bpls[i].getLocation(false);
								if (loc.equals(imp.getC_Country_ID(), imp.getC_Region_ID(),
										imp.getPostal(), "", imp.getCity(),
										imp.getAddress1(), imp.getAddress2()))
									bpl = bpls[i];
							}
						}
						if (bpl == null) {
							//	New Location
							MBPartner templateBPartner = MBPartner.getBPartnerCashTrx(getCtx(), m_AD_Client_ID);
							Optional<MBPartnerLocation> maybeTemplateLocation = Arrays.stream(templateBPartner.getLocations(false))
									.findFirst()
									;

							MLocation loc = new MLocation(getCtx(), 0, transactionName);
							maybeTemplateLocation.ifPresent(mbPartnerLocation -> PO.copyValues(mbPartnerLocation, loc));
							loc.setAddress1(imp.getAddress1());
							loc.setAddress2(imp.getAddress2());
							if (imp.getCity() != null) {
								loc.setCity(imp.getCity());
							}
							if (!Util.isEmpty(imp.getPostal())) {
								loc.setPostal(imp.getPostal());
							}

							if (imp.getC_Region_ID() != 0)
								loc.setC_Region_ID(imp.getC_Region_ID());
							if (imp.getC_Country_ID() > 0) {
								loc.setC_Country_ID(imp.getC_Country_ID());
							}
							if (!loc.save()) {
								return;
							}
							//
							bpl = new MBPartnerLocation(bp);
							bpl.setC_Location_ID(imp.getC_Location_ID() > 0 ? imp.getC_Location_ID() : loc.getC_Location_ID());
							if (!bpl.save(transactionName))
								return;
						}
						imp.setC_Location_ID(bpl.getC_Location_ID());
						imp.setC_BPartner_Location_ID(bpl.getC_BPartner_Location_ID());

						//	User/Contact
						if (imp.getContactName() != null
								|| imp.getEMail() != null
								|| imp.getPhone() != null) {
							MUser[] users = bp.getContacts(true);
							MUser user = null;
							for (int i = 0; user == null && i < users.length; i++) {
								String name = users[i].getName();
								if (name.equals(imp.getContactName())
										|| name.equals(imp.getName())) {
									user = users[i];
									imp.setAD_User_ID(user.getAD_User_ID());
								}
							}
							if (user == null) {
								user = new MUser(bp);
								if (imp.getContactName() == null)
									user.setName(imp.getName());
								else
									user.setName(imp.getContactName());
								user.setEMail(imp.getEMail());
								user.setPhone(imp.getPhone());
								if (user.save(transactionName))
									imp.setAD_User_ID(user.getAD_User_ID());
							}
						}
						imp.save();
					});

				}    //	for all new BPartners
				//
			} catch (Exception e) {
				log.log(Level.SEVERE, "CreateBP", e);
			}
		}

		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR= NO Partner, ' "
				+ "WHERE C_BPartner_ID IS NULL"
				+ " AND I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_BPartner_ID@ @NotFound@ " + no);
			log.warning ("No BPartner=" + no);
		}


		sql = new StringBuffer ("UPDATE I_Invoice i "
				+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invoice Already Exists for Partner, Document Type and Document No ' "
				+ "WHERE i.C_BPartner_ID IS NOT NULL "
				+ "AND i.DocumentNo IS NOT NULL "
				+ "AND i.C_DocType_ID IS NOT NULL "
				+ "AND EXISTS (SELECT 1 FROM C_Invoice inv WHERE inv.C_BPartner_ID = i.C_BPartner_ID "
				+ "AND inv.C_DocTypeTarget_ID = i.C_DocType_ID "
				+ "AND inv.DocumentNo = i.DocumentNo) "
				+ "AND i.I_IsImported<>'Y'").append (clientCheck);
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0) {
			addLog("@Error@ @C_Invoice_ID@ @AlreadyExists@ @SameBPartner@, @C_DocType_ID@ @AND@ @DocumentNo@");
			log.warning ("@AlreadyExists@=" + no);
		}


		commitEx();

		//	-- New Invoices -----------------------------------------------------

		AtomicInteger noInsert =  new AtomicInteger(0);
		AtomicInteger noInsertLine = new AtomicInteger(0);
		AtomicInteger noInvDeleted = new AtomicInteger(0);

		//	Go through Invoice Records w/o
		sql = new StringBuffer ("I_IsImported='N'").append (clientCheck);

		List<Integer> importInvoiceIds = new Query(getCtx(), I_I_Invoice.Table_Name, sql.toString(), get_TrxName())
				.setOrderBy("C_BPartner_ID, DateInvoiced, DocumentNo, C_BPartner_Location_ID, I_Invoice_ID")
				.getIDsAsList();
		try
		{
			Trx.run(transactionName-> {
				//	Group Change
				int oldC_BPartner_ID = 0;
				int oldC_BPartner_Location_ID = 0;
				Timestamp old_DateInvoiced = null;
				String oldDocumentNo = "";
				//
				MInvoice invoice = null;
				int lineNo = 0;

				for (Integer importInvoiceId : importInvoiceIds){
					X_I_Invoice imp = new X_I_Invoice (getCtx (), importInvoiceId, transactionName);
					String cmpDocumentNo = imp.getDocumentNo();
					if (cmpDocumentNo == null)
						cmpDocumentNo = "";
					//	New Invoice
					if (oldC_BPartner_ID != imp.getC_BPartner_ID()
							|| oldC_BPartner_Location_ID != imp.getC_BPartner_Location_ID()
							|| !oldDocumentNo.equals(cmpDocumentNo)
							|| old_DateInvoiced == null
							|| !old_DateInvoiced.equals(imp.getDateInvoiced())
					){
						if (invoice != null) {
							//Validate Number of Lines
							int countToImport = getLinesToImportCount(invoice);
							int importedLinesCount = getImportedLinesCount(invoice);
							if (importedLinesCount == countToImport) {
								invoice.processIt(m_docAction);
								invoice.saveEx();

							} else {
								setLinesNotProcessed(invoice);
								DB.executeUpdateEx("DELETE FROM C_Invoice WHERE C_Invoice_ID = " + invoice.get_ID(), transactionName);
								noInvDeleted.getAndAdd(1);
							}
						}
						//	Group Change
						oldC_BPartner_ID = imp.getC_BPartner_ID();
						old_DateInvoiced = imp.getDateInvoiced();
						//Fin #12990.
						oldC_BPartner_Location_ID = imp.getC_BPartner_Location_ID();
						oldDocumentNo = imp.getDocumentNo();
						if (oldDocumentNo == null)
							oldDocumentNo = "";
						//
						invoice = new MInvoice (getCtx(), 0, transactionName);
						invoice.setClientOrg (imp.getAD_Client_ID(), imp.getAD_Org_ID());
						invoice.setC_DocTypeTarget_ID(imp.getC_DocType_ID());
						invoice.setIsSOTrx(imp.isSOTrx());
						if (imp.getDocumentNo() != null)
							invoice.setDocumentNo(imp.getDocumentNo());
						//
						invoice.setBPartner((MBPartner) imp.getC_BPartner());

						if (imp.get_ValueAsString("PaymentRule") != null
								&& !imp.get_ValueAsString("PaymentRule").equalsIgnoreCase("")) {
							invoice.setPaymentRule(imp.get_ValueAsString("PaymentRule"));
						}

						invoice.setC_BPartner_ID(imp.getC_BPartner_ID());
						invoice.setC_BPartner_Location_ID(imp.getC_BPartner_Location_ID());
						if (imp.getAD_User_ID() != 0)
							invoice.setAD_User_ID(imp.getAD_User_ID());
						//
						if (imp.getDescription() != null)
							invoice.setDescription(imp.getDescription());
						invoice.setC_PaymentTerm_ID(imp.getC_PaymentTerm_ID());
						invoice.setM_PriceList_ID(imp.getM_PriceList_ID());
						//	SalesRep from Import or the person running the import
						if (imp.getSalesRep_ID() != 0)
							invoice.setSalesRep_ID(imp.getSalesRep_ID());
						if (invoice.getSalesRep_ID() == 0)
							invoice.setSalesRep_ID(getAD_User_ID());
						//
						if (imp.getAD_OrgTrx_ID() != 0)
							invoice.setAD_OrgTrx_ID(imp.getAD_OrgTrx_ID());
						if (imp.getC_Activity_ID() != 0)
							invoice.setC_Activity_ID(imp.getC_Activity_ID());
						if (imp.getC_Campaign_ID() != 0)
							invoice.setC_Campaign_ID(imp.getC_Campaign_ID());
						if (imp.getC_Project_ID() != 0)
							invoice.setC_Project_ID(imp.getC_Project_ID());
						//
						if (imp.getDateInvoiced() != null)
							invoice.setDateInvoiced(imp.getDateInvoiced());
						if (imp.getDateAcct() != null)
							invoice.setDateAcct(imp.getDateAcct());
						if (imp.getInvoiceCollectionType() != null)
							invoice.setInvoiceCollectionType(imp.getInvoiceCollectionType());
						if(imp.getDunningGrace() != null)
							invoice.setDunningGrace(imp.getDunningGrace());
						if(imp.getC_DunningLevel_ID() != 0)
							invoice.setC_DunningLevel_ID(imp.getC_DunningLevel_ID());

						if(orgInfo != null && orgInfo.get_ID() > 0){
							if(invoice.getUser1_ID() <= 0){
								if ("Y".equals(Env.getContext(getCtx(), "$Element_U1"))){
									if(orgInfo.get_ValueAsInt("User1_ID") > 0){
										invoice.setUser1_ID(orgInfo.get_ValueAsInt("User1_ID"));
									}
								}
							}

							if(invoice.getC_Campaign_ID() <= 0){
								if ("Y".equals(Env.getContext(getCtx(), "$Element_MC"))){
									if(orgInfo.get_ValueAsInt("C_Campaign_ID") > 0){
										invoice.setC_Campaign_ID(orgInfo.get_ValueAsInt("C_Campaign_ID"));
									}
								}
							}
						}

						if (imp.get_ValueAsInt("S_Contract_ID") != 0)
							invoice.set_ValueOfColumn("S_Contract_ID", imp.get_ValueAsInt("S_Contract_ID"));
						//
						invoice.saveEx();
						noInsert.getAndAdd(1);
						lineNo = 10;
					}
					imp.setC_Invoice_ID (invoice.getC_Invoice_ID());
					//	New InvoiceLine
					MInvoiceLine line = new MInvoiceLine(invoice);
					if (imp.getLineDescription() != null)
						line.setDescription(imp.getLineDescription());
					line.setLine(lineNo);
					lineNo += 10;

					if (imp.getM_Product_ID() != 0)
						line.setM_Product_ID(imp.getM_Product_ID(), true);
					// globalqss - import invoice with charges
					if (imp.getC_Charge_ID() != 0)
						line.setC_Charge_ID(imp.getC_Charge_ID());
					// globalqss - [2855673] - assign dimensions to lines also in case they're different
					if (imp.getC_Activity_ID() != 0)
						line.setC_Activity_ID(imp.getC_Activity_ID());
					if (imp.getC_Campaign_ID() != 0)
						line.setC_Campaign_ID(imp.getC_Campaign_ID());
					if (imp.getC_Project_ID() != 0)
						line.setC_Project_ID(imp.getC_Project_ID());
					//
					line.setQty(imp.getQtyOrdered());
					line.setPrice();
					BigDecimal price = imp.getPriceActual();

					//Validate Tax Included when using Charge
					if(imp.getC_Charge_ID() > 0 && imp.getM_Product_ID() <= 0){

						MPriceList priceList = (MPriceList) imp.getM_PriceList();
						MTax tax = (MTax) imp.getC_Tax();

						if(priceList != null && tax != null){
							if(priceList.isTaxIncluded()){
								int StdPrecision = priceList.getPricePrecision();

								BigDecimal multiplier = tax.getRate().divide(Env.ONEHUNDRED, 12, RoundingMode.HALF_UP);
								multiplier = multiplier.add(Env.ONE);
								price = price.divide(multiplier, StdPrecision, RoundingMode.HALF_UP);

								BigDecimal TaxAmt = tax.calculateTax(imp.getQtyOrdered().multiply(imp.getPriceActual()), true, StdPrecision);
								line.setTaxAmt(TaxAmt);
								line.setLineNetAmt();

							}
						}
					}

					if (price != null && Env.ZERO.compareTo(price) != 0)
						line.setPrice(price);
					if (imp.getC_Tax_ID() != 0)
						line.setC_Tax_ID(imp.getC_Tax_ID());
					else
					{
						line.setTax();
						imp.setC_Tax_ID(line.getC_Tax_ID());
					}
					BigDecimal taxAmt = imp.getTaxAmt();
					if (taxAmt != null && Env.ZERO.compareTo(taxAmt) != 0)
						line.setTaxAmt(taxAmt);
					line.saveEx();
					//
					imp.setC_InvoiceLine_ID(line.getC_InvoiceLine_ID());
					imp.setI_IsImported(true);
					imp.setProcessed(true);
					//
					if (imp.save())
						noInsertLine.getAndAdd(1);
				}
				if (invoice != null) {
					//Validate Number of Lines
					int countToImport = getLinesToImportCount(invoice);
					int importedLinesCount = getImportedLinesCount(invoice);
					if (importedLinesCount == countToImport) {
						invoice.processIt(m_docAction);
						invoice.saveEx();
					} else {
						setLinesNotProcessed(invoice);
						Object[] parameters =  {invoice.get_ID()};
						DB.executeUpdateEx("DELETE FROM C_Invoice WHERE C_Invoice_ID = ?", parameters, transactionName);
						noInvDeleted.getAndAdd(1);
					}
				}
			});

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "CreateInvoice", e);
		}
		//	Set Error to indicator to not imported
		sql = new StringBuffer ("UPDATE I_Invoice "
				+ "SET I_IsImported='N', Updated=SysDate "
				+ "WHERE I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdate(sql.toString(), null);
		addLog (0, null, new BigDecimal (no), "@Errors@");
		//
		addLog (0, null, new BigDecimal (noInsert.get()), "@C_Invoice_ID@: @Inserted@");
		addLog (0, null, new BigDecimal (noInsertLine.get()), "@C_InvoiceLine_ID@: @Inserted@");
		addLog (0, null, new BigDecimal (noInvDeleted.get()), "@C_Invoice_ID@: @Deleted@");
		return "@C_Invoice_ID@:" + noInsert.get() +" @Inserted@ @C_InvoiceLine_ID@: " + noInsertLine.get() +"@Inserted@ @C_Invoice_ID@: " + noInvDeleted.get() + " @Deleted@";
	}	//	doIt

	public int getLinesToImportCount(MInvoice invoice) {

		String whereClause = "DocumentNo = ? " +
				" AND C_BPartner_ID = ? " +
				" AND C_DocType_ID = ? ";
		int count = new Query(getCtx(), I_I_Invoice.Table_Name, whereClause, invoice.get_TrxName())
				.setParameters(invoice.getDocumentNo(), invoice.getC_BPartner_ID(), invoice.getC_DocTypeTarget_ID())
				.count();
		return count;
	}

	public int getImportedLinesCount(MInvoice invoice) {
		String whereClause = "C_Invoice_ID = ? ";
		int count = new Query(getCtx(), MInvoiceLine.Table_Name, whereClause, invoice.get_TrxName())
				.setParameters(invoice.get_ID())
				.count();
		return count;
	}

	public void setLinesNotProcessed(MInvoice invoice){
		String sql = "UPDATE I_Invoice SET I_IsImported = 'N', Processed = 'N'" +
				" WHERE DocumentNo = ?" +
				" AND C_BPartner_ID = ?" +
				" AND C_DocType_ID = ?";
		List<Object> parameters = List.of(invoice.getDocumentNo(), invoice.getC_BPartner_ID(), invoice.getC_DocTypeTarget_ID());
		DB.executeUpdateEx(sql, parameters.toArray(), invoice.get_TrxName());
	}

}	//	ImportInvoice