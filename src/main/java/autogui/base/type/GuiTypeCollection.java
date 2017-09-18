package autogui.base.type;

import java.util.Collections;
import java.util.List;

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


    public GuiTypeElement getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "collection(" + elementType + ")";
    }

    @Override
    public List<GuiTypeElement> getChildren() {
        if (elementType != null) {
            return Collections.singletonList(elementType);
        } else {
            return Collections.emptyList();
        }
    }
}
