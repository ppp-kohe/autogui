package org.autogui.base.mapping;

import java.util.Objects;
import java.util.regex.Pattern;

/** an abstract checkbox component for a {@link Boolean} or primitive boolean property
 *  <pre>
 *      &#64;GuiIncluded public boolean prop;
 *  </pre>
 * */
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

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json instanceof Boolean) {
            return json;
        } else {
            return null;
        }
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return Objects.toString(source);
    }

    /**
     * use {@link #getBooleanValue(String)}
     * @param context the context (ignored)
     * @param str the source string
     * @return a {@link Boolean} value
     */
    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return getBooleanValue(str);
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    static Pattern numPattern = Pattern.compile("\\d+");

    /**
     * @param data a source string
     * @return false for "false" or "0". true for "true" or digits. case insensitive. null for null
     */
    public Boolean getBooleanValue(String data) {
        if (data == null) {
            return null;
        } else {
            data = data.toLowerCase();
            if (data.equals("false") || data.equals("0")) {
                return false;
            } else if (data.equals("true") || numPattern.matcher(data).matches()) {
                return true;
            } else {
                return null;
            }
        }
    }
}
