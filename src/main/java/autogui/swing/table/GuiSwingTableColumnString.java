package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewStringField;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

/**
 * a column factory for {@link String}.
 * <p>
 *     both editor and renderer are realized by a sub-class of {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 */
public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ColumnEditTextPane(context, valueSpecifier, false),
                new ColumnEditTextPane(context, valueSpecifier, true))
                    .withComparator(Comparator.comparing(String.class::cast))
                    .withValueType(String.class);
    }

    /** a component for editor and renderer */
    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyStringPane {
        protected boolean editor;
        public ColumnEditTextPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager, boolean editor) {
            super(context, specifierManager);
            this.editor = editor;
            if (!editor) {
                getField().setEditable(false);
            }
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setOpaque(true);
            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
            if (!editor) {
                getField().setBorder(BorderFactory.createEmptyBorder());
                getField().setOpaque(true);
                setBorder(BorderFactory.createEmptyBorder());
            }
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
