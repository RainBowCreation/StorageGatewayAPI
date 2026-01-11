package net.rainbowcreation.storage.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnableBulkWrite {
    int batchSize() default 100;
    long flushIntervalMillis() default 500;
    boolean immediate() default false;
}