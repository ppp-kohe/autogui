package org.autogui.test.base.type;

import org.autogui.base.type.GuiTypeCollection;
import org.autogui.base.type.GuiTypeMemberActionList;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
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
        assertEquals("m", methodM.getMethod().getName(), "automatically find method by name and action conditions");
        assertEquals("noTgt", methodNoTgt.getMethod().getName(), "automatically find method by name and action conditions no target");
    }

    @Test
    public void testExecute() throws Exception {
        Example e = new Example();
        var ret = methodM.execute(e, List.of("hello", "world"), "m");
        assertEquals("m:[hello, world]", ret, "execute the method");
    }


    @Test
    public void testExecuteNoTarget() throws Exception {
        Example e = new Example();
        var ret = methodNoTgt.execute(e, List.of("hello", "world"));
        assertEquals("[hello, world]", ret, "execute the method no target");
    }

    @Test
    public void testNoSuchMethod() {
        assertNull(noSuchMethod.getMethod(), "no such method");
        try {
            noSuchMethod.execute(new Example(), List.of("a"));
            fail("no such method cannnot run");
        } catch (Exception ue) {
            //ok
        }
        try {
            noSuchMethod.execute(new Example(), List.of("a"), "target");
            fail("no such method cannnot run");
        } catch (Exception ue) {
            //ok
        }
    }
}
