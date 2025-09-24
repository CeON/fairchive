package edu.harvard.iq.dataverse.engine.command.exception;

import javax.ejb.ApplicationException;

@SuppressWarnings("serial")
@ApplicationException(rollback = true)
public class NoDatasetFilesException extends RuntimeException {

    public NoDatasetFilesException(final String message) {
        super(message);
    }

    public NoDatasetFilesException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
