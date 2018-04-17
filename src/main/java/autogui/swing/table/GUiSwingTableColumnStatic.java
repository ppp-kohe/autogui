package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;

import java.util.Collections;
import java.util.List;

public abstract class GUiSwingTableColumnStatic implements ObjectTableColumnDynamicFactory, GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        ObjectColumnIndex columnIndex;
        if (context.isParentCollectionElement()) {
            int index = context.getParent().getReprCollectionElement().getFixedColumnIndex(context.getParent(), context);
            columnIndex = new ObjectColumnIndex(null, index, index);
        } else {
            columnIndex = new ObjectColumnIndex(null, 0, 0);
        }
        return createColumnWithIndex(context, columnIndex);
    }

    @Override
    public ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context) {
        return this;
    }

    @Override
    public int getColumnCount(GuiMappingContext context, List<?> source) {
        return 1;
    }

    @Override
    public List<ObjectTableColumn> createColumns(GuiMappingContext context, List<?> source, ObjectColumnIndex index, int size) {
        return Collections.singletonList(createColumnWithIndex(context, index));
    }

    public abstract ObjectTableColumn createColumnWithIndex(GuiMappingContext context, ObjectColumnIndex index);
}
