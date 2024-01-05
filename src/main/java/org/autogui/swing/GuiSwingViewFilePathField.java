package org.autogui.swing;

import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.SearchTextFieldFilePath;
import org.autogui.base.mapping.*;
import org.autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * a swing view for {@link GuiReprValueFilePathField}
 *
 * <h2>swing-value</h2>
 * {@link PropertyFilePathPane#getSwingViewValue()}:
 * selected path as {@link Path} or {@link java.io.File}
 *
 * <p>
 *     updating is caused by {@link PropertyFilePathPane#setFile(Path)} -&gt; editingRunner -&gt;
 *       {@link SearchTextField#updateFieldInEvent(boolean,boolean)} -&gt;
 *       {@link PropertyFilePathPane#setCurrentSearchedItems(List, PopupCategorized.CategorizedMenuItem)} -&gt;
 *         {@link PropertyFilePathPane#selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem)}
 *
 *
 * <h2>history-value</h2>
 * supported.
 *
 * <h2>string-transfer</h2>
 * {@link SearchTextFieldFilePath.FileTransferHandler}.
 * reading and writing a file path string.
 */
public class GuiSwingViewFilePathField implements GuiSwingView {
    public GuiSwingViewFilePathField() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyFilePathPane field = new PropertyFilePathPane(context, new SpecifierManagerDefault(parentSpecifier));
        if (context.isTypeElementProperty()) {
            return field.wrapSwingNamed();
        } else {
            return field;
        }
    }

    public static class PropertyFilePathPane extends SearchTextFieldFilePath
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> { //ValuePane<File|Path>
        @Serial private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean currentValueSupported = true;
        protected List<Runnable> editFinishHandlers = new ArrayList<>(1);

        public PropertyFilePathPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this(context, specifierManager, new SearchTextFieldModelFilePath());
        }

        @SuppressWarnings("this-escape")
        public PropertyFilePathPane(GuiMappingContext context, SpecifierManager specifierManager, SearchTextFieldModelFilePath model) {
            super(model);
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
            GuiSwingView.setDescriptionToolTipText(context, this);
            GuiSwingView.setDescriptionToolTipText(context, getField());
        }

        public void initEditable() {
            getField().setEditable(((GuiReprValueFilePathField) context.getRepresentation())
                    .isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
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
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
        return popup.getMenuBuilder();
    }

        @Override
        public List<Object> getMenuItemsSource() {
            if (menuItemsSource == null) {
                List<Object> actions = super.getMenuItemsSource();
                actions.add(infoLabel);
                actions.add(new ContextRefreshAction(context, this));

                actions.addAll(GuiSwingJsonTransfer.getActions(this, context));
                actions.add(new HistoryMenuFilePath(this, context));
                menuItemsSource = actions;
            }
            return menuItemsSource;
        }

        /** update property: the user selects the item from the menu, and then update the target property */
        @Override
        public void selectSearchedItemFromGui(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromGui(item);
            updateFromGui(getFile(), viewClock.increment());
        }

        /** update property: search done, and then the matched item will be set to the target property*/
        @Override
        public void selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromModel(item);
            updateFromGui(getFile(), viewClock.increment());
        }

        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, value, viewClock);
        }

        /** no property update
         * @param item the selected item
         * */
        public void selectSearchedItemWithoutUpdateContext(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromGui(item);
            //no update callback
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue, contextClock));
        }

        @Override
        public void updateFieldInEvent(boolean modified, boolean immediate) {
            super.updateFieldInEvent(modified, immediate);
            if (modified && immediate) {
                editFinishHandlers.forEach(Runnable::run);
            }
        }

        public void updateFieldInEventWithoutEditFinish() {
            super.updateFieldInEvent(true, true);
        }

        @Override
        public Object getSwingViewValue() {
            return getFile();
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            FileItem item = getFileItemFromValue(value);
            selectSearchedItemWithoutUpdateContext(item);
        }

        public FileItem getFileItemFromValue(Object value) {
            Path path = ((GuiReprValueFilePathField) context.getRepresentation())
                    .toUpdateValue(context, value);
            return getFileItem(path);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            viewClock.increment();
            FileItem item = getFileItemFromValue(value);
            selectSearchedItemWithoutUpdateContext(item);
            updateFromGui(item.getPath(), viewClock);

        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                FileItem item = getFileItemFromValue(value);
                selectSearchedItemWithoutUpdateContext(item);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                FileItem item = getFileItemFromValue(value);
                selectSearchedItemWithoutUpdateContext(item);
                updateFromGui(item.getPath(), viewClock);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
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

    public static class HistoryMenuFilePath extends GuiSwingHistoryMenu<Object, PropertyFilePathPane> {
        @Serial private static final long serialVersionUID = 1L;
        public HistoryMenuFilePath(PropertyFilePathPane component, GuiMappingContext context) {
            super(component, context);
        }

        public Icon getIcon(Object v) {
            SearchTextFieldFilePath.FileItem item = component.getFileItemFromValue(v);
            if (item != null) {
                return item.getIcon();
            } else {
                return null;
            }
        }

        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            Action a = createActionBase(e);
            Icon icon = getIcon(e.getValue());
            a.putValue(Action.SMALL_ICON, icon);
            return a;
        }

        public Action createActionBase(GuiPreferences.HistoryValueEntry e) {
            return super.createAction(e);
        }

        @Override
        public int getMaxNameLength() {
            return 256;
        }
    }
}
