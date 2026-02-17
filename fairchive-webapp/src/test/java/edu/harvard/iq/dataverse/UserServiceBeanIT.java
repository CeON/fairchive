package edu.harvard.iq.dataverse;

import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.temporal.TemporalUnit;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

public class UserServiceBeanIT extends WebappArquillianDeployment {

	
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
    void save() throws Exception {
    	
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
}
