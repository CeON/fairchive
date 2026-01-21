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

/**
 * @author gdurand
 */
@SuppressWarnings("serial")
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @Inject
    SettingsServiceBean settingService;

    @Inject
    DataverseSession session;

    @Inject
    SystemConfig systemConfig;

    private final LazyLoaded<Map<String, String>> configuredLocales = new LazyLoaded<>(this::languagesLoader);
    private final LazyLoaded<Map<String, String>> configureSearchBarUrls = new LazyLoaded<>(() -> urlsLoader(SearchBarUrls));
    private final LazyLoaded<Map<String, String>> configuredAboutUrls = new LazyLoaded<>(() -> urlsLoader(NavbarAboutUrl));
    private final LazyLoaded<Map<String, String>> configuredFooterUrls = new LazyLoaded<>(() -> urlsLoader(FooterAdditionalUrl));

    // -------------------- GETTERS --------------------

    public boolean isPublicInstall() {
        return settingService.isTrueForKey(PublicInstall);
    }

    public String getMetricsUrl() {
        return settingService.getValueForKey(MetricsUrl);
    }

    public boolean isShibPassiveLoginEnabled() {
        return settingService.isTrueForKey(ShibPassiveLoginEnabled);
    }

    public boolean isProvCollectionEnabled() {
        return settingService.isTrueForKey(ProvCollectionEnabled);
    }

    public boolean isSearchResultOnMap() {
        return settingService.isTrueForKey(ShowSearchResultOnMap);
    }
    
    public String getMapTileType() {
    	final String type = this.settingService.getValueForKey(MapTileType);
    	return isEmpty(type) ? "raster" : type;
    }

    public boolean isRsyncUpload() {
        return systemConfig.isRsyncUpload();
    }

    public boolean isRsyncDownload() {
        return systemConfig.isRsyncDownload();
    }

    public boolean isRsyncOnly() {
        return systemConfig.isRsyncOnly();
    }

    public boolean isHTTPUpload() {
        return systemConfig.isHTTPUpload();
    }

    public Integer getUploadMethodsCount() {
        return systemConfig.getUploadMethodCount();
    }

    public String getGuidesBaseUrl() {
        return systemConfig.getGuidesBaseUrl(session.getLocale());
    }

    public String getGuidesVersion() {
        return systemConfig.getGuidesVersion();
    }

    public Boolean isShowAddDatasetButtonOnDataversePage() {
        return settingService.isTrueForKey(ShowAddDatasetButtonOnDataversePage);
    }

    public String getDropBoxKey() {
        String configuredDropBoxKey = getSettingValue(DropboxKey.toString());
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return EMPTY;
    }

    public String getDataCiteCitationsPageUrl() {
        return settingService.getValueForKey(DoiDataCiteCitationsPageUrl);
    }

    public Boolean isAllowDatasetPublishWithoutFiles() {
        return settingService.isTrueForKey(AllowDatasetPublishWithoutFiles);
    }

    public boolean isExpandAllAdvancedSearchBlocksEnabled() {
        return settingService.isTrueForKey(ExpandAllAdvancedSearchBlocks);
    }

    // -------------------- LOGIC --------------------

    public Boolean isHasDropBoxKey() {
        return !getDropBoxKey().isEmpty();
    }

    public String getEnumSettingValue(SettingsServiceBean.Key key) {
        return getSettingValue(key.toString());
    }

    public String getSettingValue(String settingKey) {
        return settingService.get(settingKey);
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

    // -------------------- PRIVATE --------------------

    private Map<String, String> languagesLoader() {
        return settingService.getValueForKeyAsListOfMaps(Languages).stream()
                .collect(toMap(getKey("locale"), getKey("title"), throwingMerger(), LinkedHashMap::new));
    }

    private Map<String, String> urlsLoader(SettingsServiceBean.Key key) {
        String lang = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        return settingService.getValueForKeyAsListOfMaps(key).stream()
                .collect(toMap(getKeyWithLang("url", lang), getKeyWithLang("title", lang), throwingMerger(), LinkedHashMap::new));
    }

    private static Function<Map<String, String>, String> getKey(String key) {
        return map -> map.get(key);
    }

    private static Function<Map<String, String>, String> getKeyWithLang(String key, String lang) {
        String langKey = key + '.' + lang;
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
