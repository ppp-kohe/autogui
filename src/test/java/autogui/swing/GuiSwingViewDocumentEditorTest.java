package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.KeyEvent;

public class GuiSwingViewDocumentEditorTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewDocumentEditorTest().test();
    }
    public GuiReprValue.ObjectSpecifier getSpecifier() {
        return GuiReprValue.NONE;
    }


    @Test
    public void test() throws Exception {
        GuiSwingViewDocumentEditor doc = new GuiSwingViewDocumentEditor();

        TestDocProp prop = new TestDocProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueDocumentEditor());
        JComponent c = runGet(() -> {
            JComponent comp = doc.createView(context, this::getSpecifier);
            testFrame(comp);
            return comp;
        });

        context.updateSourceFromRoot();

        JEditorPane pane = runQuery(c,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(JEditorPane.class, 0));
        run(() -> {
            try {
                System.err.println("Pane doc: " + System.identityHashCode(pane.getDocument()));
                System.err.println("prop doc: " + System.identityHashCode(prop.builder));
                pane.getDocument().insertString(0, "Hello, world", null);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        Thread.sleep(1000);

        String str = prop.builder.getText(0, prop.builder.getLength());
        Assert.assertEquals("Hello, world", str);
    }



    @Test
    public void testContent() throws Exception {
        GuiSwingViewDocumentEditor doc = new GuiSwingViewDocumentEditor();

        TestContentProp prop = new TestContentProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueDocumentEditor());
        JComponent c = runGet(() -> {
            JComponent comp = doc.createView(context, this::getSpecifier);
            testFrame(comp);
            return comp;
        });

        context.updateSourceFromRoot();

        JEditorPane pane = runQuery(c,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(JEditorPane.class, 0));
        run(() -> {
            try {
                pane.getDocument().insertString(0, "Hello, world", null);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        Thread.sleep(1000);

        String str = prop.content.getString(0, prop.content.length());
        Assert.assertEquals("Hello, world", str.trim());
    }


    @Test
    public void testBuilder() throws Exception {
        GuiSwingViewDocumentEditor doc = new GuiSwingViewDocumentEditor();

        TestBuilderProp prop = new TestBuilderProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueDocumentEditor());
        JComponent c = runGet(() -> {
            JComponent comp = doc.createView(context, this::getSpecifier);
            testFrame(comp);
            return comp;
        });

        context.updateSourceFromRoot();

        JEditorPane pane = runQuery(c,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(JEditorPane.class, 0));

        runError(() -> pane.getDocument().insertString(0, "Hello\n, world", null));

        Thread.sleep(1000);

        String str = prop.content.toString();
        Assert.assertEquals("Hello\n, world", str.trim());

        keyType("\n");
    }


    @Test
    public void testBuilderEdit() throws Exception {
        StringBuilder buf = new StringBuilder();
        PlainDocument doc = new PlainDocument(new GuiReprValueDocumentEditor.StringBuilderContent(buf));
        PlainDocument ref = new PlainDocument();

        Assert.assertEquals(ref.getLength(), doc.getLength());
        Assert.assertEquals(ref.getText(0, ref.getLength()),
                doc.getText(0, doc.getLength()));

        runError(() -> ref.insertString(0, "hello\nworld\n", null));
        runError(() -> doc.insertString(0, "hello\nworld\n", null));

        Assert.assertEquals(ref.getLength(), doc.getLength());
        Assert.assertEquals(ref.getText(0, ref.getLength()),
                doc.getText(0, doc.getLength()));

        runError(() -> ref.insertString(5, "!!!", null));
        runError(() -> doc.insertString(5, "!!!", null));

        Assert.assertEquals(ref.getLength(), doc.getLength());
        Assert.assertEquals(ref.getText(0, ref.getLength()),
                doc.getText(0, doc.getLength()));

        runError(() -> ref.remove(9, 4));
        runError(() -> doc.remove(9, 4));

        Assert.assertEquals(ref.getLength(), doc.getLength());
        Assert.assertEquals(ref.getText(0, ref.getLength()),
                doc.getText(0, doc.getLength()));

        System.err.println(doc.getText(0, doc.getLength()));
    }

    @Test
    public void testEditPane() {

        StringBuilder buf = new StringBuilder();
        PlainDocument doc = new PlainDocument(new GuiReprValueDocumentEditor.StringBuilderContent(buf));
        //PlainDocument doc = new PlainDocument(new StringContent());
        run(() -> {
            JEditorPane pane = new JEditorPane();
            pane.setDocument(doc);
            testFrame(pane);
        });

        keyType("hello\nworld");
        keyTypeAtOnce(KeyEvent.VK_LEFT, KeyEvent.VK_LEFT);
        keyTypeAtOnce(KeyEvent.VK_BACK_SPACE, KeyEvent.VK_BACK_SPACE,
                KeyEvent.VK_BACK_SPACE, KeyEvent.VK_BACK_SPACE);
        String s = runGet(() -> doc.getText(0, doc.getLength()));
        Assert.assertEquals("hellold", s);
    }


    public static class TestDocProp extends GuiTypeMemberProperty {
        public Document builder = new PlainDocument();
        public TestDocProp() {
            super("hello");
            setType(new GuiTypeValue(Document.class));
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            builder = (Document) value;
            return null;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, builder);
        }
    }

    public static class TestContentProp extends GuiTypeMemberProperty {
        public AbstractDocument.Content content = new GapContent();

        public TestContentProp() {
            super("hello");
            setType(new GuiTypeValue(AbstractDocument.Content.class));
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            content = (AbstractDocument.Content) value;
            return null;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, content);
        }
    }

    public static class TestBuilderProp extends GuiTypeMemberProperty {
        public StringBuilder content = new StringBuilder();

        public TestBuilderProp() {
            super("hello");
            setType(new GuiTypeValue(StringBuilder.class));
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            content = (StringBuilder) content;
            return null;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, content);
        }
    }
}
