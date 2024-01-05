package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprCollectionTable;
import org.autogui.swing.table.TableTargetMenu;
import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * the menu for history value selection.
 *  the menu items are dynamically created from value histories of the context, loaded by preferences.
 * @param <ValueType> the value type of the pane
 * @param <PaneType>  a valued pane
 */
@SuppressWarnings("this-escape")
public class GuiSwingHistoryMenu<ValueType, PaneType extends GuiSwingView.ValuePane<ValueType>> extends JMenu implements TableTargetMenu,
        PopupCategorized.CategorizedMenuItemComponent {
    @Serial private static final long serialVersionUID = 1L;
    protected PaneType component;
    protected GuiMappingContext context;
    protected GuiSwingTaskRunner.ContextAction runner;

    public GuiSwingHistoryMenu(PaneType component, GuiMappingContext context) {
        super("History");
        this.component = component;
        this.context = context;
        runner = new GuiSwingTaskRunner.ContextAction(context);
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
        setEnabled(context.isHistoryValueSupported());
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
        GuiPreferences prefs = context.getPreferences();
        try (var lock = prefs.lock()) {
            lock.use();
            prefs.clearHistories();
        }
    }

    public void loadItems() {
        removeAll();
        if (!isEnabled()) {
            return;
        }

        boolean added = false;
        List<GuiPreferences.HistoryValueEntry> prefEs;
        GuiPreferences prefs = context.getPreferences();
        try (var lock = prefs.lock()) {
            lock.use();
            prefEs = prefs.getHistoryValues();
        }
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
        String name = runner.executeContextTask(
                () -> context.getRepresentation().toHumanReadableString(context, v), null)
                .getValueOr("ERROR: cancel", "ERROR: timeout");
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

    /**
     * a history menu for a table-column. the factory of {@link HistorySetForColumnAction}
     * @param <ValueType> the value type of the column
     * @param <PaneType>  the column component
     */
    public static class HistoryMenuForTableColumn<ValueType, PaneType extends GuiSwingView.ValuePane<ValueType>> extends GuiSwingHistoryMenu<ValueType, PaneType> {
        @Serial private static final long serialVersionUID = 1L;
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

    /**
     * the action for setting a specified value to a component
     * @param <ValueType> the value type of the target component
     */
    public static class HistorySetAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
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

    /**
     * the action for setting a specified value to selected cells of a table-column
     * @param <ValueType> the value type of the target column
     */
    public static class HistorySetForColumnAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
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

    /**
     * the clear menu item for histories
     */
    @SuppressWarnings("rawtypes")
    public static class HistoryClearAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
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
