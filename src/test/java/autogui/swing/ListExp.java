package autogui.swing;

import autogui.base.log.*;
import autogui.swing.log.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextLayout;
import java.time.*;
import java.util.Enumeration;

public class ListExp extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
//        GuiLogManagerConsole c = new GuiLogManagerConsole();
//        c.logError(new RuntimeException("hello, world"));
//        c.log("hello:", LocalDateTime.now());
//        GuiLogEntryProgress p = c.logProgress();
//        for (int i = 0; i < 100; ++i) {
//            p.setIndeterminate(i < 10);
//            p.setValueP(i / 100.0);
//            if (i == 50) {
//                c.log("half");
//            }
//            p.setMessage("[" + i + "]");
//            Thread.sleep(400);
//        }
//        p.finish();
//        Thread.sleep(1000);
        new ListExp().test();
    }

    public void test() {
        run(() -> {
            GuiSwingLogManager manager = new GuiSwingLogManager();

            JPanel pane = new JPanel(new BorderLayout());
            {

                GuiSwingLogList list = new GuiSwingLogList(manager);
                manager.addView(list::addLogEntry);
                pane.add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

                JToolBar bar = new JToolBar();
                bar.setFloatable(false);
                bar.add(new AddAction(list, manager));
                bar.add(new AddProgressAction(list, manager));
                bar.add(new AddErrorAction(list, manager));

                JTextField findField = new JTextField(20);
                bar.add(findField);
                findField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        boolean back = e.isShiftDown();
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            String str = findField.getText();
                            System.err.println("find " + str + " back=" + back);
                            list.findText(str, !back);
                        }
                    }
                });
                pane.add(bar, BorderLayout.NORTH);

                GuiSwingLogStatusBar status = new GuiSwingLogStatusBar(manager);
                manager.addView(status::addLogEntry);
                pane.add(status, BorderLayout.SOUTH);
            }
//            for (Enumeration e = UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
//                Object k = e.nextElement();
//                Object v = UIManager.getDefaults().get(k);
//                System.out.println(k + "\t" + v.getClass() + "\t" + v);
//            }

            JFrame frame = testFrame(pane);
            frame.setSize(400, 800);
        });
    }


    static class AddAction extends AbstractAction {
        GuiSwingLogList list;
        GuiSwingLogManager maanager;

        public AddAction(GuiSwingLogList list, GuiSwingLogManager manager) {
            super("Add");
            this.list = list;
            this.maanager = manager;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            maanager.logString("hello!!!!\nworld!!!!!");
        }
    }

    static class AddProgressAction extends AbstractAction {
        GuiSwingLogList list;
        GuiSwingLogManager maanager;

        public AddProgressAction(GuiSwingLogList list, GuiSwingLogManager manager) {
            super("Add Progress");
            this.list = list;
            this.maanager = manager;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    try (GuiLogEntryProgress p = maanager.logProgress(100);) {
                        for (int i = 0; i < 100; ++i) {
                            p.addValue(1);
                            p.setMessage("hello " + i +" : " + p.getValueP());
                            Thread.sleep(400);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();

        }
    }

    static class AddErrorAction extends AbstractAction {
        GuiSwingLogList list;
        GuiSwingLogManager manager;

        public AddErrorAction(GuiSwingLogList list, GuiSwingLogManager manager) {
            super("Add Error");
            this.list = list;
            this.manager = manager;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            manager.logError(new RuntimeException("hello, world"));
        }
    }

    //////


    //////////

    //////////

}
