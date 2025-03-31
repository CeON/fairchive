package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Ingest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

public class DatasetLockTest {

    private final AuthenticatedUser user = new AuthenticatedUser();
    private final Dataset dataset = new Dataset();
    private final Date startTime = new Date();
    
    @BeforeEach
    void setUp() {
        this.user.setId(1L);
        this.user.setUserIdentifier("user1");
        
        this.dataset.setId(1L);
    }
    
    @Test
    void constructingLocks_works() {
        DatasetLock lock = new DatasetLock(Ingest, this.user);
        
        assertThat(lock.getReason()).isEqualTo(Ingest);
        assertThat(lock.getUser()).isEqualTo(this.user);
        assertThat(lock.getInfo()).isNull();
        assertThat(lock.hashCode()).isEqualTo(0);
    }
    
    @Test
    @SuppressWarnings("unlikely-arg-type")
    void lock_isNotEqualtoNonLock() {
        DatasetLock lock = new DatasetLock(Ingest, this.user);
        
        assertThat(lock.equals(null)).isFalse();
        assertThat(lock.equals(this.user)).isFalse();
    }
    
    @Test
    void lock_isEqualtoItself() {
        DatasetLock lock = new DatasetLock(Ingest, this.user);
        lock.setDataset(this.dataset);
        lock.setStartTime(this.startTime);
        lock.setId(1L);
        
        assertThat(lock.equals(lock)).isTrue();
    }
    
    @Test
    void onlyLocksWithIdsSet_areEqualt() {
        DatasetLock lock1 = new DatasetLock(Ingest, this.user);
        lock1.setDataset(this.dataset);
        lock1.setStartTime(this.startTime);
        DatasetLock lock2 = new DatasetLock(Ingest, this.user);
        lock2.setDataset(this.dataset);
        lock2.setStartTime(this.startTime);
        
        assertThat(lock1.equals(lock2)).isFalse();
        
        lock1.setId(1L);
        lock2.setId(1L);
        
        assertThat(lock1.equals(lock2)).isTrue();
    }
}
