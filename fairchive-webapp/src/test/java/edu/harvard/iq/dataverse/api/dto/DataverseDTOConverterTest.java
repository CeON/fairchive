package edu.harvard.iq.dataverse.api.dto;

import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.UNCATEGORIZED;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.api.dto.DataverseDTO.DataverseContactDTO;
import edu.harvard.iq.dataverse.api.dto.DataverseDTO.DataverseThemeDTO;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;

class DataverseDTOConverterTest {

    @Test
    void convert() {
        // given
        Dataverse dataverse = new Dataverse();
        DataverseVersion version = dataverse.addVersion(RELEASED);
        dataverse.setId(1L);
        version.setAlias("dataverse-alias");
        version.addContact("mail1@mail.com");
        version.addContact("mail2@mail.com");
        version.addContact("mail3@mail.com");
        
        version.setDataverseType(UNCATEGORIZED);
        DataverseTheme theme = new DataverseTheme();
        theme.setId(11L);
        theme.setLogo("logo");
        version.setTheme(theme);

        // when
        DataverseDTO converted = new DataverseDTO.Converter().convert(dataverse);

        // then
        assertThat(converted).extracting(DataverseDTO::getId, DataverseDTO::getAlias, DataverseDTO::getDataverseType)
                .containsExactly(1L, "dataverse-alias", "UNCATEGORIZED");
        assertThat(converted.getDataverseContacts())
                .extracting(DataverseContactDTO::getContactEmail, DataverseContactDTO::getDisplayOrder)
                .containsExactly(tuple("mail1@mail.com", 0), tuple("mail2@mail.com", 0), tuple("mail3@mail.com", 0));
        assertThat(converted.getTheme())
                .extracting(DataverseThemeDTO::getId, DataverseThemeDTO::getLogo)
                .containsExactly(11L, "logo");
    }
}