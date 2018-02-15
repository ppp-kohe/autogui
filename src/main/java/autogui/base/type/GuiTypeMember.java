package autogui.base.type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

/** the super type for actions and properties */
public abstract class GuiTypeMember implements GuiTypeElement {
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

    /** return a method whose name is equivalent to the name with matching the p */
    public Method findOwnerMethod(String name, Predicate<Method> p) {
        if (getOwner() != null) {
            Class<?> type = getOwner().getType();
            if (type != null) {
                return Arrays.stream(type.getMethods())
                        .filter(m -> m.getName().equals(name))
                        .filter(p)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

}
