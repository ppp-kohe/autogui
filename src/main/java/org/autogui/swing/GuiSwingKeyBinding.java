package org.autogui.swing;

import org.autogui.swing.util.NamedPane;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * a class for automatic key-stroke assignments.
 *  It relies on {@link KeyboardFocusManager} which can steel key-events globally.
 */
public class GuiSwingKeyBinding {
    protected List<BiConsumer<GuiSwingKeyBinding, Component>> traverseFunctions = new ArrayList<>();
    protected Map<KeyStroke, Map<KeyPrecedenceSet,List<KeyStrokeAction>>> keyToPrecToActions = new HashMap<>();
    protected List<KeyStrokeAction> assigned = new ArrayList<>();
    protected Set<KeyStroke> assignedKeys = new HashSet<>();
    protected KeyBindDispatcher dispatcher;

    public GuiSwingKeyBinding() {}

    /**
     * @return a default key-binding with excluding some keys by {@link #addDefaultExcluded()}
     * and with default traversing function by {@link #addTraverseFunctionDefault()}.
     */
    public static GuiSwingKeyBinding createWithDefaultExcluded() {
        GuiSwingKeyBinding b = new GuiSwingKeyBinding();
        b.addDefaultExcluded();
        b.addTraverseFunctionDefault();
        return b;
    }

    /**
     * @return a key-binding without automatic traversing and bindings
     */
    public static GuiSwingKeyBinding createWithoutAutomaticBindings() {
        return new GuiSwingKeyBinding();
    }

    /////////////////////////// for manual construction

    /**
     * append a traversal function for setting focus action with a key-stroke for a specific component.
     * <pre>
     *     GuiSwingKeyBinding.createWithoutAutomaticBindings()
     *        .putTraverseHighPrecedencePaneFocus(ValuePane.class, vp -&gt; vp.getSwingViewContext().getName().equals("prop"),
     *                    GuiSwingKeyBinding.getKeyStroke("shift X"))
     * </pre>
     * @param type  the component type
     * @param predicate predicate for specifying the component
     * @param keyStroke the key-stroke for focusing
     * @param <CompType> the component type
     * @return this
     */
    public <CompType extends JComponent> GuiSwingKeyBinding putTraverseHighPrecedencePaneFocus(Class<CompType> type, Predicate<CompType> predicate,
                                                                                               KeyStroke keyStroke) {
        return putTraverse(type, predicate, (self, c) ->
            putKeyStroke(new KeyStrokeActionForComponent(c, PRECEDENCE_SET_INPUT_MAP, keyStroke)));
    }

    /**
     * append a traversal function for setting an action with a key-stroke for a specific component.
     * <pre>
     *     GuiSwingKeyBinding.createWithoutAutomaticBindings()
     *        .putTraverseHighPrecedenceAction(ValuePane.class, vp -&gt; vp.getSwingViewContext().getName().equals("prop"),
     *                    GuiSwingKeyBinding.getKeyStroke("shift X"), action)
     * </pre>
     * @param type the component type
     * @param predicate predicate for specifying the component
     * @param keyStroke the key-stroke for the action
     * @param action the action executed by the key-stroke
     * @param <CompType> the component type
     * @return this
     */
    public <CompType extends JComponent> GuiSwingKeyBinding putTraverseHighPrecedenceAction(Class<CompType> type, Predicate<CompType> predicate,
                                                                                            KeyStroke keyStroke, Action action) {
        return putTraverse(type, predicate, (self, c) ->
            putKeyStroke(new KeyStrokeActionForAction(c, PRECEDENCE_SET_INPUT_MAP, keyStroke, action)));
    }

    /**
     * append a traversal function for setting a function with a key-stroke for a specific component.
     * <pre>
     *     GuiSwingKeyBinding.createWithoutAutomaticBindings()
     *        .putTraverseHighPrecedence(ValuePane.class, vp -&gt; vp.getSwingViewContext().getName().equals("prop"),
     *                    GuiSwingKeyBinding.getKeyStroke("shift X"), comp -&gt; System.out.println(comp))
     * </pre>
     * @param type the component type
     * @param predicate predicate for specifying the component
     * @param keyStroke the key-stroke for the function
     * @param f the function executed by the key-stroke
     * @param <CompType> the component type
     * @return this
     */
    public <CompType extends JComponent> GuiSwingKeyBinding putTraverseHighPrecedence(Class<CompType> type, Predicate<CompType> predicate,
                                                                                      KeyStroke keyStroke, Consumer<CompType> f) {
        return putTraverse(type, predicate, (self, c) ->
            putKeyStroke(new KeyStrokeActionForFunction<>(c, PRECEDENCE_SET_INPUT_MAP, keyStroke, f)));
    }

