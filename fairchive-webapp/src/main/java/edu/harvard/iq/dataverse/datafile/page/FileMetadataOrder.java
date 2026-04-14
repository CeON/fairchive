package edu.harvard.iq.dataverse.datafile.page;

import java.util.ArrayList;
import java.util.List;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;

class FileMetadataOrder {

    /**
     * Sets display order same as index in the list.
     *
     * @param filesToReorder
     * @return filemetadas with changed display order.
     */
    static List<FileMetadata> reorderDisplayOrder(final List<FileMetadata> filesToReorder) {
        final List<FileMetadata> changes = new ArrayList<>(filesToReorder.size());

        for (int i = 0; i < filesToReorder.size(); i++) {
            final FileMetadata fileMetadata = filesToReorder.get(i);
            if (fileMetadata.getDisplayOrder() != i) {
                fileMetadata.setDisplayOrder(i);
                changes.add(fileMetadata);
            }
        }

        return changes;
    }
}
