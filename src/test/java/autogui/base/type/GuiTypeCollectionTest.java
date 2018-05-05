package autogui.base.type;

import autogui.GuiIncluded;
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


}
