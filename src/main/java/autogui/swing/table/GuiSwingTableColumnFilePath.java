package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewFilePathField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.SearchTextField;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * a column factory for {@link java.io.File} or {@link Path}.
 * <p>
 *     both editor and renderer are realized by a sub-class of
 *     {@link autogui.swing.GuiSwingViewFilePathField.PropertyFilePathPane}.
 */
public class GuiSwingTableColumnFilePath implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);

        ColumnEditFilePath renderPane = new ColumnEditFilePath(context, valueSpecifier,false);
        ObjectTableColumn column = new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(renderPane, rowSpecifier),
                new ObjectTableColumnValue.ObjectTableCellEditor(new ColumnEditFilePath(context, valueSpecifier, true), false, rowSpecifier))
                .withComparator(Comparator.comparing(Path.class::cast))
                .withValueType(Path.class)
                .withRowHeight(28);
        renderPane.setTableColumn(column.getTableColumn());
        return column;
    }

    /**
     * an editor for a file-path
     */
    public static class ColumnEditFilePath extends GuiSwingViewFilePathField.PropertyFilePathPane {
        protected boolean editor;
        protected Graphics tester;
        protected TableColumn tableColumn;

        public ColumnEditFilePath(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager, boolean editor) {
            super(context, specifierManager, editor ?
                    new SearchTextFieldModelFilePath() :
                    new SearchTextFieldModelFilePathEmpty());
            this.editor = editor;
            if (!editor) {
                getField().setEditable(false);
            }
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
        public void selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromModel(item);
        }

        @Override
        public void setSwingViewValue(Object value) {
            super.setSwingViewValue(value);
            updateTextWithColumnWidth();
        }

        public void setTableColumn(TableColumn tableColumn) {
            this.tableColumn = tableColumn;
        }

        public void updateTextWithColumnWidth() {
            if (tableColumn != null && !editor) {
                PopupCategorized.CategorizedMenuItem item = getModel().getSelection();
                if (item instanceof FileItem && isOver(((FileItem) item).getPath().toString(), tableColumn.getWidth())) {
                    String fileName = ((FileItem) item).getPath().getFileName().toString();
                    setTextWithoutUpdateField(fileName);
                }
            }
        }

        public boolean isOver(String text, int width) {
            if (getIcon().getIcon() != null) {
                width -= getIcon().getIcon().getIconWidth();
            }
            if (tester == null) {
                tester = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
            }
            if (!text.isEmpty()) {
                TextLayout l = new TextLayout(text, getField().getFont(), tester.getFontMetrics().getFontRenderContext());
                float adv = l.getAdvance() * 1.1f;
                return width <= adv;
            } else {
                return width <= 0;
            }
        }
    }

    /**
     * a dummy editor for just rending a file-path
     */
    public static class SearchTextFieldModelFilePathEmpty extends SearchTextFieldFilePath.SearchTextFieldModelFilePath {
        @Override
        public boolean isBackgroundTask() {
            return false;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextField.SearchTextFieldPublisher publisher) {
            setSelection(text);
            return new ArrayList<>();
        }
    }
}
