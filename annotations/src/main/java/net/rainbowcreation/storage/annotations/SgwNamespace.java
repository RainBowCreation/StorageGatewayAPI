package net.rainbowcreation.storage.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface SgwNamespace {
    String value();
    int version() default 1;
}