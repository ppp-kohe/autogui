package autogui.base.mapping;

import java.util.Arrays;

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

    @Override
    public Object fromJson(GuiMappingContext context, Object json) {
        if (json instanceof String) {
            String jsonStr = (String) json;
            Class<?> enumType = getValueType(context);
            return Arrays.stream(enumType.getEnumConstants())
                    .filter(e -> ((Enum<?>) e).name().equals(jsonStr))
                    .findFirst().orElse(null);
        } else{
            return null;
        }
    }
}
