package autogui.base.mapping;

import java.util.Objects;

/**
 * a read-only text label for any type of object property
 */
public class GuiReprValueLabel extends GuiReprValue {
    public String toUpdateValue(GuiMappingContext context, Object newValue) {
        return Objects.toString(newValue);
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return always null
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }

    @Override
    public boolean isEditable(GuiMappingContext context) {
        if (context.isTypeElementValue()) {
            return false;
        } else {
            return super.isEditable(context);
        }
    }
}
