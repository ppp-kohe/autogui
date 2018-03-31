package autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the annotation for marking a member as a GUI element
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

    /**
     * @return ordinal index for sorting members. the default value is the max value of int.
     *   for properties, minimum value is used from field, getter or setter.
     */
    int index() default Integer.MAX_VALUE;

    /**
     * @return short description for the target, typically presented as a tool-tip.
     *    for properties, combines field, getter and setter.
     */
    String description() default "";

    /**
     * @return accelerator key stroke: passed to KeyStroke.getStroke(String) or "".
     *    <pre>
     *        control* key
     *
     *        control ::= "shift" | "meta" | "control" | "alt"        //lower cases
     *        key  ::= "0" | "1" | ... | "9" | "A" | "B" | ... | "Z"  //upper cases
     *    </pre>
     *
     *    for properties, selects one of field(high-precedence), getter or setter.
     *    if the stroke is empty (default), then the first character of the name will be used.
     */
    String keyStroke() default "";
}
