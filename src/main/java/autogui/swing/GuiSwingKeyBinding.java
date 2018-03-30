package autogui.swing;

import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.security.KeyStore;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GuiSwingKeyBinding {

    protected Map<KeyStroke, Map<Integer,List<KeyStrokeAction>>> keyToDepthToActions = new HashMap<>();
    protected List<KeyStrokeAction> assigned = new ArrayList<>();
    protected Set<KeyStroke> assignedKeys = new HashSet<>();

    public static KeyStroke getKeyStroke(String key) {
        if (key.isEmpty()) {
            return null;
        } else {
            KeyStroke s = KeyStroke.getKeyStroke(key);
            return copyWithModifiers(s, s.getModifiers() | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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
        traverseKeyBinding(component, 0);
        assign();
        bindTo(component);
    }


    public void traverseKeyBinding(Component c, int depth) {
        if (c instanceof GuiSwingView.ValuePane<?>) {
            List<PopupCategorized.CategorizedMenuItem> items = ((GuiSwingView.ValuePane<?>) c).getSwingStaticMenuItems();
            for (PopupCategorized.CategorizedMenuItem item : items) {
                if (item instanceof GuiSwingView.RecommendedKeyStroke) {
                    putItemRecommended((GuiSwingView.ValuePane<?>) c, depth, (GuiSwingView.RecommendedKeyStroke) item);
                }
            }

            KeyStroke paneStroke = getKeyStroke(
                    ((GuiSwingView.ValuePane<?>) c).getSwingViewContext().getAcceleratorKeyStroke());
            if (paneStroke != null) {
                putKeyStroke((GuiSwingView.ValuePane<?>) c, depth, paneStroke);
            }
        }
        if (c instanceof Container) {
            for (Component sub : ((Container) c).getComponents()) {
                traverseKeyBinding(sub, depth + 1);
            }
        }
    }

    public void assign() {
        while (!keyToDepthToActions.isEmpty()) {
            List<Map.Entry<KeyStroke, Map<Integer,List<KeyStrokeAction>>>> next = new ArrayList<>(keyToDepthToActions.entrySet());
            next.sort(Comparator.comparing(k ->
                    Integer.bitCount(k.getKey().getModifiers())));
            if (next.stream()
                    .noneMatch(e -> bind(e.getKey(), e.getValue()))) {
                break;
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

        if (!hasStroke && item instanceof GuiSwingView.RecommendedKeyStroke) {
            putItemRecommended(pane, depth, (GuiSwingView.RecommendedKeyStroke) item);
        }
        return hasStroke;
    }

    public boolean putItemRecommended(GuiSwingView.ValuePane<?> pane, int depth, GuiSwingView.RecommendedKeyStroke item) {
        KeyStroke stroke = item.getRecommendedKeyStroke();
        if (stroke != null) {
            putKeyStroke(new KeyStrokeAction(pane, (PopupCategorized.CategorizedMenuItem) item,
                    (item instanceof Action ? (Action) item : null), depth, stroke));
            return true;
        } else {
            return false;
        }
    }


    public boolean putKeyStroke(GuiSwingView.ValuePane<?> pane, int depth,
                                PopupCategorized.CategorizedMenuItem i, Action item) {
        KeyStroke stroke = (item == null ? null : (KeyStroke) item.getValue(Action.ACCELERATOR_KEY));
        if (stroke != null) {
            keyToDepthToActions.computeIfAbsent(stroke, s -> new HashMap<>())
                    .computeIfAbsent(depth, d -> new ArrayList<>())
                    .add(new KeyStrokeAction(pane, i, item, depth, stroke));
            return true;
        } else {
            return false;
        }
    }

    public boolean putKeyStroke(GuiSwingView.ValuePane<?> pane, int depth, KeyStroke stroke) {
        if (stroke != null) {
            keyToDepthToActions.computeIfAbsent(stroke, s -> new HashMap<>())
                    .computeIfAbsent(depth, d -> new ArrayList<>())
                    .add(new KeyStrokeAction(pane, null, null, depth, stroke));
            return true;
        } else {
            return false;
        }
    }

    public static class KeyStrokeAction {
        public GuiSwingView.ValuePane<?> pane;
        public PopupCategorized.CategorizedMenuItem item;
        public Action action;
        public int depth;
        public KeyStroke stroke;
        protected boolean assigned;

        protected KeyStrokeAction base;
        protected List<KeyStrokeAction> derived;

        public KeyStrokeAction(GuiSwingView.ValuePane<?> pane, PopupCategorized.CategorizedMenuItem item,
                               Action action, int depth, KeyStroke originalStroke) {
            this.pane = pane;
            this.item = item;
            this.action = action;
            this.depth = depth;
            this.stroke = originalStroke;
        }

        public void updateTooltip() {
            String newDesc = getKeyStrokeString();
            if (action != null) {
                String desc = (String) action.getValue(Action.SHORT_DESCRIPTION);
                if (desc != null) {
                    newDesc = desc + " " + newDesc;
                }
                action.putValue(Action.SHORT_DESCRIPTION, newDesc);
            } else if (pane != null) {
                JComponent comp = pane.asSwingViewComponent();
                if (comp.getToolTipText() != null) {
                    newDesc = comp.getToolTipText() + " " + newDesc;
                }
                comp.setToolTipText(newDesc);
            }
        }

        public String getKeyStrokeString() {
            String alt = "Alt";
            String meta = "Meta";
            String ctrl = "Ctrl";
            String shift = "Shift";
            boolean mac = System.getProperty("os.name", "").contains("Mac OS");
            if (mac) {
                alt = "\u2325";
                meta = "\u2318";
                ctrl = "\u2303";
                shift = "\u21E7";
            }
            int mod = stroke.getModifiers();
            List<String> words = new ArrayList<>();
            if ((mod & KeyEvent.CTRL_DOWN_MASK) != 0 ||
                    (mod & KeyEvent.CTRL_MASK) != 0) {
                words.add(ctrl);
            }
            if ((mod & KeyEvent.ALT_DOWN_MASK) != 0 ||
                    (mod & KeyEvent.ALT_MASK) != 0) {
                words.add(alt);
            }
            if ((mod & KeyEvent.SHIFT_DOWN_MASK) != 0 ||
                    (mod & KeyEvent.SHIFT_MASK) != 0) {
                words.add(shift);
            }
            if ((mod & KeyEvent.META_DOWN_MASK) != 0 ||
                    (mod & KeyEvent.META_MASK) != 0) {
                words.add(meta);
            }
            if (stroke.getKeyCode() != KeyEvent.VK_UNDEFINED) {
                words.add(KeyEvent.getKeyText(stroke.getKeyCode()));
            } else {
                words.add(Character.toString(stroke.getKeyChar()));
            }
            if (mac) {
                return String.join("", words);
            } else {
                return String.join("+", words);
            }
        }

        public void process() {
            show(pane.asSwingViewComponent());
            pane.asSwingViewComponent().requestFocusInWindow();
            if (action != null) {
                action.actionPerformed(null);
            }
        }

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

            System.err.println(c.getClass().getSimpleName() + " : show=" + c.isShowing() + ", visi=" + c.isVisible());

        }

        public boolean isAssigned() {
            return assigned;
        }

        public void setAssigned(boolean assigned) {
            getBase().setAssignedToChild(assigned);
        }

        public KeyStrokeAction getBase() {
            KeyStrokeAction b = this;
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

        public Set<KeyStrokeAction> derive() {
            int mods = stroke.getModifiers();
            List<Integer> used = getModifiers().stream()
                    .filter(i -> (i & mods) != 0)
                    .collect(Collectors.toList());
            List<Set<Integer>> available = getModifierPowerSet().stream()
                    .filter(s -> used.stream().noneMatch(s::contains))
                    .collect(Collectors.toList());

            return available.stream()
                    .map(s -> toModifiers(mods, s))
                    .map(mod -> copyWithModifiers(stroke, mod))
                    .map(k -> connectToChild(new KeyStrokeAction(pane, item, action, depth, k)))
                    .collect(Collectors.toSet());
        }

        private int toModifiers(int baseMod, Set<Integer> mods) {
            return mods.stream()
                    .reduce(baseMod, (pre,next) -> pre | next);
        }

        public KeyStrokeAction connectToChild(KeyStrokeAction child) {
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
                    ", depth=" + depth +
                    ", assigned=" + assigned +
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
                        InputEvent.ALT_DOWN_MASK,
                        InputEvent.META_DOWN_MASK));
                //InputEvent.ALT_GRAPH_DOWN_MASK);
                modifiers.remove((Object) getMenuShortcutKeyMask());
            }
            return modifiers;
        }

        public static int getMenuShortcutKeyMask() {
            int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            int menuDownMask = menuMask;
            switch (menuMask) {
                case Event.SHIFT_MASK:
                    menuDownMask = InputEvent.SHIFT_DOWN_MASK;
                    break;
                case Event.CTRL_MASK:
                    menuDownMask = InputEvent.CTRL_DOWN_MASK;
                    break;
                case Event.ALT_MASK:
                    menuDownMask = InputEvent.ALT_DOWN_MASK;
                    break;
                case Event.META_MASK:
                    menuDownMask = InputEvent.META_DOWN_MASK;
                    break;
            }
            return menuDownMask;
        }
    }


    public void putKeyStroke(KeyStrokeAction item) {
        keyToDepthToActions.computeIfAbsent(item.stroke, s -> new HashMap<>())
                .computeIfAbsent(item.depth, d -> new ArrayList<>())
                .add(item);
    }


    public boolean bind(KeyStroke key, Map<Integer,List<KeyStrokeAction>> entry) {
        if (!assignedKeys.contains(key)) {
            List<Integer> depth = new ArrayList<>(entry.keySet());
            depth.sort(Comparator.reverseOrder());
            int d = depth.remove(0); //deepest item for the key
            List<KeyStrokeAction> actions = entry.get(d);
            KeyStrokeAction assignedAction = actions.remove(0);

            assigned.add(assignedAction);
            assignedKeys.add(key);
            assignedAction.setAssigned(true);

            new ArrayList<>(entry.values()).stream()
                    .flatMap(List::stream)
                    .filter(a -> !a.isAssigned())
                    .flatMap(a -> a.derive().stream())
                    .filter(ak -> !assignedKeys.contains(ak.stroke))
                    .forEach(this::putKeyStroke);
            keyToDepthToActions.remove(key);

            new ArrayList<>(keyToDepthToActions.entrySet()).forEach(e -> {
                new ArrayList<>(e.getValue().entrySet()).forEach(e2 -> {
                    e2.getValue().removeIf(KeyStrokeAction::isAssigned);
                    if (e2.getValue().isEmpty()) {
                        e.getValue().remove(e2.getKey());
                    }
                });
                if (e.getValue().isEmpty()) {
                    keyToDepthToActions.remove(e.getKey());
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public void bindTo(JComponent component) {
        Map<KeyStroke, KeyStrokeAction> inputMap = new HashMap<>();
        for (KeyStrokeAction action : assigned) {
            action.updateTooltip();
            if (action.action != null) {
                inputMap.put(action.stroke, action);
            }
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            KeyStrokeAction action = inputMap.get(KeyStroke.getKeyStrokeForEvent(e));
            if (action != null) {
                action.process();
                return true;
            } else {
                return false;
            }
        });
    }

}
