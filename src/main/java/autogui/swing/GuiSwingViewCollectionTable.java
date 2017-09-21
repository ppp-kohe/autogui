package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.GuiSwingTableColumn;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.table.ObjectTableModel;

import javax.swing.*;
import java.util.List;

public class GuiSwingViewCollectionTable implements GuiSwingView {
    protected GuiSwingMapperSet columnMapperSet;

    @Override
    public JComponent createView(GuiMappingContext context) {
        CollectionTable table = new CollectionTable(context);
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.view(subContext);
            if (subView instanceof GuiSwingTableColumn) {
                //single element
                table.getObjectTableModel().addColumn(((GuiSwingTableColumn) subView).createColumn(subContext));
            } else if (subView instanceof GuiSwingTableColumnSet) {
                ((GuiSwingTableColumnSet) subView).createColumns(subContext, table.getObjectTableModel());
            }
        }
        return table.initAfterAddingColumns();
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

        public JScrollPane initAfterAddingColumns() {
            return getObjectTableModel().initTable(this);
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
    }
}
