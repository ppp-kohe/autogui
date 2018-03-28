package autogui.swing.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

        default String getSubCategory() {
            return "";
        }
    }

    public static String CATEGORY_LABEL = MenuBuilder.getImplicitCategory(".label");

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

    public interface CategorizedPopupItemMenuAction extends CategorizedPopupItemMenu, Action {
        @Override
        default JComponent getMenuItem(PopupCategorized sender) {
            return new JMenuItem(this);
        }
    }

    public static class CategorizedPopupItemMenuActionDelegate implements CategorizedPopupItemMenuAction {
        protected Action action;

        public CategorizedPopupItemMenuActionDelegate(Action action) {
            this.action = action;
        }

        @Override
        public Object getValue(String key) {
            return action.getValue(key);
        }

        @Override
        public void putValue(String key, Object value) {
            action.putValue(key, value);
        }

        @Override
        public void setEnabled(boolean b) {
            action.setEnabled(b);
        }

        @Override
        public boolean isEnabled() {
            return action.isEnabled();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            action.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            action.removePropertyChangeListener(listener);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
    }

    public static Supplier<? extends Collection<CategorizedPopupItem>> getSupplierWithActions(
            List<? extends Action> topActions,
            Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        return () ->
                Stream.concat(
                    Stream.concat(
                            topActions.stream()
                                .filter(CategorizedPopupItem.class::isInstance)
                                .map(CategorizedPopupItem.class::cast),
                            topActions.stream()
                                .filter(c -> !CategorizedPopupItem.class.isInstance(c))
                                .map(CategorizedPopupItemMenuActionDelegate::new)),
                    itemSupplier.get().stream())
                .collect(Collectors.toList());
    }

    public static Supplier<? extends Collection<CategorizedPopupItem>> getSupplierWithMenuItems(
            List<? extends JComponent> topItems,
            Supplier<? extends Collection<CategorizedPopupItem>> itemSupplier) {
        return () ->
                Stream.concat(
                    Stream.concat(
                            topItems.stream()
                                    .filter(CategorizedPopupItem.class::isInstance)
                                    .map(CategorizedPopupItem.class::cast),
                            topItems.stream()
                                    .filter(c -> !CategorizedPopupItem.class.isInstance(c))
                                    .<CategorizedPopupItemMenu>map(i -> (sender) -> i)),
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
    public void build(PopupExtensionSender sender, Consumer<Object> menu) {
        Iterable<CategorizedPopupItem> items = itemSupplier.get();
        if (items != null) {
            //category -> subCategory -> list item
            Map<String,Map<String,List<JComponent>>> subCategorizedMenuItems = new LinkedHashMap<>();
            subCategorizedMenuItems.put(CATEGORY_LABEL, new LinkedHashMap<>());

            int size = 0;
            for (CategorizedPopupItem item : items) {
                String category = item.getCategory();
                subCategorizedMenuItems.computeIfAbsent(category, (c) -> new LinkedHashMap<>())
                        .computeIfAbsent(item.getSubCategory(), (sc) -> new ArrayList<>())
                        .add(createMenuItem(item));
                ++size;
            }

            Map<String,List<JComponent>> categorizedMenuItems = new LinkedHashMap<>();

            //flatten sub-category maps to lists
            subCategorizedMenuItems.forEach((k,map) ->
                    map.values().stream()
                                .flatMap(List::stream)
                                .forEach(categorizedMenuItems.computeIfAbsent(k, (c) -> new ArrayList<>())::add));

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

    public void buildCategories(PopupExtensionSender sender, Consumer<Object> menu,
                                Map<String,List<JComponent>> categorizedMenuItems, int totalSize) {
        MenuBuilder menuBuilder = getMenuBuilder();
        MenuBuilder.AddingProcess process = menuBuilder.addingProcess(menu, totalSize);
        for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
            menuBuilder.addMenuItems(process, e.getValue(), e.getKey());
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
        public void buildCategories(PopupExtensionSender sender, Consumer<Object> menu,
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
