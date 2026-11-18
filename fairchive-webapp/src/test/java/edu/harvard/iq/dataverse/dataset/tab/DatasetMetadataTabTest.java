package edu.harvard.iq.dataverse.dataset.tab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.CloneDatasetDialog;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsForViewTransformer;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.translation.MetadataTranslator;

@ExtendWith(MockitoExtension.class)
public class DatasetMetadataTabTest {
	
	private final static boolean ANONYMOUS = true;
	
	private DatasetMetadataTab tab;
	private DataverseSession session;
	@Mock
	private ActionLogServiceBean logService;
	@Mock
	private MetadataTranslator metadataTranslator;
	
	private DatasetVersion version = new DatasetVersion();
	private DatasetField invisibleField = new DatasetField();
	private DatasetField visibleField = new DatasetField();
	
	public DatasetMetadataTabTest() {
		MetadataBlock block = new MetadataBlock();
		block.setName("block");
		block.setId(1L);
		block.setDisplayOrder(0);
		
		DatasetFieldType invisibleType = new DatasetFieldType();
		invisibleType.setId(1L);
		invisibleType.setName("invisible");
		invisibleType.setVisibleThroughAnonymizedUrl(false);
		invisibleType.setMetadataBlock(block);
		block.getDatasetFieldTypes().add(invisibleType);
		
		this.invisibleField = new DatasetField();
		this.invisibleField.setDatasetFieldType(invisibleType);

		this.version.addField(this.invisibleField);
		
		DatasetFieldType visibleType = new DatasetFieldType();
		visibleType.setId(2L);
		visibleType.setName("visible");
		visibleType.setVisibleThroughAnonymizedUrl(true);
		visibleType.setMetadataBlock(block);
		block.getDatasetFieldTypes().add(visibleType);
		
		this.visibleField.setDatasetFieldType(visibleType);
		
		this.version.addField(this.visibleField);
		
		this.version.setDataset(new Dataset());
		this.version.getDataset().setId(10L);
		this.version.getDataset().getVersions().add(this.version);
	}
	
	@BeforeEach
	public void setUp() {
		
		this.session = new DataverseSession(this.logService, null);
		
		DatasetFieldsInitializer initializer = new DatasetFieldsInitializer(null, 
				new DatasetFieldsForViewTransformer());
		
		CloneDatasetDialog cloneDialog = new CloneDatasetDialog();
		
		this.tab = new DatasetMetadataTab(null,
				this.session, null, null, initializer, 
				null, this.metadataTranslator, cloneDialog, null);
	}
	
	@Test
	public void emptyFieldsAreNotListed_thusInvisible_forLoggedInUser() {
		
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks = 
				this.tab.getMetadataBlocks();
		
		assertThat(blocks).isEmpty();
	}
	
	@Test
	public void emptyFieldsAreNotListed_thusInvisible_throuRegularUrl() {
		
		this.session.logIn(new PrivateUrlUser(this.version.getDatasetId()));
		this.tab.init(this.version, false);
		
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks = 
				this.tab.getMetadataBlocks();
		
		assertThat(blocks).isEmpty();
	}
	
	@Test
	public void emptyFieldsAreNotListed_thusInvisible_throuAnonymousUrl() {
		
		this.session.logIn(new PrivateUrlUser(this.version.getDatasetId(), ANONYMOUS));
		this.tab.init(this.version, false);
		
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks = 
				this.tab.getMetadataBlocks();
		
		assertThat(blocks).isEmpty();
	}
	
	@Test
	public void nonEmptyFieldsAreVisible_forAuthenticatedUser() {
		
		this.invisibleField.setValue("value");
		this.visibleField.setValue("value");
		
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		
		VerifyBlockStructure();
		
		assertThat(this.tab.shouldRenderBlock(0)).isTrue();
		assertThat(this.tab.shouldRenderField(0, 0)).isTrue();
		assertThat(this.tab.shouldRenderField(0, 1)).isTrue();
	}
	
	@Test
	public void nonEmptyFieldsAreVisible_throughRegularUrl() {
		
		this.invisibleField.setValue("value");
		this.visibleField.setValue("value");
		
		this.session.logIn(new PrivateUrlUser(this.version.getDatasetId()));
		this.tab.init(this.version, false);
		
		VerifyBlockStructure();
		
		assertThat(this.tab.shouldRenderBlock(0)).isTrue();
		assertThat(this.tab.shouldRenderField(0, 0)).isTrue();
		assertThat(this.tab.shouldRenderField(0, 1)).isTrue();
	}
	
