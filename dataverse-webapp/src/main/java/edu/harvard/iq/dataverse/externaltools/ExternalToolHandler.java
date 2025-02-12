package edu.harvard.iq.dataverse.externaltools;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.util.Base64;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.google.common.base.Preconditions;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple2;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
@Stateless
public class ExternalToolHandler {

    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ExternalToolHandler() { }

    @Inject
    public ExternalToolHandler(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    // -------------------- LOGIC --------------------

    public String buildToolUrlWithQueryParams(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken, String localeCode) {
        Preconditions.checkNotNull(externalTool);
        Preconditions.checkNotNull(dataFile);

        return getToolUrl(externalTool) + getQueryParametersForUrl(externalTool, dataFile, apiToken, localeCode);
    }
    
    private String getToolUrl(final ExternalTool tool) {
        return tool.getToolUrl().replace("{siteUrl}", systemConfig.getDataverseSiteUrl());
    }

    // -------------------- PRIVATE --------------------

    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    private String getQueryParametersForUrl(ExternalTool externalTool, DataFile datafile, ApiToken apiToken, String localeCode) {
        Dataset dataset = datafile.getLatestFileMetadata().getDatasetVersion().getDataset();

        String queryString = externalTool.getToolParametersAsMap().entrySet().stream()
                .map(keyValue -> new Tuple2<>(keyValue.getKey(), resolvePlaceholder(keyValue.getValue(),
                        datafile, dataset, apiToken, localeCode)))
                .filter(keyValue -> isNotEmpty(keyValue._2()))
                .map(keyValue -> keyValue._1() + "=" + keyValue._2())
                .collect(joining("&"));

        return "?" + queryString;
    }

    private String resolvePlaceholder(String value, DataFile datafile,
            Dataset dataset, ApiToken apiToken, String localeCode) {
        switch (ReservedWord.fromString(value)) {
        case FILE_ID:
            return datafile.getId().toString();
        case FILE_URL:
            return this.systemConfig.getDataverseSiteUrl() + "/api/access/datafile/"
                    + datafile.getId();
        case FILE_URL64:
            final String url = this.systemConfig.getDataverseSiteUrl() + "/api/access/datafile/"
                    + datafile.getId() + "#" + datafile.getDisplayName();
            return  Base64.getEncoder().encodeToString(url.getBytes());
        case SITE_URL:
            return this.systemConfig.getDataverseSiteUrl();
        case API_TOKEN:
            return apiToken != null ? apiToken.getTokenString() : null;
        case DATASET_ID:
            return dataset.getId().toString();
        case DATASET_VERSION:
            final String version = apiToken != null
                    ? dataset.getLatestVersion().getFriendlyVersionNumber()
                    : dataset.getLatestVersionForCopy().getFriendlyVersionNumber();
            // send the token needed in api calls that can be substituted for a
            // numeric version.
            return "DRAFT".equals(version) ? ":draft" : version;
        case LOCALE_CODE:
            return localeCode;
        default:
            return null;
        }
    }

}
