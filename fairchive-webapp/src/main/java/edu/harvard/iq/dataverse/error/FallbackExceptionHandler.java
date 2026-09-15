package edu.harvard.iq.dataverse.error;

import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class designed for handling exception that were not handled explicitly.
 * <p>
 * If request was async(Ajax) banner is going to be displayed, otherwise user is redirected to 500.xhtml.
 */
public class FallbackExceptionHandler extends ExceptionHandlerWrapper {

    private static final String UNHANDLED_EXCEPTION_MESSAGE = "Unhandled exception while serving a JSF request";

    private static final String SEND_ERROR_FAILURE_MESSAGE = "Failed to send the HTTP 500 error page";

    private static final Logger logger = Logger.getLogger(FallbackExceptionHandler.class.getName());

    private ExceptionHandler exceptionHandler;

    // -------------------- CONSTRUCTORS --------------------

    public FallbackExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    // -------------------- GETTERS --------------------

    @Override
    public ExceptionHandler getWrapped() {
        return exceptionHandler;
    }

    // -------------------- LOGIC --------------------

    /**
     * Logs every queued exception and then reports the failure to the user.
     * <p>
     * The whole queue is drained before anything else happens, so that the
     * log is complete even when producing the response fails.
     * <p>
     * When there is no longer a {@link FacesContext} to answer with, the
     * exceptions have already been logged and there is nothing left to report.
     */
    @Override
    public void handle() throws FacesException {
        if (!logAndDrainQueue()) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (facesContext == null) {
            return;
        }

        if (facesContext.getPartialViewContext().isAjaxRequest()) {
            JsfHelper.addErrorMessage(getStringFromBundle("error.general.message"), "");
        } else {
            sendInternalServerError(facesContext.getExternalContext());
        }
    }

    // -------------------- PRIVATE --------------------

    /**
     * Logs and removes every unhandled queued exception.
     *
     * @return true when at least one exception was logged
     */
    private boolean logAndDrainQueue() {
        boolean anyExceptionLogged = false;
        Iterator<ExceptionQueuedEvent> queue = getUnhandledExceptionQueuedEvents().iterator();

        while (queue.hasNext()) {
            Throwable exception = queue.next().getContext().getException();

            logger.log(Level.SEVERE, UNHANDLED_EXCEPTION_MESSAGE, exception);
            queue.remove();
            anyExceptionLogged = true;
        }
        return anyExceptionLogged;
    }

    private void sendInternalServerError(ExternalContext externalContext) {
        Try.run(() -> externalContext.responseSendError(SC_INTERNAL_SERVER_ERROR, null))
                .onFailure(throwable -> logger.log(Level.SEVERE, SEND_ERROR_FAILURE_MESSAGE, throwable));
    }
}
