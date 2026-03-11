package edu.harvard.iq.dataverse.datasetutility;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import edu.harvard.iq.dataverse.api.dto.FileTermsOfUseDTO;


public class OptionalFileParamsParser implements Serializable {

    public static final String DESCRIPTION_ATTR_NAME = "description";
    public static final String CATEGORIES_ATTR_NAME = "categories";
    public static final String FILE_TERMS_OF_USE = "termsOfUseAndAccess";

    private static final Logger logger = Logger.getLogger(OptionalFileParamsParser.class.getName());


    public OptionalFileParams parseFileParams(String jsonData) {

        OptionalFileParams params = new OptionalFileParams();
        
        if (jsonData == null || jsonData.isEmpty()) {
            return params;
        }
        JsonObject jsonObj;
        try {
            jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
        } catch (ClassCastException ex) {
            logger.info("Exception parsing string '" + jsonData + "': " + ex);
            return params;
        }

        // -------------------------------
        // get description as string
        // -------------------------------
        if ((jsonObj.has(DESCRIPTION_ATTR_NAME)) && (!jsonObj.get(DESCRIPTION_ATTR_NAME).isJsonNull())) {
            params.setDescription(jsonObj.get(DESCRIPTION_ATTR_NAME).getAsString());
        }

        // -------------------------------
        // get tags 
        // -------------------------------
        Gson gson = new Gson();

        Type listType = new TypeToken<List<String>>() {
        }.getType();

        //----------------------
        // Load categories
        //----------------------
        if ((jsonObj.has(CATEGORIES_ATTR_NAME)) && (!jsonObj.get(CATEGORIES_ATTR_NAME).isJsonNull())) {
            params.setCategories(gson.fromJson(jsonObj.get(CATEGORIES_ATTR_NAME), listType));
        }

        //----------------------
        // Load File Terms of Use and Access
        //----------------------
        Type objType = new TypeToken<FileTermsOfUseDTO>(){}.getType();
        if ((jsonObj.has(FILE_TERMS_OF_USE)) && (!jsonObj.get(FILE_TERMS_OF_USE).isJsonNull())) {
            params.setFileTermsOfUseDTO(gson.fromJson(jsonObj.get(FILE_TERMS_OF_USE), objType));
        }
        
        return params;
    }

}
