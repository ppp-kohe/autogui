package autogui.base.type;

import autogui.GuiIncluded;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * the factory for {@link GuiTypeElement}. {@link #get(Type)} can obtain a {@link GuiTypeElement} from {@link Type}.
 *   it's targets are members attached [at]{@link GuiIncluded}.
 *
 * */
public class GuiTypeBuilder {
    protected Map<Type, GuiTypeElement> typeElements = new HashMap<>();

    protected static List<Class<?>> langValueTypes = Arrays.asList(
            Integer.class, Short.class, Byte.class, Long.class, Character.class, Boolean.class,
            Float.class, Double.class, CharSequence.class);

    protected List<Class<?>> valueTypes = new ArrayList<>(langValueTypes);

    public GuiTypeBuilder() {
    }

    public GuiTypeBuilder(List<Class<?>> valueTypes) {
        this.valueTypes = valueTypes;
    }

    public void setValueTypes(List<Class<?>> valueTypes) {
        this.valueTypes = valueTypes;
    }

    /** @return the default value types which are boxed primitive types: {@link Integer} etc. */
    public List<Class<?>> getValueTypes() {
        return valueTypes;
    }

    /**
     * reuse a constructed type element, or {@link #create(Type)}.
     *   the reusing can avoid infinity recursion
     *   @param type for the element
     *   @return the created type element
     *   */
    public GuiTypeElement get(Type type) {
        GuiTypeElement e = typeElements.get(type);
        if (e == null) {
            return create(type);
        }
        return e;
    }

    /**
     * <ul>
     *     <li>for {@link Class}: {@link #createFromClass(Class)}</li>
     *     <li>for {@link ParameterizedType}: obtain the raw class and
     *             check it is a {@link Collection} with a type arg &lt;T&gt;.
     *             then, {@link #createCollectionFromType(ParameterizedType)},
     *             otherwise {@link #createFromClass(Class)} for the raw type</li>
     *     <li>otherwise null</li>
     * </ul>
     * @param type for creation
     * @return nullable
     */
    public GuiTypeElement create(Type type) {
        if (type instanceof Class<?>) {
            return createFromClass((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class<?> rawType = getClass(pType);
            if (Collection.class.isAssignableFrom(rawType) &&
                    pType.getActualTypeArguments().length == 1) { //currently only support C<E>
                return createCollectionFromType(pType);
            } else {
                return createFromClass(rawType);
            }
        } else {
            return null;
        }
    }

    /**
     * @param cls the target class
     * @return
     *    if {@link #isValueType(Class)} or {@link #isExcludedType(Class)} then {@link #createValueFromClass(Class)}
     *    else {@link #createObjectFromClass(Class)}*/
    public GuiTypeElement createFromClass(Class<?> cls) {
        if (isValueType(cls) || isExcludedType(cls)) {
            return createValueFromClass(cls);
        } else {
            return createObjectFromClass(cls);
        }
    }

    /**
     * @param cls the target type
     * @return a new {@link GuiTypeValue} with registering it */
    public GuiTypeValue createValueFromClass(Class<?> cls) {
        GuiTypeValue valueType = new GuiTypeValue(cls);
        put(cls, valueType);
        return valueType;
    }

    /**
     * @param type the target type
     * @return a new {@link GuiTypeCollection} with the raw type and the element type */
    public GuiTypeCollection createCollectionFromType(ParameterizedType type) {
        Class<?> rawType = getClass(type);
        GuiTypeCollection collectionType = new GuiTypeCollection(rawType);
        put(type, collectionType);
        collectionType.setElementType(get(type.getActualTypeArguments()[0]));
        return collectionType;
    }

    /**
     * create a {@link GuiTypeObject}, register it,
     *   and construct field and method members.
     *   those members are grouped by their names
     *   and passed to {@link #createMember(GuiTypeObject, MemberDefinitions)}.
     *
     *   the rules for finding members are {@link #isMemberField(Field)} and {@link #isMemberMethod(Method)}.
     *
     *   @param cls the object class
     *   @return an object type for the class
     *   */
    public GuiTypeObject createObjectFromClass(Class<?> cls) {
        GuiTypeObject objType = new GuiTypeObject(cls);
        put(cls, objType);

        Map<String, MemberDefinitions> definitionsMap = new HashMap<>();

        Arrays.stream(cls.getFields())
                .filter(this::isMemberField)
                .forEachOrdered(f -> definitionsMap.computeIfAbsent(getMemberNameFromField(f), MemberDefinitions::new)
                        .fields.add(f));

        Arrays.stream(cls.getMethods())
                .filter(this::isMemberMethod)
                .forEachOrdered(m -> definitionsMap.computeIfAbsent(getMemberNameFromMethod(m), MemberDefinitions::new)
                        .methods.add(m));

        definitionsMap.values()
                .forEach(d -> createMember(objType, d));

        objType.getProperties().sort(Comparator.comparing(GuiTypeMember::getOrdinal));
        objType.getActions().sort(Comparator.comparing(GuiTypeMember::getOrdinal));

        ((ArrayList<?>) objType.getProperties()).trimToSize();
        ((ArrayList<?>) objType.getActions()).trimToSize();

        return objType;
    }

    /** a temporarily created member group in {@link #createObjectFromClass(Class)} */
    public static class MemberDefinitions {
        public String name;
        public List<Method> methods = new ArrayList<>();
        public List<Field> fields = new ArrayList<>();

        public MemberDefinitions(String name) {
            this.name = name;
        }

        public Field getLastField() {
            return fields.isEmpty() ? null : fields.get(fields.size() - 1);
        }

        public Method getLast(Predicate<Method> builder) {
            Method g = null;
            for (Method m : methods) {
                if (builder.test(m)) {
                    g = m;
                }
            }
            return g;
        }

    }

    protected void put(Type type, GuiTypeElement e) {
        typeElements.put(type, e);
    }

    /**
     * @param cls the tested class
     * @return primitive or {@link #valueTypes} */
    public boolean isValueType(Class<?> cls) {
        return cls.isPrimitive() || valueTypes.contains(cls);
    }

    public boolean isExcludedType(Class<?> cls) {
        return !cls.isAnnotationPresent(GuiIncluded.class);
    }

    /** if the definitions has a field, a getter or a setter, create a property.
     *   otherwise create an action.
     *
     *   @param objType the owner object
     *   @param definitions  the members
     *   */
    public void createMember(GuiTypeObject objType, MemberDefinitions definitions) {
        Field fld = definitions.getLastField();
        Method getter = definitions.getLast(this::isGetterMethod);
        Method setter = definitions.getLast(this::isSetterMethod);
        if (fld != null || setter != null || getter != null) {
            //property
            createMemberProperty(objType, definitions.name, fld, getter, setter);
        } else {
            Method method = definitions.getLast(this::isActionMethod);
            boolean isList = false;
            if (method == null) {
                method = definitions.getLast(this::isActionListMethod);
                isList = true;
            }
            if (method != null) {
                createMemberAction(objType, definitions.name, method, isList);
            }
        }
    }

    public void createMemberProperty(GuiTypeObject objType, String name, Field fld, Method getter, Method setter) {
        //property
        GuiTypeMemberProperty property = new GuiTypeMemberProperty(name);
        property.setOwner(objType);

        Type type = null;
        int index = Integer.MAX_VALUE;
        if (fld != null) {
            property.setField(fld);
            type = fld.getGenericType();
            index = Math.min(index, getMemberOrdinalIndex(fld));
        }
        if (setter != null) {
            property.setSetter(setter);
            type = setter.getGenericParameterTypes()[0];
            index = Math.min(index, getMemberOrdinalIndex(setter));
        }
        if (getter != null) {
            property.setGetter(getter);
            type = getter.getGenericReturnType();
            index = Math.min(index, getMemberOrdinalIndex(getter));
        }

        property.setType(get(type));
        property.setOrdinal(new GuiTypeMemberProperty.MemberOrdinal(index, name));

        objType.getProperties().add(property);
    }

    public int getMemberOrdinalIndex(Method m) {
        if (m.isAnnotationPresent(GuiIncluded.class)) {
            return m.getAnnotation(GuiIncluded.class).index();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public int getMemberOrdinalIndex(Field f) {
        if (f.isAnnotationPresent(GuiIncluded.class)) {
            return f.getAnnotation(GuiIncluded.class).index();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    public void createMemberAction(GuiTypeObject objType, String name, Method method, boolean isList) {
        GuiTypeMemberAction action;
        if (isList) {
            ParameterizedType pType = (ParameterizedType) method.getGenericParameterTypes()[0];
            action = new GuiTypeMemberActionList(name, get(pType.getActualTypeArguments()[0]), method);
        } else {
            action = new GuiTypeMemberAction(name, method);
        }
        action.setOwner(objType);
        objType.getActions().add(action);
        int index = getMemberOrdinalIndex(method);
        action.setOrdinal(new GuiTypeMember.MemberOrdinal(index, name));
    }

    /**
     * @param f the tested field
     * @return true if {@link GuiIncluded} is attached and non-static */
    public boolean isMemberField(Field f) {
        return f.isAnnotationPresent(GuiIncluded.class) && !Modifier.isStatic(f.getModifiers());
    }

    /**
     * @param f the field
     * @return the field name itself or attached {@link GuiIncluded#name()} */
    public String getMemberNameFromField(Field f) {
        GuiIncluded included = f.getAnnotation(GuiIncluded.class);
        if (included == null || included.name().isEmpty()) {
            return f.getName();
        } else {
            return included.name();
        }
    }

    /**
     * @param m the tested method
     * @return true if {@link GuiIncluded} attached and non-static */
    public boolean isMemberMethod(Method m) {
        return m.isAnnotationPresent(GuiIncluded.class) && !Modifier.isStatic(m.getModifiers());
    }

    /**
     * @param m a method
     * @return {@link #getMemberNameFromMethodName(String)} or attached {@link GuiIncluded#name()}*/
    public String getMemberNameFromMethod(Method m) {
        GuiIncluded included = m.getAnnotation(GuiIncluded.class);
        if (included.name().isEmpty()) {
            return getMemberNameFromMethodName(m.getName());
        } else {
            return included.name();
        }
    }

    /**
     * @param name a method name
     * @return suffix of get..., is.... or set..., or name itself */
    public String getMemberNameFromMethodName(String name) {
        String pName = getNameSuffix("get", name);
        if (pName == null) {
            pName = getNameSuffix("is", name);
        }
        if (pName == null) {
            pName = getNameSuffix("set", name);
        }
        if (pName == null) {
            pName = name;
        }
        return pName;
    }

    public String getNameSuffix(String head, String name) {
        if (name.startsWith(head) && name.length() > head.length()) {
            String s = name.substring(head.length());
            if (s.length() > 1) {
                return Character.toLowerCase(s.charAt(0)) + s.substring(1);
            } else {
                return s.toLowerCase();
            }
        } else {
            return null;
        }
    }

    /**
     * @param m  the tested method
     * @return true if boolean is...() or V get...() */
    public boolean isGetterMethod(Method m) {
        return ((m.getName().startsWith("is") && m.getReturnType().equals(boolean.class)) ||
                    m.getName().startsWith("get")) &&
                m.getParameterCount() == 0 &&
                !m.getReturnType().equals(void.class);
    }

    /**
     * @param m the tested method
     * @return set...(V v) */
    public boolean isSetterMethod(Method m) {
        return m.getName().startsWith("set") && m.getParameterCount() == 1;
    }

    /** @param m the tested method
     * @return true if m() */
    public boolean isActionMethod(Method m) {
        return m.getParameterCount() == 0;
    }

    /**
     * @param m  the tested method
     * @return m(L&lt;E&gt;) and L is a super type of {@link List} except for Object. */
    public boolean isActionListMethod(Method m) {
        if (m.getParameterCount() == 1) {
            Type t = m.getGenericParameterTypes()[0];
            if (t instanceof ParameterizedType) {
                Class<?> rawType = getClass(t);
                return rawType.isAssignableFrom(List.class) && !rawType.equals(Object.class) &&
                            ((ParameterizedType) t).getActualTypeArguments().length == 1;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /** {@link Class},
     * raw type of {@link ParameterizedType},
     *  LUB of {@link WildcardType},
     *  LUB of {@link TypeVariable},
     *  Object[] for {@link GenericArrayType},
     *  or Object
     *
     * @param type the type for extraction
     * @return an extracted class or Object.class
     * */
    public Class<?> getClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof WildcardType) {
            Type[] uppers = ((WildcardType) type).getUpperBounds();
            if (uppers != null) {
                Class<?> lub = Object.class;
                for (Type upper : uppers) {
                    Class<?> b = getClass(upper);
                    if (!b.isAssignableFrom(lub)) {
                        lub = b;
                    }
                }
                return lub;
            } else {
                return Object.class;
            }
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable) type).getBounds();
            if (bounds != null) {
                Class<?> lub = Object.class;
                for (Type bound : bounds) {
                    Class<?> b = getClass(bound);
                    if (!b.isAssignableFrom(lub)) {
                        lub = b;
                    }
                }
                return lub;
            } else {
                return Object.class;
            }
        } else if (type instanceof GenericArrayType) {
            return Object[].class; //do not support
        } else {
            return Object.class;
        }
    }
}
