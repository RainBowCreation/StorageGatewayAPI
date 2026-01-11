package net.rainbowcreation.storage.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QLQuery {
    String namespace();
    String typeName() default ""; // optional
}
