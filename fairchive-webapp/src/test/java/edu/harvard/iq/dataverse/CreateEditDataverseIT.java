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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;

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
import edu.harvard.iq.dataverse.dataverse.DataversePage;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.dataverse.FeaturedDataversesDialog;
import edu.harvard.iq.dataverse.dataverse.LinkToDataverseDialog;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.DataverseRoleRepository;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.users.SamlSessionRegistry;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@Transactional(TransactionMode.ROLLBACK)
public class CreateEditDataverseIT extends WebappArquillianDeployment {
	
	private final static String logoFileName = "example_no_text.jpg";
	
	@Inject
    private DataverseService dataverseService;
	@Inject 
	private DataverseRepository dataverseRepository;
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
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private DataverseRoleRepository roleRepository;
    @Inject
    private UserNotificationRepository userNotificationRepository;
    @Inject
    private ConfirmEmailServiceBean confirmEmailService;
    @Inject
    private SamlSessionRegistry samlSessionRegistry;
	
    private UIMessages ui = Mockito.mock(UIMessages.class);
	private CreateEditDataversePage createEditPage;
	private DataversePage dataversePage;
	private DataverseHeaderFragment headerFragment;
	
	private UploadedFile uploadedFile = Mockito.mock(UploadedFile.class);
	private InputStream fileContent;
	
