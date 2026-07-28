package edu.harvard.iq.dataverse.persistence.user;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;

public class DataverseRoleRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private DataverseRoleRepository repository;

    //-------------------- TESTS --------------------

    @Test
    public void findByOwnerId() {

        List<String> roleNames = this.repository.findByOwnerId(51L)
        		.stream()
        		.map(DataverseRole::getAlias)
        		.collect(toList());

        assertThat(roleNames, containsInAnyOrder("unreleased_dv_test_role"));
    }

    @Test
    public void findWithoutOwner() {

        List<String> roleNames = this.repository.findWithoutOwner()
        		.stream()
        		.map(DataverseRole::getAlias)
        		.collect(toList());

        assertThat(roleNames, containsInAnyOrder(builtInRoleNames()));
    }

    @Test
    public void findByAlias() {

        assertEquals("admin", this.repository.findByAlias("admin").get().getAlias());
    }

    @Test
    public void findByAlias_no_such_role() {

        assertFalse(this.repository.findByAlias("not_existing_role_alias").isPresent());
    }
    
    @Test
    public void builtInRolesExist() {
    	
    	List<String> roleNames = this.repository.findAll()
    			.stream()
    			.map(DataverseRole::getAlias)
    			.collect(toList());
    	
    	assertEquals(11, roleNames.size());
    	assertTrue(roleNames.contains("admin"));
    	assertTrue(roleNames.contains("fileDownloader"));
    	assertTrue(roleNames.contains("fullContributor"));
    	assertTrue(roleNames.contains("dvContributor"));
    	assertTrue(roleNames.contains("dsContributor"));
    	assertTrue(roleNames.contains("editor"));
    	assertTrue(roleNames.contains("curator"));
    	assertTrue(roleNames.contains("member"));
    	assertTrue(roleNames.contains("depositor"));
    	assertTrue(roleNames.contains("collectionCustodian"));
    }
    
	@Test
	public void custodianHasProperPermissions() {

		DataverseRole role = this.repository.findByAlias("collectionCustodian").get();

		assertTrue(role.has(Permission.DeleteDataverse));
		assertTrue(role.has(Permission.EditDataverse));
		assertTrue(role.has(Permission.ViewUnpublishedDataverse));
		
		assertFalse(role.has(Permission.AddDataset));
		assertFalse(role.has(Permission.AddDataverse));
		assertFalse(role.has(Permission.DeleteDatasetDraft));
		assertFalse(role.has(Permission.DownloadFile));
		assertFalse(role.has(Permission.EditDataset));
		assertFalse(role.has(Permission.ManageDatasetPermissions));
		assertFalse(role.has(Permission.ManageDataversePermissions));
		assertFalse(role.has(Permission.ManageMinorDatasetPermissions));
		assertFalse(role.has(Permission.PublishDataset));
		assertFalse(role.has(Permission.PublishDataverse));
		assertFalse(role.has(Permission.ViewUnpublishedDataset));
		assertFalse(role.has(Permission.AddDataset));
	}
	
	private static Object[] builtInRoleNames() {
		return stream(BuiltInRole.values())
                .map(BuiltInRole::getAlias)
                .collect(toList())
                .toArray();
	}
}
