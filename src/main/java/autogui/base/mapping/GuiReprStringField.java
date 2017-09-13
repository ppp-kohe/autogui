package autogui.base.mapping;

public class GuiReprStringField implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementProperty() &&
                context.getTypeElementPropertyTypeName().equals(String.class.getName())) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }
}
