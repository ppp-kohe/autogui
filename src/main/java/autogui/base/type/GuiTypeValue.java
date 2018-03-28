package autogui.base.type;

import autogui.GuiIncluded;

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

    /** @return the default implementation returns the class
     * obtained from {@link Class#forName(String)} with the {@link #name} */
    public Class<?> getType() {
        if (type == null) {
            try {
                type = Class.forName(name);
            } catch (Exception ex) {
                type = null;
            }
        }
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
     *
     * @param prevValue  the previous value
     * @return returns a value other than {@link #NO_UPDATE} if the value is updated from the prevValue;
     *   the returned value is a next(current) state of the prevValue.
     *   Even if the returned value is equivalent to the prevValue, it is non-NO_UPDATE and then it indicates an update.
     * <p>
     * the default implementation returns NO_UPDATE.
     *  The main purpose of the method is provide extensibility for GUI source.
     */
    public Object updatedValue(Object prevValue) {
        return NO_UPDATE;
    }

    public Object updatedValueList(int index, Object prevValue) {
        return updatedValue(prevValue);
    }

    /**
     * @param value the checked value
     * @return whether the value is writable or not. default is true */
    public boolean isWritable(Object value) {
        return true;
    }

    /**
     * @param prevValue ignored
     * @param newValue used as the returned value
     * @return newValue
     */
    public Object writeValue(Object prevValue, Object newValue) {
        return newValue;
    }

    public Object writeValueList(int index, Object prevValue, Object newValue) {
        return writeValue(prevValue, newValue);
    }

    public static NoUpdate NO_UPDATE = NoUpdate.NoUpdate;

    /** a constant for describing a property is not updated */
    public enum NoUpdate {
        NoUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GuiTypeValue that = (GuiTypeValue) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /** check equality 2 objects: default implementation is calling (shallow) equals
     * @param prevValue the compared value 1
     * @param nextValue the compared value 2
     * @return use Objects.equals
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
}
