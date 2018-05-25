package autogui.swing;

import org.junit.Test;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

public class LambdaPropertyTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new LambdaPropertyTest().test();
    }

    TestObj obj;
    JPanel objPane;

    JLabel strLabel;
    JLabel numLabel;
    JLabel imgLabel;
    JLabel pathLabel;
    JLabel enumLabel;
    JLabel boolLabel;
    JLabel textLabel;
    JLabel listLabel;

    @Test
    public void test() {
        run(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();

            obj = new TestObj();

            LambdaProperty.LambdaTextPane stringField = new LambdaProperty.LambdaTextPane(obj::getStr, obj::setStr);
            tabbedPane.add("StringField", pane(stringField));

            LambdaProperty.LambdaNumberSpinner numSpinner = new LambdaProperty.LambdaNumberSpinner(int.class, obj::getNum, obj::setNum);
            tabbedPane.add("NumberSpinner", pane(numSpinner));

            LambdaProperty.LambdaImagePane imgPane = new LambdaProperty.LambdaImagePane(obj::getImg, obj::setImg);
            tabbedPane.add("ImagePane", imgPane);

            LambdaProperty.LambdaFilePathPane pathPane = new LambdaProperty.LambdaFilePathPane(obj::getPath, obj::setPath);
            tabbedPane.add("FilePathPane", pane(pathPane));

            LambdaProperty.LambdaEnumComboBox enumPane = new LambdaProperty.LambdaEnumComboBox(TestLambdaEnum.class, obj::getEnumVal, obj::setEnumVal);
            tabbedPane.add("EnumComboBox", pane(enumPane));

            LambdaProperty.LambdaBooleanCheckBox boolPane = new LambdaProperty.LambdaBooleanCheckBox("Bool", obj::isBoolVal, obj::setBoolVal);
            tabbedPane.add("BooleanCheckBox", pane(boolPane));

            LambdaProperty.LambdaDocumentPlainEditorPane editPane = new LambdaProperty.LambdaDocumentPlainEditorPane(obj::getDoc);
            LambdaProperty.LambdaDocumentTextPane textPane = new LambdaProperty.LambdaDocumentTextPane(obj::getBuf);
            tabbedPane.add("TextPane", new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    editPane.wrapSwingScrollPane(true, false),
                    textPane.wrapSwingScrollPane(true, false)));

            LambdaProperty.LambdaCollectionTable listTable = new LambdaProperty.LambdaCollectionTable(TestElem.class, obj::getElems);
            listTable.addColumnBoolean("boolVal", TestElem::isBoolVal, TestElem::setBoolVal);
            tabbedPane.add("ListTable", listTable.wrapSwingScrollPane(true, true));

            LambdaProperty.LambdaLabel label = new LambdaProperty.LambdaLabel(obj::toString);
            tabbedPane.add("Label", pane(label));

            objPane = new JPanel();
            {
                objPane.setLayout(new BoxLayout(objPane, BoxLayout.Y_AXIS));
                JButton updateButton = new JButton("Update");
                updateButton.addActionListener(e -> update());
                objPane.add(updateButton);

                strLabel = new JLabel();
                objPane.add(strLabel);

                numLabel = new JLabel();
                objPane.add(numLabel);

                imgLabel = new JLabel();
                objPane.add(imgLabel);

                pathLabel = new JLabel();
                objPane.add(pathLabel);

                enumLabel = new JLabel();
                objPane.add(enumLabel);

                boolLabel = new JLabel();
                objPane.add(boolLabel);

                textLabel = new JLabel();
                objPane.add(textLabel);

                listLabel = new JLabel();
                objPane.add(listLabel);

                update();
            }
            tabbedPane.add("Object", objPane);

            testFrame(tabbedPane).setSize(1000, 500);
        });
    }

    public void update() {
        {
            strLabel.setText(obj.getStr());
            numLabel.setText("" + obj.getNum());;

            Image img = obj.getImg();
            if (img != null) {
                imgLabel.setIcon(new ImageIcon(img));
            } else {
                imgLabel.setIcon(null);
            }
            imgLabel.setText("" + img);

            pathLabel.setText("" + obj.getPath());;

            enumLabel.setText("" + obj.getEnumVal());

            boolLabel.setText("" + obj.isBoolVal());

            try {
                textLabel.setText("" + obj.getDoc().getText(0, obj.getDoc().getLength()) + " : " + obj.getBuf().toString());
            } catch (Exception ex) {
                textLabel.setText("" + ex);
            }

            listLabel.setText("" + obj.getElems());

        }
        objPane.revalidate();
        objPane.repaint();
    }

    public JPanel pane(JComponent comp) {
        JPanel pane = new JPanel();
        pane.add(comp);
        return pane;
    }


    public enum TestLambdaEnum {
        hello, world;
    }

    public static class TestObj {
        protected String str = "hello";
        protected int num;
        protected Image img;
        protected Path path;
        protected TestLambdaEnum enumVal;
        protected boolean boolVal;
        protected PlainDocument doc = new PlainDocument();
        protected StringBuilder buf = new StringBuilder();

        protected ArrayList<TestElem> elems = new ArrayList<>();

        public void setStr(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public Image getImg() {
            return img;
        }

        public void setImg(Image img) {
            this.img = img;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public void setEnumVal(TestLambdaEnum enumVal) {
            this.enumVal = enumVal;
        }

        public TestLambdaEnum getEnumVal() {
            return enumVal;
        }

        public void setBoolVal(boolean boolVal) {
            this.boolVal = boolVal;
        }

        public boolean isBoolVal() {
            return boolVal;
        }

        public PlainDocument getDoc() {
            return doc;
        }

        public StringBuilder getBuf() {
            return buf;
        }

        public ArrayList<TestElem> getElems() {
            if (elems.isEmpty()) {
                for (int i = 0; i < 20; ++i) {
                    TestElem e = new TestElem();
                    elems.add(e);
                    e.setBoolVal(i % 2 == 0);
                }
            }
            return elems;
        }

        @Override
        public String toString() {
            return "test " + Instant.now();
        }
    }

    public static class TestElem {
        protected boolean boolVal;

        public boolean isBoolVal() {
            return boolVal;
        }

        public void setBoolVal(boolean boolVal) {
            this.boolVal = boolVal;
        }

        @Override
        public String toString() {
            return "{" + boolVal + "}";
        }
    }
}
