package edu.harvard.iq.dataverse.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

public class DataverseRoleTest {

	@Test
	@SuppressWarnings("unlikely-arg-type")
	void equals() {

		DataverseRole role1 = new DataverseRole();
		DataverseRole role2 = new DataverseRole();

		assertThat(role1.equals(null)).isFalse();
		assertThat(role1.equals("")).isFalse();
		assertThat(role1.equals(role2)).isTrue();
		
		assertThat(role1.equals(role2)).isTrue();
		
		role1.setId(1L);
		assertThat(role1.equals(role2)).isFalse();
		
		role2.setId(1L);
		assertThat(role1.equals(role2)).isTrue();
	}
	
    @Test
    void permissionsManagement() {
    	
    	DataverseRole role = new DataverseRole();
    	
    	assertThat(role.permissions()).isEmpty();
    	assertThat(role.hasPermissionFor(Dataverse.class)).isFalse();
    	assertThat(role.hasPermissionFor(Dataset.class)).isFalse();
    	assertThat(role.has(Permission.AddDataset)).isFalse();
    	assertThat(role.has(Permission.DeleteDatasetDraft)).isFalse();
    	assertThat(role.hasAny(Permission.AddDataset, Permission.DeleteDatasetDraft)).isFalse();
    	assertThat(role.permissions()).isEmpty();
    	
    	role.addPermission(Permission.AddDataset);
    	
    	assertThat(role.permissions()).containsExactlyInAnyOrder(Permission.AddDataset);
    	assertThat(role.hasPermissionFor(Dataverse.class)).isTrue();
    	assertThat(role.hasPermissionFor(Dataset.class)).isFalse();
    	assertThat(role.has(Permission.AddDataset)).isTrue();
    	assertThat(role.has(Permission.DeleteDatasetDraft)).isFalse();
    	assertThat(role.hasAny(Permission.AddDataset, Permission.DeleteDatasetDraft)).isTrue();
    	assertThat(role.permissions()).containsExactly(Permission.AddDataset);
    	
    	role.addPermission(Permission.DeleteDatasetDraft);
    	
    	assertThat(role.permissions())
    		.containsExactlyInAnyOrder(Permission.AddDataset, Permission.DeleteDatasetDraft);
    	assertThat(role.hasPermissionFor(Dataverse.class)).isTrue();
    	assertThat(role.hasPermissionFor(Dataset.class)).isTrue();
    	assertThat(role.has(Permission.AddDataset)).isTrue();
    	assertThat(role.has(Permission.DeleteDatasetDraft)).isTrue();
    	assertThat(role.hasAny(Permission.AddDataset, Permission.DeleteDatasetDraft)).isTrue();
    	assertThat(role.permissions()).containsExactlyInAnyOrder(Permission.AddDataset, Permission.DeleteDatasetDraft);
    	
    	role.clearPermissions();
    	
    	assertThat(role.permissions()).isEmpty();
    	assertThat(role.hasPermissionFor(Dataverse.class)).isFalse();
    	assertThat(role.hasPermissionFor(Dataset.class)).isFalse();
    	assertThat(role.has(Permission.AddDataset)).isFalse();
    	assertThat(role.has(Permission.DeleteDatasetDraft)).isFalse();
    	assertThat(role.hasAny(Permission.AddDataset, Permission.DeleteDatasetDraft)).isFalse();
    	assertThat(role.permissions()).isEmpty();
    }
}
