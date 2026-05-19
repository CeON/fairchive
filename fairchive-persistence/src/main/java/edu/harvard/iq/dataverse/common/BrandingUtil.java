package edu.harvard.iq.dataverse.common;

import javax.mail.internet.InternetAddress;

import static edu.harvard.iq.dataverse.common.BundleUtil.getCurrentLocale;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;

import java.util.Locale;

public class BrandingUtil {

    public static String getInstallationBrandName(final String rootDataverseName) {
    	
        return rootDataverseName;
    }

    public static String getSupportTeamName(final InternetAddress systemAddress, 
    		final String rootDataverseName) {
    	
        return getSupportTeamName(systemAddress, rootDataverseName, getCurrentLocale());
    }

    public static String getSupportTeamName(final InternetAddress systemAddress, 
    		final String rootDataverseName, final Locale locale) {
    	
        if (systemAddress != null && systemAddress.getPersonal() != null) {
            return systemAddress.getPersonal();
        }
        if (rootDataverseName != null && !rootDataverseName.isEmpty()) {
            return rootDataverseName + ' ' + getStringFromBundleWithLocale("contact.support", locale);
        }
        final String saneDefault = getStringFromBundleWithLocale("dataverse", locale);
        return getStringFromBundleWithLocale("contact.support", locale, saneDefault);
    }

    public static String getSupportTeamEmailAddress(final InternetAddress systemAddress) {
    	
        return systemAddress == null ? null : systemAddress.getAddress();
    }

    public static String getContactHeader() {
    	
        return getStringFromBundle("contact.header");
    }
}