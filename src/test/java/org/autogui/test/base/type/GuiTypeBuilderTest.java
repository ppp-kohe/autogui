package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiNotifierSetter;
import org.autogui.base.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


public class GuiTypeBuilderTest {
    @GuiIncluded
    public static class Example {
        @GuiNotifierSetter
        public void setXNotifier(Runnable notifier) {
        }

        @GuiNotifierSetter
        @GuiIncluded(false)
        public void setDisabledNotifier(Runnable notifier) {
        }

        @GuiIncluded
        @GuiNotifierSetter
        public void setIncludedNotifier(Runnable notifier) {
        }

        public List<String> testListString() {
            return null;
        }

        public List<?> testListWild() {
            return null;
        }

        public List<? extends String> testListWildString() {
            return null;
        }

        public List<? super String> testListWildStringSuper() {
            return null;
        }

        public <T extends Appendable & CharSequence> List<T> testListVarMultiBounds() {
            return null;
        }

        public <T extends ExA & ExB> List<T> testListVarMultiBounds2() {
            return null;
        }

        public void edited(Example e) {
        }

        public void editedString(Example s) {
        }

        public void editedStringWithOk(Example a, boolean ok) {
        }

        public void methodFinishA(Example s) {
        }

        public void methodFinishB(Example a, boolean ok) {
        }

        @GuiIncluded(name = "namedEditor")
        public void editedNamed(Example s) {
        }

        public Example edit() {
            return null;
        }

        public Example editString() {
            return null;
        }

        public ExA editNoIncluded() {
            return null;
        }

        public Example methodStartA() {
            return null;
        }

        public ExA methodStartB() {
            return null;
        }

        @GuiIncluded(index = 123)
        public Example editAnnotated() {
            return null;
        }

        @GuiIncluded(action = true)
        public Example editAction() {
            return null;
        }

        public Example editList(List<String> list) {
            return null;
        }

        public void actionList(List<String> a) {
        }

        public void actionListName(List<String> a, String name) {
        }

        public void actionIterable(Iterable<String> a) {
        }

        public void actionArrayList(ArrayList<String> a) {
        }

        public void actionObject(Object a) {
        }

        @SuppressWarnings("rawtypes")
        public void actionListRaw(List a) {
        }

        public void action() {
        }

        @GuiIncluded(name = "namedAction")
        public void actionNamed() {
        }

        @GuiIncluded
        public void actionNoNamed() {
        }

        public void setVal(String s) {
        }

        public void setNot() {
        }

        public int accessor() {
            return 0;
        }

        @GuiIncluded(action = true)
        public int accessorNot() {
            return 0;
        }

        public int getVal() {
            return 0;
        }

        public boolean isVal() {
            return false;
        }

        @GuiIncluded(name = "getHelloWorld", index = 123)
        public int getNamed() {
            return 0;
        }

        @GuiIncluded
        public int getNoNamed() {
            return 0;
        }

        @GuiIncluded(false)
        public void excluded() {
        }

        @GuiIncluded
        public static void staticMethod() {
        }

        public String fldMember;
        @GuiIncluded(name = "namedFld", index = 345)
        public String fieldNamed;
        @GuiIncluded
        public String fieldNotNamed;
        public static String staticFld;
        @GuiIncluded(false)
        public String fildNotMember;

        @GuiIncluded(index = 300)
        public String propVal;

        @GuiIncluded(index = 200)
        public void setPropVal(String s) {
            propVal = s;
        }

        @GuiIncluded(index = 100)
        public String getPropVal() {
            return propVal;
        }
    }

    public interface ExA {
    }

    public interface ExB extends ExA {
    }

