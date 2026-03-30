package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Authority;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataFilePIDFormat;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.IdentifierGenerationStyle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Protocol;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Shoulder;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.common.files.mime.MimeTypes;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;

/**
 * @author Leonid Andreev
 *
 * Basic skeleton of the new DataFile service for DVN 4.0
 */

@SuppressWarnings("serial")
@Stateless
@Named
public class DataFileServiceBean implements Serializable {

    private static final Logger logger = getLogger(DataFileServiceBean.class.getCanonicalName());
    @EJB
    private DvObjectServiceBean dvObjectService;
    @Inject
    private SettingsServiceBean settingsService;
    @Inject
    private ImageThumbConverter imageThumbConverter;
    
    @Inject
    private DataFileRepository fileRepo;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private DataAccess dataAccess = DataAccess.dataAccess();

    // -------------------- LOGIC --------------------

    public Optional<DataFile> find(final Long id) {
        return this.fileRepo.findById(id);
    }

    public DataFile findByGlobalId(final String globalId) {
        return (DataFile) this.dvObjectService.findByGlobalId(globalId, 
        		DataFile.DATAFILE_DTYPE_STRING);
    }

    public Optional<DataFile> findReplacementFile(final Long previousFileId) {
        return this.fileRepo.findReplacementFile(previousFileId);
    }

    public List<DataFile> findAllRelatedByRootDatafileId(final Long id) {
    	return this.fileRepo.findAllRelatedByRootDataFileId(id);
    }

    public List<DataFile> findDataFilesByFileMetadataIds(Collection<Long> fileMetadataIds) {
        return em.createQuery("SELECT d FROM FileMetadata f JOIN f.dataFile d WHERE f.id IN :fileMetadataIds", DataFile.class)
                .setParameter("fileMetadataIds", fileMetadataIds)
                .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                .getResultList();
    }

    public DataFile findByStorageIdAndDatasetVersion(String storageId, DatasetVersion dv) {
        try {
            Query query = em.createNativeQuery("select o.id from dvobject o, filemetadata m " +
                    "where o.storageidentifier = '" + storageId +
                    "' and o.id = m.datafile_id and m.datasetversion_id = " + dv.getId() + "");
            query.setMaxResults(1);
            if (query.getResultList().isEmpty()) {
                return null;
            } else {
                return findCheapAndEasy((Long) query.getSingleResult());
            }
        } catch (Exception e) {
            logger.error("Error finding datafile by storageID and DataSetVersion: " + e.getMessage(), e);
            return null;
        }
    }

    public List<FileMetadata> findFileMetadataByDatasetVersionId(Long datasetVersionId, 
            int maxResults, FileSortFieldAndOrder sortFieldAndOrder) {
        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        String sortFieldString = sortFieldAndOrder.getSortField();
        String sortOrderString = sortFieldAndOrder.getSortOrder() == SortOrder.desc ? "desc" : "asc";
        String qr = "select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." +
                sortFieldString + " " + sortOrderString;
        return em.createQuery(qr, FileMetadata.class)
                 .setParameter("datasetVersionId", datasetVersionId)
                 .setMaxResults(maxResults)
                 .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(Long datasetVersionId, String searchTerm,
                                                                              FileSortFieldAndOrder sortFieldAndOrder) {
        String searchClause = "";
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchClause = " and  (lower(o.label) like '%" 
                    + searchTerm.toLowerCase() 
                    + "%' or lower(o.description) like '%"
                    + searchTerm.toLowerCase() + "%')";
        }

        // the createNativeQuary takes persistant entities, which Integer.class is not,
        // which is causing the exception. Hence, this query does not need an Integer.class
        // as the second parameter.
        return em.createNativeQuery("select o.id from FileMetadata o where o.datasetVersion_id = " + datasetVersionId +
                searchClause + " order by o." + sortFieldAndOrder.getSortField() + " " +
                (sortFieldAndOrder.getSortOrder().toString()))
                .getResultList();
    }

    public FileMetadata findFileMetadata(Long fileMetadataId) {
        return em.find(FileMetadata.class, fileMetadataId);
    }

