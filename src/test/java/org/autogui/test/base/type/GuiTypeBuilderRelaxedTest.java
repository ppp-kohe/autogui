package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiNotifierSetter;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeElement;
import org.autogui.base.type.GuiTypeMemberActionList;
import org.autogui.base.type.GuiTypeObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiTypeBuilderRelaxedTest {
    GuiTypeBuilder.GuiTypeBuilderRelaxed builder;
    GuiTypeElement type;

    public GuiTypeBuilderRelaxedTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder.GuiTypeBuilderRelaxed();
    }

    static class Hello implements AutoCloseable {
        private String prvProp;
        protected String prtProp;
        String prop;
        public String pubProp;

        private void prvAction() {
        }

        protected void prtAction() {
        }

        void action() {
        }

        public void pubAction() {
        }

        public void close() {
        }
    }

    static class JdkExtTest extends AbstractMap<String, String> implements AutoCloseable {
        private final List<Entry<String, String>> es = new ArrayList<>();

        @Override
        public Set<Entry<String, String>> entrySet() {
            return new HashSet<>(es);
        }

        String prop;

        void hello() {
        }

        public void world() {
        }

        private void prvAction() {
        }

        public void close() {
        }

        @GuiIncluded(false)
        public String excluded() {
            return "!";
        }
    }

    static class ExtTest extends JdkExtTest implements Runnable {
        String subProp;

        void hello() {
        }

        public void anotherAction() {
        }

        @GuiIncluded
        public String toString() {
            return "!";
        }

        public void run() {
        }

        @GuiIncluded(false)
        public String subExcluded() {
            return "!";
        }
    }

    private <FromType, RetType> Set<RetType> toSet(Collection<FromType> col, Function<FromType, RetType> func) {
        return col.stream().map(func).collect(Collectors.toSet());
    }

    private <FromType, RetType> Set<RetType> toSet(Collection<FromType> col, Predicate<FromType> p, Function<FromType, RetType> func) {
        return col.stream().filter(p).map(func).collect(Collectors.toSet());
    }

    @Test
    public void testCreateMethods() {
        type = builder.create(Hello.class);
        assertTrue(type instanceof GuiTypeObject, "relaxed builder creates a package private object");
        GuiTypeObject typeObj = (GuiTypeObject) type;
        assertEquals(Set.of("action", "pubAction"), toSet(typeObj.getActions(), m -> m.getMethod().getName()), "public and package private, and non interface method");
    }

    @Test
    public void testCreateFields() {
        type = builder.create(Hello.class);
        assertTrue(type instanceof GuiTypeObject, "relaxed builder creates a package private object");
        GuiTypeObject typeObj = (GuiTypeObject) type;
        assertEquals(Set.of("prop", "pubProp"), toSet(typeObj.getProperties(), f -> f.getField().getName()), "public and package private fields");
    }

    @Test
    public void testCreateJdkExt() {
        type = builder.create(JdkExtTest.class);
        assertTrue(type instanceof GuiTypeObject, "relaxed builder creates a JDK extension class");
        GuiTypeObject typeObj = (GuiTypeObject) type;
        assertEquals(Set.of("hello", "world"), toSet(typeObj.getActions(), f -> f.getMethod().getName()), "public and package private declared methods without overriding JDK methods");
    }

    @Test
    public void testCreateExt() {
        type = builder.create(ExtTest.class);
        assertTrue(type instanceof GuiTypeObject, "relaxed builder creates an extension class");
        GuiTypeObject typeObj = (GuiTypeObject) type;
        assertEquals(Set.of("hello", "world", "anotherAction"), toSet(typeObj.getActions(), f -> f.getMethod().getName()), "public and package private declared methods with overriding non-JDK methods");
    }

    @Test
    public void testCreateExtFields() {
        type = builder.create(ExtTest.class);
        assertTrue(type instanceof GuiTypeObject, "relaxed builder creates an extension class");
        GuiTypeObject typeObj = (GuiTypeObject) type;
        assertEquals(Set.of("prop", "subProp"), toSet(typeObj.getProperties(), f -> f.getField() != null, //exclude toString()
                f -> f.getField().getName()),
                "public and package private declared fields with overriding non-JDK methods");
    }

    record ExampleRecord(String hello, int world) {
        String prop() {
            return "a";
        }

        void action() {
        }
    }

    static class ExampleEdit {
        String prop() {
            return "a";
        }

        @GuiIncluded(editor = true)
        ExampleEditTarget edit() {
            return new ExampleEditTarget();
        }

        void completeEdit(ExampleEditTarget e, boolean ok) {
        }

        void action() {
        }

        void setXNotifier(Runnable r) {
        }

        @GuiNotifierSetter
        void setPropNotifier(Runnable r) {
        }

        void actionList(List<String> items, String name) {
        }
    }

    static class ExampleEditTarget {
    }

    @Test
    public void testCreateRecord() {
        var rec = builder.create(ExampleRecord.class);

        assertTrue(rec instanceof GuiTypeObject, "relaxed builder creates record");

        var typeObj = (GuiTypeObject) rec;
        assertEquals(Set.of("hello", "world", "prop"), toSet(typeObj.getProperties(), f -> f.getGetter().getName()), "props incldues primary fields and methods with getters");

        assertEquals(Set.of("action"), toSet(typeObj.getActions(), f -> f.getMethod().getName()), "actions");
    }

    @Test
    public void testCreateEdit() {
        var rec = builder.create(ExampleEdit.class);

        assertTrue(rec instanceof GuiTypeObject, "obj with edit");

        var typeObj = (GuiTypeObject) rec;
        assertEquals(Set.of("prop"), toSet(typeObj.getProperties(), m -> m.getGetter() != null, m -> m.getGetter().getName()), "edit is not a prop");

        assertEquals(Set.of("setXNotifier"), toSet(typeObj.getProperties(), m -> m.getSetter() != null, m -> m.getSetter().getName()), "notifier needs annotation");

        assertEquals(Set.of("setPropNotifier"), toSet(typeObj.getNotifiers(), m -> m.getSetter().getName()), "annotated notifier");

        assertEquals(Set.of("action", "edit", "actionList"), toSet(typeObj.getActions(), m -> m.getMethod().getName()), "actions");

        var edit = typeObj.getActions().stream().filter(m -> m.getMethod().getName().equals("edit")).findFirst().orElse(null);

        assertTrue(edit.hasCompleteEditMethod(), "completeEdit");
        assertEquals("completeEdit", edit.getEditFeature().completeEditMethod().getName(), "completeEdit");

        var actionList = typeObj.getActions().stream().filter(m -> m.getMethod().getName().equals("actionList")).findFirst().orElse(null);

        assertTrue(actionList instanceof GuiTypeMemberActionList, "action list");
        assertTrue(((GuiTypeMemberActionList) actionList).isTakingTargetName(), "action list takes name");
    }
}
