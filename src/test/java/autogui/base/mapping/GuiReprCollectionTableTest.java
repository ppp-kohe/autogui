package autogui.base.mapping;

import autogui.GuiIncluded;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import autogui.base.type.GuiUpdatedValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GuiReprCollectionTableTest {
    GuiReprCollectionTable colTable;

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext contextObj;
    GuiMappingContext contextObjListProp;
    GuiMappingContext contextValListProp;
    GuiMappingContext contextObjList;
    GuiMappingContext contextValList;

    GuiMappingContext contextObjElement;
    GuiMappingContext contextValElement;

    GuiMappingContext contextObjChild;
    GuiMappingContext contextValChild;

    GuiMappingContext contextObjChildProp;

    GuiMappingContext contextValListListProp;
    GuiMappingContext contextValListList;
    GuiMappingContext contextValListListElement;
    GuiMappingContext contextValListListChild;
    GuiMappingContext contextValListListChildElement;
    GuiMappingContext contextValListListChildChild;

    TestReprCol obj;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestReprCol.class);

        colTable = new GuiReprCollectionTable(GuiRepresentation.getDefaultSet());

        obj = new TestReprCol() ;
        obj.valueList = new ArrayList<>(Arrays.asList("hello", "world"));
        obj.objList = new ArrayList<>(Arrays.asList(
                new TestReprColObj("hello"),
                new TestReprColObj("world")));
        obj.valueListList = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("hello", "world", "!!!")),
                new ArrayList<>(Arrays.asList("foo", "bar", "buzz")),
                new ArrayList<>(Arrays.asList("aaa", "bbb", "ccc"))));

        contextObj = new GuiMappingContext(typeObject, obj);
        GuiRepresentation.getDefaultSet().match(contextObj);

        contextValListProp = contextObj.getChildByName("valueList");
        contextObjListProp = contextObj.getChildByName("objList");

        contextObjList = contextObjListProp.getChildByName("List");
        contextValList = contextValListProp.getChildByName("List");

        contextObjElement = contextObjList.getReprCollectionTable().getElementContext(contextObjList);
        contextValElement = contextValList.getReprCollectionTable().getElementContext(contextValList);

        contextObjChild = contextObjElement.getChildren().get(0);
        contextValChild = contextValElement.getChildren().get(0);

        contextObjChildProp = contextObjChild.getChildByName("prop");

        //////////

        contextValListListProp = contextObj.getChildByName("valueListList");
        contextValListList = contextValListListProp.getChildByName("List");
        contextValListListElement = contextValListList.getReprCollectionTable().getElementContext(contextValListList);
        contextValListListChild = contextValListListElement.getChildren().get(0);
        contextValListListChildElement = contextValListListChild.getReprCollectionTable().getElementContext(contextValListListChild);
        contextValListListChildChild = contextValListListChildElement.getChildren().get(0);
    }

    @GuiIncluded
    public static class TestReprCol {
        @GuiIncluded
        public List<String> valueList;

        @GuiIncluded
        public List<TestReprColObj> objList;

        @GuiIncluded
        public List<List<String>> valueListList;
    }

    @GuiIncluded
    public static class TestReprColObj {
        @GuiIncluded
        public String prop;

        public TestReprColObj(String prop) {
            this.prop = prop;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestReprColObj that = (TestReprColObj) o;
            return Objects.equals(prop, that.prop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prop);
        }
    }

    //////////////// List<String>

    @Test
    public void testCollectionTableMatch() {
        Assert.assertTrue("collection property becomes property(collection)",
                contextValListProp.getRepresentation() instanceof GuiReprPropertyPane);

        Assert.assertTrue("collection table",
                contextValList.getRepresentation() instanceof GuiReprCollectionTable);

        Assert.assertTrue("collection table element",
                contextValElement.getRepresentation() instanceof GuiReprCollectionElement);

        Assert.assertTrue("collection element always has child",
                contextValChild.getRepresentation() instanceof GuiReprValueStringField);
    }

    @Test
    public void testCollectionTableGetUpdatedValue() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection table returns list",
                GuiUpdatedValue.of(Arrays.asList("hello", "world")),
                contextValList.getReprValue()
                    .getUpdatedValue(contextValList, GuiReprValue.NONE));
    }

    @Test
    public void testCollectionTableSize() throws Throwable {
        Assert.assertEquals("getValueCollectionSize returns list size",
                2,
                contextValList.getReprValue()
                    .getValueCollectionSize(contextValList, GuiMappingContext.GuiSourceValue.of(obj.valueList),
                            GuiReprValue.NONE));
    }

    @Test
    public void testCollectionElementGetUpdatedValue() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection element with index spec returns element",
                GuiUpdatedValue.of("world"),
                contextValElement.getReprValue()
                    .getUpdatedValue(contextValElement, GuiReprValue.NONE.childIndex(1)));
    }

    @Test
    public void testCollectionElementGetUpdatedValueAfterUpdate() throws Throwable {
        contextValElement.getReprValue()
                .updateFromGui(contextValElement, "HELLO", GuiReprValue.NONE.childIndex(1));

        Assert.assertEquals("getUpdatedValue collection element with index spec  returns element after update",
                GuiUpdatedValue.of("hello"),
                contextValElement.getReprValue()
                        .getUpdatedValue(contextValElement, GuiReprValue.NONE.childIndex(0)));
    }

    @Test
    public void testCollectionElementUpdateFromGui() {
        contextValElement.getReprValue()
                .updateFromGui(contextValElement, "HELLO", GuiReprValue.NONE.childIndex(1));
        Assert.assertEquals("updateFromGui collection element with index spec updates an element",
            "HELLO",
                obj.valueList.get(1));
    }

    //////////////// List<TestReprColObj>

    @Test
    public void testCollectionTableMatchObj() {
        Assert.assertTrue("collection property becomes property(collection)",
                contextObjListProp.getRepresentation() instanceof GuiReprPropertyPane);

        Assert.assertTrue("collection table",
                contextObjList.getRepresentation() instanceof GuiReprCollectionTable);

        Assert.assertTrue("collection table element",
                contextObjElement.getRepresentation() instanceof GuiReprCollectionElement);

        Assert.assertTrue("collection element always has child",
                contextObjChild.getRepresentation() instanceof GuiReprObjectPane);

        Assert.assertTrue("column",
                contextObjChildProp.getRepresentation() instanceof GuiReprValueStringField);
    }

    @Test
    public void testCollectionTableGetUpdatedValueObj() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection table returns obj list",
                GuiUpdatedValue.of(Arrays.asList(
                        new TestReprColObj("hello"),
                        new TestReprColObj("world"))),
                contextObjList.getReprValue()
                        .getUpdatedValue(contextObjList, GuiReprValue.NONE));
    }

    @Test
    public void testCollectionElementGetUpdatedValueObj() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection element with index spec returns obj element",
                GuiUpdatedValue.of(new TestReprColObj("world")),
                contextObjElement.getReprValue()
                        .getUpdatedValue(contextObjElement, GuiReprValue.NONE.childIndex(1)));
    }

    @Test
    public void testCollectionElementGetUpdatedValueAfterUpdateObj() throws Throwable {
        contextObjElement.getReprValue()
                .updateFromGui(contextObjElement, new TestReprColObj("HELLO"), GuiReprValue.NONE.childIndex(1));

        Assert.assertEquals("getUpdatedValue collection element with index spec returns obj element after update",
                GuiUpdatedValue.of(new TestReprColObj("hello")),
                contextObjElement.getReprValue()
                        .getUpdatedValue(contextObjElement, GuiReprValue.NONE.childIndex(0)));
    }

    @Test
    public void testCollectionElementPropGetUpdatedValue() throws Throwable {
        Assert.assertEquals("getUpdatedValue prop of collection element with index spec returns prop value",
                GuiUpdatedValue.of("world"),
                contextObjChildProp.getReprValue()
                        .getUpdatedValue(contextObjChildProp, GuiReprValue.NONE.childIndex(1).child(false).child(false)));
    }

    @Test
    public void testCollectionElementPropGetUpdatedValueAfterUpdate() throws Throwable {
        contextObjChildProp.getReprValue()
                .updateFromGui(contextObjChildProp, "HELLO", GuiReprValue.NONE.childIndex(0).child(false).child(false));

        Assert.assertEquals("getUpdatedValue prop of collection element with index spec returns prop value after update",
                GuiUpdatedValue.of("world"),
                contextObjChildProp.getReprValue()
                        .getUpdatedValue(contextObjChildProp, GuiReprValue.NONE.childIndex(1).child(false).child(false)));
    }

    @Test
    public void testCollectionElementPropUpdateFromGuiObj() {
        contextObjChildProp.getReprValue()
                .updateFromGui(contextObjChildProp, "HELLO", GuiReprValue.NONE.childIndex(1).child(false).child(false));
        Assert.assertEquals("updateFromGui collection element prop with index spec updates an element prop",
                "HELLO",
                obj.objList.get(1).prop);
    }


    //////////////// List<List<String>>

    @Test
    public void testCollectionTableMatchListList() {
        Assert.assertTrue("list of list property becomes property(collection(...))",
                contextValListListProp.getRepresentation() instanceof GuiReprPropertyPane);

        Assert.assertTrue("collection table",
                contextValListList.getRepresentation() instanceof GuiReprCollectionTable);

        Assert.assertTrue("collection table element",
                contextValListListElement.getRepresentation() instanceof GuiReprCollectionElement);

        Assert.assertTrue("collection element always has child",
                contextValListListChild.getRepresentation() instanceof GuiReprCollectionTable);

        Assert.assertTrue("collection table in table",
                contextValListListChildElement.getRepresentation() instanceof GuiReprCollectionElement);

        Assert.assertTrue("collection element in table has child",
                contextValListListChildChild.getRepresentation() instanceof  GuiReprValueStringField);
    }


    @Test
    public void testCollectionTableGetUpdatedValueListList() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection table returns list list",
                GuiUpdatedValue.of(Arrays.asList(
                        Arrays.asList("hello", "world", "!!!"),
                        Arrays.asList("foo", "bar", "buzz"),
                        Arrays.asList("aaa", "bbb", "ccc"))),
                contextValListListProp.getReprValue()
                        .getUpdatedValue(contextValListListProp, GuiReprValue.NONE));
    }

    @Test
    public void testCollectionElementGetUpdatedValueListList() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection element with index spec returns list list element",
                GuiUpdatedValue.of(Arrays.asList("foo", "bar", "buzz")),
                contextValListListElement.getReprValue()
                        .getUpdatedValue(contextValListListElement, GuiReprValue.NONE.childIndex(1)));
    }


    @Test
    public void testCollectionElementGetUpdatedValueListListElement() throws Throwable {
        Assert.assertEquals("getUpdatedValue collection element with index2 spec returns list list element",
                GuiUpdatedValue.of("buzz"),
                contextValListListChildElement.getReprValue()
                        .getUpdatedValue(contextValListListChildElement, GuiReprValue.NONE
                                .childIndex(1) //element
                                .child(false)  //table
                                .childIndex(2))); //element in table
    }


    @Test
    public void testCollectionElementGetUpdatedValueListListElementAfterUpdate() throws Throwable {
        contextValListListChildElement.getReprValue().updateFromGui(contextValListListChildElement,
                "HELLO", GuiReprValue.NONE
                .childIndex(1)
                .child(false)
                .childIndex(2));
        Assert.assertEquals("getUpdatedValue collection element with index2 spec returns list list element",
                GuiUpdatedValue.of("HELLO"),
                contextValListListChildElement.getReprValue()
                        .getUpdatedValue(contextValListListChildElement, GuiReprValue.NONE
                                .childIndex(1)
                                .child(false)
                                .childIndex(2)));
    }

    @Test
    public void testCollectionElementUpdateFromGuiListList() throws Throwable {
        contextValListListChildElement.getReprValue().updateFromGui(contextValListListChildElement,
                "HELLO", GuiReprValue.NONE
                        .childIndex(1)
                        .child(false)
                        .childIndex(2));
        Assert.assertEquals("updateFromGui collection table updates element in list of list",
                Arrays.asList(
                        Arrays.asList("hello", "world", "!!!"),
                        Arrays.asList("foo", "bar", "HELLO"),
                        Arrays.asList("aaa", "bbb", "ccc")),
                obj.valueListList);
    }
}
