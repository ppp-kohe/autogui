package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;

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
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<String> {
        protected GuiMappingContext context;

        public PropertyTextPane(GuiMappingContext context) {
            this.context = context;
            initLazy();
            getIcon().setVisible(false);

            //editable
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            getField().setEditable(str.isEditable(context));

            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());

            //popup menu
            setInheritsPopupMenu(true);

            //drag drop
            StringTransferHandler h = new StringTransferHandler(this);
            setTransferHandler(h);
            getField().setTransferHandler(h);
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(getField(), DnDConstants.ACTION_COPY, e -> {
                getTransferHandler().exportAsDrag(getField(), e.getTriggerEvent(), TransferHandler.COPY);
            });

        }

        @Override
        public void init() {
            //nothing
        }

        public void initLazy() {
            super.init();
        }

        @Override
        public void updateField(List<Object> events) {
            super.updateField(events);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public List<? extends JComponent> getPopupEditMenuItems() {
            List<JComponent> menus = new ArrayList<>();
            menus.add(GuiSwingContextInfo.get().getInfoLabel(context));
            menus.add(new JMenuItem(new ContextRefreshAction(context)));
            menus.addAll(GuiSwingJsonTransfer.getActionMenuItems(this, context));
            menus.addAll(super.getPopupEditMenuItems());
            menus.add(new HistoryMenu<>(this, getContext()));
            return menus;
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
            SwingUtilities.invokeLater(() -> setSwingViewValue((String) newValue));
        }

        @Override
        public String getSwingViewValue() {
            return getField().getText();
        }

        @Override
        public void setSwingViewValue(String value) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            setTextWithoutUpdateField(str.toUpdateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(String value) {
            getField().setText(value);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            getField().addActionListener(eventHandler::accept);
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    /** handle entire text */
    public static class StringTransferHandler extends TransferHandler {
        protected PropertyTextPane pane;

        public StringTransferHandler(PropertyTextPane pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getField().isEnabled() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return importString(
                        getTransferableAsString(support, DataFlavor.stringFlavor));
            } else {
                return false;
            }
        }

        public String getTransferableAsString(TransferSupport support, DataFlavor flavor) {
            try {
                return (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean importString(String str) {
            if (str != null) {
                pane.setSwingViewValueWithUpdate(str);
                return true;
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
            String text = (String) pane.getSwingViewValue();
            return new StringSelection(text);
        }
    }
}
