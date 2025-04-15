package edu.harvard.iq.dataverse.api.geoname;

import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.persistence.geonames.GeoName;
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

    @POST
    @Path("/upload")
    @Consumes(TEXT_PLAIN)
    @Produces(APPLICATION_JSON)
    public Response upload(final InputStream in) throws Exception {
        try {
            final AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (user.isSuperuser()) {
                this.geoNameRepo.deleteAll();
                new BufferedReader(new InputStreamReader(in, "utf8"))
                        .lines()
                        .map(line -> line.split("\t"))
                        .map(cells -> {
                            final GeoName result = new GeoName();
                            result.setId(parseInt(cells[0].trim()));
                            result.setName(cells[1].trim());
                            result.setAlternateNames(cells[3].trim());
                            result.setFeatureCode(cells[7].trim());
                            result.setCountryCode(cells[8].trim());
                            result.setAdmin1Code(cells[10].trim());
                            result.setAdmin2Code(cells[11].trim());
                            result.setAdmin3Code(cells[12].trim());
                            result.setAdmin4Code(cells[13].trim());
                            return result;
                        })
                        .forEach(this::save);
                return ok("Imported");
            } else {
                return error(FORBIDDEN,  "This API call can be used by superusers only");
            }
        } catch (final AbstractApiBean.WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
    }

    @Transactional
    private void save(final GeoName gn) {
        this.geoNameRepo.save(gn);
    }
}
