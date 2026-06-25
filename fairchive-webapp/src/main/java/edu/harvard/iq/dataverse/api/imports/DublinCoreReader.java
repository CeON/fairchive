package edu.harvard.iq.dataverse.api.imports;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributor;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.language;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.otherId;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.otherIdValue;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static edu.harvard.iq.dataverse.persistence.GlobalId.URL_PROTOCOL;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.lang.Character.isDigit;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.Reader;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

@Stateless
public class DublinCoreReader {
	
	private final static String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
	private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
    private DatasetFieldTypeRepository fieldTypeRepository;
	
    public DublinCoreReader() {}
    
    @Inject
	public DublinCoreReader(final DatasetFieldTypeRepository fieldTypeRepository) {
    	
		this.fieldTypeRepository = fieldTypeRepository;
		
		this.factory.setNamespaceAware(true);
	}

	public Dataset read(final HarvestingClient client, final String identifier, 
				final Reader xml) 
			throws Exception { 

        final Document document = parseXml(xml);
		
        final Dataset dataset = new Dataset();
        dataset.setOwner(client.getDataverse());
        dataset.setHarvestedFrom(client);
        dataset.setHarvestIdentifier(identifier);
        dataset.setPublicationDate(Timestamp.from(getNodes(document, "date")
				.stream()
				.map(Node::getTextContent)
				.map(DublinCoreReader::parseToInstant)
				.min(Instant::compareTo)
				.orElse(Instant.now())));
        
        final DatasetVersion version = dataset.getLatestVersion();
        version.setVersionState(RELEASED);
        
        final Node titleNode = getNode(document, "title");
        if(titleNode == null) {
        	throw new EJBException("Missing dc:title xml element");
        }
        version.addField(newField(title, titleNode.getTextContent()));
        
        for(final Node node : getNodes(document, "creator")) {
        	version.addField(newField(author, null).
        			addChild(newField(authorName, node.getTextContent())));
        }
        
        for(final Node node : getNodes(document, "contributor")) {
        	version.addField(newField(contributor, null).
        			addChild(newField(contributorName, node.getTextContent())));
        }
        
        
        final Node languageNode = getNode(document, "language");
        if(languageNode == null) {
        	throw new EJBException("Missing dc:language xml element");
        }
        version.addField(newField(language, languageNode.getTextContent()));
        
        final ArrayList<Node> identifiers = getNodes(document, "identifier");
        if(identifier.isEmpty()) {
        	throw new EJBException("Missing dc:identifier xml element");
        }
        for(final Node node : identifiers) {
        	version.addField(newField(otherId, null).
        			addChild(newField(otherIdValue, node.getTextContent())));
        }
        dataset.setGlobalId(createGlobalId(identifiers));

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

	private static Instant parseToInstant(String value) {
	    value = value.trim();
	    if(isYearAndMonthOnly(value)) {
	    	value = value.concat("-01");
	    }
	
	    try {
	        return Instant.parse(value);
	    } catch (Exception ignored) {}
	
	    try {
	        return LocalDate.parse(value).atStartOfDay(UTC).toInstant();
	    } catch (Exception ignored) {}
	
	    try {
	        final int year = Year.parse(value).getValue();
	        return LocalDate.of(year, 1, 1).atStartOfDay(UTC).toInstant();
	    } catch (Exception ignored) {}
	    
	    throw new IllegalArgumentException("Unsupported date format: ".concat(value));
	}
	
	private static boolean isYearAndMonthOnly(final String date) {
		return date.length() == 7 
				&& isDigit(date.charAt(0))
				&& isDigit(date.charAt(1))
				&& isDigit(date.charAt(2))
				&& isDigit(date.charAt(3))
				&& date.charAt(4) == '-'
				&& isDigit(date.charAt(5))
				&& isDigit(date.charAt(6));
	}
	
	private GlobalId createGlobalId(final ArrayList<Node> identifierNodes) {
		
		final List<String> identifiers = identifierNodes
				.stream()
				.map(Node::getTextContent)
				.collect(toList());
		
		final Optional<GlobalId> doi = identifiers
				.stream()
				.filter(GlobalId::isDOI)
				.findFirst()
				.map(GlobalId::fromDOIUrl);
		if(doi.isPresent()) {
			return doi.get();
		}
		
		final Optional<GlobalId> handle = identifiers
				.stream()
				.filter(GlobalId::isHDL)
				.findFirst()
				.map(GlobalId::fromHDLUrl);
		if(handle.isPresent()) {
			return handle.get();
		}
		
		final Optional<GlobalId> url = identifiers
				.stream()
				.filter(GlobalId::isURL)
				.findFirst()
				.map(id -> {
					return new GlobalId(URL_PROTOCOL, "", id);
				});
		
		if(url.isPresent()) {
			return url.get();
		}
		
		return null;
	}
	
	private static Node getNode(final Document document, final String nodeName) {

		final NodeList children = document.getDocumentElement().getChildNodes();
		
		for (int index = 0; index < children.getLength(); ++index) {
			final Node child = children.item(index);
			if (matches(child, nodeName)) {
				return child;
			}
		}

		return null;
	}
	
	private static ArrayList<Node> getNodes(final Document document, 
			final String nodeName) {

		final ArrayList<Node> result = new ArrayList<>();
		final NodeList children = document.getDocumentElement().getChildNodes();
		
		for (int index = 0; index < children.getLength(); ++index) {
			final Node child = children.item(index);
			if (matches(child, nodeName)) { 
				result.add(child);
			}
		}

		return result;
	}
	
	private static boolean matches(final Node child, final String nodeName) {
		return child.getNodeType() == ELEMENT_NODE
				&& DC_NAMESPACE.equals(child.getNamespaceURI())
				&& nodeName.equals(child.getLocalName());
	}

    private DatasetField newField(final String typeName, final String value) {
    	
        final DatasetField field = new DatasetField();
        field.setValue(value);
        field.setDatasetFieldType(getType(typeName));
        return field;
    }
    
    private DatasetFieldType getType(final String typeName) {
    	
    	return this.fieldTypeRepository.findByName(typeName).
    	    	orElseThrow(() -> new EJBException("Undefined dataset field type: ".
    	    			concat(typeName)));
    }
}
