package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiUpdatedValue;
import org.autogui.base.mapping.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GuiReprPropertyPaneTest {
    GuiReprPropertyPane prop;
    GuiReprObjectPane objPane;

    GuiTypeObject typeObject;
    GuiTypeMemberProperty typeProperty;

    GuiTypeBuilder builder;
    GuiMappingContext contextObj;
    GuiMappingContext contextProp;
    GuiMappingContext contextValue;

    GuiMappingContext contextReadOnly;

    TestObjReprProp obj;
    GuiTaskClock viewClock;

    @Before
    public void setUp() {
        viewClock = new GuiTaskClock(true);

        builder = new GuiTypeBuilder();

        typeObject = (GuiTypeObject) builder.get(TestObjReprProp.class);
        typeProperty = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        prop = new GuiReprPropertyPane(GuiRepresentation.getDefaultSet());
        objPane = new GuiReprObjectPane(prop); //custom ObjectPane; holds PropertyPane and the prop of the obj only matches to the PropertyPane

        obj = new TestObjReprProp();
        obj.value = "hello";

        contextObj = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typeObject, null, null,
                GuiMappingContext.GuiSourceValue.of(obj));

        objPane.match(contextObj);

        contextProp = contextObj.getChildByName("value");
        contextValue = contextProp.getChildByName("String");

        contextReadOnly = contextObj.getChildByName("readOnly").getChildByName("String");
    }

    @GuiIncluded
    public static class TestObjReprProp {
        @GuiIncluded
        public String value;

        @GuiIncluded
        public String getReadOnly() {
            return "read-only";
        }
    }

    @Test
    public void testPropMatchCreation() {
        Assert.assertEquals("obj creates prop as its wrapped reprs",
                "value",
                contextProp.getName());
        Assert.assertTrue("obj creates prop",
                contextProp.getRepresentation() instanceof GuiReprPropertyPane);

        Assert.assertTrue("prop pane is str",
                contextValue.getRepresentation() instanceof GuiReprValueStringField);
    }

    @Test
    public void testValueGetUpdatedValueProp() throws Throwable {
        Assert.assertEquals("prop value",
                GuiUpdatedValue.of("hello"),
                contextProp.getReprValue()
                    .getUpdatedValue(contextProp, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValuePropChild() throws Throwable {
        Assert.assertEquals("prop child value",
                GuiUpdatedValue.of("hello"),
                contextValue.getReprValue()
                        .getUpdatedValue(contextValue, GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.getReprValue()
                .updateFromGui(contextValue, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("prop child value updates parent prop",
                "world",
                obj.value);
    }

    @Test
    public void testValueIsEditableUnderProp() {
        Assert.assertFalse("isEditable for value under read-only prop returns false",
                contextReadOnly.getReprValue().isEditable(contextReadOnly));
    }

}
