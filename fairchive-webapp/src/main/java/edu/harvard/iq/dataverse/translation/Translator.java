package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MachineTranslationURL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * Uses libtranslate running as external service to translate provided texts
 */
@ApplicationScoped
public class Translator {
	
	private final static Logger log = getLogger(Translator.class);
	
	private SettingsServiceBean settings;
	
	public Translator() {}
	
	@Inject
	public Translator(final SettingsServiceBean settings) {
		this.settings =  settings;
	}
	
	
	public boolean isEnabled() {
		return ! getUrl().isEmpty();
	}
	
	public String translate(final String text, final String targetLang) {
		if (isNotBlank(text)) {
			try (final CloseableHttpClient client = HttpClients.createDefault()) {
				final HttpPost post = createRequest(text, targetLang);
				final HttpEntity entity = client.execute(post).getEntity();
				if (entity != null) {
					return translatedTextFrom(entity);
				} else {
					log.warn("Received empty response from libtranslate service");
					return EMPTY;
				}
			} catch(final IOException e) {
				log.warn(e.toString());
				return text;
			}
		} else {
			return text;
		}
	}
	
	private HttpPost createRequest(final String text, final String targetLang) {
		final HttpPost request = new HttpPost(getUrl());
		
		final List<BasicNameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("q", text));
        params.add(new BasicNameValuePair("target", targetLang));
        params.add(new BasicNameValuePair("source", "auto"));
        params.add(new BasicNameValuePair("format", "text"));
		
		request.setEntity(new UrlEncodedFormEntity(params, UTF_8));
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        
        return request;
	}
	
	private static String translatedTextFrom(final HttpEntity entity) throws IOException {
		try (final InputStreamReader in = new InputStreamReader(entity.getContent(), UTF_8)) {
			return new JsonParser().parse(in).getAsJsonObject().get("translatedText").getAsString();
		}
	}
	
	private String getUrl() {
		return this.settings.getValueForKey(MachineTranslationURL);
	}
	
}
