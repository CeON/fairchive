package edu.harvard.iq.dataverse.engine.command;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * Base interface for all commands running on Dataverse.
 *
 * @param <R> The type of result this command returns.
 * @author michael
 */
public interface Command<R> {

	public interface PermissionsProvider {
		Set<Permission> getFor(final DataverseRequest request, 
	    		final DvObject object);
	}
	
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

    String describe();
    
    /**
     * Given a {@link Command} sub-class, returns the set of permissions needed
     * to be able to execute it. Needed permissions are specified by annotating
     * the command's class with the {@link RequiredPermissions} annotation.
     *
     * @param cmdClass A class of command
     * @return Set of permissions required to execute the command.
     * @throws IllegalArgumentException if neither the command's class nor any of
     * its superclasses declares required permissions. A command whose permissions
     * are decided at runtime overrides {@link #getRequiredPermissions()} instead.
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, Set<Permission>> requiredPermissions(
    		final Class<? extends Command> cmdClass) {

    	for (Class<?> current = cmdClass; current != null; current = current.getSuperclass()) {
	        final RequiredPermissions requiredPerms =
	        		current.getAnnotation(RequiredPermissions.class);
	        if (requiredPerms != null) {
	            return singletonMap(requiredPerms.dataverseName(),
	            		Permission.setOf(requiredPerms.value()));
	        }
	        final RequiredPermissionsMap requiredPermsMap =
	        		current.getAnnotation(RequiredPermissionsMap.class);
	        if (requiredPermsMap != null) {
	            final Map<String, Set<Permission>> result = new TreeMap<>();
	            for (final RequiredPermissions rp : requiredPermsMap.value()) {
	                result.put(rp.dataverseName(), Permission.setOf(rp.value()));
	            }
	            return result;
	        }
    	}
        // fail loudly rather than running the command unchecked
        throw new IllegalArgumentException("Command class " + cmdClass.getCanonicalName()
                + ", and its superclasses, do not declare required permissions.");
    }

    /**
     * How the permissions returned by {@link #requiredPermissions(Class)} are matched
     * against the ones granted. Declared by {@link RequiredPermissions#strategy()};
     * looked up along the class hierarchy, exactly as the permissions themselves are,
     * so that a subclass cannot silently fall back to a different strategy than the
     * one its superclass declared.
     *
     * @param cmdClass A class of command
     * @return the declared strategy, or {@link Permission.MatchStrategy#allRequired}
     * when none is declared.
     */
    @SuppressWarnings("rawtypes")
    public static Permission.MatchStrategy matchStrategy(final Class<? extends Command> cmdClass) {

    	for (Class<?> current = cmdClass; current != null; current = current.getSuperclass()) {
    		final RequiredPermissions requiredPerms =
    				current.getAnnotation(RequiredPermissions.class);
    		if (requiredPerms != null) {
    			return requiredPerms.strategy();
    		}
    	}
    	return Permission.MatchStrategy.allRequired;
    }
    
	default void verifyPermissions(final PermissionsProvider permissionProvider) {

		final Map<String, Set<Permission>> requiredPermissionsMap = getRequiredPermissions();
		final Map<String, DvObject> affectedObjects = getAffectedDvObjects();
		
		for (final Map.Entry<String, Set<Permission>> pair : requiredPermissionsMap.entrySet()) {
		    final String objectName = pair.getKey();
		    final DvObject object = affectedObjects.get(objectName);
		    if (!affectedObjects.containsKey(objectName)) {
		        throw new RuntimeException("Command instance " + 
		        		getClass().getSimpleName() + 
		        		" does not have a DvObject named '" + objectName + '\'');
		    }

		    final Set<Permission> granted = (object != null) 
		    		? permissionProvider.getFor(getRequest(), object)
		            : Permission.all();
		    final Set<Permission> required = requiredPermissionsMap.get(objectName);
		    
		    final Permission.MatchStrategy strategy = matchStrategy(getClass());

		    if (!required.isEmpty() && !strategy.match(required, granted)) {
		    	final Set<Permission> missing = Permission.differenceBetween(required, granted);
		        throw new PermissionException("Can't execute command " 
		        		+ getClass().getSimpleName()
	                    + ", because request " + getRequest()
	                    + " is missing permissions " + missing
	                    + " on Object " + object.accept(DvObject.NamePrinter),
	                    this, missing, object);
		    }
		}
	}
}
