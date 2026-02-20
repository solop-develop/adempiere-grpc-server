package org.solop.util;

import org.compiere.model.PO;

/**
 * Util class to set DateAcct equal to Document Date if necessary
 *
 * @author Gabriel Escalona
 */
public class DocumentDateUtil {
    static final String DateAcctColumnName = "DateAcct";
    /***
     * Update DateAcct sets same as dateDocColumn
     * only if dateDocColumn was changed and DateAcct was not Changed
     * @param document the document to update DateAcct
     * @param dateDocColumnName name of the column used as DateDoc in the Document (DateInvoiced, DateOrdered...)
     */
    public static void updateDateAcct(PO document, String dateDocColumnName) {

        if (document.is_new() || (document.is_ValueChanged(dateDocColumnName)
            && !document.is_ValueChanged(DateAcctColumnName))) {
            document.set_ValueOfColumn(DateAcctColumnName, document.get_Value(dateDocColumnName));
        }
    }
}
