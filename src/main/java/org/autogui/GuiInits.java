package org.autogui;

import org.autogui.base.annotation.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * aggregation of static preferences of components.
 * It can control initial settings of come components.
 * <p>
 * For example, a split-pane has the flag for determining it's splitting orientation, vertical or horizontal.
 * In the library, the user can change the flag after GUI component construction.
 * This annotation can provide the initial state of the flag as like the following:
 * <pre>
 *    &#64;GuiIncluded
 *    &#64;GuiInits(splitPane = &#64;{@link GuiInitSplitPane}(vertical=true))
 *    public class MyApp {
 *         &#64;GuiIncluded
 *         public MySubPane1 getPane1() {
 *             ...
 *         }
 *         &#64;GuiIncluded
 *         public MySubPane2 getPane2() {
 *             ...
 *         }
 *    }
 * </pre>
 * <p>
 * This annotation can be attached to a class or a member.
 * Where to attach it depends on the attribute
 *  (e.g. for {@link #splitPane()}, it needs to be attached to a class, but for {@link #action()}, to a method).
 * <p>
 * The settings provided by the annotation might be overwritten by user's preferences.
 * <p>
 *  The default values of the annotation equals to abcense of the annotation.
 * @since 1.8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface GuiInits {
    GuiInitTabbedPane tabbedPane() default @GuiInitTabbedPane();
    GuiInitSplitPane splitPane() default @GuiInitSplitPane();
    GuiInitWindow window() default @GuiInitWindow();

    GuiInitTable table() default  @GuiInitTable();
    GuiInitTableColumn tableColumn() default @GuiInitTableColumn();
    GuiInitNumberSpinner numberSpinner() default @GuiInitNumberSpinner();
    GuiInitAction action() default @GuiInitAction();
}
