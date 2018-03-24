package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * <h3>representation</h3>
 * {@link GuiReprValueNumberSpinner}
 *
 * <h3>{@link PropertyNumberSpinner#getSwingViewValue()}</h3>
 *  spinner-model's value: {@link Number}, but the value type is Object because
 *     it includes {@link autogui.base.mapping.GuiReprValueNumberSpinner.Infinity}.
 *  <p>
 *     updating is caused by
 *       {@link PropertyNumberSpinner#setValue(Object)} -&gt; change-listener -&gt; taskRunner -&gt;
 *        {@link PropertyNumberSpinner#updateNumber(List)}
 *
 *
 * <h3>history-value</h3>
 *  supported.
 *
 * <h3>string-transfer</h3>
 *  {@link NumberTransferHandler}.
 *   formatted by formatter of {@link PropertyNumberSpinner#getEditorField()}
 *      which is actually a {@link TypedNumberFormatter} with format returned
 *        by {@link GuiReprValueNumberSpinner.NumberType#getFormat()}
 */
public class GuiSwingViewNumberSpinner implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyNumberSpinner spinner = new PropertyNumberSpinner(context);
        if (context.isTypeElementProperty()) {
            return spinner.wrapNamed();
        } else {
            return spinner;
        }
    }

    public static class InfinityNumberSpinner extends JSpinner {
        public InfinityNumberSpinner(TypedSpinnerNumberModel model) {
            super(model);
            setMinimumSize(new Dimension(32, getMinimumSize().height));
        }

        @Override
        protected JComponent createEditor(SpinnerModel model) {
            return new InfinityNumberEditor(this);
        }
    }

    public static class TypedNumberFormatter extends NumberFormatter {
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
        public void setMinimum(Comparable minimum) {
            model.setMinimum(minimum);
        }

        @Override
        public Comparable<?> getMinimum() {
            return model.getMinimum();
        }

        @Override
        public void setMaximum(Comparable max) {
            model.setMaximum(max);
        }

        @Override
        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }

    }

    public static class InfinityNumberEditor extends JSpinner.DefaultEditor {
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
                field.setColumns(Math.min(20, Math.max(min.length(), max.length())));
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
        protected GuiMappingContext context;
        protected ScheduledTaskRunner.EditingRunner editingRunner;
        protected PopupExtensionText popup;
        protected KeyUndoManager undoManager = new KeyUndoManager();
        protected SettingsWindow settingsWindow;

        protected TextServiceDefaultMenuSpinner menu;
        protected TypedSpinnerNumberModelPreferencesUpdater modelPreferencesUpdater;

        public PropertyNumberSpinner(GuiMappingContext context) {
            super(createModel(context));
            this.context = context;

            initModelPreferencesUpdater();

            //value update
            editingRunner = new ScheduledTaskRunner.EditingRunner(500, this::updateNumber);
            addChangeListener(editingRunner);

            //value update: text
            JTextField field = ((DefaultEditor) getEditor()).getTextField();
            field.addActionListener(editingRunner);

            //editable
            if (!((GuiReprValueNumberSpinner) context.getRepresentation()).isEditable(context)) {
                field.setEditable(false);
            }

            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());

            //popup actions
            menu = new TextServiceDefaultMenuSpinner(context, this,
                    (TypedSpinnerNumberModel) getModel(), field);
            menu.getEditActions().addAll(GuiSwingJsonTransfer.getActionMenuItems(this, context));
            menu.getEditActions().add(new HistoryMenu<>(this, context));

            //popup
            PopupExtensionText.putInputEditActions(field);
            PopupExtensionText.putUnregisteredEditActions(field);
            popup = new PopupExtensionText(field, PopupExtension.getDefaultKeyMatcher(), menu);
            setInheritsPopupMenu(true);

            //drag drop
            NumberTransferHandler h = new NumberTransferHandler(this);
            GuiSwingView.setupTransferHandler(this, h);
            GuiSwingView.setupTransferHandler(field, h);

            //undo
            undoManager.putListenersAndActionsTo(field);
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
            return new TypedSpinnerNumberModel(GuiReprValueNumberSpinner.getType(repr.getValueType(context)));
        }

        public void updateNumber(List<Object> events) {
            SwingUtilities.invokeLater(() -> {
                GuiReprValueNumberSpinner field = (GuiReprValueNumberSpinner) context.getRepresentation();
                field.updateFromGui(context, getValue());
            });
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getValue();
        }

        @Override
        public void setSwingViewValue(Object value) {
            editingRunner.setEnabled(false);
            try {
                setValue(value);
            } finally {
                editingRunner.setEnabled(true);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            setValue(value);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            getEditorField().addActionListener(eventHandler::accept);
        }

        public JFormattedTextField getEditorField() {
            return ((DefaultEditor) getEditor()).getTextField();
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void savePreferences(GuiPreferences prefs) {
            GuiSwingView.savePreferencesDefault(this, prefs);
            GuiPreferences targetPrefs = prefs.getDescendant(getContext());
            getModelTyped().saveTo(targetPrefs);
            menu.saveTo(targetPrefs);
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
            GuiPreferences targetPrefs = prefs.getDescendant(getContext());
            getModelTyped().loadFrom(targetPrefs);
            menu.loadFrom(targetPrefs);
            GuiSwingView.loadPreferencesDefault(this, prefs);
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
        public void shutDown() {
            editingRunner.getExecutor().shutdown();
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            menu.setPreferencesUpdater(updater);
            modelPreferencesUpdater.setUpdater(updater);
        }

        @Override
        public void setModel(SpinnerModel model) {
            super.setModel(model);
            if (context != null && model instanceof TypedSpinnerNumberModel) {
                initModelPreferencesUpdater();
            }
        }

        protected void initModelPreferencesUpdater() {
            TypedSpinnerNumberModel m = getModelTyped();
            modelPreferencesUpdater = new TypedSpinnerNumberModelPreferencesUpdater(context, m);
            m.addChangeListener(modelPreferencesUpdater);
        }
    }

    public static class TextServiceDefaultMenuSpinner extends PopupExtensionText.TextServiceDefaultMenu
            implements GuiSwingPreferences.PreferencesUpdateSupport {
        protected GuiMappingContext context;
        protected NumberSettingAction settingAction;

        public TextServiceDefaultMenuSpinner(GuiMappingContext context, SettingsWindowClient settings, TypedSpinnerNumberModel model, JTextComponent textComponent) {
            super(textComponent);
            this.context = context;
            GuiSwingContextInfo info = GuiSwingContextInfo.get();
            editActions.add(0, info.getInfoLabel(context));
            editActions.add(new JMenuItem(new ContextRefreshAction(context)));
            editActions.add(new JPopupMenu.Separator());
            settingAction = new NumberSettingAction(context, settings, info.getInfoLabel(context), model);
            editActions.add(new JMenuItem(settingAction));
            editActions.add(new JMenuItem(new NumberMaximumAction(false, model)));
            editActions.add(new JMenuItem(new NumberMaximumAction(true, model)));
        }

        @Override
        public List<Action> getActionsInInitEditActions(JTextComponent textComponent) {
            return super.getActionsInInitEditActions(textComponent)
                    .stream()
                    .filter(e -> !(e instanceof PopupExtensionText.TextOpenBrowserAction))
                    .collect(Collectors.toList());
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            if (settingAction.getPreferencesUpdater() != null) {
                settingAction.getPreferencesUpdater().setUpdater(updater);
            }
        }

        public void loadFrom(GuiPreferences prefs) {
            if (settingAction.getPreferencesUpdater() != null) {
                settingAction.getPreferencesUpdater().getPrefs().loadFrom(prefs);
            }
        }

        public void saveTo(GuiPreferences prefs) {
            if (settingAction.getPreferencesUpdater() != null) {
                settingAction.getPreferencesUpdater().getPrefs().saveTo(prefs);
            }
        }


    }

    public static class NumberMaximumAction extends AbstractAction {
        protected TypedSpinnerNumberModel model;
        protected boolean max;

        public NumberMaximumAction(boolean max, TypedSpinnerNumberModel model) {
            this.model = model;
            this.max = max;
            putValue(NAME, max ? "Set to Maximum" : "Set to Minimum");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.setValue(max ? model.getNumberMaximum() : model.getNumberMinimum());
        }
    }

    public static class TypedSpinnerNumberModel extends SpinnerNumberModel implements GuiSwingPreferences.Preferences {
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
        @Override
        public Object getNextValue() {
            Object next = numberType.next(getValue(), getStepSize(), 1);
            int r = getMaximum().compareTo(next);
            if (r <= 0) {
                return null;
            } else {
                return next;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        public Object getPreviousValue() {
            Object next = numberType.next(getValue(), getStepSize(), -1);
            int r = getMinimum().compareTo(next);
            if (r > 0) {
                return null;
            } else {
                return next;
            }
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
        public Comparable getMaximum() {
            return (Comparable<?>) toNumber(super.getMaximum(), true);
        }

        @Override
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
                DecimalFormat fmt = new DecimalFormat(formatPattern);
                if (getType().equals(BigInteger.class) || getType().equals(BigDecimal.class)) {
                    fmt.setParseBigDecimal(true);
                }
                return fmt;
            }
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
        protected PropertyNumberSpinner pane;
        public NumberTransferHandler(PropertyNumberSpinner pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getEditorField().isEditValid() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
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

    public static class NumberSettingAction extends AbstractAction {
        protected NumberSettingPane pane;
        protected JPanel contentPane;
        protected SettingsWindowClient client;
        protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;

        public NumberSettingAction(GuiMappingContext context, SettingsWindowClient client, JComponent label, TypedSpinnerNumberModel model) {
            putValue(NAME, "Settings...");
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

        public GuiSwingPreferences.WindowPreferencesUpdater getPreferencesUpdater() {
            return preferencesUpdater;
        }
    }

    public static class NumberSettingPane extends JPanel {
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
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
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

            Dimension spinnerSize = new Dimension(150, 28);
            minSpinner.setPreferredSize(spinnerSize);
            maxSpinner.setPreferredSize(spinnerSize);
            stepSpinner.setPreferredSize(spinnerSize);

            minSpinner.setMinimumSize(new Dimension(100, minSpinner.getMinimumSize().height));
            maxSpinner.setMinimumSize(new Dimension(100, maxSpinner.getMinimumSize().height));
            stepSpinner.setMinimumSize(new Dimension(100, stepSpinner.getMinimumSize().height));

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
    }
}
