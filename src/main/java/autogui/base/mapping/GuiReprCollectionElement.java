package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.ArrayList;
import java.util.List;

public class GuiReprCollectionElement implements GuiRepresentation {
    protected GuiRepresentation representation;

    public GuiReprCollectionElement(GuiRepresentation representation) {
        this.representation = representation;
    }

    public GuiRepresentation getRepresentation() {
        return representation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isParentCollectionTable() && !context.isCollectionElement()) {
            context.setRepresentation(this); //temporally set this for avoiding self recursion with checking isCollectionElement()
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
                               Object src, int rowIndex, int columnIndex) throws Throwable {
        if (subContext.isTypeElementProperty()) {
            GuiTypeMemberProperty prop = subContext.getTypeElementAsProperty();
            Object val = subContext.execute(() ->
                    prop.executeGetList(rowIndex, src, null));
            if (val != null && val.equals(GuiTypeValue.NO_UPDATE)) {
                return null;
            } else {
                return val;
            }
        } else {
            if (representation instanceof GuiReprPropertyPane) {
                return src;
            } else if (subContext.isTypeElementValue() || subContext.isTypeElementObject() || subContext.isTypeElementCollection()) {
                GuiTypeValue val = subContext.getTypeElementValue();
                return subContext.execute(() ->
                        val.updatedValueList(rowIndex, null));
            }
        }
        return null;
    }

    /** the table cell version of {@link GuiReprValue#updateFromGui(GuiMappingContext, Object)} */
    public void updateCellFromGui(GuiMappingContext context, GuiMappingContext subContext,
                                  Object src, int rowIndex, int columnIndex, Object newValue) {
        if (subContext.isTypeElementProperty()) {
            try {
                GuiTypeMemberProperty prop = subContext.getTypeElementAsProperty();
                subContext.execute(() ->
                        prop.executeSetList(rowIndex, src, newValue));
            } catch (Throwable ex) {
                subContext.errorWhileUpdateSource(ex);
            }
        } else if (representation instanceof GuiReprPropertyPane) {
            ((GuiReprPropertyPane) representation).updateFromGuiChild(subContext, newValue);
        } else if (subContext.isTypeElementValue() || subContext.isTypeElementObject() || subContext.isTypeElementCollection()) {
            GuiTypeValue val = subContext.getTypeElementValue();
            try {
                subContext.execute(() ->
                        val.writeValueList(rowIndex, null, newValue));
            } catch (Throwable ex) {
                subContext.errorWhileUpdateSource(ex);
            }
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return List: { elementJson, ... }.  Note: null elements are skipped
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        List<?> list = (List<?>) source;
        List<Object> array = new ArrayList<>(list.size());
        for (Object element : list) {
            //there are several cases of wrapped repr:
            //   regular object element: element(object) { property,... }
            //   value object element: element(String) { String } //child-repr == wrapped-repr
            // In both cases, the wrapped repr. can properly handle an element as its source
            Object e = getRepresentation().toJsonWithNamed(context, element);
            if (e != null) {
                array.add(e);
            }
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json instanceof List<?>) {
            List<?> listJson = (List<?>) json;

            List<Object> listTarget = GuiReprValue.castOrMake(List.class, target, () -> null);
            List<Object> listResult = new ArrayList<>(listJson.size());

            for (int i = 0, l = listJson.size(); i < l; ++i) {
                Object elementJson = listJson.get(i);
                Object elementTarget = (listTarget != null && i < listTarget.size()) ? listTarget.get(i) : null;
                Object e = getRepresentation().fromJson(context, elementTarget, elementJson);
                if (e != null) {
                    listResult.add(e);
                }
            }
            return listResult;
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        List<?> list = (List<?>) source;
        List<String> array = new ArrayList<>(list.size());
        for (Object element : list) {
            array.add(getRepresentation().toHumanReadableString(context, element));
        }
        return String.join("\n", array);
    }
}
