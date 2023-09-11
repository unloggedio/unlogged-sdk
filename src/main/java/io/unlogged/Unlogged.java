package io.unlogged;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add @Unlogged annotation at the entrypoint of your application to start recording
 * code execution. The recording can be replayed using the Unlogged IntelliJ plugin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Unlogged {
    /**
     * Comma separated list of package names which are to be included for recording
     * @return Array of strings, each one being a package name
     */
    String[] includePackage() default "";

    boolean enable() default true;

}
