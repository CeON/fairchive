package edu.harvard.iq.dataverse.api.imports;

@SuppressWarnings("serial")
public class ImportException extends Exception {
	
    public ImportException(final String message) {
        super(message);
    }
    
    public ImportException(final Throwable cause) {
        super(cause);
    }

    public ImportException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
