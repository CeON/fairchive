package edu.harvard.iq.dataverse.dataset;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ProvCollectionEnabled;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.PublicInstall;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRenderer;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRendererManager;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.NotAuthenticatedException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.importer.metadata.ImporterRegistry;
import edu.harvard.iq.dataverse.importer.metadata.MetadataImporter;
import edu.harvard.iq.dataverse.importers.ui.ImporterForm;
import edu.harvard.iq.dataverse.importers.ui.ImportersForView;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseForm;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.control.Try;

@SuppressWarnings("serial")
@ViewScoped
@Named("CreateDatasetPage")
public class CreateDatasetPage implements Serializable {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    private ImporterRegistry importerRegistry;
    private DataverseDao dataverseDao;
    private PermissionsWrapper permissionsWrapper;
    private SettingsServiceBean settingsService;
    private DatasetFieldsInitializer datasetFieldsInitializer;
    private DataverseSession session;
    private TermsOfUseFormMapper termsOfUseFormMapper;
    private UserDataFieldFiller userDataFieldFiller;
    private DatasetService datasetService;
    private InputFieldRendererManager inputFieldRendererManager;
    private DatasetFieldValidationService fieldValidationService;

    private Dataset dataset;
    private Long ownerId;
    private Long sourceDatasetId;

    private DatasetVersion workingVersion;
    private List<DataFile> newFiles = new ArrayList<>();
    private List<FileMetadata> selectedFiles = new ArrayList<>();

    private List<Template> dataverseTemplates = new ArrayList<>();
    private Template selectedTemplate;

    private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocksForEdit = new HashMap<>();
    private Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();

    private ImportersForView importers;
    private MetadataImporter selectedImporter;
    private ImporterForm importerForm;
    private AsyncExecutionService asyncExecutionService;
    private SaveDatasetProcess saveDatasetProcess;
    
    private ProvPopupFragmentBean provPopupFragmentBean;
    private IngestServiceBean ingestService;
    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    
    // -------------------- CONSTRUCTORS --------------------

    public CreateDatasetPage() { }

    @Inject
    public CreateDatasetPage(final ImporterRegistry importerRegistry, 
    		                 final DataverseDao dataverseDao,
                             final PermissionsWrapper permissionsWrapper, 
                             final SettingsServiceBean settingsService,
                             final DatasetFieldsInitializer datasetFieldsInitializer, 
                             final DataverseSession session,
                             final TermsOfUseFormMapper termsOfUseFormMapper, 
                             final UserDataFieldFiller userDataFieldFiller,
                             final DatasetService datasetService, 
                             final InputFieldRendererManager inputFieldRendererManager,
                             final DatasetFieldValidationService fieldValidationService, 
                             final AsyncExecutionService asyncExecutionService,
                             final ProvPopupFragmentBean provPopupFragmentBean,
                             final IngestServiceBean ingestService, 
                             final EjbDataverseEngine commandEngine,
                             final DataverseRequestServiceBean dvRequestService) {
        this.importerRegistry = importerRegistry;
        this.dataverseDao = dataverseDao;
        this.permissionsWrapper = permissionsWrapper;
        this.settingsService = settingsService;
        this.datasetFieldsInitializer = datasetFieldsInitializer;
        this.session = session;
        this.termsOfUseFormMapper = termsOfUseFormMapper;
        this.userDataFieldFiller = userDataFieldFiller;
        this.datasetService = datasetService;
        this.inputFieldRendererManager = inputFieldRendererManager;
        this.fieldValidationService = fieldValidationService;
        this.asyncExecutionService = asyncExecutionService;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.ingestService = ingestService;
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return this.dataset;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }
    
    public Long getSourceDatasetId() {
    	return this.sourceDatasetId;
    }

    public DatasetVersion getWorkingVersion() {
        return this.workingVersion;
    }

    public List<DataFile> getNewFiles() {
        return this.newFiles;
    }

    public List<FileMetadata> getSelectedFiles() {
        return this.selectedFiles;
    }

    public List<Template> getDataverseTemplates() {
        return this.dataverseTemplates;
    }

    public Template getSelectedTemplate() {
        return this.selectedTemplate;
    }

    public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocksForEdit() {
        return this.metadataBlocksForEdit;
    }

    public Map<DatasetFieldType, InputFieldRenderer> getInputRenderersByFieldType() {
        return this.inputRenderersByFieldType;
    }

    public ImportersForView getImporters() {
        return this.importers;
    }

    public MetadataImporter getSelectedImporter() {
        return this.selectedImporter;
    }

    public ImporterForm getImporterForm() {
        return this.importerForm;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        final Dataverse ownerDataverse = this.dataverseDao.find(this.ownerId);

        if (ownerDataverse == null) {
            return this.permissionsWrapper.notFound();
        }
        if (!this.permissionsWrapper.canIssueCreateDatasetCommand(ownerDataverse)) {
            return this.permissionsWrapper.notAuthorized();
        }

        this.dataverseTemplates = fetchApplicableTemplates(ownerDataverse);
        this.selectedTemplate = ownerDataverse.getDefaultTemplate();

        this.dataset = new Dataset();
        this.dataset.setOwner(ownerDataverse);

        this.importers = ImportersForView.createInitialized(this.dataset, 
        		this.importerRegistry.getImporters(), this.session.getLocale());

        this.workingVersion = this.dataset.getLatestVersion();
        resetDatasetFields();

        return StringUtils.EMPTY;
    }

