package autogui.base.mapping;

public class GuiReprObjectPane implements GuiRepresentation {
    protected GuiRepresentation subRepresentation;

    public GuiReprObjectPane(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    public GuiReprObjectPane() {
        this(GuiRepresentation.NONE);
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isSourceObject() && !context.isRecursive()) {
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
}
