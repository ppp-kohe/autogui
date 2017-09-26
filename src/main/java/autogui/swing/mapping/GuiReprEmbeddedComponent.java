package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;

import javax.swing.*;

public class GuiReprEmbeddedComponent extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return JComponent.class.isAssignableFrom(cls);
    }

    @Override
    public Object getUpdatedValue(GuiMappingContext context, boolean executeParent) throws Exception {
        return new GuiReprValueDocumentEditor.SwingInvoker(() -> super.getUpdatedValue(context, executeParent)).run();
    }

    @Override
    public void updateFromGui(GuiMappingContext context, Object newValue) {
        new GuiReprValueDocumentEditor.SwingInvoker(() -> {
            super.updateFromGui(context, newValue);
            return null;
        }).runNoError();
    }
}
