package autogui.swing;

import autogui.AutoGuiShell;

public class AutGuiShellLiveExp {
    public static void main(String[] args) {
        AutoGuiShell.liveShow(new Hello());
    }



}

class Hello{
    String name = "A";

    void sayHello() {
        System.out.println(name);
    }
}
