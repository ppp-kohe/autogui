package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.util.EventObject;
import java.util.function.Consumer;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }


    interface ValuePane {
        Object getSwingViewValue();
        /** updates GUI display,
         *   and it does NOT update the target model value.
         * processed under the event thread */
        void setSwingViewValue(Object value);

        default void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
        }

        PopupExtension.PopupMenuBuilder getSwingMenuBuilder();
    }
}
