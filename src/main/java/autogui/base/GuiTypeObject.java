package autogui.base;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GuiTypeObject implements GuiTypeElement {
    protected String name;
    protected List<GuiTypeMemberProperty> properties;
    protected List<GuiTypeMemberAction> actions;

    public GuiTypeObject(String name) {
        this.name = name;
        this.properties = new ArrayList<>();
        this.actions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    public List<GuiTypeMemberProperty> getProperties() {
        return properties;
    }

    public List<GuiTypeMemberAction> getActions() {
        return actions;
    }

    /**
     * @return nullable
     * */
    public GuiTypeMember getMemberByName(String name) {
        return Stream.concat(getProperties().stream(), getActions().stream())
                .filter(m -> m.getName().equals(name))
                .findFirst().orElse(null);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProperties(List<GuiTypeMemberProperty> properties) {
        this.properties = properties;
    }

    public void setActions(List<GuiTypeMemberAction> actions) {
        this.actions = actions;
    }
}
