package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Predicate;

public class PopupExtension implements MouseListener, KeyListener, ActionListener {
    protected JPopupMenu menu;
    protected JComponent pane;
    protected Predicate<KeyEvent> keyMatcher;
    protected PopupMenuBuilder menuBuilder;

    protected Action action;

    public interface PopupMenuBuilder {
        void build(PopupExtension sender, JPopupMenu menu);
    }

    public PopupExtension(JComponent pane, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder menuBuilder, JPopupMenu menu) {
        this.pane = pane;
        this.keyMatcher = keyMatcher;
        this.menuBuilder = menuBuilder;
        this.menu = menu;
        if (pane != null) {
            addListenersToPane();
        }
    }

    public PopupExtension(JComponent pane, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder menuBuilder) {
        this(pane, keyMatcher, menuBuilder, new JPopupMenu());
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


    public Predicate<KeyEvent> getKeyMatcher() {
        return keyMatcher;
    }

    public JPopupMenu getMenu() {
        return menu;
    }

    public PopupMenuBuilder getMenuBuilder() {
        return menuBuilder;
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
            show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

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
        menu.show(comp, x, y);
    }

    public void setupMenu() {
        menuBuilder.build(this, menu);
    }
}
