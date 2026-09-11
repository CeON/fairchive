package edu.harvard.iq.dataverse.engine.command.exception;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import java.util.Set;

/**
 * An exception raised when a command cannot be executed, due to the
 * issuing user lacking permissions.
 *
 * @author michael
 */
@SuppressWarnings("serial")
public class PermissionException extends CommandException {

    private final Set<Permission> missingPermissions;
    private final DvObject object;

    public PermissionException(final String message, final Command<?> failedCommand, 
    		final Set<Permission> missingPermissions, final DvObject object) {
    	
        super(message, failedCommand);
        this.missingPermissions = missingPermissions;
        this.object = object;
    }

    public PermissionException(final String message, 
    		final Set<Permission> missingPermissions, final DvObject object) {
        super(message, null);
        this.missingPermissions = missingPermissions;
        this.object = object;
    }

    public Set<Permission> getMissingPermissions() {
        return this.missingPermissions;
    }

    public DvObject getDvObject() {
        return this.object;
    }

}
