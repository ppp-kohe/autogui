package autogui.swing.mapping;

import autogui.swing.GuiSwingTestCase;
import autogui.swing.util.KeyUndoManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.StringContent;
import javax.swing.undo.UndoableEdit;

public class GuiReprValueDocumentEditorTest extends GuiSwingTestCase {
    GuiReprValueDocumentEditor.StringBuilderContent content;
    StringContent referenceImpl;

    KeyUndoManager undoManager;
    @Before
    public void setUp() {
        content = new GuiReprValueDocumentEditor.StringBuilderContent(new StringBuilder());
        referenceImpl = new StringContent();

        undoManager = new KeyUndoManager();
    }

    @Test
    public void testInsertString() throws Exception {
        content.insertString(0, "hello\nworld");
        Assert.assertEquals("getString after insertString",
                "lo\nw", content.getString(3, 4));

        Assert.assertEquals("length after insertString: inserted + newline",
                "hello\nworld\n".length(), content.length());

        referenceImpl.insertString(0, "hello\nworld");
        Assert.assertEquals("getString after insertString: reference",
                "lo\nw", referenceImpl.getString(3, 4));

        Assert.assertEquals("length after insertString: inserted + newline: reference",
                "hello\nworld\n".length(), referenceImpl.length());

    }

    @Test
    public void testInsertStringAndUndo() throws Exception {
        UndoableEdit e1 = content.insertString(0, "hello\nworld");
        UndoableEdit e2 = content.insertString("hello\nworld".length(), "\n!!!");
        Assert.assertTrue("first undoable", e1.canUndo());
        Assert.assertTrue("second undoable", e2.canUndo());
        e2.undo();
        Assert.assertTrue("after first undo", e1.canUndo());
        Assert.assertFalse("after first undo", e2.canUndo());

        Assert.assertEquals("after first undo",
                "hello\nworld\n",
                content.getString(0, content.length()));

        e1.undo();
        Assert.assertFalse("after second undo", e1.canUndo());
        Assert.assertFalse("after second undo", e2.canUndo());

        Assert.assertEquals("after second undo",
                "\n",
                content.getString(0, content.length()));

        ////////////

        e1 = referenceImpl.insertString(0, "hello\nworld");
        e2 = referenceImpl.insertString("hello\nworld".length(), "\n!!!");
        Assert.assertTrue("first undoable: reference", e1.canUndo());
        Assert.assertTrue("second undoable: reference", e2.canUndo());
        e2.undo();
        Assert.assertTrue("after first undo: reference", e1.canUndo());
        Assert.assertFalse("after first undo: reference", e2.canUndo());

        Assert.assertEquals("after first undo: reference",
                "hello\nworld\n",
                referenceImpl.getString(0, referenceImpl.length()));

        e1.undo();
        Assert.assertFalse("after second undo: reference", e1.canUndo());
        Assert.assertFalse("after second undo: reference", e2.canUndo());

        Assert.assertEquals("after second undo: reference",
                "\n",
                referenceImpl.getString(0, referenceImpl.length()));
    }

    @Test
    public void testRemove() throws Exception {
        content.insertString(0, "hello\nworld\n123");
        content.remove(3, 4);

        Assert.assertEquals("after remove",
                "helorld\n123\n", content.getString(0, content.length()));

        /////////

        referenceImpl.insertString(0, "hello\nworld\n123");
        referenceImpl.remove(3, 4);
        Assert.assertEquals("after remove: reference",
                "helorld\n123\n", referenceImpl.getString(0, referenceImpl.length()));
    }

