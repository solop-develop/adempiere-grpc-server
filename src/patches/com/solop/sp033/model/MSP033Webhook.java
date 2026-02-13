package com.solop.sp033.model;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Util;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class MSP033Webhook extends X_SP033_Webhook {


    public MSP033Webhook(Properties ctx, int SP033_Webhook_ID, String trxName) {
        super(ctx, SP033_Webhook_ID, trxName);
    }
    public MSP033Webhook(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }


    @Override
    protected boolean beforeSave(boolean newRecord) {
        if (isSP033_IsTest() && Util.isEmpty(getSP033_TestPayloadURL(), true)) {
            throw new AdempiereException("@SP033_TestPayloadURL@ @NotFound@");
        }
        return super.beforeSave(newRecord);
    }





}