package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MachineTranslationURL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * Uses libretranslate running as external service to translate provided texts.
 * A failure to translate is signalled by an {@link UncheckedIOException}.
 */
@ApplicationScoped
public class Translator implements AutoCloseable {
	
	private final static Logger log = getLogger(Translator.class);
	private final static int timeout = 30000; // 30 seconds
	
	private SettingsServiceBean settings;
	
	private final CloseableHttpClient client;
	
	public Translator() {
		this(null, createClient());
	}
	
	@Inject
	public Translator(final SettingsServiceBean settings) {
		this(settings, createClient());
	}
	
	Translator(final SettingsServiceBean settings, final CloseableHttpClient client) {
		this.settings = settings;
		this.client = client;
	}
	
	@PreDestroy
	@Override
	public void close() {
		try {
			this.client.close();
		} catch (final IOException e) {
			log.warn("error closing http client. ", e);
		}
	}
	
	public boolean isEnabled() {
		return ! getUrl().isEmpty();
	}

	/**
	 * Translates the texts, returning the translations in the same order.
	 *
	 * @param format how the service should read the texts: as plain text or as HTML
	 * @throws UncheckedIOException if the service could not be reached or did not
	 *                              answer with translations
	 */
	public List<String> translate(final List<String> texts, final String targetLang, final Format format) {
		if (texts.isEmpty()) {
			return texts;
		}
		try (final CloseableHttpResponse response = this.client.execute(createRequest(texts, targetLang, format))) {
			final int status = response.getStatusLine().getStatusCode();
			final HttpEntity entity = response.getEntity();
			if (status != 200) {
				throw new HttpResponseException(status, "Received status code " + status + " from " + getUrl()
						+ (entity != null ? ": " + IOUtils.toString(entity.getContent(), UTF_8) : ""));
			}
			if (entity == null) {
				throw new IOException("Received empty response from " + getUrl());
			}
			return translatedTextFrom(entity);
		} catch (final IOException e) {
			throw new UncheckedIOException("Error while translating with libretranslate service", e);
		}
	}
	
	private HttpPost createRequest(final List<String> texts, 
			final String targetLang, final Format format) {
		
		final HttpPost request = new HttpPost(getUrl());
		
		final JsonObject json = new JsonObject();
		final JsonArray sourceTexts = new JsonArray();

		for (final String text : texts) {
		    sourceTexts.add(new JsonPrimitive(text != null ? text : ""));
		}

		json.add("q", sourceTexts);
		json.addProperty("source", "auto");
		json.addProperty("target", targetLang);
		json.addProperty("format", format.code);
        
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
		request.setEntity(new StringEntity(json.toString(), UTF_8));
        
        return request;
	}
	
	private static List<String> translatedTextFrom(final HttpEntity entity) 
			throws IOException {
		try (final InputStreamReader in = new InputStreamReader(entity.getContent(), UTF_8)) {
			final JsonArray response = new JsonParser().parse(in).getAsJsonObject().
					get("translatedText").getAsJsonArray();
			final ArrayList<String> result = new ArrayList<>(response.size());
			response.forEach(item -> result.add(item.getAsString()));
			return result;
		}
	}
	
	private String getUrl() {
		return this.settings.getValueForKey(MachineTranslationURL);
	}
	
	private static CloseableHttpClient createClient() {
		return HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectTimeout(timeout)
						.setSocketTimeout(timeout)
						.setConnectionRequestTimeout(timeout)
						.build())
				.build();
	}
	
	/**
	 * How the translation service reads a text: plain text is translated as it
	 * is, HTML has its markup kept and only its text translated.
	 */
	public enum Format {
		TEXT("text"), HTML("html");
		
		private final String code;
		
		Format(final String code) {
			this.code = code;
		}
	}
}
