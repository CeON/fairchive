package edu.harvard.iq.dataverse.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.InputStream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.geonames.GeoNameIndexingService;

@Stateless
@Path("geonames")
public class GeoNameApi extends AbstractApiBean {

    private GeoNameIndexingService indexingService;

    public GeoNameApi() {
    }

    @Inject
    public GeoNameApi(final GeoNameIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @POST
    @Path("/")
    @Consumes(TEXT_PLAIN)
    @Produces(APPLICATION_JSON)
    public Response upload(final InputStream in) throws Exception {
        try {
            findSuperuserOrDie();
            this.indexingService.importNames(in);
            return ok("Imported");
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
    }
    
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
}

