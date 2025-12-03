package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Leonid Andreev
 */
@SuppressWarnings("serial")
@ViewScoped
public class ThumbnailServiceWrapper implements java.io.Serializable {
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @EJB
    private DataverseDao dataverseDao;
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    @EJB
    private DataFileServiceBean dataFileService;
    @Inject
    private ImageThumbConverter imageThumbConverter;
    @Inject
    private SystemConfig systemConfig;

    private Map<Long, String> dvobjectThumbnailsMap = new HashMap<>();
    private Map<Long, DvObject> dvobjectViewMap = new HashMap<>();

    private String getAssignedDatasetImage(final Dataset dataset) {
        if (dataset == null) {
            return null;
        }

       final  DataFile assignedThumbnailFile = dataset.getThumbnailFile();

        if (assignedThumbnailFile != null) {
            final Long assignedThumbnailFileId = assignedThumbnailFile.getId();

            if (this.dvobjectThumbnailsMap.containsKey(assignedThumbnailFileId)) {
                // Yes, return previous answer
                return isNotEmpty(this.dvobjectThumbnailsMap.get(assignedThumbnailFileId))
                    ? this.dvobjectThumbnailsMap.get(assignedThumbnailFileId)
                    : null;
            }

            final String imageSourceBase64 = imageThumbConverter.getImageThumbnailAsBase64(
                    assignedThumbnailFile,
                    ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

            if (imageSourceBase64 != null) {
                this.dvobjectThumbnailsMap.put(assignedThumbnailFileId, imageSourceBase64);
                return imageSourceBase64;
            }

            // OK - we can't use this "assigned" image, because of permissions, or because
            // the thumbnail failed to generate, etc... in this case we'll
            // mark this dataset in the lookup map - so that we don't have to
            // do all these lookups again...
            this.dvobjectThumbnailsMap.put(assignedThumbnailFileId, EMPTY);

            // TODO: (?)
            // do we need to cache this datafile object in the view map?
            // -- L.A., 4.2.2
        }

        return null;

    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Datafile type!
    public String getFileCardImageAsBase64Url(final SolrSearchResult result) {
        // Before we do anything else, check if it's a harvested dataset;
        // no need to check anything else if so (harvested objects never have
        // thumbnails)

        if (result.isHarvested()) {
            return null;
        }

        final Long imageFileId = Optional.ofNullable(result.getEntity())
                .map(DvObject::getId)
                .orElse(null);
        final DataFile dataFile = (DataFile) result.getEntity();

        if (imageFileId != null) {
            if (this.dvobjectThumbnailsMap.containsKey(imageFileId)) {
                // Yes, return previous answer
                return isNotEmpty(this.dvobjectThumbnailsMap.get(imageFileId)) 
                    ? this.dvobjectThumbnailsMap.get(imageFileId)
                    : null;
            }


            if (result.getTabularDataTags() != null) {
                for (String tabularTagLabel : result.getTabularDataTags()) {
                    final DataFileTag tag = new DataFileTag();
                    try {
                        tag.setTypeByLabel(tabularTagLabel);
                        tag.setDataFile((DataFile) result.getEntity());
                        ((DataFile) result.getEntity()).addTag(tag);
                    } catch (IllegalArgumentException iax) {
                        // ignore
                    }
                }
            }

            String cardImageUrl = null;
            if ((!StringUtils.equals(result.getFileAccess(), SearchConstants.RESTRICTED)
                    || permissionsWrapper.hasDownloadFilePermission(dataFile))
                    && dataFileService.isThumbnailAvailable(dataFile)) {


                cardImageUrl = imageThumbConverter.getImageThumbnailAsBase64(
                        dataFile,
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            }

            if (cardImageUrl != null) {
                this.dvobjectThumbnailsMap.put(imageFileId, cardImageUrl);

                if (!(dvobjectViewMap.containsKey(imageFileId)
                        && dvobjectViewMap.get(imageFileId).isInstanceofDataFile())) {

                    dvobjectViewMap.put(imageFileId, result.getEntity());

                }
                return cardImageUrl;
            } else {
                this.dvobjectThumbnailsMap.put(imageFileId, EMPTY);
            }
        }
        return null;
    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataset type!
    public String getDatasetCardImageAsBase64Url(final SolrSearchResult result) {
        // Before we do anything else, check if it's a harvested dataset;
        // no need to check anything else if so (harvested datasets never have
        // thumbnails)

        if (result.isHarvested()) {
            return null;
        }

        // Check if the search result ("card") contains an entity, before
        // attempting to convert it to a Dataset. It occasionally happens that
        // solr has indexed datasets that are no longer in the database. If this
        // is the case, the entity will be null here; and proceeding any further
        // results in a long stack trace in the log file.
        if (result.getEntity() == null) {
            return null;
        }
        final Dataset dataset = (Dataset) result.getEntity();

        final Long versionId = result.getDatasetVersionId();

        final boolean autoselect = result.isPublishedState() && !systemConfig.isReadonlyMode();
        return getDatasetCardImageAsBase64Url(dataset, versionId, autoselect);
    }

    public String getDatasetCardImageAsBase64Url(final Dataset dataset, 
    		final Long versionId, final boolean autoselect) {

        final Long datasetId = dataset.getId();
        if (datasetId != null) {
            if (this.dvobjectThumbnailsMap.containsKey(datasetId)) {
                // Yes, return previous answer
                // (at max, there could only be 2 cards for the same dataset
                // on the page - the draft, and the published version; but it's
                // still nice to try and cache the result - especially if it's an
                // uploaded logo - we don't want to read it off disk twice).

				return isNotEmpty(this.dvobjectThumbnailsMap.get(datasetId)) 
						? this.dvobjectThumbnailsMap.get(datasetId)
						: null;
            }
        }

        if (dataset.isUseGenericThumbnail()) {
            this.dvobjectThumbnailsMap.put(datasetId, EMPTY);
            return null;
        }

        StorageIO<Dataset> storageIO = null;
        try {
            storageIO = DataAccess.dataAccess().getStorageIO(dataset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream in = null;
        // See if the dataset already has a dedicated thumbnail ("logo") saved as
        // an auxilary file on the dataset level:
        // (don't bother checking if it exists; just try to open the input stream)
        try {
            in = storageIO.getAuxFileAsInputStream(DatasetThumbnailService.datasetLogoThumbnail48);
        } catch (Exception ioex) {
            //ignore
        }
        
        String cardImageUrl = null;

        if (in != null) {
            try {
                final byte[] bytes = IOUtils.toByteArray(in);
                final String base64image = Base64.getEncoder().encodeToString(bytes);
                cardImageUrl = FileUtil.DATA_URI_SCHEME + base64image;
                this.dvobjectThumbnailsMap.put(datasetId, cardImageUrl);
                return cardImageUrl;
            } catch (IOException ex) {
                this.dvobjectThumbnailsMap.put(datasetId, EMPTY);
                return null;
                // (alternatively, we could ignore the exception, and proceed with the
                // regular process of selecting the thumbnail from the available
                // image files - ?)
            } finally {
                closeQuietly(in);
            }
        }

        // If not, see if the dataset has one of its image files already assigned
        // to be the designated thumbnail:
        cardImageUrl = this.getAssignedDatasetImage(dataset);

        if (cardImageUrl != null) {
            return cardImageUrl;
        }

        // And finally, try to auto-select the thumbnail (unless instructed not to):

        if (!autoselect) {
            return null;
        }

        // We attempt to auto-select via the optimized, native query-based method
        // from the DatasetVersionService:
        final Long thumbnailImageFileId = datasetVersionService.getThumbnailByVersionId(versionId);

        if (thumbnailImageFileId != null) {
            if (this.dvobjectThumbnailsMap.containsKey(thumbnailImageFileId)) {
                // Yes, return previous answer
                return isNotEmpty(this.dvobjectThumbnailsMap.get(thumbnailImageFileId))
						? this.dvobjectThumbnailsMap.get(thumbnailImageFileId)
						: null;
            }

            DataFile thumbnailImageFile = null;

            if (dvobjectViewMap.containsKey(thumbnailImageFileId)
                    && dvobjectViewMap.get(thumbnailImageFileId).isInstanceofDataFile()) {
                thumbnailImageFile = (DataFile) dvobjectViewMap.get(thumbnailImageFileId);
            } else {
                thumbnailImageFile = dataFileService.findCheapAndEasy(thumbnailImageFileId);
                if (thumbnailImageFile != null) {
                    // TODO:
                    // do we need this file on the map? - it may not even produce
                    // a thumbnail!
                    dvobjectViewMap.put(thumbnailImageFileId, thumbnailImageFile);
                } else {
                    this.dvobjectThumbnailsMap.put(thumbnailImageFileId, EMPTY);
                    return null;
                }
            }

            if (dataFileService.isThumbnailAvailable(thumbnailImageFile)) {
                cardImageUrl = imageThumbConverter.getImageThumbnailAsBase64(
                        thumbnailImageFile,
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            }

            this.dvobjectThumbnailsMap.put(thumbnailImageFileId, cardImageUrl != null ? cardImageUrl : EMPTY);
        }
        return cardImageUrl;
    }

    // it's the responsibility of the user - to make sure the search result
    // passed to this method is of the Dataverse type!
    public String getDataverseCardImageAsBase64Url(final SolrSearchResult result) {
        final String thumbnailPath = dataverseDao.getDataverseLogoThumbnailFilePath(result.getEntityId());
        if (thumbnailPath != null) {
            return imageThumbConverter.getImageAsBase64FromFile(new File(thumbnailPath));
        }
        return null;
    }

    public void resetObjectMaps() {
        dvobjectThumbnailsMap = new HashMap<>();
        dvobjectViewMap = new HashMap<>();
    }


}
