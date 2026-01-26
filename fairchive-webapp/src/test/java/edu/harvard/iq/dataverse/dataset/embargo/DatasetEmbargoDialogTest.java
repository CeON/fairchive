package edu.harvard.iq.dataverse.dataset.embargo;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DefaultDateFormat;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MaximumEmbargoLength;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DatasetEmbargoDialogTest {

	@Mock
	private DatasetEmbargoDialog dialog;
	@Mock
	private SettingsServiceBean settings;
	private DatasetService datasetService = null;
	
	@BeforeEach
	void setUp() {
		this.dialog = new DatasetEmbargoDialog(this.datasetService, this.settings);
	}
	
	@Test
	void maximumEmbargoLength_notSet() {
		
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength))).thenReturn(null);
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength), anyInt())).thenReturn(0);
		when(this.settings.getValueForKeyAsLong(eq(MaximumEmbargoLength))).thenReturn(null);
		when(this.settings.getValueForKey(eq(DefaultDateFormat), anyString())).thenReturn("yyyy-MM-dd");
		
		assertThat(this.dialog.isMaximumEmbargoLengthSet()).isFalse();
		assertThat(this.dialog.getMaximumEmbargoLength()).isZero();
		assertThat(this.dialog.getMaximumEmbargoDateForDisplay()).isEmpty();
		assertThat(this.dialog.getMaximumEmbargoDate()).isEmpty();

		assertThat(this.dialog.getCurrentEmbargoDate()).isNull();
		assertThat(this.dialog.getCurrentEmbargoDateForDisplay()).isEmpty();
		assertThat(this.dialog.getTomorrowsDate()).isNotNull();
		
		assertThat(this.dialog.getDefaultDateFormat()).isNotEmpty();
	}
	
	@Test
	void maximumEmbargoLength_set() {
		
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength))).thenReturn(3);
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength), anyInt())).thenReturn(3);
		when(this.settings.getValueForKeyAsLong(eq(MaximumEmbargoLength))).thenReturn(3L);
		when(this.settings.getValueForKeyAsLong(eq(MaximumEmbargoLength))).thenReturn(3L);
		when(this.settings.getValueForKey(eq(DefaultDateFormat), anyString())).thenReturn("yyyy-MM-dd");
		
		assertThat(this.dialog.isMaximumEmbargoLengthSet()).isTrue();
		assertThat(this.dialog.getMaximumEmbargoLength()).isEqualTo(3);
		assertThat(this.dialog.getMaximumEmbargoDateForDisplay()).isNotEmpty();
		assertThat(this.dialog.getMaximumEmbargoDate()).isNotEmpty();

		assertThat(this.dialog.getCurrentEmbargoDate()).isNull();
		assertThat(this.dialog.getCurrentEmbargoDateForDisplay()).isEmpty();
		assertThat(this.dialog.getTomorrowsDate()).isNotNull();
		
		assertThat(this.dialog.getDefaultDateFormat()).isNotEmpty();
	}
}

