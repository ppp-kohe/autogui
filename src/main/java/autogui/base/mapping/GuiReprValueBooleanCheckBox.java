package autogui.base.mapping;

import java.util.regex.Pattern;

/** an abstract checkbox component for a {@link Boolean} or primitive boolean property */
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
    public boolean isJsonSetter() {
        return false;
    }

    static Pattern numPattern = Pattern.compile("\\d+");

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
