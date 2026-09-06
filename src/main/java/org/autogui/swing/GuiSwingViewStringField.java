package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.*;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.Serial;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueStringField}
 *
 * <h2>swing-value</h2>
 * {@link PropertyStringPane#getSwingViewValue()}
 *  : the field text as {@link String}.
 * <p>
 *   updating is caused by field's setText(String) -&gt;
 *      document listener -&gt;
 *      taskRunner -&gt; {@link PropertyStringPane#updateFieldInEvent(boolean, boolean)}.
 *
 * <h2>history-value</h2>
 * supported.
 *
 * <h2>string-transfer</h2>
 * {@link StringTransferHandler},
 *   reading and writing String.
 */
public class GuiSwingViewStringField implements GuiSwingView {
    public GuiSwingViewStringField() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyStringPane field = new PropertyStringPane(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return field.wrapSwingNamed();
        } else {
            return field;
        }
    }

    public static class PropertyStringPane extends SearchTextField
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<String>, GuiSwingPrefsSupports.PreferencesUpdateSupport {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean currentValueSupported = true;
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);
        /** @since 1.9 */ protected SwitchMultilineAction switchMultilineAction;

        /** @since 1.9 */ protected JScrollPane scrollPane;
        /** @since 1.9 */ protected JComponent originalButtonPane;
        /** @since 1.9 */ protected HeightInfo heightInfo = new HeightInfo();
        /** @since 1.9 */ protected JButton resizeButton;
        /** @since 1.9 */ protected boolean multiline;
        /** @since 1.9 */ protected boolean savingPrefsDisabled;
        /** @since 1.9 */ protected Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> prefsUpdater;

        @SuppressWarnings("this-escape")
        public PropertyStringPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            initLazy();
        }

        @Override
        public void init() {
            //nothing
        }

        public void initLazy() {
            initName();
            super.init();
            initNameAfter();
            initEditable();
            initContextUpdate();
            initValue();
            initDragDrop();
            initFieldAction();
            initDefaultDisplayLines();
        }

        @Override
        protected JTextComponent createField() {
            return new TextPaneBuilder(new JEditorPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }

                @Override
                public Dimension getPreferredSize() {
                    return TextPaneBuilder.getPreferredSize(this, true);
                }
            }) {
                @Override
                public void initBorder() { }

                @Override
                protected LineNumberPane createLineNumberPane(JEditorPane pane) {
                    return null;
                }

                @Override
                protected DocumentSettingPaneBase createSettingPane() {
                    return null;
                }

                @Override
                public void initPopup() { }
            }
            .init()
            .getPane();
        }

        /**
         * initializes key-action mappings for swithing single- and multi-lines.
         * @since 1.9
         */
        protected void initFieldAction() {
            String namePrefix = GuiSwingViewStringField.class.getName();
            //enter inserts a new-line if multiline, or causes the button-action if singleline
            var originalEnter = initFieldActionKey(KeyEvent.VK_ENTER, 0, DefaultEditorKit.insertBreakAction,
                    (oa, e) -> actionMultiline(isMultiline(), oa, null, e, editingRunner));

            //shift+enter causes the buttion-action if multiline, or inserts a new-line if singleline
            initFieldActionKey(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, namePrefix + ".insert-break-shift",
                    (oa, e) -> actionMultiline(!isMultiline(), oa, originalEnter, e, editingRunner));

            //alt+enter always causes the button-action
            initFieldActionKey(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK, namePrefix + ".insert-break-alt",
                    (oa, e) -> actionMultiline(isMultiline(), oa, editingRunner, e, editingRunner));

            //tab inserts a tab if multiline, or transfers focus if singleline
            var originalTab = initFieldActionKey(KeyEvent.VK_TAB, 0, DefaultEditorKit.insertTabAction,
                    (oe, e) -> actionMultiline(isMultiline(), oe, null, e, ae -> field.transferFocus()));

            //shift+tab always transfers focus backward
            initFieldActionKey(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK, namePrefix + ".insert-tab-shift",
                    (oa, e) -> actionMultiline(isMultiline(), oa, ae -> field.transferFocusBackward(), e, ae -> field.transferFocusBackward()));

            //alt+tab transfers focus if multiline, or inserts a tab if singleline
            initFieldActionKey(KeyEvent.VK_TAB, KeyEvent.ALT_DOWN_MASK, namePrefix + ".insert-tab-alt",
                    (oa, e) -> actionMultiline(!isMultiline(), oa, originalTab, e, ae -> field.transferFocus()));
        }

        /**
         * @param keyCode the key-code
         * @param mask the mask for key-code or 0
         * @param actionName the action-name
         * @param action the new-action takeing an original-action and an action-event
         * @return the original action for the actionName or null
         * @since 1.9
         */
        protected Action initFieldActionKey(int keyCode, int mask, String actionName, BiConsumer<Action, ActionEvent> action) {
            var key = KeyStroke.getKeyStroke(keyCode, mask);
            var inputAction = field.getInputMap().get(key);
            if (inputAction == null) {
                inputAction = actionName;
                field.getInputMap().put(key, inputAction);
            }
            var originalEnterAction = field.getActionMap().get(inputAction);
            field.getActionMap().put(inputAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.accept(originalEnterAction, e);
                }
            });
            return originalEnterAction;
        }

        /**
         *
         * @param multiline the flag
         * @param originalAction  used first when the flag is true
         * @param originalActionOpt secondary used when the flag is true; can be null
         * @param e the event for the action
         * @param notAction used when not multiline
         * @since 1.9
         */
        public void actionMultiline(boolean multiline, ActionListener originalAction, ActionListener originalActionOpt, ActionEvent e, ActionListener notAction) {
            if (multiline) {
                if (originalAction != null) {
                    originalAction.actionPerformed(e);
                } else if (originalActionOpt != null) {
                    originalActionOpt.actionPerformed(e);
                }
            } else {
                notAction.actionPerformed(e);
            }
        }

        @Override
        protected void initLayoutField() {
            heightInfo.init(field);

            scrollPane = new JScrollPane(field, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                @Override
                public Dimension getPreferredSize() {
                    return heightInfo.editPreferredSize(super.getPreferredSize());
                }

                @Override
                public void addFocusListener(FocusListener l) {
                    super.addFocusListener(l);
                    getViewport().getView().addFocusListener(l);
                }
            };
            add(scrollPane, BorderLayout.CENTER);
        }

        /**
         * @since 1.9
         */
        public static class HeightInfo implements Cloneable {
            public float height;
            public float rowUnit;
            public int rows;

            public HeightInfo() {}

            public void init(JTextComponent field) {
                rowUnit = field.getFontMetrics(field.getFont()).getHeight();
                setRows(1);
            }

            public Dimension editPreferredSize(Dimension dim) {
                if (dim != null && height > 0) {
                    dim.height = (int) height;
                }
                return dim;
            }

            public float height(int rows) {
                float min = UIManagerUtil.getInstance().getScaledSizeFloat(10);
                return Math.max(rowUnit * rows, min);
            }

            public void setRows(int rows) {
                this.rows = Math.max(1, rows);
                this.height = height(rows);
            }

            public HeightInfo copy() {
                try {
                    return (HeightInfo) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }

            public int rowsForHeightDelta(double delta) {
                return rows + (int) (delta / rowUnit);
            }
        }

        @Override
        protected void initLayoutButtonsPaneAfter() {
            initLayoutButtonsPaneAfterWrap();
            initLayoutButtonsPaneAfterResize();
        }

        /**
         * replace the {@link #buttonsPane} with a new panel for adding a resizing-handle
         * @since 1.9
         */
        protected void initLayoutButtonsPaneAfterWrap() {
            originalButtonPane = buttonsPane;
            initLayoutButtons().forEach(originalButtonPane::add);
            buttonsPane = new JPanel(new BorderLayout());
            var ui = UIManagerUtil.getInstance();
            buttonsPane.setBorder(BorderFactory.createEmptyBorder(0, 0, ui.getScaledSizeInt(5), 0));
            buttonsPane.add(originalButtonPane, BorderLayout.NORTH);
        }

        @Override
        public JComponent getButtonsPane() {
            return originalButtonPane;
        }

        /**
         * setting-up resizeButton for
         * @since 1.9
         */
        protected void initLayoutButtonsPaneAfterResize() {
            resizeButton = new JButton();
            {
                var ui = UIManagerUtil.getInstance();
                var icon1 = GuiSwingIcons.getInstance().getIcon("vertical-", "handle", ui.getScaledSizeInt(16), ui.getScaledSizeInt(16));
                var icon2 = GuiSwingIcons.getInstance().getPressedIcon("vertical-", "handle", ui.getScaledSizeInt(16), ui.getScaledSizeInt(16));
                resizeButton.setIcon(icon1);
                resizeButton.setPressedIcon(icon2);
                resizeButton.setBorder(BorderFactory.createEmptyBorder());
                resizeButton.setContentAreaFilled(false);
                resizeButton.setHorizontalAlignment(SwingConstants.RIGHT);
                resizeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 9));
                resizeButton.setFocusable(false);
                ResizeHeightMouseAdapter.install(this, resizeButton);
            }

            buttonsPane.add(resizeButton, BorderLayout.SOUTH);
            add(buttonsPane,  BorderLayout.EAST);
            setMultiline(false);
        }

        public void initPopup() {
            super.initPopup();
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this, getField(), a -> false);
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this, getIcon(), a -> false);
            setInheritsPopupMenu(true);
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        /**
         * @since 1.9
         */
        public void initNameAfter() {
            getIcon().setVisible(false);
            GuiSwingView.setDescriptionToolTipText(context, this);
            GuiSwingView.setDescriptionToolTipText(context, getField());
        }

        public void initEditable() {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            getField().setEditable(str.isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initDragDrop() {
            StringTransferHandler h = new StringTransferHandler(this);
            setTransferHandlerWithSettingExportingDragSource(h);
        }

        /**
         * @since 1.9
         */
        public void initDefaultDisplayLines() {
            if (context != null && context.isTypeElementProperty()) {
                setDisplayLines(context.getTypeElementAsProperty().getInits().stringField().displayLines());
            }
        }

        /**
         * @param multiline if true it expands the field rows for displaying multiline texts
         * @since 1.9
         */
        public void setMultiline(boolean multiline) {
            updateMultiline(multiline, multiline ? getDisplayLines() : 1);
        }

        /**
         *
         * @param displayLines the lines; if 1&gt; it becomes the multi-lines, otherwise it becomes the single-line;
         * @since 1.9
         */
        public void setDisplayLines(int displayLines) {
            updateMultiline(displayLines > 1, displayLines);
        }

        /**
         * @return the current displaying lines
         * @since 1.9
         */
        public int getDisplayLines() {
            return heightInfo.rows;
        }

        /**
         * actually update changes the display-lines with revalidating this pane.
         *
         * @param multiline the flag
         * @param displayLines the new displaying-lines
         * @since 1.9
         */
        protected void updateMultiline(boolean multiline, int displayLines) {
            var oldMultiline = this.multiline;
            var oldDisplayLines = this.getDisplayLines();
            this.multiline = multiline;
            resizeButton.setVisible(multiline);
            heightInfo.setRows(multiline ? Math.max(2, displayLines) : 1);
            revalidate();
            if (oldMultiline != multiline) {
                switchMultilineAction.updateSelection();
            }
            if ((oldMultiline != multiline || oldDisplayLines != displayLines)
                    && (!savingPrefsDisabled && prefsUpdater != null)) {
                var prefsObj = new PreferencesForStringField();
                prefsObj.set(this);
                prefsUpdater.accept(new GuiSwingPrefsSupports.PreferencesUpdateEvent(context, prefsObj));
            }
        }

        /**
         * @return the object hodling display-lines info.
         * @since 1.9
         */
        public HeightInfo getHeightInfo() {
            return heightInfo;
        }

        /**
         * @return true if the pane displayes multiline texts
         * @since 1.9
         */
        public boolean isMultiline() {
            return multiline;
        }

        @Override
        public void updateField(List<Object> events) {
            super.updateField(events);
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public Supplier<List<PopupCategorized.CategorizedMenuItem>> getMenuItems() {
            return () -> PopupCategorized.getMenuItems(
                    getSwingStaticMenuItems(),
                    getSearchedItems());
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(getMenuItemsSource());
            }
            return menuItems;
        }

        @Override
        public List<Object> getMenuItemsSource() {
            if (menuItemsSource == null) {
                switchMultilineAction = new SwitchMultilineAction(this);

                menuItemsSource = super.getMenuItemsSource();
                menuItemsSource.add(infoLabel);
                menuItemsSource.add(new ContextRefreshAction(context, this));
                menuItemsSource.add(new GuiSwingHistoryMenu<>(this, getSwingViewContext()));
                menuItemsSource.addAll(GuiSwingJsonTransfer.getActions(this, context));
                menuItemsSource.add(switchMultilineAction);
            }
            return menuItemsSource;
        }

        @Override
        public void updateFieldInEvent(boolean modified, boolean immediate) {
            super.updateFieldInEvent(modified, immediate);
            if (modified) {
                updateFromGui(getField().getText(), viewClock.increment());
            }
            if (modified && immediate) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public void updateFieldInEventWithoutEditFinish() {
            super.updateFieldInEvent(true, true);
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingDeferredRunner.invokeLater(() -> setSwingViewValue((String) newValue, contextClock));
        }

        @Override
        public String getSwingViewValue() {
            return getField().getText();
        }

        @Override
        public void setSwingViewValue(String value) {
            viewClock.increment();
            setValueWithoutUpdateField(value);
        }

        private String setValueWithoutUpdateField(String value) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            setTextWithoutUpdateField(str.toUpdateValue(context, value));
            return value;
        }

        @Override
        public void setSwingViewValueWithUpdate(String value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            viewClock.increment();
            updateFromGui(setValueWithoutUpdateField(value), viewClock);
        }

        @Override
        public void setSwingViewValue(String value, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) {
                setValueWithoutUpdateField(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(String value, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) {
                updateFromGui(setValueWithoutUpdateField(value), viewClock);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            //getField().addActionListener(eventHandler::accept);
            editFinishHandlers.add(eventHandler);
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void shutdownSwingView() {
            getEditingRunner().shutdown();
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void requestSwingViewFocus() {
            getField().requestFocusInWindow();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                ValuePane.super.loadSwingPreferences(prefs, options);
                options.apply(this, prefs.getDescendant(getSwingViewContext()));
            } catch (Exception e) {
                GuiLogManager.get().logError(e);
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        /**
         * load the setting-value from the prefs and apply it to the pane
         * @param prefs the source prefs
         * @since 1.9
         */
        public void apply(GuiPreferences prefs) {
            var prefsObj = new PreferencesForStringField();
            this.savingPrefsDisabled = true;
            try {
                prefsObj.loadFrom(prefs);
                prefsObj.applyTo(this);
            } finally {
                this.savingPrefsDisabled = false;
            }
        }


        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                ValuePane.super.saveSwingPreferences(prefs);
                var prefsObj = new PreferencesForStringField();
                prefsObj.set(this);
                prefsObj.saveTo(prefs);
            } catch (Exception e) {
                GuiLogManager.get().logError(e);
            }
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            this.prefsUpdater = updater;
        }
    }

    /** handle entire text */
    public static class StringTransferHandler extends TransferHandler {
        @Serial private static final long serialVersionUID = 1L;
        protected PropertyStringPane pane;

        public StringTransferHandler(PropertyStringPane pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getField().isEnabled() && pane.getField().isEditable() &&
                    pane.isSwingEditable() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor) && canImport(support)) {
                return importString(
                        getTransferableAsString(support, DataFlavor.stringFlavor));
            } else {
                return false;
            }
        }

        public String getTransferableAsString(TransferSupport support, DataFlavor flavor) {
            try {
                return (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean importString(String str) {
            if (str != null) {
                pane.getField().replaceSelection(str);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY | MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            String text = pane.getField().getSelectedText();
            return new StringSelection(text);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == MOVE) {
                pane.getField().replaceSelection("");
            }
        }
    }

    /**
     * @since 1.9
     */
    public static class SwitchMultilineAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemActionCheck {
        protected PropertyStringPane pane;
        @SuppressWarnings("this-escape")
        public SwitchMultilineAction(PropertyStringPane target) {
            this.pane = target;
            putValue(NAME, "Multiline");
            updateSelection();
        }

        public void updateSelection() {
            putValue(SELECTED_KEY, pane.isMultiline());
        }

        public void actionPerformed(ActionEvent e) {
            boolean newValue = !pane.isMultiline();
            pane.setMultiline(newValue);
            putValue(SELECTED_KEY, newValue);
        }
    }

    /**
     * @since 1.9
     */
    public static class ResizeHeightMouseAdapter extends MouseAdapter {
        protected Point startPoint;
        protected PropertyStringPane.HeightInfo startHeight;
        protected float maxHeightDelta;
        protected PropertyStringPane targetComponent;

        public static ResizeHeightMouseAdapter install(PropertyStringPane target, JComponent button) {
            var adapter = new ResizeHeightMouseAdapter(target);
            adapter.installTo(button);
            return adapter;
        }

        public ResizeHeightMouseAdapter(PropertyStringPane targetComponent) {
            this.targetComponent = targetComponent;
        }

        public void installTo(JComponent button) {
            button.addMouseListener(this);
            button.addMouseMotionListener(this);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            startPoint = e.getLocationOnScreen();
            startHeight = targetComponent.getHeightInfo().copy();
            setupMaxHeightDelta();
        }

        protected void setupMaxHeightDelta() {
            maxHeightDelta = UIManagerUtil.getInstance().getScaledSizeFloat(100);
            Component parent = targetComponent;

            float top = 0;
            float height = 0;

            while (parent != null) {
                top += parent.getX();
                if (parent instanceof JRootPane || parent instanceof JWindow || parent.getParent() == null) {
                    height = Math.max(parent.getHeight(), height);
                    break;
                }
                parent = parent.getParent();
            }
            maxHeightDelta = Math.max(height - top, maxHeightDelta);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            startPoint = null;
            startHeight = null;
            targetComponent.revalidate();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (startPoint != null && startHeight != null) {
                var delta = Math.min(maxHeightDelta, e.getLocationOnScreen().getY() - startPoint.getY());
                targetComponent.setDisplayLines(
                        startHeight.rowsForHeightDelta(delta));
            }
        }
    }

    /**
     * @since 1.9
     */
    public static class PreferencesForStringField implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected int displayLines = 0;

        public PreferencesForStringField() {}

        public void applyTo(PropertyStringPane pane) {
            if (displayLines > 0) {
                pane.setDisplayLines(displayLines);
            }
        }

        public void set(PropertyStringPane pane) {
            this.displayLines = pane.getDisplayLines();
        }

        public int getDisplayLines() {
            return displayLines;
        }

        public void setDisplayLines(int displayLines) {
            this.displayLines = displayLines;
        }

        @Override
        public String getKey() {
            return "$stringField";
        }

        @Override
        public Object toJson() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("displayLines", displayLines);
            return map;
        }

        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?, ?> map) {
                displayLines = GuiSwingPrefsSupports.getAs(map, Integer.class, "displayLines", 1);
            }
        }
    }
}
