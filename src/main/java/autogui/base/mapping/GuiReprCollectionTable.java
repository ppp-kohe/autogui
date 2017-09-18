package autogui.base.mapping;

import java.util.Collections;
import java.util.List;

public class GuiReprCollectionTable extends GuiReprValue implements GuiRepresentation {
    protected GuiRepresentation subRepresentation;

    public GuiReprCollectionTable(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementCollection()) {
            context.setRepresentation(this);
            // [Collection(List), Object(E), GuiReprCollectionTable] : GuiSwingViewCollectionTable
            //    -> [Object(E), GuiReprObjectPane]                   : null? dummy?              //List<E> & class E { String prop; }
            //        -> [Property(E, prop), GuiReprValueStringField]  : GuiSwingTableColumnString

            //or  -> [Value(String), GuiReprValueStringField]         : GuiSwingTableColumnString  //List<String>

            //or  -> [Collection(Object[]), Object(E), GuiReprCollectionTable] : null? dummy?     //List<E[]>
            //        -> [Property(E, prop), GuiReprValueStringField]  : GuiSwingTableColumnString

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
}
