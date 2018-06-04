package autogui.swing.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
public class PopupCategorized implements PopupExtension.PopupMenuBuilder, Cloneable {
    protected Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier;
    protected Consumer<CategorizedMenuItem> itemConsumer;
    protected MenuBuilder menuBuilder;

    /** the categorized menu item */
    public interface CategorizedMenuItem {
        String getName();
        Icon getIcon();

        /**
         * @return a category name, typically PopupExtension.MENU_CATEGORY_... or PopupCategorized.CATEGORY_...
         */
        String getCategory();

        /**
         * @return a sub-category name, typically PopupExtension.MENU_SUB_CATEGORY_... PopupCategorized.SUB_CATEGORY_...
         */
        default String getSubCategory() {
            return "";
        }

        CategorizedMenuItem remap(String category, String subCategory);
    }

    public static String CATEGORY_LABEL = MenuBuilder.getCategoryImplicit("Info");
    public static String CATEGORY_ACTION = MenuBuilder.getCategoryImplicit("Action");

    public static String SUB_CATEGORY_LABEL_HEADER = "header";
    public static String SUB_CATEGORY_LABEL_TYPE = "type";
    public static String SUB_CATEGORY_LABEL_VALUE = "value";
    public static String SUB_CATEGORY_LABEL_MISC = "misc";
    public static String SUB_CATEGORY_LABEL_ACTION = "action";

    /** a label: special category */
    public interface CategorizedMenuItemLabel extends CategorizedMenuItem {
        @Override
        default String getCategory() {
            return CATEGORY_LABEL;
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemLabelDelegate(this, category, subCategory);
        }
    }

    /** a categorized menu item with a menu-item component.
     *  name and icon are ignored, the default category is same as itemLabel */
    public interface CategorizedMenuItemComponent extends CategorizedMenuItem {
        JComponent getMenuItem();

