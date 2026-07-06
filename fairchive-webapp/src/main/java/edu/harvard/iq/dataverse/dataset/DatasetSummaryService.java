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

    public List<DatasetFieldsOfType> getDatasetSummaryFields(DatasetVersion datasetVersion, List<String> customFieldList) {

        Map<String, DatasetFieldsOfType> allFieldsByType = DatasetFieldUtil.groupByType(datasetVersion.getFlatDatasetFields())
                .stream()
                .collect(HashMap::new,
                        (map, fieldsByType) -> map.put(fieldsByType.getType().getName(), fieldsByType),
                        (map1, map2) -> map1.putAll(map2));

        List<DatasetFieldsOfType> fieldsOfTypes = new ArrayList<>();
        
        for (String summaryField: customFieldList) {
            if (allFieldsByType.containsKey(summaryField)) {
                fieldsOfTypes.add(allFieldsByType.get(summaryField));
            }
        }

        return fieldsOfTypes;
    }

}
