package edu.harvard.iq.dataverse.persistence.group;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.io.Serializable;

import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

public class AuthenticatedUsers implements Group, Serializable {

    private static final long serialVersionUID = 1L;
	private static final String GROUP_TYPE = "builtin";
    private static final AuthenticatedUsers instance = new AuthenticatedUsers();
    private static String alias = GROUP_TYPE + Group.PATH_SEPARATOR + "authenticated-users";

    private AuthenticatedUsers() {
    }

    public static AuthenticatedUsers get() {
        return instance;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public String getIdentifier() {
        return ":authenticated-users";
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(
        		getStringFromBundle("permission.anyoneWithAccount"), null);
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getDisplayName() {
        return "Authenticated Users";
    }

    @Override
    public String getDescription() {
        return "All users, except for guests";
    }

    @Override
    public String toString() {
        return "Group authenticated-users";
    }
    
    @Override
    public int hashCode() {
    	return 0; // since all instances are equal - this will do
    }
    
    @Override
    public boolean equals(final Object o) {
    	// all instances are equal
    	return o != null && getClass().equals(o.getClass());
    }


}
