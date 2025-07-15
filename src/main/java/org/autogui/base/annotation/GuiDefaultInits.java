package org.autogui.base.annotation;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;

import java.lang.reflect.AnnotatedElement;

/**
 * the dummy class for obtaining the default instance of {@link GuiInits}
 * @since 1.8
 */
@GuiInits
public class GuiDefaultInits {
    /**
     * @return the default instance of options
     */
    public static GuiInits get() {
        return GuiDefaultInits.class.getAnnotation(GuiInits.class);
    }

    /**
     *
     * @param e the attached member {@link Class}, {@link java.lang.reflect.Method} or {@link java.lang.reflect.Field}
     * @return the attached or the default
     */
    public static GuiInits getOrNull(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiInits.class) : null;
    }

    /**
     * @param e the attached member
     * @return non-null {@link #getOrNull(AnnotatedElement)} or the default {@link #get()}
     */
    public static GuiInits get(AnnotatedElement e) {
        var i = getOrNull(e);
        return i == null ? get() : i;
    }
}
