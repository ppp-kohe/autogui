package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@GuiIncluded
public class ValueListExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ValueListExp());
    }

    @GuiIncluded
    public List<Row> rows = new ArrayList<>();

    @GuiIncluded
    public void addRow() {
        Row r = new Row();
        r.setStr("row " + rows.size());
        r.setNum(rows.size());
        rows.add(r);
        rows = new ArrayList<>(rows);
        System.err.println("added " + rows.size());
    }

    @GuiIncluded
    public static class Row {

        String str;
        float num;
        boolean flag;
        EnumVal selection;
        Path path;
        Image image;

        public Row() { }

        public Row(String str, float num, boolean flag, EnumVal selection, Path path, Image image) {
            this.str = str;
            this.num = num;
            this.flag = flag;
            this.selection = selection;
            this.path = path;
            this.image = image;
        }

        @GuiIncluded(index = 1)
        public String getStr() {
            return str;
        }

        @GuiIncluded
        public void setStr(String str) {
            this.str = str;
        }

        @GuiIncluded(index = 2)
        public float getNum() {
            return num;
        }

        @GuiIncluded
        public void setNum(float num) {
            this.num = num;
        }

        @GuiIncluded(index = 3)
        public boolean isFlag() {
            return flag;
        }

        @GuiIncluded
        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        @GuiIncluded(index = 4)
        public EnumVal getSelection() {
            return selection;
        }

        @GuiIncluded
        public void setSelection(EnumVal selection) {
            this.selection = selection;
        }

        @GuiIncluded
        public void setPath(Path path) {
            this.path = path;
        }

        @GuiIncluded(index = 5)
        public Path getPath() {
            return path;
        }

        @GuiIncluded(index = 6)
        public Image getImage() {
            return image;
        }

        @GuiIncluded
        public void setImage(Image image) {
            this.image = image;
        }
    }

    public enum EnumVal {
        Hello,
        World
    }
}
