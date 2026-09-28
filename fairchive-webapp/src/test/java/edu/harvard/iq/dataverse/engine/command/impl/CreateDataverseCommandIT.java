package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType.DEPARTMENT;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.ADMIN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.COLLECTION_CUSTODIAN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.DEPOSITOR;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.DS_CONTRIBUTOR;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.permission.ManagePermissionsService;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

public class CreateDataverseCommandIT  extends WebappArquillianDeployment {
	
	@Inject
	private EjbDataverseEngine engine;
	@Inject
	private AuthenticatedUserRepository userRepository;
	@Inject
	private DataverseRepository dataverseRepository;
	@Inject
	private DataverseRoleServiceBean rolesService;
	@Inject 
	private ManagePermissionsService permissionsService;
	@Inject
	private DataverseSession session;
	@Inject
	private DatasetFieldTypeRepository fieldTypeRepository;
	
	private AuthenticatedUser admin;
	private AuthenticatedUser fileDownloader;
	private Dataverse owner;
	private Dataverse dataverse;
	
	@BeforeEach
	private void setUp() {
		
		this.admin = this.userRepository.findByIdentifier("dataverseAdmin").get();
		this.fileDownloader = this.userRepository.findByIdentifier("filedownloader").get();
		this.session.logIn(this.admin);
		this.owner = this.dataverseRepository.findById(68L).get();
		
		assertThat(this.rolesService.directRoleAssignments(this.owner)).isEmpty();
		
		assertThat(this.owner.getDefaultDatasetContributorRole().getAlias()).
			isEqualTo(EDITOR.getAlias());
		assertThat(this.owner.getDefaultDataverseContributorRole()).isNull();
		
		this.dataverse = new Dataverse();
		this.dataverse.setOwner(owner);
		this.dataverse.setName("test1");
		this.dataverse.setAlias(this.dataverse.getName());
		this.dataverse.setDataverseType(DEPARTMENT);
		final DataverseVersion version = this.dataverse.addVersion(RELEASED);
		version.setName(this.dataverse.getName());
		version.setAlias(this.dataverse.getName());
		version.setCreateTime(new Date());
		version.setDataverseType(this.dataverse.getDataverseType());
		version.setDescription("description");
		version.setAffiliation("affiliation1");
		version.getContacts().get(0).setEmail("abc@mail.com");
		version.setMessagesBannersAllowed(true);
	}
	
	@AfterEach
	private void tearDown() {
		this.session.logOut();
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void subXCllectionsHaveDefaultRoleAssignments_forVanillaOwner() throws Throwable {
		
		CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
				newRequest(), null, null);
		
		this.engine.submit(command);
		
		final List<RoleAssignment> assignedRoles = 
				this.rolesService.directRoleAssignments(this.dataverse);
		
		assertThat(assignedRoles).hasSize(1);
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@dataverseAdmin")
							&& assignment.getRole().getAlias().equals("admin"));
		
		assertThat(this.dataverse.getDefaultDatasetContributorRole().getAlias()).
			isEqualTo(EDITOR.getAlias());
		assertThat(this.dataverse.getDefaultDataverseContributorRole().getAlias()).
			isEqualTo(ADMIN.getAlias());
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void subCollectionsHaveRoleAssignments_forModifiedOwner() throws Throwable {
		
		this.owner.setDefaultDataverseContributorRole(
				this.rolesService.findBuiltinRoleByAlias(COLLECTION_CUSTODIAN));
		
		this.permissionsService.assignRoleWithNotification(
				this.rolesService.findBuiltinRoleByAlias(ADMIN),
				this.fileDownloader, this.owner);
		
		this.dataverseRepository.save(this.owner);
		
		CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
				newRequest(), null, null);
		
		this.engine.submit(command);
		
		final List<RoleAssignment> assignedRoles = 
				this.rolesService.directRoleAssignments(this.dataverse);
		
