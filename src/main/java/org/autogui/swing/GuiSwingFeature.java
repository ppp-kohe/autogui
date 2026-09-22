package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * abstraction for GUI element that might not have a displayable area and a specific value.
 * <p>
 *     GUI elements construct a tree.
 *     <pre>
 *         rootNode
 *            component1
 *            component2
 *               component3
 *                 ...
 *     </pre>
 *     Each node might have a {@link GuiMappingContext}. it also construct a tree.
 *     <pre>
 *         rootNode(context0)
 *            component1(context1)
 *            component2(context2)
 *               component3(context2) //share context2 with parent
 *                 component4(context3)
 *                 ...
 *     </pre>
 *     Each node might be a {@link Component}, or a {@link org.autogui.swing.GuiSwingView.ValuePane}.
 *     <pre>
 *         rootNode(context0) : JRootPane
 *            pane1 : JPanel
 *            pane2 : JPanel
 *               component1(context1) : ValuePane
 *               scrollPane(context2) : ValuePane
 *                  component2(context2) : ValuePane
 *                     component3(context3)
 *                 ...
 *
 *     </pre>
 *     Also, each node might be a non-displayable element, like {@link Action}.
 *     <pre>
 *         rootNode(context0) : JRootPane
 *            pane1 : JPanel
 *            pane2 : JPanel
 *               component1(context1) : ValuePane
 *               scrollPane(context2) : ValuePane
 *                  component2(context2) : ValuePane
 *                     component3(context3)
 *                       action1(context4) : Action
 *                 ...
 *
 *     </pre>
 * </p>
 * <p>
 *     The interface provides a set of features of GUI applications,
 *        and accessors of sub-nodes.
 *        Note that the interface methods are basically not recursive operations for the tree, except for the root-node.
 *      Static methods in the interface can traverse the sub-nodes.
 * </p>
 * @since 1.9
 */
public interface GuiSwingFeature extends GuiSwingElement.FeatureCollection {

    /**
     * notifies property update to the component.
     * getter will be called and GUI display will be updated.
     *  the method just calls {@link GuiMappingContext#updateSourceSubTree()}.
     * @since 1.1
     */
    default void updateSwingViewSource() {
        getSwingViewContext().updateSourceSubTree();
    }

    /**
     * notifies property update to the component and enclosing other components.
     * For single case, {@link #updateSwingViewSource()} is sufficient.
     * If the component is part of tree of value-panes, the method will be effective.
     *  the method just calls {@link GuiMappingContext#updateSourceFromRoot()}.
     * @since 1.1
     */
    default void updateSwingViewSourceFromRoot() {
        getSwingViewContext().updateSourceFromRoot();
    }

    GuiMappingContext getSwingViewContext();

    /**
     * the method is used for table cell-editors, in order to observe the completion of editing.
     * the default impl. is empty.
     * @param eventHandler the handler called when the editing is finished
     */
    default void addSwingEditFinishHandler(Runnable eventHandler) { }

    default boolean isSwingEditable() {
        GuiMappingContext context = getSwingViewContext();
        return !context.isReprValue() || context.getReprValue().isEditable(context);
    }

    /**
     * only save the component itself, not including children.
     *  it can use {@link GuiSwingView#savePreferencesDefault(JComponent, GuiPreferences)}
     * @param prefs target prefs or ancestor of the target;
     *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
     */
    default void saveSwingPreferences(GuiPreferences prefs) {}

