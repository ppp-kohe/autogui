package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.base.type.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class GuiTypeBuilderRelaxedTest {
    GuiTypeBuilder.GuiTypeBuilderRelaxed builder;
    GuiTypeElement type;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder.GuiTypeBuilderRelaxed();
    }

    static class Hello implements AutoCloseable {
        private String prvProp;
        protected String prtProp;
        String prop;
        public String pubProp;

        private void prvAction() {}
        protected void prtAction() {}
        void action() {}
        public void pubAction() {}

        public void close() {}
    }

    static class JdkExtTest extends AbstractMap<String,String> implements AutoCloseable {
        private List<Entry<String,String>> es = new ArrayList<>();
        @Override
        public Set<Entry<String,String>> entrySet() {
            return new HashSet<>(es);
        }

        String prop;

        void hello() {}
        public void world() {}
        private void prvAction() {}
        public void close() {}

        @GuiIncluded(false)
        public String excluded() {return "!";}
    }
    static class ExtTest extends JdkExtTest implements Runnable {
        String subProp;

        void hello() {}
        public void anotherAction() {}
        @GuiIncluded
        public String toString() {return "!";}
        public void run() {}
        @GuiIncluded(false)
        public String subExcluded() {return "!";}
    }

    @Test
    public void testCreateMethods() {
        type = builder.create(Hello.class);
        Assert.assertTrue("relaxed builder creates a package private object",
                type instanceof GuiTypeObject);
        GuiTypeObject typeObj = (GuiTypeObject) type;
        Assert.assertEquals("public and package private, and non interface method",
                new HashSet<>(Arrays.asList("action", "pubAction")),
                typeObj.getActions().stream()
                .map(m -> m.getMethod().getName())
                .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateFields() {
        type = builder.create(Hello.class);
        Assert.assertTrue("relaxed builder creates a package private object",
                type instanceof GuiTypeObject);
        GuiTypeObject typeObj = (GuiTypeObject) type;
        Assert.assertEquals("public and package private fields",
                new HashSet<>(Arrays.asList("prop", "pubProp")),
                typeObj.getProperties().stream()
                        .map(f -> f.getField().getName())
                        .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateJdkExt() {
        type = builder.create(JdkExtTest.class);
        Assert.assertTrue("relaxed builder creates a JDK extension class",
                type instanceof GuiTypeObject);
        GuiTypeObject typeObj = (GuiTypeObject) type;
        Assert.assertEquals("public and package private declared methods without overriding JDK methods",
                new HashSet<>(Arrays.asList("hello", "world")),
                typeObj.getActions().stream()
                        .map(f -> f.getMethod().getName())
                        .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateExt() {
        type = builder.create(ExtTest.class);
        Assert.assertTrue("relaxed builder creates an extension class",
                type instanceof GuiTypeObject);
        GuiTypeObject typeObj = (GuiTypeObject) type;
        Assert.assertEquals("public and package private declared methods with overriding non-JDK methods",
                new HashSet<>(Arrays.asList("hello", "world", "anotherAction", "toString")),
                typeObj.getActions().stream()
                        .map(f -> f.getMethod().getName())
                        .collect(Collectors.toSet()));
    }

    @Test
    public void testCreateExtFields() {
        type = builder.create(ExtTest.class);
        Assert.assertTrue("relaxed builder creates an extension class",
                type instanceof GuiTypeObject);
        GuiTypeObject typeObj = (GuiTypeObject) type;
        Assert.assertEquals("public and package private declared fields with overriding non-JDK methods",
                new HashSet<>(Arrays.asList("prop", "subProp")),
                typeObj.getProperties().stream()
                        .map(f -> f.getField().getName())
                        .collect(Collectors.toSet()));
    }
}
