package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.type.GuiTypeValue;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.util.List;

public class GuiSwingViewDocumentEditor implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        JComponent text = doc.isStyledDocument(context) ?
                new PropertyDocumentTextPane(context) : new PropertyDocumentEditorPane(context);
        JScrollPane pane = new GuiSwingView.ValueScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (context.isTypeElementProperty()) {
            return new GuiSwingViewPropertyPane.PropertyPane(context, true, pane);
        } else {
            return pane;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static PopupExtension initText(JEditorPane pane, GuiMappingContext context) {
        GuiMappingContext.SourceUpdateListener l = (GuiMappingContext.SourceUpdateListener) pane;
        //context update
        context.addSourceUpdateListener(l);
        //initial update
        l.update(context, context.getSource());

        pane.setPreferredSize(new Dimension(400, 400));

        //popup
        JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
        List<Action> actions = PopupExtensionText.getEditActions(pane);
        PopupExtensionText ext = new PopupExtensionText(pane, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
            menu.accept(info);
            actions.forEach(menu::accept);
            if (pane instanceof ValuePane) {
                GuiSwingJsonTransfer.getActions((ValuePane) pane, context)
                    .forEach(menu::accept);
            }
        });
        pane.setInheritsPopupMenu(true);

        return ext;
    }

    public static void updateText(JEditorPane pane, GuiMappingContext context, Object newValue) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        Document doc = docEditor.toUpdateValue(context, newValue);
        if (pane.getDocument() != doc && doc != null) {
            pane.setDocument(doc);
        }
    }

    public static Object sourceValue(JEditorPane pane, GuiMappingContext context) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        return doc.toSourceValue(context, pane.getDocument());
    }

    public static class PropertyDocumentEditorPane extends JEditorPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyDocumentEditorPane(GuiMappingContext context) {
            this.context = context;
            popup = initText(this, context);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return sourceValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            updateText(this, context, value);
        }

    }

    public static class PropertyDocumentTextPane extends JTextPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyDocumentTextPane(GuiMappingContext context) {
            this.context = context;
            popup = initText(this, context);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return sourceValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            updateText(this, context, value);
        }
    }


}
