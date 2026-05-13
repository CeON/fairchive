package edu.harvard.iq.dataverse.search.query;

import static java.lang.String.join;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

@Stateless
public class PermissionFilterQueryBuilder {

    private static final Logger logger = getLogger(SearchServiceBean.class.getCanonicalName());

    private GroupServiceBean groupService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public PermissionFilterQueryBuilder() {
        // JEE requirement
    }

    @Inject
    public PermissionFilterQueryBuilder(final GroupServiceBean groupService) {
        this.groupService = groupService;
    }

    // -------------------- LOGIC --------------------

    public String buildPermissionFilterQuery(final DataverseRequest dataverseRequest) {
        return buildPermissionFilterQuery(dataverseRequest, this::buildJoinQuery);
    }

    public String buildPermissionFilterQueryForAddDataset(final DataverseRequest dataverseRequest) {
        return buildPermissionFilterQuery(dataverseRequest, this::buildJoinQueryForAddDataset);
    }

    // -------------------- PRIVATE --------------------

    private String buildPermissionFilterQuery(final DataverseRequest dataverseRequest, 
    		final Function<List<String>, String> queryBuilder) {
        final User user = dataverseRequest.getUser();

        if (user.isSuperuser()) {
            return EMPTY;
        }

        final List<String> allUserGroups = new ArrayList<>();

        if (user.isAuthenticated()) {
            final AuthenticatedUser au = (AuthenticatedUser) user;
            allUserGroups.add(IndexServiceBean.getGroupPerUserPrefix() + au.getId());
        }

        final List<String> userGroupStrings = collectUserGroups(dataverseRequest);
        logger.fine(userGroupStrings.toString());
        allUserGroups.addAll(userGroupStrings);

        final String permissionFilterQuery = queryBuilder.apply(allUserGroups);

        logger.fine(permissionFilterQuery);

        return permissionFilterQuery;
    }

    private String buildJoinQuery(final List<String> discoverableByGroups) {
        final String discoverableByQueryPart = SearchFields.DISCOVERABLE_BY + 
        		":(" + StringUtils.join(discoverableByGroups, " OR ") + ')';
        final String discoverableByPublicQueryPart = SearchFields.DISCOVERABLE_BY_PUBLIC_FROM + 
        		":[* TO NOW/DAY]";

        final String experimentalJoin = "{!join from=" 
        		+ SearchFields.DEFINITION_POINT + " to=id}"
                + discoverableByQueryPart + " OR "
                + discoverableByPublicQueryPart;
        return experimentalJoin;
    }

    private String buildJoinQueryForAddDataset(final List<String> groups) {
        return "{!join from=" + SearchFields.DEFINITION_POINT + " to=id}" +
                SearchFields.ADD_DATASET_PERM + ":(" + join(" OR ", groups) + ')';
    }

    private List<String> collectUserGroups(final DataverseRequest dataverseRequest) {
        /**
         * From a search perspective, we don't care about if the group was
         * created within one dataverse or another. We just want a list of *all*
         * the groups the user is part of. We are greedy. We want all BuiltIn
         * Groups, Shibboleth Groups, IP Groups, "system" groups, everything.
         *
         * A JOIN on "permission documents" will determine if the user can find
         * a given "content document" (dataset version, etc) in Solr.
         */
        return this.groupService.collectAncestors(this.groupService.groupsFor(dataverseRequest))
                .stream()
                .map(Group::getAlias)
                .filter(StringUtils::isNotEmpty)
                .map(alias -> IndexServiceBean.getGroupPrefix() + alias)
                .collect(toList());
    }

}
