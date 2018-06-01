package autogui.base.mapping;

import autogui.GuiIncluded;
import autogui.base.type.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class GuiReprObjectPaneTest {
    GuiReprObjectPane objPane;
    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    GuiTypeMemberProperty typeProperty;

    TestReprObjPane objRepr;

    GuiMappingContext contextObj;
    GuiMappingContext contextStr;
    GuiMappingContext contextX;
    TestUpdater testUpdater;

    GuiTaskClock viewClock;

    @Before
    public void setUp() {
        viewClock = new GuiTaskClock(true);;
        objPane = new GuiReprObjectPane(GuiRepresentation.getDefaultSet());

        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestReprObjPane.class);
        typeProperty = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        objRepr = new TestReprObjPane();
        objRepr.value = "hello";
        objRepr.x = 123;

        contextObj = new GuiMappingContextForDebug(typeObject, objPane, null, GuiMappingContext.GuiSourceValue.of(objRepr));
        objPane.match(contextObj);

        contextStr = contextObj.getChildByName("value");
        contextX = contextObj.getChildByName("x");

        testUpdater = new TestUpdater();
        contextX.addSourceUpdateListener(testUpdater);

    }

    @GuiIncluded
    public static class TestReprObjPane {
        @GuiIncluded(index = 0)
        public String value;

        @GuiIncluded(index = 1)
        public int x;
    }

    public static class TestUpdater implements GuiMappingContext.SourceUpdateListener  {
        public List<Object> newValues = new ArrayList<>();

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock clock) {
            newValues.add(newValue);
        }
    }

    static class GuiMappingContextForDebug extends GuiMappingContext {

        public GuiMappingContextForDebug(GuiTypeElement typeElement, Object source) {
            super(typeElement, source);
        }

        public GuiMappingContextForDebug(GuiTypeElement typeElement, GuiRepresentation representation) {
            super(typeElement, representation);
        }

        public GuiMappingContextForDebug(GuiTypeElement typeElement, GuiRepresentation representation,
                                         GuiMappingContext parent, GuiSourceValue source) {
            super(typeElement, representation, parent, source);
        }

        @Override
        public void updateSourceFromGuiByThisDelayed() {
            updateSourceFromRoot(this); //immediate
        }

        @Override
        public <T> T execute(Callable<T> task) throws Throwable {
            return task.call();
        }

        @Override
        public GuiMappingContext createChildCandidate(GuiTypeElement typeElement) {
            return new GuiMappingContextForDebug(typeElement, null, this, NO_SOURCE);
        }
    }


    @Test
    public void testObjectPaneMatch() {
        GuiMappingContext ctx = new GuiMappingContext(typeObject, objPane, objRepr);
        Assert.assertTrue("match with obj type",
                objPane.match(ctx));

        Assert.assertEquals("match sets repr",
                objPane,
                ctx.getRepresentation());

        GuiMappingContext contextProp = ctx.getChildren().get(0);
        Assert.assertEquals("match creates a sub-context with prop repr",
                typeProperty, contextProp.getTypeElement());

        Assert.assertTrue("match creates a sub-context with prop supported pane",
                contextProp.getRepresentation() instanceof GuiReprValueStringField);

    }

    @Test
    public void testValueGetParentSource() throws Throwable {
        Assert.assertEquals("getParentSource returns parent source obj",
                GuiMappingContext.GuiSourceValue.of(objRepr),
                contextStr.getReprValue().getParentSource(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValue() throws Throwable {
        Assert.assertEquals("getUpdatedValue returns prop value",
                GuiUpdatedValue.of("hello"),
                contextStr.getReprValue()
                        .getUpdatedValue(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValueWithoutNoUpdate() throws Throwable {
        Assert.assertEquals("getUpdatedValueWithoutNoUpdate returns current prop",
                "hello",
                contextStr.getReprValue()
                        .getUpdatedValueWithoutNoUpdate(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueCheckAndUpdateSourceProp() {
        Assert.assertTrue("checkAndUpdateSource returns true",
                contextStr.getReprValue().checkAndUpdateSource(contextStr));

        Assert.assertEquals("checkAndUpdateSource sets source",
                GuiMappingContext.GuiSourceValue.of("hello"),
                contextStr.getSource());
    }

    @Test
    public void testValueCheckAndUpdateSourceObjNone() {
        Assert.assertFalse("checkAndUpdateSource returns false for no-update",
                contextObj.getRepresentation().checkAndUpdateSource(contextObj));
    }

    @Test
    public void testValueUpdateFromGui() {
        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("after updateFromGui, updated fields (from NO_SOURCE) are notified",
                Collections.singletonList(123),
                testUpdater.newValues);
    }


    @Test
    public void testValueUpdateFromGuiNoUpdate() {
        contextX.setSource(GuiMappingContext.GuiSourceValue.of(123));

        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("after updateFromGui, no changed values notify nothing",
                Collections.emptyList(),
                testUpdater.newValues);
    }
}
