package autogui.base.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** a combobox component for an {@link Enum} property */
public class GuiReprValueEnumComboBox extends GuiReprValue {

    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.isEnum();
    }

    public String getDisplayName(GuiMappingContext context, Enum<?> enumValue) {
        String n = enumValue.name();
        return context.nameJoinForDisplay(GuiMappingContext.nameSplit(n, true));
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


    public Object[] getEnumConstants(GuiMappingContext context) {
        return getValueType(context).getEnumConstants();
    }

    protected Pattern numPattern = Pattern.compile("\\d+");

    /**
     *
     * @param context the context which is used for obtaining the type of the enum.
     * @param nameOrIndex {@link Enum#name()} or {@link Enum#ordinal()}
     * @return a Enum member or null if not found
     */
    public Object getEnumValue(GuiMappingContext context, String nameOrIndex) {
        Object[] es = getEnumConstants(context);
        if (numPattern.matcher(nameOrIndex).matches()) {
            int ord = Integer.parseInt(nameOrIndex);
            return (ord >= 0 && ord < es.length) ? es[ord] : null;
        } else {
            List<String> names = Arrays.stream(es)
                    .map(Enum.class::cast)
                    .map(Enum::name)
                    .collect(Collectors.toList());
            int idx = names.indexOf(nameOrIndex);
            if (idx >= 0) {
                return es[idx];
            } else {
                String lower = nameOrIndex.toLowerCase();
                idx = names.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
                        .indexOf(lower);
                if (idx >= 0) {
                    return es[idx];
                }
            }
            return null;
        }
    }
}
