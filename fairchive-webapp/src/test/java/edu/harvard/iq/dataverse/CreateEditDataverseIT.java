package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.DEPARTMENT;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.LABORATORY;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.UNCATEGORIZED;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.DEFAULT_BACKGROUND_COLOR;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.DEFAULT_LINK_COLOR;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.DEFAULT_TEXT_COLOR;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.Alignment.LEFT;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.ImageFormat.SQUARE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.ocpsoft.pretty.faces.util.NullComponent;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataverse.CreateEditDataversePage;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.dataverse.themewidget.ThemeWidgetService;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@Transactional(TransactionMode.ROLLBACK)
public class CreateEditDataverseIT extends WebappArquillianDeployment {
	
	private final static String logoFileName = "example_no_text.jpg";
	
	@Inject
    private DataverseService dataverseService;
	@Inject
	private ThemeWidgetService themeWidgetService;
	@Inject
	private PermissionServiceBean permissionService;
	@Inject
    private DataverseSession session;
	@Inject
    private SystemConfig config;
	@Inject
	private AuthenticatedUserRepository userRepository;
    @Inject
    private TempLogoStorageManager storageManager;
	
	private DataverseRequestServiceBean requestService;
	private NavigationWrapper navigationWrapper;
    private PermissionsWrapper permissionsWrapper;
    private UIMessages ui = Mockito.mock(UIMessages.class);
	private CreateEditDataversePage createEditPage;
	
	private UploadedFile uploadedFile = Mockito.mock(UploadedFile.class);
	private InputStream fileContent;
	
	private 
	
	@BeforeEach
	void setUp() throws IOException {
		
		when(this.uploadedFile.getFileName()).thenReturn(logoFileName);
		when(this.uploadedFile.getInputStream()).then(invocation -> 
				this.fileContent = this.getClass().getResourceAsStream("/images/".concat(logoFileName)));
		
		this.requestService = new DataverseRequestServiceBean(this.session, null);
		this.navigationWrapper = Mockito.mock(NavigationWrapper.class);
		this.permissionsWrapper = new PermissionsWrapper(this.permissionService, 
				this.requestService, this.navigationWrapper);
		
		this.createEditPage = newPage();
		
		this.session.setLocaleCode("EN");
		loginUserById(1L); // admin
	}
	