		assertThat(assignedRoles).hasSize(3);
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@dataverseAdmin")
							&& assignment.getRole().getAlias().equals(COLLECTION_CUSTODIAN.getAlias()));
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals(":authenticated-users")
							&& assignment.getRole().getAlias().equals(DS_CONTRIBUTOR.getAlias()));
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@filedownloader")
							&& assignment.getRole().getAlias().equals(ADMIN.getAlias()));
		
		
		assertThat(this.dataverse.getDefaultDatasetContributorRole().getAlias()).
			isEqualTo(DEPOSITOR.getAlias());
		assertThat(this.dataverse.getDefaultDataverseContributorRole().getAlias()).
			isEqualTo(COLLECTION_CUSTODIAN.getAlias());
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void subCollectionsHaveRoleAssignments_forModifiedOwner_andOrdinaryUser() throws Throwable {
		
		this.owner.setDefaultDataverseContributorRole(
				this.rolesService.findBuiltinRoleByAlias(COLLECTION_CUSTODIAN));
		
		this.permissionsService.assignRoleWithNotification(
				this.rolesService.findBuiltinRoleByAlias(ADMIN),
				this.fileDownloader, this.owner);
		
		this.dataverseRepository.save(this.owner);
		
		this.session.logOut();
		this.session.logIn(this.fileDownloader);
		
		CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
				newRequest(), null, null);
		
		this.engine.submit(command);
		
		final List<RoleAssignment> assignedRoles = 
				this.rolesService.directRoleAssignments(this.dataverse);
		
		assertThat(assignedRoles).hasSize(3);
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@dataverseAdmin")
							&& assignment.getRole().getAlias().equals(COLLECTION_CUSTODIAN.getAlias()));
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals(":authenticated-users")
							&& assignment.getRole().getAlias().equals(DS_CONTRIBUTOR.getAlias()));
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@filedownloader")
							&& assignment.getRole().getAlias().equals(ADMIN.getAlias()));
		
		
		assertThat(this.dataverse.getDefaultDatasetContributorRole().getAlias()).
			isEqualTo(DEPOSITOR.getAlias());
		assertThat(this.dataverse.getDefaultDataverseContributorRole().getAlias()).
			isEqualTo(COLLECTION_CUSTODIAN.getAlias());
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void subCollectionsHaveNoAdminRolesAssigned_forNoCollectionCustodian() throws Throwable {

		this.permissionsService.assignRoleWithNotification(
				this.rolesService.findBuiltinRoleByAlias(ADMIN),
				this.fileDownloader, this.owner);
		
		this.dataverseRepository.save(this.owner);
		
		CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
				newRequest(), null, null);
		
		this.engine.submit(command);
		
		final List<RoleAssignment> assignedRoles = 
				this.rolesService.directRoleAssignments(this.dataverse);
		
		assertThat(assignedRoles).hasSize(1);
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@dataverseAdmin")
							&& assignment.getRole().getAlias().equals(ADMIN.getAlias()));
		
		assertThat(this.dataverse.getDefaultDatasetContributorRole().getAlias()).
			isEqualTo(EDITOR.getAlias());
		assertThat(this.dataverse.getDefaultDataverseContributorRole().getAlias()).
			isEqualTo(ADMIN.getAlias());
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void aRegularUser_cannotAssignOnselfAnyRoles_toSubCollections() throws Throwable {
		
		this.owner.setDefaultDataverseContributorRole(
				this.rolesService.findBuiltinRoleByAlias(COLLECTION_CUSTODIAN));
		
		this.dataverseRepository.save(this.owner);
		
		this.session.logOut();
		this.session.logIn(this.fileDownloader);
		
		CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
				newRequest(), null, null);
		
		this.engine.submit(command);
		
		List<RoleAssignment> assignedRoles = 
				this.rolesService.directRoleAssignments(this.dataverse);
		
		assertThat(assignedRoles).hasSize(2);
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals("@dataverseAdmin")
							&& assignment.getRole().getAlias().equals(COLLECTION_CUSTODIAN.getAlias()));
		assertThat(assignedRoles).anyMatch(
				assignment -> assignment.getAssigneeIdentifier().equals(":authenticated-users")
							&& assignment.getRole().getAlias().equals(DS_CONTRIBUTOR.getAlias()));
		
		try {
			this.permissionsService.assignRoleWithNotification(
					this.rolesService.findBuiltinRoleByAlias(ADMIN),
					this.fileDownloader, this.dataverse);
			fail("Assingning role shall be impossible.");
		} catch(final PermissionException e) {
			assertThat(e.getMessage()).contains("ManageDataverse");
		}
	}
	
	@Test
	@Transactional(TransactionMode.ROLLBACK)
	void savingDataverse_works() throws Throwable {	
		
		try {
			final DataverseTheme theme = new DataverseTheme();
			theme.setBackgroundColor("#FFFFFF");
			theme.setLinkColor("#CCCCCC");
			
			this.dataverse.getVersions().get(0).setTheme(theme);
			theme.setVersion(this.dataverse.getVersions().get(0));
			
			CreateDataverseCommand command = new CreateDataverseCommand(this.dataverse, 
					newRequest(), newFacetList(), newInputLevelList());
			
			this.engine.submit(command);
			
			assertThat(this.dataverse.getId()).isNotNull();
			assertThat(this.dataverse.getVersions().get(0).getId()).isNotNull();
			assertThat(this.dataverse.getVersions().get(0).getFacets().get(0).getId()).isNotNull();
			assertThat(this.dataverse.getVersions().get(0).getFacets().get(1).getId()).isNotNull();
			assertThat(this.dataverse.getVersions().get(0).getDataverseFieldTypeInputLevels().get(0).getId()).isNotNull();
			assertThat(this.dataverse.getVersions().get(0).getDataverseFieldTypeInputLevels().get(1).getId()).isNotNull();
			
			this.dataverseRepository.clear();
			
			Dataverse stored = this.dataverseRepository.getById(this.dataverse.getId());
			
			assertThat(stored).isNotSameAs(this.dataverse);
			
			assertThat(stored.getName()).isEqualTo("test1");
			assertThat(stored.getOwner().getId()).isEqualTo(this.owner.getId());
			assertThat(stored.getAlias()).isEqualTo("test1");
			assertThat(stored.getDataverseType()).isEqualTo(DEPARTMENT);
			assertThat(stored.getVersions()).hasSize(1);
			
			DataverseVersion version = stored.getVersions().get(0);
			Date now = new Date();
			
			assertThat(stored.getLatestVersion(RELEASED)).contains(version);
			assertThat(stored.getLatestVersion(DRAFT)).isEmpty();
			assertThat(version.getName()).isEqualTo("test1");
			assertThat(version.getAlias()).isEqualTo("test1");
			assertThat(version.getDataverseType()).isEqualTo(DEPARTMENT);
			assertThat(version.getState()).isEqualTo(RELEASED);
			assertThat(version.getDescription()).isEqualTo("description");
			assertThat(version.getCreateTime()).isBefore(now);
			assertThat(version.getCreateTime()).isInSameMinuteWindowAs(now);
			assertThat(version.getAffiliation()).isEqualTo("affiliation1");
			assertThat(version.getDataverse()).isSameAs(stored);
			assertThat(version.getContacts()).hasSize(1);
			assertThat(version.getContacts().get(0).getEmail()).isEqualTo("abc@mail.com");
			assertThat(version.isMessagesBannersAllowed()).isTrue();
			assertThat(version.getFacets()).hasSize(2);
			assertThat(version.getFacets().get(0).getDatasetFieldType().getId()).isEqualTo(21L);
			assertThat(version.getFacets().get(0).getDisplayOrder()).isEqualTo(0);
			assertThat(version.getFacets().get(1).getDatasetFieldType().getId()).isEqualTo(81L);
			assertThat(version.getFacets().get(1).getDisplayOrder()).isEqualTo(1);
			assertThat(version.getDataverseFieldTypeInputLevels()).hasSize(2);
			assertThat(version.getDataverseFieldTypeInputLevels().get(0).getDatasetFieldType().getId()).isEqualTo(21L);
			assertThat(version.getDataverseFieldTypeInputLevels().get(0).isInclude()).isFalse();
			assertThat(version.getDataverseFieldTypeInputLevels().get(0).isRequired()).isTrue();
			assertThat(version.getDataverseFieldTypeInputLevels().get(1).getDatasetFieldType().getId()).isEqualTo(81L);
			assertThat(version.getDataverseFieldTypeInputLevels().get(1).isInclude()).isTrue();
			assertThat(version.getDataverseFieldTypeInputLevels().get(1).isRequired()).isFalse();
			assertThat(version.getTheme().getBackgroundColor()).isEqualTo("#FFFFFF");
			assertThat(version.getTheme().getLinkColor()).isEqualTo("#CCCCCC");
			
			
			//adding and removing contacts works
			
			version.addContact("123@mail.com");
			
			this.dataverseRepository.flush();
			
			Dataverse stored2 = this.dataverseRepository.getById(this.dataverse.getId());
			
			assertThat(stored2.getVersions().get(0).getContacts()).hasSize(2);
			
			stored2.getVersions().get(0).removeContact(0);
			
			this.dataverseRepository.flush();
			
			Dataverse stored3 = this.dataverseRepository.getById(this.dataverse.getId());
			
			assertThat(stored3.getVersions().get(0).getContacts()).hasSize(1);
			assertThat(stored3.getVersions().get(0).getContacts().get(0).getEmail()).isEqualTo("123@mail.com");
		} catch(final Exception e) {
			printConstraintViolations(e);
			throw e;
		}
	}
	
	private DataverseRequest newRequest() {
		
		final HttpServletRequest httpRequest = null;
		return  new DataverseRequest(this.admin, httpRequest);
	}
	
	
	private List<DatasetFieldType> newFacetList() {
		
		final ArrayList<DatasetFieldType> list = new ArrayList<>();
		
		list.add(this.fieldTypeRepository.getById(21L));
		list.add(this.fieldTypeRepository.getById(81L));
		
		return list;
	}
	
	private List<DataverseFieldTypeInputLevel> newInputLevelList() {
		
		final ArrayList<DataverseFieldTypeInputLevel> list = new ArrayList<>();
		
		DataverseFieldTypeInputLevel level1 = new DataverseFieldTypeInputLevel();
		level1.setDatasetFieldType(this.fieldTypeRepository.getById(21L));
		level1.setInclude(false);
		level1.setRequired(true);
		list.add(level1);
		
		DataverseFieldTypeInputLevel level2 = new DataverseFieldTypeInputLevel();
		level2.setDatasetFieldType(this.fieldTypeRepository.getById(81L));
		level2.setInclude(true);
		level2.setRequired(false);
		list.add(level2);
	
		return list;
	}
	
	private static void printConstraintViolations(Throwable e) {
		while(e != null && ! (e instanceof ConstraintViolationException)) {
			e = e.getCause();
		}
		
		if(e != null) {
			System.out.println("!=============== constraint violations");
			System.out.println(((ConstraintViolationException) e).getConstraintViolations());
		}
	}
}
