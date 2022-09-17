package org.autogui.swing.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * the special button for a tool-bar that can collect overflow components as a popup-menu.
 * <pre>
 *     new ToolBarHiddenMenu().addTo(toolBar);
 * </pre>
 * The minimum-size of the toolBar will be 1.
 * @since 1.6
 */
public class ToolBarHiddenMenu extends JButton implements HierarchyBoundsListener, PropertyChangeListener {
    protected JPopupMenu menu;
    protected List<InvisibleItem> invisibleComponents = new ArrayList<>();
    protected Component glue;
    public ToolBarHiddenMenu() {
        setOpaque(false);
        setVisible(false);
        setAlignmentX(0.5f);
        setAlignmentY(0.5f);
        setBorder(BorderFactory.createEmptyBorder());
        setIcon(new IconDots());
        addActionListener(this::showMenu);
        menu = new JPopupMenu();
        setComponentPopupMenu(menu);
    }

    public void showMenu(ActionEvent e) {
        menu.show(this, 0, getHeight());
    }

    public void addTo(JToolBar bar) {
        glue = Box.createGlue();
        bar.add(glue);
        bar.add(this);
        bar.addHierarchyBoundsListener(this);
        bar.addPropertyChangeListener("orientation", this);
        bar.setMinimumSize(getPreferredSize());
    }

    @Override
    public void ancestorMoved(HierarchyEvent e) {}

    @Override
    public void ancestorResized(HierarchyEvent e) {
        updateToolBar();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateToolBar();
    }

    public void updateToolBar() {
        Container c = getParent();
        if (c instanceof JToolBar) {
            updateToolBar((JToolBar) c);
        }
    }

    protected boolean includeAsVisibleTarget(Component item) {
        return item instanceof JComponent && !(item instanceof Box.Filler) && item != this && item.isVisible();
    }

    public void updateToolBar(JToolBar bar) {
        adjustHiddenMenuComponents(bar);
        List<JComponent> newInvisibleComponents = collectExceededComponents(bar);
        List<InvisibleItem> newInvisibleItems = new ArrayList<>();
        if (!newInvisibleComponents.isEmpty()) {
            menu.removeAll();
            for (var barComponent : newInvisibleComponents) {
                var bounds = barComponent.getBounds();
                bar.remove(barComponent);
                newInvisibleItems.add(new InvisibleItem(barComponent, addMenuItem(barComponent), bounds));
            }
            for (var item : invisibleComponents) {
                menu.add(item.getMenuComponent());
            }
            invisibleComponents.addAll(0, newInvisibleItems);
        } else { //it might have sufficient space for invisible items
            for (var item : collectVisibleItems(bar)) {
                menu.remove(item.getMenuComponent());
                int idx = bar.getComponentIndex(glue);
                bar.add(item.getToolBarComponent(), idx);
                invisibleComponents.remove(item);
            }
        }
        setVisible(!invisibleComponents.isEmpty());
    }

    public void adjustHiddenMenuComponents(JToolBar bar) {
        if (bar.getComponentIndex(glue) == 0 && bar.getComponentCount() > 2) { //not only [glue, this]
            //reorder glue and this
            bar.remove(glue);
            bar.remove(this);
            bar.add(glue);
            bar.add(this);
        }
    }

    public List<JComponent> collectExceededComponents(JToolBar bar) {
        int orientation = bar.getOrientation();
        int totalSize = width(bar.getVisibleRect(), orientation);
        totalSize -= width(getPreferredSize(), orientation); //exclude this button

        List<JComponent> invisible = new ArrayList<>();
        for (var item : bar.getComponents()) {
            var bounds = item.getBounds();
            if (includeAsVisibleTarget(item) && end(bounds, orientation) > totalSize) {
                invisible.add((JComponent) item);
            }
        }
        System.err.println();
        return invisible;
    }

    private JComponent addMenuItem(JComponent item) {
        Action action;
        if (item instanceof AbstractButton &&
                (action = ((AbstractButton) item).getAction()) != null) {
            return menu.add(action);
        } else {
            menu.add(item);
            return item;
        }
    }

    public List<InvisibleItem> collectVisibleItems(JToolBar bar) {
        int orientation = bar.getOrientation();
        int totalSize = width(bar.getSize(), orientation);
        totalSize -= width(getPreferredSize(), orientation);
        List<InvisibleItem> availableItems = new ArrayList<>();
        int maxEnd = 0;
        for (var existing : bar.getComponents()) {
            if (includeAsVisibleTarget(existing)) {
                maxEnd = Math.max(maxEnd, end(existing.getBounds(), orientation));
            }
        }
        for (var last : invisibleComponents) {;
            int lastWidth = width(last.getToolBarBounds(), orientation);
            if (maxEnd + lastWidth <= totalSize) {
                availableItems.add(last);
                maxEnd += lastWidth;
            } else {
                break;
            }
        }
        return availableItems;
    }

    private int end(Rectangle r, int orientation) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return r.x + r.width;
        } else {
            return r.y + r.height;
        }
    }

    private int width(Dimension r, int orientation) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return r.width;
        } else {
            return r.height;
        }
    }

    private int width(Rectangle r, int orientation) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return r.width;
        } else {
            return r.height;
        }
    }

    public static class InvisibleItem {
        protected JComponent toolBarComponent;
        protected JComponent menuComponent;
        protected Rectangle toolBarBounds;

        public InvisibleItem(JComponent toolBarComponent, JComponent menuComponent, Rectangle toolBarBounds) {
            this.toolBarComponent = toolBarComponent;
            this.menuComponent = menuComponent;
            this.toolBarBounds = toolBarBounds;
        }

        public JComponent getMenuComponent() {
            return menuComponent;
        }

        public JComponent getToolBarComponent() {
            return toolBarComponent;
        }

        public Rectangle getToolBarBounds() {
            return toolBarBounds;
        }
    }

    public static class IconDots implements Icon {

        protected float getDotSize() {
            return UIManagerUtil.getInstance().getScaledSizeFloat(3);
        }

        protected float getDotMargin() {
            return getDotSize() / 2f;
        }

        protected float getIconWidthFloat() {
            return getDotMargin() * 2f + getDotSize();
        }

        protected float getIconHeightFloat() {
            var size = getDotSize();
            var margin = getDotMargin();
            return margin * 4f + size * 3f;
        }


        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            var size = getDotSize();
            var margin = getDotMargin();
            var dot = new Ellipse2D.Float(0, 0, size, size);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate((getIconWidth() - getIconWidthFloat()) / 2f, 0);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.translate(x + margin, y + margin);
            g2.fill(dot);
            g2.translate(0, size + margin);
            g2.fill(dot);
            g2.translate(0, size + margin);
            g2.fill(dot);
        }

        @Override
        public int getIconWidth() {
            //return (int) getIconWidthFloat();
            return getIconHeight(); //as square
        }

        @Override
        public int getIconHeight() {
            return (int) getIconHeightFloat();
        }
    }
}