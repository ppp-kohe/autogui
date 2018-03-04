package autogui.base.type;

import java.util.Collections;
import java.util.List;

/**
 * <pre>
 *     Collection&lt;E&gt;
 * </pre>
 *  the type is Collection, and its sub-types
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
     *
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

        return elementType != null ? elementType.equals(that.elementType) : that.elementType == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (elementType != null ? elementType.hashCode() : 0);
        return result;
    }

    /**
     * it also checks identity of 2 collections
     * @param prevValue the compared value 1
     * @param nextValue the compared value 2
     * @return true if two objects are equivalent
     */
    @Override
    public boolean equals(Object prevValue, Object nextValue) {
        if (prevValue == nextValue) {
            return true;
        } else {
            return super.equals(prevValue, nextValue);
        }
    }
}
