package edu.harvard.iq.dataverse.dataset.tab;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.translation.Translator;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;

@SuppressWarnings("serial")
@ViewScoped
@Named("DatasetMetadataTab")
public class DatasetMetadataTab implements Serializable {

	private PermissionsWrapper permissionsWrapper;
	private ExportService exportService;
	private SystemConfig systemConfig;
	private DatasetFieldsInitializer datasetFieldsInitializer;
	private DatasetService datasetService;
	private DataverseSession session;

	private Dataset dataset;
	private boolean isDatasetLocked;
	private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocks;
	private final TranslationDialog translationDialog = new TranslationDialog();
	private Translator translator;

	// -------------------- CONSTRUCTORS --------------------

	@Deprecated /* JEE requirement */
	DatasetMetadataTab() {
	}

	@Inject
	public DatasetMetadataTab(PermissionsWrapper permissionsWrapper, 
			                  DataverseSession session,
			                  ExportService exportService, 
			                  SystemConfig systemConfig,
			                  DatasetFieldsInitializer datasetVersionUI,
			                  DatasetService datasetService, 
			                  Translator translator) {
		this.permissionsWrapper = permissionsWrapper;
		this.session = session;
		this.exportService = exportService;
		this.systemConfig = systemConfig;
		this.datasetFieldsInitializer = datasetVersionUI;
		this.datasetService = datasetService;
		this.translator = translator;
	}

	// -------------------- GETTERS --------------------

	public Dataset getDataset() {
		return dataset;
	}

	public boolean isDatasetLocked() {
		return isDatasetLocked;
	}

	public String getDatasetGlobalIdString() {
		return this.session.isViewedFromAnonymizedPrivateUrl(this.dataset) ? null
				: this.dataset.getGlobalId().asString();
	}

	/**
	 * Metadata blocks meant for view.
	 */
	public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocks() {
		return metadataBlocks;
	}

	public TranslationDialog getTranslationDialog() {
		return this.translationDialog;
	}

	// -------------------- LOGIC --------------------

	public void init(DatasetVersion datasetVersion, boolean isDatasetLocked) {
		this.dataset = datasetVersion.getDataset();
		this.isDatasetLocked = isDatasetLocked;

		List<DatasetField> datasetFields = datasetFieldsInitializer
				.prepareDatasetFieldsForView(datasetVersion.getDatasetFields(), false);
		this.metadataBlocks = DatasetFieldUtil.groupByBlockAndType(datasetFields);
	}

	public boolean showEditMetadataButton() {
		return permissionsWrapper.canCurrentUserUpdateDataset(dataset) 
				&& !dataset.isDeaccessioned();
	}

	public boolean showExportButton() {
		return !this.session.isViewedFromAnonymizedPrivateUrl(this.dataset) 
				&& this.dataset.containsReleasedVersion();
	}

	/**
	 * Extracts exporters display name and redirect url.
	 */
	public List<Tuple2<String, String>> getExportersDisplayNameAndURL() {
		List<Tuple2<String, String>> result = new ArrayList<>();

		for (final Exporter exporter : exportService.exporters()) {
			if (exporter.isAvailableToUsers()) {
				result.add(Tuple.of(exporter.getDisplayName(),
						createExporterURL(exporter, systemConfig.getDataverseSiteUrl())));
			}
		}
		return result;
	}

	public String getAlternativePersistentIdentifier() {
		return datasetService.find(dataset.getId()).getAlternativePersistentIdentifier();
	}

	public boolean showMachineTranslation() {
		return this.translator.isEnabled();
	}

	// -------------------- PRIVATE --------------------

	private String createExporterURL(Exporter exporter, String myHostURL) {
		return myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName() 
			+ "&persistentId=" + dataset.getGlobalId();
	}

	// --------------------------------------------------------------------------
	public final class TranslationDialog implements Serializable {

		private String selectedLanguageCode;

		public List<Language> getLanguages() {
			return Language.values;
		}

		public String getSelectedLanguageCode() {
			return this.selectedLanguageCode;
		}

		public void setSelectedLanguageCode(final String code) {
			this.selectedLanguageCode = code;
		}
		
		public List<Map.Entry<MetadataBlock, List<DatasetFieldsByType>>> getMetadataBlocks() {
			return new ArrayList<>(getMetadataMap().entrySet());
		}
		
		public String getPublicationDate() {
			return DatasetMetadataTab.this.dataset.getPublicationDateFormattedYYYYMMDD();
		}
		
		public boolean isViewedFromAnonymizedPrivateUrl() {
			return DatasetMetadataTab.this.session.
					isViewedFromAnonymizedPrivateUrl(DatasetMetadataTab.this.dataset);
		}
		
		public String getAlternativePersistentIdentifier() {
			return DatasetMetadataTab.this.getAlternativePersistentIdentifier();
		}
		
