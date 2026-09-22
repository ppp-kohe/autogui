package org.autogui.base.type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GuiTypeMemberActionListTest {

    public static class Example {
        public String m(List<String> items, String target) {
            return target + ":" + items;
        }

        public String noTgt(List<String> items) {
            return items.toString();
        }
    }

    GuiTypeMemberActionList methodM;
    GuiTypeMemberActionList methodNoTgt;
    GuiTypeMemberActionList noSuchMethod;

    @Before
    public void setUp() {
        var m = new GuiTypeMemberActionList("m", new GuiTypeValue(String.class), new GuiTypeCollection(List.class, new GuiTypeValue(String.class)), "m", true);
        m.setOwner(new GuiTypeObject(Example.class));
        methodM = m;

        var m2 = new GuiTypeMemberActionList("noTgt", new GuiTypeValue(String.class), new GuiTypeCollection(List.class, new GuiTypeValue(String.class)), "noTgt", false);
        m2.setOwner(new GuiTypeObject(Example.class));
        methodNoTgt = m2;

        var m3 = new GuiTypeMemberActionList("noSuchMethod", new GuiTypeValue(String.class), new GuiTypeCollection(List.class, new GuiTypeValue(String.class)), "noSuchMethod", false);
        m3.setOwner(new GuiTypeObject(Example.class));
        noSuchMethod = m3;
    }

    @Test
    public void testGetMethod() {
        Assert.assertEquals("automatically find method by name and action conditions", "m", methodM.getMethod().getName());
        Assert.assertEquals("automatically find method by name and action conditions no target", "noTgt", methodNoTgt.getMethod().getName());
    }

    @Test
    public void testExecute() throws Exception {
        Example e = new Example();
        var ret = methodM.execute(e, List.of("hello", "world"), "m");
        Assert.assertEquals("execute the method", "m:[hello, world]", ret);
    }


    @Test
    public void testExecuteNoTarget() throws Exception {
        Example e = new Example();
        var ret = methodNoTgt.execute(e, List.of("hello", "world"));
        Assert.assertEquals("execute the method no target", "[hello, world]", ret);
    }

    @Test
    public void testNoSuchMethod()  {
        Assert.assertNull("no such method", noSuchMethod.getMethod());
        try {
            noSuchMethod.execute(new Example(), List.of("a"));
            Assert.fail("no such method cannnot run");
        } catch (Exception ue) {
            //ok
        }
        try {
            noSuchMethod.execute(new Example(), List.of("a"), "target");
            Assert.fail("no such method cannnot run");
        } catch (Exception ue) {
            //ok
        }
    }
}
