package autogui.base.mapping;

import autogui.base.type.GuiTypeValue;

import java.util.HashMap;
import java.util.Map;

public class GuiReprObjectPane extends GuiReprValue {
    protected GuiRepresentation subRepresentation;

    public GuiReprObjectPane(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    public GuiReprObjectPane() {
        this(GuiRepresentation.NONE);
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementObject() && !context.isRecursive()) {
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

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Map: { propertyName: propertyJson }
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return toJsonFromObject(context, source);
    }

    public static Object toJsonFromObject(GuiMappingContext context, Object source) {
        Map<String, Object> map = new HashMap<>(context.getChildren().size());
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isTypeElementProperty()) {
                GuiRepresentation subRepr = subContext.getRepresentation();
                try {
                    Object prevValue = subContext.getSource();
                    Object nextValue = subContext.getTypeElementAsProperty().executeGet(source, prevValue);
                    if (nextValue != null && nextValue.equals(GuiTypeValue.NO_UPDATE)) {
                        nextValue = prevValue;
                    }
                    Object subObj = subRepr.toJson(subContext, nextValue);
                    if (subObj != null) {
                        map.put(subContext.getName(), subObj);
                    }
                } catch (Exception ex) {
                    //nothing
                }
            }
        }
        return map;
    }
}