    public static class SubList extends AbstractList<String> {
        @Override
        public String get(int index) {
            return "";
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class ExampleListing {
        public void methodS() {
        }

        protected void methodSProtected() {
        }

        private void methodSPrivate() {
        }

        public int fieldS;
        protected int fieldSProtected;
        private int fieldSPrivate;
        public static int fieldSStatic;

        public static void methodSStatic() {
        } //inherited
    }

    public interface ExampleListingI {
        default void methodI() {
        }

        static void methodIStatic() {
        } //not inherited to implementing class.
    }

    public static class ExampleListingSub extends ExampleListing implements ExampleListingI {
        public void method() {
        }

        protected void methodProtected() {
        }

        private void methodPrivate() {
        }

        public int field;
        protected int fieldProtected;
        private int fieldPrivate;
        public static int fieldStatic;

        public static void methodStatic() {
        }
    }

    @GuiIncluded
    public static class ExampleObj {
        @GuiIncluded(index = 10)
        public int fld;

        @GuiIncluded(index = 100)
        public void setFld(int i) {
            fld = i;
        }

        @GuiIncluded(index = 5)
        public String prop() {
            return "";
        }

        @GuiIncluded(action = true, index = 10)
        public String action() {
            return "";
        }

        @GuiIncluded(action = true, index = 1, name = "edt")
        public ExampleEditor edit() {
            return new ExampleEditor();
        }

        @GuiIncluded(name = "edt")
        public void edited(ExampleEditor e) {
        }

        @GuiIncluded(index = 10, name = "prp")
        @GuiNotifierSetter
        public void setPNtf(Runnable r) {
        }

        @GuiIncluded(index = 5)
        @GuiNotifierSetter
        public void setFldNotifier(Runnable r) {
        }

        @GuiIncluded
        public Runnable getFldNotifier() {
            return null;
        }

        public void excluded() {
        }
    }

    @GuiIncluded
    public static class ExampleObjGen<E> extends ExampleObj {
    }

    @GuiIncluded
    public static class ExampleEditor {
    }

    public static class ExampleCollection {
        public List<ExampleObj> collection() {
            return null;
        }

        public ExampleObj[] array() {
            return null;
        }

        public ExampleSubList<ExampleObj> collectionUser() {
            return null;
        }

        public <T> ExampleObjGen<T> param() {
            return null;
        }

        public <T extends ExampleObj> T typeVar() {
            return null;
        }

        public List<?> wild() {
            return null;
        }

        public List<? extends ExampleObj> wildBound() {
            return null;
        }
    }

    @GuiIncluded(value = false)
    public static class ExampleExcluded {
    }

    public static class ExampleSubList<E> extends AbstractList<E> {
        @Override
        public E get(int index) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    @GuiIncluded
    public static class ExampleRec<T extends ExampleRec<T>> extends ArrayList<T> {
        public static <X extends ExampleRec<X>> ExampleRec<X> rec() {
            return null;
        }
    }

    GuiTypeBuilder builder;

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
    }

    @Test
    public void testConstructorWithValueTypes() {
        var b = new GuiTypeBuilder(List.of(ExampleExcluded.class, ExampleListingI.class));
        assertTrue(b.isValueType(ExampleExcluded.class), "class");
        assertTrue(b.isValueType(ExampleListingI.class), "interf");
        assertTrue(b.isValueType(ExampleListingSub.class), "interface impl.");
    }

    @Test
    public void testGet() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();

        var obj = builder.get(ExampleObj.class);
        {
            assertEquals(GuiTypeObject.class, obj.getClass(), "obj");
            assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "obj type");
            assertEquals(2, ((GuiTypeObject) obj).getProperties().size(), "obj member");

            var newObj = builder.get(ExampleObj.class);
            assertSame(obj, newObj, "obj get same");
        }

        var col = builder.get(colType);
        {
            assertEquals(GuiTypeCollection.class, col.getClass(), "collection");
            var colTyped = (GuiTypeCollection) col;
            assertEquals(List.class, colTyped.getType(), "collection type");
            var elem = colTyped.getElementType();
            assertTrue(elem instanceof GuiTypeObject, "recursive get");
            assertEquals(ExampleObj.class, ((GuiTypeObject) elem).getType(), "obj");
            assertSame(obj, elem, "reused element");
        }

    }

    @Test
    public void testGetRec() throws Exception {
        var recMethod = ExampleRec.class.getMethod("rec");
        var recType = (ParameterizedType) recMethod.getGenericReturnType();
        var obj = builder.get(recType);
        {
            assertEquals(GuiTypeCollection.class, obj.getClass(), "col");
            assertEquals(ExampleRec.class, ((GuiTypeCollection) obj).getType(), "col type");
            var elem = ((GuiTypeCollection) obj).getElementType();
            assertTrue(elem instanceof GuiTypeObject, "rec obj becomes another object from bounded type-variable");
            assertEquals(ExampleRec.class, ((GuiTypeObject) elem).getType(), "rec obj");

            var recObj = builder.get(ExampleRec.class);
            {
                assertSame(elem, recObj, "rec obj reused");
            }
        }

    }

    @Test
    public void testCreate() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();


        var obj = builder.create(ExampleObj.class);
        {
            assertEquals(GuiTypeObject.class, obj.getClass(), "obj");
            assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "obj type");

            var newObj = builder.create(ExampleObj.class);
            assertNotSame(obj, newObj, "obj create diffrent");
            assertEquals(2, ((GuiTypeObject) obj).getProperties().size(), "obj member");

