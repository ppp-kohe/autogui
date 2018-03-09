package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SearchTextField;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
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
            menus.add(new HistoryMenuBuilder(getField(), getContext()).getMenu());
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

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static class HistoryMenuBuilder {
        protected JTextField field;
        protected GuiMappingContext context;
        protected JMenu menu;
        public HistoryMenuBuilder(JTextField field, GuiMappingContext context) {
            this.field = field;
            this.context = context;
        }

        public JMenu getMenu() {
            if (menu == null) {
                menu = buildMenu();
            }
            return menu;
        }

        public JMenu buildMenu() {
            JMenu menu = new JMenu("History");

            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    loadItems();
                }
                @Override
                public void menuDeselected(MenuEvent e) { }
                @Override
                public void menuCanceled(MenuEvent e) { }
            });
            return menu;
        }


        public void clearHistory() {
            context.getPreferences().clearHistories();
        }

        public void loadItems() {
            menu.removeAll();
            boolean added = false;
            List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(context.getPreferences().getHistoryValues());
            Collections.reverse(es);
            for (GuiPreferences.HistoryValueEntry e : es) {
                if (e.getIndex() != -1 && e.getValue() != null) {
                    String strValue = e.getValue().toString();
                    String name = strValue;
                    if (name.length() > 100) {
                        name = name.substring(0, 100) + "...";
                    }
                    menu.add(new JMenuItem(new HistorySetAction(name, strValue, field)));
                    added = true;
                }
            }
            if (!added) {
                JMenuItem nothing = new JMenuItem("Nothing");
                nothing.setEnabled(false);
                menu.add(nothing);
            }

            menu.add(new HistoryClearAction(this));
        }
    }

    public static class HistorySetAction extends AbstractAction {
        protected String value;
        protected JTextField field;

        public HistorySetAction(String name, String value, JTextField field) {
            super(name);
            this.value = value;
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.setText(value);
        }
    }

    public static class HistoryClearAction extends AbstractAction {
        protected HistoryMenuBuilder menu;

        public HistoryClearAction(HistoryMenuBuilder menu) {
            putValue(NAME, "Clear");
            this.menu = menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            menu.clearHistory();
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
                pane.setSwingViewValue(str);
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
