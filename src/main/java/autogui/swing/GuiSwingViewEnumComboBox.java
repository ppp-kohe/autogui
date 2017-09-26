package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class GuiSwingViewEnumComboBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyEnumComboBox box = new PropertyEnumComboBox(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), box);
        } else {
            return box;
        }
    }

    public static class PropertyEnumComboBox extends JComboBox<Object>
            implements GuiMappingContext.SourceUpdateListener, ItemListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;

        public PropertyEnumComboBox(GuiMappingContext context) {
            super(getEnumConstants(context));
            this.context = context;

            setRenderer(new PropertyEnumListRenderer(context));

            setEnabled(((GuiReprValueEnumComboBox) context.getRepresentation())
                    .isEditable(context));

            context.addSourceUpdateListener(this);
            update(context, context.getSource());
            addItemListener(this);

            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            PopupExtension ext = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender,menu) -> {
                menu.removeAll();
                menu.add(info);
                menu.revalidate();
            });
            ext.addListenersTo(this);
            setInheritsPopupMenu(true);
        }


        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            Object item = getSelectedItem();
            GuiReprValueEnumComboBox box = (GuiReprValueEnumComboBox) context.getRepresentation();
            box.updateFromGui(context, item);
        }

        @Override
        public Object getSwingViewValue() {
            return getSelectedItem();
        }

        @Override
        public void setSwingViewValue(Object value) {
            setSelectedItem(value);
        }
    }

    public static Object[] getEnumConstants(GuiMappingContext context) {
        Class<?> e = ((GuiReprValueEnumComboBox) context.getRepresentation()).getValueType(context);
        return e.getEnumConstants();
    }

    public static class PropertyEnumListRenderer extends DefaultListCellRenderer {
        protected GuiMappingContext context;

        public PropertyEnumListRenderer(GuiMappingContext context) {
            this.context = context;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 10));
            if (value instanceof Enum<?>) {
                setText(((GuiReprValueEnumComboBox) context.getRepresentation()).getDisplayName(context, (Enum<?>) value));
            }
            return this;
        }
    }
}
