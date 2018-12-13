package org.autogui.base.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * a spinner text-field component for a {@link Number} or primitive number property
 * <pre>
 *     &#64;GuiIncluded public int      intProp;
 *     &#64;GuiIncluded public byte     byteProp;
 *     &#64;GuiIncluded public short    shortProp;
 *     &#64;GuiIncluded public long     longProp;
 *     &#64;GuiIncluded public float    floatProp;
 *     &#64;GuiIncluded public double   doubleProp;
 *
 *     &#64;GuiIncluded public Integer  intObjProp;
 *     &#64;GuiIncluded public Byte     byteObjProp;
 *     &#64;GuiIncluded public Short    shortObjProp;
 *     &#64;GuiIncluded public Long     longObjProp;
 *     &#64;GuiIncluded public Float    floatObjProp;
 *     &#64;GuiIncluded public Double   doubleObjProp;
 *
 *     &#64;GuiIncluded public BigInteger bigIntProp;
 *     &#64;GuiIncluded public BigDecimal bigDecimalProp;
 * </pre>
 */
public class GuiReprValueNumberSpinner extends GuiReprValue {
    protected NumberType type;
    protected NumberFormat format;

    public GuiReprValueNumberSpinner() { }

    public GuiReprValueNumberSpinner(NumberType type, NumberFormat format) {
        this.type = type;
        this.format = format;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        Class<?> cls = getValueType(context);
        if (cls != null && matchValueType(cls)) {
            NumberType type = getType(cls);
            context.setRepresentation(createNumberSpinner(type));
            return true;
        } else {
            return false;
        }
    }

    public GuiReprValueNumberSpinner createNumberSpinner(NumberType type) {
        return new GuiReprValueNumberSpinner(type, type == null ? null : type.getFormat());
    }

    @Override
    public boolean matchValueType(Class<?> cls) {
        return Number.class.isAssignableFrom(cls) || isPrimitiveNumberClass(cls);
    }

    public boolean isPrimitiveNumberClass(Class<?> retType) {
        return retType.equals(int.class)
                || retType.equals(float.class)
                || retType.equals(byte.class)
                || retType.equals(short.class)
                || retType.equals(long.class)
                || retType.equals(double.class);
    }

    public boolean isRealNumberType(GuiMappingContext context) {
        Class<?> cls = getValueType(context);
        return isRealNumberType(cls);
    }

    public boolean isRealNumberType(Class<?> retType) {
        return retType.equals(float.class) ||
                retType.equals(double.class) ||
                retType.equals(Float.class) ||
                retType.equals(Double.class) ||
                BigDecimal.class.isAssignableFrom(retType);
    }

