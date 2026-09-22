package org.autogui.swing;

import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprAction;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.table.GuiSwingTableColumnSet;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.SwingDeferredRunner;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * an implementation of action factory for {@link GuiReprAction}.
 * An instance of the class is also registered for table-column, but the instance will not be used.
 */
public class GuiSwingActionDefault implements GuiSwingAction {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingActionDefault(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    public GuiSwingActionDefault() {}
    @Override
    public Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                               List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        List<ResultAction> resultActions = new ArrayList<>();
        resultActions.addAll(getTableConversions(context, tables));
        resultActions.addAll(getEditStart(context, pane));

        ExecutionAction a = new ExecutionAction(context, pane::getSpecifier);
        a.setResultActions(resultActions);
        return a;
    }

    public List<ResultAction> getEditStart(GuiMappingContext context, GuiSwingView.ValuePane<?> pane) {
        if (context.isReprAction() && context.getReprAction().isEditAction(context) && mapperSet != null &&
            !context.getChildren().isEmpty()) {
            var sourceContext = context.getChildren().getFirst();
            var editorContext = sourceContext.copyDetachedTree();
            editorContext.setPreferences(sourceContext.getPreferences());
            if (mapperSet.view(editorContext) instanceof GuiSwingView paneSource) {
                return List.of(new ResultActionEditFinish(context, editorContext, paneSource.createView(editorContext, GuiSwingView.specifierManagerRoot())));
            }
        }
        return Collections.emptyList();
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
            if (action.isSelectionChangeRowIndicesAction(context, table.getSwingViewContext())) {
                conversions.add(new TableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeIndices));
            } else if (action.isSelectionChangeRowAndColumnIndicesAction(context, table.getSwingViewContext())) {
                conversions.add(new TableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeIndicesRowAndColumn));
            } else if (action.isSelectionChangeAction(context, table.getSwingViewContext())) {
                conversions.add(new TableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeValues));
            }
        }
        return conversions;
    }

    /**
     * @since 1.9
     */
    public interface ResultAction extends GuiSwingFeature {
        default void run(GuiReprValue.ObjectSpecifier objectSpecifier, ActionEvent originalEvent, Object o) {
            run(o);
        }
        void run(Object o);
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
    public static class TableSelectionConversion implements ResultAction {
        protected GuiMappingContext context;
        public GuiSwingTableColumnSet.TableSelectionSource table;
        public Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion;

        public TableSelectionConversion(GuiMappingContext context, GuiSwingTableColumnSet.TableSelectionSource table,
                                        Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion) {
            this.context = context;
            this.table = table;
            this.conversion = conversion;
        }

        @Override
        public void run(Object o) {
            table.selectionActionFinished(false, conversion.apply(o));
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return GuiReprValue.NONE;
        }

        @Override
        public void prepareForRefresh() { }

        @Override
        public List<?> getSwingViewSubNodes() {
            return List.of();
        }
    }

    /**
     * a swing-action executing the action of the context.
     * the icon will be automatically determined by the name of the context via {@link GuiSwingIcons}
     */
    public static class ExecutionAction extends GuiSwingTaskRunner.ContextAction implements PopupCategorized.CategorizedMenuItemAction,
            GuiSwingKeyBinding.RecommendedKeyStroke, GuiSwingFeature {
        @Serial private static final long serialVersionUID = 1L;

        protected Consumer<Object> resultTarget;
        protected AtomicBoolean running = new AtomicBoolean(false);
        protected GuiSwingView.SpecifierManager targetSpecifier;
        /** if true, the action shows a confirmation dialog before executing
         * @since 1.8 */
        protected boolean needToConfirm;
        /** the message of the confirmation dialog showen when {@link #needToConfirm}
         * @since 1.8 */
        protected String confirmDescription;

        /** @since 1.9 */protected List<ResultAction> resultActions = Collections.emptyList();

        @SuppressWarnings("this-escape")
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

            initNeedToConfirmDefault();
        }

        /**
         * set {@link #needToConfirm} and {@link #confirmDescription} from init-annotation attached to the action method
         * @since 1.8
         */
        protected void initNeedToConfirmDefault() {
            if (getContext().isTypeElementAction() ||
                    getContext().isTypeElementActionList()) {
                var inits = getContext().getTypeElementAsAction().getInits();
                needToConfirm = inits.action().confirm();
                confirmDescription = getContext().getDescription();
            }
        }

        public void setResultTarget(Consumer<Object> resultTarget) {
            this.resultTarget = resultTarget;
        }

        public void setResultActions(List<ResultAction> resultActions) {
            this.resultActions = resultActions;
        }

        @Override
        public List<?> getSwingViewSubNodes() {
            return resultActions;
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
            Runnable body = () -> {
                ActionPreparation.prepareAction(e);
                GuiReprValue.ObjectSpecifier specifier = targetSpecifier.getSpecifier();
                executeContextTask(() -> executeAction(specifier, e),
                        r -> {
                            running.set(false);
                            if (resultTarget != null || !resultActions.isEmpty()) {
                                r.executeIfPresent(res ->
                                        SwingDeferredRunner.invokeLater(() -> {
                                            if (resultTarget != null) {
                                                resultTarget.accept(res);
                                            }
                                            resultActions.forEach(c -> c.run(specifier, e, res));
                                        }));
                            }
                        });
            };
            if (needToConfirm) {
                executeWithConfirm(e, body, () -> running.set(false));
            } else {
                body.run();
            }
        }

        public String getIconName() {
            return getContext().getIconName();
        }

        /**
         * the void returning version of {@link #executeWithConfirmReturn(ActionEvent, Supplier, Supplier)}
         * @param e the action-event used for showing the confirmation dialog
         * @param action the action code body; non-null
         * @param cancelAction the action executed when the user selects canceling, or null
         * @since 1.8
         */
        public void  executeWithConfirm(ActionEvent e, Runnable action, Runnable cancelAction) {
            this.<Void>executeWithConfirmReturn(e, () -> {
                action.run();
                return null;
            }, () -> {
                if (cancelAction != null) {
                    cancelAction.run();
                }
                return null;
            });
        }

        /**
         * show a confirmation dialog and run the action if the user selects OK of the dialog
         * @param e the action-event used for showing the confirmation dialog;
         *          obtaining the root-pane of a source component and used for the dialog parent
         * @param action the action code body; non-null
         * @param cancelAction the action when cancelled ; non-null
         * @return the returned value of the action (or calcelAction)
         * @param <RetType> the returned type of the action
         * @since 1.8
         */
        public <RetType> RetType executeWithConfirmReturn(ActionEvent e, Supplier<RetType> action, Supplier<RetType> cancelAction) {
            String name = Objects.toString(getValue(NAME));
            int res = JOptionPane.showConfirmDialog(componentForDialogParent(e),
                    String.format("Run the action \"%s\"?%s", name,
                            (confirmDescription == null || confirmDescription.isEmpty() ? "" : (" : \n  " + confirmDescription))), name, JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                return action.get();
            } else {
                return cancelAction.get();
            }
        }

        private JComponent componentForDialogParent(ActionEvent e) {
            if (e.getSource() instanceof JComponent comp) {
                return SwingUtilities.getRootPane(comp);
            } else {
                return null;
            }
        }

        public Object executeAction(GuiReprValue.ObjectSpecifier specifier) {
            return executeAction(specifier, null);
        }

        /**
         * @param specifier a specifier for the target object
         * @param e an optional action event
         * @since 1.8
         */
        public Object executeAction(GuiReprValue.ObjectSpecifier specifier, ActionEvent e) {
            return getSwingViewContext().executeAction(specifier);
        }

        @Override
        public Icon getIcon() {
            return GuiSwingIcons.getInstance().getIcon(getIconName());
        }

        public Icon getActionPressedIcon() {
            return GuiSwingIcons.getInstance().getPressedIcon(getIconName());
        }

        @Override
        public KeyStroke getRecommendedKeyStroke() {
            return GuiSwingKeyBinding.getKeyStroke(getSwingViewContext().getAcceleratorKeyStroke());
        }

        @Override
        public GuiSwingKeyBinding.KeyPrecedenceSet getRecommendedKeyPrecedence() {
            if (getSwingViewContext().isAcceleratorKeyStrokeSpecified()) {
                return new GuiSwingKeyBinding.KeyPrecedenceSet(GuiSwingKeyBinding.PRECEDENCE_FLAG_USER_SPECIFIED);
            } else {
                return new GuiSwingKeyBinding.KeyPrecedenceSet();
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return this.getContext();
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return targetSpecifier.getSpecifier();
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

        public ActionPreparation() {}

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

    public static class ResultActionEditFinish implements ResultAction {
        protected GuiMappingContext actionContext;
        protected GuiMappingContext editorContext;
        protected JComponent rootPane;
        protected JFrame frame;

        protected GuiReprValue.ObjectSpecifier objectSpecifier;
        protected Object currentTarget;

        public ResultActionEditFinish(GuiMappingContext actionContext, GuiMappingContext editorContext, JComponent rootPane) {
            this.actionContext = actionContext;
            this.editorContext = editorContext;
            this.rootPane = rootPane;
        }

        @Override
        public void run(GuiReprValue.ObjectSpecifier objectSpecifier, ActionEvent e, Object o) {
            if (frame == null) {
                initFrame();
            }
            setupObject(objectSpecifier, o);
            show(e);
        }

        protected void initFrame() {
            frame = new JFrame();
            frame.setType(Window.Type.UTILITY);
            JPanel content = new JPanel(new BorderLayout());
            {
                content.add(rootPane, BorderLayout.CENTER);
                var s = UIManagerUtil.getInstance().getScaledSizeInt(5);
                content.setBorder(BorderFactory.createEmptyBorder(s, s, s, s));

                initButtonPane(content);
            }
            frame.setContentPane(content);
            frame.setTitle(editorContext.getDisplayName());
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    finishEdit(false);
                }
            });
            frame.pack();
        }

        protected void initButtonPane(JPanel content) {
            var actionRepr = actionContext.getReprAction();
            if (actionRepr.isEditActionWithEdited(actionContext)) {
                var actionType = actionContext.getTypeElementAsAction();
                JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                if (actionType.getEditFeature().isEditedMethodTakingOk()) {
                    buttonPane.add(new JButton(new EditFinishAction(false, this::finishEdit)));
                }
                buttonPane.add(new JButton(new EditFinishAction(true, this::finishEdit)));
                content.add(buttonPane, BorderLayout.SOUTH);
            }
        }

        @SuppressWarnings("unchecked")
        protected void setupObject(GuiReprValue.ObjectSpecifier objectSpecifier, Object o) {
            this.objectSpecifier = objectSpecifier;
            if (rootPane instanceof GuiSwingView.ValuePane<?> vp) {
                var v = (GuiSwingView.ValuePane<Object>) vp;
                v.setSwingViewValueWithUpdate(o);
            }
            this.currentTarget = o;
        }

        protected void show(ActionEvent e) {
            if (e != null && e.getSource() instanceof Component component) {
                var pos = component.getLocationOnScreen();
                pos.x += (int) (component.getWidth() * 0.5);
                pos.y += (int) (component.getHeight() * 0.7);
                frame.setLocation(pos);
            }
            frame.setVisible(true);
        }

        @Override
        public void run(Object o) {
            run(objectSpecifier, null, o);
        }

        @Override
        public void shutdownSwingView() {
            frame.dispose();
            editorContext.shutdown();
        }

        public void finishEdit(boolean ok) {
            if (currentTarget != null) {
                actionContext.getReprAction().executedEditedAction(actionContext, objectSpecifier, currentTarget, ok);
                actionContext.updateSourceFromRoot();
            }
            currentTarget = null;
            frame.setVisible(false);
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return editorContext;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return objectSpecifier;
        }

        @Override
        public void prepareForRefresh() { }

        @Override
        public List<?> getSwingViewSubNodes() {
            return List.of();
        }
    }

    public static class EditFinishAction extends AbstractAction {
        protected boolean ok;
        protected Consumer<Boolean> callback;
        @SuppressWarnings("this-escape")
        public EditFinishAction(boolean ok, Consumer<Boolean> callback) {
            this.ok = ok;
            this.callback = callback;
            putValue(NAME, ok ? "OK" : "Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            callback.accept(ok);
        }
    }
}
