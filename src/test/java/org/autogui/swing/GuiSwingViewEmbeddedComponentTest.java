package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;

public class GuiSwingViewEmbeddedComponentTest extends GuiSwingTestCase {


    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;

    TestObj obj;

    JFrame frame;

    GuiSwingViewEmbeddedComponent comp;

    public GuiSwingViewEmbeddedComponentTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);
        contextProp = context.getChildByName("value");

        comp = new GuiSwingViewEmbeddedComponent();
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @GuiIncluded
    public static class TestObj {
        public JComponent value;
        public boolean createdInEvent;

        public long waitTime;

        public TestObj() {}

        @GuiIncluded
        public JComponent getValue() throws Exception {
            if (value == null) {
                System.err.println("start creating");
                Thread.sleep(waitTime);
                value = new TestPane("hello");
                createdInEvent = SwingUtilities.isEventDispatchThread();
            }
            return value;
        }

        @GuiIncluded
        public void setValue(JComponent value) {
            this.value = value;
        }
    }

    public static class TestPane extends JComponent {
        @Serial private static final long serialVersionUID = 1L;
        public int paintCount;
        public String label;
        @SuppressWarnings("this-escape")
        public TestPane(String label) {
            this.label = label;
            setPreferredSize(new Dimension(300, 300));
        }
        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.blue);
            g.drawString(label, 30, 30);
            ++paintCount;
        }
    }

    public GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane create() {
        JComponent c = comp.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane.class);
    }

    @Test
    public void testViewEmbeddedUpdate() {
        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);

        contextProp.updateSourceFromRoot();
        run(this::runWait);
        Assert.assertEquals("value", obj.value, c.getSwingViewValue());
        Assert.assertTrue("getValue() is invoked within the event thread", obj.createdInEvent);

    }

    @Test
    public void testViewEmbeddedUpdateWithWait() {
        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);
        obj.waitTime = 2000;
        contextProp.updateSourceFromRoot();
        Assert.assertNull("value timeout", c.getSwingViewValue());
        System.err.println("after check");
        runWait(2000);
        Assert.assertNotNull("value after waiting", c.getSwingViewValue());
        Assert.assertEquals("value after waiting", obj.value, c.getSwingViewValue());

        Assert.assertTrue("painted", ((TestPane) obj.value).paintCount > 0);
    }


    @Test
    public void testViewEmbeddedUpdateWitWait() {
        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);

        TestPane pane = runGet(() -> new TestPane("TEST"));
        run(() -> c.setSwingViewValueWithUpdate(pane));
        runWait();
        Assert.assertEquals("value after set", pane, obj.value);
    }
}
