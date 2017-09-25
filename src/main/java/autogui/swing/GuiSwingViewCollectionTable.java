package autogui.swing;

import autogui.base.mapping.*;
import autogui.swing.table.*;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
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

    public static class CollectionTable extends JTable
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
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
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
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

        @Override
        public Object getSwingViewValue() {
            return getSource();
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, value);
            getObjectTableModel().setSourceFromSupplier();
            getObjectTableModel().fireTableDataChanged();
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

    public static class ObjectTableColumnValue extends ObjectTableColumn {
        protected GuiMappingContext context;

        /**
         * the representation of the context must be a sub-type of {@link GuiReprValue}.
         * view must be a {@link autogui.swing.GuiSwingView.ValuePane} */
        public ObjectTableColumnValue(GuiMappingContext context, JComponent view) {
            this.context = context;

            ObjectTableCellEditor editor = new ObjectTableCellEditor(view);

            GuiReprValue value = (GuiReprValue) context.getRepresentation();
            setTableColumn(new TableColumn(0, 64, editor,
                    value.isEditable(context) ? editor : null));
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            GuiReprValue field = (GuiReprValue) context.getRepresentation();
            GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
            try {
                return field.toUpdateValue(context,
                        col.getCellValue(context.getParent(), context, rowObject, rowIndex, columnIndex));
            } catch (Exception ex) {
                context.errorWhileUpdateSource(ex);
                return null;
            }
        }

        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
            GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
            col.updateCellFromGui(context.getParent(), context, rowObject, rowIndex, columnIndex, newColumnValue);
            return null;
        }
    }
}
