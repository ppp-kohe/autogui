package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.*;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    GuiTaskClock viewClock;

    public GuiReprCollectionTableTest() {
    }

    @BeforeEach
    public void setUp() {
        viewClock = new GuiTaskClock(true);
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestReprCol.class);

        colTable = new GuiReprCollectionTable(GuiRepresentation.getDefaultSet());

        obj = new TestReprCol();
        obj.valueList = new ArrayList<>(Arrays.asList("hello", "world"));
        obj.objList = new ArrayList<>(Arrays.asList(
                new TestReprColObj("hello"),
                new TestReprColObj("world")));
        obj.valueListList = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("hello", "world", "!!!")),
                new ArrayList<>(Arrays.asList("foo", "bar", "buzz")),
                new ArrayList<>(Arrays.asList("aaa", "bbb", "ccc"))));

        contextObj = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typeObject, obj);
        GuiRepresentation.getDefaultSet().match(contextObj);

        contextValListProp = contextObj.getChildByName("valueList");
        contextObjListProp = contextObj.getChildByName("objList");

        contextObjList = contextObjListProp.getChildByName("List");
        contextValList = contextValListProp.getChildByName("List");

        contextObjElement = contextObjList.getReprCollectionTable().getElementContext(contextObjList);
        contextValElement = contextValList.getReprCollectionTable().getElementContext(contextValList);

        contextObjChild = contextObjElement.getChildren().getFirst();
        contextValChild = contextValElement.getChildren().getFirst();

        contextObjChildProp = contextObjChild.getChildByName("prop");

        //------------------

        contextValListListProp = contextObj.getChildByName("valueListList");
        contextValListList = contextValListListProp.getChildByName("List");
        contextValListListElement = contextValListList.getReprCollectionTable().getElementContext(contextValListList);
        contextValListListChild = contextValListListElement.getChildren().getFirst();
        contextValListListChildElement = contextValListListChild.getReprCollectionTable().getElementContext(contextValListListChild);
        contextValListListChildChild = contextValListListChildElement.getChildren().getFirst();
    }

    @GuiIncluded
    public static class TestReprCol {
        @GuiIncluded
        public List<String> valueList;

        @GuiIncluded
        public List<TestReprColObj> objList;

        @GuiIncluded
        public List<List<String>> valueListList;

        public TestReprCol() {
        }
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

    //------------------ List<String>

    @Test
    public void testCollectionTableMatch() {
        assertTrue(contextValListProp.getRepresentation() instanceof GuiReprPropertyPane,
                "collection property becomes property(collection)");

        assertTrue(contextValList.getRepresentation() instanceof GuiReprCollectionTable,
                "collection table");

        assertTrue(contextValElement.getRepresentation() instanceof GuiReprCollectionElement,
                "collection table element");

        assertTrue(contextValChild.getRepresentation() instanceof GuiReprValueStringField,
                "collection element always has child");
    }

    @Test
    public void testCollectionTableGetUpdatedValue() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of(Arrays.asList("hello", "world")),
                contextValList.getReprValue()
                        .getUpdatedValue(contextValList, GuiReprValue.NONE),
                "getUpdatedValue collection table returns list");
    }

    @Test
    public void testCollectionTableSize() throws Throwable {
        assertEquals(
                2,
                contextValList.getReprValue()
                        .getValueCollectionSize(contextValList, GuiMappingContext.GuiSourceValue.of(obj.valueList),
                                GuiReprValue.NONE),
                "getValueCollectionSize returns list size");
    }

    @Test
    public void testCollectionElementGetUpdatedValue() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("world"),
                contextValElement.getReprValue()
                        .getUpdatedValue(contextValElement, GuiReprValue.NONE.childIndex(1)),
                "getUpdatedValue collection element with index spec returns element");
    }

    @Test
    public void testCollectionElementGetUpdatedValueAfterUpdate() throws Throwable {
        contextValElement.getReprValue()
                .updateFromGui(contextValElement, "HELLO", GuiReprValue.NONE.childIndex(1), viewClock.increment().copy());

        assertEquals(
                GuiUpdatedValue.of("hello"),
                contextValElement.getReprValue()
                        .getUpdatedValue(contextValElement, GuiReprValue.NONE.childIndex(0)),
                "getUpdatedValue collection element with index spec  returns element after update");
    }

    @Test
    public void testCollectionElementUpdateFromGui() {
        contextValElement.getReprValue()
                .updateFromGui(contextValElement, "HELLO", GuiReprValue.NONE.childIndex(1), viewClock.increment().copy());
        assertEquals(
                "HELLO",
                obj.valueList.get(1),
                "updateFromGui collection element with index spec updates an element");
    }

    //------------------ List<TestReprColObj>

    @Test
    public void testCollectionTableMatchObj() {
        assertTrue(
                contextObjListProp.getRepresentation() instanceof GuiReprPropertyPane,
                "collection property becomes property(collection)");

        assertTrue(
                contextObjList.getRepresentation() instanceof GuiReprCollectionTable,
                "collection table");

        assertTrue(
                contextObjElement.getRepresentation() instanceof GuiReprCollectionElement,
                "collection table element");

        assertTrue(
                contextObjChild.getRepresentation() instanceof GuiReprObjectPane,
                "collection element always has child");

        assertTrue(
                contextObjChildProp.getRepresentation() instanceof GuiReprValueStringField,
                "column");
    }

    @Test
    public void testCollectionTableGetUpdatedValueObj() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of(Arrays.asList(
                        new TestReprColObj("hello"),
                        new TestReprColObj("world"))),
                contextObjList.getReprValue()
                        .getUpdatedValue(contextObjList, GuiReprValue.NONE),
                "getUpdatedValue collection table returns obj list");
    }

    @Test
    public void testCollectionElementGetUpdatedValueObj() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of(new TestReprColObj("world")),
                contextObjElement.getReprValue()
                        .getUpdatedValue(contextObjElement, GuiReprValue.NONE.childIndex(1)),
                "getUpdatedValue collection element with index spec returns obj element");
    }

    @Test
    public void testCollectionElementGetUpdatedValueAfterUpdateObj() throws Throwable {
        contextObjElement.getReprValue()
                .updateFromGui(contextObjElement, new TestReprColObj("HELLO"), GuiReprValue.NONE.childIndex(1), viewClock.increment().copy());

        assertEquals(
                GuiUpdatedValue.of(new TestReprColObj("hello")),
                contextObjElement.getReprValue()
                        .getUpdatedValue(contextObjElement, GuiReprValue.NONE.childIndex(0)),
                "getUpdatedValue collection element with index spec returns obj element after update");
    }

    @Test
    public void testCollectionElementPropGetUpdatedValue() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("world"),
                contextObjChildProp.getReprValue()
                        .getUpdatedValue(contextObjChildProp, GuiReprValue.NONE.childIndex(1).child(false).child(false)),
                "getUpdatedValue prop of collection element with index spec returns prop value");
    }

    @Test
    public void testCollectionElementPropGetUpdatedValueAfterUpdate() throws Throwable {
        contextObjChildProp.getReprValue()
                .updateFromGui(contextObjChildProp, "HELLO", GuiReprValue.NONE.childIndex(0).child(false).child(false), viewClock.increment().copy());

        assertEquals(
                GuiUpdatedValue.of("world"),
                contextObjChildProp.getReprValue()
                        .getUpdatedValue(contextObjChildProp, GuiReprValue.NONE.childIndex(1).child(false).child(false)),
                "getUpdatedValue prop of collection element with index spec returns prop value after update");
    }

    @Test
    public void testCollectionElementPropUpdateFromGuiObj() {
        contextObjChildProp.getReprValue()
                .updateFromGui(contextObjChildProp, "HELLO", GuiReprValue.NONE.childIndex(1).child(false).child(false), viewClock.increment().copy());
        assertEquals(
                "HELLO",
                obj.objList.get(1).prop,
                "updateFromGui collection element prop with index spec updates an element prop");
    }


    //------------------ List<List<String>>

    @Test
    public void testCollectionTableMatchListList() {
        assertTrue(contextValListListProp.getRepresentation() instanceof GuiReprPropertyPane,
                "list of list property becomes property(collection(...))");

        assertTrue(contextValListList.getRepresentation() instanceof GuiReprCollectionTable,
                "collection table");

        assertTrue(contextValListListElement.getRepresentation() instanceof GuiReprCollectionElement,
                "collection table element");

        assertTrue(contextValListListChild.getRepresentation() instanceof GuiReprCollectionTable,
                "collection element always has child");

        assertTrue(contextValListListChildElement.getRepresentation() instanceof GuiReprCollectionElement,
                "collection table in table");

        assertTrue(contextValListListChildChild.getRepresentation() instanceof GuiReprValueStringField,
                "collection element in table has child");
    }


    @Test
    public void testCollectionTableGetUpdatedValueListList() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of(Arrays.asList(
                        Arrays.asList("hello", "world", "!!!"),
                        Arrays.asList("foo", "bar", "buzz"),
                        Arrays.asList("aaa", "bbb", "ccc"))),
                contextValListListProp.getReprValue()
                        .getUpdatedValue(contextValListListProp, GuiReprValue.NONE),
                "getUpdatedValue collection table returns list list");
    }

    @Test
    public void testCollectionElementGetUpdatedValueListList() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of(Arrays.asList("foo", "bar", "buzz")),
                contextValListListElement.getReprValue()
                        .getUpdatedValue(contextValListListElement, GuiReprValue.NONE.childIndex(1)),
                "getUpdatedValue collection element with index spec returns list list element");
    }


    @Test
    public void testCollectionElementGetUpdatedValueListListElement() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("buzz"),
                contextValListListChildElement.getReprValue()
                        .getUpdatedValue(contextValListListChildElement, GuiReprValue.NONE
                                .childIndex(1) //element
                                .child(false)  //table
                                .childIndex(2)),//element in table
                "getUpdatedValue collection element with index2 spec returns list list element");
    }


    @Test
    public void testCollectionElementGetUpdatedValueListListElementAfterUpdate() throws Throwable {
        contextValListListChildElement.getReprValue().updateFromGui(contextValListListChildElement,
                "HELLO", GuiReprValue.NONE
                        .childIndex(1)
                        .child(false)
                        .childIndex(2), viewClock.increment().copy());
        assertEquals(
                GuiUpdatedValue.of("HELLO"),
                contextValListListChildElement.getReprValue()
                        .getUpdatedValue(contextValListListChildElement, GuiReprValue.NONE
                                .childIndex(1)
                                .child(false)
                                .childIndex(2)),
                "getUpdatedValue collection element with index2 spec returns list list element");
    }

    @Test
    public void testCollectionElementUpdateFromGuiListList() {
        contextValListListChildElement.getReprValue().updateFromGui(contextValListListChildElement,
                "HELLO", GuiReprValue.NONE
                        .childIndex(1)
                        .child(false)
                        .childIndex(2), viewClock.increment().copy());
        assertEquals(
                Arrays.asList(
                        Arrays.asList("hello", "world", "!!!"),
                        Arrays.asList("foo", "bar", "HELLO"),
                        Arrays.asList("aaa", "bbb", "ccc")),
                obj.valueListList,
                "updateFromGui collection table updates element in list of list");
    }
}
