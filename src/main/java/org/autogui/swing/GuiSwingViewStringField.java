package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueStringField;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
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
 *      taskRunner -&gt; {@link PropertyStringPane#updateFieldInEvent(boolean, boolean)}.
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
        private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean currentValueSupported = true;
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);

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
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
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
                menuItemsSource.add(new ContextRefreshAction(context, this));
                menuItemsSource.add(new GuiSwingHistoryMenu<>(this, getSwingViewContext()));
                menuItemsSource.addAll(GuiSwingJsonTransfer.getActions(this, context));
            }
            return menuItemsSource;
        }

        @Override
        public void updateFieldInEvent(boolean modified, boolean immediate) {
            super.updateFieldInEvent(modified, immediate);
            if (modified) {
                updateFromGui(getField().getText(), viewClock.increment());
            }
            if (modified && immediate) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public void updateFieldInEventWithoutEditFinish() {
            super.updateFieldInEvent(true, true);
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
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
            updateFromGui(setValueWithoutUpdateField(value), viewClock);
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
                updateFromGui(setValueWithoutUpdateField(value), viewClock);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            //getField().addActionListener(eventHandler::accept);
            editFinishHandlers.add(eventHandler);
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

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    /** handle entire text */
    public static class StringTransferHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;
        protected PropertyStringPane pane;

        public StringTransferHandler(PropertyStringPane pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return pane.getField().isEnabled() && pane.getField().isEditable() &&
                    pane.isSwingEditable() &&
                    support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor) && canImport(support)) {
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