            obj = newObj; //create overwrites the old type-element
        }

        var col = builder.create(colType);
        {
            assertEquals(GuiTypeCollection.class, col.getClass(), "collection");
            var colTyped = (GuiTypeCollection) col;
            assertEquals(List.class, colTyped.getType(), "collection type");
            var elem = colTyped.getElementType();
            assertTrue(elem instanceof GuiTypeObject, "recursive get");
            assertEquals(ExampleObj.class, ((GuiTypeObject) elem).getType(), "obj");
            assertSame(obj, elem, "reused element");
        }
    }

    @Test
    public void testCreateForListSubType() throws Exception {
        var colSubMethod = ExampleCollection.class.getMethod("collectionUser");
        var colSubType = (ParameterizedType) colSubMethod.getGenericReturnType();
        var obj = builder.create(ExampleObj.class);

        var colSub = builder.create(colSubType);
        {
            assertEquals(GuiTypeCollection.class, colSub.getClass(), "collection");
            var colTyped = (GuiTypeCollection) colSub;
            assertEquals(ExampleSubList.class, colTyped.getType(), "collection type");
            var elem = colTyped.getElementType();
            assertTrue(elem instanceof GuiTypeObject, "recursive get");
            assertEquals(ExampleObj.class, ((GuiTypeObject) elem).getType(), "obj");
            assertSame(obj, elem, "reused element");
        }
    }

    @Test
    public void testCreateForGenerics() throws Exception {
        var paramMethod = ExampleCollection.class.getMethod("param");
        var paramType = (ParameterizedType) paramMethod.getGenericReturnType();
        var paramObj = builder.create(paramType);
        {
            assertEquals(GuiTypeObject.class, paramObj.getClass(), "param");
            assertEquals(ExampleObjGen.class, ((GuiTypeObject) paramObj).getType(), "obj");

            var paramObjNew = builder.create(paramType);
            assertNotSame(paramObj, paramObjNew, "create different");
            assertNotSame(paramObjNew, builder.create(ExampleObjGen.class), "create different");
        }
    }

    @Test
    public void testCreateForTypeVar() throws Exception {
        var typeVarMethod = ExampleCollection.class.getMethod("typeVar");
        var typeVarType = (TypeVariable<?>) typeVarMethod.getGenericReturnType();
        var obj = builder.create(typeVarType);
        {
            assertEquals(GuiTypeObject.class, obj.getClass(), "obj");
            assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "obj type");

            var objNew = builder.create(typeVarType);
            assertNotSame(obj, objNew, "create different");
            assertNotSame(objNew, builder.create(ExampleObj.class), "create different");
        }
    }

    @Test
    public void testCreateForWild() throws Exception {
        var wildMethod = ExampleCollection.class.getMethod("wild");
        var wildType = (WildcardType) (((ParameterizedType) wildMethod.getGenericReturnType()).getActualTypeArguments()[0]);
        var obj = builder.create(wildType);
        {
            assertEquals(GuiTypeValue.class, obj.getClass(), "obj");
            assertEquals(Object.class, ((GuiTypeValue) obj).getType(), "obj type");

            var objNew = builder.create(wildType);
            assertNotSame(obj, objNew, "create different");
            assertNotSame(objNew, builder.create(Object.class), "create different");
        }
    }

    @Test
    public void testCreateForWildBound() throws Exception {
        var wildMethod = ExampleCollection.class.getMethod("wildBound");
        var wildType = (WildcardType) (((ParameterizedType) wildMethod.getGenericReturnType()).getActualTypeArguments()[0]);
        var obj = builder.create(wildType);
        {
            assertEquals(GuiTypeObject.class, obj.getClass(), "obj");
            assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "obj type");

            var objNew = builder.create(wildType);
            assertNotSame(obj, objNew, "create different");
            assertNotSame(objNew, builder.create(ExampleObj.class), "create different");
        }
    }

    @Test
    public void testCreateFromClass() {
        var obj = builder.createFromClass(ExampleObj.class, ExampleObj.class);
        assertEquals(GuiTypeObject.class, obj.getClass(), "obj");
        assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "obj type");
        assertEquals(2, ((GuiTypeObject) obj).getProperties().size(), "obj member");

        var array = builder.createFromClass(ExampleObj[].class, ExampleObj[].class);
        assertEquals(GuiTypeCollectionArray.class, array.getClass(), "array");
        assertEquals(ExampleObj[].class, ((GuiTypeCollectionArray) array).getType(), "array type");
        assertEquals(obj, ((GuiTypeCollectionArray) array).getElementType(), "array component reused");

        var value = builder.createFromClass(String.class, String.class);
        assertEquals(GuiTypeValue.class, value.getClass(), "value");
        assertEquals(String.class, ((GuiTypeValue) value).getType(), "value type");

        var valueObj = builder.createFromClass(ExampleExcluded.class, ExampleExcluded.class);
        assertEquals(GuiTypeValue.class, valueObj.getClass(), "value obj");
        assertEquals(ExampleExcluded.class, ((GuiTypeValue) valueObj).getType(), "value type excluded");


        var objWithoutGenType = builder.createFromClass(ExampleObj.class);
        assertEquals(GuiTypeObject.class, objWithoutGenType.getClass(), "from class");
        assertEquals(ExampleObj.class, ((GuiTypeObject) objWithoutGenType).getType(), "from class type");
        assertNotSame(obj, objWithoutGenType, "create different obj");
    }

    @Test
    public void testIsIncludedClass() {
        assertTrue(builder.isIncludedClass(int.class), "include primitve");
        assertTrue(builder.isIncludedClass(Integer.class), "include value");
        assertTrue(builder.isIncludedClass(Boolean.class), "include value");
        assertTrue(builder.isIncludedClass(String.class), "include value");
        assertTrue(builder.isIncludedClass(ExampleObj.class), "include obj");
        assertFalse(builder.isIncludedClass(ExampleExcluded.class), "excluded");
        assertFalse(builder.isIncludedClass(Object.class), "excluded");
        assertTrue(builder.isIncludedClass(String[].class), "include array value");
        assertTrue(builder.isIncludedClass(ExampleObj[].class), "include array obj");
        assertFalse(builder.isIncludedClass(ExampleExcluded[].class), "array excluded");
    }

    @Test
    public void testCreateValueFromClass() {
        var val = builder.createValueFromClass(ExampleObj.class, ExampleObj.class);
        assertEquals(GuiTypeValue.class, val.getClass(), "value");
        assertEquals(ExampleObj.class, val.getType(), "type");
    }

    @Test
    public void testCreateCollectionFromClass() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();

        var col = builder.createCollectionFromType(colType);
        assertEquals(List.class, col.getType(), "collection");
        var elem = col.getElementType();
        assertTrue(elem instanceof GuiTypeObject, "recursive get");
        var obj = (GuiTypeObject) elem;
        assertEquals(ExampleObj.class, obj.getType(), "component type");
        assertEquals(2, obj.getProperties().size(), "recursive get with constructing members");
    }

    @Test
    public void testCreateCollectionArrayFromClass() throws Exception {
        var arrayMethod = ExampleCollection.class.getMethod("array");
        var arrayCls = arrayMethod.getReturnType();
        var arrayType = arrayMethod.getGenericReturnType();

        var array = builder.createCollectionArrayFromClass(arrayCls, arrayType);
        assertEquals(arrayCls.getName(), array.getName(), "array");
        var obj = array.getElementType();
        assertTrue(obj instanceof GuiTypeObject, "recursive get");
        assertEquals(ExampleObj.class, ((GuiTypeObject) obj).getType(), "component type");
        assertEquals(2, ((GuiTypeObject) obj).getProperties().size(), "recursive get with constructing members");
    }

    @Test
    public void testCreateObjectFromClassProps() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        assertEquals(2, obj.getProperties().size(), "props");
        {
            var fld = obj.getProperties().get(1);
            var prop = obj.getProperties().getFirst();
            assertEquals("fld", fld.getName(), "fld");
            assertEquals("prop", prop.getName(), "prop");

            assertEquals("fld", fld.getField().getName(), "fld field");
            assertNull(fld.getGetter(), "fld getter");
            assertEquals("setFld", fld.getSetter().getName(), "fld setter");

            assertNull(prop.getField(), "prop field");
            assertEquals("prop", prop.getGetter().getName(), "prop getter");
            assertNull(prop.getSetter(), "prop setter");
        }
    }

    @Test
    public void testCreateObjectFromClassActions() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        assertEquals(2, obj.getActions().size(), "ations");
        {
            var action = obj.getActions().get(1);
            var edit = obj.getActions().getFirst();
            assertEquals("action", action.getName(), "action");
            assertEquals("edt", edit.getName(), "edited");

            assertEquals("action", action.getMethod().getName(), "action method");
            assertEquals("edit", edit.getMethod().getName(), "edit method");

            assertTrue(edit.hasEditedMethod(), "has edited");
            assertEquals("edited", edit.getEditFeature().editedMethodName(), "edited name");
            assertEquals("edited", edit.getEditFeature().editedMethod().getName(), "edited method");
        }
    }

    @Test
    public void testCreateObjectFromClassNotifiers() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        assertEquals(2, obj.getNotifiers().size(), "notifiers");
        {
            var fld = obj.getNotifiers().getFirst();
            var prop = obj.getNotifiers().get(1);
            assertEquals("fldNotifier", fld.getName(), "fldNotifier");
            assertEquals("prp", prop.getName(), "prp");

            assertNull(fld.getField(), "fld field");
            assertEquals("getFldNotifier", fld.getGetter().getName(), "fld getter");
            assertEquals("setFldNotifier", fld.getSetter().getName(), "fld setter");

            assertNull(prop.getField(), "prop field");
            assertNull(prop.getGetter(), "prop getter");
            assertEquals("setPNtf", prop.getSetter().getName(), "prop setter");
        }
    }

    @Test
    public void testListFields() {
        var list = builder.listFields(ExampleListingSub.class);
        var names = list.stream().map(Field::getName).collect(Collectors.toSet());
        assertEquals(Set.of("fieldS", "field", "fieldSStatic", "fieldStatic"), names, "listed public static fields includes super types");
    }

    @Test
    public void testListMethod() {
        var list = builder.listMethods(ExampleListingSub.class);
        var names = list.stream().map(Method::getName).sorted().collect(Collectors.toList());
        var expected = new ArrayList<String>();
        Arrays.stream(Object.class.getMethods()).map(Method::getName).forEach(expected::add);
        expected.addAll(Set.of("method", "methodStatic", "methodS", "methodSStatic", "methodI"));
        expected.sort(Comparator.naturalOrder());

        assertEquals(expected, names, "listed public static methods includes super types, but excludes interface static methods");
    }

    @Test
    public void testIsValueType() {
        assertTrue(builder.isValueType(int.class), "primitive");
        assertTrue(builder.isValueType(boolean.class), "primitive");
        assertTrue(builder.isValueType(Integer.class), "primitive boxed");
        assertTrue(builder.isValueType(Short.class), "primitive boxed");
        assertTrue(builder.isValueType(Long.class), "primitive boxed");
        assertTrue(builder.isValueType(Byte.class), "primitive boxed");
        assertTrue(builder.isValueType(Character.class), "primitive boxed");
        assertTrue(builder.isValueType(Float.class), "primitive boxed");
        assertTrue(builder.isValueType(Double.class), "primitive boxed");
        assertTrue(builder.isValueType(Boolean.class), "primitive boxed");
        assertTrue(builder.isValueType(String.class), "string");
        assertTrue(builder.isValueType(StringBuilder.class), "charSequence");
        assertFalse(builder.isValueType(Object.class), "not value: object");
        assertFalse(builder.isValueType(int[].class), "not value: array");
        assertFalse(builder.isValueType(List.class), "not value: List");
        assertFalse(builder.isValueType(Example.class), "not value: other");
    }

    @Test
    public void testIsExcludedType() {
        assertTrue(builder.isExcludedType(ExA.class));
        assertFalse(builder.isExcludedType(Example.class));
    }

    @Test
    public void testIsGuiIncludedEnabled() throws Exception {
        assertTrue(builder.isGuiIncludedEnabled(Example.class), "attached");
        assertFalse(builder.isGuiIncludedEnabled(Example.class.getField("fildNotMember")), "attached but false");
        assertFalse(builder.isGuiIncludedEnabled(Example.class.getMethod("setDisabledNotifier", Runnable.class)), "attached notifier but false");
        assertTrue(builder.isGuiIncludedEnabled(Example.class.getMethod("setIncludedNotifier", Runnable.class)), "attached notifier");
    }

    @Test
    public void testCreateMemberAsProp() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("propVal");
        m.methods.add(Example.class.getMethod("getPropVal"));
        m.fields.add(Example.class.getField("propVal"));
        m.methods.add(Example.class.getMethod("setPropVal", String.class));

        builder.createMember(obj, m);
        var p = obj.getProperties().getFirst();
        assertEquals(obj, p.getOwner(), "owner");
        assertEquals("propVal", p.getName(), "name");
        assertEquals(100, p.getOrdinal().getIndex(), "ordinal");
        assertEquals("setPropVal", p.getSetter().getName(), "setter");
        assertEquals("getPropVal", p.getGetter().getName(), "getter");
        assertEquals("propVal", p.getField().getName(), "field");
    }

    @Test
    public void testCreateMemberAsNotifier() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("xNotifier");
        m.methods.add(Example.class.getMethod("setXNotifier", Runnable.class));

        builder.createMember(obj, m);

        var n = obj.getNotifiers().getFirst();
        assertEquals("xNotifier", n.getName(), "name");
        assertEquals("setXNotifier", n.getSetter().getName(), "created prop");
    }

    @Test
    public void testCreateMemberAsActionList() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("actionList");
        m.methods.add(Example.class.getMethod("actionList", List.class));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        assertTrue(a instanceof GuiTypeMemberActionList, "list");
        assertEquals("actionList", a.getName(), "name");
        assertEquals("actionList", a.getMethod().getName(), "created action");
    }

    @Test
    public void testCreateMemberAsAction() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("action");
        m.methods.add(Example.class.getMethod("action"));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        assertEquals("action", a.getName(), "name");
        assertEquals("action", a.getMethod().getName(), "created action");
    }


    @Test
    public void testCreateMemberAsActionEdit() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("editAction");
        m.methods.add(Example.class.getMethod("editAction"));
        m.supplementaryMethods.add(Example.class.getMethod("edited", Example.class));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        assertEquals("editAction", a.getName(), "name");
        assertEquals("editAction", a.getMethod().getName(), "created action");
        assertEquals("edited", a.getEditFeature().editedMethod().getName(), "edited");
    }

    @Test
    public void testCreateMemberPropertyWithoutProp() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberProperty(obj, "propVal", Example.class.getField("propVal"), Example.class.getMethod("getPropVal"), Example.class.getMethod("setPropVal", String.class));
        var p = obj.getProperties().getFirst();
        assertEquals(obj, p.getOwner(), "owner");
        assertEquals(100, p.getOrdinal().getIndex(), "ordinal");
        assertEquals("setPropVal", p.getSetter().getName(), "setter");
        assertEquals("getPropVal", p.getGetter().getName(), "getter");
        assertEquals("propVal", p.getField().getName(), "field");
    }

    @Test
    public void testCreateMemberPropertyNotifier() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberPropertyNotifier(obj, "xNotifier", null, null, Example.class.getMethod("setXNotifier", Runnable.class));

        var n = obj.getNotifiers().getFirst();
        assertEquals("setXNotifier", n.getSetter().getName(), "created prop");
    }

    @Test
    public void testCreateMemberProperty() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        var p = new GuiTypeMemberProperty("propVal");
        builder.createMemberProperty(obj, "propVal", Example.class.getField("propVal"), Example.class.getMethod("getPropVal"), Example.class.getMethod("setPropVal", String.class), p);
        assertEquals(obj, p.getOwner(), "owner");
        assertEquals(100, p.getOrdinal().getIndex(), "ordinal");
        assertEquals("setPropVal", p.getSetter().getName(), "setter");
        assertEquals("getPropVal", p.getGetter().getName(), "getter");
        assertEquals("propVal", p.getField().getName(), "field");
    }

    @Test
    public void testGeMemberOrdinalIndex() throws Exception {
        assertEquals(OptionalInt.of(345), builder.getMemberOrdinalIndex(Example.class.getField("fieldNamed")), "field oridnal");
        assertEquals(OptionalInt.empty(), builder.getMemberOrdinalIndex(Example.class.getField("fldMember")), "field oridnal none");

        assertEquals(OptionalInt.of(123), builder.getMemberOrdinalIndex(Example.class.getMethod("getNamed")), "method oridnal");
        assertEquals(OptionalInt.empty(), builder.getMemberOrdinalIndex(Example.class.getMethod("getVal")), "method oridnal none");
    }

    @Test
    public void testCreateMemberAction() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "action", Example.class.getMethod("action"), false, false, null);

        var action = obj.getActions().getFirst();
        assertEquals(GuiTypeMemberAction.class, action.getClass(), "action type");
        assertEquals("action", action.getName(), "createMemberAction");
        assertEquals(Short.MAX_VALUE, action.getOrdinal().getIndex(), "action ordial");
        assertEquals(obj, action.getOwner(), "action owner");
    }

    @Test
    public void testCreateMemberActionList() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "actionList", Example.class.getMethod("actionList", List.class), true);

        var action = obj.getActions().getFirst();
        assertEquals(GuiTypeMemberActionList.class, action.getClass(), "action type");
        assertEquals(new GuiTypeValue(String.class), ((GuiTypeMemberActionList) action).getElementType(), "element");
        assertEquals("actionList", action.getName(), "createMemberAction");
        assertEquals(Short.MAX_VALUE, action.getOrdinal().getIndex(), "action ordial");
        assertEquals(obj, action.getOwner(), "action owner");
    }

    @Test
    public void testCreateMemberActionListNamed() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "actionListName", Example.class.getMethod("actionListName", List.class, String.class), true, false, null);

        var action = obj.getActions().getFirst();
        assertEquals(GuiTypeMemberActionList.class, action.getClass(), "action type");
        assertEquals(new GuiTypeValue(String.class), ((GuiTypeMemberActionList) action).getElementType(), "element");
        assertTrue(((GuiTypeMemberActionList) action).isTakingTargetName(), "take name");
        assertEquals("actionListName", action.getName(), "createMemberAction");
        assertEquals(Short.MAX_VALUE, action.getOrdinal().getIndex(), "action ordial");
        assertEquals(obj, action.getOwner(), "action owner");
    }

    @Test
    public void testCreateMemberActionEdited() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "action", Example.class.getMethod("editAnnotated"), false, true, Example.class.getMethod("editedStringWithOk", Example.class, boolean.class));

        var action = obj.getActions().getFirst();
        assertEquals(GuiTypeMemberAction.class, action.getClass(), "action type");
        assertEquals("action", action.getName(), "createMemberAction");
        assertEquals(123, action.getOrdinal().getIndex(), "action ordial");
        assertEquals(obj, action.getOwner(), "action owner");
        assertEquals("editAnnotated", action.getMethod().getName(), "method");
        assertEquals(new GuiTypeObject(Example.class), action.getReturnType(), "rettype");

        assertTrue(action.hasEditedMethod(), "feature");
        assertEquals("editedStringWithOk", action.getEditFeature().editedMethodName(), "edited method");
        assertEquals("editedStringWithOk", action.getEditFeature().editedMethod().getName(), "edited method");
    }

    @Test
    public void testCreateMemberActionListEdited() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "editList", Example.class.getMethod("editList", List.class), true, true, Example.class.getMethod("editedStringWithOk", Example.class, boolean.class));

        var action = obj.getActions().getFirst();
        assertEquals(GuiTypeMemberActionList.class, action.getClass(), "action type");
        assertEquals("editList", action.getName(), "createMemberAction");
        assertEquals(obj, action.getOwner(), "action owner");
        assertEquals("editList", action.getMethod().getName(), "method");
        assertEquals(new GuiTypeObject(Example.class), action.getReturnType(), "rettype");

        assertTrue(action.hasEditedMethod(), "feature");
        assertEquals("editedStringWithOk", action.getEditFeature().editedMethodName(), "edited method");
        assertEquals("editedStringWithOk", action.getEditFeature().editedMethod().getName(), "edited method");
    }


    @Test
    public void testIsMemberField() throws Exception {
        assertFalse(builder.isMemberField(Example.class.getField("fldMember")), "field");
        assertTrue(builder.isMemberField(Example.class.getField("fieldNamed")), "field name=...");
        assertTrue(builder.isMemberField(Example.class.getField("fieldNamed")), "field name=...");
        assertTrue(builder.isMemberField(Example.class.getField("fieldNotNamed")), "field name");
        assertFalse(builder.isMemberField(Example.class.getField("fildNotMember")), "field name");
    }

    @Test
    public void testIsMemberFieldWithoutAnnotation() throws Exception {
        assertTrue(builder.isMemberFieldWithoutAnnotation(Example.class.getField("fldMember")), "field");
        assertFalse(builder.isMemberFieldWithoutAnnotation(Example.class.getField("staticFld")), "static field");
    }

    @Test
    public void testGetMemberNameFromField() throws Exception {
        assertEquals("fldMember", builder.getMemberNameFromField(Example.class.getField("fldMember")), "field");
        assertEquals("namedFld", builder.getMemberNameFromField(Example.class.getField("fieldNamed")), "field name=...");
        assertEquals("namedFld", builder.getMemberNameFromField(Example.class.getField("fieldNamed")), "field name=...");
        assertEquals("fieldNotNamed", builder.getMemberNameFromField(Example.class.getField("fieldNotNamed")), "field name");
    }

    @Test
    public void testIsMemberMethod() throws Exception {
        assertFalse(builder.isMemberMethod(Example.class.getMethod("excluded")), "included(false)");
        assertTrue(builder.isMemberMethod(Example.class.getMethod("getNoNamed")), "included");

        assertFalse(builder.isMemberMethod(Example.class.getMethod("staticMethod")), "static");

        assertFalse(builder.isMemberMethod(Example.class.getMethod("action")), "no included tag");

        assertTrue(builder.isMemberMethod(Example.class.getMethod("setXNotifier", Runnable.class)), "notifierSetter");
        assertFalse(builder.isMemberMethod(Example.class.getMethod("setDisabledNotifier", Runnable.class)), "disabled notifierSetter");
    }

    @Test
    public void testGetMemberNameFromMethod() throws Exception {
        assertEquals("action", builder.getMemberNameFromMethod(Example.class.getMethod("action")), "action");
        assertEquals("val", builder.getMemberNameFromMethod(Example.class.getMethod("getVal")), "getter");
        assertEquals("getHelloWorld", builder.getMemberNameFromMethod(Example.class.getMethod("getNamed")), "getter named");
        assertEquals("noNamed", builder.getMemberNameFromMethod(Example.class.getMethod("getNoNamed")), "getter no name");
    }

    @Test
    public void testGetMemberNameFromMethodForAction() throws Exception {
        assertEquals("action", builder.getMemberNameFromMethodForAction(Example.class.getMethod("action")), "action");
        assertEquals("namedAction", builder.getMemberNameFromMethodForAction(Example.class.getMethod("actionNamed")), "action with name");
        assertEquals("actionNoNamed", builder.getMemberNameFromMethodForAction(Example.class.getMethod("actionNoNamed")), "action with no name");
    }

    @Test
    public void testGetMemberNameFromMethodEditedForActionEdit() throws Exception {
        assertEquals("editString", builder.getMemberNameFromMethodEditedForActionEdit(Example.class.getMethod("editedString", Example.class)), "editedX -> editX");

        assertEquals("edit", builder.getMemberNameFromMethodEditedForActionEdit(Example.class.getMethod("edited", Example.class)), "edited -> edit");

        assertEquals("namedEditor", builder.getMemberNameFromMethodEditedForActionEdit(Example.class.getMethod("editedNamed", Example.class)), "edited with name=x -> x");
    }

    @Test
    public void testGetMemberNameFromMethodName() {
        assertEquals("x", builder.getMemberNameFromMethodName("getX"), "getter");
        assertEquals("x", builder.getMemberNameFromMethodName("setX"), "setter");
        assertEquals("x", builder.getMemberNameFromMethodName("isX"), "getter is");
        assertEquals("action", builder.getMemberNameFromMethodName("action"), "no prefix");
    }

    @Test
    public void testGetNameSuffix() {
        assertEquals("world", builder.getNameSuffix("hello", "helloWorld"), "suffix");
        assertEquals("worldSuffix", builder.getNameSuffix("hello", "helloWorldSuffix"), "suffix camelCase");
        assertEquals("x", builder.getNameSuffix("hello", "helloX"), "suffix single letter");
        assertNull(builder.getNameSuffix("hello", "hello"), "no suffix returns null");
    }

    @Test
    public void testIsGetterMethod() throws Exception {
        assertTrue(builder.isGetterMethod(Example.class.getMethod("getVal")), "getter");
        assertTrue(builder.isGetterMethod(Example.class.getMethod("isVal")), "getter is");
        assertFalse(builder.isGetterMethod(Example.class.getMethod("accessor")), "accessor is not getter");
    }

    @Test
    public void testIsAccessorMethod() throws Exception {
        assertTrue(builder.isAccessorMethod(Example.class.getMethod("accessor")), "accessor");
        assertFalse(builder.isAccessorMethod(Example.class.getMethod("accessorNot")), "accessor not action");
    }

    @Test
    public void testIsSetterMethod() throws Exception {
        assertTrue(builder.isSetterMethod(Example.class.getMethod("setVal", String.class)), "settter");
        assertFalse(builder.isSetterMethod(Example.class.getMethod("setNot")), "settter");
    }

    @Test
    public void testIsActionMethod() throws Exception {
        assertTrue(builder.isActionMethod(Example.class.getMethod("action")), "action");
        assertFalse(builder.isActionMethod(Example.class.getMethod("actionList", List.class)), "actionList : action takes 0 params");
    }

    @Test
    public void testIsActionListMethod() throws Exception {
        assertTrue(builder.isActionListMethod(Example.class.getMethod("actionList", List.class)), "actionList");
        assertTrue(builder.isActionListMethod(Example.class.getMethod("actionListName", List.class, String.class)), "actionList with name");

        assertTrue(builder.isActionListMethod(Example.class.getMethod("actionIterable", Iterable.class)), "actionIterable");

        assertFalse(builder.isActionListMethod(Example.class.getMethod("actionArrayList", ArrayList.class)), "actionArrayList is false: cannot receive List");
        assertFalse(builder.isActionListMethod(Example.class.getMethod("actionObject", Object.class)), "actionObject is false: does not support Object");
        assertFalse(builder.isActionListMethod(Example.class.getMethod("actionListRaw", List.class)), "actionListRaw is false: does not support non parameterized type");
    }

    @Test
    public void testIsActionEditMethodPrefixed() throws Exception {
        assertTrue(builder.isActionEditMethodPrefixed(Example.class.getMethod("editString")), "edit");
        assertFalse(builder.isActionEditMethodPrefixed(Example.class.getMethod("editNoIncluded")), "edit no included");
        assertFalse(builder.isActionEditMethodPrefixed(Example.class.getMethod("methodStartA")), "edit");
    }

    @Test
    public void testIsActionEditMethod() throws Exception {
        assertTrue(builder.isActionEditMethod(Example.class.getMethod("methodStartA")), "edit");
        assertFalse(builder.isActionEditMethod(Example.class.getMethod("methodStartB")), "edit no included");
    }

    @Test
    public void testIsEditedMethod() throws Exception {
        assertTrue(builder.isEditedMethod(Example.class.getMethod("methodFinishA", Example.class)), "edited");
        assertTrue(builder.isEditedMethod(Example.class.getMethod("methodFinishA", Example.class)), "edited");
        assertFalse(builder.isEditedMethod(Example.class.getMethod("testListString")), "not edited");
    }

    @Test
    public void testIsEditedMethodPrefixed() throws Exception {
        assertTrue(builder.isEditedMethodPrefixed(Example.class.getMethod("editedString", Example.class)), "edited");
        assertTrue(builder.isEditedMethodPrefixed(Example.class.getMethod("editedStringWithOk", Example.class, boolean.class)), "edited with ok");
        assertFalse(builder.isEditedMethodPrefixed(Example.class.getMethod("methodFinishA", Example.class)), "no prefixed edited");
        assertFalse(builder.isEditedMethodPrefixed(Example.class.getMethod("methodFinishB", Example.class, boolean.class)), "no prefixed edited with ok");
    }

    @Test
    public void testGetClass() throws Exception {
        assertEquals(List.class, builder.getClass(Example.class.getMethod("testListString").getGenericReturnType()), "ParameterizedType : T<...> -> T");

        var wild = (ParameterizedType) Example.class.getMethod("testListWild").getGenericReturnType();
        assertEquals(Object.class, builder.getClass(wild.getActualTypeArguments()[0]), "WildcardType ? : upper bound is Object");

        var wildExtends = (ParameterizedType) Example.class.getMethod("testListWildString").getGenericReturnType();
        assertEquals(String.class, builder.getClass(wildExtends.getActualTypeArguments()[0]), "WildcardType ? extends T : upper bound is String ");

        var wildSuper = (ParameterizedType) Example.class.getMethod("testListWildStringSuper").getGenericReturnType();
        assertEquals(Object.class, builder.getClass(wildSuper.getActualTypeArguments()[0]), "WildcardType ? super T : upper bound is Object");

        var varMultiExtends = (ParameterizedType) Example.class.getMethod("testListVarMultiBounds").getGenericReturnType();
        assertEquals(CharSequence.class, builder.getClass(varMultiExtends.getActualTypeArguments()[0]), "TypeVariable V super A & B");

        var varMultiExtends2 = (ParameterizedType) Example.class.getMethod("testListVarMultiBounds2").getGenericReturnType();
        assertEquals(ExB.class, builder.getClass(varMultiExtends2.getActualTypeArguments()[0]), "TypeVariable V super A & B");

    }

    @Test
    public void testIsNotifierSetterMethod() throws Exception {
        assertTrue(builder.isNotifierSetterMethod(Example.class.getMethod("setXNotifier", Runnable.class)), "isNotifierSetterMethod");
    }
}
