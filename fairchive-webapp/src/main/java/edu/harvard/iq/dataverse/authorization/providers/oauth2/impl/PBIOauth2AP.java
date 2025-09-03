package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;

import com.github.scribejava.apis.KeycloakApi;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.oauth.OAuth20Service;

import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;

public class PBIOauth2AP extends AbstractOAuth2AuthenticationProvider {
    
    private static final Logger logger = getLogger(Admin.class);
    private final BaseApi<OAuth20Service> api = KeycloakApi.instance("https://keycloak-dev.psnc.pl", "pbi-dev");
    

    public PBIOauth2AP(final String aClientId, final String aClientSecret) {
        this.id = "pbi";
        this.title = getStringFromBundle("auth.providers.title.pbi");
        this.clientId = aClientId;
        this.clientSecret = aClientSecret;
        this.scope = "openid";
        this.baseUserEndpoint = "https://keycloak-dev.psnc.pl/realms/pbi-dev/protocol/openid-connect/auth";
    }

    @Override
    public BaseApi<OAuth20Service> getApiInstance() {
        return this.api;
    }

    @Override
    protected ParsedUserResponse parseUserResponse(final String responseBody) {
        logger.info("!response body: " + responseBody);
        
        try (final StringReader rdr = new StringReader(responseBody);
            final JsonReader jrdr = Json.createReader(rdr)) {
            final JsonObject response = jrdr.readObject();

            final AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    response.getString("given_name", ""),
                    response.getString("family_name", ""),
                    response.getString("email", ""),
                    "",
                    ""
            );
            final String persistentUserId = response.getString("id");
            String username = response.getString("email");
            if (username != null) {
                username = username.split("@")[0].trim();
            } else {
                // compose a username from given and family names
                username = response.getString("given_name", "") + "."
                        + response.getString("family_name", "");
                username = username.trim();
                if (username.isEmpty()) {
                    username = UUID.randomUUID().toString();
                } else {
                    username = username.replaceAll(" ", "-");
                }
            }
            return new ParsedUserResponse(displayInfo, persistentUserId, username);
        }
    }

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }
}
