package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewFilePathField;
import autogui.swing.GuiSwingViewLabel;
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

        ColumnFilePathPane renderPane = new ColumnFilePathPane(context, valueSpecifier);
        ObjectTableColumn column = new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(renderPane, rowSpecifier),
                new ObjectTableColumnValue.ObjectTableCellEditor(new ColumnEditFilePathPane(context, valueSpecifier), false, rowSpecifier))
                .withComparator(Comparator.comparing(Path.class::cast))
                .withValueType(Path.class)
                .withRowHeight(28);
        renderPane.setTableColumn(column.getTableColumn());
        return column;
    }

    public static class ColumnFilePathPane extends GuiSwingViewLabel.PropertyLabel {
        protected Graphics tester;
        protected TableColumn tableColumn;
        protected SearchTextFieldModelFilePathEmpty filePathModel;
        public ColumnFilePathPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            filePathModel = new SearchTextFieldModelFilePathEmpty();
            setOpaque(true);
        }

        public void setTableColumn(TableColumn tableColumn) {
            this.tableColumn = tableColumn;
        }

        @Override
        public String format(Object value) {
            return getValueAsString(value);
        }

        @Override
        public void setTextWithFormatting(Object value) {
            if (value instanceof Path) {
                SearchTextFieldFilePath.FileItem item = filePathModel.getFileItem((Path) value, null, false);
                if (item != null) {
                    setIcon(item.getIcon());
                }
            } else {
                setIcon(null);
            }
            super.setTextWithFormatting(value);
            if (tableColumn != null && isOver(getText(), tableColumn.getWidth())) {
                if (value instanceof Path) {
                    String name = ((Path) value).getFileName().toString();
                    setText(name);
                }
            }
        }

        public boolean isOver(String text, int width) {
            if (getIcon() != null) {
                width -= getIcon().getIconWidth();
            }
            if (tester == null) {
                tester = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
            }
            if (!text.isEmpty()) {
                TextLayout l = new TextLayout(text, getFont(), tester.getFontMetrics().getFontRenderContext());
                float adv = l.getAdvance() * 1.1f;
                return width <= adv;
            } else {
                return width <= 0;
            }
        }
    }

    /**
     * an editor for a file-path
     */
    public static class ColumnEditFilePathPane extends GuiSwingViewFilePathField.PropertyFilePathPane {
        public ColumnEditFilePathPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager, new SearchTextFieldModelFilePath());
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setLayout(new BorderLayout());
            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            setOpaque(true);
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