    /**
     * append a traversal function for setting key-strokes for a specified component  by a given function.
     *  the binding will be happen only the first component matched by the predicate.
     * @param type the component type
     * @param predicate predicate for specifying the component
     * @param put a function for setting up key-strokes for the component
     * @param <T> the component type
     * @return this
     */
    public <T extends Component> GuiSwingKeyBinding putTraverse(Class<T> type, Predicate<T> predicate, BiConsumer<GuiSwingKeyBinding, T> put) {
        return addTraverseFunction((self, comp) -> {
            T t = GuiSwingView.findComponent(comp, type, predicate);
            if (t != null) {
                put.accept(self, t);
            }
        });
    }

    public GuiSwingKeyBinding putHighPrecedencePaneFocus(JComponent c, KeyStroke keyStroke) {
        return putKeyStroke(new KeyStrokeActionForComponent(c, PRECEDENCE_SET_INPUT_MAP, keyStroke));
    }

    public GuiSwingKeyBinding putHighPrecedenceAction(JComponent c, KeyStroke keyStroke, Action action) {
        return putKeyStroke(new KeyStrokeActionForAction(c, PRECEDENCE_SET_INPUT_MAP, keyStroke, action));
    }

    public <CompType extends JComponent> GuiSwingKeyBinding putHighPrecedence(CompType c, KeyStroke keyStroke, Consumer<CompType> f) {
        return putKeyStroke(new KeyStrokeActionForFunction<>(c, PRECEDENCE_SET_INPUT_MAP, keyStroke, f));
    }

    ///////////////////////////

    public GuiSwingKeyBinding addTraverseFunctionDefault() {
        return addTraverseFunction((self, comp) -> traverseKeyBinding(comp, 0));
    }

    public GuiSwingKeyBinding addTraverseFunction(BiConsumer<GuiSwingKeyBinding, Component> f) {
        traverseFunctions.add(f);
        return this;
    }

    public interface RecommendedKeyStroke {
        KeyStroke getRecommendedKeyStroke();
        default GuiSwingKeyBinding.KeyPrecedenceSet getRecommendedKeyPrecedence() {
            return new KeyPrecedenceSet();
        }
    }

    public static KeyStroke getKeyStroke(String key) {
        if (key.isEmpty() || key.equals("none")) {
            return null;
        } else {
            KeyStroke s = KeyStroke.getKeyStroke(key);
            if (s == null) {
                System.err.println("could not obtain key-stroke for \"" + key + "\"");
                return null;
            } else {
                return copyWithModifiers(s, s.getModifiers() |
                        PopupExtension.getMenuShortcutKeyMask());
            }
        }
    }

    public static KeyStroke copyWithModifiers(KeyStroke stroke, int mod) {
        if (stroke.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return KeyStroke.getKeyStroke(stroke.getKeyChar(), mod);
        } else {
            return KeyStroke.getKeyStroke(stroke.getKeyCode(), mod);
        }
    }

    public void bind(JComponent component) {
        addKeyBindingForComponentTree(component);
        assign();
        setDispatcher(component);
    }

    public GuiSwingKeyBinding addDefaultExcluded() {
        int mod = PopupExtension.getMenuShortcutKeyMask();
        int[] vk = {
                KeyEvent.VK_Q, //quit
                KeyEvent.VK_W, //close
                KeyEvent.VK_A, //select-all
                KeyEvent.VK_H, //hide
                KeyEvent.VK_Z, //undo
                KeyEvent.VK_X, //cut
                KeyEvent.VK_C, //copy
                KeyEvent.VK_V, //paste
        };
        for (int k : vk) {
            addDefaultExcludedKey(k, mod);
        }
        addDefaultExcludedKey(KeyEvent.VK_H, KeyEvent.ALT_DOWN_MASK | mod);
        return this;
    }

    public GuiSwingKeyBinding addDefaultExcludedKey(int k, int mod) {
        return putKeyStroke(new KeyStrokeActionForInputMap(null, PRECEDENCE_SET_INPUT_MAP,
                KeyStroke.getKeyStroke(k, mod)));
    }

    public GuiSwingKeyBinding addKeyBindingForComponentTree(Component c) {
        traverseFunctions.forEach(f -> f.accept(this, c));
        return this;
    }

    public GuiSwingKeyBinding traverseKeyBinding(Component c, int depth) {
        if (c instanceof GuiSwingView.ValuePane<?>) {
            putForValuePane((GuiSwingView.ValuePane<?>) c, depth);
        }
        if (c instanceof JComponent) {
            putForComponentInputMap((JComponent) c, depth);
        }
        if (c instanceof Container) {
            for (Component sub : ((Container) c).getComponents()) {
                traverseKeyBinding(sub, depth + 1);
            }
        }
        return this;
    }

