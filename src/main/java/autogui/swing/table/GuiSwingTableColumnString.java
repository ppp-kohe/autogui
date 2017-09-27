package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewCollectionTable;
import autogui.swing.GuiSwingViewStringField;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        return new GuiSwingViewCollectionTable.ObjectTableColumnValue(context,
                new GuiSwingViewLabel.PropertyLabel(context),
                new ColumnTextPane(context))
                    .withComparator(Comparator.comparing(String.class::cast));
    }

    public static class ColumnTextPane extends GuiSwingViewStringField.PropertyTextPane {
        public ColumnTextPane(GuiMappingContext context) {
            super(context);
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
            field.setBorder(BorderFactory.createEmptyBorder());
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
