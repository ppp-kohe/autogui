module org.autogui {
    requires transitive java.datatransfer;
    requires transitive java.desktop;
    requires transitive java.prefs;

    exports org.autogui;

    exports org.autogui.base;
    exports org.autogui.base.log;
    exports org.autogui.base.mapping;
    exports org.autogui.base.type;
    exports org.autogui.base.annotation;

    exports org.autogui.swing;
    exports org.autogui.swing.icons;
    exports org.autogui.swing.log;
    exports org.autogui.swing.mapping;
    exports org.autogui.swing.table;
    exports org.autogui.swing.prefs;
    exports org.autogui.swing.util;
}