package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiSwingViewFilePathFieldTest extends GuiSwingTestCase {
    GuiTypeBuilder typeBuilder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextPropFile;
    GuiMappingContext contextPropPath;

    TestObj obj;

    GuiSwingViewFilePathField field;
    JFrame frame;

    Path homePath;

    public GuiSwingViewFilePathFieldTest() {}

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public File file;
        @GuiIncluded public Path path;
        public TestObj() {}
    }

    @Before
    public void setUp() {
        typeBuilder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) typeBuilder.get(TestObj.class);

        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextPropFile = context.getChildByName("file");
        contextPropPath = context.getChildByName("path");

        field = new GuiSwingViewFilePathField();
        homePath = Paths.get(System.getProperty("user.home", "/"));
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public GuiSwingViewFilePathField.PropertyFilePathPane create(GuiMappingContext contextProp) {
        JComponent c = field.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewFilePathField.PropertyFilePathPane.class);
    }

    @Test
    public void testViewUpdateFile() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(this::runWait);

        obj.file = homePath.toFile();
        contextPropFile.updateSourceFromRoot();
        Assert.assertEquals("listener update file as Path",
                homePath, runGet(p::getSwingViewValue));

    }


    @Test
    public void testViewUpdatePath() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropPath));
        run(this::runWait);

        obj.path = homePath;
        contextPropPath.updateSourceFromRoot();
        Assert.assertEquals("listener update file",
                homePath, runGet(p::getSwingViewValue));

    }

    @Test
    public void testViewUpdateFileField() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.setSwingViewValueWithUpdate(homePath.toFile()));

        run(this::runWait);
        Assert.assertEquals("after set value", homePath.toFile(), obj.file);
        Assert.assertEquals("after set value", homePath.toString(),
                runGet(() -> p.getField().getText()));
    }

    @Test
    public void testViewUpdateFileFieldText() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.getField().setText(homePath.toString()));

        run(this::runWait);
        Assert.assertEquals("after set text", homePath.toFile(), obj.file);
        Assert.assertEquals("after set text", homePath.toString(),
                runGet(() -> p.getField().getText()));
    }

    @Test
    public void testViewUpdateFileSetFile() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.setFile(homePath));

        run(this::runWait);
        Assert.assertEquals("after set value", homePath.toFile(), obj.file);
        Assert.assertEquals("after set value", homePath.toString(),
                runGet(() -> p.getField().getText()));

    }
}
