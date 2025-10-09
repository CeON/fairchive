package edu.harvard.iq.dataverse.externaltools;

import static edu.harvard.iq.dataverse.common.files.mime.TextMimeType.TSV_ALT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalToolRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

@Stateless
public class ExternalToolServiceBean {

    @Inject
    private ExternalToolRepository repository;

    public List<ExternalTool> findAll() {
        return this.repository.findAll();
    }

    public List<ExternalTool> findBy(final Type type) {
        return streamBy(type).collect(toList());
    }

    private List<ExternalTool> findBy(final Type type, final String contentType) {
        return streamBy(type, contentType)
                .filter(tool -> tool.getFileExtention() == null)
                .collect(toList());
    }
    
    public boolean delete(final long id) {
        if (this.repository.findById(id).isPresent()) {
            this.repository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }

    public ExternalTool save(final ExternalTool tool) {
        return this.repository.save(tool);
    }

    /**
     * Should be used only in REST (ie. where it's currently used). For the other
     * cases use the method {@link ExternalToolServiceBean#findExternalToolsByFileAndVersion(List, DataFile, DatasetVersion)}
     */
    public static List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        // Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before,
        // but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType();

        return allExternalTools.stream()
                .filter(t -> t.getContentType().equals(contentType))
                .collect(toList());
    }

    /**
     * This method takes a list of tools, a file and a dataset version and
     * returns which tools that file supports. The list of tools is passed in
     * so it doesn't query the database each time
     */
    public List<ExternalTool> findExternalToolsByFileAndVersion(
            final List<ExternalTool> allExternalTools, final DataFile file,
            final DatasetVersion datasetVersion) {

        if (file.isNonPublicOrNotIngestedTsvFile(datasetVersion)) {
            return emptyList();
        } else {
            // Map tabular data to it's mimetype (the isTabularData() check assures
            // that this code works the same as before,
            // but it may need to change if tabular data is split into subtypes with
            // differing mimetypes)
            final String contentType = file.isTabularData()
                    ? TSV_ALT.getMimeValue()
                    : file.getContentType();

            return allExternalTools.stream()
                    .filter(t -> t.getContentType().equals(contentType))
                    .collect(toList());
        }
    }
    
    public List<ExternalTool> findExternalTools(final Type type,
            final String contentType, final DataFile file,
            final DatasetVersion version) {

        if (file.isNonPublicOrNotIngestedTsvFile(version)) {
            return emptyList();
        } else {
            return findBy(type, contentType,
                    file.getFileMetadata().getFileNameExtention());
        }
    }

    public ExternalTool parseAddExternalToolManifest(String manifest) {
        if (manifest == null || manifest.isEmpty()) {
            throw new IllegalArgumentException("External tool manifest was null or empty!");
        }
        JsonReader jsonReader = Json.createReader(new StringReader(manifest));
        JsonObject jsonObject = jsonReader.readObject();
        // Note: ExternalToolServiceBeanTest tests are dependent on the order of these retrievals
        String displayName = getRequiredTopLevelField(jsonObject, ExternalTool.DISPLAY_NAME);
        String description = getRequiredTopLevelField(jsonObject, ExternalTool.DESCRIPTION);
        String typeUserInput = getRequiredTopLevelField(jsonObject, ExternalTool.TYPE);
        String contentType = getOptionalTopLevelField(jsonObject, ExternalTool.CONTENT_TYPE);
        // Legacy support - assume tool manifests without any mimetype are for tabular data
        if (contentType == null) {
            contentType = TextMimeType.TSV_ALT.getMimeValue();
        }

        // Allow IllegalArgumentException to bubble up from ExternalTool.Type.fromString
        ExternalTool.Type type = ExternalTool.Type.fromString(typeUserInput);
        String toolUrl = getRequiredTopLevelField(jsonObject, ExternalTool.TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(ExternalTool.TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray(ExternalTool.QUERY_PARAMETERS);
        boolean allRequiredReservedWordsFound = false;
        for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
            Set<String> keyValuePair = queryParam.keySet();
            for (String key : keyValuePair) {
                String value = queryParam.getString(key);
                ReservedWord reservedWord = ReservedWord.fromString(value);
                if (reservedWord.equals(ReservedWord.FILE_ID)) {
                    allRequiredReservedWordsFound = true;
                }
            }
        }
        if (!allRequiredReservedWordsFound) {
            // Some day there might be more reserved words than just {fileId}.
            throw new IllegalArgumentException("Required reserved word not found: " + ReservedWord.FILE_ID.toString());
        }
        String toolParameters = toolParametersObj.toString();
        return new ExternalTool(displayName, description, type, toolUrl, toolParameters, contentType);
    }

    // -------------------- PRIVATE --------------------
    
    private List<ExternalTool> findBy(final Type type,
            final String contentType, final String fileExtention) {
        List<ExternalTool> result = streamBy(type, contentType)
                .filter(tool -> fileExtention
                        .equalsIgnoreCase(tool.getFileExtention()))
                .collect(toList());
        if (result.size() > 0) {
            return result;
        } else {
            result = findByExtention(type, fileExtention);
            if (result.size() > 0) {
                return result;
            } else {
                return findBy(type, contentType);
            }
        }
    }

    
    private List<ExternalTool> findByExtention(final Type type, final String fileExtention) {
        return streamBy(type)
                .filter(tool -> fileExtention.equalsIgnoreCase(tool.getFileExtention()))
                .collect(toList());
    }
    
    public Stream<ExternalTool> streamBy(final Type type) {
        return findAll().stream().filter(tool -> tool.getType().equals(type));
    }
    
    private Stream<ExternalTool> streamBy(final Type type, final String contentType) {
        return streamBy(type).filter(tool -> tool.getContentType().equals(contentType));
    }
    
    private String getRequiredTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

    private String getOptionalTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            return null;
        }
    }
}
