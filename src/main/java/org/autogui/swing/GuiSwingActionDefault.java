package org.autogui.swing;

import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprAction;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.table.GuiSwingTableColumnSet;
import org.autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    /** @since 1.9 */
    protected GuiSwingMapperSet mapperSet;

    /**
     * @param mapperSet the view factory for edit-action
     * @since 1.9
     */
    public GuiSwingActionDefault(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    public GuiSwingActionDefault() {}
    @Override
    public Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                               List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        ExecutionAction a = new ExecutionAction(context, pane::getSpecifier);
        a.addActionResultHandlers(getTableConversions(context, tables));
        a.addActionResultHandlers(getEditStart(context));
        return a;
    }

    @Override
    public List<ActionResultHandler> getEditStart(GuiMappingContext context) {
        if (context.isReprActionOrActionListEditAction()) {
            var resultHandler = createEditStart(context, mapperSet);
            return resultHandler == null ? List.of() : List.of(resultHandler);
        } else {
            return List.of();
        }
    }

    /**
     *
     * @param context the edit action context
     * @param mapperSet the factory of the view
     * @return the created edit-action or null if no edit feature
     * @since 1.9
     */
    public static ActionResultHandler createEditStart(GuiMappingContext context, GuiSwingMapperSet mapperSet) {
        if (mapperSet != null) {
            var editorContext = createEditorContext(context);
            if (editorContext != null && mapperSet.view(editorContext) instanceof GuiSwingView paneSource) {
                return new ActionResultHandlerEditStarter(context, editorContext, paneSource);
            }
        }
        return null;
    }

    /**
     * @param context the edit action context
     * @return a copied context for an isolated window of the editor object
     * @since 1.9
     */
    public static GuiMappingContext createEditorContext(GuiMappingContext context) {
        if (!context.getChildren().isEmpty()) {
            var sourceContext = context.getChildren().getFirst();
            var editorContext = sourceContext.copyDetachedTree();
            editorContext.setPreferences(sourceContext.getPreferences());
            return editorContext;
        } else {
            return null;
        }
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
    public List<ActionResultTableSelectionConversion> getTableConversions(GuiMappingContext context, List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        GuiReprAction action = context.getReprAction();
        List<ActionResultTableSelectionConversion> conversions = new ArrayList<>();
        for (GuiSwingViewCollectionTable.CollectionTable table : tables) {
            if (action.isSelectionChangeRowIndicesAction(context, table.getSwingViewContext())) {
                conversions.add(new ActionResultTableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeIndices));
            } else if (action.isSelectionChangeRowAndColumnIndicesAction(context, table.getSwingViewContext())) {
                conversions.add(new ActionResultTableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeIndicesRowAndColumn));
            } else if (action.isSelectionChangeAction(context, table.getSwingViewContext())) {
                conversions.add(new ActionResultTableSelectionConversion(context, table, GuiSwingTableColumnSet::createChangeValues));
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
     * @since 1.9
     */
    public static class ActionResultTableSelectionConversion implements ActionResultHandler {
        protected GuiMappingContext context;
        public GuiSwingTableColumnSet.TableSelectionSource table;
        public Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion;

        public ActionResultTableSelectionConversion(GuiMappingContext context, GuiSwingTableColumnSet.TableSelectionSource table,
                                                    Function<Object, GuiSwingTableColumnSet.TableSelectionChange> conversion) {
            this.context = context;
            this.table = table;
            this.conversion = conversion;
        }

        @Override
        public void run(ExecutionActionParameters parameters, Object o) {
            table.selectionActionFinished(isAutoSelection(parameters), conversion.apply(o));
        }

        protected boolean isAutoSelection(ExecutionActionParameters parameters) {
            return false;
        }

        public GuiSwingTableColumnSet.TableSelectionSource getTable() {
            return table;
        }

        public Function<Object, GuiSwingTableColumnSet.TableSelectionChange> getConversion() {
            return conversion;
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

        protected AtomicBoolean running = new AtomicBoolean(false);
        protected GuiSwingView.SpecifierManager targetSpecifier;
        /** if true, the action shows a confirmation dialog before executing
         * @since 1.8 */
        protected boolean needToConfirm;
        /** the message of the confirmation dialog showen when {@link #needToConfirm}
         * @since 1.8 */
        protected String confirmDescription;

        /** @since 1.9 */protected List<ActionResultHandler> actionResultHandlers = Collections.emptyList();

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

        /**
         * @param actionResultHandlers new action handlers
         * @since 1.9
         */
        public void setActionResultHandlers(List<ActionResultHandler> actionResultHandlers) {
            this.actionResultHandlers = actionResultHandlers;
        }

        /**
         * @param resultActions safely adding action handlers
         * @since 1.9
         */
        public void addActionResultHandlers(List<? extends ActionResultHandler> resultActions) {
            if (this.actionResultHandlers.isEmpty() && !resultActions.isEmpty()) {
                this.actionResultHandlers = new ArrayList<>(resultActions);
            } else if (!resultActions.isEmpty()) {
                this.actionResultHandlers.addAll(resultActions);
            }
        }

        /**
         * @return current aciton-handlers
         * @since 1.9
         */
        public List<ActionResultHandler> getActionResultHandlers() {
            return actionResultHandlers;
        }

        @Override
        public List<?> getSwingViewSubNodes() {
            return getActionResultHandlers();
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
                var parameters = new ExecutionActionParameters(e, specifier);
                executeContextTask(() -> executeAction(specifier, e),
                        r -> {
                            running.set(false);
                            afterActionPerformedWithoutCheckingRunning(parameters, r);
                        });
            };
            if (needToConfirm) {
                executeWithConfirm(e, body, () -> running.set(false));
            } else {
                body.run();
            }
        }

        /**
         * runs action-handlers
         * @param parameters the action parameters
         * @param r the task-result
         * @since 1.9
         */
        public void afterActionPerformedWithoutCheckingRunning(ExecutionActionParameters parameters, GuiSwingTaskRunner.ContextTaskResult<Object> r) {
            r.executeIfPresent(res -> {
                if (!actionResultHandlers.isEmpty()) {
                    SwingDeferredRunner.invokeLater(() ->
                            actionResultHandlers.forEach(c -> c.run(parameters, res)));
                }
            });
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

    /**
     * the handler implementation for an edit-action
     * @since 1.9
     */
    public static class ActionResultHandlerEditStarter implements ActionResultHandler {
        protected GuiMappingContext actionContext;
        protected GuiMappingContext editorContext;
        protected JComponent rootContentPane;
        protected JFrame frame;
        protected GuiSwingRootPane rootPane;

        protected GuiReprValue.ObjectSpecifier objectSpecifier;
        protected Object currentTarget;
        protected boolean showen = false;

        public ActionResultHandlerEditStarter(GuiMappingContext actionContext, GuiMappingContext editorContext, GuiSwingView view) {
            this(actionContext, editorContext, view.createView(editorContext, GuiSwingView.specifierManagerRoot()));
        }

        public ActionResultHandlerEditStarter(GuiMappingContext actionContext, GuiMappingContext editorContext, JComponent rootContentPane) {
            this.actionContext = actionContext;
            this.editorContext = editorContext;
            this.rootContentPane = rootContentPane;
            initFrame();
        }

        @Override
        public void run(ExecutionActionParameters parameters, Object o) {
            setupObject(parameters.getSpecifier(), o);
            show(parameters.getOriginalEvent());
        }

        protected void initFrame() {
            JPanel content = new JPanel(new BorderLayout());
            {
                content.add(rootContentPane, BorderLayout.CENTER);
                var s = UIManagerUtil.getInstance().getScaledSizeInt(5);
                content.setBorder(BorderFactory.createEmptyBorder(s, s, s, s));
            }
            rootPane = new ActionResultRootPane(editorContext, content);
            frame = new ActionResultWindow(rootPane);

            initButtonPane(content);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    finishEdit(false);
                }
            });
            frame.pack();
        }

        protected void initButtonPane(JPanel content) {
            if (actionContext.isReprActionOrActionListEditWithCompleteEdit()) {
                var actionType = actionContext.getTypeElementAsAction();
                JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));


                if (actionType.getEditFeature().isCompleteEditMethodTakingOk()) {
                    var cancelAction = new EditFinishAction(false, this::finishEdit);
                    buttonPane.add(new JButton(cancelAction));
                    addAction("cancel", PopupExtension.getKeyStroke(KeyEvent.VK_ESCAPE), cancelAction);
                }
                var okAction = new EditFinishAction(true, this::finishEdit);
                buttonPane.add(new JButton(okAction));
                addAction("ok", PopupExtension.getKeyStroke(KeyEvent.VK_ENTER), okAction);

                JPanel sepPane = new JPanel(new BorderLayout());
                {
                    var sep = new JSeparator(JSeparator.HORIZONTAL);
                    UIManagerUtil util = UIManagerUtil.getInstance();
                    sep.setBorder(BorderFactory.createEmptyBorder(util.getScaledSizeInt(3), 0, 0, 0));
                    sep.setPreferredSize(new Dimension(1, util.getScaledSizeInt(2)));
                    sepPane.add(sep, BorderLayout.NORTH);
                    sepPane.add(buttonPane);
                }
                content.add(sepPane, BorderLayout.SOUTH);
            }
        }

        protected void addAction(String name, KeyStroke stroke, Action a) {
            var root = frame.getRootPane();
            root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, name);
            root.getActionMap().put(name, a);
        }

        @SuppressWarnings("unchecked")
        protected void setupObject(GuiReprValue.ObjectSpecifier objectSpecifier, Object o) {
            this.objectSpecifier = objectSpecifier;
            if (rootContentPane instanceof GuiSwingView.ValuePane<?> vp) {
                var v = (GuiSwingView.ValuePane<Object>) vp;
                v.setSwingViewValueWithUpdate(o);
            }
            this.currentTarget = o;
        }

        protected void show(ActionEvent e) {
            frame.setVisible(true);
            if (e != null && e.getSource() instanceof Component component && !showen) { //after showen, move the location
                showen = true;
                try {
                    var pos = component.getLocationOnScreen();
                    pos.x += (int) (component.getWidth() * 0.5);
                    pos.y += (int) (component.getHeight() * 0.7);
                    frame.setLocation(pos);
                } catch (IllegalComponentStateException ex) {
                    //
                }
            }
        }

        @Override
        public void shutdownSwingView() {
            if (frame != null) {
                frame.dispose();
            }
            editorContext.shutdown();
        }

        public void finishEdit(boolean ok) {
            if (currentTarget != null) {
                finishEditCurrentTarget(ok, currentTarget);
            }
            currentTarget = null;
            if (frame != null) {
                frame.setVisible(false);
            }
        }

        protected void finishEditCurrentTarget(boolean ok, Object currentTarget) {
            if (actionContext.isReprAction()) {
                actionContext.getReprAction().executeCompleteEditAction(actionContext, objectSpecifier, currentTarget, ok);
                actionContext.updateSourceFromRoot();
            }
        }

        public JFrame getFrame() {
            return frame;
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
            return List.of(frame);
        }
    }

    /**
     * action for editor buttion; "OK" or "Cancel" buttons
     * @since 1.9
     */
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

    /**
     * window class for an editor object
     * @since 1.9
     */
    public static class ActionResultWindow extends GuiSwingWindow {
        public ActionResultWindow(GuiSwingRootPane contextRootPane) {
            super(contextRootPane);
        }

        @Override
        protected void initMenu() { }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            loadPreferences(prefs, options, false);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            savePreferences(prefs, false);
        }

        @Override
        public void loadPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            //nothing
        }

        @Override
        public void savePreferences(GuiPreferences prefs) {
            //nothing
        }
    }

    /**
     * root-pane for an editor object
     * @since 1.9
     */
    public static class ActionResultRootPane extends GuiSwingRootPane {
        public ActionResultRootPane(GuiMappingContext editorContext, JComponent content) {
            super(editorContext, null,
                    GuiSwingKeyBinding.createWithDefaultExcluded(),
                    false, null, //TODO
                    new GuiSwingPrefsApplyOptions.PrefsApplyOptionsDefault(true, false),
                    content);
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            super.loadPreferences(prefs, options, false);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            super.savePreferences(prefs, false);
        }
    }
}
