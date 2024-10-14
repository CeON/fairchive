package edu.harvard.iq.dataverse.dashboard;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DashboardExportSearchResultsPageTest {

    private DashboardExportSearchResultsPage page;

    @Mock
    private DataverseSession session;
    @Mock
    private SystemConfig systemConfig;
    @Mock
    private DatasetFieldTypeRepository datasetFiledTypeRepo;

    private DatasetFieldType type1 = new DatasetFieldType();
    private DatasetFieldType type2 = new DatasetFieldType();

    // -------------------------------------------------------------------------
    @BeforeEach
    public void setUp() {

        AuthenticatedUser user = new AuthenticatedUser();
        user.setSuperuser(true);
        when(this.session.getUser()).thenReturn(user);

        this.type1.setId(1L);
        this.type1.setTitle("def");
        this.type2.setId(2L);
        this.type2.setTitle("abc");

        when(this.datasetFiledTypeRepo.findAll())
                .thenReturn(asList(this.type1, this.type2));

        this.page = new DashboardExportSearchResultsPage(this.session, null, null,
                this.systemConfig, this.datasetFiledTypeRepo);
    }

    // -------------------------------------------------------------------------
    @Test
    public void selectionAndSavingWorks() throws Exception {

        this.page.init();

        assertThat(this.page.getMetadataTypes().size()).isEqualTo(2);

        assertThat(this.page.getMetadataTypes().get(0).getId()).isEqualTo(2L);
        assertThat(this.page.getMetadataTypes().get(0).getTitle()).isEqualTo("abc");
        assertThat(this.page.getMetadataTypes().get(0).isExportable()).isFalse();

        assertThat(this.page.getMetadataTypes().get(1).getId()).isEqualTo(1L);
        assertThat(this.page.getMetadataTypes().get(1).getTitle()).isEqualTo("def");
        assertThat(this.page.getMetadataTypes().get(1).isExportable()).isFalse();

        this.page.getMetadataTypes().get(0).setExportable(true);
        this.page.save();

        verify(this.datasetFiledTypeRepo).save(this.type2);
        assertThat(this.type1.isExportToFile()).isFalse();
        assertThat(this.type2.isExportToFile()).isTrue();

    }
}
