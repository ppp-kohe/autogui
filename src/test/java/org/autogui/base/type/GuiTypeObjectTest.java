package org.autogui.base.type;

import org.autogui.GuiNotifierSetter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

        Assert.assertEquals("action", List.of(a), t.getActions());
        Assert.assertEquals("owner", t, a.getOwner());

        Assert.assertEquals("property", List.of(p), t.getProperties());
        Assert.assertEquals("owner", t, p.getOwner());

        Assert.assertEquals("property", List.of(n), t.getNotifiers());
        Assert.assertEquals("owner", t, n.getOwner());

        Assert.assertEquals("prop by name", p, t.getMemberByName("name"));
        Assert.assertEquals("action by name", a, t.getMemberByName("action"));
        Assert.assertNull("notifiers are excluded", t.getMemberByName("notifier"));
    }
}
