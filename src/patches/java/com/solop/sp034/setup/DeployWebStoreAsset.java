package com.solop.sp034.setup;


import com.solop.sp034.support.WebStoreAssetGoogleDrive;
import com.solop.sp034.util.Changes;
import org.adempiere.core.domains.models.I_AD_AppRegistration;
import org.adempiere.core.domains.models.I_AD_AppRegistration_Para;
import org.adempiere.core.domains.models.I_AD_AppSupport;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.spin.model.MADAppRegistration;
import org.spin.model.MADAppRegistrationPara;
import org.spin.model.MADAppSupport;
import org.spin.util.ISetupDefinition;

import java.util.Properties;

public class DeployWebStoreAsset implements ISetupDefinition {

    private static final String DESCRIPTION = "(*Created from Setup Automatically*)";
    private static final String APPLICATION_TYPE = Changes.WebStoreAsset_ApplicationType;

    @Override
    public String doIt(Properties context, String transactionName) {
        //	App registration
        createDefaultWebStoreAsset(context, transactionName);
        return "@AD_SetupDefinition_ID@ @Ok@";
    }

    /**
     * Create notifiers as app registration
     * @param context
     * @param transactionName
     */
    private void createDefaultWebStoreAsset(Properties context, String transactionName) {
        MADAppRegistration webStoreAsset = new Query(context, I_AD_AppRegistration.Table_Name, "EXISTS(SELECT 1 FROM AD_AppSupport s "
                + "WHERE s.AD_AppSupport_ID = AD_AppRegistration.AD_AppSupport_ID "
                + "AND s.Classname = ?)", transactionName)
                .setParameters(WebStoreAssetGoogleDrive.class.getName())
                .setClient_ID()
                .first();
        if(webStoreAsset == null
                || webStoreAsset.getAD_AppRegistration_ID() <= 0) {
            MADAppSupport queueSupport = new Query(context, I_AD_AppSupport.Table_Name, "Classname = ?", transactionName)
                    .setParameters(WebStoreAssetGoogleDrive.class.getName())
                    .first();
            if(queueSupport == null
                    || queueSupport.getAD_AppSupport_ID() <= 0) {
                throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
            }
            webStoreAsset = new MADAppRegistration(context, 0, transactionName);
            webStoreAsset.setValue("WebStoreAsset_Google_Drive");
            webStoreAsset.setApplicationType(APPLICATION_TYPE);
            webStoreAsset.setAD_AppSupport_ID(queueSupport.getAD_AppSupport_ID());
            webStoreAsset.setName("Web Store Asset Handler for Google Drive");
            webStoreAsset.setDescription(DESCRIPTION);
            webStoreAsset.setVersionNo("1.0");
            webStoreAsset.setHost("https://n8n.coord.solopcloud.com/webhook/fbd3d2b8-f440-4449-8429-64af0032eae9");
            webStoreAsset.setPort(0);
            webStoreAsset.saveEx();
            //  Set Parameters
            new Query(context, I_AD_AppRegistration_Para.Table_Name, I_AD_AppRegistration_Para.COLUMNNAME_AD_AppRegistration_ID + " = ?", transactionName).setParameters(webStoreAsset.getAD_AppRegistration_ID()).getIDsAsList().forEach(parameterId -> {
                MADAppRegistrationPara parameter = new MADAppRegistrationPara(context, parameterId, transactionName);
                if(parameter.getParameterName().equals(WebStoreAssetGoogleDrive.TOKEN_PARAMETER)) {
                    parameter.setParameterValue("");
                } else if(parameter.getParameterName().equals(WebStoreAssetGoogleDrive.PROVIDER_PARAMETER)) {
                    parameter.setParameterValue("google_drive");
                } else if(parameter.getParameterName().equals(WebStoreAssetGoogleDrive.ROOT_FOLDER_PARAMETER)) {
                    parameter.setParameterValue("");
                }
                parameter.saveEx();
            });
        }
    }

}
