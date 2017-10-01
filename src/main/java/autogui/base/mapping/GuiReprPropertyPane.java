package autogui.base.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** [propertyName: [  propertyValueField  ] ]
 * */
public class GuiReprPropertyPane extends GuiReprValue {
    protected GuiRepresentation subRepresentations;

    public GuiReprPropertyPane(GuiRepresentation subRepresentations) {
        this.subRepresentations = subRepresentations;
    }

    public GuiReprPropertyPane() {
        this(GuiRepresentation.NONE);
    }

    public void setSubRepresentations(GuiRepresentation subRepresentations) {
        this.subRepresentations = subRepresentations;
    }

    public GuiRepresentation getSubRepresentations() {
        return subRepresentations;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            GuiReprPropertyPane rule = createForContext(context);
            boolean matched = false;

            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentations.match(subContext)) {
                    //now, subContext holds a matched representation, and obtain it and use as the actual sub-repr
                    rule.setSubRepresentations(subContext.getRepresentation());
                    subContext.addToParent();
                    matched = true;
                }
            }

            if (matched) {
                context.setRepresentation(rule);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public GuiReprPropertyPane createForContext(GuiMappingContext context) {
        return new GuiReprPropertyPane();
    }

    public boolean checkAndUpdateSourceFromChild(GuiMappingContext child) {
        Object prev = child.getSource();
        Object next = child.getParentSource();
        if (!Objects.equals(prev, next)) {
            child.setSource(next);
            return true;
        } else {
            return false;
        }
    }

    public void updateFromGuiChild(GuiMappingContext child, Object newValue) {
        updateFromGui(child.getParent(), newValue);
    }

    public boolean isEditableFromChild(GuiMappingContext context) {
        return isEditable(context.getParent());
    }

    /**
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Map: { propertyName: propertyJson, ... }
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return GuiReprObjectPane.toJsonFromObject(context, source);
    }
}
