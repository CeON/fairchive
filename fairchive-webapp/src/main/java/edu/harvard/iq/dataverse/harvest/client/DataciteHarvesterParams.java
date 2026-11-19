package edu.harvard.iq.dataverse.harvest.client;

import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Parameters used by the datacite DOI harvester.
 */
public class DataciteHarvesterParams extends HarvesterParams {

    private final static String DOI_PART_SEPARATOR = "/";

    /**
     * Prefix of the DataCite paging parameters ({@code page[size]},
     * {@code page[cursor]}, {@code page[number]}), which the harvester sets
     * itself.
     */
    private static final String PAGING_PARAM_PREFIX = "page[";

    static final int DEFAULT_MAX_RECORDS = 1000;

    static final String RESOURCE_TYPE_PARAM = "resource-type-id";

    static final String DEFAULT_RESOURCE_TYPE = "dataset";

    private List<DOIValue> doiImport;

    private List<DOIValue> doiRemove;

    private Map<String, String> queryParams;

    private Integer maxRecords;

    // -------------------- GETTERS --------------------

    public List<DOIValue> getDoiImport() {
        return Option.of(doiImport).getOrElse(emptyList());
    }

    public List<DOIValue> getDoiRemove() {
        return Option.of(doiRemove).getOrElse(emptyList());
    }

    /**
     * Parameters of the DataCite search whose matches are imported, sent to
     * {@code GET /dois} as they are; all of them must match (DataCite joins
     * them with AND). Empty when the harvest imports no search.
     *
     * @see #getSearchParams()
     */
    public Map<String, String> getQueryParams() {
        return Option.of(queryParams).getOrElse(emptyMap());
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
     * Upper limit of the records processed from the search &ndash; imported
     * or failed, both count; {@value #DEFAULT_MAX_RECORDS} when the
     * parameters set none, so that a mistaken query cannot import a large
     * part of DataCite unnoticed.
     */
    public int getMaxRecords() {
        return Option.of(maxRecords).getOrElse(DEFAULT_MAX_RECORDS);
    }

    // -------------------- LOGIC --------------------

    /**
     * Rejects parameters that list neither DOIs nor a search, a paging
     * parameter (the harvester pages itself), a blank search value, and a
     * {@code maxRecords} below 1 or given without a search (it limits only the
     * search, never the listed DOIs). Parameter names are not checked
     * otherwise: note that DataCite silently ignores a name it does not know.
     *
     * @throws IllegalArgumentException naming the first problem found
     */
    @Override
    public void validate() {
        if (hasNothingToHarvest()) {
            throw new IllegalArgumentException("Missing DOI's or query parameters");
        }
        for (Map.Entry<String, String> param : getQueryParams().entrySet()) {
            if (param.getKey().startsWith(PAGING_PARAM_PREFIX)) {
                throw new IllegalArgumentException("DataCite paging parameter not allowed, the harvester pages itself: "
                        + param.getKey());
            }
            if (StringUtils.isBlank(param.getValue())) {
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
        return getDoiImport().isEmpty() && getDoiRemove().isEmpty() && getQueryParams().isEmpty();
    }

    // -------------------- SETTERS --------------------

    public void setDoiImport(List<DOIValue> doiImport) {
        this.doiImport = doiImport;
    }

    public void setDoiRemove(List<DOIValue> doiRemove) {
        this.doiRemove = doiRemove;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setMaxRecords(Integer maxRecords) {
        this.maxRecords = maxRecords;
    }

    // -------------------- INNER CLASSES --------------------

    static public class DOIValue {
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
