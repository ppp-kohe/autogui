package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueDocumentEditor;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;

public class GuiSwingViewDocumentEditor implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        JComponent text = doc.isStyledDocument(context) ?
                new PropertyDocumentTextPane(context) : new PropertyDocumentEditorPane(context);
        JScrollPane pane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

    public static void initText(JEditorPane pane, GuiMappingContext context) {
        GuiMappingContext.SourceUpdateListener l = (GuiMappingContext.SourceUpdateListener) pane;
        context.addSourceUpdateListener(l);
        l.update(context, context.getSource());
        pane.setPreferredSize(new Dimension(400, 400));

        PopupExtensionText.installDefault(pane);

        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            pane.setFont(new Font("Menlo", Font.PLAIN, 14));
        }
    }

    public static void updateText(JEditorPane pane, GuiMappingContext context, Object newValue) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        Document doc = docEditor.toUpdateValue(context, newValue);
        if (pane.getDocument() != doc) {
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

        public PropertyDocumentEditorPane(GuiMappingContext context) {
            this.context = context;
            initText(this, context);
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

        public PropertyDocumentTextPane(GuiMappingContext context) {
            this.context = context;
            initText(this, context);
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
