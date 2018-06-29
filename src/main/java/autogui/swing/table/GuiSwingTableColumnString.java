package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable.TableTargetColumn;
import autogui.base.mapping.GuiTaskClock;
import autogui.swing.*;
import autogui.swing.GuiSwingView.SpecifierManager;
import autogui.swing.GuiSwingView.SpecifierManagerDefault;
import autogui.swing.GuiSwingViewLabel.PropertyLabel;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupCategorized.CategorizedMenuItem;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * a column factory for {@link String}.
 * <p>
 *     both editor and renderer are realized by a sub-class of {@link PropertyLabel}.
 */
public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ColumnTextPane(context, valueSpecifier),
                new ColumnEditTextPane(context, valueSpecifier))
                    .withComparator(Comparator.comparing(String.class::cast))
                    .withValueType(String.class);
    }

    public static class ColumnTextPane extends PropertyLabel {
        public ColumnTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
        }

        @Override
        public List<CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new LabelTextPasteAllAction(this),
                        new LabelTextLoadAction(this),
                        new ColumnLabelTextSaveAction(this),
                        new PopupExtensionText.TextOpenBrowserAction(this)
                ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class LabelTextPasteAllAction extends PopupExtensionText.TextPasteAllAction
        implements TableTargetColumnAction {
        protected PropertyLabel label;

        public LabelTextPasteAllAction(PropertyLabel label) {
            super(null);
            this.label = label;
        }

        @Override
        public boolean isEnabled() {
            return label != null && label.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(label::setSwingViewValueWithUpdateFromString);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            pasteLines(lines ->
                    target.setSelectedCellValuesLoop(
                            lines.stream()
                                .map(label::getValueFromString)
                                .collect(Collectors.toList())));
        }
    }

    public static class LabelTextLoadAction extends PopupExtensionText.TextLoadAction
            implements TableTargetColumnAction {
        protected PropertyLabel label;

        public LabelTextLoadAction(PropertyLabel label) {
            super(null);
            putValue(NAME, "Load Text...");
            this.label = label;
        }

        @Override
        public boolean isEnabled() {
            return label != null && label.isSwingEditable();
        }

        @Override
        protected JComponent getComponent() {
            return label;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = load();
            if (str == null) {
                return;
            }
            Matcher m = Pattern.compile("\\n").matcher(str);
            if (m.find()) {
                str = str.substring(0, m.start());
            }
            label.setSwingViewValueWithUpdateFromString(str);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            String str = load();
            if (str != null) {
                target.setSelectedCellValuesLoop(
                        Arrays.stream(str.split("\\n"))
                                .map(label::getValueFromString)
                                .collect(Collectors.toList()));
            }
        }
    }

    public static class ColumnLabelTextSaveAction extends GuiSwingViewLabel.LabelTextSaveAction
        implements TableTargetColumnAction {

        public ColumnLabelTextSaveAction(PropertyLabel label) {
            super(label);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            Path path = getPath();
            if (path != null) {
                saveLines(path, target.getSelectedCellValues().stream()
                        .map(label::getValueAsString)
                        .collect(Collectors.toList()));
            }
        }
    }


    /** a component for editor and renderer */
    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyStringPane {
        public ColumnEditTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
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

        @Override
        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            //nothing
        }

        @Override
        public String getSwingViewValue() {
            updateFieldInEventWithoutEditFinish();
            return super.getSwingViewValue();
        }
    }
}
