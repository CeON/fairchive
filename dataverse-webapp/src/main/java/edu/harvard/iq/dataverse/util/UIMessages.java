package edu.harvard.iq.dataverse.util;

import javax.ejb.Stateless;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

@Stateless
public class UIMessages {

    public void addSuccessMessage(final String message) {
        JsfHelper.addSuccessMessage(message);
    }

    public void addErrorMessage(final String message) {
        JsfHelper.addErrorMessage(message);
    }

    public void addFlashSuccessMessage(final String message) {
        JsfHelper.addFlashSuccessMessage(message);
    }

    public void addComponentErrorMessage(final String componentId,
            final String summary, final String detail) {
        JsfHelper.addComponentErrorMessage(componentId, summary, detail);
    }
    public void addComponentErrorMessage(final UIComponent component,
            final String summary, final String detail) {
        final FacesContext context = FacesContext.getCurrentInstance();
        context.validationFailed();
        JsfHelper.addComponentErrorMessage(component.getClientId(context), summary, detail);
    }   
}
