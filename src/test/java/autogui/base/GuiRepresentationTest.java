package autogui.base;

import autogui.GuiIncluded;
import autogui.base.mapping.*;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeElement;
import autogui.base.type.GuiTypeMemberProperty;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GuiRepresentationTest {
    @Test
    public void testString() {
        GuiReprValueStringField fld = new GuiReprValueStringField();
        GuiTypeElement type = new GuiTypeBuilder().get(String.class);

        String value = "hello\nworld";
        GuiMappingContext ctx = new GuiMappingContext(type, fld, value);

        Object strValue = fld.toUpdateValue(ctx, value);
        Assert.assertEquals(value, strValue);

        Assert.assertEquals("", fld.toUpdateValue(ctx, null));

        Assert.assertFalse(fld.isEditable(ctx));

        Assert.assertTrue(fld.matchValueType(String.class));
        Assert.assertFalse(fld.isJsonSetter());

        String jsonValue = (String) fld.toJson(ctx, value);
        Assert.assertEquals(value, jsonValue);

        Object jsonValueNull = fld.toJson(ctx, null);
        Assert.assertNull(jsonValueNull);

        Assert.assertEquals(value, fld.fromJson(ctx, null, value));
        Assert.assertNull(fld.fromJson(ctx, null, null));
    }

    @Test
    public void testBoolean() {
        GuiReprValueBooleanCheckBox repr = new GuiReprValueBooleanCheckBox();
        GuiTypeElement type = new GuiTypeBuilder().get(Boolean.class);

        GuiMappingContext ctx = new GuiMappingContext(type, repr, Boolean.TRUE);

        Assert.assertTrue(repr.matchValueType(Boolean.class));
        Assert.assertTrue(repr.matchValueType(boolean.class));
        Assert.assertFalse(repr.isEditable(ctx));
        Assert.assertFalse(repr.isJsonSetter());

        Assert.assertEquals(Boolean.TRUE, repr.toUpdateValue(ctx, Boolean.TRUE));
        Assert.assertEquals(Boolean.FALSE, repr.toUpdateValue(ctx, null));

        Assert.assertEquals(Boolean.TRUE, repr.toJson(ctx, Boolean.TRUE));
        Assert.assertNull(repr.toJson(ctx, null));

        Assert.assertEquals(Boolean.TRUE, repr.fromJson(ctx, null, Boolean.TRUE));
        Assert.assertNull(repr.fromJson(ctx, null, null));

        Assert.assertEquals(Boolean.TRUE, repr.getBooleanValue("true"));
        Assert.assertEquals(Boolean.FALSE, repr.getBooleanValue("false"));
        Assert.assertEquals(Boolean.TRUE, repr.getBooleanValue("1"));
        Assert.assertEquals(Boolean.FALSE, repr.getBooleanValue("0"));
        Assert.assertEquals(Boolean.TRUE, repr.getBooleanValue("TRUE"));
        Assert.assertEquals(Boolean.FALSE, repr.getBooleanValue("FALSE"));
        Assert.assertNull(repr.getBooleanValue("otherValues"));
        Assert.assertNull(repr.getBooleanValue(null));
    }

    @Test
    public void testEnum() {

        GuiReprValueEnumComboBox repr = new GuiReprValueEnumComboBox();
        GuiTypeElement type = new GuiTypeBuilder().get(EnumValue.class);

        GuiMappingContext ctx = new GuiMappingContext(type, repr, EnumValue.HelloWorld);

        Assert.assertTrue(repr.matchValueType(EnumValue.class));

        Assert.assertEquals("Hello World", repr.getDisplayName(ctx, EnumValue.HelloWorld));

        Assert.assertFalse(repr.isEditable(ctx));
        Assert.assertFalse(repr.isJsonSetter());

        Assert.assertEquals("HelloWorld", repr.toJson(ctx, EnumValue.HelloWorld));
        Assert.assertNull(repr.toJson(ctx, null));

        Assert.assertEquals(EnumValue.HelloWorld, repr.fromJson(ctx, null, "HelloWorld"));
        Assert.assertNull(repr.fromJson(ctx, null, "unknown"));
        Assert.assertNull(repr.fromJson(ctx, null, null));

        Assert.assertArrayEquals(new Object[] {EnumValue.HelloWorld, EnumValue.Test}, repr.getEnumConstants(ctx));

        Assert.assertEquals(EnumValue.HelloWorld, repr.getEnumValue(ctx, "HelloWorld"));
        Assert.assertEquals(EnumValue.HelloWorld, repr.getEnumValue(ctx, "helloworld"));
        Assert.assertEquals(EnumValue.HelloWorld, repr.getEnumValue(ctx, "helloWorld"));
        Assert.assertEquals(EnumValue.HelloWorld, repr.getEnumValue(ctx, "HELLOWORLD"));
        Assert.assertEquals(EnumValue.Test, repr.getEnumValue(ctx, "1"));
        Assert.assertNull(repr.getEnumValue(ctx, "3"));
        Assert.assertNull(repr.getEnumValue(ctx, "-3"));
    }

    @Test
    public void testNumber() {
        GuiReprValueNumberSpinner repr = new GuiReprValueNumberSpinner();
        GuiTypeElement type = new GuiTypeBuilder().get(int.class);
        GuiMappingContext ctx = new GuiMappingContext(type, repr, 123);

        Assert.assertTrue(repr.matchValueType(int.class));
        Assert.assertTrue(repr.matchValueType(Integer.class));

        Assert.assertFalse(repr.isRealNumberType(ctx));
        Assert.assertTrue(repr.isRealNumberType(float.class));
        Assert.assertTrue(repr.isRealNumberType(Float.class));

        Assert.assertFalse(repr.isEditable(ctx));
        Assert.assertFalse(repr.isJsonSetter());

        Assert.assertEquals(123, repr.toJson(ctx, 123));
        Assert.assertNull(repr.toJson(ctx, null));
        Assert.assertEquals(123, repr.fromJson(ctx, null, 123));
        Assert.assertEquals(123, repr.fromJson(ctx, null, 123.456));
        Assert.assertNull(repr.fromJson(ctx, null, null));
    }

    @Test
    public void testNumberFloat() {
        GuiReprValueNumberSpinner repr = new GuiReprValueNumberSpinner();
        GuiTypeElement type = new GuiTypeBuilder().get(float.class);
        GuiMappingContext ctx = new GuiMappingContext(type, repr, 123.456f);

        Assert.assertEquals(123.456f, repr.toJson(ctx, 123.456f));
        Assert.assertEquals(123.456f, repr.fromJson(ctx, null, 123.456f));
        Assert.assertEquals(123f, repr.fromJson(ctx, null, 123));
    }

    @Test
    public void testNumberBigDecimal() {
        GuiReprValueNumberSpinner repr = new GuiReprValueNumberSpinner();
        GuiTypeElement type = new GuiTypeBuilder().get(BigDecimal.class);
        GuiMappingContext ctx = new GuiMappingContext(type, repr, BigDecimal.valueOf(123.456));

        Assert.assertEquals("123.456", repr.toJson(ctx, BigDecimal.valueOf(123.456)));
        Assert.assertEquals(BigDecimal.valueOf(123.456), repr.fromJson(ctx, null, "123.456"));
        Assert.assertEquals(BigDecimal.valueOf(123.456), repr.fromJson(ctx, null, 123.456));
    }

    String pathSrcMain = "src" + File.separator + "main";
    String pathSrcTest = "src" + File.separator + "test";

    @Test
    public void testFilePathPath() {
        GuiReprValueFilePathField repr = new GuiReprValueFilePathField();
        GuiTypeElement type = new GuiTypeBuilder().get(Path.class);
        GuiMappingContext ctx = new GuiMappingContext(type, repr, Paths.get(pathSrcMain));
        ctx.setPreferences(new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(null), ctx));

        Assert.assertTrue(repr.matchValueType(File.class));
        Assert.assertTrue(repr.matchValueType(Path.class));
        Assert.assertFalse(repr.isEditable(ctx));
        Assert.assertFalse(repr.isJsonSetter());

        Assert.assertEquals(Paths.get(pathSrcMain), repr.toValueFromPath(ctx, Paths.get(pathSrcMain)));
        Assert.assertNull(repr.toValueFromPath(ctx, null));

        Assert.assertEquals(Paths.get(pathSrcMain), repr.toUpdateValue(ctx, Paths.get(pathSrcMain)));
        Assert.assertNull(repr.toUpdateValue(ctx, null));

        repr.updateFromGui(ctx, Paths.get(pathSrcTest));
        Assert.assertEquals(Paths.get(pathSrcTest), ctx.getSource());

        Assert.assertEquals(pathSrcMain, repr.toJson(ctx, Paths.get(pathSrcMain)));
        Assert.assertNull(repr.toJson(ctx, null));

        Assert.assertEquals(Paths.get(pathSrcMain), repr.fromJson(ctx, null, pathSrcMain));
        Assert.assertNull(repr.fromJson(ctx, null, null));
    }

    @Test
    public void testFilePathFile() {
        GuiReprValueFilePathField repr = new GuiReprValueFilePathField();
        GuiTypeElement type = new GuiTypeBuilder().get(File.class);
        GuiMappingContext ctx = new GuiMappingContext(type, repr, new File(pathSrcMain));
        ctx.setPreferences(new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), ctx));

        Assert.assertEquals(new File(pathSrcMain), repr.toValueFromPath(ctx, Paths.get(pathSrcMain)));
        Assert.assertNull(repr.toValueFromPath(ctx, null));

        Assert.assertEquals(Paths.get(pathSrcMain), repr.toUpdateValue(ctx, new File(pathSrcMain)));
        Assert.assertNull(repr.toUpdateValue(ctx, null));


        repr.updateFromGui(ctx, Paths.get(pathSrcTest));
        Assert.assertEquals(new File(pathSrcTest), ctx.getSource());

        Assert.assertEquals(pathSrcMain, repr.toJson(ctx, new File(pathSrcMain)));
        Assert.assertNull(repr.toJson(ctx, null));

        Assert.assertEquals(new File(pathSrcMain), repr.fromJson(ctx, null, pathSrcMain));
        Assert.assertNull(repr.fromJson(ctx, null, null));
    }

    @Test
    public void testValue() {
        GuiReprValueStringField repr = new GuiReprValueStringField();
        GuiTypeElement type = new GuiTypeBuilder().get(String.class);

        String value = "hello\nworld";
        GuiMappingContext ctx = new GuiMappingContext(type, repr, value);

        repr.setSource(ctx, "hello");
        Assert.assertEquals("hello", ctx.getSource());

        Object v = "";
        try {
            v = repr.getUpdatedValueWithoutNoUpdate(ctx, false);
        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals("hello", v);

        Assert.assertEquals("hello", GuiReprValue.castOrMake(String.class, "hello", () -> "world"));
        Assert.assertEquals("world", GuiReprValue.castOrMake(String.class, 123, () -> "world"));
        Assert.assertEquals("new", GuiReprValue.castOrMake(String.class, null, () -> "new"));

        Assert.assertEquals("hello", repr.toHumanReadableString(ctx, "hello"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProperty() {
        GuiReprPropertyPane repr = new GuiReprPropertyPane(new GuiReprValueStringField());
        GuiTypeElement objType = new GuiTypeBuilder().get(TestValueString.class);
        GuiTypeMemberProperty propType = (GuiTypeMemberProperty) objType.getChildren().get(0);

        TestValueString obj = new TestValueString();
        GuiMappingContext parent = new GuiMappingContext(objType, new GuiReprValueLabel(), obj);
        parent.setPreferences(new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), parent));

        GuiMappingContext ctx = new GuiMappingContext(propType, parent);
        ctx.setSource("hello");
        ctx.setRepresentation(repr);
        ctx.addToParent();

        GuiMappingContext child = new GuiMappingContext(propType.getType(), ctx);
        child.setRepresentation(new GuiReprValueStringField());
        child.addToParent();

        Assert.assertTrue(repr.isJsonSetter());

        Assert.assertTrue(repr.isEditable(ctx));
        Assert.assertTrue(repr.isEditableFromChild(child));

        repr.updateFromGuiChild(child, "Test");
        Assert.assertEquals("Test", obj.hello);

        Map<String,Object> json = (Map<String,Object>) repr.toJsonWithNamed(ctx, new GuiReprValue.NamedValue("hello", "world"));
        Assert.assertEquals("world", json.get("hello"));

        Map<String,Object> json2 = (Map<String,Object>) repr.toJsonWithNamed(ctx, "world");
        Assert.assertEquals("world", json2.get("hello"));

        Map<String,Object> json3 = (Map<String,Object>) repr.toJsonWithNamed(ctx, null);
        Assert.assertNull(json3.get("hello"));

        Map<String,Object> inputJson = new HashMap<>();
        inputJson.put("hello", "world");
        GuiReprValue.NamedValue ret = (GuiReprValue.NamedValue) repr.fromJsonWithNamed(ctx, new GuiReprValue.NamedValue("hello", "test"), inputJson);
        Assert.assertEquals("hello", ret.name);
        Assert.assertEquals("world", ret.value);

        String ret2 = (String) repr.fromJsonWithNamed(ctx, null, inputJson);
        Assert.assertEquals("world", ret2);

        Assert.assertNull(repr.fromJsonWithNamed(ctx, null, null));

        Assert.assertEquals("world", repr.toHumanReadableString(ctx, "world"));
    }

    @SuppressWarnings("all")
    @Test
    public void testObj() {
        GuiMappingContext context = new GuiMappingContext(new GuiTypeBuilder().get(TestObj.class));
        Assert.assertTrue(GuiRepresentation.getDefaultSet().match(context));

        TestObj obj = new TestObj();
        obj.o = new TestValueString("world");
        obj.s = "test";
        obj.i = 123;
        obj.p = Paths.get(pathSrcMain);
        obj.e = EnumValue.HelloWorld;
        obj.b = true;

        context.setSource(obj);
        context.updateSourceFromRoot();

        GuiMappingContext sub = context.getChildren().stream()
            .filter(e -> e.getName().equals("i"))
            .findFirst().orElse(null);

        Assert.assertEquals(123, sub.getSource());
        ((GuiReprValueNumberSpinner) sub.getRepresentation()).updateFromGui(sub, 456);
        Assert.assertEquals(456, obj.i);

        GuiReprObjectPane repr = (GuiReprObjectPane) context.getRepresentation();
        Assert.assertTrue(repr.isJsonSetter());

        Map<String,Object> json = (Map<String,Object>) repr.toJson(context, obj);
        Assert.assertEquals("test", json.get("s"));
        Assert.assertEquals(456, json.get("i"));
        Assert.assertEquals(pathSrcMain, json.get("p"));
        Assert.assertEquals("HelloWorld", json.get("e"));
        Assert.assertEquals(Boolean.TRUE, json.get("b"));
        Map<String,Object> subJson = (Map<String,Object>) json.get("o");
        Assert.assertEquals("world", subJson.get("hello"));

        TestObj obj2 = (TestObj) repr.fromJson(context, null, json);
        Assert.assertEquals("test", obj2.s);
        Assert.assertEquals(456, obj2.i);
        Assert.assertEquals(Paths.get(pathSrcMain), obj2.p);
        Assert.assertEquals(EnumValue.HelloWorld, obj2.e);
        Assert.assertEquals(Boolean.TRUE, obj2.b);
        Assert.assertEquals("world", obj2.o.hello);

        String str = repr.toHumanReadableString(context, obj2);
        Set<String> words = new HashSet<>(Arrays.asList(str.split("\t")));
        Assert.assertEquals(new HashSet<>(Arrays.asList("test", "456", pathSrcMain, "HelloWorld", "true", "world")), words);
    }

    public enum EnumValue {
        HelloWorld,
        Test
    }

    @GuiIncluded
    public static class TestValueString {
        @GuiIncluded public String hello;

        public TestValueString() {
        }

        public TestValueString(String hello) {
            this.hello = hello;
        }

        @Override
        public String toString() {
            return "hello:" + hello;
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public String s;
        @GuiIncluded public int i;
        @GuiIncluded public boolean b;
        @GuiIncluded public Path p;
        @GuiIncluded public EnumValue e;
        @GuiIncluded public TestValueString o;

        @GuiIncluded public void action() { ++i; }
    }
}
