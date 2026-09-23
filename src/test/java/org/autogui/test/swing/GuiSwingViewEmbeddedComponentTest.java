package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewEmbeddedComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;

import static org.junit.jupiter.api.Assertions.*;

public class GuiSwingViewEmbeddedComponentTest extends GuiSwingTestCase {


    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;

    TestObj obj;

    JFrame frame;

    GuiSwingViewEmbeddedComponent comp;

    public GuiSwingViewEmbeddedComponentTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);
        contextProp = context.getChildByName("value");

        comp = new GuiSwingViewEmbeddedComponent();
    }

    @AfterEach
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

        public TestObj() {
        }

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
        @Serial
        private static final long serialVersionUID = 1L;
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
        return GuiSwingView.findDescendantByType(c, GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane.class);
    }

    @Test
    public void testViewEmbeddedUpdate() {
        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);

        contextProp.updateSourceFromRoot();
        run(this::runWait);
        assertEquals(obj.value, c.getSwingViewValue(), "value");
        assertTrue(obj.createdInEvent, "getValue() is invoked within the event thread");

    }

    @Test
    public void testViewEmbeddedUpdateWithWait() {
        timeFactor = 1;

        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);
        obj.waitTime = 2000;
        contextProp.updateSourceFromRoot();
        assertNull(c.getSwingViewValue(), "value timeout");
        System.err.println("after check");
        runWait(2000);
        assertNotNull(c.getSwingViewValue(), "value after waiting");
        assertEquals(obj.value, c.getSwingViewValue(), "value after waiting");

        assertTrue(((TestPane) obj.value).paintCount > 0, "painted");
    }


    @Test
    public void testViewEmbeddedUpdateWitWait() {
        GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane c = runGet(this::create);

        TestPane pane = runGet(() -> new TestPane("TEST"));
        run(() -> c.setSwingViewValueWithUpdate(pane));
        runWait();
        assertEquals(pane, obj.value, "value after set");
    }
}
