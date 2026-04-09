package org.openup.core.utils;

import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.util.impexp.BankTransactionAbstract;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Created by nicolas on 05/02/2020.
 */
public class HSBC_BankTransaction extends BankTransactionAbstract {

    /**	Ignore it line because is a first line as head */
    //public static final String HEAD_REFERENCE_FIRST_LINE_FLAG = "Fecha	Referencia	 Cod";
    /**	Value Date [dddMMyyyy]	*/
    public static final String LINE_TRANSACTION_Date = "TrxDate";
    /**	Transaction type Transaction type (description)	*/
    public static final String LINE_TRANSACTION_Type = "Type";
    /**	Sequence number [35x] Sequential number of transaction on account	*/
    public static final String LINE_TRANSACTION_ReferenceNo = "ReferenceNo";
    /**	Description of transaction	*/
    public static final String LINE_TRANSACTION_Description = "Description";
    /**	Amount	*/
    public static final String LINE_TRANSACTION_Amount = "Amount";
    /**	Memo	*/
    public static final String LINE_TRANSACTION_Memo = "Memo";
    /**	Start Column Index	*/
    private static final char START_CHAR_VALUE = ';';
    /**	Is a transaction	*/
    private boolean isTransaction = false;

    /**	Debt Constant	*/
    public static final String DEBT = "DR";
    /**	Credit Constant	*/
    public static final String CREDIT = "CR";

    /**
     * Parse Line
     * @param line
     */
    public void parseLine(String line) throws Exception {
        if(Util.isEmpty(line)) {
            return;
        }
        /*if(line.startsWith(HEAD_REFERENCE_FIRST_LINE_FLAG)
                || line.contains(HEAD_BEGIN_BALANCE_FLAG)) {
            isTransaction = false;
            return;
        }*/
        //	Validate
        line = processValue(line);
        if(Util.isEmpty(line)) {
            return;
        }
        //	Replace bad characters
        line = line.replaceAll("\"", "");

        String[] columns = line.split(String.valueOf(START_CHAR_VALUE), -1);

        // Date: col[3], Format dd.MM.yy
        if (columns.length > 3 && !Util.isEmpty(columns[3])) {
            addValue(LINE_TRANSACTION_Date, getDate("dd.MM.yy", columns[3].trim()));
        }

        // Reference: col[4]
        if (columns.length > 4 && !Util.isEmpty(columns[4])) {
            addValue(LINE_TRANSACTION_ReferenceNo, columns[4].trim());
        }

        // Description: col[5]
        if (columns.length > 5 && !Util.isEmpty(columns[5])) {
            addValue(LINE_TRANSACTION_Description, columns[5].trim());
        }

        // Memo: col[6]
        if (columns.length > 6 && !Util.isEmpty(columns[6])) {
            addValue(LINE_TRANSACTION_Memo, columns[6].trim());
        }

        // Amount: col[10]
        if (columns.length > 10 && !Util.isEmpty(columns[10])) {
            String montoStr = columns[10].replaceAll("\\+", "").trim();
            BigDecimal amount = getNumber('.', "#,###,###,###,###,###.##", montoStr);
            addValue(LINE_TRANSACTION_Amount, amount);

            if (amount != null && amount.compareTo(Env.ZERO) < 0) {
                addValue(LINE_TRANSACTION_Type, DEBT);
            } else if (amount != null) {
                addValue(LINE_TRANSACTION_Type, CREDIT);
            }
        }

        isTransaction = true;
    }

    /**
     * Get Bank Transaction Date
     * @return
     */
    public Timestamp getTrxDate() {
        return getDate(LINE_TRANSACTION_Date);
    }

    /**
     * Get Amount of transaction
     * @return
     */
    public BigDecimal getAmount() {
        return getNumber(LINE_TRANSACTION_Amount);
    }

    /**
     * Get Payee Account
     * @return
     */
    public String getPayeeAccountNo() {
        return null;
    }

    /**
     * Get Check Numbers
     * @return
     */
    public String getCheckNo() {
        return getString(LINE_TRANSACTION_ReferenceNo);
    }

    /**
     * Process or change value for import
     * you can implement it method for replace special characters
     * @param value
     * @return
     */
    protected String processValue(String value) {
        return value;
    }

    @Override
    public boolean isEndTransactionLine(String line) {
        return true;
    }

    @Override
    public boolean isCompleteData() {
        return isTransaction;
    }

    @Override
    public String getCurrency() {
        return null;
    }
    @Override
    public Timestamp getValueDate() {
        return getDate(LINE_TRANSACTION_Date);
    }

    @Override
    public Timestamp getStatementDate() {
        return getDate(LINE_TRANSACTION_Date);
    }

    @Override
    public String getReferenceNo() {
        return getString(LINE_TRANSACTION_ReferenceNo);
    }

    @Override
    public String getPayeeName() {
        return null;
    }

    @Override
    public String getPayeeDescription() { return getString(LINE_TRANSACTION_Description); }

    @Override
    public String getMemo() {
        return getString(LINE_TRANSACTION_Memo);
    }

    @Override
    public String getTrxType() {
        return null;
    }

    @Override
    public String getTrxCode() {
        return null;
    }







}
