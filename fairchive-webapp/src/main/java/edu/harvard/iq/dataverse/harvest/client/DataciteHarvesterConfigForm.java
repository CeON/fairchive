package edu.harvard.iq.dataverse.harvest.client;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Fields of the form of a DataCite harvesting client, as the user enters
 * them, converted to and from the {@link DataciteHarvesterConfig} stored
 * with the client. The query and the additional parameters together make up
 * the query parameters of the configuration; the additional parameters
 * ({@code name=value}) and the DOIs are entered one per line.
 */
public class DataciteHarvesterConfigForm implements Serializable {

    static final String QUERY_PARAM = "query";

    private static final String PARAM_SEPARATOR = "=";

    private static final String LINE_SEPARATOR = "\n";

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    /**
     * What may precede a DOI pasted as a link or with its scheme:
     * {@code https://doi.org/}, {@code http://dx.doi.org/}, {@code doi:}.
     */
    private static final Pattern DOI_PREFIX = Pattern.compile("^(?i)(https?://(dx\\.)?doi\\.org/|doi:)");

    private String query;

    private String additionalParams;

    private String dois;

    private Integer maxRecords;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * An empty form, for a new client.
     */
    public DataciteHarvesterConfigForm() {
    }

    /**
     * The form filled in from the configuration of an existing client.
     */
    public DataciteHarvesterConfigForm(DataciteHarvesterConfig config) {
        this.query = config.getQueryParams().get(QUERY_PARAM);
        this.additionalParams = config.getQueryParams().entrySet().stream()
                .filter(param -> !QUERY_PARAM.equals(param.getKey()))
                .map(param -> param.getKey() + PARAM_SEPARATOR + param.getValue())
                .collect(joining(LINE_SEPARATOR));
        this.dois = String.join(LINE_SEPARATOR, config.getDois());
        this.maxRecords = config.getConfiguredMaxRecords();
    }

    // -------------------- GETTERS --------------------

    /**
     * The DataCite search query, sent as the {@value #QUERY_PARAM} parameter.
     */
    public String getQuery() {
        return query;
    }

    /**
     * The other parameters of the DataCite search, one {@code name=value}
     * per line.
     */
    public String getAdditionalParams() {
        return additionalParams;
    }

    /**
     * The DOIs imported on every run, one per line.
     */
    public String getDois() {
        return dois;
    }

    public Integer getMaxRecords() {
        return maxRecords;
    }

    // -------------------- LOGIC --------------------

    /**
     * The configuration the form describes, checked by
     * {@link DataciteHarvesterConfig#validate()}. Blank lines are skipped,
     * the values are trimmed, and a DOI loses a {@code https://doi.org/} or
     * {@code doi:} prefix.
     *
     * @throws IllegalArgumentException naming the first problem found: an
     *         additional parameter that is not {@code name=value}, is given
     *         twice, or is the query, which has its own field; or any problem
     *         {@link DataciteHarvesterConfig#validate()} rejects
     */
    public DataciteHarvesterConfig toConfig() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (isNotBlank(query)) {
            queryParams.put(QUERY_PARAM, query.trim());
        }
        for (String line : nonBlankLines(additionalParams)) {
            addAdditionalParam(queryParams, line);
        }

        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(queryParams);
        config.setDois(nonBlankLines(dois).stream()
                .map(doi -> DOI_PREFIX.matcher(doi).replaceFirst(""))
                .collect(toList()));
        config.setMaxRecords(maxRecords);
        config.validate();
        return config;
    }

    // -------------------- PRIVATE --------------------

    private static void addAdditionalParam(Map<String, String> queryParams, String line) {
        String[] nameAndValue = line.split(PARAM_SEPARATOR, 2);
        String name = nameAndValue[0].trim();
        if (nameAndValue.length != 2 || name.isEmpty()) {
            throw new IllegalArgumentException("Additional parameter must be given as name=value: " + line);
        }
        if (QUERY_PARAM.equals(name)) {
            throw new IllegalArgumentException("The query has its own field, it is not an additional parameter");
        }
        if (queryParams.containsKey(name)) {
            throw new IllegalArgumentException("Additional parameter given more than once: " + name);
        }
        queryParams.put(name, nameAndValue[1].trim());
    }

    private static List<String> nonBlankLines(String text) {
        return Stream.of(LINE_BREAK.split(defaultString(text)))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(toList());
    }

    // -------------------- SETTERS --------------------

    public void setQuery(String query) {
        this.query = query;
    }

    public void setAdditionalParams(String additionalParams) {
        this.additionalParams = additionalParams;
    }

    public void setDois(String dois) {
        this.dois = dois;
    }

    public void setMaxRecords(Integer maxRecords) {
        this.maxRecords = maxRecords;
    }
}
