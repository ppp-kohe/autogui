package autogui.base.mapping;

import java.math.BigDecimal;

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
}
