package autogui.demo;

import autogui.swing.AutoGuiShell;

public class RelaxedExp {
    public static void main(String[] args) {
        AutoGuiShell.showLive(new RelaxedExp());
    }

    String hello;

    void action() {
        System.out.println(hello);
    }
}
