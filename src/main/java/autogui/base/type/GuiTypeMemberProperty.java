package autogui.base.type;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiReprValue;

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
     * execute the property method or obtain value of the property field
     * if target is null then nothing will happen and always return {@link GuiTypeValue#NO_UPDATE}.
     * @param target the property holder
     * @param prevValue a previous value of the property, which can be compared to the new value and if it equals,
     *                   the returned value will be {@link GuiTypeValue#NO_UPDATE}.
     * @return the current property value or {@link GuiTypeValue#NO_UPDATE} if no difference from the prevValue
     * @throws Exception it might be cause exception during the method invocation
     */
    public Object executeGet(Object target, Object prevValue) throws Exception {
        Method getter = getGetter();
        if (getter != null) {
            if (target == null && !Modifier.isStatic(getter.getModifiers())) {
                return GuiTypeValue.NO_UPDATE;
            }
            return compareGet(prevValue, getter.invoke(target));
        } else {
            Field field = getField();
            if (field != null) {
                if (target == null && !Modifier.isStatic(field.getModifiers())) {
                    return GuiTypeValue.NO_UPDATE;
                }
                return compareGet(prevValue, field.get(target));
            }
        }
        throw new UnsupportedOperationException("no getter: " + name);
    }

    /**
     * use {@link Objects#equals(Object, Object)} or
     *  if {@link GuiTypeValue}, use {@link GuiTypeValue#equals(Object, Object)}.
     * @param prevValue the compared value 1
     * @param newValue the compared value 2
     * @return <code>newValue</code> if equivalent, or {@link GuiTypeValue#NO_UPDATE}.
     */
    public Object compareGet(Object prevValue, Object newValue) {
        boolean eq;
        if (type != null && type instanceof GuiTypeValue) {
            eq = ((GuiTypeValue) type).equals(prevValue, newValue);
        } else {
            eq = Objects.equals(prevValue, newValue);
        }
        if (eq) {
            return GuiTypeValue.NO_UPDATE;
        } else {
            return newValue;
        }
    }

    /**
     * execute the setter method or set the value of the property field
     * if the target is null, nothing will happen.
     * @param target the field or setter target
     * @param value the value to be set
     * @return setter returned value or null
     * @throws Exception thrown in the setter
     */
    public Object executeSet(Object target, Object value) throws Exception {
        Method setter = getSetter();
        if (setter != null) {
            if (target == null && !Modifier.isStatic(getter.getModifiers())) {
                return null;
            }
            return setter.invoke(target, value);
        } else {
            Field field = getField();
            if (field != null) {
                if (target == null && !Modifier.isStatic(field.getModifiers())) {
                    return null;
                }
                field.set(target, value);
                return null;
            }
        }
        throw new UnsupportedOperationException("no setter: " + name);
    }

    /** @return true if it has a setter of a field */
    public boolean isWritable() {
        return getSetter() != null || getField() != null;
    }

    /**
     * The default implementation just calls {@link #executeGet(Object, Object)}
     * @param index  index of the target in the list
     * @param target  an element object
     * @param prevValue it might be null
     * @return property value of the target which is an element of a list
     * @throws Exception on error
     */
    public Object executeGetList(int index, Object target, Object prevValue) throws Exception {
       return executeGet(target, prevValue);
    }

    /**
     *  the default implementation just calls {@link #executeSet(Object, Object)}
     * @param index the index in the list
     * @param target an element object
     * @param value the value to be set
     * @return the returned value of the setter or null
     * @throws Exception thrown in the setter
     */
    public Object executeSetList(int index, Object target, Object value) throws Exception {
        return executeSet(target, value);
    }

    @Override
    public String toString() {
        return "property(" + type + ", " + name + ")";
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = join(join(
                    description(getField()),
                    description(getGetter())),
                    description(getSetter()));
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

    private String select(String... ss) {
        for (String s : ss) {
            if (!s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    private String join(String a, String b) {
        return a.isEmpty() ? b :
                (b.isEmpty() ? a : a + "\t" + b);
    }

    private String description(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiIncluded.class).description() :
                "";
    }

    private String keyStroke(AnnotatedElement e) {
        return e != null && e.isAnnotationPresent(GuiIncluded.class) ?
                e.getAnnotation(GuiIncluded.class).keyStroke() :
                "";
    }
}
