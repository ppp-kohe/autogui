package org.autogui.swing;

import org.autogui.swing.table.GuiSwingTableColumnLabel;
import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.base.mapping.*;
import org.autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a swing view for {@link GuiReprValueEnumComboBox}
 *
 * <h2>swing-value </h2>
 * {@link PropertyEnumComboBox#getSwingViewValue()}:
 *  the selected {@link Enum} member.
 *
 * <p>
 *     updating is caused by {@link PropertyEnumComboBox#setSelectedItem(Object)} -&gt;
 *      item-listener: {@link PropertyEnumComboBox#itemStateChanged(ItemEvent)}
 *
 *
 * <h2>history-value</h2>
 * supported.
 *
 * <h2>string-transfer</h2>
 * {@link EnumTransferHandler}.
 *  reading {@link Enum#name()} or {@link Enum#ordinal()}, and writing {@link Enum#name()}.
 *  @see GuiReprValueEnumComboBox#getEnumValue(GuiMappingContext, String)
 */
@SuppressWarnings("this-escape")
public class GuiSwingViewEnumComboBox implements GuiSwingView {
    public GuiSwingViewEnumComboBox() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        ValuePane<Object> pane;
        if (!context.getReprValue().isEditable(context)) {
            pane = new PropertyLabelEnum(context, new SpecifierManagerDefault(parentSpecifier));
        } else {
            pane = new PropertyEnumComboBox(context, new SpecifierManagerDefault(parentSpecifier));
        }
        if (context.isTypeElementProperty()) {
            return pane.wrapSwingNamed();
        } else {
            return pane.asSwingViewComponent();
        }
    }

    public static class PropertyEnumComboBox extends JComboBox<Object>
            implements GuiMappingContext.SourceUpdateListener, ItemListener, GuiSwingView.ValuePane<Object> { //Enum
        @Serial private static final long serialVersionUID = 1L;

        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected boolean listenerEnabled = true;
        protected PopupExtension popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean currentValueSupported = true;

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
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new GuiSwingHistoryMenu<>(this, context),
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
            SwingDeferredRunner.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (listenerEnabled) {
                updateFromGui(getSelectedItem(), viewClock.increment());
            }
        }

        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, value, viewClock);
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
            GuiSwingView.updateViewClockSync(viewClock, context);
            viewClock.increment();
            setSelectedItemWithoutListener(value);
            updateFromGui(value, viewClock);
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
                updateFromGui(value, viewClock);
            }
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(text);
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            addItemListener(e -> eventHandler.run());
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

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    public static Object[] getEnumConstants(GuiMappingContext context) {
        return ((GuiReprValueEnumComboBox) context.getRepresentation()).getEnumConstants(context);
    }

    public static class PropertyEnumListRenderer extends DefaultListCellRenderer {
        @Serial private static final long serialVersionUID = 1L;

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
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyEnumComboBox pane;

        public EnumTransferHandler(PropertyEnumComboBox pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.isSwingEditable() && pane.isEnabled() &&
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
        @SuppressWarnings("rawtypes")
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
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyEnumComboBox view;

        public EnumPasteAction(PropertyEnumComboBox view) {
            super(null);

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_V,
                            PopupExtension.getMenuShortcutKeyMask()));
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
        @Serial private static final long serialVersionUID = 1L;

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
        @Serial private static final long serialVersionUID = 1L;

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

    public static class PropertyLabelEnum extends GuiSwingTableColumnLabel.PropertyLabelColumn {
        @Serial private static final long serialVersionUID = 1L;

        public PropertyLabelEnum(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        infoLabel,
                        new GuiSwingView.ContextRefreshAction(getSwingViewContext(), this),
                        new GuiSwingHistoryMenu<>(this, getSwingViewContext()),
                        new GuiSwingViewLabel.LabelToStringCopyAction(this),
                        new GuiSwingViewLabel.LabelTextSaveAction(this),
                        new GuiSwingViewLabel.LabelJsonCopyAction(this, context),
                        new GuiSwingViewLabel.LabelJsonSaveAction(this, context)));
            }
            return menuItems;
        }
    }
}
