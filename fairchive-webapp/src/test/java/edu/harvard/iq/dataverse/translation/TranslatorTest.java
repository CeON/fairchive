package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MachineTranslationURL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.translation.Translator.Format;

@ExtendWith(MockitoExtension.class)
public class TranslatorTest {

    private static final String URL = "http://libretranslate:5000/translate";

    @Mock
    private SettingsServiceBean settings;

    @Mock
    private CloseableHttpClient client;

    private Translator translator;

    @BeforeEach
    public void setUp() {
        lenient().when(this.settings.getValueForKey(MachineTranslationURL)).thenReturn(URL);
        this.translator = new Translator(this.settings, this.client);
    }

    // -------------------- TESTS --------------------

    @Test
    public void translate__serviceAnswered_returnsTranslatedTexts() throws IOException {
        // given
        mockResponse(200, "{\"translatedText\":[\"Archiwa\",\"Egipt\"]}");

        // when
        List<String> translated = this.translator.translate(asList("Archives", "Egypt"), "pl", Format.TEXT);

        // then
        assertThat(translated).containsExactly("Archiwa", "Egipt");
    }

    @ParameterizedTest
    @CsvSource({"TEXT, text", "HTML, html"})
    public void translate__givenFormat_sendsTextsLanguageAndFormat(final Format format, final String formatCode)
            throws IOException {
        // given
        mockResponse(200, "{\"translatedText\":[\"Archiwa\"]}");
        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.forClass(HttpUriRequest.class);

        // when
        this.translator.translate(singletonList("<b>Archives</b>"), "pl", format);

        // then
        verify(this.client).execute(request.capture());
        HttpPost post = (HttpPost) request.getValue();
        JsonObject body = new JsonParser()
                .parse(IOUtils.toString(post.getEntity().getContent(), UTF_8)).getAsJsonObject();
        assertThat(post.getURI().toString()).isEqualTo(URL);
        assertThat(body.get("q").getAsJsonArray().get(0).getAsString()).isEqualTo("<b>Archives</b>");
        assertThat(body.get("target").getAsString()).isEqualTo("pl");
        assertThat(body.get("format").getAsString()).isEqualTo(formatCode);
    }

    @Test
    public void translate__noTexts_noRequest() {
        // when
        List<String> translated = this.translator.translate(emptyList(), "pl", Format.TEXT);

        // then
        assertThat(translated).isEmpty();
        verifyNoInteractions(this.client);
    }

    @Test
    public void translate__errorStatus_throws() throws IOException {
        // given
        mockResponse(500, "{\"error\":\"model not loaded\"}");

        // when & then
        assertThatThrownBy(() -> this.translator.translate(singletonList("Archives"), "pl", Format.TEXT))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(HttpResponseException.class)
                .hasStackTraceContaining("model not loaded");
    }

    @Test
    public void translate__serviceUnreachable_throws() throws IOException {
        // given
        IOException failure = new IOException("connection refused");
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(failure);

        // when & then
        assertThatThrownBy(() -> this.translator.translate(singletonList("Archives"), "pl", Format.TEXT))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(failure);
    }

    // -------------------- PRIVATE --------------------

    private void mockResponse(final int status, final String body) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, status, ""));
        when(response.getEntity()).thenReturn(new StringEntity(body, UTF_8));
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(response);
    }
}
