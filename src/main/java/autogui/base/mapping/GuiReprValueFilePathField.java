package autogui.base.mapping;

import java.io.File;
import java.nio.file.Path;

public class GuiReprValueFilePathField extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return File.class.isAssignableFrom(cls) || Path.class.isAssignableFrom(cls);
    }

    public Path toUpdateValue(GuiMappingContext context, Object value) {
        if (value instanceof File) {
            return ((File) value).toPath();
        } else {
            return (Path) value;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return path String
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source != null) {
            return toUpdateValue(context, source).toString();
        } else {
            return null;
        }
    }
}
