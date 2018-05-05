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
    }

    @GuiIncluded
    public static class TestReprCol {
        @GuiIncluded
        public List<String> valueList;

        @GuiIncluded
        public List<TestReprColObj> objList;
    }

    @GuiIncluded
    public static class TestReprColObj {
        @GuiIncluded
        public String prop;

        public TestReprColObj(String prop) {
            this.prop = prop;
        }
    }

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
}
