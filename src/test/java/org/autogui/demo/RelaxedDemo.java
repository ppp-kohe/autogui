package org.autogui.demo;

import org.autogui.swing.AutoGuiShell;

public class RelaxedDemo {
    public static void main(String[] args) {
        AutoGuiShell.showLive(new RelaxedDemo());
    }

    String hello;

    void action() {
        System.out.println(hello);
    }
}
