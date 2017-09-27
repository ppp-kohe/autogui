package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewStringField;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewLabel.PropertyLabel label = new GuiSwingViewLabel.PropertyLabel(context);
        label.setOpaque(true);
        return new ObjectTableColumnValue(context,
                label,
                new ColumnEditTextPane(context))
                    .withComparator(Comparator.comparing(String.class::cast));
    }

    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyTextPane {
        public ColumnEditTextPane(GuiMappingContext context) {
            super(context);
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setOpaque(true);
            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
        }

        @Override
        public void initBackgroundPainter() {
            backgroundPainter = new SearchBackgroundPainter();
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            field.setBackground(bg);
        }

        @Override
        public void setForeground(Color fg) {
            super.setForeground(fg);
            field.setForeground(fg);
        }
    }
}
