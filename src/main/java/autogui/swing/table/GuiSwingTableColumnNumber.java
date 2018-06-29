package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.base.mapping.GuiTaskClock;
import autogui.swing.*;
import autogui.swing.GuiSwingView.SpecifierManager;
import autogui.swing.GuiSwingView.SpecifierManagerDefault;
import autogui.swing.GuiSwingViewNumberSpinner.PropertyNumberSpinner;
import autogui.swing.GuiSwingViewNumberSpinner.TypedSpinnerNumberModel;
import autogui.swing.table.ObjectTableColumnValue.ObjectTableCellRenderer;
import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * a column factory for a {@link Number}.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     the editor is realized by {@link PropertyNumberSpinner}.
 */
public class GuiSwingTableColumnNumber implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        PropertyNumberSpinner spinner = new ColumnEditNumberSpinner(context, valueSpecifier);
        ColumnNumberPane label = new ColumnNumberPane(context, valueSpecifier, spinner);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                label,
                spinner)
                .withRowHeight(spinner.getPreferredSize().height)
                .withComparator(new NumberComparator())
                .withValueType(Number.class);
    }

    /**
     * a comparator for comparing numbers */
    public static class NumberComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Number && o2 instanceof Number) {
                return GuiReprValueNumberSpinner.compare((Number) o1, (Number) o2);
            }
            return 0;
        }
    }

    public static class ColumnNumberPane extends GuiSwingViewNumberSpinner.PropertyLabelNumber
            implements ObjectTableColumnValue.ColumnViewUpdateSource, ObjectTableColumnValue.ColumnViewUpdateTarget {
        protected PropertyNumberSpinner editor;

        protected Runnable updater;
        protected int updating;

        public ColumnNumberPane(GuiMappingContext context, SpecifierManager specifierManager,
                                PropertyNumberSpinner editor) {
            super(context, specifierManager);
            setOpaque(true);
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
                if (comp instanceof ColumnNumberPane) {
                    ColumnNumberPane numPane = (ColumnNumberPane) comp;
                    setCurrentFormat(numPane.getCurrentFormat(), numPane.getEditor());
                }
            } else if (source instanceof ObjectTableColumnWithContext) {
                GuiMappingContext context = ((ObjectTableColumnWithContext) source).getContext();
                if (context.getRepresentation() instanceof GuiReprValueNumberSpinner) {
                    GuiReprValueNumberSpinner spinner = (GuiReprValueNumberSpinner) context.getRepresentation();
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
                        new GuiSwingTableColumnString.LabelTextLoadAction(this),
                        new GuiSwingTableColumnString.ColumnLabelTextSaveAction(this)
                    ), getEditorActions(), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class ColumnEditNumberSpinner extends PropertyNumberSpinner {
        public ColumnEditNumberSpinner(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            getEditorField().setBorder(BorderFactory.createEmptyBorder());
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
    }
}
