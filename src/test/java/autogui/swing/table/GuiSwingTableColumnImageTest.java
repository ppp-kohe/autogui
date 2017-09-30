package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeObject;
import autogui.base.type.GuiTypeValue;
import autogui.swing.*;
import autogui.swing.mapping.GuiReprValueImagePane;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnImageTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingTableColumnImageTest().test();
    }
    
    @Test
    public void test() {

        GuiReprSet set = new GuiReprSet();
        GuiReprCollectionTable repr = new GuiReprCollectionTable(set);

        set.add(new GuiReprCollectionElement(set));
        set.add(new GuiReprValueImagePane());
        set.add(new GuiReprValueStringField());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));

        TestImageList l = new TestImageList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueImagePane.class, new GuiSwingTableColumnImage());
        sSet.addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString());
        sSet.addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(sSet));
        sSet.addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(sSet));
        sSet.addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(sSet));
        sSet.addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(sSet));

        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            GuiSwingView view = (GuiSwingView) sSet.view(context);
            JComponent comp = view.createView(context);
            testFrame(comp);
            return comp;
        });

        GuiSwingViewCollectionTable.CollectionTable table = runQuery(component,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(GuiSwingViewCollectionTable.CollectionTable.class, 0));
    }

    public static class TestImageList extends GuiTypeMemberProperty {
        public List<TestImageElem> es = new ArrayList<>();
        public TestImageList() {
            super("hello-list");

            es.add(new TestImageElem("aaaa", getImage(Color.pink)));
            es.add(new TestImageElem("bbbb",  getImage(new Color(100, 120, 140))));
            es.add(new TestImageElem("cccc", getImage(new Color(200, 10, 180, 50))));

            GuiTypeObject e = new GuiTypeObject(TestImageElem.class);
            e.addProperties(new TestImageStrProp(), new TestImageProp());
            setType(new GuiTypeCollection(List.class, e));
        }

        private Image getImage(Color color) {
            BufferedImage img = new BufferedImage(500, 1000, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = img.createGraphics();
            {
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, 500, 1000);
                Ellipse2D.Float e = new Ellipse2D.Float(10, 10, 480, 980);
                g.setColor(color);
                g.fill(e);
            }
            g.dispose();
            return img;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, es);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            System.err.println("set list: " + value);
            es = (List<TestImageElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestImageStrProp extends GuiTypeMemberProperty {
        public TestImageStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestImageElem e = (TestImageElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.hello);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestImageElem e = (TestImageElem) target;
            System.err.println("set hello: " + target + " " + value);
            if (e != null) {
                e.hello = (String) value;
            }
            return null;
        }
        @Override
        public boolean isWritable() {
            return true;
        }
    }
    public static class TestImageProp extends GuiTypeMemberProperty {
        public TestImageProp() {
            super("world");
            setType(new GuiTypeValue(Image.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestImageElem e = (TestImageElem) target;
            System.err.println("get world: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.world);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestImageElem e = (TestImageElem) target;
            System.err.println("set world: " + target + " " + value);
            if (e != null) {
                e.world = (Image) value;
            }
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestImageElem {
        public String hello;
        public Image world;

        public TestImageElem(String hello, Image world) {
            this.hello = hello;
            this.world = world;
        }
    }
}
