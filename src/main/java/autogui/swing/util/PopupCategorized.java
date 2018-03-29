package autogui.swing.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * a menu builder for categorizing items.
 * <pre>
 *     [ labelItem1 //{@link CategorizedMenuItemLabel},
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
    protected Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier;
    protected Consumer<CategorizedMenuItem> itemConsumer;

    /** the categorized menu item */
    public interface CategorizedMenuItem {
        String getName();
        Icon getIcon();
        String getCategory();

        default String getSubCategory() {
            return "";
        }
    }

    public static String CATEGORY_LABEL = MenuBuilder.getImplicitCategory(".label");

    /** a label: special category */
    public interface CategorizedMenuItemLabel extends CategorizedMenuItem {
        @Override
        default String getCategory() {
            return CATEGORY_LABEL;
        }
    }

    /** a categorized menu item with a menu-item component.
     *  name and icon are ignored, the default category is same as itemLabel */
    public interface CategorizedMenuItemComponent extends CategorizedMenuItem {
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

    /**
     * an action with category info.
     */
    public interface CategorizedMenuItemAction extends CategorizedMenuItemComponent, Action {
        @Override
        default JComponent getMenuItem(PopupCategorized sender) {
            return new JMenuItem(this);
        }
    }

    /**
     * an action wraps another action for supplying category info. by sub-classing
     */
    public static class CategorizedMenuItemActionDelegate implements CategorizedMenuItemAction {
        protected Action action;

        public CategorizedMenuItemActionDelegate(Action action) {
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

    /**
     *
     * @param itemsList array of list of one of {@link CategorizedMenuItem}, {@link Action} or {@link JComponent}
     * @return a list of {@link CategorizedMenuItem} converted from arguments
     */
    public static List<CategorizedMenuItem> getMenuItems(List<?>... itemsList) {
        List<CategorizedMenuItem> result = new ArrayList<>();
        for (List<?> items : itemsList) {
            for (Object o : items) {
                try {
                    result.add(getMenuItem(o));
                } catch (IllegalArgumentException e) {
                    //skip
                }
            }
        }
        return result;
    }

    /**
     * @param item  one of {@link CategorizedMenuItem}, {@link Action} or {@link JComponent},
     *              otherwise, throws an exception
     * @return a {@link CategorizedMenuItem} converted from the item
     */
    public static CategorizedMenuItem getMenuItem(Object item) {
        if (item instanceof CategorizedMenuItem) {
            return (CategorizedMenuItem) item;
        } else if (item instanceof Action) {
            return new CategorizedMenuItemActionDelegate((Action) item);
        } else if (item instanceof JComponent) {
            return new CategorizedMenuItemComponent() {
                @Override
                public JComponent getMenuItem(PopupCategorized sender) {
                    return (JComponent) item;
                }
            };
        } else {
            throw new IllegalArgumentException("unsupported type: " + item.getClass());
        }
    }

    @SafeVarargs
    public static Supplier<? extends Collection<CategorizedMenuItem>> getMenuItemsSupplier(
            Supplier<? extends Collection<CategorizedMenuItem>>... itemsList) {
        return () ->
            Arrays.stream(itemsList)
                    .map(Supplier::get)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    public PopupCategorized(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier) {
        this(itemSupplier, null);
    }

    public PopupCategorized(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier,
                            Consumer<CategorizedMenuItem> itemConsumer) {
        this.itemSupplier = itemSupplier;
        this.itemConsumer = itemConsumer;
    }

    public Supplier<? extends Collection<CategorizedMenuItem>> getItemSupplier() {
        return itemSupplier;
    }

    public void setItemSupplier(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    @Override
    public void build(PopupExtensionSender sender, Consumer<Object> menu) {
        Iterable<CategorizedMenuItem> items = itemSupplier.get();
        if (items != null) {
            //category -> subCategory -> list item
            Map<String,Map<String,List<JComponent>>> subCategorizedMenuItems = new LinkedHashMap<>();
            subCategorizedMenuItems.put(CATEGORY_LABEL, new LinkedHashMap<>());

            int size = 0;
            for (CategorizedMenuItem item : items) {
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

    public JComponent createMenuItem(CategorizedMenuItem item) {
        if (item instanceof CategorizedMenuItemLabel) {
            return getMenuBuilder().createLabel(item.getName());
        } else if (item instanceof CategorizedMenuItemComponent) {
            return ((CategorizedMenuItemComponent) item).getMenuItem(this);
        } else if (item instanceof JComponent) {
            return (JComponent) item;
        } else if (item instanceof Action) {
            return new JMenuItem((Action) item);
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

    public void select(CategorizedMenuItem item) {
        if (itemConsumer != null) {
            itemConsumer.accept(item);
        }
    }

    /**
     * an action for selecting a {@link CategorizedMenuItem}
     */
    public static class SearchPopupAction extends AbstractAction {
        protected PopupCategorized popup;
        protected CategorizedMenuItem item;

        public SearchPopupAction(PopupCategorized popup, CategorizedMenuItem item) {
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
        public PopupCategorizedFixed(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier) {
            super(itemSupplier);
        }

        public PopupCategorizedFixed(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier, Consumer<CategorizedMenuItem> itemConsumer) {
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
