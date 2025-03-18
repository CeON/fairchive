/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.spi;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExporterType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * @author skraffmi
 */
public interface Exporter {

    String exportDataset(DatasetVersion version) throws ExportException;

    default String getProviderName() {
        return getExporterType().getPrefix();
    }

    String getDisplayName();

    ExporterType getExporterType();
    
    Boolean isXMLFormat();

    Boolean isHarvestable();

    Boolean isAvailableToUsers();

    String getXMLNameSpace();

    String getXMLSchemaLocation();

    String getXMLSchemaVersion();

    default String getMediaType() {
        return APPLICATION_XML;
    }

}
