package autogui.swing;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;

public class GuiSwingViewTabbedPane extends GuiSwingViewObjectPane {

    public GuiSwingViewTabbedPane(GuiSwingMapperSet mapperSet) {
        super(mapperSet);
    }

    @Override
    protected ObjectPane createObjectPane(GuiMappingContext context) {
        return new ObjectTabbedPane(context);
    }

    @Override
    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent comp = view.createView(subContext);
        if (comp != null) {
            ((ObjectTabbedPane) pane).addSubComponent(subContext, comp);
        }
    }

    public static class ObjectTabbedPane extends GuiSwingViewObjectPane.ObjectPane {
        protected JTabbedPane tabbedPane;

        public ObjectTabbedPane(GuiMappingContext context) {
            super(context);
        }

        @Override
        public void initContentPane() {
            tabbedPane = new JTabbedPane();
            this.contentPane = tabbedPane;
            add(tabbedPane);
        }

        public void addSubComponent(GuiMappingContext subContext, JComponent component) {
            tabbedPane.addTab(subContext.getDisplayName(), component);
        }

        @Override
        public void addSubComponent(JComponent component, boolean resizable) {
            if (component instanceof ValuePane<?>) {
                addSubComponent(((ValuePane) component).getContext(), component);
            }
        }
    }
}
