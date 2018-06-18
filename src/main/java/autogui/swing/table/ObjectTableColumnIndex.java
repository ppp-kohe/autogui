package autogui.swing.table;

import java.util.ArrayList;
import java.util.List;

public class ObjectTableColumnIndex implements Cloneable {
    protected GuiSwingTableColumnDynamic.ObjectTableColumnSize size;
    protected ObjectTableColumnIndex parent;
    protected int totalIndex;
    protected int index;

    public ObjectTableColumnIndex(ObjectTableColumnIndex parent, int totalIndex, int index) {
        this(parent, totalIndex, index, null);
    }

    public ObjectTableColumnIndex(ObjectTableColumnIndex parent, int totalIndex, int index, GuiSwingTableColumnDynamic.ObjectTableColumnSize size) {
        this.parent = parent;
        this.totalIndex = totalIndex;
        this.index = index;
        this.size = size;
    }

    public ObjectTableColumnIndex next(int flattenSizeForThisColumn) {
        ObjectTableColumnIndex n = copy();
        n.increment(flattenSizeForThisColumn);
        return n;
    }

    public ObjectTableColumnIndex child(GuiSwingTableColumnDynamic.ObjectTableColumnSize size) {
        return new ObjectTableColumnIndex(this, totalIndex, 0, size);
    }

    public ObjectTableColumnIndex copy() {
        try {
            return (ObjectTableColumnIndex) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ObjectTableColumnIndex getParent() {
        return parent;
    }

    public int getTotalIndex() {
        return totalIndex;
    }

    public int getIndex() {
        return index;
    }

    public void increment(int flattenSize) {
        ++index;
        totalIndex +=flattenSize;
    }

    public int[] toIndexes() {
        List<Integer> is = new ArrayList<>();
        ObjectTableColumnIndex i = this;
        while (i != null) {
            is.add(0, index);
            i = i.getParent();
        }
        return is.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
