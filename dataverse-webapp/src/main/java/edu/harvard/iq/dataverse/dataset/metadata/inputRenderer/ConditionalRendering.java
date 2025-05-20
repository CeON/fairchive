package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

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
}
