package autogui.swing.table;

public class ObjectColumnIndex implements Cloneable {
    protected ObjectColumnIndex parent;
    protected int totalIndex;
    protected int index;

    public ObjectColumnIndex(ObjectColumnIndex parent, int totalIndex, int index) {
        this.parent = parent;
        this.totalIndex = totalIndex;
        this.index = index;
    }

    public ObjectColumnIndex next(int flattenSizeForThisColumn) {
        ObjectColumnIndex n = copy();
        n.increment(flattenSizeForThisColumn);
        return n;
    }

    public ObjectColumnIndex child() {
        return new ObjectColumnIndex(this, totalIndex, 0);
    }

    public ObjectColumnIndex copy() {
        try {
            return (ObjectColumnIndex) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ObjectColumnIndex getParent() {
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
