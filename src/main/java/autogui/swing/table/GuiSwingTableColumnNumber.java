package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewNumberSpinner;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    public static class ColumnNumberPane extends GuiSwingViewLabel.PropertyLabel {
        protected GuiSwingViewNumberSpinner.PropertyNumberSpinner editor;
        protected String currentFormatPattern;
        protected NumberFormat currentFormat;

        public ColumnNumberPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                GuiSwingViewNumberSpinner.PropertyNumberSpinner editor) {
            super(context, specifierManager);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setOpaque(true);
            this.editor = editor;
            if (editor != null) {
                currentFormatPattern = editor.getModelTyped().getFormatPattern();
                currentFormat = editor.getModelTyped().getFormat();
                editor.addChangeListener(this::modelUpdated);
            }
        }

        public void modelUpdated(ChangeEvent e) {
            String fmtPat = editor.getModelTyped().getFormatPattern();
            if (!Objects.equals(fmtPat, currentFormatPattern)) {
                currentFormatPattern = fmtPat;
                currentFormat = editor.getModelTyped().getFormat();
            }
        }

        @Override
        public String format(Object value) {
            if (currentFormat != null) {
                return currentFormat.format(value);
            } else {
                return super.format(value);
            }
        }

        @Override
        public Object getValueFromString(String s) {
            if (currentFormat != null) {
                try {
                    return currentFormat.parseObject(s);
                } catch (Exception ex) {
                    throw new RuntimeException("fail to convert <" + s + ">", ex);
                }
            } else {
                return Double.parseDouble(s);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext()),
                        new GuiSwingView.HistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new GuiSwingTableColumnString.LabelTextPasteAllAction(this),
                        new GuiSwingTableColumnString.LabelTextLoadAction(this),
                        new GuiSwingTableColumnString.LabelTextSaveAction(this),
                        new GuiSwingViewNumberSpinner.NumberMaximumAction(false, editor),
                        new GuiSwingViewNumberSpinner.NumberMaximumAction(true, editor),
                        new GuiSwingViewNumberSpinner.NumberIncrementAction(true, editor),
                        new GuiSwingViewNumberSpinner.NumberIncrementAction(false, editor),
                        editor.getSettingAction()
                    ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }
}
