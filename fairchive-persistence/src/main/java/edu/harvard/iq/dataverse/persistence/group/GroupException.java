package edu.harvard.iq.dataverse.persistence.group;

import javax.ejb.ApplicationException;

/**
 * When the groups library throws an exception, it has to be a subclass of this guy.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@ApplicationException(rollback = true)
public class GroupException extends RuntimeException {

    private final Group group;

    public GroupException(final Group group, final String message) {
        this(group, message, null);
    }

    public GroupException(final Group group, final String message, 
    		final Throwable cause) {
        super(message, cause);
        this.group = group;
    }

    public Group getGroup() {
        return this.group;
    }

    @Override
    public String toString() {
        return super.toString() + "[ Group: " + this.group + ']';
    }
}
