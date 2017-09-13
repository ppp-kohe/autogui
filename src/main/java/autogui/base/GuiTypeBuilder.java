package autogui.base;

import java.lang.reflect.*;
import java.util.*;

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
            //TODO collection type
            return null;
        } else {
            return null;
        }
    }

    public GuiTypeElement createFromClass(Class<?> cls) {
        if (isValueType(cls) || isExcludedType(cls)) {
            GuiTypeValue valueType = new GuiTypeValue(cls);
            put(cls, valueType);
            return valueType;
        } else {
            GuiTypeObject objType = new GuiTypeObject(cls.getName());
            put(cls, objType);
            Arrays.stream(cls.getFields())
                    .forEachOrdered(f -> createMember(objType, f));
            Arrays.stream(cls.getMethods())
                    .forEachOrdered(m -> createMember(objType, m));

            objType.getProperties()
                    .sort(Comparator.comparing(GuiTypeMember::getName));
            objType.getActions()
                    .sort(Comparator.comparing(GuiTypeMember::getName));

            ((ArrayList<?>) objType.getProperties()).trimToSize();
            ((ArrayList<?>) objType.getActions()).trimToSize();

            return objType;
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

    public void createMember(GuiTypeObject objType, Field f) {
        if (isMemberField(f)) {
            //TODO type
            String name = getMemberNameFromField(f);
            GuiTypeMember member = objType.getMemberByName(name);
            GuiTypeMemberProperty property;
            if (member == null) {
                property = new GuiTypeMemberProperty(name);
                property.setOwner(objType);
                property.setField(true);
                objType.getProperties().add(property);
            } else if (member instanceof GuiTypeMemberProperty) {
                property = (GuiTypeMemberProperty) member;
                property.setField(true);
            } else {
                property = new GuiTypeMemberProperty(name);
                property.setOwner(objType);
                property.setField(true);
                objType.getActions().remove(member);
                objType.getProperties().add(property);
            }
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

    public void createMember(GuiTypeObject objType, Method m) {
        if (isMemberMethod(m)) {
            if (isGetterMethod(m) || isSetterMethod(m)) {
                createMemberProperty(objType, m);
            } else {
                createMemberAction(objType, m);
            }
        }
    }


    public void createMemberProperty(GuiTypeObject objType, Method m) {
        //TODO type
        String name = getMemberNameFromMethod(m);
        GuiTypeMember member = objType.getMemberByName(name);
        GuiTypeMemberProperty property;
        if (member == null) {
            property = new GuiTypeMemberProperty(name);
            property.setOwner(objType);
            objType.getProperties().add(property);
        } else if (member instanceof GuiTypeMemberProperty) {
            property = (GuiTypeMemberProperty) member;
        } else {
            property = new GuiTypeMemberProperty(name);
            property.setOwner(objType);
            objType.getActions().remove(member); //overwrites existing action
            objType.getProperties().add(property);
        }
        if (isGetterMethod(m)) {
            property.setGetterName(m.getName());
        } else { //if (setter)
            property.setSetterName(m.getName());
        }
    }

    public void createMemberAction(GuiTypeObject objType, Method m) {
        //TODO collection method: m(Collection<T>, ...)
        String name = getMemberNameFromMethod(m);
        GuiTypeMember member = objType.getMemberByName(name);
        GuiTypeMemberAction action;
        if (member == null) {
            action = new GuiTypeMemberAction(name, m.getName());
            objType.getActions().add(action);
        } else if (member instanceof GuiTypeMemberAction) {
            action = new GuiTypeMemberAction(name, m.getName());
            objType.getActions().remove(member); //overwrites only if it is an action
            objType.getActions().add(action);
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
        return m.getName().startsWith("get") && m.getParameterCount() == 0 &&
                !m.getReturnType().equals(void.class);
    }
    public boolean isSetterMethod(Method m) {
        return m.getName().startsWith("set") && m.getParameterCount() == 1;
    }

}
