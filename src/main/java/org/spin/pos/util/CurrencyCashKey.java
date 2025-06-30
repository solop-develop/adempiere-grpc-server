package org.spin.pos.util;

import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.util.Objects;

public class CurrencyCashKey implements Comparable<CurrencyCashKey> {

    public static CurrencyCashKey newInstance(int currencyId, String tenderType) {
        return new CurrencyCashKey(currencyId, tenderType);
    }

    public CurrencyCashKey(int currencyId, String tenderType) {
        this.currencyId = currencyId;
        this.tenderType = tenderType;
    }
    private int currencyId;
    private String tenderType;

    public int getCurrencyId() {
        return currencyId;
    }

    public String getTenderType() {
        return tenderType;
    }

    private String getValidType() {
        if(!Util.isEmpty(getTenderType(), true) && (getTenderType().equals(MPayment.TENDERTYPE_CreditMemo) || getTenderType().equals("G"))) {
            return getTenderType();
        }
        return "";
    }

    public String getValidDisplayValue() {
        String validValue = getValidType();
        String validDisplayValue = "";
        if(!Util.isEmpty(validValue)) {
            validDisplayValue = MRefList.getListName(Env.getCtx(), MPayment.TENDERTYPE_AD_Reference_ID, validValue);
        }
        if(validDisplayValue == null) {
            validDisplayValue = "";
        }
        return validDisplayValue;
    }

    @Override
    public String toString() {
        return currencyId + "|" + getValidType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyCashKey that = (CurrencyCashKey) o;
        return currencyId == that.currencyId &&
                Objects.equals(tenderType, that.tenderType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyId, tenderType);
    }
    @Override
    public int compareTo(CurrencyCashKey other) {
        int result = Integer.compare(this.currencyId, other.currencyId);
        if (result == 0) {
            result = this.tenderType.compareTo(other.tenderType);
        }
        return result;
    }
}
