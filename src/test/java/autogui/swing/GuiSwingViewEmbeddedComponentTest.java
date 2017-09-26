package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.swing.mapping.GuiReprEmbeddedComponent;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

public class GuiSwingViewEmbeddedComponentTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewEmbeddedComponentTest().test();
    }

    @Test
    public void test() throws Exception {
        GuiSwingViewEmbeddedComponent com = new GuiSwingViewEmbeddedComponent();
        TestComp prop = new TestComp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprEmbeddedComponent());
        context.updateSourceFromRoot();
        JComponent component = runGet(() -> {
            JComponent c = com.createView(context);
            testFrame(c);
            return c;
        });

        Thread.sleep(1000);
        SwingUtilities.windowForComponent(component).pack();

        System.out.println(component);
        TestPane p = runQuery(component, query(GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane.class, 0)
                .cat(TestPane.class, 0));
        Assert.assertNotNull(p);
    }

    public static class TestComp extends GuiTypeMemberProperty {
        protected JComponent component;

        public TestComp() {
            super("hello");
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            if (component == null) {
                component = new TestPane();
            }
            return compareGet(prevValue, component);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            component = (JComponent) value;
            return null;
        }
    }

    public static class TestPane extends JPanel {
        public TestPane() {
            setPreferredSize(new Dimension(500, 500));
            setBackground(new Color(140, 180, 210));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.black);
            g.drawString("hello", 20, 20);
        }
    }
}
