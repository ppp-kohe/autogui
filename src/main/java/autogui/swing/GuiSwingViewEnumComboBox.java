package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuiSwingViewEnumComboBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyEnumComboBox box = new PropertyEnumComboBox(context);
        if (context.isTypeElementProperty()) {
            return new GuiSwingViewPropertyPane.NamedPropertyPane(context.getDisplayName(), context.getName(), box);
        } else {
            return box;
        }
    }

    public static class PropertyEnumComboBox extends JComboBox<Object>
            implements GuiMappingContext.SourceUpdateListener, ItemListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected boolean listenerEnabled = true;
        protected PopupExtension popup;

        public PropertyEnumComboBox(GuiMappingContext context) {
            super(getEnumConstants(context));
            this.context = context;
            setRenderer(new PropertyEnumListRenderer(context));

            //editable
            setEnabled(((GuiReprValueEnumComboBox) context.getRepresentation())
                    .isEditable(context));

            //update context
            context.addSourceUpdateListener(this);
            //initial context
            update(context, context.getSource());

            addItemListener(this);

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender,menu) -> {
                menu.accept(info);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
            });
            setInheritsPopupMenu(true);

            //popup trigger
            //it supposes that the combo-box has a button that describes popup selection
            Arrays.stream(getComponents())
                    .filter(JButton.class::isInstance)
                    .forEach(c -> c.addMouseListener(popup));

            //drag drop
            EnumTransferHandler h = new EnumTransferHandler(this);
            setTransferHandler(h);
            //TODO does not work properly: need drop target?
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
        public void itemStateChanged(ItemEvent e) {
            if (listenerEnabled) {
                Object item = getSelectedItem();
                GuiReprValueEnumComboBox box = (GuiReprValueEnumComboBox) context.getRepresentation();
                box.updateFromGui(context, item);
            }
        }

        @Override
        public Object getSwingViewValue() {
            return getSelectedItem();
        }

        @Override
        public void setSwingViewValue(Object value) {
            listenerEnabled = false;
            try {
                setSelectedItem(value);
            } finally {
                listenerEnabled = true;
            }
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addItemListener(eventHandler::accept);
        }
    }

    public static Object[] getEnumConstants(GuiMappingContext context) {
        Class<?> e = ((GuiReprValueEnumComboBox) context.getRepresentation()).getValueType(context);
        return e.getEnumConstants();
    }

    public static class PropertyEnumListRenderer extends DefaultListCellRenderer {
        protected GuiMappingContext context;

        public PropertyEnumListRenderer(GuiMappingContext context) {
            this.context = context;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 10));
            if (value instanceof Enum<?>) {
                setText(((GuiReprValueEnumComboBox) context.getRepresentation()).getDisplayName(context, (Enum<?>) value));
            }
            return this;
        }
    }

    public static class EnumTransferHandler extends TransferHandler {
        protected PropertyEnumComboBox pane;

        public EnumTransferHandler(PropertyEnumComboBox pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.isEditable() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        protected Pattern numPattern = Pattern.compile("\\d+");

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String str = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    if (numPattern.matcher(str).matches()) {
                        int ord = Integer.parseInt(str);
                        pane.setSelectedIndex(ord);
                        return true;
                    } else {
                        List<String> names = Arrays.stream(pane.getSelectedObjects())
                                .map(Enum.class::cast)
                                .map(Enum::name)
                                .collect(Collectors.toList());
                        int idx = names.indexOf(str);
                        if (idx >= 0) {
                            pane.setSelectedIndex(idx);
                            return true;
                        } else {
                            String lstr = str.toLowerCase();
                            int lidx = names.stream()
                                    .map(String::toLowerCase)
                                    .collect(Collectors.toList())
                                    .indexOf(lstr);
                            if (lidx >= 0) {
                                pane.setSelectedIndex(lidx);
                                return true;
                            }
                        }
                        return false;
                    }
                } catch (Exception ex) {
                    return false;
                }
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
            Enum e= (Enum) pane.getSwingViewValue();
            if (e != null) {
                String name = e.name();
                return new StringSelection(name);
            } else {
                return null;
            }
        }
    }
}
