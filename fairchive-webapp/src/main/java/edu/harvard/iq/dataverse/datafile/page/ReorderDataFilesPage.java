package edu.harvard.iq.dataverse.datafile.page;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.ReorderEvent;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.Tuple;
import io.vavr.Tuple2;

@SuppressWarnings("serial")
@ViewScoped
@Named("ReorderDataFilesPage")
public class ReorderDataFilesPage implements java.io.Serializable {

    private DatasetVersionServiceBean datasetVersionService;
    private PermissionsWrapper permissionsWrapper;

    private DatasetVersion datasetVersion = new DatasetVersion();
    private List<FileMetadata> fileMetadatas;
    private Tuple2<Integer, Integer> lastReorderFromAndTo;
    private FileMetadata lastReorderFileMetadata;
    
    
    public ReorderDataFilesPage() {}
    
    @Inject
    public ReorderDataFilesPage(final DatasetVersionServiceBean datasetVersionService,
    		final PermissionsWrapper permissionsWrapper) {
    	
    	this.datasetVersionService = datasetVersionService;
    	this.permissionsWrapper = permissionsWrapper;
    }

    /**
     * Initializes all properties requested by frontend.
     * Like files for dataset with specific id.
     *
     * @return error if something goes wrong or null if success.
     */
    public String init() {

        final Optional<DatasetVersion> fetchedVersion = fetchDatasetVersion(this.datasetVersion.getId());

        if (!fetchedVersion.isPresent() || fetchedVersion.get().getDataset().isHarvested()) {
            return this.permissionsWrapper.notFound();
        }

        this.fileMetadatas = fetchedVersion.get().getAllFilesMetadataSorted();

        if (!this.permissionsWrapper.canCurrentUserUpdateDataset(this.datasetVersion.getDataset())) {
            return this.permissionsWrapper.notAuthorized();
        }

        return null;
    }

    public void moveUp(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(fileIndex - 1, fileToMove);

        this.lastReorderFromAndTo = new Tuple2<Integer, Integer>(fileIndex, fileIndex - 1);
        this.lastReorderFileMetadata = fileToMove;
    }

    public void moveDown(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(fileIndex + 1, fileToMove);

        this.lastReorderFromAndTo = Tuple.of(fileIndex, fileIndex + 1);
        this.lastReorderFileMetadata = fileToMove;
    }

    public void onRowReorder(final ReorderEvent event) {
        this.lastReorderFromAndTo = Tuple.of(event.getFromIndex(), event.getToIndex());
        this.lastReorderFileMetadata = this.fileMetadatas.get(event.getToIndex());
    }

    public void undoLastReorder() {
        final FileMetadata fileMoved = this.fileMetadatas.remove(this.lastReorderFromAndTo._2().intValue());
        this.fileMetadatas.add(this.lastReorderFromAndTo._1(), fileMoved);

        this.lastReorderFromAndTo = null;
        this.lastReorderFileMetadata = null;
    }

    /**
     * Reorders files display order if any were reordered, saves the changes to the database
     * and returns to the previous page.
     *
     * @return uri to previous page
     */
    public String saveFileOrder() {

        this.datasetVersionService.saveFileMetadata(FileMetadata.reorderDisplayOrder(this.fileMetadatas));

        return returnToPreviousPage();
    }

    /**
     * Method responsible for retrieving dataset from database.
     *
     * @param id
     * @return optional
     */
    private Optional<DatasetVersion> fetchDatasetVersion(final Long id) {
        return Optional.ofNullable(id)
                .map(datasetId -> this.datasetVersion = this.datasetVersionService.getById(datasetId));
    }

    /**
     * returns you to the dataset page.
     *
     * @return uri
     */
    public String returnToPreviousPage() {
        if (this.datasetVersion.isDraft()) {
            return "/dataset.xhtml?persistentId=" 
            		+ this.datasetVersion.getDataset().getGlobalId().asString() 
            		+ "&version=DRAFT&faces-redirect=true";
        }
        return "/dataset.xhtml?persistentId="
                    + this.datasetVersion.getDataset().getGlobalId().asString()
                    + "&faces-redirect=true&version="
                    + this.datasetVersion.getVersionNumber() 
                    + '.'
                    + this.datasetVersion.getMinorVersionNumber();
    }

    public String getTitle() {
        return getStringFromBundle("file.reorderFiles") + 
        		" - " + this.datasetVersion.getParsedTitle();
    }

    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }

    public List<FileMetadata> getFileMetadatas() {
        return this.fileMetadatas;
    }

    public Tuple2<Integer, Integer> getLastReorderFromAndTo() {
        return this.lastReorderFromAndTo;
    }

    public FileMetadata getLastReorderFileMetadata() {
        return this.lastReorderFileMetadata;
    }

    public void setDatasetVersion(final DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public void setFileMetadatas(final List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
    
    
}
