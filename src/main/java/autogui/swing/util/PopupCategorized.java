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

    /** the categorized menu item */
    public interface CategorizedMenuItem {
        String getName();
        Icon getIcon();
        String getCategory();

        default String getSubCategory() {
            return "";
        }

        CategorizedMenuItem remap(String category, String subCategory);
    }

    public static String CATEGORY_LABEL = MenuBuilder.getCategoryImplicit("Info");
    public static String CATEGORY_ACTION = MenuBuilder.getCategoryImplicit("Action");

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
            return CATEGORY_ACTION;
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemComponentDelegate(this, category, subCategory);
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
        public JComponent getMenuItem(PopupCategorized sender) {
            return component.getMenuItem(sender);
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
        public JComponent getMenuItem(PopupCategorized sender) {
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
     * an action with category info.
     */
    public interface CategorizedMenuItemAction extends CategorizedMenuItemComponent, Action {
        @Override
        default JComponent getMenuItem(PopupCategorized sender) {
            return new JMenuItem(this);
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
        default JComponent getMenuItem(PopupCategorized sender) {
            return new JCheckBoxMenuItem(this);
        }

        @Override
        default CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionCheckDelegate(this, category, subCategory);
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
    }

    public static class CategorizedMenuItemActionCheckDelegate extends CategorizedMenuItemActionDelegate {
        public CategorizedMenuItemActionCheckDelegate(Action action) {
            super(action);
        }

        public CategorizedMenuItemActionCheckDelegate(Action action, String category, String subCategory) {
            super(action, category, subCategory);
        }

        @Override
        public JComponent getMenuItem(PopupCategorized sender) {
            return new JCheckBoxMenuItem(action);
        }

        @Override
        public CategorizedMenuItem remap(String category, String subCategory) {
            return new CategorizedMenuItemActionCheckDelegate(action, category, subCategory);
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
            subCategorizedMenuItems.put(CATEGORY_ACTION, new LinkedHashMap<>());

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
                menuBuilder.addMenuItems(menu, e.getValue(), e.getKey());
            }
        }

        @Override
        public MenuBuilder getMenuBuilder() {
            return new MenuBuilder(15, 50);
        }
    }
}
