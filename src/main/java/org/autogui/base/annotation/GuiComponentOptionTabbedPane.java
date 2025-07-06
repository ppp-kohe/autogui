package org.autogui.base.annotation;

/**
 * options for tabbled-pane: determines the pane of the attached type becomes tab or not.
 * @since 1.8
 */
public @interface GuiComponentOptionTabbedPane {
    /**
     * @return the default is false
     */
    boolean noTab() default false;
}
