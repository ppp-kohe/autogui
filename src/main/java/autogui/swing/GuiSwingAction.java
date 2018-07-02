package autogui.swing;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;
import java.util.List;

/**
 * factory interface for {@link Action}s
 */
public interface GuiSwingAction extends GuiSwingElement {
    /**
     * @param context the context of an action
     * @param pane pane for the target object (currently used for obtaining the specifier of the target)
     * @param tables in order to support selection changer, the method takes candidates of table
     * @return a swing-action for executing the action of the context
     */
    Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                        List<GuiSwingViewCollectionTable.CollectionTable> tables);
}
