package autogui.base.mapping;

import java.util.List;

public class GuiReprActionList implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementActionList()) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        return false;
    }

    public void executeActionForList(GuiMappingContext context, List<?> selection) {
        try {
            Object target = context.getParentValuePane().getUpdatedValue(context.getParent(), true);
            context.getTypeElementAsActionList().execute(target, selection);
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }

    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object json) {
        return null;
    }
}
