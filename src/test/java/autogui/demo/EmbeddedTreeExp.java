package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;
import autogui.swing.GuiPreferencesExp;

import javax.swing.*;

@GuiIncluded
public class EmbeddedTreeExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new EmbeddedTreeExp());
    }

    GuiPreferencesExp pref;
    JScrollPane pane;

    @GuiIncluded public String name = "/";

    @GuiIncluded
    public void load() {
        if (!name.isEmpty() && pref != null) {
            pref.load(name);
        }
    }

    @GuiIncluded
    public JComponent getTree() {
        if (pref == null) {
            pref = new GuiPreferencesExp();
            pref.initTree();
            pane = new JScrollPane(pref.getTree());
        }
        return pane;
    }
}
