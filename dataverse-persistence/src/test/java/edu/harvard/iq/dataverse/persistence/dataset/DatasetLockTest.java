package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Ingest;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

public class DatasetLockTest {

    private final AuthenticatedUser user = new AuthenticatedUser();
    
    @BeforeEach
    void setUp() {
        this.user.setId(1L);
        this.user.setUserIdentifier("user1");
    }
    
    @Test
    void constructingLocks_works() {
        DatasetLock lock = new DatasetLock(Ingest, this.user);
        
        assertThat(lock.getReason()).isEqualTo(Ingest);
        assertThat(lock.getUser()).isEqualTo(this.user);
        assertThat(lock.getInfo()).isNull();
        assertThat(lock.hashCode()).isEqualTo(0);
    }
}
