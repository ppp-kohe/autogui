package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.table.GuiSwingTableColumnSetDefault;
import autogui.swing.table.ObjectTableColumn;
import autogui.swing.table.ObjectTableModel;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GuiSwingViewCollectionTable implements GuiSwingView {
    protected GuiSwingMapperSet columnMapperSet;


    public GuiSwingViewCollectionTable(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context) {
        CollectionTable table = new CollectionTable(context);
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet columnSet = (GuiSwingTableColumnSet) subView;

                columnSet.createColumns(subContext, table.getObjectTableModel());

                actions.addAll(columnSet.createColumnActions(subContext, table));
            }
        }

        if (context.getParent() != null) {
            for (GuiMappingContext siblingContext : context.getParent().getChildren()) {
                if (siblingContext.isTypeElementActionList() &&
                    siblingContext.getTypeElementAsActionList().getElementType()
                            .equals(context.getTypeElementCollection().getElementType())) {
                    //takes multiple selected items
                    actions.add(new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext, table));
                }
            }
        }
        return table.initAfterAddingColumns(actions);
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class CollectionTable extends JTable
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane,
                        GuiSwingTableColumnSet.TableSelectionSource {
        protected GuiMappingContext context;
        protected List<?> source;
        protected PopupExtensionCollection popup;
        protected List<Action> actions = new ArrayList<>();

        public CollectionTable(GuiMappingContext context) {
            this.context = context;

            //model
            ObjectTableModel model = new ObjectTableModel(this::getSource);
            model.setTable(this);
            setModel(model);
            setColumnModel(model.getColumnModel());

            //update context
            context.addSourceUpdateListener(this);

            //popup
            JComponent label = GuiSwingContextInfo.get().getInfoLabel(context);
            List<JComponent> items = new ArrayList<>();
            items.add(label);
            items.add(new JMenuItem(new ContextRefreshAction(context)));

            MouseListener[] listeners = getMouseListeners();
            Arrays.stream(listeners).forEach(this::removeMouseListener);
            popup = new PopupExtensionCollection(this, PopupExtension.getDefaultKeyMatcher(), items);
            Arrays.stream(listeners).forEach(this::addMouseListener);
            //improve precedence of the popup listener

            //cell selection
            setCellSelectionEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            //initial update
            update(context, context.getSource());

            //TODO drag drop
        }

        public JComponent initAfterAddingColumns(List<Action> actions) {
            this.actions.addAll(actions);
            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);
            if (actions.isEmpty()) {
                return initTableScrollPane();
            } else {
                JPanel pane = new GuiSwingView.ValueWrappingPane(initTableScrollPane());
                pane.add(initActionToolBar(actions), BorderLayout.PAGE_START);
                return pane;
            }
        }


        public ValueScrollPane initTableScrollPane() {
            ValueScrollPane scrollPane = new GuiSwingView.ValueScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            initTableScrollPane(scrollPane);
            return scrollPane;
        }

        public void initTableScrollPane(JScrollPane scrollPane) {
            int width = getObjectTableModel().getColumns().stream()
                    .mapToInt(e -> e.getTableColumn().getWidth())
                    .sum();
            scrollPane.setPreferredSize(new Dimension(width, Math.max(scrollPane.getPreferredSize().height, 100)));
        }

        public JToolBar initActionToolBar(List<Action> actions) {
            JToolBar actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);

            getSelectionModel().addListSelectionListener(e -> {
                boolean enabled = !isSelectionEmpty();
                actions.forEach(a -> a.setEnabled(enabled));
            });

            actions.forEach(a -> initAction(actionToolBar, a));

            return actionToolBar;
        }

        public void initAction(JToolBar actionToolBar, Action action) {
            actionToolBar.add(new GuiSwingIcons.ActionButton(action));
            action.setEnabled(false);

            String name = (String) action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
        }

        public List<Action> getActions() {
            return actions;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public ObjectTableModel getObjectTableModel() {
            return (ObjectTableModel) getModel();
        }

        public List<?> getSource() {
            return source;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getSource();
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, value);
            getObjectTableModel().setSourceFromSupplier();
        }

        /////////////////

        @Override
        public boolean isSelectionEmpty() {
            return getSelectionModel().isSelectionEmpty();
        }

        @Override
        public List<?> getSelectedItems() {
            ListSelectionModel sel = getSelectionModel();
            List<Object> selected = new ArrayList<>();
            if (source != null) {
                for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                    if (i >= 0 && sel.isSelectedIndex(i)) {
                        selected.add(source.get(convertRowIndexToModel(i)));
                    }
                }
            }
            return selected;
        }

        @Override
        public void selectionActionFinished() {
            ListSelectionModel sel = getSelectionModel();
            List<Integer> is = new ArrayList<>();
            for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                if (i >= 0 && sel.isSelectedIndex(i)) {
                    is.add(convertRowIndexToModel(i));
                }
            }

            getObjectTableModel().refreshRows(is.stream()
                    .mapToInt(Integer::intValue).toArray());
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void loadPreferences() {
            //TODO
        }

        @Override
        public void savePreferences() {
            //TODO
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            paintSelectedRows(g2);
        }

        protected void paintSelectedRows(Graphics2D g2) {
            Rectangle rect = g2.getClipBounds();
            double top = rect.getY();
            double bottom = rect.getMaxY();
            double x = rect.getX();

            int start = rowAtPoint(new Point((int) x, (int) top));
            int end = rowAtPoint(new Point((int) x, (int) bottom));
            if (end < 0) {
                end = getRowCount() - 1;
            }

            g2.setColor(getSelectionBackground());
            g2.setStroke(new BasicStroke(1));

            ListSelectionModel sel = getSelectionModel();
            int cols = getColumnCount();
            for (int i = start; i <= end; ++i) {
                if (sel.isSelectedIndex(i)) {
                    Rectangle row = null;
                    for (int c = 0; c < cols; ++c) {
                        Rectangle r = getCellRect(i, c, false);
                        if (row == null) {
                            row= r;
                        } else {
                            row.add(r);
                        }
                    }
                    if (row != null) {
                        g2.draw(new RoundRectangle2D.Float(row.x + 1, row.y + 1, row.width - 2, row.height - 2, 5, 5));
                    }
                }
            }
        }
    }


    public static class PopupExtensionCollection extends PopupExtension implements PopupMenuListener {
        protected CollectionTable table;
        protected int targetColumnIndex = -1;
        protected int lastClickColumnIndex = 0;

        protected boolean showing;
        protected Timer showingTimer;

        public PopupExtensionCollection(CollectionTable pane, Predicate<KeyEvent> keyMatcher, List<JComponent> items) {
            super(pane, keyMatcher, null);
            this.table = pane;
            setMenuBuilder(new CollectionColumnMenuSupplier(table, () -> {
                List<JComponent> comps = new ArrayList<>();
                comps.addAll(items);
                table.getActions().stream()
                        .map(JMenuItem::new)
                        .forEach(comps::add);
                return comps;
            }));

            menu.get().addPopupMenuListener(this);
            showingTimer = new Timer(100, e -> {
                showing = false;
            });
            showingTimer.setRepeats(false);
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            showing = true;
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            showingTimer.start(); //force to cause the hiding process after mousePressed
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            showingTimer.start();
        }

        public CollectionTable getTable() {
            return table;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger() && showing) { //closing the popup menu, and then nothing
                e.consume();
                return;
            }
            int viewColumn = table.columnAtPoint(e.getPoint());
            lastClickColumnIndex = table.convertColumnIndexToView(viewColumn);
            if (e.isPopupTrigger()) {
                targetColumnIndex = lastClickColumnIndex;
            }
            super.mousePressed(e);
        }

        @Override
        public void showByKey(KeyEvent e, Component comp) {
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            if (col < 0) {
                col = lastClickColumnIndex;
            } else {
                targetColumnIndex = table.convertColumnIndexToModel(col);
            }
            if (row != -1) {
                Rectangle rect = table.getCellRect(row, col, true);
                int x = rect.x + rect.width / 3;
                int y = rect.y + rect.height;
                show(comp, x, y);
            } else {
                super.showByKey(e, comp);
            }
        }

        /** model index*/
        public int getTargetColumnIndex() {
            return targetColumnIndex;
        }

        public ObjectTableColumn getTargetColumn() {
            if (targetColumnIndex >= 0 &&
                    targetColumnIndex < table.getObjectTableModel().getColumnCount()) {
                return table.getObjectTableModel().getColumns().get(targetColumnIndex);
            } else {
                return null;
            }
        }
    }

    public static class CollectionColumnMenuSupplier implements PopupExtension.PopupMenuBuilder {
        protected CollectionTable table;
        protected Supplier<? extends Collection<JComponent>> items;

        public CollectionColumnMenuSupplier(CollectionTable table, Supplier<? extends Collection<JComponent>> items) {
            this.table = table;
            this.items = items;
        }

        @Override
        public void build(PopupExtension sender, Consumer<Object> menu) {
            if (items != null) {
                items.get().forEach(menu::accept);
            }

            if (sender instanceof PopupExtensionCollection) {
                ObjectTableColumn column = ((PopupExtensionCollection) sender).getTargetColumn();
                ObjectTableColumn.PopupMenuBuilderSource src = (column == null ? null : column.getMenuBuilderSource());
                if (src != null) {
                    PopupExtension.PopupMenuBuilder builder = src.getMenuBuilder();
                    if (builder != null) {
                        builder.build(sender, new MenuTitleAppender("Column: " + column.getTableColumn().getHeaderValue(), menu));
                    }
                }
            }
        }

    }

    public static class MenuTitleAppender implements Consumer<Object> {
        protected Consumer<Object> appender;
        protected boolean added = false;
        protected String title;

        public MenuTitleAppender(String title, Consumer<Object> appender) {
            this.title = title;
            this.appender = appender;
        }

        @Override
        public void accept(Object o) {
            if (title != null && !added) {
                appender.accept(new JPopupMenu.Separator());
                appender.accept(MenuBuilder.get().createLabel(title));
                added = true;
            }
            appender.accept(o);
        }
    }
}
