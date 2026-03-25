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
package com.solop.core.process;

import org.adempiere.core.domains.models.X_T_InvoiceGL;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MDocType;
import org.compiere.model.MFactAcct;
import org.compiere.model.MGLCategory;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalBatch;
import org.compiere.model.MJournalLine;
import org.compiere.model.MOrg;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogMgt;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

public class UnrealizedExchangeDiff extends SvrProcess
{
    /**	Mandatory Acct Schema			*/
    private int				p_C_AcctSchema_ID = 0;
    /** Mandatory Conversion Type		*/
    private int				p_C_ConversionTypeReval_ID = 0;
    /** Revaluation Date				*/
    private Timestamp		p_DateReval = null;
    /** Only AP/AR Transactions			*/
    private String			p_APAR = "A";
    private static String	ONLY_AP = "P";
    private static String	ONLY_AR = "R";
    /** Report all Currencies			*/
    private boolean			p_IsAllCurrencies = false;
    /** Optional Invoice Currency		*/
    private int				p_C_Currency_ID = 0;
    /** GL Document Type				*/
    private int				p_C_DocTypeReval_ID = 0;
    private int				p_AD_Org_ID = 0;

    private MJournalBatch batch = null;

    /**
     *  Prepare - e.g., get Parameters.
     */
    protected void prepare()
    {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++)
        {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null)
                ;
            else if (name.equals("C_AcctSchema_ID"))
                p_C_AcctSchema_ID = para[i].getParameterAsInt();
            else if (name.equals("AD_Org_ID"))
                p_AD_Org_ID = para[i].getParameterAsInt();
            else if (name.equals("C_ConversionTypeReval_ID"))
                p_C_ConversionTypeReval_ID = para[i].getParameterAsInt();
            else if (name.equals("DateReval"))
                p_DateReval = (Timestamp)para[i].getParameter();
            else if (name.equals("APAR"))
                p_APAR = (String)para[i].getParameter();
            else if (name.equals("IsAllCurrencies"))
                p_IsAllCurrencies = "Y".equals((String)para[i].getParameter());
            else if (name.equals("C_Currency_ID"))
                p_C_Currency_ID = para[i].getParameterAsInt();
            else if (name.equals("C_DocTypeReval_ID"))
                p_C_DocTypeReval_ID = para[i].getParameterAsInt();
            else
                log.log(Level.SEVERE, "Unknown Parameter: " + name);
        }
    }	//	prepare

    /**
     * 	Process
     *	@return info
     *	@throws Exception
     */
    protected String doIt () throws Exception
    {
        if (p_IsAllCurrencies)
            p_C_Currency_ID = 0;
        log.info("C_AcctSchema_ID=" + p_C_AcctSchema_ID
                + ",C_ConversionTypeReval_ID=" + p_C_ConversionTypeReval_ID
                + ",DateReval=" + p_DateReval
                + ", APAR=" + p_APAR
                + ", IsAllCurrencies=" + p_IsAllCurrencies
                + ",C_Currency_ID=" + p_C_Currency_ID
                + ", C_DocType_ID=" + p_C_DocTypeReval_ID);

        //	Parameter
        if (p_DateReval == null)
            p_DateReval = new Timestamp(System.currentTimeMillis());

        //	Delete - just to be sure
        String sql = "DELETE T_InvoiceGL WHERE AD_PInstance_ID=" + getAD_PInstance_ID();
        int no = DB.executeUpdate(sql, get_TrxName());
        if (no > 0)
            log.info("Deleted #" + no);

        //	Insert Trx
        String dateStr = DB.TO_DATE(p_DateReval, true);
        sql = "INSERT INTO T_InvoiceGL (AD_Client_ID, AD_Org_ID, IsActive, Created,CreatedBy, Updated,UpdatedBy,"
                + " AD_PInstance_ID, C_Invoice_ID, GrandTotal, OpenAmt, "
                + " Fact_Acct_ID, AmtSourceBalance, AmtAcctBalance, "
                + " AmtRevalDr, AmtRevalCr, C_DocTypeReval_ID, IsAllCurrencies, "
                + " DateReval, C_ConversionTypeReval_ID, AmtRevalDrDiff, AmtRevalCrDiff, APAR, C_SalesRegion_ID) "
                + "SELECT i.AD_Client_ID, i.AD_Org_ID, i.IsActive, i.Created,i.CreatedBy, i.Updated,i.UpdatedBy,"
                +  getAD_PInstance_ID() + ", i.C_Invoice_ID, i.GrandTotal, SP028C_InvoiceOpenToDate(i.C_Invoice_ID, 0, " + dateStr + "::timestamp), "
                + " fa.Fact_Acct_ID, fa.AmtSourceDr-fa.AmtSourceCr, fa.AmtAcctDr-fa.AmtAcctCr, "
                + " currencyConvert(fa.AmtSourceDr, i.C_Currency_ID, a.C_Currency_ID, " + dateStr + ", " + p_C_ConversionTypeReval_ID + ", i.AD_Client_ID, i.AD_Org_ID),"
                + " currencyConvert(fa.AmtSourceCr, i.C_Currency_ID, a.C_Currency_ID, " + dateStr + ", " + p_C_ConversionTypeReval_ID + ", i.AD_Client_ID, i.AD_Org_ID),"
                + (p_C_DocTypeReval_ID==0 ? "NULL" : String.valueOf(p_C_DocTypeReval_ID)) + ", "
                + (p_IsAllCurrencies ? "'Y'," : "'N',")
                + dateStr + ", " + p_C_ConversionTypeReval_ID + ", 0, 0, '" + p_APAR + "', fa.C_SalesRegion_ID "
                + "FROM C_Invoice_v i"
                + " INNER JOIN Fact_Acct fa ON (fa.AD_Table_ID=318 AND fa.Record_ID=i.C_Invoice_ID"
                + " AND (i.GrandTotal=fa.AmtSourceDr OR i.GrandTotal=fa.AmtSourceCr))"
                + " INNER JOIN C_AcctSchema a ON (fa.C_AcctSchema_ID=a.C_AcctSchema_ID) "
                + " INNER JOIN C_DocType doc ON (i.C_DocTypeTarget_ID = doc.C_DocType_ID ) "
                + "WHERE SP028C_InvoiceOpenToDate(i.C_Invoice_ID, 0, " + dateStr + "::timestamp) <> 0"
                + " AND EXISTS (SELECT * FROM C_ElementValue ev "
                + "WHERE ev.C_ElementValue_ID=fa.Account_ID AND (ev.AccountType='A' OR ev.AccountType='L'))"
                + " AND fa.C_AcctSchema_ID=" + p_C_AcctSchema_ID
                + " AND i.ad_org_id = " + p_AD_Org_ID
                + " AND fa.m_product_id is null"
                + " AND doc.DocBaseType <> 'INV'"
                + " AND i.dateinvoiced <= " + dateStr;
        if (!p_IsAllCurrencies)
            sql += " AND i.C_Currency_ID<>a.C_Currency_ID";
        if (ONLY_AR.equals(p_APAR))
            sql += " AND i.IsSOTrx='Y'";
        else if (ONLY_AP.equals(p_APAR))
            sql += " AND i.IsSOTrx='N'";
        if (!p_IsAllCurrencies && p_C_Currency_ID != 0)
            sql += " AND i.C_Currency_ID=" + p_C_Currency_ID;

        no = DB.executeUpdate(sql, get_TrxName());
        if (no != 0)
            log.info("Inserted #" + no);
        else if (CLogMgt.isLevelFiner())
            log.warning("Inserted #" + no + " - " + sql);
        else
            log.warning("Inserted #" + no);

        //	Calculate Difference
        sql = "UPDATE T_InvoiceGL gl "
                + "SET (AmtRevalDrDiff,AmtRevalCrDiff)="
                + "(SELECT gl.AmtRevalDr-fa.AmtAcctDr, gl.AmtRevalCr-fa.AmtAcctCr "
                + "FROM Fact_Acct fa "
                + "WHERE gl.Fact_Acct_ID=fa.Fact_Acct_ID) "
                + "WHERE AD_PInstance_ID=" + getAD_PInstance_ID();
        int noT = DB.executeUpdate(sql, get_TrxName());
        if (noT > 0)
            log.config("Difference #" + noT);

        //	Percentage
        sql = "UPDATE T_InvoiceGL SET Percent = 100 "
                + "WHERE GrandTotal=OpenAmt AND AD_PInstance_ID=" + getAD_PInstance_ID();
        no = DB.executeUpdate(sql, get_TrxName());
        if (no > 0)
            log.info("Not Paid #" + no);

        sql = "UPDATE T_InvoiceGL SET Percent = ROUND(OpenAmt*100/GrandTotal,6) "
                + "WHERE GrandTotal<>OpenAmt AND GrandTotal <> 0 AND AD_PInstance_ID=" + getAD_PInstance_ID();
        no = DB.executeUpdate(sql, get_TrxName());
        if (no > 0)
            log.info("Partial Paid #" + no);

        sql = "UPDATE T_InvoiceGL SET AmtRevalDr = AmtRevalDr * Percent/100,"
                + " AmtRevalCr = AmtRevalCr * Percent/100,"
                + " AmtRevalDrDiff = AmtRevalDrDiff * Percent/100,"
                + " AmtRevalCrDiff = AmtRevalCrDiff * Percent/100 "
                + "WHERE Percent <> 100 AND AD_PInstance_ID=" + getAD_PInstance_ID();
        no = DB.executeUpdate(sql, get_TrxName());
        if (no > 0)
            log.config("Partial Calc #" + no);

        //	Create Document
        String info = "";
        if (p_C_DocTypeReval_ID != 0)
        {
            info = createGLJournal();
        }

        if(batch != null)
            rePost();

        return "#" + noT + info;
    }	//	doIt

    /***
     * Method that recounts the journals.
     * This is required due to the change in the source amounts of the lines after the journal is completed.
     */
    private void rePost() {

        MJournal[] journals = batch.getJournals(true);

        for(MJournal j : journals){
            if (j.getDocStatus().equals(DocAction.STATUS_Completed)) {
                String ignoreError = DocumentEngine.postImmediate(j.getCtx(),
                        j.getAD_Client_ID(), j.get_Table_ID(),
                        j.getGL_Journal_ID(), true,
                        j.get_TrxName());
                if(ignoreError != null)
                    log.warning(ignoreError);
            }
        }
    }

    /**
     * 	Create GL Journal
     * 	@return document info
     */
    private String createGLJournal()
    {
        String message = "";

        final String whereClause = "AD_PInstance_ID=?";
        List <X_T_InvoiceGL> list = new Query(getCtx(), X_T_InvoiceGL.Table_Name, whereClause, get_TrxName())
                .setParameters(getAD_PInstance_ID())
                .setOrderBy("AD_Org_ID")
                .list();

        if (list.size() == 0)
            return " - No Records found";

        MAcctSchema as = MAcctSchema.get(getCtx(), p_C_AcctSchema_ID);
        MAcctSchemaDefault asDefaultAccts = MAcctSchemaDefault.get(getCtx(), p_C_AcctSchema_ID);
        MGLCategory cat = MGLCategory.getDefaultSystem(getCtx());
        if (cat == null)
        {
            MDocType docType = MDocType.get(getCtx(), p_C_DocTypeReval_ID);
            cat = MGLCategory.get(getCtx(), docType.getGL_Category_ID());
        }
        //
        batch = new MJournalBatch(getCtx(), 0, get_TrxName());
        batch.setDescription (getName());
        batch.setC_DocType_ID(p_C_DocTypeReval_ID);
        batch.setDateDoc(new Timestamp(System.currentTimeMillis()));
        batch.setDateAcct(p_DateReval);
        batch.setC_Currency_ID(p_C_Currency_ID);
        batch.set_ValueOfColumn("IsYearEndClosing", true);
        if (!batch.save())
            return " - Could not create Batch";
        //
        MJournal journal = null;
        BigDecimal drTotal = Env.ZERO;
        BigDecimal crTotal = Env.ZERO;
        int AD_Org_ID = 0;
        for (int i = 0; i < list.size(); i++)
        {
            X_T_InvoiceGL gl = list.get(i);
            int regionID = gl.get_ValueAsInt("C_SalesRegion_ID");
            if (gl.getAmtRevalDrDiff().signum() == 0 && gl.getAmtRevalCrDiff().signum() == 0)
                continue;
            MInvoice invoice = new MInvoice(getCtx(), gl.getC_Invoice_ID(), null);
            if (invoice.getC_Currency_ID() == as.getC_Currency_ID())
                continue;
            //
            if (journal == null)
            {
                journal = new MJournal (batch);
                journal.setC_AcctSchema_ID (as.getC_AcctSchema_ID());
                journal.setC_Currency_ID(p_C_Currency_ID);//Openup. Nicolas Sarlabos. 10/09/2020. #14650.
                journal.setC_ConversionType_ID(p_C_ConversionTypeReval_ID);
                MOrg org = MOrg.get(getCtx(), gl.getAD_Org_ID());
                journal.setDescription (getName() + " - " + org.getName());
                journal.setGL_Category_ID (cat.getGL_Category_ID());
                if (!journal.save())
                    return " - Could not create Journal";
            }
            //
            MJournalLine line = new MJournalLine(journal);
            line.setLine((i+1) * 10);
            line.setDescription(invoice.getSummary());
            //
            MFactAcct fa = new MFactAcct (getCtx(), gl.getFact_Acct_ID(), null);
            MAccount account = MAccount.get(fa);
            line.setAccount_ID(account.get_ID());
            BigDecimal dr = gl.getAmtRevalDrDiff();
            BigDecimal cr = gl.getAmtRevalCrDiff();
            drTotal = drTotal.add(dr);
            crTotal = crTotal.add(cr);
            line.setAmtSourceDr (dr);
            line.setAmtAcctDr (dr);
            line.setAmtSourceCr (cr);
            line.setAmtAcctCr (cr);
            line.setC_SalesRegion_ID(regionID);
            line.setC_ValidCombination_ID(MAccount.get(fa));
            line.saveEx();
            //
            if (AD_Org_ID == 0)		//	invoice org id
                AD_Org_ID = gl.getAD_Org_ID();
            //	Change in Org
            if (AD_Org_ID != gl.getAD_Org_ID())
            {
                createBalancing (asDefaultAccts, journal, drTotal, crTotal, AD_Org_ID, (i+1) * 10);
                //
                AD_Org_ID = gl.getAD_Org_ID();
                drTotal = Env.ZERO;
                crTotal = Env.ZERO;
                journal = null;
            }
        }
        createBalancing(asDefaultAccts, journal, drTotal, crTotal, AD_Org_ID, (list.size() + 1) * 10);

        if (!batch.processIt(MJournalBatch.ACTION_Complete)) {
            message = batch.getProcessMsg();
            throw new AdempiereException(message);
        }
        if (MSysConfig.getBooleanValue("UY_REVERSE_INVOICE_NGL", false, Env.getAD_Client_ID(Env.getCtx()))){
            createReversalExchangeDiff(batch);
        }
        DB.executeUpdateEx("update gl_journalline set amtsourcecr = 0, amtsourcedr = 0 where gl_journal_id = " + journal.get_ID(), get_TrxName());

        return " - " + batch.getDocumentNo() + " #" + list.size();
    }	//	createGLJournal

    /**
     * 	Create Balancing Entry
     *	@param asDefaultAccts acct schema default accounts
     *	@param journal journal
     *	@param drTotal dr
     *	@param crTotal cr
     *	@param AD_Org_ID org
     *	@param lineNo base line no
     */
    private void createBalancing (MAcctSchemaDefault asDefaultAccts, MJournal journal,
                                  BigDecimal drTotal, BigDecimal crTotal, int AD_Org_ID, int lineNo)
    {
        if (journal == null)
            throw new IllegalArgumentException("Jornal is null");
        //		CR Entry = Gain
        if (drTotal.signum() != 0)
        {
            MJournalLine line = new MJournalLine(journal);
            line.setLine(lineNo+1);
            MAccount base = MAccount.get(getCtx(), asDefaultAccts.getUnrealizedGain_Acct());
            MAccount acct = MAccount.get(getCtx(), asDefaultAccts.getAD_Client_ID(), AD_Org_ID,
                    asDefaultAccts.getC_AcctSchema_ID(), base.getAccount_ID(), base.getC_SubAcct_ID(),
                    base.getM_Product_ID(), base.getC_BPartner_ID(), base.getAD_OrgTrx_ID(),
                    base.getC_LocFrom_ID(), base.getC_LocTo_ID(), base.getC_SalesRegion_ID(),
                    base.getC_Project_ID(), base.getC_Campaign_ID(), base.getC_Activity_ID(),
                    base.getUser1_ID(), base.getUser2_ID() , base.getUser3_ID(), base.getUser4_ID(),
                    base.getUserElement1_ID(), base.getUserElement2_ID(), null);
            line.setDescription(Msg.getElement(getCtx(), "UnrealizedGain_Acct"));
            line.setC_ValidCombination_ID(acct.getC_ValidCombination_ID());
            line.setAmtSourceCr (drTotal);
            line.setAmtAcctCr (drTotal);
            line.saveEx();
        }
        //	DR Entry = Loss
        if (crTotal.signum() != 0)
        {
            MJournalLine line = new MJournalLine(journal);
            line.setLine(lineNo+2);
            MAccount base = MAccount.get(getCtx(), asDefaultAccts.getUnrealizedLoss_Acct());
            MAccount acct = MAccount.get(getCtx(), asDefaultAccts.getAD_Client_ID(), AD_Org_ID,
                    asDefaultAccts.getC_AcctSchema_ID(), base.getAccount_ID(), base.getC_SubAcct_ID(),
                    base.getM_Product_ID(), base.getC_BPartner_ID(), base.getAD_OrgTrx_ID(),
                    base.getC_LocFrom_ID(), base.getC_LocTo_ID(), base.getC_SalesRegion_ID(),
                    base.getC_Project_ID(), base.getC_Campaign_ID(), base.getC_Activity_ID(),
                    base.getUser1_ID(), base.getUser2_ID() , base.getUser3_ID(), base.getUser4_ID(),
                    base.getUserElement1_ID(), base.getUserElement2_ID(), null);
            line.setDescription(Msg.getElement(getCtx(), "UnrealizedLoss_Acct"));
            line.setC_ValidCombination_ID(acct.getC_ValidCombination_ID());
            line.setAmtSourceDr (crTotal);
            line.setAmtAcctDr (crTotal);
            line.saveEx();
        }
    }	//	createBalancing

    private void createReversalExchangeDiff(MJournalBatch batch){
        String message = "";

        try {

            MJournalBatch reverse = new MJournalBatch(batch);
            reverse.set_ValueNoCheck ("C_Period_ID", null);
            reverse.setDateDoc(batch.getDateDoc());
            reverse.setDateAcct(TimeUtil.addDays(batch.getDateAcct(), 1));
            reverse.addDescription("** (->" + batch.getDocumentNo() + ") **");
            reverse.setReversal_ID(batch.get_ID());
            reverse.setControlAmt(batch.getControlAmt().negate());
            MDocType docType = MDocType.get(getCtx(), batch.getC_DocType_ID());
            if (docType.isCopyDocNoOnReversal()) {
                reverse.setDocumentNo(batch.getDocumentNo() + Msg.getMsg(reverse.getCtx(), "^"));
            }
            reverse.saveEx();

            MJournal[] journals = batch.getJournals(true);
            for (int i = 0; i < journals.length; i++) {

                MJournal journal = journals[i];

                MJournal revJournal = new MJournal(journal);
                revJournal.set_ValueNoCheck ("C_Period_ID", null);
                revJournal.setGL_JournalBatch_ID(reverse.get_ID());
                revJournal.setDateDoc(new Timestamp(System.currentTimeMillis()));
                revJournal.setDateAcct(TimeUtil.addDays(journal.getDateAcct(), 1));

                String description = revJournal.getDescription();
                if (description == null)
                    description = "** " + journal.getDocumentNo() + " **";
                else
                    description += " ** " + journal.getDocumentNo() + " **";
                revJournal.setDescription(description);

                revJournal.saveEx();
                revJournal.copyLinesFrom(journal, revJournal.getDateAcct(), 'R');

                revJournal.setProcessed(false);
                revJournal.setDocStatus(MJournal.DOCSTATUS_Drafted);
                revJournal.setDocAction(MJournal.DOCACTION_Complete);

                DB.executeUpdateEx("update gl_journalline set amtsourcecr = 0, amtsourcedr = 0 where gl_journal_id = " + revJournal.get_ID(), get_TrxName());

                revJournal.getCtx().setProperty("NoRequireAttachment", "x");
                if (!revJournal.processIt(MJournalBatch.ACTION_Complete)) {
                    message = revJournal.getProcessMsg();
                    throw new AdempiereException(message);
                }
                revJournal.getCtx().setProperty("NoRequireAttachment", "");

                revJournal.saveEx();
            }
            reverse.setDocAction(MJournalBatch.DOCACTION_None);
            reverse.setDocStatus(MJournalBatch.DOCSTATUS_Completed);
            reverse.setProcessed(true);
            reverse.saveEx();
            reverse.addDescription("** (" + reverse.getDocumentNo() + "<-) **");
            reverse.setReversal_ID(batch.getGL_JournalBatch_ID());
            reverse.setDocAction(MJournalBatch.DOCACTION_None);
        } catch (Exception ex) {
            throw new AdempiereException(ex.getMessage());
        }
    }
}	//	InvoiceNGL
