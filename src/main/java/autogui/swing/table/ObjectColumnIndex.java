package autogui.swing.table;

public class ObjectColumnIndex implements Cloneable {
    protected ObjectColumnIndex parent;
    protected int flattenIndex;
    protected int index;

    public ObjectColumnIndex(ObjectColumnIndex parent, int flattenIndex, int index) {
        this.parent = parent;
        this.flattenIndex = flattenIndex;
        this.index = index;
    }

    public ObjectColumnIndex next(int flattenSizeForThisColumn) {
        ObjectColumnIndex n = copy();
        n.index++;
        n.flattenIndex += flattenSizeForThisColumn;
        return n;
    }

    public ObjectColumnIndex child() {
        return new ObjectColumnIndex(this, flattenIndex, 0);
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

    public int getFlattenIndex() {
        return flattenIndex;
    }

    public int getIndex() {
        return index;
    }
}
