package org.autogui.base.annotation;

/**
 * options for split-pane: determines the default component orientation of the attached type
 * @since 1.8
 */
public @interface GuiComponentOptionSplitPane {
    /**
     * @return the default is true
     */
    boolean horizontal() default true;
}
