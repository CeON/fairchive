package edu.harvard.iq.dataverse.util;

import static edu.harvard.iq.dataverse.authorization.providers.oauth2.DevOAuthAccountType.PRODUCTION;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AccessibilityStatement;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AllowSignUp;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AllowedExternalRedirectionUrlAfterLogin;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ApiTermsOfUse;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ApplicationTermsOfUse;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AuthenticatedSessionTimeout;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CookieDomain;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CookieName;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CookieSecure;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DebugOAuthAccountType;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DownloadMethods;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GuidesBaseUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GuidesVersion;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Languages;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.LoginInfo;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MinutesUntilPasswordResetTokenExpires;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.OAuth2CallbackUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.OcrCommand;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.PrivacyPolicy;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ReadonlyMode;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.RserveConfigured;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SelectDataverseInfo;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowAccessibilityStatementFooterLink;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowPrivacyPolicyFooterLink;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowTermsOfUseFooterLink;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteFullName;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteName;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoAlt;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoContrastPath;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoContrastResponsivePath;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoLink;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoPath;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SuperiorLogoResponsivePath;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.TabularIngestSizeLimit;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.TimerServer;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.UploadMethods;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.UseOAIStrictIdentifierScheme;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.toLong;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.DevOAuthAccountType;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

/**
 * System-wide configuration
 */
@ApplicationScoped
@Named
public class SystemConfig {

    private static final Logger logger = getLogger(SystemConfig.class.getCanonicalName());

    private static final String VERSION_PROPERTIES = "/config/version.properties";
    private static final String VERSION_KEY = "dataverse.version";
    private static final String COMMIT_ID = "git.commit.id.full";
    private static final String VERSION_PLACEHOLDER = "${project.version}";
    private static final String VERSION_FALLBACK = "4.0";
    private static final long defaultThumbnailSizeLimit = 10_000_000;
    public static final String DATAVERSE_PATH = "/dataverse/";

    private static final Integer DEFAULT_AUTHENTICATED_SESSION_TIMEOUT_MINUTES = 1440;

    /**
     * A JVM option for where files are stored on the file system.
     */
    public static final String FILES_DIRECTORY = "dataverse.files.directory";

    @Inject
    private SettingsServiceBean settings;
    
    private String commitId = "";
    private String appVersion = VERSION_FALLBACK;
    
    public SystemConfig() {
        
    }
    
    public SystemConfig(final SettingsServiceBean settings) {
        this.settings = settings;
    }
    
    @PostConstruct
    void init() {
        try (final InputStream in = getClass()
                .getResourceAsStream(VERSION_PROPERTIES)) {
            final Properties properties = new Properties();
            properties.load(in);
            final String version = properties.getProperty(VERSION_KEY);
            if (VERSION_PLACEHOLDER.equals(version)) {
                logger.warning(VERSION_PROPERTIES +
                        " was not filtered by maven (check your pom.xml configuration)");
                return;
            } else {
                this.appVersion = version;
                this.commitId = properties.getProperty(COMMIT_ID);
            }
        } catch (final IOException e) {
            logger.warning(e.toString());
        }
        logger.warning("Failed to read the " + VERSION_FALLBACK + " file");
    }
    

    public String getVersionWithBuild() {
        return this.appVersion + '-' + this.commitId;
    }

    public String getVersion() {
        return this.appVersion;
    }
    
    public boolean useOAIStrictIdentifierScheme() {
        return isTrueForKey(UseOAIStrictIdentifierScheme);
    }

    public Integer getMinutesUntilPasswordResetTokenExpires() {
        return getValueForKeyAsInt(MinutesUntilPasswordResetTokenExpires);
    }

    public String getDataverseSiteUrl() {
        return getValueForKey(SiteUrl);
    }

    public boolean isReadonlyMode() {
        return isTrueForKey(ReadonlyMode);
    }

    public boolean isUnconfirmedMailRestrictionModeEnabled() {
        return isTrueForKey(Key.UnconfirmedMailRestrictionModeEnabled);
    }

    public boolean isSignupAllowed() {
        return isReadonlyMode() ? false: isTrueForKey(AllowSignUp);
    }

