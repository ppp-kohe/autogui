package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewFilePathField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    public GuiSwingViewFilePathFieldTest() {
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public File file;
        @GuiIncluded
        public Path path;

        public TestObj() {
        }
    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public GuiSwingViewFilePathField.PropertyFilePathPane create(GuiMappingContext contextProp) {
        JComponent c = field.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewFilePathField.PropertyFilePathPane.class);
    }

    @Test
    public void testViewUpdateFile() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(this::runWait);

        obj.file = homePath.toFile();
        contextPropFile.updateSourceFromRoot();
        assertEquals(homePath, runGet(p::getSwingViewValue),
                "listener update file as Path");

    }


    @Test
    public void testViewUpdatePath() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropPath));
        run(this::runWait);

        obj.path = homePath;
        contextPropPath.updateSourceFromRoot();
        assertEquals(homePath, runGet(p::getSwingViewValue),
                "listener update file");

    }

    @Test
    public void testViewUpdateFileField() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.setSwingViewValueWithUpdate(homePath.toFile()));

        run(this::runWait);
        assertEquals(homePath.toFile(), obj.file, "after set value");
        assertEquals(homePath.toString(),
                runGet(() -> p.getField().getText()), "after set value");
    }

    @Test
    public void testViewUpdateFileFieldText() {
        timeFactor = 1;
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.getField().setText(homePath.toString()));

        run(this::runWait);
        runWait();
        assertEquals(homePath.toFile(), obj.file, "after set text");
        assertEquals(homePath.toString(),
                runGet(() -> p.getField().getText()), "after set text");
    }

    @Test
    public void testViewUpdateFileSetFile() {
        GuiSwingViewFilePathField.PropertyFilePathPane p = runGet(() -> create(contextPropFile));
        run(() -> p.setFile(homePath));

        run(this::runWait);
        assertEquals(homePath.toFile(), obj.file, "after set value");
        assertEquals(homePath.toString(),
                runGet(() -> p.getField().getText()), "after set value");

    }
}
