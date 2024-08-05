package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionCallback;
import org.autogui.GuiNotifierSetter;
import org.autogui.swing.AutoGuiShell;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GuiIncluded
public class TableEmbedDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TableEmbedDemo());
    }

    List<EmbeddedItem> componenets = new ArrayList<>();

    public TableEmbedDemo() {
        for (int i = 0; i < 10; ++i) {
            componenets.add(new EmbeddedItem(i, this::update));
        }
    }

    boolean useUpdater;
    @GuiIncluded
    public void setUseUpdater(boolean useUpdater) {
        this.useUpdater = useUpdater;
    }
    @GuiIncluded
    public boolean isUseUpdater() {
        return useUpdater;
    }


    Runnable updater;
    @GuiNotifierSetter
    @GuiIncluded
    public void setComponentsUpdater(Runnable updater) {
        this.updater = updater;
    }

    public void update() {
        if (useUpdater) {
            updater.run();
        }
    }

    @GuiIncluded
    public List<EmbeddedItem> getComponents() {
        return componenets;
    }


    protected List<EmbeddedItem> selectedItems;
    @GuiListSelectionCallback
    @GuiIncluded
    public void selectComponents(List<EmbeddedItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    @GuiIncluded
    public void printSelectionInfo() {
        printInfo(selectedItems);
    }

    public static void printInfo(List<EmbeddedItem> items) {
        if (items == null || items.isEmpty()) {
            System.err.println("no items");
            return;
        }
        for (EmbeddedItem selectedItem : items) {
            var comp = selectedItem.getComponent();
            if (comp != null) {
                if (comp.getParent() != null) {
                    printInfo(comp.getParent().getParent(), 0);
                }
                printInfo(comp.getParent(), 1);
                printInfo(comp, 2);
                if (comp.getComponentCount() > 0) {
                    printInfo(comp.getComponent(0), 3);
                }
            }
        }
    }

    private static void printInfo(Component comp, int indent) {
        var indentStr = "  ".repeat(indent);
        if (comp == null) {
            System.err.printf("%snull%n", indentStr);
            return;
        }
        String name = comp.getClass().getSimpleName();
        var bounds = comp.getBounds();
        var opaque = comp.isOpaque();
        var bck = comp.getBackground();
        var bckStr = "null";
        if (bck != null) {
            bckStr = String.format("%s(%d,%d,%d,%d)", bck.getClass().getSimpleName(), bck.getRed(), bck.getGreen(), bck.getBlue(), bck.getAlpha());
        }
        System.err.printf("%s%s (%1.0f,%1.0f,%1.0f,%1.0f) opaque=%s background=%s%n", indentStr, name, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), opaque, bckStr);
    }



    @GuiIncluded
    public static class EmbeddedItem {
        JComponent component;
        int type;
        String value = "";
        Runnable updater;

        public EmbeddedItem(int type, Runnable updater) {
            this.type = type;
            this.updater = updater;
        }

        @GuiIncluded(index = 10)
        public int getType() {
            return type;
        }

        @GuiIncluded(index = 20)
        public JComponent getComponent() {
            if (component == null) {
                var pane = new JPanel();
                component = pane;
                pane.setOpaque(false);
                var content = switch (type % 10) {
                    case 0 -> button();
                    case 1 -> radioButton();
                    case 2 -> checkBox();
                    case 3 -> slider();
                    case 4 -> toggleButton();
                    case 5 -> spinner();
                    case 6 -> progressBar();
                    case 7 -> comboBox();
                    case 8 -> textField();
                    default -> label();
                };
                pane.add(content);
            }
            return component;
        }

        @GuiIncluded(index = 30)
        public String getValue() {
            return value;
        }

        private void updateByAction(String message, Object value) {
            System.err.printf("update %s : %s%n", message, value);
            this.value = Objects.toString(value);
            if (updater != null) {
                updater.run();
            }
        }

        private JLabel label() {
            return new JLabel("Hello "  + type);
        }

        private JButton button() {
            var b = new JButton("Hello");
            b.addActionListener(e ->
                updateByAction("button", Instant.now()));
            return b;
        }

        private JPanel radioButton() {
            var r1 = new JRadioButton("Hello");
            r1.addActionListener(e ->
                    updateByAction("radio-button", r1.getText()));
            var r2 = new JRadioButton("World");
            r2.addActionListener(e ->
                    updateByAction("radio-button", r2.getText()));
            ButtonGroup g = new ButtonGroup();
            g.add(r1);
            g.add(r2);
            var pane = new JPanel();
            pane.setOpaque(false);
            pane.add(r1);
            pane.add(r2);
            return pane;
        }

        private JCheckBox checkBox() {
            var b = new JCheckBox("Hello");
            b.addActionListener(e ->
                updateByAction("check-box", b.isSelected()));
            return b;
        }

        private JSlider slider() {
            var b = new JSlider(0, 100, 10);
            b.addChangeListener(e ->
                    updateByAction("slider", b.getValue()));
            return b;
        }

        private JToggleButton toggleButton() {
            var e = new JToggleButton("Hello");
            e.addActionListener(ev ->
                    updateByAction("toggle-button", e.isSelected()));
            return e;
        }

        private JSpinner spinner() {
            var s = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
            s.addChangeListener(e ->
                    updateByAction("spinner", s.getValue()));
            return s;
        }

        private JPanel progressBar() {
            var p = new JProgressBar(0, 100);
            var b = new JButton("Progress");
            b.addActionListener(e -> {
                if (p.isIndeterminate()) {
                    p.setIndeterminate(false);
                    p.setValue(0);
                    updateByAction("progress", p.getValue());
                } else if (p.getValue() >= 90) {
                    p.setIndeterminate(true);
                    updateByAction("progress-indeterminate", "_");
                } else {
                    p.setValue(p.getValue() + 10);
                    updateByAction("progress", p.getValue());
                }
            });
            var pane = new JPanel();
            pane.setOpaque(false);
            pane.add(p);
            pane.add(b);
            return pane;
        }

        private JComboBox<?> comboBox() {
            JComboBox<String> cmb = new JComboBox<>(new String[]{"AAA", "BBB", "CCC"});
            cmb.addItemListener(e ->
                    updateByAction("combo-box", cmb.getSelectedItem()));
            return cmb;
        }

        private JTextField textField() {
            var fld = new JTextField("Hello world");
            fld.addActionListener(e ->
                    updateByAction("text-field", fld.getText()));
            return fld;
        }
    }
}
