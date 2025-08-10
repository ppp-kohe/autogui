package org.autogui.base.annotation;

/**
 * initial settings for split-pane: determines the default component orientation of the attached type
 * <pre>
 *     &#64;GuiIncluded
 *     &#64;GuiInits(splitPane = &#64;{@link GuiInitSplitPane}(vertical=true)
 *                    tabbedPane = &#64;{@link GuiInitTabbedPane}(noTab=true))
 *     public class MyApp {  //the created GUI components has the splitted-pane of "Pane1" and "Pane2",
 *                           // and the orientation of the split becomes vertical.
 *                           // The tabbedPane setting can suppress creating a tabbed-pane, instead force to create a split-pane
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
public @interface GuiInitSplitPane {
    /**
     * @return indicating the split-pane orientation is vertical. the default is false
     */
    boolean vertical() default false;
}
