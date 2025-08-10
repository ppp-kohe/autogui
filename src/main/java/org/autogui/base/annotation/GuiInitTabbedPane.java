package org.autogui.base.annotation;

/**
 * initial settings for tabbled-pane: determines the pane of the attached type becomes tab or not.
 * <pre>
 *     &#64;GuiIncluded
 *     &#64;GuiInits(tabgedPane = &#64;{@link GuiInitTabbedPane}(noTab=true))
 *     public class MyApp {  //the created GUI components always has the split-pane of "Pane1" and "Pane2", instead of a tabbed-pane.
 *         &#64;GuiIncluded
 *         public MySubPane1 getPane1() {
 *             ...
 *         }
 *         &#64;GuiIncluded
 *         public MySubPane2 getPane2() {
 *             ...
 *         }
 *         ...
 *     }
 * </pre>
 * @see org.autogui.GuiInits
 * @since 1.8
 */
public @interface GuiInitTabbedPane {
    /**
     * @return indicating suppress object-panes summrized as a tabbed pane. the default is false
     */
    boolean noTab() default false;
}
