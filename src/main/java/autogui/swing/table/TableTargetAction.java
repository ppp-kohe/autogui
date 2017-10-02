package autogui.swing.table;

import javax.swing.*;
import java.awt.event.ActionEvent;

public interface TableTargetAction extends Action {
    void actionPerformedOnTable(ActionEvent e, TableTarget target);
}
