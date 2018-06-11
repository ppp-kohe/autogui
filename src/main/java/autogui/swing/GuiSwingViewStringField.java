package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.base.mapping.GuiTaskClock;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueStringField}
 *
 * <h3>swing-value</h3>
 * {@link PropertyStringPane#getSwingViewValue()}
 *  : the field text as {@link String}.
 * <p>
 *   updating is caused by field's setText(String) -&gt;
 *      document listener -&gt;
 *      taskRunner -&gt; {@link PropertyStringPane#updateFieldInEvent(boolean)}.
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * {@link StringTransferHandler},
 *   reading and writing String.
 */
public class GuiSwingViewStringField implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyStringPane field = new PropertyStringPane(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return field.wrapSwingNamed();
        } else {
            return field;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }

    public static class PropertyStringPane extends SearchTextField
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<String> {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        public PropertyStringPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            initLazy();
        }

        @Override
        public void init() {
            //nothing
        }

        public void initLazy() {
            super.init();
            initName();
            initEditable();
            initContextUpdate();
            initValue();
            initDragDrop();
        }

        public void initPopup() {
            super.initPopup();
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this, getField(), a -> false);
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this, getIcon(), a -> false);
            setInheritsPopupMenu(true);
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            getIcon().setVisible(false);
            GuiSwingView.setDescriptionToolTipText(context, this);
            GuiSwingView.setDescriptionToolTipText(context, getField());
        }

        public void initEditable() {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            getField().setEditable(str.isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initDragDrop() {
            StringTransferHandler h = new StringTransferHandler(this);
            setTransferHandlerWithSettingExportingDragSource(h);
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
        public Supplier<List<PopupCategorized.CategorizedMenuItem>> getMenuItems() {
            return () -> PopupCategorized.getMenuItems(
                    getSwingStaticMenuItems(),
                    getSearchedItems());
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(getMenuItemsSource());
            }
            return menuItems;
        }

        @Override
        public List<Object> getMenuItemsSource() {
            if (menuItemsSource == null) {
                menuItemsSource = super.getMenuItemsSource();
                menuItemsSource.add(infoLabel);
                menuItemsSource.add(new ContextRefreshAction(context));
                menuItemsSource.add(new HistoryMenu<>(this, getSwingViewContext()));
                menuItemsSource.addAll(GuiSwingJsonTransfer.getActions(this, context));
            }
            return menuItemsSource;
        }

        @Override
        public void updateFieldInEvent(boolean modified) {
            super.updateFieldInEvent(modified);
            if (modified) {
                GuiSwingView.updateFromGui(this, getField().getText(), viewClock.increment());
            }
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((String) newValue, contextClock));
        }

        @Override
        public String getSwingViewValue() {
            return getField().getText();
        }

        @Override
        public void setSwingViewValue(String value) {
            viewClock.increment();
            setValueWithoutUpdateField(value);
        }

        private String setValueWithoutUpdateField(String value) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            setTextWithoutUpdateField(str.toUpdateValue(context, value));
            return value;
        }

        @Override
        public void setSwingViewValueWithUpdate(String value) {
            viewClock.increment();
            GuiSwingView.updateFromGui(this, setValueWithoutUpdateField(value), viewClock);
        }

        @Override
        public void setSwingViewValue(String value, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) {
                setValueWithoutUpdateField(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(String value, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) {
                GuiSwingView.updateFromGui(this, setValueWithoutUpdateField(value), viewClock);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            getField().addActionListener(eventHandler::accept);
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void shutdownSwingView() {
            getEditingRunner().shutdown();
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void requestSwingViewFocus() {
            getField().requestFocusInWindow();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }
    }

    /** handle entire text */
    public static class StringTransferHandler extends TransferHandler {
        protected PropertyStringPane pane;

        public StringTransferHandler(PropertyStringPane pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getField().isEnabled() && pane.getField().isEditable() &&
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
                pane.getField().replaceSelection(str);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY | MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            String text = pane.getField().getSelectedText();
            return new StringSelection(text);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == MOVE) {
                pane.getField().replaceSelection("");
            }
        }
    }
}
