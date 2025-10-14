package edu.harvard.iq.dataverse.dataset.tab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ShowMachineTranslation;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MachineTranslationURL;

@SuppressWarnings("serial")
@ViewScoped
@Named("DatasetMetadataTab")
public class DatasetMetadataTab implements Serializable {

    private PermissionsWrapper permissionsWrapper;
    private ExportService exportService;
    private SystemConfig systemConfig;
    private DatasetFieldsInitializer datasetFieldsInitializer;
    private DatasetDao datasetDao;
    private DataverseSession session;

    private Dataset dataset;
    private boolean isDatasetLocked;
    private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocks;
    private SettingsServiceBean settingService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /*JEE requirement */
    DatasetMetadataTab() {
    }

    @Inject
    public DatasetMetadataTab(PermissionsWrapper permissionsWrapper,
                              DataverseSession session,
                              ExportService exportService,
                              SystemConfig systemConfig,
                              DatasetFieldsInitializer datasetVersionUI,
                              DatasetDao datasetDao,
                              SettingsServiceBean settingService) {
        this.permissionsWrapper = permissionsWrapper;
        this.session = session;
        this.exportService = exportService;
        this.systemConfig = systemConfig;
        this.datasetFieldsInitializer = datasetVersionUI;
        this.datasetDao = datasetDao;
        this.settingService = settingService;
    }

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return dataset;
    }

    public boolean isDatasetLocked() {
        return isDatasetLocked;
    }
    
    public String getDatasetGlobalIdString() {
        return this.session.isViewedFromAnonymizedPrivateUrl(this.dataset)
                ? null
                : this.dataset.getGlobalId().asString();
    }

    /**
     * Metadata blocks meant for view.
     */
    public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocks() {
        return metadataBlocks;
    }

    // -------------------- LOGIC --------------------

    public void init(DatasetVersion datasetVersion,
                     boolean isDatasetLocked) {
        this.dataset = datasetVersion.getDataset();
        this.isDatasetLocked = isDatasetLocked;
        
        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForView(datasetVersion.getDatasetFields());
        this.metadataBlocks = DatasetFieldUtil.groupByBlockAndType(datasetFields);
    }

    public boolean showEditMetadataButton() {
        return permissionsWrapper.canCurrentUserUpdateDataset(dataset) && !dataset.isDeaccessioned();
    }
    
    public boolean showExportButton() {
        return ! this.session.isViewedFromAnonymizedPrivateUrl(this.dataset)
            && this.dataset.containsReleasedVersion();
    }

    
    /**
     * Extracts exporters display name and redirect url.
     */
    public List<Tuple2<String, String>> getExportersDisplayNameAndURL() {
        List<Tuple2<String, String>> result = new ArrayList<>();

        for (final Exporter exporter : exportService.exporters()) {
            if (exporter.isAvailableToUsers()) {
                result.add(Tuple.of(exporter.getDisplayName(), createExporterURL(
                                exporter, systemConfig.getDataverseSiteUrl())));
            }
        }
        return result;
    }

    public String getAlternativePersistentIdentifier() {
        return datasetDao.find(dataset.getId()).getAlternativePersistentIdentifier();
    }

    /**
     * Extracts exporters display name and redirect url.
     */
    public List<Tuple2<String, String>> getTranslationLanguages() {
        List<Tuple2<String, String>> result = new ArrayList<>();
        result.add(Tuple.of("sq","Albanian"));
        result.add(Tuple.of("ar","Arabic"));
        result.add(Tuple.of("az","Azerbaijani"));
        result.add(Tuple.of("eu","Basque"));
        result.add(Tuple.of("bn","Bengali"));
        result.add(Tuple.of("bg","Bulgarian"));
        result.add(Tuple.of("ca","Catalan"));
        result.add(Tuple.of("zh-Hans","Chinese"));
        result.add(Tuple.of("zh-Hant","Chinese (traditional)"));
        result.add(Tuple.of("cs","Czech"));
        result.add(Tuple.of("da","Danish"));
        result.add(Tuple.of("nl","Dutch"));
        result.add(Tuple.of("en","English"));
        result.add(Tuple.of("eo","Esperanto"));
        result.add(Tuple.of("et","Estonian"));
        result.add(Tuple.of("fi","Finnish"));
        result.add(Tuple.of("fr","French"));
        result.add(Tuple.of("gl","Galician"));
        result.add(Tuple.of("de","German"));
        result.add(Tuple.of("el","Greek"));
        result.add(Tuple.of("he","Hebrew"));
        result.add(Tuple.of("hi","Hindi"));
        result.add(Tuple.of("hu","Hungarian"));
        result.add(Tuple.of("id","Indonesian"));
        result.add(Tuple.of("ga","Irish"));
        result.add(Tuple.of("it","Italian"));
        result.add(Tuple.of("ja","Japanese"));
        result.add(Tuple.of("ko","Korean"));
        result.add(Tuple.of("ky","Kyrgyz"));
        result.add(Tuple.of("lv","Latvian"));
        result.add(Tuple.of("lt","Lithuanian"));
        result.add(Tuple.of("ms","Malay"));
        result.add(Tuple.of("nb","Norwegian"));
        result.add(Tuple.of("fa","Persian"));
        result.add(Tuple.of("pl","Polish"));
        result.add(Tuple.of("pt","Portuguese"));
        result.add(Tuple.of("pt-BR","Portuguese (Brazil)"));
        result.add(Tuple.of("ro","Romanian"));
        result.add(Tuple.of("ru","Russian"));
        result.add(Tuple.of("sk","Slovak"));
        result.add(Tuple.of("sl","Slovenian"));
        result.add(Tuple.of("es","Spanish"));
        result.add(Tuple.of("sv","Swedish"));
        result.add(Tuple.of("tl","Tagalog"));
        result.add(Tuple.of("th","Thai"));
        result.add(Tuple.of("tr","Turkish"));
        result.add(Tuple.of("uk","Ukrainian"));
        result.add(Tuple.of("ur","Urdu"));
        return result;
    }

    public boolean showMachineTranslation() {
        return settingService.isTrueForKey(ShowMachineTranslation);
    }

    public String getMachineTranslationURL() {
        return settingService.getValueForKey(MachineTranslationURL);
    }

    // -------------------- PRIVATE --------------------

    private String createExporterURL(Exporter exporter, String myHostURL) {
        return myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName()
            + "&persistentId=" + dataset.getGlobalId();
    }
}
