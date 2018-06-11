package autogui.swing;

import autogui.base.mapping.*;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a swing view for {@link GuiReprValueEnumComboBox}
 *
 * <h3>swing-value </h3>
 * {@link PropertyEnumComboBox#getSwingViewValue()}:
 *  the selected {@link Enum} member.
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
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyEnumComboBox box = new PropertyEnumComboBox(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return box.wrapSwingNamed();
        } else {
            return box;
        }
    }

    public static class PropertyEnumComboBox extends JComboBox<Object>
            implements GuiMappingContext.SourceUpdateListener, ItemListener, GuiSwingView.ValuePane<Object> { //Enum
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected boolean listenerEnabled = true;
        protected PopupExtension popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        public PropertyEnumComboBox(GuiMappingContext context, SpecifierManager specifierManager) {
            super(getEnumConstants(context));
            this.context = context;
            this.specifierManager = specifierManager;
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
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initRenderer() {
            setRenderer(new PropertyEnumListRenderer(context));
        }

        public void initEditable() {
            //combo-box editable meaning editing with a text field, and enable meaning selectable from a popup
            setEnabled(((GuiReprValueEnumComboBox) context.getRepresentation())
                    .isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initListener() {
            addItemListener(this);
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
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
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context),
                                new HistoryMenu<>(this, context),
                                new EnumSetMenu(this),
                                new ToStringCopyAction(this, getSwingViewContext()),
                                new EnumPasteAction(this)),
                        GuiSwingJsonTransfer.getActions(this, context));
            }
            return menuItems;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (listenerEnabled) {
                GuiSwingView.updateFromGui(this, getSelectedItem(), viewClock.increment());
            }
        }

        @Override
        public Object getSwingViewValue() {
            return getSelectedItem();
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            setSelectedItemWithoutListener(value);
        }

        private void setSelectedItemWithoutListener(Object value) {
            listenerEnabled = false;
            try {
                setSelectedItem(value);
            } finally {
                listenerEnabled = true;
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            viewClock.increment();
            setSelectedItemWithoutListener(value);
            GuiSwingView.updateFromGui(this, value, viewClock);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSelectedItemWithoutListener(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSelectedItemWithoutListener(value);
                GuiSwingView.updateFromGui(this, value, viewClock);
            }
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(text);
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            addItemListener(eventHandler::accept);
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public GuiSwingViewPropertyPane.NamedPropertyPane wrapSwingNamed() {
            GuiSwingViewPropertyPane.NamedPropertyPane p = new GuiSwingViewPropertyPane.NamedPropertyPane(getSwingViewContext().getDisplayName(), getSwingViewContext().getName(),
                    asSwingViewComponent(), getSwingMenuBuilder());
            GuiSwingView.setupTransferHandler(p, getTransferHandler());
            return p;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        public PopupExtension getPopup() {
            return popup;
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        public String getValueAsString(Object v) {
            if (v == null) {
                return "null";
            } else {
                return ((GuiReprValueEnumComboBox) context.getRepresentation()).getDisplayName(context, (Enum<?>) v);
            }
        }

        public Object getValueFromString(String str) {
            return ((GuiReprValueEnumComboBox) getSwingViewContext().getRepresentation()).getEnumValue(getSwingViewContext(), str);
        }
    }

    public static Object[] getEnumConstants(GuiMappingContext context) {
        return ((GuiReprValueEnumComboBox) context.getRepresentation()).getEnumConstants(context);
    }

    public static class PropertyEnumListRenderer extends DefaultListCellRenderer {
        protected GuiMappingContext context;
        protected Color disabledForeground;

        public PropertyEnumListRenderer(GuiMappingContext context) {
            this.context = context;
            disabledForeground = UIManagerUtil.getInstance().getLabelDisabledForeground();
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(3);
            int w = ui.getScaledSizeInt(5);
            setBorder(BorderFactory.createEmptyBorder(h, w, h, w * 2));

            if (value instanceof Enum<?>) {
                setText(((GuiReprValueEnumComboBox) context.getRepresentation()).getDisplayName(context, (Enum<?>) value));
            } else if (value == null) {
                setText("null");
                setForeground(disabledForeground);
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
            return pane.isEnabled() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String str = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    Object enumValue = pane.getValueFromString(str);
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
                String name = pane.getValueAsString(e);
                return new StringSelection(name);
            } else {
                return null;
            }
        }
    }

    public static class EnumPasteAction extends PopupExtensionText.TextPasteAllAction
            implements TableTargetColumnAction {
        protected PropertyEnumComboBox view;

        public EnumPasteAction(PropertyEnumComboBox view) {
            super(null);

            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_V,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.view = view;
        }

        @Override
        public boolean isEnabled() {
            return view != null && view.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(v -> view.setSwingViewValueWithUpdate(getEnumValue(v)));
        }

        public Object getEnumValue(String s) {
            return view.getValueFromString(s);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            pasteLines(lines ->
                    target.setSelectedCellValuesLoop(
                            lines.stream()
                                    .map(this::getEnumValue)
                                    .collect(Collectors.toList())));
        }
    }

    public static class EnumSetMenu extends JMenu implements PopupCategorized.CategorizedMenuItemComponent {
        protected ValuePane<Object> pane;

        public EnumSetMenu(ValuePane<Object> pane) {
            super("Set");
            this.pane = pane;
            setItems();
        }

        //it does not override isEnabled(): always true

        public void setItems() {
            removeAll();
            Arrays.stream(getEnumConstants(pane.getSwingViewContext()))
                    .map(this::createItem)
                    .forEach(this::add);
        }

        public Action createItem(Object e) {
            return new EnumSetAction(pane, e);
        }

        @Override
        public JComponent getMenuItem() {
            return this;
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SET;
        }
    }

    public static class EnumSetAction extends AbstractAction implements
            PopupCategorized.CategorizedMenuItemAction {
        protected Object value;
        protected ValuePane<Object> pane;

        public EnumSetAction(ValuePane<Object> pane, Object value) {
            this.value = value;
            this.pane = pane;

            putValue(NAME, getValueAsString(value));
        }

        @Override
        public boolean isEnabled() {
            return pane.isSwingEditable();
        }

        public String getValueAsString(Object v) {
            if (pane instanceof GuiSwingViewLabel.PropertyLabel) {
                return ((GuiSwingViewLabel.PropertyLabel) pane).getValueAsString(v);
            } else {
                return ((PropertyEnumComboBox) pane).getValueAsString(v);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setSwingViewValueWithUpdate(value);
        }
    }
}
