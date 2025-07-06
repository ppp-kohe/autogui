package org.autogui;

import org.autogui.base.annotation.GuiComponentOptionSplitPane;
import org.autogui.base.annotation.GuiComponentOptionTabbedPane;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @since 1.8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface GuiComponentOptions {
    GuiComponentOptionTabbedPane tabbedPane() default @GuiComponentOptionTabbedPane();
    GuiComponentOptionSplitPane splitPane() default @GuiComponentOptionSplitPane();
}
