package org.autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the annotation for marking a member as a GUI element
 * <pre>
 *     //example
 *     &#64;{@link GuiIncluded}
 *     class C {
 *         &#64;{@link GuiIncluded}(index=1)
 *         public int fld;
 *
 *         &#64;{@link GuiIncluded}(index=2, description="read-only value")
 *         public String getReadOnlyProp() { ... }
 *
 *         &#64;{@link GuiIncluded}(index=3, description="property by setter and getter")
 *         public String getValue() { ... }
 *         public void setValue(String v) { ... }
 *
 *         &#64;{@link GuiIncluded}(index=4, description="action")
 *         public void action() { ... }
 *
 *         &#64;{@link GuiIncluded}(index=5, name="customizedName")
 *         public float v;
 *
 *         &#64;{@link GuiIncluded}(index=6, description="table by list of string")
 *         public List&lt;String&gt; strTable = ...;
 *
 *         &#64;{@link GuiIncluded}(index=7, description="table by list of E")
 *         public List&lt;E&gt; eTable = ...;
 *
 *         &#64;{@link GuiIncluded}
 *         public void actionForSelectedItems(List&lt;E&gt; items) {...}
 *
 *         &#64;{@link GuiIncluded}
 *         public void actionForSelectedRows(List&lt;Integer&gt; rows, String tableName) {...}
 *     }
 *
 *     &#64;{@link GuiIncluded} //column class
 *     class E {
 *         &#64;{@link GuiIncluded}
 *         public boolean flag;
 *
 *         &#64;{@link GuiIncluded}
 *         public EnumVal select;
 *
 *         &#64;{@link GuiIncluded}
 *         public void actionForSelectedRow() { ... }
 *     }
 *
 *     public enum EnumVal { ... }
 * </pre>
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
     * @return ordinal index for sorting members. the default value is the max value of short.
     *   for properties, minimum value is used from field, getter or setter.
     */
    int index() default Short.MAX_VALUE;

    /**
     * @return short description for the target, typically presented as a tool-tip.
     *    for properties, combines field, getter and setter.
     */
    String description() default "";

    /**
     * @return accelerator key stroke.
     *  For example,
     *  <pre>
     *      &#64;GuiIncluded(keyStroke="K") public String prop;
     *  </pre>
     *  then, users can focus to the field "prop" by typing Cmd or Ctrl +K.
     *  For an action method, users can invoke the method by typing the specified key.
     *  <p>
     *  The string will be passed to KeyStroke.getStroke(String), "none" or "".
     *    <pre>
     *        "none" | control* key
     *
     *        control ::= "shift" | "alt"        //lower cases ("meta" or "control" is automatically appended)
     *        key  ::= "0" | "1" | ... | "9" | "A" | "B" | ... | "Z"  //upper cases
     *    </pre>
     *    examples:
     *    <pre>
     *        "shift K"
     *        "shift alt F"
     *    </pre>
     *    for properties, selects one of field(high-precedence), getter or setter.
     *    if the stroke is empty (default), then the first character of the name will be used.
     *    if the stroke is "none", then key-binding will be skipped for the target.
     *    <p>
     *  Some keys are reserved with combination of Meta(Command) or Ctrl.
     *  <ul>
     *      <li>reserved keys:
     *            Q (Quit), W (Window close), shift R (Refresh),
     *            A (Select all), shift A (un-select),
     *            Z (Undo), shift Z (Redo),
     *            O (Open), S (Save),
     *            X (Cut), C (Copy), V (Paste),
     *            alt O (JSON Open), alt S (JSON Save),
     *            alt X (JSON Cut), alt C (JSON Copy), alt V (JSON Paste),
     *            ','  (settings)
     *            </li>
     *      <li>conditionally used keys by some components:
     *            shift B (open-in-browser for texts),
     *            shift I (increment for numbers or zoom-in for images),
     *            shift D (decrement for numbers or zoom-out for images),
     *            shift ',' (settings for numbers or documents)
     *            </li>
     *  </ul>
     */
    String keyStroke() default "";

    /**
     * @return if true, enables history-saving for the target property
     * @since 1.2
     */
    boolean history() default true;

    /**
     *
     * @return if true and the target returns non-void type, the target is handled as an action, not an accessor
     * @since 1.2
     */
    boolean action() default false;
}
