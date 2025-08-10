package org.autogui;

import org.autogui.base.annotation.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * aggregation of static preferences of components.
 * It can control initial settings of some components.
 * <p>
 * For example, a split-pane has the flag for determining it's splitting orientation, vertical or horizontal.
 * In the library, users can change the flag after GUI component construction.
 * This annotation can provide the initial state of the flag as like the following:
 * <pre>
 *    &#64;{@link GuiIncluded}
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
    /** attached to a class with sub-panes <pre>
     * &#64;GuiIncluded &#64;GuiInits(tabbedPane=&#64;{@link GuiInitTabbedPane}(noTab=true))
     * public class C { &#64;GuiIncluded public S1 s1(){...} &#64;GuiIncluded public S1 s1(){...} ...}
     * </pre> */
    GuiInitTabbedPane tabbedPane() default @GuiInitTabbedPane();

    /** attached to a class with sub-panes <pre>
     * &#64;GuiIncluded &#64;GuiInits(splitPane=&#64;{@link GuiInitSplitPane}(vertical=true))
     * public class C { &#64;GuiIncluded public S1 s1(){...} &#64;GuiIncluded public S1 s1(){...} ...}
     * </pre> */
    GuiInitSplitPane splitPane() default @GuiInitSplitPane();
    /** attached to  the class of the application root <pre>
     * &#64;GuiIncluded &#64;GuiInits(window=&#64;{@link GuiInitWindow}(width=600, height=400))
     * public class C { ... }
     * </pre> */
    GuiInitWindow window() default @GuiInitWindow();

    /** attached to a property method or field of a collection-table <pre>
     * &#64;GuiIncluded &#64;GuiInits(table=&#64;{@link GuiInitTable}(rowFitToContent=true))
     * public List&lt;E&gt; table() { ... }
     * </pre> */
    GuiInitTable table() default  @GuiInitTable();
    /**  attached to a property method or field of a collection-column <pre>
     * &#64;GuiIncluded &#64;GuiInits(tableColumn=&#64;{@link GuiInitTableColumn}(width=300))
     * public E column() { ... }
     * </pre> */
    GuiInitTableColumn tableColumn() default @GuiInitTableColumn();
    /**  attached to a property method or field of an editable string collection-column <pre>
     * &#64;GuiIncluded &#64;GuiInits(tableColumnString=&#64;{@link GuiInitTableColumnString}(editFinishByEnterAndKey=true))
     * public String column() { ... }
     * </pre> */
    GuiInitTableColumnString tableColumnString() default @GuiInitTableColumnString();
    /**  attached to a property method or field of a number-spinner  <pre>
     * &#64;GuiIncluded &#64;GuiInits(numberSpinner=&#64;{@link GuiInitNumberSpinner}(format="#.##0"))
     * public int prop() { ... }
     * </pre> */
    GuiInitNumberSpinner numberSpinner() default @GuiInitNumberSpinner();
    /**  attached to an action method  <pre>
     * &#64;GuiIncluded &#64;GuiInits(action=&#64;{@link GuiInitAction}(confirm=true))
     * public void action() { ... }
     * </pre> */
    GuiInitAction action() default @GuiInitAction();
}
