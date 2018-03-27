package autogui.base.mapping;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * a text-field component for a {@link File} or {@link Path} property.
 */
public class GuiReprValueFilePathField extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return File.class.isAssignableFrom(cls) || Path.class.isAssignableFrom(cls);
    }

    public Object toValueFromPath(GuiMappingContext context, Path path) {
        Class<?> type = getValueType(context);
        if (File.class.isAssignableFrom(type)) {
            return path == null ? null : path.toFile();
        } else {
            //Path
            return path;
        }
    }

    @Override
    public void updateFromGui(GuiMappingContext context, Object newValue) {
        if (newValue != null && newValue instanceof Path) {
            newValue = toValueFromPath(context, (Path) newValue);
        }
        super.updateFromGui(context, newValue);
    }

    public Path toUpdateValue(GuiMappingContext context, Object value) {
        if (value != null && value instanceof File) {
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

    /**
     * obtain a file-path object from the string json
     * @param context  the context of the repr.
     * @param target  unused
     * @param json suppose a {@link String}
     * @return {@link Path} or {@link File} (determined by the context type), or null
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json != null && json instanceof String) {
            Path p = Paths.get((String) json);
            if (File.class.isAssignableFrom(getValueType(context))) {
                return p.toFile();
            } else {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }
}
