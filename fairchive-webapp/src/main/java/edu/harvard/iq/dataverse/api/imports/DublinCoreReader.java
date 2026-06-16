package edu.harvard.iq.dataverse.api.imports;

import java.io.Reader;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFieldMapping;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFieldMappingRepository;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFormatMapping;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFormatMappingRepository;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@Stateless
public class DublinCoreReader {
	
	private final String OAI_DC = "http://www.openarchives.org/OAI/2.0/oai_dc/";

	private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    
	
    private ForeignMetadataFormatMappingRepository metadataMappingRepository;
    private ForeignMetadataFormatMapping metadataMapping;
    private ForeignMetadataFieldMappingRepository fieldMappingRepository;
    private DatasetFieldTypeRepository fieldTypeRepository;
    private SettingsServiceBean settings;
	
    public DublinCoreReader() {}
    
    @Inject
	public DublinCoreReader(final ForeignMetadataFormatMappingRepository metadataMappingRepository, 
			final ForeignMetadataFieldMappingRepository fieldMappingRepository,
			final DatasetFieldTypeRepository fieldTypeRepository,
			final SettingsServiceBean settings) {
		this.metadataMappingRepository = metadataMappingRepository;
		this.fieldMappingRepository = fieldMappingRepository;
		this.settings = settings;
	}
	
	@PostConstruct
	public void setUp() {
		final String DCTERMS = "http://purl.org/dc/terms/";
		
    	this.metadataMapping = this.metadataMappingRepository.findByName(DCTERMS).
        		orElseThrow(() -> new EJBException(
        				"Unknown/unsupported foreign metadata format ".concat(DCTERMS)));
	}

	public Dataset read(final HarvestingClient client, final Reader xml) throws Exception { 

        final Document document = parseXml(xml);
		
        final Dataset dataset = new Dataset();
        dataset.setOwner(client.getDataverse());
        
        final DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        dataset.getVersions().add(version);
        
        final NodeList children = document.getDocumentElement().getChildNodes();
        
        for(int index = 0; index < children.getLength(); ++index) {
        	final Node child = children.item(index);
        	findMappingFor(child.getLocalName()).ifPresent(mapping -> {
        		this.fieldTypeRepository.findByName(mapping.getDatasetfieldName()).ifPresent(type -> {
        			final DatasetField field = new DatasetField();
        			field.setDatasetFieldType(type);
        			field.setValue(child.getNodeValue());
        			field.setDatasetVersion(version);
        			version.getDatasetFields().add(field);
        		});
        		
        	});
        	
        } 
		return dataset;
	}
	
	private Document parseXml(final Reader xml) throws Exception {
		
		final Document document = factory.newDocumentBuilder().parse(new InputSource(xml));
		final String rootTag = document.getDocumentElement().getTagName();
		if(!rootTag.equals("oai_dc:dc")) {
			throw new Exception("Unsupported xml format. Expected oai_dc:dc, found ".
					concat(rootTag));
		}
		return document;
	}
	
	private Optional<ForeignMetadataFieldMapping> findMappingFor(final String pathName) {
		return this.fieldMappingRepository.find(this.metadataMapping.getName(), pathName);
	}
	
}
