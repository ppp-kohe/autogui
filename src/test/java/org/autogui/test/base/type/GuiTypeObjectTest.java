package org.autogui.test.base.type;

import org.autogui.GuiNotifierSetter;
import org.autogui.base.type.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GuiTypeObjectTest {
    public static class TestObj {
        public String name;
        public int num;

        public String value() {
            return "v";
        }

        public void action() {
            num++;
        }

        @GuiNotifierSetter
        public void setNotifier(Runnable r) {

        }
    }

    @Test
    public void testAdd() throws Exception {
        var t = new GuiTypeObject(TestObj.class.getName());
        var a = new GuiTypeMemberAction("action", null, TestObj.class.getMethod("action"));
        var p = new GuiTypeMemberProperty("name", null, null, TestObj.class.getField("name"), new GuiTypeValue(String.class));
        var n = new GuiTypeMemberPropertyNotifier("notifier", null, TestObj.class.getMethod("setNotifier", Runnable.class), null, new GuiTypeValue(Runnable.class));
        t.addActions(a);
        t.addProperties(p);
        t.addNotifiers(n);

        assertEquals(List.of(a), t.getActions(), "action");
        assertEquals(t, a.getOwner(), "owner");

        assertEquals(List.of(p), t.getProperties(), "property");
        assertEquals(t, p.getOwner(), "owner");

        assertEquals(List.of(n), t.getNotifiers(), "property");
        assertEquals(t, n.getOwner(), "owner");

        assertEquals(p, t.getMemberByName("name"), "prop by name");
        assertEquals(a, t.getMemberByName("action"), "action by name");
        assertNull(t.getMemberByName("notifier"), "notifiers are excluded");
    }
}
