package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingAction;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingViewCollectionTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnSetDefault implements GuiSwingTableColumnSet {
    protected GuiSwingMapperSet columnMappingSet;

    public GuiSwingTableColumnSetDefault(GuiSwingMapperSet columnMappingSet) {
        this.columnMappingSet = columnMappingSet;
    }

    @Override
    public void createColumns(GuiMappingContext context, ObjectTableModel model) {
        model.addColumnRowIndex();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingTableColumn) {
                model.addColumn(((GuiSwingTableColumn) subView).createColumn(subContext));
            }
        }
    }

    @Override
    public List<Action> createColumnActions(GuiMappingContext context, Supplier<Boolean> selectionEmpty,
                                            Supplier<List<?>> selectionItems) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingAction) {
                actions.add(new GuiSwingViewCollectionTable.TableSelectionAction(subContext, selectionEmpty, selectionItems));
            }
        }
        return actions;
    }
}
