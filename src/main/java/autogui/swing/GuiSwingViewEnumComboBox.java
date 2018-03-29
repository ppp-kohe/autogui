package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.swing.util.PopupCategorized;
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * <h3>representation</h3>
 * {@link GuiReprValueEnumComboBox}
 *
 * <h3>{@link PropertyEnumComboBox#getSwingViewValue()}</h3>
 * the selected {@link Enum} member.
 *
 * <p>
 *     updating is caused by {@link PropertyEnumComboBox#setSelectedItem(Object)} -&gt;
 *      item-listener: {@link PropertyEnumComboBox#itemStateChanged(ItemEvent)}
 *
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * {@link EnumTransferHandler}.
 *  reading {@link Enum#name()} or {@link Enum#ordinal()}, and writing {@link Enum#name()}.
 *  @see GuiReprValueEnumComboBox#getEnumValue(GuiMappingContext, String)
 */
public class GuiSwingViewEnumComboBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyEnumComboBox box = new PropertyEnumComboBox(context);
        if (context.isTypeElementProperty()) {
            return box.wrapNamed();
        } else {
            return box;
        }
    }

    public static class PropertyEnumComboBox extends JComboBox<Object>
            implements GuiMappingContext.SourceUpdateListener, ItemListener, GuiSwingView.ValuePane<Object> { //Enum
        protected GuiMappingContext context;
        protected boolean listenerEnabled = true;
        protected PopupExtension popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

        public PropertyEnumComboBox(GuiMappingContext context) {
            super(getEnumConstants(context));
            this.context = context;
            init();
        }

        public void init() {
            initName();
            initRenderer();
            initEditable();
            initContextUpdate();
            initValue();
            initListener();
            initPopup();
            initDragDrop();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initRenderer() {
            setRenderer(new PropertyEnumListRenderer(context));
        }

        public void initEditable() {
            setEnabled(((GuiReprValueEnumComboBox) context.getRepresentation())
                    .isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource());
        }

        public void initListener() {
            addItemListener(this);
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            setInheritsPopupMenu(true);
            //it supposes that the combo-box has a button that describes popup selection
            Arrays.stream(getComponents())
                    .filter(JButton.class::isInstance)
                    .forEach(c -> c.addMouseListener(popup));
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new EnumTransferHandler(this));
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(GuiSwingContextInfo.get().getInfoLabel(context),
                                new ContextRefreshAction(context),
                                new HistoryMenu<>(this, context)),
                        GuiSwingJsonTransfer.getActions(this, context));
            }
            return null;
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
                if (box.isEditable(context)) {
                    box.updateFromGui(context, item);
                }
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
        public void setSwingViewValueWithUpdate(Object value) {
            setSelectedItem(value);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addItemListener(eventHandler::accept);
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static Object[] getEnumConstants(GuiMappingContext context) {
        return ((GuiReprValueEnumComboBox) context.getRepresentation()).getEnumConstants(context);
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

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String str = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Object enumValue = ((GuiReprValueEnumComboBox) pane.getContext().getRepresentation()).getEnumValue(
                            pane.getContext(), str);
                    if (enumValue != null) {
                        pane.setSwingViewValueWithUpdate(enumValue);
                        return true;
                    } else {
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
