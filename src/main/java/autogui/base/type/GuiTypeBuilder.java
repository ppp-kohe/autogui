package autogui.base.type;

import autogui.GuiIncluded;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

public class GuiTypeBuilder {
    protected Map<Type, GuiTypeElement> typeElements = new HashMap<>();

    protected static List<Class<?>> langValueTypes = Arrays.asList(
            Integer.class, Short.class, Byte.class, Long.class, Character.class, Boolean.class,
            Float.class, Double.class, CharSequence.class);

    public GuiTypeElement get(Type type) {
        GuiTypeElement e = typeElements.get(type);
        if (e == null) {
            return create(type);
        }
        return e;
    }

    /**
     *
     * @param type
     * @return nullable
     */
    public GuiTypeElement create(Type type) {
        if (type instanceof Class<?>) {
            return createFromClass((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class<?> rawType = getClass(pType);
            if (Collection.class.isAssignableFrom(rawType)) {
                return createCollectionFromType(pType);
            } else {
                return createFromClass(rawType);
            }
        } else {
            return null;
        }
    }

    public GuiTypeElement createFromClass(Class<?> cls) {
        if (isValueType(cls) || isExcludedType(cls)) {
            return createValueFromClass(cls);
        } else {
            return createObjectFromClass(cls);
        }
    }

    public GuiTypeValue createValueFromClass(Class<?> cls) {
        GuiTypeValue valueType = new GuiTypeValue(cls);
        put(cls, valueType);
        return valueType;
    }

    public GuiTypeCollection createCollectionFromType(ParameterizedType type) {
        Class<?> rawType = getClass(type);
        GuiTypeCollection collectionType = new GuiTypeCollection(rawType.getName());
        put(type, collectionType);
        collectionType.setElementType(get(type.getActualTypeArguments()[0]));
        return collectionType;
    }

    public GuiTypeObject createObjectFromClass(Class<?> cls) {
        GuiTypeObject objType = new GuiTypeObject(cls.getName());
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

        ((ArrayList<?>) objType.getProperties()).trimToSize();
        ((ArrayList<?>) objType.getActions()).trimToSize();

        return objType;
    }

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

    public boolean isValueType(Class<?> cls) {
        return cls.isPrimitive() || langValueTypes.contains(cls);
    }

    public boolean isExcludedType(Class<?> cls) {
        return !cls.isAnnotationPresent(GuiIncluded.class);
    }

    public void createMember(GuiTypeObject objType, MemberDefinitions definitions) {
        Field fld = definitions.getLastField();
        Method getter = definitions.getLast(this::isGetterMethod);
        Method setter = definitions.getLast(this::isSetterMethod);
        if (fld != null || setter != null || getter != null) {
            //property
            GuiTypeMemberProperty property = new GuiTypeMemberProperty(definitions.name);
            property.setOwner(objType);

            Type type = null;
            if (fld != null) {
                property.setFieldName(fld.getName());
                type = fld.getGenericType();
            }
            if (setter != null) {
                property.setSetterName(setter.getName());
                type = setter.getGenericParameterTypes()[0];
            }
            if (getter != null) {
                property.setGetterName(getter.getName());
                type = getter.getGenericReturnType();
            }

            property.setType(get(type));

            objType.getProperties().add(property);
        } else {
            GuiTypeMemberAction action = new GuiTypeMemberAction(definitions.name,
                    definitions.methods.get(definitions.methods.size() - 1).getName());
            action.setOwner(objType);

            objType.getActions().add(action);
        }
    }

    public boolean isMemberField(Field f) {
        return f.isAnnotationPresent(GuiIncluded.class) && !Modifier.isStatic(f.getModifiers());
    }

    public String getMemberNameFromField(Field f) {
        GuiIncluded included = f.getAnnotation(GuiIncluded.class);
        if (included == null || included.name().isEmpty()) {
            return f.getName();
        } else {
            return included.name();
        }
    }

    public boolean isMemberMethod(Method m) {
        return m.isAnnotationPresent(GuiIncluded.class) && !Modifier.isStatic(m.getModifiers());
    }

    public String getMemberNameFromMethod(Method m) {
        GuiIncluded included = m.getAnnotation(GuiIncluded.class);
        if (included.name().isEmpty()) {
            return getMemberNameFromMethodName(m.getName());
        } else {
            return included.name();
        }
    }

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

    public boolean isGetterMethod(Method m) {
        return (m.getName().startsWith("is") || m.getName().startsWith("get")) && m.getParameterCount() == 0 &&
                !m.getReturnType().equals(void.class);
    }

    public boolean isSetterMethod(Method m) {
        return m.getName().startsWith("set") && m.getParameterCount() == 1;
    }

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
