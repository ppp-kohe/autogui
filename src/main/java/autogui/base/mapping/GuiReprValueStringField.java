package autogui.base.mapping;

import java.util.Objects;

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
}
