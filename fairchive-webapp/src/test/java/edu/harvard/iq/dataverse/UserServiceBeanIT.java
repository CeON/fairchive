package edu.harvard.iq.dataverse;

import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;

public class UserServiceBeanIT extends WebappArquillianDeployment {
	
	private final static int resultLimit10 = 10;
	private final static int offset0 = 0;
	private final static boolean sortAscending = true;
	private final static boolean sortDescenting = false;
	
	@Inject
	private UserServiceBean service;
	
	@Test 
	void getById() {
		
		assertThat(this.service.getById(2L).getId()).isEqualTo(2L);
	}
	
    @Test
    void countUsers() {

    	assertThat(this.service.countUsers()).isEqualTo(4);
    	assertThat(this.service.countUsers(null)).isEqualTo(4);
        assertThat(this.service.countUsers(EMPTY)).isEqualTo(4);
        assertThat(this.service.countUsers("lastname")).isEqualTo(3);
        assertThat(this.service.countUsers("Non-existent")).isZero();
    }
    
    @Test
    void getSuperUserCount() {
    	
    	assertThat(this.service.countSuperUsers()).isEqualTo(2);
    }
    
    @Test
    void save() {
    	
    	AuthenticatedUser user = new AuthenticatedUser();
    	user.setUserIdentifier("user1");
    	user.setEmail("user1@gmail.com");
    	user.setFirstName("firstName");
    	user.setLastName("lastName");
    	
    	AuthenticatedUser savedUser = this.service.save(user);
    	
    	assertThat(savedUser.getId()).isNotNull();
    	Timestamp now = Timestamp.from(now().plusMillis(1000));
    	
    	assertThat(savedUser.getCreatedTime().before(now)).isTrue();
    	assertThat(savedUser.getLastLoginTime().before(now)).isTrue();
    	assertThat(savedUser.getCreatedTime()).isEqualTo(savedUser.getLastLoginTime());
    	
    	assertThat(this.service.getById(savedUser.getId()).getId()).isEqualTo(savedUser.getId());
    }
    
    @Test
    public void find() {
    	
        List<AuthenticatedUser> users = this.service.find(EMPTY,
        		SortKey.ID.toString(), sortAscending, resultLimit10, offset0);

        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(1L, 2L, 3L, 4L);
        
        assertThat(users.get(0).getRoles()).isEqualTo("Admin");
        assertThat(users.get(1).getRoles()).isEqualTo("Contributor, Curator");
        assertThat(users.get(2).getRoles()).isEqualTo("File Downloader, Curator");
        assertThat(users.get(3).getRoles()).isEmpty();
    }
    
    @Test
    public void find_withNonExistentSortKey_defaultsTo_ID() {
    	
        List<AuthenticatedUser> users = this.service.find(EMPTY,
        		"Non-existent", sortAscending, resultLimit10, offset0);

        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(1L, 2L, 3L, 4L);
    }
    
    @Test
    public void find_withNullLimitAndOffset_usesDefaultValuesOfOne() {
    	
        List<AuthenticatedUser> users = this.service.find(EMPTY,
        		SortKey.ID.toString(), sortAscending, null, null);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(1L);
    }

    @Test
    public void find_sortedByUserIdentifier_descending() {

        List<AuthenticatedUser> users = this.service.find(EMPTY,
        		SortKey.USER_IDENTIFIER.toString(), sortDescenting, resultLimit10, offset0);

        assertThat(users.size()).isEqualTo(4);
        assertThat(users).extracting(AuthenticatedUser::getUserIdentifier)
        	.containsSequence("superuser", "rootGroupMember", "filedownloader", "dataverseAdmin");
    }

    @Test
    public void find_withLimitAndOffset() {
    	
        int resultLimit = 2;
        int offset = 1;

        List<AuthenticatedUser> users = this.service.find(EMPTY,
        		SortKey.ID.toString(), sortAscending, resultLimit, offset);

        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(2L, 3L);
    }

    @Test
    public void find_filtered() {

        List<AuthenticatedUser> users = this.service.find("some",
        		SortKey.ID.toString(), sortAscending, resultLimit10, offset0);

        assertThat(users.size()).isEqualTo(2);
        assertThat(users).extracting(AuthenticatedUser::getId).containsSequence(3L, 4L);
        assertThat(users).extracting(AuthenticatedUser::getAffiliation)
        	.containsExactly("some affiliation", "some affiliation");
    }

    @Test
    public void find_filtered_noResults() {

        List<AuthenticatedUser> users = this.service.find("Non-existent", 
        		SortKey.ID.toString(), sortAscending, resultLimit10, offset0);

        assertThat(users).isEmpty();
    }
}