    @Override
    public Object toUpdateValue(GuiMappingContext context, Object value) {
        NumberType numType = getType(getValueType(context));
        if (value == null) {
            return numType.getZero();
        } else {
            return numType.convert(value);
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Number or String (for BigInteger and BigDecimal). For null and primitive, 0. For null and object, null.
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        Class<?> type = getValueType(context);
        NumberType numType = getType(type);
        if (source == null) {
            if (type.isPrimitive()) {
                return numType.getZero();
            } else {
                return null;
            }
        } else if (numType instanceof NumberTypeBigDecimal || numType instanceof NumberTypeBigInteger) {
            return numType.toString((Comparable<?>) source);
        } else {
            if (source instanceof BigInteger || source instanceof BigDecimal) {
                return source.toString();
            } else {
                return toUpdateValue(context, source);
            }
        }
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        NumberType numType = getType(getValueType(context));
        if (json == null) {
            return null;
        } else if ((numType instanceof NumberTypeBigDecimal || numType instanceof NumberTypeBigInteger) &&
                json instanceof String) {
            String jsonStr = (String) json;
            return numType.fromString(jsonStr);
        } else if (json instanceof Number) {
            return numType.convert(json);
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    /**
     * @param context the context of the repr.
     * @param str a source string
     * @return parsed {@link Number} or {@link Comparable} (including "Infinity")
     *           by a {@link NumberFormat} returned by {@link #getFormat()}
     */
    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        NumberType type = getType(context);
        NumberFormat fmt = getFormat();
        if (fmt == null) {
            fmt = type.getFormat();
        }
        if (str.equals("null")) {
            return null;
        }
        try {
            return type.parse(fmt, str);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @param context the context of the repr.
     * @param source a {@link Comparable} converted to string
     * @return formatted string by {@link NumberFormat} returned by {@link #getFormat()}
     */
    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        NumberType type = getType(context);
        NumberFormat fmt = getFormat();
        if (fmt == null) {
            fmt = type.getFormat();
        }
        if (source instanceof Comparable<?>) {
            return type.format(fmt, (Comparable<?>) source);
        } else {
            return "" + source;
        }
    }

    public NumberType getType(GuiMappingContext context) {
        if (type == null && context != null) {
            return getType(getValueType(context));
        }
        return type;
    }

    public NumberFormat getFormat() {
        if (format == null) {
            NumberType t = type;
            if (t != null) {
                return t.getFormat();
            } else {
                return null;
            }
        }
        return format;
    }

    public void setFormat(NumberFormat format) {
        this.format = format;
    }

    /**
     * a type information of number.
     */
    public interface NumberType {
        Class<? extends Number> getNumberClass();
        /** @return the maximum value, it might be an Infinity */
        Comparable<?> getMaximum();
        /** @return the minimum value, it might be an Infinity */
        Comparable<?> getMinimum();

        /**
         * @param previous a previous number value
         * @param step     additional value to the previous
         * @param direction  1 or -1, indicating step-up or -down
         * @return         previous + step * direction
         */
        Object next(Object previous, Number step, int direction);

        Number getOne();
        Number getZero();
        Comparable<?> convert(Object value);

        String toString(Comparable<?> n);
        Comparable<?> fromString(String s);

        NumberFormat getFormat();

        Object parse(NumberFormat format, String source);
        String format(NumberFormat format, Comparable<?> value);
    }

    public static Infinity MAXIMUM = new Infinity(true);
    public static Infinity MINIMUM = new Infinity(false);

    public static NumberTypeInt INT = new NumberTypeInt();
    public static NumberTypeLong LONG = new NumberTypeLong();
    public static NumberTypeShort SHORT = new NumberTypeShort();
    public static NumberTypeByte BYTE = new NumberTypeByte();
    public static NumberTypeFloat FLOAT = new NumberTypeFloat();
    public static NumberTypeDouble DOUBLE = new NumberTypeDouble();
    public static NumberTypeBigInteger BIG_INTEGER = new NumberTypeBigInteger();
    public static NumberTypeBigDecimal BIG_DECIMAL = new NumberTypeBigDecimal();

    public static NumberType getType(Class<?> cls) {
        if (cls.equals(int.class) || cls.equals(Integer.class)) {
            return INT;
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            return LONG;
        } else if (cls.equals(short.class) || cls.equals(Short.class)) {
            return SHORT;
        } else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            return BYTE;
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            return FLOAT;
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            return DOUBLE;
        } else if (cls.equals(BigInteger.class)) {
            return BIG_INTEGER;
        } else if (cls.equals(BigDecimal.class)) {
            return BIG_DECIMAL;
        } else {
            throw new RuntimeException("unsupported " + cls);
        }
    }

    @SuppressWarnings("unchecked")
    public static int compare(Number l, Number r) {
        NumberType t = getCommonTypeForNumbers(l, r);
        return ((Comparable) t.convert(l)).compareTo(t.convert(r));
    }

    public static NumberType getCommonTypeForNumbers(Number l, Number r) {
        return getCommonType(getType(l.getClass()), getType(r.getClass()));
    }

    /**
     * obtains a common type for given 2 number types
     * <table>
     *     <caption>domains and range</caption>
     * <tr><th>l,r</th>	<th>byte</th>	<th>short</th>	<th>int</th>	<th>long</th>	<th>float</th>	<th>double</th>	<th>bigInteger</th>	<th>bigDecimal</th></tr>
     * <tr><th>byte</th>	<td>byte</td>	<td>short</td>	<td>int</td>	<td>long</td>	<td>float</td>	<td>double</td>	<td>bigInteger</td>	<td>bigDecimal</td></tr>
     * <tr><th>short</th>	<td>short</td>	<td>short</td>	<td>int</td>	<td>long</td>	<td>float</td>	<td>double</td>	<td>bigInteger</td>	<td>bigDecimal</td></tr>
     * <tr><th>int</th>  	<td>int</td>  	<td>int</td>	<td>int</td>	<td>long</td>	<td>float</td>	<td>double</td>	<td>bigInteger</td>	<td>bigDecimal</td></tr>
     * <tr><th>long</th>	<td>long</td>	<td>long</td>	<td>long</td>	<td>long</td>	<td>double</td>	<td>double</td>	<td>bigInteger</td>	<td>bigDecimal</td></tr>
     * <tr><th>float</th>	<td>float</td>	<td>float</td>	<td>float</td>	<td>double</td>	<td>float</td>	<td>double</td>	<td>bigDecimal</td>	<td>bigDecimal</td></tr>
     * <tr><th>double</th>	<td>double</td>	<td>double</td>	<td>double</td>	<td>double</td>	<td>double</td>	<td>double</td>	<td>bigDecimal</td>	<td>bigDecimal</td></tr>
     * <tr><th>bigInteger</th>	<td>bigInteger</td>	<td>bigInteger</td>	<td>bigInteger</td>	<td>bigInteger</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigInteger</td>	<td>bigDecimal</td></tr>
     * <tr><th>bigDecimal</th>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td>	<td>bigDecimal</td></tr>
     * </table>
     * @param l a number type
     * @param r a number type
     * @return a common number type, whose value can be obtained by {@link NumberType#convert(Object)}
     */
    public static NumberType getCommonType(NumberType l, NumberType r) {
        if (l.equals(r)) {
            return l;
        } else if (match(l, r, BYTE, LONG) || match(l, r, SHORT, LONG) || match(l, r, INT, LONG)) {
            return LONG;
        } else if (match(l, r, BYTE, INT) || match(l, r, SHORT, INT)) {
            return INT;
        } else if (match(l, r, BYTE, SHORT)) {
            return SHORT;
        } else if (match(l, r, BYTE, FLOAT) || match(l, r, SHORT, FLOAT) || match(l, r, INT, FLOAT)) {
            return FLOAT;
        } else if (match(l, r, LONG, FLOAT) || match(l, r, FLOAT, DOUBLE) ||
                match(l, r, BYTE, DOUBLE) || match(l, r, SHORT, DOUBLE) || match(l, r, INT, DOUBLE) || match(l, r, LONG, DOUBLE)) {
            return DOUBLE;
        } else if (match(l, r, BYTE, BIG_INTEGER) || match(l, r, SHORT, BIG_INTEGER) || match(l, r, INT, BIG_INTEGER) ||
                match(l, r, LONG, BIG_INTEGER)) {
            return BIG_INTEGER;
        } else {
            return BIG_DECIMAL;
        }
    }

    private static boolean match(NumberType l, NumberType r, NumberType l2, NumberType r2) {
        return (l.equals(l2) && r.equals(r2)) || (l.equals(r2) && r.equals(l2));
    }

    public static int toInt(Object n) {
        if (n instanceof Number) {
            return ((Number) n).intValue();
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static byte toByte(Object n) {
        if (n instanceof Number) {
            return ((Number) n).byteValue();
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static short toShort(Object n) {
        if (n instanceof Number) {
            return ((Number) n).shortValue();
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static long toLong(Object n) {
        if (n instanceof Number) {
            return ((Number) n).longValue();
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static float toFloat(Object n) {
        if (n instanceof Number) {
            return ((Number) n).floatValue();
        } else if (n instanceof Infinity) {
            return ((Infinity) n).isUpper() ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static double toDouble(Object n) {
        if (n instanceof Number) {
            return ((Number) n).doubleValue();
        } else if (n instanceof Infinity) {
            return ((Infinity) n).isUpper() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static BigDecimal toBigDecimal(Object n) {
        if (n instanceof Integer || n instanceof Byte || n instanceof Short || n instanceof Long) {
            return BigDecimal.valueOf(((Number) n).longValue());
        } else if (n instanceof Float || n instanceof Double) {
            return BigDecimal.valueOf(((Number) n).doubleValue());
        } else if (n instanceof BigInteger) {
            return new BigDecimal((BigInteger) n);
        } else if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static BigInteger toBigInteger(Object n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        } else if (n instanceof BigDecimal) {
            return ((BigDecimal) n).toBigInteger();
        } else if (n instanceof Number) {
            return BigInteger.valueOf(((Number) n).longValue());
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    /** a comparable infinity representation, which is not a Number type, but comparable to any other types.
     * Note: the compareTo method of the class is not reflexive. This means the method is incorrect implementation.
     *  The class is intended to use just for an undefined setting of upper or lower bound of a property.
     *   */
    public static class Infinity implements Comparable<Object> {
        protected boolean upper;

        public Infinity(boolean upper) {
            this.upper = upper;
        }

        public boolean isUpper() {
            return upper;
        }

        @Override
        public int compareTo(Object o) {
            if (o.equals(this)) {
                return 0;
            } else if (o instanceof Infinity) {
                if (upper == ((Infinity) o).upper) {
                    return 0;
                } else if (upper) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (o instanceof Double && ((Double) o).isInfinite()) {
                Double d = (Double) o;
                if (upper == (d > 0)) {
                    return 0;
                } else if (upper) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (o instanceof Float && ((Float) o).isInfinite()) {
                Float d = (Float) o;
                if (upper == (d > 0)) {
                    return 0;
                } else if (upper) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return upper ? 1 : -1;
            }
        }

        @Override
        public String toString() {
            return (upper ? "+\u221e" : "-\u221e");
        }
    }

    /** default impl. of the number type */
    public abstract static class NumberTypeDefault implements NumberType {
        protected Class<? extends Number> numberClass;
        protected Comparable<?> max;
        protected Comparable<?> min;

        public NumberTypeDefault(Class<? extends Number> numberClass, Comparable<?> max, Comparable<?> min) {
            this.numberClass = numberClass;
            this.max = max;
            this.min = min;
        }

        @Override
        public Comparable<?> getMinimum() {
            return min;
        }

        @Override
        public Comparable<?> getMaximum() {
            return max;
        }

        @Override
        public Class<? extends Number> getNumberClass() {
            return numberClass;
        }

        @Override
        public String toString(Comparable<?> n) {
            if (n instanceof Infinity) {
                return ((Infinity) n).isUpper() ? "Infinity" : "-Infinity";
            }
            return n.toString();
        }

        public Infinity fromStringInfinity(String str) {
            if (str.equals("-Infinity") || str.equals(MINIMUM.toString())) {
                return MINIMUM;
            } else if (str.equals("Infinity") || str.equals("+Infinity") || str.equals(MAXIMUM.toString())) {
                return MAXIMUM;
            } else {
                return null;
            }
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Infinity i = fromStringInfinity(source);
            if (i != null) {
                return i;
            } else {
                try {
                    return format.parse(source);
                } catch (Exception ex) {
                    return fromString(source);
                }
            }
        }

        @Override
        public String format(NumberFormat format, Comparable<?> value) {
            if (value instanceof Infinity) {
                return toString(value);
            }
            try {
                return format.format(value);
            } catch (Exception ex) {
                return toString(value);
            }
        }
    }

    /** the number type for int */
    public static class NumberTypeInt extends NumberTypeDefault {
        public NumberTypeInt() {
            super(Integer.class, Integer.MAX_VALUE, Integer.MIN_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return toInt(previous) + step.intValue() * direction;
        }

        @Override
        public Number getOne() {
            return 1;
        }

        @Override
        public Number getZero() {
            return 0;
        }

        public Comparable<?> convert(Object value) {
            return toInt(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            return Integer.valueOf(s);
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###");
            df.setParseIntegerOnly(true);
            df.setMaximumFractionDigits(0);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toInt(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for byte */
    public static class NumberTypeByte extends NumberTypeDefault {
        public NumberTypeByte() {
            super(Byte.class, Byte.MAX_VALUE, Byte.MIN_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (byte) ((toByte(previous)) + step.byteValue() * direction);
        }

        @Override
        public Number getOne() {
            return (byte) 1;
        }

        @Override
        public Number getZero() {
            return (byte) 0;
        }

        public Comparable<?> convert(Object value) {
            return toByte(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            return Byte.valueOf(s);
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###");
            df.setParseIntegerOnly(true);
            df.setMaximumFractionDigits(0);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toByte(v);
            } else {
                return v;
            }
        }
    }


    /** the number type for short */
    public static class NumberTypeShort extends NumberTypeDefault {
        public NumberTypeShort() {
            super(Short.class, Short.MAX_VALUE, Short.MIN_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (short) ((toShort(previous)) + step.shortValue() * direction);
        }

        @Override
        public Number getOne() {
            return (short) 1;
        }

        @Override
        public Number getZero() {
            return (short) 0;
        }

        public Comparable<?> convert(Object value) {
            return toShort(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            return Short.valueOf(s);
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###");
            df.setParseIntegerOnly(true);
            df.setMaximumFractionDigits(0);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toShort(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for long */
    public static class NumberTypeLong extends NumberTypeDefault {
        public NumberTypeLong() {
            super(Long.class, Long.MAX_VALUE, Long.MIN_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (toLong(previous) + step.longValue() * direction);
        }

        @Override
        public Number getOne() {
            return 1L;
        }

        @Override
        public Number getZero() {
            return 0L;
        }

        public Comparable<?> convert(Object value) {
            return toLong(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            return Long.valueOf(s);
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###");
            df.setParseIntegerOnly(true);
            df.setMaximumFractionDigits(0);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String src) {
            Object v = super.parse(format, src);
            if (v instanceof Number) {
                return toLong(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for float */
    public static class NumberTypeFloat extends NumberTypeDefault {
        public NumberTypeFloat() {
            super(Float.class, Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (toFloat(previous) + step.floatValue() * direction);
        }

        @Override
        public Number getOne() {
            return 1f;
        }

        @Override
        public Number getZero() {
            return 0f;
        }

        public Comparable<?> convert(Object value) {
            return toFloat(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            Infinity i = fromStringInfinity(s);
            if (i != null) {
                return i.isUpper() ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            } else {
                return Float.valueOf(s);
            }
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###.#");
            df.setMaximumFractionDigits(6);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toFloat(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for double */
    public static class NumberTypeDouble extends NumberTypeDefault {
        public NumberTypeDouble() {
            super(Double.class, Double.MAX_VALUE, -Double.MAX_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (toDouble(previous) + step.doubleValue() * direction);
        }

        @Override
        public Number getOne() {
            return 1.0;
        }

        @Override
        public Number getZero() {
            return 0.0;
        }

        public Comparable<?> convert(Object value) {
            return toDouble(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            Infinity i = fromStringInfinity(s);
            if (i != null) {
                return i.isUpper() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            } else {
                return Double.valueOf(s);
            }
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###.#");
            //df.setMaximumFractionDigits(Short.MAX_VALUE);
            df.setMaximumFractionDigits(8);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toDouble(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for {@link BigInteger} */
    public static class NumberTypeBigInteger extends NumberTypeDefault {
        public NumberTypeBigInteger() {
            super(BigInteger.class, MAXIMUM, MINIMUM);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            if (previous instanceof Infinity) {
                return previous;
            } else {
                return toBigInteger(step).multiply(BigInteger.valueOf(direction)).add(toBigInteger(previous));
            }
        }

        @Override
        public Number getOne() {
            return BigInteger.ONE;
        }

        @Override
        public Number getZero() {
            return BigInteger.ZERO;
        }

        public Comparable<?> convert(Object value) {
            return toBigInteger(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            Infinity i = fromStringInfinity(s);
            if (i != null) {
                return i;
            } else {
                return new BigInteger(s);
            }
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###.#");
            df.setMaximumFractionDigits(0);
            df.setParseBigDecimal(true);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toBigInteger(v);
            } else {
                return v;
            }
        }
    }

    /** the number type for {@link BigDecimal} */
    public static class NumberTypeBigDecimal extends NumberTypeDefault {
        public NumberTypeBigDecimal() {
            super(BigDecimal.class, MAXIMUM, MINIMUM);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            if (previous instanceof Infinity) {
                return previous;
            } else {
                return toBigDecimal(step).multiply(BigDecimal.valueOf(direction)).add(toBigDecimal(previous));
            }
        }

        @Override
        public Number getOne() {
            return BigDecimal.ONE;
        }

        @Override
        public Number getZero() {
            return BigDecimal.ZERO;
        }

        public Comparable<?> convert(Object value) {
            return toBigDecimal(value);
        }

        @Override
        public Comparable<?> fromString(String s) {
            Infinity i = fromStringInfinity(s);
            if (i != null) {
                return i;
            } else {
                return new BigDecimal(s);
            }
        }

        @Override
        public NumberFormat getFormat() {
            DecimalFormat df = new DecimalFormat("#,###.#");
            //df.setMaximumFractionDigits(Short.MAX_VALUE);
            df.setMaximumFractionDigits(16);
            df.setParseBigDecimal(true);
            return df;
        }

        @Override
        public Object parse(NumberFormat format, String source) {
            Object v = super.parse(format, source);
            if (v instanceof Number) {
                return toBigDecimal(v);
            } else {
                return v;
            }
        }
    }


}
