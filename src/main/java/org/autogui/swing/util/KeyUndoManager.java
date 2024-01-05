package org.autogui.swing.util;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.io.Serial;
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
@SuppressWarnings("this-escape")
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

        undoAction.setKeyUndoManager(this);
        redoAction.setKeyUndoManager(this);
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
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                endEdits();
                whileKeyTyping = false;
            }
        });
        c.addPropertyChangeListener("document", this::updateDocument);
    }

    public synchronized void updateDocument(PropertyChangeEvent e) {
        undoManager.discardAllEdits();
        whileKeyTyping = false;
        edits = null;
        checkEnabled();

        Document doc = (Document) e.getNewValue();
        Document oldDoc = (Document) e.getOldValue();
        if (oldDoc != null) {
            oldDoc.removeUndoableEditListener(this);
        }
        if (doc != oldDoc) {
            doc.addUndoableEditListener(this);
        }
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
    public synchronized void keyPressed(KeyEvent e) {
        if (!isRegularKeyType(e)) {
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
    public synchronized void undoableEditHappened(UndoableEditEvent e) {
        if (whileKeyTyping) {
            if (edits == null) {
                edits = new DynamicCompoundEdit(e.getEdit());
                undoManager.addEdit(edits);
            } else {
                boolean added = edits.addEdit(e.getEdit());
                if (!added) {
                    endEdits();
                    edits = new DynamicCompoundEdit(e.getEdit());
                    undoManager.addEdit(edits);
                }
            }
        } else {
            endEdits();
            undoManager.addEdit(e.getEdit());
        }
        checkEnabled();
    }

    public void checkEnabled() {
        undoAction.checkEnabled();
        redoAction.checkEnabled();
    }

    @Override
    public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e) {
        endEdits();
    }

    /** the action for undo or redo */
    public static class UndoAction extends AbstractAction
        implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected UndoManager manager;
        protected boolean undo;
        protected boolean lastEnabled;
        protected KeyUndoManager keyUndoManager;

        public UndoAction(UndoManager manager, boolean undo) {
            this.manager = manager;
            this.undo = undo;
            int mask = PopupExtension.getMenuShortcutKeyMask();
            if (undo) {
                putValue(NAME, "Undo");
                putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_Z, mask));
            } else {
                putValue(NAME, "Redo");
                putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_Z,
                        mask, KeyEvent.SHIFT_DOWN_MASK));
            }
            lastEnabled = isEnabled();
        }

        public void setKeyUndoManager(KeyUndoManager keyUndoManager) {
            this.keyUndoManager = keyUndoManager;
        }

        public void checkEnabled() {
            boolean e = isEnabled();
            if (lastEnabled != e) {
                firePropertyChange("enabled", lastEnabled, e);
                lastEnabled = e;
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
            if (keyUndoManager != null) {
                keyUndoManager.checkEnabled();
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

    /** a composition of undoable-edits */
    public static class DynamicCompoundEdit extends AbstractUndoableEdit {
        @Serial private static final long serialVersionUID = 1L;
        protected List<UndoableEdit> edits = new ArrayList<>();

        protected boolean fixed = false;

        protected String name;
        protected String undoName;
        protected String redoName;

        public DynamicCompoundEdit(UndoableEdit edit) {
            edits.add(edit);
            name = edit.getPresentationName();
            undoName = edit.getUndoPresentationName();
            redoName = edit.getRedoPresentationName();
        }

        public boolean isEmpty() {
            return edits.isEmpty();
        }

        public void end() {
            fixed = true;
        }

        @Override
        public void die() {
            for (int i = edits.size() - 1; i > 0; --i) {
                edits.get(i).die();
            }
            super.die();
        }

        @Override
        public boolean addEdit(UndoableEdit anEdit) {
            if (fixed) {
                return false;
            } else {
                if (edits.isEmpty() ||
                        edits.getLast().getPresentationName().equals(anEdit.getPresentationName())) {
                    edits.add(anEdit);
                    return true;
                } else {
                    return false;
                }
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
            for (UndoableEdit e : edits) {
                e.redo();
            }
            super.redo();
        }

        @Override
        public String getPresentationName() {
            return name;
        }

        @Override
        public String getUndoPresentationName() {
            return undoName;
        }

        @Override
        public String getRedoPresentationName() {
            return redoName;
        }
    }
}
