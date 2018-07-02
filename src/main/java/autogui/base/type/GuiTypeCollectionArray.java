package autogui.base.type;

import java.lang.reflect.Array;
import java.util.List;

/**
 * type information about an array. a sub-class of {@link GuiTypeCollection}
 */
public class GuiTypeCollectionArray extends GuiTypeCollection {
    public GuiTypeCollectionArray(String name) {
        super(name);
    }

    public GuiTypeCollectionArray(String name, GuiTypeElement elementType) {
        super(name, elementType);
    }

    public GuiTypeCollectionArray(Class<?> type) {
        super(type);
    }

    public GuiTypeCollectionArray(Class<?> type, GuiTypeElement elementType) {
        super(type, elementType);
    }

    @Override
    public GuiUpdatedValue executeGetElement(Object list, int index) {
        if (list == null) {
            return GuiUpdatedValue.NO_UPDATE;
        } else {
            Object v = Array.get(list, index);
            return GuiUpdatedValue.of(v);
        }
    }

    @Override
    public List<Object> executeAddElements(Object list, List<Object> newValues) {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    public Object executeSetElement(Object list, int index, Object newValue) {
        if (list != null) {
            Array.set(list, index, newValue);
            return newValue;
        } else {
            return null;
        }
    }

    @Override
    public int getSize(Object list) {
        if (list == null) {
            return 0;
        } else {
            return Array.getLength(list);
        }
    }
}
