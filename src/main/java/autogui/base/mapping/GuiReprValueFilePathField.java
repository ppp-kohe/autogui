package autogui.base.mapping;

import java.io.File;
import java.nio.file.Path;

public class GuiReprValueFilePathField extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return File.class.isAssignableFrom(cls) || Path.class.isAssignableFrom(cls);
    }

    public Path toUpdateValue(GuiMappingContext context, Object value) {
        return (Path) value;
    }


}
