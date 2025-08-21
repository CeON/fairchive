package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class UningestInfoService {
    private IngestServiceBean ingestService;

    // -------------------- CONSTRUCTORS --------------------

    public UningestInfoService() { }

    @Inject
    public UningestInfoService(final IngestServiceBean ingestService) {
        this.ingestService = ingestService;
    }

    // -------------------- LOGIC --------------------

    public List<DataFile> listUningestableFiles(final Dataset dataset) {
        if (dataset == null || !dataset.getLatestVersion().isDraft()) {
            return emptyList();
        }
        final List<DataFile> uningestable = new ArrayList<>();
        // Only certain files from draft version can be uningested:
        for (final FileMetadata metadata : dataset.getLatestVersion().getFileMetadatas()) {
            final DataFile dataFile = metadata.getDataFile();
            if (canUningestFile(dataFile)) {
                uningestable.add(dataFile);
            }
        }
        return uningestable;
    }

    public boolean hasUningestableFiles(final Dataset dataset) {
       if (dataset == null || !dataset.getLatestVersion().isDraft()) {
           return false;
       }
       return dataset.getLatestVersion().getFileMetadatas().stream()
               .map(FileMetadata::getDataFile)
               .anyMatch(this::canUningestFile);
    }

    // -------------------- PRIVATE --------------------

    private boolean canUningestFile(final DataFile file) {
        // File from draft version can be uningested if it:
        // (1) was not published yet (ie. it has only one metadata set, but we assume that
        //     the file is from the latest, draft version – which is NOT checked here);
        // (2) is of XLSX, CSV or TSV type;
        // (3) has been ingested (successfully or not).
        return file.getFileMetadatas().size() == 1 // 1
                && ingestService.isSelectivelyIngestableFile(file) // 2
                && (file.getIngestStatus() != DataFile.INGEST_STATUS_NONE || file.isTabularData()); // 3
    }
}