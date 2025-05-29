package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.List;

public class ConditionalRenderingHelper {
    public static boolean shouldRender(List<DatasetField> subfields, ConditionalRendering conditionalRendering) {
        if (conditionalRendering == null) {
            return true;
        }

        return subfields.stream()
                .filter(df -> df.getDatasetFieldType().getName().equals(conditionalRendering.getDatasetFieldName()))
                .findFirst()
                .map(df -> conditionalRendering.getRenderOnValue().equals(df.getValue()))
                .orElse(true);
    }
}
