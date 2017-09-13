package autogui.base.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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

    @Override
    public List<GuiTypeElement> getChildren() {
        return Stream.concat(getProperties().stream(), getActions().stream())
                .collect(Collectors.toList());
    }

    public GuiTypeObject addProperties(GuiTypeMemberProperty... properties) {
        if (this.properties == null) {
            this.properties = new ArrayList<>();
        }
        this.properties.addAll(Arrays.asList(properties));
        for (GuiTypeMemberProperty property : properties) {
            property.setOwner(this);
        }
        return this;
    }

    public GuiTypeObject addActions(GuiTypeMemberAction... actions) {
        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
        this.actions.addAll(Arrays.asList(actions));
        for (GuiTypeMemberAction action : actions) {
            action.setOwner(this);
        }
        return this;
    }
}
