package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeObject;

public class GuiReprObjectTabbedPane extends GuiReprObjectPane {

    public GuiReprObjectTabbedPane(GuiRepresentation subRepresentation) {
        super(subRepresentation);
    }

    public GuiReprObjectTabbedPane() {
    }

    @Override
    protected boolean matchWithoutSetting(GuiMappingContext context) {
        return super.matchWithoutSetting(context) &&
                !isRecursiveRepr(context) &&
                matchWithoutSettingSubContexts(context);
    }

    public boolean isRecursiveRepr(GuiMappingContext context) {
        GuiMappingContext parent = context.getParent();
        while (parent != null) {
            if (parent.getRepresentation() != null &&
                    parent.getRepresentation() instanceof GuiReprObjectTabbedPane) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public boolean matchWithoutSettingSubContexts(GuiMappingContext context) {
        int panes = 0;
        for (GuiMappingContext subContext : context.createChildCandidates()) {
            if ((subContext.isTypeElementProperty() &&
                    (subContext.getTypeElementAsProperty().getType() instanceof GuiTypeObject ||
                     subContext.getTypeElementAsProperty().getType() instanceof GuiTypeCollection)) ||
                subContext.isTypeElementObject() ||
                subContext.isTypeElementCollection()) {
                ++panes;
            } else if (subContext.isTypeElementAction() || subContext.isTypeElementActionList()) {
                //ok
            } else {
                return false;
            }
        }
        return panes >= 2;
    }
}
