package org.autogui.swing.table;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.*;
import org.autogui.base.mapping.GuiReprCollectionTable.TableTargetColumn;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.util.*;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItem;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a column factory for {@link String}.
 * <p>
 *     both editor and renderer are realized by a subclass of {@link PropertyLabel}.
 */
@SuppressWarnings("this-escape")
public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    public GuiSwingTableColumnString() {}
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new MultilineColumnTextViewPane(context, valueSpecifier),
                new MultilineColumnWrappingPane(new MultilineColumnTextPane(context, valueSpecifier).asEditorPane()))
                .withBorderType(ObjectTableColumnValue.CellBorderType.EditorPane)
//        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
//                new ColumnTextPane(context, valueSpecifier),
//                new ColumnEditTextPane(context, valueSpecifier))
                    .withComparator(Comparator.comparing(String.class::cast))
                    .withValueType(String.class);
    }

    /** a component for string cell renderer : only for single-line (switched to {@link MultilineColumnTextViewPane}) */
    public static class ColumnTextPane extends PropertyLabel {
        @Serial private static final long serialVersionUID = 1L;
        public ColumnTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            TextCellRenderer.setCellDefaultProperties(this);
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
                        new LabelTextClearAction(this),
                        new LabelTextLoadAction(this),
                        new ColumnLabelTextSaveAction(this),
                        new PopupExtensionText.TextOpenBrowserAction(this)
                ), GuiSwingJsonTransfer.getActions(this, getSwingViewContext()));
            }
            return menuItems;
        }
    }

    /**
     *
     * @param pane PropertyLabel or text pane
     * @param s the converted string
     * @return a string
     * @since 1.3
     */
    public static Object getValueFromString(GuiSwingView.ValuePane<?> pane, String s) {
        if (pane instanceof PropertyLabel) {
            return ((PropertyLabel) pane).getValueFromString(s);
        } else {
            GuiMappingContext context = pane.getSwingViewContext();
            GuiReprValue repr = context.getReprValue();
            return repr.fromHumanReadableString(context, s);
        }
    }

    /**
     *
     * @param pane PropertyLabel or text pane
     * @param s the converted string
     * @return the generated string
     * @since 1.3
     */
    public static String getValueAsString(GuiSwingView.ValuePane<?> pane, Object s) {
        if (pane instanceof PropertyLabel) {
            return ((PropertyLabel) pane).getValueAsString(s);
        } else {
            GuiMappingContext context = pane.getSwingViewContext();
            GuiReprValue repr = context.getReprValue();
            return repr.toHumanReadableString(context, s);
        }
    }

    /**
     *
     * @param pane PropertyLabel or text pane
     * @param s the value string
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static void setSwingViewValueWithUpdateFromString(GuiSwingView.ValuePane<?> pane, String s) {
        if (pane instanceof PropertyLabel) {
            ((PropertyLabel) pane).setSwingViewValueWithUpdateFromString(s);
        } else {
            ((GuiSwingView.ValuePane<Object>) pane).setSwingViewValueWithUpdate(s);
        }
    }

    public static class LabelTextPasteAllAction extends PopupExtensionText.TextPasteAllAction
        implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> label;

        public LabelTextPasteAllAction(GuiSwingView.ValuePane<?> label) {
            super(null);
            this.label = label;
        }

        @Override
        public boolean isEnabled() {
            return label != null && label.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(s -> setSwingViewValueWithUpdateFromString(label, s));
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            pasteLines(lines ->
                    target.setSelectedCellValuesLoop(
                            lines.stream()
                                .map(s -> getValueFromString(label, s))
                                .collect(Collectors.toList())));
        }
    }

    /**
     * action for clearing the label
     * @since 1.5
     */
    public static class LabelTextClearAction extends PopupExtensionText.TextClearAction
        implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> label;
        protected String clearValue;
        public LabelTextClearAction(GuiSwingView.ValuePane<?> label) {
            this(label, "");
        }
        public LabelTextClearAction(GuiSwingView.ValuePane<?> label, String clearValue) {
            super(null);
            this.label = label;
            this.clearValue = clearValue;
        }

        @Override
        public boolean isEnabled() {
            return label != null && label.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setSwingViewValueWithUpdateFromString(label, getClearValue());
        }

        protected String getClearValue() {
            return clearValue;
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            target.setSelectedCellValuesLoop(
                        Collections.singletonList(getValueFromString(label, getClearValue())));
        }
    }

    public static class LabelTextLoadAction extends PopupExtensionText.TextLoadAction
            implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> label;

        public LabelTextLoadAction(GuiSwingView.ValuePane<?> label) {
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
            return label.asSwingViewComponent();
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
            setSwingViewValueWithUpdateFromString(label, str);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            String str = load();
            if (str != null) {
                target.setSelectedCellValuesLoop(
                        Arrays.stream(str.split("\\n", 0))
                                .map(s -> getValueFromString(label, s))
                                .collect(Collectors.toList()));
            }
        }
    }



    public static class ColumnLabelTextSaveAction extends PopupExtensionText.TextSaveAction
        implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> label;

        public ColumnLabelTextSaveAction(GuiSwingView.ValuePane<?> label) {
            super(null);
            this.label = label;
        }

        @Override
        protected JComponent getComponent() {
            return label.asSwingViewComponent();
        }

        @Override
        public void save(Path path) {
            saveLines(path, Collections.singletonList(getValueAsString(label, label.getSwingViewValue())));
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            Path path = getPath();
            if (path != null) {
                saveLines(path, target.getSelectedCellValues().stream()
                        .map(s -> getValueAsString(label, s))
                        .collect(Collectors.toList()));
            }
        }
    }


    /** a component for editor (this is only for single-line. switched to {@link MultilineColumnTextPane} since 1.2.1) */
    public static class ColumnEditTextPane extends GuiSwingViewStringField.PropertyStringPane {
        @Serial private static final long serialVersionUID = 1L;
        public ColumnEditTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            //getField().setEditable(false);
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            TextCellRenderer.setCellDefaultProperties(this);
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
     * cell component for displaying multiline texts
     * @since 1.3
     */
    public static class MultilineColumnTextViewPane extends MultilineColumnTextPane {
        @Serial private static final long serialVersionUID = 1;
        public MultilineColumnTextViewPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
        }

        @Override
        public List<CategorizedMenuItem> getSwingStaticMenuItems() {
            List<CategorizedMenuItem> items = new ArrayList<>(super.getSwingStaticMenuItems());

            items.addAll(PopupCategorized.getMenuItems(Arrays.asList(
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new LabelTextLoadAction(this),
                        new ColumnLabelTextSaveAction(this)
                )));
            return items;
        }
    }

    /**
     * cell component with supporting {@link GuiReprValueStringField} instead of the document
     * @since 1.3
     */
    public static class MultilineColumnTextPane extends GuiSwingViewDocumentEditor.PropertyDocumentEditorPane
        implements ObjectTableColumn.PopupMenuBuilderSourceForHeader, GuiSwingPrefsSupports.PreferencesUpdateSupport {
        @Serial private static final long serialVersionUID = 1L;
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);
        /** @since 1.8 */
        protected boolean editFinishByEnterAndKey = false;
        /** @since 1.8 */
        protected boolean editor;
        /** @since 1.8 */
        protected Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> prefsUpdater;
        /** @since 1.8 */
        protected SwitchEnterBehaviorAction editFinishAction;

        public MultilineColumnTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            TextCellRenderer.setCellDefaultProperties(this);
            setBorder(BorderFactory.createEmptyBorder());
            init();
        }

        protected void init() {
            initEditorKit();
            initDefault();
            initAction();
        }

        protected void initEditorKit() {
            setEditorKit(new MultilineColumnEditorKit());
        }

        /**
         * setting {@link #editFinishByEnterAndKey} from the inits annotaiton attached to the context property
         * @since 1.8
         */
        protected void initDefault() {
            if (context.isTypeElementProperty()) {
                editFinishByEnterAndKey = context.getTypeElementAsProperty().getInits().tableColumnString().editFinishByEnterAndKey();
            }
        }

        protected void initAction() {
            if (editFinishByEnterAndKey) {
                switchActionFinishByEnterWithKey();
            } else {
                switchActionFinishByEnter();
            }
            editFinishAction = new SwitchEnterBehaviorAction(this);
        }

        protected void switchActionFinishByEnter() {
            InputMap iMap = getInputMap();
            Action finish = new FinishCellEditAction(editFinishHandlers);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.insertBreakAction);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.insertTabAction);

            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), DefaultEditorKit.insertBreakAction);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.ALT_DOWN_MASK), DefaultEditorKit.insertTabAction);

            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), finish);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), finish);
        }

        protected void switchActionFinishByEnterWithKey() {
            InputMap iMap = getInputMap();
            Action finish = new FinishCellEditAction(editFinishHandlers);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), finish);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), finish);

            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), finish);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.ALT_DOWN_MASK), finish);

            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), DefaultEditorKit.insertTabAction);
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), DefaultEditorKit.insertBreakAction);
        }

        /**
         * @param table the target table
         * @return a menu for switching enter-key behavior
         * @since 1.8
         */
        @Override
        public List<CategorizedMenuItem> getHeaderMenuItems(JTable table) {
            return editor ? List.of(editFinishAction) : List.of();
        }

        public boolean isEditFinishByEnterAndKey() {
            return editFinishByEnterAndKey;
        }

        public void setEditFinishByEnterAndKey(boolean editFinishByEnterAndKey) {
            this.editFinishByEnterAndKey = editFinishByEnterAndKey;
            initAction();
        }

        /**
         * update the {@link #isEditFinishByEnterAndKey()} with sending call-back
         * @param editFinishByEnterAndKey the new flag
         * @since 1.8
         */
        public void setEditFinishByEnterAndKeyAsUserAction(boolean editFinishByEnterAndKey) {
            setEditFinishByEnterAndKey(editFinishByEnterAndKey);
            if (prefsUpdater != null) {
                prefsUpdater.accept(new GuiSwingPrefsSupports.PreferencesUpdateEvent(context, createCurrentPrefsMultilineColumn()));
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            super.loadSwingPreferences(prefs, options);
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
                options.loadFromAndApplyTo(this, prefs.getDescendant(context));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        public void loadFromAndApplyTo(GuiPreferences p) {
            var prefObj = new PreferencesForMultilineColumn();
            prefObj.loadFrom(p);
            if (prefObj.hasEditFinishByEnterAndKey()) {
                setEditFinishByEnterAndKey(prefObj.isEditFinishByEnterAndKey());
            }
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                createCurrentPrefsMultilineColumn().saveTo(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        private PreferencesForMultilineColumn createCurrentPrefsMultilineColumn() {
            var prefObj = new PreferencesForMultilineColumn();
            prefObj.setEditFinishByEnterAndKey(isEditFinishByEnterAndKey());
            return prefObj;
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            this.prefsUpdater = updater;
        }

        /**
         * @since 1.8
         */
        public static class SwitchEnterBehaviorAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemActionCheck {
            protected MultilineColumnTextPane pane;
            public SwitchEnterBehaviorAction(MultilineColumnTextPane pane) {
                this.pane = pane;
                var os = UIManagerUtil.getInstance().getOsVersion();
                putValue(NAME, "Finish Editing By " + os.getKeyStrokeString(0, KeyEvent.VK_ENTER) +
                        " Instead of " + os.getKeyStrokeString(KeyEvent.ALT_DOWN_MASK, KeyEvent.VK_ENTER));
                setSelectedFromPane();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                var b = !pane.isEditFinishByEnterAndKey();
                pane.setEditFinishByEnterAndKeyAsUserAction(b);
                setSelectedFromPane();
            }

            protected void setSelectedFromPane() {
                putValue(SELECTED_KEY, !pane.isEditFinishByEnterAndKey());
            }

            @Override
            public String getCategory() {
                return PopupExtension.MENU_CATEGORY_PREFS;
            }

            @Override
            public String getSubCategory() {
                return PopupExtension.MENU_SUB_CATEGORY_PREFS_WINDOW;
            }
        }

        public static class FinishCellEditAction extends AbstractAction {
            @Serial private static final long serialVersionUID = 1;
            List<Runnable> editFinishHandlers;

            public FinishCellEditAction(List<Runnable> editFinishHandlers) {
                putValue(NAME, "FinishCellEdit");
                this.editFinishHandlers = editFinishHandlers;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public static class MultilineColumnEditorKit extends DefaultEditorKit implements ViewFactory  {
            @Serial private static final long serialVersionUID = 1;
            /** @since 1.6 */
            protected boolean verticalCentering;

            public MultilineColumnEditorKit() {
                this(false);
            }

            /**
             * @param verticalCentering centering if true
             * @since 1.6
             */
            public MultilineColumnEditorKit(boolean verticalCentering) {
                this.verticalCentering = verticalCentering;
            }

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
            /** @since 1.6 */
            protected boolean verticalCentering;

            public MultilineColumnCenterView(Element elem) {
                this(elem, false);
            }

            /**
             * @param elem the element
             * @param verticalCentering centering if true
             * @since 1.6
             */
            public MultilineColumnCenterView(Element elem, boolean verticalCentering) {
                super(elem);
                this.verticalCentering = verticalCentering;
            }

            @Override
            public void paint(Graphics g, Shape a) {
                //re-painting background for fixing custom background against SynthEditorPaneUI
                if (getElement() == getDocument().getDefaultRootElement()) {
                    paintRootBackground(g);
                }
                super.paint(g, a);
            }

            private void paintRootBackground(Graphics g) {
                Container c = getContainer();
                Dimension s = c.getSize();
                g.setColor(c.getBackground());
                g.fillRect(0, 0, s.width, s.height);
            }

            @Override
            protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
                super.layoutMajorAxis(targetSpan, axis, offsets, spans);
                if (verticalCentering) {
                    layoutMajorAxisCentering(targetSpan, axis, offsets, spans);
                }
            }

            /**
             * @param targetSpan span
             * @param axis axis
             * @param offsets offsets modified
             * @param spans spans
             * @since 1.6
             */
            protected void layoutMajorAxisCentering(int targetSpan, int axis, int[] offsets, int[]spans) {
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

        public static class MultilineColumnScrollPane extends GuiSwingViewWrapper.ValueScrollPane<Object> {
            @Serial private static final long serialVersionUID = 1L;
            public MultilineColumnScrollPane(Component view) {
                super(view,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                setBorder(BorderFactory.createEmptyBorder());
                TextCellRenderer.setCellDefaultProperties(this);
            }

            public MultilineColumnTextPane getColumnTextPane() {
                return (MultilineColumnTextPane) getSwingViewWrappedPane();
            }

            @Override
            public void requestSwingViewFocus() {
                getViewport().getView().requestFocusInWindow(); //focusing directly by clicking
            }
        }

        public MultilineColumnScrollPane asEditorPane() {
            editor = true;
            return new MultilineColumnScrollPane(this);
        }

        @Override
        protected void installLineNumberPane() {
            //no line numbering
        }

        @Override
        public List<CategorizedMenuItem> getSwingStaticMenuItems() {
            List<CategorizedMenuItem> items = new ArrayList<>(super.getSwingStaticMenuItems());
            items.add(new GuiSwingHistoryMenu<>(this, getSwingViewContext()));
            if (editor) {
                items.add(editFinishAction);
            }
            return items;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingDeferredRunner.invokeLater(() -> setSwingViewValue(newValue, contextClock));
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

    /**
     *  preferences for a multiline text field column;
     *   {"editFinishByEnterAndKey": false}
     * @since 1.8
     */
    public static class PreferencesForMultilineColumn implements GuiSwingPrefsSupports.PreferencesByJsonEntry  {
        Boolean editFinishByEnterAndKey;
        public PreferencesForMultilineColumn() {}
        @Override
        public String getKey() {
            return "$multilineColumn";
        }

        public void setEditFinishByEnterAndKey(boolean editFinishByEnterAndKey) {
            this.editFinishByEnterAndKey = editFinishByEnterAndKey;
        }

        public boolean isEditFinishByEnterAndKey() {
            return editFinishByEnterAndKey != null && editFinishByEnterAndKey;
        }

        /**
         * @return ture if {@link #setJson(Object)} has no property for it
         */
        public boolean hasEditFinishByEnterAndKey() {
            return editFinishByEnterAndKey != null;
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("editFinishByEnterAndKey", isEditFinishByEnterAndKey());
            return map;
        }

        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                editFinishByEnterAndKey = GuiSwingPrefsSupports.getAs(map, Boolean.class, "editFinishByEnterAndKey", null);
            }
        }
    }

    /**
     * a wrapper pane for scroll+editor cell.
     *    the JScrollPane seems that it cannot fill background color for the empty border.
     *    The class solve this by wrapping the scroll pane with the border.
     * @since 1.6
     */
    public static class MultilineColumnWrappingPane extends GuiSwingViewWrapper.ValueWrappingPane<Object> {
        public MultilineColumnWrappingPane(Component view) {
            super(view);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(true);
        }

        @Override
        public void requestSwingViewFocus() {
            getSwingViewWrappedPane().requestSwingViewFocus();
        }
    }
}
