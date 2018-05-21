package autogui.swing.table;

public class ObjectTableColumnIndex implements Cloneable {
    protected ObjectTableColumnIndex parent;
    protected int totalIndex;
    protected int index;

    public ObjectTableColumnIndex(ObjectTableColumnIndex parent, int totalIndex, int index) {
        this.parent = parent;
        this.totalIndex = totalIndex;
        this.index = index;
    }

    public ObjectTableColumnIndex next(int flattenSizeForThisColumn) {
        ObjectTableColumnIndex n = copy();
        n.increment(flattenSizeForThisColumn);
        return n;
    }

    public ObjectTableColumnIndex child() {
        return new ObjectTableColumnIndex(this, totalIndex, 0);
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
}
