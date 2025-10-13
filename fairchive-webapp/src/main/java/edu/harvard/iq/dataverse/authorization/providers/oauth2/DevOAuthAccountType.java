package edu.harvard.iq.dataverse.authorization.providers.oauth2;

public enum DevOAuthAccountType {
    PRODUCTION,
    RANDOM_EMAIL0,
    RANDOM_EMAIL1,
    RANDOM_EMAIL2,
    RANDOM_EMAIL3;
    
    public static DevOAuthAccountType valueOf(final String s,
            final DevOAuthAccountType defaultValue) {
        try {
            return DevOAuthAccountType.valueOf(s);
        } catch (final IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
