package autogui.swing;

import autogui.GuiIncluded;

import java.util.Arrays;
import java.util.List;

@GuiIncluded
public class MatrixExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new MatrixExp());
    }

    @GuiIncluded public List<List<String>> data = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3"));
}
