package autogui.swing;

import autogui.base.mapping.GuiPreferences;

import java.util.prefs.Preferences;

public class GuiPreferencesExp {
    public static void main(String[] args) throws Exception {
//        Preferences p = Preferences.userNodeForPackage(GuiPreferences.class)
//                .node("hello/$history");
//        p.removeNode();
//        p.flush();

        show(Preferences.userRoot(), 0);
    }

    public static void show(Preferences p, int level) {
        try {
            log("node: " + p, level);
            for (String k : p.keys()) {
                log(k + ": " + p.get(k, "?"), level + 1);
            }
            for (String n  : p.childrenNames()) {
                show(p.node(n), level + 1);
            }
        } catch (Exception ex) {
            System.err.println(p + ": " + ex);
        }
    }

    private static void log(String str, int level) {
        for (int i = 0; i < level; ++i) {
            str = " " + str;
        }
        System.err.println(str);
    }
}
