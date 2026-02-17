package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticatedUserRepositoryIT extends PersistenceArquillianDeployment {
	
	private final static int resultLimit10 = 10;
	private final static int offset0 = 0;
	private final static boolean sortAscending = true;
	private final static boolean sortDescenting = false;

    @Inject
    private AuthenticatedUserRepository repo;

    //-------------------- TESTS --------------------

    @Test
    public void findSearchedAuthenticatedUsers() {
    	
        List<AuthenticatedUser> users = this.repo.findSearchedAuthenticatedUsers(
        		SortKey.ID, resultLimit10, offset0, EMPTY, sortAscending);

        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(1L, 2L, 3L, 4L);
    }

    @Test
    public void findSearchedAuthenticatedUsers_sortedByUserIdentifier_descending() {

        List<AuthenticatedUser> users = this.repo.findSearchedAuthenticatedUsers(
        		SortKey.USER_IDENTIFIER, resultLimit10, offset0, EMPTY, sortDescenting);

        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getUserIdentifier)
        	.containsSequence("superuser", "rootGroupMember", "filedownloader", "dataverseAdmin");
    }

    @Test
    public void findSearchedAuthenticatedUsers_limitedTo_2_offset_1() {
    	
        int resultLimit = 2;
        int offset = 1;

        List<AuthenticatedUser> users = this.repo.findSearchedAuthenticatedUsers(
        		SortKey.ID, resultLimit, offset, EMPTY, sortAscending);

        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(2L, 3L);
    }

    @Test
    public void findSearchedAuthenticatedUsers_filtered() {

        List<AuthenticatedUser> users = this.repo.findSearchedAuthenticatedUsers(
        		SortKey.ID, resultLimit10, offset0, "some", sortAscending);

        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(3L, 4L);
        assertThat(users).extracting(AuthenticatedUser::getAffiliation)
        	.containsExactly("some affiliation", "some affiliation");
    }

    @Test
    public void findSearchedAuthenticatedUsers_filtered_noResults() {

        List<AuthenticatedUser> users = this.repo.findSearchedAuthenticatedUsers(
        		SortKey.ID, resultLimit10, offset0, "Non-existent", sortAscending);

        assertThat(users).isEmpty();
    }

    @Test
    public void countSearchedAuthenticatedUsers() {

        assertThat(this.repo.countSearchedAuthenticatedUsers(EMPTY)).isEqualTo(4);
    }

    @Test
    public void countSearchedAuthenticatedUsers_filtered() {
    	
        assertThat(this.repo.countSearchedAuthenticatedUsers("lastname")).isEqualTo(3);
    }

    @Test
    public void countSearchedAuthenticatedUsers_filtered_noResults() {

        assertThat(this.repo.countSearchedAuthenticatedUsers("Non-existent")).isZero();
    }
    
    @Test
    public void getSuperUserCount() {
    	
    	assertThat(this.repo.getSuperUserCount()).isEqualTo(2);
    }
}
