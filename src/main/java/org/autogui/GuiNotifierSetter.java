package org.autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates a setter method that receives a notifier for the target member
 * <pre>
 *     &#64;GuiIncluded
 *     class C {
 *         &#64;GuiIncluded public int value;
 *         &#64;GuiIncluded public String name;
 *
 *         Runnable valueNotifier;
 *         Runnable nameNotifier;
 *         Runnable selfNotifier;
 *
 *         {
 *             new Thread(() -&gt; {
 *                while (true) {
 *                  if (valueNotifier != null) valueNotifier.run();
 *                  if (nameNotifier != null) nameNotifier.run();
 *                  if (selfNotifier != null) selfNotifier.run();
 *                  sleep();
 *                }
 *             }).start();
 *         }
 *
 *         //updater for the pane of the property "value" from the setter name "set...Notifier"
 *         &#64;GuiNotifierSetter
 *         public void setValueNotifier(Runnable notifier) {
 *             this.valueNotifier = notifier;
 *         }
 *
 *         //updater for the pane of the property "name"
 *         &#64;GuiNotifierSetter(target="name")
 *         public void setNameUpdater(Runnable notifier) {
 *            this.nameNotifier = notifier;
 *         }
 *
 *         //updater for the pane of the entire pane of the C object
 *         &#64;GuiNotifierSetter
 *         public void setNotifier(Runnable notifier) {
 *            this.selfNotifier = notifier;
 *         }
 *
 *         ...
 *     }
 * </pre>
 * @since 1.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GuiNotifierSetter {
    String TARGET_SELF = "";
    String TARGET_FROM_NAME = "-";
    String TARGET_ROOT = "*";

    /**
     *
     * @return the name of the target of notifying,
     *   can be another property name (starting with a small-case letter) or
     *     {@link #TARGET_SELF} meaning the owner type itself,
     *     {@link #TARGET_FROM_NAME} meaning that
     *         the name is determined by the name of the attached setter "set<i>Target</i>Notifier" as default,
     *       or {@link #TARGET_ROOT} meaning the entire pane from the root.
     */
    String target() default TARGET_FROM_NAME;
}
