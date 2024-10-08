package org.autogui.base.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * a type information about <code>
 *     Collection&lt;E&gt;
 * </code>.
 *  the type is Collection, and its subtypes
 */
public class GuiTypeCollection extends GuiTypeValue implements GuiTypeElement {
    protected GuiTypeElement elementType;

    public GuiTypeCollection(String name) {
        super(name);
    }

    public GuiTypeCollection(String name, GuiTypeElement elementType) {
        super(name);
        this.elementType = elementType;
    }

    public GuiTypeCollection(Class<?> type) {
        super(type);
    }

    public GuiTypeCollection(Class<?> type, GuiTypeElement elementType) {
        super(type);
        this.elementType = elementType;
    }

    public void setElementType(GuiTypeElement elementType) {
        this.elementType = elementType;
    }


    /** @return the element type of the collection: usually obtained from ParameterizedType */
    public GuiTypeElement getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "collection(" + elementType + ")";
    }

    /**
     * @return a single list with {@link #elementType}
     */
    @Override
    public List<GuiTypeElement> getChildren() {
        if (elementType != null) {
            return Collections.singletonList(elementType);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GuiTypeCollection that = (GuiTypeCollection) o;

        return Objects.equals(elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (elementType != null ? elementType.hashCode() : 0);
        return result;
    }

    /**
     * it only checks identity of 2 collections
     * @param prevValue the compared value 1
     * @param nextValue the compared value 2
     * @return true if two objects are equivalent
     */
    @Override
    public boolean equals(Object prevValue, Object nextValue) {
        return prevValue == nextValue;
    }

    /**
     * @param list a list object
     * @param index an index
     * @return the element at index or {@link GuiUpdatedValue#NO_UPDATE} if list is null
     */
    public GuiUpdatedValue executeGetElement(Object list, int index) {
        if (list == null) {
            return GuiUpdatedValue.NO_UPDATE;
        } else {
            Object v = ((List<?>) list).get(index);
            return GuiUpdatedValue.of(v);
        }
    }

    /**
     * @param list a list object
     * @param index an index
     * @param prev the previous value of the element
     * @return the element at index or {@link GuiUpdatedValue#NO_UPDATE}
     */
    public GuiUpdatedValue executeGetElement(Object list, int index, Object prev) {
        GuiUpdatedValue v = executeGetElement(list, index);
        if (v.isNone()) {
            return GuiUpdatedValue.NO_UPDATE;
        } else {
            return compareGetElement(prev, v.getValue());
        }
    }

    /**
     * use {@link GuiTypeValue#equals(Object, Object)} if the element type is a {@link GuiTypeValue},
     * or {@link Objects#equals(Object, Object)}
     * @param prevValue an element value
     * @param newValue a new element value
     * @return NO_UPDATE if two values are equivalent, or newValue
     */
    public GuiUpdatedValue compareGetElement(Object prevValue, Object newValue) {
        boolean eq;
        if (elementType instanceof GuiTypeValue) {
            eq = ((GuiTypeValue) elementType).equals(prevValue, newValue);
        } else {
            eq = Objects.equals(prevValue, newValue);
        }
        if (eq) {
            return GuiUpdatedValue.NO_UPDATE;
        } else {
            return GuiUpdatedValue.of(newValue);
        }
    }

    /**
     * call {@link List#set(int, Object)}
     * @param list the target list, must be a {@link List}
     * @param index the updated index
     * @param newValue the element value
     * @return newValue if succeeded, or null if list is null
     */
    @SuppressWarnings("unchecked")
    public Object executeSetElement(Object list, int index, Object newValue) {
        if (list != null) {
            ((List<Object>) list).set(index, newValue);
            return newValue;
        } else {
            return null;
        }
    }

    /**
     * call {@link List#addAll(Collection)}
     * @param list the target list, must be a {@link List}
     * @param newValues added elements
     * @return newValues or null if list is null
     */
    @SuppressWarnings("unchecked")
    public List<Object> executeAddElements(Object list, List<Object> newValues) {
        if (list != null) {
            ((List<Object>) list).addAll(newValues);
            return newValues;
        } else {
            return null;
        }
    }

    /**
     * @param list the target list must be a {@link List}
     * @return {@link List#size()}
     */
    public int getSize(Object list) {
        if (list == null) {
            return 0;
        } else {
            return ((List<?>) list).size();
        }
    }
}
