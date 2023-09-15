package io.github.theminiluca.rapidobjectdb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a mark that shows "This needed to be in database"
 * @version 2.0.9-SNAPSHOT
 * @since 2.0.0-SNAPSHOT
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SQL {
    /**
     * Name of the table that this will be saved in.
     * */
    String value();
}
