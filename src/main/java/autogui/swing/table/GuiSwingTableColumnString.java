package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewStringField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
                new ColumnTextPane(context, valueSpecifier),
                new ColumnEditTextPane(context, valueSpecifier))
                    .withComparator(Comparator.comparing(String.class::cast))
                    .withValueType(String.class);
    }

    public static class ColumnTextPane extends GuiSwingViewLabel.PropertyLabel {
        public ColumnTextPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext()),
                        new GuiSwingView.HistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingView.ToStringCopyAction(this, context),
                        new LabelTextPasteAllAction(this),
                        new PopupExtensionText.TextOpenBrowserAction(this)
                ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class LabelTextPasteAllAction extends PopupExtensionText.TextPasteAllAction {
        protected GuiSwingViewLabel.PropertyLabel label;

        public LabelTextPasteAllAction(GuiSwingViewLabel.PropertyLabel label) {
            super(null);
            this.label = label;
        }

        @Override
        public boolean isEnabled() {
            return label.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(label::setSwingViewValue);
        }
    }

    /** a component for editor and renderer */
    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyStringPane {
        public ColumnEditTextPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            //getField().setEditable(false);
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setOpaque(true);
            setLayout(new BorderLayout());
            add(field, BorderLayout.CENTER);
            getField().setBorder(BorderFactory.createEmptyBorder());
            getField().setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
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
