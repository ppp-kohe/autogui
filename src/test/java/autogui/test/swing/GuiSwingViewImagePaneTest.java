package autogui.test.swing;

import autogui.GuiIncluded;
import autogui.base.JsonReader;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewImagePane;
import autogui.swing.util.SearchTextFieldFilePath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class GuiSwingViewImagePaneTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewImagePaneTest t = new GuiSwingViewImagePaneTest();
        t.setUp();
        t.testViewTransferHandlerImportFile();
    }

    String testDir = "target";

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewImagePane img;
    JFrame frame;

    BufferedImage image;
    BufferedImage image2;
    Path imageFile;
    Image savedImage;

    @Before
    public void setUp() {

        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        img = new GuiSwingViewImagePane();

    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        if (imageFile != null) {
            try {
                Files.deleteIfExists(imageFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public Image value;
    }


    public GuiSwingViewImagePane.PropertyImagePane create() {
        JComponent c = img.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        frame.setSize(300, 300);
        return GuiSwingView.findChildByType(c, GuiSwingViewImagePane.PropertyImagePane.class);
    }

    public BufferedImage getImage() {
        if (image == null) {
            image = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = image.createGraphics();
            {
                g.setPaint(Color.blue);
                g.fillRect(0, 0, 16, 16);
            }
            g.dispose();
        }
        return image;
    }

    public BufferedImage getImage2() {
        if (image2 == null) {
            image2 = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = image2.createGraphics();
            {
                g.setPaint(Color.red);
                g.fillRect(0, 0, 16, 16);
            }
            g.dispose();
        }
        return image2;
    }

    public Path getImageFile() {
        if (imageFile == null) {
            imageFile = Paths.get(testDir).resolve("test-image.png");
            try {
                Files.createDirectories(imageFile.getParent());

                ImageIO.write(getImage(), "png", imageFile.toFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return imageFile;
    }

    /////////////////////

    @Test
    public void testViewImageUpdate() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);

        obj.value = runGet(this::getImage);
        contextProp.updateSourceFromRoot();

        Assert.assertEquals("image value", getImage(),
                runGet(i::getSwingViewValue));

        Assert.assertEquals("image size", new Dimension(16, 16), runGet(i::getImageSize));

    }

    @Test
    public void testViewImageSetValue() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);

        run(() -> i.setImage(getImage()));

        Assert.assertEquals("after set image", getImage(), obj.value);

        Assert.assertEquals("image size", new Dimension(16, 16), runGet(i::getImageSize));
    }

    @Test
    public void testViewScaleSize() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);
        Dimension size = runGet(() -> i.getImageScale().getScaledImageSize(new Dimension(10, 10), new Dimension(100, 100)));
        Assert.assertEquals("default scale", new Dimension(10, 10), size);

        size = runGet(() -> i.getImageScaleFit().getScaledImageSize(new Dimension(10, 10), new Dimension(100, 100)));
        Assert.assertEquals("default scale fit", new Dimension(100, 100), size);

        run(() -> i.getImageScaleMouseWheel().setCurrentZoom(2.0f));
        size = runGet(() -> i.getImageScaleMouseWheel().getScaledImageSize(new Dimension(10, 10), new Dimension(100, 100)));
        Assert.assertEquals("default scale zoom", new Dimension(20, 20), size);
    }

    @Test
    public void testViewScaleSizeSet() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);
        obj.value = runGet(this::getImage);
        contextProp.updateSourceFromRoot();

        run(this::runWait);

        run(() -> i.setImageScale(new GuiSwingViewImagePane.ImageScaleFit(3))); //fit max x3
        run(i::updateScale);
        run(() -> i.setImageScale(i.getImageScaleMouseWheel()));

        Assert.assertEquals("set to zoom after setting fit", 3,
                runGet(() -> i.getImageScaleMouseWheel().getCurrentZoom()), 0.1f);
    }

    @Test
    public void testViewTransferHandlerImport() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);

        run(() -> i.setImage(getImage2()));

        GuiSwingViewImagePane.ImageSelection se = new GuiSwingViewImagePane.ImageSelection(getImage(), contextProp);
        TransferHandler.TransferSupport ts = new TransferHandler.TransferSupport(i, se);
        Assert.assertTrue("can import",
                runGet(() -> i.getTransferHandler().canImport(ts)));
        Assert.assertTrue("import",
                runGet(() -> i.getTransferHandler().importData(ts)));

        Assert.assertEquals("image value after import", getImage(),
                runGet(i::getSwingViewValue));

    }

    @Test
    public void testViewTransferHandlerExport() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);
        run(() -> i.setImage(getImage()));
        run(() ->
                i.getTransferHandler()
                        .exportToClipboard(i,
                                Toolkit.getDefaultToolkit().getSystemClipboard(),
                                TransferHandler.COPY));

        Assert.assertEquals("after export",
                getImage(),
                runGet(this::getClipboardImage));
    }

    @Test
    public void testViewTransferHandlerImportFile() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);

        run(() -> i.setImage(getImage2()));

        SearchTextFieldFilePath.FileSelection se = new SearchTextFieldFilePath.FileSelection(
                Collections.singletonList(getImageFile()));
        TransferHandler.TransferSupport ts = new TransferHandler.TransferSupport(i, se);
        Assert.assertTrue("can import",
                runGet(() -> i.getTransferHandler().canImport(ts)));
        Assert.assertTrue("import",
                runGet(() -> i.getTransferHandler().importData(ts)));


        GuiSwingJsonTransfer.JsonCopyAction a = runGet(() ->
                findMenuItemAction(i.getSwingStaticMenuItems(),
                        GuiSwingJsonTransfer.JsonCopyAction.class));
        run(() -> a.actionPerformed(null));

        Assert.assertEquals("path", getImageFile(),
                runGet(() -> i.getImagePath(i.getImage())));

        String data = JsonReader.create(runGet(this::getClipboardText)).parseString();
        BufferedImage img = (BufferedImage) contextProp.getReprValue().fromJson(contextProp, null, data);
        Assert.assertEquals("pixel",
                Color.blue.getRGB(), img.getRGB(0, 0));

        Assert.assertEquals("size", 16, img.getWidth());
        Assert.assertEquals("size", 16, img.getHeight());
    }

    @Test
    public void testViewTransferHandlerImportString() {
        GuiSwingViewImagePane.PropertyImagePane i = runGet(this::create);

        run(() -> i.setImage(getImage2()));

        String data = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAFUlEQVR42mNgYPhPIhrVMKph2GoAAJLb/wHQPqPSAAAAAElFTkSuQmCC";
        StringSelection se = new StringSelection(data);
        TransferHandler.TransferSupport ts = new TransferHandler.TransferSupport(i, se);
        Assert.assertTrue("can import",
                runGet(() -> i.getTransferHandler().canImport(ts)));
        Assert.assertTrue("import",
                runGet(() -> i.getTransferHandler().importData(ts)));

        BufferedImage img = (BufferedImage) runGet(i::getImage);
        Assert.assertEquals("pixel",
                Color.blue.getRGB(), img.getRGB(0, 0));

        Assert.assertEquals("size", 16, img.getWidth());
        Assert.assertEquals("size", 16, img.getHeight());
    }

    public Image getClipboardImage() {
        Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (board.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            try {
                return (Image) board.getData(DataFlavor.imageFlavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new RuntimeException("no string");
        }
    }
}
