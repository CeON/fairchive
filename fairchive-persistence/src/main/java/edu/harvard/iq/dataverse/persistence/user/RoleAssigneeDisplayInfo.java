package edu.harvard.iq.dataverse.persistence.user;

import java.util.Objects;
import java.io.Serializable;

/**
 * Contains display info for an assignee.
 *
 * @author michael
 */

@SuppressWarnings("serial")
public class RoleAssigneeDisplayInfo implements Serializable {

    private String title;
    private String emailAddress;
    private String affiliation;
    private String affiliationROR;

    public RoleAssigneeDisplayInfo(final String title, final String emailAddress) {
    	
        this(title, emailAddress, null, null);
    }

    public RoleAssigneeDisplayInfo(final String title, final String emailAddress, 
    		final String anAffiliation, final String affiliationROR) {
    	
        this.title = title;
        this.emailAddress = emailAddress;
        this.affiliation = anAffiliation;
        this.affiliationROR = affiliationROR;
    }

    public String getTitle() {
        return this.title;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public String getAffiliation() {
        return this.affiliation;
    }

    public String getAffiliationROR() {
        return this.affiliationROR;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setAffiliation(final String affiliation) {
        this.affiliation = affiliation;
    }

    public void setAffiliationROR(final String affiliationROR) {
        this.affiliationROR = affiliationROR;
    }

    @Override
    public String toString() {
        return "RoleAssigneeDisplayInfo{" + "title=" + title + ", emailAddress=" + emailAddress +
                ", affiliation=" + affiliation + ", affiliationROR=" + affiliationROR + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.title);
    }

	@Override
	public boolean equals(final Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			final RoleAssigneeDisplayInfo other = (RoleAssigneeDisplayInfo) obj;
			return  Objects.equals(this.title, other.title)
					&& Objects.equals(this.emailAddress, other.emailAddress)
					&& Objects.equals(this.affiliation, other.affiliation)
					&& Objects.equals(this.affiliationROR, other.affiliationROR);
		} else {
			return false;
		}
	}
}
