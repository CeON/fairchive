package edu.harvard.iq.dataverse.api.geoname;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.InputStream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.persistence.geonames.GeoNameRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

@Stateless
@Path("geoname")
public class GeoNameApi extends AbstractApiBean {

    private GeoNameRepository geoNameRepo;

    public GeoNameApi() {
    }

    @Inject
    public GeoNameApi(final GeoNameRepository geoNameRepo) {
        this.geoNameRepo = geoNameRepo;
    }

    /**
     * The following code assumes that geonames are being imported in chunks. 
     * Importing the whole allCountries.txt may cause OutOfMemoryError
     */
    @POST
    @Path("/upload")
    @Consumes(TEXT_PLAIN)
    @Produces(APPLICATION_JSON)
    public Response upload(final InputStream in) throws Exception {
        try {
            final AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (user.isSuperuser()) {
                this.geoNameRepo.importNames(in);
                return ok("Imported");
            } else {
                return error(FORBIDDEN,  "This API call can be used by superusers only");
            }
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
    }
}
