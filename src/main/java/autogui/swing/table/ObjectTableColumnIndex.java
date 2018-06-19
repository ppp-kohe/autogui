package autogui.swing.table;

import java.util.ArrayList;
import java.util.List;

public class ObjectTableColumnIndex implements Cloneable {
    protected GuiSwingTableColumnDynamic.ObjectTableColumnSize size;
    protected ObjectTableColumnIndex parent;
    protected int index;

    public ObjectTableColumnIndex(ObjectTableColumnIndex parent, int index) {
        this(parent, index, null);
    }

    public ObjectTableColumnIndex(ObjectTableColumnIndex parent, int index, GuiSwingTableColumnDynamic.ObjectTableColumnSize size) {
        this.parent = parent;
        this.index = index;
        this.size = size;
    }

    public ObjectTableColumnIndex child(GuiSwingTableColumnDynamic.ObjectTableColumnSize size) {
        return new ObjectTableColumnIndex(this, 0, size);
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

    public int getIndex() {
        return index;
    }

    public ObjectTableColumnIndex next() {
        ObjectTableColumnIndex index = copy();
        ++index.index;
        return index;
    }

    public void injectIndexToSpecifier() {
        if (parent != null) {
            parent.injectIndexToSpecifier();
        }
        GuiSwingTableColumn.SpecifierManagerIndex specIndex;
        if (size != null && (specIndex = size.getElementSpecifierIndex()) != null) {
            specIndex.setIndex(index);
        }
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
