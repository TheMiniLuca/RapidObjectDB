package io.github.theminiluca.rapidobjectdb.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SQL {
    String value();
}
