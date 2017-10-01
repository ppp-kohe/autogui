package autogui.base.mapping;

public class GuiReprValueEnumComboBox extends GuiReprValue {

    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.isEnum();
    }

    public String getDisplayName(GuiMappingContext context, Enum<?> enumValue) {
        return enumValue.name(); //TODO display name
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Enum#name
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source != null) {
            return ((Enum<?>) source).name();
        } else {
            return null;
        }
    }
}
