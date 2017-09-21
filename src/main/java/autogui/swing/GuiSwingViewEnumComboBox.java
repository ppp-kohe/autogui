package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.swing.util.NamedPane;

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
            implements GuiMappingContext.SourceUpdateListener, ItemListener {
        protected GuiMappingContext context;

        public PropertyEnumComboBox(GuiMappingContext context) {
            super(getEnumConstants(context));
            this.context = context;

            setRenderer(new PropertyEnumListRenderer(context));

            context.addSourceUpdateListener(this);
            update(context, context.getSource());
        }

        public static Object[] getEnumConstants(GuiMappingContext context) {
            Class<?> e = ((GuiReprValueEnumComboBox) context.getRepresentation()).getValueType(context);
            return e.getEnumConstants();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            setSelectedItem(newValue);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            Object item = getSelectedItem();
            GuiReprValueEnumComboBox box = (GuiReprValueEnumComboBox) context.getRepresentation();
            box.updateFromGui(context, item);
        }
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
