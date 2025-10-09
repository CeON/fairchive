package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

public class AuthenticatedUserUtil {

    /**
     * Given the AuthenticationProvider id, return the friendly name
     * of the AuthenticationProvider as defined in the bundle
     * <p>
     * If no name is defined, return the id itself
     *
     * @param authProviderId
     * @return
     */
    public static String getAuthenticationProviderFriendlyName(String authProviderId) {
        if (authProviderId == null) {
            return getStringFromBundle("authenticationProvider.name.null");
        } else {
            final String friendlyName = getStringFromBundle(
                    "authenticationProvider.name.".concat(authProviderId));
            return friendlyName.isEmpty() ? authProviderId : friendlyName;
        }
    }
}
