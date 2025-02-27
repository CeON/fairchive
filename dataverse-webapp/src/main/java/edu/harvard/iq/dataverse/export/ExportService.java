package edu.harvard.iq.dataverse.export;

import java.util.Iterator;

import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;


/**
 * Class responsible for managing exporters and mainly exporting.
 */
@Stateless
public class ExportService implements Iterable<Exporter>{

    private Instance<Exporter> exporters;

    @Deprecated
    ExportService() {
        //JEE requirement
    }

    @Inject
    public ExportService(final Instance<Exporter> exporters) {
        this.exporters = exporters;
    }

    @Override
    public Iterator<Exporter> iterator() {
        return this.exporters.iterator();
    }

    public String toString(final DatasetVersion datasetVersion, final ExporterType type) 
        throws ExportException { 
        return getExporterOf(type).exportDataset(datasetVersion);
    }

    /**
     * @return MediaType of given exporter or {@link Exporter#getMediaType()} default value.
     */
    public String getMediaType(final ExporterType type) 
            throws ExportException {
        return getExporterOf(type).getMediaType();
    }

    private Exporter getExporterOf(final ExporterType type) throws ExportException {
        for(final Exporter exporter : this.exporters) {
            if(exporter.getExporterType() == type) {
                return exporter;
            }
        }
        throw new ExportException(type + " was not found among exporters");
    }
}
