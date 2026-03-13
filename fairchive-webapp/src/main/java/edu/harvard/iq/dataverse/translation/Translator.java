package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MachineTranslationURL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
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
 * Uses libretranslate running as external service to translate provided texts
 */
@ApplicationScoped
public class Translator implements AutoCloseable {
	
	private final static Logger log = getLogger(Translator.class);
	private final static int timeout = 30000; // 30 seconds
	
	private SettingsServiceBean settings;
	
	private final CloseableHttpClient client = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .build())
            .build();
	
	public Translator() {}
	
	@Inject
	public Translator(final SettingsServiceBean settings) {
		this.settings =  settings;
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

	public List<String> translate(final List<String> texts, final String targetLang) {
		if (! texts.isEmpty()) {
			try {
				final HttpPost post = createRequest(texts, targetLang);
				try (final CloseableHttpResponse response = client.execute(post)) {
					final int status = response.getStatusLine().getStatusCode();
					if (status == 200) {
						final HttpEntity entity = response.getEntity();
						if (entity != null) {
							return translatedTextFrom(entity);
						} else {
							log.warn("Received empty response from libretranslate service");
							return texts;
						}
					} else {
						log.warn("Received status code " + status + " from " + getUrl());
						log.warn(IOUtils.toString(response.getEntity().getContent(), UTF_8));
						return texts;
					}
				}
			} catch (final IOException e) {
				log.warn("Error while contacting libretranslate service.", e);
				return texts;
			}
		} else {
			return texts;
		}
	}
	
	private HttpPost createRequest(final List<String> texts, 
			final String targetLang) {
		
		final HttpPost request = new HttpPost(getUrl());
		
		final JsonObject json = new JsonObject();
		final JsonArray sourceTexts = new JsonArray();

		for (final String text : texts) {
		    sourceTexts.add(new JsonPrimitive(text != null ? text : ""));
		}

		json.add("q", sourceTexts);
		json.addProperty("source", "auto");
		json.addProperty("target", targetLang);
		json.addProperty("format", "text");
        
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
}