    public FileMetadata findFileMetadataByDatasetVersionIdAndDataFileId(Long datasetVersionId, Long dataFileId) {

        Query query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId  and o.dataFile.id = :dataFileId");
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setParameter("dataFileId", dataFileId);
        try {
            return (FileMetadata) query.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public FileMetadata findMostRecentVersionFileIsIn(DataFile file) {
        if (file == null) {
            return null;
        }
        List<FileMetadata> fileMetadatas = file.getFileMetadatas();
        return fileMetadatas != null && !fileMetadatas.isEmpty()
                ? fileMetadatas.get(0) : null;
    }

    public DataFile findCheapAndEasy(Long id) {
        DataFile dataFile;
        Object[] result;
        try {
            result = (Object[]) em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, " +
                    "t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, " +
                    "t0.PREVIEWIMAGEAVAILABLE, t1.CONTENTTYPE, t0.STORAGEIDENTIFIER, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, " +
                    "t3.ID, t2.AUTHORITY, t2.IDENTIFIER, t1.CHECKSUMTYPE, t1.PREVIOUSDATAFILEID, t1.ROOTDATAFILEID, t0.AUTHORITY, " +
                    "T0.PROTOCOL, T0.IDENTIFIER FROM DVOBJECT t0, DATAFILE t1, DVOBJECT t2, DATASET t3 WHERE ((t0.ID = " + id + ") " +
                    "AND (t0.OWNER_ID = t2.ID) AND (t2.ID = t3.ID) AND (t1.ID = t0.ID))")
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }

        if (result == null) {
            return null;
        }

        Integer file_id = (Integer) result[0];
        dataFile = new DataFile();
        dataFile.setMergeable(false);
        dataFile.setId(file_id.longValue());

        Timestamp createDate = (Timestamp) result[1];
        Timestamp indexTime = (Timestamp) result[2];
        Timestamp modificationTime = (Timestamp) result[3];
        Timestamp permissionIndexTime = (Timestamp) result[4];
        Timestamp permissionModificationTime = (Timestamp) result[5];
        Timestamp publicationDate = (Timestamp) result[6];

        dataFile.setCreateDate(createDate);
        dataFile.setIndexTime(indexTime);
        dataFile.setModificationTime(modificationTime);
        dataFile.setPermissionIndexTime(permissionIndexTime);
        dataFile.setPermissionModificationTime(permissionModificationTime);
        dataFile.setPublicationDate(publicationDate);

        Boolean previewAvailable = (Boolean) result[9];
        if (previewAvailable != null) {
            dataFile.setPreviewImageAvailable(previewAvailable);
        }

        String contentType = (String) result[10];
        if (contentType != null) {
            dataFile.setContentType(contentType);
        }

        String storageIdentifier = (String) result[11];
        if (storageIdentifier != null) {
            dataFile.setStorageIdentifier(storageIdentifier);
        }

        Long fileSize = (Long) result[12];
        if (fileSize != null) {
            dataFile.setFilesize(fileSize);
        }

        if (result[13] != null) {
            String ingestStatusString = (String) result[13];
            dataFile.setIngestStatus(ingestStatusString.charAt(0));
        }

        String md5 = (String) result[14];
        if (md5 != null) {
            dataFile.setChecksumValue(md5);
        }

        Dataset owner = new Dataset();

        // TODO: check for nulls
        owner.setId((Long) result[15]);
        owner.setAuthority((String) result[16]);
        owner.setIdentifier((String) result[17]);

        String checksumType = (String) result[18];
        if (checksumType != null) {
            try {
                // In the database we store "SHA1" rather than "SHA-1".
                DataFile.ChecksumType typeFromStringInDatabase = DataFile.ChecksumType.valueOf(checksumType);
                dataFile.setChecksumType(typeFromStringInDatabase);
            } catch (IllegalArgumentException ex) {
                logger.info("Exception trying to convert " + checksumType + " to enum: " + ex);
            }
        }

        Long previousDataFileId = (Long) result[19];
        if (previousDataFileId != null) {
            dataFile.setPreviousDataFileId(previousDataFileId);
        }

        Long rootDataFileId = (Long) result[20];
        if (rootDataFileId != null) {
            dataFile.setRootDataFileId(rootDataFileId);
        }

        String authority = (String) result[21];
        if (authority != null) {
            dataFile.setAuthority(authority);
        }

        String protocol = (String) result[22];
        if (protocol != null) {
            dataFile.setProtocol(protocol);
        }

        String identifier = (String) result[23];
        if (identifier != null) {
            dataFile.setIdentifier(identifier);
        }

        dataFile.setOwner(owner);

        // If content type indicates it's tabular data, spend 2 extra queries
        // looking up the data table and tabular tags objects:

        if (MimeTypes.TSV.equalsIgnoreCase(contentType)) {
            Object[] dtResult;
            try {
                dtResult = (Object[]) em
                        .createNativeQuery("SELECT ID, UNF, CASEQUANTITY, VARQUANTITY, ORIGINALFILEFORMAT, ORIGINALFILESIZE FROM dataTable WHERE DATAFILE_ID = " + id)
                        .getSingleResult();
            } catch (Exception ex) {
                dtResult = null;
            }

            if (dtResult == null) {
                return dataFile;
            }
            DataTable dataTable = new DataTable();
            dataTable.setId(((Integer) dtResult[0]).longValue());
            dataTable.setUnf((String) dtResult[1]);
            dataTable.setCaseQuantity((Long) dtResult[2]);
            dataTable.setVarQuantity((Long) dtResult[3]);
            dataTable.setOriginalFileFormat((String) dtResult[4]);
            dataTable.setOriginalFileSize((Long) dtResult[5]);
            dataTable.setDataFile(dataFile);
            dataFile.setDataTable(dataTable);

            // tabular tags:
            try {
                @SuppressWarnings("unchecked")
                List<Object[]> tagResults = em.createNativeQuery("SELECT t.TYPE, t.DATAFILE_ID FROM DATAFILETAG t WHERE t.DATAFILE_ID = " + id)
                        .getResultList();
                List<String> fileTagLabels = DataFileTag.listTags();

                for (Object[] tagResult : tagResults) {
                    Integer tagId = (Integer) tagResult[0];
                    DataFileTag tag = new DataFileTag();
                    tag.setTypeByLabel(fileTagLabels.get(tagId));
                    tag.setDataFile(dataFile);
                    dataFile.addTag(tag);
                }
            } catch (Exception ex) {
                logger.info("EXCEPTION looking up tags.");
            }
        }
        return dataFile;
    }

    public DataTable findDataTableByFileId(Long fileId) {
        Query query = em.createQuery("select object(o) from DataTable as o where o.dataFile.id =:fileId order by o.id");
        query.setParameter("fileId", fileId);
        try {
            return (DataTable) query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public List<DataFile> findAll() {
        return this.fileRepo.findAll();
    }

    public DataFile save(final DataFile file) {
        if (file.isMergeable()) {
            return this.fileRepo.save(file);
        } else {
            throw new IllegalArgumentException(
            		"This DataFile object has been set to NOT MERGEABLE; please ensure " +
                    "a MERGEABLE object is passed to the save method.");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DataFile saveInNewTransaction(DataFile dataFile) {
        return save(dataFile);
    }

    public void deleteFromVersion(DatasetVersion d, DataFile f) {
        em.createNamedQuery("DataFile.removeFromDatasetVersion")
          .setParameter("versionId", d.getId()).setParameter("fileId", f.getId())
          .executeUpdate();
    }

    /*
     Convenience methods for merging and removingindividual file metadatas,
     without touching the rest of the DataFile object:
    */
    public void removeFileMetadata(FileMetadata fileMetadata) {
        FileMetadata mergedFM = em.merge(fileMetadata);
        em.remove(mergedFM);
    }

    // Same, for DataTables:
    public DataTable saveDataTable(DataTable dataTable) {
        DataTable merged = em.merge(dataTable);
        em.flush();
        return merged;
    }

    public List<DataFile> findHarvestedFilesByClient(HarvestingClient harvestingClient) {
        String qr = "SELECT d FROM DataFile d, DvObject o, Dataset s WHERE o.id = d.id AND o.owner.id = s.id " +
                "AND s.harvestedFrom.id = :harvestingClientId";
        return em.createQuery(qr, DataFile.class)
                 .setParameter("harvestingClientId", harvestingClient.getId())
                 .getResultList();
    }

    /**
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
     */
    public boolean isThumbnailAvailable(DataFile file) {
        if (file == null) {
            return false;
        }

        // If this file already has the "thumbnail generated" flag set, we'll just trust that:
        if (file.isPreviewImageAvailable()) {
            logger.trace("returning true");
            return true;
        }

        // Checking the permission here was resulting in extra queries; it is now the responsibility
        // of the client - such as the DatasetPage - to make sure the permission check out, before
        // calling this method. (or *after* calling this method? - checking permissions costs db queries;
        // checking if the thumbnail is available may cost cpu time, if it has to be generated on the fly
        //  - so you have to figure out which is more important...
        if (imageThumbConverter.isThumbnailAvailable(file)) {
            file = find(file.getId()).get();
            file.setPreviewImageAvailable(true);
            save(file);
            return true;
        }

        return false;
    }

    public boolean hasReplacement(final DataFile file) {
        return file.isNew() 
        		? false 
        		: this.fileRepo.findReplacementFile(file.getId()).isPresent();
    }

    public boolean hasBeenDeleted(DataFile df) {
        Dataset dataset = df.getOwner();
        DatasetVersion dsv = dataset.getLatestVersion();
        return findFileMetadataByDatasetVersionIdAndDataFileId(dsv.getId(), df.getId()) == null;
    }

    @SuppressWarnings("unchecked")
    public List<Long> selectFilesWithMissingOriginalTypes() {
        return em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id " +
                "AND (t.originalfileformat='" + MimeTypes.TSV
                + "' OR t.originalfileformat IS NULL) ORDER BY f.id")
                .getResultList(); 
    }

    @SuppressWarnings("unchecked")
    public List<Long> selectFilesWithMissingOriginalSizes() {
        return em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id " +
                "AND (t.originalfilesize IS NULL) AND (t.originalfileformat IS NOT NULL) ORDER BY f.id")
                .getResultList();
    }

    public String generateDataFileIdentifier(DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String doiIdentifierType = settingsService.getValueForKey(IdentifierGenerationStyle);
        String doiDataFileFormat = settingsService.getValueForKey(DataFilePIDFormat);

        String prepend = "";
        if (doiDataFileFormat.equals("DEPENDENT")) {
            // If format is dependent then pre-pend the dataset identifier
            prepend = datafile.getOwner().getIdentifier() + "/";
        } else {
            // If there's a shoulder prepend independent identifiers with it
            prepend = settingsService.getValueForKey(Shoulder);
        }

        switch (doiIdentifierType) {
            case "randomString":
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
            case "sequentialNumber":
                return doiDataFileFormat.equals("INDEPENDENT")
                        ? generateIdentifierAsIndependentSequentialNumber(datafile, idServiceBean, prepend)
                        : generateIdentifierAsDependentSequentialNumber(datafile, idServiceBean, prepend);
            default:
                // Should we throw an exception instead?? -- L.A. 4.6.2
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
        }
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network). Also check for duplicate
     * in the remote PID service if needed
     *
     * @return {@code true} iff the global identifier is unique.
     */
    public boolean isGlobalIdUnique(String userIdentifier, DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String testAuthority = datafile.getAuthority() != null
                ? datafile.getAuthority()
                : settingsService.getValueForKey(Authority);
        String testProtocol = datafile.getProtocol() != null
                ? datafile.getProtocol()
                : settingsService.getValueForKey(Protocol);

        boolean unique = em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
                      .setParameter("protocol", testProtocol)
                      .setParameter("authority", testAuthority)
                      .setParameter("identifier", userIdentifier)
                      .getResultList().isEmpty();

        try {
            if (idServiceBean.alreadyExists(new GlobalId(testProtocol,
                    testAuthority, userIdentifier))) {
                unique = false;
            }
        } catch (Exception e) {
            // we can live with failure - means identifier not found remotely
        }
        return unique;
    }

    /**
     * (File service will double-check that the datafile no
     * longer exists in the database, before proceeding to
     * delete the physical file)
     */
    public void finalizeFileDelete(Long dataFileId, String storageLocation) 
            throws IOException {
        // Verify that the DataFile no longer exists:
        // force a read from the database, to make the entity manager not use the potentially obsolete local cache
        // https://github.com/CeON/fairchive/issues/2810
        if (em.find(DataFile.class, dataFileId, LockModeType.PESSIMISTIC_READ) != null) {
            throw new IOException("Attempted to permanently delete a physical file still associated with an existing DvObject "
                                          + "(id: " + dataFileId + ", location: " + storageLocation);
        }
        StorageIO<?> directStorageAccess = dataAccess.getDirectStorageIO(storageLocation);
        directStorageAccess.delete();
    }

    public void finalizeFileDeletes(Map<Long, String> storageLocations) {
        storageLocations.keySet().stream().forEach(dataFileId -> {
            String storageLocation = storageLocations.get(dataFileId);
            try {
                finalizeFileDelete(dataFileId, storageLocation);
            } catch (IOException ioex) {
                logger.warn("Failed to delete the physical file associated with the deleted datafile id="
                                    + dataFileId + ", storage location: " + storageLocation, ioex);
            }
        });
    }

    public Map<Long, String> getPhysicalFilesToDelete(DatasetVersion datasetVersion) {
        return getPhysicalFilesToDelete(datasetVersion, false);
    }

    public Map<Long, String> getPhysicalFilesToDelete(DatasetVersion datasetVersion, boolean destroy) {
        // Gather the locations of the physical files associated with DRAFT
        // (unpublished) DataFiles (or ALL the DataFiles, if "destroy") in the
        // DatasetVersion, that will need to be deleted once the
        // DeleteDatasetVersionCommand execution has been finalized:

        return getPhysicalFilesToDelete(
                datasetVersion.getFileMetadatas().stream()
                              .map(FileMetadata::getDataFile)
                              .collect(toList()),
                destroy);
    }

    public Map<Long, String> getPhysicalFilesToDelete(List<DataFile> filesToDelete) {
        return getPhysicalFilesToDelete(filesToDelete, false);
    }

    public Map<Long, String> getPhysicalFilesToDelete(List<DataFile> filesToDelete, boolean destroy) {
        Map<Long, String> deleteStorageLocations = new HashMap<>();

        filesToDelete.stream()
                     .filter(file -> !file.isReleased() || destroy)
                     .forEach(file -> deleteStorageLocations.put(file.getId(), getPhysicalFileToDelete(file)));

        return deleteStorageLocations;
    }

    public Map<Long, String> getPhysicalFilesToDelete(Dataset dataset) {
        // Gather the locations of ALL the physical files associated with
        // a DATASET that is being DESTROYED, that will need to be deleted
        // once the DestroyDataset command execution has been finalized.
        // Once again, note that we are selecting all the files from the dataset
        // - not just drafts.

        Map<Long, String> deleteStorageLocations = new HashMap<>();

        for (DataFile df : dataset.getFiles()) {
            String storageLocation = getPhysicalFileToDelete(df);
            if (storageLocation != null) {
                deleteStorageLocations.put(df.getId(), storageLocation);
            }
        }
        return deleteStorageLocations;
    }

    public String getPhysicalFileToDelete(DataFile dataFile) {
        try {
            StorageIO<DataFile> storageIO = dataAccess.getStorageIO(dataFile);
            return storageIO.getStorageLocation();
        } catch (IOException ioe) {
            // something potentially wrong with the physical file,
            // or connection to the physical storage?
            // we don't care (?) - we'll still try to delete the datafile from the database.
            logger.warn("IO issue encountered:", ioe);
        }
        return null;
    }

    public boolean isSameTermsOfUse(FileTermsOfUse termsOfUse1, FileTermsOfUse termsOfUse2) {
        return termsOfUse1.isSameAs(termsOfUse2);
    }

    // -------------------- PRIVATE --------------------

    private String generateIdentifierAsRandomString(DataFile datafile, 
            GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier = null;
        do {
            identifier = prepend + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));
        return identifier;
    }

    private String generateIdentifierAsIndependentSequentialNumber(DataFile datafile, 
            GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier;
        do {
            StoredProcedureQuery query = em.createNamedStoredProcedureQuery("Dataset.generateIdentifierAsSequentialNumber");
            query.execute();
            Integer identifierNumeric = (Integer) query.getOutputParameterValue(1);
            // some diagnostics here maybe - is it possible to determine that it's failing
            // because the stored procedure hasn't been created in the database?
            if (identifierNumeric == null) {
                return null;
            }
            identifier = prepend + identifierNumeric.toString();
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }

    private String generateIdentifierAsDependentSequentialNumber(DataFile datafile, 
            GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier;
        long retVal = 0L;
        do {
            retVal++;
            identifier = prepend + retVal;
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }
}
