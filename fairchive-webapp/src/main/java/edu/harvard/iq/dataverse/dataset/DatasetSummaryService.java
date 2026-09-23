package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class DatasetSummaryService {

    /**
     * Returns dataset fields to be shown in the dataset summary box, grouped
     * by their type and ordered as in the given custom field list.
     *
     * @param anonymizedView if true, field types that are not visible through
     *                       an anonymized private URL are left out.
     */
    public List<DatasetFieldsOfType> getDatasetSummaryFields(DatasetVersion datasetVersion,
                                                             List<String> customFieldList,
                                                             boolean anonymizedView) {

        Map<String, DatasetFieldsOfType> allFieldsByType = DatasetFieldUtil.groupByType(datasetVersion.getFlatDatasetFields())
                .stream()
                .collect(HashMap::new,
                        (map, fieldsOfType) -> map.put(fieldsOfType.getType().getName(), fieldsOfType),
                        (map1, map2) -> map1.putAll(map2));

        List<DatasetFieldsOfType> fieldsOfTypes = new ArrayList<>();
        
        for (String summaryField: customFieldList) {
            DatasetFieldsOfType fieldsOfType = allFieldsByType.get(summaryField);
            if (fieldsOfType != null && (!anonymizedView || fieldsOfType.isVisibleThroughAnonymizedUrl())) {
                fieldsOfTypes.add(fieldsOfType);
            }
        }

        return fieldsOfTypes;
    }

}
