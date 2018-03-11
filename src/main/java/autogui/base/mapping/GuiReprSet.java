package autogui.base.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** a composition of representations.
 * if {@link #match(GuiMappingContext)} matches a member representation, it immediately stops the matching and returns true.
 *  the matched member representation will be set to the context */
public class GuiReprSet implements GuiRepresentation {
    protected List<GuiRepresentation> representations = new ArrayList<>();

    public GuiReprSet add(GuiRepresentation... rs) {
        representations.addAll(Arrays.asList(rs));
        return this;
    }

    public GuiReprSet insert(GuiRepresentation... rs) {
        representations.addAll(0, Arrays.asList(rs));
        return this;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        for (GuiRepresentation representation : representations) {
            if (representation.match(context)) {
                return true;
            }
        }
        return false;
    }

    /** the class is basically used for matching; so the definition of the method is useless. */
    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        for (GuiRepresentation representation : representations) {
            if (representation.checkAndUpdateSource(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }
}
