package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewFilePathField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.SearchTextField;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GuiSwingTableColumnFilePath implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {

        return new ObjectTableColumnValue(context,
                new ObjectTableColumnValue.ObjectTableCellRenderer(new ColumnEditFilePath(context, false)),
                new ObjectTableColumnValue.ObjectTableCellEditor(new ColumnEditFilePath(context, true)))
                .withComparator(Comparator.comparing(Path.class::cast));
    }

    public static class ColumnEditFilePath extends GuiSwingViewFilePathField.PropertyFilePathPane {
        protected boolean editor;
        public ColumnEditFilePath(GuiMappingContext context, boolean editor) {
            super(context, editor ?
                    new SearchTextFieldModelFilePath() :
                    new SearchTextFieldModelFilePathEmpty());
            this.editor = editor;
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setLayout(new BorderLayout());
            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            setOpaque(true);
            if (!editor) {
                getField().setBorder(BorderFactory.createEmptyBorder());
                getField().setOpaque(true);
                getIcon().setOpaque(true);
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
            icon.setBackground(bg);
            field.setBackground(bg);
        }

        @Override
        public void setForeground(Color fg) {
            super.setForeground(fg);
            icon.setForeground(fg);
            field.setForeground(fg);
        }

        @Override
        public void selectSearchedItemFromModel(PopupCategorized.CategorizedPopupItem item) {
            super.selectSearchedItemFromModel(item);
        }
    }

    public static class SearchTextFieldModelFilePathEmpty extends SearchTextFieldFilePath.SearchTextFieldModelFilePath {
        @Override
        public boolean isBackgroundTask() {
            return false;
        }

        @Override
        public List<PopupCategorized.CategorizedPopupItem> getCandidates(String text, boolean editable, SearchTextField.SearchTextFieldPublisher publisher) {
            setSelection(text);
            return new ArrayList<>();
        }
    }
}
