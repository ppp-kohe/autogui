package autogui.swing.table;

public interface ObjectTableColumnDynamicFactory {
    int getColumnCount(Object collection);
    ObjectTableColumn createColumn(ObjectColumnIndex columnIndex);
}
