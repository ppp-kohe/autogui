package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * a utility for menu construction.
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

    protected int[] maxItems = { 30 };

    public static MenuBuilder get() {
        return instance;
    }

    public MenuBuilder() { }

    public MenuBuilder(int... maxItems) {
        this.maxItems = maxItems;
    }

    public int[] getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int... maxItems) {
        this.maxItems = maxItems;
    }

    public AddingProcess addingProcess(Consumer<Object> root, int size) {
        return new AddingProcess(root, size, maxItems);
    }

    public void addMenuItems(Consumer<Object> menu, List<? extends JComponent> items) {
        addMenuItems(menu, items, null, items.size());
    }

    public void addMenuItems(Consumer<Object> menu, List<? extends JComponent> items, String title) {
        addMenuItems(menu, items, title, items.size());
    }

    public void addMenuItems(Consumer<Object> menu, List<? extends JComponent> items, String title, int maxSize) {
        AddingProcess process = addingProcess(menu, maxSize);
        addMenuItems(process, items, title);
    }

    public void addMenuItems(AddingProcess process, List<? extends JComponent> items, String title) {
        Consumer<Object>  menu = process.getMenu();
        if (process.getCount() > 0 && !items.isEmpty()) {
            menu.accept(new JPopupMenu.Separator());
        }
        if (title != null && !title.startsWith(".")) {
            menu.accept(createLabel(title));
        }
        items.forEach(process::add);
    }

    public static String getImplicitCategory(String name) {
        return "." + name;
    }

    public static class AddingProcess {
        protected Consumer<Object> menu;
        protected int count;
        protected int[] max;
        protected int size;
        protected int level;

        public AddingProcess(Consumer<Object> menu, int size, int... max) {
            this.menu = menu;
            this.count = 0;
            this.max = max;
            this.level = 0;
            this.size = size;
        }

        public Consumer<Object> getMenu() {
            return menu;
        }

        public int getCount() {
            return count;
        }

        public void add(JComponent item) {
            if (item instanceof JMenuItem) {
                Action a = ((JMenuItem) item).getAction();
                if (a != null) {
                    item.setEnabled(a.isEnabled());
                }
            }
            menu.accept(item);
            ++count;
            --size;
            if (count >= max[level]) {
                count = 0;
                JMenu subMenu = new JMenu(size <= 0 ? "..." : (size + " Items"));
                subMenu.setForeground(Color.darkGray);
                menu.accept(subMenu);
                menu = new MenuAppender(subMenu);
                if (level + 1 < max.length) {
                    ++level;
                }
            }
        }
    }

    public JComponent createLabel(String name) {
        JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 3));
        JLabel label = new JLabel(name);
        label.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
        label.setForeground(Color.darkGray);
        comp.add(label);
        comp.setOpaque(false);
        return comp;
    }

    public MenuAppender createAppender(JComponent menu) {
        return new MenuAppender(menu);
    }

    public static class MenuAppender implements Consumer<Object> {
        protected JComponent menu;

        public MenuAppender(JComponent menu) {
            this.menu = menu;
        }

        @Override
        public void accept(Object o) {
            if (o instanceof Action) {
                if (menu instanceof JMenu) {
                    ((JMenu) menu).add((Action) o);
                } else if (menu instanceof JPopupMenu) {
                    ((JPopupMenu) menu).add((Action) o);
                } else {
                    menu.add(new JMenuItem((Action) o));
                }
            } else if (o instanceof JMenuItem) {
                menu.add((JMenuItem) o);
            } else if (o instanceof JComponent) {
                menu.add((JComponent) o);
            }
        }
    }
}
