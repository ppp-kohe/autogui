package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiNotifierSetter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;


public class GuiTypeBuilderTest {
    @GuiIncluded
    public static class Example {
        @GuiNotifierSetter
        public void setXNotifier(Runnable notifier) {}
        @GuiNotifierSetter @GuiIncluded(false)
        public void setDisabledNotifier(Runnable notifier) {}
        @GuiIncluded @GuiNotifierSetter
        public void setIncludedNotifier(Runnable notifier) {}

        public List<String> testListString() {return null;}
        public List<?> testListWild() {return null;}
        public List<? extends String> testListWildString() {return null;}
        public List<? super String> testListWildStringSuper() {return null;}
        public <T extends Appendable & CharSequence>  List<T> testListVarMultiBounds() {return null;}
        public <T extends ExA & ExB>  List<T> testListVarMultiBounds2() {return null;}

        public void edited(Example e) {}
        public void editedString(Example s) {}
        public void editedStringWithOk(Example a, boolean ok) {}
        public void methodFinishA(Example s) {}
        public void methodFinishB(Example a, boolean ok) {}
        @GuiIncluded(name="namedEditor") public void editedNamed(Example s) {}

        public Example edit() {return null;}
        public Example editString() {return null;}
        public ExA editNoIncluded() {return null;}
        public Example methodStartA() {return null;}
        public ExA methodStartB() {return null;}
        @GuiIncluded(index = 123) public Example editAnnotated() {return null;}
        @GuiIncluded(action = true) public Example editAction() {return null;}
        public Example editList(List<String> list) {return null;}

        public void actionList(List<String> a) {}
        public void actionListName(List<String> a, String name) {}
        public void actionIterable(Iterable<String> a) {}
        public void actionArrayList(ArrayList<String> a) {}
        public void actionObject(Object a) {}
        @SuppressWarnings("rawtypes")
        public void actionListRaw(List a) {}

        public void action() {}
        @GuiIncluded(name="namedAction") public void actionNamed() {}
        @GuiIncluded public void actionNoNamed() {}

        public void setVal(String s) {}
        public void setNot(){}

        public int accessor() {return 0;}
        @GuiIncluded(action = true)
        public int accessorNot() {return 0;}

        public int getVal() {return 0;}
        public boolean isVal() {return false;}
        @GuiIncluded(name="getHelloWorld", index = 123) public int getNamed() {return 0;}
        @GuiIncluded public int getNoNamed() {return 0;}

        @GuiIncluded(false) public void excluded() {}

        @GuiIncluded public static void staticMethod() {}

        public String fldMember;
        @GuiIncluded(name="namedFld", index = 345) public String fieldNamed;
        @GuiIncluded public String fieldNotNamed;
        public static String staticFld;
        @GuiIncluded(false) public String fildNotMember;

        @GuiIncluded(index = 300) public String propVal;
        @GuiIncluded(index = 200)  public void setPropVal(String s) {propVal = s;}
        @GuiIncluded(index = 100)  public String getPropVal() {return propVal;}
    }