	//--------------------------------------------------------------------------
	@BeforeEach
	void setUp() throws IOException {
		
		when(this.uploadedFile.getFileName()).thenReturn(logoFileName);
		when(this.uploadedFile.getInputStream()).then(invocation -> 
				this.fileContent = this.getClass().getResourceAsStream("/images/".concat(logoFileName)));
		
		this.createEditPage = newEditPage();
		
		this.session.setLocaleCode("EN");
		logInAdmin();
		publishRootDataverse();
	}
	//--------------------------------------------------------------------------
	@AfterEach
	void clean() throws IOException {
		if(this.fileContent != null) {
			this.fileContent.close();
		}
	}
	//--------------------------------------------------------------------------
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDraftVersion_andPublish_asAdminUser() throws IOException {
		
		// open edition page
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
		//verify default values
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
		
		// fill in values
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
		
		// save version
		assertThat(this.createEditPage.save()).
			isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias1");
		
		verify(this.ui, times(1)).addFlashSuccessMessage(anyString());
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
		
		// verify saved version
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isTrue();
		
		DataverseVersion storedVersion = stored.getLatestVersion();
		
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
		
		// open dataverse page	
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(stored.getId());
		this.dataversePage.init();
		
		dataverse = this.dataversePage.getDataverse();
		
		assertThat(dataverse.isInReview()).isFalse();
		
		// since dataverse was created by admin, it can be published
		assertThat(this.dataversePage.displaySubmitForReviewButton()).isFalse();
		assertThat(this.dataversePage.disabledSubmitForReviewButton()).isFalse();
		assertThat(this.dataversePage.displayPublishButton()).isTrue();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo("BBBBBB");
		assertThat(this.headerFragment.getLinkColorStyle()).contains("CCCCCC");
		assertThat(this.headerFragment.getTextColorStyle()).contains("DDDDDD");
		assertThat(this.headerFragment.getLinkUrl()).isEqualTo("http://google.com");
		assertThat(this.headerFragment.getTagLine()).isEqualTo("tagline");
		assertThat(this.headerFragment.getLogo()).contains(logoFileName);
		
		// publish dataverse
		this.dataversePage.releaseDataverse();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		// verify published dataverse
		stored = this.dataverseService.
				findById(this.dataversePage.getDataverse().getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isFalse();
		assertThat(stored.isReleased()).isTrue();
		assertThat(stored.isInReview()).isFalse();
		
		storedVersion = stored.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isFalse();
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
	//--------------------------------------------------------------------------
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editDraftVersion() throws Exception {
		
		// opne edition page
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
		// fill in values & save
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
		this.createEditPage.save();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		// open new edition page
		this.createEditPage = newEditPage();
		this.createEditPage.setDataverseId(dataverse.getId());
		this.createEditPage.init();
		
		// verify initial values as the ones previously saved
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
		assertThat(this.createEditPage.getBackgroundColor()).isEqualTo("BBBBBB");
		assertThat(this.createEditPage.getLinkColor()).isEqualTo("CCCCCC");
		assertThat(this.createEditPage.getTextColor()).isEqualTo("DDDDDD");
		assertThat(this.createEditPage.isLogoPresent()).isTrue();
		assertThat(this.createEditPage.getLogo()).isEqualTo(logoFileName);
		assertThat(this.createEditPage.getLogoPath()).endsWith(logoFileName);
		assertThat(this.createEditPage.getLogoAlignment()).isEqualTo(LEFT);
		assertThat(this.createEditPage.getLogoFormat()).isEqualTo(SQUARE);
		assertThat(this.createEditPage.getLinkUrl()).isEqualTo("http://google.com");
		assertThat(this.createEditPage.getTagline()).isEqualTo("tagline");
		
		dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		
		// change values and save again
		this.createEditPage.setName("name2");
		this.createEditPage.setAlias("alias2");
		this.createEditPage.setAffiliation("affiliation2");
		this.createEditPage.setType(DEPARTMENT);
		this.createEditPage.addContact(1);
		this.createEditPage.getContacts().get(1).setEmail("admin@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(false);
		this.createEditPage.setBackgroundColor("EEEEEE");
		this.createEditPage.setLinkColor("101010");
		this.createEditPage.setTextColor("FF00FF");
		this.createEditPage.setLinkUrl("http://amazon.com");
		this.createEditPage.setTagline("tagline2");
		this.createEditPage.removeLogo();
		
		assertThat(this.createEditPage.save()).
			isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		// verify that saved dataverse contains only one version with changed values
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
		assertThat(storedVersion.isMessagesBannersAllowed()).isFalse();
		assertThat(storedVersion.getContacts()).hasSize(2);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(storedVersion.getContacts().get(1).getEmail()).isEqualTo("admin@email.com");
		assertThat(storedVersion.getTheme().getBackgroundColor()).isEqualTo("EEEEEE");
		assertThat(storedVersion.getTheme().getLinkColor()).isEqualTo("101010");
		assertThat(storedVersion.getTheme().getTextColor()).isEqualTo("FF00FF");
		assertThat(storedVersion.getTheme().getLinkUrl()).isEqualTo("http://amazon.com");
		assertThat(storedVersion.getTheme().getTagline()).isEqualTo("tagline2");
		assertThat(storedVersion.getTheme().getLogo()).isNull();
		
		//TODO: verify what gets indexed by solr - see DataverseServiceIT
	}
	//--------------------------------------------------------------------------
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editReleasedVersion() {
		
		// open edition page
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
		// fill in values & save
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.save();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		Dataverse dataverse = this.createEditPage.getDataverse();
		
		// open dataverse page & publish
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		this.dataversePage.releaseDataverse();
		Timestamp publicationDate = this.dataversePage.getDataverse().getPublicationDate();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		// open new edition page
		this.createEditPage = newEditPage();
		this.createEditPage.setDataverseId(dataverse.getId());
		this.createEditPage.init();
		
		// verify initial values are the ones previously saved
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
		
		dataverse = this.createEditPage.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		//second version will be added after saving - so wer're looking at previous one
		assertThat(dataverse.getVersions()).hasSize(1); 
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isReleased()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo(DEFAULT_BACKGROUND_COLOR);
		assertThat(this.headerFragment.getLinkColorStyle()).contains(DEFAULT_LINK_COLOR);
		assertThat(this.headerFragment.getTextColorStyle()).contains(DEFAULT_TEXT_COLOR);
		assertThat(this.headerFragment.getLinkUrl()).isNull();
		assertThat(this.headerFragment.getTagLine()).isNull();
		assertThat(this.headerFragment.getLogo()).isNull();
		
		// modify values & save
		this.createEditPage.setName("name2");
		this.createEditPage.setAlias("alias2");
		this.createEditPage.setAffiliation("affiliation2");
		this.createEditPage.setType(DEPARTMENT);
		this.createEditPage.addContact(1);
		this.createEditPage.getContacts().get(1).setEmail("admin@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		
		assertThat(this.createEditPage.save()).
			isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
		// verify that there are two versions: draft and released
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
		
		// open dataverse page & publish
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		this.dataversePage.releaseDataverse();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		// verify that there are two released versions
		stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.isDraft()).isFalse();
		assertThat(stored.isFacetRoot()).isTrue();
		assertThat(stored.getPublicationDate()).isEqualTo(publicationDate);
		
		assertThat(stored.getVersions()).hasSize(2);
		assertThat(stored.getVersions().get(0).isReleased()).isTrue();
		assertThat(stored.getVersions().get(1).isReleased()).isTrue();
		
		releasedVersion = dataverse.getVersions().get(0);
		
		assertThat(releasedVersion.isDraft()).isFalse();
		assertThat(releasedVersion.getName()).isEqualTo("name1");
		assertThat(releasedVersion.getAlias()).isEqualTo("alias1");
		assertThat(releasedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(releasedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(releasedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(releasedVersion.getContacts()).hasSize(1);
		assertThat(releasedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		
		latestVersion = dataverse.getLatestVersion();
		
		assertThat(latestVersion.isDraft()).isFalse();
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
	//--------------------------------------------------------------------------
	@Test
	void rootCollection_cannotBeCreated() {
				
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
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
		
		this.createEditPage.getDataverse().setOwner(null);
		
		assertThat(this.createEditPage.save()).isEqualTo("");
		
		verify(this.ui, times(0)).addFlashSuccessMessage(anyString());
		verify(this.ui, times(1)).addErrorMessage(anyString(), anyString());
	}
	//--------------------------------------------------------------------------
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDraftVersion_asRegularUser_andSendForReview() throws IOException {
		
		logInAdmin();
		
		Dataverse root = this.dataverseRepository.getById(1L);
		root.setDefaultDataverseContributorRole(this.roleRepository.
				findByAlias("collectionCustodian").get());
		
		logInRegularUser();
		
		// create new child collection
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();
		
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
		this.createEditPage.save();
		
		// open dataverse page
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(this.createEditPage.getDataverse().getId());
		this.dataversePage.init();
		
		// verify initial values
		Dataverse dataverse = this.dataversePage.getDataverse();
		
		assertThat(dataverse.isInReview()).isFalse();
		assertThat(dataverse.isReleased()).isFalse();
		
		assertThat(this.dataversePage.displaySubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.disabledSubmitForReviewButton()).isFalse();
		assertThat(this.dataversePage.displayPublishButton()).isFalse();
		
		// send for review
		this.dataversePage.getSubmitForReviewDialog().clear();
		this.dataversePage.getSubmitForReviewDialog().setMessage("message1");
		this.dataversePage.getSubmitForReviewDialog().send();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		// verify updated button visibility
		assertThat(this.dataversePage.displaySubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.disabledSubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.displayPublishButton()).isFalse();
		
		assertThat(dataverse.isInReview()).isTrue();
		assertThat(dataverse.isReleased()).isFalse();
		
		// decline review - return collection to author
		logInAdmin();
		
		// open new dataverse page
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		
		// verify initial values
		dataverse = this.dataversePage.getDataverse();
		
		assertThat(this.dataversePage.displaySubmitForReviewButton()).isFalse();
		assertThat(this.dataversePage.displayPublishButton()).isTrue();
		assertThat(this.dataversePage.displayReturnToAuthorButton()).isTrue();
		
		// send back to contributor
		this.dataversePage.getSendBackToContributorDialog().clear();
		this.dataversePage.getSendBackToContributorDialog().setMessage("message1");
		this.dataversePage.getSendBackToContributorDialog().send();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		assertThat(dataverse.isInReview()).isFalse();
		assertThat(dataverse.isReleased()).isFalse();
		
		//send for review again
		logInRegularUser();
		
		// open new dataverse page
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(this.createEditPage.getDataverse().getId());
		this.dataversePage.init();

		dataverse = this.dataversePage.getDataverse();

		assertThat(this.dataversePage.displaySubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.disabledSubmitForReviewButton()).isFalse();
		assertThat(this.dataversePage.displayPublishButton()).isFalse();

		// submit for review again
		this.dataversePage.getSubmitForReviewDialog().clear();
		this.dataversePage.getSubmitForReviewDialog().setMessage("message1");
		this.dataversePage.getSubmitForReviewDialog().send();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());

		assertThat(this.dataversePage.displaySubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.disabledSubmitForReviewButton()).isTrue();
		assertThat(this.dataversePage.displayPublishButton()).isFalse();

		assertThat(dataverse.isInReview()).isTrue();
		assertThat(dataverse.isReleased()).isFalse();
		
		// publish
		logInAdmin();
		
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		
		assertThat(this.dataversePage.displayPublishButton()).isTrue();
		
		this.dataversePage.releaseDataverse();
		
		verify(this.ui, times(0)).addErrorMessage(anyString());
		
		assertThat(this.dataversePage.displayPublishButton()).isFalse();
		assertThat(this.dataversePage.displayReturnToAuthorButton()).isFalse();
		assertThat(this.dataversePage.displaySubmitForReviewButton()).isFalse();
		
		dataverse = this.dataversePage.getDataverse();
		
		assertThat(dataverse.isInReview()).isFalse();
		assertThat(dataverse.isReleased()).isTrue();
		assertThat(dataverse.hasDraft()).isFalse();
		assertThat(dataverse.getLatestVersion().isReleased()).isTrue();
		assertThat(dataverse.getLatestVersion().isDraft()).isFalse();
	}
	//--------------------------------------------------------------------------
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void releasedCollectionWithaDraft_displaysProperValuesOnPageHeader() throws IOException {

		// open edition page
		this.createEditPage.setOwnerId(1L);
		this.createEditPage.init();

		// fill in values & save
		this.createEditPage.setName("name1");
		this.createEditPage.setAlias("alias1");
		this.createEditPage.setAffiliation("affiliation1");
		this.createEditPage.setType(LABORATORY);
		this.createEditPage.getContacts().get(0).setEmail("user@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.save();

		Dataverse dataverse = this.createEditPage.getDataverse();

		// open dataverse page & publish
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		this.dataversePage.releaseDataverse();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		
		assertThat(this.headerFragment.getName()).isEqualTo("name1");
		assertThat(this.headerFragment.getAlias()).isEqualTo("alias1");
		assertThat(this.headerFragment.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo(DEFAULT_BACKGROUND_COLOR);
		assertThat(this.headerFragment.getLinkColorStyle()).contains(DEFAULT_LINK_COLOR);
		assertThat(this.headerFragment.getTextColorStyle()).contains(DEFAULT_TEXT_COLOR);
		assertThat(this.headerFragment.getLinkUrl()).isNull();
		assertThat(this.headerFragment.getTagLine()).isNull();
		assertThat(this.headerFragment.getLogo()).isNull();

		// open new edition page
		this.createEditPage = newEditPage();
		this.createEditPage.setDataverseId(dataverse.getId());
		this.createEditPage.init();

		// modify values & save
		this.createEditPage.setName("name2");
		this.createEditPage.setAlias("alias2");
		this.createEditPage.setAffiliation("affiliation2");
		this.createEditPage.setType(DEPARTMENT);
		this.createEditPage.addContact(1);
		this.createEditPage.getContacts().get(1).setEmail("admin@email.com");
		this.createEditPage.setInheritFacetFromParent(false);
		this.createEditPage.setMessagesBannersAllowed(true);
		this.createEditPage.setBackgroundColor("BBBBBB");
		this.createEditPage.setLinkColor("CCCCCC");
		this.createEditPage.setTextColor("DDDDDD");
		this.createEditPage.setLinkUrl("http://google.com");
		this.createEditPage.setTagline("tagline");
		this.createEditPage.handleImageFileUpload(
				new FileUploadEvent(new NullComponent(), this.uploadedFile));
		this.createEditPage.save();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		
		assertThat(this.headerFragment.getName()).isEqualTo("name2");
		assertThat(this.headerFragment.getAlias()).isEqualTo("alias2");
		assertThat(this.headerFragment.getAffiliation()).isEqualTo("affiliation2");
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo("BBBBBB");
		assertThat(this.headerFragment.getLinkColorStyle()).contains("CCCCCC");
		assertThat(this.headerFragment.getTextColorStyle()).contains("DDDDDD");
		assertThat(this.headerFragment.getLinkUrl()).isEqualTo("http://google.com");
		assertThat(this.headerFragment.getTagLine()).isEqualTo("tagline");
		assertThat(this.headerFragment.getLogo()).contains(logoFileName);
		
		logInRegularUser();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		
		assertThat(this.headerFragment.getName()).isEqualTo("name1");
		assertThat(this.headerFragment.getAlias()).isEqualTo("alias1");
		assertThat(this.headerFragment.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo(DEFAULT_BACKGROUND_COLOR);
		assertThat(this.headerFragment.getLinkColorStyle()).contains(DEFAULT_LINK_COLOR);
		assertThat(this.headerFragment.getTextColorStyle()).contains(DEFAULT_TEXT_COLOR);
		assertThat(this.headerFragment.getLinkUrl()).isNull();
		assertThat(this.headerFragment.getTagLine()).isNull();
		assertThat(this.headerFragment.getLogo()).isNull();
		
		logInAdmin();
		
		// open dataverse page & publish
		this.dataversePage = newDataversePage();
		this.dataversePage.setDataverseId(dataverse.getId());
		this.dataversePage.init();
		this.dataversePage.releaseDataverse();
		
		logInRegularUser();
		
		// verify header
		this.headerFragment = newHeaderFragment();
		this.headerFragment.initBreadcrumbs(dataverse);
		
		assertThat(this.headerFragment.getName()).isEqualTo("name2");
		assertThat(this.headerFragment.getAlias()).isEqualTo("alias2");
		assertThat(this.headerFragment.getAffiliation()).isEqualTo("affiliation2");
		assertThat(this.headerFragment.getBackgroundColor()).isEqualTo("BBBBBB");
		assertThat(this.headerFragment.getLinkColorStyle()).contains("CCCCCC");
		assertThat(this.headerFragment.getTextColorStyle()).contains("DDDDDD");
		assertThat(this.headerFragment.getLinkUrl()).isEqualTo("http://google.com");
		assertThat(this.headerFragment.getTagLine()).isEqualTo("tagline");
		assertThat(this.headerFragment.getLogo()).contains(logoFileName);
	}
	
	//--------------------------------------------------------------------------
	private void loginUserById(final Long id) {
		this.session.logOut();
		this.session.logIn(this.userRepository.getById(id));
	}
	//--------------------------------------------------------------------------
	private void logInAdmin() {
		loginUserById(1L);
	}
	//--------------------------------------------------------------------------
	private void logInRegularUser() {
		loginUserById(3L);
	}
	//--------------------------------------------------------------------------
	private CreateEditDataversePage newEditPage() {
		final PermissionsWrapper permissionsWrapper = newPermissionsWrapper();	
		return new CreateEditDataversePage(this.dataverseService,
				permissionsWrapper, this.session, this.config,
				this.storageManager, this.ui);
	}
	//--------------------------------------------------------------------------
	private DataversePage newDataversePage() {
	    final PermissionsWrapper permissionsWrapper = newPermissionsWrapper();	
		return new DataversePage(this.dataverseRepository, this.dataverseService,
				this.featuredDataverseService, this.permissionService , 
				permissionsWrapper, this.session, 
				Mockito.mock(SearchIncludeFragment.class),
				Mockito.mock(LinkToDataverseDialog.class),
				Mockito.mock(FeaturedDataversesDialog.class),
				this.config, this.ui);
	}
	//--------------------------------------------------------------------------
	private PermissionsWrapper newPermissionsWrapper() {
		final DataverseRequestServiceBean requestService = 
				new DataverseRequestServiceBean(this.session, null);
		final NavigationWrapper navigationWrapper = 
				Mockito.mock(NavigationWrapper.class);
	    return new PermissionsWrapper(this.permissionService, 
	    				requestService, navigationWrapper);
	}
	//--------------------------------------------------------------------------
	private DataverseHeaderFragment newHeaderFragment() {
		final PermissionsWrapper permissionsWrapper = newPermissionsWrapper();	
		return new DataverseHeaderFragment(this.dataverseService, permissionsWrapper, 
				this.config, Mockito.mock(DataFileServiceBean.class),
				this.session, Mockito.mock(NavigationWrapper.class),
				this.userNotificationRepository, this.confirmEmailService, 
				Mockito.mock(WidgetWrapper.class), this.samlSessionRegistry);
		
	}
	//--------------------------------------------------------------------------
	private void publishRootDataverse() {
		this.dataverseService.publish(this.dataverseService.findById(1L).get());
	}
}
