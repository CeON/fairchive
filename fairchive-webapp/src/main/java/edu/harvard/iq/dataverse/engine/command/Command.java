package edu.harvard.iq.dataverse.engine.command;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * Base interface for all commands running on Dataverse.
 *
 * @param <R> The type of result this command returns.
 * @author michael
 */
public interface Command<R> {

    /**
     * Override this method to execute the actual command.
     *
     * @param ctxt the context on which the command work. All dependency injections, if any, should be done using this.
     * @return A result. May be {@code null}
     */
    R execute(CommandContext ctxt);


    /**
     * Retrieves the {@link DvObject}s this command works on. Used by the {@link DataverseEngine}
     * to validate that the user
     * has the permissions required to execute {@code this} command.
     *
     * @return The DvObjects on which the command will work
     */
    Map<String, DvObject> getAffectedDvObjects();


    /**
     * @return The request under which this command is being executed.
     */
    DataverseRequest getRequest();

    /**
     * @return A map of the permissions required for this command
     */
    default Map<String, Set<Permission>> getRequiredPermissions() {
    	return requiredPermissions(getClass());
    }

    default boolean isAllPermissionsRequired() {
        final RequiredPermissions required = getClass().getAnnotation(RequiredPermissions.class);
        return required == null || required.isAllPermissionsRequired();
    }

    String describe();
    
    /**
     * Given a {@link Command} sub-class, returns the set of permissions needed
     * to be able to execute it. Needed permissions are specified by annotating
     * the command's class with the {@link RequiredPermissions} annotation.
     *
     * @param cmdClass A class of command
     * @return Set of permissions, or {@code null} if the command's class was
     * not annotated.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static public Map<String, Set<Permission>> requiredPermissions(
    		final Class<? extends Command> cmdClass) {
    	
        final RequiredPermissions requiredPerms = 
        		cmdClass.getAnnotation(RequiredPermissions.class);
        if (requiredPerms != null) {
            return singletonMap(requiredPerms.dataverseName(), 
            		Permission.setOf(requiredPerms.value()));
        } else {
            final RequiredPermissionsMap requiredPermsMap = 
            		cmdClass.getAnnotation(RequiredPermissionsMap.class);
            if (requiredPermsMap != null) {
                final Map<String, Set<Permission>> result = new TreeMap<>();
                for (final RequiredPermissions rp : requiredPermsMap.value()) {
                    result.put(rp.dataverseName(), Permission.setOf(rp.value()));
                }
                return result;
            } else {
                final Class superClass = cmdClass.getSuperclass();
                if (superClass != null) {
                    return requiredPermissions(superClass);
                } else {
                    throw new IllegalArgumentException(
                    		"Command class " + 
                    		cmdClass.getCanonicalName() +
                            ", and its superclasses, do not declare required permissions.");
                }
            }
        }
    }
}
