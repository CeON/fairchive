package edu.harvard.iq.dataverse.dataverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class DataversePageTest {

    @InjectMocks
    private DataversePage dataversePage;
    @Mock
    private SystemConfig sysConfig;

    // -------------------- TESTS --------------------

    @Test
    void displayMetrics__aggregatorMode() {
        // given
        when(sysConfig.isAggregatorMode()).thenReturn(true);

        // when
        boolean display = dataversePage.displayMetrics();

        // then
        assertThat(display).isFalse();
    }
}
