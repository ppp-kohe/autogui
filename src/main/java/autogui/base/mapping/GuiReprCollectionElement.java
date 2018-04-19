package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * elements in a collection table {@link GuiReprCollectionTable}
 */
public class GuiReprCollectionElement extends GuiReprValue {
    protected GuiRepresentation representation;

    public GuiReprCollectionElement(GuiRepresentation representation) {
        this.representation = representation;
    }

    public GuiRepresentation getRepresentation() {
        return representation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isParentCollectionTable() && !context.isReprCollectionElement()) {
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

    /**
     * nothing happen
     * @param context the context of the repr.
     * @return always false
     */
    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        return false;
    }

    /**
     *
     * @param context the context of the repr.
     * @param parentUpdate ignored
     * @return always false
     */
    @Override
    public boolean continueCheckAndUpdateSourceForChildren(GuiMappingContext context, boolean parentUpdate) {
        return false;
    }

    @Override
    public Class<?> getValueType(GuiMappingContext context) {
        if (representation instanceof GuiReprValue) {
            return ((GuiReprValue) representation).getValueType(context);
        } else {
            return null;
        }
    }

    @Override
    public Object getValue(GuiMappingContext context, Object parentSource, ObjectSpecifier specifier, Object prev) throws Throwable {
        if (representation instanceof GuiReprValue) {
            return ((GuiReprValue) representation).getValue(context, parentSource, specifier, prev);
        } else {
            return null;
        }
    }

    @Override
    public Object getValueCollectionElement(GuiMappingContext context, Object collection, ObjectSpecifier elementSpecifier, Object prev) throws Throwable {
        if (representation instanceof GuiReprValue) {
            return ((GuiReprValue) representation).getValueCollectionElement(context, collection, elementSpecifier, prev);
        } else {
            return null;
        }
    }

    @Override
    public int getUpdatedValueCollectionSize(GuiMappingContext context, ObjectSpecifier specifier) throws Throwable {
        if (representation instanceof GuiReprValue) {
            return ((GuiReprValue) representation).getUpdatedValueCollectionSize(context, specifier);
        } else {
            return 1;
        }
    }



    /** the table cell version of {@link GuiReprValue#getUpdatedValue(GuiMappingContext, boolean)}.
     * <ul>
     *     <li>use {@link GuiMappingContext#execute(Callable)}  of <code>subContext</code>.</li>
     *     <li>if the <code>subContext</code> is a property,
     *          call {@link GuiTypeMemberProperty#executeGetList(int, Object, Object)}</li>
     *     <li>basically a GUI table reuses an old value,
     *        it updates when the value is null as cleared, and it obtains the new value.
     *           Thus, the method does not consider a previous value, which will be null.
     *           This is because mainly performance reason.</li>
     * </ul>
     * @param context the context of the repr.
     * @param subContext the main context as a value of the collection
     * @param rowSource  the target collection element
     * @param rowIndex the element index of the collection
     * @param columnIndex the table column index, ignored in the impl.
     * @return the updated value of the cell
     * @throws Throwable an error caused by the execution
     */
    @Deprecated
    public Object getCellValue(GuiMappingContext context, GuiMappingContext subContext,
                               Object rowSource, int rowIndex, int columnIndex) throws Throwable {
        if (subContext.isTypeElementProperty()) {
            GuiTypeMemberProperty prop = subContext.getTypeElementAsProperty();
            Object val = subContext.execute(() ->
                    prop.executeGetList(rowIndex, rowSource, null));
            if (val != null && val.equals(GuiTypeValue.NO_UPDATE)) {
                return null;
            } else {
                return val;
            }
        } else {
            if (representation instanceof GuiReprPropertyPane) {
                return rowSource;
            } else if (subContext.isTypeElementValue() || subContext.isTypeElementObject() || subContext.isTypeElementCollection()) {
                GuiTypeValue val = subContext.getTypeElementValue();
                 Object ret = subContext.execute(() ->
                        val.updatedValueList(rowIndex, rowSource));
                 if (ret != null && ret.equals(GuiTypeValue.NO_UPDATE)) {
                     return rowSource;
                 } else {
                     return ret;
                 }
            }
        }
        return null;
    }

    /** the table cell version of {@link GuiReprValue#updateFromGui(GuiMappingContext, Object)}
     * @param context the context of the repr.
     * @param subContext the main context as a value of the collection
     * @param rowSource  the target collection element
     * @param rowIndex the element index of the collection
     * @param columnIndex the table column index, ignored in the impl.
     * @param newValue the edited new value of the cell
     */
    @Deprecated
    public void updateCellFromGui(GuiMappingContext context, GuiMappingContext subContext,
                                  Object rowSource, int rowIndex, int columnIndex, Object newValue) {
        if (subContext.isTypeElementProperty()) {
            try {
                GuiTypeMemberProperty prop = subContext.getTypeElementAsProperty();
                subContext.execute(() ->
                        prop.executeSetList(rowIndex, rowSource, newValue));
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
     * @return List ({@link ArrayList}): [ elementJson, ... ].  Note: null elements are skipped.
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

    /**
     * @param context the context of the repr.
     * @return the size of columns which is equivalent to the size of the children,
     *    or -1 which means that the number of columns might be dynamic.
     */
    public int getFixedColumnSize(GuiMappingContext context) {
        if (!(representation instanceof GuiReprCollectionElement) &&
                !(representation instanceof GuiReprCollectionTable)) {
            return context.getChildren().size();
        } else {
            return -1;
        }
    }

    public int getFixedColumnIndex(GuiMappingContext context, GuiMappingContext columnContext) {
        return context.getChildren().indexOf(columnContext);
    }

    @Override
    public String toString() {
        return toStringHeader() + "(" + representation + ")";
    }
}
