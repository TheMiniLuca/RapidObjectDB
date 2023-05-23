package io.github.theminiluca.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SQL {
    /**
     * Moved to value
     * */
    @Deprecated
    String tableName() default "";
    /**
     * TableName
     * */
    String value();
    /**
     * Register Saving Except-or id
     * */
    int savingException() default -1;
    /**
     <Strong>NOT RECOMMENDED. USE ONLY WHEN ERROR OCCURRED</Strong><br/>
     <i>DISK CAN CRITICAL DAMAGED DUE TO THIS OPTION.</i><br/>
     <br/>
     Reset the table and save values at save schedule.
     <br/>
     */
    boolean resetTableAtSave() default false;
    /**
     * Check Value-Changes at saving this thing (only sqlmap supports).
     * */
    boolean checkValueChangesAtSave() default true;
}