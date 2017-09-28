package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.swing.util.NamedPane;
import autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiSwingViewStringField implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyTextPane field = new PropertyTextPane(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), field);
        } else {
            return field;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }

    public static class PropertyTextPane extends SearchTextField
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;

        public PropertyTextPane(GuiMappingContext context) {
            this.context = context;
            initLazy();

            getIcon().setVisible(false);

            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            getField().setEditable(str.isEditable(context));
            context.addSourceUpdateListener(this);

            update(context, context.getSource());

            setInheritsPopupMenu(true);
        }

        @Override
        public void init() {
            //nothing
        }

        public void initLazy() {
            super.init();
        }

        @Override
        public List<? extends JComponent> getPopupEditMenuItems() {
            return Stream.concat(Stream.of(GuiSwingContextInfo.get().getInfoLabel(context)),
                    super.getPopupEditMenuItems().stream())
                    .collect(Collectors.toList());
        }

        @Override
        public void updateFieldInEvent(boolean modified) {
            super.updateFieldInEvent(modified);
            if (modified) {
                GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
                str.updateFromGui(context, getField().getText());
            }
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getField().getText();
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            setTextWithoutUpdateField(str.toUpdateValue(context, value));
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            getField().addActionListener(eventHandler::accept);
        }
    }
}