		public String getDatasetGlobalIdString() {
			return DatasetMetadataTab.this.getDatasetGlobalIdString();
		}

		private Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataMap() {
			if (this.selectedLanguageCode == null) {
				return DatasetMetadataTab.this.metadataBlocks;
			} else {
				return getTranslatedMetadataMap();
			}
		}

		private Map<MetadataBlock, List<DatasetFieldsByType>> getTranslatedMetadataMap() {
			final HashMap<MetadataBlock, List<DatasetFieldsByType>> result = new HashMap<>();
			final Map<MetadataBlock, List<DatasetFieldsByType>> blocks = DatasetMetadataTab.this.metadataBlocks;

			for (final MetadataBlock block : blocks.keySet()) {
				final List<DatasetFieldsByType> translated =  translateByType(blocks.get(block));
				result.put(block,  translated);
			}

			return result;
		}

		private List<DatasetFieldsByType> translateByType(List<DatasetFieldsByType> fieldsByType) {
			final ArrayList<DatasetFieldsByType> result = new ArrayList<>(fieldsByType.size());

			for (final DatasetFieldsByType fieldByType : fieldsByType) {
				final List<DatasetField> translated = translate(fieldByType.getDatasetFields());
				result.add(new DatasetFieldsByType(fieldByType.getDatasetFieldType(), translated));
			}

			return result;
		}

		private List<DatasetField> translate(List<DatasetField> fields) {
			final ArrayList<DatasetField> result = new ArrayList<>(fields.size());

			for (final DatasetField field : fields) {
				final DatasetField newField = field.copy();
				newField.setValue(translate(field.getValue()));
				for(final DatasetField child : field.getChildren()) {
					final DatasetField newChild = child.copy();
					newChild.setValue(translate(child.getValue()));
					newField.getChildren().add(newChild);
				}
				
				result.add(newField);
			}

			return result;
		}

		private String translate(final String text) {
			final String sanitized = text != null 
					? text.replace("<p>", " ").replace("</p>", " ").
							replace("<br>", " ").replace("<br/>", " ")
					: text;
			return DatasetMetadataTab.this.translator.translate(sanitized, this.selectedLanguageCode);
		}
	}

	// -------------------------------------------------------------------------
	public final static class Language {

		private final String code;
		private final String label;

		final static List<Language> values = asList(new Language("sq", "Albanian"), 
				new Language("ar", "Arabic"),
				new Language("az", "Azerbaijani"), 
				new Language("eu", "Basque"), 
				new Language("bn", "Bengali"),
				new Language("bg", "Bulgarian"), 
				new Language("ca", "Catalan"), 
				new Language("zh-Hans", "Chinese"),
				new Language("zh-Hant", "Chinese (traditional)"), 
				new Language("cs", "Czech"),
				new Language("da", "Danish"), 
				new Language("nl", "Dutch"), 
				new Language("en", "English"),
				new Language("eo", "Esperanto"), 
				new Language("et", "Estonian"), 
				new Language("fi", "Finnish"),
				new Language("fr", "French"), 
				new Language("gl", "Galician"), 
				new Language("de", "German"),
				new Language("el", "Greek"), 
				new Language("he", "Hebrew"), 
				new Language("hi", "Hindi"),
				new Language("hu", "Hungarian"), 
				new Language("id", "Indonesian"), 
				new Language("ga", "Irish"),
				new Language("it", "Italian"), 
				new Language("ja", "Japanese"), 
				new Language("ko", "Korean"),
				new Language("ky", "Kyrgyz"), 
				new Language("lv", "Latvian"), 
				new Language("lt", "Lithuanian"),
				new Language("ms", "Malay"), 
				new Language("nb", "Norwegian"), 
				new Language("fa", "Persian"),
				new Language("pl", "Polish"), 
				new Language("pt", "Portuguese"),
				new Language("pt-BR", "Portuguese (Brazil)"), 
				new Language("ro", "Romanian"),
				new Language("ru", "Russian"), 
				new Language("sk", "Slovak"), 
				new Language("sl", "Slovenian"),
				new Language("es", "Spanish"), 
				new Language("sv", "Swedish"), 
				new Language("tl", "Tagalog"),
				new Language("th", "Thai"), 
				new Language("tr", "Turkish"), 
				new Language("uk", "Ukrainian"),
				new Language("ur", "Urdu"));

		private Language(final String code, final String label) {
			this.code = code;
			this.label = label;
		}

		public String getCode() {
			return this.code;
		}

		public String getLabel() {
			return this.label;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (!(o instanceof Language)) {
				return false;
			} else {
				final Language other = (Language) o;
				return this.code.equals(other.code);
			}
		}

		@Override
		public int hashCode() {
			return this.code.hashCode();
		}
	}
}
