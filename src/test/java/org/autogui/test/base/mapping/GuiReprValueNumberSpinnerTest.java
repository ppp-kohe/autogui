package org.autogui.test.base.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValueNumberSpinner;
import org.autogui.base.type.GuiTypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprValueNumberSpinnerTest {
    GuiReprValueNumberSpinner spinner;

    GuiTypeBuilder builder;

    public GuiReprValueNumberSpinnerTest() {
    }


    @BeforeEach
    public void setUp() {
        spinner = new GuiReprValueNumberSpinner();
        builder = new GuiTypeBuilder();
    }

    @Test
    public void testValueGetType() {
        assertEquals(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.getType(int.class), "getType int");

        assertEquals(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.getType(Integer.class), "getType Integer");

        assertEquals(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.getType(long.class), "getType long");

        assertEquals(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.getType(Long.class), "getType Long");

        assertEquals(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.getType(short.class), "getType short");

        assertEquals(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.getType(Short.class), "getType Short");

        assertEquals(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.getType(byte.class), "getType byte");

        assertEquals(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.getType(Byte.class), "getType Byte");

        assertEquals(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.getType(float.class), "getType float");

        assertEquals(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.getType(Float.class), "getType Float");

        assertEquals(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.getType(double.class), "getType double");

        assertEquals(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.getType(Double.class), "getType Double");

        assertEquals(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.getType(BigInteger.class), "getType BigInteger");

        assertEquals(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.getType(BigDecimal.class), "getType BigDecimal");
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(builder.get(int.class));
        assertTrue(spinner.match(ctx), "match with typed context");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match sets a new repr");

        GuiReprValueNumberSpinner matchedSpinner = (GuiReprValueNumberSpinner) ctx.getRepresentation();
        assertEquals(GuiReprValueNumberSpinner.INT, matchedSpinner.getType((GuiMappingContext) null), "match sets repr type");

        assertEquals(GuiReprValueNumberSpinner.INT.getFormat().format(1234), matchedSpinner.getFormat().format(1234), "match sets format from repr type");
    }

    @Test
    public void testValueMatchTypes() {
        GuiMappingContext ctx = new GuiMappingContext(builder.get(float.class));
        assertTrue(spinner.match(ctx), "match float");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match float sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.FLOAT, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match float");
        assertTrue(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(double.class));
        assertTrue(spinner.match(ctx), "match double");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match double sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.DOUBLE, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match double");
        assertTrue(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(long.class));
        assertTrue(spinner.match(ctx), "match long");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match long sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.LONG, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match long");
        assertFalse(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(short.class));
        assertTrue(spinner.match(ctx), "match short");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match short sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.SHORT, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match short");
        assertFalse(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(byte.class));
        assertTrue(spinner.match(ctx), "match byte");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match byte sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.BYTE, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match byte");
        assertFalse(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(BigInteger.class));
        assertTrue(spinner.match(ctx), "match BigInteger");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match BigInteger sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.BIG_INTEGER, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match BigInteger");
        assertFalse(spinner.isRealNumberType(ctx), "real number type");

        ctx = new GuiMappingContext(builder.get(BigDecimal.class));
        assertTrue(spinner.match(ctx), "match BigDecimal");
        assertTrue(ctx.getRepresentation() instanceof GuiReprValueNumberSpinner, "match BigDecimal sets a new repr");
        assertEquals(GuiReprValueNumberSpinner.BIG_DECIMAL, ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null), "match BigDecimal");
        assertTrue(spinner.isRealNumberType(ctx), "real number type");
    }

    @Test
    public void testValueNumberTypeInt() {
        assertEquals(Integer.class, GuiReprValueNumberSpinner.INT.getNumberClass(), "Int type");

        assertEquals(Integer.MAX_VALUE, GuiReprValueNumberSpinner.INT.getMaximum(), "Int max");

        assertEquals(Integer.MIN_VALUE, GuiReprValueNumberSpinner.INT.getMinimum(), "Int min");

        assertEquals(1, GuiReprValueNumberSpinner.INT.getOne(), "Int 1");

        assertEquals(0, GuiReprValueNumberSpinner.INT.getZero(), "Int 0");

        assertEquals(3, GuiReprValueNumberSpinner.INT.next(1, 2, +1), "Int next");
        assertEquals(-1, GuiReprValueNumberSpinner.INT.next(1, 2, -1), "Int next -1");

        assertEquals(3, GuiReprValueNumberSpinner.INT.convert(3), "Int convert returns as is");

        assertEquals("12345", GuiReprValueNumberSpinner.INT.toString(12345), "Int toString");

        assertEquals(12345, GuiReprValueNumberSpinner.INT.fromString("00012345"), "Int fromString");

        assertEquals("12,345", GuiReprValueNumberSpinner.INT.format(GuiReprValueNumberSpinner.INT.getFormat(), 12345), "Int format");

        assertEquals(12345, GuiReprValueNumberSpinner.INT.parse(GuiReprValueNumberSpinner.INT.getFormat(), "12,345"), "Int parse");


        assertEquals("Infinity", GuiReprValueNumberSpinner.INT.format(GuiReprValueNumberSpinner.INT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Int format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.INT.format(GuiReprValueNumberSpinner.INT.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Int format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.INT.parse(GuiReprValueNumberSpinner.INT.getFormat(), "Infinity"), "Int parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.INT.parse(GuiReprValueNumberSpinner.INT.getFormat(), "-Infinity"), "Int format");

    }

    @Test
    public void testValueNumberTypeByte() {
        assertEquals(Byte.class, GuiReprValueNumberSpinner.BYTE.getNumberClass(), "Byte type");

        assertEquals(Byte.MAX_VALUE, GuiReprValueNumberSpinner.BYTE.getMaximum(), "Byte max");

        assertEquals(Byte.MIN_VALUE, GuiReprValueNumberSpinner.BYTE.getMinimum(), "Byte min");

        assertEquals((byte) 1, GuiReprValueNumberSpinner.BYTE.getOne(), "Byte 1");

        assertEquals((byte) 0, GuiReprValueNumberSpinner.BYTE.getZero(), "Byte 0");

        assertEquals((byte) 3, GuiReprValueNumberSpinner.BYTE.next(1, 2, +1), "Byte next");
        assertEquals((byte) -1, GuiReprValueNumberSpinner.BYTE.next(1, 2, -1), "Byte next -1");

        assertEquals((byte) 3, GuiReprValueNumberSpinner.BYTE.convert(3), "Byte convert returns as is");

        assertEquals("123", GuiReprValueNumberSpinner.BYTE.toString(123), "Byte toString");

        assertEquals((byte) 123, GuiReprValueNumberSpinner.BYTE.fromString("00123"), "Byte fromString");

        assertEquals("123", GuiReprValueNumberSpinner.BYTE.format(GuiReprValueNumberSpinner.BYTE.getFormat(), 123), "Byte format");

        assertEquals((byte) 123, GuiReprValueNumberSpinner.BYTE.parse(GuiReprValueNumberSpinner.BYTE.getFormat(), "123"), "Byte parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.BYTE.format(GuiReprValueNumberSpinner.BYTE.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Byte format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.BYTE.format(GuiReprValueNumberSpinner.BYTE.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Byte format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.BYTE.parse(GuiReprValueNumberSpinner.BYTE.getFormat(), "Infinity"), "Byte parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.BYTE.parse(GuiReprValueNumberSpinner.BYTE.getFormat(), "-Infinity"), "Byte format");

    }

    @Test
    public void testValueNumberTypeShort() {
        assertEquals(Short.class, GuiReprValueNumberSpinner.SHORT.getNumberClass(), "Short type");

        assertEquals(Short.MAX_VALUE, GuiReprValueNumberSpinner.SHORT.getMaximum(), "Short max");

        assertEquals(Short.MIN_VALUE, GuiReprValueNumberSpinner.SHORT.getMinimum(), "Short min");

        assertEquals((short) 1, GuiReprValueNumberSpinner.SHORT.getOne(), "Short 1");

        assertEquals((short) 0, GuiReprValueNumberSpinner.SHORT.getZero(), "Short 0");

        assertEquals((short) 3, GuiReprValueNumberSpinner.SHORT.next(1, 2, +1), "Short next");
        assertEquals((short) -1, GuiReprValueNumberSpinner.SHORT.next(1, 2, -1), "Short next -1");

        assertEquals((short) 3, GuiReprValueNumberSpinner.SHORT.convert(3), "Short convert returns as is");

        assertEquals("1234", GuiReprValueNumberSpinner.SHORT.toString(1234), "Short toString");

        assertEquals((short) 1234, GuiReprValueNumberSpinner.SHORT.fromString("001234"), "Short fromString");

        assertEquals("1,234", GuiReprValueNumberSpinner.SHORT.format(GuiReprValueNumberSpinner.SHORT.getFormat(), 1234), "Short format");

        assertEquals((short) 1234, GuiReprValueNumberSpinner.SHORT.parse(GuiReprValueNumberSpinner.SHORT.getFormat(), "1,234"), "Short parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.SHORT.format(GuiReprValueNumberSpinner.SHORT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Short format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.SHORT.format(GuiReprValueNumberSpinner.SHORT.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Short format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.SHORT.parse(GuiReprValueNumberSpinner.SHORT.getFormat(), "Infinity"), "Short parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.SHORT.parse(GuiReprValueNumberSpinner.SHORT.getFormat(), "-Infinity"), "Short format");
    }

    @Test
    public void testValueNumberTypeLong() {
        assertEquals(Long.class, GuiReprValueNumberSpinner.LONG.getNumberClass(), "Long type");

        assertEquals(Long.MAX_VALUE, GuiReprValueNumberSpinner.LONG.getMaximum(), "Long max");

        assertEquals(Long.MIN_VALUE, GuiReprValueNumberSpinner.LONG.getMinimum(), "Long min");

        assertEquals((long) 1, GuiReprValueNumberSpinner.LONG.getOne(), "Long 1");

        assertEquals((long) 0, GuiReprValueNumberSpinner.LONG.getZero(), "Long 0");

        assertEquals((long) 3, GuiReprValueNumberSpinner.LONG.next(1, 2, +1), "Long next");
        assertEquals((long) -1, GuiReprValueNumberSpinner.LONG.next(1, 2, -1), "Long next -1");

        assertEquals((long) 3, GuiReprValueNumberSpinner.LONG.convert(3), "Long convert returns as is");

        assertEquals("1234567890123", GuiReprValueNumberSpinner.LONG.toString(1234567890123L), "Long toString");

        assertEquals(1234567890123L, GuiReprValueNumberSpinner.LONG.fromString("001234567890123"), "Long fromString");

        assertEquals("1,234,567,890,123", GuiReprValueNumberSpinner.LONG.format(GuiReprValueNumberSpinner.LONG.getFormat(), 1234567890123L), "Long format");

        assertEquals(1234567890123L, GuiReprValueNumberSpinner.LONG.parse(GuiReprValueNumberSpinner.LONG.getFormat(), "1,234,567,890,123"), "Long parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.LONG.format(GuiReprValueNumberSpinner.LONG.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Long format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.LONG.format(GuiReprValueNumberSpinner.LONG.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Long format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.LONG.parse(GuiReprValueNumberSpinner.LONG.getFormat(), "Infinity"), "Long parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.LONG.parse(GuiReprValueNumberSpinner.LONG.getFormat(), "-Infinity"), "Long format");

    }

    @Test
    public void testValueNumberTypeFloat() {
        assertEquals(Float.class, GuiReprValueNumberSpinner.FLOAT.getNumberClass(), "Float type");

        assertEquals(Float.MAX_VALUE, GuiReprValueNumberSpinner.FLOAT.getMaximum(), "Float max");

        assertEquals(-Float.MAX_VALUE, (Float) GuiReprValueNumberSpinner.FLOAT.getMinimum(), 0.00001f, "Float min : -MAX");

        assertEquals((float) 1, GuiReprValueNumberSpinner.FLOAT.getOne(), "Float 1");

        assertEquals((float) 0, GuiReprValueNumberSpinner.FLOAT.getZero(), "Float 0");

        assertEquals((float) 3, GuiReprValueNumberSpinner.FLOAT.next(1, 2, +1), "Float next");
        assertEquals((float) -1, GuiReprValueNumberSpinner.FLOAT.next(1, 2, -1), "Float next -1");

        assertEquals((float) 3, GuiReprValueNumberSpinner.FLOAT.convert(3), "Float convert returns as is");

        assertEquals("1234", GuiReprValueNumberSpinner.FLOAT.toString(1234), "Float toString");

        assertEquals((float) 1234, GuiReprValueNumberSpinner.FLOAT.fromString("001234"), "Float fromString");

        assertEquals("1,234", GuiReprValueNumberSpinner.FLOAT.format(GuiReprValueNumberSpinner.FLOAT.getFormat(), 1234), "Float format");

        assertEquals((float) 1234, GuiReprValueNumberSpinner.FLOAT.parse(GuiReprValueNumberSpinner.FLOAT.getFormat(), "1,234"), "Float parse");

        assertEquals("1234.56", GuiReprValueNumberSpinner.FLOAT.toString(1234.56f).substring(0, 7), "Float toString");

        assertEquals(1234.56f, (Float) GuiReprValueNumberSpinner.FLOAT.fromString("001234.56"), 0.01f, "Float fromString");

        assertEquals("1,234.56", GuiReprValueNumberSpinner.FLOAT.format(GuiReprValueNumberSpinner.FLOAT.getFormat(), 1234.56f).substring(0, 8), "Float format");

        assertEquals(1234.56f, (Float) GuiReprValueNumberSpinner.FLOAT.parse(GuiReprValueNumberSpinner.FLOAT.getFormat(), "1,234.56"), 0.01f, "Float parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.FLOAT.format(GuiReprValueNumberSpinner.FLOAT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Float format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.FLOAT.format(GuiReprValueNumberSpinner.FLOAT.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Float format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.FLOAT.parse(GuiReprValueNumberSpinner.FLOAT.getFormat(), "Infinity"), "Float parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.FLOAT.parse(GuiReprValueNumberSpinner.FLOAT.getFormat(), "-Infinity"), "Float format");
    }


    @Test
    public void testValueNumberTypeDouble() {
        assertEquals(Double.class, GuiReprValueNumberSpinner.DOUBLE.getNumberClass(), "Double type");

        assertEquals(Double.MAX_VALUE, GuiReprValueNumberSpinner.DOUBLE.getMaximum(), "Double max");

        assertEquals(-Double.MAX_VALUE, (Double) GuiReprValueNumberSpinner.DOUBLE.getMinimum(), 0.00001f, "Double min : -MAX");

        assertEquals((double) 1, GuiReprValueNumberSpinner.DOUBLE.getOne(), "Double 1");

        assertEquals((double) 0, GuiReprValueNumberSpinner.DOUBLE.getZero(), "Double 0");

        assertEquals((double) 3, GuiReprValueNumberSpinner.DOUBLE.next(1, 2, +1), "Double next");
        assertEquals((double) -1, GuiReprValueNumberSpinner.DOUBLE.next(1, 2, -1), "Double next -1");

        assertEquals((double) 3, GuiReprValueNumberSpinner.DOUBLE.convert(3), "Double convert returns as is");

        assertEquals("1234", GuiReprValueNumberSpinner.DOUBLE.toString(1234), "Double toString");

        assertEquals((double) 1234, GuiReprValueNumberSpinner.DOUBLE.fromString("001234"), "Double fromString");

        assertEquals("1,234", GuiReprValueNumberSpinner.DOUBLE.format(GuiReprValueNumberSpinner.DOUBLE.getFormat(), 1234), "Double format");

        assertEquals((double) 1234, GuiReprValueNumberSpinner.DOUBLE.parse(GuiReprValueNumberSpinner.DOUBLE.getFormat(), "1,234"), "Double parse");


        assertEquals("1234.56", GuiReprValueNumberSpinner.DOUBLE.toString(1234.56).substring(0, 7), "Double toString");

        assertEquals(1234.56, (Double) GuiReprValueNumberSpinner.DOUBLE.fromString("001234.56"), 0.01, "Double fromString");

        assertEquals("1,234.56", GuiReprValueNumberSpinner.DOUBLE.format(GuiReprValueNumberSpinner.DOUBLE.getFormat(), 1234.56).substring(0, 8), "Double format");

        assertEquals(1234.56, (Double) GuiReprValueNumberSpinner.DOUBLE.parse(GuiReprValueNumberSpinner.DOUBLE.getFormat(), "1,234.56"), 0.01, "Double parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.DOUBLE.format(GuiReprValueNumberSpinner.DOUBLE.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "Double format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.DOUBLE.format(GuiReprValueNumberSpinner.DOUBLE.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "Double format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.DOUBLE.parse(GuiReprValueNumberSpinner.DOUBLE.getFormat(), "Infinity"), "Double parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.DOUBLE.parse(GuiReprValueNumberSpinner.DOUBLE.getFormat(), "-Infinity"), "Double format");
    }

    @Test
    public void testValueNumberTypeBigInteger() {
        assertEquals(BigInteger.class, GuiReprValueNumberSpinner.BIG_INTEGER.getNumberClass(), "BigInteger type");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.BIG_INTEGER.getMaximum(), "BigInteger max");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.BIG_INTEGER.getMinimum(), "BigInteger min");

        assertEquals(BigInteger.ONE, GuiReprValueNumberSpinner.BIG_INTEGER.getOne(), "BigInteger 1");

        assertEquals(BigInteger.ZERO, GuiReprValueNumberSpinner.BIG_INTEGER.getZero(), "BigInteger 0");

        assertEquals(BigInteger.valueOf(3), GuiReprValueNumberSpinner.BIG_INTEGER.next(BigInteger.ONE, BigInteger.valueOf(2), +1), "BigInteger next");
        assertEquals(BigInteger.valueOf(-1), GuiReprValueNumberSpinner.BIG_INTEGER.next(BigInteger.ONE, BigInteger.valueOf(2), -1), "BigInteger next -1");

        assertEquals(BigInteger.valueOf(3), GuiReprValueNumberSpinner.BIG_INTEGER.convert(3), "BigInteger convert returns as is");

        assertEquals("1234567890123", GuiReprValueNumberSpinner.BIG_INTEGER.toString(BigInteger.valueOf(1234567890123L)), "BigInteger toString");

        assertEquals(BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.BIG_INTEGER.fromString("001234567890123"), "BigInteger fromString");

        assertEquals("1,234,567,890,123", GuiReprValueNumberSpinner.BIG_INTEGER.format(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), BigInteger.valueOf(1234567890123L)), "BigInteger format");

        assertEquals(BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.BIG_INTEGER.parse(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), "1,234,567,890,123"), "BigInteger parse");

        assertEquals("Infinity", GuiReprValueNumberSpinner.BIG_INTEGER.format(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "BigInteger format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.BIG_INTEGER.format(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "BigInteger format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.BIG_INTEGER.parse(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), "Infinity"), "BigInteger parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.BIG_INTEGER.parse(GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), "-Infinity"), "BigInteger format");

    }

    @Test
    public void testValueNumberTypeBigDecimal() {
        assertEquals(BigDecimal.class, GuiReprValueNumberSpinner.BIG_DECIMAL.getNumberClass(), "BigDecimal type");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.BIG_DECIMAL.getMaximum(), "BigDecimal max");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.BIG_DECIMAL.getMinimum(), "BigDecimal min");

        assertEquals(BigDecimal.ONE, GuiReprValueNumberSpinner.BIG_DECIMAL.getOne(), "BigDecimal 1");

        assertEquals(BigDecimal.ZERO, GuiReprValueNumberSpinner.BIG_DECIMAL.getZero(), "BigDecimal 0");

        assertEquals(BigDecimal.valueOf(3), GuiReprValueNumberSpinner.BIG_DECIMAL.next(BigDecimal.ONE, BigDecimal.valueOf(2), +1), "BigDecimal next");
        assertEquals(BigDecimal.valueOf(-1), GuiReprValueNumberSpinner.BIG_DECIMAL.next(BigDecimal.ONE, BigDecimal.valueOf(2), -1), "BigDecimal next -1");

        assertEquals(BigDecimal.valueOf(3), GuiReprValueNumberSpinner.BIG_DECIMAL.convert(3), "BigDecimal convert returns as is");

        assertEquals("1234567890123", GuiReprValueNumberSpinner.BIG_DECIMAL.toString(BigDecimal.valueOf(1234567890123L)), "BigDecimal toString");

        assertEquals(BigDecimal.valueOf(1234567890123L), GuiReprValueNumberSpinner.BIG_DECIMAL.fromString("001234567890123"), "BigDecimal fromString");

        assertEquals("1,234,567,890,123", GuiReprValueNumberSpinner.BIG_DECIMAL.format(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), BigDecimal.valueOf(1234567890123L)), "BigDecimal format");

        assertEquals(BigDecimal.valueOf(1234567890123L), GuiReprValueNumberSpinner.BIG_DECIMAL.parse(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "1,234,567,890,123"), "BigDecimal parse");


        assertEquals("1234567890123.56", GuiReprValueNumberSpinner.BIG_DECIMAL.toString(new BigDecimal("1234567890123.56")), "BigDecimal toString");

        assertEquals(new BigDecimal("1234567890123.56"), GuiReprValueNumberSpinner.BIG_DECIMAL.fromString("001234567890123.56"), "BigDecimal fromString");

        assertEquals("1,234,567,890,123.56", GuiReprValueNumberSpinner.BIG_DECIMAL.format(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), new BigDecimal("1234567890123.56")), "BigDecimal format");

        assertEquals(new BigDecimal("1234567890123.56"), GuiReprValueNumberSpinner.BIG_DECIMAL.parse(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "1,234,567,890,123.56"), "BigDecimal parse");


        assertEquals("Infinity", GuiReprValueNumberSpinner.BIG_DECIMAL.format(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), GuiReprValueNumberSpinner.MAXIMUM), "BigDecimal format Inf");

        assertEquals("-Infinity", GuiReprValueNumberSpinner.BIG_DECIMAL.format(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), GuiReprValueNumberSpinner.MINIMUM), "BigDecimal format -Inf");

        assertEquals(GuiReprValueNumberSpinner.MAXIMUM, GuiReprValueNumberSpinner.BIG_DECIMAL.parse(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "Infinity"), "BigDecimal parse Inf");

        assertEquals(GuiReprValueNumberSpinner.MINIMUM, GuiReprValueNumberSpinner.BIG_DECIMAL.parse(GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "-Infinity"), "BigDecimal format");

    }

    @Test
    public void testValueGetCommonType() {
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BYTE);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.SHORT);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.INT);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.SHORT);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.SHORT);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.INT);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.INT);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.INT);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.INT);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.LONG);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.FLOAT);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.DOUBLE);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_INTEGER);
        assertCommonType(GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);

        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BYTE, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.SHORT, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.LONG, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.FLOAT, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_INTEGER, GuiReprValueNumberSpinner.BIG_DECIMAL);
        assertCommonType(GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL, GuiReprValueNumberSpinner.BIG_DECIMAL);
    }

    private void assertCommonType(GuiReprValueNumberSpinner.NumberType l, GuiReprValueNumberSpinner.NumberType r,
                                  GuiReprValueNumberSpinner.NumberType exp) {
        assertEquals(
                exp, GuiReprValueNumberSpinner.getCommonType(l, r),
                "getCommonType " + l + ", " + r + " : " + exp);

    }

    @Test
    public void testValueGetCommonTypeForNumbers() {
        assertEquals(GuiReprValueNumberSpinner.INT, GuiReprValueNumberSpinner.getCommonTypeForNumbers(123, 456), "getCommonTypeForNumbers");

        assertEquals(GuiReprValueNumberSpinner.DOUBLE, GuiReprValueNumberSpinner.getCommonTypeForNumbers(123, 456.78), "getCommonTypeForNumbers");
    }

    @Test
    public void testValueCompare() {
        assertEquals(0, GuiReprValueNumberSpinner.compare(456, 456L), "compare by common type");

        assertEquals(1, GuiReprValueNumberSpinner.compare(457, 456.999), "compare by common type");

        assertEquals(-1, GuiReprValueNumberSpinner.compare(456.999f, new BigDecimal("456.99900000000000000000000000000000001")), "compare by common type");
    }

    @Test
    public void testValueTo() {
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte((byte) 123), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte((short) 123), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(123), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(123L), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(123.45f), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(123.45), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(BigInteger.valueOf(123)), "toByte");
        assertEquals((byte) 123, GuiReprValueNumberSpinner.toByte(BigDecimal.valueOf(123.45)), "toByte");

        assertEquals((short) 123, GuiReprValueNumberSpinner.toShort((byte) 123), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort((short) 12345), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(12345), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(12345L), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(12345.45f), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(12345.45), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(BigInteger.valueOf(12345)), "toShort");
        assertEquals((short) 12345, GuiReprValueNumberSpinner.toShort(BigDecimal.valueOf(12345.45)), "toShort");

        assertEquals(123, GuiReprValueNumberSpinner.toInt((byte) 123), "toInt");
        assertEquals(12345, GuiReprValueNumberSpinner.toInt((short) 12345), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(1234567), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(1234567L), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(1234567.45f), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(1234567.45), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(BigInteger.valueOf(1234567)), "toInt");
        assertEquals(1234567, GuiReprValueNumberSpinner.toInt(BigDecimal.valueOf(1234567.45)), "toInt");

        assertEquals(123L, GuiReprValueNumberSpinner.toLong((byte) 123), "toLong");
        assertEquals(12345L, GuiReprValueNumberSpinner.toLong((short) 12345), "toLong");
        assertEquals(1234567L, GuiReprValueNumberSpinner.toLong(1234567), "toLong");
        assertEquals(1234567890123L, GuiReprValueNumberSpinner.toLong(1234567890123L), "toLong");
        assertEquals(1234567L, GuiReprValueNumberSpinner.toLong(1234567.45f), "toLong");
        assertEquals(1234567890123L, GuiReprValueNumberSpinner.toLong(1234567890123.45), "toLong");
        assertEquals(1234567890123L, GuiReprValueNumberSpinner.toLong(BigInteger.valueOf(1234567890123L)), "toLong");
        assertEquals(1234567890123L, GuiReprValueNumberSpinner.toLong(BigDecimal.valueOf(1234567890123.45)), "toLong");

        assertEquals(123f, GuiReprValueNumberSpinner.toFloat((byte) 123), 0.001f, "toFloat");
        assertEquals(12345f, GuiReprValueNumberSpinner.toFloat((short) 12345), 0.001f, "toFloat");
        assertEquals(1234567f, GuiReprValueNumberSpinner.toFloat(1234567), 0.001f, "toFloat");
        assertEquals(1234567f, GuiReprValueNumberSpinner.toFloat(1234567L), 0.001f, "toFloat");
        assertEquals(1234567.45f, GuiReprValueNumberSpinner.toFloat(1234567.45f), 0.001f, "toFloat");
        assertEquals(1234567.45f, GuiReprValueNumberSpinner.toFloat(1234567.45), 0.001f, "toFloat");
        assertEquals(1234567f, GuiReprValueNumberSpinner.toFloat(BigInteger.valueOf(1234567)), 0.001f, "toFloat");
        assertEquals(1234567.45f, GuiReprValueNumberSpinner.toFloat(BigDecimal.valueOf(1234567.45)), 0.001f, "toFloat");
        assertEquals(Float.NEGATIVE_INFINITY, GuiReprValueNumberSpinner.toFloat(GuiReprValueNumberSpinner.MINIMUM), 0, "toFloat");
        assertEquals(Float.POSITIVE_INFINITY, GuiReprValueNumberSpinner.toFloat(GuiReprValueNumberSpinner.MAXIMUM), 0, "toFloat");

        assertEquals(123, GuiReprValueNumberSpinner.toDouble((byte) 123), 0.001, "toDouble");
        assertEquals(12345, GuiReprValueNumberSpinner.toDouble((short) 12345), 0.001, "toDouble");
        assertEquals(1234567, GuiReprValueNumberSpinner.toDouble(1234567), 0.001, "toDouble");
        assertEquals(1234567, GuiReprValueNumberSpinner.toDouble(1234567L), 0.001, "toDouble");
        assertEquals(1234567.45, GuiReprValueNumberSpinner.toDouble(1234567.45f), 0.1, "toDouble");
        assertEquals(1234567.45, GuiReprValueNumberSpinner.toDouble(1234567.45), 0.001, "toDouble");
        assertEquals(1234567, GuiReprValueNumberSpinner.toDouble(BigInteger.valueOf(1234567)), 0.001, "toDouble");
        assertEquals(1234567.45, GuiReprValueNumberSpinner.toDouble(BigDecimal.valueOf(1234567.45)), 0.001, "toDouble");
        assertEquals(Double.NEGATIVE_INFINITY, GuiReprValueNumberSpinner.toDouble(GuiReprValueNumberSpinner.MINIMUM), 0, "toDouble");
        assertEquals(Double.POSITIVE_INFINITY, GuiReprValueNumberSpinner.toDouble(GuiReprValueNumberSpinner.MAXIMUM), 0, "toDouble");

        assertEquals(BigInteger.valueOf(123L), GuiReprValueNumberSpinner.toBigInteger((byte) 123), "toBigInteger");
        assertEquals(BigInteger.valueOf(12345L), GuiReprValueNumberSpinner.toBigInteger((short) 12345), "toBigInteger");
        assertEquals(BigInteger.valueOf(1234567L), GuiReprValueNumberSpinner.toBigInteger(1234567), "toBigInteger");
        assertEquals(BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.toBigInteger(1234567890123L), "toBigInteger");
        assertEquals(BigInteger.valueOf(1234567L), GuiReprValueNumberSpinner.toBigInteger(1234567.45f), "toBigInteger");
        assertEquals(BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.toBigInteger(1234567890123.45), "toBigInteger");
        assertEquals(new BigInteger("12345678901234567890123"), GuiReprValueNumberSpinner.toBigInteger(new BigInteger("12345678901234567890123")), "toBigInteger");
        assertEquals(new BigInteger("12345678901234567890123"), GuiReprValueNumberSpinner.toBigInteger(new BigDecimal("12345678901234567890123.45")), "toBigInteger");

        assertEquals(BigDecimal.valueOf(123), GuiReprValueNumberSpinner.toBigDecimal((byte) 123), "toBigDecimal");
        assertEquals(BigDecimal.valueOf(12345), GuiReprValueNumberSpinner.toBigDecimal((short) 12345), "toBigDecimal");
        assertEquals(BigDecimal.valueOf(1234567), GuiReprValueNumberSpinner.toBigDecimal(1234567), "toBigDecimal");
        assertEquals(BigDecimal.valueOf(1234567L), GuiReprValueNumberSpinner.toBigDecimal(1234567L), "toBigDecimal");
        assertEquals(BigDecimal.valueOf(1234567.45f), GuiReprValueNumberSpinner.toBigDecimal(1234567.45f), "toBigDecimal");
        assertEquals(BigDecimal.valueOf(1234567.45), GuiReprValueNumberSpinner.toBigDecimal(1234567.45), "toBigDecimal");
        assertEquals(new BigDecimal("12345678901234567890123"), GuiReprValueNumberSpinner.toBigDecimal(new BigInteger("12345678901234567890123")), "toBigDecimal");
        assertEquals(new BigDecimal("12345678901234567890123.45"), GuiReprValueNumberSpinner.toBigDecimal(new BigDecimal("12345678901234567890123.45")), "toBigDecimal");
    }

    @Test
    public void testValueInfinity() {
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Long.MAX_VALUE), "Infinity always larger than long");
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(new BigInteger("12345678901234567890123")), "Infinity always larger than big-integer");
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Double.MAX_VALUE), "Infinity always larger than double");
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(new BigDecimal("12345678901234567890123.45")), "Infinity always larger than big-decimal");

        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(Long.MIN_VALUE), "-Infinity always smaller than long");
        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(new BigInteger("-12345678901234567890123")), "-Infinity always smaller than big-integer");
        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(-Double.MAX_VALUE), "-Infinity always smaller than double");
        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(new BigDecimal("-12345678901234567890123.45")), "-Infinity always smaller than big-decimal");

        assertEquals(0, GuiReprValueNumberSpinner.MAXIMUM.compareTo(GuiReprValueNumberSpinner.MAXIMUM), "Inifity vs Infinity");
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(GuiReprValueNumberSpinner.MINIMUM), "Inifity vs -Infinity");
        assertEquals(0, GuiReprValueNumberSpinner.MINIMUM.compareTo(GuiReprValueNumberSpinner.MINIMUM), "-Inifity vs -Infinity");
        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(GuiReprValueNumberSpinner.MAXIMUM), "-Inifity vs Infinity");

        assertEquals(0, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Float.POSITIVE_INFINITY), "Inifity vs Float.Infinity");
        assertEquals(1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Float.NEGATIVE_INFINITY), "Inifity vs -Float.Infinity");
        assertEquals(0, GuiReprValueNumberSpinner.MINIMUM.compareTo(Double.NEGATIVE_INFINITY), "-Inifity vs -Double.Infinity");
        assertEquals(-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(Double.POSITIVE_INFINITY), "-Inifity vs Double.Infinity");
    }

    //------------------

    @Test
    public void testValueToJson() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        assertEquals(123456, spinner.toJson(ctx, 123456), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        assertEquals((byte) 123, spinner.toJson(ctx, (byte) 123), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        assertEquals((short) 12345, spinner.toJson(ctx, (short) 12345), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        assertEquals(12356789012L, spinner.toJson(ctx, 12356789012L), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        assertEquals(123.5f, spinner.toJson(ctx, 123.5f), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        assertEquals(123.45, spinner.toJson(ctx, 123.45), "toJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        assertEquals("12345678901234567890123", spinner.toJson(ctx, new BigInteger("12345678901234567890123")), "toJson returns str for BigInteger");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        assertEquals("12345678901234567890123.45", spinner.toJson(ctx, new BigDecimal("12345678901234567890123.45")), "toJson returns str for BigDecimal");
    }

    @Test
    public void testValueFromJson() {
        assertFalse(spinner.isJsonSetter(), "non jsonSetter");

        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        assertEquals(123456, spinner.fromJson(ctx, null, 123456), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        assertEquals((byte) 123, spinner.fromJson(ctx, null, (byte) 123), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        assertEquals((short) 12345, spinner.fromJson(ctx, null, (short) 12345), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        assertEquals(12356789012L, spinner.fromJson(ctx, null, 12356789012L), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        assertEquals(123.5f, spinner.fromJson(ctx, null, 123.5f), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        assertEquals(123.45, spinner.fromJson(ctx, null, 123.45), "fromJson returns same value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        assertEquals(new BigInteger("12345678901234567890123"), spinner.fromJson(ctx, null, "12345678901234567890123"), "fromJson returns BigInteger from str");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        assertEquals(new BigDecimal("12345678901234567890123.45"), spinner.fromJson(ctx, null, "12345678901234567890123.45"), "fromJson returns BigDecimal from str");
    }

    @Test
    public void testValueToHumanReadableString() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        assertEquals("123,456", spinner.toHumanReadableString(ctx, 123456), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        assertEquals("123", spinner.toHumanReadableString(ctx, (byte) 123), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        assertEquals("12,345", spinner.toHumanReadableString(ctx, (short) 12345), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        assertEquals("12,356,789,012", spinner.toHumanReadableString(ctx, 12356789012L), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        assertEquals("123.5", spinner.toHumanReadableString(ctx, 123.5f), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        assertEquals("123.45", spinner.toHumanReadableString(ctx, 123.45), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        assertEquals("12,345,678,901,234,567,890,123", spinner.toHumanReadableString(ctx, new BigInteger("12345678901234567890123")), "toHRS returns format value as args");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        assertEquals("12,345,678,901,234,567,890,123.45", spinner.toHumanReadableString(ctx, new BigDecimal("12345678901234567890123.45")), "toHRS returns format value as args");
    }

    @Test
    public void testValueFromHumanReadableString() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        assertEquals(123456, spinner.fromHumanReadableString(ctx, "123,456"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        assertEquals((byte) 123, spinner.fromHumanReadableString(ctx, "123"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        assertEquals((short) 12345, spinner.fromHumanReadableString(ctx, "12,345"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        assertEquals(12356789012L, spinner.fromHumanReadableString(ctx, "12,356,789,012"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        assertEquals(123.5f, spinner.fromHumanReadableString(ctx, "123.5"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        assertEquals(123.45, spinner.fromHumanReadableString(ctx, "123.45"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        assertEquals(new BigInteger("12345678901234567890123"), spinner.fromHumanReadableString(ctx, "12,345,678,901,234,567,890,123"), "fromHRS returns value from format value");

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        assertEquals(new BigDecimal("12345678901234567890123.45"), spinner.fromHumanReadableString(ctx, "12,345,678,901,234,567,890,123.45"), "fromHRS returns value from format value");
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        assertEquals("null", spinner.toHumanReadableString(ctx, null), "toHRS null");
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        assertNull(spinner.fromHumanReadableString(ctx, "null"), "fromHRS null");
    }

}
