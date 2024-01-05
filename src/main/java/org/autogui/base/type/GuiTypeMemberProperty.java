package org.autogui.base.type;

import org.autogui.GuiIncluded;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * a type information about a property.
 * <pre>
 *     public R setP(E e) { ... }
 *     public E getP() { ... }
 * </pre>
 *
 * <pre>
 *     public R setP(boolean e) { ... }
 *     public boolean isP() { ... }
 * </pre>
 *
 * <pre>
 *     public E p;
 * </pre>
 *
 * children: the type of the property.
 * */
public class GuiTypeMemberProperty extends GuiTypeMember {
    protected String setterName;
    protected String getterName;
    protected String fieldName;
    protected GuiTypeElement type;

    protected Method setter;
    protected Method getter;
    protected Field field;

    protected String description;
    protected String keyStroke;
    /** true if the history value storing is supported
     * @since 1.2 */
    protected Boolean history;

    public GuiTypeMemberProperty(String name) {
        super(name);
    }

    public GuiTypeMemberProperty(String name, String setterName, String getterName, String fieldName, GuiTypeElement type) {
        super(name);
        this.setterName = setterName;
        this.getterName = getterName;
        this.fieldName = fieldName;
        this.type = type;
    }

    public GuiTypeMemberProperty(String name, Method setter, Method getter, Field field, GuiTypeElement type) {
        this(name, setter.getName(), getter.getName(), field.getName(), type);
        this.setter = setter;
        this.getter = getter;
        this.field = field;
        this.type = type;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Method getSetter() {
        if (setter == null && setterName != null) {
            GuiTypeBuilder b = new GuiTypeBuilder();
            setter = findOwnerMethod(setterName, b::isSetterMethod);
        }
        return setter;
    }

    public Method getGetter() {
        if (getter == null && getterName != null) {
            GuiTypeBuilder b = new GuiTypeBuilder();
            getter = findOwnerMethod(getterName, b::isGetterMethod);
        }
        return getter;
    }

    public Field getField() {
        if (field != null && fieldName != null && getOwner() != null) {
            Class<?> type = getOwner().getType();
            if (type != null) {
                GuiTypeBuilder b = new GuiTypeBuilder();
                field = Arrays.stream(type.getFields())
                        .filter(f -> f.getName().equals(fieldName))
                        .filter(b::isMemberField)
                        .findFirst()
                        .orElse(null);
            }
        }
        return field;
    }

    public void setSetterName(String setterName) {
        this.setterName = setterName;
    }

    public String getSetterName() {
        return setterName;
    }

    public void setGetterName(String getterName) {
        this.getterName = getterName;
    }

    public String getGetterName() {
        return getterName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setType(GuiTypeElement type) {
        this.type = type;
    }

    /** @return property type */
    public GuiTypeElement getType() {
        return type;
    }

    /** @return singleton with the property type */
    @Override
    public List<GuiTypeElement> getChildren() {
        if (type == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(type);
        }
    }

    /**
     * execute the property method or obtain the value of the property field
     * @param target the property holder, nullable.
     * @return the property value or {@link GuiUpdatedValue#NO_UPDATE} if the target is null or the property is static
     * @throws Exception it might be caused exception during the execution
     */
    public GuiUpdatedValue executeGet(Object target) throws Exception {
        Method getter = getGetter();
        if (getter != null) {
            if (target == null && !Modifier.isStatic(getter.getModifiers())) {
                return GuiUpdatedValue.NO_UPDATE;
            }
            return GuiUpdatedValue.of(getter.invoke(target));
        } else {
            Field field = getField();
            if (field != null) {
                if (target == null && !Modifier.isStatic(field.getModifiers())) {
                    return GuiUpdatedValue.NO_UPDATE;
                }
                return GuiUpdatedValue.of(field.get(target));
            }
        }
        throw new UnsupportedOperationException("no getter: " + name);
    }

    /**
     * execute the property method or obtain the value of the property field
     * if target is null then nothing will happen and always return {@link GuiUpdatedValue#NO_UPDATE}.
     * @param target the property holder
     * @param prevValue a previous value of the property, which can be compared to the new value and if it equals,
     *                   the returned value will be {@link GuiUpdatedValue#NO_UPDATE}.
     * @return the current property value or {@link GuiUpdatedValue#NO_UPDATE} if no difference from the prevValue
     * @throws Exception it might be caused exception during the method invocation
     */
    public GuiUpdatedValue executeGet(Object target, Object prevValue) throws Exception {
        GuiUpdatedValue v = executeGet(target);
        if (v.isNone()) {
            return v;
        } else {
            return compareGet(prevValue, v.getValue());
        }
    }

    /**
     * use {@link Objects#equals(Object, Object)} or
     *  if {@link GuiTypeValue}, use {@link GuiTypeValue#equals(Object, Object)}.
     * @param prevValue the compared value 1
     * @param newValue the compared value 2
     * @return <code>newValue</code> if equivalent, or {@link GuiUpdatedValue#NO_UPDATE}.
     */
    public GuiUpdatedValue compareGet(Object prevValue, Object newValue) {
        boolean eq;
        if (type instanceof GuiTypeValue) {
            eq = ((GuiTypeValue) type).equals(prevValue, newValue);
        } else {
            eq = Objects.equals(prevValue, newValue);
        }
        if (eq) {
            return GuiUpdatedValue.NO_UPDATE;
        } else {
            return GuiUpdatedValue.of(newValue);
        }
    }

    /**
     * execute the setter method or set the value of the property field
     * if the target is null, nothing will happen.
     * @param target the field or setter target
     * @param value the value to be set
     * @return value, or null if error
     * @throws Exception thrown in the setter
     */
    public Object executeSet(Object target, Object value) throws Exception {
        Method setter = getSetter();
        if (setter != null) {
            if (target == null && !Modifier.isStatic(getter.getModifiers())) {
                return null;
            }
            setter.invoke(target, value);
            return value;
        } else {
            Field field = getField();
            if (field != null) {
                if (target == null && !Modifier.isStatic(field.getModifiers())) {
                    return null;
                }
                field.set(target, value);
                return value;
            }
        }
        throw new UnsupportedOperationException("no setter: " + name);
    }

    /** @return true if it has a setter or a field */
    public boolean isWritable() {
        return getSetter() != null || getField() != null;
    }

    @Override
    public String toString() {
        return "property(" + type + ", " + name + ")";
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = join(join(join(
                    description(getField()),
                    description(getGetter())),
                    description(getSetter())),
                    type != null ? type.getDescription() : "");
        }
        return description;
    }


    @Override
    public String getAcceleratorKeyStroke() {
        if (keyStroke == null) {
            keyStroke = select(
                    keyStroke(getField()),
                    keyStroke(getGetter()),
                    keyStroke(getSetter()));
        }
        return keyStroke;
    }

    protected String select(String... ss) {
        for (String s : ss) {
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    protected String join(String a, String b) {
        return a.isEmpty() ? b :
                (b.isEmpty() ? a : a + "\t" + b);
    }

    protected String description(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiIncluded.class).description() :
                "";
    }

    protected String keyStroke(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiIncluded.class).keyStroke() :
                "";
    }

    /**
     * @return true if a member for the property returns true by {@link #history(AnnotatedElement)}.
     * @since 1.2
     */
    public boolean isHistoryValueSupported() {
        if (history == null) {
            history = select(
                    history(getField()),
                    history(getGetter()),
                    history(getSetter()));
        }
        return history;
    }

    /**
     * @param bs nullable booleans
     * @return first non-null item in bs
     * @since 1.2
     */
    protected boolean select(Boolean... bs) {
        for (Boolean b : bs) {
            if (b != null) {
                return b;
            }
        }
        return true;
    }

    /**
     * @param e the tested annotation, nullable
     * @return true e has {@link GuiIncluded#history()}==true
     * @since 1.2
     */
    protected Boolean history(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiIncluded.class).history() :
                null;
    }
}
