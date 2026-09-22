package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuiTypeCollectionTest {

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    GuiTypeMemberProperty property;
    GuiTypeCollection typeCollection;
    TestObjCol obj;

    public GuiTypeCollectionTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObjCol.class);

        property = (GuiTypeMemberProperty) typeObject.getMemberByName("col");
        typeCollection = (GuiTypeCollection) property.getType();

        obj = new TestObjCol();
        obj.col = new ArrayList<>(Arrays.asList(123, 456));
    }

    @GuiIncluded
    public static class TestObjCol {
        @GuiIncluded
        public List<Integer> col;

        public TestObjCol() {}
    }

    @Test
    public void testCollectionElementType() {
        Assert.assertEquals("collection element type is the param type",
                builder.get(Integer.class),
                typeCollection.getElementType());
    }

    @Test
    public void testCollectionChildren() {
        Assert.assertEquals("collection children is its element type",
                Collections.singletonList(builder.get(Integer.class)),
                typeCollection.getChildren());
    }

    @Test
    public void testCollectionEqualsDiff() {
        Assert.assertFalse("collection equals only checks identity",
                typeCollection.equals(
                        new ArrayList<>(Arrays.asList(123, 456)),
                        new ArrayList<>(Arrays.asList(123, 456))));
    }

    @Test
    public void testCollectionEqualsSame() {
        List<Integer> is = new ArrayList<>(Arrays.asList(123, 456));
        Assert.assertTrue("collection equals only checks identity",
                typeCollection.equals(
                        is, is));
    }

    @Test
    public void testCollectionExecuteGetElement() {
        Assert.assertEquals("collection executeGetElement returns an element",
                GuiUpdatedValue.of(456),
                typeCollection.executeGetElement(obj.col, 1));
    }

    @Test
    public void testCollectionExecuteGetElementNull() {
        Assert.assertEquals("collection executeGetElement with null returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                typeCollection.executeGetElement(null, 1));
    }
    @Test
    public void testCollectionExecuteGetElementNullPrev() {
        Assert.assertEquals("collection executeGetElement with null returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                typeCollection.executeGetElement(null, 1, obj.col));
    }


    @Test
    public void testCollectionExecuteGetElementPrevSame() {
        Assert.assertEquals("collection executeGetElement with same prevValue returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                typeCollection.executeGetElement(obj.col, 1, 456));
    }

    @Test
    public void testCollectionExecuteGetElementPrevDiff() {
        Assert.assertEquals("collection executeGetElement with diff prevValue returns an element",
                GuiUpdatedValue.of(456),
                typeCollection.executeGetElement(obj.col, 1, 100));
    }

    @Test
    public void testCollectionExecuteSetElement() {
        Assert.assertEquals("collection executeSetElement returns newValue",
                10203,
                typeCollection.executeSetElement(obj.col, 1, 10203));
        Assert.assertEquals("collection executeSetElement sets an element",
                10203,
                obj.col.get(1).longValue());
    }

    @Test
    public void testCollectionExecuteSetElementNull() {
        Assert.assertNull("collection executeSetElement with null returns null",
                typeCollection.executeSetElement(null, 1, 10203));
        Assert.assertEquals("collection executeSetElement with null no effect",
                456,
                obj.col.get(1).longValue());
    }

    @Test
    public void testCollectionGetChildren() {
        Assert.assertEquals("collection children", 1, typeCollection.getChildren().size());
        Assert.assertEquals("collection element integer", new GuiTypeValue(Integer.class), typeCollection.getChildren().getFirst());
    }

    @Test
    public void testCollectionGetChildrenEmpty() {
        var col = new GuiTypeCollection(List.class);
        Assert.assertEquals("collection children is empty", 0, col.getChildren().size());
    }

    @Test
    public void testCollectionEquals() {
        Assert.assertEquals("collection equals", new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)), new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)));
        Assert.assertEquals("collection equals identity", typeCollection, typeCollection);
    }

    @Test
    public void testCollectionCompareGetElement() {
        Assert.assertEquals("collection compareGetElement",
                GuiUpdatedValue.NO_UPDATE,
                typeCollection.compareGetElement(List.of(1, 2), List.of(1, 2)));
    }

    @Test
    public void testCollectionExecuteAddElements() {
        var a = new ArrayList<Object>(List.of(1,2));
        Assert.assertEquals("add elements returns added list",
                List.of(100, 200),
                typeCollection.executeAddElements(a, List.of(100, 200)));

        Assert.assertNull("null add elements returns null",
                typeCollection.executeAddElements(null, List.of(100, 200)));
    }

    @Test
    public void testCollectionGetSize() {
        Assert.assertEquals("size", 3, typeCollection.getSize(List.of(1,2,3)));
        Assert.assertEquals("size null 0", 0, typeCollection.getSize(null));
    }
}
