package net.jadenxgamer.elysium_api.api.charon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Toll {
    String value();
    String target() default "";
    int ordinal() default -1;
    Shift shift() default Shift.NONE;
    int by() default 0;
    String[] args() default {};
    int opcode() default -1;

    enum Shift {
        NONE,
        BEFORE,
        AFTER,
        BY
    }
}