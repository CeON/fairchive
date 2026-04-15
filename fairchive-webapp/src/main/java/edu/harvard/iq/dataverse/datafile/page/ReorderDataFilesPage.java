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
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

@SuppressWarnings("serial")
@ViewScoped
@Named("ReorderDataFilesPage")
public class ReorderDataFilesPage implements java.io.Serializable {

    private DatasetVersionServiceBean datasetVersionService;
    private PermissionsWrapper permissionsWrapper;

    private Long datasetVersionId;
    private DatasetVersion datasetVersion = new DatasetVersion();
    private List<FileMetadata> fileMetadatas;
    private Change lastChange;
    
    
    public ReorderDataFilesPage() {}
    
    @Inject
    public ReorderDataFilesPage(final DatasetVersionServiceBean datasetVersionService,
    		final PermissionsWrapper permissionsWrapper) {
    	this.datasetVersionService = datasetVersionService;
    	this.permissionsWrapper = permissionsWrapper;
    }

    public String init() {
    	if(this.datasetVersionId == null) {
    		return this.permissionsWrapper.notFound();
    	}
    	
        final Optional<DatasetVersion> version = 
        		this.datasetVersionService.findById(this.datasetVersionId);

        if (!version.isPresent()) {
            return this.permissionsWrapper.notFound();
        }
        
        this.datasetVersion = version.get();
        
        if (getDataset().isHarvested()) {
            return this.permissionsWrapper.notFound();
        }

        if (!this.permissionsWrapper.canCurrentUserUpdateDataset(getDataset())) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        this.fileMetadatas = this.datasetVersion.getAllFilesMetadataSorted();
        return null;
    }

    public void moveUp(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(fileIndex - 1, fileToMove);

        this.lastChange = new Change(fileIndex, fileIndex - 1, fileToMove);
    }
    
    public void moveToTop(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(0, fileToMove);

        this.lastChange = new Change(fileIndex, 0, fileToMove);
    }

    public void moveDown(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(fileIndex + 1, fileToMove);

        this.lastChange = new Change(fileIndex, fileIndex + 1, fileToMove);
    }
    
    public void moveToBottom(final int fileIndex) {
        final FileMetadata fileToMove = this.fileMetadatas.remove(fileIndex);
        this.fileMetadatas.add(fileToMove);

        this.lastChange = new Change(fileIndex, this.fileMetadatas.size() -1, fileToMove);
    }

    public void onRowReorder(final ReorderEvent event) { 
        this.lastChange = new Change(event.getFromIndex(), event.getToIndex(), 
        		this.fileMetadatas.get(event.getToIndex()));
    }

    public void undoLastReorder() {
        final FileMetadata fileMoved = this.fileMetadatas.remove(this.lastChange.getToIndex());
        this.fileMetadatas.add(this.lastChange.getFromIndex(), fileMoved);

        this.lastChange = null;
    }

    public String saveFileOrder() {
        this.datasetVersionService.saveInOrder(this.fileMetadatas);
        return returnToPreviousPage();
    }

    public String returnToPreviousPage() {
        if (this.datasetVersion.isDraft()) {
            return "/dataset.xhtml?version=DRAFT&faces-redirect=true&persistentId=".
            		concat(this.datasetVersion.getDataset().getGlobalId().asString());
        } else {
        	return "/dataset.xhtml?faces-redirect=true&persistentId="
                    + this.datasetVersion.getDataset().getGlobalId().asString()
                    + "&version="
                    + this.datasetVersion.getVersionNumber() 
                    + '.'
                    + this.datasetVersion.getMinorVersionNumber();
        }
    }
    
    public String urlFor(final FileMetadata fileMetadata) {
    	final String gid = fileMetadata.getDataFile().getGlobalId().toString();
    	if(gid.isEmpty()) {
    		return "/file.xhtml?fileId=" + fileMetadata.getDataFile().getId() + 
    				"&version=" + fileMetadata.getDatasetVersion().getFriendlyVersionNumber();
    	} else {
    		return "/file.xhtml?persistentId=" + gid + 
    				"&version=" + fileMetadata.getDatasetVersion().getFriendlyVersionNumber();
    	}
    }

    public String getTitle() {
        return getStringFromBundle("file.reorderFiles") + " - " + 
        		this.datasetVersion.getParsedTitle();
    }
    
    public Long getDatasetVersionId() {
        return this.datasetVersionId;
    }
    
    public void setDatasetVersionId(final Long id) {
        this.datasetVersionId = id;
    }

    public Dataset getDataset() {
        return this.datasetVersion.getDataset();
    }

    public List<FileMetadata> getFileMetadatas() {
        return this.fileMetadatas;
    }
    
    public Change getLastChange() {
        return this.lastChange;
    }
    
    public boolean displayUndoLastReorder() {
    	return this.lastChange != null;
    }
    
    //--------------------------------------------------------------------------
    public static class Change {
    	private final int fromIndex;
    	private final int toIndex;
    	private final FileMetadata fileMetadata;
    	
		public Change(final int fromIndex, final int toIndex, 
				final FileMetadata fileMetadata) {
			
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
			this.fileMetadata = fileMetadata;
		}
		public int getFromIndex() {
			return this.fromIndex;
		}
		public int getToIndex() {
			return this.toIndex;
		}
		public FileMetadata getFileMetadata() {
			return this.fileMetadata;
		}
    }
}
