package edu.harvard.iq.dataverse.export;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;


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
