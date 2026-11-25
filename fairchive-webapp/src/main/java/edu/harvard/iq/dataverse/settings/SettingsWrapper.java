package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.util.SystemConfig;

import org.omnifaces.cdi.ViewScoped;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.AllowDatasetPublishWithoutFiles;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiDataCiteCitationsPageUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiProvider;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DropboxKey;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ExpandAllAdvancedSearchBlocks;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.FooterAdditionalUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Languages;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MapTileType;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MapTileApiKey;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MetricsUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.NavbarAboutUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ProvCollectionEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.PublicInstall;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SearchBarUrls;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShibPassiveLoginEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowAddDatasetButtonOnDataversePage;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowSearchResultOnMap;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@SuppressWarnings("serial")
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    private DataverseSession session;
    private SystemConfig config;
    
    public SettingsWrapper() {}
    
    @Inject
    public SettingsWrapper(final DataverseSession session,
    	                   final SystemConfig config) {
    	this.session = session;
    	this.config = config;
    }

    private final LazyLoaded<Map<String, String>> configuredLocales = new LazyLoaded<>(this::languagesLoader);
    private final LazyLoaded<Map<String, String>> configureSearchBarUrls = new LazyLoaded<>(() -> urlsLoader(SearchBarUrls));
    private final LazyLoaded<Map<String, String>> configuredAboutUrls = new LazyLoaded<>(() -> urlsLoader(NavbarAboutUrl));
    private final LazyLoaded<Map<String, String>> configuredFooterUrls = new LazyLoaded<>(() -> urlsLoader(FooterAdditionalUrl));

    // -------------------- GETTERS --------------------

    public boolean isPublicInstall() {
        return this.config.isTrueForKey(PublicInstall);
    }

    public String getMetricsUrl() {
        return this.config.getValueForKey(MetricsUrl);
    }

    public boolean isShibPassiveLoginEnabled() {
        return this.config.isTrueForKey(ShibPassiveLoginEnabled);
    }

    public boolean isProvCollectionEnabled() {
        return this.config.isTrueForKey(ProvCollectionEnabled);
    }

    public boolean isSearchResultOnMap() {
        return this.config.isTrueForKey(ShowSearchResultOnMap);
    }
    
    public String getMapTileType() {
    	final String type = this.config.getValueForKey(MapTileType);
    	return isEmpty(type) ? "raster" : type;
    }
    
    public String getMapTileApiKey() {
    	return this.config.getValueForKey(MapTileApiKey);
    }

    public boolean isRsyncUpload() {
        return this.config.isRsyncUpload();
    }

    public boolean isRsyncDownload() {
        return this.config.isRsyncDownload();
    }

    public boolean isRsyncOnly() {
        return this.config.isRsyncOnly();
    }

    public boolean isHTTPUpload() {
        return this.config.isHTTPUpload();
    }

    public Integer getUploadMethodsCount() {
        return this.config.getUploadMethodCount();
    }

    public String getGuidesBaseUrl() {
        return this.config.getGuidesBaseUrl(this.session.getLocale());
    }

    public String getGuidesVersion() {
        return this.config.getGuidesVersion();
    }

    public Boolean isShowAddDatasetButtonOnDataversePage() {
        return this.config.isTrueForKey(ShowAddDatasetButtonOnDataversePage);
    }

    public String getDropBoxKey() {
        final String configuredDropBoxKey = getSettingValue(DropboxKey.toString());
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return EMPTY;
    }

    public String getDataCiteCitationsPageUrl() {
        return this.config.getValueForKey(DoiDataCiteCitationsPageUrl);
    }

    public Boolean isAllowDatasetPublishWithoutFiles() {
        return this.config.isTrueForKey(AllowDatasetPublishWithoutFiles);
    }

    public boolean isExpandAllAdvancedSearchBlocksEnabled() {
        return this.config.isTrueForKey(ExpandAllAdvancedSearchBlocks);
    }

    // -------------------- LOGIC --------------------

    public Boolean isHasDropBoxKey() {
        return !getDropBoxKey().isEmpty();
    }

    public String getEnumSettingValue(SettingsServiceBean.Key key) {
        return getSettingValue(key.toString());
    }

    public String getSettingValue(String settingKey) {
        return this.config.get(settingKey);
    }

    public boolean isLocalesConfigured() {
        return configuredLocales.get().size() > 1;
    }

    public Map<String, String> getConfiguredLocales() {
        return configuredLocales.get();
    }

    public String getConfiguredLocaleName(String localeCode) {
        return configuredLocales.get().get(localeCode);
    }

    public Map<String, String> getConfigureSearchBarUrls() {
        return configureSearchBarUrls.get();
    }

    public Map<String, String> getConfiguredAboutUrls() {
        return configuredAboutUrls.get();
    }

    public Map<String, String> getConfiguredFooterUrls() {
        return configuredFooterUrls.get();
    }

    public boolean isDataCiteInstallation() {
        String protocol = getEnumSettingValue(DoiProvider);
        return "DataCite".equals(protocol);
    }
    
    public String getDataverseSiteUrl() {
        return this.config.getDataverseSiteUrl();
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> languagesLoader() {
        return this.config.getValueForKeyAsListOfMaps(Languages).stream()
                .collect(toMap(getKey("locale"), getKey("title"), throwingMerger(), LinkedHashMap::new));
    }

    private Map<String, String> urlsLoader(SettingsServiceBean.Key key) {
        String lang = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        return this.config.getValueForKeyAsListOfMaps(key).stream()
                .collect(toMap(getKeyWithLang("url", lang), getKeyWithLang("title", lang), throwingMerger(), LinkedHashMap::new));
    }

    private static Function<Map<String, String>, String> getKey(String key) {
        return map -> map.get(key);
    }

    private static Function<Map<String, String>, String> getKeyWithLang(String key, String lang) {
        final String langKey = key + '.' + lang;
        return map -> {
            if (map.containsKey(langKey)) {
                return map.get(langKey);
            } else {
                return map.get(key);
            }
        };
    }

    private static BinaryOperator<String> throwingMerger() {
        return (u,v) -> { throw new IllegalStateException(format("Duplicate key %s", u)); };
    }
}