    @Test
    public void testRemoveAndUndo() throws Exception {
        content.insertString(0, "hello\nworld\n123");
        UndoableEdit e1 = content.remove(3, 4);
        UndoableEdit e2 = content.remove(6, 3);
        Assert.assertTrue("first undoable", e1.canUndo());
        Assert.assertTrue("second undoable", e2.canUndo());
        e2.undo();
        Assert.assertTrue("after first undo", e1.canUndo());
        Assert.assertFalse("after first undo", e2.canUndo());

        Assert.assertEquals("after first undo",
                "helorld\n123\n",
                content.getString(0, content.length()));

        e1.undo();
        Assert.assertFalse("after second undo", e1.canUndo());
        Assert.assertFalse("after second undo", e2.canUndo());

        Assert.assertEquals("after second undo",
                "hello\nworld\n123\n",
                content.getString(0, content.length()));

        ////////////

        referenceImpl.insertString(0, "hello\nworld\n123");
        e1 = referenceImpl.remove(3, 4);
        e2 = referenceImpl.remove(6, 3);
        Assert.assertTrue("first undoable: reference", e1.canUndo());
        Assert.assertTrue("second undoable: reference", e2.canUndo());
        e2.undo();
        Assert.assertTrue("after first undo: reference", e1.canUndo());
        Assert.assertFalse("after first undo: reference", e2.canUndo());

        Assert.assertEquals("after first undo: reference",
                "helorld\n123\n",
                referenceImpl.getString(0, referenceImpl.length()));

        e1.undo();
        Assert.assertFalse("after second undo: reference", e1.canUndo());
        Assert.assertFalse("after second undo: reference", e2.canUndo());

        Assert.assertEquals("after second undo: reference",
                "hello\nworld\n123\n",
                referenceImpl.getString(0, referenceImpl.length()));
    }

    @Test
    public void testCaretPosition() throws Exception {
        Position c = content.createPosition(1);
        content.insertString(0, "hello\nworld");
        Assert.assertEquals("after insertion",
                "hello\nworld\n".length(), c.getOffset());

        Position c2 = content.createPosition(7);
        content.insertString(0, "!!!\n");

        Assert.assertEquals("after 2 insertion",
                "!!!\nhello\nworld\n".length(), c.getOffset());
        Assert.assertEquals("after 2 insertion",
                11, c2.getOffset());

        // !!! \n hello \n w<c2>orld \n<c>
        content.insertString(13, "---");

        Assert.assertEquals("after 3 insertion",
                "!!!\nhello\nwor---ld\n".length(), c.getOffset());
        Assert.assertEquals("after 3 insertion",
                11, c2.getOffset());

        System.err.println(content.getString(0, content.length()));
        // !!! \n hello \n w<c2>or---ld \n<c>
        content.insertString(c2.getOffset(), "X");
        Assert.assertEquals("after 4 insertion",
                12, c2.getOffset());

        // !!! \n hello \n wX<c2>or---ld \n<c>

        content.insertString(c2.getOffset() - 1, "_");
        Assert.assertEquals("after 5 insertion",
                13, c2.getOffset());

        // !!! \n hello \n w_X<c2>or---ld \n<c>

        System.err.println(content.getString(0, content.length()));

        content.insertString(c2.getOffset() + 1, "_");
        Assert.assertEquals("after 5 insertion",
                13, c2.getOffset());

        // !!! \n hello \n w_X<c2>o_r---ld \n<c>

        content.remove(c2.getOffset(), 1);
        Assert.assertEquals("after caret remove",
                "!!!\nhello\nw_X_r---ld\n",
                content.getString(0, content.length()));

        content.remove(12, 3);
        Assert.assertEquals("after caret remove",
                "!!!\nhello\nw_---ld\n",
                content.getString(0, content.length()));

        Assert.assertEquals("after caret remove",
                12, c2.getOffset());
        Assert.assertEquals("after caret remove",
                content.length(), c.getOffset());


        ///////////

        c = referenceImpl.createPosition(1);
        referenceImpl.insertString(0, "hello\nworld");
        Assert.assertEquals("after insertion",
                "hello\nworld\n".length(), c.getOffset());

        c2 = referenceImpl.createPosition(7);
        referenceImpl.insertString(0, "!!!\n");

        Assert.assertEquals("after 2 insertion",
                "!!!\nhello\nworld\n".length(), c.getOffset());
        Assert.assertEquals("after 2 insertion",
                11, c2.getOffset());

        // !!! \n hello \n w<c2>orld \n<c>
        referenceImpl.insertString(13, "---");

        Assert.assertEquals("after 3 insertion",
                "!!!\nhello\nwor---ld\n".length(), c.getOffset());
        Assert.assertEquals("after 3 insertion",
                11, c2.getOffset());

        System.err.println(referenceImpl.getString(0, referenceImpl.length()));
        // !!! \n hello \n w<c2>or---ld \n<c>
        referenceImpl.insertString(c2.getOffset(), "X");
        Assert.assertEquals("after 4 insertion",
                12, c2.getOffset());

        // !!! \n hello \n wX<c2>or---ld \n<c>

        referenceImpl.insertString(c2.getOffset() - 1, "_");
        Assert.assertEquals("after 5 insertion",
                13, c2.getOffset());

        // !!! \n hello \n w_X<c2>or---ld \n<c>

        System.err.println(referenceImpl.getString(0, referenceImpl.length()));

        referenceImpl.insertString(c2.getOffset() + 1, "_");
        Assert.assertEquals("after 5 insertion",
                13, c2.getOffset());

        // !!! \n hello \n w_X<c2>o_r---ld \n<c>

        referenceImpl.remove(c2.getOffset(), 1);
        Assert.assertEquals("after caret remove",
                "!!!\nhello\nw_X_r---ld\n",
                referenceImpl.getString(0, referenceImpl.length()));

        referenceImpl.remove(12, 3);
        Assert.assertEquals("after caret remove",
                "!!!\nhello\nw_---ld\n",
                referenceImpl.getString(0, referenceImpl.length()));

        Assert.assertEquals("after caret remove",
                12, c2.getOffset());
        Assert.assertEquals("after caret remove",
                referenceImpl.length(), c.getOffset());
    }

