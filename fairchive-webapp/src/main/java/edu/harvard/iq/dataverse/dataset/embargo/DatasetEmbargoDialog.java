package edu.harvard.iq.dataverse.dataset.embargo;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DefaultDateFormat;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MaximumEmbargoLength;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;

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


    // -------------------- CONSTRUCTORS --------------------
    @Inject
    public DatasetEmbargoDialog(final DatasetService datasetService,
    		                    final SettingsServiceBean settings) {
    	this.datasetService = datasetService;
    	this.settings = settings;
    }

    // -------------------- GETTERS --------------------

    public boolean isRenderEmbargoDialog() {
        return this.renderEmbargoDialog;
    }

    /**
    *
    * @return current embargo date set on dataset
    */
    public Date getCurrentEmbargoDate() {
        return this.currentEmbargoDate;
    }
    
    public boolean isMaximumEmbargoLengthSet() {
        return getMaximumEmbargoLength() > 0;
    }

    public int getMaximumEmbargoLength() {
        return this.settings.getValueForKeyAsInt(MaximumEmbargoLength, 0);
    }
    
    public Optional<Date> getMaximumEmbargoDate() {
        if(isMaximumEmbargoLengthSet()) {
            return Optional.of(Date.from(Instant
                    .now().atOffset(ZoneOffset.UTC)
                    .plus(getMaximumEmbargoLength(), ChronoUnit.MONTHS)
                    .toInstant()));
        }
        return Optional.empty();
    }

    public String getMaximumEmbargoDateForDisplay() {
        return getMaximumEmbargoDate().isPresent() ? format(getMaximumEmbargoDate().get()) : "";
    }

    public Date getTomorrowsDate() {
        return Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS));
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
        return this.currentEmbargoDate != null ? format(this.currentEmbargoDate) : "";
    }

    public void validateEmbargoDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
        validateVersusMinimumDate(context, toValidate, embargoDate);
        validateVersusMaximumDate(context, toValidate, embargoDate);
    }

    public String updateEmbargoDate() {
        Try.of(() -> this.datasetService.setDatasetEmbargoDate(dataset, currentEmbargoDate))
                .onSuccess(ds -> JsfHelper.addSuccessMessage(getStringFromBundle("dataset.embargo.save.successMessage")))
                .onFailure(ds -> JsfHelper.addErrorMessage(getStringFromBundle("dataset.embargo.save.failureMessage")));
        return returnToDataset();

    }

    public String liftEmbargo() {
        Try.of(() -> this.datasetService.liftDatasetEmbargoDate(dataset))
                .onSuccess(ds -> this.currentEmbargoDate = null)
                .onSuccess(ds -> JsfHelper.addSuccessMessage(getStringFromBundle("dataset.embargo.lift.successMessage")))
                .onFailure(ds -> JsfHelper.addErrorMessage(getStringFromBundle("dataset.embargo.lift.failureMessage")));
        return returnToDataset();
    }
    
    // -------------------- PRIVATE --------------------
    
    private String format(final Date date) {
    	return new SimpleDateFormat(getDefaultDateFormat()).format(date);
    	
    }

    private void validateVersusMaximumDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
        if(isMaximumEmbargoLengthSet() &&
                !Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isAfter(getMaximumEmbargoDate().get().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    getStringFromBundle("dataset.embargo.validate.max.failureMessage", getMaximumEmbargoDateForDisplay()), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    private void validateVersusMinimumDate(final FacesContext context, 
    		final UIComponent toValidate, final Object embargoDate) {
        if(!Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isBefore(getTomorrowsDate().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, getStringFromBundle("dataset.embargo.validate.min.failureMessage"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    private String returnToDataset() {
        return "/dataset.xhtml?persistentId=" + this.dataset.getGlobalId().asString() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setRenderEmbargoDialog(final boolean renderEmbargoPopup) {
        this.renderEmbargoDialog = renderEmbargoPopup;
    }

    public void setCurrentEmbargoDate(final Date currentEmbargoDate) {
        this.currentEmbargoDate = currentEmbargoDate;
    }

}
