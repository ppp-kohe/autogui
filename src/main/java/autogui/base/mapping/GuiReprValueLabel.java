package autogui.base.mapping;

import java.util.Objects;

public class GuiReprValueLabel extends GuiReprValue {
    public String toUpdateValue(GuiMappingContext context, Object newValue) {
        return Objects.toString(newValue);
    }
}
