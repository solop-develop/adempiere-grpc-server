package org.compiere.model;
import org.adempiere.core.domains.models.X_AD_DistributionListMember;
import org.adempiere.exceptions.AdempiereException;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MADDistributionListMember extends X_AD_DistributionListMember {


    public MADDistributionListMember(Properties ctx, int AD_DistributionListMember_ID, String trxName) {
        super(ctx, AD_DistributionListMember_ID, trxName);
    }
    public MADDistributionListMember(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        if (getMemberType().equals(MEMBERTYPE_List)){
            setAD_Role_ID(-1);
            setAD_User_ID(-1);
        } else if (getMemberType().equals(MEMBERTYPE_Role)) {
            setRef_DistributionList_ID(-1);
            setAD_User_ID(-1);
        } else if (getMemberType().equals(MEMBERTYPE_User)) {
            setRef_DistributionList_ID(-1);
            setAD_Role_ID(-1);
        }
        if (getRef_DistributionList_ID() <= 0 && getAD_User_ID() <= 0 && getAD_Role_ID() <= 0) {
            throw new AdempiereException("@AD_User_ID@ @AND@ @Ref_DistributionList_ID@ @AND@ @AD_Role_ID@ @NotFound@");
        }


        return super.beforeSave(newRecord);
    }

}