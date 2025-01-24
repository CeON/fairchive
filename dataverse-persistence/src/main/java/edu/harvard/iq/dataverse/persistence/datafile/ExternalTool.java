package edu.harvard.iq.dataverse.persistence.datafile;

import static java.util.Collections.emptyMap;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * A specification or definition for how an external tool is intended to
 * operate. The specification is applied dynamically on a per-file basis through
 * an {@link ExternalToolHandler}.
 */
@SuppressWarnings("serial")
@Entity
public class ExternalTool implements Serializable, JpaEntity<Long> {

    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String TYPE = "type";
    public static final String TOOL_URL = "toolUrl";
    public static final String TOOL_PARAMETERS = "toolParameters";
    public static final String CONTENT_TYPE = "contentType";
    public static final String ID = "id";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    /**
     * The display name (on the button, for example) of the tool in English.
     */
    // TODO: How are we going to internationalize the display name?
    @Column(nullable = false)
    private String displayName;

    /**
     * The description of the tool in English.
     */
    // TODO: How are we going to internationalize the description?
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Whether the tool is an "explore" tool or a "configure" tool, for example.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = false)
    private String toolUrl;

    /**
     * Parameters the tool requires such as DataFile id and API Token as a JSON
     * object, persisted as a String.
     */
    @Column(nullable = false)
    private String toolParameters;

    /**
     * The file content type the tool works on. For tabular files, the type text/tab-separated-values should be sent
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentType;


    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     * <p>
     * Exception Description: The instance creation method
     * [...ExternalTool.<Default Constructor>], with no parameters, does not
     * exist, or is not accessible
     * <p>
     * Don't use it.
     */
    @Deprecated
    public ExternalTool() {
    }

    public ExternalTool(final String displayName, final String description,
            final Type type,final String toolUrl, final String toolParameters,
            final String contentType) {
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.toolUrl = toolUrl;
        this.toolParameters = toolParameters;
        this.contentType = contentType;
    }

    public enum Type {
        EXPLORE("explore"),
        CONFIGURE("configure"),
        PREVIEW("preview");

        private final String text;

        Type(final String text) {
            this.text = text;
        }

        public static Type fromString(final String text) {
            for (final Type type : Type.values()) {
                if (type.text.equals(text)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Type must be one of these values " +
                    Type.values());
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Type getType() {
        return this.type;
    }

    public String getToolUrl() {
        return this.toolUrl;
    }

    public void setToolUrl(final String toolUrl) {
        this.toolUrl = toolUrl;
    }

    public String getToolParameters() {
        return this.toolParameters;
    }
    
    private JsonObject getToolParametersAsJSON() {
        return Json.createReader(new StringReader(this.toolParameters)).readObject();
    }
    
    public Map<String, String> getToolParametersAsMap() {
        final JsonArray queryParams = getToolParametersAsJSON()
                .getJsonArray("queryParameters");
        if (queryParams != null) {
            final Map<String, String> result = new HashMap<>();
            for(final JsonObject param : queryParams.getValuesAs(JsonObject.class)) {
                param.forEach((key, value) -> result.put(key, param.getString(key)));
            }
            return result;
        } else {
            return emptyMap();
        }
    }

    public void setToolParameters(final String toolParameters) {
        this.toolParameters = toolParameters;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public JsonObjectBuilder toJson() {
        final JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add(ID, getId());
        jab.add(DISPLAY_NAME, getDisplayName());
        jab.add(DESCRIPTION, getDescription());
        jab.add(TYPE, getType().text);
        jab.add(TOOL_URL, getToolUrl());
        jab.add(TOOL_PARAMETERS, getToolParameters());
        jab.add(CONTENT_TYPE, getContentType());
        return jab;
    }

    public enum ReservedWord {

        // TODO: Research if a format like "{reservedWord}" is easily parse-able or if another format would be
        // better. The choice of curly braces is somewhat arbitrary, but has been observed in documenation for
        // various REST APIs. For example, "Variable substitutions will be made when a variable is named in {brackets}."
        // from https://swagger.io/specification/#fixed-fields-29 but that's for URLs.
        FILE_ID("fileId"),
        FULE_URL("fileUrl"),
        SITE_URL("siteUrl"),
        API_TOKEN("apiToken"),
        DATASET_ID("datasetId"),
        DATASET_VERSION("datasetVersion"),
        LOCALE_CODE("localeCode");

        private final String text;

        ReservedWord(final String text) {
            this.text = "{" + text + "}";
        }

        /**
         * This is a centralized method that enforces that only reserved words
         * are allowed to be used by external tools. External tool authors
         * cannot pass their own query parameters through Dataverse such as
         * "mode=mode1".
         *
         * @throws IllegalArgumentException
         */
        public static ReservedWord fromString(final String text) {
            for (final ReservedWord reservedWord : ReservedWord.values()) {
                if (reservedWord.text.equals(text)) {
                    return reservedWord;
                }
            }
            throw new IllegalArgumentException("Unknown reserved word: " + text);
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

}
