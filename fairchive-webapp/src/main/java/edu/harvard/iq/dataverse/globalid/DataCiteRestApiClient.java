package edu.harvard.iq.dataverse.globalid;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiDataCiteRestApiUrl;
import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * Http client for communicating with datacite via rest api
 * 
 * @author madryk
 * @see https://support.datacite.org/docs/api
 */
@ApplicationScoped
public class DataCiteRestApiClient {
	private static final Logger logger = getLogger(DataCiteRestApiClient.class);

	private SettingsServiceBean settings;
	private final ObjectMapper mapper = new ObjectMapper();

	// -------------------- CONSTRUCTORS --------------------

	DataCiteRestApiClient() {
		// JEE requirement
	}

	@Inject
	public DataCiteRestApiClient(final SettingsServiceBean settings) {
		
		this.settings = settings;
	}

	// -------------------- LOGIC --------------------

	@PostConstruct
	public void postConstruct() {
		
		this.mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Makes an GET request to DataCite api which obtains information about provided
	 * doi.
	 * 
	 * @param doiAuthority  - authority part of the doi
	 * @param doiIdentifier - identifier part of the doi
	 * @return response from DataCite parsed into an object
	 * @throws IOException in case of in communication with DataCite
	 * @see https://support.datacite.org/docs/api-get-doi - documentation of invoked
	 *      api endpoint
	 * @see https://api.test.datacite.org/dois/10.5438/0012 - example response
	 */
	public DataCiteFindDoiResponse findDoi(final String doiAuthority, 
			final String doiIdentifier) throws IOException {

		final URI uri = createUri(doiAuthority, doiIdentifier);

		try (final CloseableHttpClient client = createDefault();
				final CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
			return handleResponse(response);

		} catch (final HttpResponseException e) {
			logger.warn("Non 200 response code on find doi - url: {}, statusCode: {}, reason: {}", uri.toString(),
					e.getStatusCode(), e.getReasonPhrase());
			throw e;
		}
	}

	private URI createUri(final String doiAuthority, final String doiIdentifier) {

		return URI.create(this.settings.getValueForKey(DoiDataCiteRestApiUrl) + 
				"/dois/" + doiAuthority + '/' + doiIdentifier);
	}

	private DataCiteFindDoiResponse handleResponse(final HttpResponse response) 
			throws IOException {

		final StatusLine status = response.getStatusLine();

		if (status.getStatusCode() != 200) {
			throw new HttpResponseException(status.getStatusCode(), status.getReasonPhrase());
		}

		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			throw new ClientProtocolException("Response contains no content");
		}

		try (final InputStream in = entity.getContent()) {
			return this.mapper.readValue(in, DataCiteFindDoiResponse.class);
		}
	}

}
