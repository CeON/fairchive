package edu.harvard.iq.dataverse.datafile.file;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import java.io.ByteArrayInputStream;
import java.util.Locale;

import static edu.harvard.iq.dataverse.MultipartPartsCleanupFilter.DELETE_PARTS_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplaceDatafilesPageTest {

    @Mock
    private ReplaceFileHandler replaceFileHandler;

    @Mock
    private FileUploadEvent event;

    @Mock
    private UploadedFile uploadedFile;

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
    void handleFileUpload__marks_request_for_deleting_its_parts() throws Exception {
        // given
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequest()).thenReturn(currentRequest);
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        when(event.getFile()).thenReturn(uploadedFile);
        when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(replaceFileHandler.createDataFile(any(), any(), any(), any())).thenThrow(new IllegalStateException("cannot store the file"));
        ReplaceDatafilesPage page = new ReplaceDatafilesPage(replaceFileHandler, null, null, null, null, null, null);

        // when
        page.handleFileUpload(event);

        // then
        verify(currentRequest).setAttribute(DELETE_PARTS_ATTRIBUTE, Boolean.TRUE);
    }
}
