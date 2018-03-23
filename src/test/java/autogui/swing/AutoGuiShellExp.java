package autogui.swing;

import autogui.AutoGuiShell;
import autogui.GuiIncluded;
import autogui.base.type.GuiTypeObject;
import autogui.swing.icons.GuiSwingIcons;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AutoGuiShellExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TestClass());
    }

    @GuiIncluded
    public static class TestClass {
        protected TestTopPane topPane = new TestTopPane();
        protected TestValueList valueList = new TestValueList();
        protected TestNumbers numbers = new TestNumbers();

        @GuiIncluded(index = 1)
        public TestTopPane getTopPane() {
            return topPane;
        }

        @GuiIncluded(index = 2)
        public TestValueList getValueList() {
            return valueList;
        }

        @GuiIncluded(index = 3)
        public TestNumbers getNumbers() {
            return numbers;
        }
    }

    @GuiIncluded
    public static class TestTopPane {
        @GuiIncluded(index = 0)
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
                    rows.add(r);
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
    }

    @GuiIncluded
    public static class TestNumbers {
        @GuiIncluded(index = 1) public int intValue;
        @GuiIncluded(index = 2) public float floatValue;
        @GuiIncluded(index = 3) public long longValue;
        @GuiIncluded(index = 4) public double doubleValue;
        @GuiIncluded(index = 5) public Integer intObj;
        @GuiIncluded(index = 6) public BigInteger bigIntegerValue = BigInteger.ZERO;
        @GuiIncluded(index = 7) public BigDecimal bigDecimalValue = BigDecimal.ZERO;
    }

    @GuiIncluded
    public static class TestRow {
        @GuiIncluded(index = 0) public int x;
        @GuiIncluded(index = 1) public int y;
        @GuiIncluded(index = 2) public String name;
        @GuiIncluded(index = 3) public Image icon;
    }
}
