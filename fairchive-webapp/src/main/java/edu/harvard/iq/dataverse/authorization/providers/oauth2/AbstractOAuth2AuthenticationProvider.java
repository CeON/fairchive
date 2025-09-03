package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import static com.github.scribejava.core.model.Verb.GET;
import static java.util.Collections.emptyList;
import static java.util.Objects.hash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;

import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.EditableAccountField;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;

/**
 * Base class for OAuth2 identity providers, such as GitHub and ORCiD.
 *
 * @author michael
 */
public abstract class AbstractOAuth2AuthenticationProvider implements OAuth2AuthenticationProvider {

    protected static class ParsedUserResponse {
        public final AuthenticatedUserDisplayInfo displayInfo;
        public final String userIdInProvider;
        public final String username;
        public final List<String> emails = new ArrayList<>();

        public ParsedUserResponse(final AuthenticatedUserDisplayInfo aDisplayInfo,
                final String aUserIdInProvider, final String aUsername,
                final List<String> emails) {
            this.displayInfo = aDisplayInfo;
            this.userIdInProvider = aUserIdInProvider;
            this.username = aUsername;
            this.emails.addAll(emails);
        }

        public ParsedUserResponse(final AuthenticatedUserDisplayInfo displayInfo, 
                final String userIdInProvider, final String username) {
            this(displayInfo, userIdInProvider, username, emptyList());
        }

        @Override
        public int hashCode() {
            return hash(this.userIdInProvider, this.username);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ParsedUserResponse other = (ParsedUserResponse) obj;
            if (!Objects.equals(this.userIdInProvider, other.userIdInProvider)) {
                return false;
            }
            if (!Objects.equals(this.username, other.username)) {
                return false;
            }
            if (!Objects.equals(this.displayInfo, other.displayInfo)) {
                return false;
            }
            return Objects.equals(this.emails, other.emails);
        }

        @Override
        public String toString() {
            return "ParsedUserResponse{" + "displayInfo=" + displayInfo + ", userIdInProvider=" + userIdInProvider + ", username=" + username + ", emails=" + emails + '}';
        }
    }

    protected String id;
    protected String title;
    protected String subTitle;
    protected String clientId;
    protected String clientSecret;
    protected String baseUserEndpoint;
    protected String redirectUrl;
    protected String scope;

    public abstract BaseApi<OAuth20Service> getApiInstance();

    protected abstract ParsedUserResponse parseUserResponse(String responseBody);

    protected OAuth20Service getService(final String state, final String redirectUrl) {
        final ServiceBuilder builder = new ServiceBuilder(getClientId())
                .apiSecret(getClientSecret())
                .state(state)
                .callback(redirectUrl);
        if (this.scope != null) {
            builder.scope(this.scope);
        }
        return builder.build(getApiInstance());
    }

    @Override
    public String createAuthorizationUrl(final String state, final String redirectUrl) {
        return getService(state, redirectUrl).getAuthorizationUrl();
    }

    @Override
    public ExternalIdpUserRecord getUserRecord(final String code,
            final String state, final String redirectUrl)
            throws IOException, OAuth2Exception {
        try {
            final OAuth20Service service = getService(state, redirectUrl);
            final OAuth2AccessToken accessToken = service.getAccessToken(code);
            final String userEndpoint = getUserEndpoint(accessToken);

            final OAuthRequest request = new OAuthRequest(GET, userEndpoint);
            service.signRequest(accessToken, request);
            request.setCharset("UTF-8");

            final Response response = service.execute(request);
            final int responseCode = response.getCode();
            final String body = response.getBody();

            if (responseCode == 200) {
                final ParsedUserResponse parsed = parseUserResponse(body);
                return new ExternalIdpUserRecord(getId(), parsed.userIdInProvider,
                        parsed.username,
                        OAuth2TokenData.from(accessToken),
                        parsed.displayInfo,
                        parsed.emails);
            } else {
                throw new OAuth2Exception(responseCode, body,
                        "Error getting the user info record.");
            }
        } catch (final ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isUserInfoUpdateAllowed() {
        return true;
    }

    @Override
    public Set<EditableAccountField> getEditableFields() {
        return EditableAccountField.secondary();
    }

    @Override
    public void updateUserInfo(String userIdInProvider, AuthenticatedUserDisplayInfo updatedUserData) {
        // ignore - no account info is stored locally.
        // We override this to prevent the UnsupportedOperationException thrown by
        // the default implementation.
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), getTitle(), getSubTitle());
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public String getClientId() {
        return this.clientId;
    }

    @Override
    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getUserEndpoint(final OAuth2AccessToken token) {
        return this.baseUserEndpoint;
    }

    public String getRedirectUrl() {
        return this.redirectUrl;
    }

    public Optional<String> getIconHtml() {
        return Optional.empty();
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setSubTitle(final String subtitle) {
        this.subTitle = subtitle;
    }

    public String getSubTitle() {
        return this.subTitle;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.clientId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractOAuth2AuthenticationProvider)) {
            return false;
        }
        final AbstractOAuth2AuthenticationProvider other = (AbstractOAuth2AuthenticationProvider) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        return Objects.equals(this.clientSecret, other.clientSecret);
    }

    @Override
    public boolean isOAuthProvider() {
        return true;
    }

}
