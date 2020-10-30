package org.autogui.swing.table;

import org.autogui.base.mapping.*;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellEditor;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellRenderer;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItem;
import org.autogui.swing.util.SearchTextField;
import org.autogui.swing.util.SearchTextFieldFilePath;
import org.autogui.swing.util.SearchTextFieldFilePath.SearchTextFieldModelFilePath;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * a column factory for {@link java.io.File} or {@link Path}.
 * <p>
 *     both editor and renderer are realized by a sub-class of
 *     {@link GuiSwingViewFilePathField.PropertyFilePathPane}.
 */
public class GuiSwingTableColumnFilePath implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);

        ColumnFilePathPane renderPane = new ColumnFilePathPane(context, valueSpecifier);
        ObjectTableColumn column = new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableCellRenderer(renderPane, rowSpecifier),
                new ObjectTableCellEditor(new ColumnEditFilePathPane(context, valueSpecifier), false, rowSpecifier))
                .withComparator(new FilePathComparator(context))
                .withValueType(Path.class)
                .withRowHeight(UIManagerUtil.getInstance().getScaledSizeInt(28));
        renderPane.setTableColumn(column.getTableColumn());
        return column;
    }

    /**
     * comparator supporting both File and Path
     * @since 1.1
     */
    public static class FilePathComparator implements Comparator<Object> {
        protected GuiMappingContext context;
        protected GuiReprValueFilePathField filePathField;

        public FilePathComparator(GuiMappingContext context) {
            this.context = context;
            if (context.getRepresentation() instanceof GuiReprValueFilePathField) {
                this.filePathField = (GuiReprValueFilePathField) context.getRepresentation();
            } else {
                this.filePathField = new GuiReprValueFilePathField();
            }
        }

        @Override
        public int compare(Object o1, Object o2) {
            Path p1 = filePathField.toUpdateValue(context, o1);
            Path p2 = filePathField.toUpdateValue(context, o2);
            if (p1 == null) {
                return p2 == null ? 0 : -1;
            } else if (p2 == null) {
                return 1;
            } else {
                return p1.compareTo(p2);
            }
        }
    }

    public static class ColumnFilePathPane extends PropertyLabel {
        private static final long serialVersionUID = 1L;

        protected Graphics tester;
        protected TableColumn tableColumn;
        protected SearchTextFieldModelFilePathEmpty filePathModel;
        public ColumnFilePathPane(GuiMappingContext context, SpecifierManager specifierManager) {
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

        public Icon getValueIcon(Object v) {
            if (v instanceof Path) {
                SearchTextFieldFilePath.FileItem item = filePathModel.getFileItem((Path) v, null, false);
                if (item != null) {
                    return item.getIcon();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public void setTextWithFormatting(Object value) {
            setIcon(getValueIcon(value));
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

        @Override
        public List<CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new ColumnHistoryMenuFilePath(this),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new ColumnFileCopyAction(this),
                        new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                        new GuiSwingTableColumnString.LabelTextLoadAction(this),
                        new GuiSwingTableColumnString.ColumnLabelTextSaveAction(this),
                        new ColumnDesktopOpenAction(this),
                        new ColumnDesktopRevealAction(this),
                        new ColumnOpenDialogAction(this)
                ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class ColumnFileCopyAction extends SearchTextFieldFilePath.FileCopyAllAction {
        private static final long serialVersionUID = 1L;

        protected PropertyLabel view;
        public ColumnFileCopyAction(PropertyLabel view) {
            super(null);
            this.view = view;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = view.getSwingViewValue();
            if (v instanceof Path) {
                run(Collections.singletonList((Path) v));
            }
        }
    }

    public static class ColumnDesktopOpenAction extends SearchTextFieldFilePath.DesktopOpenAction {
        private static final long serialVersionUID = 1L;

        protected PropertyLabel view;
        public ColumnDesktopOpenAction(PropertyLabel view) {
            super(null);
            this.view = view;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = view.getSwingViewValue();
            if (v instanceof Path) {
                run(Collections.singletonList((Path) v));
            }
        }
    }

    public static class ColumnDesktopRevealAction extends SearchTextFieldFilePath.DesktopRevealAction {
        private static final long serialVersionUID = 1L;

        protected PropertyLabel view;
        public ColumnDesktopRevealAction(PropertyLabel view) {
            super(null);
            this.view = view;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = view.getSwingViewValue();
            if (v instanceof Path) {
                run(Collections.singletonList((Path) v));
            }
        }
    }

    public static class ColumnOpenDialogAction extends SearchTextFieldFilePath.OpenDialogAction {
        private static final long serialVersionUID = 1L;

        protected PropertyLabel view;
        public ColumnOpenDialogAction(PropertyLabel view) {
            super(null);
            this.view = view;
        }

        @Override
        public Path getComponentFile() {
            return (Path) view.getSwingViewContext();
        }

        @Override
        public JComponent getDialogComponent() {
            return component;
        }
    }

    public static class ColumnHistoryMenuFilePath extends GuiSwingHistoryMenu<Object, ColumnFilePathPane> {
        private static final long serialVersionUID = 1L;

        public ColumnHistoryMenuFilePath(ColumnFilePathPane view) {
            super(view, view.getSwingViewContext());
        }

        public Icon getIcon(Object v) {
            return component.getValueIcon(v);
        }

        @Override
        public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
            return new ColumnHistoryMenuFilePathForTableColumn(component, target);
        }

        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            Action a = createActionBase(e);
            Icon icon = getIcon(e.getValue());
            a.putValue(Action.SMALL_ICON, icon);
            return a;
        }

        public Action createActionBase(GuiPreferences.HistoryValueEntry e) {
            return new HistorySetAction<>(getActionName(e), e.getValue(), component);
        }
    }

    public static class ColumnHistoryMenuFilePathForTableColumn extends ColumnHistoryMenuFilePath {
        private static final long serialVersionUID = 1L;

        protected GuiReprCollectionTable.TableTargetColumn target;

        public ColumnHistoryMenuFilePathForTableColumn(ColumnFilePathPane view, GuiReprCollectionTable.TableTargetColumn target) {
            super(view);
            this.target = target;
        }

        @Override
        public Action createActionBase(GuiPreferences.HistoryValueEntry e) {
            return new HistorySetForColumnAction<>(getActionName(e), e.getValue(), target);
        }
    }

    /**
     * an editor for a file-path
     */
    public static class ColumnEditFilePathPane extends GuiSwingViewFilePathField.PropertyFilePathPane {
        private static final long serialVersionUID = 1L;

        public ColumnEditFilePathPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager, new SearchTextFieldModelFilePath());
            setCurrentValueSupported(false);
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setLayout(new BorderLayout());
            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            icon.setOpaque(true);
            field.setOpaque(true);
            field.setBorder(BorderFactory.createEmptyBorder());
            setOpaque(true);
        }

        @Override
        public void initBackgroundPainter() {
            backgroundPainter = new SearchBackgroundPainter();
            setBorder(BorderFactory.createEmptyBorder());
            getInsets().set(0, 0, 0, 0);
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
        public boolean isUpdateFieldImmediateEvent(Object e) {
            return super.isUpdateFieldImmediateEvent(e);
        }

        @Override
        public void updateFieldInEvent(boolean modified, boolean immediate) {
            super.updateFieldInEvent(modified, immediate);
        }

        @Override
        public void selectSearchedItemFromModel(CategorizedMenuItem item) {
            super.selectSearchedItemFromModel(item);
        }

        @Override
        public void setSwingViewValue(Object value) {
            super.setSwingViewValue(value);
        }

        @Override
        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            //nothing
        }

        @Override
        public Object getSwingViewValue() {
            //force to update: tab-key's focus lost causes a stopEditing which precedes any other events
            updateFieldInEventWithoutEditFinish();
            return super.getSwingViewValue();
        }
    }

    /**
     * a dummy editor for just rending a file-path
     */
    public static class SearchTextFieldModelFilePathEmpty extends SearchTextFieldModelFilePath {
        @Override
        public boolean isBackgroundTask() {
            return false;
        }

        @Override
        public List<CategorizedMenuItem> getCandidates(String text, boolean editable, SearchTextField.SearchTextFieldPublisher publisher) {
            setSelection(text);
            return new ArrayList<>();
        }
    }
}
