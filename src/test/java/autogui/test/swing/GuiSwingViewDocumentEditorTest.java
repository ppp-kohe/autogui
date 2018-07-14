package autogui.test.swing;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewDocumentEditor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewDocumentEditorTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewDocumentEditorTest t = new GuiSwingViewDocumentEditorTest();
        t.setUp();
        t.testViewDocSetText();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewDocumentEditor doc;
    JFrame frame;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        doc = new GuiSwingViewDocumentEditor();
    }

    @After
    public void tearDown() {
        frame.dispose();
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public StringBuilder value = new StringBuilder();
    }

    //////////////

    public GuiSwingViewDocumentEditor.PropertyDocumentTextPane create() {
        JComponent c = doc.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewDocumentEditor.PropertyDocumentTextPane.class);
    }

    @Test
    public void testViewDoc() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);

        run(this::runWait);
        obj.value = new StringBuilder("hello, world");
        contextProp.updateSourceFromRoot();

        Assert.assertEquals("listener update",
                "hello, world",
                runGet(propText::getText));
    }

    @Test
    public void testViewDocSetText() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(this::runWait);

        run(() -> propText.setText("hello, world"));
        run(this::runWait);

        Assert.assertEquals("listener update",
                "hello, world",
                obj.value.toString());
    }

    @Test
    public void testViewDocSet() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(() -> propText.setSwingViewValueWithUpdate(new StringBuilder("hello, world")));
        run(this::runWait);

        Assert.assertEquals("view update",
                "hello, world",
                obj.value.toString());
        Assert.assertEquals("view update: text",
                "hello, world",
                runGet(propText::getText));
    }
}
