package autogui.swing.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * a menu builder for categorizing items.
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
public class PopupCategorized implements PopupExtension.PopupMenuBuilder {
    protected Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier;
    protected Consumer<CategorizedPopupItem> itemConsumer;

    /** the categorized menu item */
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

    /** a categorized menu item with a menu-item component.
     *  name and icon are ignored, the default category is same as itemLabel */
    public interface CategorizedPopupItemMenu extends CategorizedPopupItem {
        JComponent getMenuItem(PopupCategorized sender);

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

    public PopupCategorized(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        this(itemSupplier, null);
    }

    public PopupCategorized(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier,
                            Consumer<CategorizedPopupItem> itemConsumer) {
        this.itemSupplier = itemSupplier;
        this.itemConsumer = itemConsumer;
    }

    public Supplier<? extends Collection<CategorizedPopupItem>> getItemSupplier() {
        return itemSupplier;
    }

    public void setItemSupplier(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    @Override
    public void build(PopupExtension sender, Consumer<Object> menu) {
        Iterable<CategorizedPopupItem> items = itemSupplier.get();
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

            buildCategories(sender, menu, categorizedMenuItems, size);

            if (size == 0) {
                menu.accept(getMenuBuilder().createLabel("Nothing"));
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

    public void buildCategories(PopupExtension sender, Consumer<Object> menu,
                                Map<String,List<JComponent>> categorizedMenuItems, int totalSize) {
        MenuBuilder menuBuilder = getMenuBuilder();
        MenuBuilder.AddingProcess process = menuBuilder.addingProcess(menu, totalSize);
        for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
            if (e.getKey() != null && e.getKey().equals(CATEGORY_LABEL)) {
                menuBuilder.addMenuItems(process, e.getValue(), null);
            } else {
                menuBuilder.addMenuItems(process, e.getValue(), e.getKey());
            }
        }
    }

    public void select(CategorizedPopupItem item) {
        if (itemConsumer != null) {
            itemConsumer.accept(item);
        }
    }

    /**
     * an action for selecting a {@link CategorizedPopupItem}
     */
    public static class SearchPopupAction extends AbstractAction {
        protected PopupCategorized popup;
        protected CategorizedPopupItem item;

        public SearchPopupAction(PopupCategorized popup, CategorizedPopupItem item) {
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

    /**
     * another menu builder with limiting items
     */
    public static class PopupCategorizedFixed extends PopupCategorized {
        public PopupCategorizedFixed(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
            super(itemSupplier);
        }

        public PopupCategorizedFixed(Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier, Consumer<CategorizedPopupItem> itemConsumer) {
            super(itemSupplier, itemConsumer);
        }

        @Override
        public void buildCategories(PopupExtension sender, Consumer<Object> menu,
                                    Map<String,List<JComponent>> categorizedMenuItems, int totalSize) {
            MenuBuilder menuBuilder = getMenuBuilder();
            for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
                if (e.getKey() != null && e.getKey().equals(CATEGORY_LABEL)) {
                    menuBuilder.addMenuItems(menu, e.getValue(), null);
                } else {
                    menuBuilder.addMenuItems(menu, e.getValue(), e.getKey());
                }
            }
        }

        @Override
        public MenuBuilder getMenuBuilder() {
            return new MenuBuilder(15, 50);
        }
    }
}
