package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;


@FacesConverter("multiControlledVocabularyValueConverter")
public class MultiControlledVocabularyValueConverter implements Converter {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty()) {
            return "";
        }

        Long dataFieldTypeId = (Long)component.getAttributes().get("data-field-id");
        return controlledVocabularyValueServiceBean.findByStrValue(dataFieldTypeId, submittedValue).stream().findFirst().orElse(null);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || "".equals(value)) {
            return "";
        }

        if (value instanceof ControlledVocabularyValue)
            return String.valueOf(((ControlledVocabularyValue) value).getStrValue());

        if (value instanceof Suggestion)
            return String.valueOf(((Suggestion) value).getValue());

        return String.valueOf(value);
    }
}
