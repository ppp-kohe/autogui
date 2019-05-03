package org.autogui.base.type;

import org.autogui.GuiNotifierSetter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * a type information about setter of notifier: basically, it is intended to have only a setter with the {@link GuiNotifierSetter} annotation
 * <pre>
 *   &#64;{@link GuiNotifierSetter} public void setNotifier(Runnable r) { ... }
 * </pre>
 * The name of the setter is intended to be "set<i>Target</i>Notifier",
 *     where <i>Target</i> specifies another name <i>target</i> of the property of the owner type as the target of notifying,
 *           or the empty string "" if the target is the object of the type itself.
 *  <p>
 *       {@link GuiNotifierSetter#target()} can explicitly set the <i>target</i> name and
 *           there are 3 special names, {@link GuiNotifierSetter#TARGET_FROM_NAME} (default, and then above <i>Target</i> is used),
 *                 {@link GuiNotifierSetter#TARGET_SELF} and {@link GuiNotifierSetter#TARGET_ROOT}.
 *   </p>
 */
public class GuiTypeMemberPropertyNotifier extends GuiTypeMemberProperty {
    protected String targetName;

    public GuiTypeMemberPropertyNotifier(String name) {
        super(name);
    }

    public GuiTypeMemberPropertyNotifier(String name, Method getter, Method setter, Field field, GuiTypeElement type) {
        super(name, setter, getter, field, type);
    }

    public GuiTypeMemberPropertyNotifier(String name, String setterName, String getterName, String fieldName, GuiTypeElement type) {
        super(name, setterName, getterName, fieldName, type);
    }

    public boolean isTargetSelf() {
        String t = getTargetName();
        return t != null && t.equals(GuiNotifierSetter.TARGET_SELF);
    }

    public boolean isTargetRoot() {
        String t = getTargetName();
        return t != null && t.equals(GuiNotifierSetter.TARGET_ROOT);
    }

    public String getTargetName() {
        if (targetName == null) {
            targetName = select(
                    targetName(getField()),
                    targetName(getGetter()),
                    targetName(getSetter()));
        }
        return targetName;
    }


    protected String targetName(AnnotatedElement e) {
        if (e != null && e.isAnnotationPresent(GuiNotifierSetter.class)) {
            String annotationName = e.getAnnotation(GuiNotifierSetter.class).target();
            if (annotationName.equals(GuiNotifierSetter.TARGET_SELF)) {
                return "";
            } else if (annotationName.equals(GuiNotifierSetter.TARGET_FROM_NAME)) {
                return getNamePrefix("Notifier", getName());
            } else {
                return annotationName;
            }
        } else {
            return null;
        }
    }

    public String getNamePrefix(String tail, String name) {
        if (name != null && name.endsWith(tail)) {
            String s = name.substring(0, tail.length());
            if (s.length() > 1) {
                return Character.toLowerCase(s.charAt(0)) + s.substring(1);
            } else if (s.length() == 1) {
                return s.toLowerCase();
            } else {
                return "";
            }
        } else {
            return null;
        }
    }
}
