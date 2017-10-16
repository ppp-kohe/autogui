package autogui.base.mapping;

import java.util.Collections;
import java.util.List;

public class GuiReprCollectionTable extends GuiReprValue implements GuiRepresentation {
    protected GuiRepresentation subRepresentation;

    public GuiReprCollectionTable(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementCollection()) {
            context.setRepresentation(this);
            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentation.match(subContext)) {
                    subContext.addToParent();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public List<?> toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue == null) {
            return Collections.emptyList();
        } else {
            return (List<?>) newValue;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return List: { elementJson, ... }.  Note: null elements are skipped
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().toJsonWithNamed(elementContext, source);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object json) {
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().fromJson(elementContext, json);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }
}
