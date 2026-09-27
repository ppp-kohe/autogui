package org.autogui.test.swing.mapping;

import org.autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.autogui.swing.util.KeyUndoManager;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.StringContent;
import javax.swing.undo.UndoableEdit;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprValueDocumentEditorTest extends GuiSwingTestCase {
    GuiReprValueDocumentEditor.StringBuilderContent content;
    StringContent referenceImpl;

    KeyUndoManager undoManager;
    JFrame frame;

    public GuiReprValueDocumentEditorTest() {
    }

    @BeforeEach
    public void setUp() {
        content = new GuiReprValueDocumentEditor.StringBuilderContent(new StringBuilder());
        referenceImpl = new StringContent();

        undoManager = new KeyUndoManager();
    }

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @Test
    public void testInsertString() throws Exception {
        content.insertString(0, "hello\nworld");
        assertEquals("lo\nw", content.getString(3, 4), "getString after insertString");

        assertEquals("hello\nworld\n".length(), content.length(), "length after insertString: inserted + newline");

        referenceImpl.insertString(0, "hello\nworld");
        assertEquals("lo\nw", referenceImpl.getString(3, 4), "getString after insertString: reference");

        assertEquals("hello\nworld\n".length(), referenceImpl.length(), "length after insertString: inserted + newline: reference");

    }

    @Test
    public void testInsertStringAndUndo() throws Exception {
        UndoableEdit e1 = content.insertString(0, "hello\nworld");
        UndoableEdit e2 = content.insertString("hello\nworld".length(), "\n!!!");
        assertTrue(e1.canUndo(), "first undoable");
        assertTrue(e2.canUndo(), "second undoable");
        e2.undo();
        assertTrue(e1.canUndo(), "after first undo");
        assertFalse(e2.canUndo(), "after first undo");

        assertEquals("hello\nworld\n", content.getString(0, content.length()), "after first undo");

        e1.undo();
        assertFalse(e1.canUndo(), "after second undo");
        assertFalse(e2.canUndo(), "after second undo");

        assertEquals("\n", content.getString(0, content.length()), "after second undo");

        //------------------

        e1 = referenceImpl.insertString(0, "hello\nworld");
        e2 = referenceImpl.insertString("hello\nworld".length(), "\n!!!");
        assertTrue(e1.canUndo(), "first undoable: reference");
        assertTrue(e2.canUndo(), "second undoable: reference");
        e2.undo();
        assertTrue(e1.canUndo(), "after first undo: reference");
        assertFalse(e2.canUndo(), "after first undo: reference");

        assertEquals("hello\nworld\n", referenceImpl.getString(0, referenceImpl.length()), "after first undo: reference");

        e1.undo();
        assertFalse(e1.canUndo(), "after second undo: reference");
        assertFalse(e2.canUndo(), "after second undo: reference");

        assertEquals("\n", referenceImpl.getString(0, referenceImpl.length()), "after second undo: reference");
    }

    @Test
    public void testRemove() throws Exception {
        content.insertString(0, "hello\nworld\n123");
        content.remove(3, 4);

        assertEquals("helorld\n123\n", content.getString(0, content.length()), "after remove");

        //------------------

        referenceImpl.insertString(0, "hello\nworld\n123");
        referenceImpl.remove(3, 4);
        assertEquals("helorld\n123\n", referenceImpl.getString(0, referenceImpl.length()), "after remove: reference");
    }

    @Test
    public void testRemoveAndUndo() throws Exception {
        content.insertString(0, "hello\nworld\n123");
        UndoableEdit e1 = content.remove(3, 4);
        UndoableEdit e2 = content.remove(6, 3);
        assertTrue(e1.canUndo(), "first undoable");
        assertTrue(e2.canUndo(), "second undoable");
        e2.undo();
        assertTrue(e1.canUndo(), "after first undo");
        assertFalse(e2.canUndo(), "after first undo");

        assertEquals("helorld\n123\n", content.getString(0, content.length()), "after first undo");

        e1.undo();
        assertFalse(e1.canUndo(), "after second undo");
        assertFalse(e2.canUndo(), "after second undo");

        assertEquals("hello\nworld\n123\n", content.getString(0, content.length()), "after second undo");

        //------------------

        referenceImpl.insertString(0, "hello\nworld\n123");
        e1 = referenceImpl.remove(3, 4);
        e2 = referenceImpl.remove(6, 3);
        assertTrue(e1.canUndo(), "first undoable: reference");
        assertTrue(e2.canUndo(), "second undoable: reference");
        e2.undo();
        assertTrue(e1.canUndo(), "after first undo: reference");
        assertFalse(e2.canUndo(), "after first undo: reference");

        assertEquals("helorld\n123\n", referenceImpl.getString(0, referenceImpl.length()), "after first undo: reference");

        e1.undo();
        assertFalse(e1.canUndo(), "after second undo: reference");
        assertFalse(e2.canUndo(), "after second undo: reference");

        assertEquals("hello\nworld\n123\n", referenceImpl.getString(0, referenceImpl.length()), "after second undo: reference");
    }

    @Test
    public void testCaretPosition() throws Exception {
        Position c = content.createPosition(1);
        content.insertString(0, "hello\nworld");
        assertEquals("hello\nworld\n".length(), c.getOffset(), "after insertion");

        Position c2 = content.createPosition(7);
        content.insertString(0, "!!!\n");

        assertEquals("!!!\nhello\nworld\n".length(), c.getOffset(), "after 2 insertion");
        assertEquals(11, c2.getOffset(), "after 2 insertion");

        // !!! \n hello \n w<c2>orld \n<c>
        content.insertString(13, "---");

        assertEquals("!!!\nhello\nwor---ld\n".length(), c.getOffset(), "after 3 insertion");
        assertEquals(11, c2.getOffset(), "after 3 insertion");

        System.err.println(content.getString(0, content.length()));
        // !!! \n hello \n w<c2>or---ld \n<c>
        content.insertString(c2.getOffset(), "X");
        assertEquals(12, c2.getOffset(), "after 4 insertion");

        // !!! \n hello \n wX<c2>or---ld \n<c>

        content.insertString(c2.getOffset() - 1, "_");
        assertEquals(13, c2.getOffset(), "after 5 insertion");

        // !!! \n hello \n w_X<c2>or---ld \n<c>

        System.err.println(content.getString(0, content.length()));

        content.insertString(c2.getOffset() + 1, "_");
        assertEquals(13, c2.getOffset(), "after 5 insertion");

        // !!! \n hello \n w_X<c2>o_r---ld \n<c>

        content.remove(c2.getOffset(), 1);
        assertEquals("!!!\nhello\nw_X_r---ld\n", content.getString(0, content.length()), "after caret remove");

        content.remove(12, 3);
        assertEquals("!!!\nhello\nw_---ld\n", content.getString(0, content.length()), "after caret remove");

        assertEquals(12, c2.getOffset(), "after caret remove");
        assertEquals(content.length(), c.getOffset(), "after caret remove");


        //------------------

        c = referenceImpl.createPosition(1);
        referenceImpl.insertString(0, "hello\nworld");
        assertEquals("hello\nworld\n".length(), c.getOffset(), "after insertion");

        c2 = referenceImpl.createPosition(7);
        referenceImpl.insertString(0, "!!!\n");

        assertEquals("!!!\nhello\nworld\n".length(), c.getOffset(), "after 2 insertion");
        assertEquals(11, c2.getOffset(), "after 2 insertion");

        // !!! \n hello \n w<c2>orld \n<c>
        referenceImpl.insertString(13, "---");

        assertEquals("!!!\nhello\nwor---ld\n".length(), c.getOffset(), "after 3 insertion");
        assertEquals(11, c2.getOffset(), "after 3 insertion");

        System.err.println(referenceImpl.getString(0, referenceImpl.length()));
        // !!! \n hello \n w<c2>or---ld \n<c>
        referenceImpl.insertString(c2.getOffset(), "X");
        assertEquals(12, c2.getOffset(), "after 4 insertion");

        // !!! \n hello \n wX<c2>or---ld \n<c>

        referenceImpl.insertString(c2.getOffset() - 1, "_");
        assertEquals(13, c2.getOffset(), "after 5 insertion");

        // !!! \n hello \n w_X<c2>or---ld \n<c>

        System.err.println(referenceImpl.getString(0, referenceImpl.length()));

        referenceImpl.insertString(c2.getOffset() + 1, "_");
        assertEquals(13, c2.getOffset(), "after 5 insertion");

        // !!! \n hello \n w_X<c2>o_r---ld \n<c>

        referenceImpl.remove(c2.getOffset(), 1);
        assertEquals("!!!\nhello\nw_X_r---ld\n", referenceImpl.getString(0, referenceImpl.length()), "after caret remove");

        referenceImpl.remove(12, 3);
        assertEquals("!!!\nhello\nw_---ld\n", referenceImpl.getString(0, referenceImpl.length()), "after caret remove");

        assertEquals(12, c2.getOffset(), "after caret remove");
        assertEquals(referenceImpl.length(), c.getOffset(), "after caret remove");
    }

    @Test
    public void testCaretRemove() throws Exception {
        Position p1 = content.createPosition(0);
        Position p2 = content.createPosition(1);
        UndoableEdit u = content.insertString(0, "a\nb\nc");
        assertEquals(0, p1.getOffset());
        assertEquals(6, p2.getOffset());
        u.undo();
        assertEquals("\n", content.getString(0, content.length()));
        assertEquals(0, p1.getOffset());
        assertEquals(1, p2.getOffset());
        u.redo();
        assertEquals("a\nb\nc\n", content.getString(0, content.length()));
        assertEquals(0, p1.getOffset());
        assertEquals(6, p2.getOffset());

        //------------------

        p1 = referenceImpl.createPosition(0);
        p2 = referenceImpl.createPosition(1);
        u = referenceImpl.insertString(0, "a\nb\nc");
        assertEquals(0, p1.getOffset());
        assertEquals(6, p2.getOffset());
        u.undo();
        assertEquals("\n", referenceImpl.getString(0, referenceImpl.length()));
        assertEquals(0, p1.getOffset());
        assertEquals(1, p2.getOffset());
        u.redo();
        assertEquals("a\nb\nc\n", referenceImpl.getString(0, referenceImpl.length()));
        assertEquals(0, p1.getOffset());
        assertEquals(6, p2.getOffset());
    }

    public JTextPane create() {
        JTextPane pane = new JTextPane(GuiReprValueDocumentEditor.ContentWrappingDocument.create(content));
        undoManager.putListenersAndActionsTo(pane);
        frame = createFrame(pane);
        frame.setSize(400, 400);
        return pane;
    }

    @Test
    public void testViewUndoRedo() {
        JTextPane pane = runGet(this::create);

        run(() -> {
            try {
                Document doc = pane.getDocument();
                doc.insertString(0, "a\nb\nc", null);
                assertEquals("a\nb\nc", doc.getText(0, doc.getLength()), "after insertion");

                undoManager.getUndoAction().actionPerformed(null);

                assertEquals("", doc.getText(0, doc.getLength()), "after undo");

                undoManager.getRedoAction().actionPerformed(null);
                assertEquals("a\nb\nc", doc.getText(0, doc.getLength()), "after redo");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        GuiReprValueDocumentEditorTest t = new GuiReprValueDocumentEditorTest();
        t.setUp();
        t.testViewUndoRedo();
    }
}
