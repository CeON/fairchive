package edu.harvard.iq.dataverse.api.imports;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;

@Transactional(TransactionMode.ROLLBACK)
public class ImportGenericServiceBeanIT extends WebappArquillianDeployment {

    @Inject
    private ImportGenericServiceBean importGenericServiceBean;

    @Inject
    private DatasetFieldServiceBean datasetFieldService;

    @Inject
    private MetadataBlockRepository metadataBlockRepo;

    @Inject
    private SettingsServiceBean settingsService;

    @Test
    public void doImport() throws IOException, XMLStreamException, JsonParseException {

        //given
        final String xml = IOUtils.resourceToString("xml/imports/oaidc.xml", StandardCharsets.UTF_8, ImportGenericServiceBeanIT.class
                .getClassLoader());

        //when
        final DatasetDTO datasetDTO = importGenericServiceBean.processOAIDCxml(xml);

        //then
        //Assertions.assertThat(parseDTOtoEntity(datasetDTO)).isNotNull();
        
        Dataset dataset = parseDTOtoEntity(datasetDTO);
        
        System.out.println("==============================================");
        System.out.println("GlogalbId: " + dataset.getGlobalId());
        System.out.println("==============================================");
        
        List<DatasetField> fields = dataset.getLatestVersion().getDatasetFields();
        
        fields.forEach(field -> {
        	System.out.println(field.getTypeName() + " -> " + field.getValue());
        	
        	field.getChildren().forEach(child -> {
        		System.out.println("> " + child.getTypeName() + " -> " + child.getValue());
        	});
        	
        });
        
        System.out.println("==============================================");
        
    }

    private Dataset parseDTOtoEntity(DatasetDTO datasetDTO) throws JsonParseException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(datasetDTO);

        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject obj = jsonReader.readObject();

        JsonParser parser = new JsonParser(datasetFieldService, metadataBlockRepo, settingsService);
        return parser.parseDataset(obj);
    }
}
