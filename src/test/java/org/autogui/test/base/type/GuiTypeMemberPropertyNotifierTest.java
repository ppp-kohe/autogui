package org.autogui.test.base.type;

import org.autogui.GuiNotifierSetter;
import org.autogui.base.type.GuiTypeMemberPropertyNotifier;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        assertFalse(p.isTargetRoot(), "not root target");
        assertFalse(p.isTargetSelf(), "not self target");
        assertEquals("testVal", p.getTargetName(), "name");
        assertEquals("setTestValNotifier", p.getSetter().getName(), "find setter from notifier");
    }

    @Test
    public void testTargetName() {
        var p = new GuiTypeMemberPropertyNotifier("x", "setX", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        assertFalse(p.isTargetRoot(), "not root target");
        assertFalse(p.isTargetSelf(), "not self target");
        assertEquals("x", p.getTargetName(), "name");
        assertEquals("setX", p.getSetter().getName(), "find setter from notifier");
    }


    @Test
    public void testTargetNameSelfUpdater() {
        var p = new GuiTypeMemberPropertyNotifier("Notifier", "setNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        assertFalse(p.isTargetRoot(), "not root target");
        assertTrue(p.isTargetSelf(), "self target setNotifier");
        assertEquals("", p.getTargetName(), "self name");
        assertEquals("setNotifier", p.getSetter().getName(), "find setter from notifier");
    }

    @Test
    public void testTargetNameRoot() {
        var p = new GuiTypeMemberPropertyNotifier("root", "setRoot", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        assertTrue(p.isTargetRoot(), "root target");
        assertFalse(p.isTargetSelf(), "not self target setRoot");
        assertEquals("*", p.getTargetName(), "root name");
        assertEquals("setRoot", p.getSetter().getName(), "find setter from notifier");
    }

    @Test
    public void testTargetNameSingleLetter() {
        var p = new GuiTypeMemberPropertyNotifier("yNotifier", "setYNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        assertFalse(p.isTargetRoot(), "not root target");
        assertFalse(p.isTargetSelf(), "not self target");
        assertEquals("y", p.getTargetName(), "name");
        assertEquals("setYNotifier", p.getSetter().getName(), "find setter from notifier");
    }

    @Test
    public void testTargetNameWithoutAnnotation() {
        var p = new GuiTypeMemberPropertyNotifier("zNotifier", "setZNotifier", null, null, new GuiTypeValue(Runnable.class));
        p.setOwner(new GuiTypeObject(TestObj.class));

        assertFalse(p.isTargetRoot(), "not root target");
        assertFalse(p.isTargetSelf(), "not self target");
        assertEquals("z", p.getTargetName(), "name");
        assertEquals("setZNotifier", p.getSetter().getName(), "find setter from notifier");
    }
}
