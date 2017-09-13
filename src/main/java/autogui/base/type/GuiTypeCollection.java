package autogui.base.type;

public class GuiTypeCollection implements GuiTypeElement {
    protected String name;
    protected GuiTypeElement elementType;

    public GuiTypeCollection(String name) {
        this.name = name;
    }

    public GuiTypeCollection(String name, GuiTypeElement elementType) {
        this.name = name;
        this.elementType = elementType;
    }

    public void setElementType(GuiTypeElement elementType) {
        this.elementType = elementType;
    }

    @Override
    public String getName() {
        return name;
    }

    public GuiTypeElement getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "collection(" + elementType + ")";
    }
}
