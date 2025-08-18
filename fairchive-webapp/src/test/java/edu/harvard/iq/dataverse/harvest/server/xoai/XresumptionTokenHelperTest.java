package edu.harvard.iq.dataverse.harvest.server.xoai;

import org.dspace.xoai.model.oaipmh.ResumptionToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class XresumptionTokenHelperTest {

    @Test
    void resolve__cursor_last_page() {
        // given
        ResumptionToken.Value val = new ResumptionToken.Value().withOffset(3);
        XresumptionTokenHelper xresumptionTokenHelper = new XresumptionTokenHelper(val, 3);

        //when
        ResumptionToken first = xresumptionTokenHelper.resolve(false);

        //then
        assertThat(first.getCursor()).isEqualTo(3);
    }

    @Test
    void resolve__cursor_with_more_results() {
        // given
        ResumptionToken.Value val = new ResumptionToken.Value().withOffset(3);
        XresumptionTokenHelper xresumptionTokenHelper = new XresumptionTokenHelper(val, 3);

        //when
        ResumptionToken first = xresumptionTokenHelper.resolve(true);

        //then
        assertThat(first.getCursor()).isEqualTo(3);
    }
}