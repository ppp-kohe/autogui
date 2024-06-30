package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.swing.AutoGuiShell;
import org.autogui.swing.LambdaProperty;
import org.autogui.swing.util.ResizableFlowLayout;

import javax.swing.*;
import java.time.LocalDateTime;

@GuiIncluded
public class LambdaPropertyDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new LambdaPropertyDemo());
    }

    LambdaValuePane valuePane = new LambdaValuePane();

    @GuiIncluded(index = 1)
    public LambdaValuePane getValuePane() {
        return valuePane;
    }

    @GuiIncluded
    public static class LambdaValuePane {
        JComponent component;

        String strProp;
        boolean boolProp;
        int numProp;

        @GuiIncluded
        public JComponent getComponent() {
            if (component == null) {
                var layout = ResizableFlowLayout.create(false);

                layout.add(new LambdaProperty.LambdaLabel("Time", () -> LocalDateTime.now().toString())
                        .wrapSwingNamed());

                layout.add(new LambdaProperty.LambdaStringPane("Str", this::getStrProp, this::setStrProp)
                        .wrapSwingNamed());

                layout.add(new LambdaProperty.LambdaBooleanCheckBox("Check", "Flag", this::isBoolProp, this::setBoolProp)
                        .wrapSwingNamed());

                layout.add(new LambdaProperty.LambdaNumberSpinner("Num", Integer.class, this::getNumProp, this::setNumProp)
                        .wrapSwingNamed());

                component = layout.getContainer();
                component.revalidate();
            }
            return component;
        }

        public void setStrProp(String strProp) {
            this.strProp = strProp;
        }

        public String getStrProp() {
            return strProp;
        }

        public void setBoolProp(boolean boolProp) {
            this.boolProp = boolProp;
        }

        public boolean isBoolProp() {
            return boolProp;
        }

        public void setNumProp(int numProp) {
            this.numProp = numProp;
        }

        public int getNumProp() {
            return numProp;
        }
    }
}
