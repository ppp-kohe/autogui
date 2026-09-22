package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionCallback;
import org.autogui.GuiListSelectionUpdater;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GuiTypeMemberActionTest {
    GuiTypeMemberAction action;
    @Before
    public void setUp() {
        action = new GuiTypeMemberAction("hello", new GuiTypeValue(int.class), "hello");
    }
    @Test
    public void testExecute() throws Exception {
        var strType = new GuiTypeObject(String.class);
        var len = new GuiTypeMemberAction("length",  new GuiTypeValue(int.class), "length");
        len.setOwner(strType);
        var m = len.getMethod();
        Assert.assertEquals("String.length", "length", m.getName());

        Assert.assertEquals("run length", 5, len.execute("hello"));

        try {
            var nm = new GuiTypeMemberAction("noSuchMethod",  new GuiTypeValue(int.class), "noSuchMethod");
            nm.setOwner(strType);
            nm.execute("a");
            Assert.fail("no method");
        } catch (UnsupportedOperationException ex) {
            //ok
        }
    }

    @Test
    public void testOrdial() {
        var len = new GuiTypeMemberAction("length",  new GuiTypeValue(int.class), "length");
        var ord = len.getOrdinal();
        Assert.assertEquals("ordial name", "length", ord.getName());
        Assert.assertEquals("default ordial index", -1, ord.getIndex());
    }

    public static class ExampleAnnotated {
        @GuiListSelectionCallback(index = true)
        @GuiListSelectionUpdater(index = true)
        @GuiIncluded(description = "hello", keyStroke = "shift X")
        public Object m() {return null;}

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
        public void editedHello(Example e, boolean ok) {
            this.editObj = e;
            this.ok = ok;
        }

        public Example worldObj;
        public void editedWorld(Example e) {
            worldObj = e;
        }
    }

    public static class Example {}

    @Test
    public void testAnnotation() {
        var m = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertTrue("annontated selection-callback", m.isSelectionAction());
            Assert.assertTrue("annontated selection-updater", m.isSelectionChangeAction());
            Assert.assertTrue("annontated selection-updater-index", m.isSelectionChangeIndexAction());
            Assert.assertTrue("annontated selection-callback-index", m.isSelectionIndexAction());
        }

        var nm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertFalse("no-method selection-callback", nm.isSelectionAction());
            Assert.assertFalse("no-method selection-updater", nm.isSelectionChangeAction());
            Assert.assertFalse("no-method selection-updater-index", nm.isSelectionChangeIndexAction());
            Assert.assertFalse("no-method selection-callback-index", nm.isSelectionIndexAction());
        }
    }

    @Test
    public void testSelectTarget() {
        var m = new GuiTypeMemberAction("update", new  GuiTypeValue(Object.class), "update");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("selection-callback target", "hello", m.getSelectionChangeTargetName());
        }

        var m2 = new GuiTypeMemberAction("noTarget", new  GuiTypeValue(void.class), "noTarget");
        {
            m2.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertNull("selection-callback no target", m2.getSelectionChangeTargetName());
        }

        var nm = new GuiTypeMemberAction("notUpdater", new  GuiTypeValue(void.class), "notUpdater");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertNull("selection-callback not updater", nm.getSelectionChangeTargetName());
        }

        var m4 = new GuiTypeMemberAction("selectHelloWorld", new  GuiTypeValue(void.class), "selectHelloWorld");
        {
            m4.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("selection-callback target from name", "helloWorld", m4.getSelectionChangeTargetName());
        }

        var nsm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nsm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertNull("selection-callback target no method", nsm.getSelectionChangeTargetName());
        }
    }

    @Test
    public void testDescription() {
        var m = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("description", "hello", m.getDescription());
            Assert.assertEquals("key", "shift X", m.getAcceleratorKeyStroke());
        }

        var nm = new GuiTypeMemberAction("noSuchMethod", new GuiTypeValue(void.class), "noSuchMethod");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("description no method", "", nm.getDescription());
            Assert.assertEquals("key no method", "", nm.getAcceleratorKeyStroke());
        }

        var nam = new GuiTypeMemberAction("notUpdater", new GuiTypeValue(void.class), "notUpdater");
        {
            nam.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("description no annotated", "", nam.getDescription());
            Assert.assertEquals("key no annotated", "", nam.getAcceleratorKeyStroke());
        }
    }


    @Test
    public void testEdited() throws Exception {
        ExampleAnnotated a = new ExampleAnnotated();
        Example e = new Example();

        var retType = new GuiTypeValue(Example.class);
        var m = new  GuiTypeMemberAction("editHello", retType, "editHello");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            m.setEditFeature(new GuiTypeMemberAction.EditFeature("editedHello", ExampleAnnotated.class.getMethod("editedHello", Example.class, boolean.class)));

            m.executeEcited(a, e, true);

            Assert.assertTrue("has edited", m.getEditFeature().hasEditedMethod());
            Assert.assertTrue("take ok", m.getEditFeature().isEditedMethodTakingOk());
            Assert.assertEquals("chilren are edited-returned-type", List.of(retType), m.getChildren());

            Assert.assertEquals("edited", e, a.editObj);
            Assert.assertTrue("edited boolean", a.ok);
        }

        var m2 = new  GuiTypeMemberAction("editHello", new  GuiTypeValue(Example.class), "editHello");
        {
            m2.setOwner(new GuiTypeObject(ExampleAnnotated.class));
            m2.setEditFeature(new GuiTypeMemberAction.EditFeature("editedWorld", ExampleAnnotated.class.getMethod("editedWorld", Example.class)));
            m2.executeEcited(a, e, true);

            Assert.assertFalse("not take ok", m2.getEditFeature().isEditedMethodTakingOk());
            Assert.assertEquals("edited", e, a.worldObj);
        }

        var nm = new GuiTypeMemberAction("m", new GuiTypeValue(Object.class), "m");
        {
            nm.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            //as edit
            nm.setEditFeature(new GuiTypeMemberAction.EditFeature());

            Assert.assertFalse("the method is marked as 'edit' but no 'edited'", nm.getEditFeature().hasEditedMethod());

            nm.executeEcited(a, e, true); //nothing happen
        }

        var nef = new GuiTypeMemberAction("update", new GuiTypeValue(void.class), "update");
        {
            nef.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            Assert.assertEquals("no editFeature", List.of(), nef.getChildren());

            nef.executeEcited(a, e, true); //nothing happen
        }
    }

    @Test
    public void testFindMethod() throws Exception {
        ExampleAnnotated a = new ExampleAnnotated();
        Example e = new Example();

        var retType = new GuiTypeValue(Example.class);
        var m = new  GuiTypeMemberAction("editHello", retType, "editHello");
        {
            m.setOwner(new GuiTypeObject(ExampleAnnotated.class));

            m.setEditFeature(new GuiTypeMemberAction.EditFeature("editedHello"));

            m.executeEcited(a, e, true);

            Assert.assertTrue("has edited", m.hasEditedMethod());
            Assert.assertTrue("take ok", m.getEditFeature().isEditedMethodTakingOk());
            Assert.assertEquals("chilren are edited-returned-type", List.of(retType), m.getChildren());

            Assert.assertEquals("edited", e, a.editObj);
            Assert.assertTrue("edited boolean", a.ok);
        }
    }
}
