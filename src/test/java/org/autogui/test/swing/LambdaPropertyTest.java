package org.autogui.test.swing;

import org.autogui.swing.LambdaProperty;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LambdaPropertyTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        LambdaPropertyTest t = new LambdaPropertyTest();
        t.setUp();
        t.testLambdaCollectionTable();
    }

    JFrame frame;

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    ////////////// String

    String propString;

    public void setPropString(String propString) {
        this.propString = propString;
    }

    public String getPropString() {
        return propString;
    }

    LambdaProperty.LambdaStringPane createStringPane() {
        LambdaProperty.LambdaStringPane p = new LambdaProperty.LambdaStringPane(this::getPropString, this::setPropString);
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaStringPane() {
        setPropString("init");
        LambdaProperty.LambdaStringPane pane = runGet(this::createStringPane);

        Assert.assertEquals("init", runGet(pane::getSwingViewValue));
        Assert.assertEquals("init", runGet(() -> pane.getField().getText()));
        run(() -> {
            pane.getField().setText("update1");
        });

        runWait(600);
        Assert.assertEquals("update1", getPropString());

        setPropString("update2");
        pane.updateSwingViewSource();
        runWait();
        Assert.assertEquals("update2", runGet(pane::getSwingViewValue));
        Assert.assertEquals("update2", runGet(() -> pane.getField().getText()));
    }

    ////////////// Number

    int propInt;

    public void setPropInt(int propInt) {
        this.propInt = propInt;
    }

    public int getPropInt() {
        return propInt;
    }

    LambdaProperty.LambdaNumberSpinner createNumberSpinner() {
        LambdaProperty.LambdaNumberSpinner p = new LambdaProperty.LambdaNumberSpinner(Integer.class, this::getPropInt, this::setPropInt);
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaNumberSpinner() {
        setPropInt(123);
        LambdaProperty.LambdaNumberSpinner pane = runGet(this::createNumberSpinner);

        Assert.assertEquals(123, runGet(pane::getSwingViewValue));
        run(() -> pane.setValue(456));
        runWait(600);
        Assert.assertEquals(456, getPropInt());

        setPropInt(789);
        pane.updateSwingViewSource();
        runWait();
        Assert.assertEquals(789, runGet(pane::getSwingViewValue));
    }

    ////////////// Label

    LambdaProperty.LambdaLabel createLabel() {
        LambdaProperty.LambdaLabel p = new LambdaProperty.LambdaLabel(this::getPropString);
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaLabel() {
        setPropString("init");
        LambdaProperty.LambdaLabel pane = runGet(this::createLabel);

        Assert.assertEquals("init", runGet(pane::getSwingViewValue));
        Assert.assertEquals("init", runGet(pane::getText));
        setPropString("update");
        pane.updateSwingViewSource();

        runWait();
        Assert.assertEquals("update", runGet(pane::getSwingViewValue));
        Assert.assertEquals("update", runGet(pane::getText));
    }

    ////////////// Image

    Image propImage;

    public void setPropImage(Image propImage) {
        this.propImage = propImage;
    }

    public Image getPropImage() {
        return propImage;
    }

    public BufferedImage createImage1() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
        {
            Graphics2D g = image.createGraphics();
            {
                g.setPaint(Color.blue);
                g.fillRect(0, 0, 16, 16);
            }
            g.dispose();
        }
        return image;
    }

    public BufferedImage createImage2() {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_3BYTE_BGR);
        {
            Graphics2D g = image.createGraphics();
            {
                g.setPaint(Color.red);
                g.fillRect(0, 0, 24, 24);
            }
            g.dispose();
        }
        return image;
    }

    public BufferedImage createImage3() {
        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_3BYTE_BGR);
        {
            Graphics2D g = image.createGraphics();
            {
                g.setPaint(Color.green);
                g.fillRect(0, 0, 30, 30);
            }
            g.dispose();
        }
        return image;
    }

    LambdaProperty.LambdaImagePane createImagePane() {
        LambdaProperty.LambdaImagePane p = new LambdaProperty.LambdaImagePane(this::getPropImage, this::setPropImage);
        p.setPreferredSize(new Dimension(32, 32));
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaImagePane() {
        Image i1 = createImage1();
        setPropImage(i1);

        LambdaProperty.LambdaImagePane pane = runGet(this::createImagePane);
        Assert.assertEquals(i1, runGet(pane::getSwingViewValue));

        Image i2 = createImage2();
        run(() -> pane.setImage(i2));
        runWait(600);

        Assert.assertEquals(i2, getPropImage());

        Image i3 = createImage3();
        setPropImage(i3);
        pane.updateSwingViewSource();
        runWait();

        Assert.assertEquals(i3, runGet(pane::getSwingViewValue));
    }

    ////////////// File

    Path propFile;

    public void setPropFile(Path propFile) {
        this.propFile = propFile;
    }

    public Path getPropFile() {
        return propFile;
    }

    LambdaProperty.LambdaFilePathPane createFilePane() {

        LambdaProperty.LambdaFilePathPane p = new LambdaProperty.LambdaFilePathPane(this::getPropFile, this::setPropFile);
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaFilePathPane() {
        Path p1 = Paths.get(System.getProperty("user.home"));
        Path p2 = Paths.get(System.getProperty("user.home") + File.separator + "hello");
        Path p3 = Paths.get(System.getProperty("user.home") + File.separator + "world");
        setPropFile(p1);

        LambdaProperty.LambdaFilePathPane pane = runGet(this::createFilePane);
        Assert.assertEquals(p1, runGet(pane::getSwingViewValue));
        Assert.assertEquals(p1.toString(), runGet(() -> pane.getField().getText()));

        run(() -> pane.setFile(p2));
        runWait(600);
        Assert.assertEquals(p2, getPropFile());

        setPropFile(p3);
        pane.updateSwingViewSource();
        runWait();

        Assert.assertEquals(p3, runGet(pane::getSwingViewValue));
        Assert.assertEquals(p3.toString(), runGet(() -> pane.getField().getText()));
    }

    ////////////// Enum

    public enum TestEnum {
        Init,
        Update1,
        Update2
    }

    TestEnum propEnum;

    public void setPropEnum(TestEnum propEnum) {
        this.propEnum = propEnum;
    }

    public TestEnum getPropEnum() {
        return propEnum;
    }

    LambdaProperty.LambdaEnumComboBox createEnumPane() {
        LambdaProperty.LambdaEnumComboBox b = new LambdaProperty.LambdaEnumComboBox(TestEnum.class, this::getPropEnum, this::setPropEnum);
        frame = createFrame(b);
        return b;
    }

    @Test
    public void testLambdaEnumComboBox() {
        setPropEnum(TestEnum.Init);
        LambdaProperty.LambdaEnumComboBox pane = runGet(this::createEnumPane);

        Assert.assertEquals(TestEnum.Init, runGet(pane::getSwingViewValue));
        Assert.assertEquals(TestEnum.Init, runGet(pane::getSelectedItem));

        run(() -> pane.setSelectedItem(TestEnum.Update1));
        runWait(600);

        Assert.assertEquals(TestEnum.Update1, getPropEnum());

        setPropEnum(TestEnum.Update2);
        pane.updateSwingViewSource();
        runWait();

        Assert.assertEquals(TestEnum.Update2, runGet(pane::getSwingViewValue));
        Assert.assertEquals(TestEnum.Update2, runGet(pane::getSelectedItem));
    }

    ////////////// Boolean

    boolean propBoolean;

    public void setPropBoolean(boolean propBoolean) {
        this.propBoolean = propBoolean;
    }

    public boolean isPropBoolean() {
        return propBoolean;
    }

    LambdaProperty.LambdaBooleanCheckBox createBooleanPane() {
        LambdaProperty.LambdaBooleanCheckBox b = new LambdaProperty.LambdaBooleanCheckBox("test", this::isPropBoolean, this::setPropBoolean);
        frame = createFrame(b);
        return b;
    }

    @Test
    public void testLambdaBooleanCheckBox() {
        setPropBoolean(true);
        LambdaProperty.LambdaBooleanCheckBox pane = runGet(this::createBooleanPane);

        Assert.assertTrue(runGet(pane::getSwingViewValue));
        Assert.assertTrue(runGet(pane::isSelected));
        run(() -> pane.setSwingViewValueWithUpdate(false)); //Note: both setSwingViewValue(b) and setSelected(b) does not cause any update
        runWait(600);

        Assert.assertFalse(isPropBoolean());

        setPropBoolean(true);
        pane.updateSwingViewSource();
        runWait();
        Assert.assertTrue(runGet(pane::getSwingViewValue));
        Assert.assertTrue(runGet(pane::isSelected));
    }


    ////////////// Document

    StyledDocument propStyledDoc;

    public void setPropStyledDoc(StyledDocument propStyledDoc) {
        this.propStyledDoc = propStyledDoc;
    }

    public StyledDocument getPropStyledDoc() {
        return propStyledDoc;
    }

    StringBuilder propBuilder;

    public void setPropBuilder(StringBuilder propBuilder) {
        this.propBuilder = propBuilder;
    }

    public StringBuilder getPropBuilder() {
        return propBuilder;
    }

    LambdaProperty.LambdaDocumentTextPane createDocumentTextPane(Supplier<?> src) {
        LambdaProperty.LambdaDocumentTextPane p = new LambdaProperty.LambdaDocumentTextPane(src);
        p.setPreferredSize(new Dimension(500, 300));
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaDocumentTestPaneStyledDoc() {
        setPropStyledDoc(runGet(DefaultStyledDocument::new));
        LambdaProperty.LambdaDocumentTextPane pane = runGet(() -> createDocumentTextPane(this::getPropStyledDoc));
        Assert.assertEquals(getPropStyledDoc(), runGet(pane::getDocument));

        run(() -> pane.setText("hello, world"));
        runWait(600);

        Assert.assertEquals("hello, world", runGet(() -> {
            try {
                return getPropStyledDoc().getText(0, getPropStyledDoc().getLength());
            } catch (Exception ex) {
                return "?";
            }
        }));

        setPropStyledDoc(runGet(DefaultStyledDocument::new));
        pane.updateSwingViewSource();
        runWait();

        Assert.assertEquals(getPropStyledDoc(), runGet(pane::getDocument));
    }

    @Test
    public void testLambdaDocumentTestPaneBuilder() {
        setPropBuilder(new StringBuilder());
        LambdaProperty.LambdaDocumentTextPane pane = runGet(() -> createDocumentTextPane(this::getPropBuilder));

        run(() -> pane.setText("hello, world"));
        runWait(600);

        Assert.assertEquals("hello, world", getPropBuilder().toString());

        setPropBuilder(new StringBuilder("new value"));
        pane.updateSwingViewSource();
        runWait();

        Assert.assertEquals("new value", runGet(pane::getText));
    }

    ////////////// PlainDocument

    Document propPlainDoc;

    public void setPropPlainDoc(Document propPlainDoc) {
        this.propPlainDoc = propPlainDoc;
    }

    public Document getPropPlainDoc() {
        return propPlainDoc;
    }

    LambdaProperty.LambdaDocumentPlainEditorPane createDocumentPlainEditor() {
        LambdaProperty.LambdaDocumentPlainEditorPane p = new LambdaProperty.LambdaDocumentPlainEditorPane(this::getPropPlainDoc);
        p.setPreferredSize(new Dimension(500, 300));
        frame = createFrame(p);
        return p;
    }

    @Test
    public void testLambdaDocumentPlainEditorPane() {
        setPropPlainDoc(runGet(PlainDocument::new));
        LambdaProperty.LambdaDocumentPlainEditorPane pane = runGet(this::createDocumentPlainEditor);
        Assert.assertEquals(getPropPlainDoc(), runGet(pane::getDocument));

        run(() -> pane.setText("hello, world"));
        runWait(600);

        Assert.assertEquals("hello, world", runGet(() -> {
            try {
                return getPropPlainDoc().getText(0, getPropPlainDoc().getLength());
            } catch (Exception ex) {
                return "?";
            }
        }));
        
        setPropPlainDoc(runGet(PlainDocument::new));
        pane.updateSwingViewSource();
        runWait();
        Assert.assertEquals(getPropPlainDoc(), runGet(pane::getDocument));
    }

    ////////////// List

    public static class TestElem {
        String str;

        public void setStr(String str) { this.str = str; }
        public String getStr() {  return str; }

        boolean bool;

        public void setBool(boolean bool) { this.bool = bool; }

        public boolean isBool() {  return bool; }

        int num;

        public void setNum(int num) { this.num = num; }

        public int getNum() {  return num; }

        Path path;

        public Path getPath() {  return path; }

        public void setPath(Path path) { this.path = path; }

        TestEnum select;

        public void setSelect(TestEnum select) { this.select = select; }

        public TestEnum getSelect() {  return select; }

        String label;

        public void setLabel(String label) { this.label = label; }

        public String getLabel() { return label; }

        Image img;

        public void setImg(Image img) { this.img = img; }

        public Image getImg() { return img; }

        public int actionCount;

        public void action() {
            ++actionCount;
        }
    }

    List<TestElem> propList;

    public void setPropList(List<TestElem> propList) {
        this.propList = propList;
    }

    public List<TestElem> getPropList() {
        return propList;
    }

    LambdaProperty.LambdaCollectionTable createTablePane() {
        LambdaProperty.LambdaCollectionTable t = new LambdaProperty.LambdaCollectionTable(TestElem.class, this::getPropList)
                .addColumnString("str", TestElem::getStr, TestElem::setStr)
                .addColumnBoolean("bool", TestElem::isBool, TestElem::setBool)
                .addColumnNumber("num", int.class, TestElem::getNum, TestElem::setNum)
                .addColumnFilePath("file", TestElem::getPath, TestElem::setPath)
                .addColumnEnum("select", TestEnum.class, TestElem::getSelect, TestElem::setSelect)
                .addColumnLabel("label", TestElem::getLabel)
                .addColumnImage("img", Image.class, TestElem::getImg, TestElem::setImg)
                .<TestElem>addAction("action", es -> es.forEach(TestElem::action));

        frame = createFrame(t.wrapSwingScrollPane(true, false));
        return t;
    }

    @Test
    public void testLambdaCollectionTable() {
        setPropList(new ArrayList<>());
        for (int i = 0; i < 10; ++i) {
            TestElem e = new TestElem();
            e.setBool(true);
            e.setImg(createImage1());
            e.setLabel("L" + i);
            e.setNum(i);
            e.setPath(Paths.get(System.getProperty("user.home") + File.separator + "/n" + i));
            e.setSelect(TestEnum.Init);
            e.setStr("s" + i);
            getPropList().add(e);
        }
        LambdaProperty.LambdaCollectionTable pane = runGet(this::createTablePane);
        Assert.assertEquals(getPropList(), runGet(pane::getSwingViewValue));
        runWait();


    }
}
