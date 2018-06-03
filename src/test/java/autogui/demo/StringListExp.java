package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.List;

@GuiIncluded
public class StringListExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new StringListExp());
    }

    @GuiIncluded
    public List<Row> rows = new ArrayList<>();

    @GuiIncluded
    public void addRow() {
        rows.add(new Row("row " + rows.size()));
        rows = new ArrayList<>(rows);
        System.err.println("added " + rows.size());
    }

    @GuiIncluded
    public static class Row {

        public String value;

        public Row(String value) {
            this.value = value;
        }

        @GuiIncluded
        public void setValue(String value) {
            this.value = value;
        }
        @GuiIncluded
        public String getValue() {
            return value;
        }
    }
}