    public String getFilesDirectory() {
        return getFilesDirectoryStatic();
    }

    public static String getFilesDirectoryStatic() {
        return getProperty(FILES_DIRECTORY, "/tmp/files");
    }

    /**
     * The "official" server's fully-qualified domain name:
     */
    public String getDataverseServer() {
        try {
            return new URL(getValueForKey(SiteUrl)).getHost();
        } catch (final MalformedURLException e) {
            return "localhost";
        }
    }

    public String getSiteName(final Locale locale) {
        return getLocalizedProperty(SiteName, locale);
    }

    public String getSiteFullName(final Locale locale) {
        return getLocalizedProperty(SiteFullName, locale);
    }

    public boolean isSuperiorLogoDefined(final Locale locale) {
        return !getLocalizedProperty(SuperiorLogoPath, locale).isEmpty() 
                || !getLocalizedProperty(SuperiorLogoResponsivePath, locale).isEmpty();
    }
    
    public boolean isOcrEnabled() {
        return isNotBlank(getValueForKey(OcrCommand));
    }

    public String getSuperiorLogoLink(final Locale locale) {
        return getLocalizedProperty(SuperiorLogoLink, locale);
    }

    public String getSuperiorLogoPath(final Locale locale) {
        return getLocalizedProperty(SuperiorLogoPath, locale);
    }

    public String getSuperiorLogoResponsivePath(Locale locale) {
        return getLocalizedProperty(SuperiorLogoResponsivePath, locale);
    }

    public String getSuperiorLogoContrastPath(final Locale locale) {
        return getLocalizedProperty(SuperiorLogoContrastPath, locale);
    }

    public String getSuperiorLogoContrastResponsivePath(final Locale locale) {
        return getLocalizedProperty(SuperiorLogoContrastResponsivePath, locale);
    }

    public String getSuperiorLogoAlt(final Locale locale) {
        return getLocalizedProperty(SuperiorLogoAlt, locale);
    }

    public String getGuidesBaseUrl(final Locale locale) {
        return getValueForKey(GuidesBaseUrl) + '/' + locale;
    }

    public String getGuidesVersion() {
        final String guidesVersion = getValueForKey(GuidesVersion);
        return isNotBlank(guidesVersion) ? guidesVersion : getVersion();
    }

    public boolean isRserveConfigured() {
        return isTrueForKey(RserveConfigured);
    }

    public long getUploadLogoSizeLimit() {
        return 500000;
    }

    public long getThumbnailSizeLimitImage() {
        return getThumbnailSizeLimit("dataverse.dataAccess.thumbnail.image.limit");
    }

    public long getThumbnailSizeLimitPDF() {
        return getThumbnailSizeLimit("dataverse.dataAccess.thumbnail.pdf.limit");
            
    }

    private long getThumbnailSizeLimit(final String key) {
        return isReadonlyMode() 
                ? -1
                : toLong(getProperty(key), defaultThumbnailSizeLimit);
    }

    public boolean isThumbnailGenerationDisabledForImages() {
        return isReadonlyMode();
    }

    public boolean isThumbnailGenerationDisabledForPDF() {
        return isReadonlyMode();
    }
    
    public long getDefaultThumbnailSizeLimit() {
        return defaultThumbnailSizeLimit;
    }

    public String getApplicationTermsOfUse(final Locale locale) {
        return getFromBundleIfEmptyLocalizedProperty(ApplicationTermsOfUse, 
                locale, "system.app.terms");
    }

    public String getApiTermsOfUse() {
        return getFromBundleIfEmptyProperty(ApiTermsOfUse, "system.api.terms");
    }

    public String getPrivacyPolicy(final Locale locale) {
        return getFromBundleIfEmptyLocalizedProperty(PrivacyPolicy, locale, 
                "system.privacy.policy");
    }

    public String getAccessibilityStatement(final Locale locale) {
        return getFromBundleIfEmptyLocalizedProperty(AccessibilityStatement, 
                locale, "system.accessibility.statement");
    }

