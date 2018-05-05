package autogui.base.mapping;

import autogui.GuiIncluded;
import autogui.base.type.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GuiReprStringFieldTest {

    GuiReprValueStringField fld;

    GuiTypeBuilder builder;
    GuiTypeValue type;

    GuiTypeObject typeObject;
    GuiTypeMemberProperty property;

    String value;
    TestObjRepr obj;

    GuiMappingContext contextValue;
    GuiMappingContext contextProp;

    @Before
    public void setUp() {
        fld = new GuiReprValueStringField();

        builder = new GuiTypeBuilder();
        type = (GuiTypeValue) builder.get(String.class);

        typeObject = (GuiTypeObject) builder.get(TestObjRepr.class);
        property = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        value = "hello\nworld";

        obj = new TestObjRepr();
        obj.value = "hello";

        contextValue = new GuiMappingContext(type, fld);
        contextProp = new GuiMappingContext(property, fld);
    }

    @GuiIncluded
    public static class TestObjRepr {
        @GuiIncluded
        public String value;
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, value);
        Assert.assertTrue("match with typed context",
                fld.match(ctx));
        Assert.assertEquals("match sets contexts repr",
                fld,
                ctx.getRepresentation());
    }


    @Test
    public void testValueMatchProp() {
        GuiMappingContext ctx = new GuiMappingContext(property, value);
        Assert.assertTrue("match prop with typed context",
                fld.match(ctx));
        Assert.assertEquals("match prop sets contexts repr",
                fld,
                ctx.getRepresentation());
    }

    @Test
    public void testValueGetValueNoParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with no-source parent and no-source prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }


    @Test
    public void testValueGetValueParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with a parent and no-source prev return no-update",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }


    @Test
    public void testValueGetValueParentPrevSame() throws Throwable {
        Assert.assertEquals("getValue with a parent and same prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValueNoParentPrev() throws Throwable {
        Assert.assertEquals("getValue with no-parent and a prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValueParentPrevDiff() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the parent",
                GuiUpdatedValue.of("world"),
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("world"),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValuePropNoParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with no parent and no prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }

    @Test
    public void testValueGetValuePropParentPrevDiff() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the prop value",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")));
    }

    @Test
    public void testValueGetValuePropNoParentPrev() throws Throwable {
        Assert.assertEquals("getValue with no-parent and prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")));
    }


    @Test
    public void testValueGetValuePropParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the prop value",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }

    @Test
    public void testValueGetValuePropParentPrevSame() throws Throwable {
        Assert.assertEquals("getValue with a parent and same prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueUpdateNone() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.NO_SOURCE,
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        Assert.assertEquals("update with same prev source returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.NO_SOURCE,
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateNoneParent() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateParent() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        Assert.assertEquals("update with same prev source returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateProp() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "world",
                fld.update(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        "world", GuiReprValue.NONE));
        Assert.assertEquals("update updates field",
                "world",
                obj.value);
    }

    @Test
    public void testValueGetParentSourceNone() throws Throwable {
        Assert.assertEquals("getParentSource with no parent returns none",
                GuiMappingContext.NO_SOURCE,
                fld.getParentSource(contextValue, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetValueWithoutNoUpdate() throws Throwable {
        Assert.assertEquals("getValueWithoutNoUpdate returns parent value",
                "hello",
                fld.getValueWithoutNoUpdate(contextValue,
                        GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE));
    }

    @Test
    public void testValueGetValueWithoutNoUpdateNone() throws Throwable {
        Assert.assertNull("getValueWithoutNoUpdate with no parent returns null",
                fld.getValueWithoutNoUpdate(contextValue,
                        GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE));
    }
}
