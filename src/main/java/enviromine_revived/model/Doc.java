package enviromine_revived.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a tuning value. The server config is generated from these (comment and allowed
 * range), so every number in the tuning records is editable without extra code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
public @interface Doc {
    /** Shown as the comment in the config file. */
    String value();

    double min() default -1000;

    double max() default 1000;
}
