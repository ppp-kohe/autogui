package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    public static class PropertyNumberSpinner extends JSpinner
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected ScheduledTaskRunner.EditingRunner editingRunner;

        public PropertyNumberSpinner(GuiMappingContext context) {
            super(createModel(context));
            this.context = context;
            editingRunner = new ScheduledTaskRunner.EditingRunner(500, this::updateNumber);

            addChangeListener(editingRunner);

            JTextField field = ((DefaultEditor) getEditor()).getTextField();
            field.addActionListener(editingRunner);

            context.addSourceUpdateListener(this);
            update(context, context.getSource());


            PopupExtensionText ext = new PopupExtensionText(field, PopupExtension.getDefaultKeyMatcher(),
                    new TextServiceDefaultMenuSpinner(context, (TypedSpinnerNumberModel) getModel(), field));
            ext.addListenersTo(field);
            setInheritsPopupMenu(true);
        }

        public static SpinnerNumberModel createModel(GuiMappingContext context) {
            GuiReprValueNumberSpinner repr = (GuiReprValueNumberSpinner) context.getRepresentation();
            return new TypedSpinnerNumberModel(repr.getValueType(context));
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
            ((JTextField) getEditor()).addActionListener(eventHandler::accept);
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

        @Override
        public void build(PopupExtension sender, JPopupMenu menu) {
            menu.setFocusable(true);
            super.build(sender, menu);
        }
    }

    public static class TypedSpinnerNumberModel extends SpinnerNumberModel {
        protected Class<?> type;
        protected NumberChanger changer;
        protected boolean init = false;

        public TypedSpinnerNumberModel(Class<?> type) {
            this.type = type;
            init = true;
            if (type.equals(Double.class) || type.equals(double.class)) {
                setMinimum(-Double.MAX_VALUE);
                setMaximum(Double.MAX_VALUE);
                setStepSize(0.1);
                setValue((double) 0);
                changer = NumberChangerDefault.ChangerDouble;
            } else if (type.equals(Float.class) || type.equals(float.class)) {
                setMinimum(-Float.MAX_VALUE);
                setMaximum(Float.MAX_VALUE);
                setStepSize(0.1f);
                setValue((float) 0);
                changer = NumberChangerDefault.ChangerFloat;
            } else if (type.equals(Long.class) || type.equals(long.class) || type.equals(AtomicLong.class)) {
                setMinimum(Long.MIN_VALUE);
                setMaximum(Long.MAX_VALUE);
                setStepSize(1);
                setValue((long) 0);
                changer = NumberChangerDefault.ChangerLong;
            } else if (type.equals(Integer.class) || type.equals(int.class) || type.equals(AtomicInteger.class)) {
                setMinimum(Integer.MIN_VALUE);
                setMaximum(Integer.MAX_VALUE);
                setStepSize(1);
                setValue((int) 0);
                changer = NumberChangerDefault.ChangerInteger;
            } else if (type.equals(Short.class) || type.equals(short.class)) {
                setMinimum(Short.MIN_VALUE);
                setMaximum(Short.MAX_VALUE);
                setStepSize(1);
                setValue((short) 0);
                changer = NumberChangerDefault.ChangerShort;
            } else if (type.equals(Byte.class) || type.equals(byte.class)) {
                setMinimum(Byte.MIN_VALUE);
                setMaximum(Byte.MAX_VALUE);
                setStepSize(1);
                setValue((byte) 0);
                changer = NumberChangerDefault.ChangerByte;
            } else if (type.equals(BigInteger.class)) {
                setMinimum(lower);
                setMaximum(upper);
                setStepSize(BigInteger.valueOf(1));
                setValue(BigInteger.valueOf(0));
                changer = NumberChangerDefault.ChangerBigInteger;
            } else if (type.equals(BigDecimal.class)) {
                setMinimum(lower);
                setMaximum(upper);
                setStepSize(BigDecimal.valueOf(0.1));
                setValue(BigDecimal.valueOf(0));
                changer = NumberChangerDefault.ChangerBigDecimal;
            }
            init = false;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public Object getNextValue() {
            //TODO formatting Bound?
            return changer.next(getNumber(), getStepSize(), 1);
        }

        @Override
        public void setValue(Object value) {
            super.setValue(value);
        }

        @Override
        protected void fireStateChanged() {
            if (!init) {
                super.fireStateChanged();
            }
        }

        @Override
        public Object getPreviousValue() {
            return changer.next(getNumber(), getStepSize(), -1);
        }
    }

    public interface NumberChanger {
        Object next(Object prev, Number step, int direction);
    }

    public enum NumberChangerDefault implements NumberChanger {
        ChangerByte {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (byte) (((Byte) prev) + step.byteValue() * direction);
            }
        },
        ChangerShort {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (short) (((Short) prev) + step.shortValue() * direction);
            }
        },
        ChangerInteger {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (int) (((Integer) prev) + step.intValue() * direction);
            }
        },
        ChangerLong {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (long) (((Long) prev) + step.longValue() * direction);
            }
        },

        ChangerFloat {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (float) (((Float) prev) + step.floatValue() * (float) direction);
            }
        },
        ChangerDouble {
            @Override
            public Number next(Object prev, Number step, int direction) {
                return (double) (((Double) prev) + step.doubleValue() * (double) direction);
            }
        },

        ChangerBigInteger {
            @Override
            public Object next(Object prev, Number step, int direction) {
                if (prev instanceof Bound) {
                    return prev;
                }
                return ((BigInteger) step).multiply(BigInteger.valueOf(direction)).add((BigInteger) prev);
            }
        },
        ChangerBigDecimal {
            @Override
            public Object next(Object prev, Number step, int direction) {
                if (prev instanceof Bound) {
                    return prev;
                }
                return ((BigDecimal) step).multiply(BigDecimal.valueOf(direction)).add((BigDecimal) prev);
            }
        }
    }

    public static Bound upper = new Bound(1);
    public static Bound lower = new Bound(-1);

    public static class Bound implements Comparable<Object> {
        protected int n;

        public Bound(int n) {
            this.n = n;
        }

        @Override
        public int compareTo(Object o) {
            return n;
        }

        @Override
        public String toString() {
            return (n < 0 ? "-" : "+") + "\u221e";
        }
    }

    public static class NumberSettingPane extends JPanel {
        protected SpinnerNumberModel model;
        protected JSpinner minSpinner;
        protected JSpinner maxSpinner;
        protected JSpinner stepSpinner;

        protected boolean disableChange = false;

        public NumberSettingPane(TypedSpinnerNumberModel model) {
            setLayout(new FlowLayout(FlowLayout.LEADING));
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            setOpaque(false);
            this.model = model;

            minSpinner = new JSpinner(new TypedSpinnerNumberModel(model.getType()));
            maxSpinner = new JSpinner(new TypedSpinnerNumberModel(model.getType()));
            stepSpinner = new JSpinner(new TypedSpinnerNumberModel(model.getType()));

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

            add(new JLabel("Min:"));
            add(minSpinner);
            add(new JLabel("Max:"));
            add(maxSpinner);
            add(new JLabel("Step:"));
            add(stepSpinner);
            updateFromModel(null);
        }

        public void updateFromModel(ChangeEvent e) {
            disableChange = true;
            Comparable<?> min = model.getMinimum();
            Comparable<?> max = model.getMaximum();
            minSpinner.setValue(min);
            maxSpinner.setValue(max);
            stepSpinner.setValue(model.getStepSize());
            disableChange = false;
        }

        public void updateFromGui(ChangeEvent e) {
            if (disableChange) {
                return;
            }
            Object min = minSpinner.getValue();
            Object max = maxSpinner.getValue();
            Object step = stepSpinner.getValue();
            model.setMinimum((Comparable<?>) min);
            model.setMaximum((Comparable<?>) max);
            model.setStepSize((Number) step);
        }
    }
}
