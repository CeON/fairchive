package edu.harvard.iq.dataverse.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.InputStream;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import edu.harvard.iq.dataverse.search.periodo.PeriodoIndexingService;

@Stateless
@Path("periodo")
public class PeriodoApi extends AbstractApiBean {

    private PeriodoIndexingService indexingService;

    public PeriodoApi() {
    }

    @Inject
    public PeriodoApi(final PeriodoIndexingService indexingService) {
        this.indexingService = indexingService;
    }
    
    /**
     * Invoke with
     * curl --insecure -X POST -F "file=@./periodo-dataset.json;type=text/json" \
     *      -F "file=@./translations.tsv;type=text/csv" \
     *      -H "X-Dataverse-key: {key}" https://localhost:8181/api/v1/periodo
     */

    @POST
    @Path("/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public Response upload(@FormDataParam("file") List<FormDataBodyPart> files)
            throws Exception {
        try {
            findSuperuserOrDie();

            if (files.size() == 1) {
                try (final InputStream json = getPeriodoJson(files)) {
                    this.indexingService.importNames(json);
                }
            } else if (files.size() == 2) {
                try (final InputStream json = getPeriodoJson(files)) {
                    try (final InputStream tsv = getPeriodoTranslation(files)) {
                        this.indexingService.importNames(json, tsv);
                    }
                }
            }
            return ok("Imported");
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        } catch(final Exception e) {
            return status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
    
    /**
     * Invoke with
     * curl --insecure -X DELETE -H "X-Dataverse-key: {key}" \ 
     *      https://localhost:8181/api/v1/periodo
     */

    @DELETE
    @Path("/")
    @Produces(APPLICATION_JSON)
    public Response clear() throws Exception {
        try {
            findSuperuserOrDie();
            this.indexingService.clear();
            return ok("Cleared");
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
    }

    private static InputStream getPeriodoJson(List<FormDataBodyPart> files)
            throws Exception {
        return get(files, ".json", "Periodo JSON content not found.");
    }

    private static InputStream getPeriodoTranslation(List<FormDataBodyPart> files)
            throws Exception {
        return get(files, ".tsv", "Periodo translation TSV content not found.");
    }

    private static InputStream get(List<FormDataBodyPart> files,
            final String extention,
            final String errMessage)
            throws Exception {

        return files.stream()
                .filter(part -> part.getContentDisposition().getFileName()
                        .endsWith(extention))
                .map(part -> part.getEntityAs(InputStream.class))
                .findAny()
                .orElseThrow(() -> new Exception(errMessage));
    }
}
