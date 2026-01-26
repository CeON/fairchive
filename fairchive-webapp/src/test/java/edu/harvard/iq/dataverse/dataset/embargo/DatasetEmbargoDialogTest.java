package edu.harvard.iq.dataverse.dataset.embargo;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DefaultDateFormat;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.MaximumEmbargoLength;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.common.DateUtil;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.UIMessages;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DatasetEmbargoDialogTest {
	
	private DatasetEmbargoDialog dialog;
	@Mock
	private SettingsServiceBean settings;
	@Mock
	private DatasetService datasetService;
	@Mock
	private UIMessages ui;
	
	private Dataset set = new Dataset();
	
	@BeforeEach
	void setUp() {
		this.set.setGlobalId(new GlobalId("doi:10.5072/FK2/BYM3IW"));
		
		this.dialog = new DatasetEmbargoDialog(this.datasetService, this.settings, this.ui);
		this.dialog.init(this.set);
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
		assertThat(this.dialog.getMaximumEmbargoDate()).isNull();

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
		assertThat(this.dialog.getMaximumEmbargoDate()).isNotNull();

		assertThat(this.dialog.getCurrentEmbargoDate()).isNull();
		assertThat(this.dialog.getCurrentEmbargoDateForDisplay()).isEmpty();
		assertThat(this.dialog.getTomorrowsDate()).isNotNull();
		
		assertThat(this.dialog.getDefaultDateFormat()).isNotEmpty();
	}
	
	@Test
	void setAnfLiftEmbargo_isSuccessful() {
		
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength))).thenReturn(null);
		when(this.settings.getValueForKeyAsInt(eq(MaximumEmbargoLength), anyInt())).thenReturn(0);
		when(this.settings.getValueForKeyAsLong(eq(MaximumEmbargoLength))).thenReturn(null);
		when(this.settings.getValueForKey(eq(DefaultDateFormat), anyString())).thenReturn("yyyy-MM-dd");
		
		this.dialog.setCurrentEmbargoDate(DateUtil.todayPlusDays(2));
		
		this.dialog.updateEmbargoDate();
		
		verify(this.ui, times(1)).addSuccessMessage(anyString());
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		this.dialog.liftEmbargo();
		
		verify(this.ui, times(2)).addSuccessMessage(anyString());
		verify(this.ui, times(0)).addErrorMessage(anyString());
	}
}

