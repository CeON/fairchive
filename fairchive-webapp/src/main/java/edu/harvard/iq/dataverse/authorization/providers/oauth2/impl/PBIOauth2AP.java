package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.java8.Base64;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;
import edu.harvard.iq.dataverse.util.StringUtil;

public class PBIOauth2AP extends AbstractOAuth2AuthenticationProvider {

    private static final String AUTH_URL = "https://keycloak-dev.psnc.pl/realms/pbi-dev/protocol/openid-connect/auth";
    private static final String ACCESS_TOKEN_URL = "https://keycloak-dev.psnc.pl/realms/pbi-dev/protocol/openid-connect/token";
    private final BaseApi<OAuth20Service> api = new PBIApi();

    public PBIOauth2AP(final String aClientId, final String aClientSecret) {
        this.id = "pbi";
        this.title = getStringFromBundle("auth.providers.title.pbi");
        this.clientId = aClientId;
        this.clientSecret = aClientSecret;
        this.scope = "openid";
        this.baseUserEndpoint = AUTH_URL;
    }

    @Override
    public BaseApi<OAuth20Service> getApiInstance() {
        return this.api;
    }

    @Override
    public String createAuthorizationUrl(final String state,
            final String redirectUrl) {
        final String challenge = getChallengeFrom(state);
        final PKCE pkce = new PKCE();
        pkce.setCodeChallenge(challenge);
        pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.S256);
        return getService(state, redirectUrl).getAuthorizationUrl(pkce);
    }

    @Override
    public ExternalIdpUserRecord getUserRecord(final String code,
            final String state, final String redirectUrl)
            throws IOException, OAuth2Exception {
        try {
            final OAuth20Service service = getService(state, redirectUrl);
            final String verifier = getVerifierFrom(state);
            final OAuth2AccessToken token = service.getAccessToken(code, verifier);
            final JsonObject json = extractPayloadFrom(token.getAccessToken());
            final String userName = json.get("preferred_username").getAsString();
            return new ExternalIdpUserRecord(getId(), userName,
                    userName,
                    OAuth2TokenData.from(token),
                    new AuthenticatedUserDisplayInfo(
                            json.get("given_name").getAsString(),
                            json.get("family_name").getAsString(),
                            json.get("email").getAsString(), "", ""),
                    singletonList(json.get("email").getAsString()));

        } catch (final ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String createState(final Optional<String> redirectPage) {
        try {
            final String verifier = randomAlphanumeric(50);
            final String challenge = PKCECodeChallengeMethod.S256
                    .transform2CodeChallenge(verifier);
            final String encrytpted = StringUtil.encrypt(verifier + "~" + challenge,
                    getClientSecret());
            return getId() + "~" + encrytpted;
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String getChallengeFrom(final String state) {
        final String enctypted = state.substring(getId().length() + 1);
        return StringUtil.decrypt(enctypted, getClientSecret()).split("~")[1];
    }

    private String getVerifierFrom(final String state) {
        final String enctypted = state.substring(getId().length() + 1);
        return StringUtil.decrypt(enctypted, getClientSecret()).split("~")[0];
    }

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        throw new RuntimeException("Not implemented.");
    }

    private static JsonObject extractPayloadFrom(final String jwt) 
        throws IOException{
        final int firstDotIndex = jwt.indexOf('.');
        if(firstDotIndex == -1) {
            throw new IOException("Invalid JWT.");
        }
        final int secondDotIndex = jwt.indexOf('.', firstDotIndex + 1);
        final String payload = secondDotIndex > -1
                ? jwt.substring(firstDotIndex + 1, secondDotIndex)
                : jwt.substring(secondDotIndex + 1);
        final byte decoded[] = Base64.getDecoder().decode(payload);
        return new JsonParser()
                .parse(new InputStreamReader(new ByteArrayInputStream(decoded),
                        defaultCharset()))
                .getAsJsonObject();
    }

    private static class PBIApi extends DefaultApi20 {

        @Override
        public String getAccessTokenEndpoint() {
            return ACCESS_TOKEN_URL;
        }

        @Override
        protected String getAuthorizationBaseUrl() {
            return AUTH_URL;
        }

        @Override
        public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
            return OpenIdJsonTokenExtractor.instance();
        }
    }
}
