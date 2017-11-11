package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;

public class GuiSwingViewLabel implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyLabel label = new PropertyLabel(context);
        if (context.isTypeElementProperty()) {
            return label.wrapNamed();
        } else {
            return label;
        }
    }

    public static class PropertyLabel extends JLabel
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected Object value;
        protected PopupExtension popup;

        public PropertyLabel(GuiMappingContext context) {
            this.context = context;
            putClientProperty("html.disable", Boolean.TRUE);
            setMinimumSize(new Dimension(100, 20));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            PopupExtensionText.TextOpenBrowserAction browserAction = new PopupExtensionText.TextOpenBrowserAction(this);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                menu.accept(browserAction);
            });
            setInheritsPopupMenu(true);

            //drag
            setTransferHandler(new LabelTransferHandler(this));
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, e -> {
                getTransferHandler().exportAsDrag(this, e.getTriggerEvent(), TransferHandler.COPY);
            });
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
            return value;
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();
            this.value = value;
            setText("" + label.toUpdateValue(context, value));
        }

        public String getValueAsString() {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();;
            return "" + label.toUpdateValue(context, getSwingViewValue());
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static class LabelTransferHandler extends  TransferHandler {
        protected PropertyLabel pane;

        public LabelTransferHandler(PropertyLabel pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(pane.getValueAsString());
        }
    }

}

