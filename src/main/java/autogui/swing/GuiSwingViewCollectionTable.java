package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.table.GuiSwingTableColumnSetDefault;
import autogui.swing.table.ObjectTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
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

        public CollectionTable(GuiMappingContext context) {
            this.context = context;

            ObjectTableModel model = new ObjectTableModel(this::getSource);
            model.setTable(this);
            setModel(model);
            setColumnModel(model.getColumnModel());

            context.addSourceUpdateListener(this);

            update(context, context.getSource());
        }

        public JComponent initAfterAddingColumns(List<Action> actions) {
            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);
            if (actions.isEmpty()) {
                return model.initTableScrollPane(this);
            } else {
                JPanel pane = new JPanel(new BorderLayout());
                pane.add(initActionToolBar(actions), BorderLayout.PAGE_START);
                pane.add(model.initTableScrollPane(this), BorderLayout.CENTER);
                return pane;
            }
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
            actionToolBar.add(new GuiSwingViewObjectPane.ActionButton(action));
            action.setEnabled(false);

            String name = (String) action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
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
                        selected.add(source.get(i));
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
                    is.add(i);
                }
            }

            getObjectTableModel().refreshRows(is.stream()
                    .mapToInt(Integer::intValue).toArray());
        }

    }


}
