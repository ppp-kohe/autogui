package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.GuiSwingTableColumn;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.table.ObjectTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingViewCollectionTable implements GuiSwingView {
    protected GuiSwingMapperSet columnMapperSet;

    @Override
    public JComponent createView(GuiMappingContext context) {
        CollectionTable table = new CollectionTable(context);
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet columnSet = (GuiSwingTableColumnSet) subView;

                columnSet.createColumns(subContext, table.getObjectTableModel());

                actions.addAll(columnSet.createColumnActions(subContext,
                        table::isSelectionEmpty, table::getSourceSelection));
            }
        }

        if (context.getParent() != null) {
            for (GuiMappingContext siblingContext : context.getParent().getChildren()) {
                if (siblingContext.isTypeElementActionList() &&
                    siblingContext.getTypeElementAsActionList().getElementType()
                            .equals(context.getTypeElementCollection().getElementType())) {
                    //takes multiple selected items
                    actions.add(new TableSelectionListAction(siblingContext,
                            table::isSelectionEmpty, table::getSourceSelection));
                }
            }
        }
        return table.initAfterAddingColumns(actions);
    }

    public static class TableColumnSetDefault implements GuiSwingTableColumnSet {
        protected GuiSwingMapperSet columnMappingSet;

        public TableColumnSetDefault(GuiSwingMapperSet columnMappingSet) {
            this.columnMappingSet = columnMappingSet;
        }

        @Override
        public void createColumns(GuiMappingContext context, ObjectTableModel model) {
            for (GuiMappingContext subContext : context.getChildren()) {
                GuiSwingElement subView = columnMappingSet.view(subContext);
                if (subView != null && subView instanceof GuiSwingTableColumn) {
                    model.addColumn(((GuiSwingTableColumn) subView).createColumn(subContext));
                }
            }
        }

        @Override
        public List<Action> createColumnActions(GuiMappingContext context, Supplier<Boolean> selectionEmpty,
                                                Supplier<List<?>> selectionItems) {
            List<Action> actions = new ArrayList<>();
            for (GuiMappingContext subContext : context.getChildren()) {
                GuiSwingElement subView = columnMappingSet.view(subContext);
                if (subView != null && subView instanceof GuiSwingAction) {
                    actions.add(new TableSelectionAction(subContext, selectionEmpty, selectionItems));
                }
            }
            return actions;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class CollectionTable extends JTable implements GuiMappingContext.SourceUpdateListener {
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

        public JScrollPane initAfterAddingColumns(List<Action> actions) {
            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);
            if (actions.isEmpty()) {
                return model.initTableScrollPane(this);
            } else {
                JPanel pane = new JPanel(new BorderLayout());
                {
                    JToolBar actionToolBar = new JToolBar();
                    actionToolBar.setFloatable(false);
                    actionToolBar.setOpaque(false);
                    pane.add(actionToolBar, BorderLayout.PAGE_START);

                    pane.add(this, BorderLayout.CENTER);
                }
                return model.initTableScrollPane(pane);
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
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, newValue);
            getObjectTableModel().setSourceFromSupplier();
            SwingUtilities.invokeLater(getObjectTableModel()::fireTableDataChanged);
        }

        public List<?> getSourceSelection() {
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

        public boolean isSelectionEmpty() {
            return getSelectionModel().isSelectionEmpty();
        }
    }



    public static class TableSelectionListAction extends GuiSwingActionDefault.ExecutionAction {
        protected GuiMappingContext context;
        protected Supplier<Boolean> selectionEmpty;
        protected Supplier<List<?>> selectionItems;

        public TableSelectionListAction(GuiMappingContext context, Supplier<Boolean> selectionEmpty,
                                    Supplier<List<?>> selectionItems) {
            super(context);
            this.selectionEmpty = selectionEmpty;
            this.selectionItems = selectionItems;
        }

        @Override
        public boolean isEnabled() {
            return !selectionEmpty.get();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprActionList) context.getRepresentation())
                        .executeActionForList(context, selectionItems.get());
            } finally {
                setEnabled(true);
            }
        }
    }

    public static class TableSelectionAction extends TableSelectionListAction {
        public TableSelectionAction(GuiMappingContext context, Supplier<Boolean> selectionEmpty,
                                    Supplier<List<?>> selectionItems) {
            super(context, selectionEmpty, selectionItems);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprAction) context.getRepresentation())
                        .executeActionForTargets(context, selectionItems.get());
            } finally {
                setEnabled(true);
            }
        }
    }
}
