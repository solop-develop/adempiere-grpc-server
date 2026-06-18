package org.compiere.model;
import org.adempiere.core.domains.models.X_AD_LandingApp;
import org.compiere.util.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MLandingApp extends X_AD_LandingApp {


    public MLandingApp(Properties ctx, int AD_LandingApp_ID, String trxName) {
        super(ctx, AD_LandingApp_ID, trxName);
    }
    public MLandingApp(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {

        String route = getRoute();
        String url = getURL();
        boolean hasRoute = !Util.isEmpty(route, true);
        boolean hasURL = !Util.isEmpty(url, true);

        // Exclusivity rule: Route OR URL, never both, but at least one
        if (hasRoute && hasURL) {
            log.saveError("Error", "Complete Route (internal app) or URL (external app), but not both");
            return false;
        }
        if (!hasRoute && !hasURL) {
            log.saveError("FillMandatory", "Route or URL");
            return false;
        }

        // Internal app: Route must start with '/'
        if (hasRoute) {
            if (!route.trim().startsWith("/")) {
                log.saveError("Error", "Route must start with '/'");
                return false;
            }
        }

        // External app: URL must be a valid http/https URL
        if (hasURL) {
            String trimmed = url.trim();
            try {
                URL parsed = new URL(trimmed);
                String protocol = parsed.getProtocol();
                if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                    log.saveError("Error", "URL must use http or https");
                    return false;
                }
            } catch (MalformedURLException e) {
                log.saveError("Error", "Invalid URL: " + trimmed);
                return false;
            }
        }

        return super.beforeSave(newRecord);
    }

}