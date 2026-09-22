package org.autogui.base.type;

import org.autogui.GuiNotifierSetter;
import org.junit.Assert;
import org.junit.Test;

public class GuiTypeMemberPropertyNotifierTest {

    public static class TestObj {
        public Runnable updater;
        @GuiNotifierSetter()
        public void setTestValNotifier(Runnable updater) {
            this.updater = updater;
        }

        @GuiNotifierSetter(target = "x")
        public void setX(Runnable u) {
            this.updater = u;
        }

        @GuiNotifierSetter
        public void setNotifier(Runnable updater) { //self updater
            this.updater = updater;
        }

        @GuiNotifierSetter(target = GuiNotifierSetter.TARGET_ROOT)
        public void setRoot(Runnable root) {
            this.updater = root;
        }

        @GuiNotifierSetter
        public void setYNotifier(Runnable updater) {
            this.updater = updater;
        }

        public void setZNotifier(Runnable updater) {
            this.updater = updater;
        }
    }

    @Test
    public void testConstructor() {
        var p = new GuiTypeMemberPropertyNotifier("testValNotifier", "setTestValNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertFalse("not root target", p.isTargetRoot());
        Assert.assertFalse("not self target", p.isTargetSelf());
        Assert.assertEquals("name", "testVal", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setTestValNotifier", p.getSetter().getName());
    }

    @Test
    public void testTargetName() {
        var p = new GuiTypeMemberPropertyNotifier("x", "setX", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertFalse("not root target", p.isTargetRoot());
        Assert.assertFalse("not self target", p.isTargetSelf());
        Assert.assertEquals("name", "x", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setX", p.getSetter().getName());
    }


    @Test
    public void testTargetNameSelfUpdater() {
        var p = new GuiTypeMemberPropertyNotifier("Notifier", "setNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertFalse("not root target", p.isTargetRoot());
        Assert.assertTrue("self target setNotifier", p.isTargetSelf());
        Assert.assertEquals("self name", "", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setNotifier", p.getSetter().getName());
    }

    @Test
    public void testTargetNameRoot() {
        var p = new GuiTypeMemberPropertyNotifier("root", "setRoot", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertTrue("root target", p.isTargetRoot());
        Assert.assertFalse("not self target setRoot", p.isTargetSelf());
        Assert.assertEquals("root name", "*", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setRoot", p.getSetter().getName());
    }

    @Test
    public void testTargetNameSingleLetter() {
        var p = new GuiTypeMemberPropertyNotifier("yNotifier", "setYNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertFalse("not root target", p.isTargetRoot());
        Assert.assertFalse("not self target", p.isTargetSelf());
        Assert.assertEquals("name", "y", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setYNotifier", p.getSetter().getName());
    }

    @Test
    public void testTargetNameWithoutAnnotation() {
        var p = new GuiTypeMemberPropertyNotifier("zNotifier", "setZNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        Assert.assertFalse("not root target", p.isTargetRoot());
        Assert.assertFalse("not self target", p.isTargetSelf());
        Assert.assertEquals("name", "z", p.getTargetName());
        Assert.assertEquals("find setter from notifier", "setZNotifier", p.getSetter().getName());
    }
}
