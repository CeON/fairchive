package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.globalid.DataCiteDoisPage;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient.FIRST_CURSOR;
import static edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient.MAX_PAGE_SIZE;
import static java.util.stream.Collectors.toList;

/**
 * Harvests records from the DataCite REST API: the DOIs and the records
 * matching a DataCite search, both as configured with the harvesting client
 * ({@link DataciteHarvesterConfig}).
 */
@Stateless
@LocalBean
public class DataciteDOIHarvester implements Harvester {

    @Inject
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Inject
    private ImportServiceBean importService;

    @Inject
    private DataciteDatasetMapper dataciteDatasetMapper;

    // -------------------- LOGIC --------------------

    @Override
    public HarvestType harvestType() {
        return HarvestType.DATACITE_DOI;
    }

    /**
     * Imports the configured DOIs, then the records the configured DataCite
     * search matches (at most {@code maxRecords} of them, failed ones
     * included). A record that fails is counted as failed and the harvest goes
     * on; a failed search request ends the harvest.
     * <p>
     * The configuration is validated when it is saved with the client; it is
     * validated again here, since a stored configuration may predate a rule.
     *
     * @throws ImportException when the client has no valid configuration, or
     *                         a search request fails
     */
    @Override
    public HarvesterResult harvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger)
            throws ImportException {
        DataciteHarvesterConfig config = readConfig(harvestingClient);
        HarvesterResult rs = new HarvesterResult();

        for (DataciteHarvesterConfig.DOIValue doi : config.getDoiValues()) {
            importDOI(rs, dataverseRequest, harvestingClient, hdLogger, doi);
        }

        if (!config.getQueryParams().isEmpty()) {
            importQueryResults(rs, dataverseRequest, harvestingClient, hdLogger, config);
        }

        return rs;
    }

    // -------------------- PRIVATE --------------------

    private DataciteHarvesterConfig readConfig(HarvestingClient harvestingClient) throws ImportException {
        try {
            DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(harvestingClient.getHarvesterConfig());
            config.validate();
            return config;
        } catch (IllegalArgumentException e) {
            throw new ImportException("Invalid DataCite configuration of client " + harvestingClient.getName()
                    + ": " + e.getMessage(), e);
        }
    }

    private void importDOI(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterConfig.DOIValue doi) {
        try {
            importRecord(dataverseRequest, harvestingClient, doi.getFull(),
                    dataCiteRestApiClient.findDoi(doi.getAuthority(), doi.getId()));
            rs.incrementHarvested();
        } catch (Exception e) {
            rs.incrementFailed();
            hdLogger.log(Level.SEVERE, "Failed to import DOI (" + doi.getFull() + ") "
                    + harvestingClient.getName()
                    + "; "
                    + e.getMessage(), e);
        }
    }

    private void importQueryResults(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient,
                                    Logger hdLogger, DataciteHarvesterConfig config) throws ImportException {
        int pageSize = Math.min(MAX_PAGE_SIZE, config.getMaxRecords());
        int processed = 0;
        String cursor = FIRST_CURSOR;

        while (cursor != null && processed < config.getMaxRecords()) {
            DataCiteDoisPage page = searchDois(config, cursor, pageSize);
            if (FIRST_CURSOR.equals(cursor)) {
                hdLogger.info("DataCite query " + config.getSearchParams() + " matches "
                        + page.getMeta().getTotal() + " records; importing at most " + config.getMaxRecords());
            }
            List<DataCiteFindDoiResponse> records = page.getData().stream()
                    .limit(config.getMaxRecords() - processed)
                    .collect(toList());
            records.forEach(record -> importQueryResult(rs, dataverseRequest, harvestingClient, hdLogger, record));
            processed += records.size();
            cursor = records.isEmpty() ? null : DataCiteRestApiClient.nextCursor(page).orElse(null);
        }
    }

    private DataCiteDoisPage searchDois(DataciteHarvesterConfig config, String cursor, int pageSize) throws ImportException {
        try {
            return dataCiteRestApiClient.searchDois(config.getSearchParams(), cursor, pageSize);
        } catch (IOException e) {
            throw new ImportException("DataCite query " + config.getSearchParams() + " failed: " + e.getMessage(), e);
        }
    }

    private void importQueryResult(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient,
                                   Logger hdLogger, DataCiteFindDoiResponse record) {
        String doi = record.getAttributes().getDoi();
        try {
            importRecord(dataverseRequest, harvestingClient, doi, record);
            rs.incrementHarvested();
        } catch (Exception e) {
            rs.incrementFailed();
            hdLogger.log(Level.SEVERE, "Failed to import DOI (" + doi + ") matched by the DataCite query; client "
                    + harvestingClient.getName()
                    + "; "
                    + e.getMessage(), e);
        }
    }

    private void importRecord(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, String doi,
                              DataCiteFindDoiResponse record) throws ImportException {
        importService.doImportHarvestedDataset(dataverseRequest, harvestingClient, doi,
                dataciteDatasetMapper.toDataset(record));
    }
}
