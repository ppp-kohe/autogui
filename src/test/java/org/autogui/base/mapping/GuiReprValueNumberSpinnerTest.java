package org.autogui.base.mapping;

import org.autogui.base.type.GuiTypeBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class GuiReprValueNumberSpinnerTest {
    GuiReprValueNumberSpinner spinner;

    GuiTypeBuilder builder;


    @Before
    public void setUp() {
        spinner = new GuiReprValueNumberSpinner();
        builder = new GuiTypeBuilder();
    }

    @Test
    public void testValueGetType() {
        Assert.assertEquals("getType int",
                GuiReprValueNumberSpinner.INT,
                GuiReprValueNumberSpinner.getType(int.class));

        Assert.assertEquals("getType Integer",
                GuiReprValueNumberSpinner.INT,
                GuiReprValueNumberSpinner.getType(Integer.class));

        Assert.assertEquals("getType long",
                GuiReprValueNumberSpinner.LONG,
                GuiReprValueNumberSpinner.getType(long.class));

        Assert.assertEquals("getType Long",
                GuiReprValueNumberSpinner.LONG,
                GuiReprValueNumberSpinner.getType(Long.class));

        Assert.assertEquals("getType short",
                GuiReprValueNumberSpinner.SHORT,
                GuiReprValueNumberSpinner.getType(short.class));

        Assert.assertEquals("getType Short",
                GuiReprValueNumberSpinner.SHORT,
                GuiReprValueNumberSpinner.getType(Short.class));

        Assert.assertEquals("getType byte",
                GuiReprValueNumberSpinner.BYTE,
                GuiReprValueNumberSpinner.getType(byte.class));

        Assert.assertEquals("getType Byte",
                GuiReprValueNumberSpinner.BYTE,
                GuiReprValueNumberSpinner.getType(Byte.class));

        Assert.assertEquals("getType float",
                GuiReprValueNumberSpinner.FLOAT,
                GuiReprValueNumberSpinner.getType(float.class));

        Assert.assertEquals("getType Float",
                GuiReprValueNumberSpinner.FLOAT,
                GuiReprValueNumberSpinner.getType(Float.class));

        Assert.assertEquals("getType double",
                GuiReprValueNumberSpinner.DOUBLE,
                GuiReprValueNumberSpinner.getType(double.class));

        Assert.assertEquals("getType Double",
                GuiReprValueNumberSpinner.DOUBLE,
                GuiReprValueNumberSpinner.getType(Double.class));

        Assert.assertEquals("getType BigInteger",
                GuiReprValueNumberSpinner.BIG_INTEGER,
                GuiReprValueNumberSpinner.getType(BigInteger.class));

        Assert.assertEquals("getType BigDecimal",
                GuiReprValueNumberSpinner.BIG_DECIMAL,
                GuiReprValueNumberSpinner.getType(BigDecimal.class));
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(builder.get(int.class));
        Assert.assertTrue("match with typed context",
                spinner.match(ctx));
        Assert.assertTrue("match sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);

        GuiReprValueNumberSpinner matchedSpinner = (GuiReprValueNumberSpinner) ctx.getRepresentation();
        Assert.assertEquals("match sets repr type",
                GuiReprValueNumberSpinner.INT,
                matchedSpinner.getType((GuiMappingContext) null));

        Assert.assertEquals("match sets format from repr type",
                GuiReprValueNumberSpinner.INT.getFormat().format(1234),
                matchedSpinner.getFormat().format(1234));
    }

    @Test
    public void testValueMatchTypes() {
        GuiMappingContext ctx = new GuiMappingContext(builder.get(float.class));
        Assert.assertTrue("match float",
                spinner.match(ctx));
        Assert.assertTrue("match float sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match float",
                GuiReprValueNumberSpinner.FLOAT,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertTrue("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(double.class));
        Assert.assertTrue("match double",
                spinner.match(ctx));
        Assert.assertTrue("match double sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match double",
                GuiReprValueNumberSpinner.DOUBLE,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertTrue("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(long.class));
        Assert.assertTrue("match long",
                spinner.match(ctx));
        Assert.assertTrue("match long sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match long",
                GuiReprValueNumberSpinner.LONG,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertFalse("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(short.class));
        Assert.assertTrue("match short",
                spinner.match(ctx));
        Assert.assertTrue("match short sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match short",
                GuiReprValueNumberSpinner.SHORT,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertFalse("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(byte.class));
        Assert.assertTrue("match byte",
                spinner.match(ctx));
        Assert.assertTrue("match byte sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match byte",
                GuiReprValueNumberSpinner.BYTE,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertFalse("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(BigInteger.class));
        Assert.assertTrue("match BigInteger",
                spinner.match(ctx));
        Assert.assertTrue("match BigInteger sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match BigInteger",
                GuiReprValueNumberSpinner.BIG_INTEGER,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertFalse("real number type",
                spinner.isRealNumberType(ctx));

        ctx = new GuiMappingContext(builder.get(BigDecimal.class));
        Assert.assertTrue("match BigDecimal",
                spinner.match(ctx));
        Assert.assertTrue("match BigDecimal sets a new repr",
                ctx.getRepresentation() instanceof GuiReprValueNumberSpinner);
        Assert.assertEquals("match BigDecimal",
                GuiReprValueNumberSpinner.BIG_DECIMAL,
                ((GuiReprValueNumberSpinner) ctx.getRepresentation()).getType((GuiMappingContext) null));
        Assert.assertTrue("real number type",
                spinner.isRealNumberType(ctx));
    }

    @Test
    public void testValueNumberTypeInt() {
        Assert.assertEquals("Int type",
                Integer.class,
                GuiReprValueNumberSpinner.INT.getNumberClass());

        Assert.assertEquals("Int max",
                Integer.MAX_VALUE,
                GuiReprValueNumberSpinner.INT.getMaximum());

        Assert.assertEquals("Int min",
                Integer.MIN_VALUE,
                GuiReprValueNumberSpinner.INT.getMinimum());

        Assert.assertEquals("Int 1",
                1,
                GuiReprValueNumberSpinner.INT.getOne());

        Assert.assertEquals("Int 0",
                0,
                GuiReprValueNumberSpinner.INT.getZero());

        Assert.assertEquals("Int next",
                3,
                GuiReprValueNumberSpinner.INT.next(1, 2, +1));
        Assert.assertEquals("Int next -1",
                -1,
                GuiReprValueNumberSpinner.INT.next(1, 2, -1));

        Assert.assertEquals("Int convert returns as is",
                3,
                GuiReprValueNumberSpinner.INT.convert(3));

        Assert.assertEquals("Int toString",
                "12345",
                GuiReprValueNumberSpinner.INT.toString(12345));

        Assert.assertEquals("Int fromString",
                12345,
                GuiReprValueNumberSpinner.INT.fromString("00012345"));

        Assert.assertEquals("Int format",
                "12,345",
                GuiReprValueNumberSpinner.INT.format(
                        GuiReprValueNumberSpinner.INT.getFormat(), 12345));

        Assert.assertEquals("Int parse",
                12345,
                GuiReprValueNumberSpinner.INT.parse(
                        GuiReprValueNumberSpinner.INT.getFormat(),"12,345"));



        Assert.assertEquals("Int format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.INT.format(
                        GuiReprValueNumberSpinner.INT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Int format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.INT.format(
                        GuiReprValueNumberSpinner.INT.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Int parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.INT.parse(
                        GuiReprValueNumberSpinner.INT.getFormat(), "Infinity"));

        Assert.assertEquals("Int format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.INT.parse(
                        GuiReprValueNumberSpinner.INT.getFormat(), "-Infinity"));

    }

    @Test
    public void testValueNumberTypeByte() {
        Assert.assertEquals("Byte type",
                Byte.class,
                GuiReprValueNumberSpinner.BYTE.getNumberClass());

        Assert.assertEquals("Byte max",
                Byte.MAX_VALUE,
                GuiReprValueNumberSpinner.BYTE.getMaximum());

        Assert.assertEquals("Byte min",
                Byte.MIN_VALUE,
                GuiReprValueNumberSpinner.BYTE.getMinimum());

        Assert.assertEquals("Byte 1",
                (byte) 1,
                GuiReprValueNumberSpinner.BYTE.getOne());

        Assert.assertEquals("Byte 0",
                (byte) 0,
                GuiReprValueNumberSpinner.BYTE.getZero());

        Assert.assertEquals("Byte next",
                (byte) 3,
                GuiReprValueNumberSpinner.BYTE.next(1, 2, +1));
        Assert.assertEquals("Byte next -1",
                (byte) -1,
                GuiReprValueNumberSpinner.BYTE.next(1, 2, -1));

        Assert.assertEquals("Byte convert returns as is",
                (byte) 3,
                GuiReprValueNumberSpinner.BYTE.convert(3));

        Assert.assertEquals("Byte toString",
                "123",
                GuiReprValueNumberSpinner.BYTE.toString(123));

        Assert.assertEquals("Byte fromString",
                (byte) 123,
                GuiReprValueNumberSpinner.BYTE.fromString("00123"));

        Assert.assertEquals("Byte format",
                "123",
                GuiReprValueNumberSpinner.BYTE.format(
                        GuiReprValueNumberSpinner.BYTE.getFormat(), 123));

        Assert.assertEquals("Byte parse",
                (byte) 123,
                GuiReprValueNumberSpinner.BYTE.parse(
                        GuiReprValueNumberSpinner.BYTE.getFormat(),"123"));

        Assert.assertEquals("Byte format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.BYTE.format(
                        GuiReprValueNumberSpinner.BYTE.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Byte format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.BYTE.format(
                        GuiReprValueNumberSpinner.BYTE.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Byte parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.BYTE.parse(
                        GuiReprValueNumberSpinner.BYTE.getFormat(), "Infinity"));

        Assert.assertEquals("Byte format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.BYTE.parse(
                        GuiReprValueNumberSpinner.BYTE.getFormat(), "-Infinity"));

    }

    @Test
    public void testValueNumberTypeShort() {
        Assert.assertEquals("Short type",
                Short.class,
                GuiReprValueNumberSpinner.SHORT.getNumberClass());

        Assert.assertEquals("Short max",
                Short.MAX_VALUE,
                GuiReprValueNumberSpinner.SHORT.getMaximum());

        Assert.assertEquals("Short min",
                Short.MIN_VALUE,
                GuiReprValueNumberSpinner.SHORT.getMinimum());

        Assert.assertEquals("Short 1",
                (short) 1,
                GuiReprValueNumberSpinner.SHORT.getOne());

        Assert.assertEquals("Short 0",
                (short) 0,
                GuiReprValueNumberSpinner.SHORT.getZero());

        Assert.assertEquals("Short next",
                (short) 3,
                GuiReprValueNumberSpinner.SHORT.next(1, 2, +1));
        Assert.assertEquals("Short next -1",
                (short) -1,
                GuiReprValueNumberSpinner.SHORT.next(1, 2, -1));

        Assert.assertEquals("Short convert returns as is",
                (short) 3,
                GuiReprValueNumberSpinner.SHORT.convert(3));

        Assert.assertEquals("Short toString",
                "1234",
                GuiReprValueNumberSpinner.SHORT.toString(1234));

        Assert.assertEquals("Short fromString",
                (short) 1234,
                GuiReprValueNumberSpinner.SHORT.fromString("001234"));

        Assert.assertEquals("Short format",
                "1,234",
                GuiReprValueNumberSpinner.SHORT.format(
                        GuiReprValueNumberSpinner.SHORT.getFormat(), 1234));

        Assert.assertEquals("Short parse",
                (short) 1234,
                GuiReprValueNumberSpinner.SHORT.parse(
                        GuiReprValueNumberSpinner.SHORT.getFormat(),"1,234"));

        Assert.assertEquals("Short format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.SHORT.format(
                        GuiReprValueNumberSpinner.SHORT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Short format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.SHORT.format(
                        GuiReprValueNumberSpinner.SHORT.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Short parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.SHORT.parse(
                        GuiReprValueNumberSpinner.SHORT.getFormat(), "Infinity"));

        Assert.assertEquals("Short format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.SHORT.parse(
                        GuiReprValueNumberSpinner.SHORT.getFormat(), "-Infinity"));
    }

    @Test
    public void testValueNumberTypeLong() {
        Assert.assertEquals("Long type",
                Long.class,
                GuiReprValueNumberSpinner.LONG.getNumberClass());

        Assert.assertEquals("Long max",
                Long.MAX_VALUE,
                GuiReprValueNumberSpinner.LONG.getMaximum());

        Assert.assertEquals("Long min",
                Long.MIN_VALUE,
                GuiReprValueNumberSpinner.LONG.getMinimum());

        Assert.assertEquals("Long 1",
                (long) 1,
                GuiReprValueNumberSpinner.LONG.getOne());

        Assert.assertEquals("Long 0",
                (long) 0,
                GuiReprValueNumberSpinner.LONG.getZero());

        Assert.assertEquals("Long next",
                (long) 3,
                GuiReprValueNumberSpinner.LONG.next(1, 2, +1));
        Assert.assertEquals("Long next -1",
                (long) -1,
                GuiReprValueNumberSpinner.LONG.next(1, 2, -1));

        Assert.assertEquals("Long convert returns as is",
                (long) 3,
                GuiReprValueNumberSpinner.LONG.convert(3));

        Assert.assertEquals("Long toString",
                "1234567890123",
                GuiReprValueNumberSpinner.LONG.toString(1234567890123L));

        Assert.assertEquals("Long fromString",
                1234567890123L,
                GuiReprValueNumberSpinner.LONG.fromString("001234567890123"));

        Assert.assertEquals("Long format",
                "1,234,567,890,123",
                GuiReprValueNumberSpinner.LONG.format(
                        GuiReprValueNumberSpinner.LONG.getFormat(), 1234567890123L));

        Assert.assertEquals("Long parse",
                1234567890123L,
                GuiReprValueNumberSpinner.LONG.parse(
                        GuiReprValueNumberSpinner.LONG.getFormat(),"1,234,567,890,123"));

        Assert.assertEquals("Long format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.LONG.format(
                        GuiReprValueNumberSpinner.LONG.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Long format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.LONG.format(
                        GuiReprValueNumberSpinner.LONG.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Long parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.LONG.parse(
                        GuiReprValueNumberSpinner.LONG.getFormat(), "Infinity"));

        Assert.assertEquals("Long format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.LONG.parse(
                        GuiReprValueNumberSpinner.LONG.getFormat(), "-Infinity"));

    }

    @Test
    public void testValueNumberTypeFloat() {
        Assert.assertEquals("Float type",
                Float.class,
                GuiReprValueNumberSpinner.FLOAT.getNumberClass());

        Assert.assertEquals("Float max",
                Float.MAX_VALUE,
                GuiReprValueNumberSpinner.FLOAT.getMaximum());

        Assert.assertEquals("Float min : -MAX",
                -Float.MAX_VALUE,
                (Float) GuiReprValueNumberSpinner.FLOAT.getMinimum(),
                0.00001f);

        Assert.assertEquals("Float 1",
                (float) 1,
                GuiReprValueNumberSpinner.FLOAT.getOne());

        Assert.assertEquals("Float 0",
                (float) 0,
                GuiReprValueNumberSpinner.FLOAT.getZero());

        Assert.assertEquals("Float next",
                (float) 3,
                GuiReprValueNumberSpinner.FLOAT.next(1, 2, +1));
        Assert.assertEquals("Float next -1",
                (float) -1,
                GuiReprValueNumberSpinner.FLOAT.next(1, 2, -1));

        Assert.assertEquals("Float convert returns as is",
                (float) 3,
                GuiReprValueNumberSpinner.FLOAT.convert(3));

        Assert.assertEquals("Float toString",
                "1234",
                GuiReprValueNumberSpinner.FLOAT.toString(1234));

        Assert.assertEquals("Float fromString",
                (float) 1234,
                GuiReprValueNumberSpinner.FLOAT.fromString("001234"));

        Assert.assertEquals("Float format",
                "1,234",
                GuiReprValueNumberSpinner.FLOAT.format(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), 1234));

        Assert.assertEquals("Float parse",
                (float) 1234,
                GuiReprValueNumberSpinner.FLOAT.parse(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(),"1,234"));

        Assert.assertEquals("Float toString",
                "1234.56",
                GuiReprValueNumberSpinner.FLOAT.toString(1234.56f)
                .substring(0, 7));

        Assert.assertEquals("Float fromString",
                1234.56f,
                (Float) GuiReprValueNumberSpinner.FLOAT.fromString("001234.56"),
                0.01f);

        Assert.assertEquals("Float format",
                "1,234.56",
                GuiReprValueNumberSpinner.FLOAT.format(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), 1234.56f)
                .substring(0, 8));

        Assert.assertEquals("Float parse",
                1234.56f,
                (Float) GuiReprValueNumberSpinner.FLOAT.parse(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(),"1,234.56"),
                0.01f);

        Assert.assertEquals("Float format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.FLOAT.format(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Float format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.FLOAT.format(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Float parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.FLOAT.parse(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), "Infinity"));

        Assert.assertEquals("Float format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.FLOAT.parse(
                        GuiReprValueNumberSpinner.FLOAT.getFormat(), "-Infinity"));
    }


    @Test
    public void testValueNumberTypeDouble() {
        Assert.assertEquals("Double type",
                Double.class,
                GuiReprValueNumberSpinner.DOUBLE.getNumberClass());

        Assert.assertEquals("Double max",
                Double.MAX_VALUE,
                GuiReprValueNumberSpinner.DOUBLE.getMaximum());

        Assert.assertEquals("Double min : -MAX",
                -Double.MAX_VALUE,
                (Double) GuiReprValueNumberSpinner.DOUBLE.getMinimum(),
                0.00001f);

        Assert.assertEquals("Double 1",
                (double) 1,
                GuiReprValueNumberSpinner.DOUBLE.getOne());

        Assert.assertEquals("Double 0",
                (double) 0,
                GuiReprValueNumberSpinner.DOUBLE.getZero());

        Assert.assertEquals("Double next",
                (double) 3,
                GuiReprValueNumberSpinner.DOUBLE.next(1, 2, +1));
        Assert.assertEquals("Double next -1",
                (double) -1,
                GuiReprValueNumberSpinner.DOUBLE.next(1, 2, -1));

        Assert.assertEquals("Double convert returns as is",
                (double) 3,
                GuiReprValueNumberSpinner.DOUBLE.convert(3));

        Assert.assertEquals("Double toString",
                "1234",
                GuiReprValueNumberSpinner.DOUBLE.toString(1234));

        Assert.assertEquals("Double fromString",
                (double) 1234,
                GuiReprValueNumberSpinner.DOUBLE.fromString("001234"));

        Assert.assertEquals("Double format",
                "1,234",
                GuiReprValueNumberSpinner.DOUBLE.format(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), 1234));

        Assert.assertEquals("Double parse",
                (double) 1234,
                GuiReprValueNumberSpinner.DOUBLE.parse(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(),"1,234"));


        Assert.assertEquals("Double toString",
                "1234.56",
                GuiReprValueNumberSpinner.DOUBLE.toString(1234.56)
                        .substring(0, 7));

        Assert.assertEquals("Double fromString",
                1234.56,
                (Double) GuiReprValueNumberSpinner.DOUBLE.fromString("001234.56"),
                0.01);

        Assert.assertEquals("Double format",
                "1,234.56",
                GuiReprValueNumberSpinner.DOUBLE.format(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), 1234.56)
                        .substring(0, 8));

        Assert.assertEquals("Double parse",
                1234.56,
                (Double) GuiReprValueNumberSpinner.DOUBLE.parse(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(),"1,234.56"),
                0.01);

        Assert.assertEquals("Double format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.DOUBLE.format(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Double format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.DOUBLE.format(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("Double parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.DOUBLE.parse(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), "Infinity"));

        Assert.assertEquals("Double format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.DOUBLE.parse(
                        GuiReprValueNumberSpinner.DOUBLE.getFormat(), "-Infinity"));
    }

    @Test
    public void testValueNumberTypeBigInteger() {
        Assert.assertEquals("BigInteger type",
                BigInteger.class,
                GuiReprValueNumberSpinner.BIG_INTEGER.getNumberClass());

        Assert.assertEquals("BigInteger max",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.BIG_INTEGER.getMaximum());

        Assert.assertEquals("BigInteger min",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.BIG_INTEGER.getMinimum());

        Assert.assertEquals("BigInteger 1",
                BigInteger.ONE,
                GuiReprValueNumberSpinner.BIG_INTEGER.getOne());

        Assert.assertEquals("BigInteger 0",
                BigInteger.ZERO,
                GuiReprValueNumberSpinner.BIG_INTEGER.getZero());

        Assert.assertEquals("BigInteger next",
                BigInteger.valueOf(3),
                GuiReprValueNumberSpinner.BIG_INTEGER.next(BigInteger.ONE, BigInteger.valueOf(2), +1));
        Assert.assertEquals("BigInteger next -1",
                BigInteger.valueOf(-1),
                GuiReprValueNumberSpinner.BIG_INTEGER.next(BigInteger.ONE, BigInteger.valueOf(2), -1));

        Assert.assertEquals("BigInteger convert returns as is",
                BigInteger.valueOf(3),
                GuiReprValueNumberSpinner.BIG_INTEGER.convert(3));

        Assert.assertEquals("BigInteger toString",
                "1234567890123",
                GuiReprValueNumberSpinner.BIG_INTEGER.toString(BigInteger.valueOf(1234567890123L)));

        Assert.assertEquals("BigInteger fromString",
                BigInteger.valueOf(1234567890123L),
                GuiReprValueNumberSpinner.BIG_INTEGER.fromString("001234567890123"));

        Assert.assertEquals("BigInteger format",
                "1,234,567,890,123",
                GuiReprValueNumberSpinner.BIG_INTEGER.format(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), BigInteger.valueOf(1234567890123L)));

        Assert.assertEquals("BigInteger parse",
                BigInteger.valueOf(1234567890123L),
                GuiReprValueNumberSpinner.BIG_INTEGER.parse(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(),"1,234,567,890,123"));

        Assert.assertEquals("BigInteger format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.BIG_INTEGER.format(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("BigInteger format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.BIG_INTEGER.format(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("BigInteger parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.BIG_INTEGER.parse(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), "Infinity"));

        Assert.assertEquals("BigInteger format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.BIG_INTEGER.parse(
                        GuiReprValueNumberSpinner.BIG_INTEGER.getFormat(), "-Infinity"));

    }

    @Test
    public void testValueNumberTypeBigDecimal() {
        Assert.assertEquals("BigDecimal type",
                BigDecimal.class,
                GuiReprValueNumberSpinner.BIG_DECIMAL.getNumberClass());

        Assert.assertEquals("BigDecimal max",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.BIG_DECIMAL.getMaximum());

        Assert.assertEquals("BigDecimal min",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.BIG_DECIMAL.getMinimum());

        Assert.assertEquals("BigDecimal 1",
                BigDecimal.ONE,
                GuiReprValueNumberSpinner.BIG_DECIMAL.getOne());

        Assert.assertEquals("BigDecimal 0",
                BigDecimal.ZERO,
                GuiReprValueNumberSpinner.BIG_DECIMAL.getZero());

        Assert.assertEquals("BigDecimal next",
                BigDecimal.valueOf(3),
                GuiReprValueNumberSpinner.BIG_DECIMAL.next(BigDecimal.ONE, BigDecimal.valueOf(2), +1));
        Assert.assertEquals("BigDecimal next -1",
                BigDecimal.valueOf(-1),
                GuiReprValueNumberSpinner.BIG_DECIMAL.next(BigDecimal.ONE, BigDecimal.valueOf(2), -1));

        Assert.assertEquals("BigDecimal convert returns as is",
                BigDecimal.valueOf(3),
                GuiReprValueNumberSpinner.BIG_DECIMAL.convert(3));

        Assert.assertEquals("BigDecimal toString",
                "1234567890123",
                GuiReprValueNumberSpinner.BIG_DECIMAL.toString(BigDecimal.valueOf(1234567890123L)));

        Assert.assertEquals("BigDecimal fromString",
                BigDecimal.valueOf(1234567890123L),
                GuiReprValueNumberSpinner.BIG_DECIMAL.fromString("001234567890123"));

        Assert.assertEquals("BigDecimal format",
                "1,234,567,890,123",
                GuiReprValueNumberSpinner.BIG_DECIMAL.format(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), BigDecimal.valueOf(1234567890123L)));

        Assert.assertEquals("BigDecimal parse",
                BigDecimal.valueOf(1234567890123L),
                GuiReprValueNumberSpinner.BIG_DECIMAL.parse(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(),"1,234,567,890,123"));


        Assert.assertEquals("BigDecimal toString",
                "1234567890123.56",
                GuiReprValueNumberSpinner.BIG_DECIMAL.toString(new BigDecimal("1234567890123.56")));

        Assert.assertEquals("BigDecimal fromString",
                new BigDecimal("1234567890123.56"),
                GuiReprValueNumberSpinner.BIG_DECIMAL.fromString("001234567890123.56"));

        Assert.assertEquals("BigDecimal format",
                "1,234,567,890,123.56",
                GuiReprValueNumberSpinner.BIG_DECIMAL.format(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), new BigDecimal("1234567890123.56")));

        Assert.assertEquals("BigDecimal parse",
                new BigDecimal("1234567890123.56"),
                GuiReprValueNumberSpinner.BIG_DECIMAL.parse(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(),"1,234,567,890,123.56"));


        Assert.assertEquals("BigDecimal format Inf",
                "Infinity",
                GuiReprValueNumberSpinner.BIG_DECIMAL.format(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("BigDecimal format -Inf",
                "-Infinity",
                GuiReprValueNumberSpinner.BIG_DECIMAL.format(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), GuiReprValueNumberSpinner.MINIMUM));

        Assert.assertEquals("BigDecimal parse Inf",
                GuiReprValueNumberSpinner.MAXIMUM,
                GuiReprValueNumberSpinner.BIG_DECIMAL.parse(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "Infinity"));

        Assert.assertEquals("BigDecimal format",
                GuiReprValueNumberSpinner.MINIMUM,
                GuiReprValueNumberSpinner.BIG_DECIMAL.parse(
                        GuiReprValueNumberSpinner.BIG_DECIMAL.getFormat(), "-Infinity"));

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
        Assert.assertEquals("getCommonType " + l + ", " + r + " : " + exp,
                exp, GuiReprValueNumberSpinner.getCommonType(l, r));

    }

    @Test
    public void testValueGetCommonTypeForNumbers() {
        Assert.assertEquals("getCommonTypeForNumbers",
                GuiReprValueNumberSpinner.INT,
                GuiReprValueNumberSpinner.getCommonTypeForNumbers(123, 456));

        Assert.assertEquals("getCommonTypeForNumbers",
                GuiReprValueNumberSpinner.DOUBLE,
                GuiReprValueNumberSpinner.getCommonTypeForNumbers(123, 456.78));
    }

    @Test
    public void testValueCompare() {
        Assert.assertEquals("compare by common type",
                0, GuiReprValueNumberSpinner.compare(456, 456L));

        Assert.assertEquals("compare by common type",
                1, GuiReprValueNumberSpinner.compare(457, 456.999));

        Assert.assertEquals("compare by common type",
                -1, GuiReprValueNumberSpinner.compare(456.999f, new BigDecimal("456.99900000000000000000000000000000001")));
    }

    @Test
    public void testValueTo() {
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte((byte) 123));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte((short) 123));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(123));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(123L));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(123.45f));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(123.45));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(BigInteger.valueOf(123)));
        Assert.assertEquals("toByte", (byte) 123, GuiReprValueNumberSpinner.toByte(BigDecimal.valueOf(123.45)));

        Assert.assertEquals("toShort", (short) 123, GuiReprValueNumberSpinner.toShort((byte) 123));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort((short) 12345));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(12345));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(12345L));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(12345.45f));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(12345.45));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(BigInteger.valueOf(12345)));
        Assert.assertEquals("toShort", (short) 12345, GuiReprValueNumberSpinner.toShort(BigDecimal.valueOf(12345.45)));

        Assert.assertEquals("toInt", 123, GuiReprValueNumberSpinner.toInt((byte) 123));
        Assert.assertEquals("toInt", 12345, GuiReprValueNumberSpinner.toInt((short) 12345));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(1234567));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(1234567L));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(1234567.45f));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(1234567.45));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(BigInteger.valueOf(1234567)));
        Assert.assertEquals("toInt", 1234567, GuiReprValueNumberSpinner.toInt(BigDecimal.valueOf(1234567.45)));

        Assert.assertEquals("toLong", 123L, GuiReprValueNumberSpinner.toLong((byte) 123));
        Assert.assertEquals("toLong", 12345L, GuiReprValueNumberSpinner.toLong((short) 12345));
        Assert.assertEquals("toLong", 1234567L, GuiReprValueNumberSpinner.toLong(1234567));
        Assert.assertEquals("toLong", 1234567890123L, GuiReprValueNumberSpinner.toLong(1234567890123L));
        Assert.assertEquals("toLong", 1234567L, GuiReprValueNumberSpinner.toLong(1234567.45f));
        Assert.assertEquals("toLong", 1234567890123L, GuiReprValueNumberSpinner.toLong(1234567890123.45));
        Assert.assertEquals("toLong", 1234567890123L, GuiReprValueNumberSpinner.toLong(BigInteger.valueOf(1234567890123L)));
        Assert.assertEquals("toLong", 1234567890123L, GuiReprValueNumberSpinner.toLong(BigDecimal.valueOf(1234567890123.45)));

        Assert.assertEquals("toFloat", 123f, GuiReprValueNumberSpinner.toFloat((byte) 123), 0.001f);
        Assert.assertEquals("toFloat", 12345f, GuiReprValueNumberSpinner.toFloat((short) 12345), 0.001f);
        Assert.assertEquals("toFloat", 1234567f, GuiReprValueNumberSpinner.toFloat(1234567), 0.001f);
        Assert.assertEquals("toFloat", 1234567f, GuiReprValueNumberSpinner.toFloat(1234567L), 0.001f);
        Assert.assertEquals("toFloat", 1234567.45f, GuiReprValueNumberSpinner.toFloat(1234567.45f), 0.001f);
        Assert.assertEquals("toFloat", 1234567.45f, GuiReprValueNumberSpinner.toFloat(1234567.45), 0.001f);
        Assert.assertEquals("toFloat", 1234567f, GuiReprValueNumberSpinner.toFloat(BigInteger.valueOf(1234567)), 0.001f);
        Assert.assertEquals("toFloat", 1234567.45f, GuiReprValueNumberSpinner.toFloat(BigDecimal.valueOf(1234567.45)), 0.001f);
        Assert.assertEquals("toFloat", Float.NEGATIVE_INFINITY, GuiReprValueNumberSpinner.toFloat(GuiReprValueNumberSpinner.MINIMUM), 0);
        Assert.assertEquals("toFloat", Float.POSITIVE_INFINITY, GuiReprValueNumberSpinner.toFloat(GuiReprValueNumberSpinner.MAXIMUM), 0);

        Assert.assertEquals("toDouble", 123, GuiReprValueNumberSpinner.toDouble((byte) 123), 0.001);
        Assert.assertEquals("toDouble", 12345, GuiReprValueNumberSpinner.toDouble((short) 12345), 0.001);
        Assert.assertEquals("toDouble", 1234567, GuiReprValueNumberSpinner.toDouble(1234567), 0.001);
        Assert.assertEquals("toDouble", 1234567, GuiReprValueNumberSpinner.toDouble(1234567L), 0.001);
        Assert.assertEquals("toDouble", 1234567.45, GuiReprValueNumberSpinner.toDouble(1234567.45f), 0.1);
        Assert.assertEquals("toDouble", 1234567.45, GuiReprValueNumberSpinner.toDouble(1234567.45), 0.001);
        Assert.assertEquals("toDouble", 1234567, GuiReprValueNumberSpinner.toDouble(BigInteger.valueOf(1234567)), 0.001);
        Assert.assertEquals("toDouble", 1234567.45, GuiReprValueNumberSpinner.toDouble(BigDecimal.valueOf(1234567.45)), 0.001);
        Assert.assertEquals("toDouble", Double.NEGATIVE_INFINITY, GuiReprValueNumberSpinner.toDouble(GuiReprValueNumberSpinner.MINIMUM), 0);
        Assert.assertEquals("toDouble", Double.POSITIVE_INFINITY, GuiReprValueNumberSpinner.toDouble(GuiReprValueNumberSpinner.MAXIMUM), 0);

        Assert.assertEquals("toBigInteger", BigInteger.valueOf(123L), GuiReprValueNumberSpinner.toBigInteger((byte) 123));
        Assert.assertEquals("toBigInteger", BigInteger.valueOf(12345L), GuiReprValueNumberSpinner.toBigInteger((short) 12345));
        Assert.assertEquals("toBigInteger", BigInteger.valueOf(1234567L), GuiReprValueNumberSpinner.toBigInteger(1234567));
        Assert.assertEquals("toBigInteger", BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.toBigInteger(1234567890123L));
        Assert.assertEquals("toBigInteger", BigInteger.valueOf(1234567L), GuiReprValueNumberSpinner.toBigInteger(1234567.45f));
        Assert.assertEquals("toBigInteger", BigInteger.valueOf(1234567890123L), GuiReprValueNumberSpinner.toBigInteger(1234567890123.45));
        Assert.assertEquals("toBigInteger", new BigInteger("12345678901234567890123"), GuiReprValueNumberSpinner.toBigInteger(new BigInteger("12345678901234567890123")));
        Assert.assertEquals("toBigInteger", new BigInteger("12345678901234567890123"), GuiReprValueNumberSpinner.toBigInteger(new BigDecimal("12345678901234567890123.45")));

        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(123), GuiReprValueNumberSpinner.toBigDecimal((byte) 123));
        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(12345), GuiReprValueNumberSpinner.toBigDecimal((short) 12345));
        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(1234567), GuiReprValueNumberSpinner.toBigDecimal(1234567));
        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(1234567L), GuiReprValueNumberSpinner.toBigDecimal(1234567L));
        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(1234567.45f), GuiReprValueNumberSpinner.toBigDecimal(1234567.45f));
        Assert.assertEquals("toBigDecimal", BigDecimal.valueOf(1234567.45), GuiReprValueNumberSpinner.toBigDecimal(1234567.45));
        Assert.assertEquals("toBigDecimal", new BigDecimal("12345678901234567890123"), GuiReprValueNumberSpinner.toBigDecimal(new BigInteger("12345678901234567890123")));
        Assert.assertEquals("toBigDecimal", new BigDecimal("12345678901234567890123.45"), GuiReprValueNumberSpinner.toBigDecimal(new BigDecimal("12345678901234567890123.45")));
    }

    @Test
    public void testValueInfinity() {
        Assert.assertEquals("Infinity always larger than long" ,1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Long.MAX_VALUE));
        Assert.assertEquals("Infinity always larger than big-integer" ,1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(new BigInteger("12345678901234567890123")));
        Assert.assertEquals("Infinity always larger than double" ,1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Double.MAX_VALUE));
        Assert.assertEquals("Infinity always larger than big-decimal" ,1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(new BigDecimal("12345678901234567890123.45")));

        Assert.assertEquals("-Infinity always smaller than long" ,-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(Long.MIN_VALUE));
        Assert.assertEquals("-Infinity always smaller than big-integer" ,-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(new BigInteger("-12345678901234567890123")));
        Assert.assertEquals("-Infinity always smaller than double" ,-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(-Double.MAX_VALUE));
        Assert.assertEquals("-Infinity always smaller than big-decimal" ,-1, GuiReprValueNumberSpinner.MINIMUM.compareTo(new BigDecimal("-12345678901234567890123.45")));

        Assert.assertEquals("Inifity vs Infinity", 0, GuiReprValueNumberSpinner.MAXIMUM.compareTo(GuiReprValueNumberSpinner.MAXIMUM));
        Assert.assertEquals("Inifity vs -Infinity", 1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(GuiReprValueNumberSpinner.MINIMUM));
        Assert.assertEquals("-Inifity vs -Infinity", 0, GuiReprValueNumberSpinner.MINIMUM.compareTo(GuiReprValueNumberSpinner.MINIMUM));
        Assert.assertEquals("-Inifity vs Infinity", -1, GuiReprValueNumberSpinner.MINIMUM.compareTo(GuiReprValueNumberSpinner.MAXIMUM));

        Assert.assertEquals("Inifity vs Float.Infinity", 0, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Float.POSITIVE_INFINITY));
        Assert.assertEquals("Inifity vs -Float.Infinity", 1, GuiReprValueNumberSpinner.MAXIMUM.compareTo(Float.NEGATIVE_INFINITY));
        Assert.assertEquals("-Inifity vs -Double.Infinity", 0, GuiReprValueNumberSpinner.MINIMUM.compareTo(Double.NEGATIVE_INFINITY));
        Assert.assertEquals("-Inifity vs Double.Infinity", -1, GuiReprValueNumberSpinner.MINIMUM.compareTo(Double.POSITIVE_INFINITY));
    }

    ///////////

    @Test
    public void testValueToJson() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        Assert.assertEquals("toJson returns same value as args",123456, spinner.toJson(ctx, 123456));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        Assert.assertEquals("toJson returns same value as args",(byte) 123, spinner.toJson(ctx, (byte) 123));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        Assert.assertEquals("toJson returns same value as args",(short) 12345, spinner.toJson(ctx, (short) 12345));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        Assert.assertEquals("toJson returns same value as args", 12356789012L, spinner.toJson(ctx, 12356789012L));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        Assert.assertEquals("toJson returns same value as args",123.5f, spinner.toJson(ctx, 123.5f));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        Assert.assertEquals("toJson returns same value as args",123.45, spinner.toJson(ctx, 123.45));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        Assert.assertEquals("toJson returns str for BigInteger","12345678901234567890123", spinner.toJson(ctx, new BigInteger("12345678901234567890123")));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        Assert.assertEquals("toJson returns str for BigDecimal","12345678901234567890123.45", spinner.toJson(ctx, new BigDecimal("12345678901234567890123.45")));
    }

    @Test
    public void testValueFromJson() {
        Assert.assertFalse("non jsonSetter", spinner.isJsonSetter());

        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        Assert.assertEquals("fromJson returns same value as args",123456, spinner.fromJson(ctx, null, 123456));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        Assert.assertEquals("fromJson returns same value as args",(byte) 123, spinner.fromJson(ctx, null, (byte) 123));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        Assert.assertEquals("fromJson returns same value as args",(short) 12345, spinner.fromJson(ctx, null, (short) 12345));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        Assert.assertEquals("fromJson returns same value as args", 12356789012L, spinner.fromJson(ctx, null, 12356789012L));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        Assert.assertEquals("fromJson returns same value as args",123.5f, spinner.fromJson(ctx, null, 123.5f));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        Assert.assertEquals("fromJson returns same value as args",123.45, spinner.fromJson(ctx, null, 123.45));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        Assert.assertEquals("fromJson returns BigInteger from str",new BigInteger("12345678901234567890123"), spinner.fromJson(ctx, null, "12345678901234567890123"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        Assert.assertEquals("fromJson returns BigDecimal from str",new BigDecimal("12345678901234567890123.45"), spinner.fromJson(ctx, null, "12345678901234567890123.45"));
    }

    @Test
    public void testValueToHumanReadableString() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        Assert.assertEquals("toHRS returns format value as args","123,456", spinner.toHumanReadableString(ctx, 123456));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        Assert.assertEquals("toHRS returns format value as args","123", spinner.toHumanReadableString(ctx, (byte) 123));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        Assert.assertEquals("toHRS returns format value as args", "12,345", spinner.toHumanReadableString(ctx, (short) 12345));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        Assert.assertEquals("toHRS returns format value as args", "12,356,789,012", spinner.toHumanReadableString(ctx, 12356789012L));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        Assert.assertEquals("toHRS returns format value as args","123.5", spinner.toHumanReadableString(ctx, 123.5f));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        Assert.assertEquals("toHRS returns format value as args","123.45", spinner.toHumanReadableString(ctx, 123.45));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        Assert.assertEquals("toHRS returns format value as args", "12,345,678,901,234,567,890,123", spinner.toHumanReadableString(ctx, new BigInteger("12345678901234567890123")));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        Assert.assertEquals("toHRS returns format value as args", "12,345,678,901,234,567,890,123.45", spinner.toHumanReadableString(ctx, new BigDecimal("12345678901234567890123.45")));
    }

    @Test
    public void testValueFromHumanReadableString() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);

        Assert.assertEquals("fromHRS returns value from format value", 123456, spinner.fromHumanReadableString(ctx, "123,456"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(byte.class), (byte) 123);
        Assert.assertEquals("fromHRS returns value from format value", (byte) 123, spinner.fromHumanReadableString(ctx, "123"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(short.class), (short) 12345);
        Assert.assertEquals("fromHRS returns value from format value", (short) 12345, spinner.fromHumanReadableString(ctx, "12,345"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(long.class), 12356789012L);
        Assert.assertEquals("fromHRS returns value from format value", 12356789012L, spinner.fromHumanReadableString(ctx, "12,356,789,012"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(float.class), 123.5f);
        Assert.assertEquals("fromHRS returns value from format value", 123.5f, spinner.fromHumanReadableString(ctx, "123.5"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(double.class), 123.45);
        Assert.assertEquals("fromHRS returns value from format value", 123.45, spinner.fromHumanReadableString(ctx,  "123.45"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigInteger.class), new BigInteger("12345678901234567890123"));
        Assert.assertEquals("fromHRS returns value from format value", new BigInteger("12345678901234567890123"), spinner.fromHumanReadableString(ctx, "12,345,678,901,234,567,890,123"));

        ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(BigDecimal.class), new BigDecimal("12345678901234567890123.45"));
        Assert.assertEquals("fromHRS returns value from format value", new BigDecimal("12345678901234567890123.45"), spinner.fromHumanReadableString(ctx, "12,345,678,901,234,567,890,123.45"));
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        Assert.assertEquals("toHRS null", "null", spinner.toHumanReadableString(ctx, null));
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        GuiMappingContext ctx = new GuiReprObjectPaneTest.GuiMappingContextForDebug(builder.get(int.class), 123456);
        Assert.assertNull("fromHRS null", spinner.fromHumanReadableString(ctx, "null"));
    }

}