    public GuiSwingKeyBinding putForValuePane(GuiSwingView.ValuePane<?> pane, int depth) {
        List<PopupCategorized.CategorizedMenuItem> items = pane.getSwingStaticMenuItems();
        for (PopupCategorized.CategorizedMenuItem item : items) {
            if (item instanceof RecommendedKeyStroke) {
                putItemRecommended(pane, depth, (RecommendedKeyStroke) item);
            }
        }

        KeyStroke paneStroke = pane.getSwingFocusKeyStroke();
        if (paneStroke != null) {
            putKeyStrokeFocus(pane, depth, paneStroke);
        }
        return this;
    }

    public GuiSwingKeyBinding putForComponentInputMap(JComponent c, int depth) {
        return putForComponentInputMap(c);
    }

    public GuiSwingKeyBinding putForComponentInputMap(JComponent c) {
        InputMap map = c.getInputMap();
        if (map != null) {
            KeyStroke[] keys = map.allKeys();
            if (keys != null) {
                for (KeyStroke k : keys) {
                    putKeyStroke(new KeyStrokeActionForInputMap(c, PRECEDENCE_SET_INPUT_MAP, k));
                }
            }
        }
        return this;
    }

    public void assign() {
        while (!keyToPrecToActions.isEmpty()) {
            List<Map.Entry<KeyStroke, Map<KeyPrecedenceSet,List<KeyStrokeAction>>>> next = new ArrayList<>(keyToPrecToActions.entrySet());
            next.sort(Comparator.comparing(k ->
                    Integer.bitCount(k.getKey().getModifiers()))); //a smaller number of modifiers precedes
            if (next.stream()
                    .noneMatch(e -> bind(e.getKey(), e.getValue()))) {
                break; //no new assignments: give up
            }
        }
    }

    public boolean putItem(GuiSwingView.ValuePane<?> pane, int depth, PopupCategorized.CategorizedMenuItem item) {
        boolean hasStroke = false;
        if (item instanceof PopupCategorized.CategorizedMenuItemAction) {
            if (putKeyStroke(pane, depth, item, (PopupCategorized.CategorizedMenuItemAction) item)) {
                hasStroke = true;
            }
        } else if (item instanceof PopupCategorized.CategorizedMenuItemComponent) {
            JComponent comp = ((PopupCategorized.CategorizedMenuItemComponent) item).getMenuItem();
            if (comp instanceof AbstractButton) {
                if (putKeyStroke(pane, depth, item, ((AbstractButton) comp).getAction())) {
                    hasStroke = true;
                }
            }
        } else if (item instanceof AbstractButton) {
            if (putKeyStroke(pane, depth, item, ((AbstractButton) item).getAction())) {
                hasStroke = true;
            }
        }

        if (!hasStroke && item instanceof RecommendedKeyStroke) {
            putItemRecommended(pane, depth, (RecommendedKeyStroke) item);
        }
        return hasStroke;
    }

    public boolean putItemRecommended(GuiSwingView.ValuePane<?> pane, int depth, RecommendedKeyStroke item) {
        KeyStroke stroke = item.getRecommendedKeyStroke();
        if (stroke != null) {
            putKeyStroke(new KeyStrokeActionForValuePane(pane, (PopupCategorized.CategorizedMenuItem) item,
                    (item instanceof Action ? (Action) item : null),
                    new KeyPrecedenceSet(item.getRecommendedKeyPrecedence(), new KeyPrecedenceDepth(depth)),
                    stroke));
            return true;
        } else {
            return false;
        }
    }


    public boolean putKeyStroke(GuiSwingView.ValuePane<?> pane, int depth,
                                PopupCategorized.CategorizedMenuItem i, Action item) {
        KeyStroke stroke = (item == null ? null : (KeyStroke) item.getValue(Action.ACCELERATOR_KEY));
        if (stroke != null) {
            KeyPrecedenceSet prec = new KeyPrecedenceSet(PRECEDENCE_FLAG_LIB_SPECIFIED, new KeyPrecedenceDepth(depth));
            putKeyStroke(new KeyStrokeActionForValuePane(pane, i, item, prec, stroke));
            return true;
        } else {
            return false;
        }
    }

    public boolean putKeyStrokeFocus(GuiSwingView.ValuePane<?> pane, int depth, KeyStroke stroke) {
        if (stroke != null) {
            KeyPrecedenceSet prec;
            if (pane.getSwingViewContext().isAcceleratorKeyStrokeSpecified()) {
                prec = new KeyPrecedenceSet(PRECEDENCE_FLAG_USER_SPECIFIED, new KeyPrecedenceDepth(depth));
            } else {
                prec = new KeyPrecedenceSet(new KeyPrecedenceDepth(depth));
            }
            putKeyStroke(new KeyStrokeActionForValuePane(pane, null, null, prec, stroke));
            return true;
        } else {
            return false;
        }
    }

