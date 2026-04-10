package edu.harvard.iq.dataverse.doi;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiBackgroundReservationInterval;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DoiProvider;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.Timer;
import java.util.TimerTask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.globalid.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class DOIBackgroundReservationServiceTest {

    @Mock
    private SettingsServiceBean settings;

    @Mock
    private Timer timer;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private DOIDataCiteServiceBean doiDataCiteService;

    @Mock
    private IndexServiceBean indexServiceBean;

    @InjectMocks
    private DOIBackgroundReservationService reservationService;

    @Test
    void registerDoiPeriodically_WithFakeProvider() {
    	
        when(this.settings.getValueForKey(DoiProvider)).thenReturn("FAKE");
        
        this.reservationService.reserveDoiPeriodically(this.timer);

        verify(this.timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void registerDoiPeriodically_WithMissingInterval() {
 
        when(this.settings.getValueForKey(DoiProvider)).thenReturn("DataCite");
        when(this.settings.getValueForKeyAsLong(DoiBackgroundReservationInterval)).thenReturn(null);
        
        this.reservationService.reserveDoiPeriodically(this.timer);

        verify(this.timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void registerDoiPeriodically() {
    	
    	when(this.settings.getValueForKey(DoiProvider)).thenReturn("DataCite");
        when(this.settings.getValueForKey(DoiBackgroundReservationInterval)).thenReturn("20");

        this.reservationService.reserveDoiPeriodically(this.timer);

        verify(this.timer, times(1)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void registerDataCiteIdentifier() {
    	
        final Dataset dataset = prepareDataset();
        when(this.datasetRepository.findByNonRegisteredIdentifier()).thenReturn(singletonList(dataset));
        when(this.doiDataCiteService.alreadyExists(any(GlobalId.class))).thenReturn(false);
        when(this.datasetRepository.save(any(Dataset.class))).thenReturn(dataset);

        this.reservationService.registerDataCiteIdentifier();

        assertThat(dataset.isIdentifierRegistered()).isTrue();
    }

    private static Dataset prepareDataset(){
        Dataset dataset = new Dataset();

        dataset.setIdentifier("TestID");
        dataset.setProtocol("doi");
        dataset.setAuthority("FK");

        return dataset;
    }
}