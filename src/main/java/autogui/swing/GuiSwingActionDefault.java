package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GuiSwingActionDefault implements GuiSwingAction {
    @Override
    public Action createAction(GuiMappingContext context) {
        return new ExecutionAction(context);
    }


    public static class ExecutionAction extends AbstractAction {
        protected GuiMappingContext context;
        public ExecutionAction(GuiMappingContext context) {
            this.context = context;
            putValue(Action.NAME, context.getDisplayName());

            Icon icon = getActionIcon(context.getName());
            if (icon != null) {
                putValue(LARGE_ICON_KEY, icon);
            }
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprAction) context.getRepresentation()).executeAction(context);
                //TODO run in another thread?
            } finally {
                setEnabled(true);
            }
        }

        public Icon getActionIcon(String name) {
            //TODO
            return null;
        }
    }
}
