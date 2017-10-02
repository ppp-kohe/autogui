package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.naming.NameParser;
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
                    pane.setContent(subComp);
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

    public static class PropertyPane extends JComponent implements ValuePane {
        protected GuiMappingContext context;
        protected JComponent content;
        protected PopupExtension popup;

        public PropertyPane(GuiMappingContext context, boolean showName) {
            this.context = context;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setName(context.getName());
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
            setContent(content);
        }

        public void initNameLabel() {
            JLabel label = new JLabel(context.getDisplayName());

            label.setName(context.getName() + ".label");
            label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            add(label, BorderLayout.NORTH);
        }

        public void setContent(JComponent content) {
            this.content = content;
            add(this.content, BorderLayout.CENTER);
        }

        @Override
        public Object getSwingViewValue() {
            if (content != null && content instanceof ValuePane) {
                return ((ValuePane) content).getSwingViewValue();
            }
            return context.getSource();
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (content != null && content instanceof ValuePane) {
                ((ValuePane) content).setSwingViewValue(value);
            }
            context.setSource(value);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }


        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            if (content != null && content instanceof ValuePane) {
                ((ValuePane) content).addSwingEditFinishHandler(eventHandler);
            }
        }
    }

    public static class NamedPropertyPane extends NamedPane implements ValuePane {

        public NamedPropertyPane(String displayName) {
            super(displayName);
        }

        public NamedPropertyPane(String displayName, JComponent contentPane) {
            super(displayName, contentPane);
        }

        @Override
        public Object getSwingViewValue() {
            if (contentPane != null && contentPane instanceof ValuePane) {
                return ((ValuePane) contentPane).getSwingViewValue();
            }
            return null;
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (contentPane != null && contentPane instanceof ValuePane) {
                ((ValuePane) contentPane).setSwingViewValue(value);
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
    }
}
