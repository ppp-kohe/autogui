package autogui.base.type;

import java.util.Collections;
import java.util.List;

public interface GuiTypeElement {
    String getName();

    default List<GuiTypeElement> getChildren() {
        return Collections.emptyList();
    }
}
