package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewFilePathField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.SearchTextField;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import java.awt.*;
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
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(new ColumnEditFilePath(context, valueSpecifier,false), rowSpecifier),
                new ObjectTableColumnValue.ObjectTableCellEditor(new ColumnEditFilePath(context, valueSpecifier, true), false, rowSpecifier))
                .withComparator(Comparator.comparing(Path.class::cast));
    }

    /**
     * an editor for a file-path
     */
    public static class ColumnEditFilePath extends GuiSwingViewFilePathField.PropertyFilePathPane {
        protected boolean editor;
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
