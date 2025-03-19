package edu.harvard.iq.dataverse.search.response;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.index.IndexedTermOfUse;

public class FacetLocaleNameResolver {

    public static final String FACETBUNDLE_MASK_GROUP_AND_VALUE = "facets.search.fieldtype.%s.%s.label";
    public static final String FACETBUNDLE_MASK_VALUE = "facets.search.fieldtype.%s.label";
    private static final String FACETBUNDLE_MASK_DVCATEGORY_VALUE = "dataverse.type.selectTab.%s";

    private LicenseRepository licenseRepository;
    private Map<String, DatasetFieldType> fieldIndex;

    public FacetLocaleNameResolver(LicenseRepository licenseRepository, Map<String, DatasetFieldType> index) {
        this.licenseRepository = licenseRepository;
        this.fieldIndex = index;
    }

    public String getLocaleFacetCategoryName(String facetCategoryName) {
        final String formattedFacetFieldName = removeSolrFieldSuffix(facetCategoryName);

        if (fieldIndex.containsKey(formattedFacetFieldName)) {
            return getDatasetFieldFacetCategoryName(fieldIndex.get(formattedFacetFieldName));
        } else {
            return getNonDatasetFieldFacetCategoryName(facetCategoryName);
        }
    }

    public String getLocaleFacetLabelName(String facetLabelName, String facetCategoryName) {
        String formattedFacetCategoryName = removeSolrFieldSuffix(facetCategoryName);
        String formattedFacetLabelName = toBundleNameFormat(facetLabelName);

        if (fieldIndex.containsKey(formattedFacetCategoryName)) {
            return getDatasetFieldFacetLabelName(facetLabelName, formattedFacetLabelName, fieldIndex.get(formattedFacetCategoryName));

        } else {
            return getNonDatasetFieldFacetLabelName(facetLabelName, formattedFacetCategoryName);
        }
    }

    private String getDatasetFieldFacetCategoryName(DatasetFieldType matchedDatasetField) {
        if (matchedDatasetField.isFacetable() && !matchedDatasetField.isHasParent()) {
            String key = format(FACETBUNDLE_MASK_VALUE, matchedDatasetField.getName());
            return Optional.ofNullable(BundleUtil.getStringFromBundle(key))
                    .filter(name -> !name.isEmpty())
                    .orElse(matchedDatasetField.getDisplayName());
        }
        return matchedDatasetField.getDisplayName();
    }

    private String getNonDatasetFieldFacetCategoryName(String facetCategoryName) {
        if(facetCategoryName.equals(SearchFields.TYPE)) {
            return facetCategoryName;
        }
        String key = format(FACETBUNDLE_MASK_VALUE, facetCategoryName);
        return BundleUtil.getStringFromBundle(key);
    }

    private String getDatasetFieldFacetLabelName(String facetLabelName, String formattedFacetLabelName,
                                                 DatasetFieldType matchedDatasetField) {
        if (matchedDatasetField.isControlledVocabulary()) {
            String key = "controlledvocabulary." + matchedDatasetField.getName() + "." + formattedFacetLabelName;
            String bundleName = matchedDatasetField.getMetadataBlock().getName().toLowerCase();
            
            String facetLabelTranslation = BundleUtil.getStringFromNonDefaultBundle(key, bundleName);
            if (StringUtils.isNotBlank(facetLabelTranslation)) {
                return facetLabelTranslation;
            }
        }
        return facetLabelName;
    }

    private String getNonDatasetFieldFacetLabelName(String facetLabelName, String formattedFacetCategoryName) {
        String formattedFacetLabelName = toBundleNameFormat(facetLabelName);
        List<String> translatableNonDictionaryFacets = Lists.newArrayList(SearchFields.PUBLICATION_STATUS,
                SearchFields.DATAVERSE_CATEGORY, SearchFields.FILE_TYPE, SearchFields.ACCESS);

        if(translatableNonDictionaryFacets.contains(formattedFacetCategoryName)) {
            if(formattedFacetCategoryName.equals(SearchFields.DATAVERSE_CATEGORY)) {
                String key = format(FACETBUNDLE_MASK_DVCATEGORY_VALUE, formattedFacetLabelName);
                return BundleUtil.getStringFromBundle(key);
            }
            String key = format(FACETBUNDLE_MASK_GROUP_AND_VALUE, formattedFacetCategoryName, formattedFacetLabelName);
            return BundleUtil.getStringFromBundle(key);
        }

        if(formattedFacetCategoryName.equals(SearchFields.METADATA_SOURCE) && formattedFacetLabelName.equals("harvested")) {
            return BundleUtil.getStringFromBundle(formattedFacetLabelName);
        }

        if(formattedFacetCategoryName.equals(SearchFields.LICENSE)) {
            return licenseRepository.findLicenseByName(facetLabelName)
                .map(l -> l.getLocalizedName(BundleUtil.getCurrentLocale()))
                .orElseGet(() -> {
                    String label = BundleUtil.getStringFromBundle(IndexedTermOfUse.getLabelFromName(facetLabelName));
                    return StringUtils.isBlank(label)?facetLabelName:label;
                });
        }

        return facetLabelName;
    }

    private String removeSolrFieldSuffix(String name) {
        if(name.endsWith("_ss")) {
            name = name.substring(0, name.length() - 3);
        } else if (name.endsWith("_s")) {
            name = name.substring(0, name.length() - 2);
        }
        return name;
    }

    /**
     * if exist, multi word bundle names are connected with underscores and formatted toLowerCase
     * @param name text for which we want to create its bundle name
     * @return text with replaced spaces with underscores, and leading/trailing whitespaces removed, toLowerCased
     */
    private String toBundleNameFormat(String name) {
        return StringUtils.stripAccents(name.toLowerCase().replace(" ", "_"));
    }
}