    public GuiSwingKeyBinding putKeyStroke(KeyStrokeAction item) {
        keyToPrecToActions.computeIfAbsent(item.stroke, s -> new HashMap<>())
                .computeIfAbsent(item.precedence, d -> new ArrayList<>())
                .add(item);
        return this;
    }

    public abstract static class KeyStrokeAction {
        public KeyPrecedenceSet precedence;
        public KeyStroke stroke;
        protected boolean assigned;

        public KeyStrokeAction() {}

        public boolean isAssigned() {
            return assigned;
        }

        public abstract boolean isDispatcherRequired();
        public abstract void process();

        public void assigned() {
            assigned = true;
        }

        public Set<KeyStrokeAction> derive() {
            return Collections.emptySet();
        }

        public void updateInfo() { }

        public void show(Component c) {
            if (c == null) {
                return;
            }
            Component parent = c.getParent();
            show(parent);

            if (!c.isShowing()) {
                if (parent instanceof JTabbedPane) {
                    ((JTabbedPane) parent).setSelectedComponent(c);
                }
            }
            if (c instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane<?>) c).requestSwingViewFocus();
            }
        }
    }

    /** a predefined key-binding by the component UI */
    public static class KeyStrokeActionForInputMap extends KeyStrokeAction {
        protected JComponent component;

        public KeyStrokeActionForInputMap(JComponent component, KeyPrecedenceSet precedence, KeyStroke stroke) {
            this.component = component;
            this.precedence = precedence;
            this.stroke = stroke;
        }

        @Override
        public boolean isDispatcherRequired() {
            return false;
        }

        @Override
        public void process() {
            if (component == null) {
                return;
            }
            component.getActionForKeyStroke(this.stroke)
                    .actionPerformed(new ActionEvent(component, -1,
                            "" + component.getInputMap().get(stroke)));
        }
    }

    public static class KeyStrokeActionForComponent extends KeyStrokeAction {
        protected JComponent component;

        public KeyStrokeActionForComponent(JComponent component, KeyPrecedenceSet precedence, KeyStroke stroke) {
            this.component = component;
            this.precedence = precedence;
            this.stroke = stroke;
        }

        @Override
        public boolean isDispatcherRequired() {
            return true;
        }

        @Override
        public void process() {
            show(component);
        }
    }

    public static class KeyStrokeActionForAction extends KeyStrokeAction {
        protected JComponent component;
        protected Action action;

        public KeyStrokeActionForAction(JComponent component, KeyPrecedenceSet precedence, KeyStroke stroke, Action action) {
            this.component = component;
            this.precedence = precedence;
            this.stroke = stroke;
            this.action = action;
        }

        @Override
        public boolean isDispatcherRequired() {
            return true;
        }

        @Override
        public void process() {
            show(component);
            action.actionPerformed(new ActionEvent(component, -1, "key-stroke-action"));
        }
    }

    public static class KeyStrokeActionForFunction<CompType extends JComponent> extends KeyStrokeAction {
        protected CompType component;
        protected Consumer<CompType> action;

        public KeyStrokeActionForFunction(CompType component, KeyPrecedenceSet precedence, KeyStroke stroke, Consumer<CompType> action) {
            this.component = component;
            this.precedence = precedence;
            this.stroke = stroke;
            this.action = action;
        }

        @Override
        public boolean isDispatcherRequired() {
            return true;
        }

        @Override
        public void process() {
            show(component);
            action.accept(component);
        }
    }

    public static class KeyStrokeActionForValuePane extends KeyStrokeAction {
        public GuiSwingView.ValuePane<?> pane;
        public PopupCategorized.CategorizedMenuItem item;
        public Action action;

        protected KeyStrokeActionForValuePane base;
        protected List<KeyStrokeActionForValuePane> derived;

        protected String description;

        public KeyStrokeActionForValuePane(GuiSwingView.ValuePane<?> pane, PopupCategorized.CategorizedMenuItem item,
                               Action action, KeyPrecedenceSet precedence, KeyStroke originalStroke) {
            this.pane = pane;
            this.item = item;
            this.action = action;
            this.precedence = precedence;
            this.stroke = originalStroke;
        }

        public Action getAction() {
            return action;
        }

        public PopupCategorized.CategorizedMenuItem getItem() {
            return item;
        }

        public KeyStroke getStroke() {
            return stroke;
        }

        public KeyPrecedenceSet getPrecedence() {
            return precedence;
        }

        public GuiSwingView.ValuePane<?> getPane() {
            return pane;
        }

        public void updateInfo() {
            updateActionKey();
            updateToolTip();
        }

        public void updateActionKey() {
            if (action != null) {
                action.putValue(Action.ACCELERATOR_KEY, stroke);
            } else if (pane != null) {
                pane.setKeyStrokeString(getKeyStrokeString());
            }
        }

        public void updateToolTip() {
            String newDesc = getDescription();
            if (action != null) {
                action.putValue(Action.SHORT_DESCRIPTION,
                        appendToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION),  newDesc));
            } else if (pane != null) {
                JComponent comp = pane.asSwingViewComponent();
                appendToolTip(comp, newDesc);
                if (comp.getParent() instanceof NamedPane namedPane) {
                    appendToolTip(namedPane, newDesc);
                    if (namedPane.getParent() instanceof JTabbedPane tabbedPane) {
                        updateToolTipForTab(namedPane, tabbedPane, newDesc);
                    }
                } else if (comp.getParent() instanceof JTabbedPane tabbedPane) {
                    updateToolTipForTab(comp, tabbedPane, newDesc);
                }
            }
        }

        public void updateToolTipForTab(JComponent child, JTabbedPane tabbedPane, String newDesc) {
            int i = tabbedPane.indexOfComponent(child);
            if (i != -1) {
                tabbedPane.setToolTipTextAt(i,
                        appendToolTipText(tabbedPane.getToolTipTextAt(i), newDesc));
            }
        }

        public void appendToolTip(JComponent comp, String newDesc) {
            comp.setToolTipText(appendToolTipText(comp.getToolTipText(), newDesc));
        }

        public String appendToolTipText(String existing, String newDesc) {
            if (existing != null && !existing.isEmpty()) {
                String exTrim = existing.trim();
                if (exTrim.toLowerCase().endsWith("</html>")) { //casual insertion for html
                    int i = existing.lastIndexOf("</");
                    int bodyEnd = existing.lastIndexOf("</body>", i);
                    if (bodyEnd != -1) {
                        i = bodyEnd;
                    }
                    return existing.substring(0, i) + " <br>" + newDesc + existing.substring(i);
                } else {
                    int tail = tailCodePoint(exTrim);
                    if (!exTrim.isEmpty() && Character.isLetterOrDigit(tail)) {
                        return existing + ", " + newDesc;
                    } else {
                        return existing + " " + newDesc;
                    }
                }
            } else {
                return newDesc;
            }
        }

        private int tailCodePoint(String s) {
            if (!s.isEmpty()) {
                char c = s.charAt(s.length() - 1);
                if (Character.isSurrogate(c) && s.length() >= 2) {
                    char c2 = s.charAt(s.length() - 2);
                    if (Character.isHighSurrogate(c2)) {
                        return Character.toCodePoint(c2, c);
                    } else {
                        return Character.toCodePoint(c, c2);
                    }
                } else {
                    return c;
                }
            } else {
                return -1;
            }
        }

        public String getDescription() {
            if (description == null) {
                String actionStr = getActionString();
                String keyStr = getKeyStrokeString();
                description = actionStr + ": " + keyStr + " ";
            }
            return description;
        }


        public String getActionString() {
            String name = "";
            if (pane != null) {
                name = "\"" + pane.getSwingViewContext().getDisplayName() + "\"";
            }
            if (action == null) {
                return "Focus " + name;
            } else {
                return "Action \"" + action.getValue(Action.NAME) + "\" of " + name;
            }
        }

        public String getKeyStrokeString() {
            return UIManagerUtil.getInstance().getOsVersion()
                    .getKeyStrokeString(stroke.getModifiers(), stroke.getKeyCode());
        }

        @Override
        public boolean isDispatcherRequired() {
            return true;
        }

        public void process() {
            show(pane.asSwingViewComponent());
            if (action != null && action.isEnabled()) {
                System.err.println("Executing by Key: " + getDescription());
                action.actionPerformed(null);
            }
        }

        @Override
        public void assigned() {
            this.assigned = true;
            getBase().setAssignedToChild(assigned);
            putAsPaneInputAction();
        }

        protected void putAsPaneInputAction() {
            if (action != null) {
                InputMap inputMap = pane.asSwingViewComponent().getInputMap();
                ActionMap actionMap = pane.asSwingViewComponent().getActionMap();
                KeyStroke stroke = getStroke();
                Object name = action.getValue(Action.NAME);
                if (inputMap.get(stroke) == null &&
                        (actionMap.get(name) == null || actionMap.get(name).equals(action))) {
                    inputMap.put(stroke, name);
                    actionMap.put(name, action);
                }
            }
        }

        public KeyStrokeActionForValuePane getBase() {
            KeyStrokeActionForValuePane b = this;
            while (b.base != null) {
                b = b.base;
            }
            return b;
        }

        private void setAssignedToChild(boolean assigned) {
            this.assigned = assigned;
            if (derived != null) {
                derived.forEach(d -> d.setAssignedToChild(assigned));
            }
        }

        @Override
        public Set<KeyStrokeAction> derive() {
            int mods = stroke.getModifiers();
            List<Integer> used = getModifiers().stream()
                    .filter(i -> (i & mods) != 0)
                    .toList();
            List<Set<Integer>> available = getModifierPowerSet().stream()
                    .filter(s -> used.stream().noneMatch(s::contains))
                    .toList();

            return available.stream()
                    .map(s -> toModifiers(mods, s))
                    .map(mod -> copyWithModifiers(stroke, mod))
                    .map(k -> connectToChild(new KeyStrokeActionForValuePane(pane, item, action, precedence, k)))
                    .collect(Collectors.toSet());
        }

        private int toModifiers(int baseMod, Set<Integer> mods) {
            return mods.stream()
                    .reduce(baseMod, (pre,next) -> pre | next);
        }

        public KeyStrokeActionForValuePane connectToChild(KeyStrokeActionForValuePane child) {
            child.base = this;
            if (derived == null) {
                derived = new ArrayList<>();
            }
            derived.add(child);
            return child;
        }

        @Override
        public String toString() {
            return "(stroke=" + stroke + ",action=" +
                    (action == null ? "null" :
                            (action.getClass().getSimpleName() + "@" +
                             Integer.toHexString(action.hashCode()) + ":" +
                                    action.getValue(Action.NAME))) +
                    ", precedence=" + precedence +
                    ", assigned=" + assigned +
                    ", pane=" + (pane != null ? pane.getClass().getSimpleName() + ":"+ pane.getSwingViewContext().getName() : "null") +
                    ")";
        }

        private static Set<Set<Integer>> modifierSet;
        private static List<Integer> modifiers;

        public static Set<Set<Integer>> getModifierPowerSet() {
            if (modifierSet == null) {
                Set<Set<Integer>> set = new HashSet<>();
                List<Integer> elements = getModifiers();

                for (int i : elements) {
                    for (Set<Integer> s : new HashSet<>(set)) {
                        Set<Integer> s2 = new HashSet<>(s);
                        s2.add(i);
                        set.add(s2);
                    }
                    set.add(Collections.singleton(i));
                }
                modifierSet = set;
            }
            return modifierSet;
        }

        public static List<Integer> getModifiers() {
            if (modifiers == null) {
                modifiers = new ArrayList<>(Arrays.asList(
                        InputEvent.SHIFT_DOWN_MASK,
                        InputEvent.CTRL_DOWN_MASK,
                        InputEvent.ALT_DOWN_MASK));
                        //,InputEvent.META_DOWN_MASK)); //meta is invalid for generic keyboards in Win/Linux, for mac getMenuShortcutKeyMask() returns it
                //InputEvent.ALT_GRAPH_DOWN_MASK);
                modifiers.remove((Object) PopupExtension.getMenuShortcutKeyMask());
                // for Linux Desktop (GNOME only?): (Alt|Ctrl[+Alt])[+Shift] [+key]
                // for Windows: Ctrl[+Alt][+Shift] [+key]
                // for macOS:  Cmd=Meta, Opt=Alt, Meta[+Ctrl][+Alt][+Shift] [+key]
            }
            return modifiers;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean checkMenuModifiersMask(int constKey, int testedMods) {
        return UIManagerUtil.checkMenuModifiersMask(constKey, testedMods);
    }


    public boolean bind(KeyStroke key, Map<KeyPrecedenceSet,List<KeyStrokeAction>> precToActions) {
        if (!assignedKeys.contains(key)) {
            List<KeyPrecedenceSet> precedences = new ArrayList<>(precToActions.keySet());
            precedences.sort(Comparator.naturalOrder());

            KeyPrecedenceSet prec = precedences.removeFirst();
            List<KeyStrokeAction> actions = precToActions.get(prec);
            KeyStrokeAction assignedAction = actions.removeFirst();

            assigned.add(assignedAction);
            assignedAction.assigned();
            assignedKeys.add(key);

            putDerived(precToActions); //derive other possible key-strokes for rest of actions
            keyToPrecToActions.remove(key);
            cleanEmptyActions();
            return true;
        } else {
            return false;
        }
    }

    private void putDerived(Map<KeyPrecedenceSet,List<KeyStrokeAction>> precToActions) {
        new ArrayList<>(precToActions.values()).stream()
                .flatMap(List::stream)
                .filter(a -> !a.isAssigned())
                .flatMap(a -> a.derive().stream())
                .filter(ak -> !assignedKeys.contains(ak.stroke))
                .forEach(this::putKeyStroke);
    }

    private void cleanEmptyActions() {
        new ArrayList<>(keyToPrecToActions.entrySet()).forEach(e -> {
            new ArrayList<>(e.getValue().entrySet()).forEach(e2 -> {
                e2.getValue().removeIf(KeyStrokeAction::isAssigned);
                if (e2.getValue().isEmpty()) {
                    e.getValue().remove(e2.getKey());
                }
            });
            if (e.getValue().isEmpty()) {
                keyToPrecToActions.remove(e.getKey());
            }
        });
    }

    @Deprecated
    public void setDispatcher() {
        setDispatcher(null);
    }

    /**
     * @param component the root pane
     * @since 1.4
     */
    public void setDispatcher(JComponent component) {
        Map<KeyStroke, KeyStrokeAction> inputMap = new HashMap<>();
        for (KeyStrokeAction action : assigned) {
            action.updateInfo();
            if (action.isDispatcherRequired()) {
                inputMap.put(action.stroke, action);
            }
        }
        unbind(component);
        dispatcher = new KeyBindDispatcher(inputMap);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
        if (component != null) {
            component.addHierarchyListener(dispatcher);
        }
    }


    @Deprecated
    public void unbind() {
        unbind(null);
    }

    /**
     * @param component the root pane, the former parameter of {@link #setDispatcher(JComponent)}
     * @since 1.4
     */
    public void unbind(JComponent component) {
        if (dispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            if (component != null) {
                component.removeHierarchyListener(dispatcher);
            }
        }
        dispatcher = null;
    }

    public static class KeyBindDispatcher implements KeyEventDispatcher, HierarchyListener {
        protected Map<KeyStroke, KeyStrokeAction> inputMap;
        protected Window targetWindow;

        public KeyBindDispatcher(Map<KeyStroke, KeyStrokeAction> inputMap) {
            this.inputMap = inputMap;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
            KeyStrokeAction action = inputMap.get(stroke);
            if (targetWindow != null && !targetWindow.isFocused()) {
                return false;
            }
            if (action != null) {
                action.process();
                return true;
            } else {
                return false;
            }
        }

        public Map<KeyStroke, KeyStrokeAction> getInputMap() {
            return inputMap;
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            targetWindow = SwingUtilities.getWindowAncestor(e.getComponent());
        }
    }

    public KeyBindDispatcher getDispatcher() {
        return dispatcher;
    }

    public List<KeyStrokeAction> getAssigned() {
        return assigned;
    }

    public List<KeyStrokeAction> getAssignedFromDispatcher() {
        if (dispatcher == null) {
            return Collections.emptyList();
        } else {
            return getDispatcher().getInputMap().values().stream()
                    .sorted(Comparator.comparingInt(a -> assigned.indexOf(a)))
                    .collect(Collectors.toList());
        }
    }

    public static int PRECEDENCE_TYPE_FLAG_HIGH = 1;
    public static int PRECEDENCE_TYPE_FLAG_USER_SPECIFIED = 10;
    public static int PRECEDENCE_TYPE_DEPTH = 1000;
    public static int PRECEDENCE_TYPE_FLAG_LIB_SPECIFIED = 10000;

    public static KeyPrecedenceFlag PRECEDENCE_FLAG_HIGH = new KeyPrecedenceFlag(PRECEDENCE_TYPE_FLAG_HIGH);
    /**
     * user specified a key-stroke by the GuiIncluded(keyStroke=...) annotation
     */
    public static KeyPrecedenceFlag PRECEDENCE_FLAG_USER_SPECIFIED = new KeyPrecedenceFlag(PRECEDENCE_TYPE_FLAG_USER_SPECIFIED);
    public static KeyPrecedenceFlag PRECEDENCE_FLAG_LIB_SPECIFIED = new KeyPrecedenceFlag(PRECEDENCE_TYPE_FLAG_LIB_SPECIFIED);

    public static KeyPrecedenceSet PRECEDENCE_SET_INPUT_MAP = new KeyPrecedenceSet(PRECEDENCE_FLAG_HIGH);

    /**
     * a set of key-precedence information:
     *   a key-precedence becomes a flag value ({@link KeyPrecedenceFlag}
     *      which indicates the type of action, or
     *    a view-depth ({@link KeyPrecedenceDepth}).
     */
    @SuppressWarnings("this-escape")
    public static class KeyPrecedenceSet implements Comparable<KeyPrecedenceSet> {
        protected List<KeyPrecedence> set;

        public KeyPrecedenceSet(List<KeyPrecedence> set) {
            this.set = new ArrayList<>(set);
            setup();
        }

        public KeyPrecedenceSet(KeyPrecedence... set) {
            this(Arrays.asList(set));
        }

        public KeyPrecedenceSet(KeyPrecedenceSet set, KeyPrecedence... additional) {
            this.set = new ArrayList<>(set.set);
            this.set.addAll(Arrays.asList(additional));
            setup();
        }

        protected void setup() {
            List<KeyPrecedence> used = new ArrayList<>();
            for (KeyPrecedence p : this.set) {
                if (used.stream()
                        .noneMatch(existing -> existing.getPrecedenceType() == p.getPrecedenceType())) {
                    used.add(p);
                }
            }
            this.set = new ArrayList<>(used);
            this.set.sort(Comparator.comparing(KeyPrecedence::getPrecedenceType));
        }

        @Override
        public int compareTo(KeyPrecedenceSet o) {
            List<KeyPrecedence> p1 = set;
            List<KeyPrecedence> p2 = o.set;

            int i = 0;
            int j = 0;
            while (i < p1.size() || j < p2.size()) {
                boolean p1Proceed = i < p1.size();
                boolean p2Proceed = j < p2.size();
                KeyPrecedence p1Next = (p1Proceed ? p1.get(i) : null);
                KeyPrecedence p2Next = (p2Proceed ? p2.get(j) : null);

                if (p1Next == null && p2Next != null) {
                    p1Next = p2Next.getNone();
                    p1Proceed = false; //for comprehension
                } else if (p1Next != null && p2Next == null) {
                    p2Next = p1Next.getNone();
                    p2Proceed = false; //for comprehension
                } else if (p1Next == null/* && p2Next == null*/) {
                    break; //never
                }

                int typePre = Integer.compare(p1Next.getPrecedenceType(), p2Next.getPrecedenceType());

                if (typePre < 0 && p1Proceed) { //p1:smaller(higher), p2:larger
                    p2Proceed = false;
                } else if (typePre > 0 && p2Proceed) { //p1:larger, p2:smaller(higher)
                    p1Proceed = false;
                }

                int r = p1Next.compareTo(p2Next);
                if (r != 0) {
                    return r;
                }

                if (p1Proceed) {
                    i++;
                }
                if (p2Proceed) {
                    j++;
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            return set.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyPrecedenceSet that = (KeyPrecedenceSet) o;
            return Objects.equals(set, that.set);
        }

        @Override
        public int hashCode() {
            return Objects.hash(set);
        }
    }

    public interface KeyPrecedence extends Comparable<KeyPrecedence> {
        /**
         * @return comparable type info. of the precedence
         */
        int getPrecedenceType();

        /**
         * @return a default precedence of this type, used as compared opponent if no one in a set
         */
        KeyPrecedence getNone();

        default boolean equalsDefault(Object o) {
            return o.getClass().equals(getClass()) &&
                    compareTo((KeyPrecedence) o) == 0;
        }
    }

    public static class KeyPrecedenceDepth implements KeyPrecedence {
        protected int depth;

        public KeyPrecedenceDepth(int depth) {
            this.depth = depth;
        }

        public KeyPrecedenceDepth getNone() {
            return new KeyPrecedenceDepth(Integer.MAX_VALUE);
        }

        @Override
        public int getPrecedenceType() {
            return PRECEDENCE_TYPE_DEPTH;
        }

        @Override
        public int compareTo(KeyPrecedence o) {
            if (o instanceof KeyPrecedenceDepth) {
                return Integer.compare(depth, ((KeyPrecedenceDepth) o).depth);
            }
            return Integer.compare(getPrecedenceType(), o.getPrecedenceType());
        }

        @Override
        public String toString() {
            return "depth(" + depth + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPrecedenceType(), depth);
        }

        @Override
        public boolean equals(Object obj) {
            return equalsDefault(obj);
        }
    }

    public static class KeyPrecedenceFlag implements KeyPrecedence {
        protected int type;
        protected boolean flag;

        public KeyPrecedenceFlag(int type) {
            this(type, true);
        }

        public KeyPrecedenceFlag(int type, boolean flag) {
            this.type = type;
            this.flag = flag;
        }

        @Override
        public KeyPrecedence getNone() {
            return new KeyPrecedenceFlag(type, false);
        }

        @Override
        public int getPrecedenceType() {
            return type;
        }

        @Override
        public int compareTo(KeyPrecedence o) {
            if (o instanceof KeyPrecedenceFlag) {
                int p = Integer.compare(type, ((KeyPrecedenceFlag) o).type);
                if (p == 0) {
                    return Boolean.compare(flag, ((KeyPrecedenceFlag) o).flag);
                } else {
                    return p;
                }
            } else {
                return Integer.compare(getPrecedenceType(), o.getPrecedenceType());
            }
        }

        @Override
        public String toString() {
            return "flag(" + type + "," + flag + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPrecedenceType(), flag);
        }

        @Override
        public boolean equals(Object obj) {
            return equalsDefault(obj);
        }
    }
}
