package edu.harvard.iq.dataverse.datafile.page;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.IngestType.NON;
import static java.util.Arrays.stream;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.math.NumberUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datafile.DataFileCreator;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.datafile.pojo.RsyncInfo;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.dataset.OneAtATimeExecutionGuard;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestRequest;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseForm;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Option;
import io.vavr.control.Try;


/**
 * @author Leonid Andreev
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("EditDatafilesPage")
public class EditDatafilesPage implements java.io.Serializable {

    private static final long TEMP_VALID_TIME_MILLIS = 24 * 60 * 60 * 1000;

	private static final Logger logger = getLogger(EditDatafilesPage.class.getCanonicalName());

	private static final int NUMBER_OF_SCROLL_ROWS = 25;

    public enum FileEditMode {
        EDIT, UPLOAD, CREATE
    }

    private DatasetDao datasetDao;
    private DataFileServiceBean datafileDao;
    private DataFileCreator dataFileCreator;
    private PermissionServiceBean permissionService;
    private IngestServiceBean ingestService;
    private DataverseSession session;
    private SettingsServiceBean settings;
    private SystemConfig systemConfig;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ProvPopupFragmentBean provPopupFragmentBean;
    private DatasetVersionServiceBean datasetVersionService;
    private TermsOfUseFormMapper termsOfUseFormMapper;
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    private DatasetService datasetService;
    private FileService fileService;
    private DatasetThumbnailService datasetThumbnailService;
    private ImageThumbConverter imageThumbConverter;
    private DuplicatesService duplicatesService;

    private Dataset dataset = new Dataset();

    private String selectedFileIdsString = null;
    private FileEditMode mode = FileEditMode.EDIT;
    private List<FileMetadata> fileMetadatas = new ArrayList<>();


    private Long ownerId;
    private Long versionId;
    private List<DataFile> newFiles = new ArrayList<>();
    private List<DataFile> uploadedFiles = new ArrayList<>();
    private DataFileUploadInfo dataFileUploadInfo = new DataFileUploadInfo();
    private DatasetVersion workingVersion;
    private String dropBoxSelection = "";

    private String persistentId;

    private String versionString = "";

    private long currentBatchSize = 0L;
    private boolean ignoringMaxUploadLimit = false;

    private boolean saveEnabled = false;

    private Long maxFileUploadSizeInBytes = null;
    private Long multipleUploadFilesLimit = null;

    private List<SelectItem> termsOfUseSelectItems;
    private List<FileMetadata> selectedFiles;
    private List<DataFile> filesToBeDeleted = new ArrayList<>();

    private Boolean hasRsyncScript = false;

    /** The contents of the script. */
    private String rsyncScript = "";
    private String rsyncScriptFilename;
    private String warningMessageForPopUp;

    private String uploadWarningMessage = null;
    private String uploadSuccessMessage = null;
    private String uploadComponentId = null;

    private boolean uploadInProgress = false;

    private Map<String, String> temporaryThumbnailsMap = new HashMap<>();
    private Set<String> fileLabelsExisting = null;

    private Boolean lockedFromEditsVar;

    private FileMetadata fileMetadataSelectedForThumbnailPopup = null;
    private boolean alreadyDesignatedAsDatasetThumbnail = false;
    private FileMetadata selectedFile = null;
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null;
    private String ingestLanguageEncoding = null;
    private String savedLabelsTempFile = null;

    private boolean hasDuplicates;
    private List<DuplicatesService.DuplicateGroup> duplicatesList = new ArrayList<>();
    private List<FileMetadata> filesTableBackup = new ArrayList<>();
    private final OneAtATimeExecutionGuard<String> performSave = new OneAtATimeExecutionGuard<>(this::performSave);
    private TextRecognitionDialog textRecognitionDialog = new TextRecognitionDialog();

    // -------------------- CONSTRUCTORS --------------------

    public EditDatafilesPage() { }

    @Inject
    public EditDatafilesPage(final DatasetDao datasetDao, 
                             final DataFileServiceBean datafileDao,
                             final DataFileCreator dataFileCreator, 
                             final PermissionServiceBean permissionService,
                             final IngestServiceBean ingestService, 
                             final DataverseSession session,
                             final SettingsServiceBean settingsService, 
                             final SystemConfig systemConfig,
                             final DataverseRequestServiceBean dvRequestService, 
                             final PermissionsWrapper permissionsWrapper,
                             final FileDownloadHelper fileDownloadHelper, 
                             final ProvPopupFragmentBean provPopupFragmentBean,
                             final DatasetVersionServiceBean datasetVersionService,
                             final TermsOfUseFormMapper termsOfUseFormMapper, 
                             final TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory,
                             final DatasetService datasetService, 
                             final FileService fileService,
                             final DatasetThumbnailService datasetThumbnailService, 
                             final ImageThumbConverter imageThumbConverter,
                             final DuplicatesService duplicatesService) {
        this.datasetDao = datasetDao;
        this.datafileDao = datafileDao;
        this.dataFileCreator = dataFileCreator;
        this.permissionService = permissionService;
        this.ingestService = ingestService;
        this.session = session;
        this.settings = settingsService;
        this.systemConfig = systemConfig;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.datasetVersionService = datasetVersionService;
        this.termsOfUseFormMapper = termsOfUseFormMapper;
        this.termsOfUseSelectItemsFactory = termsOfUseSelectItemsFactory;
        this.datasetService = datasetService;
        this.fileService = fileService;
        this.datasetThumbnailService = datasetThumbnailService;
        this.imageThumbConverter = imageThumbConverter;
        this.duplicatesService = duplicatesService;
    }

    // -------------------- GETTERS --------------------

    public boolean isIgnoringMaxUploadLimit() {
        return this.ignoringMaxUploadLimit;
    }
    
    public String getSelectedFileIds() {
        return this.selectedFileIdsString;
    }

    public FileEditMode getMode() {
        return this.mode;
    }

    public Long getMaxFileUploadSizeInBytes() {
        return this.maxFileUploadSizeInBytes;
    }

    // The number of files the GUI user is allowed to upload in one batch,
    // via drag-and-drop, or through the file select dialog. Now configurable
    // in the Settings table.
    public Long getMaxNumberOfFiles() {
        return this.multipleUploadFilesLimit;
    }

    public String getGlobalId() {
        return this.persistentId;
    }

    public String getPersistentId() {
        return this.persistentId;
    }

    public String getDropBoxSelection() {
        return this.dropBoxSelection;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setSelectedFileIds(final String fileIds) {
        this.selectedFileIdsString = fileIds;
    }

    public DatasetVersion getWorkingVersion() {
        return this.workingVersion;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public Long getVersionId() {
        return this.versionId;
    }

    public long getCurrentBatchSize() {
        return this.currentBatchSize;
    }

    public List<FileMetadata> getSelectedFiles() {
        return this.selectedFiles;
    }

    public String getVersionString() {
        return this.versionString;
    }

    public Boolean isHasRsyncScript() {
        return this.hasRsyncScript;
    }

    public String getRsyncScript() {
        return this.rsyncScript;
    }

    public String getRsyncScriptFilename() {
        return this.rsyncScriptFilename;
    }

    public String getWarningMessageForPopUp() {
        return this.warningMessageForPopUp;
    }

    public FileMetadata getSelectedFile() {
        return this.selectedFile;
    }

    public FileMetadata getFileMetadataSelectedForIngestOptionsPopup() {
        return this.fileMetadataSelectedForIngestOptionsPopup;
    }

    public void setMode(final FileEditMode mode) {
        this.mode = mode;
    }

    public List<SelectItem> getTermsOfUseSelectItems() {
        return this.termsOfUseSelectItems;
    }

    public boolean getHasDuplicates() {
        return this.hasDuplicates;
    }

    public List<DuplicatesService.DuplicateGroup> getDuplicatesList() {
        return this.duplicatesList;
    }

    // -------------------- LOGIC --------------------

    public List<FileMetadata> getFileMetadatas() {
        return this.fileMetadatas;
    }

    /**
     *   The method below is for setting up the p:dataTable component
     *   used to display the uploaded files, or the files selected for editing.
     *   It supplies the value of the component attribute "scrollable".
     *   When we have more than NUMBER_OF_SCROLL_ROWS worth of files (currently
     *   set to 25), we will add a scroller to the table, showing NUMBER_OF_SCROLL_ROWS
     *   at a time; thus making the page a little bit more usable.
     *   When there is fewer rows, however, the attribute needs to be set to
     *   false - because otherwise some (idiosyncratic) amount of white space
     *   is added to the bottom of the table, making the page look silly.
     */
    public boolean isScrollable() {
        return !(this.fileMetadatas == null 
                || this.fileMetadatas.size() <= NUMBER_OF_SCROLL_ROWS + 1);
    }

    /**
     *  This may be null, signifying unlimited download size.
     */
    public String getHumanMaxFileUploadSize() {
        return getMaxFileUploadSizeInBytes() == null
                ? EMPTY
                : bytesToHumanReadable(getMaxFileUploadSizeInBytes());
    }

    public boolean isUnlimitedUploadFileSize() {
        return this.maxFileUploadSizeInBytes == null;
    }

    public String getHumanMaxBatchUploadSize() {
        Long batchSize = getMaxBatchSize();
        return batchSize == null || batchSize.equals(0L)
                ? EMPTY
                : bytesToHumanReadable(batchSize);
    }

    public String getUploadBatchTooBigMessage() {
        return getStringFromBundle("dataset.file.uploadBatchTooBig", 
                getHumanMaxBatchUploadSize());
    }

    public void reset() { }

    public String getDropBoxKey() {
        // Site-specific DropBox application registration key is configured
        // via a JVM option under glassfish.
        // if (true) return "some-test-key";  // for debugging
        return this.settings.getValueForKey(Key.DropboxKey);
    }

    public String initCreateMode(final DatasetVersion version, 
            final List<DataFile> newFilesList, 
            final List<FileMetadata> selectedFileMetadatasList) {
        logger.fine("Initializing Edit Files page in CREATE mode;");
        if (version == null) {
            return this.permissionsWrapper.notFound();
        }

        this.maxFileUploadSizeInBytes = this.settings.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes);
        this.multipleUploadFilesLimit = this.settings.getValueForKeyAsLong(Key.MultipleUploadFilesLimit);
        this.workingVersion = version;
        this.dataset = version.getDataset();
        this.mode = FileEditMode.CREATE;
        this.newFiles = newFilesList;
        this.uploadedFiles = new ArrayList<>();
        this.selectedFiles = selectedFileMetadatasList;
        this.termsOfUseSelectItems = this.termsOfUseSelectItemsFactory.buildLicenseSelectItems();
        this.saveEnabled = true;
        return null;
    }


    public String init() {
        this.fileMetadatas = new ArrayList<>();
        this.newFiles = new ArrayList<>();
        this.uploadedFiles = new ArrayList<>();
        cleanupTempFiles();

        this.maxFileUploadSizeInBytes = this.settings.getValueForKeyAsLong(Key.MaxFileUploadSizeInBytes);
        this.multipleUploadFilesLimit = this.settings.getValueForKeyAsLong(Key.MultipleUploadFilesLimit);
        this.termsOfUseSelectItems = this.termsOfUseSelectItemsFactory.buildLicenseSelectItems();

        if (this.dataset.getId() != null) {
            // Set Working Version and Dataset by Dataset Id
            this.dataset = this.datasetDao.find(this.dataset.getId());
            // Is the Dataset harvested? (we don't allow editing of harvested files)
            if (this.dataset == null || this.dataset.isHarvested()) {
                return this.permissionsWrapper.notFound();
            }
        } else {
            return this.permissionsWrapper.notFound();
        }
        workingVersion = dataset.getEditVersion();

        if (!permissionsWrapper.canCurrentUserUpdateDataset(this.dataset)) {
            return this.permissionsWrapper.notAuthorized();
        }
        if (this.dataset.isInReview() && 
                !this.permissionsWrapper.canUpdateAndPublishDataset(this.dataset)) {
            return this.permissionsWrapper.notAuthorized();
        }

        if (this.mode == FileEditMode.EDIT) {
            final Set<Long> selectedFileIds = stream(split(trimToEmpty(this.selectedFileIdsString), ','))
                    .map(NumberUtils::toLong)
                    .filter(fileId -> fileId != 0)
                    .collect(toSet());

            if (selectedFileIds.isEmpty()) {
                logger.fine("No numeric file ids supplied to the page, in the edit mode. Redirecting to the 404 page.");
                return this.permissionsWrapper.notFound();
            }

            logger.fine("The page is called with " + selectedFileIds.size() +
                    " file ids.");

            populateFileMetadatas(selectedFileIds);
            setUpRsync();
            if (this.fileMetadatas.isEmpty()) {
                return this.permissionsWrapper.notFound();
            }
        }

        this.saveEnabled = true;
        if (this.mode == FileEditMode.UPLOAD && this.workingVersion.getFileMetadatas().isEmpty() 
                && this.systemConfig.isRsyncUpload()) {
            setUpRsync();
        }
        return null;
    }

    public boolean isInUploadMode() {
        return this.mode == FileEditMode.UPLOAD;
    }

    public String getMultiUploadDetailsMessage() {
        return getStringFromBundle("dataset.message.uploadFilesSingle.message",
                this.systemConfig.getGuidesBaseUrl(this.session.getLocale()), 
                this.systemConfig.getGuidesVersion());
    }

    public boolean isInstallationPublic() {
        return this.settings.isTrueForKey(Key.PublicInstall);
    }

    // This deleteFilesCompleted method is used in editFilesFragment.xhtml
    public void deleteFilesCompleted() { }

    public void deleteFiles() {
        logger.fine("entering bulk file delete (EditDataFilesPage)");

        final String fileNames = this.selectedFiles.stream()
                .map(FileMetadata::getLabel)
                .collect(joining(", "));

        for (final FileMetadata markedForDelete : getSelectedFiles()) {
            logger.fine(String.format("delete requested on file %s, file metadata id: %d, datafile id: %d, page is in edit mode %s",
                    markedForDelete.getLabel(), markedForDelete.getId(), markedForDelete.getDataFile().getId(), this.mode.name()));

            // Has this filemetadata been saved already? Or is it a brand new filemetadata, created as part of a brand
            // new version, created when the user clicked 'delete', that hasn't been saved in the db yet?
            if (!markedForDelete.isNew()) {
                logger.fine("this is a filemetadata from an existing draft version");
                // so all we remove is the file from the fileMetadatas (from the file metadatas attached to the
                // editVersion, and from the display list of file metadatas that are being edited) and let the delete be
                // handled in the command (by adding it to the filesToBeDeleted list):

                this.fileMetadatas.remove(markedForDelete);
                this.filesToBeDeleted.add(markedForDelete.getDataFile());
            } else {
                logger.fine("this is a brand-new (unsaved) filemetadata");
                // If the bean is in the 'CREATE' mode, the page is using dataset.getEditVersion().getFileMetadatas()
                // directly, so there's no need to delete this meta from the local fileMetadatas list. (but doing both
                // just adds a no-op and won't cause an error)

                // delete the filemetadata from the local display list and version
                removeFileMetadataFromList(this.fileMetadatas, markedForDelete);
                removeFileMetadataFromList(this.dataset.getEditVersion().getFileMetadatas(), markedForDelete);
            }

            if (markedForDelete.getDataFile().isNew()) {
                DataFile dataFileToDelete = markedForDelete.getDataFile();
                logger.fine("this is a brand new file.");
                // the file was just added during this step, so in addition to removing it from the fileMetadatas lists
                // (above), we also remove it from the newFiles list and the dataset's files, so it never gets saved.

                removeDataFileFromList(this.dataset.getFiles(), dataFileToDelete);
                removeDataFileFromList(this.newFiles, dataFileToDelete);
                deleteTempFile(dataFileToDelete);
                updateCurrentBatchSizeForDeletedDataFile(dataFileToDelete);
            }
        }
        logger.fine("Files was removed from the list - changes will persist after save changes will be executed");
        JsfHelper.addFlashSuccessMessage(getStringFromBundle("file.deleted.success", fileNames));
        this.hasDuplicates = hasDuplicatesInUploadedFiles(this.newFiles);
    }

    /**
     * The method is used to clean temporary files on various events
     * such as closing the upload tab or logging out.
     */
	@PreDestroy
	void cleanTempFilesOnViewDestroy() {
	    this.newFiles.forEach(this::deleteTempFile);
	    this.uploadedFiles.forEach(this::deleteTempFile);
    }

    public void checkSaveStatus() {
        if (performSave.isRunning()) {
            JsfHelper.addFlashWarningMessage(BundleUtil.getStringFromBundle("dataset.save.inprogress"));
        } else {
            // refreshing the form, allowing it to be un-blocked
            PrimeFaces.current().ajax().update("datasetForm");
        }
    }

    public boolean getIsSaveRunning() {
        return performSave.isRunning();
    }

    public String save() {
        return performSave.execute().getOrElse(EMPTY);
    }

    private String performSave() {
        // Once all the filemetadatas pass the validation, we'll only allow the user to try to save once – this it to
        // prevent them from creating multiple DRAFT versions, if the page gets stuck in that state where it
        // successfully creates a new version, but can't complete the remaining tasks. -- L.A. 4.2

        if (!this.saveEnabled) {
            return EMPTY;
        }

        final int oldFilesNumber = this.workingVersion.getFileMetadatas().size();
        final int newFilesNumber = this.newFiles.size();
        final int expectedFilesTotal = oldFilesNumber + newFilesNumber;

        if (newFilesNumber > 0) {
            // SEK 10/15/2018 only apply the following tests if dataset has already been saved.
            if (this.dataset.getId() != null) {
                final Dataset lockTest = this.datasetDao.find(this.dataset.getId());
                // SEK 09/19/18 Get Dataset again to test for lock just in case the user downloads the rsync script via
                // the api while the edit files page is open and has already loaded a file in http upload for Dual Mode
                if (this.dataset.isLockedFor(Reason.DcmUpload) 
                        || lockTest.isLockedFor(Reason.DcmUpload)) {
                    logger.log(INFO, "Couldn''t save dataset: {0}", "DCM script has been downloaded for " +
                            "this dataset. Additional files are not permitted.");
                    populateDatasetUpdateFailureMessage();
                    return null;
                }
                for (final DatasetVersion version : lockTest.getVersions()) {
                    if (version.isHasPackageFile()) {
                        logger.log(INFO, ResourceBundle.getBundle("Bundle")
                                .getString("file.api.alreadyHasPackageFile"));
                        populateDatasetUpdateFailureMessage();
                        return null;
                    }
                }
            }

            for (final DataFile newFile : this.newFiles) {
                final TermsOfUseForm termsOfUseForm = newFile.getFileMetadata()
                        .getTermsOfUseForm();
                final FileTermsOfUse termsOfUse = termsOfUseFormMapper
                        .mapToFileTermsOfUse(termsOfUseForm);
                newFile.getFileMetadata().setTermsOfUse(termsOfUse);
            }

            // Try to save the NEW files permanently:
            final List<DataFile> filesAdded = this.ingestService
                    .saveAndAddFilesToDataset(this.workingVersion, this.newFiles);

            // reset the working list of fileMetadatas, as to only include the ones
            // that have been added to the version successfully:
            this.fileMetadatas.clear();
            for (final DataFile addedFile : filesAdded) {
                this.fileMetadatas.add(addedFile.getFileMetadata());
            }
        }

        if (this.settings.isTrueForKey(Key.ProvCollectionEnabled)) {
            this.provPopupFragmentBean.updatePageMetadatasWithProvFreeform(this.fileMetadatas);
            try {
                // Note that the user may have uploaded provenance metadata file(s) for some of the new files that have
                // since failed to be permanently saved in storage (in the ingestService.saveAndAddFilesToDataset()
                // step, above); these files have been dropped from the fileMetadatas list, and we are not adding them
                // to the dataset; but the provenance update set still has entries for these failed files, so we are
                // passing the fileMetadatas list to the saveStagedProvJson() method below - so that it doesn't attempt
                // to save the entries that are no longer valid.
                this.provPopupFragmentBean.saveStagedProvJson(false, this.fileMetadatas);
            } catch (AbstractApiBean.WrappedResponse ex) {
                JsfHelper.addFlashErrorMessage(getStringFromBundle("file.metadataTab.provenance.error"));
                logger.log(SEVERE, null, ex);
            }
        }

        logger.fine("issuing the dataset update command");
        Map<Long, String> deleteStorageLocations = null;

        if (!this.filesToBeDeleted.isEmpty()) {
            deleteStorageLocations = this.datafileDao.getPhysicalFilesToDelete(this.filesToBeDeleted);
        }

        Try<Dataset> updateDatasetOperation = Try.of(() -> this.datasetVersionService.updateDatasetVersion(this.workingVersion, this.filesToBeDeleted, true))
                .onSuccess(updatedDataset -> this.dataset = updatedDataset)
                .onFailure(ex -> {
                    logger.log(SEVERE, "Couldn't update dataset with id: " + 
                            this.workingVersion.getDataset().getId(), ex);
                    populateDatasetUpdateFailureMessage();
                });

        if (updateDatasetOperation.isFailure()) {
            return EMPTY;
        }

        // Have we just deleted some draft datafiles successfully? Finalize the physical file deletes:
        // (DataFileService will double-check that the datafiles no longer exist in the database, before attempting to
        // delete the physical files)
        if (deleteStorageLocations != null) {
            this.datafileDao.finalizeFileDeletes(deleteStorageLocations);
        }

        this.saveEnabled = false;

        if (!this.newFiles.isEmpty()) {
            logger.fine("clearing newfiles list.");
            this.newFiles.clear();
        }

        this.workingVersion = this.dataset.getEditVersion();
        logger.fine("working version id: " + workingVersion.getId());

        final int filesTotal = this.workingVersion.getFileMetadatas().size();
        if (newFilesNumber == 0 || filesTotal == expectedFilesTotal) {
            JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataset.message.filesSuccess"));
        } else if (filesTotal == oldFilesNumber) {
            JsfHelper.addFlashErrorMessage(getStringFromBundle("dataset.message.addFiles.Failure"));
        } else {
            JsfHelper.addFlashWarningMessage(getStringFromBundle(
                    "dataset.message.addFiles.partialSuccess", 
                    filesTotal - oldFilesNumber, newFilesNumber));
        }

        // Call Ingest Service one more time to queue the data ingest jobs for asynchronous execution:
        if (this.mode == FileEditMode.UPLOAD) {
            this.ingestService.startIngestJobsForDataset(this.dataset, 
                    (AuthenticatedUser) this.session.getUser());
        }

        logger.fine("Redirecting to the dataset page, from the edit/upload page.");
        return returnToDraftVersion();
    }

    public String returnToDatasetOnly() {
        this.dataset = this.datasetDao.find(dataset.getId());
        return "/dataset.xhtml?persistentId=" + this.dataset.getGlobalId().asString() 
                + "&faces-redirect=true";
    }

    public String getPageTitle(final boolean datasetPage) {
        return datasetPage || showFileUploadFragment()
                ? getStringFromBundle("file.uploadFiles")
                : getStringFromBundle("file.editFiles") + " - " + 
                        this.workingVersion.getParsedTitle();
    }

    public String cancel() {
        this.uploadInProgress = false;
        // Files that have been finished and are now in the lower list on the page
        for (final DataFile newFile : this.newFiles) {
            deleteTempFile(newFile);
        }
        // Files in the upload process but not yet finished
        for (final DataFile newFile : this.uploadedFiles) {
            deleteTempFile(newFile);
        }
        if (this.workingVersion.getId() != null) {
            return returnToDraftVersion();
        }
        return returnToDatasetOnly();
    }

    public boolean allowMultipleFileUpload() {
        return true;
    }

    public boolean showFileUploadFragment() {
        return this.mode == FileEditMode.UPLOAD || this.mode == FileEditMode.CREATE;
    }

    public boolean showFileUploadComponent() {
        return this.mode == FileEditMode.UPLOAD || this.mode == FileEditMode.CREATE;
    }

    /**
     * Using information from the DropBox choose, ingest the chosen files
     * https://www.dropbox.com/developers/dropins/chooser/js
     */
    public void handleDropBoxUpload(final ActionEvent event) throws IOException {
        if (!this.uploadInProgress) {
            this.uploadInProgress = true;
        }
        logger.fine("handleDropBoxUpload");
        this.uploadComponentId = event.getComponent().getClientId();

        // Read JSON object from the output of the DropBox Chooser:
        final JsonReader dbJsonReader = Json.createReader(new StringReader(this.dropBoxSelection));
        final JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();

        // Iterate through the Dropbox file information (JSON)
        final List<String> localWarningMessages = new ArrayList<>();
        for (int i = 0; i < dbArray.size(); i++) {
            final JsonObject dbObject = dbArray.getJsonObject(i);

            // Parse information for a single file
            final String fileLink = dbObject.getString("link");
            final String fileName = dbObject.getString("name");
            final int fileSize = dbObject.getInt("bytes");

            logger.fine("DropBox url: " + fileLink + ", filename: " 
                        + fileName + ", size: " + fileSize);

            // Check file size
            //  - Max size NOT specified in db: default is unlimited
            //  - Max size specified in db: check too make sure file is within limits
            if (!isUnlimitedUploadFileSize() && fileSize > getMaxFileUploadSizeInBytes()) {
                String warningMessage = "Dropbox file \"" + fileName + 
                        "\" exceeded the limit of " + fileSize 
                        + " bytes and was not uploaded.";
                localWarningMessages.add(warningMessage);
                continue;
            }

            final GetMethod dropBoxMethod = new GetMethod(fileLink);
            List<DataFile> datafiles = new ArrayList<>();

            // Send it through the ingest service
            try (final InputStream dropBoxStream = getDropBoxContent(dropBoxMethod)) {
                // Note: A single uploaded file may produce multiple datafiles -
                // for example, multiple files can be extracted from an uncompressed
                // zip file.
                datafiles = this.dataFileCreator.createDataFiles(dropBoxStream, 
                        fileName, "application/octet-stream");
            } catch (final IOException | FileExceedsMaxSizeException ex) {
                logger.log(SEVERE, "Error during ingest of DropBox file {0} from link {1}", 
                        new Object[]{fileName, fileLink});
                continue;
            } catch (final VirusFoundException e) {
                localWarningMessages.add(getStringFromBundle("dataset.file.uploadScannerWarning"));
                continue;
            } finally {
                dropBoxMethod.releaseConnection();
            }

            this.uploadWarningMessage = processUploadedFileList(datafiles);
            logger.fine("Warning message during upload: " + this.uploadWarningMessage);

            if (!this.uploadInProgress) {
                logger.warning("Upload in progress cancelled");
                for (final DataFile newFile : datafiles) {
                    deleteTempFile(newFile);
                }
            }
        }

        if (!localWarningMessages.isEmpty()) {
            this.uploadWarningMessage = this.uploadWarningMessage == null
                    ? join(localWarningMessages, "; ")
                    : this.uploadWarningMessage + "; " + join(localWarningMessages, "; ");
        }
    }

    public void downloadRsyncScript() {
        final FacesContext ctx = FacesContext.getCurrentInstance();
        final HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        final String contentDispositionString = "attachment;filename=" + this.rsyncScriptFilename;
        response.setHeader("Content-Disposition", contentDispositionString);

        try {
            final ServletOutputStream out = response.getOutputStream();
            out.write(getRsyncScript().getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (final IOException e) {
            logger.warning("Problem getting bytes from rsync script: " + e);
            return;
        }

        // If the script has been successfully downloaded, lock the dataset:
        final String lockInfoMessage = "script downloaded";
        final DatasetLock lock = this.datasetDao.addDatasetLock(dataset.getId(), Reason.DcmUpload, this.session.isUserLoggedIn() 
                ? ((AuthenticatedUser) session.getUser()).getId() 
                : null, lockInfoMessage);
        if (lock != null) {
            this.dataset.addLock(lock);
        } else {
            logger.log(WARNING, "Failed to lock the dataset (dataset id={0})", 
                    this.dataset.getId());
        }
    }

    public void uploadFinished() {
        // This method is triggered from the page, by the <p:upload ... onComplete=... attribute.
        // Note that its behavior is different from that of of <p:upload ... onStart=...
        // that's triggered only once, even for a multiple file upload. In contrast,
        // onComplete=... gets executed for each of the completed multiple upload events.
        // So when you drag-and-drop a bunch of files, you CANNOT rely on onComplete=...
        // to notify the page when the batch finishes uploading! There IS a way
        // to detect ALL the current uploads completing: the p:upload widget has
        // the property "files", that contains the list of all the files currently
        // uploading; so checking on the size of the list tells you if any uploads
        // are still in progress. Once it's zero, you know it's all done.
        // This is super important - because if the user is uploading 1000 files
        // via drag-and-drop, you don't want to re-render the entire page each
        // time every single of the 1000 uploads finishes!
        // (check editFilesFragment.xhtml for the exact code handling this; and
        // http://stackoverflow.com/questions/20747201/when-multiple-upload-is-finished-in-pfileupload
        // for more info). -- 4.6
        logger.fine("upload finished");

        // Add the file(s) added during this last upload event, single or multiple,
        // to the full list of new files, and the list of filemetadatas
        // used to render the page:

        for (final DataFile dataFile : this.uploadedFiles) {
            this.fileMetadatas.add(dataFile.getFileMetadata());
            this.newFiles.add(dataFile);
        }
        if (this.uploadInProgress) {
            this.uploadedFiles = new ArrayList<>();
            this.uploadInProgress = false;
        }
        // refresh the warning message below the upload component, if exists:
        if (this.uploadComponentId != null && this.uploadWarningMessage != null) {
            FacesContext.getCurrentInstance()
                    .addMessage(this.uploadComponentId, 
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                                    getStringFromBundle("dataset.file.uploadWarning"), 
                                    this.uploadWarningMessage));
        } else if (this.uploadComponentId != null && this.uploadSuccessMessage != null) {
            FacesContext.getCurrentInstance()
                    .addMessage(this.uploadComponentId, 
                            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                                    getStringFromBundle("dataset.file.uploadWorked"), 
                                    this.uploadSuccessMessage));
        }

        this.uploadWarningMessage = null;
        this.uploadSuccessMessage = null;

        this.hasDuplicates = hasDuplicatesInUploadedFiles(this.newFiles);
    }

    public Long getMaxBatchSize() {
        return this.settings.getValueForKeyAsLong(Key.SingleUploadBatchMaxSize);
    }
    
    private boolean sizeExceedsLimit(final long fileSize) {
        if (isSuperuserLoggedIn() && this.ignoringMaxUploadLimit) {
            return false;
        } else {
            return getMaxBatchSize() > 0
                    && (this.currentBatchSize + fileSize) > getMaxBatchSize();
        }
    }

    /**
     * Handle native file replace
     */
    public void handleFileUpload(final FileUploadEvent event) throws IOException {
        final UploadedFile uploadedFile = event.getFile();
        final long fileSize = uploadedFile.getSize();

        if (sizeExceedsLimit(fileSize)) {
            this.uploadWarningMessage = getUploadBatchTooBigMessage();
            this.uploadComponentId = event.getComponent().getClientId();
        } else {
            this.uploadInProgress = true;
            this.currentBatchSize += fileSize;

            try (final InputStream inputStream = uploadedFile.getInputStream()) {
                // Note: A single uploaded file may produce multiple datafiles -
                // for example, multiple files can be extracted from an uncompressed
                // zip file.
                final List<DataFile> files = this.dataFileCreator.createDataFiles(
                        inputStream, uploadedFile.getFileName(), 
                        uploadedFile.getContentType(), this.ignoringMaxUploadLimit);
                this.dataFileUploadInfo.addSizeAndDataFiles(fileSize, files);

                // These raw datafiles are then post-processed, in order to drop any
                // files
                // already in the dataset/already uploaded, and to correct duplicate
                // file names, etc.
                final String warningMessage = processUploadedFileList(files);

                if (warningMessage != null) {
                    this.uploadWarningMessage = warningMessage;
                    // save the component id of the p:upload widget, so that we could
                    // send an info message there, from elsewhere in the code:
                    this.uploadComponentId = event.getComponent().getClientId();
                }
                if (!this.uploadInProgress) {
                    logger.warning("Upload in progress cancelled");
                    files.forEach(this::deleteTempFile);
                }
            } catch (final EJBException | IOException
                    | FileExceedsMaxSizeException ex) {
                logger.warning("Failed to process and/or save the file " +
                        uploadedFile.getFileName() + "; " + ex.getMessage());
                this.uploadWarningMessage = getUploadBatchTooBigMessage();
                this.uploadComponentId = event.getComponent().getClientId();
            } catch (final VirusFoundException e) {
                this.uploadWarningMessage = getStringFromBundle(
                        "dataset.file.uploadScannerWarning");
                this.uploadComponentId = event.getComponent().getClientId();
            }
        }
    }

    public boolean isTemporaryPreviewAvailable(final String fileSystemId, 
            final String mimeType) {
        if (this.temporaryThumbnailsMap.get(fileSystemId) != null 
                && !this.temporaryThumbnailsMap.get(fileSystemId).isEmpty()) {
            return true;
        }

        if ("".equals(this.temporaryThumbnailsMap.get(fileSystemId))) {
            // we've already looked once - and there's no thumbnail.
            return false;
        }

        final String filesRootDirectory = systemConfig.getFilesDirectory();
        final String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        final String imageThumbFileName = fileSystemName + ".thumb" + 
                    ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE;

        // ATTENTION! TODO: the current version of the method below may not be checking if files are already cached!
        if ("application/pdf".equals(mimeType)) {
            this.imageThumbConverter.generatePDFThumbnailFromFile(fileSystemName,
                    ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE, imageThumbFileName);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            this.imageThumbConverter.generateImageThumbnailFromFile(fileSystemName, 
                    ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE, imageThumbFileName);
        }

        final File imageThumbFile = new File(imageThumbFileName);
        if (imageThumbFile.exists()) {
            final String previewAsBase64 = this.imageThumbConverter.getImageAsBase64FromFile(imageThumbFile);
            if (previewAsBase64 != null) {
                this.temporaryThumbnailsMap.put(fileSystemId, previewAsBase64);
                return true;
            } else {
                this.temporaryThumbnailsMap.put(fileSystemId, EMPTY);
            }
        }
        return false;
    }

    public String getTemporaryPreviewAsBase64(final String fileSystemId) {
        return this.temporaryThumbnailsMap.get(fileSystemId);
    }
    
    public boolean isSuperuserLoggedIn() {
        return this.session.isSuperUserLoggedIn();
    }
    

    public boolean isLocked() {
        if (this.dataset != null) {
            logger.log(Level.FINE, "checking lock status of dataset {0}", this.dataset.getId());
            final Dataset lookedupDataset = datasetDao.find(dataset.getId());
            if (lookedupDataset != null && lookedupDataset.isLocked()) {
                logger.fine("locked!");
                return true;
            }
        }
        return false;
    }

    public boolean isThumbnailAvailable(final FileMetadata fileMetadata) {
        // new and optimized logic:
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        return this.fileDownloadHelper.canUserDownloadFile(fileMetadata)
                && this.datafileDao.isThumbnailAvailable(fileMetadata.getDataFile());
    }

    public boolean isLockedFromEdits() {
        if (null == this.lockedFromEditsVar) {
            try {
                this.permissionService.checkEditDatasetLock(this.dataset, 
                        this.dvRequestService.getDataverseRequest(),
                        new UpdateDatasetVersionCommand(this.dataset, 
                                this.dvRequestService.getDataverseRequest()));
                this.lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                this.lockedFromEditsVar = true;
            }
        }
        return this.lockedFromEditsVar;
    }

    // Methods for edit functions that are performed on one file at a time,
    // in popups that block the rest of the page:

    public boolean isDesignatedDatasetThumbnail(final FileMetadata fileMetadata) {
        return fileMetadata != null && fileMetadata.getDataFile() != null 
                && fileMetadata.getDataFile().getId() != null
                && fileMetadata.getDataFile().equals(this.dataset.getThumbnailFile());
    }

    /**
     * @todo For consistency, we should disallow users from setting the
     * thumbnail to a restricted file. We enforce this rule in the newer
     * workflow in dataset-widgets.xhtml. The logic to show the "Set Thumbnail"
     * button is in editFilesFragment.xhtml and it would be nice to move it to
     * Java since it's getting long and a bit complicated.
     */
    public void setFileMetadataSelectedForThumbnailPopup(final FileMetadata fm) {
        this.fileMetadataSelectedForThumbnailPopup = fm;
        this.alreadyDesignatedAsDatasetThumbnail = getUseAsDatasetThumbnail();
    }

    public void clearFileMetadataSelectedForThumbnailPopup() {
        this.fileMetadataSelectedForThumbnailPopup = null;
    }

    public boolean getUseAsDatasetThumbnail() {
        return isDesignatedDatasetThumbnail(this.fileMetadataSelectedForThumbnailPopup);
    }

    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (this.fileMetadataSelectedForThumbnailPopup != null 
                && this.fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
            if (useAsThumbnail) {
                this.dataset.setThumbnailFile(this.fileMetadataSelectedForThumbnailPopup.getDataFile());
            } else if (getUseAsDatasetThumbnail()) {
                this.dataset.setThumbnailFile(null);
            }
        }
    }

    public void saveAsDesignatedThumbnail() {
        logger.fine("saving as the designated thumbnail");
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately.
        // However, once the "save" button is pressed, we want to show a success message, if this is
        // a new image has been designated as such:
        if (getUseAsDatasetThumbnail() && !this.alreadyDesignatedAsDatasetThumbnail) {
            final String successMessage = getStringFromBundle("file.assignedDataverseImage.success",
                    this.fileMetadataSelectedForThumbnailPopup.getLabel());
            logger.fine(successMessage);
            JsfHelper.addFlashSuccessMessage(successMessage);
        }

        // And reset the selected fileMetadata:
        this.fileMetadataSelectedForThumbnailPopup = null;
    }

    public void deleteDatasetLogoAndUseThisDataFileAsThumbnailInstead() {
        logger.log(FINE, "For dataset id {0} the current thumbnail is from a dataset logo rather than a dataset file, blowing away the logo and using this FileMetadata id instead: {1}", new Object[]{dataset.getId(), fileMetadataSelectedForThumbnailPopup});

        Try.of(() -> this.datasetService.changeDatasetThumbnail(dataset, fileMetadataSelectedForThumbnailPopup.getDataFile()))
                .onFailure(ex -> logger.log(SEVERE, "Problem setting thumbnail for dataset id " + dataset.getId(), ex))
                .onSuccess(datasetThumbnail -> this.dataset = this.datasetDao.find(dataset.getId()));
    }

    public boolean isThumbnailIsFromDatasetLogoRatherThanDatafile() {
        DatasetThumbnail datasetThumbnail = this.datasetThumbnailService.getThumbnail(this.dataset);
        return datasetThumbnail != null && !datasetThumbnail.isFromDataFile();
    }

    public void refreshTagsPopUp(final FileMetadata fm) {
        setSelectedFile(fm);
    }

    public void saveFileTagsAndCategories(final FileMetadata selectedFile,
                                          final Collection<String> selectedFileMetadataTags,
                                          final Collection<String> selectedDataFileTags) {
        selectedFile.getCategories().clear();
        selectedFileMetadataTags.forEach(selectedFile::addCategoryByName);
        setTagsForTabularData(selectedDataFileTags, selectedFile);
    }

    public boolean exceedsIngestSizeLimit(final DataFile dataFile) {
        return this.ingestService.exceedsIngestSizeLimit(dataFile);
    }

    public boolean supportsAdvancedIngestOptions(final DataFile file) {
        return supportsPickingEncoding(file) || supportsInclusionOfLabelsFile(file);
    }

    public boolean supportsPickingEncoding(DataFile file) {
        return this.ingestService.supportsPickingEncoding(file);
    }

    public boolean supportsInclusionOfLabelsFile(DataFile file) {
        return this.ingestService.supportsInclusionOfLabelsFile(file);
    }

    public boolean isSelectivelyIngestableFile(DataFile file) {
        return this.ingestService.isSelectivelyIngestableFile(file);
    }

    public void clearFileMetadataSelectedForIngestOptionsPopup() {
        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    public String getIngestLanguageEncoding() {
        return this.ingestLanguageEncoding == null
                ? getStringFromBundle("editdatafilepage.defaultLanguageEncoding")
                : this.ingestLanguageEncoding;
    }

    public void handleLabelsFileUpload(final FileUploadEvent event) {
        final UploadedFile file = event.getFile();
        if (file != null) {
            try (final InputStream uploadStream = file.getInputStream()) {
                this.savedLabelsTempFile = saveTempFile(uploadStream);
                FacesMessage message = new FacesMessage(getStringFromBundle(
                        "dataset.file.upload", file.getFileName()));
                FacesContext.getCurrentInstance().addMessage(null, message);
            } catch (final IOException ioe) {
                logger.info("the file " + file.getFileName() + " failed to upload!");
                final String msg = getStringFromBundle(
                        "dataset.file.uploadFailure.detailmsg", file.getFileName());
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN,
                        getStringFromBundle("dataset.file.uploadFailure"), msg);
                FacesContext.getCurrentInstance().addMessage(null, message);
            }
        }
    }

    public void saveAdvancedOptions() {
        // Language encoding for SPSS SAV (and, possibly, other tabular ingests:)
        if (this.ingestLanguageEncoding != null 
                && this.fileMetadataSelectedForIngestOptionsPopup != null
                && this.fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
            if (this.fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                final IngestRequest ingestRequest = new IngestRequest();
                ingestRequest.setDataFile(this.fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                this.fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);

            }
            this.fileMetadataSelectedForIngestOptionsPopup.getDataFile()
                .getIngestRequest().setTextEncoding(ingestLanguageEncoding);
        }
        ingestLanguageEncoding = null;

        // Extra labels for SPSS POR (and, possibly, other tabular ingests)
        // (we are adding this parameter to the IngestRequest now, instead of back when it was uploaded. This is because
        // we want the user to be able to hit cancel and bail out, until they actually click 'save' in the "advanced
        // options" popup) -- L.A. 4.0 beta 11
        if (this.savedLabelsTempFile != null 
                && this.fileMetadataSelectedForIngestOptionsPopup != null
                && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
            if (this.fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                final IngestRequest ingestRequest = new IngestRequest();
                ingestRequest.setDataFile(this.fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);
            }
            this.fileMetadataSelectedForIngestOptionsPopup.getDataFile()
                .getIngestRequest().setLabelsFile(this.savedLabelsTempFile);
        }
        this.savedLabelsTempFile = null;
        this.fileMetadataSelectedForIngestOptionsPopup = null;
    }

    public void updateTermsOfUseForSelectedFiles(final TermsOfUseForm termsOfUseForm) {
        for (final FileMetadata selectedFile : this.selectedFiles) {
            final TermsOfUseForm termsOfUseCopy = new TermsOfUseForm();
            termsOfUseCopy.setTypeWithLicenseId(termsOfUseForm.getTypeWithLicenseId());
            termsOfUseCopy.setRestrictType(termsOfUseForm.getRestrictType());
            termsOfUseCopy.setCustomRestrictText(termsOfUseForm.getCustomRestrictText());
            selectedFile.setTermsOfUseForm(termsOfUseCopy);
        }
    }
    
    private void updateIngestTypeForSelectedImages(final DataFile.IngestType type) {
        streamSelectedMetadatas()
        .map(FileMetadata::getDataFile)
        .filter(DataFile::isImage)
        .forEach(f -> f.setIngestType(type));
    }
    
    private Stream<FileMetadata> streamSelectedMetadatas() {
        return this.selectedFiles.stream();
    } 

    public void initDuplicatesDialog() {
        this.duplicatesList = listDuplicates();
    }

    public boolean hasDuplicatesSelected() {
        return this.duplicatesList.stream()
                .map(DuplicatesService.DuplicateGroup::getDuplicates)
                .flatMap(Collection::stream)
                .anyMatch(DuplicatesService.DuplicateItem::isSelected);
    }

    public boolean hasDuplicatesWithWorkingVersion() {
        return this.duplicatesList.stream()
                .anyMatch(g -> !g.getExistingDuplicatesLabels().isEmpty());
    }

    public void deleteSelectedDuplicates() {
        this.selectedFiles = duplicatesList.stream()
                .map(DuplicatesService.DuplicateGroup::getDuplicates)
                .flatMap(Collection::stream)
                .filter(DuplicatesService.DuplicateItem::isSelected)
                .map(f -> f.getDataFile().getFileMetadata())
                .collect(toList());
        deleteFiles();
        this.duplicatesList = listDuplicates();
        this.selectedFiles.clear();

        // We store the contents of the datatable and clear it.
        // The reason is following: when we're removing a row it happens that contents of deleted row
        // (ie. file label and description) are inserted into the next remaining row. So we store the data,
        // destroy current table and then reconstruct it.
        this.filesTableBackup.addAll(fileMetadatas);
        this.fileMetadatas.clear();
    }

    public void duplicatesDeletionFinished() {
        // We reconstruct the contents of files table.
        this.fileMetadatas.addAll(this.filesTableBackup);
        this.filesTableBackup.clear();
    }

    // -------------------- PRIVATE --------------------

    private List<DuplicatesService.DuplicateGroup> listDuplicates() {
        return this.duplicatesService.listDuplicates(listExistingFiles(), this.newFiles);
    }

    private boolean hasDuplicatesInUploadedFiles(final List<DataFile> files) {
        return this.duplicatesService.hasDuplicatesInUploadedFiles(listExistingFiles(), files);
    }

    private List<DataFile> listExistingFiles() {
        return this.workingVersion.getFileMetadatas().stream()
                .map(FileMetadata::getDataFile)
                .collect(Collectors.toList());
    }

    private void updateCurrentBatchSizeForDeletedDataFile(DataFile dataFileToDelete) {
        this.dataFileUploadInfo.removeFromDataFilesToSave(dataFileToDelete);
        if (this.dataFileUploadInfo.canSubtractSize(dataFileToDelete)) {
            this.currentBatchSize -= this.dataFileUploadInfo.getSourceFileSize(dataFileToDelete);
        }
    }

    private void cleanupTempFiles() {
        final long purgeTime = System.currentTimeMillis() - TEMP_VALID_TIME_MILLIS;
        final File tempDirectory = new File(FileUtil.getFilesTempDirectory());
        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            logger.warning("Failed to cleanup temporary file " + FileUtil.getFilesTempDirectory());
        }
        final File[] tempFilesList = tempDirectory.listFiles();
        for (File tempFile : (tempFilesList != null ? tempFilesList : new File[0])) {
            if (tempFile.isFile() && tempFile.lastModified() < purgeTime && !tempFile.delete()) {
                logger.warning("Failed to delete temporary file " + tempFile.getName());
            }
        }
    }

    private void deleteTempFile(final DataFile dataFile) {
        // Before we remove the file from the list and forget about it:
        //
        // The physical uploaded file is still sitting in the temporary directory. If it were saved, it would be moved
        // into its permanent location. But since the user chose not to save it, we have to delete the temp file too.
        //
        // Eventually, we will likely add a dedicated mechanism for managing temp files, similar to (or part of) the
        // storage access framework, that would allow us to handle specialized configurations - highly sensitive/private
        // data, that has to be kept encrypted even in temp files, and such. But for now, we just delete the file
        // directly on the local filesystem:

        try {
            final List<Path> generatedTempFiles = this.ingestService.listGeneratedTempFiles(
                    Paths.get(FileUtil.getFilesTempDirectory()), dataFile.getStorageIdentifier());
            if (generatedTempFiles != null) {
                for (final Path generated : generatedTempFiles) {
                    logger.fine("(Deleting generated thumbnail file " 
                                + generated.toString() + ')');
                    try {
                        Files.delete(generated);
                    } catch (final IOException ioex) {
                        logger.warning("Failed to delete generated file " 
                                + generated.toString());
                    }
                }
            }
            Files.delete(Paths.get(FileUtil.getFilesTempDirectory() 
                    + File.separatorChar + dataFile.getStorageIdentifier()));
        } catch (final IOException ioe) {
            // safe to ignore - it's just a temp file.
            logger.warning("Failed to delete temporary file " 
                    + FileUtil.getFilesTempDirectory() + File.separatorChar
                    + dataFile.getStorageIdentifier());
        }
    }

    private void removeFileMetadataFromList(final List<FileMetadata> metadatas,
            final FileMetadata toDelete) {
        final Iterator<FileMetadata> iterator = metadatas.iterator();
        while (iterator.hasNext()) {
            final FileMetadata fileMetadata = iterator.next();
            if (toDelete.getDataFile().getStorageIdentifier()
                    .equals(fileMetadata.getDataFile().getStorageIdentifier())) {
                iterator.remove();
                break;
            }
        }
    }

    private void removeDataFileFromList(final List<DataFile> datafiles, 
            final DataFile toDelete) {
        final Iterator<DataFile> iterator = datafiles.iterator();
        while (iterator.hasNext()) {
            final DataFile file = iterator.next();
            if (toDelete.getStorageIdentifier().equals(file.getStorageIdentifier())) {
                iterator.remove();
                break;
            }
        }
    }

    private void populateDatasetUpdateFailureMessage() {
        JsfHelper.addErrorMessage(getStringFromBundle("dataset.message.filesFailure"), "");
    }

    private String returnToDraftVersion() {
        return "/dataset.xhtml?version=DRAFT&faces-redirect=true&persistentId=".
                concat(dataset.getGlobalId().asString());
    }

    /** Download a file from drop box */
    private InputStream getDropBoxContent(final GetMethod dropBoxMethod) 
            throws IOException {
        try {
            final HttpClient httpclient = new HttpClient();
            final int status = httpclient.executeMethod(dropBoxMethod);
            if (status != 200) {
                logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}, status code: {1}",
                        new Object[] {dropBoxMethod.getPath(), status});
                throw new IOException("Non 200 status code returned from dropbox");
            }
            return dropBoxMethod.getResponseBodyAsStream();
        } catch (final IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", 
                    dropBoxMethod.getPath());
            throw ex;
        }
    }

    private void setUpRsync() {
        logger.fine("setUpRsync called...");
        if (this.systemConfig.isRsyncUpload()
                && dataset.getFiles().isEmpty()) {

            Try<Option<RsyncInfo>> rsyncFetchOperation = Try.of(() -> this.fileService.retrieveRsyncScript(this.dataset, this.workingVersion))
                    .onFailure(ex -> logger.log(WARNING, "There was a problem with getting rsync script", ex));

            rsyncFetchOperation.onSuccess(this::setupScriptInfo);
        }
    }

    private void setupScriptInfo(final Option<RsyncInfo> rsyncScript) {
        rsyncScript.peek(rsyncInfo -> {
            setRsyncScript(rsyncInfo.getRsyncScript());
            this.rsyncScriptFilename = rsyncInfo.getRsyncScriptFileName();
            setHasRsyncScript(true);
        })
                .onEmpty(() -> setHasRsyncScript(false));
    }

    /**
     * After uploading via the site or Dropbox,
     * check the list of DataFile objects
     */
    private String processUploadedFileList(final List<DataFile> dFileList) {
        if (dFileList == null) {
            return null;
        }

        String warningMessage = null;

        // NOTE: for native file uploads, the dFileList will only
        // contain 1 file--method is called for every file even if the UI shows "simultaneous uploads"

        // Iterate through list of DataFile objects
        for (final DataFile currentFile : dFileList) {
            // Check for ingest warnings
            if (currentFile.isIngestProblem()) {
                if (currentFile.getIngestReport() != null) {
                    warningMessage = warningMessage == null
                            ? currentFile.getIngestReport().getIngestReportMessage()
                            : warningMessage.concat("; " + currentFile.getIngestReport().getIngestReportMessage());
                }
                currentFile.setIngestDone();
            }

            // Let's check if filename is a duplicate of another
            // file already uploaded, or already in the dataset:
            currentFile.getFileMetadata().setLabel(createNewNameIfDuplicated(currentFile.getFileMetadata()));
            if (isTemporaryPreviewAvailable(currentFile.getStorageIdentifier(), currentFile.getContentType())) {
                currentFile.setPreviewImageAvailable(true);
            }
            this.uploadedFiles.add(currentFile);
        }

        if (warningMessage != null) {
            logger.severe(warningMessage);
            return warningMessage;
        }
        return null;
    }

    private String createNewNameIfDuplicated(final FileMetadata fileMetadata) {
        if (this.fileLabelsExisting == null) {
            this.fileLabelsExisting = IngestUtil.existingPathNamesAsSet(this.workingVersion);
        }
        return IngestUtil.createNewNameIfDuplicated(fileMetadata, this.fileLabelsExisting);
    }

    private void setTagsForTabularData(final Collection<String> selectedDataFileTags, 
            final FileMetadata fileMetadata) {
        fileMetadata.getDataFile().getTags().clear();

        selectedDataFileTags.forEach(selectedTag -> {
            DataFileTag tag = new DataFileTag();
            tag.setTypeByLabel(selectedTag);
            tag.setDataFile(fileMetadata.getDataFile());
            fileMetadata.getDataFile().addTag(tag);
        });
    }

    private String saveTempFile(final InputStream input) throws IOException {
        final File file = File.createTempFile("tempIngestLabels.", ".txt");
        try (final FileOutputStream output = new FileOutputStream(file)) {
            copy(input, output);
            return file.getAbsolutePath();
        }
    }

    private void populateFileMetadatas(final Set<Long> selectedFileIds) {
        for (final FileMetadata fileMetadata : this.workingVersion.getFileMetadatas()) {
            final Long fileId = fileMetadata.getDataFile().getId();

            if (selectedFileIds.contains(fileId)) {
                logger.fine("Success! - found the file id " 
                        + fileId + " in the edit version.");
                this.fileMetadatas.add(fileMetadata);
                selectedFileIds.remove(fileId);
            }

            if (selectedFileIds.isEmpty()) {
                break;
            }
        }
    }

    // -------------------- SETTERS --------------------

    public void setIgnoringMaxUploadLimit(final boolean value) {
        this.ignoringMaxUploadLimit = value;
    }
    
    public void setFileMetadatas(final List<FileMetadata> etadatas) {
        this.fileMetadatas = etadatas;
    }

    public void setPersistentId(final String id) {
        this.persistentId = id;
    }

    public void setDropBoxSelection(final String selection) {
        this.dropBoxSelection = selection;
    }

    public void setDataset(final Dataset dataset) {
        this.dataset = dataset;
    }

    public void setOwnerId(final Long id) {
        this.ownerId = id;
    }

    public void setVersionId(final Long id) {
        this.versionId = id;
    }

    public void setCurrentBatchSize(final long size) {
        this.currentBatchSize = size;
    }

    public void setSelectedFiles(final List<FileMetadata> files) {
        this.selectedFiles = files;
    }

    public void setVersionString(final String value) {
        this.versionString = value;
    }

    public void setHasRsyncScript(final Boolean value) {
        this.hasRsyncScript = value;
    }

    public void setRsyncScript(final String script) {
        this.rsyncScript = script;
    }

    public void setWarningMessageForPopUp(final String warningMessageForPopUp) {
        this.warningMessageForPopUp = warningMessageForPopUp;
    }

    public void setSelectedFile(final FileMetadata metadata) {
        this.selectedFile = metadata;
    }

    public void setFileMetadataSelectedForIngestOptionsPopup(final FileMetadata metadata) {
        this.fileMetadataSelectedForIngestOptionsPopup = metadata;
    }

    public void setIngestLanguageEncoding(final String encoding) {
        this.ingestLanguageEncoding = encoding;
    }

    public void setIngestEncoding(final String encoding) {
        this.ingestLanguageEncoding = encoding;
    }
    
    public TextRecognitionDialog getTextRecognitionDialog() {
        return this.textRecognitionDialog;
    }
    
    
    //--------------------------------------------------------------------------
    public class TextRecognitionDialog {
        
        private String action = NON.toString();
        
        public String getAction() {
            return this.action;
        }
        
        public void setAction(final String action) {
            this.action = action;
        }
        
        public void updateSelectedFiles() {
            updateIngestTypeForSelectedImages(
                    DataFile.IngestType.valueOf(this.action));
        }
    }
}
