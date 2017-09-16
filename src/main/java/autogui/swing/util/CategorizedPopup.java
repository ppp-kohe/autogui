package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CategorizedPopup extends AbstractAction {
    protected JPopupMenu menu;
    protected JComponent button;
    protected Supplier<? extends Iterable<CategorizedPopupItem>> itemSupplier;
    protected Consumer<CategorizedPopupItem> itemConsumer;

    public interface CategorizedPopupItem {
        String getName();
        Icon getIcon();
        String getCategory();
    }

    public static String CATEGORY_LABEL = "#label";

    /** a label: special category */
    public interface CategorizedPopupItemLabel extends CategorizedPopupItem {
        @Override
        default String getCategory() {
            return CATEGORY_LABEL;
        }
    }

    public interface CategorizedPopupItemMenu extends CategorizedPopupItem {
        JComponent getMenuItem(CategorizedPopup sender);
    }

    public CategorizedPopup(Supplier<? extends Iterable<CategorizedPopupItem>> itemSupplier,
                            Consumer<CategorizedPopupItem> itemConsumer) {
        this.itemSupplier = itemSupplier;
        this.itemConsumer = itemConsumer;
        initMenu();
    }


    public void initMenu() {
        menu = new JPopupMenu();
        //TODO putValue(LARGE_ICON_KEY, );
    }

    public CategorizedPopup(Supplier<? extends Iterable<CategorizedPopupItem>> itemSupplier,
                            Consumer<CategorizedPopupItem> itemConsumer, JComponent button) {
        this(itemSupplier, itemConsumer);
        setButton(button);
    }

    public void setButton(JComponent button) {
        this.button = button;
    }

    public JComponent getButton() {
        return button;
    }

    public Supplier<? extends Iterable<CategorizedPopupItem>> getItemSupplier() {
        return itemSupplier;
    }

    public void setItemSupplier(Supplier<? extends Iterable<CategorizedPopupItem>> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    public JPopupMenu getMenu() {
        return menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        show(button, 0, button.getHeight());
    }

    public void show(Component comp, int x, int y) {
        setupMenu(menu);
        menu.show(comp, x, y);
    }

    public void setupMenu(JPopupMenu menu) {
        Iterable<CategorizedPopupItem> items = itemSupplier.get();
        menu.removeAll();
        if (items != null) {
            Map<String,List<JComponent>> categorizedMenuItems = new LinkedHashMap<>();
            categorizedMenuItems.put(CATEGORY_LABEL, new ArrayList<>());

            for (CategorizedPopupItem item : items) {
                String category = item.getCategory();
                categorizedMenuItems.computeIfAbsent(category, (c) -> new ArrayList<>())
                        .add(createMenuItem(item));
            }

            MenuBuilder menuBuilder = getMenuBuilder();
            for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
                if (e.getKey() != null && e.getKey().equals(CATEGORY_LABEL)) {
                    e.getValue().forEach(menu::add);
                } else {
                    menuBuilder.addMenuItems(menu, e.getValue(), e.getKey());
                }
            }

            if (menu.getComponentCount() == 0) {
                menu.add(menuBuilder.createLabel("Nothing"));
            }
        }
    }

    public MenuBuilder getMenuBuilder() {
        return MenuBuilder.get();
    }

    public JComponent createMenuItem(CategorizedPopupItem item) {
        if (item instanceof CategorizedPopupItemLabel) {
            return getMenuBuilder().createLabel(((CategorizedPopupItemLabel) item).getName());
        } else if (item instanceof CategorizedPopupItemMenu) {
            return ((CategorizedPopupItemMenu) item).getMenuItem(this);
        } else {
            return new JMenuItem(new SearchPopupAction(this, item));
        }
    }

    public void select(CategorizedPopupItem item) {
        if (itemConsumer != null) {
            itemConsumer.accept(item);
        }
    }

    public static class SearchPopupAction extends AbstractAction {
        protected CategorizedPopup popup;
        protected CategorizedPopupItem item;

        public SearchPopupAction(CategorizedPopup popup, CategorizedPopupItem item) {
            this.popup = popup;
            this.item = item;
            putValue(NAME, item.getName());
            putValue(SMALL_ICON, item.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (popup != null) {
                popup.select(item);
            }
        }
    }
}
