package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.annotation.GuiInitTableColumnString;
import org.autogui.swing.AutoGuiShell;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@GuiIncluded
public class ValueListDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ValueListDemo());
    }

    @GuiIncluded
    public List<Row> rows = new ArrayList<>();

    @GuiIncluded
    @GuiListSelectionUpdater
    public List<Row> addRow() {
        Row r = new Row();
        r.setStr("row " + rows.size());
        r.setNum(rows.size());
        rows.add(r);
        rows = new ArrayList<>(rows);
        System.err.println("added " + rows.size());
        return Collections.singletonList(r);
    }

    @GuiIncluded
    public void selection(List<Row> rows, String name) {
        System.out.println(name + " : " + rows);
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

        @GuiIncluded(index = 1, description = "str property")
        @GuiInits(tableColumnString = @GuiInitTableColumnString(editFinishByEnterAndKey = true))
        public String getStr() {
            return str;
        }

        @GuiIncluded
        public void setStr(String str) {
            this.str = str;
        }

        @GuiIncluded(index = 2, description = "num property")
        public float getNum() {
            return num;
        }

        @GuiIncluded
        public void setNum(float num) {
            this.num = num;
        }

        @GuiIncluded(index = 3, description = "flag property")
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

        @Override
        public String toString() {
            return "Row{" +
                    "str='" + str + '\'' +
                    ", num=" + num +
                    ", flag=" + flag +
                    ", selection=" + selection +
                    ", path=" + path +
                    ", image=" + image +
                    '}';
        }
    }

    public enum EnumVal {
        Hello,
        World
    }
}
