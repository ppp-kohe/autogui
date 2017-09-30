package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.ScheduledTaskRunner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.*;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;

public class GuiSwingViewNumberSpinner implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyNumberSpinner spinner = new PropertyNumberSpinner(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), spinner);
        } else {
            return spinner;
        }
    }

    public static class InfinityNumberSpinner extends JSpinner {
        public InfinityNumberSpinner(TypedSpinnerNumberModel model) {
            super(model);
        }

        @Override
        protected JComponent createEditor(SpinnerModel model) {
            return new InfinityNumberEditor(this);
        }

    }

    public static class InfinityNumberFormatter extends NumberFormatter {
        protected SpinnerNumberModel model;

        public InfinityNumberFormatter(NumberFormat format, SpinnerNumberModel model) {
            super(format);
            this.model = model;
        }

        @Override
        public void setMinimum(Comparable minimum) {
            model.setMinimum(minimum);
        }

        @Override
        public Comparable<?> getMinimum() {
            return new InfinityComparable(model.getMinimum());
        }

        @Override
        public void setMaximum(Comparable max) {
            model.setMaximum(max);
        }

        @Override
        public Comparable<?> getMaximum() {
            return new InfinityComparable(model.getMaximum());
        }
    }

    public static class InfinityComparable implements Comparable<Object> {
        protected Comparable<?> value;
        protected GuiReprValueNumberSpinner.NumberType numberType;

        public InfinityComparable(Comparable<?> value) {
            this.value = value;
            if (value instanceof Number) {
                numberType = GuiReprValueNumberSpinner.getType(value.getClass());
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(Object o) {
            if (value instanceof GuiReprValueNumberSpinner.Infinity) {
                return ((GuiReprValueNumberSpinner.Infinity) value).compareTo(o);
            } else if (o instanceof Number) {
                GuiReprValueNumberSpinner.NumberType common = GuiReprValueNumberSpinner.getCommonType(
                        numberType,
                        GuiReprValueNumberSpinner.getType(o.getClass()));
                return ((Comparable) common.convert(value)).compareTo(common.convert(o));

            }
            return 0;
        }
    }

    public static class InfinityNumberEditor extends JSpinner.DefaultEditor {
        public InfinityNumberEditor(JSpinner spinner) {
            super(spinner);
            DecimalFormat df = new DecimalFormat();
            df.setParseBigDecimal(true);
            InfinityDecimalFormat format = new InfinityDecimalFormat(df);
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            InfinityNumberFormatter formatter = new InfinityNumberFormatter(format, model);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField field = getTextField();
            field.setEditable(true);
            field.setFormatterFactory(factory);
            field.setHorizontalAlignment(JTextField.RIGHT);
            try {
                String max = formatter.valueToString(model.getMaximum());
                String min = formatter.valueToString(model.getMinimum());
                field.setColumns(Math.max(max.length(), max.length()));
            } catch (ParseException ex) {
                System.err.println("ex: " + ex);
                //nothing
            }
        }

        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel) getSpinner().getModel();
        }

        public InfinityDecimalFormat getFormat() {
            return (InfinityDecimalFormat) ((InfinityNumberFormatter) getTextField().getFormatter()).getFormat();
        }
    }

    public static class InfinityDecimalFormat extends NumberFormat {
        protected DecimalFormat format;

        public InfinityDecimalFormat(DecimalFormat format) {
            this.format = format;
        }

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            return format.format(number, toAppendTo, pos);
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return format.format(number, toAppendTo, pos);
        }

        @Override
        public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
            if (number instanceof GuiReprValueNumberSpinner.Infinity) {
                toAppendTo.append(number.toString());
                return toAppendTo;
            }  else {
                return format.format(number, toAppendTo, pos);
            }
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return format.parse(source, parsePosition);
        }
    }


    public static class PropertyNumberSpinner extends InfinityNumberSpinner
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected ScheduledTaskRunner.EditingRunner editingRunner;
        protected PopupExtensionText popup;

        public PropertyNumberSpinner(GuiMappingContext context) {
            super(createModel(context));
            this.context = context;
            editingRunner = new ScheduledTaskRunner.EditingRunner(500, this::updateNumber);

            addChangeListener(editingRunner);

            JTextField field = ((DefaultEditor) getEditor()).getTextField();
            field.addActionListener(editingRunner);

            context.addSourceUpdateListener(this);
            update(context, context.getSource());

            popup = new PopupExtensionText(field, PopupExtension.getDefaultKeyMatcher(),
                    new TextServiceDefaultMenuSpinner(context, (TypedSpinnerNumberModel) getModel(), field));
            setInheritsPopupMenu(true);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public static TypedSpinnerNumberModel createModel(GuiMappingContext context) {
            GuiReprValueNumberSpinner repr = (GuiReprValueNumberSpinner) context.getRepresentation();
            return new TypedSpinnerNumberModel(repr.getType(repr.getValueType(context)));
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
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            getEditorField().addActionListener(eventHandler::accept);
        }

        public JFormattedTextField getEditorField() {
            return ((DefaultEditor) getEditor()).getTextField();
        }

    }

    public static class TextServiceDefaultMenuSpinner extends PopupExtensionText.TextServiceDefaultMenu {
        protected GuiMappingContext context;
        protected NumberSettingPane settingPane;

        public TextServiceDefaultMenuSpinner(GuiMappingContext context, TypedSpinnerNumberModel model, JTextComponent textComponent) {
            super(textComponent);
            this.context = context;
            editActions.add(0, GuiSwingContextInfo.get().getInfoLabel(context));

            settingPane = new NumberSettingPane(model);
            editActions.add(settingPane);
        }

    }

    public static class TypedSpinnerNumberModel extends SpinnerNumberModel {
        protected GuiReprValueNumberSpinner.NumberType numberType;
        protected int depth;
        protected Object extendedValue;

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

        @Override
        public Object getNextValue() {
            return numberType.next(getValue(), getStepSize(), 1);
        }


        @Override
        public Object getPreviousValue() {
            return numberType.next(getValue(), getStepSize(), -1);
        }


        @Override
        public void setValue(Object value) {
            if (extendedValue == null || !extendedValue.equals(value)) {
                extendedValue = value;
                fireStateChanged();
            }
        }

        @Override
        public Object getValue() {
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
        public Comparable<?> getMaximum() {
            return toNumber(super.getMaximum(), true);
        }

        @Override
        public Comparable<?> getMinimum() {
            return toNumber(super.getMinimum(), false);
        }

        protected Comparable<?> toNumber(Comparable<?> o, boolean max) {
            if (o instanceof Number) {
                return o;
            } else {
                return max ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
        }

        public Comparable<?> getActualMaximum() {
            return super.getMaximum();
        }

        public Comparable<?> getActualMinimum() {
            return super.getMinimum();
        }
    }

    public static class NumberSettingPane extends JPanel {
        protected TypedSpinnerNumberModel model;
        protected JCheckBox minCheckBox;
        protected JCheckBox maxCheckBox;
        protected JSpinner minSpinner;
        protected JSpinner maxSpinner;
        protected JSpinner stepSpinner;

        protected boolean disableChange = false;

        public NumberSettingPane(TypedSpinnerNumberModel model) {
            setLayout(new FlowLayout(FlowLayout.LEADING));
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

            Dimension spinnerSize = new Dimension(150, 32);
            minSpinner.setPreferredSize(spinnerSize);
            maxSpinner.setPreferredSize(spinnerSize);
            stepSpinner.setPreferredSize(spinnerSize);

            minSpinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 25));
            maxSpinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 25));
            stepSpinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            add(minCheckBox);
            add(minSpinner);
            add(maxCheckBox);
            add(maxSpinner);
            add(new JLabel("Step:"));
            add(stepSpinner);
            updateFromModel(null);
        }

        public void updateFromModel(ChangeEvent e) {
            disableChange = true;

            updateFromModelField(true);
            updateFromModelField(false);
            stepSpinner.setValue(model.getStepSize());
            disableChange = false;
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
            if (disableChange) {
                return;
            }
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
        }
    }
}
