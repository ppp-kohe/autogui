package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiInitAction;
import org.autogui.base.annotation.GuiInitTable;
import org.autogui.base.annotation.GuiInitTableColumn;
import org.autogui.base.annotation.GuiInitWindow;
import org.autogui.swing.AutoGuiShell;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@GuiIncluded
@GuiInits(window = @GuiInitWindow(width = 400, height = 150))
public class TableSubPropDemo {
    List<Elem> elems = new ArrayList<>();

    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TableSubPropDemo());
    }

    public TableSubPropDemo() {
        elems.add(new Elem("hello"));
        elems.add(new Elem("world"));
        elems.add(new Elem("!!!!"));
    }

    String str = "";
    @GuiIncluded  public void setStr(String str) {
        this.str = str;
    }
    @GuiIncluded public String str() {
        return str;
    }

    @GuiIncluded(description = "add a new item")
//    @GuiInits(action = @GuiInitAction(confirm = true))
    public void addItem() {
        elems.add(new Elem(str));
        elems = new ArrayList<>(elems);
    }

    @GuiInits(table = @GuiInitTable(rowFitToContent = true, dynamicColumnAutoResize = true))
    @GuiIncluded public List<Elem> elems() {
        return elems;
    }

    @GuiInits(action = @GuiInitAction(confirm = true))
    @GuiIncluded public void selectAction(List<Elem> es) {
        System.err.println("selected " + es);
    }

    @GuiIncluded
    public static class Elem {
        SubElem subElem;
        String name;
        List<DynSubElem> dynSubElems;

        public Elem(String name) {
            this.name = name;
            subElem = new SubElem((name.length() % 2) == 0, name.length());
            dynSubElems = List.of(new DynSubElem(name.toUpperCase()), new DynSubElem(name.toLowerCase()));
        }

        @GuiIncluded(index = 10) public SubElem getSubElem() {
            return subElem;
        }
        @GuiIncluded(index = 20) public String name() {
            return name;
        }

        @GuiIncluded(index = 30) public List<DynSubElem> getDynSubElems() {
            return dynSubElems;
        }
    }

    @GuiIncluded
    public static class SubElem {
        boolean flag;
        int num;
        String s;

        public SubElem(boolean flag, int num) {
            this.flag = flag;
            this.num = num;
            s = String.format("%x", num);
        }

        @GuiIncluded public boolean isFlag() {
            return flag;
        }

        @GuiIncluded public int getNum() {
            return num;
        }

        @GuiIncluded public void setS(String s) {
            this.s = s;
        }

        @GuiInits(tableColumn = @GuiInitTableColumn(width = 300, sortOrder = SortOrder.ASCENDING))
        @GuiIncluded  public String getS() {
            return s;
        }
    }

    @GuiIncluded
    public static class DynSubElem {
        String str;

        public DynSubElem(String str) {
            this.str = str;
        }

        @GuiIncluded public String getStr() {
            return str;
        }

        @GuiIncluded public void setStr(String str) {
            this.str = str;
        }
        @GuiInits(action = @GuiInitAction(confirm = true))
        @GuiIncluded public void dynamicAction() {
            System.err.println("action " + this);
        }
    }
}
