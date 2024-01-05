package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.base.mapping.*;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * a swing view for {@link GuiReprValueNumberSpinner}
 *
 * <h2>swing-value</h2>
 * {@link PropertyNumberSpinner#getSwingViewValue()}:
 *  spinner-model's value as {@link Number}, but the value type is actually Object because
 *     it includes {@link GuiReprValueNumberSpinner.Infinity}.
 *  <p>
 *     updating is caused by
 *       ({@link PropertyNumberSpinner#setValue(Object)} -&gt; change-listener,
 *         focus-lost or action-performed) -&gt; taskRunner -&gt;
 *        {@link PropertyNumberSpinner#updateNumber(List)}
 *
 *
 * <h2>history-value</h2>
 *  supported.
 *
 * <h2>string-transfer</h2>
 *  {@link NumberTransferHandler}.
 *   formatted by formatter of {@link PropertyNumberSpinner#getEditorField()}
 *      which is actually a {@link TypedNumberFormatter} with format returned
 *        by {@link GuiReprValueNumberSpinner.NumberType#getFormat()}
 *
 * <h2>preference</h2>
 *   <pre>
 *       {
 *           "maximum" : String,
 *           "minimum" : String,
 *           "stepSize" : String,
 *           "format" : String
 *       }
 *   </pre>
 *
 *   <code>$settingWindow</code> : {@link GuiSwingPreferences.WindowPreferencesUpdater}
 */
@SuppressWarnings("this-escape")
public class GuiSwingViewNumberSpinner implements GuiSwingView {
    public GuiSwingViewNumberSpinner() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        ValuePane<Object> pane;
        if (!context.getReprValue().isEditable(context)) {
            pane = new PropertyLabelNumber(context, new SpecifierManagerDefault(parentSpecifier));
        } else {
            pane = new PropertyNumberSpinner(context, new SpecifierManagerDefault(parentSpecifier));
        }
        if (context.isTypeElementProperty()) {
            return pane.wrapSwingNamed();
        } else {
            return pane.asSwingViewComponent();
        }
    }

    public static class InfinityNumberSpinner extends JSpinner {
        @Serial private static final long serialVersionUID = 1L;
        public InfinityNumberSpinner(TypedSpinnerNumberModel model) {
            super(model);
            setMinimumSize(new Dimension(UIManagerUtil.getInstance().getScaledSizeInt(32), getMinimumSize().height));
        }

        @Override
        protected JComponent createEditor(SpinnerModel model) {
            return new InfinityNumberEditor(this);
        }
    }

    public static class TypedNumberFormatter extends NumberFormatter {
        @Serial private static final long serialVersionUID = 1L;
        protected SpinnerNumberModel model;

        public TypedNumberFormatter(NumberFormat format, SpinnerNumberModel model) {
            super(format);
            this.model = model;
            if (model instanceof TypedSpinnerNumberModel) {
                setValueClass(((TypedSpinnerNumberModel) model).getType());
            } else {
                setValueClass(BigDecimal.class);
            }
        }


        @Override
        public void setMinimum(Comparable<?> minimum) {
            model.setMinimum(minimum);
        }

        @Override
        public Comparable<?> getMinimum() {
            return model.getMinimum();
        }

        @Override
        public void setMaximum(Comparable<?> max) {
            model.setMaximum(max);
        }

        @Override
        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }

    }

    public static class InfinityNumberEditor extends JSpinner.DefaultEditor {
        @Serial private static final long serialVersionUID = 1L;
        public InfinityNumberEditor(JSpinner spinner) {
            super(spinner);

            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            DefaultFormatterFactory formatterFactory = createFormat(model);

            JFormattedTextField field = getTextField();
            field.setEditable(true);
            field.setFormatterFactory(formatterFactory);
            field.setHorizontalAlignment(JTextField.RIGHT);
            try {
                JFormattedTextField.AbstractFormatter formatter = formatterFactory.getDefaultFormatter();
                String max = formatter.valueToString(model.getMaximum());
                String min = formatter.valueToString(model.getMinimum());
                field.setColumns(Math.min(UIManagerUtil.getInstance().getScaledSizeInt(20), Math.max(min.length(), max.length())));
            } catch (ParseException ex) {
                System.err.println("ex: " + ex);
                //nothing
            }
        }

        public DefaultFormatterFactory createFormat(SpinnerNumberModel model) {
            NumberFormat fmt;
            if (model instanceof TypedSpinnerNumberModel) {
                fmt = ((TypedSpinnerNumberModel) model).getFormat();
            } else {
                fmt = GuiReprValueNumberSpinner.DOUBLE.getFormat();
            }
            TypedNumberFormatter formatter = new TypedNumberFormatter(fmt, model);

            return new DefaultFormatterFactory(formatter);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            getTextField().setFormatterFactory(createFormat(getModel()));
            super.stateChanged(e);
        }

        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel) getSpinner().getModel();
        }
    }

    public static class PropertyNumberSpinner extends InfinityNumberSpinner
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>, SettingsWindowClient,
            GuiSwingPreferences.PreferencesUpdateSupport {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected EditingRunner editingRunner;
        protected PopupExtensionText popup;
        protected KeyUndoManager undoManager = new KeyUndoManager();
        protected SettingsWindow settingsWindow;

        protected TypedSpinnerNumberModelPreferencesUpdater modelPreferencesUpdater;

        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected NumberSettingAction settingAction;
        protected MenuBuilder.MenuLabel infoLabel;

        protected boolean currentValueSupported = true;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);

        public PropertyNumberSpinner(GuiMappingContext context, SpecifierManager specifierManager) {
            super(createModel(context));
            this.context = context;
            this.specifierManager = specifierManager;
            init();
        }

        public void init() {
            initName();
            initEditable();
            initModelPreferencesUpdater();
            initContextUpdate();
            initValue();
            initListener();
            initPopup();
            initDragDrop();
            initUndo();
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initEditable() {
            if (!((GuiReprValueNumberSpinner) context.getRepresentation()).isEditable(context)) {
                getEditorField().setEditable(false);
            }
        }

        public void initModelPreferencesUpdater() {
            TypedSpinnerNumberModel m = getModelTyped();
            modelPreferencesUpdater = new TypedSpinnerNumberModelPreferencesUpdater(context, m);
            m.addChangeListener(modelPreferencesUpdater);
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initListener() {
            editingRunner = new EditingRunner(500, this::updateNumber);
            addChangeListener(editingRunner);
            getEditorField().addActionListener(editingRunner);
            getEditorField().addFocusListener(editingRunner);
        }

        public void initPopup() {
            settingAction = new NumberSettingAction(context, this,
                    GuiSwingContextInfo.get().getInfoLabel(context), getModelTyped());

            JTextField field = getEditorField();
            PopupExtensionText.putInputEditActions(field);
            PopupExtensionText.putUnregisteredEditActions(field);
            popup = new PopupExtensionText(field, PopupExtension.getDefaultKeyMatcher(),
                        new PopupCategorized(this::getSwingStaticMenuItems));
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this, field, a -> false);
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            NumberTransferHandler h = new NumberTransferHandler(this);
            GuiSwingView.setupTransferHandler(this, h);
            GuiSwingView.setupTransferHandler(getEditorField(), h);
        }

        public void initUndo() {
            undoManager.putListenersAndActionsTo(getEditorField());
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
        }

        public NumberSettingAction getSettingAction() {
            return settingAction;
        }

        /**
         * @return the editingRunner for the number
         * @since 1.3
         */
        public EditingRunner getEditingRunner() {
            return editingRunner;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                JTextField field = getEditorField();
                menuItems = PopupCategorized.getMenuItems(
                        PopupExtensionText.getEditActions(field).stream()
                                .filter(e -> !(e instanceof PopupExtensionText.TextOpenBrowserAction) &&
                                             !(e instanceof PopupExtensionText.TextSaveAction) &&
                                             !(e instanceof PopupExtensionText.TextLoadAction) &&
                                             !(e instanceof PopupExtensionText.TextPasteAllAction))
                                .collect(Collectors.toList()),
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new NumberMaximumAction(false, this),
                                new NumberMaximumAction(true, this),
                                new NumberIncrementAction(true, this),
                                new NumberIncrementAction(false, this),
                                new GuiSwingHistoryMenu<>(this, context),
                                new NumberTextPasteAllAction(this),
                                settingAction),
                        GuiSwingJsonTransfer.getActions(this, context));
            }
            return menuItems;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public TypedSpinnerNumberModel getModelTyped() {
            return (TypedSpinnerNumberModel) getModel();
        }

        public static TypedSpinnerNumberModel createModel(GuiMappingContext context) {
            GuiReprValueNumberSpinner repr = (GuiReprValueNumberSpinner) context.getRepresentation();
            return new TypedSpinnerNumberModelWithRepr(context, repr);
        }

        public void updateNumber(List<Object> events) {
            boolean immediate = isImmediate(events);
            if (immediate && SwingDeferredRunner.isEventThreadOrDispatchedFromEventThread()) {
                updateFromGui(getValue(), viewClock.increment());
            } else {
                SwingUtilities.invokeLater(() ->
                        updateFromGui(getValue(), viewClock.increment()));
            }
            if (immediate) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public boolean isImmediate(List<Object> events) {
            return events.stream()
                    .anyMatch(e -> e instanceof ActionEvent);
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public Object getSwingViewValue() {
            return getValue();
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            setValueWithoutUpdate(value);
        }

        private void setValueWithoutUpdate(Object value) {
            editingRunner.setEnabled(false);
            try {
                setValue(value);
            } finally {
                editingRunner.setEnabled(true);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            viewClock.increment();
            setValueWithoutUpdate(value);
            updateFromGui(value, viewClock);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setValueWithoutUpdate(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setValueWithoutUpdate(value);
                updateFromGui(value, viewClock);
            }
        }

        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, value, viewClock);
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            editFinishHandlers.add(eventHandler);
        }

        public JFormattedTextField getEditorField() {
            return ((DefaultEditor) getEditor()).getTextField();
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                getModelTyped().saveTo(targetPrefs);
                settingAction.saveTo(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPreferences.PrefsApplyOptions options) {
            try {
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                getModelTyped().loadFrom(targetPrefs);
                settingAction.loadFrom(targetPrefs);
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingsWindow) {
            this.settingsWindow = settingsWindow;
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow == null ? SettingsWindow.get() : settingsWindow;
        }

        @Override
        public void shutdownSwingView() {
            editingRunner.shutdown();
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            settingAction.setUpdater(updater);
            modelPreferencesUpdater.setUpdater(updater);
        }

        @Override
        public void setModel(SpinnerModel model) {
            super.setModel(model);
            if (context != null && model instanceof TypedSpinnerNumberModel) {
                initModelPreferencesUpdater();
            }
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void requestSwingViewFocus() {
            getEditorField().requestFocusInWindow();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    public static abstract class NumberSetAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemAction, TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected ValuePane<Object> spinner;
        protected TypedSpinnerNumberModel model;

        public NumberSetAction(PropertyNumberSpinner spinner) {
            this(spinner, spinner.getModelTyped());
        }

        public NumberSetAction(ValuePane<Object> spinner, TypedSpinnerNumberModel model) {
            this.spinner = spinner;
            this.model = model;
        }

        @Override
        public boolean isEnabled() {
            return spinner != null && spinner.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setValue(getNumber(spinner.getSwingViewValue()));
        }

        public void setValue(Object n) {
            spinner.setSwingViewValueWithUpdate(n);
        }

        public abstract Object getNumber(Object current);

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            target.setSelectedCellValuesLoop(target.getSelectedCellValues()
                    .stream()
                    .map(this::getNumber)
                    .collect(Collectors.toList()));
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SET;
        }
    }

    public static class NumberTextPasteAllAction extends PopupExtensionText.TextPasteAllAction {
        @Serial private static final long serialVersionUID = 1L;
        protected PropertyNumberSpinner spinner;
        public NumberTextPasteAllAction(PropertyNumberSpinner spinner) {
            super(spinner.getEditorField());
            this.spinner = spinner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(this::pasteNumber);
        }

        public void pasteNumber(String str) {
            try {
                Object val = spinner.getEditorField().getFormatter().stringToValue(str);
                spinner.setSwingViewValueWithUpdate(val);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class NumberMaximumAction extends NumberSetAction {
        @Serial private static final long serialVersionUID = 1L;
        protected boolean max;

        public NumberMaximumAction(boolean max, PropertyNumberSpinner spinner) {
            this(max, spinner, spinner.getModelTyped());
        }

        public NumberMaximumAction(boolean max, ValuePane<Object> spinner, TypedSpinnerNumberModel model) {
            super(spinner, model);
            this.max = max;
            putValue(NAME, max ? "Set Maximum" : "Set Minimum");
        }

        public boolean isMax() {
            return max;
        }

        @Override
        public Object getNumber(Object current) {
            return max ? model.getNumberMaximum() : model.getNumberMinimum();
        }
    }

    public static class NumberIncrementAction extends NumberSetAction {
        @Serial private static final long serialVersionUID = 1L;
        protected boolean inc;

        public NumberIncrementAction(boolean inc, PropertyNumberSpinner spinner) {
            this(inc, spinner, spinner.getModelTyped());
        }

        public NumberIncrementAction(boolean inc, ValuePane<Object> spinner, TypedSpinnerNumberModel model) {
            super(spinner, model);
            this.inc = inc;
            putValue(NAME, inc ? "Increment" : "Decrement");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(inc ? KeyEvent.VK_I : KeyEvent.VK_D,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
        }

        public boolean isInc() {
            return inc;
        }

        @Override
        public Object getNumber(Object current) {
            return inc ? model.getNextValue(current) : model.getPreviousValue(current);
        }
    }

    public static class TypedSpinnerNumberModelWithRepr extends TypedSpinnerNumberModel {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiReprValueNumberSpinner numberSpinner;

        public TypedSpinnerNumberModelWithRepr(GuiMappingContext context, GuiReprValueNumberSpinner numberSpinner) {
            super(numberSpinner.getType(context));
            this.numberSpinner = numberSpinner;
            NumberFormat format = numberSpinner.getFormat();
            if (format instanceof DecimalFormat) {
                formatPattern = ((DecimalFormat) format).toPattern();
            }
        }

        @Override
        public void setFormatPattern(String formatPattern) {
            try {
                DecimalFormat format = createFormat(formatPattern);
                numberSpinner.setFormat(format);
            } catch (Exception ex) {
                //
            }
            super.setFormatPattern(formatPattern);
        }

        @Override
        public NumberFormat getFormat() {
            NumberFormat format = numberSpinner.getFormat();
            if (format == null) {
                format = super.getFormat();
            }
            return format;
        }
    }

    public static class TypedSpinnerNumberModel extends SpinnerNumberModel implements GuiSwingPreferences.Preferences {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiReprValueNumberSpinner.NumberType numberType;
        protected int depth;
        protected Object extendedValue;
        protected String formatPattern;

        public TypedSpinnerNumberModel(GuiReprValueNumberSpinner.NumberType numberType) {
            this.numberType = numberType;
            depth++;
            setMinimum(numberType.getMinimum());
            setMaximum(numberType.getMaximum());
            setStepSize(numberType.getOne());
            setValue(numberType.getZero());
            depth--;
        }

        public GuiReprValueNumberSpinner.NumberType getNumberType() {
            return numberType;
        }

        public Class<?> getType() {
            return numberType.getNumberClass();
        }

        @Override
        protected void fireStateChanged() {
            if (depth == 0) {
                depth++;
                try {
                    super.fireStateChanged();
                } finally {
                    depth--;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public Object getNextValue(Object prev) {
            Object next = numberType.next(prev, getStepSize(), 1);
            int r = getMaximum().compareTo(next);
            if (r <= 0) {
                return null;
            } else {
                return next;
            }
        }

        @SuppressWarnings("unchecked")
        public Object getPreviousValue(Object prev) {
            Object next = numberType.next(prev, getStepSize(), -1);
            int r = getMinimum().compareTo(next);
            if (r > 0) {
                return null;
            } else {
                return next;
            }
        }

        @Override
        public Object getNextValue() {
            return getNextValue(getValue());
        }

        @Override
        public Object getPreviousValue() {
            return getPreviousValue(getValue());
        }


        @Override
        public void setValue(Object value) {
            if (value instanceof Number) {
                value = numberType.convert(value);
            }
            if (extendedValue == null || !extendedValue.equals(value)) {
                extendedValue = value;
                fireStateChanged();
            }
        }

        @Override
        public Object getValue() {
            if (extendedValue == null) {
                return numberType.getZero();
            }
            return extendedValue;
        }

        @Override
        public Number getNumber() {
            if (extendedValue instanceof Number) {
                return (Number) extendedValue;
            } else {
                return 0;
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Comparable getMaximum() {
            return (Comparable<?>) toNumber(super.getMaximum(), true);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Comparable getMinimum() {
            return (Comparable<?>) toNumber(super.getMinimum(), false);
        }

        static BigInteger nayuta = BigInteger.valueOf(10).pow(60);

        protected Number toNumber(Comparable<?> o, boolean max) {
            if (o instanceof Number) {
                return (Number) numberType.convert(o);
            } else {
                Object m = max ? numberType.getMaximum() : numberType.getMinimum();
                if (m instanceof Number) {
                    return (Number) m;
                } else {
                    if (m.equals(GuiReprValueNumberSpinner.MAXIMUM)) {
                        if (numberType instanceof GuiReprValueNumberSpinner.NumberTypeBigInteger) {
                            return nayuta;
                        } else {
                            return new BigDecimal(nayuta);
                        }
                    } else {
                        if (numberType instanceof GuiReprValueNumberSpinner.NumberTypeBigInteger) {
                            return nayuta.multiply(BigInteger.valueOf(-1));
                        } else {
                            return new BigDecimal(nayuta).multiply(BigDecimal.valueOf(-1));
                        }
                    }
                }
            }
        }

        public Comparable<?> getActualMaximum() {
            return super.getMaximum();
        }

        public Comparable<?> getActualMinimum() {
            return super.getMinimum();
        }

        public Number getNumberMaximum() {
            return toNumber(super.getMaximum(), true);
        }

        public Number getNumberMinimum() {
            return toNumber(super.getMinimum(), false);
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore prefsStore = prefs.getValueStore();
            if (getActualMaximum().equals(getNumberType().getMaximum())) {
                prefsStore.remove("maximum");
            } else {
                prefsStore.putString("maximum", getActualMaximum().toString());
            }
            if (getActualMinimum().equals(getNumberType().getMinimum())) {
                prefsStore.remove("minimum");
            } else {
                prefsStore.putString("minimum", getActualMinimum().toString());
            }
            if (getStepSize().equals(getNumberType().getOne())) {
                prefsStore.remove("stepSize");
            } else {
                prefsStore.putString("stepSize", getStepSize().toString());
            }

            if (formatPattern == null) {
                prefsStore.remove("format");
            } else {
                prefsStore.putString("format", formatPattern);
            }
        }

        @Override
        public void loadFrom(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            TypedSpinnerNumberModel model = this;
            GuiReprValueNumberSpinner.NumberType type = model.getNumberType();
            String max = store.getString("maximum", "");
            String min = store.getString("minimum", "");
            String step = store.getString("stepSize", "");
            if (!max.isEmpty()) {
                model.setMaximum(type.fromString(max));
            }
            if (!min.isEmpty()) {
                model.setMinimum(type.fromString(min));
            }
            if (!step.isEmpty()) {
                Comparable<?> c = type.fromString(step);
                if (c instanceof Number) {
                    model.setStepSize((Number) c);
                }
            }

            String format = store.getString("format", "");
            if (!format.isEmpty()) {
                setFormatPattern(format);
            }
        }

        public void setFormatPattern(String formatPattern) {
            this.formatPattern = formatPattern;
            fireStateChanged();
        }

        public String getFormatPattern() {
            return formatPattern;
        }

        public NumberFormat getFormat() {
            if (formatPattern == null) {
                return getNumberType().getFormat();
            } else {
                return createFormat(formatPattern);
            }
        }

        public DecimalFormat createFormat(String formatPattern) {
            DecimalFormat fmt = new DecimalFormat(formatPattern);
            if (getType().equals(BigInteger.class) || getType().equals(BigDecimal.class)) {
                fmt.setParseBigDecimal(true);
            }
            return fmt;
        }

    }

    public static class TypedSpinnerNumberModelPreferencesUpdater implements ChangeListener {
        protected GuiMappingContext context;
        protected TypedSpinnerNumberModel model;

        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater;

        public TypedSpinnerNumberModelPreferencesUpdater(GuiMappingContext context, TypedSpinnerNumberModel model) {
            this.context = context;
            this.model = model;
        }

        public void setUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (updater != null) {
                updater.accept(new GuiSwingPreferences.PreferencesUpdateEvent(context, model));
            }
        }
    }


    public static class NumberTransferHandler extends TransferHandler {
        @Serial private static final long serialVersionUID = 1L;
        protected PropertyNumberSpinner pane;
        public NumberTransferHandler(PropertyNumberSpinner pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getEditorField().isEditValid() && pane.isSwingEditable() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor) && canImport(support)) {
                try {
                    String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Object val = pane.getEditorField().getFormatter().stringToValue(data);
                    pane.setSwingViewValueWithUpdate(val);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            Object val = pane.getSwingViewValue();
            try {
                String str = pane.getEditorField().getFormatter().valueToString(val);
                return new StringSelection(str);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    ///////////////////

    public static class NumberSettingAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction,
        TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;
        protected NumberSettingPane pane;
        protected JPanel contentPane;
        protected SettingsWindowClient client;
        protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;

        public NumberSettingAction(GuiMappingContext context, SettingsWindowClient client, JComponent label, TypedSpinnerNumberModel model) {
            putValue(NAME, "Settings...");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_COMMA,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
            this.client = client;
            pane = new NumberSettingPane(model);
            contentPane = new JPanel(new BorderLayout());
            contentPane.add(label, BorderLayout.NORTH);
            contentPane.add(pane, BorderLayout.CENTER);

            if (context != null) {
                preferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(null,
                        context, "$settingsWindow");
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            client.getSettingsWindow().show("Number Settings", pane, contentPane, preferencesUpdater);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformed(e);
        }

        public NumberSettingPane getPane() {
            return pane;
        }

        public GuiSwingPreferences.WindowPreferencesUpdater getPreferencesUpdater() {
            return preferencesUpdater;
        }

        public void setUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            if (getPreferencesUpdater() != null) {
                getPreferencesUpdater().setUpdater(updater);
            }
        }

        public void loadFrom(GuiPreferences prefs) {
            if (getPreferencesUpdater() != null) {
                getPreferencesUpdater().getPrefs().loadFrom(prefs);
            }
        }

        public void saveTo(GuiPreferences prefs) {
            if (getPreferencesUpdater() != null) {
                getPreferencesUpdater().getPrefs().saveTo(prefs);
            }
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

    public static class NumberSettingPane extends JPanel {
        @Serial private static final long serialVersionUID = 1L;
        protected TypedSpinnerNumberModel model;
        protected JCheckBox minCheckBox;
        protected JCheckBox maxCheckBox;
        protected JSpinner minSpinner;
        protected JSpinner maxSpinner;
        protected JSpinner stepSpinner;

        protected JCheckBox formatCheckBox;
        protected JTextField formatField;

        protected int disableChange;

        public NumberSettingPane(TypedSpinnerNumberModel model) {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(3);
            int w = ui.getScaledSizeInt(10);
            setBorder(BorderFactory.createEmptyBorder(h, w, h, w));
            setOpaque(false);
            this.model = model;

            minCheckBox = new JCheckBox("Min:");
            minCheckBox.addChangeListener(this::updateFromGui);
            maxCheckBox = new JCheckBox("Max:");
            maxCheckBox.addChangeListener(this::updateFromGui);
            minSpinner = new InfinityNumberSpinner(new TypedSpinnerNumberModel(model.getNumberType()));
            maxSpinner = new InfinityNumberSpinner(new TypedSpinnerNumberModel(model.getNumberType()));
            stepSpinner = new InfinityNumberSpinner(new TypedSpinnerNumberModel(model.getNumberType()));

            minSpinner.addChangeListener(this::updateFromGui);
            maxSpinner.addChangeListener(this::updateFromGui);
            stepSpinner.addChangeListener(this::updateFromGui);
            model.addChangeListener(this::updateFromModel);

            Dimension spinnerSize = new Dimension(ui.getScaledSizeInt(150), ui.getScaledSizeInt(28));
            minSpinner.setPreferredSize(spinnerSize);
            maxSpinner.setPreferredSize(spinnerSize);
            stepSpinner.setPreferredSize(spinnerSize);

            int minWidth = ui.getScaledSizeInt(100);

            minSpinner.setMinimumSize(new Dimension(minWidth, minSpinner.getMinimumSize().height));
            maxSpinner.setMinimumSize(new Dimension(minWidth, maxSpinner.getMinimumSize().height));
            stepSpinner.setMinimumSize(new Dimension(minWidth, stepSpinner.getMinimumSize().height));

            formatCheckBox = new JCheckBox("Format:");
            formatCheckBox.addChangeListener(this::updateFromGui);
            formatField = new JTextField("#,###.######");
            formatField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateFormat(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateFormat(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateFormat(e);
                }
            });

            updateFromModel(null);
            new SettingsWindow.LabelGroup(this)
                    .addRow(minCheckBox, minSpinner)
                    .addRow(maxCheckBox, maxSpinner)
                    .addRow("Step:", stepSpinner)
                    .addRow(formatCheckBox, formatField)
                    .fitWidth();
        }

        public void updateFromModel(ChangeEvent e) {
            if (disableChange <= 0) {
                disableChange++;

                updateFromModelField(true);
                updateFromModelField(false);
                stepSpinner.setValue(model.getStepSize());

                String fmt = model.getFormatPattern();
                formatCheckBox.setSelected(fmt != null);
                if (fmt != null) {
                    formatField.setText(fmt);
                }
                disableChange--;
            }
        }

        private void updateFromModelField(boolean max) {
            Comparable<?> val = max ? model.getActualMaximum() : model.getActualMinimum();
            JCheckBox checkBox = max ? maxCheckBox : minCheckBox;
            JSpinner spinner = max ? maxSpinner : minSpinner;
            if (val instanceof Number && !uncheckedBoundValue(max)) {
                spinner.setValue(val);
                spinner.setEnabled(true);
                checkBox.setSelected(true);
            } else {
                if (val instanceof Number) {
                    spinner.setValue(val);
                }
                spinner.setEnabled(false);
                checkBox.setSelected(false);
            }
        }

        private boolean uncheckedBoundValue(boolean max) {
            Object val = max ? model.getActualMaximum() : model.getActualMinimum();
            JCheckBox checkBox = max ? maxCheckBox : minCheckBox;
            Object bound = max ? model.getNumberType().getMaximum() : model.getNumberType().getMinimum();
            if (!checkBox.isSelected()) {
                return val.equals(bound);
            } else {
                return false;
            }
        }

        public void updateFromGui(ChangeEvent e) {
            if (disableChange > 0) {
                return;
            }
            disableChange++;
            Object min;
            if (minCheckBox.isSelected()) {
                minSpinner.setEnabled(true);
                min = minSpinner.getValue();
            } else {
                minSpinner.setEnabled(false);
                min = model.getNumberType().getMinimum();
            }
            Object max;
            if (maxCheckBox.isSelected()) {
                maxSpinner.setEnabled(true);
                max = maxSpinner.getValue();
            } else {
                maxSpinner.setEnabled(false);
                max = model.getNumberType().getMaximum();
            }
            Object step = stepSpinner.getValue();
            model.setMinimum((Comparable<?>) min);
            model.setMaximum((Comparable<?>) max);
            model.setStepSize((Number) step);

            if (formatCheckBox.isSelected()) {
                formatField.setEnabled(true);
                String fmt = formatField.getText();
                if (validFormat(fmt)) {
                    model.setFormatPattern(fmt);
                }
            } else {
                formatField.setEnabled(false);
                model.setFormatPattern(null);
            }
            disableChange--;
        }

        public void updateFormat(DocumentEvent e) {
            if (validFormat(formatField.getText())) {
                formatField.setForeground(Color.black);
                updateFromGui(null);
            } else {
                formatField.setForeground(Color.red);
            }
        }

        public boolean validFormat(String pat) {
            try {
                new DecimalFormat(pat);
                return true;
            } catch (Exception ex){
                return false;
            }
        }

        public TypedSpinnerNumberModel getModel() {
            return model;
        }

        public JCheckBox getMinCheckBox() {
            return minCheckBox;
        }

        public JCheckBox getMaxCheckBox() {
            return maxCheckBox;
        }

        public JSpinner getMinSpinner() {
            return minSpinner;
        }

        public JSpinner getMaxSpinner() {
            return maxSpinner;
        }

        public JSpinner getStepSpinner() {
            return stepSpinner;
        }

        public JCheckBox getFormatCheckBox() {
            return formatCheckBox;
        }

        public JTextField getFormatField() {
            return formatField;
        }
    }

    public static class PropertyLabelNumber extends GuiSwingViewLabel.PropertyLabel
            implements SettingsWindowClient, GuiSwingPreferences.PreferencesUpdateSupport {
        @Serial private static final long serialVersionUID = 1L;
        protected TypedSpinnerNumberModel model;
        protected TypedSpinnerNumberModelPreferencesUpdater modelPreferencesUpdater;
        protected SettingsWindow settingsWindow;
        protected NumberSettingAction settingAction;
        protected NumberFormat currentFormat;

        public PropertyLabelNumber(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        public void init() {
            super.init();
            initModel();
            initModelPreferencesUpdater();
            initSettingAction();
            super.initKeyBindingsForStaticMenuItems();
        }

        @Override
        public void initKeyBindingsForStaticMenuItems() {
            //delay
        }

        public void initModel() {
            model = PropertyNumberSpinner.createModel(context);
            model.addChangeListener(this::modelUpdated);
            currentFormat = model.getFormat();
        }

        public void initModelPreferencesUpdater() {
            TypedSpinnerNumberModel m = getModelTyped();
            if (m != null) {
                modelPreferencesUpdater = new TypedSpinnerNumberModelPreferencesUpdater(context, m);
                m.addChangeListener(modelPreferencesUpdater);
            }
        }

        public void initSettingAction() {
            TypedSpinnerNumberModel m = getModelTyped();
            if (m != null) {
                settingAction = new NumberSettingAction(context, this,
                        GuiSwingContextInfo.get().getInfoLabel(context), m);
            }
        }

        public TypedSpinnerNumberModel getModelTyped() {
            return model;
        }

        public NumberFormat getCurrentFormat() {
            return currentFormat;
        }

        public void modelUpdated(ChangeEvent e) {
            currentFormat = getModelTyped().getFormat();
            setTextWithFormattingCurrentValue();
        }


        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new GuiSwingViewLabel.LabelTextSaveAction(this),
                        new GuiSwingViewLabel.LabelJsonCopyAction(this, context),
                        new GuiSwingViewLabel.LabelJsonSaveAction(this, context),
                        settingAction));
            }
            return menuItems;
        }

        public List<Action> getEditorActions() {
            TypedSpinnerNumberModel m = getModelTyped();
            return Arrays.asList(
                    new NumberMaximumAction(false, this, m),
                    new NumberMaximumAction(true, this, m),
                    new NumberIncrementAction(true, this, m),
                    new NumberIncrementAction(false, this, m),
                    settingAction);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                getModelTyped().saveTo(targetPrefs);
                if (settingAction != null) {
                    settingAction.saveTo(targetPrefs);
                }
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPreferences.PrefsApplyOptions options) {
            try {
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                getModelTyped().loadFrom(targetPrefs);
                if (settingAction != null) {
                    settingAction.loadFrom(targetPrefs);
                }
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingsWindow) {
            this.settingsWindow = settingsWindow;
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow == null ? SettingsWindow.get() : settingsWindow;
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            if (settingAction != null) {
                settingAction.setUpdater(updater);
            }
            if (modelPreferencesUpdater != null) {
                modelPreferencesUpdater.setUpdater(updater);
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
    }
}
