package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.swing.icons.GuiSwingIcons;

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

            Icon icon = getActionIcon();
            if (icon != null) {
                putValue(LARGE_ICON_KEY, icon);
            }
            Icon pressIcon = getActionPressedIcon();
            if (pressIcon != null) {
                putValue(GuiSwingIcons.PRESSED_ICON_KEY, pressIcon);
            }
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprAction) context.getRepresentation()).executeAction(context);
            } finally {
                setEnabled(true);
            }
        }

        public String getIconName() {
            return context.getIconName();
        }

        public Icon getActionIcon() {
            return GuiSwingIcons.getInstance().getIcon(getIconName());
        }

        public Icon getActionPressedIcon() {
            return GuiSwingIcons.getInstance().getPressedIcon(getIconName());
        }
    }
}
