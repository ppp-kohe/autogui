package autogui.base.mapping;

public class GuiReprBooleanCheckbox implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isSourceProperty()) {
            Class<?> cls = context.getSourcePropertyTypeAsClass();
            if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
                context.setRepresentation(this);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
