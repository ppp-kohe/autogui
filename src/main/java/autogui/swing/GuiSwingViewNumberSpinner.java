package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.ScheduledTaskRunner;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

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

            PopupExtension ext = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.removeAll();
                menu.add(GuiSwingContextInfo.get().getInfoLabel(context));
            }); //TODO
            ext.addListenersTo(field);
        }

        public static SpinnerNumberModel createModel(GuiMappingContext context) {
            GuiReprValueNumberSpinner repr = (GuiReprValueNumberSpinner) context.getRepresentation();
            SpinnerNumberModel model;
            if (repr.isRealNumberType(context)) {
                model = new SpinnerNumberModel(0.0, null, null, 0.1);
            } else {
                model = new SpinnerNumberModel();
            }
            return model;
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
            setValue(value);
        }
    }
}
