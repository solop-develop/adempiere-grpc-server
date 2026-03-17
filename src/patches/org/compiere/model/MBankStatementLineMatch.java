package org.compiere.model;
import org.adempiere.core.domains.models.X_C_BankStatementLineMatch;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MBankStatementLineMatch extends X_C_BankStatementLineMatch {


    public MBankStatementLineMatch(Properties ctx, int C_BankStatementLineMatch_ID, String trxName) {
        super(ctx, C_BankStatementLineMatch_ID, trxName);
    }
    public MBankStatementLineMatch(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }
    /**
     * 	Before Save
     *	@param newRecord new
     *	@return true
     */
    protected boolean beforeSave (boolean newRecord)
    {
        if (getC_BankStatementLine_ID() > 0 && getC_BankStatement_ID() <= 0) {
            MBankStatementLine statementLine = (MBankStatementLine) getC_BankStatementLine();
            setC_BankStatement_ID(statementLine.getC_BankStatement_ID());
        }

        return true;
    }	//	beforeSave


}