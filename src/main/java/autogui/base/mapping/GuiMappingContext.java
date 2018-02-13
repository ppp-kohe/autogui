package autogui.base.mapping;

import autogui.base.log.GuiLogManager;
import autogui.base.type.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
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

    protected List<GuiMappingContext> getChildrenForAdding() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public GuiMappingContext createChildCandidate(GuiTypeElement typeElement) {
        return new GuiMappingContext(typeElement, this);
    }

    public void addToParent() {
        if (parent != null) {
            parent.getChildrenForAdding().add(this);
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
        while (ctx.getParent() != null) {
            ctx = ctx.getParent();
        }
        return ctx;
    }

    public String getName() {
        return typeElement.getName();
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

    /** it includes {@link GuiTypeObject} and {@link GuiTypeCollection}
     *    which are subtypes of {@link GuiTypeValue} */
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


    public void setSource(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }

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

    /** set the source to newValue, call {@link #updateSourceFromRoot(GuiMappingContext)} starting with this*/
    public void updateSourceFromGui(Object newValue) {
        this.source = newValue;
        updateSourceFromRoot(this);
    }

    /** {@link #updateSourceFromGui(Object)} with the null cause */
    public void updateSourceFromRoot() {
        updateSourceFromRoot(null);
    }

    /** obtains the root context, recursively collect updated sub-contexts from the root,
     *    and call listeners with each updated context.  */
    public void updateSourceFromRoot(GuiMappingContext cause) {
        GuiMappingContext ctx = getRoot();
        List<GuiMappingContext> updated = new ArrayList<>();
        ctx.collectUpdatedSource(cause, updated);

        updated.forEach(c -> c.getListeners()
                .forEach(l -> l.update(this, c.getSource())));
    }

    /** recursively collect updated sub-contexts from this and call listeners*/
    public void updateSourceSubTree() {
        List<GuiMappingContext> updated = new ArrayList<>();
        collectUpdatedSource(null, updated);

        updated.forEach(c -> c.getListeners()
                .forEach(l -> l.update(this, c.getSource())));
    }

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

    /** space separated words from the camel-case context name */
    public String getDisplayName() {
        if (displayName == null) {
            displayName = nameJoinForDisplay(nameSplit(getName(), true));
        }
        return displayName;
    }

    /** the top word of the split name, which is an action verb in most cases */
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
     * If <code>forDisplay=true</code>,
     * <pre>
     *  "helloWorld" =&gt; ["hello", "World"]
     *  "HelloWorld" =&gt; ["Hello", "World"]
     *  "helloWORLD" =&gt; ["hello", "WORLD"]
     *  "HELLOWorld" =&gt; ["HELLO", "World"]
     *  "" =&gt; [""]
     * </pre>
     */
    public List<String> nameSplit(String name, boolean forDisplay) {
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

    public boolean isNameCharUpper(char c, int i, int len, String name, boolean forDisplay) {
        if (forDisplay) {
            return (Character.isUpperCase(c) &&
                    (i > 0 && Character.isLowerCase(name.charAt(i - 1)) ||  // a[A]
                   (i + 1 < len && Character.isLowerCase(name.charAt(i + 1))))); //[.]a
        } else {
            return Character.isUpperCase(c);
        }
    }

    public boolean isNameSeparator(char c) {
        return Character.isWhitespace(c) || c == '$' || c =='_' || !Character.isJavaIdentifierPart(c);
    }


    public void errorWhileUpdateSource(Throwable error) {
        GuiLogManager.get().logError(error);
    }

    public boolean isParentPropertyPane() {
        return getParent() != null &&
                getParent().getRepresentation() instanceof GuiReprPropertyPane;
    }


    public GuiReprPropertyPane getParentPropertyPane() {
        return (GuiReprPropertyPane) getParent().getRepresentation();
    }

    public boolean isParentValuePane() {
        return getParent() != null &&
                getParent().getRepresentation() instanceof GuiReprValue;
    }

    public GuiReprValue getParentValuePane() {
        return (GuiReprValue) getParent().getRepresentation();
    }

    public Object getParentSource() {
        return getParent() != null ? getParent().getSource() : null;
    }


    public boolean isParentCollectionTable() {
        return getParent() != null &&
                getParent().getRepresentation() instanceof GuiReprCollectionTable;
    }

    public boolean isParentCollectionElement() {
        return getParent() != null &&
                getParent().getRepresentation() instanceof GuiReprCollectionElement;
    }

    public boolean isCollectionElement() {
        return getRepresentation() != null && getRepresentation() instanceof GuiReprCollectionElement;
    }

    //////////////////////

    /** take taskRunner from parent context */
    public ScheduledExecutorService getTaskRunner() {
        if (taskRunner == null) {
            GuiMappingContext parent = getParent();
            if (parent != null) {
                taskRunner = parent.getTaskRunner();
            } else {
                taskRunner = Executors.newSingleThreadScheduledExecutor();
            }
        }
        return taskRunner;
    }

    public <T> T execute(Callable<T> task) throws Throwable {
        try {
            return getTaskRunner()
                    .submit(task)
                    .get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public GuiPreferences getPreferences() {
        if (preferences == null) {
            GuiMappingContext parent = getParent();
            if (parent != null) {
                preferences = parent.getPreferences().getChild(this);
            } else {
                preferences = new GuiPreferences(this);
            }
        }
        return preferences;
    }
}
