package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.persistence.user.AuthenticationProviderRow;

/**
 * Creates OAuth2 authentication providers based on rows from the database.
 *
 * @author michael
 */
public class OAuth2AuthenticationProviderFactory implements AuthenticationProviderFactory {

    private final Map<String, Function<Map<String, String>, AbstractOAuth2AuthenticationProvider>> builders = new HashMap<>();

    public OAuth2AuthenticationProviderFactory() {
        this.builders.put("github", data -> new GitHubOAuth2AP(data.get("clientId"),
                data.get("clientSecret")));
        this.builders.put("google", data -> new GoogleOAuth2AP(data.get("clientId"),
                data.get("clientSecret")));
        this.builders.put("orcid", data -> new OrcidOAuth2AP(data.get("clientId"),
                data.get("clientSecret"), data.get("userEndpoint")));
    }

    @Override
    public String getAlias() {
        return "oauth2";
    }
    
    @Override
    public String getInfo() {
        return "Factory for OAuth2 identity providers.";
    }

    @Override
    public AuthenticationProvider buildProvider(final AuthenticationProviderRow row)
            throws AuthorizationSetupException {
        final Map<String, String> factoryData = getFactoryDataOf(row);
        final AbstractOAuth2AuthenticationProvider result = buildForm(factoryData);
        result.setId(row.getId());
        result.setTitle(row.getTitle());
        result.setSubTitle(row.getSubtitle());
        return result;
    }

    private Map<String, String> getFactoryDataOf(final AuthenticationProviderRow row)
            throws AuthorizationSetupException {
        final Map<String, String> factoryData = parseFactoryData(row.getFactoryData());
        final String type = factoryData.get("type");
        if (type == null) {
            throw new AuthorizationSetupException(
                    "Authentication provider row with id " + row.getId()
                            + " describes an OAuth2 provider but does not provide a type. Available types are "
                            + builders.keySet());
        }
        if (!this.builders.containsKey(type)) {
            throw new AuthorizationSetupException(
                    "Authentication provider row with id " + row.getId()
                            + " describes an OAuth2 provider of type " + type
                            + ". This type is not supported."
                            + " Available types are " + builders.keySet());
        }
        return factoryData;
    }
    
    private AbstractOAuth2AuthenticationProvider buildForm(final Map<String, String> factoryData) {
        return this.builders.get(factoryData.get("type")).apply(factoryData);
    }
    

    /**
     * Expected map format.: {@code name: value|name: value|...}
     */
    public static Map<String, String> parseFactoryData(final String factoryData) {
        return stream(factoryData.split("\\|"))
                .map(s -> s.split(":", 2))
                .filter(p -> p.length == 2)
                .collect(toMap(kv -> kv[0].trim(), kv -> kv[1].trim()));
    }
}
