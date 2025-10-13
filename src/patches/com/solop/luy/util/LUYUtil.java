package com.solop.luy.util;
/**
 * Uruguay Localization Utils
 * @author Gabriel Escalona, Solop <a href="http://www.solopsoftware.com">...</a>
 */
public class LUYUtil {
    /***
     * Validate TaxID according to DGI verification.
     * @param taxId taxId
     * @return boolean
     */
    public static boolean validateTaxID(String taxId){
        boolean result = true;
        try {
            int verificationDigit = Integer.parseInt(taxId.substring(taxId.length()-1));
            String auxiliary = taxId.substring(0, taxId.length()-1);

            int factor = 2, sum = 0;
            int total = auxiliary.length()-1;

            for (int i = total; i >= 0 ; i--) {
                int digit =  Character.getNumericValue(auxiliary.charAt(i));
                sum += digit*factor;
                factor = factor==9 ? 2 : (factor+1);
            }

            int module = sum % 11;
            int calculatedDigit = 11 - module;
            if(calculatedDigit == 11) {
                calculatedDigit = 0;
            } else if(calculatedDigit == 10) {
                calculatedDigit = 1;
            }
            if (calculatedDigit != verificationDigit){
                result = false;
            }
        }
        catch (Exception e) {
            result = false;
        }
        return result;
    }
}
