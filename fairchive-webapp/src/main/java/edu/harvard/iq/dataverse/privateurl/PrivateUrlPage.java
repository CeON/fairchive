package edu.harvard.iq.dataverse.privateurl;

import java.io.Serializable;

import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@SuppressWarnings("serial")
@ViewScoped
@Named("PrivateUrlPage")
public class PrivateUrlPage implements Serializable {
    
    private String token;

    public String getToken() {
        return this.token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String redirect() {
        return "dataset.xhtml?token=".concat(this.token);
    }

}
