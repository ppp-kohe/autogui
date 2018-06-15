package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@GuiIncluded
public class ValuePaneExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new ValuePaneExp());
    }

    NonEditablePane immutable = new NonEditablePane();
    EditablePane mutable = new EditablePane();

    public ValuePaneExp() {
        immutable.setStr("hello, world");
        immutable.setFlag(true);
        immutable.setNum(123.456f);
        immutable.setSelection(ValueListExp.EnumVal.Hello);
        immutable.setPath(Paths.get("src/main/resources/autogui/swing/icons"));
        try {
            immutable.setImage(
                    ImageIO.read(Paths.get("src/main/resources/autogui/swing/icons/action@4x.png").toFile()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @GuiIncluded
    public NonEditablePane getImmutable() {
        return immutable;
    }

    @GuiIncluded
    public EditablePane getMutable() {
        return mutable;
    }

    @GuiIncluded
    public static class NonEditablePane {
        protected String str;
        protected float num;
        protected boolean flag;
        protected ValueListExp.EnumVal selection;
        protected Path path;
        protected Image image;

        @GuiIncluded(index = 1)
        public String getStr() {
            return str;
        }

        @GuiIncluded(index = 2)
        public float getNum() {
            return num;
        }

        @GuiIncluded(index = 3)
        public boolean isFlag() {
            return flag;
        }

        @GuiIncluded(index = 4)
        public ValueListExp.EnumVal getSelection() {
            return selection;
        }

        @GuiIncluded(index = 5)
        public Path getPath() {
            return path;
        }

        @GuiIncluded(index = 6)
        public Image getImage() {
            return image;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public void setNum(float num) {
            this.num = num;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public void setSelection(ValueListExp.EnumVal selection) {
            this.selection = selection;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public void setImage(Image image) {
            this.image = image;
        }
    }


    @GuiIncluded
    public static class EditablePane extends NonEditablePane {

        @GuiIncluded @Override
        public void setStr(String str) {
            super.setStr(str);
        }

        @GuiIncluded @Override
        public void setNum(float num) {
            super.setNum(num);
        }

        @GuiIncluded @Override
        public void setFlag(boolean flag) {
            super.setFlag(flag);
        }

        @GuiIncluded @Override
        public void setSelection(ValueListExp.EnumVal selection) {
            super.setSelection(selection);
        }

        @GuiIncluded @Override
        public void setPath(Path path) {
            super.setPath(path);
        }

        @GuiIncluded @Override
        public void setImage(Image image) {
            super.setImage(image);
        }
    }
}
