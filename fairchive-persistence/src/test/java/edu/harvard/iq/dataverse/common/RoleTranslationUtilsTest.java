package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.RoleTranslationUtil.getLocaleNameFromAlias;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RoleTranslationUtilsTest {

	@Test
	void getLocaleNameFromAliasTest() {

		assertThat(getLocaleNameFromAlias(null, "xyz")).isEqualTo("xyz");
		assertThat(getLocaleNameFromAlias("admin", "xyz")).isEqualTo("Admin");
		
	}
}
