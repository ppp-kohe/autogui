package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.ResizableFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingViewObjectPane implements GuiSwingView {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingViewObjectPane(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context) {
        ObjectPane pane = new ObjectPane(context);
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement e = mapperSet.view(subContext);
            if (e != null) {
                if (e instanceof GuiSwingView) {
                    createSubView(subContext, pane, (GuiSwingView) e);
                } else if (e instanceof GuiSwingAction) {
                    createSubAction(subContext, pane, (GuiSwingAction) e);
                }
            }
        }
        return pane;
    }

    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent subComp = view.createView(subContext);
        if (subComp != null) {
            pane.addSubComponent(subComp, view.isComponentResizable(subContext));
        }
    }

    public void createSubAction(GuiMappingContext subContext, ObjectPane pane, GuiSwingAction action) {
        Action act = action.createAction(subContext);
        if (act != null) {
            pane.addAction(act);
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    /**
     * <pre>
     * actionToolBar: [JToolBar: actionButtons...],
     * contentPane: [
     *   fixedSizeComponent1,
     *   ...
     *   resizableSubComponents [
     *       [splitPane: resizableComponent1 |
     *         [splitPane: resizableComponent2 | ... ]
     *       ]
     *   ],
     *   fixedSizeComponent2,
     *   fixedSizeComponent3,
     *   ... ]
     *  </pre>
     */
    public static class ObjectPane extends JPanel implements GuiMappingContext.SourceUpdateListener, ValuePane {
        protected GuiMappingContext context;
        protected JToolBar actionToolBar;
        protected JComponent contentPane;
        protected JComponent resizableSubComponents;
        protected PopupExtension popup;
        protected List<Action> actions = new ArrayList<>();

        public ObjectPane(GuiMappingContext context) {
            this.context = context;
            setLayout(new BorderLayout());
            initContentPane();
            add(contentPane, BorderLayout.CENTER);

            //context update
            context.addSourceUpdateListener(this);

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                menu.accept(new JPopupMenu.Separator());
                actions.forEach(menu::accept);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
            });
            setInheritsPopupMenu(true);
        }

        public void initContentPane() {
            contentPane = new JPanel();
            contentPane.setOpaque(false);
            ResizableFlowLayout layout = new ResizableFlowLayout(false, 10);
            layout.setFitHeight(true);
            contentPane.setLayout(layout);
        }

        public List<Action> getActions() {
            return actions;
        }

        public JComponent getContentPane() {
            return contentPane;
        }

        public JToolBar getActionToolBar() {
            return actionToolBar;
        }

        public JComponent getResizableSubComponents() {
            return resizableSubComponents;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            //nothing to do?
        }

        public void addSubComponent(JComponent component, boolean resizable) {
            if (resizable) {
                addSubComponentResizable(component);
            } else {
                ResizableFlowLayout.add(contentPane, component, false);
            }
        }

        public void addSubComponentResizable(JComponent component) {
            if (resizableSubComponents == null) {
                resizableSubComponents = createResizableSubComponents();
                resizableSubComponents.add(component);
                ResizableFlowLayout.add(contentPane, resizableSubComponents, true);
            } else {
                Component prev = resizableSubComponents.getComponent(0);
                resizableSubComponents.removeAll();
                JComponent split = createResizableSplit(false, prev, component);
                resizableSubComponents.add(split);
            }
        }

        public JComponent createResizableSubComponents() {
            JComponent resizableSubComponents = new JPanel();
            resizableSubComponents.setLayout(new BorderLayout());
            resizableSubComponents.setBorder(BorderFactory.createEmptyBorder());
            resizableSubComponents.setOpaque(false);
            return resizableSubComponents;
        }

        public JComponent createResizableSplit(boolean horizontal, Component left, Component right) {
            double prevWidth = getSize(horizontal, left);
            double nextWidth = getSize(horizontal, right);

            JSplitPane pane = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, left, right);
            pane.setOpaque(false);
            pane.setBorder(BorderFactory.createEmptyBorder());
            pane.setDividerLocation(prevWidth / (prevWidth + nextWidth));
            return pane;
        }

        private double getSize(boolean horizontal, Component comp) {
            Dimension size = comp.getPreferredSize();
            return horizontal ? size.getWidth() : size.getHeight();
        }

        public void addAction(Action action) {
            Object name = action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
            if (actionToolBar == null) {
                initActionToolBar();
            }
            actionToolBar.add(new ActionButton(action));
            actions.add(action);
        }

        public void initActionToolBar() {
            actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);
            add(actionToolBar, BorderLayout.PAGE_START);
        }

        @Override
        public Object getSwingViewValue() {
            return context.getSource();
        }

        @Override
        public void setSwingViewValue(Object value) {
            context.setSource(value);
            revalidate();
            repaint();
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

    public static class ActionButton extends JButton {
        public ActionButton(Action a) {
            super(a);
            setBorderPainted(false);
            if (a instanceof GuiSwingActionDefault.ExecutionAction) {
                Icon icon = ((GuiSwingActionDefault.ExecutionAction) a).getActionPressedIcon();
                if (icon != null) {
                    setPressedIcon(icon);
                }
            }
        }
    }

}
