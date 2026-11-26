package edu.harvard.iq.dataverse.harvest.client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import io.vavr.control.Option;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Configuration of a DataCite harvesting client: the DOIs it imports and the
 * DataCite search whose matches it imports. Stored with the client as JSON
 * ({@link HarvestingClient#getHarvesterConfig()}), read by the DataCite
 * harvester on every run.
 */
public class DataciteHarvesterConfig {

    private static final String DOI_PART_SEPARATOR = "/";

    /**
     * Prefix of the DataCite paging parameters ({@code page[size]},
     * {@code page[cursor]}, {@code page[number]}), which the harvester sets
     * itself.
     */
    private static final String PAGING_PARAM_PREFIX = "page[";

    static final int DEFAULT_MAX_RECORDS = 1000;

    static final String RESOURCE_TYPE_PARAM = "resource-type-id";

    static final String DEFAULT_RESOURCE_TYPE = "dataset";

    private List<String> dois;

    private Map<String, String> queryParams;

    private Integer maxRecords;

    // -------------------- GETTERS --------------------

    /**
     * Full DOIs imported on every run, each as {@code <authority>/<id>}.
     */
    public List<String> getDois() {
        return Option.of(dois).getOrElse(emptyList());
    }

    /**
     * Parameters of the DataCite search whose matches are imported, sent to
     * {@code GET /dois} as they are; all of them must match (DataCite joins
     * them with AND). Empty when the client imports no search.
     *
     * @see #getSearchParams()
     */
    public Map<String, String> getQueryParams() {
        return Option.of(queryParams).getOrElse(emptyMap());
    }

    /**
     * Upper limit of the records processed from the search &ndash; imported
     * or failed, both count; {@value #DEFAULT_MAX_RECORDS} when the
     * configuration sets none, so that a mistaken query cannot import a large
     * part of DataCite unnoticed.
     */
    public int getMaxRecords() {
        return Option.of(maxRecords).getOrElse(DEFAULT_MAX_RECORDS);
    }

    /**
     * Upper limit of the records processed from the search as configured:
     * {@code null} when the configuration sets none, unlike
     * {@link #getMaxRecords()}, which then gives the default.
     */
    public Integer getConfiguredMaxRecords() {
        return maxRecords;
    }

    // -------------------- LOGIC --------------------

    /**
     * Reads the configuration stored with a client.
     *
     * @throws IllegalArgumentException when the JSON is blank or malformed
     */
    public static DataciteHarvesterConfig fromJson(String json) {
        if (isBlank(json)) {
            throw new IllegalArgumentException("Missing DataCite harvester configuration");
        }
        try {
            return new Gson().fromJson(json, DataciteHarvesterConfig.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Malformed DataCite harvester configuration: " + e.getMessage(), e);
        }
    }

    /**
     * The configuration as JSON, in the form {@link #fromJson(String)} reads.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * {@link #getDois() The DOIs} split into authority and identifier.
     *
     * @throws IllegalArgumentException when a DOI has no slash
     */
    public List<DOIValue> getDoiValues() {
        return getDois().stream()
                .map(DOIValue::new)
                .collect(toList());
    }

    /**
     * The parameters actually sent to the DataCite search: the
     * {@link #getQueryParams() query parameters}, limited to datasets
     * ({@code resource-type-id=dataset}) unless they name a resource type
     * themselves.
     */
    public Map<String, String> getSearchParams() {
        Map<String, String> searchParams = new LinkedHashMap<>(getQueryParams());
        searchParams.putIfAbsent(RESOURCE_TYPE_PARAM, DEFAULT_RESOURCE_TYPE);
        return searchParams;
    }

    /**
     * Rejects a configuration that lists neither DOIs nor a search, a DOI
     * without a slash, a paging parameter (the harvester pages itself), a
     * blank search value, and a {@code maxRecords} below 1 or given without a
     * search (it limits only the search, never the listed DOIs). Parameter
     * names are not checked otherwise: note that DataCite silently ignores a
     * name it does not know.
     *
     * @throws IllegalArgumentException naming the first problem found
     */
    public void validate() {
        if (hasNothingToHarvest()) {
            throw new IllegalArgumentException("Missing DOI's or query parameters");
        }
        getDoiValues();
        for (Map.Entry<String, String> param : getQueryParams().entrySet()) {
            if (param.getKey().startsWith(PAGING_PARAM_PREFIX)) {
                throw new IllegalArgumentException("DataCite paging parameter not allowed, the harvester pages itself: "
                        + param.getKey());
            }
            if (isBlank(param.getValue())) {
                throw new IllegalArgumentException("Empty value of DataCite query parameter: " + param.getKey());
            }
        }
        if (maxRecords != null) {
            if (maxRecords < 1) {
                throw new IllegalArgumentException("maxRecords must be at least 1, was: " + maxRecords);
            }
            if (getQueryParams().isEmpty()) {
                throw new IllegalArgumentException("maxRecords applies only to queryParams, which are missing");
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private boolean hasNothingToHarvest() {
        return getDois().isEmpty() && getQueryParams().isEmpty();
    }

    // -------------------- SETTERS --------------------

    public void setDois(List<String> dois) {
        this.dois = dois;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setMaxRecords(Integer maxRecords) {
        this.maxRecords = maxRecords;
    }

    // -------------------- INNER CLASSES --------------------

    public static class DOIValue {
        private final String authority;
        private final String id;

        // -------------------- CONSTRUCTORS --------------------

        public DOIValue(String authority, String id) {
            this.authority = authority;
            this.id = id;
        }

        /**
         * Splits a DOI at its first slash: the identifier part may contain
         * further slashes ({@code 10.5072/FK2/ABC}).
         */
        public DOIValue(String fullDoi) {
            String[] doiParts = fullDoi.split(DOI_PART_SEPARATOR, 2);
            if (doiParts.length != 2) {
                throw new IllegalArgumentException("Invalid DOI: " + fullDoi);
            }
            this.authority = doiParts[0];
            this.id = doiParts[1];
        }

        // -------------------- GETTERS --------------------

        public String getAuthority() {
            return authority;
        }

        public String getId() {
            return id;
        }

        public String getFull() {
            return authority + DOI_PART_SEPARATOR + id;
        }
    }
}
