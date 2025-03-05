package edu.harvard.iq.dataverse;

import org.omnifaces.cdi.ViewScoped;

import javax.inject.Named;

@SuppressWarnings("serial")
@ViewScoped
@Named("anonymizedPrivateUrlDialog")
public class AnonymizedPrivateUrlDialog extends AbstractPrivateUrlDialog {

    @Override
    public boolean isAnonymized() {
        return true;
    }
}