    public void updateSelectedTemplate(final AjaxBehaviorEvent event) {
        resetDatasetFields();
    }

    public void checkSaveStatus() {
        if (getIsSaveRunning()) {
            JsfHelper.addFlashWarningMessage(getStringFromBundle("dataset.save.inprogress"));
        }
    }

    public boolean getIsSaveRunning() {
        return this.saveDatasetProcess != null &&
                this.saveDatasetProcess.getAddingFiles() != null &&
                !this.saveDatasetProcess.getAddingFiles().isDone();
    }

    public boolean getIsSaveStarted() {
        return (this.saveDatasetProcess != null && !this.saveDatasetProcess.hasPreconditionErrors());
    }

    public void save() {
        this.saveDatasetProcess = new SaveDatasetProcess();

        this.workingVersion.setDatasetFields(DatasetFieldUtil.flattenDatasetFieldsFromBlocks(this.metadataBlocksForEdit));

        final List<FieldValidationResult> fieldValidationResults = 
        		this.fieldValidationService.validateFieldsOfDatasetVersion(this.workingVersion);
        final Set<ConstraintViolation<FileMetadata>> constraintViolations = this.workingVersion.validateFileMetadata();
        if (!fieldValidationResults.isEmpty() || !constraintViolations.isEmpty()) {
            JsfHelper.addErrorMessage("", getStringFromBundle("dataset.message.validationErrorDetails"));
            this.saveDatasetProcess.setPreconditionErrors(true);
            return;
        }

        mapTermsOfUseInFiles(this.newFiles);

        final Try<Dataset> createDatasetOperation = 
        		Try.of(() -> this.datasetService.createDataset(this.dataset, this.selectedTemplate))
                .onFailure(NotAuthenticatedException.class,
                    ex -> handleErrorMessage(getStringFromBundle("dataset.create.authenticatedUsersOnly"), ex))
                .onFailure(EJBException.class,
                    ex -> handleErrorMessage(getStringFromBundle("dataset.message.createFailure"), ex))
                .onFailure(CommandException.class,
                    ex -> handleErrorMessage(getStringFromBundle("dataset.message.createFailure"), ex));

        if (createDatasetOperation.isFailure()) {
            this.saveDatasetProcess.setPreconditionErrors(true);
            return;
        }

        if (isProvenanceCollectionEnabled()) {
            this.provPopupFragmentBean.saveStageProvFreeformToLatestVersion();
        }

        //we need full refresh of the dataset to properly link with role assignments
        this.dataset = this.datasetService.find(this.dataset.getId());

        this.saveDatasetProcess.setAddingFiles(
        		this.asyncExecutionService.executeAsync(() -> 
        			this.datasetService.addFilesToDataset(this.dataset, this.newFiles)));

    }

    public String finalizeSave() {
        if (this.saveDatasetProcess == null || this.saveDatasetProcess.hasPreconditionErrors()) {
            return null;
        }

        final AuthenticatedUser user = retrieveAuthenticatedUser();
        
        //After dataset saved, then persist prov json data
        boolean hasProvenanceErrors = false;

        if (isProvenanceCollectionEnabled()) {
            try {
                this.provPopupFragmentBean.saveStagedProvJson(false, 
                		this.dataset.getLatestVersion().getFileMetadatas());
            } catch (AbstractApiBean.WrappedResponse ex) {
                logger.log(Level.SEVERE, null, ex);
                hasProvenanceErrors = true;
            }
        }
        
        final boolean showProvenanceErrors = hasProvenanceErrors;
        
        Try.of(() -> this.saveDatasetProcess.getAddingFiles().get())
            .onFailure(ex -> handleErrorMessage(getStringFromBundle("dataset.message.createSuccess.failedToSaveFiles"), ex))
            .onSuccess(addFilesResult -> updateVersion(addFilesResult))
            .onSuccess(addFilesResult -> handleSuccessOrPartialSuccessMessages(this.newFiles.size(), addFilesResult, showProvenanceErrors))
            .onSuccess(addFilesResult -> this.dataset = this.datasetService.find(this.dataset.getId()))
            .onSuccess(addFilesResult -> this.ingestService.startIngestJobsForDataset(this.dataset, user));

        return returnToDraftVersion();
    }

    public void initMetadataImportDialog() {
        this.importerForm = ImporterForm.createInitializedForm(this.selectedImporter, 
        		this.session.getLocale(), this::getMetadataBlocksForEdit);
    }

    public boolean isInstallationPublic() {
        return this.settingsService.isTrueForKey(PublicInstall);
    }

    // -------------------- PRIVATE --------------------

    private boolean isProvenanceCollectionEnabled() {
    	return this.settingsService.isTrueForKey(ProvCollectionEnabled);
    }
    
