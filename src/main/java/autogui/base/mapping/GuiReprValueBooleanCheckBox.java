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
}
