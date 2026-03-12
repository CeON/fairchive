package edu.harvard.iq.dataverse.datasetutility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonSyntaxException;

import edu.harvard.iq.dataverse.api.dto.FileTermsOfUseDTO;

/**
 * @author rmp553
 */
public class OptionalFileParamsParserTest {

    private OptionalFileParamsParser parser = new OptionalFileParamsParser();


    @Test
    public void parseFileParams__jsonDescriptionGood() {

        String val = "A new file";
        String jsonParams = "{\"description\": \"" + val + "\"}";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isEqualTo(val);
        assertThat(instance.getCategories()).isNull();

    }

    @Test
    public void parseFileParams__jsonDescriptionNumeric() {

        String jsonParams = "{\"description\": 250 }";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isEqualTo("250");

    }

    @Test
    public void parseFileParams__jsonNull() {

        String jsonParams = null;

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isNull();

    }

    @Test
    public void parseFileParams__jsonPrimitive() {

        String jsonParams = "abc";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getCategories()).isNull();

        assertThat(instance.hasCategories()).isFalse();

    }

    @Test
    public void parseFileParams__notParseableJson() {

        String jsonParams = "{\"description\": 250 ";

        assertThatThrownBy(() -> parser.parseFileParams(jsonParams))
            .isInstanceOf(JsonSyntaxException.class);
    }

    @Test
    public void parseFileParams__jsonCategoriesGood() {

        String val = "A new file";
        String jsonParams = "{\"description\": \"A new file\", \"categories\": [\"dog\", \"cat\", \"mouse\"]}";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isEqualTo(val);

        assertThat(instance.getCategories()).containsExactly("dog", "cat", "mouse");

        assertThat(instance.hasCategories()).isTrue();
        assertThat(instance.hasDescription()).isTrue();

    }

    @Test
    public void parseFileParams__categories_notAnArray() {

        String jsonParams = "{\"categories\": \"dog, cat, mouse\"}";

        assertThatThrownBy(() -> parser.parseFileParams(jsonParams))
            .isInstanceOf(JsonSyntaxException.class);

    }

    @Test
    public void parseFileParams__unusedParamsGood() {

        String jsonParams = "{\"forceReplace\": \"unused within OptionalFileParams\", \"oldFileId\": \"unused within OptionalFileParams\", \"description\": null, \"unusedParam1\": \"haha\", \"categories\": []}";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isNull();
        assertThat(instance.hasDescription()).isFalse();

        assertThat(instance.getCategories()).isNull();
        assertThat(instance.hasCategories()).isFalse();

    }

    @Test
    public void parseFileParams__emptyString() {

        String jsonParams = "";

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getDescription()).isNull();
        assertThat(instance.hasDescription()).isFalse();


        assertThat(instance.getCategories()).isNull();
        assertThat(instance.hasCategories()).isFalse();

    }

    @Test
    public void parseFileParams__termsOfUse() {

        String jsonParams = "{'termsOfUseAndAccess': {'termsType': 'LICENSE_BASED', 'license': 'license name'}}"
                .replace('\'', '"');

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getFileTermsOfUseDTO())
            .extracting(
                    FileTermsOfUseDTO::getTermsType, FileTermsOfUseDTO::getLicense,
                    FileTermsOfUseDTO::getAccessConditions, FileTermsOfUseDTO::getAccessConditionsCustomText)
            .containsExactly("LICENSE_BASED", "license name", null, null);

    }

    @Test
    public void parseFileParams__termsOfUse_allRightsReserved() {

        String jsonParams = "{'termsOfUseAndAccess': {'termsType': 'ALL_RIGHTS_RESERVED'}}"
                .replace('\'', '"');

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getFileTermsOfUseDTO())
            .extracting(
                    FileTermsOfUseDTO::getTermsType, FileTermsOfUseDTO::getLicense,
                    FileTermsOfUseDTO::getAccessConditions, FileTermsOfUseDTO::getAccessConditionsCustomText)
            .containsExactly("ALL_RIGHTS_RESERVED", null, null, null);

    }

    @Test
    public void parseFileParams__termsOfUse_restricted_academicPurpose() {

        String jsonParams = "{'termsOfUseAndAccess': {'termsType': 'RESTRICTED', 'accessConditions': 'ACADEMIC_PURPOSE'}}"
                .replace('\'', '"');

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getFileTermsOfUseDTO())
            .extracting(
                    FileTermsOfUseDTO::getTermsType, FileTermsOfUseDTO::getLicense,
                    FileTermsOfUseDTO::getAccessConditions, FileTermsOfUseDTO::getAccessConditionsCustomText)
            .containsExactly("RESTRICTED", null, "ACADEMIC_PURPOSE", null);

    }

    @Test
    public void parseFileParams__termsOfUse_restricted_customText() {

        String jsonParams = "{'termsOfUseAndAccess':"
                + " {'termsType': 'RESTRICTED', 'accessConditions': 'CUSTOM', 'accessConditionsCustomText': 'some condition'}}"
                .replace('\'', '"');

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getFileTermsOfUseDTO())
            .extracting(
                    FileTermsOfUseDTO::getTermsType, FileTermsOfUseDTO::getLicense,
                    FileTermsOfUseDTO::getAccessConditions, FileTermsOfUseDTO::getAccessConditionsCustomText)
            .containsExactly("RESTRICTED", null, "CUSTOM", "some condition");

    }

    @Test
    public void parseFileParams__termsOfUse_unknown_terms_property() {

        String jsonParams = "{'termsOfUseAndAccess':"
                + " {'unknown': true}}"
                .replace('\'', '"');

        OptionalFileParams instance = parser.parseFileParams(jsonParams);

        assertThat(instance.getFileTermsOfUseDTO())
            .extracting(
                    FileTermsOfUseDTO::getTermsType, FileTermsOfUseDTO::getLicense,
                    FileTermsOfUseDTO::getAccessConditions, FileTermsOfUseDTO::getAccessConditionsCustomText)
            .containsExactly(null, null, null, null);

    }
}
