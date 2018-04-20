package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiReprValueFilePathField;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import java.nio.file.Path;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * <h3>representation</h3>
 * {@link GuiReprValueFilePathField}
 *
 * <h3>{@link PropertyFilePathPane#getSwingViewValue()}</h3>
 * selected path: {@link Path} or {@link java.io.File}
 *
 * <p>
 *     updating is caused by {@link PropertyFilePathPane#setFile(Path)} -&gt; editingRunner -&gt;
 *       {@link autogui.swing.util.SearchTextField#updateFieldInEvent(boolean)} -&gt;
 *       {@link PropertyFilePathPane#setCurrentSearchedItems(List, PopupCategorized.CategorizedMenuItem)} -&gt;
 *         {@link PropertyFilePathPane#selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem)}
 *
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * {@link autogui.swing.util.SearchTextFieldFilePath.FileTransferHandler}.
 * reading and writing a file path string.
 */
public class GuiSwingViewFilePathField implements GuiSwingView {
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
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

        public PropertyFilePathPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this(context, specifierManager, new SearchTextFieldModelFilePath());
        }

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
            initPopup();
        }

        public void initPopup() {
            super.initPopup();
            setInheritsPopupMenu(true);
        }

        public void initName() {
            setName(context.getName());
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
            update(context, context.getSource());
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
                actions.add(GuiSwingContextInfo.get().getInfoLabel(context));
                actions.add(new ContextRefreshAction(context));

                actions.addAll(GuiSwingJsonTransfer.getActions(this, context));
                actions.add(new HistoryMenu<>(this, context));
                menuItemsSource = actions;
            }
            return menuItemsSource;
        }

        /** update property: the user selects the item from the menu, and then update the target property */
        @Override
        public void selectSearchedItemFromGui(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromGui(item);
            GuiReprValueFilePathField path = (GuiReprValueFilePathField) context.getRepresentation();
            path.updateFromGui(context, getFile(), getSpecifier());
        }

        /** update property: search done, and then the matched item will be set to the target property*/
        @Override
        public void selectSearchedItemFromModel(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromModel(item);
            GuiReprValueFilePathField path = (GuiReprValueFilePathField) context.getRepresentation();
            if (path.isEditable(context)) {
                path.updateFromGui(context, getFile(), getSpecifier());
            }
        }

        /** no property update
         * @param item the selected item
         * */
        public void selectSearchedItemWithoutUpdateContext(PopupCategorized.CategorizedMenuItem item) {
            super.selectSearchedItemFromGui(item);
            //no update callback
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getFile();
        }

        @Override
        public void setSwingViewValue(Object value) {
            Path path = ((GuiReprValueFilePathField) context.getRepresentation())
                    .toUpdateValue(context, value);
            FileItem item = getFileItem(path);
            selectSearchedItemWithoutUpdateContext(item);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            Path path = ((GuiReprValueFilePathField) context.getRepresentation())
                    .toUpdateValue(context, value);
            setFile(path);
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
    }
}
