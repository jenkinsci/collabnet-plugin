package hudson.plugins.collabnet;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates a parameter that should be injected from {@link TestConfiguration}.
 * 
 * @author Kohsuke Kawaguchi
 */
@Retention(RUNTIME)
@Target(FIELD)
@Documented
public @interface TestParam {
}
