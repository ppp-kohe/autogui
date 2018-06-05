package autogui.swing.util;

import autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * {@link #menuBuilder} determines menu items in the popup menu via {@link PopupMenuBuilder#buildWithClear(PopupMenuFilter, JPopupMenu)}.
 *
 */
public class PopupExtension implements MouseListener, KeyListener, ActionListener {
    public static String MENU_CATEGORY_UNDO = MenuBuilder.getCategoryImplicit("Undo");
    public static String MENU_CATEGORY_EDIT = MenuBuilder.getCategoryImplicit("Edit");
    public static String MENU_CATEGORY_SET = MenuBuilder.getCategoryImplicit("Set");
    public static String MENU_CATEGORY_JUMP = MenuBuilder.getCategoryImplicit("Jump");
    public static String MENU_CATEGORY_SELECT = MenuBuilder.getCategoryImplicit("Select");
    public static String MENU_CATEGORY_TRANSFER = MenuBuilder.getCategoryImplicit("Transfer");
    public static String MENU_CATEGORY_VIEW = MenuBuilder.getCategoryImplicit("View");
    public static String MENU_CATEGORY_PREFS = MenuBuilder.getCategoryImplicit("Preferences");
    public static String MENU_CATEGORY_WINDOW = MenuBuilder.getCategoryImplicit("Window");

    public static String MENU_SUB_CATEGORY_NEW = "new";

    ///undo
    public static String MENU_SUB_CATEGORY_UNDO = "undo";
    public static String MENU_SUB_CATEGORY_REDO = "redo";

    ///edit
    public static String MENU_SUB_CATEGORY_CUT = "cut";
    public static String MENU_SUB_CATEGORY_COPY = "copy";
    public static String MENU_SUB_CATEGORY_PASTE = "paste";
    public static String MENU_SUB_CATEGORY_DELETE = "delete";

    ///select
    public static String MENU_SUB_CATEGORY_SELECT = "select";

    ///transfer
    public static String MENU_SUB_CATEGORY_IMPORT = "import";
    public static String MENU_SUB_CATEGORY_EXPORT = "export";

    //prefs
    public static String MENU_SUB_CATEGORY_PREFS_CHANGE = "prefsChange";
    public static String MENU_SUB_CATEGORY_PREFS_WINDOW = "prefsWindow";

    //window
    public static String MENU_SUB_CATEGORY_WINDOW_VIEW = "windowView";
    public static String MENU_SUB_CATEGORY_WINDOW_SELECT = "windowSelect";

    /** the supplier is frequently called and expected to return same menu object */
    protected Supplier<JPopupMenu> menu;
    protected PopupMenuFilter filter = MENU_FILTER_IDENTITY;
    protected JComponent pane;
    protected Predicate<KeyEvent> keyMatcher;
    protected PopupMenuBuilder menuBuilder;
    protected PopupCancelChecker cancelChecker;

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
         * @param filter an item filter
         * @param menu the target for appending menus
         */
        void build(PopupMenuFilter filter, Consumer<Object> menu);

        /** the default behavior reconstruct entire items by {@link #build(PopupMenuFilter, Consumer)}.
         *    The Consumer can append an item to the menu.
         * @param filter an item filter
         * @param menu the target for appending menus
         */
        default void buildWithClear(PopupMenuFilter filter, JPopupMenu menu) {
            menu.removeAll();
            build(filter, new MenuBuilder.MenuAppender(menu));
            menu.revalidate();
        }
    }

    public static class PopupMenuBuilderEmpty implements PopupMenuBuilder {
        @Override
        public void build(PopupMenuFilter filter, Consumer<Object> menu) {
            filter.aroundItems(true).forEach(menu);
            filter.aroundItems(false).forEach(menu);
        }
    }

    public interface PopupMenuFilter {
        /**
         * @param item a converted item, one of {@link JComponent} (including {@link JMenuItem}), {@link Action},
         *             {@link autogui.swing.table.TableTargetCellAction}
         *             or {@link autogui.swing.util.PopupCategorized.CategorizedMenuItem}
         * @return a converted item or null if not matched
         */
        Object convert(Object item);

        default <T> List<T> convertItems(Class<T> type, Iterable<? extends T> items) {
            ArrayList<T> result = new ArrayList<>();
            for (Object item : items) {
                Object r = convert(item);
                if (r != null) {
                    result.add(type.cast(r));
                }
            }
            return result;
        }

        /**
         * a menu-builder calls the method before and after of a building process and appends returned items
         * @param before true if before of a building process or false if after of the process.
         * @return additional menu items provided by the filter. it needs to return once for the same item list
         */
        default List<Object> aroundItems(boolean before) {
            return Collections.emptyList();
        }
    }

    public static PopupMenuFilterAsIs MENU_FILTER_IDENTITY = new PopupMenuFilterAsIs();

    public static class PopupMenuFilterAsIs implements PopupMenuFilter {
        @Override
        public Object convert(Object item) {
            return item;
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
        setupCancelChecker();
    }

    public void setFilter(PopupMenuFilter filter) {
        this.filter = filter;
    }

    public PopupMenuFilter getFilter() {
        return filter;
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

    /**
     * call {@link #PopupExtension(JComponent, Predicate, PopupMenuBuilder)} with {@link #getDefaultKeyMatcher()}
     * @param pane the host pane of the popup-menu, can be null
     * @param menuBuilder a builder for constructing menu-items
     */
    public PopupExtension(JComponent pane, PopupMenuBuilder menuBuilder) {
        this(pane, getDefaultKeyMatcher(), menuBuilder);
    }

    protected void setupCancelChecker() {
        JPopupMenu m = menu.get();
        //reuse existing checker
        for (PopupMenuListener l : m.getPopupMenuListeners()) {
            if (l instanceof PopupCancelChecker) {
                PopupCancelChecker existingChecker = (PopupCancelChecker) l;
                if (existingChecker.isReusable()) {
                    cancelChecker = existingChecker;
                }
            }
        }
        if (cancelChecker == null) {
            cancelChecker = new PopupCancelChecker();
            m.addPopupMenuListener(cancelChecker);
        }
    }


    /** improve default behavior of showing popup menu: clicking a menu button while the menu is visible can hide it  */
    public static class PopupCancelChecker implements PopupMenuListener {
        protected Instant lastCancelTime = Instant.EPOCH;
        protected Duration limit = Duration.ofMillis(500);

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            lastCancelTime = Instant.now();
        }

        public boolean isReusable() {
            return true;
        }

        public boolean isValidMenuOpen() {
            Duration diff = Duration.between(lastCancelTime, Instant.now());
            return diff.compareTo(limit) >= 0;
        }
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
                .or(PopupExtensionText.getKeyCode(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK))
                .or(PopupExtensionText.getKeyCode(KeyEvent.VK_ENTER, getMenuShortcutKeyMask()))
                .or(PopupExtensionText.getKeyCode(KeyEvent.VK_F5, 0));
    }

    public static KeyStroke getDefaultKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
    }

    @SuppressWarnings("deprecation")
    public static int getMenuShortcutKeyMask() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        int menuDownMask = menuMask;
        switch (menuMask) {
            case InputEvent.SHIFT_MASK:
                menuDownMask = InputEvent.SHIFT_DOWN_MASK;
                break;
            case InputEvent.CTRL_MASK:
                menuDownMask = InputEvent.CTRL_DOWN_MASK;
                break;
            case InputEvent.ALT_MASK:
                menuDownMask = InputEvent.ALT_DOWN_MASK;
                break;
            case InputEvent.META_MASK:
                menuDownMask = InputEvent.META_DOWN_MASK;
                break;
        }
        return menuDownMask;
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
        if (!cancelChecker.isValidMenuOpen()) {
            return;
        }
        Object src = e.getSource();
        if (src instanceof Component) {
            show((Component) src);
        } else {
            show(pane);
        }
    }

    public Action getAction() {
        if (action == null) {
            action = new PopupExtensionDisplayAction(this);
        }
        return action;
    }

    public static class PopupExtensionDisplayAction extends AbstractAction {
        protected PopupExtension extension;

        public PopupExtensionDisplayAction(PopupExtension extension) {
            this.extension = extension;
            UIManagerUtil ui = UIManagerUtil.getInstance();
            putValue(Action.LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("search-", "pulldown",
                    ui.getScaledSizeInt(16), ui.getScaledSizeInt(10)));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            extension.actionPerformed(e);
        }
    }



    ////////////////

    public void show(Component comp) {
        int x = comp.getWidth() / 2;

        int y = Math.min(UIManagerUtil.getInstance().getScaledSizeInt(64), comp.getHeight());
        show(comp, x, y);
    }

    public void show(Component comp, int x, int y) {
        setupMenu();
        menu.get().show(comp, x, y);
    }

    public void setupMenu() {
        menuBuilder.buildWithClear(filter, menu.get());
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
