package edu.harvard.iq.dataverse.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * @author michael
 */
public class PermissionTest {

    /**
     * Test of appliesTo method, of class Permission.
     */
    @Test
    public void testAppliesTo() {
        assertThat(Permission.EditDataverse.appliesTo(DvObject.class)).isFalse();
        assertThat(Permission.EditDataverse.appliesTo(Dataverse.class)).isTrue();
        assertThat(Permission.EditDataverse.appliesTo(DataFile.class)).isFalse();

        assertThat(Permission.EditDataset.appliesTo(Dataset.class)).isTrue();
        assertThat(Permission.EditDataset.appliesTo(DvObject.class)).isFalse();
        assertThat(Permission.EditDataset.appliesTo(Dataverse.class)).isFalse();
    }
    
    @Test
    void bitValue() {
    	
    	assertThat(Permission.AddDataverse.bitValue()).isEqualTo(1L);
    	assertThat(Permission.AddDataset.bitValue()).isEqualTo(2L);
    	assertThat(Permission.ViewUnpublishedDataverse.bitValue()).isEqualTo(4L);
    	assertThat(Permission.ViewUnpublishedDataset.bitValue()).isEqualTo(8L);
    	assertThat(Permission.DownloadFile.bitValue()).isEqualTo(16L);	
    	assertThat(Permission.EditDataverse.bitValue()).isEqualTo(32L);
    	assertThat(Permission.EditDataset.bitValue()).isEqualTo(64L);
    	assertThat(Permission.ManageDataversePermissions.bitValue()).isEqualTo(128L);
    	assertThat(Permission.ManageDatasetPermissions.bitValue()).isEqualTo(256L);
    	assertThat(Permission.PublishDataverse.bitValue()).isEqualTo(512L);  	
    	assertThat(Permission.PublishDataset.bitValue()).isEqualTo(1024L);
    	assertThat(Permission.DeleteDataverse.bitValue()).isEqualTo(2048L);
    	assertThat(Permission.DeleteDatasetDraft.bitValue()).isEqualTo(4096L);
    	assertThat(Permission.ManageMinorDatasetPermissions.bitValue()).isEqualTo(8192L);
    }
    
    @Test
    void setOf() {
    	
    	assertThat(Permission.streamFrom(0)).isEmpty();
    	assertThat(Permission.streamFrom(1)).containsExactly(Permission.AddDataverse);
    	assertThat(Permission.streamFrom(2)).containsExactly(Permission.AddDataset);
    	assertThat(Permission.streamFrom(3)).containsExactlyInAnyOrder(Permission.AddDataverse, Permission.AddDataset);
    }
}
