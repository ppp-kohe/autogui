package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class GuiSwingActionDefault implements GuiSwingAction {
    @Override
    public Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                               List<GuiSwingViewCollectionTable.CollectionTable> tables) {

        List<TableSelectionConversion> conversions = getTableConversions(context, tables);
        ExecutionAction a = new ExecutionAction(context);
        a.setResultTarget(r ->
                conversions.forEach(c -> c.run(r)));
        return a;
    }

    public List<TableSelectionConversion> getTableConversions(GuiMappingContext context, List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        GuiReprAction action = context.getReprAction();

        List<TableSelectionConversion> conversions = new ArrayList<>();
        for (GuiSwingViewCollectionTable.CollectionTable table : tables) {
            if (action.isSelectionChangeAction(context, table.getSwingViewContext())) {
                conversions.add(new TableSelectionConversion(table, GuiSwingTableColumnSet::createChangeValues));
            } else if (action.isSelectionChangeRowIndexesAction(context)) {
                conversions.add(new TableSelectionConversion(table, GuiSwingTableColumnSet::createChangeIndexes));
            } else if (action.isSelectionChangeRowAndColumnIndexesAction(context)) {
                conversions.add(new TableSelectionConversion(table, GuiSwingTableColumnSet::createChangeIndexesRowAndColumn));
            }
        }
        return conversions;
    }

    public static class TableSelectionConversion {
        public GuiSwingTableColumnSet.TableSelectionSource table;
        public Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion;

        public TableSelectionConversion(GuiSwingTableColumnSet.TableSelectionSource table,
                                        Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion) {
            this.table = table;
            this.conversion = conversion;
        }

        public void run(Object o) {
            table.selectionActionFinished(false, conversion.apply(o));
        }
    }

    public static class ExecutionAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction,
            GuiSwingKeyBinding.RecommendedKeyStroke {
        protected GuiMappingContext context;
        protected Consumer<Object> resultTarget;

        public ExecutionAction(GuiMappingContext context) {
            this.context = context;
            putValue(Action.NAME, context.getDisplayName());

            Icon icon = getIcon();
            if (icon != null) {
                putValue(LARGE_ICON_KEY, icon);
            }
            Icon pressIcon = getActionPressedIcon();
            if (pressIcon != null) {
                putValue(GuiSwingIcons.PRESSED_ICON_KEY, pressIcon);
            }

            String desc = context.getDescription();
            if (!desc.isEmpty()) {
                putValue(Action.SHORT_DESCRIPTION, desc);
            }
        }

        public void setResultTarget(Consumer<Object> resultTarget) {
            this.resultTarget = resultTarget;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object r = executeAction();
            if (resultTarget != null) {
                resultTarget.accept(r);
            }
        }

        public String getIconName() {
            return context.getIconName();
        }

        public GuiMappingContext getContext() {
            return context;
        }

        public Object executeAction() {
            return context.executeAction();
        }

        @Override
        public Icon getIcon() {
            return GuiSwingIcons.getInstance().getIcon(getIconName());
        }

        public Icon getActionPressedIcon() {
            return GuiSwingIcons.getInstance().getPressedIcon(getIconName());
        }

        @Override
        public String getCategory() {
            return PopupCategorized.CATEGORY_ACTION;
        }


        @Override
        public KeyStroke getRecommendedKeyStroke() {
            return GuiSwingKeyBinding.getKeyStroke(context.getAcceleratorKeyStroke());
        }

        @Override
        public GuiSwingKeyBinding.KeyPrecedenceSet getRecommendedKeyPrecedence() {
            if (context.isAcceleratorKeyStrokeSpecified()) {
                return new GuiSwingKeyBinding.KeyPrecedenceSet(GuiSwingKeyBinding.PRECEDENCE_FLAG_USER_SPECIFIED);
            } else {
                return new GuiSwingKeyBinding.KeyPrecedenceSet();
            }
        }
    }
}
