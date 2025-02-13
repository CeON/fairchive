package edu.harvard.iq.dataverse.dashboard;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DashboardExportSearchResultsPageTest {
    @InjectMocks
    private DashboardExportSearchResultsPage page;

    @Mock
    private DataverseSession session;
    @Mock
    private SystemConfig systemConfig;
    @Mock
    private DatasetFieldTypeRepository datasetFiledTypeRepo;
    @Mock
    private PermissionsWrapper permissionsWrapper;
    @Mock
    private DataverseDao dataverseDao;
    @Mock
    private MetadataBlockRepository metadataBlockRepo;
    @Mock
    private UIMessages uiMessages;

    private DatasetFieldType type1 = new DatasetFieldType();
    private DatasetFieldType type2 = new DatasetFieldType();
    private MetadataBlock metadataBlock = new MetadataBlock();
    private List<DatasetFieldType> types = asList(this.type1, this.type2);

    @BeforeEach
    public void setUp() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setSuperuser(true);
        when(this.session.getUser()).thenReturn(user);

        this.type1.setId(1L);
        this.type1.setTitle("def");
        this.type2.setId(2L);
        this.type2.setTitle("abc");

        this.metadataBlock.setDatasetFieldTypes(types);

        when(this.metadataBlockRepo.findSystemMetadataBlocks())
                .thenReturn(asList(this.metadataBlock));

        this.page.init();
    }

    @Test
    public void selectionAndSavingWorks() throws Exception {
        assertThat(this.page.getBlocks().size()).isEqualTo(1);
        assertThat(this.page.getBlocks().get(0)).isSameAs(this.metadataBlock);

        this.page.getBlocks().get(0).getDatasetFieldTypes().get(0).setExportToFile(true);
        this.page.save();

        verify(this.datasetFiledTypeRepo).saveAll(this.types);
        assertThat(this.type1.isExportToFile()).isTrue();
        assertThat(this.type2.isExportToFile()).isFalse();
    }
}