    @Test
    public void testCaretRemove() throws Exception {
        Position p1 = content.createPosition(0);
        Position p2 = content.createPosition(1);
        UndoableEdit u = content.insertString(0, "a\nb\nc");
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(6, p2.getOffset());
        u.undo();
        Assert.assertEquals("\n", content.getString(0, content.length()));
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(1, p2.getOffset());
        u.redo();
        Assert.assertEquals("a\nb\nc\n", content.getString(0, content.length()));
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(6, p2.getOffset());

        /////////////////////

        p1 = referenceImpl.createPosition(0);
        p2 = referenceImpl.createPosition(1);
        u = referenceImpl.insertString(0, "a\nb\nc");
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(6, p2.getOffset());
        u.undo();
        Assert.assertEquals("\n", referenceImpl.getString(0, referenceImpl.length()));
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(1, p2.getOffset());
        u.redo();
        Assert.assertEquals("a\nb\nc\n", referenceImpl.getString(0, referenceImpl.length()));
        Assert.assertEquals(0, p1.getOffset());
        Assert.assertEquals(6, p2.getOffset());
    }

    public JTextPane create() {
        JTextPane pane = new JTextPane(GuiReprValueDocumentEditor.ContentWrappingDocument.create(content));
        undoManager.putListenersAndActionsTo(pane);
        JFrame frame = createFrame(pane);
        frame.setSize(400, 400);
        return pane;
    }

    @Test
    public void testViewUndoRedo() throws Exception {
        JTextPane pane = runGet(this::create);

        run(() -> {
            try {
                Document doc = pane.getDocument();
                doc.insertString(0, "a\nb\nc", null);
                Assert.assertEquals("after insertion",
                        "a\nb\nc",
                        doc.getText(0, doc.getLength()));

                undoManager.getUndoAction().actionPerformed(null);

                Assert.assertEquals("after undo",
                        "",
                        doc.getText(0, doc.getLength()));

                undoManager.getRedoAction().actionPerformed(null);
                Assert.assertEquals("after redo",
                        "a\nb\nc",
                        doc.getText(0, doc.getLength()));
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }

    public static void main(String[] args) throws Exception {
        GuiReprValueDocumentEditorTest t = new GuiReprValueDocumentEditorTest();
        t.setUp();
        t.testViewUndoRedo();
    }
}
