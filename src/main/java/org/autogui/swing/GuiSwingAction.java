package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
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

    /**
     * @param context the context of the action
     * @return edit-action handler
     * @since 1.9
     */
    List<ActionResultHandler> getEditStart(GuiMappingContext context);

    /**
     * the action handler for task after running an action
     * @since 1.9
     */
    interface ActionResultHandler extends GuiSwingFeature {
        void run(GuiSwingActionDefault.ExecutionActionParameters parameters, Object o);
    }

    /**
     * context-information for {@link GuiSwingActionDefault.ExecutionAction} and {@link ActionResultHandler}
     * @since 1.9
     */
    class ExecutionActionParameters {
        protected GuiReprValue.ObjectSpecifier specifier;
        protected ActionEvent originalEvent;

        public ExecutionActionParameters(ActionEvent originalEvent, GuiReprValue.ObjectSpecifier specifier) {
            this.originalEvent = originalEvent;
            this.specifier = specifier;
        }

        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifier;
        }

        public ActionEvent getOriginalEvent() {
            return originalEvent;
        }
    }
}
