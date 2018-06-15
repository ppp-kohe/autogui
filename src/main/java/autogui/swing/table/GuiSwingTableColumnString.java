package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewStringField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingView.HistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new LabelTextPasteAllAction(this),
                        new LabelTextLoadAction(this),
                        new LabelTextSaveAction(this),
                        new PopupExtensionText.TextOpenBrowserAction(this)
                ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    public static class LabelTextPasteAllAction extends PopupExtensionText.TextPasteAllAction
        implements TableTargetColumnAction {
        protected GuiSwingViewLabel.PropertyLabel label;

        public LabelTextPasteAllAction(GuiSwingViewLabel.PropertyLabel label) {
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
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            pasteLines(lines ->
                    target.setSelectedCellValuesLoop(
                            lines.stream()
                                .map(label::getValueFromString)
                                .collect(Collectors.toList())));
        }
    }

    public static class LabelTextLoadAction extends PopupExtensionText.TextLoadAction
            implements TableTargetColumnAction {
        protected GuiSwingViewLabel.PropertyLabel label;

        public LabelTextLoadAction(GuiSwingViewLabel.PropertyLabel label) {
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
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            String str = load();
            if (str != null) {
                target.setSelectedCellValuesLoop(
                        Arrays.stream(str.split("\\n"))
                                .map(label::getValueFromString)
                                .collect(Collectors.toList()));
            }
        }
    }

    public static class LabelTextSaveAction extends PopupExtensionText.TextSaveAction
        implements TableTargetColumnAction {
        protected GuiSwingViewLabel.PropertyLabel label;

        public LabelTextSaveAction(GuiSwingViewLabel.PropertyLabel label) {
            super(null);
            putValue(NAME, "Save Text...");
            this.label = label;
        }

        @Override
        protected JComponent getComponent() {
            return label;
        }

        @Override
        public void save(Path path) {
            saveLines(path, Collections.singletonList(label.getValueAsString()));
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
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
