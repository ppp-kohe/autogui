package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class MenuBuilder {
    protected static MenuBuilder instance = new MenuBuilder();

    protected int maxItems = 5;
    protected int maxSubItems = 100;

    public static MenuBuilder get() {
        return instance;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public void setMaxSubItems(int maxSubItems) {
        this.maxSubItems = maxSubItems;
    }

    public int getMaxSubItems() {
        return maxSubItems;
    }

    public MenuBuilder addMenuItems(JComponent menu, List<? extends JComponent> items, String title) {
        if (menu.getComponentCount() > 0 && !items.isEmpty()) {
            menu.add(new JPopupMenu.Separator());
        }
        if (title != null) {
            menu.add(createLabel(title));
        }
        return addMenuItems(menu, items);
    }

    public MenuBuilder addMenuItems(JComponent menu, List<? extends JComponent> items) {
        addMenuItemsRecursive(menu, items.iterator(), maxItems, items.size());
        return this;
    }

    protected void addMenuItemsRecursive(JComponent menu, Iterator<? extends JComponent> iter, int max, int size) {
        int count = 0;
        while (count < max && iter.hasNext()) {
            JComponent comp = iter.next();
            if (comp instanceof JMenuItem) {
                comp.setEnabled(((JMenuItem) comp).getAction().isEnabled());
            }
            menu.add(comp);
            ++count;
            --size;
        }

        if (iter.hasNext()) {
            JMenu subMenu = new JMenu(size + " Items");
            addMenuItemsRecursive(menu, iter, maxSubItems, size);
            menu.add(subMenu);
        }
    }

    public JLabel createLabel(String name) {
        JLabel label = new JLabel(name);
        label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        label.setForeground(Color.darkGray);
        return label;
    }
}
