package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * <pre>
 *     [menu:
 *        JLabel("title"),
 *        item1,
 *        item2,
 *        item3,
 *        ...
 *        item_maxItems-1,
 *        JMenu("... items"): [
 *            item_maxItems,
 *            item_maxItems+1,
 *            ...
 *            item_maxItems+maxSubItems-1,
 *            JMenu("... items"): [ ...]
 *          ]]
 * </pre>
 */
public class MenuBuilder {
    protected static MenuBuilder instance = new MenuBuilder();

    protected int maxItems = 30;

    public static MenuBuilder get() {
        return instance;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public AddingProcess addingProcess(JComponent root, int size) {
        return new AddingProcess(root, maxItems, size);
    }

    public void addMenuItems(JComponent menu, List<? extends JComponent> items) {
        addMenuItems(menu, items, null, items.size());
    }

    public void addMenuItems(JComponent menu, List<? extends JComponent> items, String title) {
        addMenuItems(menu, items, title, items.size());
    }

    public void addMenuItems(JComponent menu, List<? extends JComponent> items, String title, int maxSize) {
        AddingProcess process = addingProcess(menu, maxSize);
        addMenuItems(process, items, title);
    }

    public void addMenuItems(AddingProcess process, List<? extends JComponent> items, String title) {
        JComponent menu = process.getMenu();
        if (menu.getComponentCount() > 0 && !items.isEmpty()) {
            menu.add(new JPopupMenu.Separator());
        }
        if (title != null) {
            menu.add(createLabel(title));
        }
        items.forEach(process::add);
    }

    public static class AddingProcess {
        protected JComponent menu;
        protected int count;
        protected int max;
        protected int size;

        public AddingProcess(JComponent menu, int max, int size) {
            this.menu = menu;
            this.count = 0;
            this.max = max;
            this.size = size;
        }

        public JComponent getMenu() {
            return menu;
        }

        public void add(JComponent item) {
            if (item instanceof JMenuItem) {
                item.setEnabled(((JMenuItem) item).getAction().isEnabled());;
            }
            menu.add(item);
            ++count;
            --size;
            if (count >= max) {
                count = 0;
                JMenu subMenu = new JMenu(size <= 0 ? "..." : (size + " Items"));
                menu.add(subMenu);
                menu = subMenu;
            }
        }
    }

    public JLabel createLabel(String name) {
        JLabel label = new JLabel(name);
        label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        label.setForeground(Color.darkGray);
        return label;
    }
}
