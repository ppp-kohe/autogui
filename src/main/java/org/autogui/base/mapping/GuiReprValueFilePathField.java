package org.autogui.base.mapping;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * a text-field component for a {@link File} or {@link Path} property.
 * <pre>
 *     &#64;GuiIncluded public Path propPath;
 *     &#64;GuiIncluded public File propFile;
 * </pre>
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
    public void updateFromGui(GuiMappingContext context, Object newValue, ObjectSpecifier specifier, GuiTaskClock viewClock) {
        if (newValue instanceof Path) {
            newValue = toValueFromPath(context, (Path) newValue);
        }
        super.updateFromGui(context, newValue, specifier, viewClock);
    }

    public Path toUpdateValue(GuiMappingContext context, Object value) {
        if (value instanceof File) {
            return ((File) value).toPath();
        } else if (value instanceof Path) {
            return (Path) value;
        } else {
            return null;
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
            Path p = toUpdateValue(context, source);
            if (p != null) {
                return p.toString();
            } else {
                return null;
            }
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
        if (json instanceof String) {
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

    /**
     *  same as {@link #toJson(GuiMappingContext, Object)}
     * @param context the context of the repr.
     * @param source converted to string
     * @return a path string
     */
    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return Objects.toString(toJson(context, source));
    }

    /**
     * same as {@link #fromJson(GuiMappingContext, Object, Object)}
     * @param context the context of the repr
     * @param str a path string
     * @return {@link Path} or {@link File}
     */
    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return fromJson(context, null, str);
    }
}
