package edu.harvard.iq.dataverse.persistence.datafile;

import static edu.harvard.iq.dataverse.common.files.mime.TextMimeType.TSV_ALT;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.CONTENT_TYPE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.DESCRIPTION;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.DISPLAY_NAME;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.TOOL_PARAMETERS;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.TOOL_URL;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.TYPE;
import static edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type.EXPLORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import org.junit.jupiter.api.Test;

public class ExternalToolTest {

    private final String toolParameters = "{\"queryParameters\":[{\"fileid\":\"{fileId}\"},{\"siteUrl\":\"{siteUrl}\"},{\"datasetid\":\"{datasetId}\"},{\"datasetversion\":\"{datasetVersion}\"},{\"locale\":\"{localeCode}\"}]}";
    
    private ExternalTool createTool() {
        ExternalTool tool = new ExternalTool("myDisplayName",
                "myDescription", EXPLORE,
                "http://example.com",
                toolParameters,
                TSV_ALT.getMimeValue());
        tool.setId(42l);

        return tool;
    }
    
    @Test
    public void toJson() {
        ExternalTool tool = createTool();
        JsonObject jsonObject = tool.toJson().build();

        assertThat(jsonObject.getString(DISPLAY_NAME))
                .isEqualTo(tool.getDisplayName());
        assertThat(jsonObject.getString(DESCRIPTION))
                .isEqualTo(tool.getDescription());
        assertThat(jsonObject.getString(TYPE)).isEqualTo(tool.getType().toString());
        assertThat(jsonObject.getString(TOOL_URL)).isEqualTo(tool.getToolUrl());
        assertThat(jsonObject.getString(TOOL_PARAMETERS))
                .isEqualTo(tool.getToolParameters());
        assertThat(jsonObject.getString(CONTENT_TYPE))
                .isEqualTo(tool.getContentType());
    }
    
    @Test
    public void getToolParametersAsMap() {
        ExternalTool tool = createTool();
        
        Map<String, String> params = tool.getToolParametersAsMap();
        
        assertThat(params.size()).isEqualTo(5);
        assertThat(params.get("fileid")).isEqualTo("{fileId}");
        assertThat(params.get("siteUrl")).isEqualTo("{siteUrl}");
        assertThat(params.get("datasetid")).isEqualTo("{datasetId}");
        assertThat(params.get("datasetversion")).isEqualTo("{datasetVersion}");
        assertThat(params.get("locale")).isEqualTo("{localeCode}");
    }
    
    @Test
    public void getToolParametersAsMap_throwsException_forEmptyToolParams() {
        ExternalTool tool = createTool();
        tool.setToolParameters("");
        
        assertThrows(JsonParsingException.class, () -> tool.getToolParametersAsMap());
    }

}
