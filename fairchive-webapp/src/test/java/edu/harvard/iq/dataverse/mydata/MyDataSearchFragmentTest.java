package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class MyDataSearchFragmentTest {

    private static final String ENTITY_ID_FILTER = "(entityId:(1 OR 2))";
    private static final String UNHARVESTED_FILTER = "(isHarvested:false)";

    // -------------------- TESTS --------------------

    @Test
    void matchAllSelectedPublicationStatuses__someStatusesSelected() {
        // given
        List<String> statuses = asList("Published", "Draft");
        List<String> filterQueries = filterQueriesFor(statuses);

        // when
        MyDataSearchFragment.matchAllSelectedPublicationStatuses(filterQueries, statuses);

        // then
        assertThat(filterQueries).contains("(publicationStatus:(\"Published\" AND \"Draft\"))");
    }

    @Test
    void matchAllSelectedPublicationStatuses__someStatusesSelected_otherFiltersUntouched() {
        // given
        List<String> statuses = asList("Published", "Draft");
        List<String> filterQueries = filterQueriesFor(statuses);

        // when
        MyDataSearchFragment.matchAllSelectedPublicationStatuses(filterQueries, statuses);

        // then
        assertThat(filterQueries).startsWith(ENTITY_ID_FILTER).endsWith(UNHARVESTED_FILTER).hasSize(3);
    }

    @Test
    void matchAllSelectedPublicationStatuses__singleStatusSelected() {
        // given
        List<String> statuses = singletonList("Draft");
        List<String> filterQueries = filterQueriesFor(statuses);

        // when
        MyDataSearchFragment.matchAllSelectedPublicationStatuses(filterQueries, statuses);

        // then
        assertThat(filterQueries).containsExactly(
                ENTITY_ID_FILTER, "(publicationStatus:\"Draft\")", UNHARVESTED_FILTER);
    }

    @Test
    void matchAllSelectedPublicationStatuses__allStatusesSelected() {
        // given
        List<String> statuses = MyDataFilterParams.defaultPublishedStates;
        List<String> filterQueries = filterQueriesFor(statuses);
        List<String> original = new ArrayList<>(filterQueries);

        // when
        MyDataSearchFragment.matchAllSelectedPublicationStatuses(filterQueries, statuses);

        // then
        assertThat(filterQueries).isEqualTo(original);
    }

    // -------------------- PRIVATE --------------------

    /**
     * Builds the filter queries in the shape MyDataFinder returns them, with
     * the publication status filter produced by the real MyDataFilterParams.
     */
    private List<String> filterQueriesFor(List<String> publicationStatuses) {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserIdentifier("user");
        MyDataFilterParams params = new MyDataFilterParams(new DataverseRequest(user, (IpAddress) null),
                singletonList(DvObject.DATASET_DTYPE_STRING), publicationStatuses, singletonList(1L), "");
        return new ArrayList<>(asList(
                ENTITY_ID_FILTER, params.getSolrFragmentForPublicationStatus(), UNHARVESTED_FILTER));
    }
}
