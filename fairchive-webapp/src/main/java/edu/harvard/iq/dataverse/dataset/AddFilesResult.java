package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

public class AddFilesResult {

    private Dataset dataset;
    
    private int savedFilesCount;

    private int filesCount;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public AddFilesResult(Dataset dataset, int savedFilesCount, int filesCount) {
        this.dataset = dataset;
        this.filesCount = filesCount;
        this.savedFilesCount = savedFilesCount;
    }

    // -------------------- GETTERS --------------------
    
    public Dataset getDataset() {
        return dataset;
    }

    public int getSavedFilesCount() {
        return savedFilesCount;
    }

    public int getFilesCount() {
        return filesCount;
    }

}
