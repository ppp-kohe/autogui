package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <pre>
 *     CategorizedPopup popup = new CategorizedPopup(supplier, consumer);
 *     ...
 *     new MouseAdapter() {
 *         public void mousePressed(MouseEvent e) {
 *             if (e.isPopupTrigger()) {
 *                 popup.show(e.getComponent(), e.getX(), e.getY());
 *             }
 *         }
 *     };
 *     ...
 *     //or
 *     JPopupMenu menu = ...;
 *     popup.setupMenu(menu);
 * </pre>
 *
 * <pre>
 *     [ labelItem1 //{@link CategorizedPopupItemLabel},
 *       labelItem2,
 *       ...
 *      category1,
 *      [item1_1: icon, name],
 *      [item1_2  ... ],
 *      ...
 *      category2,
 *      item2_1,
 *      item2_2,
 *      ...,
 *      ]
 * </pre>
 */
public class CategorizedPopup extends AbstractAction {
    protected JPopupMenu menu;
    protected JComponent button;
    protected Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier;
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

    /** name and icon are ignored, the default category is same as itemLabel */
    public interface CategorizedPopupItemMenu extends CategorizedPopupItem {
        JComponent getMenuItem(CategorizedPopup sender);

        @Override
        default String getName() {
            return "";
        }

        @Override
        default Icon getIcon() {
            return null;
        }

        @Override
        default String getCategory() {
            return CATEGORY_LABEL;
        }
    }


    public static Supplier<? extends Collection<CategorizedPopupItem>> getSupplierWithActions(
            List<? extends Action> topActions,
            Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        return getSupplierWithMenuItems(topActions.stream()
                .map(JMenuItem::new)
                .collect(Collectors.toList()), itemSupplier);
    }

    public static Supplier<? extends Collection<CategorizedPopupItem>> getSupplierWithMenuItems(
            List<? extends JComponent> topItems,
            Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        return () ->
                Stream.concat(
                        topItems.stream()
                                .<CategorizedPopupItemMenu>map(i -> (sender) -> i),
                        itemSupplier.get().stream())
                        .collect(Collectors.toList());
    }

    public CategorizedPopup(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        this(itemSupplier, null);
    }

    public CategorizedPopup(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier,
                            Consumer<CategorizedPopupItem> itemConsumer) {
        this.itemSupplier = itemSupplier;
        this.itemConsumer = itemConsumer;
        initMenu();
    }

    public CategorizedPopup(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier,
                            Consumer<CategorizedPopupItem> itemConsumer, JComponent button) {
        this(itemSupplier, itemConsumer);
        setButton(button);
    }


    public void initMenu() {
        menu = new JPopupMenu();
        //TODO putValue(LARGE_ICON_KEY, );
    }

    public void setButton(JComponent button) {
        this.button = button;
    }

    public JComponent getButton() {
        return button;
    }

    public Supplier<? extends Collection<CategorizedPopupItem>> getItemSupplier() {
        return itemSupplier;
    }

    public void setItemSupplier(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
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

            int size = 0;
            for (CategorizedPopupItem item : items) {
                String category = item.getCategory();
                categorizedMenuItems.computeIfAbsent(category, (c) -> new ArrayList<>())
                        .add(createMenuItem(item));
                ++size;
            }

            MenuBuilder menuBuilder = getMenuBuilder();
            MenuBuilder.AddingProcess process = menuBuilder.addingProcess(menu, size);
            for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
                if (e.getKey() != null && e.getKey().equals(CATEGORY_LABEL)) {
                    menuBuilder.addMenuItems(process, e.getValue(), null);
                } else {
                    menuBuilder.addMenuItems(process, e.getValue(), e.getKey());
                }
            }

            if (size == 0) {
                menu.add(menuBuilder.createLabel("Nothing"));
            }
        }
    }

    public MenuBuilder getMenuBuilder() {
        return MenuBuilder.get();
    }

    public JComponent createMenuItem(CategorizedPopupItem item) {
        if (item instanceof CategorizedPopupItemLabel) {
            return getMenuBuilder().createLabel(item.getName());
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
