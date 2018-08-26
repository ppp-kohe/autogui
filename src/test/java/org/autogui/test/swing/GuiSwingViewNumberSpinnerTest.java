package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingPreferences;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewNumberSpinner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class GuiSwingViewNumberSpinnerTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        GuiSwingViewNumberSpinnerTest t = new GuiSwingViewNumberSpinnerTest();
        t.setUp();
        t.testViewUpdateByteField();
    }

    GuiTypeBuilder typeBuilder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextPropByte;
    GuiMappingContext contextPropInt;
    GuiMappingContext contextPropLong;
    GuiMappingContext contextPropDouble;
    GuiMappingContext contextPropBigInt;
    GuiMappingContext contextPropBigDec;
    GuiMappingContext contextPropObj;
    TestObj obj;

    GuiSwingViewNumberSpinner spinner;

    JFrame frame;

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public byte numByte;
        @GuiIncluded public int numInt;
        @GuiIncluded public long numLong;
        @GuiIncluded public double numDouble;
        @GuiIncluded public BigInteger numBigInt = BigInteger.ZERO;
        @GuiIncluded public BigDecimal numBigDec = BigDecimal.ZERO;
        @GuiIncluded public Integer numObj;
    }

    @Before
    public void setUp() {
        typeBuilder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) typeBuilder.get(TestObj.class);

        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextPropByte = context.getChildByName("numByte");
        contextPropInt = context.getChildByName("numInt");
        contextPropLong = context.getChildByName("numLong");
        contextPropDouble = context.getChildByName("numDouble");
        contextPropBigInt = context.getChildByName("numBigInt");
        contextPropBigDec = context.getChildByName("numBigDec");
        contextPropObj = context.getChildByName("numObj");

        spinner = new GuiSwingViewNumberSpinner();
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public GuiSwingViewNumberSpinner.PropertyNumberSpinner create(GuiMappingContext contextProp) {
        JComponent c = spinner.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class);
    }


    @Test
    public void testViewUpdateByte() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropByte));
        run(this::runWait);

        obj.numByte = 127;
        contextPropByte.updateSourceFromRoot();
        Assert.assertEquals("listener update byte",
                (byte) 127, runGet(i::getValue));
    }


    @Test
    public void testViewUpdateInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        run(this::runWait);

        obj.numInt = 123;
        contextPropInt.updateSourceFromRoot();
        Assert.assertEquals("listener update int",
                123, runGet(i::getValue));
    }

    @Test
    public void testViewUpdateLong() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropLong));
        run(this::runWait);

        obj.numLong = 1234567890L;
        contextPropLong.updateSourceFromRoot();
        Assert.assertEquals("listener update long",
                1234567890L, runGet(i::getValue));
    }

    @Test
    public void testViewUpdateDouble() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropDouble));
        run(this::runWait);

        obj.numDouble = 123456.789;
        contextPropDouble.updateSourceFromRoot();
        Assert.assertEquals("listener update double",
                123456.789, runGet(i::getValue));
    }

    @Test
    public void testViewUpdateBigInteger() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigInt));
        run(this::runWait);

        obj.numBigInt = new BigInteger(Long.MAX_VALUE + "012345");
        contextPropBigInt.updateSourceFromRoot();
        Assert.assertEquals("listener update bigInt",
                new BigInteger(Long.MAX_VALUE + "012345"), runGet(i::getValue));
    }

    @Test
    public void testViewUpdateBigDecimal() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigDec));
        run(this::runWait);

        obj.numBigDec = new BigDecimal(Long.MAX_VALUE + "012345.678");
        contextPropBigDec.updateSourceFromRoot();
        Assert.assertEquals("listener update bigDecimal",
                new BigDecimal(Long.MAX_VALUE + "012345.678"), runGet(i::getValue));
    }

    @Test
    public void testViewUpdateIntObj() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropObj));
        run(this::runWait);

        obj.numObj = 123;
        contextPropObj.updateSourceFromRoot();
        Assert.assertEquals("listener update num-obj",
                123, runGet(i::getValue));
    }


    @Test
    public void testViewUpdateIntField() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        i.setSwingViewValueWithUpdate(123456);
        //i.getEditorField().setText("123456") ; //this does not cause a change event.
        run(this::runWait);
        Assert.assertEquals("after value set int",
                123456, obj.numInt);
        Assert.assertEquals("after value set, field update int",
                "123,456",
                i.getEditorField().getText());
    }


    @Test
    public void testViewUpdateBigDecField() throws Exception {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigDec));
        i.getEditorField().setText(Long.MAX_VALUE + "012345.678");
        i.commitEdit();
        run(() -> runWait(500));
        runWait(500);
        Assert.assertEquals("after value set bigDec",
                new BigDecimal(Long.MAX_VALUE + "012345.678"), obj.numBigDec);

        i.getEditorField().setText(Long.MAX_VALUE + "012345");
        i.commitEdit();
        run(() -> runWait(500));
        runWait(500);
        Assert.assertEquals("after value set bigDec",
                new BigDecimal(Long.MAX_VALUE + "012345"), obj.numBigDec);
    }

    @Test
    public void testViewUpdateByteField() throws Exception {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropByte));
        i.getEditorField().setText("554") ; //0b10_0010_1010 -> 0b0010_1010 =42
        i.commitEdit();
        run(() -> runWait(500));
        runWait(500);
        Assert.assertEquals("after value set byte",
                ((byte) 42), obj.numByte);
        Assert.assertEquals("after value set, field update byte",
                "42",
                i.getEditorField().getText());
    }


    @Test
    public void testViewMaxBigInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigInt));
        i.getModelTyped().setMaximum(128);
        i.getModelTyped().setMinimum(100);
        Assert.assertNull("custom max: return null by over the max", i.getModelTyped().getNextValue(BigInteger.valueOf(127)));
        Assert.assertEquals("custom max: return the max (-1)", BigInteger.valueOf(127), i.getModelTyped().getNextValue(BigInteger.valueOf(126)));
        Assert.assertNull("custom min: return null by over the min", i.getModelTyped().getPreviousValue(BigInteger.valueOf(100)));
        Assert.assertEquals("custom min: return the max", BigInteger.valueOf(100), i.getModelTyped().getPreviousValue(BigInteger.valueOf(101)));
        i.getModelTyped().setStepSize(11);
        Assert.assertEquals("custom step size: next", BigInteger.valueOf(121), i.getModelTyped().getNextValue(BigInteger.valueOf(110)));
        Assert.assertEquals("custom step size: prev", BigInteger.valueOf(100), i.getModelTyped().getPreviousValue(BigInteger.valueOf(111)));
    }

    @Test
    public void testViewPasteAllInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        setClipboardText("1234");
        GuiSwingViewNumberSpinner.NumberTextPasteAllAction a = findMenuItemAction(i.getSwingStaticMenuItems(), GuiSwingViewNumberSpinner.NumberTextPasteAllAction.class);
        a.actionPerformed(null);
        Assert.assertEquals("after paste-all", "1,234", i.getEditorField().getText());
        Assert.assertEquals("after paste-all", 1234, obj.numInt);
    }

    @Test
    public void testViewSetMaxInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberMaximumAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberMaximumAction.class,
                null, null, null, GuiSwingViewNumberSpinner.NumberMaximumAction::isMax);
        a.actionPerformed(null);
        Assert.assertEquals("set-maximum action", Integer.MAX_VALUE, obj.numInt);
    }

    @Test
    public void testViewSetMinInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberMaximumAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberMaximumAction.class,
                null, null, null, act -> !act.isMax());
        a.actionPerformed(null);
        Assert.assertEquals("set-minimum action", Integer.MIN_VALUE, obj.numInt);
    }

    @Test
    public void testViewIncInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberIncrementAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberIncrementAction.class,
                null, null, null, GuiSwingViewNumberSpinner.NumberIncrementAction::isInc);
        a.actionPerformed(null);
        Assert.assertEquals("increment action", 1, obj.numInt);
    }

    @Test
    public void testViewDecInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberIncrementAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberIncrementAction.class,
                null, null, null, act -> !act.isInc());
        a.actionPerformed(null);
        Assert.assertEquals("decrement action", -1, obj.numInt);
    }

    @Test
    public void testViewLoadSettings() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        GuiPreferences prefs = parentPrefs.getChild(contextPropInt);
        prefs.getValueStore().putString("maximum", "1000000");
        prefs.getValueStore().putString("minimum", "100");
        prefs.getValueStore().putString("stepSize", "3");
        prefs.getValueStore().putString("format", "0000.0");
        prefs.getValueStore().putString("$value", "500");
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        run(() -> i.loadSwingPreferences(prefs));

        boolean hasMin = runGet(() -> i.getSettingAction().getPane().getMinCheckBox().isSelected());
        Assert.assertTrue("after prefs load: minimum", hasMin);

        boolean hasMax = runGet(() -> i.getSettingAction().getPane().getMaxCheckBox().isSelected());
        Assert.assertTrue("after prefs load: maximum", hasMax);

        Object min = runGet(() -> i.getSettingAction().getPane().getMinSpinner().getValue());
        Assert.assertEquals("after prefs load: minimum value", 100, min);
        Object modelMin = runGet(() -> i.getModelTyped().getMinimum());
        Assert.assertEquals("after prefs load: minimum value", 100, modelMin);

        Object max = runGet(() -> i.getSettingAction().getPane().getMaxSpinner().getValue());
        Assert.assertEquals("after prefs load: maximum value", 1000_000, max);
        Object modelMax = runGet(() -> i.getModelTyped().getMaximum());
        Assert.assertEquals("after prefs load: maximum value", 1000_000, modelMax);

        Object step = runGet(() -> i.getSettingAction().getPane().getStepSpinner().getValue());
        Assert.assertEquals("after prefs load: step value", 3, step);
        Object modelStep = runGet(() -> i.getModelTyped().getStepSize());
        Assert.assertEquals("after prefs load: step value", 3, modelStep);

        String str = runGet(() -> i.getEditorField().getText());
        Assert.assertEquals("after prefs load: format and $value", "0500.0", str);

        Object val = runGet(i::getValue);
        Assert.assertEquals("after prefs load: value", 500, val);

        boolean hasFmt = runGet(() -> i.getSettingAction().getPane().getFormatCheckBox().isSelected());
        Assert.assertTrue("after prefs load: format", hasFmt);
        String fmt = runGet(() -> i.getSettingAction().getPane().getFormatField().getText());
        Assert.assertEquals("after prefs load: format", "0000.0", fmt);
    }

    @Test
    public void testViewSaveSettings() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(parentPrefs);

        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        i.setPreferencesUpdater(GuiSwingPreferences.PreferencesUpdateEvent::save);

        run(() -> i.getSettingAction().getPane().getMinCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getMinSpinner().setValue(123));

        run(() -> i.getSettingAction().getPane().getMaxCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getMaxSpinner().setValue(123_123_123));

        run(() -> i.getSettingAction().getPane().getStepSpinner().setValue(4));

        run(() -> i.getSettingAction().getPane().getFormatCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getFormatField().setText("##.0"));

        run(() -> i.setSwingViewValueWithUpdate(123_123));

        run(() -> runWait(500));
        run(this::runWait);

        GuiPreferences targetPrefs = parentPrefs.getDescendant(contextPropInt);
        String max = targetPrefs.getValueStore().getString("maximum", "");
        String min = targetPrefs.getValueStore().getString("minimum", "");
        String step = targetPrefs.getValueStore().getString("stepSize", "");
        String fmt = targetPrefs.getValueStore().getString("format", "");
        String val = targetPrefs.getValueStore().getString("$value", "");

        Assert.assertEquals("after prefs update: maximum", "123123123", max);
        Assert.assertEquals("after prefs update: minimum", "123", min);
        Assert.assertEquals("after prefs update: stepSize", "4", step);
        Assert.assertEquals("after prefs update: format", "##.0", fmt);
        Assert.assertEquals("after prefs update: $value", "123123", val);

        GuiPreferences savePrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        run(() -> i.saveSwingPreferences(savePrefs));

        GuiPreferences savedTarget = savePrefs.getDescendant(contextPropInt);
        max = savedTarget.getValueStore().getString("maximum", "");
        min = savedTarget.getValueStore().getString("minimum", "");
        step = savedTarget.getValueStore().getString("stepSize", "");
        fmt = savedTarget.getValueStore().getString("format", "");
        val = savedTarget.getValueStore().getString("$value", "");

        Assert.assertEquals("after prefs update: maximum", "123123123", max);
        Assert.assertEquals("after prefs update: minimum", "123", min);
        Assert.assertEquals("after prefs update: stepSize", "4", step);
        Assert.assertEquals("after prefs update: format", "##.0", fmt);
        Assert.assertEquals("after prefs update: $value", "123123", val);


    }
}