    public interface ExA {}
    public interface ExB extends ExA {}
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
        public void methodS() {}
        protected void methodSProtected() {}
        private void methodSPrivate() {}
        public int fieldS;
        protected int fieldSProtected;
        private int fieldSPrivate;
        public static int fieldSStatic;
        public static void methodSStatic() {} //inherited
    }

    public interface ExampleListingI {
        default void methodI() {}
        static void methodIStatic() {} //not inherited to implementing class.
    }

    public static class ExampleListingSub extends ExampleListing implements ExampleListingI {
        public void method() {}
        protected void methodProtected() {}
        private void methodPrivate() {}
        public int field;
        protected int fieldProtected;
        private int fieldPrivate;
        public static int fieldStatic;
        public static void methodStatic() {}
    }

    @GuiIncluded
    public static class ExampleObj {
        @GuiIncluded(index = 10) public int fld;
        @GuiIncluded(index = 100) public void setFld(int i) {fld = i;}
        @GuiIncluded(index = 5) public String prop() {return "";}
        @GuiIncluded(action = true, index = 10) public String action() {return "";}
        @GuiIncluded(action = true, index = 1, name="edt") public ExampleEditor edit() {return new ExampleEditor();}
        @GuiIncluded(name="edt") public void edited(ExampleEditor e) {}
        @GuiIncluded(index = 10, name="prp") @GuiNotifierSetter public void setPNtf(Runnable r) {}
        @GuiIncluded(index = 5) @GuiNotifierSetter public void setFldNotifier(Runnable r) {}
        @GuiIncluded public Runnable getFldNotifier() {return null;}
        public void excluded() {}
    }

    @GuiIncluded
    public static class ExampleObjGen<E> extends ExampleObj {}

    @GuiIncluded
    public static class ExampleEditor {}

    public static class ExampleCollection {
        public List<ExampleObj> collection() {return null;}
        public ExampleObj[] array() {return null;}
        public ExampleSubList<ExampleObj> collectionUser() {return null;}
        public <T> ExampleObjGen<T> param() {return null;}
        public <T extends ExampleObj> T typeVar() {return null;}
        public List<?> wild() {return null;}
        public List<? extends ExampleObj> wildBound() {return null;}
    }

    @GuiIncluded(value = false)
    public static class ExampleExcluded {}

    public static class ExampleSubList<E> extends AbstractList<E> {
        @Override public E get(int index) {return null;}
        @Override public int size() { return 0; }
    }

    @GuiIncluded
    public static class ExampleRec<T extends ExampleRec<T>> extends ArrayList<T> {
        public static <X extends ExampleRec<X>> ExampleRec<X> rec() {return null;}
    }

    GuiTypeBuilder builder;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
    }

    @Test
    public void testConstructorWithValueTypes() {
        var b = new GuiTypeBuilder(List.of(ExampleExcluded.class, ExampleListingI.class));
        Assert.assertTrue("class", b.isValueType(ExampleExcluded.class));
        Assert.assertTrue("interf", b.isValueType(ExampleListingI.class));
        Assert.assertTrue("interface impl.", b.isValueType(ExampleListingSub.class));
    }

    @Test
    public void testGet() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();

        var obj = builder.get(ExampleObj.class);
        {
            Assert.assertEquals("obj", GuiTypeObject.class, obj.getClass());
            Assert.assertEquals("obj type", ExampleObj.class, ((GuiTypeObject) obj).getType());
            Assert.assertEquals("obj member", 2, ((GuiTypeObject) obj).getProperties().size());

            var newObj = builder.get(ExampleObj.class);
            Assert.assertSame("obj get same", obj, newObj);
        }

        var col = builder.get(colType);
        {
            Assert.assertEquals("collection", GuiTypeCollection.class, col.getClass());
            var colTyped = (GuiTypeCollection) col;
            Assert.assertEquals("collection type", List.class, colTyped.getType());
            var elem = colTyped.getElementType();
            Assert.assertTrue("recursive get", elem instanceof GuiTypeObject);
            Assert.assertEquals("obj", ExampleObj.class, ((GuiTypeObject) elem).getType());
            Assert.assertSame("reused element", obj, elem);
        }

    }

    @Test
    public void testGetRec() throws Exception{
        var recMethod = ExampleRec.class.getMethod("rec");
        var recType = (ParameterizedType) recMethod.getGenericReturnType();
        var obj = builder.get(recType);
        {
            Assert.assertEquals("col", GuiTypeCollection.class, obj.getClass());
            Assert.assertEquals("col type", ExampleRec.class, ((GuiTypeCollection) obj).getType());
            var elem = ((GuiTypeCollection) obj).getElementType();
            Assert.assertTrue("rec obj becomes another object from bounded type-variable", elem instanceof GuiTypeObject);
            Assert.assertEquals("rec obj", ExampleRec.class, ((GuiTypeObject) elem).getType());

            var recObj = builder.get(ExampleRec.class);
            {
                Assert.assertSame("rec obj reused", elem, recObj);
            }
        }

    }

    @Test
    public void testCreate() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();


        var obj = builder.create(ExampleObj.class);
        {
            Assert.assertEquals("obj", GuiTypeObject.class, obj.getClass());
            Assert.assertEquals("obj type", ExampleObj.class, ((GuiTypeObject) obj).getType());

            var newObj = builder.create(ExampleObj.class);
            Assert.assertNotSame("obj create diffrent", obj, newObj);
            Assert.assertEquals("obj member", 2, ((GuiTypeObject) obj).getProperties().size());

            obj = newObj; //create overwrites the old type-element
        }

        var col = builder.create(colType);
        {
            Assert.assertEquals("collection", GuiTypeCollection.class, col.getClass());
            var colTyped = (GuiTypeCollection) col;
            Assert.assertEquals("collection type", List.class, colTyped.getType());
            var elem = colTyped.getElementType();
            Assert.assertTrue("recursive get", elem instanceof GuiTypeObject);
            Assert.assertEquals("obj", ExampleObj.class, ((GuiTypeObject) elem).getType());
            Assert.assertSame("reused element", obj, elem);
        }
    }

    @Test
    public void testCreateForListSubType()  throws Exception {
        var colSubMethod = ExampleCollection.class.getMethod("collectionUser");
        var colSubType = (ParameterizedType) colSubMethod.getGenericReturnType();
        var obj = builder.create(ExampleObj.class);

        var colSub = builder.create(colSubType);
        {
            Assert.assertEquals("collection", GuiTypeCollection.class, colSub.getClass());
            var colTyped = (GuiTypeCollection) colSub;
            Assert.assertEquals("collection type", ExampleSubList.class, colTyped.getType());
            var elem = colTyped.getElementType();
            Assert.assertTrue("recursive get", elem instanceof GuiTypeObject);
            Assert.assertEquals("obj", ExampleObj.class, ((GuiTypeObject) elem).getType());
            Assert.assertSame("reused element", obj, elem);
        }
    }

    @Test
    public void testCreateForGenerics() throws Exception {
        var paramMethod = ExampleCollection.class.getMethod("param");
        var paramType = (ParameterizedType) paramMethod.getGenericReturnType();
        var paramObj = builder.create(paramType);
        {
            Assert.assertEquals("param", GuiTypeObject.class, paramObj.getClass());
            Assert.assertEquals("obj", ExampleObjGen.class, ((GuiTypeObject) paramObj).getType());

            var paramObjNew = builder.create(paramType);
            Assert.assertNotSame("create different", paramObj, paramObjNew);
            Assert.assertNotSame("create different", paramObjNew, builder.create(ExampleObjGen.class));
        }
    }

    @Test
    public void testCreateForTypeVar() throws Exception {
        var typeVarMethod = ExampleCollection.class.getMethod("typeVar");
        var typeVarType = (TypeVariable<?>) typeVarMethod.getGenericReturnType();
        var obj = builder.create(typeVarType);
        {
            Assert.assertEquals("obj", GuiTypeObject.class, obj.getClass());
            Assert.assertEquals("obj type", ExampleObj.class, ((GuiTypeObject) obj).getType());

            var objNew = builder.create(typeVarType);
            Assert.assertNotSame("create different", obj, objNew);
            Assert.assertNotSame("create different", objNew, builder.create(ExampleObj.class));
        }
    }

    @Test
    public void testCreateForWild() throws Exception {
        var wildMethod = ExampleCollection.class.getMethod("wild");
        var wildType = (WildcardType) (((ParameterizedType) wildMethod.getGenericReturnType()).getActualTypeArguments()[0]);
        var obj = builder.create(wildType);
        {
            Assert.assertEquals("obj", GuiTypeValue.class, obj.getClass());
            Assert.assertEquals("obj type", Object.class, ((GuiTypeValue) obj).getType());

            var objNew = builder.create(wildType);
            Assert.assertNotSame("create different", obj, objNew);
            Assert.assertNotSame("create different", objNew, builder.create(Object.class));
        }
    }

    @Test
    public void testCreateForWildBound() throws Exception {
        var wildMethod = ExampleCollection.class.getMethod("wildBound");
        var wildType = (WildcardType) (((ParameterizedType) wildMethod.getGenericReturnType()).getActualTypeArguments()[0]);
        var obj = builder.create(wildType);
        {
            Assert.assertEquals("obj", GuiTypeObject.class, obj.getClass());
            Assert.assertEquals("obj type", ExampleObj.class, ((GuiTypeObject) obj).getType());

            var objNew = builder.create(wildType);
            Assert.assertNotSame("create different", obj, objNew);
            Assert.assertNotSame("create different", objNew, builder.create(ExampleObj.class));
        }
    }

    @Test
    public void testCreateFromClass() {
        var obj = builder.createFromClass(ExampleObj.class, ExampleObj.class);
        Assert.assertEquals("obj", GuiTypeObject.class, obj.getClass());
        Assert.assertEquals("obj type", ExampleObj.class, ((GuiTypeObject) obj).getType());
        Assert.assertEquals("obj member", 2, ((GuiTypeObject) obj).getProperties().size());

        var array = builder.createFromClass(ExampleObj[].class, ExampleObj[].class);
        Assert.assertEquals("array", GuiTypeCollectionArray.class, array.getClass());
        Assert.assertEquals("array type", ExampleObj[].class, ((GuiTypeCollectionArray) array).getType());
        Assert.assertEquals("array component reused", obj, ((GuiTypeCollectionArray) array).getElementType());

        var value = builder.createFromClass(String.class, String.class);
        Assert.assertEquals("value", GuiTypeValue.class, value.getClass());
        Assert.assertEquals("value type", String.class, ((GuiTypeValue) value).getType());

        var valueObj = builder.createFromClass(ExampleExcluded.class, ExampleExcluded.class);
        Assert.assertEquals("value obj", GuiTypeValue.class, valueObj.getClass());
        Assert.assertEquals("value type excluded", ExampleExcluded.class, ((GuiTypeValue) valueObj).getType());


        var objWithoutGenType = builder.createFromClass(ExampleObj.class);
        Assert.assertEquals("from class", GuiTypeObject.class, objWithoutGenType.getClass());
        Assert.assertEquals("from class type", ExampleObj.class, ((GuiTypeObject) objWithoutGenType).getType());
        Assert.assertNotSame("create different obj", obj, objWithoutGenType);
    }

    @Test
    public void testIsIncludedClass() {
        Assert.assertTrue("include primitve", builder.isIncludedClass(int.class));
        Assert.assertTrue("include value", builder.isIncludedClass(Integer.class));
        Assert.assertTrue("include value", builder.isIncludedClass(Boolean.class));
        Assert.assertTrue("include value", builder.isIncludedClass(String.class));
        Assert.assertTrue("include obj", builder.isIncludedClass(ExampleObj.class));
        Assert.assertFalse("excluded", builder.isIncludedClass(ExampleExcluded.class));
        Assert.assertFalse("excluded", builder.isIncludedClass(Object.class));
        Assert.assertTrue("include array value", builder.isIncludedClass(String[].class));
        Assert.assertTrue("include array obj", builder.isIncludedClass(ExampleObj[].class));
        Assert.assertFalse("array excluded", builder.isIncludedClass(ExampleExcluded[].class));
    }

    @Test
    public void testCreateValueFromClass() {
        var val = builder.createValueFromClass(ExampleObj.class, ExampleObj.class);
        Assert.assertEquals("value", GuiTypeValue.class, val.getClass());
        Assert.assertEquals("type", ExampleObj.class, val.getType());
    }

    @Test
    public void testCreateCollectionFromClass() throws Exception {
        var colMethod = ExampleCollection.class.getMethod("collection");
        var colType = (ParameterizedType) colMethod.getGenericReturnType();

        var col = builder.createCollectionFromType(colType);
        Assert.assertEquals("collection", List.class, col.getType());
        var elem = col.getElementType();
        Assert.assertTrue("recursive get", elem instanceof GuiTypeObject);
        var obj =  (GuiTypeObject) elem;
        Assert.assertEquals("component type", ExampleObj.class, obj.getType());
        Assert.assertEquals("recursive get with constructing members", 2, obj.getProperties().size());
    }

    @Test
    public void testCreateCollectionArrayFromClass() throws Exception {
        var arrayMethod = ExampleCollection.class.getMethod("array");
        var arrayCls = arrayMethod.getReturnType();
        var arrayType = arrayMethod.getGenericReturnType();

        var array = builder.createCollectionArrayFromClass(arrayCls, arrayType);
        Assert.assertEquals("array", arrayCls.getName(), array.getName());
        var obj = array.getElementType();
        Assert.assertTrue("recursive get", obj instanceof GuiTypeObject);
        Assert.assertEquals("component type", ExampleObj.class, ((GuiTypeObject) obj).getType());
        Assert.assertEquals("recursive get with constructing members", 2, ((GuiTypeObject) obj).getProperties().size());
    }

    @Test
    public void testCreateObjectFromClassProps() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        Assert.assertEquals("props", 2, obj.getProperties().size());
        {
            var fld = obj.getProperties().get(1);
            var prop = obj.getProperties().getFirst();
            Assert.assertEquals("fld", fld.getName());
            Assert.assertEquals("prop", prop.getName());

            Assert.assertEquals("fld field", "fld", fld.getField().getName());
            Assert.assertNull("fld getter", fld.getGetter());
            Assert.assertEquals("fld setter", "setFld", fld.getSetter().getName());

            Assert.assertNull("prop field", prop.getField());
            Assert.assertEquals("prop getter", "prop", prop.getGetter().getName());
            Assert.assertNull("prop setter", prop.getSetter());
        }
    }
    @Test
    public void testCreateObjectFromClassActions() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        Assert.assertEquals("ations", 2, obj.getActions().size());
        {
            var action = obj.getActions().get(1);
            var edit = obj.getActions().getFirst();
            Assert.assertEquals("action", "action", action.getName());
            Assert.assertEquals("edited", "edt", edit.getName());

            Assert.assertEquals("action method", "action", action.getMethod().getName());
            Assert.assertEquals("edit method", "edit", edit.getMethod().getName());

            Assert.assertTrue("has edited", edit.hasEditedMethod());
            Assert.assertEquals("edited name", "edited", edit.getEditFeature().editedMethodName());
            Assert.assertEquals("edited method", "edited", edit.getEditFeature().editedMethod().getName());
        }
    }
    @Test
    public void testCreateObjectFromClassNotifiers() {
        var obj = builder.createObjectFromClass(ExampleObj.class, ExampleObj.class);

        Assert.assertEquals("notifiers", 2, obj.getNotifiers().size());
        {
            var fld = obj.getNotifiers().getFirst();
            var prop = obj.getNotifiers().get(1);
            Assert.assertEquals("fldNotifier", fld.getName());
            Assert.assertEquals("prp", prop.getName());

            Assert.assertNull("fld field", fld.getField());
            Assert.assertEquals("fld getter", "getFldNotifier", fld.getGetter().getName());
            Assert.assertEquals("fld setter", "setFldNotifier", fld.getSetter().getName());

            Assert.assertNull("prop field", prop.getField());
            Assert.assertNull("prop getter", prop.getGetter());
            Assert.assertEquals("prop setter", "setPNtf", prop.getSetter().getName());
        }
    }

    @Test
    public void testListFields() {
        var list = builder.listFields(ExampleListingSub.class);
        var names = list.stream()
                .map(Field::getName)
                .collect(Collectors.toSet());
        Assert.assertEquals("listed public static fields includes super types",
                Set.of("fieldS","field", "fieldSStatic", "fieldStatic"),
                names);
    }

    @Test
    public void testListMethod() {
        var list = builder.listMethods(ExampleListingSub.class);
        var names = list.stream()
                .map(Method::getName)
                .sorted()
                .collect(Collectors.toList());
        var expected = new ArrayList<String>();
        Arrays.stream(Object.class.getMethods())
                .map(Method::getName)
                .forEach(expected::add);
        expected.addAll(Set.of("method","methodStatic","methodS", "methodSStatic", "methodI"));
        expected.sort(Comparator.naturalOrder());

        Assert.assertEquals("listed public static methods includes super types, but excludes interface static methods",
                expected,
                names);
    }

    @Test
    public void testIsValueType() {
        Assert.assertTrue("primitive", builder.isValueType(int.class));
        Assert.assertTrue("primitive", builder.isValueType(boolean.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Integer.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Short.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Long.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Byte.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Character.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Float.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Double.class));
        Assert.assertTrue("primitive boxed", builder.isValueType(Boolean.class));
        Assert.assertTrue("string", builder.isValueType(String.class));
        Assert.assertTrue("charSequence", builder.isValueType(StringBuilder.class));
        Assert.assertFalse("not value: object", builder.isValueType(Object.class));
        Assert.assertFalse("not value: array", builder.isValueType(int[].class));
        Assert.assertFalse("not value: List", builder.isValueType(List.class));
        Assert.assertFalse("not value: other", builder.isValueType(Example.class));
    }

    @Test
    public void testIsExcludedType() {
        Assert.assertTrue(builder.isExcludedType(ExA.class));
        Assert.assertFalse(builder.isExcludedType(Example.class));
    }

    @Test
    public void testIsGuiIncludedEnabled() throws Exception {
        Assert.assertTrue("attached", builder.isGuiIncludedEnabled(Example.class));
        Assert.assertFalse("attached but false", builder.isGuiIncludedEnabled(Example.class.getField("fildNotMember")));
        Assert.assertFalse("attached notifier but false", builder.isGuiIncludedEnabled(Example.class.getMethod("setDisabledNotifier", Runnable.class)));
        Assert.assertTrue("attached notifier", builder.isGuiIncludedEnabled(Example.class.getMethod("setIncludedNotifier", Runnable.class)));
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
        Assert.assertEquals("owner", obj, p.getOwner());
        Assert.assertEquals("name", "propVal", p.getName());
        Assert.assertEquals("ordinal", 100, p.getOrdinal().getIndex());
        Assert.assertEquals("setter", "setPropVal", p.getSetter().getName());
        Assert.assertEquals("getter", "getPropVal", p.getGetter().getName());
        Assert.assertEquals("field", "propVal", p.getField().getName());
    }

    @Test
    public void testCreateMemberAsNotifier() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("xNotifier");
        m.methods.add(Example.class.getMethod("setXNotifier", Runnable.class));

        builder.createMember(obj, m);

        var n = obj.getNotifiers().getFirst();
        Assert.assertEquals("name", "xNotifier", n.getName());
        Assert.assertEquals("created prop", "setXNotifier", n.getSetter().getName());
    }

    @Test
    public void testCreateMemberAsActionList() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("actionList");
        m.methods.add(Example.class.getMethod("actionList",List.class));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        Assert.assertTrue("list", a instanceof GuiTypeMemberActionList);
        Assert.assertEquals("name", "actionList", a.getName());
        Assert.assertEquals("created action", "actionList", a.getMethod().getName());
    }

    @Test
    public void testCreateMemberAsAction() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("action");
        m.methods.add(Example.class.getMethod("action"));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        Assert.assertEquals("name", "action", a.getName());
        Assert.assertEquals("created action", "action", a.getMethod().getName());
    }


    @Test
    public void testCreateMemberAsActionEdit() throws Exception {
        var obj = new GuiTypeObject(Example.class);

        GuiTypeBuilder.MemberDefinitions m = new GuiTypeBuilder.MemberDefinitions("editAction");
        m.methods.add(Example.class.getMethod("editAction"));
        m.supplementaryMethods.add(Example.class.getMethod("edited", Example.class));

        builder.createMember(obj, m);

        var a = obj.getActions().getFirst();
        Assert.assertEquals("name", "editAction", a.getName());
        Assert.assertEquals("created action", "editAction", a.getMethod().getName());
        Assert.assertEquals("edited", "edited", a.getEditFeature().editedMethod().getName());
    }

    @Test
    public void testCreateMemberPropertyWithoutProp() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberProperty(obj, "propVal",
                Example.class.getField("propVal"),
                Example.class.getMethod("getPropVal"),
                Example.class.getMethod("setPropVal", String.class));
        var p = obj.getProperties().getFirst();
        Assert.assertEquals("owner", obj, p.getOwner());
        Assert.assertEquals("ordinal", 100, p.getOrdinal().getIndex());
        Assert.assertEquals("setter", "setPropVal", p.getSetter().getName());
        Assert.assertEquals("getter", "getPropVal", p.getGetter().getName());
        Assert.assertEquals("field", "propVal", p.getField().getName());
    }

    @Test
    public void testCreateMemberPropertyNotifier() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberPropertyNotifier(obj, "xNotifier", null, null,
                Example.class.getMethod("setXNotifier", Runnable.class));

        var n = obj.getNotifiers().getFirst();
        Assert.assertEquals("created prop", "setXNotifier", n.getSetter().getName());
    }

    @Test
    public void testCreateMemberProperty() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        var p = new GuiTypeMemberProperty("propVal");
        builder.createMemberProperty(obj, "propVal",
                Example.class.getField("propVal"),
                Example.class.getMethod("getPropVal"),
                Example.class.getMethod("setPropVal", String.class), p);
        Assert.assertEquals("owner", obj, p.getOwner());
        Assert.assertEquals("ordinal", 100, p.getOrdinal().getIndex());
        Assert.assertEquals("setter", "setPropVal", p.getSetter().getName());
        Assert.assertEquals("getter", "getPropVal", p.getGetter().getName());
        Assert.assertEquals("field", "propVal", p.getField().getName());
    }

    @Test
    public void testGeMemberOrdinalIndex() throws Exception {
        Assert.assertEquals("field oridnal", OptionalInt.of(345),
                builder.getMemberOrdinalIndex(Example.class.getField("fieldNamed")));
        Assert.assertEquals("field oridnal none", OptionalInt.empty(),
                builder.getMemberOrdinalIndex(Example.class.getField("fldMember")));

        Assert.assertEquals("method oridnal", OptionalInt.of(123),
                builder.getMemberOrdinalIndex(Example.class.getMethod("getNamed")));
        Assert.assertEquals("method oridnal none", OptionalInt.empty(),
                builder.getMemberOrdinalIndex(Example.class.getMethod("getVal")));
    }

    @Test
    public void testCreateMemberAction() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "action", Example.class.getMethod("action"), false, false, null);

        var action = obj.getActions().getFirst();
        Assert.assertEquals("action type", GuiTypeMemberAction.class, action.getClass());
        Assert.assertEquals("createMemberAction", "action", action.getName());
        Assert.assertEquals("action ordial", Short.MAX_VALUE, action.getOrdinal().getIndex());
        Assert.assertEquals("action owner", obj, action.getOwner());
    }

    @Test
    public void testCreateMemberActionList() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "actionList", Example.class.getMethod("actionList", List.class), true);

        var action = obj.getActions().getFirst();
        Assert.assertEquals("action type", GuiTypeMemberActionList.class, action.getClass());
        Assert.assertEquals("element", new GuiTypeValue(String.class), ((GuiTypeMemberActionList) action).getElementType());
        Assert.assertEquals("createMemberAction", "actionList", action.getName());
        Assert.assertEquals("action ordial", Short.MAX_VALUE, action.getOrdinal().getIndex());
        Assert.assertEquals("action owner", obj, action.getOwner());
    }

    @Test
    public void testCreateMemberActionListNamed() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "actionListName", Example.class.getMethod("actionListName", List.class, String.class), true, false, null);

        var action = obj.getActions().getFirst();
        Assert.assertEquals("action type", GuiTypeMemberActionList.class, action.getClass());
        Assert.assertEquals("element", new GuiTypeValue(String.class), ((GuiTypeMemberActionList) action).getElementType());
        Assert.assertTrue("take name",  ((GuiTypeMemberActionList) action).isTakingTargetName());
        Assert.assertEquals("createMemberAction", "actionListName", action.getName());
        Assert.assertEquals("action ordial", Short.MAX_VALUE, action.getOrdinal().getIndex());
        Assert.assertEquals("action owner", obj, action.getOwner());
    }

    @Test
    public void testCreateMemberActionEdited() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "action", Example.class.getMethod("editAnnotated"), false, true,
                Example.class.getMethod("editedStringWithOk", Example.class, boolean.class));

        var action = obj.getActions().getFirst();
        Assert.assertEquals("action type", GuiTypeMemberAction.class, action.getClass());
        Assert.assertEquals("createMemberAction", "action", action.getName());
        Assert.assertEquals("action ordial", 123, action.getOrdinal().getIndex());
        Assert.assertEquals("action owner", obj, action.getOwner());
        Assert.assertEquals("method", "editAnnotated", action.getMethod().getName());
        Assert.assertEquals("rettype", new GuiTypeObject(Example.class), action.getReturnType());

        Assert.assertTrue("feature", action.hasEditedMethod());
        Assert.assertEquals("edited method", "editedStringWithOk", action.getEditFeature().editedMethodName());
        Assert.assertEquals("edited method", "editedStringWithOk", action.getEditFeature().editedMethod().getName());
    }

    @Test
    public void testCreateMemberActionListEdited() throws Exception {
        var obj = new GuiTypeObject(Example.class);
        builder.createMemberAction(obj, "editList", Example.class.getMethod("editList", List.class), true, true,
                Example.class.getMethod("editedStringWithOk", Example.class, boolean.class));

        var action = obj.getActions().getFirst();
        Assert.assertEquals("action type", GuiTypeMemberActionList.class, action.getClass());
        Assert.assertEquals("createMemberAction", "editList", action.getName());
        Assert.assertEquals("action owner", obj, action.getOwner());
        Assert.assertEquals("method", "editList", action.getMethod().getName());
        Assert.assertEquals("rettype", new GuiTypeObject(Example.class), action.getReturnType());

        Assert.assertTrue("feature", action.hasEditedMethod());
        Assert.assertEquals("edited method", "editedStringWithOk", action.getEditFeature().editedMethodName());
        Assert.assertEquals("edited method", "editedStringWithOk", action.getEditFeature().editedMethod().getName());
    }


    @Test
    public void testIsMemberField() throws Exception {
        Assert.assertFalse("field", builder.isMemberField(
                Example.class.getField("fldMember")));
        Assert.assertTrue("field name=...", builder.isMemberField(
                Example.class.getField("fieldNamed")));
        Assert.assertTrue("field name=...", builder.isMemberField(
                Example.class.getField("fieldNamed")));
        Assert.assertTrue("field name", builder.isMemberField(
                Example.class.getField("fieldNotNamed")));
        Assert.assertFalse("field name", builder.isMemberField(
                Example.class.getField("fildNotMember")));
    }

    @Test
    public void testIsMemberFieldWithoutAnnotation() throws Exception {
        Assert.assertTrue("field", builder.isMemberFieldWithoutAnnotation(
                Example.class.getField("fldMember")));
        Assert.assertFalse("static field", builder.isMemberFieldWithoutAnnotation(
                Example.class.getField("staticFld")));
    }

    @Test
    public void testGetMemberNameFromField() throws Exception {
        Assert.assertEquals("field", "fldMember", builder.getMemberNameFromField(
                Example.class.getField("fldMember")));
        Assert.assertEquals("field name=...", "namedFld", builder.getMemberNameFromField(
                Example.class.getField("fieldNamed")));
        Assert.assertEquals("field name=...", "namedFld", builder.getMemberNameFromField(
                Example.class.getField("fieldNamed")));
        Assert.assertEquals("field name", "fieldNotNamed", builder.getMemberNameFromField(
                Example.class.getField("fieldNotNamed")));
    }

    @Test
    public void testIsMemberMethod() throws Exception {
        Assert.assertFalse("included(false)", builder.isMemberMethod(
                Example.class.getMethod("excluded")));
        Assert.assertTrue("included", builder.isMemberMethod(
                Example.class.getMethod("getNoNamed")));

        Assert.assertFalse("static", builder.isMemberMethod(
                Example.class.getMethod("staticMethod")));

        Assert.assertFalse("no included tag", builder.isMemberMethod(
                Example.class.getMethod("action")));

        Assert.assertTrue("notifierSetter", builder.isMemberMethod(
                Example.class.getMethod("setXNotifier", Runnable.class)));
        Assert.assertFalse("disabled notifierSetter", builder.isMemberMethod(
                Example.class.getMethod("setDisabledNotifier", Runnable.class)));
    }

    @Test
    public void testGetMemberNameFromMethod() throws Exception {
        Assert.assertEquals("action", "action", builder.getMemberNameFromMethod(
                Example.class.getMethod("action")));
        Assert.assertEquals("getter", "val", builder.getMemberNameFromMethod(
                Example.class.getMethod("getVal")));
        Assert.assertEquals("getter named", "getHelloWorld", builder.getMemberNameFromMethod(
                Example.class.getMethod("getNamed")));
        Assert.assertEquals("getter no name", "noNamed", builder.getMemberNameFromMethod(
                Example.class.getMethod("getNoNamed")));
    }

    @Test
    public void testGetMemberNameFromMethodForAction() throws Exception {
        Assert.assertEquals("action", "action", builder.getMemberNameFromMethodForAction(
                Example.class.getMethod("action")));
        Assert.assertEquals("action with name", "namedAction", builder.getMemberNameFromMethodForAction(
                Example.class.getMethod("actionNamed")));
        Assert.assertEquals("action with no name", "actionNoNamed", builder.getMemberNameFromMethodForAction(
                Example.class.getMethod("actionNoNamed")));
    }

    @Test
    public void testGetMemberNameFromMethodEditedForActionEdit() throws Exception {
        Assert.assertEquals("editedX -> editX", "editString", builder.getMemberNameFromMethodEditedForActionEdit(
                Example.class.getMethod("editedString", Example.class)));

        Assert.assertEquals("edited -> edit", "edit", builder.getMemberNameFromMethodEditedForActionEdit(
                Example.class.getMethod("edited", Example.class)));

        Assert.assertEquals("edited with name=x -> x", "namedEditor", builder.getMemberNameFromMethodEditedForActionEdit(
                Example.class.getMethod("editedNamed", Example.class)));
    }

    @Test
    public void testGetMemberNameFromMethodName() {
        Assert.assertEquals("getter", "x", builder.getMemberNameFromMethodName("getX"));
        Assert.assertEquals("setter", "x", builder.getMemberNameFromMethodName("setX"));
        Assert.assertEquals("getter is", "x", builder.getMemberNameFromMethodName("isX"));
        Assert.assertEquals("no prefix", "action", builder.getMemberNameFromMethodName("action"));
    }

    @Test
    public void testGetNameSuffix() {
        Assert.assertEquals("suffix", "world", builder.getNameSuffix("hello", "helloWorld"));
        Assert.assertEquals("suffix camelCase","worldSuffix", builder.getNameSuffix("hello", "helloWorldSuffix"));
        Assert.assertEquals("suffix single letter", "x", builder.getNameSuffix("hello", "helloX"));
        Assert.assertNull("no suffix returns null", builder.getNameSuffix("hello", "hello"));
    }

    @Test
    public void testIsGetterMethod() throws Exception {
        Assert.assertTrue("getter", builder.isGetterMethod(Example.class.getMethod("getVal")));
        Assert.assertTrue("getter is", builder.isGetterMethod(Example.class.getMethod("isVal")));
        Assert.assertFalse("accessor is not getter", builder.isGetterMethod(Example.class.getMethod("accessor")));
    }

    @Test
    public void testIsAccessorMethod() throws Exception{
        Assert.assertTrue("accessor", builder.isAccessorMethod(Example.class.getMethod("accessor")));
        Assert.assertFalse("accessor not action", builder.isAccessorMethod(Example.class.getMethod("accessorNot")));
    }

    @Test
    public void testIsSetterMethod() throws Exception {
        Assert.assertTrue("settter", builder.isSetterMethod(Example.class.getMethod("setVal", String.class)));
        Assert.assertFalse("settter", builder.isSetterMethod(Example.class.getMethod("setNot")));
    }

    @Test
    public void testIsActionMethod() throws Exception {
        Assert.assertTrue("action", builder.isActionMethod(Example.class.getMethod("action")));
        Assert.assertFalse("actionList : action takes 0 params", builder.isActionMethod(Example.class.getMethod("actionList", List.class)));
    }

    @Test
    public void testIsActionListMethod() throws Exception {
        Assert.assertTrue("actionList", builder.isActionListMethod(Example.class.getMethod("actionList", List.class)));
        Assert.assertTrue("actionList with name", builder.isActionListMethod(Example.class.getMethod("actionListName", List.class, String.class)));

        Assert.assertTrue("actionIterable", builder.isActionListMethod(Example.class.getMethod("actionIterable", Iterable.class)));

        Assert.assertFalse("actionArrayList is false: cannot receive List", builder.isActionListMethod(Example.class.getMethod("actionArrayList", ArrayList.class)));
        Assert.assertFalse("actionObject is false: does not support Object", builder.isActionListMethod(Example.class.getMethod("actionObject", Object.class)));
        Assert.assertFalse("actionListRaw is false: does not support non parameterized type", builder.isActionListMethod(Example.class.getMethod("actionListRaw", List.class)));
    }

    @Test
    public void testIsActionEditMethodPrefixed() throws Exception {
        Assert.assertTrue("edit", builder.isActionEditMethodPrefixed(Example.class.getMethod("editString")));
        Assert.assertFalse("edit no included", builder.isActionEditMethodPrefixed(Example.class.getMethod("editNoIncluded")));
        Assert.assertFalse("edit", builder.isActionEditMethodPrefixed(Example.class.getMethod("methodStartA")));
    }

    @Test
    public void testIsActionEditMethod() throws Exception {
        Assert.assertTrue("edit", builder.isActionEditMethod(Example.class.getMethod("methodStartA")));
        Assert.assertFalse("edit no included", builder.isActionEditMethod(Example.class.getMethod("methodStartB")));
    }

    @Test
    public void testIsEditedMethod() throws Exception {
        Assert.assertTrue("edited", builder.isEditedMethod(Example.class.getMethod("methodFinishA", Example.class)));
        Assert.assertTrue("edited", builder.isEditedMethod(Example.class.getMethod("methodFinishA", Example.class)));
        Assert.assertFalse("not edited", builder.isEditedMethod(Example.class.getMethod("testListString")));
    }

    @Test
    public void testIsEditedMethodPrefixed() throws Exception {
        Assert.assertTrue("edited", builder.isEditedMethodPrefixed(Example.class.getMethod("editedString", Example.class)));
        Assert.assertTrue("edited with ok", builder.isEditedMethodPrefixed(Example.class.getMethod("editedStringWithOk", Example.class, boolean.class)));
        Assert.assertFalse("no prefixed edited", builder.isEditedMethodPrefixed(Example.class.getMethod("methodFinishA", Example.class)));
        Assert.assertFalse("no prefixed edited with ok", builder.isEditedMethodPrefixed(Example.class.getMethod("methodFinishB", Example.class, boolean.class)));
    }

    @Test
    public void testGetClass() throws Exception {
        Assert.assertEquals("ParameterizedType : T<...> -> T", List.class,
                builder.getClass(Example.class.getMethod("testListString").getGenericReturnType()));

        var wild = (ParameterizedType) Example.class.getMethod("testListWild").getGenericReturnType();
        Assert.assertEquals("WildcardType ? : upper bound is Object", Object.class,
                builder.getClass(wild.getActualTypeArguments()[0]));

        var wildExtends = (ParameterizedType) Example.class.getMethod("testListWildString").getGenericReturnType();
        Assert.assertEquals("WildcardType ? extends T : upper bound is String ", String.class,
                builder.getClass(wildExtends.getActualTypeArguments()[0]));

        var wildSuper = (ParameterizedType) Example.class.getMethod("testListWildStringSuper").getGenericReturnType();
        Assert.assertEquals("WildcardType ? super T : upper bound is Object", Object.class,
                builder.getClass(wildSuper.getActualTypeArguments()[0]));

        var varMultiExtends = (ParameterizedType) Example.class.getMethod("testListVarMultiBounds").getGenericReturnType();
        Assert.assertEquals("TypeVariable V super A & B", CharSequence.class,
                builder.getClass(varMultiExtends.getActualTypeArguments()[0]));

        var varMultiExtends2 = (ParameterizedType) Example.class.getMethod("testListVarMultiBounds2").getGenericReturnType();
        Assert.assertEquals("TypeVariable V super A & B", ExB.class,
                builder.getClass(varMultiExtends2.getActualTypeArguments()[0]));

    }

    @Test
    public void testIsNotifierSetterMethod() throws Exception {
        Assert.assertTrue("isNotifierSetterMethod",
                builder.isNotifierSetterMethod(Example.class.getMethod("setXNotifier", Runnable.class)));
    }
}
