package edu.harvard.iq.dataverse.api.imports;

import static java.util.Arrays.asList;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeStartIgnoreCase;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockWithFieldsDTO;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Reads a DataCite (kernel-4) <code>resource</code> element into a
 * {@link DatasetDTO}. The mapping is the reverse of the one used by
 * {@link edu.harvard.iq.dataverse.export.datacite.DataCiteResourceCreator};
 * elements that exporter does not write are ignored. So is the publisher:
 * the exporter writes there the name of the root collection, and a harvested
 * dataset has no field to keep it in.
 *
 * @author Arkadiusz Kowal
 */
final class DataCiteReader {

    private static final String UNKNOWN_PUBLICATION_YEAR = "9999";
    private static final String RELATION_OF_FILES = "HasPart";
    private static final String ROR = "ROR";
    private static final Set<String> SUPPORTED_NAME_IDENTIFIER_SCHEMES =
            new HashSet<>(asList("ORCID", "ISNI", "LCNA"));
    private static final List<String> DOI_PREFIXES =
            asList("doi:", "https://doi.org/", "http://doi.org/", "https://dx.doi.org/", "http://dx.doi.org/");
    private static final List<String> HANDLE_PREFIXES =
            asList("hdl:", "https://hdl.handle.net/", "http://hdl.handle.net/");

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    // -------------------- CONSTRUCTORS --------------------

