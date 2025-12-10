package edu.harvard.iq.dataverse.dataset.embargo;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.config.URLValidator;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    SettingsServiceBean settingsService;


    // -------------------- CONSTRUCTORS --------------------
    @Inject
    public DatasetEmbargoDialog(DatasetService datasetService,
    		                    SettingsServiceBean settingsService) {
    	this.datasetService = datasetService;
    	this.settingsService = settingsService;
    }

    // -------------------- GETTERS --------------------

    public boolean isRenderEmbargoDialog() {
        return renderEmbargoDialog;
    }

    /**
    *
    * @return current embargo date set on dataset or [TODAY] whichever is greater
    */
    public Date getCurrentEmbargoDate() {
        return currentEmbargoDate;
    }

    public int getMaximumEmbargoLength() {
        return settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MaximumEmbargoLength);
    }
    
    public Option<Date> getMaximumEmbargoDate() {
        if(isMaximumEmbargoLengthSet()) {
            return Option.of(Date.from(Instant
                    .now().atOffset(ZoneOffset.UTC)
                    .plus(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaximumEmbargoLength), ChronoUnit.MONTHS)
                    .toInstant()));
        }
        return Option.none();
    }

    public String getMaximumEmbargoDateForDisplay() {
        SimpleDateFormat format = new SimpleDateFormat(settingsService.getValueForKey(SettingsServiceBean.Key.DefaultDateFormat));
        return getMaximumEmbargoDate().isDefined() ? format.format(getMaximumEmbargoDate().get()) : "";
    }

    public Date getTomorrowsDate() {
        return Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS));
    }

    // -------------------- LOGIC --------------------

    /**
     * Method that must be executed before any
     * further operations can be done on this dialog.
     * <p>
     * Note that it not initialize dataset versions
     * which is done in {@link #reloadAndRenderDialog()} to
     * enable lazy loading of them.
     */
    public void init(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Loads dataset versions and causes deaccession dialog to render
     */
    public void reloadAndRenderDialog() {
        initCurrentEmbargo();
        renderEmbargoDialog = true;
    }

    public void initCurrentEmbargo() {
        currentEmbargoDate = dataset.getEmbargoDate().getOrNull();
    }

    public boolean isMaximumEmbargoLengthSet() {
        return getMaximumEmbargoLength() > 0;
    }

    public String getCurrentEmbargoDateForDisplay() {
        SimpleDateFormat format = new SimpleDateFormat(settingsService.getValueForKey(SettingsServiceBean.Key.DefaultDateFormat));
        return currentEmbargoDate != null ? format.format(currentEmbargoDate) : "";
    }

    public void validateEmbargoDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        validateVersusMinimumDate(context, toValidate, embargoDate);
        validateVersusMaximumDate(context, toValidate, embargoDate);
    }

    public String updateEmbargoDate() {
        Try.of(() -> datasetService.setDatasetEmbargoDate(dataset, currentEmbargoDate))
                .onSuccess(ds -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.embargo.save.successMessage")))
                .onFailure(ds -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.embargo.save.failureMessage")));
        return returnToDataset();

    }

    public String liftEmbargo() {
        Try.of(() -> datasetService.liftDatasetEmbargoDate(dataset))
                .onSuccess(ds -> currentEmbargoDate = null)
                .onSuccess(ds -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.embargo.lift.successMessage")))
                .onFailure(ds -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.embargo.lift.failureMessage")));
        return returnToDataset();
    }
    
    // -------------------- PRIVATE --------------------

    private void validateVersusMaximumDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        if(isMaximumEmbargoLengthSet() &&
                !Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isAfter(getMaximumEmbargoDate().get().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("dataset.embargo.validate.max.failureMessage", getMaximumEmbargoDateForDisplay()), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    private void validateVersusMinimumDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        if(!Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isBefore(getTomorrowsDate().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.embargo.validate.min.failureMessage"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    private String returnToDataset() {
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId().asString() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setRenderEmbargoDialog(boolean renderEmbargoPopup) {
        this.renderEmbargoDialog = renderEmbargoPopup;
    }

    public void setCurrentEmbargoDate(Date currentEmbargoDate) {
        this.currentEmbargoDate = currentEmbargoDate;
    }

}
