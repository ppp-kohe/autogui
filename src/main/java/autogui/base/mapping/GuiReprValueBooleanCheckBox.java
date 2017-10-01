package autogui.base.mapping;

public class GuiReprValueBooleanCheckBox extends GuiReprValue {

    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.equals(Boolean.class) || cls.equals(boolean.class);
    }

    public Boolean toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue == null) {
            return false;
        } else {
            return (Boolean) newValue;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Boolean
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof Boolean) {
            return source;
        } else {
            return null;
        }
    }
}
