package edu.harvard.iq.dataverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class DatasetPageTest {

    @InjectMocks
    private DatasetPage datasetPage;
    @Mock
    private SystemConfig systemConfig;

    // -------------------- TESTS --------------------

    @Test
    void displayMetricsBlock__aggregatorMode() {
        // given
        when(systemConfig.isAggregatorMode()).thenReturn(true);

        // when
        boolean display = datasetPage.displayMetricsBlock();

        // then
        assertThat(display).isFalse();
    }
}