    /**
     * the convient method for loading preferences with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}.
     * In the library, the method should not call be called directly, instead, use {@link #loadSwingPreferences(GuiPreferences, GuiSwingPrefsApplyOptions)}.
     * @param prefs target prefs
     */
    default void loadSwingPreferences(GuiPreferences prefs) {
        loadSwingPreferences(prefs, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * a sub-class which wraps another value-pane should overrides the method
     *   in order to omit needless value setting
     *  it can use {@link GuiSwingView#loadPreferencesDefault(JComponent, GuiPreferences)}
     * @param prefs target prefs
     * @param options options for applying
     */
    default void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {}

    /**
     * for each component, at the end of the process, this method will be called
     */
    default void shutdownSwingView() { }


    /**
     * @return the specifier for the context value, typically obtained from {@link GuiSwingView.SpecifierManager}
     */
    GuiReprValue.ObjectSpecifier getSpecifier();

    default void requestSwingViewFocus() { }

    /**
     * set the key-stroke info. to the pane as a guide
     * @param keyStrokeString the key stroke info to be set.
     */
    default void setKeyStrokeString(String keyStrokeString) { }

    /**
     * for each component, the method is called before refreshing in order to clear the view-clock.
     */
    default void prepareForRefresh() { }

    /**
     *
     * @param task the task
     * @param afterTask the task executed for deferred value
     * @param <RetType> the value type
     * @return {@link GuiSwingTaskRunner#executeContextTask(Supplier, Consumer)}
     */
    default <RetType> GuiSwingTaskRunner.ContextTaskResult<RetType> executeContextTask(Supplier<RetType> task,
                                                                                       Consumer<GuiSwingTaskRunner.ContextTaskResult<RetType>> afterTask) {
        return new GuiSwingTaskRunner(getSwingViewContext()).executeContextTask(task, afterTask);
    }

    /**
     * prepare, clear and update via the context
     * @since 1.5
     */
    default void refreshByContext() {
        prepareForRefresh();
        if (this instanceof Component component) {
            GuiSwingActionDefault.ActionPreparation.prepareAction(component);
        }
        GuiMappingContext context = getSwingViewContext();
        context.clearSourceSubTree();
        context.updateSourceSubTree();
    }

    /**
     * @return prefs JSON
     * @since 1.5
     */
    default Object getPrefsJsonSupported() {
        Object o = this;
        if (o instanceof GuiPreferences.PreferencesJsonSupport j) {
            return j.getPrefsJson();
        } else {
            return null;
        }
    }

    /**
     * @param json prefs JSON
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    default void setPrefsJsonSupported(Object json) {
        Object o = this;
        if (o instanceof GuiPreferences.PreferencesJsonSupport j && json instanceof Map<?,?> map) {
            j.setPrefsJson((Map<String,Object>) map);
        }
    }

    /**
     * It returns a list of sub-nodes of the node.
     * <p>
     * A sub-node is a displayable view under the same window,
     *   a displayable view under the different window,
     *   or a non-displayable element.
     *  <p>
     *    If this is a {@link Container}, it typically returns {@link Container#getComponents()}.
     *  <p>
     *  A sub-node might have a different context or a shared context with {@link #getSwingViewContext()} of this.
     * @return a list of sub-nodes
     */
    List<?> getSwingViewSubNodes();

    ////////////////////////////////////////

    enum TraversalOption {
        NonTop,
        OncePertContext,
        VisitAfter,
        SameDisplay,
    }

    interface TraversalPredicate extends Predicate<Object> {
        default TraversalPredicate next(Object next) {
            return this;
        }
    }

    static <TargetType> void traverse(Object obj, Class<TargetType> type, Consumer<TargetType> task, TraversalOption... options) {
        var optionsSet = Set.of(options);
        Consumer<Object> taskActual = next -> {
            if (type.isInstance(next)) task.accept(type.cast(next));
        };

        if (optionsSet.contains(TraversalOption.NonTop)) {
            var taskPrev = taskActual;
            taskActual = next -> {
                if (next != obj) taskPrev.accept(next);
            };
        }

        TraversalPredicate enteringPredicate =
            optionsSet.contains(TraversalOption.OncePertContext) ?
                predicateOncePerContext() :
                next -> true;

        Function<Object, Stream<?>> directChildGetter =
                optionsSet.contains(TraversalOption.SameDisplay) ?
                        GuiSwingFeature::directChildren :
                        GuiSwingFeature::directChildrenSameDisplay;

        if (optionsSet.contains(TraversalOption.VisitAfter)) {
            traverseChildrenVisitAfter(obj, taskActual, enteringPredicate.next(obj), directChildGetter);
        } else {
            traverseChildren(obj, taskActual, enteringPredicate.next(obj), directChildGetter);
        }
    }

    private static TraversalPredicate predicateOncePerContext() {
        Set<GuiMappingContext> contextsHistory = new HashSet<>();
        return next ->
                !(next instanceof GuiSwingFeature) ||
                (next instanceof GuiSwingFeature feature && contextsHistory.add(feature.getSwingViewContext()));
    }

    private static void traverseChildren(Object obj, Consumer<Object> task, TraversalPredicate entering, Function<Object, Stream<?>> directChildGetter) {
        task.accept(obj);
        for (var iter = directChildGetter.apply(obj).iterator(); iter.hasNext(); ) {
            var next = iter.next();
            if (entering.test(next)) {
                traverseChildren(next, task, entering.next(next), directChildGetter);
            }
        }
    }

    private static void traverseChildrenVisitAfter(Object obj, Consumer<Object> task, TraversalPredicate entering, Function<Object, Stream<?>> directChildGetter) {
        for (var iter = directChildGetter.apply(obj).iterator(); iter.hasNext(); ) {
            var next = iter.next();
            if (entering.test(next)) {
                traverseChildrenVisitAfter(next, task, entering.next(next), directChildGetter);
            }
        }
        task.accept(obj);
    }

    static Stream<?> directChildren(Object obj) {
        if (obj instanceof GuiSwingElement.FeatureCollection collection) {
            return collection.getSwingViewSubNodes().stream();
        } else {
            return directChildrenSameDisplay(obj);
        }
    }

    static Stream<?> directChildrenSameDisplay(Object obj) {
        if (obj instanceof GuiSwingElement.FeatureCollection collection) {
            return collection.getSwingViewSubNodesSameDisplay().stream();
        } else if (obj instanceof Container container) {
            return Stream.of(container.getComponents());
        } else if (obj instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false);
        } else {
            return Stream.empty();
        }
    }

    ////////////////////////////////////////

    static <RetType> RetType traverseFindFirstIf(Object obj, Class<RetType> type, Predicate<RetType> predicate, TraversalOption... options) {
        return traverseFindFirst(obj, type,
                next -> predicate.test(next) ? next : null, options);
    }

    static <TargetType, RetType> RetType traverseFindFirst(Object obj, Class<TargetType> type, Function<TargetType, RetType> mapper, TraversalOption... options) {
        var optionsSet = Set.of(options);

        Function<TargetType, RetType> mapperActual =
                optionsSet.contains(TraversalOption.NonTop) ?
                        (next -> (next != obj) ? mapper.apply(next) : null) :
                        mapper;

        TraversalPredicate enteringPredicate =
                optionsSet.contains(TraversalOption.OncePertContext) ?
                        predicateOncePerContext() :
                        next -> true;

        Function<Object, RetType> conv = next ->
                type.isInstance(next) ? mapperActual.apply(type.cast(next)) : null;

        Function<Object, Stream<?>> directChildGetter =
                optionsSet.contains(TraversalOption.SameDisplay) ?
                        GuiSwingFeature::directChildren :
                        GuiSwingFeature::directChildrenSameDisplay;

        if (optionsSet.contains(TraversalOption.VisitAfter)) {
            return traverseFindFirstVisitAfter(obj, enteringPredicate.next(obj), conv, directChildGetter, new ArrayList<>());
        } else {
            return traverseFindFirst(obj, enteringPredicate.next(obj), conv, directChildGetter, new ArrayList<>());
        }
    }

    private static <RetType> RetType traverseFindFirst(Object obj, TraversalPredicate entering, Function<Object, RetType> mapper,  Function<Object, Stream<?>> directChildGetter, List<Object> stack) {
        if (stack.contains(obj)) {
            return null;
        }
        stack.add(obj);
        try {
            var ret = mapper.apply(obj);
            if (ret != null) {
                return ret;
            } else {
                for (var iter = directChildGetter.apply(obj).iterator(); iter.hasNext(); ) {
                    var next = iter.next();
                    if (entering.test(next)) {
                        ret = traverseFindFirst(next, entering.next(next), mapper, directChildGetter, stack);
                        if (ret != null) return ret;
                    }
                }
                return null;
            }
        } finally {
            stack.removeLast();
        }
    }

    private static <RetType> RetType traverseFindFirstVisitAfter(Object obj, TraversalPredicate entering, Function<Object, RetType> mapper,  Function<Object, Stream<?>> directChildGetter, List<Object> stack) {
        if (stack.contains(obj)) {
            return null;
        }
        stack.addLast(obj);
        try {
            for (var iter = directChildGetter.apply(obj).iterator(); iter.hasNext(); ) {
                var next = iter.next();
                if (entering.test(next)) {
                    var ret = traverseFindFirstVisitAfter(next, entering.next(next), mapper, directChildGetter, stack);
                    if (ret != null) return ret;
                }
            }
            return mapper.apply(obj);
        } finally {
            stack.removeLast();
        }
    }

    ////////////////////////////////////////

    static boolean walk(Object obj, Visitor visitor) {
        boolean cont = true;
        boolean added = false;
        try {
            boolean enter = visitor.visitBefore(obj);
            if (visitor.stack.contains(obj)) {
                return true;
            }
            visitor.stack.addLast(obj);
            added = true;
            if (enter) {
                for (var iter = directChildren(obj).iterator(); iter.hasNext(); ) {
                    var next = iter.next();
                    if (!walk(next, visitor)) {
                        cont = false;
                        break;
                    }
                }
            }
        } finally {
            if (added) {
                visitor.stack.removeLast();
            }
            if (cont) {
                cont = visitor.visitAfter(obj);
            }
        }
        return cont;
    }

    abstract class Visitor {
        public List<Object> stack = new ArrayList<>();
        boolean visitBefore(Object next) {
            return true;
        }
        boolean visitAfter(Object next) {
            return true;
        }
    }

    ////////////////////////////////////////

    static <RetType> RetType findFirstChildIf(Object obj, Class<RetType> type, Predicate<RetType> predicate, TraversalOption... options) {
        return findFirstChild(obj, type,
                next -> predicate.test(next) ? next : null, options);
    }

    static <TargetType, RetType> RetType findFirstChild(Object obj, Class<TargetType> type, Function<TargetType, RetType> mapper, TraversalOption... options) {
        var optionsSet = Set.of(options);

        Function<TargetType, RetType> mapperActual =
                optionsSet.contains(TraversalOption.NonTop) ?
                        (next -> (next != obj) ? mapper.apply(next) : null) :
                        mapper;

        TraversalPredicate enteringPredicate =
                optionsSet.contains(TraversalOption.OncePertContext) ?
                        TraversalPredicateOnceContext.get() :
                        TraversalPredicateOnce.get();

        Function<Object, RetType> conv = next ->
                type.isInstance(next) ? mapperActual.apply(type.cast(next)) : null;

        Function<Object, Stream<?>> directChildGetter =
                optionsSet.contains(TraversalOption.SameDisplay) ?
                        GuiSwingFeature::directChildren :
                        GuiSwingFeature::directChildrenSameDisplay;

        if (optionsSet.contains(TraversalOption.VisitAfter)) {
            return traverseFindFirstVisitAfter(obj, enteringPredicate.next(obj), conv, directChildGetter, new ArrayList<>());
        } else {
            return traverseFindFirst(obj, enteringPredicate.next(obj), conv, directChildGetter, new ArrayList<>());
        }
    }

    class TraversalPredicateOnce implements TraversalPredicate {
        protected int depth;

        public static TraversalPredicateOnce get() {
           return new  TraversalPredicateOnce(0);
        }

        public TraversalPredicateOnce(int depth) {
            this.depth = depth;
        }

        @Override
        public TraversalPredicate next(Object next) {
            return new TraversalPredicateOnce(depth + 1);
        }

        @Override
        public boolean test(Object obj) {
            return depth < 2;
        }
    }

    class TraversalPredicateOnceContext implements TraversalPredicate {
        protected GuiMappingContext context;
        protected int depth;

        public static TraversalPredicateOnceContext get() {
            return new TraversalPredicateOnceContext(null, 0);
        }

        public TraversalPredicateOnceContext(GuiMappingContext context, int depth) {
            this.context = context;
            this.depth = depth;
        }

        @Override
        public TraversalPredicate next(Object next) {
            if (next instanceof GuiSwingFeature feature) {
                return new TraversalPredicateOnceContext(feature.getSwingViewContext(), depth + 1);
            } else {
                return this;
            }
        }

        @Override
        public boolean test(Object o) {
            return !(o instanceof GuiSwingFeature) ||
                    (o instanceof GuiSwingFeature feature && (depth < 2 || Objects.equals(context, feature.getSwingViewContext())));
        }
    }

    ////////////////////////////////////////

    /**
     * traverse descendant components and call {@link GuiSwingFeature#saveSwingPreferences(GuiPreferences)} for each component.
     * the root pane calls the method.
     * @param prefs the preferences for the comp
     * @param comp the top component
     */
    static void saveChildren(GuiPreferences prefs, Object comp) {
        traverse(comp, GuiSwingFeature.class, e -> {
            var subPrefs = prefs.getDescendant(e.getSwingViewContext());
            try {
                e.saveSwingPreferences(subPrefs);
            } catch (IllegalArgumentException iaex) {
                if (iaex.getMessage().contains("Value too long")) {
                    GuiLogManager.get().logFormat("saveChildren(%s): Value too long", subPrefs.getName());
                } else {
                    GuiLogManager.get().logError(iaex);
                }
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }, TraversalOption.NonTop);
    }
    /**
     * {@link #loadChildren(GuiPreferences, Object, GuiSwingPrefsApplyOptions)}
     *  with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}
     * @param prefs a top prefs
     * @param comp a top component
     */
    static void loadChildren(GuiPreferences prefs, Object comp) {
        loadChildren(prefs, comp, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * traverses descendant components and call {@link GuiSwingView.ValuePane#loadSwingPreferences(GuiPreferences, GuiSwingPrefsApplyOptions)}
     * @param prefs a top prefs
     * @param comp  a top component
     * @param options  options for applying
     * @since 1.4
     */
    static void loadChildren(GuiPreferences prefs, Object comp, GuiSwingPrefsApplyOptions options) {
        traverse(comp, GuiSwingFeature.class, c -> {
            try {
                c.loadSwingPreferences(prefs.getDescendant(c.getSwingViewContext()), options);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }, TraversalOption.NonTop);
    }

    /**
     * @param context the searched context, a descendant child of the context of the pane
     * @return a descendant value pane holding the context, or null.
     *     wrappers holding the same context are avoided.
     */
    static <RetType extends GuiSwingFeature> RetType getDescendantByContext(Object obj, Class<RetType> type, GuiMappingContext context) {
        return traverseFindFirstIf(obj, type,
                next -> Objects.equals(context, next.getSwingViewContext()));
    }

    /**
     * @param name the searched context name
     * @return a child (or descendant for wrappers) value pane holding the named context, or null.
     *    wrappers holding the same context are avoided.
     */
    static <RetType extends GuiSwingFeature> RetType getChildByName(Object obj, Class<RetType> type, String name) {
        return findFirstChildIf(obj, type,
                next -> next.getSwingViewContext() != null && Objects.equals(next.getSwingViewContext().getName(), name), TraversalOption.NonTop, TraversalOption.OncePertContext);
    }
}
