package autogui.swing;

import autogui.GuiIncluded;
import autogui.GuiListSelectionCallback;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.SwingDeferredRunner;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoGuiShellExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TestClass());
    }

    @GuiIncluded
    public static class TestClass {
        protected TestTopPane topPane = new TestTopPane();
        protected TestValueList valueList = new TestValueList();
        protected TestNumbers numbers = new TestNumbers();
        protected TestDoc doc = new TestDoc();
        protected TestOther other = new TestOther();

        @GuiIncluded(index = 1, description = "the top pane", keyStroke = "1")
        public TestTopPane getTopPane() {
            return topPane;
        }

        @GuiIncluded(index = 2, description = "a immutable value list", keyStroke = "2")
        public TestValueList getValueList() {
            return valueList;
        }

        @GuiIncluded(index = 3, description = "number spinners", keyStroke = "3")
        public TestNumbers getNumbers() {
            return numbers;
        }

        @GuiIncluded(index = 4, description = "document editor", keyStroke = "4")
        public TestDoc getDoc() {
            return doc;
        }

        @GuiIncluded(keyStroke = "5")
        public TestOther getOther() {
            return other;
        }
    }

    @GuiIncluded
    public static class TestTopPane {
        @GuiIncluded(index = 0, description = "<html><body>hello, <B>w</B>orld!</body></html>", keyStroke = "P")
        public String hello = "Hello";

        private Path dir = Paths.get(".");

        @GuiIncluded(index = 1)
        public Image image;

        @GuiIncluded
        public void setDir(Path dir) {
            this.dir = dir;
            if (dir.getFileName().toString().endsWith(".png")) {
                try {
                    image = ImageIO.read(dir.toFile());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        @GuiIncluded(index = 2)
        public Path getDir() {
            return dir;
        }

        @GuiIncluded(index = 3)
        public void helloWorld() {
            System.err.println("hello");
        }
        @GuiIncluded(index = 4)
        public void thankYou() {
            System.err.println("thankx");
        }

        protected List<TestRow> rows = new ArrayList<>();
        {
            int idx = 0;
            for (String w : GuiSwingIcons.getInstance().getIconWords()) {
                Icon i = GuiSwingIcons.getInstance().getIcon(w);
                if (i instanceof GuiSwingIcons.ResourceIcon) {
                    GuiSwingIcons.ResourceIcon ri =(GuiSwingIcons.ResourceIcon) i;
                    Image img = ri.getImage();
                    TestRow r = new TestRow();
                    r.x = ri.getIconWidth();
                    r.y = ri.getIconHeight();
                    r.name = w;
                    r.icon = img;
                    r.setEven(idx % 2 == 0);
                    rows.add(r);
                    ++idx;
                }
            }
        }

        @GuiIncluded(index = 5)
        public List<TestRow> getList() {
            return rows;
        }
    }

    @GuiIncluded
    public static class TestValueList {
        @GuiIncluded
        public List<String> values = new ArrayList<>();

        public TestValueList() {
            for (int i = 0; i < 100; ++i) {
                values.add("value-" + i);
            }
        }

        @GuiIncluded(keyStroke = "P")
        public void updateList() {
            values = new ArrayList<>(values.stream()
                    .map(e -> e + "!")
                    .collect(Collectors.toList()));
        }

        @GuiIncluded @GuiListSelectionCallback
        public void select(List<String> items) {
            System.err.println("selected : " + items);
        }

        @GuiIncluded(keyStroke = "O") @GuiListSelectionCallback(index = true)
        public void selectRows(List<Integer> rows) {
            System.err.println("selected rows: " + rows);
        }
    }

    @GuiIncluded
    public static class TestNumbers {
        private int intValue;
        private float floatValue;
        private long longValue;
        private double doubleValue;
        private Integer intObj;
        private BigInteger bigIntegerValue = BigInteger.ZERO;
        private BigDecimal bigDecimalValue = BigDecimal.ZERO;

        @GuiIncluded(index = 1) public int getIntValue() {
            return intValue;
        }

        @GuiIncluded public void setIntValue(int intValue) {
            this.intValue = intValue;
            System.err.println("int " + intValue);
        }

        @GuiIncluded(index = 2) public float getFloatValue() {
            return floatValue;
        }

        @GuiIncluded public void setFloatValue(float floatValue) {
            this.floatValue = floatValue;
            System.err.println("float " + floatValue);
        }

        @GuiIncluded(index = 3) public long getLongValue() {
            return longValue;
        }

        @GuiIncluded public void setLongValue(long longValue) {
            this.longValue = longValue;
            System.err.println("long " + longValue);
        }

        @GuiIncluded(index = 4) public double getDoubleValue() {
            return doubleValue;
        }

        @GuiIncluded public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
            System.err.println("double " + doubleValue);
        }

        @GuiIncluded(index = 5) public Integer getIntObj() {
            return intObj;
        }

        @GuiIncluded public void setIntObj(Integer intObj) {
            this.intObj = intObj;
            System.err.println("intObj " + intObj);
        }

        @GuiIncluded(index = 6) public BigInteger getBigIntegerValue() {
            return bigIntegerValue;
        }

        @GuiIncluded public void setBigIntegerValue(BigInteger bigIntegerValue) {
            this.bigIntegerValue = bigIntegerValue;
            System.err.println("bigInt " + bigIntegerValue);
        }

        @GuiIncluded(index = 7) public BigDecimal getBigDecimalValue() {
            return bigDecimalValue;
        }

        @GuiIncluded public void setBigDecimalValue(BigDecimal bigDecimalValue) {
            this.bigDecimalValue = bigDecimalValue;
            System.err.println("bigDecimal " + bigDecimalValue);
        }
    }

    @GuiIncluded
    public static class TestOther {
        @GuiIncluded public boolean flag;
        @GuiIncluded public TestEnum selection = TestEnum.Hello;
        @GuiIncluded public Object label = Arrays.asList("hello", "world");
        @GuiIncluded public TestSubObj subObj = new TestSubObj();

        @GuiIncluded public String getReadOnly() {
            return "hello, world";
        }
    }

    @GuiIncluded
    public static class TestSubObj {
        @GuiIncluded public String str = "";
    }

    public enum TestEnum {
        Hello,
        World
    }

    @GuiIncluded
    public static class TestDoc {
        protected StyledDocument doc;
        @GuiIncluded
        public StyledDocument getDoc() {
            if (doc == null) {
                System.err.println("before wait");
                SwingDeferredRunner.getDefaultService().execute(() -> System.err.println("hello"));
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {}
                System.err.println("after wait eventThread=" + SwingUtilities.isEventDispatchThread());
                doc = new DefaultStyledDocument();
            }
            return doc;
        }
    }

    @GuiIncluded
    public static class TestRow {
        @GuiIncluded(index = 0) public int x;
        @GuiIncluded(index = 1) public int y;
        @GuiIncluded(index = 2) public String name;
        @GuiIncluded(index = 3) public Image icon;

        private TestEnum select = TestEnum.Hello;

        private boolean even;

        @GuiIncluded(index = 4) public boolean isEven() {
            return even;
        }

        @GuiIncluded
        public void setEven(boolean even) {
            this.even = even;
            select = (even ? TestEnum.Hello : TestEnum.World);
        }

        @GuiIncluded(index = 5)
        public TestEnum getSelect() {
            return select;
        }

        @GuiListSelectionCallback
        @GuiIncluded(keyStroke = "E")
        public void sayHello() {
            System.err.println(x + "," + y + "," + name + "," + icon);
        }
    }
}
