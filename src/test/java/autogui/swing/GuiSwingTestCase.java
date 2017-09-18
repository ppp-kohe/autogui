package autogui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuiSwingTestCase {
    protected Robot robot;

    public JFrame testFrame(JComponent pane) {
        JFrame frame = new JFrame("test");
        frame.add(pane);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        return frame;
    }

    public Robot getRobot() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return robot;
    }

    public void keyType(String text) {
        Robot robot = getRobot();
        robot.delay(10);
        for (char c : text.toCharArray()) {
            int code = KeyStroke.getKeyStroke(c).getKeyCode();
            if (Character.isUpperCase(c)) {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            robot.delay(10);
            robot.keyPress(Character.toUpperCase(c));
            robot.delay(10);
            robot.keyRelease(Character.toUpperCase(c));
            robot.delay(10);
            if (Character.isUpperCase(c)) {
                getRobot().keyRelease(KeyEvent.VK_SHIFT);
            }
            robot.delay(10);
        }
    }

    public void keyTypeAtOnce(int... codes) {
        Robot robot = getRobot();
        robot.delay(10);
        for (int code : codes) {
            robot.keyPress(code);
            robot.delay(10);
        }
        for (int code : codes) {
            robot.keyRelease(code);
            robot.delay(10);
        }
    }

    public void run(Runnable r) {
        runGet(() -> { r.run(); return null;} );
    }

    public <T> T runGet(final Callable<T> r) {
        try {
            Thread.sleep(300);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ArrayBlockingQueue<GuiRes<T>> q = new ArrayBlockingQueue<>(1);
        SwingUtilities.invokeLater(() -> {
            try {
                q.add(new GuiRes<>(r.call()));
            } catch (Throwable ex) {
                ex.printStackTrace();
                q.add(new GuiRes<>(ex));
            }
        });
        try {
            Thread.sleep(200);
            GuiRes<T> o = q.poll(10, TimeUnit.MINUTES);
            if (o == null) {
                throw new RuntimeException("timeout");
            } else if (o.exception != null) {
                throw new RuntimeException(o.exception);
            } else {
                return o.value;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T extends Container> T runQuery(Container pane, GuiQuery<T> q) {
        return runGet(() -> q.get(pane));
    }

    public <T extends Container> GuiQueryType<T> query(Class<T> type, int index) {
        return new GuiQueryType<>(type, index);
    }


    public static class GuiRes<T> {
        public T value;
        public Throwable exception;

        public GuiRes(T value) {
            this.value = value;
        }

        public GuiRes(Throwable exception) {
            this.exception = exception;
        }
    }

    public interface GuiQuery<RetComp extends Component> {
        RetComp get(Container comp);
    }

    public static class GuiQueryConcat<RetComp extends Container> implements GuiQuery<RetComp> {
        public GuiQuery<? extends Container> container;
        public GuiQuery<RetComp> component;

        public GuiQueryConcat(GuiQuery<? extends Container> container, GuiQuery<RetComp> component) {
            this.container = container;
            this.component = component;
        }

        @Override
        public RetComp get(Container comp) {
            return component.get(container.get(comp));
        }

        public <NextComp extends Container> GuiQueryConcat<NextComp> cat(Class<NextComp> x, int index) {
            return new GuiQueryConcat<>(this, new GuiQueryType<>(x, index));
        }
    }

    public static class GuiQueryType<RetComp extends Container> implements GuiQuery<RetComp> {
        protected Class<RetComp> type;
        protected int index;
        public GuiQueryType(Class<RetComp> cls, int index) {
            this.type = cls;
            this.index = index;
        }

        public <NextComp extends Container> GuiQueryConcat<NextComp> cat(Class<NextComp> x, int index) {
            return new GuiQueryConcat<>(this, new GuiQueryType<>(x, index));
        }

        public RetComp get(Container c) {
            try {
                return Arrays.stream(c.getComponents())
                        .filter(type::isInstance)
                        .map(type::cast)
                        .collect(Collectors.toList())
                        .get(index);
            } catch (Exception ex) {
                throw new RuntimeException("query:" + type + "," + index, ex);
            }
        }
    }

}
