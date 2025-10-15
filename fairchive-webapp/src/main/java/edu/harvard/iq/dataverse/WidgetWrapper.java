package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.omnifaces.cdi.ViewScoped;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * @author gdurand
 */
@SuppressWarnings("serial")
@ViewScoped
@Named
public class WidgetWrapper implements Serializable {

    private final static String WIDGET = "widget";
    private final static char SEPARATOR = '@';

    private Boolean widgetView;
    private String widgetHome;
    private String widgetScope;

    private boolean initWidget() {
        // first check for widgetScope; if not found use alias (if null then this is not a dataverse widget)
        if (this.widgetView == null) {
            String widgetParam = FacesContext.getCurrentInstance().
                    getExternalContext().getRequestParameterMap().get(WIDGET);
            // you are in widget view ONLY if this param is supplied AND you have the separator 
            this.widgetView = widgetParam != null && widgetParam.indexOf(SEPARATOR) != -1;

            if (this.widgetView) {
                this.widgetScope = widgetParam.substring(0, widgetParam.indexOf(SEPARATOR));
                this.widgetHome = widgetParam.substring(widgetParam.indexOf(SEPARATOR) + 1);
            }
        }
        return this.widgetView;
    }

    public boolean isWidgetView() {
        return initWidget();
    }
    
    public boolean isStandaloneView() {
        return ! isWidgetView();
    }

    public boolean isWidgetTarget(DvObject dvo) {
        if (isWidgetView()) {

            while (dvo != null) {
                if (dvo instanceof DataFile) {
                    if ("datafile".equals(this.widgetScope)) {
                        //todo: add logic for when we add file widgets
                    }
                } else if (dvo instanceof Dataset) {
                    switch (this.widgetScope) {
                        case "dataverse":
                            break; // keep looping
                        case "dataset":
                            if (dvo.getGlobalId().toString().equals(this.widgetHome)) {
                                return true;
                            }
                            break;
                        default:
                            return false; // scope is for lower type dvObject
                    }
                } else if (dvo instanceof Dataverse) {
                    if ("dataverse".equals(widgetScope)) {
                        if (((Dataverse) dvo).getAlias().equals(this.widgetHome)) {
                            return true;
                        }
                    } else {
                        return false; // scope is for lower type dvObject
                    }
                }

                dvo = dvo.getOwner();
            }
        }

        return false;
    }

    public String wrapURL(final String url) {
        if (isWidgetView()) {
            return url + getParamSeparator(url) + WIDGET +
                    '=' + this.widgetScope + SEPARATOR + this.widgetHome;
        } else {
            return url;
        }
    }

    private static char getParamSeparator(final String url) {
        return url.indexOf('?') > -1 ? '&' : '?';
    }

}
