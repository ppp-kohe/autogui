package org.autogui.base.type;

import org.autogui.GuiIncluded;

import java.util.Objects;

/**
 * primitive types, their associated boxed types, String, ...
 *   specified by {@link GuiTypeBuilder#getValueTypes()}
 */
public class GuiTypeValue implements GuiTypeElement {
    protected String name;

    protected Class<?> type;

    public GuiTypeValue(Class<?> type) {
        this(type.getSimpleName());
        this.type = type;
    }

    public GuiTypeValue(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** @return the default implementation returns the class.
     *  1.6.3 removed the feature of calling Class.forName(name)
     *  */
    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "type(" + name + ")";
    }

    @Override
    public String getDescription() {
        Class<?> cls = getType();
        if (cls != null) {
            GuiIncluded included = cls.getAnnotation(GuiIncluded.class);
            if (included != null) {
                return included.description();
            }
        }
        return "";
    }

    @Override
    public String getAcceleratorKeyStroke() {
        Class<?> cls = getType();
        if (cls != null) {
            GuiIncluded included = cls.getAnnotation(GuiIncluded.class);
            if (included != null) {
                return included.keyStroke();
            }
        }
        return "";
    }

    /**
     * @return NO_UPDATE. a subclass may return a special value
     */
    public GuiUpdatedValue getValue() {
        return GuiUpdatedValue.NO_UPDATE;
    }

    /**
     * @param inheritedValue the value returned by the upper property
     * @return inheritedValue if {@link #getValue()} returns NO_UPDATE
     */
    public GuiUpdatedValue getValue(Object inheritedValue) {
        GuiUpdatedValue upValue = getValue();
        if (upValue.isNone()) {
            return GuiUpdatedValue.of(inheritedValue);
        } else {
            return upValue;
        }
    }

    /**
     *
     * @param prevValue  the previous value
     * @return a value other than {@link GuiUpdatedValue#NO_UPDATE} if the value is updated from the prevValue;
     *   the returned value is a next(current) state of the prevValue.
     *   Even if the returned value is equivalent to the prevValue, it is non-NO_UPDATE, and then it indicates an update.
     * <p>
     * the default implementation simply call {@link #getValue()} and it will return NO_UPDATE.
     *  The main purpose of the method is providing extensibility for GUI source.
     */
    public GuiUpdatedValue updatedValue(Object prevValue) {
        return getValue();
    }

    /**
     *
     * @param prevValue the previous value
     * @param inheritedValue the value obtained from upper type:
     *                      e.g. for property(type) case, property value is supplied as the inherited value
     * @return {@link #updatedValue(Object)}, or NO_UPDATE if both values are same as prevValue (NO_UPDATE)
     */
    public GuiUpdatedValue updatedValue(Object prevValue, Object inheritedValue) {
        GuiUpdatedValue upValue = updatedValue(prevValue);
        if (upValue.isNone()) {
            if (equals(prevValue, inheritedValue)) {
                return GuiUpdatedValue.NO_UPDATE;
            } else {
                return GuiUpdatedValue.of(inheritedValue);
            }
        } else {
            return upValue;
        }
    }

    /**
     * @param value the checked value
     * @return whether the value is writable or not. default is true */
    public boolean isWritable(Object value) {
        return true;
    }

    /**
     * nothing to do in the class.
     * @param newValue used as the returned value
     * @return newValue
     */
    public Object writeValue(Object newValue) {
        return newValue;
    }

    /**
     * @param prevValue ignored
     * @param newValue used as the returned value
     * @return newValue, by calling {@link #writeValue(Object)}
     */
    public Object writeValue(Object prevValue, Object newValue) {
        return writeValue(newValue);
    }

    public Object writeValueInheritedValue(Object inheritedValue, Object newValue) {
        return writeValue(newValue);
    }

    public Object writeValue(Object prevValue, Object inheritedValue, Object newValue) {
        return writeValue(prevValue, newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GuiTypeValue that = (GuiTypeValue) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /** check equality 2 objects: default implementation is calling (shallow) equals
     * @param prevValue the compared value 1
     * @param nextValue the compared value 2
     * @return use {@link Objects#equals(Object, Object)}
     */
    public boolean equals(Object prevValue, Object nextValue) {
        return Objects.equals(prevValue, nextValue);
    }

    /** optional operation
     * @return a newly created by a default constructor, or null  */
    public Object createNewValue() {
        try {
            return getType().getConstructor().newInstance();
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * @return true if the type implements {@link AutoCloseable}
     */
    public boolean isAutoCloseable() {
        return AutoCloseable.class.isAssignableFrom(getType());
    }
}
