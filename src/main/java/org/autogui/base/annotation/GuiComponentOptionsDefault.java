package org.autogui.base.annotation;

import org.autogui.GuiComponentOptions;

/**
 * the dummy class for obtaining the default instance of {@link GuiComponentOptions}
 * @since 1.8
 */
@GuiComponentOptions
public class GuiComponentOptionsDefault {
    /**
     * @return the default instance of options
     */
    public static GuiComponentOptions get() {
        return GuiComponentOptionsDefault.class.getAnnotation(GuiComponentOptions.class);
    }
}