	@Test
	public void nonEmptyComplyWithVisibilityRule_throughAnoanymousUrl() {
		
		this.invisibleField.setValue("value");
		this.visibleField.setValue("value");
		
		this.session.logIn(new PrivateUrlUser(this.version.getDatasetId(), ANONYMOUS));
		this.tab.init(this.version, false);
		
		VerifyBlockStructure();
		
		assertThat(this.tab.shouldRenderBlock(0)).isTrue();
		assertThat(this.tab.shouldRenderField(0, 0)).isFalse();
		assertThat(this.tab.shouldRenderField(0, 1)).isTrue();
	}
	
	@Test
	public void blockIsInvisible_ifItContainsNoVisibleValues_throughAnoanymousUrl() {
		
		this.invisibleField.setValue("value");
		
		this.session.logIn(new PrivateUrlUser(this.version.getDatasetId(), ANONYMOUS));
		this.tab.init(this.version, false);
		
		assertThat(this.tab.shouldRenderBlock(0)).isFalse();
	}
	
	
	@Test
	public void translationDialog_getMetadataBlocks__noLanguageSelected_returnsOriginal() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);

		// when
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks =
				this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		assertThat(blocks).isEqualTo(this.tab.getMetadataBlocks());
		verifyNoInteractions(this.metadataTranslator);
	}

	@Test
	public void translationDialog_getMetadataBlocks__sameLanguage_translatedOnce() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		when(this.metadataTranslator.translate(anyMap(), any())).thenReturn(new HashMap<>());
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");
		this.tab.getTranslationDialog().getMetadataBlocks();

		// when
		this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		verify(this.metadataTranslator).translate(anyMap(), eq("pl"));
	}

	@Test
	public void translationDialog_getMetadataBlocks__otherLanguageInBetween_translatedAgain() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		when(this.metadataTranslator.translate(anyMap(), any())).thenReturn(new HashMap<>());
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");
		this.tab.getTranslationDialog().getMetadataBlocks();
		this.tab.getTranslationDialog().setSelectedLanguageCode("de");
		this.tab.getTranslationDialog().getMetadataBlocks();
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");

		// when
		this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		verify(this.metadataTranslator, times(2)).translate(anyMap(), eq("pl"));
	}

	@Test
	public void translationDialog_getMetadataBlocks__afterInit_translatedAgain() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		when(this.metadataTranslator.translate(anyMap(), any())).thenReturn(new HashMap<>());
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");
		this.tab.getTranslationDialog().getMetadataBlocks();
		this.tab.init(this.version, false);

		// when
		this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		verify(this.metadataTranslator, times(2)).translate(anyMap(), eq("pl"));
	}

	@Test
	public void translationDialog_getMetadataBlocks__translationFailed_returnsOriginal() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		when(this.metadataTranslator.translate(anyMap(), any()))
				.thenThrow(new UncheckedIOException(new IOException("service down")));
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");

		// when
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks =
				this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		assertThat(blocks).isEqualTo(this.tab.getMetadataBlocks());
	}

	@Test
	public void translationDialog_getMetadataBlocks__translationFailed_notRetriedUntilReselected() {
		// given
		this.visibleField.setValue("value");
		this.session.logIn(new AuthenticatedUser());
		this.tab.init(this.version, false);
		when(this.metadataTranslator.translate(anyMap(), any()))
				.thenThrow(new UncheckedIOException(new IOException("service down")))
				.thenReturn(new HashMap<>());
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");
		this.tab.getTranslationDialog().getMetadataBlocks();
		this.tab.getTranslationDialog().getMetadataBlocks();
		this.tab.getTranslationDialog().setSelectedLanguageCode("pl");

		// when
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks =
				this.tab.getTranslationDialog().getMetadataBlocks();

		// then
		assertThat(blocks).isEmpty();
		verify(this.metadataTranslator, times(2)).translate(anyMap(), eq("pl"));
	}

	private void VerifyBlockStructure() {
		
		List<Map.Entry<MetadataBlock, List<DatasetFieldsOfType>>> blocks = 
				this.tab.getMetadataBlocks();
		
		assertThat(blocks).hasSize(1);
		assertThat(blocks.get(0).getKey().getName()).isEqualTo("block");
		assertThat(blocks.get(0).getValue()).hasSize(2);
		
		assertThat(blocks.get(0).getValue().get(0).getType().getName()).isEqualTo("invisible");
		assertThat(blocks.get(0).getValue().get(1).getType().getName()).isEqualTo("visible");
	} 
}
