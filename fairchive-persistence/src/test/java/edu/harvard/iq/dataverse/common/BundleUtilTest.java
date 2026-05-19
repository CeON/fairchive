package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.hasKeyInBundle;
import static edu.harvard.iq.dataverse.common.BundleUtil.hasKeyInNonDefaultBundle;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class BundleUtilTest {
	
	private final static Locale PL = new Locale("pl");

    @Test
    public void hasKeyInBundleTest() {
        assertThat(hasKeyInBundle("search")).isTrue();
        assertThat(hasKeyInBundle("login.error")).isTrue();
        assertThat(hasKeyInBundle("")).isFalse();
        assertThat(hasKeyInBundle("junkKeyWeDoNotExpectToFind")).isFalse();
    }

    @Test
    public void hasKeyInNonDefaultBundleTest() {
        assertThat(hasKeyInNonDefaultBundle("role.admin.description", "BuiltInRoles")).isTrue();
        assertThat(hasKeyInNonDefaultBundle("nonExistingKey", "BuiltInRoles")).isFalse();
        assertThat(hasKeyInNonDefaultBundle("role.admin.description", "nonexistingBundle")).isFalse();
    }

	@Test
	public void getStringFromBundleTest() {
		assertThat(getStringFromBundle(null)).isEmpty();
		assertThat(getStringFromBundle("")).isEmpty();
		assertThat(getStringFromBundle("junkKeyWeDoNotExpectToFind")).isEmpty();
		assertThat(getStringFromBundle("search")).isEqualTo("Search");
		assertThat(getStringFromBundle("login.error")).isEqualTo(
				"Error validating the username, email address, or password. Please try again. If the problem persists, contact an administrator.");
	}

	@Test
	public void getStringFromBundleWithLocaleTest() {
		assertThat(getStringFromBundleWithLocale(null, ENGLISH)).isEmpty();
		assertThat(getStringFromBundleWithLocale("search", ENGLISH)).isEqualTo("Search");
		assertThat(getStringFromBundleWithLocale("search", PL)).isEqualTo("Szukaj");
	}
    
	@Test
	public void getStringFromBundleWithArguments() {
		assertThat(getStringFromBundle("dataverse.create.success", "http://guides.dataverse.org/en", "4.0"))
			.isEqualTo(
				"You have successfully created your collection! To learn more about what you can do with your collection, "
				+ "check out the <a href=\"http://guides.dataverse.org/en/4.0/user/dataverse-management.html\" title=\"Collection Management - "
				+ "Fairchive User Guide\" target=\"_blank\">User Guide</a>.");
		
		assertThat(getStringFromBundle("notification.email.createDataverse", "dvName", "dvUrl", "parentDvName",
				"parentDvUrl", "http://guides.dataverse.org/en", "4.0"))
			.isEqualTo(
				"Your new collection named " + "dvName (view at dvUrl ) "
				+ "was created in parentDvName (view at parentDvUrl ). To learn more "
				+ "about what you can do with your collection, check out "
				+ "the Collection Management - User Guide at "
				+ "http://guides.dataverse.org/en/4.0/user/dataverse-management.html .");

		assertThat(getStringFromBundle("notification.email.createDataset", "dsName", "dsUrl", "parentDvName",
				"parentDvUrl", "http://guides.dataverse.org/en", "4.0"))
			.isEqualTo(
				"Your new dataset named dsName (view at dsUrl ) "
				+ "was created in parentDvName (view at parentDvUrl ). "
				+ "To learn more about what you can do with a dataset, "
				+ "check out the Dataset Management - User Guide at "
				+ "http://guides.dataverse.org/en/4.0/user/dataset-management.html .");
		
		assertThat(getStringFromBundle("dataverse.results.empty.zero", "http://guides.dataverse.org/en","4.2"))
			.isEqualTo(
				"There are no collections, datasets, or files that match your search. "
				+ "Please try a new search by using other or broader terms. You can also check out "
				+ "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
				+ "Data - Fairchive User Guide\" target=\"_blank\">search guide</a> for tips.");
		
		assertThat(getStringFromBundle("dataverse.results.empty.hidden", "http://guides.dataverse.org/en","4.2"))
			.isEqualTo(
				"There are no search results based on how you have narrowed your search. You can check out "
				+ "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
				+ "Data - Fairchive User Guide\" target=\"_blank\">search guide</a> for tips.");
		
		assertThat(getStringFromBundle("dataverse.saved.search.success",
				"<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>"))
			.isEqualTo(
				"The saved search has been successfully linked to "
				+ "<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>.");
		
		assertThat(getStringFromBundle("shib.welcomeExistingUserMessage", "TestShib Test IdP"))
			.isEqualTo(
				"Your institutional log in for TestShib Test IdP matches an email address already used by one of the repository "
				+ "accounts. By entering your current repository password below, your existing repository account can be "
				+ "converted to use your institutional log in. After converting, you will only need to use your institutional log in.");
		
		assertThat(getStringFromBundle("shib.welcomeExistingUserMessage",
				getStringFromBundle("shib.welcomeExistingUserMessageDefaultInstitution")))
			.isEqualTo(
				"Your institutional log in for your institution matches an email address already used by one of the repository "
				+ "accounts. By entering your current repository password below, your existing repository account can be "
				+ "converted to use your institutional log in. After converting, you will only need to use your institutional log in.");
	}

	@Test
	public void getStringFromNonDefaultBundleTest() {
		assertThat(getStringFromNonDefaultBundle("application/zip", "MimeTypeFacets")).
			isEqualTo("ZIP");
	}

    @Test
    public void getStringFromNonDefaultBundle_expectedEmpty() {
    	assertThat(getStringFromNonDefaultBundle("FAKE", "MimeTypeFacets")).isEmpty();
    }
}
