package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;
import org.autogui.swing.LambdaProperty;
import org.autogui.swing.util.ResizableFlowLayout;
import org.autogui.swing.util.SearchFilterTextField;
import org.autogui.swing.util.ValueListPane;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@GuiIncluded
public class UtilValueListPaneDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new UtilValueListPaneDemo());
    }

    private JComponent component;
    protected ValueListPaneForDemo listPane;
    @GuiIncluded
    public JComponent getComponent() {
        if (component == null) {
            var list = new ValueListPaneForDemo();
            {
                IntStream.range(0, 100)
                        .forEach(i -> list.takeSource().add(new ValueHolder("Value List Pane Demo " + i)));
                list.syncElements();
            }

            var contentPane = new JPanel(new BorderLayout());
            {
                JPanel tool = new JPanel(new ResizableFlowLayout(false).setFitHeight(true));
                {
                    ResizableFlowLayout.add(tool, SearchFilterTextField.createTextHighlightCollection(list::elementTexts)
                            .setPlaceHolderText("Search..."), false);
                }
                contentPane.add(tool, BorderLayout.NORTH);
                contentPane.add(list, BorderLayout.CENTER);
            }
            component = contentPane;
            listPane = list;
        }
        return component;
    }
    @GuiIncluded
    public void printList() {
        System.err.printf("size=%,d%n", listPane.takeSource().size());
        int i = 0;
        for (var h : listPane.takeSource()) {
            System.err.printf("[%,d]: \"%s\"%n", i, h.getStr());
            ++i;
        }
        System.err.printf("view size=%,d%n", listPane.getModel().getSize());
        for (i = 0; i < listPane.getModel().getSize(); ++i) {
            var el = listPane.getModel().getElementAt(i);
            System.err.printf("[%,d: %,d]: select=%s \"%s\"%n", i, el.index(), el.isSelected(), el.value().getStr());
        }
    }

    public static class ValueHolder {
        String str;

        public ValueHolder(String str) {
            this.str = str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    public static class ValueListPaneForDemo extends ValueListPane<ValueHolder, ValueListItemPaneForDemo> {
        protected List<ValueHolder> values = new ArrayList<>();

        @SuppressWarnings("this-escape")
        public ValueListPaneForDemo() {
            super(scrollWrapper());
            syncElements();
        }

        public List<JTextComponent> elementTexts() {
            List<JTextComponent> texts = new ArrayList<>(getModel().getSize());
            for (int i = 0; i < getModel().getSize(); ++i) {
                texts.add(getModel().getElementAt(i).contentPane().getText().getField());
            }
            return texts;
        }

        @Override
        public List<ValueHolder> takeSource() {
            return values;
        }

        @Override
        public ValueHolder newSourceValue(int i) {
            return new ValueHolder("" + i);
        }

        @Override
        public boolean updateSourceValueToElementPane(int i, ValueHolder value, ValueListElementPane<ValueHolder, ValueListItemPaneForDemo> pane) {
            return pane.contentPane().setValue(value);
        }

        @Override
        public ValueListItemPaneForDemo newElementPane(int i, ValueListElementPane<ValueHolder, ValueListItemPaneForDemo> elementPane) {
            return new ValueListItemPaneForDemo();
        }
    }

    public static class ValueListItemPaneForDemo extends JPanel {
        ValueHolder value = new ValueHolder("init");
        LambdaProperty.LambdaStringPane text;

        @SuppressWarnings("this-escape")
        public ValueListItemPaneForDemo() {
            setLayout(new ResizableFlowLayout(true));
            text = new LambdaProperty.LambdaStringPane(this::getStr, this::setStr);
            ResizableFlowLayout.add(this, text, true);

            text.getField().addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    System.err.printf("Focus + %s%n", getStr());
                }

                @Override
                public void focusLost(FocusEvent e) {
                    System.err.printf("Focus - %s%n", getStr());
                    repaint();
                }
            });
        }

        public LambdaProperty.LambdaStringPane getText() {
            return text;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            //System.err.printf("Paint %s%n", getStr());
        }

        public boolean setValue(ValueHolder value) {
            boolean diff = !Objects.equals(this.value, value);
            this.value = value;
            if (diff) {
                text.updateSwingViewSource();
            }
            return diff;
        }

        public String getStr() {
            return value == null ? "" : value.getStr();
        }
        public void setStr(String s) {
            if (value == null) {
                value = new ValueHolder(s);
            } else {
                value.setStr(s);;
            }
        }
    }
}
