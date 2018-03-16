package autogui.swing.util;

import javax.swing.*;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * a popup menu manager.
 * it support showing the popup menu supplied by {@link #menu} via the following event listeners:
 * <ul>
 *     <li>{@link MouseListener} </li>
 *     <li>{@link KeyListener}  with {@link #keyMatcher}, can be {@link #getDefaultKeyMatcher()}</li>
 *     <li>{@link ActionListener} caused by {@link #getAction()} </li>
 * </ul>
 * {@link #menuBuilder} determines menu items in the popup menu via {@link PopupMenuBuilder#buildWithClear(PopupExtensionSender, JPopupMenu)}.
 *
 */
public class PopupExtension implements MouseListener, KeyListener, ActionListener, PopupExtensionSender {
    /** the supplier is frequently called and expected to return same menu object */
    protected Supplier<JPopupMenu> menu;
    protected JComponent pane;
    protected Predicate<KeyEvent> keyMatcher;
    protected PopupMenuBuilder menuBuilder;

    protected Action action;

    /** the interface for constructing menus */
    public interface PopupMenuBuilder {
        /**
         * the Consumer accepts
         *  <ul>
         *    <li>{@link Action}
         *        including {@link JMenu} and  {@link JPopupMenu}</li>
         *    <li>{@link JMenuItem}</li>
         *    <li>{@link JComponent}</li>
         *  </ul>
         * @param sender the sender extension
         * @param menu the target for appending menus
         */
        void build(PopupExtensionSender sender, Consumer<Object> menu);

        /** the default behavior reconstruct entire items by {@link #build(PopupExtensionSender, Consumer)}.
         *    The Consumer can append an item to the menu.
         * @param sender the sender extension
         * @param menu the target for appending menus
         */
        default void buildWithClear(PopupExtensionSender sender, JPopupMenu menu) {
            menu.removeAll();
            build(sender, new MenuBuilder.MenuAppender(menu));
            menu.revalidate();
        }
    }

    /** call {@link #PopupExtension(JComponent, Predicate, PopupMenuBuilder, Supplier)}
     * @param pane the host pane of the popup-menu, can be null
     * @param keyMatcher matching with a key-event for displaying the menu
     * @param menuBuilder a builder for constructing menu-items
     * @param menu the displayed popup-menu, can be null
     */
    public PopupExtension(JComponent pane, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder menuBuilder, JPopupMenu menu) {
        this(pane, keyMatcher, menuBuilder,
                menu == null ? new DefaultPopupGetter(pane) : () -> menu);
    }

    /** add this to pane as {@link KeyListener} and {@link MouseListener}
     * @param pane the host pane of the popup-menu, can be null
     * @param keyMatcher matching with a key-event for displaying the menu
     * @param menuBuilder a builder for constructing menu-items
     * @param menu the supplier of the displayed popup-menu
     */
    public PopupExtension(JComponent pane, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder menuBuilder, Supplier<JPopupMenu> menu) {
        this.pane = pane;
        this.keyMatcher = keyMatcher;
        this.menuBuilder = menuBuilder;
        this.menu = menu;
        if (pane != null) {
            addListenersToPane();
        }
    }

    /**
     * call {@link #PopupExtension(JComponent, Predicate, PopupMenuBuilder, Supplier)}.
     * the menu is supplied by the pane's {@link JComponent#getComponentPopupMenu()} or creating a new one if it returns null.
     *   it also call {@link MenuKeySelector#addToMenu(JPopupMenu)} for incremental item search while showing the popup menu
     * @param pane the host pane of the popup-menu, can be null
     * @param keyMatcher  matching with a key-event for displaying the menu
     * @param menuBuilder a builder for constructing menu-items
     */
    public PopupExtension(JComponent pane, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder menuBuilder) {
        this(pane, keyMatcher, menuBuilder, new DefaultPopupGetter(pane));
        new MenuKeySelector().addToMenu(menu.get());
        //new PopupMenuHidingFix(menu);
    }

    public void addListenersToPane() {
        addListenersTo(pane);
    }

    public void addListenersTo(JComponent pane) {
        pane.addMouseListener(this);
        pane.addKeyListener(this);
    }



    ////////////////


    public static Predicate<KeyEvent> getDefaultKeyMatcher() {
        return PopupExtensionText.getKeyCode(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)
                .or(PopupExtensionText.getKeyCode(KeyEvent.VK_F5, 0));
    }

    /**
     * @param keyCode a key code such as KeyEvent.VK_SPACE
     * @param modifiersEx extended modifiers such as KeyEvent.CTRL_DOWN_MASK, or 0
     * @return a predicate matching with the parameters
     */
    public static PredicateKeyMatcher getKeyCode(int keyCode, int modifiersEx) {
        return new PredicateKeyMatcher(keyCode, modifiersEx);
    }

    /** keyCode and modifierEx: ALT_DOWN_MASK, ... */
    public static class PredicateKeyMatcher implements Predicate<KeyEvent> {
        protected int keyCode;
        protected int modifiersEx;

        public PredicateKeyMatcher(int keyCode, int modifiersEx) {
            this.keyCode = keyCode;
            this.modifiersEx = modifiersEx;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public int getModifiersEx() {
            return modifiersEx;
        }

        @Override
        public boolean test(KeyEvent keyEvent) {
            return keyEvent.getKeyCode() == keyCode &&
                    (modifiersEx == 0 || (keyEvent.getModifiersEx() & modifiersEx) != 0);
        }
    }

    //////////////////

    /** a default popup-menu factory, obtaining a component or a creating a new popup */
    public static class DefaultPopupGetter implements Supplier<JPopupMenu> {
        protected JComponent component;
        protected JPopupMenu menu;

        public DefaultPopupGetter(JComponent component) {
            this.component = component;
        }

        @Override
        public JPopupMenu get() {
            if (menu == null) {
                if (component != null) {
                    menu = component.getComponentPopupMenu();
                }
                if (menu == null) {
                    menu = new JPopupMenu();
                }
            }
            return menu;
        }
    }

    public Predicate<KeyEvent> getKeyMatcher() {
        return keyMatcher;
    }

    public JPopupMenu getMenu() {
        return menu.get();
    }

    public PopupMenuBuilder getMenuBuilder() {
        return menuBuilder;
    }

    public void setMenuBuilder(PopupMenuBuilder menuBuilder) {
        this.menuBuilder = menuBuilder;
    }

    public JComponent getPane() {
        return pane;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyMatcher != null && keyMatcher.test(e)) {
            e.consume();
            Component comp = e.getComponent();
            if (comp == null) {
                comp = pane;
            }
            showByKey(e, comp);
        }
    }

    public void showByKey(KeyEvent e, Component comp) {
        show(comp);
    }


    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        keyPressed(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            e.consume();
            show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src instanceof Component) {
            show((Component) src);
        } else {
            show(pane);
        }
    }

    public Action getAction() {
        if (action == null) {
            action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PopupExtension.this.actionPerformed(e);
                }
            };
        }
        return action;
    }

    ////////////////

    public void show(Component comp) {
        int x = comp.getWidth() / 2;
        int y = Math.min(64, comp.getHeight());
        show(comp, x, y);
    }

    public void show(Component comp, int x, int y) {
        setupMenu();
        menu.get().show(comp, x, y);
    }

    public void setupMenu() {
        menuBuilder.buildWithClear(this, menu.get());
    }

    /////////////////

    /**
     * a menu-selector by incremental key-typing
     * <pre>
     *     new MenuKeySelector().addToMenu(popupMenu);
     * </pre>
     * <p>
     *  select menu items by key types
     *  <ul>
     *      <li> any displayable key : put it into the buffer and select a next item.
     *         if no item is found, search from the top
     *      <li> backspace, delete: clear the buffer
     *      <li> tab: no change for the buffer and search a next item
     *      <li> newline, arrow-keys, ... : no consumption of the key event
     *  </ul>
     * <p>
     *  The order of the search is
     *     children of the current item,
     *     rest sibling items,
     *     children of rest sibling items (recursion) ... ,
     *     upper rest items,
     *     children of upper rest items (recursion), ...
     *  <p>
     *      Matching : it uses startsWith for JMenuItem's getText()
     */
    public static class MenuKeySelector implements MenuKeyListener, PopupMenuListener {
        protected StringBuilder buffer = new StringBuilder();

        public void addToMenu(JPopupMenu menu) {
            menu.addMenuKeyListener(this);
            menu.addPopupMenuListener(this);
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            clearBuffer();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) { }

        @Override
        public void menuKeyTyped(MenuKeyEvent e) { }

        @Override
        public void menuKeyReleased(MenuKeyEvent e) { }

        @Override
        public void menuKeyPressed(MenuKeyEvent e) {
            run(e);
        }

        public void clearBuffer() {
            if (buffer.length() > 0) {
                buffer.delete(0, buffer.length());
            }
        }

        public void run(MenuKeyEvent e) {
            if (processKeyInput(e)) {
                String str = buffer.toString().toLowerCase();
                processTarget(e.getMenuSelectionManager(), str);
                e.consume();
            }
        }

        public boolean processKeyInput(MenuKeyEvent e) {
            int code = e.getKeyCode();
            char c = e.getKeyChar();
            boolean noModifiers = e.getModifiersEx() == 0;
            if ((code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) && noModifiers) {
                clearBuffer();
                return true;
            } else if (c == ' ' && noModifiers) {
                return true;
            } else if (c <= 0x1F) {
                return false;
            } else {
                if (c != KeyEvent.CHAR_UNDEFINED && noModifiers) {
                    buffer.append(c);
                    return true;
                } else {
                    return false;
                }
            }
        }

        public void processTarget(MenuSelectionManager manager, String str) {
            MenuElement[] elements = manager.getSelectedPath();
            if (elements == null || elements.length == 0) {
                return;
            }

            if (str.isEmpty()) {
                return;
            }

            MenuElement[] found = searchDown(elements, elements[elements.length - 1].getSubElements(), null, str);
            if (found == null) {
                found = searchUp(elements, str);
            }
            //loop
            if (found == null) {
                MenuElement current = elements[elements.length - 1];
                found = searchDown(new MenuElement[] {elements[0]}, elements[0].getSubElements(), current, str);
            }

            if (found != null && found.length > 0) {
                processFound(manager, found, str);
            }
        }

        public void processFound(MenuSelectionManager manager, MenuElement[] found, String str) {
            manager.setSelectedPath(found);
        }

        public MenuElement[] searchDown(MenuElement[] path, MenuElement[] children, MenuElement stopAt, String str) {
            if (children == null) {
                return null;
            }
            for (MenuElement e : children) {
                if (stopAt != null && e.equals(stopAt)) {
                    return new MenuElement[0];
                } else if (match(e, str)) {
                    return child(path, e);
                }
            }

            for (MenuElement e : children) {
                MenuElement[] childPath = child(path, e);
                MenuElement[] found = searchDown(childPath, e.getSubElements(), stopAt, str);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        private MenuElement[] child(MenuElement[] path, MenuElement e) {
            MenuElement[] childPath = Arrays.copyOf(path, path.length + 1);
            childPath[childPath.length - 1] = e;
            return childPath;
        }

        private MenuElement[] sibling(MenuElement[] path, MenuElement e) {
            MenuElement[] childPath = Arrays.copyOf(path, path.length);
            childPath[childPath.length - 1] = e;
            return childPath;
        }

        public MenuElement[] searchUp(MenuElement[] path, String str) {
            if (path.length <= 1) {
                return null;
            }
            MenuElement e = path[path.length - 1];
            MenuElement parent = path[path.length - 2];
            List<MenuElement> children = Arrays.asList(parent.getSubElements());
            int start = children.indexOf(e);
            start++;
            for (int i = start; i < children.size(); ++i) {
                MenuElement next = children.get(i);
                if (match(next, str)) {
                    return sibling(path, next);
                }
            }

            for (int i = start; i < children.size(); ++i) {
                MenuElement next = children.get(i);
                MenuElement[] found = searchDown(sibling(path, next), next.getSubElements(), null, str);
                if (found != null) {
                    return found;
                }
            }

            return searchUp(Arrays.copyOf(path, path.length - 1), str);
        }

        public boolean match(MenuElement e, String str) {
            Component comp = e.getComponent();
            if (comp instanceof JMenuItem) {
                String text = ((JMenuItem) comp).getText();
                return matchText(text, str);
            } else {
                return false;
            }
        }

        private Pattern spaces = Pattern.compile(" +");

        public boolean matchText(String text, String str) {
            String low = text.toLowerCase();
            if (low.startsWith(str)) {
                return true;
            } else if (low.contains(" ")) {
                String shrink = spaces.matcher(low).replaceAll("");
                return shrink.startsWith(str);
            } else {
                return false;
            }
        }
    }


    /** a window-listener and popup-menu listener for fixing the behavior of showing/hiding a popup menu */
    public class PopupMenuHidingFix extends WindowAdapter implements PopupMenuListener {
        protected JPopupMenu menu;

        public PopupMenuHidingFix(JPopupMenu menu) {
            this.menu = menu;
            menu.addPopupMenuListener(this);
        }

        public JPopupMenu getMenu() {
            return menu;
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            menu.setVisible(false);
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            Window win = getWindow(e.getSource());
            if (win != null) {
                win.removeWindowListener(this);
            }
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            Window win = getWindow(e.getSource());
            if (win != null) {
                win.addWindowListener(this);
            }
        }

        public Window getWindow(Object comp) {
            if (comp == null) {
                return null;
            } else if (comp instanceof JPopupMenu) {
                return getWindow(((JPopupMenu) comp).getInvoker());
            } else if (comp instanceof Window) {
                return (Window) comp;
            } else if (comp instanceof JRootPane) {
                return getWindow(((JRootPane) comp).getParent());
            } else if (comp instanceof JComponent) {
                return getWindow(((JComponent) comp).getRootPane());
            } else if (comp instanceof Component) {
                return getWindow(((Component) comp).getParent());
            } else {
                return null;
            }
        }
    }

}
