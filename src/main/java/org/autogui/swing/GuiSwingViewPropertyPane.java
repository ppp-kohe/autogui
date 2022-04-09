package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprPropertyPane;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a swing view for
 * {@link GuiReprPropertyPane},
 *  or another repr. called {@link ValuePane#wrapSwingProperty()}.
 *  The latter case is that a context has a property type, but the representation matched to the value type of the property.
 *    (see {@link GuiReprValue#getValueType(GuiMappingContext)}).
 *
 * <h3>swing-value</h3>
 * {@link PropertyPane#getSwingViewValue()}
 *  {@link GuiReprValue.NamedValue}, or other value types.
 *     obtaining and setting ({@link PropertyPane#setSwingViewValue(Object)} and
 *        {@link PropertyPane#setSwingViewValueWithUpdate(Object)}) delete to the contentPane.
 *  <p>
 *     Note the setting of the property will be treated by the value's contentPane.
 *        {@link GuiReprValue#updateFromGui(GuiMappingContext, Object, GuiReprValue.ObjectSpecifier, GuiTaskClock)}
 *        by the value's contentPane
 *         supports the case of the parent is a property.
 *
 *  <h3>history-value</h3>
 *  unsupported. (supported by the contentPane)
 *
 *  <h3>string-transfer</h3>
 *  only copying is supported by {@link GuiSwingView.ToStringCopyAction}
 */
public class GuiSwingViewPropertyPane implements GuiSwingView {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingViewPropertyPane(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyPane pane = new PropertyPane(context, context.isTypeElementProperty(),
                new SpecifierManagerDefault(parentSpecifier));
        Supplier<GuiReprValue.ObjectSpecifier> specifierForChildren = pane::getSpecifier;

        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement e = mapperSet.view(subContext);
            if (e instanceof GuiSwingView) {
                GuiSwingView view = (GuiSwingView) e;
                JComponent subComp = view.createView(subContext, specifierForChildren);
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
        private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean showName;

        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;

        public PropertyPane(GuiMappingContext context, boolean showName, SpecifierManager specifierManager) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            this.showName = showName;
            this.specifierManager = specifierManager;
            initLazy();
        }

        public PropertyPane(GuiMappingContext context, boolean showName, SpecifierManager specifierManager, JComponent content) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            this.showName = showName;
            this.specifierManager = specifierManager;
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
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
            if (showName) {
                initNameLabel();
            }
        }

        public void initNameLabel() {
            label = new JLabel(displayName);

            label.setName(context.getName() + ".label");
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(3);
            int w = ui.getScaledSizeInt(10);
            label.setBorder(BorderFactory.createEmptyBorder(h, w, h, w));

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
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new ToStringCopyAction(this, context)),
                        GuiSwingJsonTransfer.getActions(this, context)).stream()
                        .map(i -> i.remap("Property " + MenuBuilder.getCategoryName(i.getCategory()), i.getSubCategory()))
                        .collect(Collectors.toList());
                for (PopupCategorized.CategorizedMenuItem i : menuItems) {
                    Action a = PopupCategorized.getMenuItemAction(i);
                    if (a != null) {
                        a.putValue(Action.ACCELERATOR_KEY, null);
                    }
                }
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
    }

    public static class PropertyWrapperPane extends PropertyPane {
        private static final long serialVersionUID = 1L;
        public PropertyWrapperPane(GuiMappingContext context, boolean showName, ValuePane<?> content) {
            super(context, showName, null, content.asSwingViewComponent());
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return ((ValuePane<?>) contentPane).getSpecifier();
        }
    }

    public static class NamedPropertyPane extends NamedPane implements GuiSwingViewWrapper.ValuePaneWrapper<Object> {
        private static final long serialVersionUID = 1L;
        protected PopupExtension popup;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);

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
            return contentPane instanceof ValuePane;
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
                viewClock.increment();
                getContentPaneAsValuePane().setSwingViewValue(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            if (hasContentValuePane() && value != null) {
                if (isProperty() && value instanceof GuiReprValue.NamedValue) {
                    value = ((GuiReprValue.NamedValue) value).value;
                }
                GuiSwingView.updateViewClockSync(viewClock, getSwingViewContext());
                viewClock.increment();
                getContentPaneAsValuePane().setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (hasContentValuePane() && value != null) {
                if (isProperty() && value instanceof GuiReprValue.NamedValue) {
                    value = ((GuiReprValue.NamedValue) value).value;
                }
                if (viewClock.isOlderWithSet(clock)) {
                    getContentPaneAsValuePane().setSwingViewValue(value);
                }
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (hasContentValuePane() && value != null) {
                if (isProperty() && value instanceof GuiReprValue.NamedValue) {
                    value = ((GuiReprValue.NamedValue) value).value;
                }
                if (viewClock.isOlderWithSet(clock)) {
                    getContentPaneAsValuePane().setSwingViewValueWithUpdate(value);
                }
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
        public void addSwingEditFinishHandler(Runnable eventHandler) {
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

        /**
         * @return null
         *        meaning the named pane will never become key-binding target
         */
        @Override
        public KeyStroke getSwingFocusKeyStroke() {
//            if (getSwingViewContext().isAcceleratorKeyStrokeSpecified()) {
//                return GuiSwingKeyBinding.getKeyStroke(getSwingViewContext().getAcceleratorKeyStroke());
//            } else {
                return null;
//            }
        }

        @Override
        public ValuePane<Object> getSwingViewWrappedPane() {
            if (hasContentValuePane()) {
                return getContentPaneAsValuePane();
            } else {
                return null;
            }
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return GuiReprValue.NONE;
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }
}
