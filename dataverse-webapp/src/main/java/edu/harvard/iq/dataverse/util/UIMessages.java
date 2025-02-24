package edu.harvard.iq.dataverse.util;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;

@Stateless
public class UIMessages {

    public void addSuccessMessage(final String message) {
        addComponentSuccessMessage("successMessage",
                getStringFromBundle("messages.success"), message);
    }

    public void addErrorMessage(final String message) {
        addAjaxHasErrorParam();
        addComponentErrorMessage((String) null, message);
    }
    
    public void addErrorMessage(final String message, final String detail) {
        addAjaxHasErrorParam();
        addComponentErrorMessage((String) null, message, detail);
    }
    
    public void addInfoMessage(final String detail) {
        addComponentInfoMessage(null, getStringFromBundle("messages.info"), detail);
    }

    public void addFlashSuccessMessage(final String message) {
        addAjaxHasErrorParam();
        addSuccessMessage(message);
    }

    private static void addAjaxHasErrorParam() {
        PrimeFaces.current().ajax().addCallbackParam("hasErrorMessage", true);
    }

    public void addComponentSuccessMessage(final String componentId,
            final String summary, final String detail) {
        FacesContext.getCurrentInstance().addMessage(componentId,
                new FacesMessage(summary, detail));
    }
    
    public void addComponentInfoMessage(final String componentId,
            final String message, final String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(SEVERITY_INFO, message, detail));
    }

    public void addComponentErrorMessage(final String componentId,
            final String summary, final String detail) {
        FacesContext.getCurrentInstance().addMessage(componentId,
                new FacesMessage(SEVERITY_ERROR, summary, detail));
    }

    public void addComponentErrorMessage(final String componentId,
            final String detail) {
        FacesContext.getCurrentInstance().addMessage(componentId,
                new FacesMessage(SEVERITY_ERROR,
                        getStringFromBundle("messages.error"), detail));
    }

    public void addComponentErrorMessage(final UIComponent component,
            final String summary, final String detail) {
        final FacesContext context = FacesContext.getCurrentInstance();
        final String componentId = component.getClientId(context);
        addComponentErrorMessage(componentId, summary, detail);
    }
    
    public void addComponentErrorMessage(final UIComponent component,
            final String detail) {
        final FacesContext context = FacesContext.getCurrentInstance();
        final String componentId = component.getClientId(context);
        addComponentErrorMessage(componentId, detail);
    }
}
