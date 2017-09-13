package autogui.base.mapping;

public class GuiReprBooleanCheckbox implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            Class<?> cls = context.getTypeElementPropertyTypeAsClass();
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

    @Override
    public boolean update(GuiMappingContext context) {
        boolean prev = toUpdateValue(context, context.getSource());
        Object src = context.getParent().getSource();

        return false;
    }

    public void updateFromGui(GuiMappingContext context, boolean newValue) {
        //TODO
        System.err.println("update " + newValue);
    }

    public boolean toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue == null) {
            return false;
        } else {
            return (Boolean) newValue;
        }
    }
}
