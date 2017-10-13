package autogui.swing;

import autogui.base.log.*;
import autogui.swing.log.*;

import javax.swing.*;
import java.awt.event.*;

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
        //System.setProperty("sun.awt.exception.handler", MyHandler.class.getName());

        run(() -> {
            GuiSwingLogManager manager = new GuiSwingLogManager();
            manager.setupConsole(true, true, true);
            GuiLogManager.setManager(manager);

            GuiSwingLogManager.GuiSwingLogWindow w = manager.createWindow();

            JToolBar bar = w.getToolbar();
            bar.add(new AddAction(w.getList(), manager));
            bar.add(new AddProgressAction(w.getList(), manager));
            bar.add(new AddErrorAction(w.getList(), manager));

            JPanel pane = new JPanel();
            {
                JTextField field = new JTextField(20);
                field.addActionListener(e -> {
                    System.out.println(field.getText());
                    throw new RuntimeException("hello");
                });
                pane.add(field);

                JButton btn = new JButton("Task");
                btn.addActionListener(e -> {
                    try (GuiLogEntryProgress p = GuiLogManager.get().logProgress()) {
                        p.addValueP(0.1);
                        p.setMessage("hello");
                    }
                });
                pane.add(btn);
            }
            pane = w.getPaneWithStatusBar(pane);
//            for (Enumeration e = UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
//                Object k = e.nextElement();
//                Object v = UIManager.getDefaults().get(k);
//                System.out.println(k + "\t" + v.getClass() + "\t" + v);
//            }

            JFrame frame = testFrame(pane);
            frame.setSize(400, 300);
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
