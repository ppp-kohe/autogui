package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewNumberSpinner;
import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.text.NumberFormat;
import java.util.*;

/**
 * a column factory for a {@link Number}.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     the editor is realized by {@link autogui.swing.GuiSwingViewNumberSpinner.PropertyNumberSpinner}.
 */
public class GuiSwingTableColumnNumber implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        GuiSwingViewNumberSpinner.PropertyNumberSpinner spinner = new GuiSwingViewNumberSpinner.PropertyNumberSpinner(context, valueSpecifier);
        spinner.getEditorField().setBorder(BorderFactory.createEmptyBorder());
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

    public static class ColumnNumberPane extends GuiSwingViewLabel.PropertyLabel
            implements ObjectTableColumnValue.ColumnViewUpdateSource, ObjectTableColumnValue.ColumnViewUpdateTarget {
        protected GuiSwingViewNumberSpinner.PropertyNumberSpinner editor;
        protected NumberFormat currentFormat;

        protected Runnable updater;
        protected int updating;

        public ColumnNumberPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                GuiSwingViewNumberSpinner.PropertyNumberSpinner editor) {
            super(context, specifierManager);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setOpaque(true);
            this.editor = editor;
            if (editor != null) {
                currentFormat = editor.getModelTyped().getFormat();
                editor.addChangeListener(this::modelUpdated);
            }
            super.initKeyBindingsForStaticMenuItems();
        }

        public GuiSwingViewNumberSpinner.PropertyNumberSpinner getEditor() {
            return editor;
        }

        @Override
        public void initKeyBindingsForStaticMenuItems() {
            //delay
        }

        public NumberFormat getCurrentFormat() {
            return currentFormat;
        }

        @Override
        public void setColumnViewUpdater(Runnable updater) {
            this.updater = updater;
        }

        @Override
        public void columnViewUpdateAsDynamic(ObjectTableColumn source) {
            TableCellRenderer renderer = source.getTableColumn().getCellRenderer();
            if (renderer instanceof ObjectTableColumnValue.ObjectTableCellRenderer) {
                JComponent comp = ((ObjectTableColumnValue.ObjectTableCellRenderer) renderer).getComponent();
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

        public void setCurrentFormat(NumberFormat currentFormat, GuiSwingViewNumberSpinner.PropertyNumberSpinner updatedEditor) {
            try {
                ++updating;

                if (editor != null && updatedEditor != null) {
                    GuiSwingViewNumberSpinner.TypedSpinnerNumberModel updatedModel = updatedEditor.getModelTyped();
                    GuiSwingViewNumberSpinner.TypedSpinnerNumberModel myModel = editor.getModelTyped();

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

        public void modelUpdated(ChangeEvent e) {
            if (updating <= 0) {
                if (editor != null) {
                    NumberFormat fmt = editor.getModelTyped().getFormat();
                    if (!Objects.equals(fmt, currentFormat)) {
                        currentFormat = fmt;
                    }
                }
                if (updater != null) {
                    updater.run();
                }
            }
        }

        public GuiReprValueNumberSpinner getNumberSpinner() {
            GuiMappingContext context = getSwingViewContext();
            if (context.getRepresentation() instanceof GuiReprValueNumberSpinner) {
                return (GuiReprValueNumberSpinner) context.getRepresentation();
            } else {
                return null;
            }
        }

        @Override
        public String format(Object value) {
            GuiReprValueNumberSpinner spinner = getNumberSpinner();
            if (spinner != null) {
                return spinner.toHumanReadableString(context, value);
            }
            if (currentFormat != null) {
                return currentFormat.format(value);
            } else {
                return super.format(value);
            }
        }

        @Override
        public Object getValueFromString(String s) {
            GuiReprValueNumberSpinner spinner = getNumberSpinner();
            if (spinner != null) {
                return spinner.fromHumanReadableString(context, s);
            }
            if (currentFormat != null) {
                try {
                    return currentFormat.parseObject(s);
                } catch (Exception ex) {
                    return super.getValueFromString(s);
                }
            } else {
                return super.getValueFromString(s);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingView.HistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                        new GuiSwingTableColumnString.LabelTextLoadAction(this),
                        new GuiSwingTableColumnString.LabelTextSaveAction(this)
                    ), getEditorActions(), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }

        public List<Action> getEditorActions() {
            return editor == null ? Collections.emptyList() : Arrays.asList(
                    new GuiSwingViewNumberSpinner.NumberMaximumAction(false, editor),
                    new GuiSwingViewNumberSpinner.NumberMaximumAction(true, editor),
                    new GuiSwingViewNumberSpinner.NumberIncrementAction(true, editor),
                    new GuiSwingViewNumberSpinner.NumberIncrementAction(false, editor),
                    editor.getSettingAction());
        }
    }
}
