package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.TableTargetMenu;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiSwingHistoryMenu<ValueType, PaneType extends GuiSwingView.ValuePane<ValueType>> extends JMenu implements TableTargetMenu,
        PopupCategorized.CategorizedMenuItemComponent {
    protected PaneType component;
    protected GuiMappingContext context;
    protected GuiSwingView.ContextAction runner;

    public GuiSwingHistoryMenu(PaneType component, GuiMappingContext context) {
        super("History");
        this.component = component;
        this.context = context;
        runner = new GuiSwingView.ContextAction(context);
        buildMenu();

    }

    protected void buildMenu() {
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                loadItems();
            }
            @Override
            public void menuDeselected(MenuEvent e) { }
            @Override
            public void menuCanceled(MenuEvent e) { }
        });
    }

    @Override
    public JComponent getMenuItem() {
        return this;
    }

    @Override
    public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
        return new HistoryMenuForTableColumn<>(component, context, target);
    }

    public void clearHistory() {
        context.getPreferences().clearHistories();
    }

    public void loadItems() {
        removeAll();
        boolean added = false;
        List<GuiPreferences.HistoryValueEntry> prefEs = context.getPreferences().getHistoryValues();
        List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(prefEs);
        Collections.reverse(es);
        for (GuiPreferences.HistoryValueEntry e : es) {
            if (e.getIndex() != -1 && e.getValue() != null) {
                addAction(e);
                added = true;
            }
        }
        if (!added) {
            JMenuItem nothing = new JMenuItem("Nothing");
            nothing.setEnabled(false);
            add(nothing);
        }
        HistoryClearAction clearAction = new HistoryClearAction(this);
        if (!added) {
            clearAction.setEnabled(false);
        }
        addSeparator();
        add(clearAction);
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void addAction(GuiPreferences.HistoryValueEntry e) {
        JMenuItem item = new JMenuItem(createAction(e));
        item.setText(item.getText() + " : " + formatter.format(LocalDateTime.ofInstant(e.getTime(), ZoneId.systemDefault())));
        add(item);
    }

    @SuppressWarnings("unchecked")
    public Action createAction(GuiPreferences.HistoryValueEntry e) {
        ValueType v = (ValueType) e.getValue();
        return new HistorySetAction<>(getActionName(e), v, component);
    }

    public String getActionName(GuiPreferences.HistoryValueEntry e) {
        Object v = e.getValue();
        String name = runner.execute(() ->
                context.getRepresentation().toHumanReadableString(context, v),
                "ERROR: Timeout", "ERROR: Cancel", null);
        return getActionNameFromString(name);
    }

    public String getActionNameFromString(String name) {
        int max = getMaxNameLength();
        if (name.length() > max) {
            name = name.substring(0, max) + "...";
        }
        return name;
    }

    public int getMaxNameLength() {
        return 30;
    }

    @Override
    public String getCategory() {
        return PopupExtension.MENU_CATEGORY_SET;
    }

    public static class HistoryMenuForTableColumn<ValueType, PaneType extends GuiSwingView.ValuePane<ValueType>> extends GuiSwingHistoryMenu<ValueType, PaneType> {
        protected GuiReprCollectionTable.TableTargetColumn target;
        public HistoryMenuForTableColumn(PaneType component, GuiMappingContext context, GuiReprCollectionTable.TableTargetColumn target) {
            super(component, context);
            this.target = target;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            return new HistorySetForColumnAction<>(getActionName(e), (ValueType) e.getValue(), target);
        }

        @Override
        public String getCategory() {
            return MenuBuilder.getCategoryWithPrefix(MENU_COLUMN_ROWS, PopupExtension.MENU_CATEGORY_SET);
        }
    }

    public static class HistorySetAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected ValueType value;
        protected GuiSwingView.ValuePane<ValueType> component;

        public HistorySetAction(String name, ValueType value, GuiSwingView.ValuePane<ValueType> component) {
            super(name);
            this.value = value;
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            component.setSwingViewValueWithUpdate(value);
        }
    }

    public static class HistorySetForColumnAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected ValueType value;
        protected GuiReprCollectionTable.TableTargetColumn target;

        public HistorySetForColumnAction(String name, ValueType value, GuiReprCollectionTable.TableTargetColumn target) {
            super(name);
            this.value = value;
            this.target = target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            target.setCellValues(target.getSelectedCellIndices(), i -> value);
        }
    }

    public static class HistoryClearAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected GuiSwingHistoryMenu menu;

        public HistoryClearAction(GuiSwingHistoryMenu menu) {
            putValue(NAME, "Clear");
            this.menu = menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            menu.clearHistory();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_DELETE;
        }
    }
}
