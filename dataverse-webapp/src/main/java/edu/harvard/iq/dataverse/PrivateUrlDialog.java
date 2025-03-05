package edu.harvard.iq.dataverse;

import org.omnifaces.cdi.ViewScoped;

import javax.inject.Named;

@SuppressWarnings("serial")
@ViewScoped
@Named("privateUrlDialog")
public class PrivateUrlDialog extends AbstractPrivateUrlDialog {

    @Override
    public boolean isAnonymized() {
        return false;
    }
}
