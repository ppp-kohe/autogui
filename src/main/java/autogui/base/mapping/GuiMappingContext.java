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
    protected GuiTypeElement source;
    protected GuiRepresentation representation;

    protected GuiMappingContext parent;
    protected List<GuiMappingContext> children;

    public GuiMappingContext(GuiTypeElement source) {
        this.source = source;
    }

    public GuiMappingContext(GuiTypeElement source, GuiMappingContext parent) {
        this.source = source;
        this.parent = parent;
    }

    public void setRepresentation(GuiRepresentation representation) {
        this.representation = representation;
    }

    public GuiTypeElement getSource() {
        return source;
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
        return source.getChildren().stream()
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
        GuiTypeElement s = getSource();
        GuiMappingContext ctx = getParent();
        while (ctx != null) {
            if (ctx.getSource().equals(s)) {
                return true;
            }
            ctx = ctx.getParent();
        }
        return false;
    }


    ///////////////////////

    public boolean isSourceProperty() {
        return source instanceof GuiTypeMemberProperty;
    }

    public String getSourcePropertyTypeName() {
        return ((GuiTypeMemberProperty) source).getType().getName();
    }

    public Class<?> getSourcePropertyTypeAsClass() {
        try {
            return Class.forName(getSourcePropertyTypeName());
        } catch (Exception ex) {
            return Object.class;
        }
    }

    public boolean isSourceAction() {
        return source instanceof GuiTypeMemberAction;
    }

    public boolean isSourceObject() {
        return source instanceof GuiTypeObject;
    }
}
