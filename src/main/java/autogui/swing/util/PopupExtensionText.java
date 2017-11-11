package autogui.swing.util;

import autogui.base.log.GuiLogManager;
import autogui.swing.log.GuiSwingLogManager;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PopupExtensionText extends PopupExtension implements FocusListener {
    protected int selectionStart;
    protected int selectionEnd;
    protected int documentLength;

    public static PopupExtensionText installDefault(JTextComponent textComponent) {
        putInputEditActions(textComponent);
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
        public void build(PopupExtension sender, Consumer<Object> menu) {
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
                new TextPasteAllAction(textComponent),
                new TextSelectAllAction(textComponent),
                new TextLoadAction(textComponent),
                new TextSaveAction(textComponent),
                new TextOpenBrowserAction(textComponent));
    }

    //////////////

    /** component is optional */
    public static List<Action> getInputEditActions(JTextComponent component) {
        return Arrays.asList(
                new TextDeleteNextWordAction(component),
                new TextDeletePreviousWordAction(component),
                new TextDeleteToLineEndAction(component),
                new TextPasteHistoryAction(component));
    }

    public static void putInputEditActions(JTextComponent component) {
        component.addCaretListener(defaultHistory);
        putInputEditActionsToKeys(component.getInputMap(), component);
        putInputEditActionsToMap(component.getActionMap(), component);
    }

    public static void putInputEditActionsToMap(ActionMap map, JTextComponent component) {
        getInputEditActions(component)
            .forEach(a -> map.put(a.getValue(Action.NAME), a));
    }

    public static void putInputEditActionsToKeys(InputMap map, JTextComponent component) {
        getInputEditActions(component)
            .forEach(a -> map.put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), a.getValue(Action.NAME)));
    }


    /**
     * usually, it will register "Copy Value" and "Paste Value"
     */
    public static void putUnregisteredEditActions(JTextComponent component) {
        List<Action> actions = getEditActions(component);
        InputMap inputMap = component.getInputMap();
        ActionMap actionMap = component.getActionMap();
        for (Action a : actions) {
            KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
            if (k != null) {
                Object existing = inputMap.get(k);
                if (existing == null) {
                    Object name = a.getValue(Action.NAME);
                    inputMap.put(k, name);
                    actionMap.put(name, a);
                }
            }
        }

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

    public static class TextOpenBrowserAction extends AbstractAction {
        protected JComponent component;
        public TextOpenBrowserAction(JComponent component) {
            this.component = component;
            putValue(NAME, "Open URL in Browser");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String text = null;
            if (component instanceof JLabel) {
                text = ((JLabel) component).getText();
            } else if (component instanceof JTextComponent) {
                text = ((JTextComponent) component).getText();
            }
            if (text != null) {
                try {
                    if (!text.contains("://")) {
                        text = "http://" + text;
                    }
                    Desktop.getDesktop().browse(URI.create(text));
                } catch (Exception ex) {
                    GuiLogManager.get().logFormat("Open URL Error:%s", ex);
                }
            }
        }
    }

    public static class TextCutAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCutAction(JTextComponent field) {
            putValue(NAME, "Cut");
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_X,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_C,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_V,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_C,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK));
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            copy(field.getText());
        }

        public void actionPerformedOnTable(ActionEvent e, Collection<Object> values) {
            copy(values.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n")));
        }

        public void copy(String data) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection sel = new StringSelection(data);
            board.setContents(sel, sel);
        }
    }

    public static class TextPasteAllAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextPasteAllAction(JTextComponent field) {
            putValue(NAME, "Paste Value");
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_V,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
                            InputEvent.SHIFT_DOWN_MASK));
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(field::setText);
        }

        public void pasteLines(Consumer<List<String>> setter) {
            paste(s -> setter.accept(Arrays.asList(s.split("\\n"))));
        }

        public void paste(Consumer<String> setter) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (board.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                try {
                    String data = (String) board.getData(DataFlavor.stringFlavor);
                    setter.accept(data);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static class TextSelectAllAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;

        public TextSelectAllAction(JTextComponent field) {
            putValue(NAME, "Select All");
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_A,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.selectAll();
        }
    }

    //////// text action with history buffer

    public abstract static class TextAbstractHistoryAction extends AbstractAction {
        protected JTextComponent field;

        public TextAbstractHistoryAction(String name, JTextComponent field) {
            putValue(NAME, name);
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target;
            if (field == null && e.getSource() instanceof JTextComponent) {
                target = (JTextComponent) e.getSource();
            } else if (field != null) {
                target = field;
            } else {
                return;
            }

            if (!target.isEditable() || !target.isEnabled()) {
                return;
            }

            try {
                actionPerformedOnText(target);
            } catch (Exception ex) {
                //ignore
            }
        }

        public abstract void actionPerformedOnText(JTextComponent target) throws BadLocationException;

        public void cut(JTextComponent target, int from, int to) throws BadLocationException {
            if (from > to) {
                int tmp = to;
                to = from;
                from = tmp;
            }
            if (from >= 0 && to >= 0) {
                String removedText = target.getDocument().getText(from, to - from);
                target.getDocument().remove(from, to - from);
                putKillBuffer(removedText);
            }
        }

        public void putKillBuffer(String str) {
            defaultHistory.put(str);
        }
    }

    public static class TextDeleteToLineEndAction extends TextAbstractHistoryAction {

        public TextDeleteToLineEndAction(JTextComponent field) {
            super("delete-line-end", field);
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_K,
                            InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public void actionPerformedOnText(JTextComponent target) throws BadLocationException {
            int sel = target.getSelectionStart();
            int end = Utilities.getRowEnd(target, sel);
            if (end == sel) {
                //remove new line
                try {
                    end = Utilities.getRowStart(target, end + 1);
                } catch (BadLocationException ex) {
                    //nothing
                }
            }
            cut(target, sel, end);
        }
    }

    public static class TextDeleteNextWordAction extends TextAbstractHistoryAction {
        public TextDeleteNextWordAction(JTextComponent field) {
            super("delete-next-word", field);
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,
                            InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformedOnText(JTextComponent target) throws BadLocationException {
            int sel = target.getSelectionStart();
            int end = Utilities.getNextWord(target, sel);
            try {
                int lineEnd = Utilities.getRowEnd(target, sel);
                if (lineEnd < end && sel != lineEnd) {
                    end = lineEnd;
                }
            } catch (BadLocationException ex) {
                //
            }
            if (end < 0) {
                end = Utilities.getRowEnd(target, sel);
            }
            cut(target, sel, end);
        }
    }

    public static class TextDeletePreviousWordAction extends TextAbstractHistoryAction {
        public TextDeletePreviousWordAction(JTextComponent field) {
            super("delete-previous-word", field);
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,
                            InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformedOnText(JTextComponent target) throws BadLocationException {
            int sel = target.getSelectionStart();
            int end = Utilities.getPreviousWord(target, sel);
            try {
                int lineStart = Utilities.getRowStart(target, sel);
                if (end < lineStart && sel != lineStart) {
                    end = lineStart;
                }
            } catch (BadLocationException ex) {
                //
            }
            if (end < 0) {
                end = Utilities.getRowStart(target, sel);
            }
            cut(target, sel, end);
        }
    }

    public static class TextPasteHistoryAction extends TextAbstractHistoryAction {
        public TextPasteHistoryAction(JTextComponent field) {
            super("yank", field);
            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                            InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public void actionPerformedOnText(JTextComponent target) throws BadLocationException {
            try {
                String content = defaultHistory.take();
                target.replaceSelection(content);
            } catch (Exception ex) {
                //nothing
            }
        }
    }

    public static TextInputHistory defaultHistory = new TextInputHistory();

    public static class TextInputHistory implements CaretListener {
        protected List<TextInputEdit> buffer = new ArrayList<>();
        protected Object nextSource;
        protected int nextPosition = -1;

        public void put(String str) {
            if (!buffer.isEmpty()) {
                TextInputEdit prev = buffer.get(0);
                if (nextPosition == prev.position &&
                        nextSource == prev.source) {
                    //remove next
                    prev.text += str;
                } else if (nextPosition == prev.position - prev.text.length() &&
                        nextSource == prev.source) {
                    //remove prev
                    prev.position = nextPosition;
                    prev.text = str + prev.text;
                } else {
                    buffer.add(0, new TextInputEdit(nextSource, nextPosition, str));
                }
            } else {
                buffer.add(0, new TextInputEdit(nextSource, nextPosition, str));
            }
            while (buffer.size() > 1000) {
                buffer.remove(buffer.size() - 1);
            }
        }

        public String take() {
            return buffer.get(0).text;
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            nextSource = e.getSource();
            nextPosition = e.getDot();
        }
    }

    public static class TextInputEdit {
        public Object source;
        public int position;
        public String text;

        public TextInputEdit(Object source, int position, String text) {
            this.source = source;
            this.position = position;
            this.text = text;
        }
    }

    ////////////

    public static class TextLoadAction extends AbstractAction {
        protected JTextComponent field;
        protected static JFileChooser fileChooser;
        protected static Charset charset = StandardCharsets.UTF_8;

        public TextLoadAction(JTextComponent field) {
            putValue(NAME, "Load...");
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.isEnabled() && field.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = load();
            if (str != null) {
                field.setText(str);
            }
        }

        public String load() {
            JFileChooser fileChooser = getFileChooser();
            int r = fileChooser.showOpenDialog(field);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                try {
                    return new String(Files.readAllBytes(path), charset);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return null;
        }

        public static JFileChooser getFileChooser() {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                JPanel pane = new JPanel();
                SortedMap<String,Charset> map = Charset.availableCharsets();
                JComboBox<String> box = new JComboBox<>(map.keySet()
                        .toArray(new String[map.size()]));
                box.setSelectedItem(StandardCharsets.UTF_8.displayName());
                box.addActionListener(e -> {
                    charset = map.get((String) box.getSelectedItem());
                });
                pane.add(new JLabel("Encoding:"));
                pane.add(box);
                fileChooser.setAccessory(pane);
            }
            return fileChooser;
        }
    }

    public static class TextSaveAction extends TextLoadAction {
        public TextSaveAction(JTextComponent component) {
            super(component);
            putValue(NAME, "Save...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = getFileChooser();
            int r = fileChooser.showSaveDialog(field);
            if (r == JFileChooser.APPROVE_OPTION) {
                Path path = fileChooser.getSelectedFile().toPath();
                if (Files.exists(path)) {
                    int op = JOptionPane.showConfirmDialog(field, path.toString() + " exists. Overwrites?",
                            "File Saving", JOptionPane.OK_CANCEL_OPTION);
                    if (op == JOptionPane.OK_OPTION) {
                        save(path);
                    }
                }
            }
        }

        public void save(Path path) {
            String text = field.getText();
            try {
                Files.write(path, Collections.singletonList(text), charset);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
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
