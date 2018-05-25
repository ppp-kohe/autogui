package autogui.swing.log;

import autogui.base.log.*;
import autogui.swing.GuiSwingTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingLogManagerTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingLogManagerTest().test();
    }

    @Test
    public void test() {
        GuiSwingLogManager manager = new GuiSwingLogManager();
        //manager.setupConsole(true, true, true);
        GuiLogManager.setManager(manager);

        GuiSwingLogManager.GuiSwingLogWindow w = runGet(manager::createWindow);

        JPanel paneWithStatus = runGet(() -> {

            JPanel pane = new JPanel();
            {
                JTextField field = new JTextField(20);
                field.addActionListener(e -> {
                    GuiLogManager.get().log(field.getText());
                });
                pane.add(field);

                JButton btn = new JButton("Task");
                btn.addActionListener(e -> {
                    new Thread(() -> {
                        try (GuiLogEntryProgress p = GuiLogManager.get().logProgress()) {
                            for (int i = 0; i < 10; ++i) {
                                p.addValueP(0.1);
                                p.setMessage("hello");
                                Thread.sleep(500);
                            }
                        } catch (Exception ex) {
                            //
                        }
                    }).start();
                });
                pane.add(btn);

                btn = new JButton("TaskLong");
                btn.addActionListener(e -> {
                    new Thread(() -> {
                        try (GuiLogEntryProgress p = GuiLogManager.get().logProgress()) {
                            for (int i = 0; i < 100; ++i) {
                                p.addValueP(0.01);
                                p.setMessage("hello long");
                                Thread.sleep(500);
                            }
                        } catch (Exception ex) {
                            //
                        }
                    }).start();
                });
                pane.add(btn);
            }
            pane = w.getPaneWithStatusBar(pane);

            JFrame frame = testFrame(pane);
            frame.setSize(400, 300);
            return pane;
        });

        GuiSwingLogStatusBar bar = runQuery(paneWithStatus, query(GuiSwingLogStatusBar.class, 0));
        JButton showButton = runQuery(bar, query(JButton.class, 0));

        run(showButton::doClick);

        GuiLogManager.get().logString("hello");
        GuiLogEntry e = GuiLogManager.get().logError(new RuntimeException("world"));

        try (GuiLogEntryProgress p = GuiLogManager.get().logProgress()) {
            for (int i = 0; i < 10; ++i) {
                p.addValueP(0.1);
                p.setMessage("progress " + i);
                Thread.sleep(500);
            }
        } catch (Exception ex) {
            //
        }

        Assert.assertEquals(Integer.valueOf(3), runGet(() -> w.getList().getRowCount() ));
        Assert.assertEquals("hello", runGet(() ->
                ((GuiLogEntryString) w.getList().getValueAt(0)).getData()));
        Assert.assertEquals("world", runGet(() ->
                ((GuiLogEntryException) w.getList().getValueAt(1)).getException().getMessage()));
        GuiLogEntryProgress p = runGet(() -> (GuiLogEntryProgress) w.getList().getValueAt(2));
        Assert.assertTrue(p.isFinished());
        Assert.assertFalse(p.isIndeterminate());
        Assert.assertEquals(1.0, p.getValueP(), 0.001);
        Assert.assertEquals("progress 9", p.getMessage());

        ((GuiSwingLogEntry) e).setSelected(true);
        ((GuiSwingLogEntryException) e).setExpanded(true);
        manager.show(e);

        try {
            Thread.sleep(1000);
        } catch (Exception ex){}
        String ex = String.join("\n", runGet(() -> w.getList().getSelectedText()));
        Assert.assertTrue(ex.contains("RuntimeException"));
    }
}
