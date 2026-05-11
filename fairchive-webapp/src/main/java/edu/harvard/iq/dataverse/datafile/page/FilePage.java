package edu.harvard.iq.dataverse.datafile.page;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.CONFIGURE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.EXPLORE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.PREVIEW;
import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED;
import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType.RESTRICTED;
import static java.lang.Boolean.FALSE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.MimeTypes;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datafile.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UpdateDatasetException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseDialog;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileVersionDifference;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseIcon;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Try;


/**
 * @author skraffmi
 */

@SuppressWarnings("serial")
@ViewScoped
@Named("FilePage")
public class FilePage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    private DataFileServiceBean datafileService;
    private DatasetVersionServiceBean datasetVersionService;
    private PermissionServiceBean permissionService;
    private SystemConfig systemConfig;
    private ExternalToolServiceBean externalTools;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ExportService exportService;
    private FileService fileService;
    private GuestbookResponseDialog guestbookResponseDialog;
    private CitationFactory citationFactory;
    private DataverseSession session;
    private ExternalToolHandler externalToolHandler;
    private UIMessages ui;
    private FileDownloadServiceBean fileDownloadService;
    private WorldMapPermissionHelper worldMapPermissionHelper;

    private FileMetadata fileMetadata;
    private Long fileId;
    private String version;
    private DataFile file;
    private int selectedTabIndex;
    private Dataset dataset;
    private DatasetVersion datasetVersion;
    private List<FileMetadata> fileMetadatasForTab;
    private String persistentId;
    private List<ExternalTool> configureTools;
    private List<ExternalTool> exploreTools;
    //private List<ExternalTool> previewTools;
    private Boolean thumbnailAvailable = null;
    private Boolean lockedFromEditsVar;
    private Boolean lockedFromDownloadVar;
    private Boolean guestbookResponseProvided = false;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public FilePage() { }

    @Inject
    public FilePage(final DataFileServiceBean datafileService, 
    				final DatasetVersionServiceBean datasetVersionService,
                    final PermissionServiceBean permissionService, 
                    final SystemConfig systemConfig,
                    final ExternalToolServiceBean externalToolService, 
                    final DataverseRequestServiceBean dvRequestService,
                    final PermissionsWrapper permissionsWrapper, 
                    final FileDownloadHelper fileDownloadHelper,
                    final ExportService exportService, 
                    final FileService fileService,
                    final GuestbookResponseDialog guestbookResponseDialog, 
                    final CitationFactory citationFactory,
                    final DataverseSession session, 
                    final ExternalToolHandler externalToolHandler,
                    final UIMessages ui,
                    final FileDownloadServiceBean fileDownloadService,
                    final WorldMapPermissionHelper worldMapPermissionHelper) {
        this.datafileService = datafileService;
        this.datasetVersionService = datasetVersionService;
        this.permissionService = permissionService;
        this.systemConfig = systemConfig;
        this.externalTools = externalToolService;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.exportService = exportService;
        this.fileService = fileService;
        this.guestbookResponseDialog = guestbookResponseDialog;
        this.citationFactory = citationFactory;
        this.session = session;
        this.externalToolHandler = externalToolHandler;
        this.ui = ui;
        this.fileDownloadService = fileDownloadService;
        this.worldMapPermissionHelper = worldMapPermissionHelper;
    }

    // -------------------- GETTERS --------------------
    
    public String getPageTitle() {
        return this.file.getDisplayName() + " - " +
                this.dataset.getOwner().getDisplayName();
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public DataFile getFile() {
        return file;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getVersion() {
        return version;
    }

    public List<FileMetadata> getFileMetadatasForTab() {
        return fileMetadatasForTab;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public List<ExternalTool> getConfigureTools() {
        return configureTools;
    }

    public List<ExternalTool> getExploreTools() {
        return exploreTools;
    }

    public Boolean getGuestbookResponseProvided() {
        return guestbookResponseProvided;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (fileId == null && persistentId == null) {
            return permissionsWrapper.notFound();
        }

        // Set the file and datasetVersion
        if (fileId != null) {
            file = datafileService.find(fileId).orElse(null);
        } else {
            file = datafileService.findByGlobalId(persistentId);
            if (file != null) {
                fileId = file.getId();
            }
        }

        if (file == null || fileId == null) {
            return permissionsWrapper.notFound();
        }

        if (file.getOwner().isHarvested()) {
            // if so, we'll simply forward to the remote URL for the original
            // source of this harvested dataset:
            final String originalSourceURL = file.getOwner().getRemoteArchiveURL();
            if (isNotBlank(originalSourceURL)) {
                logger.fine("redirecting to " + originalSourceURL);
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                } catch (IOException ioe) {
                    // must be a bad URL...
                    // we don't need to do anything special here - we'll redirect
                    // to the local 404 page, below.
                    logger.warning("failed to issue a redirect to " + originalSourceURL);
                }
            }
            return permissionsWrapper.notFound();
        }

        if (!permissionsWrapper.canViewUnpublishedDataset(file.getOwner()) &&
                file.getOwner().hasActiveEmbargo()) {
            return permissionsWrapper.notAuthorized();
        }

        final RetrieveDatasetVersionResponse retrieveDatasetVersionResponse
                = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
        this.datasetVersion = retrieveDatasetVersionResponse.getDatasetVersion();
        fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(datasetVersion.getId(), fileId).orElse(null);

        if (fileMetadata == null) {
        	return permissionsWrapper.notFound();
        }

        // If this DatasetVersion is unpublished and permission is doesn't have permissions:
        // -> Go to the Login page
        // Check permisisons

        final boolean authorized = fileMetadata.getDatasetVersion().isReleased()
                || (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());

        if (!authorized) {
            return permissionsWrapper.notAuthorized();
        }

        guestbookResponseDialog.initForDatasetVersion(fileMetadata.getDatasetVersion());
        this.dataset = fileMetadata.getDataFile().getOwner();

        // Find external tools based on their type, the file content type, and whether
        // ingest has created a derived file for that type
        // Currently, tabular data files are the only type of derived file created, so
        // isTabularData() works - true for tabular types where a .tab file has been
        // created and false for other mimetypes
        // For tabular data, indicate successful ingest by returning a contentType for the derived .tab file
        final String contentType = file.isTabularData() ? MimeTypes.TAB_SEPARATED_VALUES : file.getContentType();
        configureTools = externalTools.findExternalTools(CONFIGURE, contentType, file, datasetVersion);
        exploreTools = externalTools.findExternalTools(EXPLORE, contentType, file, datasetVersion);
        return null;
    }
    
    public boolean displayMetadataMapTab() {
        return !isDatasetDeaccesioned() || canUpdateDataset();
    }
    
    public boolean displayEditButtonGroup() throws Exception {
        return canUpdateDataset()
                && (this.datafileService.hasReplacement(this.file)
                        || this.datafileService.hasBeenDeleted(this.file));
    }
    
    public boolean displayRestrictedIcon() {
        return this.fileMetadata.isFileUseRestricted() && ! canUserDownloadFile();
    }
    
    public boolean displayFileDescriptionBlock() {
        return isNotEmpty(this.fileMetadata.getDescription());
    }
    
    public boolean displayCategoriesBlock() {
        return !this.fileMetadata.getCategoryNames().isEmpty()
                || !this.fileMetadata.getDataFile().getTags().isEmpty();
    }
    
    public boolean displayPreviewTab() { 	
    	return getPreviewTool().
    		map(viewer -> this.fileMetadata.isFilePubliclyAccessible() ||
    			(canUserDownloadFile() && viewer.isTrusted())).
    		orElse(FALSE);
    }
    
    public boolean isFileSizeUnderLimit() {   	
    	return getPreviewTool()
    			.map(tool -> tool.acceptsFileOfSize(this.file.getFilesize()))
    			.orElse(FALSE);
    }
    
    public boolean displayDownloadPopup() {
        return isDownloadPopupRequired() && !getGuestbookResponseProvided();
    }
    
    public boolean displayPreviewArea() {
        return !isDownloadPopupRequired() || getGuestbookResponseProvided();
    }
    
    public boolean displayEditMenu() throws Exception {
        return canUpdateDataset()
                && !isLockedFromEdits()
                && !(this.datafileService.hasReplacement(this.file)
                        || this.datafileService.hasBeenDeleted(this.file));
    }
    
    public boolean displayDeleteMenuItem() {
        return !this.file.isReleased() || !this.file.isFilePackage();
    }
    
    public boolean displayReplaceMenuItem() {
        return this.file.isReleased()
                && isDraftReplacementFile() == false
                && !this.file.isFilePackage();
    }
    
    public boolean displayAlreadyReplacedMenuItem() {
        return !this.file.isFilePackage()
                && ((this.file.isReleased()
                        && isDraftReplacementFile())
                        || (!this.file.isReleased()
                                && this.file.getFileMetadata().getDatasetVersion()
                                        .getDataset().isReleased()));
    }
    
    public boolean displayDownloadButtons() {
        return (!isDatasetDeaccesioned() ||
                (isDatasetDeaccesioned() && canUpdateDataset()))
                && (!this.file.isFilePackage() ||
                        this.file.isFilePackage()
                                && this.systemConfig.isHTTPDownload());
    }
    
    public boolean displayAccessTab() {
        return this.systemConfig.isRsyncDownload() 
                && this.fileMetadata.getDataFile().isFilePackage() 
                && !this.fileMetadata.getDataFile().getOwner().
                    getStorageIdentifier().startsWith("s3://");
    }
    
    public boolean displayDraftIcon() {
        return this.fileMetadata.getDatasetVersion().getDataset().isReleased() 
                && this.fileMetadata.getDatasetVersion().isDraft();
    }
    
    public boolean displayUnlockIcon() {
        return this.fileMetadata.getTermsOfUse().getTermsOfUseType() == RESTRICTED 
                && canUserDownloadFile();
    }
    
    public boolean displayPublicDownloadUrl() {
        return isPubliclyDownloadable()
                && !this.fileMetadata.getDataFile().isFilePackage();
    }
    
    public boolean displayIngestProblem() {
        return this.file.isIngestProblem() && canUpdateDataset();
    }
    
    public boolean displayAddEditMetadataButton() throws Exception {
        return this.session.isUserLoggedIn()
                && this.permissionsWrapper.canIssueUpdateDatasetCommand(
                        this.fileMetadata.getDatasetVersion().getDataset())
                && !(this.datafileService
                        .hasReplacement(this.fileMetadata.getDataFile())
                        || this.datafileService
                                .hasBeenDeleted(this.fileMetadata.getDataFile()));
    }
    
    public boolean displayEditMetadataButton() throws Exception {
        return this.session.isUserLoggedIn()
                && this.permissionsWrapper.canIssueUpdateDatasetCommand(
                        this.fileMetadata.getDatasetVersion().getDataset())
                && (this.datafileService
                        .hasReplacement(this.fileMetadata.getDataFile())
                        || this.datafileService
                                .hasBeenDeleted(this.fileMetadata.getDataFile()));
    }
    
    
    public boolean displayMetrics() {
        return !(this.fileMetadata.getDataFile().isFilePackage() ||
                isDatasetDeaccesioned());
    }
    
	public boolean displayFileConfigureDropdownFragment() {
		return canUpdateDataset() && (getConfigureTools().size() > 0
				|| this.worldMapPermissionHelper.canUserSeeMapDataButtonFromPage(this.fileMetadata)
				|| this.worldMapPermissionHelper.canSeeMapButtonReminderToPublishFromPage(this.fileMetadata));
	}

    private boolean canViewUnpublishedDataset() {
        return this.permissionsWrapper.canViewUnpublishedDataset(
                this.fileMetadata.getDatasetVersion().getDataset());
    }

    private boolean isDownloadPopupRequired() {
        return fileMetadata.getId() != null
                && fileMetadata.getDatasetVersion().getId() != null
                && FileUtil.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
    }

    public boolean isRequestAccessPopupRequired() {
        return fileMetadata.getId() != null
                && fileMetadata.getDatasetVersion().getId() != null
                && FileUtil.isRequestAccessPopupRequired(fileMetadata);
    }
    
    public boolean isOCRedFilePresent() {
        try {
            final StorageIO<DataFile> storage = DataAccess.dataAccess()
                    .getStorageIO(this.file);
            return storage.isAuxObjectCached("ocr");
        } catch (final IOException e) {
            logger.warning("Problem with checking ocr file on file page" + e.toString());
            return false;
        }
    }

    public boolean isHTRedFilePresent() {
        try {
            final StorageIO<DataFile> storage = DataAccess.dataAccess()
                    .getStorageIO(this.file);
            return storage.isAuxObjectCached("htr");
        } catch (final IOException e) {
            logger.warning("Problem with checking ocr file on file page" + e.toString());
            return false;
        }
    }
    
    public void downloadCitationXML() {
        this.fileDownloadService.downloadCitationXML(this.fileMetadata, null,
                this.fileMetadata.getDataFile().isIdentifierRegistered());
    }
    
    public void downloadCitationRIS() {
        this.fileDownloadService.downloadCitationRIS(this.fileMetadata, null,
                this.fileMetadata.getDataFile().isIdentifierRegistered());
    }
    
    public void downloadCitationBibtex() {
        this.fileDownloadService.downloadCitationBibtex(this.fileMetadata, null,
                this.fileMetadata.getDataFile().isIdentifierRegistered());
    }
    
    public void downloadDatasetCitationXML() {
        this.fileDownloadService
                .downloadDatasetCitationXML(this.fileMetadata.getDatasetVersion());
    }

    public void downloadDatasetCitationRIS() {
        this.fileDownloadService
                .downloadDatasetCitationRIS(this.fileMetadata.getDatasetVersion());
    }

    public void downloadDatasetCitationBibtex() {
        this.fileDownloadService
                .downloadDatasetCitationBibtex(this.fileMetadata.getDatasetVersion());
    }

    public List<String[]> getExporters() {
        final List<String[]> retList = new ArrayList<>();

        for (final Exporter exporter : exportService.exporters()) {
            if (exporter.isAvailableToUsers()) {
                final String myHostURL = systemConfig.getDataverseSiteUrl();
                final String[] temp = new String[2];
                temp[0] = exporter.getDisplayName();
                temp[1] = myHostURL + "/api/datasets/export?exporter=" 
                        + exporter.getProviderName() + "&persistentId="
                        + dataset.getGlobalId();
                retList.add(temp);
            }
        }
        return retList;
    }

    public String saveProvFreeform(final String freeformTextInput, 
    		                       final DataFile dataFileFromPopup){

        final Try<Dataset> saveProvOperation = Try.of(
                () -> fileService.saveProvenanceFileWithDesc(fileMetadata, dataFileFromPopup, freeformTextInput))
                .onFailure(this::handleProvenanceExceptions);
        if (saveProvOperation.isFailure()){
            return EMPTY;
        }
        this.ui.addFlashSuccessMessage(getStringFromBundle("file.message.editSuccess"));
        return returnToDraftVersion();
    }

    public String createCitation() {
        boolean isDirectCitation = fileMetadata.getDataFile().isIdentifierRegistered();
        return citationFactory
                .create(fileMetadata, isDirectCitation)
                .toString(true);
    }

    public String createCitationFromFileDatasetVersion() {
        return citationFactory
                .create(fileMetadata.getDatasetVersion())
                .toString(true);
    }

    public String deleteFile() {
        final Try<Dataset> deleteFileOperation = Try.of(() -> fileService.deleteFile(this.fileMetadata))
                .onFailure(this::handleDeleteFileExceptions);

        if (deleteFileOperation.isFailure()) {
            return EMPTY;
        }

        this.ui.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.deleteSuccess"));
        return returnToDatasetOnly(fileMetadata.getDataFile().getOwner());
    }

    public void tabChanged(TabChangeEvent<?> event) {
        final TabView tv = (TabView) event.getComponent();
        final int activeTabIndex = tv.getActiveIndex();
        setFileMetadatasForTab(activeTabIndex == 1 || activeTabIndex == 2
                ? loadFileMetadataTabList()
                : new ArrayList<>());
    }
    
    public boolean isThumbnailAvailable() {
        // new and optimized logic:
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        // (Also, cache the result the first time the check is performed...
        // remember - methods referenced in "rendered=..." attributes are
        // called *multiple* times as the page is loading!)

        if (thumbnailAvailable != null) {
            return thumbnailAvailable;
        }

        thumbnailAvailable = canUserDownloadFile()
                && datafileService.isThumbnailAvailable(this.fileMetadata.getDataFile());

        return thumbnailAvailable;
    }

    public Optional<StreamedContent> getLicenseIconContent(final FileTermsOfUse termsOfUse) {
        return termsOfUse.getIcon().map(this::toStreamedContent);
    }
    
    private DefaultStreamedContent toStreamedContent(final LicenseIcon icon) {
        return DefaultStreamedContent.builder()
                .contentType(icon.getContentType())
                .stream(icon::getContentAsStream)
                .build();
    }

    public StreamedContent getOtherTermsIconContent(final FileTermsOfUse.TermsOfUseType termsOfUseType) {
        if (termsOfUseType.equals(RESTRICTED)) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                            "/images/restrictedaccess.png")))
                    .build();
        }

        if (termsOfUseType.equals(ALL_RIGHTS_RESERVED)) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                            "/images/allrightsreserved.png")))
                    .build();
        }

        return null;
    }

    public boolean canUpdateDataset() {
        return permissionsWrapper.canCurrentUserUpdateDataset(file.getOwner());
    }

    public boolean isDraftReplacementFile() {
        /*
        This method tests to see if the file has been replaced in a draft version of the dataset
        Since it must must work when you are on prior versions of the dataset
        it must accrue all replacement files that may have been created
        */

        DataFile dataFileToTest = fileMetadata.getDataFile();
        final DatasetVersion currentVersion = dataset.getLatestVersion();

        if (!currentVersion.isDraft() || dataset.getReleasedVersion() == null) {
            return false;
        }

        List<DataFile> dataFiles = new ArrayList<>();
        dataFiles.add(dataFileToTest);

        while (datafileService.findReplacementFile(dataFileToTest.getId()).isPresent()) {
            dataFiles.add(datafileService.findReplacementFile(dataFileToTest.getId()).get());
            dataFileToTest = datafileService.findReplacementFile(dataFileToTest.getId()).get();
        }

        if (dataFiles.size() < 2) {
            return false;
        }

        int numFiles = dataFiles.size();
        final DataFile current = dataFiles.get(numFiles - 1);
        DatasetVersion publishedVersion = dataset.getReleasedVersion();

        return datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(publishedVersion.getId(), current.getId()) == null;
    }

    /**
     * To help with replace development
     */
    public boolean isReplacementFile() {
        return this.file.isReplacementFile();
    }

    public boolean isPubliclyDownloadable() {
        return FileUtil.isPubliclyDownloadable(fileMetadata);
    }
    
    public boolean isDatasetDeaccesioned() {
        return this.fileMetadata.getDatasetVersion().isDeaccessioned(); 
    }

    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(),
                        new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }

    public boolean isLockedFromDownload() {
        if (null == lockedFromDownloadVar) {
            try {
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(),
                        new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromDownloadVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromDownloadVar = true;
            }
        }
        return lockedFromDownloadVar;
    }

    public String getPublicDownloadUrl() {
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), persistentId, fileId);
    }

    //Provenance fragment bean calls this to show error dialogs after popup failure
    //This can probably be replaced by calling JsfHelper from the provpopup bean
    public void showProvError() {
        this.ui.addErrorMessage(getStringFromBundle("file.metadataTab.provenance.error"), EMPTY);
    }

    public String getPreviewUrl() {
    	return getPreviewTool().map(this::buildPreviewUrl).orElse(EMPTY);
    }

    public void showPreview(final GuestbookResponse guestbookResponse) {
        fileDownloadHelper.writeGuestbookResponseForPreview(guestbookResponse, fileMetadata, 
        		this.externalTools.findFor(PREVIEW, this.file, this.datasetVersion).orElse(null));
        guestbookResponseProvided = true;
    }
    
    public String getShareUrl() {
        return this.systemConfig.getDataverseSiteUrl() + 
                "/dataset.xhtml?persistentId=" +
                this.fileMetadata.getDatasetVersion().getDataset().getGlobalId();
    }

    // -------------------- PRIVATE --------------------
    
    private Optional<ExternalTool> getPreviewTool() {
    	return this.externalTools.findFor(PREVIEW, this.file, this.datasetVersion);
    }
    
    private String buildPreviewUrl(final ExternalTool viewer) {
    	return this.externalToolHandler.buildToolUrlWithQueryParams(viewer,
                this.file, null, this.session.getLocaleCode()).concat("&preview=true");
    }
    
    private boolean canUserDownloadFile() {
    	return this.fileDownloadHelper.canUserDownloadFile(this.fileMetadata);
    }

    private void handleProvenanceExceptions(Throwable throwable){
        throwable = throwable instanceof EJBException ? throwable.getCause() : throwable;

        if (throwable instanceof ValidationException){
            this.ui.addErrorMessage(
                    getStringFromBundle("dataset.message.validationError"), EMPTY);
        } else if (throwable instanceof UpdateDatasetException){
            this.ui.addErrorMessage(
                    getStringFromBundle("dataset.save.fail"),
                    throwable.toString());
        } else {
            this.ui.addErrorMessage(
                    getStringFromBundle("dataset.save.fail"), EMPTY);
        }
    }

    private void handleDeleteFileExceptions(Throwable throwable){
        throwable = throwable instanceof EJBException ? throwable.getCause() : throwable;

        if (throwable instanceof ValidationException){
            this.ui.addErrorMessage(
                    getStringFromBundle("dataset.message.validationError"), EMPTY);
        } else if (throwable instanceof UpdateDatasetException){
            this.ui.addErrorMessage(
                    getStringFromBundle("dataset.delete.fail"),
                    throwable.toString());
        } else {
            this.ui.addErrorMessage(getStringFromBundle("dataset.delete.fail"), EMPTY);
        }
    }

    private List<FileMetadata> loadFileMetadataTabList() {
        final List<DataFile> allfiles = allRelatedFiles();
        final List<FileMetadata> retList = new ArrayList<>();
        for (final DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;

            if (versionLoop.isReleased() || versionLoop.isDeaccessioned()
                    || permissionsWrapper.canViewUnpublishedDataset(fileMetadata.getDatasetVersion().getDataset())) {
                for (final DataFile df : allfiles) {
                    FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionLoop.getId(), df.getId()).orElse(null);
                    if (fmd != null) {
                        fmd.setContributorNames(datasetVersionService.getContributorsNames(versionLoop));
                        FileVersionDifference fvd = new FileVersionDifference(fmd, getPreviousFileMetadata(fmd));
                        fmd.setFileVersionDifference(fvd);
                        retList.add(fmd);
                        foundFmd = true;
                        break;
                    }
                }
                // no File metadata found make dummy one
                if (!foundFmd) {
                    final FileMetadata dummy = new FileMetadata();
                    dummy.setDatasetVersion(versionLoop);
                    dummy.setDataFile(null);
                    final FileVersionDifference fvd = new FileVersionDifference(dummy, getPreviousFileMetadata(versionLoop));
                    dummy.setFileVersionDifference(fvd);
                    retList.add(dummy);
                }
            }
        }
        return retList;
    }

    private FileMetadata getPreviousFileMetadata(final DatasetVersion currentversion) {
        final List<DataFile> allfiles = allRelatedFiles();
        boolean foundCurrent = false;
        DatasetVersion priorVersion = null;
        for (final DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (foundCurrent) {
                priorVersion = versionLoop;
                break;
            }
            if (versionLoop.equals(currentversion)) {
                foundCurrent = true;
            }
        }
        if (priorVersion != null && priorVersion.getAllFilesMetadataSorted() != null) {
            for (final FileMetadata fmdTest : priorVersion.getAllFilesMetadataSorted()) {
                for (final DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }
        return null;
    }

    private FileMetadata getPreviousFileMetadata(final FileMetadata fmdIn) {
        final DataFile dfPrevious = datafileService.find(fmdIn.getDataFile().getPreviousDataFileId()).orElse(null);
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (final DatasetVersion dvloop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (gotCurrent) {
                dvPrevious = dvloop;
                break;
            }
            if (dvloop.equals(fmdIn.getDatasetVersion())) {
                gotCurrent = true;
            }
        }

        final List<DataFile> allfiles = allRelatedFiles();

        if (dvPrevious != null && dvPrevious.getAllFilesMetadataSorted() != null) {
            for (final FileMetadata fmdTest : dvPrevious.getAllFilesMetadataSorted()) {
                for (final DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }

        final Long dfId = dfPrevious != null ? dfPrevious.getId() : fmdIn.getDataFile().getId();
        final Long versionId = dvPrevious != null ? dvPrevious.getId() : null;

        return datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId).orElse(null);
    }

    private String returnToDatasetOnly(Dataset draftDataset) {
        return "/dataset.xhtml?version=DRAFT&faces-redirect=true&persistentId=" 
                + draftDataset.getGlobalId();
    }

    private String returnToDraftVersion() {
        return "/file.xhtml?version=DRAFT&faces-redirect=true&fileId=" + fileId;
    }

    private List<DataFile> allRelatedFiles() {
        final List<DataFile> dataFiles = new ArrayList<>();
        final DataFile dataFileToTest = fileMetadata.getDataFile();
        final Long rootDataFileId = dataFileToTest.getRootDataFileId();
        if (rootDataFileId < 0) {
            dataFiles.add(dataFileToTest);
        } else {
            dataFiles.addAll(datafileService.findAllRelatedByRootDatafileId(rootDataFileId));
        }

        return dataFiles;
    }

    // -------------------- SETTERS --------------------

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setFileMetadatasForTab(List<FileMetadata> fileMetadatasForTab) {
        this.fileMetadatasForTab = fileMetadatasForTab;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
}