package autogui.base;

public abstract class GuiTypeMember {
    protected String name;
    /** nullable */
    protected GuiTypeObject owner;

    public GuiTypeMember(String name) {
        this.name = name;
    }

    public void setOwner(GuiTypeObject owner) {
        this.owner = owner;
    }

    public GuiTypeObject getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }
}
