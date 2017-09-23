package autogui.base.mapping;

public class GuiReprCollectionElement implements GuiRepresentation {
    protected GuiRepresentation representation;

    public GuiReprCollectionElement(GuiRepresentation representation) {
        this.representation = representation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isParentCollectionTable()) {
            if (representation.match(context)) {
                //overwrites the matched and set representation with wrapping it
                GuiRepresentation elementRepr = context.getRepresentation();
                context.setRepresentation(createElement(elementRepr));

                if (context.getChildren().isEmpty()) { //no children: create a new child with the wrapped representation
                    GuiMappingContext subContext = context.createChildCandidate(context.getTypeElement());
                    subContext.setRepresentation(elementRepr);
                    subContext.addToParent();
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public GuiRepresentation createElement(GuiRepresentation wrapped) {
         return new GuiReprCollectionElement(wrapped);
    }

    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        return false;
    }

    @Override
    public boolean continueCheckAndUpdateSourceForChildren(GuiMappingContext context, boolean parentUpdate) {
        return false;
    }

    /** the table cell version of {@link GuiReprValue#getUpdatedValue(GuiMappingContext, boolean)} */
    public Object getCellValue(GuiMappingContext context, GuiMappingContext subContext,
                               Object src, int rowIndex, int columnIndex) throws Exception {
        if (subContext.isTypeElementProperty()) {
            return subContext.getTypeElementAsProperty().executeGet(src, null);
        } else {
            if (representation instanceof GuiReprPropertyPane) {
                return src;
            } else if (subContext.isTypeElementValue() || subContext.isTypeElementObject() || subContext.isTypeElementCollection()) {
                return subContext.getTypeElementValue().updatedValue(null);
            }
        }
        return null;
    }

    /** the table cell version of {@link GuiReprValue#updateFromGui(GuiMappingContext, Object)} */
    public void updateCellFromGui(GuiMappingContext context, GuiMappingContext subContext,
                                  Object src, int rowIndex, int columnIndex, Object newValue) {
        if (subContext.isTypeElementProperty()) {
            try {
                subContext.getTypeElementAsProperty().executeSet(src, newValue);
            } catch (Exception ex) {
                subContext.errorWhileUpdateSource(ex);
            }
        } else if (representation instanceof GuiReprPropertyPane) {
            ((GuiReprPropertyPane) representation).updateFromGuiChild(subContext, newValue);
        } else if (subContext.isTypeElementValue() || subContext.isTypeElementObject() || subContext.isTypeElementCollection()) {
            subContext.getTypeElementValue().writeValue(null, newValue);
        }
    }
}
