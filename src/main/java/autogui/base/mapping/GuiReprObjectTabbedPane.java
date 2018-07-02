package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeObject;

/**
 * a tabbed version of object pane;
 *   match with a context that has
 *    2 or more sub-contexts which are a sub-object or a collection property.
 * <pre>
 *     &#64;GuiIncluded
 *     public class TabC {
 *         &#64;GuiIncluded public C1 tab1 = new C1();
 *         &#64;GuiIncluded public C2 tab2 = new C2();
 *         ...
 *
 *         &#64;GuiIncluded public void action() { ... }
 *         ...
 *     }
 *
 *     &#64;GuiIncluded public class C1 { ... }
 *     &#64;GuiIncluded public class C2 { ... }
 * </pre>
 */
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
        GuiMappingContext parent = context;
        while (parent.hasParent()) {
            parent = parent.getParent();
            if (parent.getRepresentation() != null &&
                    parent.getRepresentation() instanceof GuiReprObjectTabbedPane) {
                return true;
            }
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
