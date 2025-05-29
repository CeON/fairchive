package edu.harvard.iq.dataverse.ingest;

import java.util.ArrayList;
import java.util.List;

public class StartIngestResult {

    private List<DataFileExceededSizeInfo> skippedExceedingSizeDataFiles = new ArrayList<>();


    public List<DataFileExceededSizeInfo> getSkippedExceedingSizeDataFiles() {
        return skippedExceedingSizeDataFiles;
    }

    public void addSkippedExceedingSizeInfo(DataFileExceededSizeInfo info) {
        skippedExceedingSizeDataFiles.add(info);
    }

    public boolean hasSkippedExceedingSizeDataFiles() {
        return !skippedExceedingSizeDataFiles.isEmpty();
    }


    public static class DataFileExceededSizeInfo {
        private String label;
        private long maxSize;

        public DataFileExceededSizeInfo(String label, long maxSize) {
            this.label = label;
            this.maxSize = maxSize;
        }

        public String getLabel() {
            return label;
        }

        public long getMaxSize() {
            return maxSize;
        }

    }
}
