package org.autogui.test.base.type;

import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GuiTypeValueTest {

    GuiTypeBuilder builder;
    GuiTypeValue intType;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        intType = (GuiTypeValue) builder.get(int.class);
    }

    @Test
    public void testValueIntValue() {
        Assert.assertEquals("always no-update",
                GuiUpdatedValue.NO_UPDATE, intType.getValue());
    }

    @Test
    public void testValueIntValueInherited() {
        Assert.assertEquals("returns inherited",
                GuiUpdatedValue.of(123),
                intType.getValue(123));
    }

    @Test
    public void testValueIntUpdatedValue() {
        Assert.assertEquals("always no-update",
                GuiUpdatedValue.NO_UPDATE,
                intType.updatedValue(123));
    }

    @Test
    public void testValueIntUpdatedValueInherited() {
        Assert.assertEquals("if different inherited value, returns inherited",
                GuiUpdatedValue.of(456),
                intType.updatedValue(123, 456));
    }

    @Test
    public void testValueIntUpdatedValueInheritedSame() {
        Assert.assertEquals("if prevValue and inherited is same value, returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                intType.updatedValue(123, 123));
    }

    @Test
    public void testValueIntWritable() {
        Assert.assertTrue("always true",
                intType.isWritable(123));
    }

    @Test
    public void testValueIntWrite() {
        Assert.assertEquals("always newValue",
                456,
                intType.writeValue(456));
    }

    @Test
    public void testValueIntWritePrev() {
        Assert.assertEquals("always newValue",
                456,
                intType.writeValue(123, 456));
    }

    @SuppressWarnings("all")
    @Test
    public void testValueIntEqualsSame() {
        Assert.assertTrue("use equals",
                intType.equals(Integer.valueOf(1234567), Integer.valueOf(1234567)));
    }

    @SuppressWarnings("all")
    public void testValueIntEqualsNull() {
        Assert.assertFalse("use equals with null",
                intType.equals(null, Integer.valueOf(1234567)));
    }

}
