package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MyDataFilterParamsTest {

    private final DataverseRequest request = new DataverseRequest(new AuthenticatedUser(), (HttpServletRequest) null);

    // -------------------- TESTS --------------------

    @Test
    void getSolrFragmentForPublicationStatus__anyStatusByDefault() {
        // given
        MyDataFilterParams params = new MyDataFilterParams(request, Arrays.asList("Dataverse", "Dataset"),
                Arrays.asList("Published", "Draft"), Arrays.asList(1L), "");

        // when
        String fragment = params.getSolrFragmentForPublicationStatus();

        // then
        assertThat(fragment).isEqualTo("(publicationStatus:(\"Published\" OR \"Draft\"))");
    }

    @Test
    void getSolrFragmentForPublicationStatus__allStatusesRequired() {
        // given
        MyDataFilterParams params = create(Arrays.asList("Published", "Draft"), true);

        // when
        String fragment = params.getSolrFragmentForPublicationStatus();

        // then
        assertThat(fragment).isEqualTo("(publicationStatus:(\"Published\" AND \"Draft\"))");
    }

    @Test
    void getSolrFragmentForPublicationStatus__singleStatusRequired() {
        // given
        MyDataFilterParams params = create(Arrays.asList("Draft"), true);

        // when
        String fragment = params.getSolrFragmentForPublicationStatus();

        // then
        assertThat(fragment).isEqualTo("(publicationStatus:\"Draft\")");
    }

    @Test
    void getSolrFragmentForPublicationStatus__everyStatusSelectedWhileAllRequired() {
        // given
        MyDataFilterParams params = create(MyDataFilterParams.allPublishedStates, true);

        // when
        String fragment = params.getSolrFragmentForPublicationStatus();

        // then
        assertThat(fragment)
                .isEqualTo("(publicationStatus:(\"Published\" OR \"Unpublished\" OR \"Draft\" OR \"In_Review\" OR \"Deaccessioned\"))");
    }

    // -------------------- PRIVATE --------------------

    private MyDataFilterParams create(List<String> publicationStatuses, boolean allPublicationStatusesRequired) {
        return new MyDataFilterParams(request, Arrays.asList("Dataverse", "Dataset"), publicationStatuses,
                Arrays.asList(1L), "", allPublicationStatusesRequired);
    }
}
