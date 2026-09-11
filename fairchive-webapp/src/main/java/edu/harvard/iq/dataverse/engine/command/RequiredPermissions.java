package edu.harvard.iq.dataverse.engine.command;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import edu.harvard.iq.dataverse.persistence.user.Permission;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface RequiredPermissions {
	
    Permission[] value();

    boolean isAllPermissionsRequired() default true;

    String dataverseName() default "";
}
