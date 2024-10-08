package org.autogui.base.mapping;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.type.*;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * a tree node describing a mapping between {@link GuiTypeElement} and {@link GuiRepresentation}.
 *  The tree constructs an abstract GUI representation of an object tree (like HTML DOM tree).
 *
 *  <p>
 *      First, an instance of this class is created with a {@link GuiTypeElement} tree of a target object:
 *       {@link #GuiMappingContext(GuiTypeElement)}.
 *        The type-element tree can be obtained by GuiTypeBuilder.
 *  <p>
 *      Second, {@link GuiReprSet} receives the unmapped context object, and constructs subtrees.
 *       The subtrees are created by using {@link #createChildCandidates()} and
 *         matched with {@link GuiReprSet#match(GuiMappingContext)}.
 *          The only matched sub-context becomes an actual child of a context by {@link #addToParent()}.
 *          Also, if matched, the context becomes to have a {@link GuiRepresentation}.
 *       The entry point of construction can be the match method of {@link GuiRepresentation#getDefaultSet()}.
 *
 *  <p>
 *      Third, concrete entities of {@link GuiRepresentation}s, like GuiSwingView, can create concrete GUI components.
 *       Then the components can compute updating of their displays by using retained mapped objects and listeners,
 *         which can be obtained by {@link #getSource()} and {@link #getListeners()}.
 *
 *        <ol>
 *            <li> a GUI component can call {@link #updateSourceFromRoot(GuiMappingContext)}
 *              with passing an associated context.
 *               Note, {@link #updateSourceFromRoot()}) is the no-source version and can be used for obtaining initial values.
 *            <li>
 *              The method obtains the root context by {@link #getRoot()},
 *            <li>
 *              and traverses the tree and collects updated contexts, by {@link #collectUpdatedSource(GuiMappingContext, List)}.
 *            <li>
 *               {@link GuiRepresentation#checkAndUpdateSource(GuiMappingContext)} do the actual computation of checking the update.
 *            <li>
 *                After that, {@link SourceUpdateListener#update(GuiMappingContext, Object, GuiTaskClock)} of each updated context will be called.
 *        </ol>
 *
 *    <p>
 *        getType...(), isType...() check and obtain {@link #typeElement} as a specified type.
 *        isParent...() check the type of the parent.
 */
public class GuiMappingContext {
    protected GuiTypeElement typeElement;
    protected GuiRepresentation representation;

    protected GuiMappingContext parent;
    protected List<GuiMappingContext> children;

    protected GuiSourceValue source;
    protected List<SourceUpdateListener> listeners = Collections.emptyList();

    protected ContextExecutorService taskRunner;
    protected GuiPreferences preferences;
    protected ScheduledTaskRunner<DelayedTask> delayedTaskRunner;

    protected String displayName;
    protected String iconName;

    protected GuiTaskClock contextClock = new GuiTaskClock( false);

    /**
     * source value held by a context. it can distinguish null and nothing.
     *  To distinguish other values, it is defined as an independent class, instead of using Optional
     */
    public static class GuiSourceValue {
        public boolean isNone() {
            return false;
        }

        public GuiSourceValue() {}

        /**
         * @return null if none
         */
        public Object getValue() {
            return null;
        }

        public static GuiSourceValueObject of(Object v) {
            return new GuiSourceValueObject(v);
        }
    }

    /**
     * concrete source value
     */
    public static final class GuiSourceValueObject extends GuiSourceValue {
        Object value;

        public GuiSourceValueObject(Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Source(" + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GuiSourceValueObject that = (GuiSourceValueObject) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

    }

    public static final GuiSourceValueNone NO_SOURCE = new GuiSourceValueNone();

    /**
     * nothing of source value, can be obtained by {@link #NO_SOURCE}
     */
    public static final class GuiSourceValueNone extends GuiSourceValue {
        public GuiSourceValueNone() {}

        @Override
        public boolean isNone() {
            return true;
        }

        @Override
        public String toString() {
            return "NO_SOURCE";
        }
    }

    /**
     * a task for update propagation.
     *  frequently created tasks are aggregated for each task-type {@link #getTaskType()}.
     *  and sorted by each clock {@link #getTaskType()} and
     *   last one is executed {@link #run(List)} with taking accumulated tasks of same task-type.
     */
    public static class DelayedTask {
        protected GuiTaskClock taskClock;
        protected Object taskType;
        protected Consumer<List<DelayedTask>> task;

        public DelayedTask(GuiTaskClock taskClock, Object taskType, Consumer<List<DelayedTask>> task) {
            this.taskClock = taskClock;
            this.taskType = taskType;
            this.task = task;
        }

        public GuiTaskClock getTaskClock() {
            return taskClock;
        }

        public Object getTaskType() {
            return taskType;
        }

        public void run(List<DelayedTask> accumulatedTasks) {
            task.accept(accumulatedTasks);
        }
    }

    ////////////////////

    public GuiMappingContext(GuiTypeElement typeElement) {
        this(typeElement, null, null, NO_SOURCE);
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiMappingContext parent) {
        this(typeElement, null, parent, NO_SOURCE);
    }

    public GuiMappingContext(GuiTypeElement typeElement, Object source) {
        this(typeElement, null, null, GuiSourceValue.of(source));
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation, Object source) {
        this(typeElement, representation, null, GuiSourceValue.of(source));
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation) {
        this(typeElement, representation, null, NO_SOURCE);
    }

    @SuppressWarnings("this-escape")
    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation, GuiMappingContext parent, GuiSourceValue source) {
        this.typeElement = typeElement;
        this.representation = representation;
        this.parent = parent;
        setSource(source);
    }

    public GuiTaskClock getContextClock() {
        return contextClock;
    }

    public void setRepresentation(GuiRepresentation representation) {
        this.representation = representation;
    }

    public GuiTypeElement getTypeElement() {
        return typeElement;
    }

    public GuiRepresentation getRepresentation() {
        return representation;
    }

    public List<GuiMappingContext> getChildren() {
        return Objects.requireNonNullElse(children, Collections.emptyList());
    }

    public GuiMappingContext getParent() {
        return parent;
    }

    public List<GuiMappingContext> createChildCandidates() {
        return typeElement.getChildren().stream()
                .map(this::createChildCandidate)
                .collect(Collectors.toList());
    }

    public List<GuiMappingContext> getChildrenForAdding() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public GuiMappingContext createChildCandidate(GuiTypeElement typeElement) {
        return new GuiMappingContext(typeElement, this);
    }

    public void addToParent() {
        if (hasParent()) {
            getParent().getChildrenForAdding().add(this);
        }
    }

    public boolean isRecursive() {
        GuiTypeElement s = getTypeElement();
        GuiMappingContext ctx = getParent();
        while (ctx != null) {
            if (ctx.getTypeElement().equals(s) &&
                    !ctx.isReprCollectionElement()) { // (Obj,ObjectPane) { (List,CollectionTable) { (Obj,CollectionElement()) } }
                return true;
            }
            ctx = ctx.getParent();
        }
        return false;
    }

    public GuiMappingContext getRoot() {
        GuiMappingContext ctx = this;
        while (ctx.hasParent()) {
            ctx = ctx.getParent();
        }
        return ctx;
    }

    public boolean hasParent() {
        return parent != null;
    }

    /** @return the name of {@link #typeElement} */
    public String getName() {
        return typeElement.getName();
    }

    public String getDescription() {
        return typeElement.getDescription();
    }

    public String getAcceleratorKeyStroke() {
        String s = typeElement.getAcceleratorKeyStroke();
        if (s.isEmpty()) {
            String name = getName();
            if (!name.isEmpty()) {
                char c = name.charAt(0);
                return Character.toString(Character.toUpperCase(c));
            }
            return "";
        } else {
            return s;
        }
    }

    public boolean isAcceleratorKeyStrokeSpecified() {
        return !typeElement.getAcceleratorKeyStroke().isEmpty();
    }

    ///////////////////////

    public boolean isTypeElementProperty() {
        return typeElement instanceof GuiTypeMemberProperty;
    }

    public Class<?> getTypeElementPropertyTypeAsClass() {
        GuiTypeElement e = ((GuiTypeMemberProperty) typeElement).getType();
        if (e instanceof GuiTypeValue) {
            return ((GuiTypeValue) e).getType();
        } else {
            return Object.class;
        }
    }

    public boolean isTypeElementAction() {
        return typeElement instanceof GuiTypeMemberAction && !isTypeElementActionList();
    }

    public boolean isTypeElementActionList() {
        return typeElement instanceof GuiTypeMemberActionList;
    }

    public boolean isTypeElementObject() {
        return typeElement instanceof GuiTypeObject;
    }

    /**
     * @return includes {@link GuiTypeObject} and {@link GuiTypeCollection}
     *    which are subtypes of {@link GuiTypeValue}
     */
    public boolean isTypeElementValue() {
        return typeElement instanceof GuiTypeValue;
    }

    public boolean isTypeElementCollection() {
        return typeElement instanceof GuiTypeCollection;
    }

    public Class<?> getTypeElementValueAsClass() {
        return ((GuiTypeValue) typeElement).getType();
    }

    public GuiTypeMemberProperty getTypeElementAsProperty() {
        return (GuiTypeMemberProperty) typeElement;
    }

    public GuiTypeMemberAction getTypeElementAsAction() {
        return (GuiTypeMemberAction) typeElement;
    }

    public GuiTypeMemberActionList getTypeElementAsActionList() {
        return (GuiTypeMemberActionList) typeElement;
    }

    public GuiTypeCollection getTypeElementCollection() {
        return (GuiTypeCollection) typeElement;
    }

    public GuiTypeValue getTypeElementValue() {
        return (GuiTypeValue) typeElement;
    }

    /**
     * @return typeElement as type-object
     * @since 1.2
     */
    public GuiTypeObject getTypeElementObject() {
        return (GuiTypeObject) typeElement;
    }

    ///////////////////////

    /** only set the source value
     * @param source  the source */
    public void setSource(GuiSourceValue source) {
        boolean eq = Objects.equals(this.source, source);
        this.source = source;
        if (!eq && !source.isNone() && getRepresentation() != null) { //only updates the notifiers if the representation is set, which means there are sub-contexts
            setNotifiers(source.getValue());
        }
    }

    /**
     * @return actual value of the context */
    public GuiSourceValue getSource() {
        return source;
    }

    /** called from {@link #updateSourceFromRoot(GuiMappingContext)} and {@link #updateSourceSubTree()}.
     *  used for updating GUI components, setSwingViewValue(v) */
    public interface SourceUpdateListener {
        void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock);
    }

    public void addSourceUpdateListener(SourceUpdateListener listener) {
        if (listeners.isEmpty()) {
            listeners = Collections.singletonList(listener);
        } else if (listeners.size() == 1) {
            ArrayList<SourceUpdateListener> ls = new ArrayList<>(3);
            ls.addAll(listeners);
            ls.add(listener);
            listeners = ls;
        } else {
            listeners.add(listener);
        }
    }

    public void removeSourceUpdateListener(SourceUpdateListener listener) {
        int i = listeners.indexOf(listener);
        if (i != -1) {
            if (listeners.size() == 1) {
                listeners = Collections.emptyList();
            } else {
                listeners.remove(i);
            }
        }
    }

    public List<SourceUpdateListener> getListeners() {
        return listeners;
    }

    /** set the source to newValue, call {@link #updateSourceFromRoot(GuiMappingContext)} starting with this.
     * Note: the method does not update any properties of parent or other source objects
     * The clock is already set.
     * @param newValue the new source value to be set
     */
    public void updateSourceFromGui(Object newValue) {
        setSource(GuiSourceValue.of(newValue));
        updateSourceFromGuiByThisDelayed();
    }

    public void updateSourceFromGuiByThisDelayed() {
        getDelayedTaskRunner().schedule(
                new DelayedTask(getContextClock().copy(), // the clock is used only for sorting the accumulated tasks
                        "updateSourceFromRoot",
                        tasks -> updateSourceFromRoot(this)));
    }

    /** {@link #updateSourceFromRoot(GuiMappingContext)} with the null cause */
    public void updateSourceFromRoot() {
        updateSourceFromRoot(null);
    }

    /** obtains the root context, recursively collect updated sub-contexts from the root,
     *    and call listeners with each updated context.
     * @param cause the cause of the updating process,
     *               and the process avoids to update the context.
     */
    public void updateSourceFromRoot(GuiMappingContext cause) {
        GuiMappingContext ctx = getRoot();
        List<GuiMappingContext> updated = new ArrayList<>();
        ctx.collectUpdatedSource(cause, updated);

        updated.forEach(c -> c.sendUpdateToListeners(this));
    }

    public void sendUpdateToListeners(GuiMappingContext cause) {
        GuiTaskClock c = getContextClock().copy(); //the sending value is obtained here, thus its clock is the current instant.
        getListeners().forEach(l ->
                l.update(cause, getSource().getValue(), c));
    }

    public void clearSourceSubTree() {
        if (hasParent()) {
            getContextClock().increment();
            setSource(NO_SOURCE);
        }
        getChildren()
                .forEach(GuiMappingContext::clearSourceSubTree);
    }

    /** recursively collect updated sub-contexts from this and call listeners*/
    public void updateSourceSubTree() {
        List<GuiMappingContext> updated = new ArrayList<>();
        collectUpdatedSource(null, updated);

        updated.forEach(c -> c.sendUpdateToListeners(this));
    }

    /** recursively call {@link GuiRepresentation#checkAndUpdateSource(GuiMappingContext)}:
     *   the checkAndUpdateSource invokes the getter of the target source object in order to update the value.
     *   <p>
     *   the recursion can be controlled by {@link GuiRepresentation#continueCheckAndUpdateSourceForChildren(GuiMappingContext, boolean)}:
     *     this is used for avoiding recursion of list elements.
     *   <p>
     *    the method alone does not cause GUI updates because of no listener calls
     *
     *    @param cause the cause context
     *    @param updated the list which the updated contexts will be added
     *   */
    public void collectUpdatedSource(GuiMappingContext cause, List<GuiMappingContext> updated) {
        boolean thisUpdated = false;
        if (this != cause) {
            if (getRepresentation().checkAndUpdateSource(this)) {
                updated.add(this);
                thisUpdated = true;
            }
        }
        if (getRepresentation().continueCheckAndUpdateSourceForChildren(this, thisUpdated)) {
            getChildren()
                    .forEach(c -> c.collectUpdatedSource(cause, updated));
        }
    }

    /**
     * @return  space separated words from the camel-case context name
     *  e.g. "myPropName" -&gt; "My Prop Name"
     */
    public String getDisplayName() {
        if (displayName == null) {
            displayName = nameJoinForDisplay(nameSplit(getName(), true));
        }
        return displayName;
    }

    /** @return the top word of the split name, which is an action verb in most cases:
     *     e.g. "getProp" -&gt; "get"
     */
    public String getIconName() {
        if (iconName == null) {
            iconName = nameSplit(getName(), true)
                    .getFirst().toLowerCase();
        }
        return iconName;
    }

    public String nameJoinForDisplay(List<String> words) {
        return words.stream()
            .map(s -> s.isEmpty() ? s :
                    (Character.toUpperCase(s.charAt(0)) + s.substring(1)))
            .collect(Collectors.joining(" "));
    }

    /**
     * split a name to a list of words by camel case.
     * @param name the split name
     * @param forDisplay if true,
     * <pre>
     *  "helloWorld" =&gt; ["hello", "World"]
     *  "HelloWorld" =&gt; ["Hello", "World"]
     *  "helloWORLD" =&gt; ["hello", "WORLD"]
     *  "HELLOWorld" =&gt; ["HELLO", "World"]
     *  "" =&gt; [""]
     * </pre>
     * @return the split list, never null
     * If <code>forDisplay=false</code>, "MYName" =&gt; ["M", "Y", "Name"]
     */
    public static List<String> nameSplit(String name, boolean forDisplay) {
        List<String> words = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (int i = 0, len = name.length(); i < len; ++i) {
            char c = name.charAt(i);
            if (!buf.isEmpty() &&
                    (isNameSeparator(c) ||
                     isNameCharUpper(c, i, len, name, forDisplay))) {
                words.add(buf.toString());
                buf = new StringBuilder();
            }
            if (!isNameSeparator(c)) {
                buf.append(c);
            }
        }
        if (!buf.isEmpty() || words.isEmpty()) {
            words.add(buf.toString());
        }
        return words;
    }

    public static boolean isNameCharUpper(char c, int i, int len, String name, boolean forDisplay) {
        boolean upperOrDigit = Character.isUpperCase(c) || Character.isDigit(c);
        if (forDisplay) {
            return (upperOrDigit &&
                    ((i > 0 && Character.isLowerCase(name.charAt(i - 1))) ||  // a[A]
                     (i + 1 < len && Character.isLowerCase(name.charAt(i + 1))))); //[A]a
        } else {
            return upperOrDigit;
        }
    }

    public static boolean isNameSeparator(char c) {
        return Character.isWhitespace(c) || c == '$' || c =='_' || !Character.isJavaIdentifierPart(c);
    }


    /** notify to {@link GuiLogManager}
     * @param error the reported error
     * */
    public void errorWhileUpdateSource(Throwable error) {
        GuiLogManager.get().logError(error);
    }

    public void errorWhileJson(Throwable error) {
        GuiLogManager.get().logError(error);
    }

    public boolean isParentPropertyPane() {
        return getParentRepresentation() instanceof GuiReprPropertyPane;
    }

    public GuiRepresentation getParentRepresentation() {
        return hasParent() ? getParent().getRepresentation() : null;
    }

    public GuiReprPropertyPane getParentPropertyPane() {
        return (GuiReprPropertyPane) getParentRepresentation();
    }

    public boolean isParentValuePane() {
        return hasParent() && getParent().isReprValue();
    }

    public boolean isReprValue() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprValue;
    }

    public GuiReprValue getReprValue() {
        return (GuiReprValue) getRepresentation();
    }

    public GuiReprValue getParentValuePane() {
        return hasParent() ? getParent().getReprValue() : null;
    }

    public GuiReprCollectionTable getParentCollectionTable() {
        return isParentCollectionTable() ? getParent().getReprCollectionTable() : null;
    }

    public GuiReprCollectionElement getParentCollectionElement() {
        return isParentCollectionElement() ? getParent().getReprCollectionElement() : null;
    }

    public GuiSourceValue getParentSource() {
        return hasParent() ? getParent().getSource() : GuiMappingContext.NO_SOURCE;
    }

    public boolean isReprAction() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprAction;
    }

    public boolean isReprActionList() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprActionList;
    }

    public GuiReprAction getReprAction() {
        return (GuiReprAction) getRepresentation();
    }

    public GuiReprActionList getReprActionList() {
        return (GuiReprActionList) getRepresentation();
    }

    public boolean isParentCollectionTable() {
        return hasParent() && getParent().isReprCollectionTable();
    }

    public boolean isReprCollectionTable() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprCollectionTable;
    }

    public GuiReprCollectionTable getReprCollectionTable() {
        return (GuiReprCollectionTable) getRepresentation();
    }

    public boolean isParentCollectionElement() {
        return hasParent() && getParent().isReprCollectionElement();
    }

    public boolean isReprCollectionElement() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprCollectionElement;
    }

    public GuiReprCollectionElement getReprCollectionElement() {
        return (GuiReprCollectionElement) getRepresentation();
    }

    public boolean isHistoryValueSupported() {
        return isReprValue()
                && getReprValue().isHistoryValueSupported(this);
    }

    public boolean isHistoryValueStored(Object value) {
        return isReprValue()
                && getReprValue().isHistoryValueStored(this, value);
    }

    //////////////////////

    /**
     * flag for changing the type of {@link #taskRunner}:
     *  the default is false and then {@link ContextExecutorServiceForkJoin} is used.
     *  If the flag is set to true before the first call of {@link #getTaskRunner()},
     *    then {@link ContextExecutorServiceNoThread} is used.
     * @since 1.2
     */
    public static boolean taskRunnerSingleThread = false;

    /** @return taskRunner taken from parent context or single thread executor in the root.
     * @see #taskRunnerSingleThread
     *   */
    public ContextExecutorService getTaskRunner() {
        if (taskRunner == null) {
            GuiMappingContext parent = getParent();
            if (parent != null) {
                taskRunner = parent.getTaskRunner();
            } else {if (taskRunnerSingleThread) {
                    taskRunner = new ContextExecutorServiceNoThread();
                } else {
                    taskRunner = new ContextExecutorServiceForkJoin();
                }
            }
        }
        return taskRunner;
    }

    /**
     * call {@link #shutdownTaskRunner()} and
     *  also close the target if the target is an {@link AutoCloseable}
     */
    public void shutdown() {
        shutdownAutoCloseable();
        shutdownTaskRunner();
    }

    public void shutdownAutoCloseable() {
        GuiMappingContext root = getRoot();
        root.shutdownAutoCloseableSubTree();
    }

    protected void shutdownAutoCloseableSubTree() {
        getRepresentation().shutdown(this, getSource().getValue());
        for (GuiMappingContext child : getChildren()) {
            child.shutdownAutoCloseableSubTree();
        }
    }

    public void shutdownTaskRunner() {
        GuiMappingContext root = getRoot();
        root.shutdownTaskRunnerSubTree();
    }

    protected void shutdownTaskRunnerSubTree() {
        if (taskRunner != null) {
            taskRunner.shutdown();
            taskRunner = null;
        }
        if (delayedTaskRunner != null) {
            delayedTaskRunner.shutdown();
            delayedTaskRunner = null;
        }
        for (GuiMappingContext child : getChildren()) {
            child.shutdownTaskRunnerSubTree();
        }
    }

    /** run the task. used for wrapping tasks that execute type-element's actions
     * @param task the task to be executed
     * @param <T> the returned type
     * @return the returned value of the task
     * @throws Throwable an exception from the task
     * */
    public <T> T execute(Callable<T> task) throws Throwable {
        return ScheduledTaskRunner.withDepthInfo("GMC(" + getName() + ")", task);
    }

    /**
     * task runner for deferring frequent events and dispatching with accumulated events
     * @return the runner created as a root or obtained from the parent
     */
    public ScheduledTaskRunner<DelayedTask> getDelayedTaskRunner() {
        if (delayedTaskRunner == null) {
            GuiMappingContext parent = getParent();
            if (parent != null) {
                delayedTaskRunner = parent.getDelayedTaskRunner();
            } else {
                delayedTaskRunner = new ScheduledTaskRunner<>(200, this::executeAccumulated);
            }
        }
        return delayedTaskRunner;
    }

    public void executeAccumulated(List<DelayedTask> list) {
        Map<Object, List<DelayedTask>> tasks = new HashMap<>();
        list.forEach(e ->
            tasks.computeIfAbsent(e.getTaskType(), k -> new ArrayList<>())
                .add(e));
        tasks.forEach((k, v) -> {
            v.sort(Comparator.comparing(DelayedTask::getTaskClock));
            v.getLast().run(v);
        });
    }

    /** @return obtains from the parent.
     *     a preference object occasionally cleared from the parent context.
     *     so it does not recommend to hold the returned instance. */
    public GuiPreferences getPreferences() {
        if (preferences == null) {
            if (hasParent()) {
                preferences = getParent().getPreferences().getChild(this);
            } else {
                preferences = new GuiPreferences(this);
            }
        }
        return preferences;
    }

    public void clearPreferences() {
        if (!hasParent() && preferences != null) { //root :
            this.preferences = preferences.copyInitAsRoot();
        } else {
            this.preferences = null;
        }
    }

    public void setPreferences(GuiPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + "(" + getName() + "," + getTypeElement() + "," + getRepresentation() + "," + getSource() + ")"
                + (!hasParent() ? "!" : "") + "[" + getChildren().size() + "]";
    }

    ////////////////////////

    /**
     * @param name the name
     * @return a child whose name is the name
     */
    public GuiMappingContext getChildByName(String name) {
        return getChildren().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found: " + name));
    }

    /**
     * @param specifier the specifier for the value
     * @return get current value of the associated property
     *    obtaining through {@link GuiReprValue} (can be checked by {@link #isReprValue()}) from current value of the parent.
     */
    public Object getValue(GuiReprValue.ObjectSpecifier specifier) {
        if (isReprValue()) {
            try {
                return getReprValue().getUpdatedValueWithoutNoUpdate(this, specifier);
            } catch (Throwable ex) {
                throw new RuntimeException("" + getRepresentation(), ex);
            }
        } else {
            throw new UnsupportedOperationException("" + getRepresentation());
        }
    }

    /**
     * execute the associated action through {@link GuiReprAction} (can be checked by {@link #isReprAction()})
     *   otherwise throw an exception.
     *   the method causes same effects by GUI operation,
     *     i.e. updating other components.
     * @param targetSpecifier the specifier for the target object
     * @return result of execution
     */
    public Object executeAction(GuiReprValue.ObjectSpecifier targetSpecifier) {
        if (isReprAction()) {
            return getReprAction().executeAction(this, targetSpecifier);
        } else {
            throw new UnsupportedOperationException("" + getRepresentation());
        }
    }

    ////////////////////////

    /**
     * set appropriate notifiers to the target if defined
     * @param target the target object obtained from the source of the context. nullable
     * @since 1.2
     */
    public void setNotifiers(Object target) {
        if (target != null && isTypeElementObject()) {
            GuiTypeObject obj = getTypeElementObject();
            obj.getNotifiers()
                    .forEach(n -> setNotifier(target, n));
        }
    }

    /**
     * set notifier to the target member of the target object, determined from the parameter of the annotation
     * @param target the target object obtained from the source of the context
     * @param notifier the notifier property
     * @since 1.2
     */
    public void setNotifier(Object target, GuiTypeMemberPropertyNotifier notifier) {
        try {
            if (notifier.isTargetSelf()) {
                Runnable r = getNotifierForTarget(false);
                execute(() -> notifier.executeSet(target, r));
            } else if (notifier.isTargetRoot()) {
                Runnable r = getNotifierForTarget(true);
                execute(() -> notifier.executeSet(target, r));
            } else {
                GuiMappingContext child = getChildByName(notifier.getTargetName());
                if (child != null) {
                    Runnable r = child.getNotifierForTarget(false);
                    execute(() -> notifier.executeSet(target, r));
                }
            }
        } catch (Throwable ex) {
            errorWhileUpdateSource(ex);
        }
    }

    /**
     * @param fromRoot true if the entire pane of the context
     * @return a {@link ContextNotifier}
     * @since 1.2
     */
    public Runnable getNotifierForTarget(boolean fromRoot) {
        return new ContextNotifier(this, fromRoot);
    }

    /**
     * a runnable callback passed to the target object.
     *  the run method of the class clears and updates the associated context
     * @since 1.2
     */
    public static class ContextNotifier implements Runnable {
        protected GuiMappingContext context;
        protected boolean root;

        public ContextNotifier(GuiMappingContext context, boolean root) {
            this.context = context;
            this.root = root;
        }

        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void run() {
            ScheduledTaskRunner.withDepthInfo("notify(" + context + ")", () ->
                context.getTaskRunner().execute(this::runBody));
        }

        public void runBody() {
            if (root) {
                context.getRoot().clearSourceSubTree();
                context.updateSourceFromRoot();
            } else {
                context.clearSourceSubTree();
                context.updateSourceSubTree();
            }
        }
    }

    /**
     * a sub-set of {@link ExecutorService} as the returned type of {@link #getTaskRunner()}
     * @since 1.2
     */
    public interface ContextExecutorService {
        /**
         * submit a task v
         * @param v submitted task
         * @param <V> the returned type of the task
         * @return future of the task
         * @see ExecutorService#submit(Callable)
         */
        <V> Future<V> submit(Callable<V> v);

        /**
         * submit a task guaranteed running; non-threading executors can immediately run it
         * @param task a task
         * @since 1.6
         */
        default void execute(Runnable task) {
            var h = Thread.currentThread().getUncaughtExceptionHandler();
            submit(() -> {
                try {
                    task.run();
                } catch (Throwable ex) {
                    if (h != null) {
                        h.uncaughtException(Thread.currentThread(), ex);
                    } else {
                        ex.printStackTrace();
                    }
                }
                return (Void) null;
            });
        }

        /**
         * shut down the service
         * @see ExecutorService#shutdown()
         */
        void shutdown();
    }

    /**
     * executor-service without creating threads for tasks
     * @since 1.6
     */
    public static class ContextExecutorServiceNoThread implements ContextExecutorService {
        protected ExecutorService timeoutService = Executors.newCachedThreadPool();

        public ContextExecutorServiceNoThread() {}

        @Override
        public <V> Future<V> submit(Callable<V> v) {
            return new FutureImmediate<>(v, timeoutService);
        }

        @Override
        public void execute(Runnable task) {
            task.run();
        }

        @Override
        public void shutdown() {
            timeoutService.shutdown();
        }
    }

    /**
     * the task future without thread
     * @param <V> the task value
     * @since 1.6
     */
    public static class FutureImmediate<V> implements Future<V> {
        protected Callable<V> task;
        protected V result;
        protected AtomicBoolean finish = new AtomicBoolean(false);
        protected AtomicBoolean cancelled = new AtomicBoolean(false);
        protected ExecutorService timeoutService;

        public FutureImmediate(Callable<V> task, ExecutorService timeoutService) {
            this.task = task;
            this.timeoutService = timeoutService;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean success = finish.compareAndSet(false, true);
            if (success) {
                cancelled.set(true);
            }
            return success;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return finish.get();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            if (!finish.get()) {
                try {
                    result = task.call();
                } catch (InterruptedException ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new ExecutionException(ex);
                } finally {
                    finish.set(true);
                }
            }
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            Future<Boolean> timeoutTask = null;
            Thread runnerThread = Thread.currentThread();
            if (timeoutService != null) {
                timeoutTask = timeoutService.submit(() -> {
                    try {
                        unit.sleep(timeout);
                        if (!finish.get() && !cancelled.get()) {
                            runnerThread.interrupt();
                            return false;
                        }
                    } catch (InterruptedException e) {
                        //none
                    }
                    return true;
                });
            }
            try {
                return get();
            } finally {
                if (timeoutTask != null) {
                    timeoutTask.cancel(true);
                }
            }
        }
    }

    /**
     * a simple executor-service wrapping single-thread executor
     *  by {@link Executors#newSingleThreadScheduledExecutor(ThreadFactory)}
     * @deprecated
     * @since 1.2
     */
    @Deprecated
    public static class ContextExecutorServiceSingleThread implements ContextExecutorService {
        protected ExecutorService service;

        public ContextExecutorServiceSingleThread(ExecutorService service) {
            this.service = service;
        }

        public ContextExecutorServiceSingleThread() {
            this(Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = defaultFactory.newThread(r);
                    th.setDaemon(true);
                    th.setName(GuiMappingContext.class.getSimpleName() + "-" + th.getName());
                    return th;
                }
            }));
        }

        @Override
        public <V> Future<V> submit(Callable<V> v) {
            return service.submit(ScheduledTaskRunner.depthRunner(v));
        }

        @Override
        public void shutdown() {
            service.shutdown();
        }
    }

    /**
     * a {@link ForkJoinPool} based executor:
     *   the fork-join pool have ability of work-stealing:
     *      i.e. a running task in the pool can invoke another new task
     *          which immediately runs in the same context to the caller task of the pool
     *           with "stealing" the running of the caller task.
     * @since 1.2
     */
    public static class ContextExecutorServiceForkJoin implements ContextExecutorService {
        protected ForkJoinPool pool;

        public ContextExecutorServiceForkJoin(ForkJoinPool pool) {
            this.pool = pool;
        }

        public ContextExecutorServiceForkJoin() {
            this(new ForkJoinPool(1, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
                final ForkJoinPool.ForkJoinWorkerThreadFactory defaultFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

                @Override
                public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                    ForkJoinWorkerThread th = defaultFactory.newThread(pool);
                    th.setDaemon(true);
                    th.setName(GuiMappingContext.class.getSimpleName() + "-" + th.getName());
                    return th;
                }
            }, null, true));
        }

        @Override
        public <V> Future<V> submit(Callable<V> v) {
            return pool.submit((ForkJoinTask<V>) new ContextExecutorForkJoinTask<>(ScheduledTaskRunner.depthRunner(v)));
        }

        @Override
        public void shutdown() {
            pool.shutdown();
        }
    }

    /**
     * a task wrapper for {@link ContextExecutorServiceForkJoin},
     *   which enables to interrupt tasks by {@link #cancel(boolean)}
     * @param <V> the returning type of the task
     * @since 1.2
     */
    public static class ContextExecutorForkJoinTask<V> extends ForkJoinTask<V>
        implements RunnableFuture<V> {
        @Serial private static final long serialVersionUID = 1L;
        protected Callable<V> task;
        protected V value;
        protected AtomicReference<Thread> runnerThread = new AtomicReference<>();

        public ContextExecutorForkJoinTask(Callable<V> task) {
            this.task = task;
        }

        @Override
        public V getRawResult() {
            return value;
        }

        @Override
        protected void setRawResult(V value) {
            this.value = value;
        }

        @Override
        public void run() {
            invoke();
        }

        @Override
        protected boolean exec() {
            try {
                runnerThread.set(Thread.currentThread());
                value = task.call();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                runnerThread.set(null);
            }
            return true;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Thread t = runnerThread.get();
            if (t != null) {
                t.interrupt();
            }
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + task + ")";
        }
    }
}
