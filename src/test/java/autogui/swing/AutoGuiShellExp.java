package autogui.swing;

import autogui.AutoGuiShell;
import autogui.GuiIncluded;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AutoGuiShellExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TestClass());
    }

    @GuiIncluded
    public static class TestClass {
        @GuiIncluded(index = 0)
        public String hello = "Hello";

        private Path dir = Paths.get(".");

        @GuiIncluded(index = 1)
        public Image image;

        @GuiIncluded
        public void setDir(Path dir) {
            this.dir = dir;
            if (dir.getFileName().toString().endsWith(".png")) {
                try {
                    image = ImageIO.read(dir.toFile());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        @GuiIncluded(index = 2)
        public Path getDir() {
            return dir;
        }

        @GuiIncluded(index = 3)
        public void helloWorld() {
            System.err.println("hello");
        }
        @GuiIncluded(index = 4)
        public void thankYou() {
            System.err.println("thankx");
        }
    }
}
