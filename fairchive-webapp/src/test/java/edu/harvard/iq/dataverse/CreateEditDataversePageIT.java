package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.LABORATORY;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.DEPARTMENT;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.UNCATEGORIZED;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.dataverse.CreateEditDataversePage;
import edu.harvard.iq.dataverse.dataverse.DataverseService;
import edu.harvard.iq.dataverse.dataverse.MetadataBlockService;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@Transactional(TransactionMode.ROLLBACK)
public class CreateEditDataversePageIT extends WebappArquillianDeployment {
	
	@Inject
    private DatasetFieldServiceBean datasetFieldService;
	@Inject
    private MetadataBlockService metadataBlockService;
	@Inject
    private DataverseService dataverseService;
	@Inject
	private PermissionServiceBean permissionService;
	@Inject
    private DataverseSession session;
	@Inject
    private SystemConfig config;
	@Inject
	private AuthenticatedUserRepository userRepository;
	
	private DataverseRequestServiceBean requestService;
	private NavigationWrapper navigationWrapper;
    private PermissionsWrapper permissionsWrapper;
    private UIMessages ui;
	private CreateEditDataversePage page;
	
	@BeforeEach
	void setUp() {
		
		this.requestService = new DataverseRequestServiceBean(this.session, null);
		this.navigationWrapper = Mockito.mock(NavigationWrapper.class);
		this.permissionsWrapper = new PermissionsWrapper(this.permissionService, 
				this.requestService, this.navigationWrapper);
		this.ui = Mockito.mock(UIMessages.class);
		
		this.page = newPage();
		
		this.session.setLocaleCode("EN");
		loginUserById(1L); // admin
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDataverseDraft() {
		
		this.page.setOwnerId(1L);
		this.page.init();
		
		assertThat(this.page.getTitle()).isEqualTo("New Collection");
		assertThat(this.page.getName()).isEmpty();
		assertThat(this.page.getAlias()).isEmpty();
		assertThat(this.page.getDescription()).isNull();
		assertThat(this.page.getAdditionalDescription()).isNull();
		assertThat(this.page.getAffiliation()).isNull();
		assertThat(this.page.getType()).isEqualTo(UNCATEGORIZED);
		assertThat(this.page.isInheritFacetFromParent()).isTrue();
		assertThat(this.page.isInheritMetaBlocksFromParent()).isTrue();
		assertThat(this.page.isMessagesBannersAllowed()).isFalse();
		assertThat(this.page.canUserPublishCollection()).isTrue();
		assertThat(this.page.getFacets().getSource()).isNotEmpty();
		assertThat(this.page.getFacets().getTarget()).isEmpty();
		
		Dataverse dataverse = this.page.getDataverse();
		
		assertThat(dataverse.isNew()).isTrue();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		assertThat(dataverse.isFacetRoot()).isFalse();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isEmpty();
		
		this.page.setName("name1");
		this.page.setAlias("alias1");
		this.page.setAffiliation("affiliation1");
		this.page.setType(LABORATORY);
		this.page.getContacts().get(0).setEmail("user@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.getFacets().getTarget().add(this.page.getFacets().getSource().get(0));
		this.page.setMessagesBannersAllowed(true);
		
		assertThat(this.page.save()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias1");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isTrue();
		assertThat(stored.isFacetRoot()).isTrue();
		
		DataverseVersion storedVersion = dataverse.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isTrue();
		assertThat(storedVersion.getName()).isEqualTo("name1");
		assertThat(storedVersion.getAlias()).isEqualTo("alias1");
		assertThat(storedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(storedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(storedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(storedVersion.getContacts()).hasSize(1);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(storedVersion.getFacets()).hasSize(1);
		assertThat(storedVersion.getFacets().get(0).getDatasetFieldType().getName()).
			isEqualTo("authorName");
		
		//TODO: verify what gets indexed by solr
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void createNewDataverseReleased() {
		
		this.page.setOwnerId(1L);
		this.page.init();
		
		assertThat(this.page.getTitle()).isEqualTo("New Collection");
		assertThat(this.page.getName()).isEmpty();
		assertThat(this.page.getAlias()).isEmpty();
		assertThat(this.page.getDescription()).isNull();
		assertThat(this.page.getAdditionalDescription()).isNull();
		assertThat(this.page.getAffiliation()).isNull();
		assertThat(this.page.getType()).isEqualTo(UNCATEGORIZED);
		assertThat(this.page.isInheritFacetFromParent()).isTrue();
		assertThat(this.page.isInheritMetaBlocksFromParent()).isTrue();
		assertThat(this.page.isMessagesBannersAllowed()).isFalse();
		assertThat(this.page.canUserPublishCollection()).isTrue();
		assertThat(this.page.getFacets().getSource()).isNotEmpty();
		assertThat(this.page.getFacets().getTarget()).isEmpty();
		
		Dataverse dataverse = this.page.getDataverse();
		
		assertThat(dataverse.isNew()).isTrue();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		assertThat(dataverse.isFacetRoot()).isFalse();
		
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isEmpty();
		
		this.page.setName("name1");
		this.page.setAlias("alias1");
		this.page.setAffiliation("affiliation1");
		this.page.setType(LABORATORY);
		this.page.getContacts().get(0).setEmail("user@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.getFacets().getTarget().add(this.page.getFacets().getSource().get(0));
		this.page.setMessagesBannersAllowed(true);
		
		assertThat(this.page.saveAndRelease()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias1");
		
		Dataverse stored = this.dataverseService.findById(dataverse.getId()).get();
		
		assertThat(stored.isNew()).isFalse();
		assertThat(stored.getOwner().getId()).isEqualTo(1L);
		assertThat(stored.getVersions()).hasSize(1);
		assertThat(stored.isDraft()).isFalse();
		assertThat(stored.isFacetRoot()).isTrue();
		
		DataverseVersion storedVersion = dataverse.getLatestVersion();
		
		assertThat(storedVersion.isDraft()).isFalse();
		assertThat(storedVersion.getName()).isEqualTo("name1");
		assertThat(storedVersion.getAlias()).isEqualTo("alias1");
		assertThat(storedVersion.getAffiliation()).isEqualTo("affiliation1");
		assertThat(storedVersion.getDataverseType()).isEqualTo(LABORATORY);
		assertThat(storedVersion.isMessagesBannersAllowed()).isTrue();
		assertThat(storedVersion.getContacts()).hasSize(1);
		assertThat(storedVersion.getContacts().get(0).getEmail()).isEqualTo("user@email.com");
		assertThat(storedVersion.getFacets()).hasSize(1);
		assertThat(storedVersion.getFacets().get(0).getDatasetFieldType().getName()).
			isEqualTo("authorName");
		
		//TODO: verify what gets indexed by solr
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editDataverseDraft() {
		
		this.page.setOwnerId(1L);
		this.page.init();
		this.page.setName("name1");
		this.page.setAlias("alias1");
		this.page.setAffiliation("affiliation1");
		this.page.setType(LABORATORY);
		this.page.getContacts().get(0).setEmail("user@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.getFacets().getTarget().add(this.page.getFacets().getSource().get(0));
		this.page.setMessagesBannersAllowed(true);
		this.page.save();
		Dataverse dataverse = this.page.getDataverse();
		
		this.page = newPage();
		this.page.setDataverseId(dataverse.getId());
		this.page.init();
		
		assertThat(this.page.getTitle()).isEqualTo("Edit Collection - name1");
		assertThat(this.page.getName()).isEqualTo("name1");
		assertThat(this.page.getAlias()).isEqualTo("alias1");
		assertThat(this.page.getDescription()).isNull();
		assertThat(this.page.getAdditionalDescription()).isNull();
		assertThat(this.page.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.page.getType()).isEqualTo(LABORATORY);
		assertThat(this.page.isInheritFacetFromParent()).isFalse();
		assertThat(this.page.isInheritMetaBlocksFromParent()).isTrue();
		assertThat(this.page.isMessagesBannersAllowed()).isTrue();
		assertThat(this.page.canUserPublishCollection()).isTrue();
		assertThat(this.page.getFacets().getSource()).isNotEmpty();
		assertThat(this.page.getFacets().getTarget()).hasSize(1);
		assertThat(this.page.getFacets().getTarget().get(0).getName()).isEqualTo("authorName");
		
		dataverse = this.page.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		assertThat(dataverse.getVersions()).hasSize(1);
		assertThat(dataverse.isDraft()).isTrue();
		assertThat(dataverse.isFacetRoot()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isDraft()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		this.page.setName("name2");
		this.page.setAlias("alias2");
		this.page.setAffiliation("affiliation2");
		this.page.setType(DEPARTMENT);
		this.page.addContact(1);
		this.page.getContacts().get(1).setEmail("admin@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.setMessagesBannersAllowed(true);
		
		
		assertThat(this.page.save()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
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
		assertThat(storedVersion.getFacets()).hasSize(1);
		assertThat(storedVersion.getFacets().get(0).getDatasetFieldType().getName()).
			isEqualTo("authorName");
		
		//TODO: verify what gets indexed by solr
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void editDataverseRelease() {
		
		this.page.setOwnerId(1L);
		this.page.init();
		this.page.setName("name1");
		this.page.setAlias("alias1");
		this.page.setAffiliation("affiliation1");
		this.page.setType(LABORATORY);
		this.page.getContacts().get(0).setEmail("user@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.getFacets().getTarget().add(this.page.getFacets().getSource().get(0));
		this.page.setMessagesBannersAllowed(true);
		this.page.saveAndRelease();
		Dataverse dataverse = this.page.getDataverse();
		
		this.page = newPage();
		this.page.setDataverseId(dataverse.getId());
		this.page.init();
		
		assertThat(this.page.getTitle()).isEqualTo("Edit Collection - name1");
		assertThat(this.page.getName()).isEqualTo("name1");
		assertThat(this.page.getAlias()).isEqualTo("alias1");
		assertThat(this.page.getDescription()).isNull();
		assertThat(this.page.getAdditionalDescription()).isNull();
		assertThat(this.page.getAffiliation()).isEqualTo("affiliation1");
		assertThat(this.page.getType()).isEqualTo(LABORATORY);
		assertThat(this.page.isInheritFacetFromParent()).isFalse();
		assertThat(this.page.isInheritMetaBlocksFromParent()).isTrue();
		assertThat(this.page.isMessagesBannersAllowed()).isTrue();
		assertThat(this.page.canUserPublishCollection()).isTrue();
		assertThat(this.page.getFacets().getSource()).isNotEmpty();
		assertThat(this.page.getFacets().getTarget()).hasSize(1);
		assertThat(this.page.getFacets().getTarget().get(0).getName()).isEqualTo("authorName");
		
		dataverse = this.page.getDataverse();
		
		assertThat(dataverse.isNew()).isFalse();
		assertThat(dataverse.getOwner().getId()).isEqualTo(1L);
		//second version will be added after saving - so wer're looking at previous one
		assertThat(dataverse.getVersions()).hasSize(1); 
		assertThat(dataverse.isFacetRoot()).isTrue();
		
		DataverseVersion version = dataverse.getLatestVersion();
		
		assertThat(version.isReleased()).isTrue();
		assertThat(version.getContacts()).hasSize(1);
		assertThat(version.getContacts().get(0).getEmail()).isNotEmpty();
		
		this.page.setName("name2");
		this.page.setAlias("alias2");
		this.page.setAffiliation("affiliation2");
		this.page.setType(DEPARTMENT);
		this.page.addContact(1);
		this.page.getContacts().get(1).setEmail("admin@email.com");
		this.page.setInheritFacetFromParent(false);
		this.page.setMessagesBannersAllowed(true);
		
		
		assertThat(this.page.save()).isEqualTo("/dataverse.xhtml?faces-redirect=true&alias=alias2");
		
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
		assertThat(releasedVersion.getFacets()).hasSize(1);
		assertThat(releasedVersion.getFacets().get(0).getDatasetFieldType().getName()).
			isEqualTo("authorName");
		
		
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
		assertThat(latestVersion.getFacets()).hasSize(1);
		assertThat(latestVersion.getFacets().get(0).getDatasetFieldType().getName()).
			isEqualTo("authorName");
		
		//TODO: verify what gets indexed by solr
	}
	
	private void loginUserById(final Long id) {
		this.session.logOut();
		this.session.logIn(this.userRepository.getById(id));
	}
	
	private CreateEditDataversePage newPage() {
		return new CreateEditDataversePage(this.dataverseService, 
				this.datasetFieldService, this.metadataBlockService, 
				this.permissionsWrapper, this.session, this.config, this.ui);
	}
}
