package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
@ViewScoped
@Named("selectGuestbookPage")
public class SelectGuestbookPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SelectGuestbookPage.class.getCanonicalName());

    private DatasetService datasetService;
    private PermissionsWrapper permissionsWrapper;
    private SelectGuestBookService selectGuestBookService;

    private String persistentId;
    private Long datasetId;

    private Dataset dataset;
    private Guestbook selectedGuestbook;
    private List<Guestbook> availableGuestbooks;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public SelectGuestbookPage() {
    }

    @Inject
    public SelectGuestbookPage(DatasetService datasetService, PermissionsWrapper permissionsWrapper,
                               SelectGuestBookService selectGuestBookService) {
        this.datasetService = datasetService;
        this.permissionsWrapper = permissionsWrapper;
        this.selectGuestBookService = selectGuestBookService;
    }

    // -------------------- GETTERS --------------------
    public String getPersistentId() {
        return persistentId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public List<Guestbook> getAvailableGuestbooks() {
        return availableGuestbooks;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        if (persistentId != null) {
            dataset = datasetService.findByGlobalId(persistentId);
        } else if (datasetId != null) {
            dataset = datasetService.find(datasetId);
        }

        if (dataset == null) {
            return permissionsWrapper.notFound();
        }

        // Check permisisons
        if (!permissionsWrapper.canCurrentUserUpdateDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (dataset.isInReview() && !permissionsWrapper.canUpdateAndPublishDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        selectedGuestbook = dataset.getGuestbook();
        availableGuestbooks = dataset.getDataverseContext().getAvailableGuestbooks();

        return StringUtils.EMPTY;
    }

    public String save() {

        Try<Dataset> selectGuestbookOperation = Try.of(() -> selectGuestBookService.saveGuestbookChanges(dataset, Option.of(selectedGuestbook)));

        if (selectGuestbookOperation.isFailure()) {
            Throwable ex = selectGuestbookOperation.getCause();
            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + ex.getMessage(), ex);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.guestbookFailure"));
            return StringUtils.EMPTY;
        }

        logger.fine("Successfully executed SaveDatasetCommand.");
        dataset = selectGuestbookOperation.get();

        return returnToLatestVersion();
    }

    public String cancel() {
        return returnToLatestVersion();
    }

    public void reset() {
        selectedGuestbook = null;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public String getPageTitle() {
        return dataset.getLatestVersion().getParsedTitle()+ " - " 
                    + Jsoup.parse(dataset.getOwner().getName()).text();
    }

    // -------------------- PRIVATE --------------------
    private String returnToLatestVersion() {
        dataset = datasetService.find(dataset.getId());
        DatasetVersion workingVersion = dataset.getLatestVersion();
        if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
            workingVersion = dataset.getReleasedVersion();
        }
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() 
                + "&version=" + workingVersion.getFriendlyVersionNumber()
                + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }
}
