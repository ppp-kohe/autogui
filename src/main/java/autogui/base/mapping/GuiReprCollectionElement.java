package autogui.base.mapping;

import autogui.base.type.GuiUpdatedValue;

import java.util.ArrayList;

/**
 * elements in a collection table {@link GuiReprCollectionTable}.
 *
 * <p>
 *    value obtaining and updating in this class are intended to operate an element in a collection.
 *    <ul>
 *     <li>the class overrides {@link #getValue(GuiMappingContext, GuiMappingContext.GuiSourceValue, ObjectSpecifier, GuiMappingContext.GuiSourceValue)}
 *     and {@link #update(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, ObjectSpecifier)}.
 *     </li>
 *
 *     <li>Those methods delegate to the parent {@link GuiReprCollectionTable}'s collection methods
 *       (i.e. {@link #getValueCollectionElement(GuiMappingContext, GuiMappingContext.GuiSourceValue, ObjectSpecifier, GuiMappingContext.GuiSourceValue)},
 *          and {@link #updateCollectionElement(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, ObjectSpecifier)}).
 *     </li>
 *      <li>Those methods takes an indexed specifier ({@link autogui.base.mapping.GuiReprValue.ObjectSpecifierIndex})
 *          for specifying an element in a collection.
 *        </li>
 *     </ul>
 *
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
        if (context.isParentCollectionTable() &&
                !context.isReprCollectionElement()) { //recursive guard
            GuiMappingContext subContext = context.createChildCandidate(context.getTypeElement());
            subContext.setRepresentation(this); //temporally set this for avoiding self recursion with checking isReprCollectionElement()
            context.setRepresentation(this);

            if (representation.match(subContext)) {
                //wraps elementRepr with the collection-element
                GuiRepresentation elementRepr = subContext.getRepresentation();
                context.setRepresentation(createElement(elementRepr)); //context(GuiReprCollectionElement(elementRepr))

                subContext.addToParent();

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }

    public GuiRepresentation createElement(GuiRepresentation wrapped) {
         return new GuiReprCollectionElement(wrapped);
    }

    /**
     * @param context the target context
     * @return the child context whose repr is the wrapped representation of the element
     */
    public GuiMappingContext getElementChild(GuiMappingContext context) {
        return context.getChildren().get(0);
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
    public GuiUpdatedValue getValue(GuiMappingContext context, GuiMappingContext.GuiSourceValue parentSource,
                                    ObjectSpecifier specifier, GuiMappingContext.GuiSourceValue prev) throws Throwable {
        if (context.isParentCollectionTable()) {
            return context.getParentCollectionTable()
                    .getValueCollectionElement(context.getParent(), parentSource, specifier, prev);
        } else {
            return super.getValue(context, parentSource, specifier, prev);
        }
    }

    @Override
    public int getValueCollectionSize(GuiMappingContext context, GuiMappingContext.GuiSourceValue collection,
                                      ObjectSpecifier specifier) throws Throwable {
        if (context.isParentCollectionTable()) {
            return context.getParentCollectionTable()
                    .getValueCollectionSize(context.getParent(), collection, specifier.getParent());
        } else {
            return super.getValueCollectionSize(context, collection, specifier);
        }
    }

    @Override
    public Object update(GuiMappingContext context, GuiMappingContext.GuiSourceValue parentSource, Object newValue,
                         ObjectSpecifier specifier) throws Throwable {
        if (context.isParentCollectionTable()) {
            return context.getParentCollectionTable()
                    .updateCollectionElement(context.getParent(), parentSource, newValue, specifier);
        } else {
            return super.update(context, parentSource, newValue, specifier);
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
        //there are several cases of wrapped repr:
        //   regular object element: element(object) { Object {property,...} }
        //   value object element: element(String) { String }
        // In both cases, the wrapped repr. == child-repr
        for (GuiMappingContext valueContext : context.getChildren()) { //element has a single-child
            Object v = valueContext.getRepresentation().toJson(valueContext, source);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return getRepresentation().fromJson(context, target, json);
    }


    @Override
    public boolean isJsonSetter() {
        return false;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return getRepresentation().toHumanReadableString(context, source);
    }

    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return getRepresentation().toHumanReadableString(context, str);
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
