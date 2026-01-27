package edu.harvard.iq.dataverse.dataset.embargo;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.DateUtil.isBefore;
import static edu.harvard.iq.dataverse.common.DateUtil.todayPlusDays;
import static edu.harvard.iq.dataverse.common.DateUtil.todayPlusMonths;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DefaultDateFormat;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MaximumEmbargoLength;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.UIMessages;

/**
 * Backing bean responsible for handling dataset embargo
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("datasetEmbargoDialog")
public class DatasetEmbargoDialog implements Serializable {

    private Dataset dataset;

    private boolean renderEmbargoDialog = false;
    private Date currentEmbargoDate;

    private DatasetService datasetService;
    
    private SettingsServiceBean settings;
    private UIMessages ui;


    // -------------------- CONSTRUCTORS --------------------
    @Inject
    public DatasetEmbargoDialog(final DatasetService datasetService,
    		                    final SettingsServiceBean settings,
    		                    final UIMessages ui) {
    	this.datasetService = datasetService;
    	this.settings = settings;
    	this.ui = ui;
    }

    // -------------------- GETTERS --------------------

    public boolean isRenderEmbargoDialog() {
        return this.renderEmbargoDialog;
    }
    
    public void setRenderEmbargoDialog(final boolean renderEmbargoPopup) {
        this.renderEmbargoDialog = renderEmbargoPopup;
    }

    public Date getCurrentEmbargoDate() {
        return this.currentEmbargoDate;
    }
    
    public void setCurrentEmbargoDate(final Date currentEmbargoDate) {
        this.currentEmbargoDate = currentEmbargoDate;
    }
    
    public boolean isMaximumEmbargoLengthSet() {
        return getMaximumEmbargoLength() > 0;
    }

    public int getMaximumEmbargoLength() {
        return this.settings.getValueForKeyAsInt(MaximumEmbargoLength, 0);
    }
    
    public Date getMaximumEmbargoDate() {
        return isMaximumEmbargoLengthSet()
            ? todayPlusMonths(getMaximumEmbargoLength())
            : null;
    }

    public String getMaximumEmbargoDateForDisplay() {
    	return format(getMaximumEmbargoDate());
    }

    public Date getTomorrowsDate() {
        return todayPlusDays(1);
    }
    
    public String getDefaultDateFormat() {
    	return this.settings.getValueForKey(DefaultDateFormat, "yyyy-MM-dd");
    }

    // -------------------- LOGIC --------------------

    public void init(final Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Loads current embargo date and causes deaccession dialog to render
     */
    public void reloadAndRenderDialog() {
        initCurrentEmbargo();
        this.renderEmbargoDialog = true;
    }

    public void initCurrentEmbargo() {
        this.currentEmbargoDate = this.dataset.getEmbargoDate().getOrNull();
    }

    public String getCurrentEmbargoDateForDisplay() {
        return format(this.currentEmbargoDate);
    }

    public void validateEmbargoDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
        validateVersusMinimumDate(context, toValidate, embargoDate);
        validateVersusMaximumDate(context, toValidate, embargoDate);
    }

	public String updateEmbargoDate() {
		try {
			this.datasetService.setDatasetEmbargoDate(this.dataset, this.currentEmbargoDate);
			showSuccess();
		} catch (final Exception e) {
			showError();
		}
		return returnToDataset();
	}

	public String liftEmbargo() {
		try {
			this.datasetService.liftDatasetEmbargoDate(this.dataset);
			this.currentEmbargoDate = null;
			showSuccess();
		} catch (final Exception e) {
			showError();
		}
		return returnToDataset();
	}
    
    // -------------------- PRIVATE --------------------
    
    private void showSuccess() {
    	this.ui.addSuccessMessage(getStringFromBundle("dataset.embargo.save.successMessage"));
    }
    
    private void showError() {
    	this.ui.addErrorMessage(getStringFromBundle("dataset.embargo.lift.failureMessage"));
    }
    
    private String format(final Date date) {
    	return date != null 
    			? new SimpleDateFormat(getDefaultDateFormat()).format(date)
    			: EMPTY;
    	
    }

    private void validateVersusMaximumDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
    	
        if(embargoDate != null && isMaximumEmbargoLengthSet() &&
        	 !isBefore((Date) embargoDate, getMaximumEmbargoDate())) {
            this.ui.throwValidationException(getStringFromBundle(
            		"dataset.embargo.validate.max.failureMessage", getMaximumEmbargoDateForDisplay()));
        }
    }

    private void validateVersusMinimumDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
    	
    	if(embargoDate != null && isBefore((Date) embargoDate, getTomorrowsDate())) {
        	this.ui.throwValidationException(getStringFromBundle(
        			"dataset.embargo.validate.min.failureMessage"));
    	}
    }

    private String returnToDataset() {
        return "/dataset.xhtml?faces-redirect=true&persistentId="
        		.concat(this.dataset.getGlobalId().asString());
    }
}
