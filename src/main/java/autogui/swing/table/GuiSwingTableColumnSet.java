package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;

public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, ObjectTableModel model);

    List<Action> createColumnActions(GuiMappingContext context, Supplier<Boolean> selectionEmpty,
                                     Supplier<List<?>> selectionItems);
}
