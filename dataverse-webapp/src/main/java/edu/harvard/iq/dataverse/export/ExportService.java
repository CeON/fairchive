package edu.harvard.iq.dataverse.export;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Either;


/**
 * Class responsible for managing exporters and mainly exporting.
 */
@Stateless
public class ExportService {

    private Instance<Exporter> exporters;
    private Map<ExporterType, Exporter> exportersMap = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    ExportService() {
        //JEE requirement
    }

    @Inject
    public ExportService(final Instance<Exporter> exporters) {
        this.exporters = exporters;
    }

    @PostConstruct
    void loadAllExporters() {
        this.exporters.iterator().forEachRemaining(
                exporter -> this.exportersMap.put(exporter.getExporterType(), exporter));

    }

    // -------------------- LOGIC --------------------

    /**
     * Exports datasetVersion with given exporter.
     *
     * @return {@code Error} if exporting failed or exporter was not found in the list of exporters.
     * <p>
     * {@code String} if exporting was a success.
     */
    public Either<DataverseError, String> exportDatasetVersionAsString(
            final DatasetVersion datasetVersion, final ExporterType type) {
        final Exporter exporter = this.exportersMap.get(type);
        if(exporter != null) {
            try {
                return Either.right(exporter.exportDataset(datasetVersion));
            } catch(final ExportException e) {
                return Either.left(new DataverseError(
                        "Failed to export the dataset as " + type));
            }
        } else {
            return Either.left(
                    new DataverseError(type + " was not found among exporter list"));
        }
    }
    
    public String toString(final DatasetVersion datasetVersion, final ExporterType type) 
        throws ExportException {
        
        final Exporter exporter = this.exportersMap.get(type);
        if(exporter != null) {
            return exporter.exportDataset(datasetVersion);
        } else {
            throw new ExportException(type + " was not found among exporter list");
        }
    }

    public Map<ExporterType, Exporter> getAllExporters() {
        return unmodifiableMap(this.exportersMap);
    }

    /**
     * @return MediaType of given exporter or {@link Exporter#getMediaType()} default value.
     */
    public String getMediaType(ExporterType provider) {

        return findExporter(provider)
                .map(Exporter::getMediaType)
                .get();
    }

    // -------------------- PRIVATE --------------------

    private Optional<Exporter> findExporter(final ExporterType type) {
        return Optional.ofNullable(exportersMap.get(type));
    }

}
