package edu.harvard.iq.dataverse.export.dublincore;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockWithFieldsDTO;
import edu.harvard.iq.dataverse.export.DeserializartionHelper;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author skraffmi
 */
class DublinCoreExportUtilTest {

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should convert json to DublinCore")
    void datasetJson2dublincore() throws Exception {

        // given
        String datasetVersionAsJson = readFileFromResources("json/export/ddi/dataset-finch1.json");
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();

        // when
        OutputStream output = new ByteArrayOutputStream();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(json.toString(), DatasetDTO.class);
        DeserializartionHelper.repairNestedDatasetFields(datasetDto);
        DublinCoreExportUtil.datasetJson2dublincore(datasetDto, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = XmlPrinter.prettyPrintXml(output.toString());

        // then
        String datasetAsDdi = XmlPrinter.prettyPrintXml(readFileFromResources("xml/export/ddi/dataset-finchDC.xml"));
        assertThat(result).isEqualTo(datasetAsDdi);
    }

    @Test
    @DisplayName("Should handle cases when there is a single value instead of expected array")
    void datasetJson2dublincore__singleValueInsteadOfArray() throws Exception {

        // given
        DatasetFieldDTO field = DatasetFieldDTOFactory.createVocabulary("language", "Polish");

        MetadataBlockWithFieldsDTO metadataBlock = new MetadataBlockWithFieldsDTO();
        metadataBlock.setDisplayName("citation");
        metadataBlock.setFields(Collections.singletonList(field));

        DatasetVersionDTO version = new DatasetVersionDTO();
        version.setMetadataBlocks(Collections.singletonMap("citation", metadataBlock));

        DatasetDTO dataset = new DatasetDTO();
        dataset.setIdentifier("PCA2E3");
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setDatasetVersion(version);

        // when
        OutputStream output = new ByteArrayOutputStream();
        DublinCoreExportUtil.datasetJson2dublincore(dataset, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = output.toString();

        // then
        assertThat(result).contains("<dcterms:language>Polish</dcterms:language>");
    }

    @Test
    @DisplayName("Should handle cases when there are multiple child values")
    void datasetJson2dublincore__childValues() throws Exception {

        // given
    	DatasetFieldDTO field1 = DatasetFieldDTOFactory.createPrimitive("relatedDatasetCitation", "First relation");
    	DatasetFieldDTO field2 = DatasetFieldDTOFactory.createPrimitive("relatedDatasetCitation", "Second relation");
    	DatasetFieldDTO compound = DatasetFieldDTOFactory.createMultipleCompound("relatedDataset");
    	List<Map<String, DatasetFieldDTO>> compoundValue = new ArrayList<>();
    	compoundValue.add(Collections.singletonMap(field1.getTypeName(), field1));
    	compoundValue.add(Collections.singletonMap(field2.getTypeName(), field2));
    	compound.setValue(compoundValue);

        MetadataBlockWithFieldsDTO metadataBlock = new MetadataBlockWithFieldsDTO();
        metadataBlock.setDisplayName("Citation Metadata");
        metadataBlock.setFields(Collections.singletonList(compound));

        DatasetVersionDTO version = new DatasetVersionDTO();
        version.setMetadataBlocks(Collections.singletonMap("citation", metadataBlock));

        DatasetDTO dataset = new DatasetDTO();
        dataset.setIdentifier("PCA2E3");
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setDatasetVersion(version);

        // when
        OutputStream output = new ByteArrayOutputStream();
        DublinCoreExportUtil.datasetJson2dublincore(dataset, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = output.toString();

        // then
        assertThat(result).contains("<dcterms:relation>First relation</dcterms:relation><dcterms:relation>Second relation</dcterms:relation>");
    }

    
    
    // -------------------- PRIVATE --------------------

    private String readFileFromResources(String path) throws URISyntaxException, IOException {
        URI uri = getClass().getClassLoader()
                .getResource(path)
                .toURI();
        return String.join("\n", Files.readAllLines(Paths.get(uri)));
    }
}
