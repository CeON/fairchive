package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.ingest.UningestInfoService;
import edu.harvard.iq.dataverse.ingest.UningestService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.MD5;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
@ViewScoped
@Named("UningestPage")
public class UningestPage implements Serializable {

    private static final Logger log = getLogger(UningestPage.class);
    private UningestService uningestService;
    private DatasetRepository datasetRepository;
    private DataverseSession dataverseSession;
    private DataverseRequestServiceBean dataverseRequestService;
    private PermissionsWrapper permissionsWrapper;
    private PermissionServiceBean permissionService;
    private SystemConfig systemConfig;
    private UningestInfoService uningestInfoService;
    private UIMessages messages;

    private List<UningestableItem> uningestableFiles = new ArrayList<>();
    private List<UningestableItem> selectedFiles = new ArrayList<>();

    private Long datasetId;
    private Dataset dataset;

    // -------------------- GETTERS --------------------

    public Long getDatasetId() {
        return this.datasetId;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public List<UningestableItem> getUningestableFiles() {
        return this.uningestableFiles;
    }

    public List<UningestableItem> getSelectedFiles() {
        return this.selectedFiles;
    }

    // -------------------- CONSTRUCTORS --------------------

    public UningestPage() { }

    @Inject
    public UningestPage(final UningestService uningestService, 
                        final DatasetRepository datasetRepository,
                        final DataverseSession dataverseSession, 
                        final PermissionsWrapper permissionsWrapper,
                        final SystemConfig systemConfig, 
                        final UningestInfoService uningestInfoService,
                        final PermissionServiceBean permissionServiceBean,
                        final DataverseRequestServiceBean dataverseRequestService,
                        final UIMessages messages) {
        this.uningestService = uningestService;
        this.datasetRepository = datasetRepository;
        this.dataverseSession = dataverseSession;
        this.permissionsWrapper = permissionsWrapper;
        this.systemConfig = systemConfig;
        this.uningestInfoService = uningestInfoService;
        this.permissionService = permissionServiceBean;
        this.dataverseRequestService = dataverseRequestService;
        this.messages = messages;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (this.datasetId == null) {
            return this.permissionsWrapper.notFound();
        }
        this.dataset = this.datasetRepository.getById(this.datasetId);
        if (!this.permissionsWrapper.canCurrentUserUpdateDataset(this.dataset)
                || this.systemConfig.isReadonlyMode()) {
            return this.permissionsWrapper.notAuthorized();
        }
        if (this.permissionService.checkEditDatasetLockNonThrowing(this.dataset, 
                this.dataverseRequestService.getDataverseRequest())) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        DatasetVersion version = this.dataset.getLatestVersion();
        if (!version.isDraft()) {
            return this.permissionsWrapper.notFound();
        }
        this.uningestableFiles.addAll(prepareItemList());
        this.selectedFiles.clear();
        return EMPTY;
    }

    public void uningest() {
        if (this.selectedFiles.isEmpty() || !this.dataverseSession.isUserLoggedIn()) {
            return;
        }

        final AuthenticatedUser user = (AuthenticatedUser) this.dataverseSession.getUser();
        final List<String> uningestFailedFileNames = new ArrayList<>();
        this.selectedFiles.forEach(toUningest -> {
            try {
                this.uningestService.uningest(toUningest.getDataFile(), user);
            } catch (Exception e) {
                log.error("Could not uningest data file: {}", toUningest.getDataFile().getId(), e);
                uningestFailedFileNames.add(toUningest.getFileName());
            }
        });
        this.uningestableFiles = prepareItemList();
        this.selectedFiles.clear();

        if (!uningestFailedFileNames.isEmpty()) {
            this.messages.addComponentErrorMessage(
                    getStringFromBundle("uningest.error"),
                    uningestFailedFileNames.stream().collect(joining(", ", "[", "]. ")));
        }
    }

    public String cancel() {
        return "/dataset.xhtml?version=DRAFT&faces-redirect=true&persistentId="
                .concat(this.dataset.getGlobalId().asString());
    }

    // -------------------- PRIVATE --------------------

    private List<UningestableItem> prepareItemList() {
        return this.uningestInfoService.listUningestableFiles(this.dataset).stream()
                .map(UningestableItem::fromDatafile)
                .collect(toList());
    }

    // -------------------- SETTERS --------------------

    public void setDatasetId(final Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setSelectedFiles(final List<UningestableItem> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    // -------------------- INNER CLASSES --------------------

    public static class UningestableItem {
        private DataFile dataFile;
        private String fileName;
        private String originalFormat;
        private String md5;
        private String unf;

        // -------------------- GETTERS --------------------

        public DataFile getDataFile() {
            return this.dataFile;
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getOriginalFormat() {
            return this.originalFormat;
        }

        public String getMd5() {
            return this.md5;
        }

        public String getUnf() {
            return this.unf;
        }

        // -------------------- LOGIC --------------------

        public static UningestableItem fromDatafile(final DataFile file) {
            final UningestableItem item = new UningestableItem();
            item.dataFile = file;
            item.fileName = file.getFileMetadata().getLabel();
            item.originalFormat = extractAndFormatExtension(file);
            item.md5 = file.getChecksumType() == MD5
                    ? file.getChecksumValue() : EMPTY;
            item.unf = file.getUnf();
            return item;
        }

        // -------------------- PRIVATE --------------------

        public static String extractAndFormatExtension(final DataFile file) {
            String extension = FileUtil.generateOriginalExtension(file.isTabularData()
                    ? file.getDataTable().getOriginalFileFormat()
                    : file.getContentType());
            return extension.replaceFirst("\\.", EMPTY).toUpperCase();
        }
    }
}
