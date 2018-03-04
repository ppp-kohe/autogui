package autogui.base.mapping;

import java.util.Arrays;

public class GuiReprValueEnumComboBox extends GuiReprValue {

    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.isEnum();
    }

    public String getDisplayName(GuiMappingContext context, Enum<?> enumValue) {
        String n = enumValue.name();
        return context.nameJoinForDisplay(context.nameSplit(n, true));
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

    /**
     *
     * @param context the context of the repr.
     * @param target unused
     * @param json a {@link String} as a name of a member of an enum.
     * @return an {@link Enum} member of the name
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
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

    @Override
    public boolean isJsonSetter() {
        return false;
    }
}
