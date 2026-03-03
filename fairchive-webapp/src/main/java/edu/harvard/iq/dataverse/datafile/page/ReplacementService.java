package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
public class ReplacementService {

    // -------------------- LOGIC --------------------

    public List<ReplacementGroup> listReplacements(List<DataFile> existingFiles, List<DataFile> newFiles) {
        if (existingFiles == null || newFiles == null || newFiles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<DataFile>> existingFilesByName = existingFiles.stream()
                .filter(f -> f.getFileMetadata() != null && f.getFileMetadata().getLabel() != null)
                .filter(f -> f.getId() != null)
                .collect(Collectors.groupingBy(f -> f.getFileMetadata().getLabel()));

        List<ReplacementGroup> result = new ArrayList<>();
        for (DataFile newFile : newFiles) {
            if (newFile.getFileMetadata() == null || newFile.getFileMetadata().getLabel() == null || !hasChecksum(newFile)) {
                continue;
            }
            String name = newFile.getFileMetadata().getLabel();
            List<DataFile> existingWithSameName = existingFilesByName.getOrDefault(name, Collections.emptyList());
            
            existingWithSameName.stream()
                    .filter(this::hasChecksum)
                    .filter(f -> !newFile.getChecksumValue().equals(f.getChecksumValue()))
                    .findFirst()
                    .ifPresent(existingFile -> result.add(new ReplacementGroup(newFile, existingFile)));
        }
        return result;
    }

    // -------------------- PRIVATE --------------------

    private boolean hasChecksum(DataFile dataFile) {
        return dataFile != null && StringUtils.isNotBlank(dataFile.getChecksumValue());
    }

    // -------------------- INNER CLASSES --------------------

    public static class ReplacementGroup {
        private final DataFile newFile;
        private final CandidateForReplacement existingFile;

        private ReplacementGroup(DataFile newFile, DataFile existingFile) {
            this.newFile = newFile;
            this.existingFile = new CandidateForReplacement(existingFile);
        }

        public DataFile getNewFile() {
            return newFile;
        }

        public CandidateForReplacement getExistingFile() {
            return existingFile;
        }
    }

    public static class CandidateForReplacement {
        private final String label;
        private final DataFile dataFile;
        private boolean selected = false;

        private CandidateForReplacement(DataFile dataFile) {
            this.label = dataFile.getFileMetadata().getLabel();
            this.dataFile = dataFile;
        }

        public String getLabel() {
            return label;
        }

        public DataFile getDataFile() {
            return dataFile;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