        default JComponent getMenuItemWithAction(Action a) {
            return getMenuItem();
        }

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
            return CATEGORY_ACTION;
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemComponentDelegate(this, category, subCategory);
        }
    }

    /**
     * an action with category info.
     */
    public interface CategorizedMenuItemAction extends CategorizedMenuItemComponent, Action {
        @Override
        default JComponent getMenuItem() {
            return getMenuItemWithAction(this);
        }

        @Override
        default JComponent getMenuItemWithAction(Action a) {
            return new JMenuItem(a);
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionDelegate(this, category, subCategory);
        }
    }

    /**
     * an action with category info, which becomes an check-box menu item
     */
    public interface CategorizedMenuItemActionCheck extends CategorizedMenuItemAction {
        @Override
        default JComponent getMenuItemWithAction(Action a) {
            return new JCheckBoxMenuItem(a);
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionCheckDelegate(this, category, subCategory);
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
            return new CategorizedMenuItemComponentDefault((JComponent) item);
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
        this(itemSupplier, itemConsumer, MenuBuilder.get());
    }

    public PopupCategorized(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier,
                            Consumer<CategorizedMenuItem> itemConsumer, MenuBuilder menuBuilder) {
        this.itemSupplier = itemSupplier;
        this.itemConsumer = itemConsumer;
        this.menuBuilder = menuBuilder;
    }

    public Supplier<? extends Collection<CategorizedMenuItem>> getItemSupplier() {
        return itemSupplier;
    }

    public void setItemSupplier(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    @Override
    public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
        PopupCategorizedBuildingItems items = new PopupCategorizedBuildingItems(filter, itemSupplier.get());

        //category -> subCategory -> list item:
        Map<String,Map<String,List<JComponent>>> subCategorizedMenuItems = createNewCategoryToSubCategoryToItems();

        int size = 0;
        for (CategorizedMenuItem item : items.categorizedItems) {
            String category = item.getCategory();
            subCategorizedMenuItems.computeIfAbsent(category, (c) -> createNewSubCategoryToItems())
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

        //remove empty categories
        new ArrayList<>(categorizedMenuItems.keySet())
                .forEach(k -> categorizedMenuItems.computeIfPresent(k, (ek,v) -> v.isEmpty() ? null : v));

        getMenuBuilder().addMenuItems(menu, createMenuItems(items.beforeItems));
        buildCategories(menu, categorizedMenuItems, size);
        getMenuBuilder().addMenuItems(menu, createMenuItems(items.afterItems));

        if (size == 0) {
            Object none = filter.convert(getMenuBuilder().createLabel("Nothing"));
            if (none != null) {
                menu.accept(none);
            }
        }
    }

    public static class PopupCategorizedBuildingItems {
        public List<Object> beforeItems;
        public List<CategorizedMenuItem> categorizedItems;
        public List<Object> afterItems;

        public PopupCategorizedBuildingItems(PopupExtension.PopupMenuFilter filter, Iterable<CategorizedMenuItem> items) {
            beforeItems = new ArrayList<>();
            afterItems = new ArrayList<>();
            this.categorizedItems = new ArrayList<>();
            separate(filter.aroundItems(true), this.categorizedItems, beforeItems);
            if (items != null) {
                this.categorizedItems.addAll(filter.convertItems(CategorizedMenuItem.class, items));
            }
            separate(filter.aroundItems(false), this.categorizedItems, afterItems);
        }

        private void separate(List<Object> src, List<CategorizedMenuItem> categorized, List<Object> objects) {
            for (Object o : src) {
                if (o instanceof CategorizedMenuItem) {
                    categorized.add((CategorizedMenuItem) o);
                } else {
                    objects.add(o);
                }
            }
        }
    }

    public Map<String,Map<String,List<JComponent>>> createNewCategoryToSubCategoryToItems() {
        Map<String,Map<String,List<JComponent>>> subCategorizedMenuItems = new LinkedHashMap<>();
        getPredefinedCategoriesOrder().forEach(k ->
                subCategorizedMenuItems.put(k, createNewSubCategoryToItems()));
        return subCategorizedMenuItems;
    }

    public Map<String,List<JComponent>> createNewSubCategoryToItems() {
        //LinkedHashMap is important factor for the mechanism
        LinkedHashMap<String,List<JComponent>> comps = new LinkedHashMap<>();
        getPredefinedSubCategoriesOrder().forEach(k ->
            comps.put(k, new ArrayList<>()));
        return comps;
    }

    /**
     * @return always initially added categories.
     *   Note: categories which has no items will be removed after adding,
     *         and thus there are no separators for the categories
     */
    public List<String> getPredefinedCategoriesOrder() {
        return Arrays.asList(
                CATEGORY_LABEL,
                CATEGORY_ACTION);
    }

    /**
     * @return always initially added sub-categories for any categories
     */
    public List<String> getPredefinedSubCategoriesOrder() {
        return Arrays.asList(
                SUB_CATEGORY_LABEL_HEADER,
                SUB_CATEGORY_LABEL_TYPE,
                SUB_CATEGORY_LABEL_VALUE,
                SUB_CATEGORY_LABEL_MISC,
                SUB_CATEGORY_LABEL_ACTION);
    }

    public MenuBuilder getMenuBuilder() {
        return menuBuilder;
    }

    public void setMenuBuilder(MenuBuilder menuBuilder) {
        this.menuBuilder = menuBuilder;
    }

    public JComponent createMenuItem(CategorizedMenuItem item) {
        if (item instanceof CategorizedMenuItemLabel) {
            return getMenuBuilder().createLabel(item.getName());
        } else if (item instanceof CategorizedMenuItemComponent) {
            return ((CategorizedMenuItemComponent) item).getMenuItem();
        } else if (item instanceof JComponent) {
            return (JComponent) item;
        } else if (item instanceof Action) {
            return new JMenuItem((Action) item);
        } else {
            return new JMenuItem(new SearchPopupAction(this, item));
        }
    }

    public JComponent createMenuItemFromObject(Object i) {
        if (i instanceof PopupCategorized.CategorizedMenuItem) {
            return createMenuItem((PopupCategorized.CategorizedMenuItem) i);
        } else if (i instanceof JComponent) {
            return (JComponent) i;
        } else if (i instanceof Action) {
            return new JMenuItem((Action) i);
        } else {
            return null;
        }
    }

    public List<JComponent> createMenuItems(List<Object> items) {
        List<JComponent> cs = new ArrayList<>();
        for (Object i : items) {
            JComponent c = createMenuItemFromObject(i);
            if (c != null) {
                cs.add(c);
            }
        }
        return cs;
    }

    public void buildCategories(Consumer<Object> menu,
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
        public void buildCategories(Consumer<Object> menu,
                                    Map<String,List<JComponent>> categorizedMenuItems, int totalSize) {
            MenuBuilder menuBuilder = getMenuBuilder();
            MenuBuilder.AddingProcess process = menuBuilder.addingProcess(menu, totalSize);
            boolean first = false;
            for (Map.Entry<String, List<JComponent>> e : categorizedMenuItems.entrySet()) {
                process.setSeparatorNeeded(first);
                first = true;
                menuBuilder.addMenuItems(process, e.getValue(), e.getKey());
            }
        }

        @Override
        public MenuBuilder getMenuBuilder() {
            return new MenuBuilder(15, 50);
        }
    }

    public static class CategorizedMenuItemLabelDelegate implements CategorizedMenuItemLabel {
        protected CategorizedMenuItemLabel label;
        protected String category;
        protected String subCategory;

        public CategorizedMenuItemLabelDelegate(CategorizedMenuItemLabel label) {
            this.label = label;
            category = label.getCategory();
            subCategory = label.getSubCategory();
        }

        public CategorizedMenuItemLabelDelegate(CategorizedMenuItemLabel label, String category, String subCategory) {
            this.label = label;
            this.category = category;
            this.subCategory = subCategory;
        }

        @Override
        public String getName() {
            return label.getName();
        }

        @Override
        public Icon getIcon() {
            return label.getIcon();
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemLabelDelegate(label, category, subCategory);
        }
    }

    public static class CategorizedMenuItemComponentDelegate implements CategorizedMenuItemComponent  {
        protected CategorizedMenuItemComponent component;
        protected String category = "";
        protected String subCategory = "";

        public CategorizedMenuItemComponentDelegate(CategorizedMenuItemComponent component) {
            this.component = component;
            category = component.getCategory();
            subCategory = component.getSubCategory();
        }

        public CategorizedMenuItemComponentDelegate(CategorizedMenuItemComponent component, String category, String subCategory) {
            this.component = component;
            this.category = category;
            this.subCategory = subCategory;
        }

        @Override
        public JComponent getMenuItem() {
            return component.getMenuItem();
        }

        @Override
        public String getName() {
            return component.getName();
        }

        @Override
        public Icon getIcon() {
            return component.getIcon();
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemComponentDelegate(component, category, subCategory);
        }
    }

    public static class CategorizedMenuItemComponentDefault implements CategorizedMenuItemComponent  {
        protected JComponent component;
        protected String category = "";
        protected String subCategory = "";

        public CategorizedMenuItemComponentDefault(JComponent component) {
            this.component = component;
            if (component instanceof JLabel) {
                category = CATEGORY_LABEL;
            } else {
                category = CATEGORY_ACTION;
            }
        }

        public CategorizedMenuItemComponentDefault(JComponent component, String category, String subCategory) {
            this.component = component;
            this.category = category;
            this.subCategory = subCategory;
        }

        @Override
        public JComponent getMenuItem() {
            return component;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemComponentDefault(component, category, subCategory);
        }
    }


    /**
     * an action wraps another action for supplying category info.
     */
    public static class CategorizedMenuItemActionDelegate implements CategorizedMenuItemAction {
        protected Action action;
        protected String category = PopupCategorized.CATEGORY_ACTION;
        protected String subCategory = "";

        public CategorizedMenuItemActionDelegate(Action action) {
            this.action = action;
            if (action instanceof CategorizedMenuItem) {
                this.category = ((CategorizedMenuItem) action).getCategory();
                this.subCategory = ((CategorizedMenuItem) action).getSubCategory();
            }
        }

        public CategorizedMenuItemActionDelegate(Action action, String category, String subCategory) {
            this.action = action;
            this.category = category;
            this.subCategory = subCategory;
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

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionDelegate(action, category, subCategory);
        }

        public Action getAction() {
            return action;
        }
    }

    public static class CategorizedMenuItemActionCheckDelegate extends CategorizedMenuItemActionDelegate {
        public CategorizedMenuItemActionCheckDelegate(Action action) {
            super(action);
        }

        public CategorizedMenuItemActionCheckDelegate(Action action, String category, String subCategory) {
            super(action, category, subCategory);
        }

        @Override
        public JComponent getMenuItem() {
            return getMenuItemWithAction(action);
        }

        @Override
        public JComponent getMenuItemWithAction(Action a) {
            return new JCheckBoxMenuItem(action);
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionCheckDelegate(action, category, subCategory);
        }
    }
}
