package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.io.File;

@GuiIncluded
public class BooleanExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new BooleanExp());
    }

    boolean flag = true;
    @GuiIncluded
    public void setFlag(boolean flag) {
        System.err.println(this.flag + " -> " + flag);
        this.flag = flag;
    }
    @GuiIncluded
    public boolean isFlag() {
        return flag;
    }

    File file;

    @GuiIncluded
    public File getFile() {
        return file;
    }
    @GuiIncluded
    public void setFile(File file) {
        this.file = file;
    }

    @GuiIncluded
    public void run() {
        System.err.println("flag: " + flag);
    }
}
