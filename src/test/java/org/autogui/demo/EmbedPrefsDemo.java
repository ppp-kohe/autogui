package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.swing.AutoGuiShell;
import org.autogui.swing.GuiSwingViewDocumentEditor;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@GuiIncluded
public class EmbedPrefsDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EmbedPrefsDemo());
    }

    EmbedPane comp;
    @GuiIncluded
    public EmbedPane component() {
        if (comp == null) {
            comp = new EmbedPane();
        }
        return comp;
    }

    Random rand = new Random();

    @GuiIncluded
    public void randomColor() {
        comp.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        comp.repaint();
    }

    public static class EmbedPane extends JComponent implements GuiPreferences.PreferencesJsonSupport {
        int count;
        Color color = Color.blue;
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(color);
            g.drawString("count=" + count, 20, 20);
            if (count < 10) {
                g.drawString("The count & color can be saved as prefs", 40, 40);
            }
            ++count;
        }

        @Override
        public Map<String, Object> getPrefsJson() {
            var map = new HashMap<String, Object>();
            map.put("count", count);
            map.put("color", GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.toJsonColor(color));
            return map;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public void setPrefsJson(Map<String, Object> map) {
            if (map.get("count") instanceof Number n) {
                count = n.intValue();
            }
            if (map.get("color") instanceof List<?> l) {
                color = GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.fromJsonColor(l);
            }
        }
    }
}
