open module org.autogui.test {
    requires transitive org.autogui;
//    requires transitive java.desktop;
    requires org.junit.jupiter;

    exports org.autogui.demo;
}