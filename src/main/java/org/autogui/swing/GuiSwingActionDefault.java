package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprAction;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.table.GuiSwingTableColumnSet;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.GuiListSelectionUpdater;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * an implementation of action factory for {@link GuiReprAction}.
 * An instance of the class is also registered for table-column, but the instance will not be used.
 */
public class GuiSwingActionDefault implements GuiSwingAction {
    @Override
    public Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                               List<GuiSwingViewCollectionTable.CollectionTable> tables) {

        List<TableSelectionConversion> conversions = getTableConversions(context, tables);
        ExecutionAction a = new ExecutionAction(context, pane::getSpecifier);
        a.setResultTarget(r -> {
            conversions.forEach(c -> c.run(r));
        });
        return a;
    }

    /**
     * creating runners of selection updating for tables.
     * if there are multiple lists matching to the return type of the action, the selection of those lists updated by the action at once.
     * <pre> //e.g.
     *     &#64;GuiIncluded public class C {
     *         &#64;GuiIncluded public List&lt;String&gt; a;
     *         &#64;GuiIncluded public List&lt;String&gt; b;
     *
     *         &#64;GuiListSelectionUpdater
     *         &#64;GuiIncluded public List&lt;String&gt; act() { ... } //the action updates selection of both a and b;
     *     }
     * </pre>
     * @param context the context of the action
     * @param tables the candidates of tables for the target of updating
     * @return runners
     */
    public List<TableSelectionConversion> getTableConversions(GuiMappingContext context, List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        GuiReprAction action = context.getReprAction();
        List<TableSelectionConversion> conversions = new ArrayList<>();
        for (GuiSwingViewCollectionTable.CollectionTable table : tables) {
            if (action.isSelectionChangeAction(context, table.getSwingViewContext())) {
                conversions.add(new TableSelectionConversion(table, GuiSwingTableColumnSet::createChangeValues));
            }
        }
        return conversions;
    }

    /**
     * the table selection updater for object-actions:
     *  <pre>
     *      &#64;GuiIncluded public class C {
     *          &#64;GuiIncluded public List&lt;String&gt; list;
     *
     *          &#64;{@link GuiListSelectionUpdater}
     *          &#64;GuiIncluded public List&lt;String&gt; action() { ... }
     *           //the action does not specify the list directly
     *      }
     *  </pre>
     */
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

    /**
     * a swing-action executing the action of the context.
     * the icon will be automatically determined by the name of the context via {@link GuiSwingIcons}
     */
    public static class ExecutionAction extends GuiSwingTaskRunner.ContextAction implements PopupCategorized.CategorizedMenuItemAction,
            GuiSwingKeyBinding.RecommendedKeyStroke {
        private static final long serialVersionUID = 1L;

        protected Consumer<Object> resultTarget;
        protected AtomicBoolean running = new AtomicBoolean(false);
        protected GuiSwingView.SpecifierManager targetSpecifier;

        public ExecutionAction(GuiMappingContext context, GuiSwingView.SpecifierManager targetSpecifier) {
            super(context);
            this.targetSpecifier = targetSpecifier;
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
            if (!running.getAndSet(true)) {
                actionPerformedWithoutCheckingRunning(e);
            } else {
                System.err.printf("already running action \"%s\" \n", getValue(NAME));
            }
        }

        public void actionPerformedWithoutCheckingRunning(ActionEvent e) {
            ActionPreparation.prepareAction(e);
            GuiReprValue.ObjectSpecifier specifier = targetSpecifier.getSpecifier();
            executeContextTask(() -> executeAction(specifier),
                    r -> {
                        running.set(false);
                        if (resultTarget != null) {
                            r.executeIfPresent(res ->
                                SwingUtilities.invokeLater(() -> resultTarget.accept(res)));
                        }
                    });
        }

        public String getIconName() {
            return getContext().getIconName();
        }

        public Object executeAction(GuiReprValue.ObjectSpecifier specifier) {
            return getContext().executeAction(specifier);
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
            return GuiSwingKeyBinding.getKeyStroke(getContext().getAcceleratorKeyStroke());
        }

        @Override
        public GuiSwingKeyBinding.KeyPrecedenceSet getRecommendedKeyPrecedence() {
            if (getContext().isAcceleratorKeyStrokeSpecified()) {
                return new GuiSwingKeyBinding.KeyPrecedenceSet(GuiSwingKeyBinding.PRECEDENCE_FLAG_USER_SPECIFIED);
            } else {
                return new GuiSwingKeyBinding.KeyPrecedenceSet();
            }
        }
    }

    /**
     * the action that may lead to
     *      refreshing data or discarding cell-editor of table
     *      needs to call {@link #prepareAction(AWTEvent)} or {@link #prepareAction(Component)}.
     *       The menu action might have no parent components, so it needs to call the component version.
     * @since 1.2
     */
    public static class ActionPreparation {
        protected Map<Object, Runnable> editingTables = new WeakHashMap<>();

        public void register(Object key, Runnable task) {
            editingTables.put(key, task);
        }

        public void unregister(Object key) {
            editingTables.remove(key);
        }

        public void prepareAction() {
            new ArrayList<>(editingTables.values())
                    .forEach(Runnable::run);
        }

        static Map<Component, ActionPreparation> finisher = new HashMap<>();

        public static ActionPreparation get(Component component) {
            Container c = component.getParent();
            if (c != null) {
                return get(c);
            } else {
                return finisher.computeIfAbsent(component,
                        _c -> new ActionPreparation());
            }
        }

        public static void prepareAction(AWTEvent e) {
            if (e != null && e.getSource() instanceof Component) {
                get((Component) e.getSource()).prepareAction();
            }
        }

        public static void prepareAction(Component c) {
            if (c != null) {
                get(c).prepareAction();
            }
        }
    }
}
