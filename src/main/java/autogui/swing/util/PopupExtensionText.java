package autogui.swing.util;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PopupExtensionText extends PopupExtension implements FocusListener {
    protected int selectionStart;
    protected int selectionEnd;
    protected int documentLength;

    public static PopupExtensionText installDefault(JTextComponent textComponent) {
        return new PopupExtensionText(textComponent,
                getDefaultKeyMatcher(),
                getServiceDefaultMenu(textComponent));
    }

    public PopupExtensionText(JTextComponent textComponent, Predicate<KeyEvent> keyMatcher, PopupMenuBuilder builder) {
        super(textComponent, keyMatcher, builder);
    }

    public JTextComponent getTextComponent() {
        return (JTextComponent) getPane();
    }

    @Override
    public void addListenersTo(JComponent pane) {
        super.addListenersTo(pane);
        //pane.addFocusListener(this); //this overwrites effect of selectAll(), but no re-selection causes entire selection of tab focus gain

    }

    ////////////////

    @Override
    public void showByKey(KeyEvent e, Component comp) {
        JTextComponent textComponent = getTextComponent();
        int sel = textComponent.getSelectionStart();
        try {
            Rectangle rect = textComponent.modelToView(sel); //TODO in JDK9, replaced by modelToView2D
            show(textComponent, rect.x, rect.y + rect.height);
        } catch (BadLocationException ex) {
            error(ex);
        }
    }

    protected void error(Exception ex) {
        System.err.println("error: " + ex);
    }


    ////////////////////


    /** a {@link PopupExtension.PopupMenuBuilder} which has typical actions for the text component*/
    public static TextServiceDefaultMenu getServiceDefaultMenu(JTextComponent component) {
        return new TextServiceDefaultMenu(component);
    }

    public static class TextServiceDefaultMenu implements PopupMenuBuilder {
        protected List<JComponent> editActions;

        public TextServiceDefaultMenu(JTextComponent textComponent) {
            initEditActions(textComponent);
        }

        /** called from constructor */
        public void initEditActions(JTextComponent textComponent) {
            editActions = PopupExtensionText.getEditActions(textComponent).stream()
                    .map(JMenuItem::new)
                    .collect(Collectors.toList());
        }

        public List<JComponent> getEditActions() {
            return editActions;
        }

        @Override
        public void build(PopupExtension sender, JPopupMenu menu) {
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

    /////////////

    @Override
    public void focusGained(FocusEvent e) {
        if (selectionStart >= 0) {
            JTextComponent text = getTextComponent();
            int start = selectionStart;
            int end = selectionEnd;
            boolean expandEnd = (end >= documentLength);
            int len = text.getDocument().getLength();
            if (expandEnd) {
                end = len;
            }
            start = Math.max(Math.min(start, len), 0);
            end = Math.max(Math.min(end, len), 0);

            text.setSelectionStart(start);
            text.setSelectionEnd(end);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        JTextComponent text = getTextComponent();
        int start = text.getSelectionStart();
        int end = text.getSelectionEnd();
        selectionStart = start;
        selectionEnd = end;
        documentLength = text.getDocument().getLength();
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
