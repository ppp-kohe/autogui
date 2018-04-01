package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprPropertyPane;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <h3>representation</h3>
 * {@link autogui.base.mapping.GuiReprPropertyPane},
 *  or another repr. called {@link ValuePane#wrapSwingProperty()}
 *  The latter case is that a context has a property type, but the representation matched to the value type of the property.
 *    (see {@link GuiReprValue#getValueType(GuiMappingContext)}).
 *
 * <h3>{@link PropertyPane#getSwingViewValue()}</h3>
 *  {@link autogui.base.mapping.GuiReprValue.NamedValue}, or other value type.
 *     obtaining and setting ({@link PropertyPane#setSwingViewValue(Object)} and
 *        {@link PropertyPane#setSwingViewValueWithUpdate(Object)}) delete to the contentPane.
 *     Note the setting of the property will be treated by the value's contentPane.
 *        {@link GuiReprValue#updateFromGui(GuiMappingContext, Object)} by the value's contentPane
 *         supports the case of the parent is a property.
 *
 *  <h3>history-value</h3>
 *  unsupported. (supported by the contentPane)
 *
 *  <h3>string-transfer</h3>
 *  only copying is supported by {@link autogui.swing.GuiSwingView.ToStringCopyAction}
 */
public class GuiSwingViewPropertyPane implements GuiSwingView {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingViewPropertyPane(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyPane pane = new PropertyPane(context, context.isTypeElementProperty());

        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement e = mapperSet.view(subContext);
            if (e != null && e instanceof GuiSwingView) {
                GuiSwingView view = (GuiSwingView) e;
                JComponent subComp = view.createView(subContext);
                if (subComp != null) {
                    pane.setContentPane(subComp);
                }
            }
        }
        return pane;
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingView view = (GuiSwingView) mapperSet.view(subContext);
            if (view != null && view.isComponentResizable(subContext)) {
                return true;
            }
        }
        return false;
    }

    public static class PropertyPane extends NamedPropertyPane {
        protected GuiMappingContext context;
        protected PopupExtension popup;
        protected boolean showName;

        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

        public PropertyPane(GuiMappingContext context, boolean showName) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            this.showName = showName;
            initLazy();
        }

        public PropertyPane(GuiMappingContext context, boolean showName, JComponent content) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            this.showName = showName;
            setContentPane(content);
            initLazy();
        }

        @Override
        public void init() {
            setLayout(new BorderLayout());
            setOpaque(false);
        }

        public void initLazy() {
            initName();
            initPopup();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
            if (showName) {
                initNameLabel();
            }
        }

        public void initNameLabel() {
            label = new JLabel(displayName);

            label.setName(context.getName() + ".label");
            label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

            add(label, BorderLayout.NORTH);
        }

        public void initPopup() {
            popup = new PopupExtension(this, null);
            setPopupBuilder();
            setInheritsPopupMenu(true);
        }

        public void setPopupBuilder() {
            if (popup != null) {
                PopupExtension.PopupMenuBuilder builder;
                if (hasContentValuePane()) {
                    PopupExtension.PopupMenuBuilder contentBuilder = getContentPaneAsValuePane().getSwingMenuBuilder();
                    if (contentBuilder instanceof PopupCategorized) {
                        builder = new PopupCategorized(PopupCategorized.getMenuItemsSupplier(
                                this::getSwingStaticMenuItems,
                                () -> ((PopupCategorized) contentBuilder).getItemSupplier().get().stream()
                                        .map(c -> c.remap(MenuBuilder.getCategoryWithPrefix("Value ", c.getCategory()), c.getSubCategory()))
                                        .collect(Collectors.toList())));
                    } else {
                        builder = new PopupCategorized(this::getSwingStaticMenuItems);
                    }
                } else {
                    builder = new PopupCategorized(this::getSwingStaticMenuItems);
                }
                popup.setMenuBuilder(builder);
            }
        }

        @Override
        public void setContentPane(JComponent content) {
            boolean diff = (this.contentPane != content);
            if (this.contentPane != null) {
                remove(contentPane);
            }
            this.contentPane = content;
            add(this.contentPane, BorderLayout.CENTER);

            if (diff) {
                setPopupBuilder();
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                GuiSwingContextInfo.get().getInfoLabel(context),
                                new ContextRefreshAction(context),
                                new ToStringCopyAction(this, context)),
                        GuiSwingJsonTransfer.getActions(this, context)).stream()
                        .map(i -> i.remap("Property " + MenuBuilder.getCategoryName(i.getCategory()), i.getSubCategory()))
                        .collect(Collectors.toList());
            }
            return menuItems;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }
    }

    public static class NamedPropertyPane extends NamedPane implements ValuePaneWrapper<Object> {
        protected PopupExtension popup;

        public NamedPropertyPane() { }

        public NamedPropertyPane(String displayName, String name) {
            super(displayName);
            setName(name);
        }

        public NamedPropertyPane(String displayName, String name, JComponent contentPane) {
            super(displayName, contentPane);
            setName(name);
        }

        public NamedPropertyPane(String displayName, String name, JComponent contentPane,
                                 PopupExtension.PopupMenuBuilder menuBuilder) {
            this(displayName, name, contentPane);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), menuBuilder);
        }

        @Override
        public void init() {
            super.init();
            setBorder(new GuiSwingViewLabel.FocusBorder(this));
        }

        public boolean hasContentValuePane() {
            return contentPane != null && contentPane instanceof ValuePane;
        }

        @SuppressWarnings("unchecked")
        public ValuePane<Object> getContentPaneAsValuePane() {
            return (ValuePane<Object>) contentPane;
        }

        @Override
        public Object getSwingViewValue() {
            if (hasContentValuePane()) {
                Object value = getContentPaneAsValuePane().getSwingViewValue();
                if (value != null && isProperty()) {
                    return new GuiReprValue.NamedValue(getName(), value);
                } else {
                    return value;
                }
            }
            return null;
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (hasContentValuePane() && value != null) {
                if (isProperty() && value instanceof GuiReprValue.NamedValue) {
                    value = ((GuiReprValue.NamedValue) value).value;
                }
                getContentPaneAsValuePane().setSwingViewValue(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            if (hasContentValuePane() && value != null) {
                if (isProperty() && value instanceof GuiReprValue.NamedValue) {
                    value = ((GuiReprValue.NamedValue) value).value;
                }
                getContentPaneAsValuePane().setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return Collections.emptyList();
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            if (hasContentValuePane()) {
                return getContentPaneAsValuePane().getSwingMenuBuilder();
            } else {
                return null;
            }
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            if (hasContentValuePane()) {
                getContentPaneAsValuePane().addSwingEditFinishHandler(eventHandler);
            }
        }

        public boolean isProperty() {
            GuiMappingContext context = getSwingViewContext();
            if (context != null && context.getRepresentation() instanceof GuiReprPropertyPane) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            if (hasContentValuePane()) {
                return getContentPaneAsValuePane().getSwingViewContext();
            } else {
                return null;
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            GuiSwingView.loadChildren(prefs.getDescendant(getSwingViewContext()), this);
        }

        /**
         * @return null
         *        meaning the named pane will never become key-binding target,
         *        unless the type-element is explicitly attached a keyStroke annotation.
         */
        @Override
        public KeyStroke getSwingFocusKeyStroke() {
            if (getSwingViewContext().isAcceleratorKeyStrokeSpecified()) {
                return GuiSwingKeyBinding.getKeyStroke(getSwingViewContext().getAcceleratorKeyStroke());
            } else {
                return null;
            }
        }

        @Override
        public ValuePane<Object> getSwingViewWrappedPane() {
            if (hasContentValuePane()) {
                return getContentPaneAsValuePane();
            } else {
                return null;
            }
        }
    }
}
