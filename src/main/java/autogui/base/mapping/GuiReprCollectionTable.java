package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;

import java.util.Collections;
import java.util.List;

/**
 * representation for {@link autogui.base.type.GuiTypeCollection}.
 *
 * <h3>matching and sub-contexts</h3>
 *  <p>
 *
 *        {@link GuiTypeCollection#getChildren()} will return the element type of the collection.
 *        Also, the {@link #subRepresentation} will take a factory of {@link GuiReprCollectionElement}.
 *         The factory will match the element type with the {@link GuiTypeCollection} as it's parent
 *           (the element is regular element type but the collection element repr. has higher precedence in the factory).
 *
 *        So, sub-context is a single element with the {@link GuiReprCollectionElement}.
 *
 *   <p>  The sub-contexts of the sub-collection element are regular sub-contexts of the wrapped element representation.
 *        For example, if the wrapped collection element is an {@link GuiReprObjectPane}'s context,
 *          then the context of the collection element has
 *             properties ({@link GuiReprPropertyPane}) (and also actions) of the object members.
 *         As summarize, {@link GuiReprCollectionTable} -&gt;
 *            {@link GuiReprCollectionElement}(wrapping a {@link GuiRepresentation}) -&gt;
 *              member representations.
 *
 * <h3>accessing collection values</h3>
 * <p>
 *    The {@link GuiMappingContext#getSource()} of {@link GuiReprCollectionElement}
 *      holds a collection type, but contexts of {@link GuiReprCollectionElement} do not have any values.
 *      {@link GuiReprCollectionElement#checkAndUpdateSource(GuiMappingContext)} do nothing,
 *  <p>
 *    A concrete GUI component for the repr., like GuiSwingViewCollectionTable,
 *       can partially obtains property values of the collection as on-demand cells.
 *
 *    {@link GuiReprCollectionElement#getCellValue(GuiMappingContext, GuiMappingContext, Object, int, int)}
 *       can be used for the purpose. ObjectTableColumnValue's getCellValue(...) is an actual implementation.
 *     It takes the context of the collection element,
 *        an it's sub-context of the property of the row object,
 *           the source row object and row and column indices.
 *
 * */
public class GuiReprCollectionTable extends GuiReprValue implements GuiRepresentation {
    protected GuiRepresentation subRepresentation;

    public GuiReprCollectionTable(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementCollection()) {
            context.setRepresentation(this);
            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentation.match(subContext)) {
                    subContext.addToParent();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public List<?> toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue == null) {
            return Collections.emptyList();
        } else {
            return (List<?>) newValue;
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
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().toJsonWithNamed(elementContext, source);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().fromJson(elementContext, target, json);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }
}
