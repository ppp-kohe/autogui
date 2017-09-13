package autogui.base.mapping;

import autogui.base.type.GuiTypeElement;
import autogui.base.type.GuiTypeMemberAction;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuiMappingContext {
    protected GuiTypeElement typeElement;
    protected GuiRepresentation representation;

    protected GuiMappingContext parent;
    protected List<GuiMappingContext> children;

    protected Object source;
    protected List<SourceUpdateListener> listeners = Collections.emptyList();

    public GuiMappingContext(GuiTypeElement typeElement) {
        this.typeElement = typeElement;
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiMappingContext parent) {
        this.typeElement = typeElement;
        this.parent = parent;
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation, Object source) {
        this.typeElement = typeElement;
        this.representation = representation;
        this.source = source;
    }

    public void setRepresentation(GuiRepresentation representation) {
        this.representation = representation;
    }

    public GuiTypeElement getTypeElement() {
        return typeElement;
    }

    public GuiRepresentation getRepresentation() {
        return representation;
    }

    public List<GuiMappingContext> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        } else {
            return children;
        }
    }

    public GuiMappingContext getParent() {
        return parent;
    }

    public List<GuiMappingContext> createChildCandidates() {
        return typeElement.getChildren().stream()
                .map(e -> new GuiMappingContext(e, this))
                .collect(Collectors.toList());
    }

    protected List<GuiMappingContext> getChildrenForAdding() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public void addToParent() {
        if (parent != null) {
            parent.getChildrenForAdding().add(this);
        }
    }

    public boolean isRecursive() {
        GuiTypeElement s = getTypeElement();
        GuiMappingContext ctx = getParent();
        while (ctx != null) {
            if (ctx.getTypeElement().equals(s)) {
                return true;
            }
            ctx = ctx.getParent();
        }
        return false;
    }

    public GuiMappingContext getRoot() {
        GuiMappingContext ctx = this;
        while (ctx.getParent() != null) {
            ctx = ctx.getParent();
        }
        return ctx;
    }


    ///////////////////////

    public boolean isTypeElementProperty() {
        return typeElement instanceof GuiTypeMemberProperty;
    }

    public String getTypeElementPropertyTypeName() {
        return ((GuiTypeMemberProperty) typeElement).getType().getName();
    }

    public Class<?> getTypeElementPropertyTypeAsClass() {
        try {
            return Class.forName(getTypeElementPropertyTypeName());
        } catch (Exception ex) {
            return Object.class;
        }
    }

    public boolean isTypeElementAction() {
        return typeElement instanceof GuiTypeMemberAction;
    }

    public boolean isTypeElementObject() {
        return typeElement instanceof GuiTypeObject;
    }

    ///////////////////////


    public void setSource(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }

    public interface SourceUpdateListener {
        void update(GuiMappingContext cause, Object newValue);
    }

    public void addSourceUpdateListener(SourceUpdateListener listener) {
        if (listeners.isEmpty()) {
            listeners = Collections.singletonList(listener);
        } else if (listeners.size() == 1) {
            ArrayList<SourceUpdateListener> ls = new ArrayList<>(3);
            ls.addAll(listeners);
            ls.add(listener);
            listeners = ls;
        } else {
            listeners.add(listener);
        }
    }

    public void removeSourceUpdateListener(SourceUpdateListener listener) {
        int i = listeners.indexOf(listener);
        if (i != -1) {
            if (listeners.size() == 1) {
                listeners = Collections.emptyList();
            } else {
                listeners.remove(i);
            }
        }
    }

    public List<SourceUpdateListener> getListeners() {
        return listeners;
    }

    public void updateSourceFromGui(Object newValue) {
        this.source = newValue;
        GuiMappingContext ctx = getRoot();
        List<GuiMappingContext> updated = new ArrayList<>();
        ctx.collectUpdatedSource(this, updated);

        updated.forEach(c -> c.getListeners()
                .forEach(l -> l.update(this, c.getSource())));
    }

    public void collectUpdatedSource(GuiMappingContext cause, List<GuiMappingContext> updated) {
        if (this != cause) {
            if (getRepresentation().update(this)) {
                updated.add(this);
            }

            getChildren()
                    .forEach(c -> c.collectUpdatedSource(cause, updated));
        }
    }
}
