package edu.harvard.iq.dataverse.api.imports;

import static java.util.stream.Collectors.toList;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.Reader;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFieldMapping;
import edu.harvard.iq.dataverse.persistence.dataset.ForeignMetadataFormatMappingRepository;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@Stateless
public class DublinCoreReader {
	
	private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
    private ForeignMetadataFormatMappingRepository metadataMappingRepository;
    private List<ForeignMetadataFieldMapping> fieldMappings;
    private DatasetFieldTypeRepository fieldTypeRepository;
    private SettingsServiceBean settings;
	
    public DublinCoreReader() {}
    
    @Inject
	public DublinCoreReader(final ForeignMetadataFormatMappingRepository metadataMappingRepository, 
			final DatasetFieldTypeRepository fieldTypeRepository,
			final SettingsServiceBean settings) {
		this.metadataMappingRepository = metadataMappingRepository;
		this.fieldTypeRepository = fieldTypeRepository;
		this.settings = settings;
	}
	
	@PostConstruct
	public void setUp() {
		this.factory.setNamespaceAware(true);
		
		final String DCTERMS = "http://purl.org/dc/terms/";
		
    	this.fieldMappings = this.metadataMappingRepository
    			.findByName(DCTERMS)
        		.orElseThrow(() -> new EJBException(
        				"Mapping for 'http://purl.org/dc/terms/' not found in database."))
        		.getDatasetFieldTypes()
        		.stream()
        		.filter(mapping -> !mapping.isChild())
        		.collect(toList());
	}

	public Dataset read(final HarvestingClient client, final Reader xml) throws Exception { 

        final Document document = parseXml(xml);
		
        final Dataset dataset = new Dataset();
        dataset.setOwner(client.getDataverse());
        
        final DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        dataset.getVersions().add(version);
        
        
        
//        this.fieldMappings.forEach(m -> System.out.println(m.getDatasetfieldName() +
//        		" " + m.isChild()
//        		+ " + " + m.getChildFieldMappings().stream()
//        			.map(ForeignMetadataFieldMapping::getDatasetfieldName).collect(Collectors.joining(","))));
        
       
        this.fieldMappings.forEach(mapping -> {
        	this.fieldTypeRepository.findByName(mapping.getDatasetfieldName()).ifPresent(type -> {
    			final DatasetField field = new DatasetField();
    			field.setDatasetFieldType(type);
    			field.setValue(getValueOfNode(document, mapping.getForeignFieldXPath()));
    			field.setDatasetVersion(version);
    			version.getDatasetFields().add(field);
    		});
        	
        });
        
       
		return dataset;
	}
	
	private Document parseXml(final Reader xml) throws Exception {
		
		final Document document = this.factory.newDocumentBuilder().parse(new InputSource(xml));
		final Element root = document.getDocumentElement();
		
		if(!"http://www.openarchives.org/OAI/2.0/oai_dc/".equals(root.getNamespaceURI())) {
			throw new EJBException("Unsupported xml format. Root element namspace is not 'http://www.openarchives.org/OAI/2.0/oai_dc/'.");
		}
		if(!"dc".equals(root.getLocalName())) {
			throw new EJBException("Unsupported xml format. Root element is not 'dc'.");
		}
		
		return document;
	}
	
	private static String getValueOfNode(final Document document, final String nodeName) {

		final NodeList children = document.getDocumentElement().getChildNodes();
		
		for (int index = 0; index < children.getLength(); ++index) {
			final Node child = children.item(index);
			if (child.getNodeType() == ELEMENT_NODE
					&& "http://purl.org/dc/elements/1.1/".equals(child.getNamespaceURI())
					&& nodeName.endsWith(child.getLocalName())) { // endsWith since nodeName will start with ':'
				return child.getTextContent();
			}
		}

		return null;
	}
	
	private static String getValueOfNodeAttribute(final Document document, 
			final String nodeName, final String attrName) {

		final NodeList children = document.getDocumentElement().getChildNodes();
		
		for (int index = 0; index < children.getLength(); ++index) {
			final Node child = children.item(index);
			if (child.getNodeType() == ELEMENT_NODE
					&& "http://purl.org/dc/elements/1.1/".equals(child.getNamespaceURI())
					&& nodeName.endsWith(child.getLocalName())) { // endsWith since nodeName will start with ':'
				final NamedNodeMap attrs = child.getAttributes();
				for(int i = 0; i < attrs.getLength(); ++i) {
					if(attrs.item(i).getLocalName().equals(attrName)) {
						return attrs.item(i).getNodeValue();
					}
				}
			}
		}

		return null;
	}
}
