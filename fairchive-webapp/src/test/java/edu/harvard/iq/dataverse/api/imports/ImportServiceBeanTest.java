package edu.harvard.iq.dataverse.api.imports;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

@ExtendWith(MockitoExtension.class)
class ImportServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private EjbDataverseEngine engineSvc;

    @Mock
    private DatasetService datasetService;

    @Mock
    private HarvestedJsonParser harvestedJsonParser;

    @Mock
    private DatasetFieldValidationService fieldValidationService;
    
    @Mock
    private DatasetFieldTypeRepository typeRepository;

    @InjectMocks
    private ImportServiceBean importServiceBean;

    private DataverseRequest dataverseRequest;
    private HarvestingClient harvestingClient;
    private Dataverse dataverse;
    private String harvestIdentifier;
    private String jsonMetadata;

    @BeforeEach
    void setUp() {
        // Setup common test data
        dataverseRequest = mock(DataverseRequest.class);
        dataverse = new Dataverse();
        dataverse.setId(1L);
        dataverse.setAlias("testDataverse");

        harvestingClient = new HarvestingClient();
        harvestingClient.setDataverse(dataverse);

        harvestIdentifier = "test:123";
        jsonMetadata = "{\"datasetVersion\":{\"metadataBlocks\":{}}}";
    }

    @Test
    void shouldRemoveInvalidFieldsWhenImportingDataverseJson() throws Exception {
        // Given
        Dataset dataset = createDatasetWithInvalidFields();
        DatasetVersion datasetVersion = dataset.getLatestVersion();

        DatasetField invalidField = createDatasetField("invalidField");
        DatasetField validField = createDatasetField("validField");
        DatasetField parentField = createDatasetField("parentField");
        DatasetField childValidField = createDatasetField("childValidField");
        DatasetField childInvalidField = createDatasetField("childInvalidField");

        List<DatasetField> childFields = new ArrayList<>(asList(childValidField, childInvalidField));

        parentField.getChildren().addAll(childFields);
        
        List<DatasetField> datasetFields = new ArrayList<>(asList(validField, invalidField, parentField));
        datasetVersion.setDatasetFields(datasetFields);

        FieldValidationResult invalidFieldResult1 = mock(FieldValidationResult.class);
        when(invalidFieldResult1.getField()).thenReturn(invalidField);
        FieldValidationResult invalidFieldResult2 = mock(FieldValidationResult.class);
        when(invalidFieldResult2.getField()).thenReturn(childInvalidField);
        List<FieldValidationResult> validationResults = asList(invalidFieldResult1, invalidFieldResult2);

        when(harvestedJsonParser.parseDataset(jsonMetadata)).thenReturn(dataset);
        when(datasetService.findByGlobalId(anyString())).thenReturn(null);
        when(fieldValidationService.validateFieldsOfDatasetVersion(datasetVersion))
                .thenReturn(validationResults);
        when(engineSvc.submit(any(CreateHarvestedDatasetCommand.class))).thenReturn(dataset);

        // When
        Dataset result = importServiceBean.doImportHarvestedDataset(
                dataverseRequest,
                harvestingClient,
                harvestIdentifier,
                HarvestImporterType.DATAVERSE_JSON,
                jsonMetadata
        );

        // Then
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .hasSize(2);
        Assertions.assertThat(checkIfContainsNotPersistedDataField(datasetVersion.getDatasetFields(), validField))
                .isTrue();
        Assertions.assertThat(checkIfContainsNotPersistedDataField(datasetVersion.getDatasetFields(), parentField))
                .isTrue();
        Assertions.assertThat(checkIfContainsNotPersistedDataField(datasetVersion.getDatasetFields(), invalidField))
                .isFalse();

        Assertions.assertThat(parentField.getDatasetFieldsChildren()).hasSize(1);
        Assertions.assertThat(checkIfContainsNotPersistedDataField(parentField.getDatasetFieldsChildren(), childValidField))
                 .isTrue();
        Assertions.assertThat(checkIfContainsNotPersistedDataField(parentField.getDatasetFieldsChildren(), childInvalidField))
                 .isFalse();
        
    }

    private Dataset createDatasetWithInvalidFields() {
        Dataset dataset = new Dataset();
        dataset.setId(100L);
        dataset.setGlobalId(new GlobalId("doi:10.5072/FK2/TEST123"));

        DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        version.setVersionState(DatasetVersion.VersionState.RELEASED);

        dataset.setVersions(new ArrayList<>(asList(version)));

        return dataset;
    }

    private DatasetField createDatasetField(String fieldName) {
        DatasetField field = new DatasetField();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName(fieldName);
        field.setDatasetFieldType(fieldType);
        return field;
    }
    
    private boolean checkIfContainsNotPersistedDataField(List<DatasetField> fields, DatasetField field) {
    	for (DatasetField datasetField:fields) {
    		if (datasetField == field) {
    			return true;
    		}
    	}
    	return false;
    }
}
