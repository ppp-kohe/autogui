package autogui.base.mapping;

/**
 * a text-field component for a {@link String} property
 */
public class GuiReprValueStringField extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return cls.equals(String.class);
    }

    public String toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue != null) {
            return (String) newValue;
        } else {
            return "";
        }
    }

    /**
     * @param context the context of the repr.
     * @return false if the type is a value (String).
     *          a String is immutable.
     */
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
     * @return String
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof String) {
            return source;
        } else {
            return null;
        }
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json instanceof String) {
            return json;
        } else {
            return null;
        }
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }
}
