package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@FacesConverter("dataFileConverter")
public class DataFileConverter implements Converter {

    @EJB
    DataFileServiceBean dataFileService;

    @Override
    public Object getAsObject(final FacesContext facesContext, 
            final UIComponent component, final String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty()) {
            return EMPTY;
        } else {
            return dataFileService.find(new Long(submittedValue));
        }
    }

    @Override
    public String getAsString(final FacesContext facesContext, 
            final UIComponent component, final Object value) {
        if (value == null || value.equals(EMPTY)) {
            return EMPTY;
        } else {
            return ((DataFile) value).getId().toString();
        }
    }
}
