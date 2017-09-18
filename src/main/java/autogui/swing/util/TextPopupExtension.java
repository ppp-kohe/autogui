package autogui.swing.util;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TextPopupExtension implements KeyListener, MouseListener {
    protected JTextComponent textComponent;
    protected Predicate<KeyEvent> keyMatcher;
    protected PopupRunner popupRunner;

    public static TextPopupExtension installDefault(JTextComponent textComponent) {
        return new TextPopupExtension(textComponent,
                getDefaultKeyMatcher(),
                getServiceDefaultMenu(textComponent));
    }

    public static Predicate<KeyEvent> getDefaultKeyMatcher() {
        return TextPopupExtension.getKeyCode(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)
                .or(TextPopupExtension.getKeyCode(KeyEvent.VK_F5, 0));
    }

    public interface PopupRunner {
        void show(Component component, int x, int y);
    }

    public TextPopupExtension(JTextComponent textComponent, Predicate<KeyEvent> keyMatcher, PopupRunner runner) {
        this.textComponent = textComponent;
        this.keyMatcher = keyMatcher;
        this.popupRunner = runner;
        if (textComponent != null) {
            addListenersToTextComponent();
        }
    }

    /** called from constructor */
    public void addListenersToTextComponent() {
        textComponent.addKeyListener(this);
        textComponent.addMouseListener(this);
    }

    public JTextComponent getTextComponent() {
        return textComponent;
    }

    public Predicate<KeyEvent> getKeyMatcher() {
        return keyMatcher;
    }

    public PopupRunner getPopupRunner() {
        return popupRunner;
    }

    public void setTextComponent(JTextComponent textComponent) {
        this.textComponent = textComponent;
    }

    public void setKeyMatcher(Predicate<KeyEvent> keyMatcher) {
        this.keyMatcher = keyMatcher;
    }

    public void setPopupRunner(PopupRunner popupRunner) {
        this.popupRunner = popupRunner;
    }

    ////////////////

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyMatcher != null && keyMatcher.test(e)) {
            e.consume();
            int sel = textComponent.getSelectionStart();
            try {
                Rectangle rect = textComponent.modelToView(sel); //TODO in JDK9, replaced by modelToView2D
                popupRunner.show(textComponent, rect.x, rect.y + rect.height);
            } catch (BadLocationException ex) {
                error(ex);
            }
        }
    }

    protected void error(Exception ex) {
        System.err.println("error: " + ex);
    }

    @Override
    public void keyReleased(KeyEvent e) { }
    @Override
    public void keyTyped(KeyEvent e) { }


    @Override
    public void mousePressed(MouseEvent e) {
        if (popupRunner != null && e.isPopupTrigger()) {
            popupRunner.show(e.getComponent(), e.getX(), e.getY());
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

    ////////////////////

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

    /** a {@link PopupRunner} which has typical actions for the text component*/
    public static TextServiceDefaultMenu getServiceDefaultMenu(JTextComponent component) {
        return new TextServiceDefaultMenu(component);
    }

    public static class TextServiceDefaultMenu implements PopupRunner {
        protected JTextComponent textComponent;
        protected JPopupMenu menu;
        protected List<JMenuItem> editActions;

        public TextServiceDefaultMenu(JTextComponent textComponent) {
            this(textComponent, new JPopupMenu());
        }

        public TextServiceDefaultMenu(JTextComponent textComponent, JPopupMenu menu) {
            this.textComponent = textComponent;
            this.menu = menu;
            initEditActions();
        }

        /** called from constructor */
        public void initEditActions() {
            editActions = TextPopupExtension.getEditActions(textComponent).stream()
                    .map(JMenuItem::new)
                    .collect(Collectors.toList());
        }

        public JTextComponent getTextComponent() {
            return textComponent;
        }

        public JPopupMenu getMenu() {
            return menu;
        }

        public List<JMenuItem> getEditActions() {
            return editActions;
        }

        @Override
        public void show(Component component, int x, int y) {
            setupMenu(menu);
            menu.show(component, x, y);
        }

        public void setupMenu(JPopupMenu menu) {
            menu.removeAll();
            MenuBuilder builder = getMenuBuilder();
            builder.addMenuItems(menu, editActions, null);
        }

        public MenuBuilder getMenuBuilder() {
            return MenuBuilder.get();
        }
    }

    /** JDK has standard implementations for cut, copy and paste as DefaultEditorKit.
     *   However, those actions take the target from event source, and do not have user friendly name */
    public static List<Action> getEditActions(JTextComponent textComponent) {
        return Arrays.asList(
                new TextCutAction(textComponent),
                new TextCopyAction(textComponent),
                new TextCopyAllAction(textComponent),
                new TextPasteAction(textComponent),
                new TextSelectAllAction(textComponent));
    }

    ///////////// text actions for a specific target


    public static class TextCutAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCutAction(JTextComponent field) {
            putValue(NAME, "Cut");
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.isEditable() && field.isEnabled() &&
                    field.getSelectionStart() != field.getSelectionEnd();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.cut();
        }
    }

    public static class TextCopyAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCopyAction(JTextComponent field) {
            putValue(NAME, "Copy");
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.getSelectionStart() != field.getSelectionEnd();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.copy();
        }
    }

    public static class TextPasteAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextPasteAction(JTextComponent field) {
            putValue(NAME, "Paste");
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.isEditable() && field.isEnabled();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.paste();
        }
    }

    public static class TextCopyAllAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCopyAllAction(JTextComponent field) {
            putValue(NAME, "Copy Value");
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String data = field.getText();
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection sel = new StringSelection(data);
            board.setContents(sel, sel);
        }
    }

    public static class TextSelectAllAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;

        public TextSelectAllAction(JTextComponent field) {
            putValue(NAME, "Select All");
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.selectAll();
        }
    }
//
//    public static class TextTabStopFitAction extends AbstractAction {
//        private static final long serialVersionUID = 1L;
//        protected JEditorPane text;
//        protected Document lastDoc;
//        protected TextTableLayout tableLayout = new TextTableLayout();
//
//        public TextTabStopFitAction(JTextComponent text) {
//            putValue(NAME, "Fit Tab Stops");
//            if (text instanceof JEditorPane) {
//                this.text = (JEditorPane) text;
//                setEnabled(true);
//            } else {
//                setEnabled(false);
//            }
//            tableLayout.setWidthMargin(10);
//        }
//
//        @Override
//        public boolean isEnabled() {
//            if (text != null && lastDoc != text.getDocument()) {
//                Document d = text.getDocument();
//                lastDoc = d;
//                if (d instanceof DocumentWrapper) {
//                    d = ((DocumentWrapper) d).getDoc();
//                }
//                setEnabled(d instanceof StyledDocument);
//            }
//            return super.isEnabled();
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            Document doc = text.getDocument();
//            if (doc instanceof DocumentWrapper) {
//                doc = ((DocumentWrapper) doc).getDoc();
//            }
//            if (doc instanceof StyledDocument) {
//                try {
//                    int len = doc.getLength();
//                    String str = doc.getText(0, len);
//
//                    TabSet ts = tableLayout.getColumnPositionTabSetWithoutLayouts(str, text.getFont(),
//                            ((Graphics2D) text.getGraphics()).getFontRenderContext(), true);
//
//                    SimpleAttributeSet attr = new SimpleAttributeSet();
//                    StyleConstants.setTabSet(attr, ts);
//
//                    ((StyledDocument) doc).setParagraphAttributes(0, len, attr, true);
//                } catch (Exception ex) {
//                    ExceptionShell.v().throwWrapped(ex);
//                }
//            }
//        }
//    }
}
