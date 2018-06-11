package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.awt.*;
import java.nio.file.Path;

@GuiIncluded
public class ValuePaneExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ValuePaneExp());
    }

    String str;
    float num;
    boolean flag;
    ValueListExp.EnumVal selection;
    Path path;
    Image image;

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
    public ValueListExp.EnumVal getSelection() {
        return selection;
    }

    @GuiIncluded
    public void setSelection(ValueListExp.EnumVal selection) {
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
