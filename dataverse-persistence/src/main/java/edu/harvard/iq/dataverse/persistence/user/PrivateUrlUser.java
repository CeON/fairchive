package edu.harvard.iq.dataverse.persistence.user;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import edu.harvard.iq.dataverse.persistence.DvObject;

/**
 * A PrivateUrlUser is virtual in the sense that it does not have a row in the
 * authenticateduser table. It exists so when a Private URL is enabled for a
 * dataset, we can assign a read-only role ("member") to the identifier for the
 * PrivateUrlUser. (We will make no attempt to internationalize the identifier,
 * which is stored in the roleassignment table.) 
 */
@SuppressWarnings("serial")
public class PrivateUrlUser implements User {

    public static final String PREFIX = "#";
    /**
     * In the future, this could probably be dvObjectId rather than datasetId, if
     * necessary. It's really just roleAssignment.getDefinitionPoint(), which is a
     * DvObject.
     */
    private final long datasetId;
    private final boolean anonymized;

    public PrivateUrlUser(final long datasetId) {
        this.datasetId = datasetId;
        this.anonymized = false;
    }

    public PrivateUrlUser(final long datasetId, final boolean anonymized) {
        this.datasetId = datasetId;
        this.anonymized = anonymized;
    }

    public long getDatasetId() {
        return this.datasetId;
    }
    
    /**
     * By always returning false for isAuthenticated(), we prevent a
     * name from appearing in the corner as well as preventing an account page
     * and MyData from being accessible. The user can still navigate to the home
     * page but can only see published datasets.
     *
     * @return {@code false}.
     */
    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean isSuperuser() {
        return false;
    }

    @Override
    public String getIdentifier() {
        return PREFIX + this.datasetId;
    }

    @Override
    public boolean isAnonymized() {
        return this.anonymized;
    }
    
    @Override
    public boolean isAllowedToView(final DvObject object) {
        return this.datasetId == object.getId();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(
                getStringFromBundle("dataset.privateurl.roleassigeeTitle"), null);
    }
}
