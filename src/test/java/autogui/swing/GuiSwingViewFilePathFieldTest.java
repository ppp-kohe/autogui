package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueFilePathField;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.swing.util.SearchTextFieldFilePath;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiSwingViewFilePathFieldTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewFilePathFieldTest().test();
    }

    @Test
    public void test() throws Exception {
        GuiSwingViewFilePathField fld = new GuiSwingViewFilePathField();

        TestFile test = new TestFile();
        GuiMappingContext context = new GuiMappingContext(test, new GuiReprValueFilePathField());
        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            JComponent comp = fld.createView(context);
            testFrame(comp).setSize(1000, 100);
            return comp;
        });

        SearchTextFieldFilePath pathField = runQuery(component, query(SearchTextFieldFilePath.class, 0));
        System.err.println(pathField);

        String home = System.getProperty("user.home",
                FileSystems.getDefault().getRootDirectories().iterator().next().toString());

        run(() -> pathField.getField().setText(home));

        Thread.sleep(1000);

        Assert.assertEquals(Paths.get(home), runGet(() -> test.file));

        test.file = Paths.get(home).getParent();
        run(context::updateSourceFromRoot);

        Assert.assertEquals(test.file.toString(), runGet(() -> pathField.getField().getText()));
    }

    public static class TestFile extends GuiTypeMemberProperty {
        public Path file;

        public TestFile() {
            super("hello");
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, file);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            this.file = (Path) value;
            System.err.println("update : " + file);
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
