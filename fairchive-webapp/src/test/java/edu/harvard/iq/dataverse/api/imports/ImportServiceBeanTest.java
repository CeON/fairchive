package edu.harvard.iq.dataverse.api.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import edu.harvard.iq.dataverse.DatasetDao;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestedDatasetCommand;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.validation.DatasetFieldValidationService;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static java.util.Arrays.asList;

@ExtendWith(MockitoExtension.class)
class ImportServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private EjbDataverseEngine engineSvc;

    @Mock
    private DatasetDao datasetDao;

    @Mock
    private HarvestedJsonParser harvestedJsonParser;

    @Mock
    private DatasetFieldValidationService fieldValidationService;

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

        DatasetField invalidField = createDatasetField(1L, "invalidField");
        DatasetField validField = createDatasetField(2L,"fieldName");

        List<DatasetField> datasetFields = new ArrayList<>(asList(validField, invalidField));
        datasetVersion.setDatasetFields(datasetFields);

        FieldValidationResult invalidFieldResult = mock(FieldValidationResult.class);
        when(invalidFieldResult.getField()).thenReturn(invalidField);
        List<FieldValidationResult> validationResults = asList(invalidFieldResult);

        List<ValidationDescriptor> validationDescriptors = new ArrayList<>();

        when(harvestedJsonParser.parseDataset(jsonMetadata)).thenReturn(dataset);
        when(datasetDao.findByGlobalId(anyString())).thenReturn(null);
        when(fieldValidationService.validateFieldsOfDatasetVersion(datasetVersion))
                .thenReturn(validationResults);
        when(fieldValidationService.retrieveValidatorDescriptor(invalidField))
                .thenReturn(validationDescriptors);
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
                .hasSize(1);
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .contains(validField);
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .doesNotContain(invalidField);
    }

    @Test
    void shouldRemoveDependantFieldWhenParentFieldIsInvalid() throws Exception {
        // Given
        Dataset dataset = createDatasetWithInvalidFields();
        DatasetVersion datasetVersion = dataset.getLatestVersion();

        DatasetField invalidParentField = createDatasetField(1L, "author");
        DatasetField dependentField = createDatasetField(2L,"authorAffiliation");
        DatasetField unrelatedField = createDatasetField(3L,"title");

        List<DatasetField> datasetFields = new ArrayList<>(
                asList(invalidParentField, dependentField, unrelatedField)
        );
        datasetVersion.setDatasetFields(datasetFields);

        FieldValidationResult invalidFieldResult = mock(FieldValidationResult.class);
        when(invalidFieldResult.getField()).thenReturn(invalidParentField);
        List<FieldValidationResult> validationResults = asList(invalidFieldResult);

        ValidationDescriptor validationDescriptor = mock(ValidationDescriptor.class);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dependantField", "authorAffiliation");
        when(validationDescriptor.getParameters()).thenReturn(parameters);
        List<ValidationDescriptor> validationDescriptors = asList(validationDescriptor);

        when(harvestedJsonParser.parseDataset(jsonMetadata)).thenReturn(dataset);
        when(datasetDao.findByGlobalId(anyString())).thenReturn(null);
        when(fieldValidationService.validateFieldsOfDatasetVersion(datasetVersion))
                .thenReturn(validationResults);
        when(fieldValidationService.retrieveValidatorDescriptor(invalidParentField))
                .thenReturn(validationDescriptors);
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
                .hasSize(1);
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .contains(unrelatedField);
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .doesNotContain(invalidParentField);
        Assertions.assertThat(datasetVersion.getDatasetFields())
                .doesNotContain(dependentField);
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

    private DatasetField createDatasetField(Long id, String fieldName) {
        DatasetField field = new DatasetField();
        field.setId(id);
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setId(id);
        fieldType.setName(fieldName);
        field.setDatasetFieldType(fieldType);
        return field;
    }
}
