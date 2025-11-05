package edu.harvard.iq.dataverse.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.io.InputStream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrServerException;
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
     * curl --insecure -X POST -F "dataset=@./periodo-dataset.json;type=text/json" \
     *      -F "translations=@./translations.tsv;type=text/csv" \
     *      -H "X-Dataverse-key: {API key}" https://localhost:8181/api/v1/periodo
     */

    @POST
    @Path("/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public Response upload(@FormDataParam("dataset") FormDataBodyPart dataset,
            @FormDataParam("translations") FormDataBodyPart translations)
            throws Exception {
        try {
            findSuperuserOrDie();

            if (dataset != null) {
                try (final InputStream json = inputStreamFrom(dataset)) {
                    if (translations != null) {
                        try (final InputStream tsv = inputStreamFrom(translations)) {
                            this.indexingService.importNames(json, tsv);
                            return ok("Imported");
                        }
                    } else {
                        this.indexingService.importNames(json);
                        return ok("Imported");
                    }
                }
            } else {
                return status(BAD_REQUEST).entity("Periodo JSON content not found.")
                        .build();
            }
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        } catch(final SolrServerException e) {
            return status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (final Exception e) {
            return status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
    
    /**
     * Invoke with
     * curl --insecure -X DELETE -H "X-Dataverse-key: {API key}" \ 
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
    
    private static InputStream inputStreamFrom(final FormDataBodyPart file) {
        return file.getEntityAs(InputStream.class);
    }
}
