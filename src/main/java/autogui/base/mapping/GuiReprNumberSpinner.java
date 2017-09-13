package autogui.base.mapping;

public class GuiReprNumberSpinner implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isSourceProperty()) {
            Class<?> cls = context.getSourcePropertyTypeAsClass();
            if (Number.class.isAssignableFrom(cls) || isPrimitiveNumberClass(cls)) {
                context.setRepresentation(this);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
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
