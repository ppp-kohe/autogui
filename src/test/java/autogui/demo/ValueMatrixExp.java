package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.Arrays;
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
                row.add(new Cell("item-r[" + j + "]-c[" + i + "]", i * j));
            }
            list.add(row);
        }
    }

    @GuiIncluded
    public static class Cell {
        @GuiIncluded public String name;
        int value;

        public Cell(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @GuiIncluded
        public int getValue() {
            return value;
        }

        @GuiIncluded
        public void setValue(int value) {
            this.value = value;
        }

        @GuiIncluded
        public void hello() {
            System.err.println("cell-action: " + name + "," + value);
        }
    }

    @GuiIncluded
    public void show(List<int[]> items) {
        System.err.println("----- " + items.size());
        for (int[] i : items) {
            System.err.println(Arrays.toString(i));
        }
    }

    @GuiIncluded
    public void sum(List<List<Cell>> cells) {
        System.out.println(cells.stream().flatMapToInt(c -> c.stream().mapToInt(e -> e.value)).sum());
    }
}
