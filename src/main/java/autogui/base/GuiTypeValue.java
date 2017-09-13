package autogui.base;

public class GuiTypeValue implements GuiTypeElement {
    protected String name;

    public GuiTypeValue(Class<?> type) {
        this.name = type.getName();
    }

    public GuiTypeValue(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "type(" + name + ")";
    }
}
