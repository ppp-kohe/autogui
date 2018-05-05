package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GuiSwingActionDefault implements GuiSwingAction {
    @Override
    public Action createAction(GuiMappingContext context) {
        return new ExecutionAction(context);
    }


    public static class ExecutionAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction,
            GuiSwingKeyBinding.RecommendedKeyStroke {
        protected GuiMappingContext context;
        public ExecutionAction(GuiMappingContext context) {
            this.context = context;
            putValue(Action.NAME, context.getDisplayName());

            Icon icon = getIcon();
            if (icon != null) {
                putValue(LARGE_ICON_KEY, icon);
            }
            Icon pressIcon = getActionPressedIcon();
            if (pressIcon != null) {
                putValue(GuiSwingIcons.PRESSED_ICON_KEY, pressIcon);
            }

            String desc = context.getDescription();
            if (!desc.isEmpty()) {
                putValue(Action.SHORT_DESCRIPTION, desc);
            }
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            executeAction();
        }

        public String getIconName() {
            return context.getIconName();
        }

        public GuiMappingContext getContext() {
            return context;
        }

        public void executeAction() {
            context.executeAction();
        }

        @Override
        public Icon getIcon() {
            return GuiSwingIcons.getInstance().getIcon(getIconName());
        }

        public Icon getActionPressedIcon() {
            return GuiSwingIcons.getInstance().getPressedIcon(getIconName());
        }

        @Override
        public String getCategory() {
            return PopupCategorized.CATEGORY_ACTION;
        }


        @Override
        public KeyStroke getRecommendedKeyStroke() {
            return GuiSwingKeyBinding.getKeyStroke(context.getAcceleratorKeyStroke());
        }

        @Override
        public GuiSwingKeyBinding.KeyPrecedenceSet getRecommendedKeyPrecedence() {
            if (context.isAcceleratorKeyStrokeSpecified()) {
                return new GuiSwingKeyBinding.KeyPrecedenceSet(GuiSwingKeyBinding.PRECEDENCE_FLAG_USER_SPECIFIED);
            } else {
                return new GuiSwingKeyBinding.KeyPrecedenceSet();
            }
        }
    }
}