	@AfterEach
	void clean() throws IOException {
		if(this.fileContent != null) {
			this.fileContent.close();
		}
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDataverseDraft() throws IOException {
		
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
		assertThat(this.createEditPage.getTitle()).isEqualTo("New Collection");
		assertThat(this.createEditPage.getName()).isEmpty();
		assertThat(this.createEditPage.getAlias()).isEmpty();
		assertThat(this.createEditPage.getDescription()).isNull();
		assertThat(this.createEditPage.getAdditionalDescription()).isNull();
		assertThat(this.createEditPage.getAffiliation()).isNull();
		assertThat(this.createEditPage.getType()).isEqualTo(UNCATEGORIZED);
		assertThat(this.createEditPage.isInheritFacetFromParent()).isTrue();
		assertThat(this.createEditPage.isMessagesBannersAllowed()).isFalse();
		assertThat(this.createEditPage.canUserPublishCollection()).isTrue();
		assertThat(this.createEditPage.getInheritCustomization()).isFalse();
		assertThat(this.createEditPage.getBackgroundColor()).isEqualTo(DEFAULT_BACKGROUND_COLOR);
		assertThat(this.createEditPage.getLinkColor()).isEqualTo(DEFAULT_LINK_COLOR);
		assertThat(this.createEditPage.getTextColor()).isEqualTo(DEFAULT_TEXT_COLOR);
		assertThat(this.createEditPage.isLogoPresent()).isFalse();
		assertThat(this.createEditPage.getLogo()).isNull();
		assertThat(this.createEditPage.getLogoPath()).isEmpty();
		assertThat(this.createEditPage.getLogoAlignment()).isEqualTo(LEFT);
		assertThat(this.createEditPage.getLogoFormat()).isEqualTo(SQUARE);
		assertThat(this.createEditPage.getLinkUrl()).isNull();
		assertThat(this.createEditPage.getTagline()).isNull();
		
		
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isTrue();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isEmpty();
		assertThat(version.getTheme()).isNotNull();
		assertThat(version.getTheme().getBackgroundColor()).isEqualTo(DEFAULT_BACKGROUND_COLOR);
		assertThat(version.getTheme().getLinkColor()).isEqualTo(DEFAULT_LINK_COLOR);
		assertThat(version.getTheme().getTextColor()).isEqualTo(DEFAULT_TEXT_COLOR);
		assertThat(version.getTheme().getLinkUrl()).isNull();
		assertThat(version.getTheme().getLogo()).isNull();
		assertThat(version.getTheme().getLogoAlignment()).isEqualTo(LEFT);
		assertThat(version.getTheme().getLogoFormat()).isEqualTo(SQUARE);
		assertThat(version.getTheme().getTagline()).isNull();
		assertThat(version.getTheme().getVersion()).isSameAs(version);
		
		
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.setBackgroundColor("BBBBBB");
		this.createEditPage.setLinkColor("CCCCCC");
		this.createEditPage.setTextColor("DDDDDD");
		this.createEditPage.setLinkUrl("http://google.com");
		this.createEditPage.setTagline("tagline");
		
		this.createEditPage.handleImageFileUpload(
				new FileUploadEvent(new NullComponent(), this.uploadedFile));
		
		assertThat(this.createEditPage.isLogoPresent()).isTrue();
		assertThat(this.createEditPage.getLogo()).isEqualTo(logoFileName);
		
		
		assertThat(this.createEditPage.save()).
			isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias1");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isTrue();
		
		DataverseVersion storedVersion = dataverse.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isTrue();
		assertThat(storedVersion.getName()).isEqualTo("name1");
		assertThat(storedVersion.getAlias()).isEqualTo("alias1");
		assertThat(storedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(storedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(storedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(storedVersion.getContacts()).hasSize(1);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(storedVersion.getTheme().getBackgroundColor()).isEqualTo("BBBBBB");
		assertThat(storedVersion.getTheme().getLinkColor()).isEqualTo("CCCCCC");
		assertThat(storedVersion.getTheme().getTextColor()).isEqualTo("DDDDDD");
		assertThat(storedVersion.getTheme().getLinkUrl()).isEqualTo("http://google.com");
		assertThat(storedVersion.getTheme().getTagline()).isEqualTo("tagline");
		assertThat(storedVersion.getTheme().getLogo()).isEqualTo(logoFileName);
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDataverseReleased() {
		
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
		assertThat(this.createEditPage.getTitle()).isEqualTo("New Collection");
		assertThat(this.createEditPage.getName()).isEmpty();
		assertThat(this.createEditPage.getAlias()).isEmpty();
		assertThat(this.createEditPage.getDescription()).isNull();
		assertThat(this.createEditPage.getAdditionalDescription()).isNull();
		assertThat(this.createEditPage.getAffiliation()).isNull();
		assertThat(this.createEditPage.getType()).isEqualTo(UNCATEGORIZED);
		assertThat(this.createEditPage.isInheritFacetFromParent()).isTrue();
		assertThat(this.createEditPage.isMessagesBannersAllowed()).isFalse();
		assertThat(this.createEditPage.canUserPublishCollection()).isTrue();
		assertThat(this.createEditPage.getInheritCustomization()).isFalse();
		
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isTrue();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isEmpty();
		
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		
		assertThat(this.createEditPage.saveAndRelease()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias1");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isFalse();
		
		DataverseVersion storedVersion = dataverse.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isFalse();
		assertThat(storedVersion.getName()).isEqualTo("name1");
		assertThat(storedVersion.getAlias()).isEqualTo("alias1");
		assertThat(storedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(storedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(storedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(storedVersion.getContacts()).hasSize(1);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editDataverseDraft() {
		
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.save();
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		this.createEditPage = newPage();
		this.createEditPage.setDataverseId(dataverse.getId());
		this.createEditPage.init();
		
		assertThat(this.createEditPage.getTitle()).isEqualTo("Edit Collection - name1");
		assertThat(this.createEditPage.getName()).isEqualTo("name1");
		assertThat(this.createEditPage.getAlias()).isEqualTo("alias1");
		assertThat(this.createEditPage.getDescription()).isNull();
		assertThat(this.createEditPage.getAdditionalDescription()).isNull();
		assertThat(this.createEditPage.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.createEditPage.getType()).isEqualTo(LABORATORY);
		assertThat(this.createEditPage.isInheritFacetFromParent()).isFalse();
		assertThat(this.createEditPage.isMessagesBannersAllowed()).isTrue();
		assertThat(this.createEditPage.canUserPublishCollection()).isTrue();
		assertThat(this.createEditPage.getInheritCustomization()).isFalse();
		
		dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		this.createEditPage.setName("name2");
		this.createEditPage.setAlias("alias2");
		this.createEditPage.setAffiliation("affiliation2");
		this.createEditPage.setType(DEPARTMENT);
		this.createEditPage.addContact(1);
		this.createEditPage.getContacts().get(1).setEmail("admin@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		
		
		assertThat(this.createEditPage.save()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isTrue();
		assertThat(stored.isFacetRoot()).isTrue();
		
		DataverseVersion storedVersion = dataverse.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isTrue();
		assertThat(storedVersion.getName()).isEqualTo("name2");
		assertThat(storedVersion.getAlias()).isEqualTo("alias2");
		assertThat(storedVersion.getAffiliation()).isEqualTo("affiliation2");
		assertThat(storedVersion.getDataverseType()).isEqualTo(DEPARTMENT);
		assertThat(storedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(storedVersion.getContacts()).hasSize(2);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(storedVersion.getContacts().get(1).getEmail()).isEqualTo("admin@email.com");
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editDataverseRelease() {
		
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.saveAndRelease();
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		this.createEditPage = newPage();
		this.createEditPage.setDataverseId(dataverse.getId());
		this.createEditPage.init();
		
		assertThat(this.createEditPage.getTitle()).isEqualTo("Edit Collection - name1");
		assertThat(this.createEditPage.getName()).isEqualTo("name1");
		assertThat(this.createEditPage.getAlias()).isEqualTo("alias1");
		assertThat(this.createEditPage.getDescription()).isNull();
		assertThat(this.createEditPage.getAdditionalDescription()).isNull();
		assertThat(this.createEditPage.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.createEditPage.getType()).isEqualTo(LABORATORY);
		assertThat(this.createEditPage.isInheritFacetFromParent()).isFalse();
		assertThat(this.createEditPage.isMessagesBannersAllowed()).isTrue();
		assertThat(this.createEditPage.canUserPublishCollection()).isTrue();
		assertThat(this.createEditPage.getInheritCustomization()).isFalse();
		
		dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		//second version will be added after saving - so wer're looking at previous one
		assertThat(dataverse.getVersions()).hasSize(1); 
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isReleased()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		this.createEditPage.setName("name2");
		this.createEditPage.setAlias("alias2");
		this.createEditPage.setAffiliation("affiliation2");
		this.createEditPage.setType(DEPARTMENT);
		this.createEditPage.addContact(1);
		this.createEditPage.getContacts().get(1).setEmail("admin@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		
		
		assertThat(this.createEditPage.save()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.isDraft()).isTrue();
		assertThat(stored.isFacetRoot()).isTrue();
		
		assertThat(stored.getVersions()).hasSize(2);
		assertThat(stored.getVersions().get(0).isReleased()).isTrue();
		assertThat(stored.getVersions().get(1).isDraft()).isTrue();
		
		DataverseVersion releasedVersion = dataverse.getVersions().get(0);
		
		assertThat(releasedVersion.isDraft()).isFalse();
		assertThat(releasedVersion.getName()).isEqualTo("name1");
		assertThat(releasedVersion.getAlias()).isEqualTo("alias1");
		assertThat(releasedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(releasedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(releasedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(releasedVersion.getContacts()).hasSize(1);
		assertThat(releasedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		
		DataverseVersion latestVersion = dataverse.getLatestVersion();
		
		assertThat(latestVersion.isDraft()).isTrue();
		assertThat(latestVersion.getName()).isEqualTo("name2");
		assertThat(latestVersion.getAlias()).isEqualTo("alias2");
		assertThat(latestVersion.getAffiliation()).isEqualTo("affiliation2");	
		assertThat(latestVersion.getDataverseType()).isEqualTo(DEPARTMENT);
		assertThat(latestVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(latestVersion.getContacts()).hasSize(2);
		assertThat(latestVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(latestVersion.getContacts().get(1).getEmail()).isEqualTo("admin@email.com");
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
	}
	
	private void loginUserById(final Long id) {
		this.session.logOut();
		this.session.logIn(this.userRepository.getById(id));
	}
	
	private CreateEditDataversePage newPage() {
		return new CreateEditDataversePage(this.dataverseService, this.themeWidgetService,
				this.permissionsWrapper, this.session, this.config,
				this.storageManager, this.ui);
	}
}
