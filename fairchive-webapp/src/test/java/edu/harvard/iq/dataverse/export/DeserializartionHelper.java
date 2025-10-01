package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockWithFieldsDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeserializartionHelper {

    // -------------------- LOGIC --------------------

    /**
     * Fixes {@link DatasetDTO} deserialized from JSON, ie. when its {@link DatasetFieldDTO}s
     * are deserialized they could have maps as their values instead of single or multiple
     * {@link DatasetFieldDTO}s – this method corrects that by making appropriate conversions.
     */
    public static void repairNestedDatasetFields(DatasetDTO datasetDTO) {
        Collection<MetadataBlockWithFieldsDTO> blocks = Optional.ofNullable(datasetDTO.getDatasetVersion()).map(DatasetVersionDTO::getMetadataBlocks)
                .map(Map::values)
                .orElse(emptyList());
        for (MetadataBlockWithFieldsDTO block : blocks) {
            List<DatasetFieldDTO> fields = block.getFields();
            for (DatasetFieldDTO field : fields) {
                if (!DatasetFieldDTO.COMPOUND.equals(field.getTypeClass())) {
                    continue;
                }
                if (field.getMultiple()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Map<String, Object>>> values = (List<Map<String, Map<String, Object>>>) field.getValue();
                    field.setValue(values.stream()
                            .map(v -> v.entrySet().stream()
                                    .collect(toMap(Map.Entry::getKey, e -> createField(e.getValue()), (prev, next) -> next)))
                            .collect(toList()));
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> value = (Map<String, Map<String, Object>>) field.getValue();
                    field.setValue(value.entrySet().stream()
                            .collect(toMap(Map.Entry::getKey, e -> createField(e.getValue()), (prev, next) -> next)));
                }
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private static DatasetFieldDTO createField(Map<String, Object> fieldData) {
        DatasetFieldDTO dto = new DatasetFieldDTO();
        dto.setTypeName((String) fieldData.get("typeName"));
        dto.setValue(fieldData.get("value"));
        dto.setTypeClass((String) fieldData.get("typeClass"));
        dto.setMultiple((Boolean) fieldData.get("multiple"));
        return dto;
    }
}
