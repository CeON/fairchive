package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundle;

public class RoleTranslationUtil {

    // -------------------- LOGIC --------------------

    /**
     *
     * @param alias db alias used to check for property
     * @param name default value we want to use in case property based on alias isn't found,
     *             usually original role name should be passed here
     * @return localized Role name if found, or provided in second parameter default
     */
    public static String getLocaleNameFromAlias(final String alias, final String name) {
    	return getFromAlias(alias, name, ".name");
    }

    /**
     * Returns localized name for the role alias or the provided alias if the
     * localized version was not found.
     */
    public static String getLocaleNameFromAlias(final String alias) {
        return getLocaleNameFromAlias(alias, alias);
    }

    /**
     *
     * @param alias db alias used to check for property
     * @param description default value we want to use in case property based on alias isn't found,
     *             usually original role description should be passed here
     * @return localized Role description if found, or provided in second parameter default
     */
    public static String getLocaleDescriptionFromAlias(final String alias, 
    		final String description) {
    	return getFromAlias(alias, description, ".description");
    }
    
    private static String getFromAlias(final String alias, final String text, 
    		final String keySuffix) {
        if (alias != null) {
            final String key = "role." + alias.toLowerCase() + keySuffix;
            final String localized= getStringFromNonDefaultBundle(key, "BuiltInRoles");
            return localized.isEmpty() ? text : localized;
        } else {
        	return text;
        }
    }
}
