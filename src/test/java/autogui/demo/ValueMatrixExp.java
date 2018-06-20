package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.List;

@GuiIncluded
public class ValueMatrixExp {

    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ValueMatrixExp());
    }

    @GuiIncluded public List<List<Cell>> list = new ArrayList<>();

    {
        for (int j = 0; j < 10; ++j) {
            List<Cell> row = new ArrayList<>();
            for (int i = 0; i < j + 1; ++i) {
                row.add(new Cell("item-" + i + "," + j, i * j));
            }
            list.add(row);
        }
    }

    @GuiIncluded
    public static class Cell {
        @GuiIncluded public String name;
        @GuiIncluded public int value;

        public Cell(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @GuiIncluded
    public void show(List<int[]> items) {
        for (int[] i : items) {
            System.err.println("(" + i[0] + "," + i[1] + ")");
        }
    }
}