    DataCiteReader() {
        try {
            // Not namespace aware on purpose: a record cut out of an OAI-PMH
            // response may use prefixes (xsi) declared on the envelope only.
            this.factory.setNamespaceAware(false);
            this.factory.setFeature(FEATURE_SECURE_PROCESSING, true);
            this.factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            this.factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            this.factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            this.factory.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    // -------------------- LOGIC --------------------

    /**
     * Maps the given DataCite record to a released dataset with a single
     * citation metadata block.
     *
     * @param xml the <code>resource</code> element
     * @return the dataset described by the record
     * @throws ImportException if the xml cannot be parsed, its root is not
     *         a <code>resource</code> element, or it carries neither a DOI
     *         nor a Handle identifier
     */
    DatasetDTO read(Reader xml) throws ImportException {
        Element resource = parse(xml);

        DatasetDTO dataset = new DatasetDTO();
        readIdentifier(resource, dataset);
        text(child(resource, "publicationYear"))
                .filter(year -> year.matches("\\d{4}") && !UNKNOWN_PUBLICATION_YEAR.equals(year))
                .ifPresent(year -> dataset.setPublicationDate(year + "-01-01"));

        Map<String, MetadataBlockWithFieldsDTO> blocks = new HashMap<>();
        blocks.put("citation", readCitationBlock(resource));
        DatasetVersionDTO version = new DatasetVersionDTO();
        version.setVersionState(DatasetVersion.VersionState.RELEASED.name());
        version.setMetadataBlocks(blocks);
        dataset.setDatasetVersion(version);
        return dataset;
    }

    // -------------------- PRIVATE --------------------

    private Element parse(Reader xml) throws ImportException {
        Element root;
        try {
            root = this.factory.newDocumentBuilder().parse(new InputSource(xml)).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ImportException("Failed to parse DataCite XML record: " + e.getMessage(), e);
        }
        if (!"resource".equals(localName(root))) {
            throw new ImportException("Unsupported xml format. Root element is not 'resource'.");
        }
        return root;
    }

    private MetadataBlockWithFieldsDTO readCitationBlock(Element resource) {
        MetadataBlockWithFieldsDTO citation = new MetadataBlockWithFieldsDTO();
        citation.setFields(new ArrayList<>());
        readTitle(resource).ifPresent(title -> citation.getFields()
                .add(DatasetFieldDTOFactory.createPrimitive(DatasetFieldConstant.title, title)));
        addCompound(citation, DatasetFieldConstant.author,
                map(grandchildren(resource, "creators", "creator"), this::readAuthor));
        addCompound(citation, DatasetFieldConstant.relatedMaterial,
                map(grandchildren(resource, "relatedIdentifiers", "relatedIdentifier"), this::readRelatedMaterial));
        addCompound(citation, DatasetFieldConstant.description,
                map(grandchildren(resource, "descriptions", "description"), this::readDescription));
        addCompound(citation, DatasetFieldConstant.datasetContact,
                map(contributors(resource, "ContactPerson"), contributor -> readContributor(contributor,
                        DatasetFieldConstant.datasetContactName, DatasetFieldConstant.datasetContactAffiliation)));
        addCompound(citation, DatasetFieldConstant.producer,
                map(contributors(resource, "Producer"), contributor -> readContributor(contributor,
                        DatasetFieldConstant.producerName, DatasetFieldConstant.producerAffiliation)));
        addCompound(citation, DatasetFieldConstant.grantNumber,
                map(grandchildren(resource, "fundingReferences", "fundingReference"), this::readGrant));
        return citation;
    }

    private void readIdentifier(Element resource, DatasetDTO dataset) throws ImportException {
        String value = text(child(resource, "identifier")).orElse("");
        String doi = removePrefix(value, DOI_PREFIXES);
        String handle = removePrefix(value, HANDLE_PREFIXES);
        String type = child(resource, "identifier").map(e -> e.getAttribute("identifierType")).orElse("");

        if (doi.startsWith("10.") && doi.indexOf('/') > 0) {
            dataset.setProtocol(GlobalId.DOI_PROTOCOL);
            dataset.setAuthority(doi.substring(0, doi.indexOf('/')));
            dataset.setIdentifier(doi.substring(doi.indexOf('/') + 1));
        } else if (handle.indexOf('/') > 0 && (handle.length() < value.length() || "Handle".equalsIgnoreCase(type))) {
            dataset.setProtocol(GlobalId.HDL_PROTOCOL);
            dataset.setAuthority(handle.substring(0, handle.indexOf('/')));
            dataset.setIdentifier(handle.substring(handle.indexOf('/') + 1));
        } else {
            throw new ImportException("DataCite record has no DOI or Handle identifier: '" + value + "'.");
        }
    }

    private Optional<String> readTitle(Element resource) {
        List<Element> titles = grandchildren(resource, "titles", "title");
        Optional<String> mainTitle = titles.stream()
                .filter(title -> isBlank(title.getAttribute("titleType")))
                .map(title -> text(Optional.of(title)))
                .filter(Optional::isPresent).map(Optional::get)
                .findFirst();
        return mainTitle.isPresent()
                ? mainTitle
                : titles.stream().map(title -> text(Optional.of(title)))
                        .filter(Optional::isPresent).map(Optional::get).findFirst();
    }

    private Set<DatasetFieldDTO> readAuthor(Element creator) {
        Set<DatasetFieldDTO> author = new HashSet<>();
        add(author, DatasetFieldConstant.authorName, text(child(creator, "creatorName")));

        children(creator, "nameIdentifier").stream()
                .filter(id -> SUPPORTED_NAME_IDENTIFIER_SCHEMES.contains(id.getAttribute("nameIdentifierScheme")))
                .filter(id -> text(Optional.of(id)).isPresent())
                .findFirst()
                .ifPresent(id -> {
                    addVocabulary(author, DatasetFieldConstant.authorIdType, Optional.of(id.getAttribute("nameIdentifierScheme")));
                    add(author, DatasetFieldConstant.authorIdValue, text(Optional.of(id)).map(this::withoutResolverUrl));
                });

        child(creator, "affiliation").ifPresent(affiliation -> {
            add(author, DatasetFieldConstant.authorAffiliation, text(Optional.of(affiliation)));
            if (ROR.equalsIgnoreCase(affiliation.getAttribute("affiliationIdentifierScheme"))) {
                add(author, DatasetFieldConstant.authorAffiliationIdentifier,
                        Optional.of(affiliation.getAttribute("affiliationIdentifier")));
            }
        });
        return author;
    }

    private Set<DatasetFieldDTO> readRelatedMaterial(Element relatedIdentifier) {
        Set<DatasetFieldDTO> material = new HashSet<>();
        Optional<String> id = text(Optional.of(relatedIdentifier));
        String idType = relatedIdentifier.getAttribute("relatedIdentifierType");
        String relationType = relatedIdentifier.getAttribute("relationType");
        // The exporter lists the files of a dataset as its parts. They are
        // not related material, and the vocabulary has no such relation.
        if (!id.isPresent() || RELATION_OF_FILES.equals(relationType)) {
            return material;
        }
        add(material, "url".equalsIgnoreCase(idType)
                ? DatasetFieldConstant.relatedMaterialURL
                : DatasetFieldConstant.relatedMaterialIDNumber, id);
        addVocabulary(material, DatasetFieldConstant.relatedMaterialIDType, Optional.of(toVocabularyIdType(idType)));
        addVocabulary(material, DatasetFieldConstant.relatedMaterialRelationType, Optional.of(relationType));
        return material;
    }

    /**
     * Reverses the exporter's mapping of related identifier types: the
     * vocabulary keeps them in lower case, except for arXiv.
     */
    private String toVocabularyIdType(String dataCiteIdType) {
        return "arXiv".equalsIgnoreCase(dataCiteIdType) ? "arXiv" : dataCiteIdType.toLowerCase();
    }

    private Set<DatasetFieldDTO> readDescription(Element description) {
        Set<DatasetFieldDTO> fields = new HashSet<>();
        add(fields, DatasetFieldConstant.descriptionText, text(Optional.of(description)));
        return fields;
    }

    private List<Element> contributors(Element resource, String contributorType) {
        return grandchildren(resource, "contributors", "contributor").stream()
                .filter(contributor -> contributorType.equals(contributor.getAttribute("contributorType")))
                .collect(Collectors.toList());
    }

    private Set<DatasetFieldDTO> readContributor(Element contributor, String nameType, String affiliationType) {
        Set<DatasetFieldDTO> fields = new HashSet<>();
        Optional<String> name = text(child(contributor, "contributorName"));
        if (name.isPresent()) {
            add(fields, nameType, name);
            add(fields, affiliationType, text(child(contributor, "affiliation")));
        }
        return fields;
    }

    private Set<DatasetFieldDTO> readGrant(Element fundingReference) {
        Set<DatasetFieldDTO> grant = new HashSet<>();
        Optional<String> funderName = text(child(fundingReference, "funderName"));
        if (!funderName.isPresent()) {
            return grant;
        }
        add(grant, DatasetFieldConstant.grantNumberAgency, funderName);
        child(fundingReference, "funderIdentifier")
                .filter(id -> ROR.equalsIgnoreCase(id.getAttribute("funderIdentifierType")))
                .ifPresent(id -> add(grant, DatasetFieldConstant.grantNumberAgencyIdentifier, text(Optional.of(id))));
        add(grant, DatasetFieldConstant.grantNumberValue, text(child(fundingReference, "awardNumber")));
        return grant;
    }

    /**
     * Name identifiers may come as bare values or as resolver URLs
     * (<code>https://orcid.org/0000-...</code>); only the value is kept.
     */
    private String withoutResolverUrl(String nameIdentifier) {
        return nameIdentifier.startsWith("http")
                ? nameIdentifier.substring(nameIdentifier.lastIndexOf('/') + 1)
                : nameIdentifier;
    }

    private String removePrefix(String value, List<String> prefixes) {
        for (String prefix : prefixes) {
            String stripped = removeStartIgnoreCase(value, prefix);
            if (stripped.length() < value.length()) {
                return stripped;
            }
        }
        return value;
    }

    private void add(Set<DatasetFieldDTO> fields, String typeName, Optional<String> value) {
        value.filter(StringUtils::isNotBlank)
                .ifPresent(v -> fields.add(DatasetFieldDTOFactory.createPrimitive(typeName, v.trim())));
    }

    private void addVocabulary(Set<DatasetFieldDTO> fields, String typeName, Optional<String> value) {
        value.filter(StringUtils::isNotBlank)
                .ifPresent(v -> fields.add(DatasetFieldDTOFactory.createVocabulary(typeName, v.trim())));
    }

    private void addCompound(MetadataBlockWithFieldsDTO block, String typeName, List<Set<DatasetFieldDTO>> values) {
        if (!values.isEmpty()) {
            block.getFields().add(DatasetFieldDTOFactory.createMultipleCompound(typeName, values));
        }
    }

    private List<Set<DatasetFieldDTO>> map(List<Element> elements, Function<Element, Set<DatasetFieldDTO>> mapper) {
        return elements.stream()
                .map(mapper)
                .filter(fields -> !fields.isEmpty())
                .collect(Collectors.toList());
    }

    private Optional<String> text(Optional<Element> element) {
        return element.map(Element::getTextContent)
                .map(String::trim)
                .filter(text -> !text.isEmpty());
    }

    private Optional<Element> child(Element parent, String name) {
        return children(parent, name).stream().findFirst();
    }

    private List<Element> grandchildren(Element parent, String wrapperName, String name) {
        return child(parent, wrapperName)
                .map(wrapper -> children(wrapper, name))
                .orElseGet(ArrayList::new);
    }

    private List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); ++index) {
            Node node = nodes.item(index);
            if (node.getNodeType() == ELEMENT_NODE && name.equals(localName(node))) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private String localName(Node node) {
        String name = node.getNodeName();
        return name.substring(name.indexOf(':') + 1);
    }
}
