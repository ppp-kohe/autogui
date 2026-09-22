package org.autogui.base.type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GuiTypeCollectionArrayTest {
    GuiTypeCollectionArray array;
    GuiTypeCollectionArray arrayObj;

    public static class TestObj {

    }

    @Before
    public void setUp() {
        array = new GuiTypeCollectionArray(int[].class);
        arrayObj = new GuiTypeCollectionArray(TestObj[].class, new GuiTypeValue(TestObj.class));
    }
    @Test
    public void testConstructor() {
        Assert.assertEquals("int[]class", int[].class, array.getType());
        Assert.assertEquals("name of int[].class", "[I", array.getName());

        Assert.assertEquals("element", new GuiTypeValue(TestObj.class), arrayObj.getElementType());
    }

    @Test
    public void testExecuteGetElement() {
        Assert.assertEquals("executeGetElement", GuiUpdatedValue.of(2), array.executeGetElement(new int[] {1,2,3}, 1));

        Assert.assertEquals("executeGetElement null", GuiUpdatedValue.NO_UPDATE, array.executeGetElement(null, 1));
    }

    @Test
    public void testExecuteAddElement() {
        try {
            array.executeAddElements(new int[]{1, 2, 3}, List.of(1,2,3));
            Assert.fail("unsupport executeAddElements");
        } catch (UnsupportedOperationException u) {
            //ok
        }
    }

    @Test
    public void testExecuteSetElement() {
        var a = new int[] {1, 2, 3};
        var nv = array.executeSetElement(a, 1, 123);
        Assert.assertEquals("executeSetElement returns newValue", 123, nv);
        Assert.assertEquals("executeSetElement", Arrays.toString(new int[] {1, 123, 3}), Arrays.toString(a));

        Assert.assertNull("executeSetElement null", array.executeSetElement(null, 1, 2));
    }

    @Test
    public void testGetSize() {
        Assert.assertEquals("getSize", 3, array.getSize(new int[] {1, 2, 3}));
        Assert.assertEquals("getSize null", 0, array.getSize(null));
    }

}
