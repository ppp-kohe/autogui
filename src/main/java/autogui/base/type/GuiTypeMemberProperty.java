package autogui.base.type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 *     public R setP(E e) { ... }
 *     public E getP() { ... }
 * </pre>
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

    public GuiTypeElement getType() {
        return type;
    }

    @Override
    public List<GuiTypeElement> getChildren() {
        if (type == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(type);
        }
    }

    /**
     *
     * @param target the property holder
     * @param prevValue a previous value of the property, which can be compared to the new value and if it equals,
     *                   the returned value will be {@link GuiTypeValue#NO_UPDATE}.
     * @return the current property value or {@link GuiTypeValue#NO_UPDATE} if no difference from the prevValue
     * @throws Exception it might be cause exception during the method invocation
     */
    public Object executeGet(Object target, Object prevValue) throws Exception {
        Method getter = getGetter();
        if (getter != null) {
            return compareGet(prevValue, getter.invoke(target));
        } else {
            Field field = getField();
            if (field != null) {
                return compareGet(prevValue, field.get(target));
            }
        }
        throw new UnsupportedOperationException("no getter: " + name);
    }

    public Object compareGet(Object prevValue, Object newValue) {
        if (Objects.equals(prevValue, newValue)) {
            return GuiTypeValue.NO_UPDATE;
        } else {
            return newValue;
        }
    }

    public Object executeSet(Object target, Object value) throws Exception {
        Method setter = getSetter();
        if (setter != null) {
            return setter.invoke(target, value);
        } else {
            Field field = getField();
            if (field != null) {
                field.set(target, value);
                return null;
            }
        }
        throw new UnsupportedOperationException("no setter: " + name);
    }

    public boolean isWritable() {
        return getSetter() != null || getField() != null;
    }
}
