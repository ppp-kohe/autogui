package autogui.base.mapping;

import java.util.Objects;

/**
 * a read-only text label for any type of object property
 * <pre>
 *     &#64;GuiIncluded public Obj proLabel = new Obj();
 *
 *     public class Obj { //non GuiIncluded
 *         public String toString() {return "label-text";}
 *     }
 * </pre>
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

    /**
     * @param context  a context of the repr.
     * @param target  a target object
     * @param json json object, ignored
     * @return the target object
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }
}
