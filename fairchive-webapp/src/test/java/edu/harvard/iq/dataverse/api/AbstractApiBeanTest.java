package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.util.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractApiBeanTest {

    private static final Logger logger = Logger.getLogger(AbstractApiBeanTest.class.getCanonicalName());

    AbstractApiBeanImpl sut;

    @BeforeEach
    public void before() {
        sut = new AbstractApiBeanImpl();
    }

    @Test
    public void testParseBooleanOrDie_ok() throws Exception {
        assertTrue(sut.parseBooleanOrDie("1"));
        assertTrue(sut.parseBooleanOrDie("yes"));
        assertTrue(sut.parseBooleanOrDie("true"));
        assertFalse(sut.parseBooleanOrDie("false"));
        assertFalse(sut.parseBooleanOrDie("0"));
        assertFalse(sut.parseBooleanOrDie("no"));
    }

    @Test
    public void testParseBooleanOrDie_invalid() throws Exception {
        assertThrows(Exception.class, () -> sut.parseBooleanOrDie("I'm not a boolean value!"));
    }

    @Test
    public void testAllowCors() {
        Response r = sut.allowCors(new MockResponse(200));
        assertEquals("*", r.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    public void testMessagesNoJsonObject() {
        String message = "myMessage";
        Response response = sut.ok(message);
        JsonReader jsonReader = Json.createReader(new StringReader(response.getEntity().toString()));
        JsonObject jsonObject = jsonReader.readObject();
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jwf = Json.createWriterFactory(config);
        StringWriter sw = new StringWriter();
        try (JsonWriter jsonWriter = jwf.createWriter(sw)) {
            jsonWriter.writeObject(jsonObject);
        }
        logger.info(sw.toString());
        assertEquals(message, jsonObject.getJsonObject("data").getString("message"));
    }

    @Test
    public void isViewedFromAnonymizedPrivateUrl__apiTokenOfAnonymizedPrivateUrl() {
        // given
        Dataset dataset = dataset(7L);
        sut.session = mock(DataverseSession.class);

        // when
        boolean anonymized = sut.isViewedFromAnonymizedPrivateUrl(new PrivateUrlUser(7L, true), dataset);

        // then
        assertThat(anonymized).isTrue();
    }

    @Test
    public void isViewedFromAnonymizedPrivateUrl__sessionOfAnonymizedPrivateUrl() {
        // given
        Dataset dataset = dataset(7L);
        sut.session = mock(DataverseSession.class);
        when(sut.session.isViewedFromAnonymizedPrivateUrl(dataset)).thenReturn(true);

        // when
        boolean anonymized = sut.isViewedFromAnonymizedPrivateUrl(GuestUser.get(), dataset);

        // then
        assertThat(anonymized).isTrue();
    }

    @Test
    public void isViewedFromAnonymizedPrivateUrl__neither() {
        // given
        Dataset dataset = dataset(7L);
        sut.session = mock(DataverseSession.class);

        // when
        boolean anonymized = sut.isViewedFromAnonymizedPrivateUrl(new PrivateUrlUser(7L, false), dataset);

        // then
        assertThat(anonymized).isFalse();
    }

    private static Dataset dataset(Long id) {
        Dataset dataset = new Dataset();
        dataset.setId(id);
        return dataset;
    }

    /**
     * dummy implementation
     */
    public class AbstractApiBeanImpl extends AbstractApiBean {

    }

}
