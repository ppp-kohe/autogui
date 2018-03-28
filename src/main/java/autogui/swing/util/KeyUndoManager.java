package autogui.swing.util;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 *  the class provides Undo and Redo actions for text panes.
 * <p>
 * usage:
 * <pre>
 *     KeyUndoManager km = new KeyUndoManager();
 *     km.putListenersAndActionsTo(editorPane);
 * </pre>
 */
public class KeyUndoManager implements KeyListener, UndoableEditListener, FocusListener {
    protected UndoManager undoManager;
    protected DynamicCompoundEdit edits;

    protected boolean whileKeyTyping;

    protected UndoAction undoAction;
    protected UndoAction redoAction;

    public KeyUndoManager() {
        this(new UndoManager());
    }

    public KeyUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
        this.undoAction = new UndoAction(undoManager, true);
        this.redoAction = new UndoAction(undoManager, false);
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public UndoAction getUndoAction() {
        return undoAction;
    }

    public UndoAction getRedoAction() {
        return redoAction;
    }

    public void putListenersAndActionsTo(JTextComponent c) {
        addListenersTo(c);
        putUndoActionsTo(c);
    }

    public void addListenersTo(JTextComponent c) {
        c.addKeyListener(this);
        c.addFocusListener(this);
        c.getDocument().addUndoableEditListener(this);
    }

    public void putUndoActionsTo(JTextComponent c) {
        Object undoName = undoAction.getValue(Action.NAME);
        Object redoName = redoAction.getValue(Action.NAME);
        c.getActionMap().put(undoName, undoAction);
        c.getActionMap().put(redoName, redoAction);

        c.getInputMap().put((KeyStroke) undoAction.getValue(Action.ACCELERATOR_KEY), undoName);
        c.getInputMap().put((KeyStroke) redoAction.getValue(Action.ACCELERATOR_KEY), redoName);
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        if (isRegularKeyType(e)) {
            if (edits == null) {
                edits = new DynamicCompoundEdit();
            }
        } else {
            endEdits();
        }
        whileKeyTyping = true;
    }

    public boolean isRegularKeyType(KeyEvent e) {
        int ex = e.getModifiersEx();
        return ex == KeyEvent.SHIFT_DOWN_MASK || ex == 0;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        whileKeyTyping = false;
        //subsequent keyPressed events continues to add to the same edits
    }

    private void endEdits() {
        if (edits != null) {
            edits.end();
            edits = null;
        }
    }


    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        if (edits != null) {
            if (whileKeyTyping) {
                boolean init = edits.isEmpty();
                edits.addEdit(e.getEdit());
                if (init) {
                    undoManager.addEdit(edits);
                }
            } else {
                endEdits();
                undoManager.addEdit(e.getEdit());
            }
        } else {
            undoManager.addEdit(e.getEdit());
        }
    }

    @Override
    public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e) {
        endEdits();
    }

    public static class UndoAction extends AbstractAction
        implements PopupCategorized.CategorizedPopupItemMenuAction {
        protected UndoManager manager;
        protected boolean undo;

        public UndoAction(UndoManager manager, boolean undo) {
            this.manager = manager;
            this.undo = undo;
            int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            if (undo) {
                putValue(NAME, "Undo");
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        mask));
            } else {
                putValue(NAME, "Redo");
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        mask | KeyEvent.SHIFT_DOWN_MASK));
            }
        }

        @Override
        public boolean isEnabled() {
            return undo ? manager.canUndo() : manager.canRedo();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (undo) {
                if (manager.canUndo()) {
                    manager.undo();
                }
            } else {
                if (manager.canRedo()) {
                    manager.redo();
                }
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_UNDO;
        }

        @Override
        public String getSubCategory() {
            return undo ? PopupExtension.MENU_SUB_CATEGORY_UNDO :
                          PopupExtension.MENU_SUB_CATEGORY_REDO;
        }
    }

    public static class DynamicCompoundEdit extends AbstractUndoableEdit {
        protected List<UndoableEdit> edits = new ArrayList<>();

        protected boolean fixed = false;

        public boolean isEmpty() {
            return edits.isEmpty();
        }

        public void end() {
            fixed = true;
        }

        @Override
        public void die() {
            edits.forEach(UndoableEdit::die);
            super.die();
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            if (fixed) {
                return false;
            } else {
                edits.add(anEdit);
                return true;
            }
        }

        @Override
        public void undo() throws CannotUndoException {
            if (!canUndo()) {
                throw new CannotUndoException();
            }
            for (int i = edits.size() - 1; i >= 0; --i) {
                edits.get(i).undo();
            }
            super.undo();
        }

        @Override
        public void redo() throws CannotRedoException {
            if (!canRedo()) {
                throw new CannotRedoException();
            }

            edits.forEach(UndoableEdit::redo);
            super.redo();
        }
    }
}
