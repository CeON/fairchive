package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import java.util.ArrayList;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.FeaturedDataversesSorting.BY_DATASET_COUNT;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.FeaturedDataversesSorting.BY_HAND;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.FeaturedDataversesSorting.BY_NAME_ASC;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeaturedDataversesDialogTest {

    private static final Long DATAVERSE_ID = 1L;

    @Mock
    private DataverseService dataverseService;
    @Mock
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Mock
    private PermissionsWrapper permissionWrapper;

    @InjectMocks
    private FeaturedDataversesDialog dialog;

    private final Dataverse dataverse = new Dataverse();
    private final Dataverse alpha = createFeaturable("Alpha");
    private final Dataverse beta = createFeaturable("Beta");

    private FacesContext facesContext;

    @BeforeEach
    void setUp() {
        dataverse.setId(DATAVERSE_ID);
        dataverse.setAlias("root");
        dataverse.setFeaturedDataversesSorting(BY_NAME_ASC);

        when(permissionWrapper.canIssueUpdateDataverseCommand(dataverse)).thenReturn(true);
        dialog.init(dataverse);
    }

    @AfterEach
    void tearDown() {
        if (facesContext != null) {
            facesContext.release();
        }
    }

    // -------------------- TESTS --------------------

    @Test
    void setupDialog__appliesPersistedSortingToTarget() {
        // given
        List<Dataverse> displayOrder = new ArrayList<>(asList(beta, alpha));
        givenFeaturedDataverses(displayOrder);
        when(featuredDataverseService.sortFeaturedDataverses(displayOrder, BY_NAME_ASC))
                .thenReturn(asList(alpha, beta));

        // when
        dialog.setupDialog();

        // then
        assertThat(dialog.getFeaturedDataversesSorting()).isEqualTo(BY_NAME_ASC);
        assertThat(dialog.getFeaturedDataverses().getTarget()).containsExactly(alpha, beta);
    }

    @Test
    void setupDialog__discardsSortingSelectedButNotSaved() {
        // given
        List<Dataverse> displayOrder = new ArrayList<>(asList(beta, alpha));
        givenFeaturedDataverses(displayOrder);
        when(featuredDataverseService.sortFeaturedDataverses(displayOrder, BY_NAME_ASC))
                .thenReturn(asList(alpha, beta));
        dialog.setFeaturedDataversesSorting(BY_DATASET_COUNT);

        // when
        dialog.setupDialog();

        // then
        assertThat(dialog.getFeaturedDataversesSorting()).isEqualTo(BY_NAME_ASC);
        assertThat(dataverse.getFeaturedDataversesSorting()).isEqualTo(BY_NAME_ASC);
    }

    @Test
    void setFeaturedDataversesSorting__leavesDataverseUntouched() {
        // given & when
        dialog.setFeaturedDataversesSorting(BY_DATASET_COUNT);
        dialog.manualReorder();

        // then
        assertThat(dialog.getFeaturedDataversesSorting()).isEqualTo(BY_HAND);
        assertThat(dataverse.getFeaturedDataversesSorting()).isEqualTo(BY_NAME_ASC);
    }

    @Test
    void saveFeaturedDataverse__persistsSelectedSorting() {
        // given
        mockFacesContext();
        List<Dataverse> displayOrder = new ArrayList<>(asList(beta, alpha));
        givenFeaturedDataverses(displayOrder);
        when(featuredDataverseService.sortFeaturedDataverses(displayOrder, BY_NAME_ASC))
                .thenReturn(asList(alpha, beta));
        dialog.setupDialog();
        dialog.setFeaturedDataversesSorting(BY_DATASET_COUNT);

        // when
        String outcome = dialog.saveFeaturedDataverse();

        // then
        assertThat(dataverse.getFeaturedDataversesSorting()).isEqualTo(BY_DATASET_COUNT);
        verify(dataverseService).saveFeaturedDataverse(dataverse, asList(alpha, beta));
        assertThat(outcome).contains("alias=root");
    }

    // -------------------- PRIVATE --------------------

    private Dataverse createFeaturable(String name) {
        Dataverse featurable = new Dataverse();
        featurable.setName(name);
        return featurable;
    }

    private void givenFeaturedDataverses(List<Dataverse> displayOrder) {
        when(featuredDataverseService.findFeaturableDataverses(DATAVERSE_ID))
                .thenReturn(new ArrayList<>(asList(alpha, beta)));
        when(featuredDataverseService.findByDataverseId(DATAVERSE_ID)).thenReturn(displayOrder);
    }

    private void mockFacesContext() {
        facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(ENGLISH);
        when(facesContext.getExternalContext().getFlash()).thenReturn(mock(Flash.class));
    }
}
