package edu.harvard.iq.dataverse.api.imports;

import static java.util.logging.Logger.getLogger;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * @author ellenk
 */
@Stateless
public class ImportServiceBean {
    private static final Logger logger = getLogger(ImportServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    protected EjbDataverseEngine engineSvc;
    @EJB
    private DatasetService datasetService;
    @EJB
    private ImportDDIServiceBean importDDIService;
    @EJB
    private ImportGenericServiceBean importGenericService;
    @EJB
    private IndexServiceBean indexService;
    @Inject
    private HarvestedJsonParser harvestedJsonParser;
    @Inject
    private DatasetFieldValidationService fieldValidationService;
    @Inject
    private DublinCoreReader dublinCoreReader;

    // -------------------- LOGIC --------------------

    @TransactionAttribute(REQUIRES_NEW)
    public Dataset doImportHarvestedDataset(final DataverseRequest request, 
    		final HarvestingClient client, final String identifier, 
    		final DatasetDTO datasetDTO) throws ImportException {
    	
        return importDatasetDTOJson(request, client, identifier, toJson(datasetDTO));
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Dataset doImportHarvestedDataset(final DataverseRequest request, 
    		final HarvestingClient client, final String identifier, 
    		final HarvestImporterType importType, final String xml) throws ImportException {
    	
        if (client == null || client.getDataverse() == null) {
            throw new ImportException("importHarvestedDataset called wiht a null harvestingClient, or an invalid harvestingClient.");
        }

        // TODO:
        // At the moment (4.5; the first official "export/harvest release"), there
        // are 3 supported metadata formats: DDI, DC and native Dataverse metadata
        // encoded in JSON. The 2 XML formats are handled by custom implementations;
        // each of the 2 implementations uses its own parsing approach. (see the
        // ImportDDIServiceBean and ImportGenerciServiceBean for details).
        // TODO: Need to create a system of standardized import plugins - similar to Stephen
        // Kraffmiller's export modules; replace the logic below with clean
        // programmatic lookup of the import plugin needed.

        if (importType == HarvestImporterType.DDI) {
            try {
                // TODO:
                // import type should be configurable - it should be possible to
                // select whether you want to harvest with or without files,
                // ImportType.HARVEST vs. ImportType.HARVEST_WITH_FILES
                logger.fine("importing DDI");
                DatasetDTO dsDTO = this.importDDIService.doImport(ImportType.HARVEST, xml);
                return importDatasetDTOJson(request, client, identifier, toJson(dsDTO));
            } catch (XMLStreamException | ImportException e) {
                throw new ImportException("Failed to process DDI XML record: " + 
                		e.getClass() + " (" + e.getMessage() + ")");
            }
        } else if (importType == HarvestImporterType.DUBLIN_CORE) {
//            try {
//                DatasetDTO dsDTO = this.importGenericService.processOAIDCxml(xml);
//                return importDatasetDTOJson(request, client, identifier, toJson(dsDTO));
//            } catch (XMLStreamException e) {
//                throw new ImportException("Failed to process Dublin Core XML record: " + 
//                		e.getClass() + " (" + e.getMessage() + ")");
//            }
        	
        	  try {
        		  final Dataset set = this.dublinCoreReader.read(client, identifier, 
        				  new StringReader(xml));
        		  return createHarvestedDataset(request, set, client.getDataverse());
        	  } catch (final Exception e) {
        		  logger.warning("error processing\n" + xml);
        		  throw new ImportException(xml, e);
        	  }
        } else if (importType == HarvestImporterType.DATAVERSE_JSON) {
            // This is Dataverse metadata already formatted in JSON.
            // Simply read it into a string, and pass to the final import further down:
            return importDatasetDTOJson(request, client, identifier, xml);
        } else {
            throw new ImportException("Unsupported import type: " + importType);
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void doDeleteHarvestedDataset(final DataverseRequest request, 
    		final HarvestingClient client, final String identifier) throws ImportException {
    	
        final Dataset dataset = this.datasetService.getDatasetByHarvestInfo(client.getDataverse(), identifier);
        if (dataset != null) {
            // Purge all the SOLR documents associated with this client from the
            // index server:
            this.indexService.deleteHarvestedDocuments(dataset);

            // files from harvested datasets are removed unceremoniously,
            // directly in the database. no need to bother calling the
            // DeleteFileCommand on them.
            for (DataFile harvestedFile : dataset.getFiles()) {
                DataFile merged = em.merge(harvestedFile);
                em.remove(merged);
            }

            dataset.setFiles(null);
            Dataset merged = em.merge(dataset);
            this.engineSvc.submit(new DeleteDatasetCommand(request, merged));
        } else {
            throw new ImportException("No dataset found for " + identifier + 
            		", skipping delete. ");
        }
    }

    private Dataset importDatasetDTOJson(final DataverseRequest request, 
    		final HarvestingClient client, final String identifier, 
    		final String json) throws ImportException {
    	
        try {
            Dataset ds = harvestedJsonParser.parseDataset(json);

            Dataverse owner = client.getDataverse();
            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against validation constraints
            DatasetVersion versionToValidate = ds.getVersions().get(0);
            // For migration and harvest, remove invalid dataset fields
            removeInvalidFieldsFromDataset(versionToValidate);

            // A Global ID is required, in order for us to be able to harvest and import
            // this dataset:
            if (StringUtils.isEmpty(ds.getGlobalId().toString())) {
                throw new ImportException("The harvested metadata record with the OAI server identifier " + identifier
                        + " does not contain a global unique identifier that we could recognize, skipping.");
            }

            ds.setHarvestedFrom(client);
            ds.setHarvestIdentifier(identifier);

            return createHarvestedDataset(request, ds, owner);
        } catch (JsonParseException | ImportException ex) {
            logger.fine("Failed to import harvested dataset: " + ex.getClass() + 
            		": " + ex.getMessage());
            throw new ImportException(
                    String.format("Failed to import harvested dataset: %s (%s)",
                    		ex.getClass(), ex.getMessage()), ex);
        }
    }

	private Dataset createHarvestedDataset(final DataverseRequest request, Dataset ds, Dataverse owner)
			throws ImportException {
		Dataset existingDs = datasetService.findByGlobalId(ds.getGlobalId().toString());

		if (existingDs != null) {
		    // If this dataset already exists IN ANOTHER DATAVERSE
		    // we are just going to skip it!
		    if (existingDs.isNotRoot() && !owner.getId().equals(existingDs.getOwner().getId())) {
		        throw new ImportException("The dataset with the global id " + 
		        		ds.getGlobalId() + " already exists, in the dataverse "
		                + existingDs.getOwner().getAlias() + ", skipping.");
		    }
		    // And if we already have a dataset with this same id, in this same
		    // dataverse, but it is  LOCAL dataset (can happen!), we're going to
		    // skip it also:
		    if (!existingDs.isHarvested()) {
		        throw new ImportException("A LOCAL dataset with the global id " + 
		        		ds.getGlobalId() + " already exists in this dataverse; skipping.");
		    }
		    // For harvested datasets, there should always only be one version.
		    // We will replace the current version with the imported version.
		    if (existingDs.getVersions().size() != 1) {
		        throw new ImportException("Error importing Harvested Dataset, existing dataset has " + 
		        		existingDs.getVersions().size() + " versions");
		    }
		    // Purge all the SOLR documents associated with this client from the
		    // index server:
		    indexService.deleteHarvestedDocuments(existingDs);
		    // files from harvested datasets are removed unceremoniously,
		    // directly in the database. no need to bother calling the
		    // DeleteFileCommand on them.
		    for (DataFile harvestedFile : existingDs.getFiles()) {
		        DataFile merged = em.merge(harvestedFile);
		        em.remove(merged);
		    }
		    // TODO:
		    // Verify what happens with the indexed files in SOLR?
		    // are they going to be overwritten by the reindexing of the dataset?
		    existingDs.setFiles(null);
		    Dataset merged = em.merge(existingDs);
		    // harvested datasets don't have physical files - so no need to worry about that.
		    engineSvc.submit(new DestroyDatasetCommand(merged, request));
		}

		return engineSvc.submit(new CreateHarvestedDatasetCommand(ds, request));
	}

    private void removeInvalidFieldsFromDataset(final DatasetVersion  version) {
        // We do not expect the validation-removal process to require many iterations.
        // 10 attempts is a safe upper bound to prevent accidental infinite loops.
        // Under normal circumstances, all invalid fields should be resolved in first pass
        // Only dependent fields may use more passes
        int validationAttemptNumber = 0;
        while (validationAttemptNumber <= 10) {
            List<FieldValidationResult> fieldValidationResults = 
            		fieldValidationService.validateFieldsOfDatasetVersion(version);
            if (fieldValidationResults.isEmpty()) {
                break;
            }
            for (FieldValidationResult fieldValidationResult : fieldValidationResults) {
                DatasetField invalidField = (DatasetField)fieldValidationResult.getField();
                removeInvalidField(version.getDatasetFields(), invalidField);
            }

            validationAttemptNumber++;
        }
    }

    private void removeInvalidField(final List<DatasetField> fields, 
    		final DatasetField invalidField) {
    	
    	fields.removeIf(df -> df == invalidField);
        for (DatasetField datasetField : fields) {
        	if (datasetField.getChildren() != null) {
        		removeInvalidField(datasetField.getChildren(), invalidField);
        	}
        }
    }
    
    public JsonObject ddiToJson(final String xmlToParse) throws ImportException {

        try {
        	DatasetDTO dsDTO = importDDIService.doImport(ImportType.IMPORT, xmlToParse);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(dsDTO);
            return Json.createReader(new StringReader(json)).readObject();
        } catch (XMLStreamException e) {
            throw new ImportException("XMLStreamException" + e);
        }
    }

    private String toJson(final DatasetDTO dto) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(dto);
    }
}
