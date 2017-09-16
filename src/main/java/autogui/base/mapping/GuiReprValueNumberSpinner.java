package autogui.base.mapping;

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
}
