package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValueNumberSpinner;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewNumberSpinner.PropertyNumberSpinner;
import org.autogui.swing.GuiSwingViewNumberSpinner.TypedSpinnerNumberModel;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellRenderer;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.TextCellRenderer;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.Serial;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * a column factory for a {@link Number}.
 *
 * <p>
 *     the renderer is realized by {@link GuiSwingViewLabel.PropertyLabel}.
 *     the editor is realized by {@link PropertyNumberSpinner}.
 */
public class GuiSwingTableColumnNumber implements GuiSwingTableColumn {
    public GuiSwingTableColumnNumber() {}
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        PropertyNumberSpinner editorSpinner = new ColumnEditNumberSpinner(context, valueSpecifier);
        ColumnNumberPane viewerLabel = new ColumnNumberPane(context, valueSpecifier, editorSpinner);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                viewerLabel,
                GuiSwingTableColumn.wrapEditor(editorSpinner))
                .withBorderType(ObjectTableColumnValue.CellBorderType.Spinner)
                .withRowHeight(editorSpinner.getPreferredSize().height)
                .withComparator(new NumberComparator())
                .withValueType(Number.class);
    }

    /**
     * a comparator for comparing numbers */
    public static class NumberComparator implements Comparator<Object> {
        public NumberComparator() {}
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Number && o2 instanceof Number) {
                return GuiReprValueNumberSpinner.compare((Number) o1, (Number) o2);
            }
            return 0;
        }
    }

    public static class ColumnNumberPane extends GuiSwingViewNumberSpinner.PropertyLabelNumber
            implements ObjectTableColumnValue.ColumnViewUpdateSource, ObjectTableColumnValue.ColumnViewUpdateTarget,
            ObjectTableColumn.PopupMenuBuilderSourceForHeader {
        @Serial private static final long serialVersionUID = 1L;
        protected PropertyNumberSpinner editor;

        protected Runnable updater;
        protected int updating;

        @SuppressWarnings("this-escape")
        public ColumnNumberPane(GuiMappingContext context, SpecifierManager specifierManager,
                                PropertyNumberSpinner editor) {
            super(context, specifierManager);
            setBackground(UIManagerUtil.getInstance().getTableBackground());
            TextCellRenderer.setCellDefaultProperties(this);
            this.editor = editor;
            if (editor != null) {
                currentFormat = editor.getModelTyped().getFormat();
                editor.addChangeListener(this::modelUpdated);
            }
            super.init();
        }

        @Override
        public void init() {
            //delayed
        }

        @Override
        public void initModel() {
            //nothing
        }

        @Override
        public TypedSpinnerNumberModel getModelTyped() {
            return editor != null ? editor.getModelTyped() : model;
        }

        public PropertyNumberSpinner getEditor() {
            return editor;
        }

        @Override
        public void setColumnViewUpdater(Runnable updater) {
            this.updater = updater;
        }

        @Override
        public void columnViewUpdateAsDynamic(ObjectTableColumn source) {
            TableCellRenderer renderer = source.getTableColumn().getCellRenderer();
            if (renderer instanceof ObjectTableCellRenderer) {
                JComponent comp = ((ObjectTableCellRenderer) renderer).getComponent();
                if (comp instanceof ColumnNumberPane numPane) {
                    setCurrentFormat(numPane.getCurrentFormat(), numPane.getEditor());
                }
            } else if (source instanceof ObjectTableColumnWithContext) {
                GuiMappingContext context = ((ObjectTableColumnWithContext) source).getContext();
                if (context.getRepresentation() instanceof GuiReprValueNumberSpinner spinner) {
                    setCurrentFormat(spinner.getFormat(), null);
                }
            }
        }

        public void setCurrentFormat(NumberFormat currentFormat, PropertyNumberSpinner updatedEditor) {
            try {
                ++updating;

                if (editor != null && updatedEditor != null) {
                    TypedSpinnerNumberModel updatedModel = updatedEditor.getModelTyped();
                    TypedSpinnerNumberModel myModel = editor.getModelTyped();

                    myModel.setMaximum(updatedModel.getMaximum());
                    myModel.setMinimum(updatedModel.getMinimum());
                    myModel.setStepSize(updatedModel.getStepSize());
                    myModel.setFormatPattern(updatedModel.getFormatPattern());
                } else {
                    GuiReprValueNumberSpinner spinner = getNumberSpinner();
                    if (spinner != null) {
                        spinner.setFormat(currentFormat);
                    }
                }
            } finally {
                --updating;
            }
        }

        @Override
        public void modelUpdated(ChangeEvent e) {
            if (updating <= 0) {
                if (editor != null) {
                    currentFormat = editor.getModelTyped().getFormat();
                }
                if (updater != null) {
                    updater.run();
                }
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                        new GuiSwingTableColumnString.LabelTextClearAction(this, "0"),
                        new GuiSwingTableColumnString.LabelTextLoadAction(this),
                        new GuiSwingTableColumnString.ColumnLabelTextSaveAction(this)
                    ), getEditorActions(), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getHeaderMenuItems(JTable table) {
            return List.of(settingAction);
        }
    }

    public static class ColumnEditNumberSpinner extends PropertyNumberSpinner {
        @Serial private static final long serialVersionUID = 1L;
        /** @since 1.6 */
        protected List<Runnable> finishRunners = new ArrayList<>(1);
        @SuppressWarnings("this-escape")
        public ColumnEditNumberSpinner(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            getEditorField().setBorder(BorderFactory.createEmptyBorder());
            TextCellRenderer.setCellDefaultProperties(getEditorField());
            setBorder(BorderFactory.createEmptyBorder());
            //setBorder(TextCellRenderer.createBorder(4, 0, 0, 0));
            var h = ObjectTableColumnValue.KeyHandlerFinishEditing.installFinishEditingKeyHandler(this, finishRunners);
            for (Component sub : getComponents()) {
                if (sub.isFocusable()) {
                    sub.addKeyListener(h);
                }
            }
            getEditorField().addKeyListener(h);
        }

        @Override
        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            //nothing
        }

        @Override
        public Object getSwingViewValue() {
            try {
                commitEdit();
            } catch (ParseException e) {
                //e.printStackTrace();
            }
            return super.getSwingViewValue();
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            finishRunners.add(eventHandler);
            super.addSwingEditFinishHandler(eventHandler);
        }

        @Override
        protected void paintComponent(Graphics g) {
            var back = getBackground();
            if (back != null) {
                g.setColor(back);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }
}
