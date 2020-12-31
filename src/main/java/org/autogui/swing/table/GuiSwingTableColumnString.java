package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprCollectionTable.TableTargetColumn;
import org.autogui.base.mapping.GuiReprValueStringField;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItem;
import org.autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                new MultilineColumnTextPane(context, valueSpecifier),
                new MultilineColumnTextPane(context, valueSpecifier).asEditorPane())
//        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
//                new ColumnTextPane(context, valueSpecifier),
//                new ColumnEditTextPane(context, valueSpecifier))
                    .withComparator(Comparator.comparing(String.class::cast))
                    .withValueType(String.class);
    }

    public static class ColumnTextPane extends PropertyLabel {
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
                        Arrays.stream(str.split("\\n", 0))
                                .map(label::getValueFromString)
                                .collect(Collectors.toList()));
            }
        }
    }

    public static class ColumnLabelTextSaveAction extends GuiSwingViewLabel.LabelTextSaveAction
        implements TableTargetColumnAction {
        private static final long serialVersionUID = 1L;

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


    /** a component for editor and renderer (for single-line: switched to {@link MultilineColumnTextPane} since 1.2.1) */
    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyStringPane {
        private static final long serialVersionUID = 1L;
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

    /**
     * cell component with supporting {@link GuiReprValueStringField} instead of the document
     * @since 1.2.1
     */
    public static class MultilineColumnTextPane extends GuiSwingViewDocumentEditor.PropertyDocumentEditorPane {
        private static final long serialVersionUID = 1L;
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);
        public MultilineColumnTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
            init();
        }
        protected void init() {
            initEditorKit();
            initAction();
        }

        protected void initEditorKit() {
            setEditorKit(new MultilineColumnEditorKit());
        }

        protected void initAction() {
            InputMap iMap = getInputMap();
            Action finish = new FinishCellEditAction();
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), finish);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), finish);

            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.insertTabAction);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.insertBreakAction);
        }

        public class FinishCellEditAction extends AbstractAction {
            FinishCellEditAction() {
                putValue(NAME, "FinishCellEdit");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public static class MultilineColumnEditorKit extends DefaultEditorKit implements ViewFactory  {
            @Override
            public Document createDefaultDocument() {
                Document doc = super.createDefaultDocument();
                doc.putProperty(PlainDocument.tabSizeAttribute, 4);
                return doc;
            }

            @Override
            public ViewFactory getViewFactory() {
                return this;
            }

            @Override
            public View create(Element elem) {
                return new MultilineColumnCenterView(elem);
            }
        }

        public static class MultilineColumnCenterView extends WrappedPlainView {
            public MultilineColumnCenterView(Element elem) {
                super(elem);
            }

            @Override
            protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
                super.layoutMajorAxis(targetSpan, axis, offsets, spans);
                int textHeight = (offsets.length == 0 ? 0 :
                        offsets[offsets.length - 1] + spans[spans.length - 1]);
                //adjust Y starting point if the textHeight is smaller than the cell height (targetSpan)
                if (targetSpan > textHeight) {
                    int startY = (targetSpan - textHeight) / 2;
                    IntStream.range(0, offsets.length)
                            .forEach(i -> offsets[i] += startY);
                }
            }
        }

        public static class MultilineColumnScrollPane extends GuiSwingViewWrapper.ValueScrollPane {
            public MultilineColumnScrollPane(Component view) {
                super(view,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            }

            @Override
            public void requestSwingViewFocus() {
                getViewport().getView().requestFocusInWindow(); //focusing directly by clicking
            }
        }

        public MultilineColumnScrollPane asEditorPane() {
            return new MultilineColumnScrollPane(this);
        }

        @Override
        protected void installLineNumberPane() {
            //no line numbering
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public Object getSwingViewValue() {
            return getText();
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            setSwingViewValueWithUpdate(value);
            requestFocusInWindow();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            setText(str.toUpdateValue(context, value));
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            editFinishHandlers.add(eventHandler);
        }
    }

}
