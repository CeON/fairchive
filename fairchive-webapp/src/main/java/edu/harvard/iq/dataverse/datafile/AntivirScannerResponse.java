package edu.harvard.iq.dataverse.datafile;

public class AntivirScannerResponse {

    private final boolean fileInfected;
    private final String message;
    
    public AntivirScannerResponse(final boolean fileInfected, final String message) {
        this.fileInfected = fileInfected;
        this.message = message;
    }
    public boolean isFileInfected() {
        return this.fileInfected;
    }
    public String getMessage() {
        return this.message;
    }
}
