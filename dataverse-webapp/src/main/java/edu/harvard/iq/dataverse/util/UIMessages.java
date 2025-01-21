package edu.harvard.iq.dataverse.util;

import javax.ejb.Stateless;

@Stateless
public class UIMessages {
    
    public void addSuccessMessage(final String message) {
        JsfHelper.addSuccessMessage(message);
    }
    
    public void addErrorMessage(final String message) {
        JsfHelper.addErrorMessage(message);
    }
}
