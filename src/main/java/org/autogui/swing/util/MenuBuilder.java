package org.autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
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
        addMenuSeparator(process, !items.isEmpty());
        addMenuTitle(process, title);
        items.forEach(process::add);
    }

    public boolean addMenuSeparator(AddingProcess process, boolean nonEmpty) {
        if (process.isSeparatorNeeded() && nonEmpty) {
            process.setSeparatorNeeded(false);
            process.getMenu().accept(new JPopupMenu.Separator());
            return true;
        } else {
            return false;
        }
    }

    public boolean addMenuTitle(AddingProcess process, String title) {
        if (title != null && !title.startsWith(".")) {
            process.getMenu().accept(createLabel(title));
            return true;
        } else {
            return false;
        }
    }

    public static String getCategoryImplicit(String name) {
        return "." + name;
    }

    public static String getCategoryName(String name) {
        if (name.startsWith(".")) {
            return name.substring(1);
        } else {
            return name;
        }
    }

    public static String getCategoryWithPrefix(String prefix, String name) {
        if (name.startsWith(".")) {
            return "." + prefix + name;
        } else {
            return prefix + name;
        }
    }

    /** intermediate state for constructing menus */
    public static class AddingProcess {
        protected Consumer<Object> menu;
        protected int count;
        protected int[] max;
        protected int size;
        protected int level;
        protected boolean separatorNeeded;

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

        public boolean isSeparatorNeeded() {
            return count > 0 || separatorNeeded;
        }

        public void setSeparatorNeeded(boolean separatorNeeded) {
            this.separatorNeeded = separatorNeeded;
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

    public MenuLabel createLabel(String name) {
        return createLabel(name, PopupCategorized.SUB_CATEGORY_LABEL_HEADER);
    }

    public MenuLabel createLabel(String name, String subCategory) {
        return new MenuLabel(name, subCategory);
    }

    /** a label menu-item component  */
    public static class MenuLabel extends JPanel implements PopupCategorized.CategorizedMenuItemComponent {
        @Serial private static final long serialVersionUID = 1L;
        protected String subCategory = "";
        protected JLabel label;
        protected String name;
        @SuppressWarnings("this-escape")
        public MenuLabel(String name, String subCategory) {
            super(new FlowLayout(FlowLayout.LEADING, 3, 3));
            this.name = name;
            label = new JLabel(name);
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(1);
            int l = ui.getScaledSizeInt(15);
            int r = ui.getScaledSizeInt(10);

            Color color = ui.getMenuDisabledForeground();

            if (subCategory.equals(PopupCategorized.SUB_CATEGORY_LABEL_VALUE)) {
                label.setBorder(BorderFactory.createEmptyBorder(h, l, h, r));
                label.setForeground(convertColor(color, fs -> {
                    fs[0] = 0.4f; //set hue
                    changeSaturation(fs);
                    changeBrightness(fs);
                }));
            } else if (subCategory.equals(PopupCategorized.SUB_CATEGORY_LABEL_TYPE)) {
                label.setBorder(BorderFactory.createEmptyBorder(h, l, h, r));
                label.setForeground(convertColor(color, fs -> {
                    fs[0] = 0.55f; //set hue
                    changeSaturation(fs);
                    changeBrightness(fs);
                }));
            } else if (subCategory.equals(PopupCategorized.SUB_CATEGORY_LABEL_MISC)) {
                label.setBorder(BorderFactory.createEmptyBorder(h, l, h, r));
                label.setForeground(convertColor(color, fs -> {
                    fs[0] = 0.1f; //set hue
                    changeSaturation(fs);
                    changeBrightness(fs);
                }));
            } else {
                label.setBorder(BorderFactory.createEmptyBorder(h, l, h, r));
                label.setForeground(convertColor(color, this::changeBrightness));
            }
            add(label);
            setOpaque(false);
            this.subCategory = subCategory;
        }

        private void changeSaturation(float[] fs) {
            fs[1] = Math.min(1.0f, fs[1] + 0.3f); //up saturation
        }

        private void changeBrightness(float[] fs) {
            if (fs[2] < 0.5f) { //check brightness
                fs[2] = Math.min(1.0f, fs[2] + 0.2f); //brighter
            } else {
                fs[2] = Math.max(0.0f, fs[2] - 0.2f); //darker
            }
        }

        public Color convertColor(Color base, Consumer<float[]> conv) {
            float[] fs = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
            conv.accept(fs);
            return Color.getHSBColor(fs[0], fs[1], fs[2]);
        }

        public JLabel getLabel() {
            return label;
        }

        public void setAdditionalInfo(String info) {
            label.setText(name + " "  +info);
        }

        @Override
        public JComponent getMenuItem() {
            return this;
        }

        @Override
        public String getCategory() {
            return PopupCategorized.CATEGORY_LABEL;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }
    }

    public MenuAppender createAppender(JComponent menu) {
        return new MenuAppender(menu);
    }

    /** an appender of menu-items: supporting {@link JMenu}, {@link Action}, {@link JPopupMenu}, {@link JMenuItem} and {@link JComponent} */
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
