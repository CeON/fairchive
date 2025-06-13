/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.util.function.Function;

/**
 * @author xyang
 */
@FacesConverter("culturalTypeValueConverter")
public class CulturalTypeValueConverter implements Converter {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty()) {
            return "";
        }

        return controlledVocabularyValueServiceBean.findByIdentifier(submittedValue).stream().findFirst().orElse(null);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || "".equals(value)) {
            return "";
        }
        return value instanceof ControlledVocabularyValue ?
                String.valueOf(((ControlledVocabularyValue) value).getId()):
                String.valueOf(value);
    }
}
