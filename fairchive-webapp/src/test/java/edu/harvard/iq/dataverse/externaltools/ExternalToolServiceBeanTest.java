package edu.harvard.iq.dataverse.externaltools;

import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.CONFIGURE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.EXPLORE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.PREVIEW;
import static edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.util.Arrays.asList;
import static java.util.Calendar.DAY_OF_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalToolRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class ExternalToolServiceBeanTest {
    
    private final static String TEXT_PLAIN = "text/plain";
    private final static String APPLICATION_PDF = "application/pdf";

    @Mock
    private ExternalToolRepository repository;
    
    @InjectMocks
    private ExternalToolServiceBean service; 
    
    private final ExternalTool tool1 = new ExternalTool("tool1", "", CONFIGURE, "", "{}", TEXT_PLAIN);
    private final ExternalTool tool2 = new ExternalTool("tool2", "", PREVIEW, "", "{}", APPLICATION_PDF);
    private final ExternalTool tool3 = new ExternalTool("tool3", "", PREVIEW, "", "{}", TEXT_PLAIN);
    private final ExternalTool tool4 = new ExternalTool("tool4", "", PREVIEW, "", "{}", TEXT_PLAIN, "obj");
    private final ExternalTool tool5 = new ExternalTool("tool5", "", PREVIEW, "", "{}", "application/x-nintendo-3ds-rom", "3ds");
    
    @BeforeEach
    public void setUp() {
        when(repository.findAll()).thenReturn(asList(tool1, tool2, tool3, tool4, tool5));
    }

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @DisplayName("Should show explore tools for ingested files only if they are public")
    @CsvSource({"false, false, false, 0",
                "true, true, false, 0",
                "true, false, true, 0",
                "true, false, false, 1"})
    void findExternalToolsByFileAndVersion(boolean released, boolean embargoed, boolean restricted, int expectedSize) {
        // given
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(released ? RELEASED : DRAFT);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);

        Calendar futureEmbargoExpirationDate = Calendar.getInstance();
        futureEmbargoExpirationDate.setTime(new Date());
        futureEmbargoExpirationDate.add(DAY_OF_MONTH, 1);
        dataset.setEmbargoDate(embargoed ? futureEmbargoExpirationDate.getTime() : null);

        FileMetadata metadata = new FileMetadata();
        metadata.setDatasetVersion(datasetVersion);
        metadata.setLabel("abc");
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(metadata);
        dataFile.setOwner(dataset);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setRestrictType(restricted ? NOT_FOR_REDISTRIBUTION : null);
        metadata.setTermsOfUse(termsOfUse);

        dataFile.setFileMetadatas(metadataList);
        dataFile.setDataTable(new DataTable());

        ExternalTool.Type type = EXPLORE;
        ExternalTool externalTool = new ExternalTool("displayName", "description", type, "http://foo.com", "{}", TextMimeType.TSV_ALT.getMimeValue());
        List<ExternalTool> externalTools = new ArrayList<>();
        externalTools.add(externalTool);

        // when
        List<ExternalTool> availableExternalTools
                = service.findExternalToolsByFileAndVersion(externalTools, dataFile, datasetVersion);

        // then
        assertThat(availableExternalTools).hasSize(expectedSize);
    }
    
    @Test
    void findExternalTools_returnsToolsBasedOnMimeType() {
        // given
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setContentType(TEXT_PLAIN);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(RELEASED);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);

        FileMetadata metadata = new FileMetadata();
        metadata.setDatasetVersion(datasetVersion);
        metadata.setLabel("abc");
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(metadata);
        dataFile.setOwner(dataset);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        metadata.setTermsOfUse(termsOfUse);

        dataFile.setFileMetadatas(metadataList);

        // when
        assertThat(this.service.findExternalTools(PREVIEW, TEXT_PLAIN, dataFile,
                datasetVersion)).containsExactly(tool3);
    }
    
    @Test
    public void findExternalTools_returnToolsBasedOnFileExtention() {
        // given
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setContentType(TEXT_PLAIN);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(RELEASED);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);

        FileMetadata metadata = new FileMetadata();
        metadata.setDatasetVersion(datasetVersion);
        metadata.setLabel("abc.obj");
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(metadata);
        dataFile.setOwner(dataset);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        metadata.setTermsOfUse(termsOfUse);

        dataFile.setFileMetadatas(metadataList);

        assertThat(this.service.findExternalTools(PREVIEW, TEXT_PLAIN, dataFile,
                datasetVersion)).containsExactly(tool4);
    }
    
    @Test
    public void findExternalTools_returnToolsBasedOnFileExtention_ifSearchingByContentTypeAndExtentionFails() {
        // given
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setContentType(TEXT_PLAIN);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(RELEASED);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);

        FileMetadata metadata = new FileMetadata();
        metadata.setDatasetVersion(datasetVersion);
        metadata.setLabel("abc.3ds");
        List<FileMetadata> metadataList = new ArrayList<>();
        metadataList.add(metadata);
        dataFile.setOwner(dataset);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        metadata.setTermsOfUse(termsOfUse);

        dataFile.setFileMetadatas(metadataList);

        assertThat(this.service.findExternalTools(PREVIEW, TEXT_PLAIN, dataFile,
                datasetVersion)).containsExactly(tool5);
    }

    @Test
    void parseAddExternalToolManifest() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
                .add("description", "This tool is awesome.")
                .add("type", "explore")
                .add("toolUrl", "http://awesometool.com")
                .add("toolParameters", Json.createObjectBuilder()
                    .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("fileid", "{fileId}")
                            .build())
                        .add(Json.createObjectBuilder()
                            .add("key", "{apiToken}")
                            .build())
                        .build())
                    .build())
                .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when
        ExternalTool externalTool = service.parseAddExternalToolManifest(tool);

        // then
        assertThat(externalTool.getDisplayName()).isEqualTo("AwesomeTool");
        assertThat(externalTool.getDescription()).isEqualTo("This tool is awesome.");
        assertThat(externalTool.getType()).isEqualTo(ExternalTool.Type.EXPLORE);
        assertThat(externalTool.getToolUrl()).isEqualTo("http://awesometool.com");
        assertThat(externalTool.getToolParameters()).isEqualTo("{\"queryParameters\":[{\"fileid\":\"{fileId}\"},{\"key\":\"{apiToken}\"}]}");
        assertThat(externalTool.getContentType()).isEqualTo(TextMimeType.TSV_ALT.getMimeValue());
    }

    @Test
    void parseAddExternalToolManifest__noFileId() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
                .add("description", "This tool is awesome.")
                .add("type", "explore")
                .add("toolUrl", "http://awesometool.com")
                .add("toolParameters", Json.createObjectBuilder()
                    .add("queryParameters", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("key", "{apiToken}")
                            .build())
                        .build())
                    .build())
                .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Required reserved word not found: {fileId}");
    }

    @Test
    void parseAddExternalToolManifest__null() {
        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External tool manifest was null or empty!");
    }

    @Test
    void parseAddExternalToolManifest__emptyString() {
        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External tool manifest was null or empty!");
    }

    @Test
    void parseAddExternalToolManifest__unknownReservedWord() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
            .add("description", "This tool is awesome.")
                .add("type", "explore")
                .add("toolUrl", "http://awesometool.com")
                .add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                         .add("fileid", "{fileId}")
                         .build())
                    .add(Json.createObjectBuilder()
                         .add("key", "{apiToken}")
                         .build())
                    .add(Json.createObjectBuilder()
                         .add("mode", "mode1")
                         .build())
                    .build())
                .build())
                .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown reserved word: mode1");
    }

    @Test
    void parseAddExternalToolManifest__noDisplayName() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("description", "This tool is awesome.")
            .add("toolUrl", "http://awesometool.com")
            .add("toolParameters", Json.createObjectBuilder().build())
            .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayName is required.");
    }

    @Test
    void parseAddExternalToolManifest__noDescription() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
            .add("toolUrl", "http://awesometool.com")
            .add("toolParameters", Json.createObjectBuilder().build())
            .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description is required.");
    }

    @Test
    void parseAddExternalToolManifest__noToolUrl() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
            .add("description", "Ths tool is awesome.")
            .add("type", "explore")
            .add("toolParameters", Json.createObjectBuilder().build())
            .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("toolUrl is required.");
    }

    @Test
    void parseAddExternalToolManifest__wrongType() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
            .add("description", "This tool is awesome.")
            .add("type", "noSuchType")
            .add("toolUrl", "http://awesometool.com")
            .add("toolParameters", Json.createObjectBuilder().build())
            .add(ExternalTool.CONTENT_TYPE, TextMimeType.TSV_ALT.getMimeValue());
        String tool = json.build().toString();

        // when & then
        assertThatThrownBy(() -> service.parseAddExternalToolManifest(tool))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseAddExternalToolManifest__noContentType() {
        // given
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("displayName", "AwesomeTool")
            .add("description", "This tool is awesome.")
            .add("type", "explore")
            .add("toolUrl", "http://awesometool.com")
            .add("toolParameters", Json.createObjectBuilder()
                .add("queryParameters", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("fileid", "{fileId}")
                        .build())
                    .add(Json.createObjectBuilder()
                        .add("key", "{apiToken}")
                        .build())
                    .build())
                .build());
        String tool = json.build().toString();

        // when
        ExternalTool externalTool = service.parseAddExternalToolManifest(tool);

        // then
        assertThat(externalTool.getContentType()).isEqualTo(TextMimeType.TSV_ALT.getMimeValue());
    }
    
    
    @Test
    public void findAll_returnsproperResults() {
        
        assertThat(this.service.findAll()).containsExactly(tool1, tool2, tool3, tool4, tool5);
    }
    
    @Test
    public void findBy_withType_returnsproperResults() {
        
        assertThat(this.service.findBy(EXPLORE)).isEmpty();
        assertThat(this.service.findBy(CONFIGURE)).containsExactly(tool1);
        assertThat(this.service.findBy(PREVIEW)).containsExactly(tool2, tool3, tool4, tool5);
    }
}
