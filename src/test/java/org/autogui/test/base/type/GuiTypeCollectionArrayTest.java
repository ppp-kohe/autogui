package org.autogui.test.base.type;

import org.autogui.base.type.GuiTypeCollectionArray;
import org.autogui.base.type.GuiTypeValue;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GuiTypeCollectionArrayTest {
    GuiTypeCollectionArray array;
    GuiTypeCollectionArray arrayObj;

    public static class TestObj {

    }

    @BeforeEach
    public void setUp() {
        array = new GuiTypeCollectionArray(int[].class);
        arrayObj = new GuiTypeCollectionArray(TestObj[].class, new GuiTypeValue(TestObj.class));
    }

    @Test
    public void testConstructor() {
        assertEquals(int[].class, array.getType(), "int[]class");
        assertEquals("[I", array.getName(), "name of int[].class");

        assertEquals(new GuiTypeValue(TestObj.class), arrayObj.getElementType(), "element");
    }

    @Test
    public void testExecuteGetElement() {
        assertEquals(GuiUpdatedValue.of(2), array.executeGetElement(new int[]{1, 2, 3}, 1), "executeGetElement");

        assertEquals(GuiUpdatedValue.NO_UPDATE, array.executeGetElement(null, 1), "executeGetElement null");
    }

    @Test
    public void testExecuteAddElement() {
        try {
            array.executeAddElements(new int[]{1, 2, 3}, List.of(1, 2, 3));
            fail("unsupport executeAddElements");
        } catch (UnsupportedOperationException u) {
            //ok
        }
    }

    @Test
    public void testExecuteSetElement() {
        var a = new int[]{1, 2, 3};
        var nv = array.executeSetElement(a, 1, 123);
        assertEquals(123, nv, "executeSetElement returns newValue");
        assertEquals(Arrays.toString(new int[]{1, 123, 3}), Arrays.toString(a), "executeSetElement");

        assertNull(array.executeSetElement(null, 1, 2), "executeSetElement null");
    }

    @Test
    public void testGetSize() {
        assertEquals(3, array.getSize(new int[]{1, 2, 3}), "getSize");
        assertEquals(0, array.getSize(null), "getSize null");
    }

}
