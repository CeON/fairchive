package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.List;

public class ConditionalRendering {
    private String datasetFieldName;
    private String renderOnValue;

    public ConditionalRendering(String datasetFieldName, String renderOnValue) {
        this.datasetFieldName = datasetFieldName;
        this.renderOnValue = renderOnValue;
    }

    public String getDatasetFieldName() {
        return datasetFieldName;
    }

    public void setDatasetFieldName(String datasetFieldName) {
        this.datasetFieldName = datasetFieldName;
    }

    public String getRenderOnValue() {
        return renderOnValue;
    }

    public void setRenderOnValue(String renderOnValue) {
        this.renderOnValue = renderOnValue;
    }

    public boolean shouldRender(List<DatasetField> subfields) {
        return subfields.stream()
                .filter(df -> df.getDatasetFieldType().getName().equals(this.datasetFieldName))
                .findFirst()
                .map(df -> this.renderOnValue.equals(df.getValue()))
                .orElse(true);
    }
}
