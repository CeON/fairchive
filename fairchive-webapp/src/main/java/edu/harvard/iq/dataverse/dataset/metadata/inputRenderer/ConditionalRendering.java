package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.List;


public class ConditionalRendering {
    private String datasetFieldName;
    private String renderOnValue;

    /***
     *
     * @param datasetFieldName - dataset field name which will control other fields
     * @param renderOnValue - value of datasetFieldName on which given field will be rendered
     */
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

    /***
     * Check subfields for main vocab value which controls rest of fields
     */
    public boolean shouldRender(List<DatasetField> subfields) {
        return subfields.stream()
                .filter(df -> df.getDatasetFieldType().getName().equals(this.datasetFieldName))
                .findFirst()
                .map(df -> this.renderOnValue.equals(df.getValue()))
                .orElse(true);
    }
}
