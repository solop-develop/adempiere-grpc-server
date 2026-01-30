package com.solop.sp013.luy.support.documents.v2;

import com.solop.sp013.luy.cfe.dto.invoicy.v2.CFEInvoiCyType;
import com.solop.sp013.luy.support.documents.ICFEDocument;

/**
 * Document Contract Version 2
 * Uses CFEInvoiCyType for the new provider API structure.
 *
 * @author Gabriel Escalona
 */
public interface ICFEDocument_v2 extends ICFEDocument {

    /**
     * Get the converted document as v2 type
     * @return CFEInvoiCyType
     */
    CFEInvoiCyType getConvertedDocument_v2();
}
