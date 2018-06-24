package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.List;

@GuiIncluded
public class CellListExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new CellListExp());
    }

    @GuiIncluded public List<Cell> list = new ArrayList<>();
    {
        list.add(new Cell());
    }

    @GuiIncluded
    public static class Cell {
        @GuiIncluded public Member m = new Member();
    }

    @GuiIncluded
    public static class Member {
        @GuiIncluded public String str = "hello";
    }
}
