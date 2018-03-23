package autogui.base.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * a spinner text-field component for a {@link Number} or primitive number property
 */
public class GuiReprValueNumberSpinner extends GuiReprValue {
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
    public boolean isEditable(GuiMappingContext context) {
        if (context.isTypeElementValue()) {
            return false;
        } else {
            return super.isEditable(context);
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Number or String (for BigInteger and BigDecimal)
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof BigInteger || source instanceof BigDecimal) {
            return source.toString();
        } else {
            return toUpdateValue(context, source);
        }
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        NumberType numType = getType(getValueType(context));
        if (numType instanceof NumberTypeBigDecimal || numType instanceof NumberTypeBigInteger) {
            if (json instanceof String) {
                String jsonStr = (String) json;
                return numType.fromString(jsonStr);
            }
        } else {
            if (json instanceof Number) {
                return (Number) json;
            }
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
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

        String toString(Number n);
        Number fromString(String s);
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
            return null;
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
        } else {
            throw new RuntimeException("illegal: " + n);
        }
    }

    public static double toDouble(Object n) {
        if (n instanceof Number) {
            return ((Number) n).doubleValue();
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

    /** a comparable infinity representation, which is not a Number type, but comparable to any other types */
    public static class Infinity implements Comparable<Object> {
        protected boolean upper;

        public Infinity(boolean upper) {
            this.upper = upper;
        }

        @Override
        public int compareTo(Object o) {
            if (o.equals(this)) {
                return 0;
            }
            return upper ? 1 : 0;
        }

        @Override
        public String toString() {
            return (upper ? "+" : "-") + "\u221e";
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
        public String toString(Number n) {
            return n.toString();
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
        public Number fromString(String s) {
            return Integer.valueOf(s);
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
        public Number fromString(String s) {
            return Byte.valueOf(s);
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
        public Number fromString(String s) {
            return Short.valueOf(s);
        }
    }

    /** the number type for long */
    public static class NumberTypeLong extends NumberTypeDefault {
        public NumberTypeLong() {
            super(Long.class, Long.MAX_VALUE, Long.MIN_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (long) (toLong(previous) + step.longValue() * direction);
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
        public Number fromString(String s) {
            return Long.valueOf(s);
        }
    }

    /** the number type for float */
    public static class NumberTypeFloat extends NumberTypeDefault {
        public NumberTypeFloat() {
            super(Float.class, Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (float) (toFloat(previous) + step.floatValue() * direction);
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
        public Number fromString(String s) {
            return Float.valueOf(s);
        }
    }

    /** the number type for double */
    public static class NumberTypeDouble extends NumberTypeDefault {
        public NumberTypeDouble() {
            super(Double.class, Double.MAX_VALUE, -Double.MAX_VALUE);
        }

        @Override
        public Object next(Object previous, Number step, int direction) {
            return (double) (toDouble(previous) + step.doubleValue() * direction);
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
        public Number fromString(String s) {
            return Double.valueOf(s);
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
        public Number fromString(String s) {
            return new BigInteger(s);
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
        public Number fromString(String s) {
            return new BigDecimal(s);
        }
    }


}
