package autogui.base.mapping;

import autogui.base.log.GuiLogManager;
import autogui.base.type.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;
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
 *      Second, {@link GuiReprSet} receives the unmapped context object, and constructs sub-trees.
 *       The sub-trees are created by using {@link #createChildCandidates()} and
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
 *                After that, {@link SourceUpdateListener#update(GuiMappingContext, Object)} of each updated context will be called.
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

    protected Object source;
    protected List<SourceUpdateListener> listeners = Collections.emptyList();

    protected ScheduledExecutorService taskRunner;
    protected GuiPreferences preferences;

    protected String displayName;
    protected String iconName;

    public GuiMappingContext(GuiTypeElement typeElement) {
        this.typeElement = typeElement;
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiMappingContext parent) {
        this.typeElement = typeElement;
        this.parent = parent;
    }

    public GuiMappingContext(GuiTypeElement typeElement, Object source) {
        this.typeElement = typeElement;
        this.source = source;
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation, Object source) {
        this.typeElement = typeElement;
        this.representation = representation;
        this.source = source;
    }

    public GuiMappingContext(GuiTypeElement typeElement, GuiRepresentation representation) {
        this(typeElement, representation, null);
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
        if (children == null) {
            return Collections.emptyList();
        } else {
            return children;
        }
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
            if (ctx.getTypeElement().equals(s)) {
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

    protected static Pattern keyPattern = Pattern.compile("[0-9a-zA-Z]");

    public String getAcceleratorKeyStroke() {
        String s = typeElement.getAcceleratorKeyStroke();
        if (s.isEmpty()) {
            String name = getName();
            if (name.length() > 0) {
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


    ///////////////////////

    /** only set the source value
     * @param source  the source */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * @return actual value of the context */
    public Object getSource() {
        return source;
    }

    /** called from {@link #updateSourceFromRoot(GuiMappingContext)} and {@link #updateSourceSubTree()}.
     *  used for updating GUI components, setSwingViewValue(v) */
    public interface SourceUpdateListener {
        void update(GuiMappingContext cause, Object newValue);
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
     * @param newValue the new source value to be set
     */
    public void updateSourceFromGui(Object newValue) {
        this.source = newValue;
        updateSourceFromRoot(this);
    }

    /** {@link #updateSourceFromGui(Object)} with the null cause */
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
        getListeners().forEach(l ->
                l.update(cause, getSource()));
    }

    public void clearSourceSubTree() {
        if (hasParent()) {
            setSource(null);
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
                    .get(0).toLowerCase();
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
        List<String> words = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        for (int i = 0, len = name.length(); i < len; ++i) {
            char c = name.charAt(i);
            if (buf.length() > 0 &&
                    (isNameSeparator(c) ||
                     isNameCharUpper(c, i, len, name, forDisplay))) {
                words.add(buf.toString());
                buf = new StringBuilder();
            }
            if (!isNameSeparator(c)) {
                buf.append(c);
            }
        }
        if (buf.length() > 0 || words.isEmpty()) {
            words.add(buf.toString());
        }
        return words;
    }

    public static boolean isNameCharUpper(char c, int i, int len, String name, boolean forDisplay) {
        if (forDisplay) {
            return (Character.isUpperCase(c) &&
                    (i > 0 && Character.isLowerCase(name.charAt(i - 1)) ||  // a[A]
                   (i + 1 < len && Character.isLowerCase(name.charAt(i + 1))))); //[.]a
        } else {
            return Character.isUpperCase(c);
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

    public Object getParentSource() {
        return hasParent() ? getParent().getSource() : null;
    }

    public boolean isReprAction() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprAction;
    }

    public GuiReprAction getReprAction() {
        return (GuiReprAction) getRepresentation();
    }

    public boolean isParentCollectionTable() {
        return hasParent() && getParent().isReprCollectionTable();
    }

    public boolean isReprCollectionTable() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprCollectionTable;
    }

    public boolean isParentCollectionElement() {
        return hasParent() && getParent().isReprCollectionElement();
    }

    public boolean isReprCollectionElement() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprCollectionElement;
    }

    public boolean isHistoryValueSupported() {
        return isReprValue()
                && getReprValue().isHistoryValueSupported();
    }

    public boolean isHistoryValueStored() {
        return isReprValue()
                && getReprValue().isHistoryValueStored();
    }

    //////////////////////

    /** @return taskRunner taken from parent context or single thread executor in the root.
     *   */
    public ScheduledExecutorService getTaskRunner() {
        if (taskRunner == null) {
            GuiMappingContext parent = getParent();
            if (parent != null) {
                taskRunner = parent.getTaskRunner();
            } else {
                taskRunner = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread th = defaultFactory.newThread(r);
                        th.setDaemon(true);
                        th.setName(GuiMappingContext.class.getSimpleName() + "-" + th.getName());
                        return th;
                    }
                });
            }
        }
        return taskRunner;
    }

    /** submit the task to the task runner and wait the completion of the task
     * @param task the task submitted to the runner
     * @param <T> the returned type
     * @return the returned value of the task
     * @throws Throwable an exception from the task
     * */
    public <T> T execute(Callable<T> task) throws Throwable {
        try {
            if (getRepresentation() == null || getRepresentation().isTaskRunnerUsedFor(task)) {
                return getTaskRunner()
                        .submit(task)
                        .get();
            } else {
                return task.call();
            }
        } catch (ExecutionException e) {
            throw e.getCause();
        }
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
        this.preferences = null;
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
     * @return get current value of the associated property
     *    obtaining through {@link GuiReprValue} (can be checked by {@link #isReprValue()}) from current value of the parent.
     */
    public Object getValue() {
        if (isReprValue()) {
            try {
                return getReprValue().getUpdatedValueWithoutNoUpdate(this, true);
            } catch (Throwable ex) {
                throw new RuntimeException("" + getRepresentation(), ex);
            }
        } else {
            throw new UnsupportedOperationException("" + getRepresentation());
        }
    }

    /**
     * update the value of the associated property
     *   through {@link GuiReprValue} (can be checked by {@link #isReprValue()}).
     *    the method causes same effects by GUI operation,
     *      i.e. updating history values, updating other components ...
     *     Also, the listeners of the context will be updated.
     * @param value the updated value
     */
    public void setValue(Object value) {
        if (isReprValue()) {
            getReprValue().updateFromGui(this, value);
            sendUpdateToListeners(getRoot());
        } else {
            throw new UnsupportedOperationException("" + getRepresentation());
        }
    }

    /**
     * execute the associated action through {@link GuiReprAction} (can be checked by {@link #isReprAction()})
     *   otherwise throw an exception.
     *   the method causes same effects by GUI operation,
     *     i.e. updating other components.
     */
    public void executeAction() {
        if (isReprAction()) {
            getReprAction().executeAction(this);
        } else {
            throw new UnsupportedOperationException("" + getRepresentation());
        }
    }
}
