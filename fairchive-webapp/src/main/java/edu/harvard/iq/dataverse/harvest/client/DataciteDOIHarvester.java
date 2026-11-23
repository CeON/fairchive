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
 * Harvests records from the DataCite REST API: the DOIs listed in the
 * parameters, and the records matching a DataCite search; removes the listed
 * harvested DOIs.
 */
@Stateless
@LocalBean
public class DataciteDOIHarvester implements Harvester<DataciteHarvesterParams> {

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

    @Override
    public Class<DataciteHarvesterParams> getParamsClass() {
        return DataciteHarvesterParams.class;
    }

    /**
     * Imports the listed DOIs, then the records the DataCite search matches
     * (at most {@code maxRecords} of them, failed ones included), then removes
     * the listed harvested DOIs. A record that fails is counted as failed and
     * the harvest goes on;
     * a failed search request ends the harvest.
     * <p>
     * Expects parameters already checked with
     * {@link DataciteHarvesterParams#validate()}: the REST API, the only
     * caller that passes DataCite parameters, does that before the harvest
     * starts, so that a mistake is answered with 400 and not only logged.
     *
     * @throws ImportException when a search request fails
     */
    @Override
    public HarvesterResult harvest(DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterParams params) throws ImportException {
        HarvesterResult rs = new HarvesterResult();

        for (DataciteHarvesterParams.DOIValue doi: params.getDoiImport()) {
            importDOI(rs, dataverseRequest, harvestingClient, hdLogger, doi);
        }

        if (!params.getQueryParams().isEmpty()) {
            importQueryResults(rs, dataverseRequest, harvestingClient, hdLogger, params);
        }

        for (DataciteHarvesterParams.DOIValue doi: params.getDoiRemove()) {
            removeDOI(rs, dataverseRequest, harvestingClient, hdLogger, doi);
        }

        return rs;
    }

    // -------------------- PRIVATE --------------------

    private void importDOI(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterParams.DOIValue doi) {
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
                                    Logger hdLogger, DataciteHarvesterParams params) throws ImportException {
        int pageSize = Math.min(MAX_PAGE_SIZE, params.getMaxRecords());
        int processed = 0;
        String cursor = FIRST_CURSOR;

        while (cursor != null && processed < params.getMaxRecords()) {
            DataCiteDoisPage page = searchDois(params, cursor, pageSize);
            if (FIRST_CURSOR.equals(cursor)) {
                hdLogger.info("DataCite query " + params.getSearchParams() + " matches "
                        + page.getMeta().getTotal() + " records; importing at most " + params.getMaxRecords());
            }
            List<DataCiteFindDoiResponse> records = page.getData().stream()
                    .limit(params.getMaxRecords() - processed)
                    .collect(toList());
            records.forEach(record -> importQueryResult(rs, dataverseRequest, harvestingClient, hdLogger, record));
            processed += records.size();
            cursor = records.isEmpty() ? null : DataCiteRestApiClient.nextCursor(page).orElse(null);
        }
    }

    private DataCiteDoisPage searchDois(DataciteHarvesterParams params, String cursor, int pageSize) throws ImportException {
        try {
            return dataCiteRestApiClient.searchDois(params.getSearchParams(), cursor, pageSize);
        } catch (IOException e) {
            throw new ImportException("DataCite query " + params.getSearchParams() + " failed: " + e.getMessage(), e);
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

    private void removeDOI(HarvesterResult rs, DataverseRequest dataverseRequest, HarvestingClient harvestingClient, Logger hdLogger, DataciteHarvesterParams.DOIValue doi) {
        try {
            if (!importService.doDeleteHarvestedDataset(dataverseRequest, harvestingClient, doi.getFull())) {
                throw new ImportException("No dataset found for " + doi.getFull() + ", skipping delete.");
            }
            rs.incrementDeleted();
        } catch (Exception e) {
            rs.incrementFailed();
            hdLogger.log(Level.SEVERE, "Failed to delete DOI " + doi.getFull() + " (" + harvestingClient.getName() + "): " + e.getMessage(), e);
        }
    }
}
