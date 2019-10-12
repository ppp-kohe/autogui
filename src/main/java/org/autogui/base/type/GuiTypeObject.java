package org.autogui.base.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * an object type, which has actions and properties.
 *
 * children: actions and properties
 */
public class GuiTypeObject extends GuiTypeValue {
    protected List<GuiTypeMemberProperty> properties;
    protected List<GuiTypeMemberAction> actions;
    /** notifiers for the type @since 1.2 */
    protected List<GuiTypeMemberPropertyNotifier> notifiers;

    public GuiTypeObject(Class<?> type) {
        super(type);
        this.properties = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.notifiers = new ArrayList<>();
    }

    public GuiTypeObject(String name) {
        super(name);
        this.properties = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.notifiers = new ArrayList<>();
    }

    /** @return property members */
    public List<GuiTypeMemberProperty> getProperties() {
        return properties;
    }

    /** @return action members */
    public List<GuiTypeMemberAction> getActions() {
        return actions;
    }

    /** @return notifier setters
     * @since 1.2 */
    public List<GuiTypeMemberPropertyNotifier> getNotifiers() {
        return notifiers;
    }

    /**
     * @param name the name compared with {@link GuiTypeMember#getName()}
     * @return nullable
     * */
    public GuiTypeMember getMemberByName(String name) {
        return Stream.concat(getProperties().stream(), getActions().stream())
                .filter(m -> m.getName().equals(name))
                .findFirst().orElse(null);
    }

    public void setProperties(List<GuiTypeMemberProperty> properties) {
        this.properties = properties;
    }

    public void setActions(List<GuiTypeMemberAction> actions) {
        this.actions = actions;
    }

    /**
     * set the notifiers
     * @param notifiers the notifiers to be set
     * @since 1.2
     */
    public void setNotifiers(List<GuiTypeMemberPropertyNotifier> notifiers) {
        this.notifiers = notifiers;
    }

    /** @return properties and actions */
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

    /**
     * add notifiers. if the {@link #notifiers} is null, it creates a new list.
     *  items of notifiers will be set {@link GuiTypeMemberPropertyNotifier#setOwner(GuiTypeObject)} with this.
     * @param notifiers the added notifiers
     * @return this
     * @since 1.2
     */
    public GuiTypeObject addNotifiers(GuiTypeMemberPropertyNotifier... notifiers) {
        if (this.notifiers == null) {
            this.notifiers = new ArrayList<>();
        }
        this.notifiers.addAll(Arrays.asList(notifiers));
        for (GuiTypeMemberPropertyNotifier property : notifiers) {
            property.setOwner(this);
        }
        return this;
    }

    @Override
    public String toString() {
        return "object(" + name + ")";
    }
}
