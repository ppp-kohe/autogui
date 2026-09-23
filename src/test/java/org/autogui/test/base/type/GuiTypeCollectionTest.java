package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.base.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GuiTypeCollectionTest {

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    GuiTypeMemberProperty property;
    GuiTypeCollection typeCollection;
    TestObjCol obj;

    public GuiTypeCollectionTest() {
    }

    @BeforeEach
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

        public TestObjCol() {
        }
    }

    @Test
    public void testCollectionElementType() {
        assertEquals(builder.get(Integer.class), typeCollection.getElementType(), "collection element type is the param type");
    }

    @Test
    public void testCollectionChildren() {
        assertEquals(Collections.singletonList(builder.get(Integer.class)), typeCollection.getChildren(), "collection children is its element type");
    }

    @Test
    public void testCollectionEqualsDiff() {
        assertFalse(typeCollection.equals(new ArrayList<>(Arrays.asList(123, 456)), new ArrayList<>(Arrays.asList(123, 456))), "collection equals only checks identity");
    }

    @Test
    public void testCollectionEqualsSame() {
        List<Integer> is = new ArrayList<>(Arrays.asList(123, 456));
        assertTrue(typeCollection.equals(is, is), "collection equals only checks identity");
    }

    @Test
    public void testCollectionExecuteGetElement() {
        assertEquals(GuiUpdatedValue.of(456), typeCollection.executeGetElement(obj.col, 1), "collection executeGetElement returns an element");
    }

    @Test
    public void testCollectionExecuteGetElementNull() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, typeCollection.executeGetElement(null, 1), "collection executeGetElement with null returns no-update");
    }

    @Test
    public void testCollectionExecuteGetElementNullPrev() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, typeCollection.executeGetElement(null, 1, obj.col), "collection executeGetElement with null returns no-update");
    }


    @Test
    public void testCollectionExecuteGetElementPrevSame() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, typeCollection.executeGetElement(obj.col, 1, 456), "collection executeGetElement with same prevValue returns no-update");
    }

    @Test
    public void testCollectionExecuteGetElementPrevDiff() {
        assertEquals(GuiUpdatedValue.of(456), typeCollection.executeGetElement(obj.col, 1, 100), "collection executeGetElement with diff prevValue returns an element");
    }

    @Test
    public void testCollectionExecuteSetElement() {
        assertEquals(10203, typeCollection.executeSetElement(obj.col, 1, 10203), "collection executeSetElement returns newValue");
        assertEquals(10203, obj.col.get(1).longValue(), "collection executeSetElement sets an element");
    }

    @Test
    public void testCollectionExecuteSetElementNull() {
        assertNull(typeCollection.executeSetElement(null, 1, 10203), "collection executeSetElement with null returns null");
        assertEquals(456, obj.col.get(1).longValue(), "collection executeSetElement with null no effect");
    }

    @Test
    public void testCollectionGetChildren() {
        assertEquals(1, typeCollection.getChildren().size(), "collection children");
        assertEquals(new GuiTypeValue(Integer.class), typeCollection.getChildren().getFirst(), "collection element integer");
    }

    @Test
    public void testCollectionGetChildrenEmpty() {
        var col = new GuiTypeCollection(List.class);
        assertEquals(0, col.getChildren().size(), "collection children is empty");
    }

    @Test
    public void testCollectionEquals() {
        assertEquals(new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)), new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)), "collection equals");
        assertEquals(typeCollection, typeCollection, "collection equals identity");
    }

    @Test
    public void testCollectionCompareGetElement() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, typeCollection.compareGetElement(List.of(1, 2), List.of(1, 2)), "collection compareGetElement");
    }

    @Test
    public void testCollectionExecuteAddElements() {
        var a = new ArrayList<Object>(List.of(1, 2));
        assertEquals(List.of(100, 200), typeCollection.executeAddElements(a, List.of(100, 200)), "add elements returns added list");

        assertNull(typeCollection.executeAddElements(null, List.of(100, 200)), "null add elements returns null");
    }

    @Test
    public void testCollectionGetSize() {
        assertEquals(3, typeCollection.getSize(List.of(1, 2, 3)), "size");
        assertEquals(0, typeCollection.getSize(null), "size null 0");
    }
}