    private void updateVersion(final AddFilesResult addFilesResult) {
    	if (addFilesResult.getSavedFilesCount() > 0) {
    		this.dataset = this.commandEngine.submit(
    				new UpdateDatasetVersionCommand(this.dataset, 
    						this.dvRequestService.getDataverseRequest()));
    	}
    }
    
    private AuthenticatedUser retrieveAuthenticatedUser() {
        if (!this.session.isUserLoggedIn()) {
            throw new NotAuthenticatedException();
        }
        return this.session.getAuthenticatedUser();
    }
    
    private List<Template> fetchApplicableTemplates(final Dataverse dataverse) {
        final List<Template> templates = new ArrayList<>(dataverse.getTemplates());
        if (!dataverse.isTemplateRoot()) {
            templates.addAll(dataverse.getParentTemplates());
        }
        templates.sort(Template.comparator);
        return templates;
    }

    private void resetDatasetFields() {
        List<DatasetField> datasetFields = new ArrayList<>();

        if (this.selectedTemplate != null) {
            datasetFields = DatasetFieldUtil.copyDatasetFields(this.selectedTemplate.getDatasetFields());
        }

        datasetFields = this.datasetFieldsInitializer.prepareDatasetFieldsForEdit(datasetFields, 
        		this.dataset.getOwner().getMetadataBlockRootDataverse());

        if (this.session.isUserLoggedIn()) {
        	this.userDataFieldFiller.fillUserDataInDatasetFields(datasetFields, this.session.getAuthenticatedUser());
        }

        this.inputRenderersByFieldType = this.inputFieldRendererManager.obtainRenderersByType(datasetFields);

        this.metadataBlocksForEdit = this.datasetFieldsInitializer.
        		groupAndUpdateFlagsForEdit(datasetFields, this.dataset.getOwner().getMetadataBlockRootDataverse());

    }

    private void mapTermsOfUseInFiles(final List<DataFile> files) {
        for (final DataFile file : files) {
            final TermsOfUseForm termsOfUseForm = file.getFileMetadata().getTermsOfUseForm();
            final FileTermsOfUse termsOfUse = this.termsOfUseFormMapper.mapToFileTermsOfUse(termsOfUseForm);

            file.getFileMetadata().setTermsOfUse(termsOfUse);
        }
    }

    private void handleSuccessOrPartialSuccessMessages(final int filesToSaveCount, 
    		final AddFilesResult addFilesResult, final boolean hasProvenanceErrors) {

        if (filesToSaveCount == addFilesResult.getSavedFilesCount()) {
            JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataset.message.createSuccess"));
        } else if (addFilesResult.getSavedFilesCount() == 0) {
            JsfHelper.addFlashWarningMessage(getStringFromBundle("dataset.message.createSuccess.failedToSaveFiles"));
        } else {
            final String partialSuccessMessage = getStringFromBundle("dataset.message.createSuccess.partialSuccessSavingFiles",
            		addFilesResult.getSavedFilesCount(), filesToSaveCount);
            JsfHelper.addFlashWarningMessage(partialSuccessMessage);
        }

        if (hasProvenanceErrors) {
            JsfHelper.addFlashErrorMessage(getStringFromBundle("file.metadataTab.provenance.error"));
        }
    }

    private void handleErrorMessage(final String messageToUser, final Throwable ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        JsfHelper.addFlashErrorMessage(messageToUser);
    }

    private String returnToDraftVersion() {
        return "/dataset.xhtml?faces-redirect=true&version=DRAFT&persistentId=" 
                + this.dataset.getGlobalId();
    }
    
    public List<DatasetField> findCopySources(final String sourceId) {
        final List<DatasetField> sourceFields = new ArrayList<>();
        for (final List<DatasetFieldsByType> datasetFieldsByTypeList : this.metadataBlocksForEdit.values()) {
            for (final DatasetFieldsByType datasetFieldsByType : datasetFieldsByTypeList) {
                for (final DatasetField datasetField : datasetFieldsByType.getDatasetFields()) {
                    if (sourceId.equals(datasetField.getTypeName())) {
                        sourceFields.add(datasetField);
                    }
                }
            }
        }
        return sourceFields;
    }

    // -------------------- SETTERS --------------------

    public void setOwnerId(final Long id) {
        this.ownerId = id;
    }
    
    public void setSourceDatasetId(final Long id) {
    	this.sourceDatasetId = id;
    }

    public void setSelectedTemplate(final Template template) {
        this.selectedTemplate = template;
    }

    public void setSelectedImporter(final MetadataImporter importer) {
        this.selectedImporter = importer;
    }

    public static class SaveDatasetProcess {
        private boolean preconditionErrors = false;
        private CompletableFuture<AddFilesResult> addingFiles;

        public boolean hasPreconditionErrors() {
            return this.preconditionErrors;
        }

        public void setPreconditionErrors(final boolean preconditionErrors) {
            this.preconditionErrors = preconditionErrors;
        }

        public void setAddingFiles(final CompletableFuture<AddFilesResult> addingFiles) {
            this.addingFiles = addingFiles;
        }

        public CompletableFuture<AddFilesResult> getAddingFiles() {
            return this.addingFiles;
        }
    }
}
