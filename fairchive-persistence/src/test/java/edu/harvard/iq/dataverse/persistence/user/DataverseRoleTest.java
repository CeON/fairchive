package edu.harvard.iq.dataverse.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DataverseRoleTest {

	@Test
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
}
