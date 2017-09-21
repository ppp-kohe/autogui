package autogui.base.mapping;

public class GuiReprValueEnumComboBox extends GuiReprValue {

    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.isEnum();
    }

    public String getDisplayName(GuiMappingContext context, Enum<?> enumValue) {
        return enumValue.name(); //TODO display name
    }
}
