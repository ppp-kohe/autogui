package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;
import java.util.function.Consumer;

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

    public static class PropertyPane extends NamedPropertyPane implements ValuePane {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyPane(GuiMappingContext context, boolean showName) {
            super(context.getDisplayName(), context.getName());
            this.context = context;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            if (showName) {
                initNameLabel();
            }

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
            });
            setInheritsPopupMenu(true);
        }

        public PropertyPane(GuiMappingContext context, boolean showName, JComponent content) {
            this(context, showName);
            setContentPane(content);
        }

        public void initNameLabel() {
            label = new JLabel(displayName);

            label.setName(context.getName() + ".label");
            label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            add(label, BorderLayout.NORTH);
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

    public static class NamedPropertyPane extends NamedPane implements ValuePane {

        public NamedPropertyPane() { }

        public NamedPropertyPane(String displayName, String name) {
            super(displayName);
            setName(name);
        }

        public NamedPropertyPane(String displayName, String name, JComponent contentPane) {
            super(displayName, contentPane);
            setName(name);
        }

        @Override
        public Object getSwingViewValue() {
            if (contentPane != null && contentPane instanceof ValuePane) {
                Object value = ((ValuePane) contentPane).getSwingViewValue();
                if (value != null) {
                    return new GuiReprValue.NamedValue(getName(), value);
                }
            }
            return null;
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (contentPane != null && contentPane instanceof ValuePane) {
                if (value != null && value instanceof GuiReprValue.NamedValue) {
                    ((ValuePane) contentPane).setSwingViewValue(((GuiReprValue.NamedValue) value).value);
                }
            }
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            if (contentPane != null && contentPane instanceof ValuePane) {
                return ((ValuePane) contentPane).getSwingMenuBuilder();
            } else {
                return null;
            }
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            if (contentPane != null && contentPane instanceof ValuePane) {
                ((ValuePane) contentPane).addSwingEditFinishHandler(eventHandler);
            }
        }

        @Override
        public GuiMappingContext getContext() {
            if (contentPane != null && contentPane instanceof ValuePane) {
                return ((ValuePane) contentPane).getContext();
            } else {
                return null;
            }
        }
    }
}
