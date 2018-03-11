package autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the annotation for marking a member is treated as a GUI element
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface GuiIncluded {
    /**
     * @return true if the attached member is part of the GUI
     */
    boolean value() default true;

    /**
     * @return custom name instead of the auto-generated name based on the member name
     */
    String name() default "";
}
