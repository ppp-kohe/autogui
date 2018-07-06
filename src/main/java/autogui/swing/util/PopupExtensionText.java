package autogui.swing.util;

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

/** default popup menu support for text components.
 *   The default impl. can be set by {@link #installDefault(JTextComponent)}
 *     which supplies {@link PopupExtension#getDefaultKeyMatcher()} and {@link #getServiceDefaultMenu(JTextComponent)}.
 *    The method calls {@link #putInputEditActions(JTextComponent)} which instruments additional key-actions regarding deleting.
 *
 *    <p>
 *       the default items flow:
 *       <ol>
 *        <li>{@link TextServiceDefaultMenu#initEditActions(JTextComponent)} -&gt;
 *        <li>  {@link TextServiceDefaultMenu#getActionsInInitEditActions(JTextComponent)} -&gt;
 *         <li>  {@link #getEditActions(JTextComponent)}
 *       </ol>
 *
 *    <p>
 *      Note {@link #putUnregisteredEditActions(JTextComponent)} is not called automatically.
 *
 *  */
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

    /** call {@link #show(Component, int, int)} with the bottom right position of the current text selection start
     * @param e the key-event
     * @param comp the target component
     */
    @Override
    public void showByKey(KeyEvent e, Component comp) {
        JTextComponent textComponent = getTextComponent();
        int sel = textComponent.getSelectionStart();
        try {
            Rectangle rect = textComponentModelToView(textComponent, sel);
            show(textComponent, rect.x, rect.y + rect.height);
        } catch (BadLocationException ex) {
            error(ex);
        }
    }

    @SuppressWarnings("deprecation")
    public static Rectangle textComponentModelToView(JTextComponent textComponent, int sel) throws BadLocationException {
        return textComponent.modelToView(sel); //TODO in JDK9, replaced by modelToView2D
    }

    @SuppressWarnings("deprecation")
    public static int textComponentViewToModel(JTextComponent textComponent, Point p) {
        return textComponent.viewToModel(p); //TODO in JDK9, replaced by viewToModel2D
    }

    protected void error(Exception ex) {
        System.err.println("error: " + ex);
    }


    ////////////////////


    public static TextServiceDefaultMenu getServiceDefaultMenu(JTextComponent component) {
        return new TextServiceDefaultMenu(component);
    }

    /** a {@link PopupExtension.PopupMenuBuilder} which has typical actions for the text component */
    public static class TextServiceDefaultMenu implements PopupMenuBuilder {
        protected List<JComponent> editActions;

        public TextServiceDefaultMenu(JTextComponent textComponent) {
            initEditActions(textComponent);
        }

        /** called from constructor
         * @param textComponent the target component
         * */
        public void initEditActions(JTextComponent textComponent) {
            editActions = getActionsInInitEditActions(textComponent).stream()
                    .map(JMenuItem::new)
                    .collect(Collectors.toList());
        }

        public List<Action> getActionsInInitEditActions(JTextComponent textComponent) {
            return PopupExtensionText.getEditActions(textComponent);
        }

        public List<JComponent> getEditActions() {
            return editActions;
        }

        @Override
        public void build(PopupMenuFilter filter, Consumer<Object> menu) {
            List<Object> aroundItems = filter.aroundItems(true);
            MenuBuilder builder = getMenuBuilder();
            builder.addMenuItems(menu, convert(aroundItems), null);
            builder.addMenuItems(menu, filter.convertItems(JComponent.class, editActions), null);
            aroundItems = filter.aroundItems(false);
            builder.addMenuItems(menu, convert(aroundItems), null);
        }

        private List<JComponent> convert(List<Object> items) {
            return new PopupCategorized(null).createMenuItems(items); //just for converting items
        }

        public MenuBuilder getMenuBuilder() {
            return MenuBuilder.get();
        }
    }

    /** JDK has standard implementations for cut, copy and paste as DefaultEditorKit.
     *   However, those actions take the target from event source, and do not have user friendly name
     * @param textComponent the target component
     * @return list of actions for the component: cut, copy, copy-all, paste, paste-all, select-all, load, save,
     *    and open-browser
     */
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

    /**
     *
     * @param component optional component
     * @return list of actions of additional edit-actions:
     *    delete-next-word, delete-previous-word, delete-to-line-end, and paste-history
     */
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
     * @param component the host of action- and input-map
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

    /** the action for opening selection as an URL in a browser */
    public static class TextOpenBrowserAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        protected JComponent component;
        public TextOpenBrowserAction(JComponent component) {
            this.component = component;
            putValue(NAME, "Open URL in Browser");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_B,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String text = null;
            if (component instanceof JLabel) {
                text = ((JLabel) component).getText();
            } else if (component instanceof JTextComponent) {
                JTextComponent comp = (JTextComponent) component;
                int start = comp.getSelectionStart();
                int end = comp.getSelectionEnd();
                if (start != end) {
                    if (start > end) {
                        int tmp = start;
                        start = end;
                        end = tmp;
                    }
                    try {
                        text = comp.getText(Math.max(0, start),
                                Math.min(comp.getDocument().getLength(), end - start));
                    } catch (Exception ex) {
                        //
                    }
                } else {
                    text = comp.getText();
                }
            }
            open(text);
        }

        public void open(String text) {
            if (text != null) {
                try {
                    if (!text.contains("://")) {
                        text = "http://" + text;
                    }
                    text = text.trim();
                    Desktop.getDesktop().browse(URI.create(text));
                } catch (Exception ex) {
                    System.err.printf("Open URL Error:%s", ex);
                    ex.printStackTrace();
                }
            }
        }

        public void openList(List<Object> values) {
            for (Object o : values) {
                if (o instanceof String) {
                    open((String) o);
                }
            }
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_JUMP;
        }
    }

    /** a cut action */
    public static class TextCutAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCutAction(JTextComponent field) {
            putValue(NAME, "Cut");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_X,
                        PopupExtension.getMenuShortcutKeyMask()));
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_CUT;
        }
    }

    /** a copy action */
    public static class TextCopyAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCopyAction(JTextComponent field) {
            putValue(NAME, "Copy");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_C,
                        PopupExtension.getMenuShortcutKeyMask()));
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_COPY;
        }
    }

    /** a paste action*/
    public static class TextPasteAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextPasteAction(JTextComponent field) {
            putValue(NAME, "Paste");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_V,
                            PopupExtension.getMenuShortcutKeyMask()));
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_PASTE;
        }
    }

    /** a copy-all action: copying all text in a text-component */
    public static class TextCopyAllAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextCopyAllAction(JTextComponent field) {
            putValue(NAME, "Copy Value");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_C,
                            PopupExtension.getMenuShortcutKeyMask(), InputEvent.SHIFT_DOWN_MASK));
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_COPY;
        }
    }

    /** a paste-all action: replacing entire text with the clipboard contents */
    public static class TextPasteAllAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;
        public TextPasteAllAction(JTextComponent field) {
            putValue(NAME, "Paste Value");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_V,
                            PopupExtension.getMenuShortcutKeyMask() |
                            InputEvent.SHIFT_DOWN_MASK));
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.isEditable();
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_PASTE;
        }
    }

    /** a select-all action */
    public static class TextSelectAllAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected JTextComponent field;

        public TextSelectAllAction(JTextComponent field) {
            putValue(NAME, "Select All");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_A,
                            PopupExtension.getMenuShortcutKeyMask()));
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            field.selectAll();
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_SELECT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_SELECT;
        }
    }

    //////// text action with history buffer

    /** an abstract action for interacting with the history-buffer (kill-buffer) */
    public abstract static class TextAbstractHistoryAction extends AbstractAction {
        protected JTextComponent field;

        public TextAbstractHistoryAction(String name, JTextComponent field) {
            putValue(NAME, name);
            this.field = field;
        }

        @Override
        public boolean isEnabled() {
            return field.isEditable();
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

    /** an delete-to-line-end action */
    public static class TextDeleteToLineEndAction extends TextAbstractHistoryAction
        implements PopupCategorized.CategorizedMenuItemAction {

        public TextDeleteToLineEndAction(JTextComponent field) {
            super("delete-line-end", field);
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_K,
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_DELETE;
        }
    }

    /** an delete-next-word action */
    public static class TextDeleteNextWordAction extends TextAbstractHistoryAction
        implements PopupCategorized.CategorizedMenuItemAction {
        public TextDeleteNextWordAction(JTextComponent field) {
            super("delete-next-word", field);
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_DELETE,
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_DELETE;
        }
    }

    /** an delete-previous-word action */
    public static class TextDeletePreviousWordAction extends TextAbstractHistoryAction
        implements PopupCategorized.CategorizedMenuItemAction {
        public TextDeletePreviousWordAction(JTextComponent field) {
            super("delete-previous-word", field);
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_BACK_SPACE,
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_DELETE;
        }
    }

    /** an paste the history-buffer action (yank) */
    public static class TextPasteHistoryAction extends TextAbstractHistoryAction
        implements PopupCategorized.CategorizedMenuItemAction {
        public TextPasteHistoryAction(JTextComponent field) {
            super("yank", field);
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_Y,
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

        @Override
        public String getCategory() {
            return MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_PASTE;
        }
    }

    public static TextInputHistory defaultHistory = new TextInputHistory();

    /** a history buffer (kill buffer) */
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

    /** an edit in the history buffer */
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

    public static Charset selectedCharset = StandardCharsets.UTF_8;
    protected static JComponent encodingPane;

    public static JComponent getEncodingPane() {
        if (encodingPane == null) {
            JPanel pane = new JPanel(new BorderLayout());
            SortedMap<String,Charset> map = Charset.availableCharsets();
            JComboBox<String> box = new JComboBox<>(map.keySet()
                    .toArray(new String[map.size()]));
            box.setSelectedItem(StandardCharsets.UTF_8.displayName());
            box.addActionListener(e -> {
                selectedCharset = map.get((String) box.getSelectedItem());
            });
            JLabel label = new JLabel("Encoding:");
            label.setHorizontalAlignment(SwingConstants.LEADING);
            pane.add(label, BorderLayout.NORTH);
            pane.add(box, BorderLayout.CENTER);
            encodingPane = pane;
        }
        return encodingPane;
    }

    /** a file loading action */
    public static class TextLoadAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        protected JTextComponent field;

        public TextLoadAction(JTextComponent field) {
            putValue(NAME, "Load...");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_O,
                    PopupExtension.getMenuShortcutKeyMask()));
            this.field = field;
        }

        protected JComponent getComponent() {
            return field;
        }

        @Override
        public boolean isEnabled() {
            return field == null || field.isEnabled() && field.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = load();
            if (str != null) {
                field.setText(str);
            }
        }

        public String load() {
            Path path = SettingsWindow.getFileDialogManager().showOpenDialog(getComponent(), getEncodingPane());
            if (path != null) {
                try {
                    return new String(Files.readAllBytes(path), selectedCharset);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return null;
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_IMPORT;
        }
    }

    /** a file saving action */
    public static class TextSaveAction extends TextLoadAction
        implements PopupCategorized.CategorizedMenuItemAction {
        public TextSaveAction(JTextComponent component) {
            super(component);
            putValue(NAME, "Save...");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_S,
                    PopupExtension.getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Path path = getPath();
            if (path != null) {
                save(path);
            }
        }

        public Path getPath() {
            SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();
            return fd.showConfirmDialogIfOverwriting(getComponent(),
                    fd.showSaveDialog(getComponent(), getEncodingPane(), null));
        }

        public void save(Path path) {
            String text = field.getText();
            saveLines(path, Collections.singletonList(text));
        }

        public void saveLines(Path path, List<String> lines) {
            try {
                Files.write(path, lines, selectedCharset);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return MENU_SUB_CATEGORY_EXPORT;
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
