package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprPropertyPane;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;
import java.util.function.Consumer;

/**
 * <h3>representation</h3>
 * {@link autogui.base.mapping.GuiReprPropertyPane},
 *  or another repr. called {@link ValuePane#wrapProperty()}
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

        public PropertyPane(GuiMappingContext context, boolean showName) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            if (showName) {
                initNameLabel();
            }
            initPopup();
        }

        public PropertyPane(GuiMappingContext context, boolean showName, JComponent content) {
            this(context, showName);
            setContentPane(content);
        }

        @Override
        public void init() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            //skip init
        }

        public void initNameLabel() {
            label = new JLabel(displayName);

            label.setName(context.getName() + ".label");
            label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

            add(label, BorderLayout.NORTH);
        }

        public void initPopup() {
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
                menu.accept(new ToStringCopyAction(this, context));
            });
            setInheritsPopupMenu(true);

        }

        @Override
        public void setContentPane(JComponent content) {
            if (this.contentPane != null) {
                remove(contentPane);
            }
            this.contentPane = content;
            add(this.contentPane, BorderLayout.CENTER);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static class NamedPropertyPane extends NamedPane implements ValuePane<Object> {
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
            GuiMappingContext context = getContext();
            if (context != null && context.getRepresentation() instanceof GuiReprPropertyPane) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public GuiMappingContext getContext() {
            if (hasContentValuePane()) {
                return getContentPaneAsValuePane().getContext();
            } else {
                return null;
            }
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
            GuiSwingView.loadChildren(prefs.getDescendant(getContext()), this);
        }
    }
}
