package edu.harvard.iq.dataverse.globalid;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiDataCiteRestApiUrl;
import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.slf4j.LoggerFactory.getLogger;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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

	/** Cursor of the first page of a search. */
	public static final String FIRST_CURSOR = "1";

	/** The largest page DataCite returns. */
	public static final int MAX_PAGE_SIZE = 1000;

	static final String PAGE_CURSOR_PARAM = "page[cursor]";
	static final String PAGE_SIZE_PARAM = "page[size]";

	private SettingsServiceBean settings;
	private final ObjectMapper mapper = new ObjectMapper();
	private final ObjectMapper pageMapper = new ObjectMapper();

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
		this.pageMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

	/**
	 * Makes a GET request to the DataCite search for DOIs and returns one page
	 * of the records it matches. Pages are read with cursors: the first page
	 * with {@link #FIRST_CURSOR}, every next one with the cursor
	 * {@link #nextCursor(DataCiteDoisPage)} reads from the previous page.
	 * Facets are not requested.
	 *
	 * @param queryParams search parameters sent as they are, e.g.
	 *        {@code affiliation-country=PL} or
	 *        {@code query=creators.affiliation.name:"AGH University of Krakow"};
	 *        must not contain the paging parameters
	 * @param cursor  cursor of the page to read
	 * @param pageSize number of records per page, at most
	 *        {@link #MAX_PAGE_SIZE}
	 * @return the page; its list of records is empty past the last match
	 * @throws IOException in case of a communication error, or when DataCite
	 *         answers with a status other than 200 &ndash; the exception then
	 *         carries DataCite's error message (for example a query syntax
	 *         error)
	 * @see <a href="https://support.datacite.org/reference/get_dois">GET
	 *      /dois</a>
	 */
	public DataCiteDoisPage searchDois(final Map<String, String> queryParams,
			final String cursor, final int pageSize) throws IOException {

		final URI uri = createSearchUri(queryParams, cursor, pageSize);

		try (final CloseableHttpClient client = createDefault();
				final CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
			return handleSearchResponse(uri, response);
		}
	}

	/**
	 * Reads the cursor of the next page from the {@code links.next} URL of a
	 * search page.
	 *
	 * @return the cursor, or empty on the last page
	 */
	public static Optional<String> nextCursor(final DataCiteDoisPage page) {

		return Optional.ofNullable(page.getLinks().getNext())
				.flatMap(next -> URLEncodedUtils.parse(URI.create(next), UTF_8).stream()
						.filter(param -> PAGE_CURSOR_PARAM.equals(param.getName()))
						.map(NameValuePair::getValue)
						.findFirst());
	}

	private URI createSearchUri(final Map<String, String> queryParams,
			final String cursor, final int pageSize) {

		try {
			final URIBuilder builder = new URIBuilder(
					this.settings.getValueForKey(DoiDataCiteRestApiUrl) + "/dois");
			queryParams.forEach(builder::addParameter);
			return builder.addParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize))
					.addParameter(PAGE_CURSOR_PARAM, cursor)
					.addParameter("disable-facets", "true")
					.build();
		} catch (final URISyntaxException e) {
			throw new IllegalStateException("Invalid DataCite REST API URL", e);
		}
	}

	private DataCiteDoisPage handleSearchResponse(final URI uri, final HttpResponse response)
			throws IOException {

		final StatusLine status = response.getStatusLine();
		final HttpEntity entity = response.getEntity();

		if (status.getStatusCode() != 200) {
			final String body = entity == null ? "" : EntityUtils.toString(entity, UTF_8);
			logger.warn("Non 200 response code on search dois - url: {}, statusCode: {}, body: {}",
					uri, status.getStatusCode(), body);
			throw new HttpResponseException(status.getStatusCode(),
					status.getReasonPhrase() + " " + body);
		}
		if (entity == null) {
			throw new ClientProtocolException("Response contains no content");
		}

		try (final InputStream in = entity.getContent()) {
			return this.pageMapper.readValue(in, DataCiteDoisPage.class);
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
