package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionCallback;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.type.GuiTypeMemberAction;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GuiTypeMemberActionTest {
    GuiTypeMemberAction action;

    @BeforeEach
    public void setUp() {
        action = new GuiTypeMemberAction("hello", new GuiTypeValue(int.class), "hello");
    }

    @Test
    public void testExecute() throws Exception {
        var strType = new GuiTypeObject(String.class);
        var len = new GuiTypeMemberAction("length", new GuiTypeValue(int.class), "length");
        len.setOwner(strType);
        var m = len.getMethod();
        assertEquals("length", m.getName(), "String.length");

        assertEquals(5, len.execute("hello"), "run length");

        try {
            var nm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(int.class), "noSuchMethod");
            nm.setOwner(strType);
            nm.execute("a");
            fail("no method");
        } catch (UnsupportedOperationException ex) {
            //ok
        }
    }

    @Test
    public void testOrdial() {
        var len = new GuiTypeMemberAction("length", new GuiTypeValue(int.class), "length");
        var ord = len.getOrdinal();
        assertEquals("length", ord.getName(), "ordial name");
        assertEquals(-1, ord.getIndex(), "default ordial index");
    }

    public static class ExampleAnnotated {
        @GuiListSelectionCallback(index = true)
        @GuiListSelectionUpdater(index = true)
        @GuiIncluded(description = "hello", keyStroke = "shift X")
        public Object m() {
            return null;
        }

        @GuiListSelectionUpdater(target = "hello")
        public Object update() {
            return "hello";

        }

        @GuiListSelectionUpdater()
        public void noTarget() {
        }

        public void notUpdater() {
        }

        @GuiListSelectionUpdater(target = "")
        public void selectHelloWorld() {
        }

        public Example editHello() {
            return new Example();
        }

        public Example editObj;
        public boolean ok;

        public void completeEditHello(Example e, boolean ok) {
            this.editObj = e;
            this.ok = ok;
        }

        public Example worldObj;

        public void completeEditWorld(Example e) {
            worldObj = e;
        }
    }

    public static class Example {
    }

    @Test
    public void testAnnotation() {
        var m = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertTrue(m.isSelectionAction(), "annontated selection-callback");
            assertTrue(m.isSelectionChangeAction(), "annontated selection-updater");
            assertTrue(m.isSelectionChangeIndexAction(), "annontated selection-updater-index");
            assertTrue(m.isSelectionIndexAction(), "annontated selection-callback-index");
        }

        var nm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertFalse(nm.isSelectionAction(), "no-method selection-callback");
            assertFalse(nm.isSelectionChangeAction(), "no-method selection-updater");
            assertFalse(nm.isSelectionChangeIndexAction(), "no-method selection-updater-index");
            assertFalse(nm.isSelectionIndexAction(), "no-method selection-callback-index");
        }
    }

    @Test
    public void testSelectTarget() {
        var m = new GuiTypeMemberAction("update", new GuiTypeValue(Object.class), "update");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals("hello", m.getSelectionChangeTargetName(), "selection-callback target");
        }

        var m2 = new GuiTypeMemberAction("noTarget", new GuiTypeValue(void.class), "noTarget");
        {
            m2.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertNull(m2.getSelectionChangeTargetName(), "selection-callback no target");
        }

        var nm = new GuiTypeMemberAction("notUpdater", new GuiTypeValue(void.class), "notUpdater");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertNull(nm.getSelectionChangeTargetName(), "selection-callback not updater");
        }

        var m4 = new GuiTypeMemberAction("selectHelloWorld", new GuiTypeValue(void.class), "selectHelloWorld");
        {
            m4.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals("helloWorld", m4.getSelectionChangeTargetName(), "selection-callback target from name");
        }

        var nsm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nsm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertNull(nsm.getSelectionChangeTargetName(), "selection-callback target no method");
        }
    }

    @Test
    public void testDescription() {
        var m = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals("hello", m.getDescription(), "description");
            assertEquals("shift X", m.getAcceleratorKeyStroke(), "key");
        }

        var nm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals("", nm.getDescription(), "description no method");
            assertEquals("", nm.getAcceleratorKeyStroke(), "key no method");
        }

        var nam = new GuiTypeMemberAction("notUpdater", new GuiTypeValue(void.class), "notUpdater");
        {
            nam.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals("", nam.getDescription(), "description no annotated");
            assertEquals("", nam.getAcceleratorKeyStroke(), "key no annotated");
        }
    }


    @Test
    public void testCompleteEdit() throws Exception {
        ExampleAnnotated a = new ExampleAnnotated();
        Example e = new Example();

        var retType = new GuiTypeValue(Example.class);
        var m = new GuiTypeMemberAction("editHello", retType, "editHello");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            m.setEditFeature(new GuiTypeMemberAction.EditFeature("completeEditHello", ExampleAnnotated.class.getMethod("completeEditHello", Example.class, boolean.class)));

            m.executeCompleteEdit(a, e, true);

            assertTrue(m.getEditFeature().hasCompleteEditMethod(), "has completeEdit");
            assertTrue(m.getEditFeature().isCompleteEditMethodTakingOk(), "take ok");
            assertEquals(List.of(retType), m.getChildren(), "chilren are completeEdit-returned-type");

            assertEquals(e, a.editObj, "completeEdit");
            assertTrue(a.ok, "completeEdit boolean");
        }

        var m2 = new GuiTypeMemberAction("editHello", new GuiTypeValue(Example.class), "editHello");
        {
            m2.setOwner(new GuiTypeObject(ExampleAnnotated.class));
            m2.setEditFeature(new GuiTypeMemberAction.EditFeature("completeEditWorld", ExampleAnnotated.class.getMethod("completeEditWorld", Example.class)));
            m2.executeCompleteEdit(a, e, true);

            assertFalse(m2.getEditFeature().isCompleteEditMethodTakingOk(), "not take ok");
            assertEquals(e, a.worldObj, "completeEdit");
        }

        var nm = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            //as edit
            nm.setEditFeature(new GuiTypeMemberAction.EditFeature());

            assertFalse(nm.getEditFeature().hasCompleteEditMethod(), "the method is marked as 'edit' but no 'completeEdit'");

            nm.executeCompleteEdit(a, e, true); //nothing happen
        }

        var nef = new GuiTypeMemberAction("update", new GuiTypeValue(void.class), "update");
        {
            nef.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            assertEquals(List.of(), nef.getChildren(), "no editFeature");

            nef.executeCompleteEdit(a, e, true); //nothing happen
        }
    }

    @Test
    public void testFindMethod() throws Exception {
        ExampleAnnotated a = new ExampleAnnotated();
        Example e = new Example();

        var retType = new GuiTypeValue(Example.class);
        var m = new GuiTypeMemberAction("editHello", retType, "editHello");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            m.setEditFeature(new GuiTypeMemberAction.EditFeature("completeEditHello"));

            m.executeCompleteEdit(a, e, true);

            assertTrue(m.hasCompleteEditMethod(), "has completeEdit");
            assertTrue(m.getEditFeature().isCompleteEditMethodTakingOk(), "take ok");
            assertEquals(List.of(retType), m.getChildren(), "chilren are completeEdit-returned-type");

            assertEquals(e, a.editObj, "completeEdit");
            assertTrue(a.ok, "completeEdit boolean");
        }
    }
}
