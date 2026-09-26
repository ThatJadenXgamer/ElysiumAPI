package net.jadenxgamer.elysium_api.api.charon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CharonEvent {
    Class<?> target() default void.class;
    String targetName() default "";
    String method();
    Toll at();
}