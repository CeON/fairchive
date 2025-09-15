package edu.harvard.iq.dataverse.api.imports;

/**
 * Thrown when something goes wrong in the import process
 *
 * @author ellenk
 */
@SuppressWarnings("serial")
public class ImportException extends Exception {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
