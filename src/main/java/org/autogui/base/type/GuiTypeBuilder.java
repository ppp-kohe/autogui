package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.GuiNotifierSetter;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * the factory for {@link GuiTypeElement}. {@link #get(Type)} can obtain a {@link GuiTypeElement} from {@link Type}.
 *   it's targets are members attached [at]{@link GuiIncluded}.
 * */
public class GuiTypeBuilder {
    protected Map<Type, GuiTypeElement> typeElements = new HashMap<>();

    protected static List<Class<?>> langValueTypes = Arrays.asList(
            Integer.class, Short.class, Byte.class, Long.class, Character.class, Boolean.class,
            Float.class, Double.class, CharSequence.class);

    protected List<Class<?>> valueTypes;
    /** for supporting {@link CharSequence}, extracting it from {@link #valueTypes}
     * @since 1.8 */
    protected List<Class<?>> valueInterfaceTypes;
    /** the prefix of the complete action method
     * @since 1.9*/
    public static final String PREFIX_COMPLETE_EDIT = "complete";

    @SuppressWarnings("this-escape")
    public GuiTypeBuilder() {
        setValueTypes(langValueTypes);
    }

    @SuppressWarnings("this-escape")
    public GuiTypeBuilder(List<Class<?>> valueTypes) {
        setValueTypes(valueTypes);
    }

    public void setValueTypes(List<Class<?>> valueTypes) {
        this.valueTypes = new ArrayList<>(valueTypes);
        valueInterfaceTypes = new ArrayList<>(
                valueTypes.stream()
                        .filter(Class::isInterface)
                        .toList());
    }

    /**
     * @return the default value types which are boxed primitive types: {@link Integer} etc.
     */
    public List<Class<?>> getValueTypes() {
        return valueTypes;
    }

    /**
     * reuse a constructed type element, or {@link #create(Type)}.
     * the reusing can avoid infinity recursion
     *
     * @param type for the element
     * @return the created type element
     */
    public GuiTypeElement get(Type type) {
        GuiTypeElement e = typeElements.get(type);
        if (e == null) {
            return create(type);
        }
        return e;
    }

    /**
     * <ul>
     * <li>for {@link Class}: {@link #createFromClass(Class)}</li>
     * <li>for {@link ParameterizedType}: obtain the raw class and
     * check it is a {@link Collection} with a type arg &lt;T&gt;.
     * then, {@link #createCollectionFromType(ParameterizedType)},
     * otherwise {@link #createFromClass(Class)} for the raw type</li>
     * <li>otherwise obtains the raw-type of the type, and {@link #createFromClass(Class, Type)}</li>
     * <li>if no raw-type, returns null</li>
     * </ul>
     *
     * @param type for creation
     * @return nullable
     */
    public GuiTypeElement create(Type type) {
        if (type instanceof Class<?>) {
            return createFromClass((Class<?>) type);
            //GenericArrayType is not supported
        } else if (type instanceof ParameterizedType pType) {
            Class<?> rawType = getClass(pType);
            if (Collection.class.isAssignableFrom(rawType) &&
                    pType.getActualTypeArguments().length == 1) { //currently only support C<E>
                return createCollectionFromType(pType);
            } else {
                return createFromClass(rawType, pType);
            }
        } else {
            Class<?> rawType = getClass(type);
            if (rawType != null) {
                return createFromClass(rawType, type);
            } else {
                return null;
            }
        }
    }

    /**
     * @param cls the target class
     * @return
     *    if array class and {@link #isIncludedClass(Class)}, {@link #createCollectionArrayFromClass(Class,Type)}.
     *    if {@link #isValueType(Class)} or {@link #isExcludedType(Class)} then {@link #createValueFromClass(Class,Type)}
     *    else {@link #createObjectFromClass(Class,Type)}*/
    public GuiTypeElement createFromClass(Class<?> cls) {
        return createFromClass(cls, null);
    }

    public GuiTypeElement createFromClass(Class<?> cls, Type genericTypeOfCls) {
        if (cls.isArray() && isIncludedClass(cls.getComponentType())) {
            return createCollectionArrayFromClass(cls, genericTypeOfCls);
        } else if (isValueType(cls) || isExcludedType(cls)) {
            return createValueFromClass(cls, genericTypeOfCls);
        } else {
            return createObjectFromClass(cls, genericTypeOfCls);
        }
    }

    /**
     *
     * @param cls the target class
     * @return if array class, recursively checks component type.
     * otherwise, {@link #isValueType(Class)} or non-{@link #isExcludedType(Class)}
     */
    public boolean isIncludedClass(Class<?> cls) {
        if (cls.isArray()) {
            return isIncludedClass(cls.getComponentType());
        } else {
            return isValueType(cls)  || !isExcludedType(cls);
        }
    }

    /**
     * @param cls the target type
     * @param genericTypeOfCls a generic type version of cls, which will also be registered. nullable.
     * @return a new {@link GuiTypeValue} with registering it */
    public GuiTypeValue createValueFromClass(Class<?> cls, Type genericTypeOfCls) {
        GuiTypeValue valueType = new GuiTypeValue(cls);
        put(cls, valueType);
        if (genericTypeOfCls != null) {
            put(genericTypeOfCls, valueType);
        }
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

    public GuiTypeCollectionArray createCollectionArrayFromClass(Class<?> cls, Type genericTypeOfCls) {
        GuiTypeCollectionArray array = new GuiTypeCollectionArray(cls);
        put(cls, array);
        if (genericTypeOfCls != null) {
            put(genericTypeOfCls, array);
        }
        array.setElementType(get(cls.getComponentType()));
        return array;
    }

    /**
     * create a {@link GuiTypeObject}, register it,
     *   and construct field and method members.
     *   those members are grouped by their names
     *   and passed to {@link #createMember(GuiTypeObject, MemberDefinitions)}.
     *   the rules for finding members are {@link #isMemberField(Field)} and {@link #isMemberMethod(Method)}.
     * <pre>
     *     //example
     *     &#64;{@link GuiIncluded}
     *     class C {
     *         &#64;{@link GuiIncluded}(index=1)
     *         public int fld;
     *
     *         &#64;{@link GuiIncluded}(index=2, description="read-only value")
     *         public String getReadOnlyProp() { ... }
     *
     *         &#64;{@link GuiIncluded}(index=3, description="property by setter and getter")
     *         public String getValue() { ... }
     *         public void setValue(String v) { ... }
     *
     *         &#64;{@link GuiIncluded}(index=4, description="action")
     *         public void action() { ... }
     *     }
     * </pre>
     *   @param cls the object class
     *   @param genericTypeOfCls a generic type of cls, which is also registered. nullable
     *   @return an object type for the class
     *   */
    public GuiTypeObject createObjectFromClass(Class<?> cls, Type genericTypeOfCls) {
        GuiTypeObject objType = new GuiTypeObject(cls);
        put(cls, objType);
        if (genericTypeOfCls != null) {
            put(genericTypeOfCls, objType);
        }

        Map<String, MemberDefinitions> definitionsMap = new HashMap<>();

        listFields(cls).stream()
                .filter(this::isMemberField)
                .forEachOrdered(f -> definitionsMap.computeIfAbsent(getMemberNameFromField(f), MemberDefinitions::new)
                        .fields.add(f));

        listMethods(cls).stream()
                .filter(this::isMemberMethod)
                .forEachOrdered(m -> addMethodToDefinition(definitionsMap, m));

        definitionsMap.values()
                .forEach(d -> createMember(objType, d));

        objType.getProperties().sort(Comparator.comparing(GuiTypeMember::getOrdinal));
        objType.getActions().sort(Comparator.comparing(GuiTypeMember::getOrdinal));
        objType.getNotifiers().sort(Comparator.comparing(GuiTypeMember::getOrdinal));

        ((ArrayList<?>) objType.getProperties()).trimToSize();
        ((ArrayList<?>) objType.getActions()).trimToSize();
        ((ArrayList<?>) objType.getNotifiers()).trimToSize();

        return objType;
    }

    private void addMethodToDefinition(Map<String, MemberDefinitions> definitionsMap, Method m) {
        if (isCompleteEditMethodPrefixed(m)) {
            definitionsMap.computeIfAbsent(getMemberNameFromMethodCompleteEditForActionEdit(m), MemberDefinitions::new).supplementaryMethods.add(m);
        } else {
            definitionsMap.computeIfAbsent(getMemberNameFromMethod(m), MemberDefinitions::new).methods.add(m);
        }
    }

    public List<Field> listFields(Class<?> cls) {
        return Arrays.asList(cls.getFields());
    }

    public List<Method> listMethods(Class<?> cls) {
        return Arrays.asList(cls.getMethods());
    }

    /** a temporarily created member group in {@link #createObjectFromClass(Class,Type)} */
    public static class MemberDefinitions {
        public String name;
        public List<Method> methods = new ArrayList<>(3);
        public List<Field> fields = new ArrayList<>(1);
        /**  releated methods for the named member; completion-methods
         * @since 1.9 */
        public List<Method> supplementaryMethods = new ArrayList<>(1);

        public MemberDefinitions(String name) {
            this.name = name;
        }

        public Field getLastField() {
            return fields.isEmpty() ? null : fields.getLast();
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

        /**
         * @param builder a test for selecting supplementry-methods
         * @return the last element of supplementary methods
         * @since 1.9
         */
        public Method getLastSupplimentary(Predicate<Method> builder) {
            Method g = null;
            for (Method m : supplementaryMethods) {
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
        return cls.isPrimitive() || valueTypes.contains(cls) ||
             valueInterfaceTypes.stream()
                .anyMatch(it -> it.isAssignableFrom(cls));
    }

    public boolean isExcludedType(Class<?> cls) {
        return !isGuiIncludedEnabled(cls);
    }

    /**
     * @param e the target member
     * @return true if e has the annotation {@link GuiIncluded#value()}==true or e has annotation {{@link GuiNotifierSetter}}
     * @since 1.2
     */
    public boolean isGuiIncludedEnabled(AnnotatedElement e) {
        return e.isAnnotationPresent(GuiIncluded.class) &&
                e.getAnnotation(GuiIncluded.class).value() ||
                (!e.isAnnotationPresent(GuiIncluded.class) && //exclude GuiIncluded(false)
                        e.isAnnotationPresent(GuiNotifierSetter.class));
    }

    /** if the definitions has a field, a getter or a setter, create a property.
     *   otherwise create actions.
     *  Note: any methods starting with "get", "set" or "is" are collected as a {@link MemberDefinitions}.
     *      So,  getM() and setM() are MemberDefinitions("m") and here create actions "Get M" and "Set M".
     *   @param objType the owner object
     *   @param definitions  the members
     *   */
    public void createMember(GuiTypeObject objType, MemberDefinitions definitions) {
        Field fld = definitions.getLastField();
        Method getter = definitions.getLast(this::isGetterMethod);
        if (getter == null) {
            getter = definitions.getLast(this::isAccessorMethod); //accessor T prop() of record
        }
        Method setter = definitions.getLast(this::isSetterMethod);
        if (fld != null || setter != null || getter != null) {
            //property
            if (setter != null && isNotifierSetterMethod(setter)) {
                createMemberPropertyNotifier(objType, definitions.name, fld, getter, setter);
            } else {
                createMemberProperty(objType, definitions.name, fld, getter, setter);
            }
        } else {
            for (Method method : definitions.methods) {
                var name = getMemberNameFromMethodForAction(method);
                boolean isEdit = isActionEditMethod(method);
                var completeEditMethod = isEdit ? definitions.getLastSupplimentary(this::isCompleteEditMethodPrefixed) : null;
                if (isActionMethod(method)) {
                    createMemberAction(objType, name, method, false, isEdit, completeEditMethod);
                } else if (isActionListMethod(method)) {
                    createMemberAction(objType, name, method, true, isEdit, completeEditMethod);
                }
            }
        }
    }


    public void createMemberProperty(GuiTypeObject objType, String name, Field fld, Method getter, Method setter) {
        GuiTypeMemberProperty property = new GuiTypeMemberProperty(name);
        createMemberProperty(objType, name, fld, getter, setter, property);
        objType.getProperties().add(property);
    }

    public void createMemberPropertyNotifier(GuiTypeObject objType, String name, Field fld, Method getter, Method setter) {
        GuiTypeMemberPropertyNotifier notifier = new GuiTypeMemberPropertyNotifier(name);
        createMemberProperty(objType, name, fld, getter, setter, notifier);
        objType.getNotifiers().add(notifier);
    }

    public void createMemberProperty(GuiTypeObject objType, String name, Field fld, Method getter, Method setter, GuiTypeMemberProperty property) {
        //property
        property.setOwner(objType);

        Type type = null;
        Set<Integer> indices = new HashSet<>();
        if (fld != null) {
            property.setField(fld);
            type = fld.getGenericType();
            getMemberOrdinalIndex(fld)
                    .ifPresent(indices::add);
        }
        if (setter != null) {
            property.setSetter(setter);
            type = setter.getGenericParameterTypes()[0];
            getMemberOrdinalIndex(setter)
                    .ifPresent(indices::add);
        }
        if (getter != null) {
            property.setGetter(getter);
            type = getter.getGenericReturnType();
            getMemberOrdinalIndex(getter)
                    .ifPresent(indices::add);
        }

        property.setType(get(type));
        property.setOrdinal(new GuiTypeMemberProperty.MemberOrdinal(
                indices.stream()
                        .mapToInt(Integer::intValue)
                        .min().orElse(Short.MAX_VALUE), name));
    }

    public OptionalInt getMemberOrdinalIndex(Method m) {
        if (m.isAnnotationPresent(GuiIncluded.class)) {
            return OptionalInt.of(m.getAnnotation(GuiIncluded.class).index());
        } else {
            return OptionalInt.empty();
        }
    }

    public OptionalInt getMemberOrdinalIndex(Field f) {
        if (f.isAnnotationPresent(GuiIncluded.class)) {
            return OptionalInt.of(f.getAnnotation(GuiIncluded.class).index());
        } else {
            return OptionalInt.empty();
        }
    }

    public void createMemberAction(GuiTypeObject objType, String name, Method method, boolean isList) {
        createMemberAction(objType, name, method, isList, false, null);
    }

    /**
     *
     * @param objType the owner type
     * @param name the naame of the action (can be different from the method name)
     * @param method the method of the action
     * @param isList true if the method takes a list
     * @param isEdit true if the method returns a editing object and "edit" method; then the completeEditMethod mgiht be not null
     * @param completeEditMethod a supplementary "completeEdit" method for an isEdit action
     * @since 1.9
     */
    public void createMemberAction(GuiTypeObject objType, String name, Method method, boolean isList, boolean isEdit, Method completeEditMethod) {
        GuiTypeMemberAction action;
        GuiTypeElement retType = get(method.getGenericReturnType());
        if (isList) {
            ParameterizedType pType = (ParameterizedType) method.getGenericParameterTypes()[0];
            action = new GuiTypeMemberActionList(name, retType, get(pType.getActualTypeArguments()[0]), method,
                    method.getParameterCount() == 2);
        } else {
            action = new GuiTypeMemberAction(name, retType, method);
        }
        if (isEdit) {
            action.setEditFeature(new GuiTypeMemberAction.EditFeature(completeEditMethod == null ? null : completeEditMethod.getName(), completeEditMethod));
        }
        action.setOwner(objType);
        objType.getActions().add(action);
        action.setOrdinal(new GuiTypeMember.MemberOrdinal(
                getMemberOrdinalIndex(method).orElse(Short.MAX_VALUE), name));
    }

    /**
     * @param f the tested field
     * @return true if {@link GuiIncluded} is attached and non-static */
    public boolean isMemberField(Field f) {
        return isGuiIncludedEnabled(f) && isMemberFieldWithoutAnnotation(f);
    }

    public boolean isMemberFieldWithoutAnnotation(Field f) {
        return !Modifier.isStatic(f.getModifiers());
    }

    /**
     * @param f the field
     * @return the field name itself or attached {@link GuiIncluded#name()} */
    public String getMemberNameFromField(Field f) {
        return getNameFromAnnotationOr(f, Field::getName);
    }

    /**
     * @param m the tested method
     * @return true if {@link GuiIncluded} attached and non-static */
    public boolean isMemberMethod(Method m) {
        return isGuiIncludedEnabled(m) && !Modifier.isStatic(m.getModifiers());
    }

    /**
     * @param m a method
     * @return {@link #getMemberNameFromMethodName(String)} or attached {@link GuiIncluded#name()}*/
    public String getMemberNameFromMethod(Method m) {
        return getNameFromAnnotationOr(m, mth -> getMemberNameFromMethodName(mth.getName()));
    }

    /**
     * @param m a method
     * @return the name of the method or attached {@link GuiIncluded#name()}
     */
    public String getMemberNameFromMethodForAction(Method m) {
        return getNameFromAnnotationOr(m, Method::getName);
    }

    /**
     * @param element the tested member
     * @param elementGetter the fallback function
     * @return the non-empty {@link GuiIncluded#name()} or
     *        the result of elementGetter(element)
     * @param <TargetType> the target member that the annotation {@link GuiIncluded} attached to
     * @since 1.9
     */
    protected <TargetType extends AnnotatedElement> String getNameFromAnnotationOr(TargetType element, Function<TargetType, String> elementGetter) {
        GuiIncluded included = element.getAnnotation(GuiIncluded.class);
        if (included == null || included.name().isEmpty()) {
            return elementGetter.apply(element);
        } else {
            return included.name();
        }
    }

    /**
     *  There are 2 cases for the method:
     * <ol>
     *  <li><code>  &#64;GuiIncluded(name="tgtAct") public void completeSomething(R a)</code> : <code>"tgtAct"</code>.
     *  </li>
     *  <li><code>  public void completeTgtAct(R a)</code> : <code>"tgtAct"</code>
     *  </li>
     * </ol>
     * @param completeEditMethod the complete method; e.g. "void completeTgtAct(R r)"
     * @return the member-name for the edit-method
     * @since 1.9
     */
    public String getMemberNameFromMethodCompleteEditForActionEdit(Method completeEditMethod) {
        return getNameFromAnnotationOr(completeEditMethod, m -> {
            var suffix = getNameSuffix(PREFIX_COMPLETE_EDIT, m.getName());
            if (suffix == null) {
                suffix = m.getName().equals(PREFIX_COMPLETE_EDIT) ? "" : m.getName();
            }
            return suffix;
        });
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
                isAccessorMethod(m);
    }

    /**
     * @param m the tested method
     * @return true if T m(), no annotation {@link GuiListSelectionUpdater} and {@link GuiIncluded#action()}==false and {@link GuiIncluded#editor()}=false (or no {@link GuiIncluded})
     * @since 1.2
     */
    public boolean isAccessorMethod(Method m) {
        return m.getParameterCount() == 0 && !m.getReturnType().equals(void.class)
                && !m.isAnnotationPresent(GuiListSelectionUpdater.class)
                && !isGuiIncludedAction(m);
                    //no @GuiIncluded or no @GuiIncluded(action=false)
    }

    private boolean isGuiIncludedAction(Method m) {
        if (m.isAnnotationPresent(GuiIncluded.class)) {
            var i = m.getAnnotation(GuiIncluded.class);
            return i.action() || i.editor();
        } else {
            return false;
        }
    }

    /**
     * @param m the tested method
     * @return set...(V v) */
    public boolean isSetterMethod(Method m) {
        return m.getName().startsWith("set") && m.getParameterCount() == 1;
    }

    /**
     * Note: it might conflict with {@link #isAccessorMethod(Method)}
     * @param m the tested method
     * @return true if m() */
    public boolean isActionMethod(Method m) {
        return m.getParameterCount() == 0;
    }

    /**
     * @param m  the tested method
     * @return m(L&lt;E&gt; ) and L is a super type of {@link List} except for Object (SequencedCollection, Colleciton, Iterable).
     *          or, m(L&lt;E&gt;, String)
     *   */
    public boolean isActionListMethod(Method m) {
        if (m.getParameterCount() == 1 || m.getParameterCount() == 2) {
            Type t = m.getGenericParameterTypes()[0];
            if (t instanceof ParameterizedType pType) {
                Class<?> rawType = getClass(t);
                if (rawType.isAssignableFrom(List.class) && !rawType.equals(Object.class) &&
                            pType.getActualTypeArguments().length == 1) {
                    if (m.getParameterCount() == 2) {
                        return m.getParameterTypes()[1].equals(String.class);
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @param m tested method
     * @return true if m is an edit action; an action method that has non-value and non-array returned type with <code>&#64;GuiIncluded(editor=true)</code>
     * @since 1.9
     */
    public boolean isActionEditMethod(Method m) {
        return (isActionMethod(m) || isActionListMethod(m)) &&
                !isValueType(m.getReturnType()) && !m.getReturnType().isArray() && isIncludedClass(m.getReturnType()) &&
                m.isAnnotationPresent(GuiIncluded.class) && m.getAnnotation(GuiIncluded.class).editor();
    }

    /**
     * @param m tested method
     * @return true if m is an completeEdit method; void completeEdit(R o) or void completeEdit(R o, boolean ok)
     * @since 1.9
     */
    public boolean isCompleteEditMethod(Method m) {
        if ((m.getParameterCount() == 1 || m.getParameterCount() == 2) && m.getReturnType().equals(void.class)) {
            var pType = m.getParameterTypes()[0];
            return (!isValueType(pType) && !pType.isArray()) &&
                    (m.getParameterCount() == 1) || (m.getParameterCount() == 2 && m.getParameterTypes()[1].equals(boolean.class));
        } else {
            return false;
        }
    }

    /**
     *
     * @param m the tested method
     * @return true if m {@link #isCompleteEditMethod(Method)} and m has the prefix {@link #PREFIX_COMPLETE_EDIT}
     * @since 1.9
     */
    public boolean isCompleteEditMethodPrefixed(Method m) {
        return m.getName().startsWith(PREFIX_COMPLETE_EDIT) && isCompleteEditMethod(m);
    }

    /** {@link Class},
     * raw type of {@link ParameterizedType},
     *  LUB of {@link WildcardType},
     *  LUB of {@link TypeVariable},
     *  Object[] for {@link GenericArrayType},
     *  or Object
     *
     * @param type the type for extraction
     * @return an extracted class or {@code Object.class}
     * */
    public Class<?> getClass(Type type) {
        switch (type) {
            case Class<?> aClass -> {
                return aClass;
            }
            case ParameterizedType parameterizedType -> {
                return getClass(parameterizedType.getRawType());
            }
            case WildcardType wildcardType -> {
                Type[] uppers = wildcardType.getUpperBounds();
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
            }
            case TypeVariable<?> typeVariable -> {
                Type[] bounds = typeVariable.getBounds();
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
            }
            case GenericArrayType genericArrayType -> {
                return Object[].class; //do not support
            }
            case null, default -> {
                return Object.class;
            }
        }
    }

    /**
     * @param m the tested method
     * @return true if the method is non-static and has {@link GuiNotifierSetter} with taking a {@link Runnable} argument
     */
    public boolean isNotifierSetterMethod(Method m) {
        return m.isAnnotationPresent(GuiNotifierSetter.class) && !Modifier.isStatic(m.getModifiers()) &&
                m.getParameterCount() == 1 &&
                m.getParameterTypes()[0].equals(Runnable.class);
    }

    /**
     * another type-builder with relaxed rules:
     *   including non-public (without access-modifiers) members.
     * */
    public static class GuiTypeBuilderRelaxed extends GuiTypeBuilder {
        public GuiTypeBuilderRelaxed() {}
        @Override
        public boolean isExcludedType(Class<?> cls) {
            return Modifier.isPrivate(cls.getModifiers()) ||
                    (cls.getName().startsWith("java.") ||
                     cls.getName().startsWith("javax.") ||
                     cls.getName().startsWith("sun.") ||
                     cls.getName().startsWith("com.sun."));
        }

        @Override
        public List<Field> listFields(Class<?> cls) {
            Class<?> p = cls;
            List<Field> fs = new ArrayList<>(Arrays.asList(p.getFields()));
            while (p != null && !p.equals(Object.class) && !isExcludedType(p)) {
                Arrays.stream(p.getDeclaredFields())
                        .filter(f -> fs.stream()
                                        .noneMatch(parent -> parent.getName().equals(f.getName())))
                        .filter(this::setAccessible)
                        .forEach(fs::add);
                p = p.getSuperclass();
            }
            return fs;
        }

        @Override
        public List<Method> listMethods(Class<?> cls) {
            Class<?> p = cls;
            List<Method> ms = new ArrayList<>();
            while (p != null && !p.equals(Object.class)) {
                if (isExcludedType(p)) {
                    listMethodExcludeLibraryOverride(p, ms);
                } else {
                    listMethodsDeclared(p, ms);
                }
                Arrays.stream(p.getInterfaces())
                        .flatMap(c -> Arrays.stream(c.getMethods()))
                        .forEach(im ->
                                ms.removeIf(em -> match(im, em) && !isGuiIncludedEnabled(em)));
                p = p.getSuperclass();
            }
            return ms;
        }

        public void listMethodExcludeLibraryOverride(Class<?> p, List<Method> ms) {
            Arrays.stream(p.getDeclaredMethods())
                    .filter(m -> isMemberModifiers(m.getModifiers()))
                    .forEach(im ->
                            ms.removeIf(em -> match(im, em) && !isGuiIncludedEnabled(em)));
        }

        public void listMethodsDeclared(Class<?> p, List<Method> ms) {
            Arrays.stream(p.getDeclaredMethods())
                    .filter(m -> isMemberModifiers(m.getModifiers()))
                    .filter(m -> ms.stream()
                            .noneMatch(ex -> match(ex, m)))
                    .filter(this::setAccessible)
                    .forEach(ms::add);
        }

        public boolean setAccessible(AccessibleObject a) {
            try {
                a.setAccessible(true);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        public boolean match(Method parent, Method child) {
            if (parent.getName().equals(child.getName())) {
                //simple overloading checking
                return Arrays.asList(parent.getParameterTypes())
                        .equals(Arrays.asList(child.getParameterTypes()));
            } else {
                return false;
            }
        }

        @Override
        public boolean isMemberMethod(Method m) {
            return isMemberModifiers(m.getModifiers()) &&
                   ((!m.getDeclaringClass().equals(Object.class) && isGuiIncludedEnabledRelaxed(m))
                    || isGuiIncludedEnabled(m));
        }

        /**
         * @param e the target member
         * @return true
         *     if {@link GuiIncluded#value()}==true or the annotation is not presented.
         * @since 1.2
         */
        public boolean isGuiIncludedEnabledRelaxed(AnnotatedElement e) {
            boolean presented = e.isAnnotationPresent(GuiIncluded.class);
            return !presented || e.getAnnotation(GuiIncluded.class).value();
        }

        @Override
        public boolean isMemberField(Field f) {
            return isMemberModifiers(f.getModifiers()) &&
                    ((!f.getDeclaringClass().equals(Object.class) && isGuiIncludedEnabledRelaxed(f))
                     || isGuiIncludedEnabled(f));
        }

        public boolean isMemberModifiers(int mod){
            return !Modifier.isStatic(mod) && !Modifier.isPrivate(mod) && !Modifier.isProtected(mod);
        }
    }
}
