package org.autogui.base.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** a combo-box component for an {@link Enum} property
 *  <pre>
*      &#64;GuiIncluded public EnumVal prop;
 *
 *      public enum EnumVal {...}
 *  </pre>
 */
public class GuiReprValueEnumComboBox extends GuiReprValue {
    public GuiReprValueEnumComboBox() {}
    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.isEnum();
    }

    public String getDisplayName(GuiMappingContext context, Enum<?> enumValue) {
        String n = enumValue.name();
        return context.nameJoinForDisplay(GuiMappingContext.nameSplit(n, true));
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return a name of a {@link Enum} member
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof Enum<?>) {
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
        if (json instanceof String jsonStr) {
            Class<?> enumType = getValueType(context);
            return Arrays.stream(enumType.getEnumConstants())
                    .filter(e -> ((Enum<?>) e).name().equals(jsonStr))
                    .findFirst().orElse(null);
        } else{
            return null;
        }
    }

    /**
     * same as {@link #toJson(GuiMappingContext, Object)}
     * @param context the context of the repr.
     * @param source converted to string
     * @return a name of a {@link Enum} member
     */
    @Override
    public TreeString toHumanReadableStringTree(GuiMappingContext context, Object source) {
        return new TreeStringValue(Objects.toString(toJson(context, source)));
    }

    /**
     * same as {@link #fromJson(GuiMappingContext, Object, Object)}
     * @param context the context of the repr.
     * @param str the source str: name of a member
     * @return an {@link Enum} member of the name
     */
    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return fromJson(context, null, str);
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
     * @return an Enum member or null if not found
     */
    @SuppressWarnings("rawtypes")
    public Object getEnumValue(GuiMappingContext context, String nameOrIndex) {
        Object[] es = getEnumConstants(context);
        if (numPattern.matcher(nameOrIndex).matches()) {
            int ord = Integer.parseInt(nameOrIndex);
            return Arrays.stream(es)
                    .map(Enum.class::cast)
                    .filter(e -> e.ordinal() == ord)
                    .findFirst()
                    .orElse(null);
        } else {
            List<String> names = Arrays.stream(es)
                    .map(Enum.class::cast)
                    .map(Enum::name)
                    .toList();
            int idx = names.indexOf(nameOrIndex);
            if (idx >= 0) {
                return es[idx];
            } else {
                String lower = nameOrIndex.toLowerCase();
                idx = names.stream()
                        .map(String::toLowerCase)
                        .toList()
                        .indexOf(lower);
                if (idx >= 0) {
                    return es[idx];
                }
            }
            return null;
        }
    }
}
