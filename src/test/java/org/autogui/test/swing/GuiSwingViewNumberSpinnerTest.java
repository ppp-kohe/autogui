package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewNumberSpinner;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

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

    public GuiSwingViewNumberSpinnerTest() {
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public byte numByte;
        @GuiIncluded
        public int numInt;
        @GuiIncluded
        public long numLong;
        @GuiIncluded
        public double numDouble;
        @GuiIncluded
        public BigInteger numBigInt = BigInteger.ZERO;
        @GuiIncluded
        public BigDecimal numBigDec = BigDecimal.ZERO;
        @GuiIncluded
        public Integer numObj;

        public TestObj() {
        }
    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public GuiSwingViewNumberSpinner.PropertyNumberSpinner create(GuiMappingContext contextProp) {
        JComponent c = spinner.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewNumberSpinner.PropertyNumberSpinner.class);
    }


    @Test
    public void testViewUpdateByte() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropByte));
        run(this::runWait);

        obj.numByte = 127;
        contextPropByte.updateSourceFromRoot();
        assertEquals((byte) 127, runGet(i::getValue),
                "listener update byte");
    }


    @Test
    public void testViewUpdateInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        run(this::runWait);

        obj.numInt = 123;
        contextPropInt.updateSourceFromRoot();
        assertEquals(123, runGet(i::getValue),
                "listener update int");
    }

    @Test
    public void testViewUpdateLong() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropLong));
        run(this::runWait);

        obj.numLong = 1234567890L;
        contextPropLong.updateSourceFromRoot();
        assertEquals(1234567890L, runGet(i::getValue),
                "listener update long");
    }

    @Test
    public void testViewUpdateDouble() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropDouble));
        run(this::runWait);

        obj.numDouble = 123456.789;
        contextPropDouble.updateSourceFromRoot();
        assertEquals(123456.789, runGet(i::getValue),
                "listener update double");
    }

    @Test
    public void testViewUpdateBigInteger() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigInt));
        run(this::runWait);

        obj.numBigInt = new BigInteger(Long.MAX_VALUE + "012345");
        contextPropBigInt.updateSourceFromRoot();
        assertEquals(new BigInteger(Long.MAX_VALUE + "012345"), runGet(i::getValue),
                "listener update bigInt");
    }

    @Test
    public void testViewUpdateBigDecimal() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigDec));
        run(this::runWait);

        obj.numBigDec = new BigDecimal(Long.MAX_VALUE + "012345.678");
        contextPropBigDec.updateSourceFromRoot();
        assertEquals(new BigDecimal(Long.MAX_VALUE + "012345.678"), runGet(i::getValue),
                "listener update bigDecimal");
    }

    @Test
    public void testViewUpdateIntObj() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropObj));
        run(this::runWait);

        obj.numObj = 123;
        contextPropObj.updateSourceFromRoot();
        assertEquals(123, runGet(i::getValue),
                "listener update num-obj");
    }


    @Test
    public void testViewUpdateIntField() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        i.setSwingViewValueWithUpdate(123456);
        //i.getEditorField().setText("123456") ; //this does not cause a change event.
        run(this::runWait);
        assertEquals(123456, obj.numInt,
                "after value set int");
        assertEquals(
                "123,456",
                i.getEditorField().getText(), "after value set, field update int");
    }


    @Test
    public void testViewUpdateBigDecField() throws Exception {
        timeFactor = 1;
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigDec));
        EditWait wait = editWait(i.getEditingRunner());
        wait.await();

        i.getEditorField().setText(Long.MAX_VALUE + "012345.678");
        i.commitEdit();
        wait.awaitNextFinish();

        assertEquals(new BigDecimal(Long.MAX_VALUE + "012345.678"), obj.numBigDec,
                "after value set bigDec");

        i.getEditorField().setText(Long.MAX_VALUE + "012345");
        i.commitEdit();
        wait.awaitNextFinish();
        assertEquals(new BigDecimal(Long.MAX_VALUE + "012345"), obj.numBigDec,
                "after value set bigDec");
    }

    @Test
    public void testViewUpdateByteField() throws Exception {
        timeFactor = 1;
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropByte));
        EditWait wait = editWait(i.getEditingRunner());
        i.getEditorField().setText("554"); //0b10_0010_1010 -> 0b0010_1010 =42
        i.commitEdit();
        wait.awaitNextFinish();
        assertEquals(((byte) 42), obj.numByte,
                "after value set byte");
        assertEquals(
                "42",
                i.getEditorField().getText(), "after value set, field update byte");
    }


    @Test
    public void testViewMaxBigInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropBigInt));
        i.getModelTyped().setMaximum(128);
        i.getModelTyped().setMinimum(100);
        assertNull(i.getModelTyped().getNextValue(BigInteger.valueOf(127)), "custom max: return null by over the max");
        assertEquals(BigInteger.valueOf(127), i.getModelTyped().getNextValue(BigInteger.valueOf(126)), "custom max: return the max (-1)");
        assertNull(i.getModelTyped().getPreviousValue(BigInteger.valueOf(100)), "custom min: return null by over the min");
        assertEquals(BigInteger.valueOf(100), i.getModelTyped().getPreviousValue(BigInteger.valueOf(101)), "custom min: return the max");
        i.getModelTyped().setStepSize(11);
        assertEquals(BigInteger.valueOf(121), i.getModelTyped().getNextValue(BigInteger.valueOf(110)), "custom step size: next");
        assertEquals(BigInteger.valueOf(100), i.getModelTyped().getPreviousValue(BigInteger.valueOf(111)), "custom step size: prev");
    }

    @Test
    public void testViewPasteAllInt() {
        withClipLock(() -> {
            GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
            setClipboardText("1234");
            GuiSwingViewNumberSpinner.NumberTextPasteAllAction a = findMenuItemAction(i.getSwingStaticMenuItems(), GuiSwingViewNumberSpinner.NumberTextPasteAllAction.class);
            a.actionPerformed(null);
            assertEquals("1,234", i.getEditorField().getText(), "after paste-all");
            assertEquals(1234, obj.numInt, "after paste-all");
        });
    }

    @Test
    public void testViewSetMaxInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberMaximumAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberMaximumAction.class,
                null, null, null, GuiSwingViewNumberSpinner.NumberMaximumAction::isMax);
        a.actionPerformed(null);
        assertEquals(Integer.MAX_VALUE, obj.numInt, "set-maximum action");
    }

    @Test
    public void testViewSetMinInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberMaximumAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberMaximumAction.class,
                null, null, null, act -> !act.isMax());
        a.actionPerformed(null);
        assertEquals(Integer.MIN_VALUE, obj.numInt, "set-minimum action");
    }

    @Test
    public void testViewIncInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberIncrementAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberIncrementAction.class,
                null, null, null, GuiSwingViewNumberSpinner.NumberIncrementAction::isInc);
        a.actionPerformed(null);
        assertEquals(1, obj.numInt, "increment action");
    }

    @Test
    public void testViewDecInt() {
        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        GuiSwingViewNumberSpinner.NumberIncrementAction a = findMenuItem(i.getSwingStaticMenuItems(),
                GuiSwingViewNumberSpinner.NumberIncrementAction.class,
                null, null, null, act -> !act.isInc());
        a.actionPerformed(null);
        assertEquals(-1, obj.numInt, "decrement action");
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
        assertTrue(hasMin, "after prefs load: minimum");

        boolean hasMax = runGet(() -> i.getSettingAction().getPane().getMaxCheckBox().isSelected());
        assertTrue(hasMax, "after prefs load: maximum");

        Object min = runGet(() -> i.getSettingAction().getPane().getMinSpinner().getValue());
        assertEquals(100, min, "after prefs load: minimum value");
        Object modelMin = runGet(() -> i.getModelTyped().getMinimum());
        assertEquals(100, modelMin, "after prefs load: minimum value");

        Object max = runGet(() -> i.getSettingAction().getPane().getMaxSpinner().getValue());
        assertEquals(1000_000, max, "after prefs load: maximum value");
        Object modelMax = runGet(() -> i.getModelTyped().getMaximum());
        assertEquals(1000_000, modelMax, "after prefs load: maximum value");

        Object step = runGet(() -> i.getSettingAction().getPane().getStepSpinner().getValue());
        assertEquals(3, step, "after prefs load: step value");
        Object modelStep = runGet(() -> i.getModelTyped().getStepSize());
        assertEquals(3, modelStep, "after prefs load: step value");

        String str = runGet(() -> i.getEditorField().getText());
        assertEquals("0500.0", str, "after prefs load: format and $value");

        Object val = runGet(i::getValue);
        assertEquals(500, val, "after prefs load: value");

        boolean hasFmt = runGet(() -> i.getSettingAction().getPane().getFormatCheckBox().isSelected());
        assertTrue(hasFmt, "after prefs load: format");
        String fmt = runGet(() -> i.getSettingAction().getPane().getFormatField().getText());
        assertEquals("0000.0", fmt, "after prefs load: format");
    }

    @Test
    public void testViewSaveSettings() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(parentPrefs);

        GuiSwingViewNumberSpinner.PropertyNumberSpinner i = runGet(() -> create(contextPropInt));
        i.setPreferencesUpdater(GuiSwingPrefsSupports.PreferencesUpdateEvent::save);

        EditWait wait = editWait(i.getEditingRunner());

        run(() -> i.getSettingAction().getPane().getMinCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getMinSpinner().setValue(123));

        run(() -> i.getSettingAction().getPane().getMaxCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getMaxSpinner().setValue(123_123_123));

        run(() -> i.getSettingAction().getPane().getStepSpinner().setValue(4));

        run(() -> i.getSettingAction().getPane().getFormatCheckBox().setSelected(true));
        run(() -> i.getSettingAction().getPane().getFormatField().setText("##.0"));

        run(() -> i.setSwingViewValueWithUpdate(123_123));

        wait.awaitNextFinish();
        run(this::runWait);

        GuiPreferences targetPrefs = parentPrefs.getDescendant(contextPropInt);
        String max = targetPrefs.getValueStore().getString("maximum", "");
        String min = targetPrefs.getValueStore().getString("minimum", "");
        String step = targetPrefs.getValueStore().getString("stepSize", "");
        String fmt = targetPrefs.getValueStore().getString("format", "");
        String val = targetPrefs.getValueStore().getString("$value", "");

        assertEquals("123123123", max, "after prefs update: maximum");
        assertEquals("123", min, "after prefs update: minimum");
        assertEquals("4", step, "after prefs update: stepSize");
        assertEquals("##.0", fmt, "after prefs update: format");
        assertEquals("123123", val, "after prefs update: $value");

        GuiPreferences savePrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        run(() -> i.saveSwingPreferences(savePrefs));

        GuiPreferences savedTarget = savePrefs.getDescendant(contextPropInt);
        max = savedTarget.getValueStore().getString("maximum", "");
        min = savedTarget.getValueStore().getString("minimum", "");
        step = savedTarget.getValueStore().getString("stepSize", "");
        fmt = savedTarget.getValueStore().getString("format", "");
        val = savedTarget.getValueStore().getString("$value", "");

        assertEquals("123123123", max, "after prefs update: maximum");
        assertEquals("123", min, "after prefs update: minimum");
        assertEquals("4", step, "after prefs update: stepSize");
        assertEquals("##.0", fmt, "after prefs update: format");
        assertEquals("123123", val, "after prefs update: $value");


    }
}
