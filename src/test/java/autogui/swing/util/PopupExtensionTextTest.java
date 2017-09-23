package autogui.swing.util;

import autogui.swing.GuiSwingTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PopupExtensionTextTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new PopupExtensionTextTest().testDefault();
    }

    PopupExtensionText ext;
    PopupExtensionText.TextServiceDefaultMenu defMenu;

    int count;

    public class TestAction extends AbstractAction {
        public TestAction() {
            putValue(NAME, "test-action");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            ++count;
            System.err.println("action: " + e + " : count=" + count);
        }
    }

    @Test
    public void testDefault() throws Exception {

        JTextField fld = runGet(() -> {
            JPanel pane = new JPanel();

            JTextField field = new JTextField(20);
            ext = PopupExtensionText.installDefault(field);
            defMenu = (PopupExtensionText.TextServiceDefaultMenu) ext.getMenuBuilder();
            defMenu.getEditActions().add(new JMenuItem(new TestAction()));
            pane.add(field);

            JFrame frame = testFrame(pane);

            return field;
        });

        run(() -> fld.setText("hello, world"));

        run(fld::grabFocus);

        keyType("HELLO");
        keyTypeAtOnce(KeyEvent.VK_CONTROL, KeyEvent.VK_SPACE);

        JPopupMenu menu = ext.getMenu();

        Assert.assertTrue(runGet(menu::isVisible));

        JMenuItem item = getMenu(menu, "test-action");
        run(item::doClick);

        run(() -> menu.setVisible(false));

        Assert.assertEquals("hello, worldHELLO", runGet(fld::getText));

        Assert.assertEquals(1, count);
    }

    public static JMenuItem getMenu(JComponent menu, String name) {
        for (Component comp : menu.getComponents()) {
            if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                Object actionName;
                if (item.getAction() != null &&
                        (actionName = item.getAction().getValue(Action.NAME)) != null &&
                            actionName.equals(name)) {
                    return item;
                }
            }
        }
        throw new RuntimeException(menu  + " has no action item named : " + name);
    }
}
