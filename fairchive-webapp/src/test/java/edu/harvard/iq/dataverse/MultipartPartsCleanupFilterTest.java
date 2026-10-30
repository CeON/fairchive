package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.context.FacesContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

import static edu.harvard.iq.dataverse.MultipartPartsCleanupFilter.DELETE_PARTS_ATTRIBUTE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultipartPartsCleanupFilterTest {

    private final MultipartPartsCleanupFilter filter = new MultipartPartsCleanupFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private Part filePart;

    @Mock
    private Part fieldPart;

    @Mock
    private ServletRequest currentRequest;

    @AfterEach
    void tearDown() {
        if (FacesContext.getCurrentInstance() != null) {
            FacesContext.getCurrentInstance().release();
        }
    }

    // -------------------- TESTS --------------------

    @Test
    void deletePartsAfterRequest__marks_current_request() {
        // given
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequest()).thenReturn(currentRequest);

        // when
        MultipartPartsCleanupFilter.deletePartsAfterRequest();

        // then
        verify(currentRequest).setAttribute(DELETE_PARTS_ATTRIBUTE, Boolean.TRUE);
    }

    @Test
    void doFilter__marked_request_deletes_parts_after_processing() throws Exception {
        // given
        when(request.getAttribute(DELETE_PARTS_ATTRIBUTE)).thenReturn(Boolean.TRUE);
        when(request.getParts()).thenReturn(asList(filePart, fieldPart));

        // when
        filter.doFilter(request, response, chain);

        // then
        InOrder order = inOrder(chain, filePart, fieldPart);
        order.verify(chain).doFilter(request, response);
        order.verify(filePart).delete();
        order.verify(fieldPart).delete();
    }

    @Test
    void doFilter__unmarked_request_keeps_parts() throws Exception {
        // given
        when(request.getAttribute(DELETE_PARTS_ATTRIBUTE)).thenReturn(null);
        lenient().when(request.getParts()).thenReturn(asList(filePart, fieldPart));

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(chain).doFilter(request, response);
        verify(filePart, never()).delete();
        verify(fieldPart, never()).delete();
    }

    @Test
    void doFilter__processing_failed_still_deletes_parts() throws Exception {
        // given
        when(request.getAttribute(DELETE_PARTS_ATTRIBUTE)).thenReturn(Boolean.TRUE);
        when(request.getParts()).thenReturn(asList(filePart, fieldPart));
        doThrow(new ServletException("processing failed")).when(chain).doFilter(request, response);

        // when
        Throwable thrown = catchThrowable(() -> filter.doFilter(request, response, chain));

        // then
        assertThat(thrown).isInstanceOf(ServletException.class).hasMessage("processing failed");
        verify(filePart).delete();
        verify(fieldPart).delete();
    }

    @Test
    void doFilter__first_part_cannot_be_deleted_rest_still_deleted() throws Exception {
        // given
        when(request.getAttribute(DELETE_PARTS_ATTRIBUTE)).thenReturn(Boolean.TRUE);
        when(request.getParts()).thenReturn(asList(filePart, fieldPart));
        doThrow(new IOException("disk error")).when(filePart).delete();

        // when
        Throwable thrown = catchThrowable(() -> filter.doFilter(request, response, chain));

        // then
        assertThat(thrown).isNull();
        InOrder order = inOrder(filePart, fieldPart);
        order.verify(filePart).delete();
        order.verify(fieldPart).delete();
    }

    @Test
    void doFilter__parts_cannot_be_read_no_exception() throws Exception {
        // given
        when(request.getAttribute(DELETE_PARTS_ATTRIBUTE)).thenReturn(Boolean.TRUE);
        when(request.getParts()).thenThrow(new IllegalStateException("request too large"));

        // when
        Throwable thrown = catchThrowable(() -> filter.doFilter(request, response, chain));

        // then
        assertThat(thrown).isNull();
    }
}