    public String getLoginInfo(final Locale locale) {
        return getLocalizedProperty(LoginInfo, locale);
    }

    public String getSelectDataverseInfo(final Locale locale) {
        return getLocalizedProperty(SelectDataverseInfo, locale);
    }


    public String getAllowedExternalRedirectionUrl() {
        return getValueForKey(AllowedExternalRedirectionUrlAfterLogin);
    }

    public String getCookieName() {
        return getValueForKey(CookieName);
    }

    public String getCookieDomain() {
        return getValueForKey(CookieDomain);
    }

    public Boolean getCookieSecure() {
        return parseBoolean(getValueForKey(CookieSecure));
    }
    /**
     * This method will return the blanket ingestable size limit, if
     * set on the system. I.e., the universal limit that applies to all
     * tabular ingests, regardless of fromat:
     **/
    public long getTabularIngestSizeLimit() {
        return getValueForKeyAsLong(TabularIngestSizeLimit);
    }
    /**
     *  This method returns the size limit set specifically for this format name,
     *  if available, otherwise - the blanket limit that applies to all tabular
     *  ingests regardless of a format.
     * */
    public long getTabularIngestSizeLimit(final String formatName) {
        if (isEmpty(formatName)) {
            return getTabularIngestSizeLimit();
        } else {
            final String limit = this.settings
                    .get(TabularIngestSizeLimit.toString() + ':' + formatName);
            return toLong(limit, getTabularIngestSizeLimit());
        }
    }

    public boolean isTimerServer() {
        return isTrueForKey(TimerServer);
    }

    public DevOAuthAccountType getDevOAuthAccountType() {
        return DevOAuthAccountType.valueOf(getValueForKey(DebugOAuthAccountType), 
                PRODUCTION);
    }

    public String getOAuth2CallbackUrl() {
        final String value = getValueForKey(OAuth2CallbackUrl);
        return isNotEmpty(value) 
                ? value 
                : getDataverseSiteUrl().concat("/oauth2/callback.xhtml");
    }

    public Integer getAuthenticatedSessionTimeoutMinutes() {
        return getValueForKeyAsInt(AuthenticatedSessionTimeout, 
                DEFAULT_AUTHENTICATED_SESSION_TIMEOUT_MINUTES);
    }
    
    private String getValueForKey(final Key key) {
        return this.settings.getValueForKey(key);
    }
    
    private boolean isTrueForKey(final Key key) {
        return this.settings.isTrueForKey(key);
    }
    
    private Integer getValueForKeyAsInt(final Key key) {
        return this.settings.getValueForKeyAsInt(key);
    }
    
    private Long getValueForKeyAsLong(final Key key) {
        return this.settings.getValueForKeyAsLong(key);
    }
    
    private Integer getValueForKeyAsInt(final Key key, final Integer defaultValue) {
        return this.settings.getValueForKeyAsInt(key, defaultValue);
    }

    /**
     * Below are three related enums having to do with big data support:
     * <p>
     * - FileUploadMethods
     * <p>
     * - FileDownloadMethods
     * <p>
     * - TransferProtocols
     * <p>
     * There is a good chance these will be consolidated in the future.
     */
    public enum FileUploadMethods {
        /**
         * DCM stands for Data Capture Module. Right now it supports upload over
         * rsync+ssh but DCM may support additional methods in the future.
         */
        RSYNC("dcm/rsync+ssh"),
        /**
         * Traditional Dataverse file handling, which tends to involve users
         * uploading and downloading files using a browser or APIs.
         */
        NATIVE("native/http");

        private final String text;

        FileUploadMethods(final String text) {
            this.text = text;
        }
        
        private boolean equalsIgnoreCase(final String text) {
            return StringUtils.equalsIgnoreCase(this.text, text);
        }
        
