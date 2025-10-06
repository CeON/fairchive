package edu.harvard.iq.dataverse.api.ror;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO;
import edu.harvard.iq.dataverse.api.dto.RorDataResponse;
import edu.harvard.iq.dataverse.ror.RorDataService;
import edu.harvard.iq.dataverse.search.ror.RorIndexingService;
import edu.harvard.iq.dataverse.util.FileUtil;

@Stateless
@Path("ror")
public class RorDataApi extends AbstractApiBean {

    private static final Logger logger = getLogger(RorDataApi.class);
    private RorDataService rorDataService;
    private RorIndexingService rorIndexingService;

    // -------------------- CONSTRUCTORS --------------------

    public RorDataApi() {
    }

    @Inject
    public RorDataApi(final RorDataService rorDataService,
            final RorIndexingService rorIndexingService) {
        this.rorDataService = rorDataService;
        this.rorIndexingService = rorIndexingService;
    }

    // -------------------- LOGIC --------------------

    @POST
    @Path("/upload")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public Response uploadRorData(
            @FormDataParam("file") final InputStream inputStream,
            @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader) {

        try {
            findSuperuserOrDie();
            final File file = FileUtil.inputStreamToFile(inputStream, 8192);
            try {
                final RorDataService.UpdateResult result = this.rorDataService
                        .refreshRorData(file, contentDispositionHeader);
                this.rorIndexingService.indexRorRecordsAsync(result.getSavedRorData());
                return Response
                        .ok(new RorDataResponse(result.getTotal(),
                                result.getStats()))
                        .build();
            } finally {
                file.delete();
            }
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        } catch (IOException ioe) {
            logger.warn("Exception during file upload: ", ioe);
            return Response.status(BAD_REQUEST)
                    .entity(ApiErrorResponseDTO.errorResponse(
                            BAD_REQUEST.getStatusCode(),
                            "There was an IO error with file being uploaded"))
                    .build();
        }
    }
}
