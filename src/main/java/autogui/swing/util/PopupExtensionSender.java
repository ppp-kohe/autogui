package autogui.swing.util;

import javax.swing.*;
import java.util.function.Consumer;

/** a sender object passed to {@link PopupExtension.PopupMenuBuilder#build(PopupExtensionSender, Consumer)} */
public interface PopupExtensionSender {
    JComponent getPane();
}
