package edu.harvard.iq.dataverse;

import org.slf4j.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Deletes the temporary files of a multipart request's parts once the
 * request has been processed, if it was marked with
 * {@link #deletePartsAfterRequest()}.
 * <p>
 * GlassFish writes every part of a multipart request to a temporary file and
 * keeps it until the part is deleted, which nothing else does, so uploaded
 * files would otherwise pile up until the application is redeployed.
 * <p>
 * Only a listener that is done with the uploaded file by the end of the
 * request may mark it. One that keeps the file for a later request must not;
 * the contact form, for one, reads its attachments again when the message is
 * sent.
 *
 * @author akowal
 */
public class MultipartPartsCleanupFilter implements Filter {

    private static final Logger log = getLogger(MultipartPartsCleanupFilter.class);

    public static final String DELETE_PARTS_ATTRIBUTE = MultipartPartsCleanupFilter.class.getName() + ".deleteParts";

    // -------------------- LOGIC --------------------

    /**
     * Marks the current JSF request so that the temporary files of its parts
     * are deleted after it has been processed, also when processing fails.
     * To be called by an upload listener that does not keep the uploaded file
     * beyond the current request.
     */
    public static void deletePartsAfterRequest() {
        ((ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
                .setAttribute(DELETE_PARTS_ATTRIBUTE, Boolean.TRUE);
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    /**
     * Passes the request on and then, if the request was marked, deletes the
     * temporary files of all its parts, also when processing the request
     * failed. A part that cannot be deleted is logged and skipped.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (Boolean.TRUE.equals(request.getAttribute(DELETE_PARTS_ATTRIBUTE))) {
                deleteParts((HttpServletRequest) request);
            }
        }
    }

    @Override
    public void destroy() { }

    // -------------------- PRIVATE --------------------

    private void deleteParts(HttpServletRequest request) {
        try {
            for (Part part : request.getParts()) {
                deletePart(part);
            }
        } catch (IOException | ServletException | IllegalStateException ex) {
            log.warn("Could not read the parts of a multipart request to delete them", ex);
        }
    }

    private void deletePart(Part part) {
        try {
            part.delete();
        } catch (IOException ex) {
            log.warn("Could not delete the temporary file of multipart part {}", part.getName(), ex);
        }
    }
}
