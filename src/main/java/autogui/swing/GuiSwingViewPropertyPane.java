package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;

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

    public static class PropertyPane extends JComponent {
        protected GuiMappingContext context;
        protected JComponent content;

        public PropertyPane(GuiMappingContext context, boolean showName) {
            this.context = context;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setName(context.getName());
            if (showName) {
                initNameLabel();
            }

            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.removeAll();
                menu.add(info);
                menu.revalidate();
            }); //TODO
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
    }
}
