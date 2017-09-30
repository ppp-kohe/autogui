package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.function.Consumer;

public class GuiSwingViewBooleanCheckBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        //wraps the check-box by a panel with flow layout: the size of check-box becomes its preferred size.
        //  mouse clicks only pointing within bounds of the check-box will be processed
        JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.add(new PropertyCheckBox(context));
        return pane;
    }

    public static class PropertyCheckBox extends JCheckBox
            implements ActionListener, GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyCheckBox(GuiMappingContext context) {
            addActionListener(this);
            this.context = context;
            setName(context.getName());

            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();

            setEnabled(repr.isEditable(context));
            context.addSourceUpdateListener(this);

            if (context.isTypeElementProperty()) {
                setText(context.getDisplayName());
            }

            update(context, context.getSource());

            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
            });
            setInheritsPopupMenu(true);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            repr.updateFromGui(context, isSelected());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return isSelected();
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            //setSelected seems not to cause ActionEvent
            setSelected(repr.toUpdateValue(context, value));
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addActionListener(eventHandler::accept);
        }
    }
}
