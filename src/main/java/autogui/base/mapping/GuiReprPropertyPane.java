package autogui.base.mapping;

/** [propertyName: [  propertyValueField  ] ]
 * */
public class GuiReprPropertyPane implements GuiRepresentation {
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
}
