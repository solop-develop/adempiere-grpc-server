package org.openup.core.utils;

import org.spin.util.impexp.BankStatementHandler;
import org.spin.util.impexp.BankTransactionAbstract;

/**
 * Created by nicolas on 05/02/2020.
 */
public class HSBC_BankStatementLoader extends BankStatementHandler {

    @Override
    protected BankTransactionAbstract getBankTransactionInstance() {
        return new HSBC_BankTransaction();
    }

}
