package autogui.base.type;

public class GuiTypeMemberAction extends GuiTypeMember {
    protected String methodName;
    public GuiTypeMemberAction(String name, String methodName) {
        super(name);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }
}
