package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.common.base.Strings;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.List;

/***
 * ConditionalRendering can be assigned in two situations:
 *  * main vocab field which controls the rest. Only datasetFieldName is set
 *  * fields dependent on main vocab field. Both values must be filled
 */
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
     * renderOnValue is not set only for vocab field controlling rest of fields
     */
    public boolean shouldRender(List<DatasetField> subfields) {
        if (Strings.isNullOrEmpty(this.renderOnValue)) {
            return true;
        }

        return subfields.stream()
                .filter(df -> df.getDatasetFieldType().getName().equals(this.datasetFieldName))
                .findFirst()
                .map(df -> this.renderOnValue.equals(df.getValue()))
                .orElse(true);
    }

    /***
     * Check if dataset field is responsible for controlling other fields
     * eq
     * country (parent field which unit, name) <- looking for this field
     * unit (render if US)
     * name (render if PL)
     */
    public boolean controlledBy(DatasetField datasetField) {
        return datasetField.getDatasetFieldType().getName().equals(this.datasetFieldName);
    }
}