        private boolean isPresentIn(final String text) {
            return StringUtils.containsIgnoreCase(text, this.text);
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    public boolean isRsyncUpload() {
        return FileUploadMethods.RSYNC.isPresentIn(getValueForKey(UploadMethods));
    }

    public boolean isHTTPUpload() {
        return FileUploadMethods.NATIVE.isPresentIn(getValueForKey(UploadMethods));
    }

    public boolean isRsyncOnly() {
        return FileDownloadMethods.RSYNC.equalsIgnoreCase(getValueForKey(DownloadMethods))
            && FileUploadMethods.RSYNC.equalsIgnoreCase(getValueForKey(UploadMethods));
    }

    public boolean isRsyncDownload() {
        return FileDownloadMethods.RSYNC.isPresentIn(getValueForKey(DownloadMethods));
    }

    public boolean isHTTPDownload() {
        return FileDownloadMethods.NATIVE.isPresentIn(getValueForKey(DownloadMethods));
    }

    public int getUploadMethodCount() {
        final int rsync = isRsyncUpload() ? 1 : 0;
        final int http = isHTTPUpload() ? 1 : 0;
        return rsync + http;
    }

    public Map<String, String> getConfiguredLocales() {
        final Map<String, String> result = new LinkedHashMap<>();

        for (final Object obj : new JSONArray(getValueForKey(Languages))) {
            final JSONObject entry = (JSONObject) obj;
            result.put(entry.getString("locale"), entry.getString("title"));
        }

        return result;
    }

    public boolean isShowPrivacyPolicyFooterLinkRendered() {
        return isTrueForKey(ShowPrivacyPolicyFooterLink);
    }

    public boolean isShowTermsOfUseFooterLinkRendered() {
        return isTrueForKey(ShowTermsOfUseFooterLink);
    }

    public boolean isShowAccessibilityStatementFooterLinkRendered() {
        return isTrueForKey(ShowAccessibilityStatementFooterLink);
    }

    private String getFromBundleIfEmptyLocalizedProperty(final Key key, 
            final Locale locale, final String bundleKey) {
        final String result = getLocalizedProperty(key, locale);
        return isNotBlank(result) ? result : getFromBundleIfEmptyProperty(key, bundleKey);
    }

    private String getFromBundleIfEmptyProperty(final SettingsServiceBean.Key key, 
            final String bundleKey) {
        final String result = getValueForKey(key);
        return isNotBlank(result) ? result : getStringFromBundle(bundleKey) ;
    }

    private String getLocalizedProperty(final Key key, final Locale locale) {
        final String result = this.settings.
                getValueForKeyWithPostfix(key, locale.toLanguageTag());
        return isNotBlank(result) ? result : getValueForKey(key);
    }

    /**
     * See FileUploadMethods.
     * <p>
     * TODO: Consider if dataverse.files.s3-download-redirect belongs here since
     * it's a way to bypass Glassfish when downloading.
     */
    private enum FileDownloadMethods {
        /**
         * RSAL stands for Repository Storage Abstraction Layer. Downloads don't
         * go through Glassfish.
         */
        RSYNC("rsal/rsync"),
        NATIVE("native/http");
        private final String text;

        FileDownloadMethods(final String text) {
            this.text = text;
        }
        
        private boolean equalsIgnoreCase(final String text) {
            return StringUtils.equalsIgnoreCase(this.text, text);
        }
        
        private boolean isPresentIn(final String text) {
            return StringUtils.containsIgnoreCase(text, this.text);
        }
    }

    public enum DataFilePIDFormat {
        DEPENDENT("DEPENDENT"),
        INDEPENDENT("INDEPENDENT");
        
        private final String text;

        public String getText() {
            return text;
        }

        DataFilePIDFormat(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this.text;
        }

    }

    /**
     * See FileUploadMethods.
     */
    public enum TransferProtocols {

        RSYNC("rsync"),
        /**
         * POSIX includes NFS. This is related to Key.LocalDataAccessPath in
         * SettingsServiceBean.
         */
        POSIX("posix"),
        GLOBUS("globus");

        private final String text;

        TransferProtocols(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this.text;
        }

        public static TransferProtocols fromString(final String text) {
            for (final TransferProtocols protocoles : TransferProtocols.values()) {
                if (protocoles.text.equals(text)) {
                    return protocoles;
                }
            }
            throw new IllegalArgumentException(
                    "Must be one of: " + TransferProtocols.values() + '.');
        }
    }
}
