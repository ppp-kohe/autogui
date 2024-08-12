package org.autogui.swing.util;


import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class NimbusLookAndFeelCustomFlat extends NimbusLookAndFeel {
    protected DarkLight darkLight;
    protected boolean init = false;

    public enum DarkLight {
        Dark, Light, System
    }

    public NimbusLookAndFeelCustomFlat() {
        this(DarkLight.System);
    }

    public NimbusLookAndFeelCustomFlat(DarkLight darkLight) {
        this.darkLight = darkLight;
    }


    public static class NimbusLookAndFeelCustomFlatLight extends NimbusLookAndFeelCustomFlat {
        public NimbusLookAndFeelCustomFlatLight() {
            super(DarkLight.Light);
        }
    }

    public static class NimbusLookAndFeelCustomFlatDark extends NimbusLookAndFeelCustomFlat {
        public NimbusLookAndFeelCustomFlatDark() {
            super(DarkLight.Dark);
        }
    }

    public DarkLight getDarkLight() {
        return darkLight;
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();
        if (!init) {
            initDefault(defaults, isDarkTheme());
            init = true;
        }
        return defaults;
    }

    protected boolean isDarkTheme() {
        return switch (darkLight) {
            case Dark -> true;
            case Light -> false;
            default -> UIManagerUtil.getInstance().getOsVersion().isDarkTheme();
        };
    }

    protected Color back;
    protected Color base;
    protected Color backLight;
    protected Color textSelect;
    protected Color disabledText;
    protected Color selection;
    protected Color text2;
    protected Color text;
    protected Color dark;

    protected void initDefault(UIDefaults defaults, boolean dark) {
        if (dark) {
            initDefaultDarkColor();
            initDefault(defaults);
        } else {
            initDefaultLightColor();
            initDefault(defaults);
        }
    }

    protected void initDefaultDarkColor() {
        back = new ColorUIResource(41, 41, 41);
        base = back;
        backLight = new ColorUIResource(60, 60, 60);
        textSelect = Color.white; //for list-selected-text
        disabledText = new ColorUIResource(100, 100, 100);
        selection = new ColorUIResource(70, 112, 152);
        text2 = new ColorUIResource(210, 210, 215);
        text = new ColorUIResource(220, 220, 220);
        dark = new ColorUIResource(105, 105, 105);
    }

    protected void initDefaultLightColor() {
        back = new ColorUIResource(242, 242, 242);
        base = new ColorUIResource(140, 140, 180);
        backLight = new ColorUIResource(250, 250, 250);
        textSelect = Color.white; //for list-selected-text
        disabledText = new ColorUIResource(170, 170, 170);
        selection = new ColorUIResource(80, 160, 220);
        text2 = new ColorUIResource(40, 40, 40);
        text = new ColorUIResource(50, 50, 50);
        dark = new ColorUIResource(185, 185, 185);
    }

    public void initDefault(UIDefaults defaults) {
        initKeys(defaults);
        initDefaultColors(defaults);
        initDefaultTableHeader(defaults);
        initDefaultMenu(defaults);
        initDefaultScroll(defaults);
        initDefaultButton(defaults);
        initDefaultToggleButton(defaults);
        initDefaultTabbedPane(defaults);
        initDefaultComboBox(defaults);
        initDefaultSpinner(defaults);
        initDefaultCheckBox(defaults);
        initDefaultProgressBar(defaults);
        initDefaultSlider(defaults);
        initDefaultRadioButton(defaults);
        initDefaultSplitPane(defaults);
        initDefaultToolBar(defaults);
        initDefaultTable(defaults);
        initDefaultTree(defaults);
        initDefaultList(defaults);
    }

    protected void initKeys(UIDefaults defaults) {
        if (isMetaKeyBinding()) {
            for (var keys = defaults.keys(); keys.hasMoreElements(); ) {
                var name = keys.nextElement().toString();
                if (name.endsWith(".focusInputMap")) {
                    var v = defaults.get(name);
                    if (v instanceof InputMap im) {
                        replaceKeys(name, im);
                    }
                }
            }
        }
    }

    protected void replaceKeys(String name, InputMap im) {
        replaceKey(im, "activate-link-action", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_SPACE, InputEvent.META_DOWN_MASK, KeyEvent.VK_SPACE);
        replaceKey(im, "caret-begin", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_HOME, InputEvent.META_DOWN_MASK, KeyEvent.VK_UP);
        replaceKey(im, "caret-begin-line", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A);
        replaceKey(im, "caret-end", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_END, InputEvent.META_DOWN_MASK, KeyEvent.VK_DOWN);
        replaceKey(im, "caret-end-line", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_E);
        replaceKey(im, "caret-next-word", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_RIGHT);
        replaceKey(im, "caret-previous-word", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_LEFT);
        replaceKey(im, "clearSelection", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_BACK_SLASH, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_A);
        replaceKey(im, "copy", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_C, InputEvent.META_DOWN_MASK, KeyEvent.VK_C);
        replaceKey(im, "copy-to-clipboard", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_C, InputEvent.META_DOWN_MASK, KeyEvent.VK_C);
        replaceKey(im, "cut", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_X, InputEvent.META_DOWN_MASK, KeyEvent.VK_X);
        replaceKey(im, "cut-to-clipboard", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_X, InputEvent.META_DOWN_MASK, KeyEvent.VK_X);
        replaceKey(im, "delete-next-word", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_DELETE);
        replaceKey(im, "delete-previous", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_H);
        replaceKey(im, "delete-previous-word", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_BACK_SPACE, InputEvent.ALT_DOWN_MASK, KeyEvent.VK_BACK_SPACE);
        replaceKey(im, "moveSelectionTo", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_SPACE);
        replaceKey(im, "next-link-action", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_T, InputEvent.META_DOWN_MASK, KeyEvent.VK_T);
        replaceKey(im, "paste", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_V, InputEvent.META_DOWN_MASK, KeyEvent.VK_V);
        replaceKey(im, "paste-from-clipboard", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_V, InputEvent.META_DOWN_MASK, KeyEvent.VK_V);
        replaceKey(im, "previous-link-action", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_T);
        replaceKey(im, "select-all", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, InputEvent.META_DOWN_MASK, KeyEvent.VK_A);
        replaceKey(im, "selectAll", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, InputEvent.META_DOWN_MASK, KeyEvent.VK_A);
        replaceKey(im, "selection-begin", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_UP);
        replaceKey(im, "selection-begin-line", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_LEFT);
        replaceKey(im, "selection-end", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_DOWN);
        replaceKey(im, "selection-end-line", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_RIGHT);
        replaceKey(im, "selection-next-word", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_RIGHT);
        replaceKey(im, "selection-page-left", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_PAGE_UP);
        replaceKey(im, "selection-page-right", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK, KeyEvent.VK_PAGE_DOWN);
        replaceKey(im, "selection-previous-word", InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_LEFT);
        replaceKey(im, "selectLastChangeLead", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_DOWN, InputEvent.META_DOWN_MASK, KeyEvent.VK_DOWN);
        replaceKey(im, "unselect", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_BACK_SLASH, InputEvent.META_DOWN_MASK, KeyEvent.VK_BACK_SLASH);

        replaceKey(im, "page-up", InputEvent.CTRL_DOWN_MASK,  KeyEvent.VK_U); //original binding
        replaceKey(im, "page-down", InputEvent.CTRL_DOWN_MASK,  KeyEvent.VK_V);

        if (Arrays.stream(im.keys())
                .map(im::get)
                .anyMatch(action -> Objects.equals(action, "caret-backward"))) { //text component
            replaceKey(im, "caret-backward", 0, KeyEvent.VK_KP_LEFT);
            replaceKey(im, "caret-backward", 0, KeyEvent.VK_LEFT);
            replaceKey(im, "caret-backward", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_B);
            replaceKey(im, "caret-begin-line-and-up", InputEvent.ALT_DOWN_MASK, KeyEvent.VK_UP);
            replaceKey(im, "caret-down", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_N);
            replaceKey(im, "caret-end-line-and-down", InputEvent.ALT_DOWN_MASK, KeyEvent.VK_DOWN);
            replaceKey(im, "caret-forward", 0, KeyEvent.VK_KP_RIGHT);
            replaceKey(im, "caret-forward", 0, KeyEvent.VK_RIGHT);
            replaceKey(im, "caret-forward", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_F);
            replaceKey(im, "caret-up", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_P);
            replaceKey(im, "delete-next", 0, KeyEvent.VK_DELETE);
            replaceKey(im, "delete-next", InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_D);
            replaceKey(im, "insert-tab", 0, KeyEvent.VK_TAB);
            replaceKey(im, "selection-backward", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_KP_LEFT);
            replaceKey(im, "selection-backward", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_LEFT);
            replaceKey(im, "selection-begin-paragraph", InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_KP_UP);
            replaceKey(im, "selection-begin-paragraph", InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_UP);
            replaceKey(im, "selection-down", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_DOWN);
            replaceKey(im, "selection-down", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_KP_DOWN);
            replaceKey(im, "selection-end-paragraph", InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_DOWN);
            replaceKey(im, "selection-end-paragraph", InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, KeyEvent.VK_KP_DOWN);
            replaceKey(im, "selection-forward", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_KP_RIGHT);
            replaceKey(im, "selection-forward", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_RIGHT);
            replaceKey(im, "selection-page-down", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_PAGE_DOWN);
            replaceKey(im, "selection-page-up", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_PAGE_UP);
            replaceKey(im, "selection-up", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_KP_UP);
            replaceKey(im, "selection-up", InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UP);
        }
        doReplaceAll();
    }

    protected List<Runnable> lazyReplaceTasks = new ArrayList<>();

    protected void replaceKey(InputMap im, String action, int newMod, int newKeyCode) {
        lazyReplaceTasks.add(() -> im.put(KeyStroke.getKeyStroke(newKeyCode, newMod), action));
    }

    protected void replaceKey(InputMap im, String action, int oldMod, int keyCode, int newMod, int newKeyCode) {
        var key = KeyStroke.getKeyStroke(keyCode, oldMod);
        var exAction = im.get(key);
        if (Objects.equals(exAction, action)) {
            lazyReplaceTasks.add(() -> {
                var currentAction = im.get(key);
                if (Objects.equals(currentAction, action)) { //it might already be overwritten by another task
                    im.remove(key);
                }
                im.put(KeyStroke.getKeyStroke(newKeyCode, newMod), action);
            });
        }
    }

    protected void doReplaceAll() {
        lazyReplaceTasks.forEach(Runnable::run);
        lazyReplaceTasks.clear();
    }

    protected boolean isMetaKeyBinding() {
        return UIManagerUtil.getInstance().getOsVersion().isMacOS();
    }

    protected void initDefaultColors(UIDefaults defaults) {
        defaults.put("nimbusBase", base);
        defaults.put("control", back);
        defaults.put("nimbusSelectionBackground", selection);
        defaults.put("nimbusSelection", selection);
        defaults.put("nimbusLightBackground", backLight);
        defaults.put("nimbusSelectedText", textSelect);
        defaults.put("nimbusDisabledText", disabledText);
        defaults.put("text", text);
        defaults.put("textForeground", text);
        defaults.put("background", back);

        defaults.put("nimbusBlueGrey", dark);
        defaults.put("nimbusBorder", back);

        var font = defaults.getFont("defaultFont");
        defaults.put("defaultFont", font.deriveFont((float) (font.getSize2D() * 1.08)));
        defaults.put("info", back);
        defaults.put("nimbusInfoBlue", back);

        defaults.put(LafProp.of("ColorChooser",         "swatchesDefaultRecentColor").toString(), text);
    }

    protected void initDefaultMenu(UIDefaults defaults) {
        defaults.put(LafProp.of("MenuBar", "Menu", "contentMargins").toString(), new Insets(5, 15, 5, 15));

        var propTextFore = LafProp.name("textForeground");
        var propTextForeEnabled = propTextFore.withStates(LafState.Enabled);
        var propBackPaint = LafProp.name("backgroundPainter");
        for (String itemHead : List.of("CheckBoxMenuItem", "RadioButtonMenuItem", "MenuItem", "Menu")) {
            defaults.put(LafProp.of(itemHead, "contentMargins").toString(), new Insets(3, 12, 3, 15));

            defaults.put(propTextForeEnabled.withHead(itemHead).toString(), text2); //MenuItem[Enabled].textForeground: menu text-color
            //Note: settings other text will break menu text colors

            var selectedHighlight = "Menu".equals(itemHead);
            defaults.put(propBackPaint.withHead(itemHead).toString(),                                        new MenuItemPainter(false, false));
            defaults.put(propBackPaint.withHead(itemHead, LafState.Enabled).toString(),                      new MenuItemPainter(false, false));
            defaults.put(propBackPaint.withHead(itemHead, LafState.Selected).toString(),                     new MenuItemPainter(selectedHighlight, false));
            defaults.put(propBackPaint.withHead(itemHead, LafState.Enabled, LafState.Selected).toString(),   new MenuItemPainter(selectedHighlight, false));
            defaults.put(propBackPaint.withHead(itemHead, LafState.MouseOver).toString(),                    new MenuItemPainter(true, false));
            defaults.put(propBackPaint.withHead(itemHead, LafState.Selected, LafState.MouseOver).toString(), new MenuItemPainter(true, false));
        }
        defaults.put(propTextForeEnabled.withHead("MenuBar", "Menu").toString(), text); //MenuBar:Menu[Enabled].textForeground: menu-bar menu text-color

        defaults.put(propBackPaint.withHead("MenuBar", "Menu", LafState.Selected).toString(),  new MenuItemPainter(true, true));
        defaults.put(propBackPaint.withHead("MenuBar", LafState.Enabled).toString(),                   new MenuItemPainter(false, true));

        //menu background
        defaults.put(propBackPaint.withHead("PopupMenu", LafState.Enabled).toString(),      new PopupMenuPainter());

        var checkCheck = LafProp.of("CheckBoxMenuItem", "checkIconPainter");
        defaults.put(checkCheck.withStates(LafState.Enabled, LafState.Selected).toString(),       new CheckBoxMenuItemPainter( false, true, false));
        defaults.put(checkCheck.withStates(LafState.MouseOver, LafState.Selected).toString(),     new CheckBoxMenuItemPainter( false, true, true));
        defaults.put(checkCheck.withStates(LafState.Disabled, LafState.Selected).toString(),      new CheckBoxMenuItemPainter( true, true, false));

        var radioCheck = LafProp.of("RadioButtonMenuItem", "checkIconPainter");
        defaults.put(radioCheck.withStates(LafState.Enabled, LafState.Selected).toString(),       new CheckBoxMenuItemPainter(false, true, false));
        defaults.put(radioCheck.withStates(LafState.MouseOver, LafState.Selected).toString(),     new CheckBoxMenuItemPainter(false, true, true));
        defaults.put(radioCheck.withStates(LafState.Disabled, LafState.Selected).toString(),      new CheckBoxMenuItemPainter(true, true, false));

        var arrowPainter = LafProp.of("Menu", "arrowIconPainter");
        defaults.put(arrowPainter.withStates(LafState.Disabled).toString(),                       new ArrowPainter(false, false));
        defaults.put(arrowPainter.withStates(LafState.Enabled, LafState.Selected).toString(),     new ArrowPainter(false, true));
        defaults.put(arrowPainter.withStates(LafState.Enabled).toString(),                        new ArrowPainter(false, false));
    }

    protected void initDefaultScroll(UIDefaults defaults) {
        defaults.put(LafProp.of("ScrollBar", "minimumThumbSize").toString(), new Dimension(16, 16));
        var button = LafProp.head("ScrollBar", "ScrollBar.button");
        defaults.put(button.withName("size").toString(), 10);
        defaults.put(LafProp.of("ScrollBar", "thumbHeight").toString(), 10);
        var trackPainter = LafProp.of("ScrollBar", "ScrollBarTrack", "backgroundPainter");
        defaults.put(trackPainter.withStates(LafState.Enabled).toString(),   new EmptyPainter());
        defaults.put(trackPainter.withStates(LafState.Disabled).toString(),  new EmptyPainter());

        var thumbPainter = LafProp.of("ScrollBar", "ScrollBarThumb", "backgroundPainter");
        defaults.put(thumbPainter.withStates(LafState.Enabled).toString(),   new ScrollBarThumbPainter());
        defaults.put(thumbPainter.withStates(LafState.MouseOver).toString(), new ScrollBarThumbPainter());
        defaults.put(thumbPainter.withStates(LafState.Pressed).toString(),   new ScrollBarThumbPainter());

        var buttonPainter = button.withName("foregroundPainter");
        defaults.put(buttonPainter.withStates(LafState.Enabled).toString(),  new ScrollBarButtonPainter(false));
        defaults.put(buttonPainter.withStates(LafState.Pressed).toString(),  new ScrollBarButtonPainter(true));
    }

    protected void initDefaultButton(UIDefaults defaults) {
        var button = LafProp.of("Button", "backgroundPainter");
        initDefaultButton(defaults, button);
    }

    protected void initDefaultButton(UIDefaults defaults, LafProp button) {
        defaults.put(button.withStates(LafState.Default, LafState.Focused, LafState.MouseOver).toString(),  new ButtonPainter(false, true, false, false));
        defaults.put(button.withStates(LafState.Default, LafState.Focused, LafState.Pressed).toString(),    new ButtonPainter(true, true, false, false));
        defaults.put(button.withStates(LafState.Default, LafState.Focused).toString(),                      new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Default, LafState.MouseOver).toString(),                    new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Default, LafState.Pressed).toString(),                      new ButtonPainter(true, false, false, false));
        defaults.put(button.withStates(LafState.Default).toString(),                                        new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Disabled).toString(),                                       new ButtonPainter(false, false, true, false));
        defaults.put(button.withStates(LafState.Enabled).toString(),                                        new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Focused, LafState.MouseOver).toString(),                    new ButtonPainter(false, true, false, false));
        defaults.put(button.withStates(LafState.Focused, LafState.Pressed).toString(),                      new ButtonPainter(true, true, false, false));
        defaults.put(button.withStates(LafState.Focused).toString(),                                        new ButtonPainter(false, true, false, false));
        defaults.put(button.withStates(LafState.MouseOver).toString(),                                      new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Pressed).toString(),                                        new ButtonPainter(true, false, false, false));
    }

    protected void initDefaultToggleButton(UIDefaults defaults) {
        initDefaultToggleButton(defaults, LafProp.of("ToggleButton", "backgroundPainter"));
    }

    protected void initDefaultToggleButton(UIDefaults defaults, LafProp button) {
        defaults.put(button.withStates(LafState.Selected, LafState.Focused, LafState.MouseOver).toString(), new ButtonPainter(false, true, false, true));
        defaults.put(button.withStates(LafState.Selected, LafState.Focused, LafState.Pressed).toString(),   new ButtonPainter(true, true, false, true));
        defaults.put(button.withStates(LafState.Selected, LafState.Focused).toString(),                     new ButtonPainter(false, false, false, true));
        defaults.put(button.withStates(LafState.Selected, LafState.MouseOver).toString(),                   new ButtonPainter(false, false, false, true));
        defaults.put(button.withStates(LafState.Selected, LafState.Pressed).toString(),                     new ButtonPainter(true, false, false, true));
        defaults.put(button.withStates(LafState.Selected).toString(),                                       new ButtonPainter(false, false, false, true));
        defaults.put(button.withStates(LafState.Disabled).toString(),                                       new ButtonPainter(false, false, true, false));
        defaults.put(button.withStates(LafState.Selected, LafState.Disabled).toString(),                    new ButtonPainter(false, false, true, true));
        defaults.put(button.withStates(LafState.Enabled).toString(),                                        new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Focused, LafState.MouseOver).toString(),                    new ButtonPainter(false, true, false, false));
        defaults.put(button.withStates(LafState.Focused, LafState.Pressed).toString(),                      new ButtonPainter(true, true, false, false));
        defaults.put(button.withStates(LafState.Focused).toString(),                                        new ButtonPainter(false, true, false, false));
        defaults.put(button.withStates(LafState.MouseOver).toString(),                                      new ButtonPainter(false, false, false, false));
        defaults.put(button.withStates(LafState.Pressed).toString(),                                        new ButtonPainter(true, false, false, false));
    }

    protected void initDefaultTabbedPane(UIDefaults defaults) {
        var tab = LafProp.head("TabbedPane", "TabbedPaneTab");
        defaults.put(tab.withName("contentMargins").toString(), new Insets(5, 20, 3, 20));
        var tabArea = LafProp.head("TabbedPane", "TabbedPaneTabArea");
        defaults.put(tabArea.withName("contentMargins").toString(), new Insets(5, 20, 3, 20));

        var propTextFore = tab.withName("textForeground");
        defaults.put(propTextFore.withStates(LafState.Focused, LafState.Pressed, LafState.Selected).toString(), text);
        defaults.put(propTextFore.withStates(LafState.Pressed, LafState.Selected).toString(), textSelect);

        var backPainter = tab.withName("backgroundPainter");
        defaults.put(backPainter.withStates(LafState.Selected).toString(),                                          new TabbedPanePainter(false, false, false, true));

        defaults.put(backPainter.withStates(LafState.Enabled).toString(),                                           new TabbedPanePainter(false, false, false, false));
        defaults.put(backPainter.withStates(LafState.Enabled, LafState.MouseOver).toString(),                       new TabbedPanePainter(false, false, false, false));
        defaults.put(backPainter.withStates(LafState.Enabled, LafState.Pressed).toString(),                         new TabbedPanePainter(true, false, true, false));

        defaults.put(backPainter.withStates(LafState.Focused).toString(),                                           new TabbedPanePainter(false, true, false, false));
        defaults.put(backPainter.withStates(LafState.Focused, LafState.Selected).toString(),                        new TabbedPanePainter(false, true, false, true));
        defaults.put(backPainter.withStates(LafState.Focused, LafState.Pressed, LafState.Selected).toString(),      new TabbedPanePainter(true, true, false, true));
        defaults.put(backPainter.withStates(LafState.Focused, LafState.MouseOver, LafState.Selected).toString(),    new TabbedPanePainter(false, true, false, true));

        defaults.put(backPainter.withStates(LafState.Selected, LafState.MouseOver).toString(),                      new TabbedPanePainter(false, false, false, true));
        defaults.put(backPainter.withStates(LafState.Pressed, LafState.Selected).toString(),                        new TabbedPanePainter(true, false, false, true));

        defaults.put(backPainter.withStates(LafState.Disabled).toString(),                                          new TabbedPanePainter(false, false, true, false));
        defaults.put(backPainter.withStates(LafState.Disabled, LafState.Selected).toString(),                       new TabbedPanePainter(false, false, true, true));

        var tabAreaPainter = LafProp.of("TabbedPane", "TabbedPaneTabArea", "backgroundPainter");
        defaults.put(tabAreaPainter.withStates(LafState.Disabled).toString(), new EmptyPainter());
        defaults.put(tabAreaPainter.withStates(LafState.Enabled, LafState.MouseOver).toString(), new EmptyPainter());
        defaults.put(tabAreaPainter.withStates(LafState.Enabled, LafState.Pressed).toString(), new EmptyPainter());
        defaults.put(tabAreaPainter.withStates(LafState.Enabled).toString(), new EmptyPainter());
    }

    protected void initDefaultComboBox(UIDefaults defaults) {
        var arrowBtn = LafProp.head("ComboBox", "ComboBox.arrowButton");
        var arrowBtnPainter = arrowBtn.withName("backgroundPainter");
        defaults.put(arrowBtnPainter.withStates(LafState.Disabled, LafState.Editable).toString(),   new ComboBoxPainter(false, false, true, false, true));
        defaults.put(arrowBtnPainter.withStates(LafState.Enabled, LafState.Editable).toString(),    new ComboBoxPainter(false, false, false, false, true));
        defaults.put(arrowBtnPainter.withStates(LafState.MouseOver, LafState.Editable).toString(),  new ComboBoxPainter(false, false, false, false, true));
        defaults.put(arrowBtnPainter.withStates(LafState.Pressed, LafState.Editable).toString(),    new ComboBoxPainter(true, false, false, false, true));
        defaults.put(arrowBtnPainter.withStates(LafState.Selected, LafState.Editable).toString(),   new ComboBoxPainter(false, false, false, true, true));

        var combo = LafProp.head("ComboBox");
        var comboPainter = combo.withName("backgroundPainter");
        defaults.put(comboPainter.withStates(LafState.Disabled, LafState.Editable).toString(),   new ComboBoxPainter(false, false, true, false, false));
        defaults.put(comboPainter.withStates(LafState.Disabled, LafState.Pressed).toString(),    new ComboBoxPainter(true, false, true, false, false));
        defaults.put(comboPainter.withStates(LafState.Disabled).toString(),                      new ComboBoxPainter(false, false, true, false, false));
        defaults.put(comboPainter.withStates(LafState.Enabled, LafState.Selected).toString(),    new ComboBoxPainter(false, false, false, true, false));
        defaults.put(comboPainter.withStates(LafState.Enabled).toString(),                       new ComboBoxPainter(false, false, false, false, false));
        defaults.put(comboPainter.withStates(LafState.MouseOver, LafState.Focused).toString(),   new ComboBoxPainter(false, true, false, false, false));
        defaults.put(comboPainter.withStates(LafState.Pressed, LafState.Focused).toString(),     new ComboBoxPainter(true, true, false, false, false));
        defaults.put(comboPainter.withStates(LafState.Focused).toString(),                       new ComboBoxPainter(false, true, false, false, false));
        defaults.put(comboPainter.withStates(LafState.MouseOver).toString(),                     new ComboBoxPainter(false, false, false, false, false));
        defaults.put(comboPainter.withStates(LafState.Pressed).toString(),                       new ComboBoxPainter(true, false, false, true, false));
    }

    protected void initDefaultSpinner(UIDefaults defaults) {
        var spinnerNext = LafProp.of("Spinner", "Spinner.nextButton", "backgroundPainter");
        var spinnerPrev = LafProp.of("Spinner", "Spinner.previousButton", "backgroundPainter");

        defaults.put(spinnerNext.withStates(LafState.Disabled).toString(),                      new SpinnerPainter(false, false, true, false, true));
        defaults.put(spinnerNext.withStates(LafState.Enabled).toString(),                       new SpinnerPainter(false, false, false, false, true));
        defaults.put(spinnerNext.withStates(LafState.Focused, LafState.MouseOver).toString(),   new SpinnerPainter(false, true, false, false, true));
        defaults.put(spinnerNext.withStates(LafState.Focused, LafState.Pressed).toString(),     new SpinnerPainter(true, true, false, false, true));
        defaults.put(spinnerNext.withStates(LafState.Focused).toString(),                       new SpinnerPainter(false, true, false, false, true));
        defaults.put(spinnerNext.withStates(LafState.MouseOver).toString(),                     new SpinnerPainter(false, false, false, false, true));
        defaults.put(spinnerNext.withStates(LafState.Pressed).toString(),                       new SpinnerPainter(true, false, false, false, true));

        defaults.put(spinnerPrev.withStates(LafState.Disabled).toString(),                      new SpinnerPainter(false, false, true, false, false));
        defaults.put(spinnerPrev.withStates(LafState.Enabled).toString(),                       new SpinnerPainter(false, false, false, false, false));
        defaults.put(spinnerPrev.withStates(LafState.Focused, LafState.MouseOver).toString(),   new SpinnerPainter(false, true, false, false, false));
        defaults.put(spinnerPrev.withStates(LafState.Focused, LafState.Pressed).toString(),     new SpinnerPainter(true, true, false, false, false));
        defaults.put(spinnerPrev.withStates(LafState.Focused).toString(),                       new SpinnerPainter(false, true, false, false, false));
        defaults.put(spinnerPrev.withStates(LafState.MouseOver).toString(),                     new SpinnerPainter(false, false, false, false, false));
        defaults.put(spinnerPrev.withStates(LafState.Pressed).toString(),                       new SpinnerPainter(true, false, false, false, false));
    }

    protected void initDefaultCheckBox(UIDefaults defaults) {
        var checkBoxPainter = LafProp.of("CheckBox", "iconPainter");
        defaults.put(checkBoxPainter.withStates(LafState.Disabled, LafState.Selected).toString(),                       new CheckBoxPainter(false, false, true, true));
        defaults.put(checkBoxPainter.withStates(LafState.Disabled).toString(),                                          new CheckBoxPainter(false, false, true, false));
        defaults.put(checkBoxPainter.withStates(LafState.Enabled).toString(),                                           new CheckBoxPainter(false, false, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.Focused, LafState.MouseOver, LafState.Selected).toString(),    new CheckBoxPainter(false, true, false, true));
        defaults.put(checkBoxPainter.withStates(LafState.Focused, LafState.MouseOver).toString(),                       new CheckBoxPainter(false, true, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.Focused, LafState.Pressed, LafState.Selected).toString(),      new CheckBoxPainter(true, true, false, true));
        defaults.put(checkBoxPainter.withStates(LafState.Focused, LafState.Pressed).toString(),                         new CheckBoxPainter(true, true, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.Focused, LafState.Selected).toString(),                        new CheckBoxPainter(false, true, false, true));
        defaults.put(checkBoxPainter.withStates(LafState.Focused).toString(),                                           new CheckBoxPainter(false, true, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.MouseOver, LafState.Selected).toString(),                      new CheckBoxPainter(false, false, false, true));
        defaults.put(checkBoxPainter.withStates(LafState.MouseOver).toString(),                                         new CheckBoxPainter(false, false, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.Pressed, LafState.Selected).toString(),                        new CheckBoxPainter(true, false, false, true));
        defaults.put(checkBoxPainter.withStates(LafState.Pressed).toString(),                                           new CheckBoxPainter(true, false, false, false));
        defaults.put(checkBoxPainter.withStates(LafState.Selected).toString(),                                          new CheckBoxPainter(false, false, false, true));
    }

    protected void initDefaultProgressBar(UIDefaults defaults) {
        var bar = LafProp.head("ProgressBar");
        var barBackPainter = bar.withName("backgroundPainter");
        var barFrontPainter = bar.withName("foregroundPainter");

        defaults.put(barFrontPainter.withStates(LafState.Disabled, LafState.Finished).toString(),           new ProgressBarPainter(true, true, true, false));
        defaults.put(barFrontPainter.withStates(LafState.Disabled, LafState.Indeterminate).toString(),      new ProgressBarPainter(true, true, true, true));
        defaults.put(barBackPainter.withStates(LafState.Disabled).toString(),                               new ProgressBarPainter(true, false, true, false));
        defaults.put(barFrontPainter.withStates(LafState.Disabled).toString(),                              new ProgressBarPainter(true, true, true, false));
        defaults.put(barFrontPainter.withStates(LafState.Disabled).toString(),                              new ProgressBarPainter(true, true, true, false));
        defaults.put(barFrontPainter.withStates(LafState.Enabled, LafState.Finished).toString(),            new ProgressBarPainter(false, true, true, false));
        defaults.put(barFrontPainter.withStates(LafState.Enabled, LafState.Indeterminate).toString(),       new ProgressBarPainter(false, true, false, true));
        defaults.put(barBackPainter.withStates(LafState.Enabled).toString(),                                new ProgressBarPainter(false, false, false, false));
        defaults.put(barFrontPainter.withStates(LafState.Enabled).toString(),                               new ProgressBarPainter(false, true, false, false));
    }

    protected void initDefaultSlider(UIDefaults defaults) {
        var slider = LafProp.of("Slider", "SliderThumb", "backgroundPainter");

        defaults.put(LafProp.of("Slider", "tickColor").toString(), dark);

        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Disabled).toString(),                       new SliderPainter(false, false, true, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Enabled).toString(),                        new SliderPainter(false, false, false, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Focused, LafState.MouseOver).toString(),    new SliderPainter(false, true, false, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Focused, LafState.Pressed).toString(),      new SliderPainter(true, true, false, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Focused).toString(),                        new SliderPainter(false, true, false, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.MouseOver).toString(),                      new SliderPainter(false, false, false, true));
        defaults.put(slider.withStates(LafState.ArrowShape, LafState.Pressed).toString(),                        new SliderPainter(true, false, false, true));

        defaults.put(slider.withStates(LafState.Disabled).toString(),                                            new SliderPainter(false, false, true, false));
        defaults.put(slider.withStates(LafState.Enabled).toString(),                                             new SliderPainter(false, false, false, false));
        defaults.put(slider.withStates(LafState.Focused, LafState.MouseOver).toString(),                         new SliderPainter(false, true, false, false));
        defaults.put(slider.withStates(LafState.Focused, LafState.Pressed).toString(),                           new SliderPainter(true, true, false, false));
        defaults.put(slider.withStates(LafState.Focused).toString(),                                             new SliderPainter(false, true, false, false));
        defaults.put(slider.withStates(LafState.MouseOver).toString(),                                           new SliderPainter(false, false, false, false));
        defaults.put(slider.withStates(LafState.Pressed).toString(),                                             new SliderPainter(true, false, false, false));
    }

    protected void initDefaultRadioButton(UIDefaults defaults) {
        var radio = LafProp.of("RadioButton", "iconPainter");

        defaults.put(radio.withStates(LafState.Disabled, LafState.Selected).toString(),                       new RadioButtonPainter(false, false, true, true));
        defaults.put(radio.withStates(LafState.Disabled).toString(),                                          new RadioButtonPainter(false, false, true, false));
        defaults.put(radio.withStates(LafState.Enabled).toString(),                                           new RadioButtonPainter(false, false, false, false));
        defaults.put(radio.withStates(LafState.Focused, LafState.MouseOver, LafState.Selected).toString(),    new RadioButtonPainter(false, true, false, true));
        defaults.put(radio.withStates(LafState.Focused, LafState.MouseOver).toString(),                       new RadioButtonPainter(false, true, false, false));
        defaults.put(radio.withStates(LafState.Focused, LafState.Pressed, LafState.Selected).toString(),      new RadioButtonPainter(true, true, false, true));
        defaults.put(radio.withStates(LafState.Focused, LafState.Pressed).toString(),                         new RadioButtonPainter(true, true, false, false));
        defaults.put(radio.withStates(LafState.Focused, LafState.Selected).toString(),                        new RadioButtonPainter(false, true, false, true));
        defaults.put(radio.withStates(LafState.Focused).toString(),                                           new RadioButtonPainter(false, true, false, false));
        defaults.put(radio.withStates(LafState.MouseOver, LafState.Selected).toString(),                      new RadioButtonPainter(false, false, false, true));
        defaults.put(radio.withStates(LafState.MouseOver).toString(),                                         new RadioButtonPainter(false, false, false, false));
        defaults.put(radio.withStates(LafState.Pressed, LafState.Selected).toString(),                        new RadioButtonPainter(true, false, false, true));
        defaults.put(radio.withStates(LafState.Pressed).toString(),                                           new RadioButtonPainter(true, false, false, false));
        defaults.put(radio.withStates(LafState.Selected).toString(),                                          new RadioButtonPainter(false, false, false, true));
    }

    protected void initDefaultSplitPane(UIDefaults defaults) {
        var splitBackPainter = LafProp.of("SplitPane", "SplitPaneDivider", "backgroundPainter");
        defaults.put(splitBackPainter.withStates(LafState.Enabled).toString(),                      new SplitPainter(false));
        defaults.put(splitBackPainter.withStates(LafState.Enabled, LafState.Focused).toString(),    new SplitPainter(true));
    }

    protected void initDefaultToolBar(UIDefaults defaults) {
        var btnPainter = LafProp.of("ToolBar", "Button", "backgroundPainter");
        var togglePainter = LafProp.of("ToolBar", "ToggleButton", "backgroundPainter");

        initDefaultButton(defaults, btnPainter);
        initDefaultToggleButton(defaults, togglePainter);
    }

    protected void initDefaultTableHeader(UIDefaults defaults) {
        defaults.put(LafProp.of("TableHeader", "opaque").toString(), false);
        defaults.put(LafProp.of("TableHeader", "foreground").toString(), Color.blue);
        var tableHeaderBackPainter = LafProp.of("TableHeader", "TableHeader.renderer", "backgroundPainter");
        defaults.put(tableHeaderBackPainter.withStates(LafState.Disabled).toString(),                                    new TableHeaderPainter(false, false, true, false));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Enabled).toString(),                                     new TableHeaderPainter(false, false, false, false));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Enabled, LafState.Focused).toString(),                   new TableHeaderPainter(false, true, false, false));
        defaults.put(tableHeaderBackPainter.withStates(LafState.MouseOver).toString(),                                   new TableHeaderPainter(false, false, false, false));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Pressed).toString(),                                     new TableHeaderPainter(true, false, false, false));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Enabled, LafState.Sorted).toString(),                    new TableHeaderPainter(false, false, false, true));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Enabled, LafState.Focused, LafState.Sorted).toString(),  new TableHeaderPainter(false, true, false, true));
        defaults.put(tableHeaderBackPainter.withStates(LafState.Disabled, LafState.Sorted).toString(),                   new TableHeaderPainter(false, false, false, true));
    }

    protected void initDefaultTable(UIDefaults defaults) {
        var tableText = LafProp.of("Table", "textForeground");
        defaults.put(tableText.toString(), text);
        defaults.put(tableText.withStates(LafState.Enabled, LafState.Selected).toString(), textSelect);
    }

    protected void initDefaultList(UIDefaults defaults) {
        var listText = LafProp.of("List", "textForeground");
        defaults.put(listText.toString(), text);
        defaults.put(listText.withStates(LafState.Selected).toString(), textSelect);
        defaults.put(listText.withStates(LafState.Disabled, LafState.Selected).toString(), disabledText);
        defaults.put(listText.withStates(LafState.Disabled).toString(), disabledText);
    }

    protected void initDefaultTree(UIDefaults defaults) {
        var treeCellText = LafProp.of("Tree", "TreeCell", "textForeground");
        defaults.put(treeCellText.withStates(LafState.Enabled, LafState.Selected).toString(), textSelect);
        defaults.put(treeCellText.withStates(LafState.Focused, LafState.Selected).toString(), textSelect);
    }

    public enum LafState {
        ArrowShape, Default, Disabled, Editable, Enabled, Finished, Focused, Indeterminate, MouseOver, Pressed, Selected, Sorted
    }

    public record LafProp(String head, String subItem, String name, EnumSet<LafState> state) {
        public static LafProp of(String head, String subItem, String name, LafState... state) {
            return new LafProp(head, subItem, name, state.length == 0 ? EnumSet.noneOf(LafState.class) : EnumSet.copyOf(List.of(state)));
        }
        public static LafProp of(String head, String name, LafState... state) {
            return of(head, null, name, state);
        }

        public static LafProp of(String head, String subItem, String name, EnumSet<LafState> state) {
            return new LafProp(head, subItem, name, state);
        }

        public static LafProp name(String name) {
            return of(null, null, name);
        }

        public static LafProp head(String head) {
            return new LafProp(head, null, null, EnumSet.noneOf(LafState.class));
        }

        public static LafProp head(String head, String subItem) {
            return new LafProp(head, subItem, null, EnumSet.noneOf(LafState.class));
        }

        public LafProp withName(String name, LafState... state) {
            return of(head, subItem, name, merge(state));
        }

        public LafProp withHead(String head, String subItem, LafState... state) {
            return of(head, subItem, name, merge(state));
        }

        public LafProp withHead(String head, LafState... state) {
            return of(head, subItem, name, merge(state));
        }

        public LafProp withStates(LafState... state) {
            return of(head, subItem, name, merge(state));
        }

        EnumSet<LafState> merge(LafState... state) {
            var s = new HashSet<>(this.state);
            s.addAll(Arrays.asList(state));
            return s.isEmpty() ? EnumSet.noneOf(LafState.class) : EnumSet.copyOf(s);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            if (head != null) {
                buf.append(head);
            }
            if (subItem != null) {
                buf.append(":");
                if (subItem.contains(".")) {
                    buf.append("\"").append(subItem).append("\"");
                } else {
                    buf.append(subItem);
                }
            }
            if (!state.isEmpty()) {
                buf.append(state.stream()
                        .sorted()
                        .map(Enum::name)
                        .collect(Collectors.joining("+", "[", "]")));
            }
            if (!buf.isEmpty()) {
                buf.append(".");
            }
            buf.append(name);
            return buf.toString();
        }
    }


    public static abstract class BasePainter extends AbstractRegionPainter {
        protected PaintContext context;
        public BasePainter(int insetsWidth, int insetsHeight, int canvasWidth, int canvasHeight) {
            context = new PaintContext(new Insets(insetsHeight, insetsWidth, insetsHeight, insetsWidth),
                    new Dimension(canvasWidth, canvasHeight), false);
        }

        @Override
        protected PaintContext getPaintContext() {
            return context;
        }

        protected Color[] gradientColors() {
            var fillName = "text";
            return new Color[] {
                    decodeColor(fillName, 0, 0, -0.3f, 0),
                    decodeColor(fillName, 0, 0, 0.2f, -255) };
        }

        protected Paint gradientVertical(float y, float h, Color[] colors2) {
            return new LinearGradientPaint(0, y, 0, y + h,
                    new float[]{0.1f, 0.5f, 1f}, new Color[]{colors2[1], colors2[0], colors2[1]});
        }

        protected void drawWithFocusStroke(Graphics2D g, Shape shape) {
            var back = g.getStroke();
            g.setStroke(new BasicStroke(2f));
            g.draw(shape);
            g.setStroke(back);
        }
    }

    public static class EmptyPainter extends BasePainter {
        public EmptyPainter() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {}
    }

    public static class ScrollBarThumbPainter extends BasePainter {
        protected Color colorFill;
        @SuppressWarnings("this-escape")
        public ScrollBarThumbPainter() {
            super(15, 0, 15, 15);
            colorFill = decodeColor("nimbusBlueGrey", 0, 0, 0, 0);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            //colorFill = decodeColorFlip("background", 0.3f);
            float w = decodeX(3f) - decodeX(0f);
            float h = decodeY(1.9f) - decodeY(1.1f);
            RoundRectangle2D.Float r = new RoundRectangle2D.Float(
                    decodeX(0f), decodeY(1.1f), w, h,
                    Math.min(w / 2f, 5f), Math.min(h / 2f, 10f));
            g.setPaint(colorFill);
            g.fill(r);
        }
    }

    public static class ScrollBarButtonPainter extends BasePainter {
        protected Color colorFill;
        @SuppressWarnings("this-escape")
        public ScrollBarButtonPainter(boolean pressed) {
            super(0, 15, 15, 15);
            colorFill = decodeColor("nimbusBlueGrey", 0, 0, pressed ? 0.4f : 0.1f, 0);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            Path2D.Float p = new Path2D.Float();
            p.moveTo(width * 0.3f, height / 2f);
            p.lineTo(width * 0.7f, height * 0.2f);
            p.lineTo(width * 0.7f, height * 0.8f);
            p.closePath();
            g.setPaint(colorFill);
            g.fill(p);
        }
    }

    public static class ButtonPainter extends BasePainter {
        protected boolean initialized;
        protected Color colorFill;
        protected Color colorDraw;
        protected boolean pressed;
        protected boolean focused;
        protected boolean disabled;
        protected boolean selected;

        public ButtonPainter(boolean pressed, boolean focused, boolean disabled, boolean selected,
                             int insetsWidth, int insetsHeight, int canvasWidth, int canvasHeight) {
            super(insetsWidth, insetsHeight, canvasWidth, canvasHeight);
            this.pressed = pressed;
            this.focused = focused;
            this.disabled = disabled;
            this.selected = selected;
        }

        public ButtonPainter(boolean pressed, boolean focused, boolean disabled, boolean selected) {
            this(pressed, focused, disabled, selected,  7, 7, 104, 33);
        }

        protected void init() {
            if (!initialized) {
                doInit();
                initialized = true;
            }
        }

        protected void doInit() {
            var fillName = colorName(true);
            var bOffsetFill = brightnessOffset(true);
            colorFill = decodeColor(fillName, 0, 0, bOffsetFill, 0);

            var name = colorName(false);
            var bOffset = brightnessOffset(false);
            colorDraw = decodeColor(name, 0, 0, bOffset, 0);
        }

        protected String colorName(boolean fillOrDraw) {
            if (fillOrDraw) {
                return pressed ? "nimbusBlueGrey" : "background";
            } else {
                return focused ? "nimbusFocus" : "background";
            }
        }

        protected float brightnessOffset(boolean fillOrDraw) {
            if (fillOrDraw) {
                return (pressed ? 0f : 0.2f) + (selected ? -0.3f : 0f);
            } else {
                return (focused ? 0f : -0.4f) + (disabled ? 0.2f : 0.0f);
            }
        }

        protected void draw(Graphics2D g, Shape r) {
            if (focused) {
                drawWithFocusStroke(g, r);
            } else {
                g.draw(r);
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0.4f);
            float y = decodeY(0.3f);
            float w = decodeX(2.6f) - x;
            float h = decodeY(2.7f) - y;
            float rw = decodeX(3) - decodeX(2);
            float rh = decodeY(3) - decodeY(2);
            RoundRectangle2D.Float r = new RoundRectangle2D.Float(x, y, w, h, rw, rh);
            g.setPaint(colorFill);
            g.fill(r);
            g.setPaint(colorDraw);
            draw(g, r);
        }
    }

    public static class MenuItemPainter extends ButtonPainter {
        protected boolean menuBar;
        public MenuItemPainter(boolean selected, boolean menuBar) {
            super(false, false, false, selected, 1, 1, 9, 10);
            this.menuBar = menuBar;
        }

        @Override
        protected String colorName(boolean fillOrDraw) {
            if (fillOrDraw && selected) {
                return "nimbusSelectionBackground";
            } else {
                return super.colorName(fillOrDraw);
            }
        }

        @Override
        protected float brightnessOffset(boolean fillOrDraw) {
            if (fillOrDraw && selected) {
                return 0;
            } else if (fillOrDraw) {
                return menuBar ? 0f : -0.05f;
            } else {
                return super.brightnessOffset(fillOrDraw);
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            g.setPaint(colorFill);
            Shape r;
            float x = decodeX(0f) + 2;
            float y = decodeY(0f);
            float w = decodeX(2f) - x - 1;
            float h = decodeY(3f) - y;
            float rw = Math.max(decodeX(3) - decodeX(2), 5f);
            float rh = Math.max(decodeY(3) - decodeY(2), 5f);
            if (selected) {
                r = new RoundRectangle2D.Float(x, y, w, h, rw, rh);
            } else {
                r = new Rectangle2D.Float(x, y, w, h);
            }
            g.fill(r);
        }
    }

    public static class PopupMenuPainter extends ButtonPainter {
        public PopupMenuPainter() {
            super(false, false, false, false, 5, 5, 9, 10);
        }

        @Override
        protected String colorName(boolean fillOrDraw) {
            return "background";
        }

        @Override
        protected float brightnessOffset(boolean fillOrDraw) {
            if (fillOrDraw) {
                return -0.05f;
            } else {
                return -0.1f;
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0.0f) + 1;
            float y = decodeY(0.0f) + 1;
            float w = decodeX(3f) - x - 2;
            float h = decodeY(3f) - y - 2;
            Rectangle2D.Float r = new Rectangle2D.Float(x, y, w, h);
            g.setPaint(colorFill);
            g.fill(r);
            g.setPaint(colorDraw);
            draw(g, r);
        }
    }

    public static class TabbedPanePainter extends ButtonPainter {
        protected Color[] colorGradient;
        protected Color colorSelectedFill;

        public TabbedPanePainter(boolean pressed, boolean focused, boolean disabled, boolean selected) {
            super(pressed, focused, disabled, selected, 7, 7, 44, 20);
        }

        @Override
        protected void doInit() {
            colorGradient = gradientColors();

            var selFillName = colorName(true);
            var selBOffsetFill = (pressed ? 0f : 0.2f);
            colorSelectedFill = decodeColor(selFillName, 0, 0, selBOffsetFill, 0);
            var name = colorName(false);
            var bOffset = brightnessOffset(false);
            colorDraw = decodeColor(name, 0, 0, bOffset, 0);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float y = decodeY(0.3f);
            float h = decodeY(2.7f) - y;
            float x = decodeX(0.0f);
            float w = decodeX(2.88f) - x;
            Paint p = gradientVertical(y, h, colorGradient);
            Line2D.Float line = new Line2D.Float(x + w, y, x + w, y + h);
            var strokeBack = g.getStroke();
            g.setStroke(new BasicStroke(0.5f));
            g.setPaint(p);
            g.draw(line);
            if (selected) {
                float rw = decodeX(3) - decodeX(2);
                float rh = decodeY(3) - decodeY(2);
                RoundRectangle2D.Float r = new RoundRectangle2D.Float(x, y, w, h, rw, rh);
                g.setStroke(strokeBack);
                g.setPaint(colorSelectedFill);
                g.fill(r);
                g.setPaint(colorDraw);
                draw(g, r);
            }
        }
    }

    public static class ComboBoxPainter extends ButtonPainter {
        protected boolean oneSide;
        protected Color[] colorGradient;
        public ComboBoxPainter(boolean pressed, boolean focused, boolean disabled, boolean selected, boolean oneSide) {
            super(pressed, focused, disabled, selected, 7, 7, oneSide ? 24 : 104, 24);
            this.oneSide = oneSide;
        }

        @Override
        protected void doInit() {
            super.doInit();
            if (!oneSide) {
                colorGradient = gradientColors();
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            if (oneSide) {
                init();
                Path2D.Float path = new Path2D.Float();
                path.moveTo(decodeX(0.0f), decodeY(0.358f));
                path.lineTo(decodeX(2.125f), decodeY(0.358f));
                path.curveTo(decodeAnchorX(2.0f, 3.0f), decodeAnchorY(0.358f, 0.0f), decodeAnchorX(2.7f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeX(2.7f), decodeY(0.875f));
                path.lineTo(decodeX(2.7f), decodeY(2.125f));
                path.curveTo(decodeAnchorX(2.7f, 0.0f), decodeAnchorY(2.125f, 3.0f), decodeAnchorX(2.125f, 3.0f), decodeAnchorY(2.7f, 0.0f), decodeX(2.125f), decodeY(2.7f));
                path.lineTo(decodeX(0.0f), decodeY(2.7f));
                path.lineTo(decodeX(0.0f), decodeY(0.358f));
                path.closePath();
                g.setPaint(colorFill);
                g.fill(path);
                g.setPaint(colorDraw);
                draw(g, path);
            } else {
                super.doPaint(g, c, width, height, extendedCacheKeys);
                float y = decodeY(0.3f);
                var lh = decodeY(2.5f) - y;
                var p = gradientVertical(y, lh, colorGradient);
                float lx = decodeX(2.0f) - (decodeX(3.0f) - decodeX(2.0f)) * 1.6f;
                Line2D.Float l = new Line2D.Float(lx, y, lx, y + lh);
                g.setStroke(new BasicStroke(0.5f));
                g.setPaint(p);
                g.draw(l);
            }
        }
    }

    public static class SpinnerPainter extends ButtonPainter {
        protected boolean next;

        public SpinnerPainter(boolean pressed, boolean focused, boolean disabled, boolean selected, boolean next) {
            super(pressed, focused, disabled, selected);
            this.next = next;
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            Path2D.Float path = new Path2D.Float();
            if (next) {
                path.moveTo(decodeX(0.0f), decodeY(0.358f)); //top-left
                path.lineTo(decodeX(2.125f), decodeY(0.358f)); //top-right
                path.curveTo(decodeAnchorX(2.0f, 3.0f), decodeAnchorY(0.358f, 0.0f), decodeAnchorX(2.7f, 0.0f), decodeAnchorY(0.875f, -3.0f), decodeX(2.7f), decodeY(0.875f));
                path.lineTo(decodeX(2.7f), decodeY(3f)); //bottom-right
                path.lineTo(decodeX(0.0f), decodeY(3f)); //bottom-left
                path.lineTo(decodeX(0.0f), decodeY(0.358f)); //top-left;
                path.closePath();
            } else {
                path.moveTo(decodeX(0.0f), decodeY(0f));
                path.lineTo(decodeX(2.7f), decodeY(0f));
                path.lineTo(decodeX(2.7f), decodeY(2.125f));
                path.curveTo(decodeAnchorX(2.7f, 0.0f), decodeAnchorY(2.125f, 3.0f), decodeAnchorX(2.125f, 3.0f), decodeAnchorY(2.7f, 0.0f), decodeX(2.125f), decodeY(2.7f));
                path.lineTo(decodeX(0.0f), decodeY(2.7f));
                path.lineTo(decodeX(0.0f), decodeY(0));
                path.closePath();
            }
            g.setPaint(colorFill);
            g.fill(path);
            g.setPaint(colorDraw);
            draw(g, path);
        }
    }

    public static class CheckBoxPainter extends ButtonPainter {
        protected boolean selected;
        protected Color colorCheck;
        public CheckBoxPainter(boolean pressed, boolean focused, boolean disabled, boolean selected) {
            super(pressed, focused, disabled, selected, 5, 5, 18, 18);
            this.selected = selected;
        }

        @Override
        protected void doInit() {
            super.doInit();
            colorCheck = decodeColor("nimbusSelectedText", 0, 0, 0.2f, 0);
        }

        @Override
        protected String colorName(boolean fillOrDraw) {
            if (fillOrDraw) {
                if (disabled) {
                    return "background";
                } else {
                    return selected ? "nimbusSelectionBackground" : "background";
                }
            } else {
                return super.colorName(fillOrDraw);
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            super.doPaint(g, c, width, height, extendedCacheKeys);
            if (selected) {
                doPaintCheckMark(g);
            }
        }

        protected void doPaintCheckMark(Graphics2D g) {
            Path2D.Float path = new GeneralPath();
            path.moveTo(decodeX(1.0f), decodeY(1.5f));
            path.lineTo(decodeX(1.45f), decodeY(1.9f));
            path.lineTo(decodeX(2.0f), decodeY(1.1f));
            float w = Math.max(1.0f, (decodeX(2.0f) - decodeX(1.0f)) / 5f);
            var strokeBack = g.getStroke();
            g.setPaint(colorCheck);
            g.setStroke(new BasicStroke(w));
            g.draw(path);
            g.setStroke(strokeBack);
        }
    }

    public static class CheckBoxMenuItemPainter extends CheckBoxPainter {
        protected boolean onSelection;
        public CheckBoxMenuItemPainter(boolean disabled, boolean selected, boolean onSelection) {
            super(false, false, disabled, selected);
            this.onSelection = onSelection;
        }

        @Override
        protected void doInit() {
            super.doInit();
            colorCheck = decodeColor(onSelection ? "nimbusSelectedText" : "text", 0, 0, 0.2f, 0);
        }

        @Override
        protected String colorName(boolean fillOrDraw) {
            if (fillOrDraw && !onSelection) {
                return "background";
            }
            return super.colorName(fillOrDraw);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            Path2D.Float path = new Path2D.Float();
            path.moveTo(decodeX(0.3f), decodeY(1.5f));
            path.lineTo(decodeX(1.75f), decodeY(2.5f));
            path.lineTo(decodeX(2.8f), decodeY(0.5f));
            float w = Math.max(1.0f, (decodeX(2.0f) - decodeX(1.0f)) / 5f);
            var strokeBack = g.getStroke();
            g.setPaint(colorCheck);
            g.setStroke(new BasicStroke(w));
            g.draw(path);
            g.setStroke(strokeBack);
        }
    }

    public static class ProgressBarPainter extends ButtonPainter {
        protected boolean foreground;
        protected boolean finished;
        protected boolean indeterminate;
        protected Color[] colorsIndeterminate;
        public ProgressBarPainter(boolean disabled, boolean foreground, boolean finished, boolean indeterminate) {
            super(false, false, disabled, false);
            this.foreground = foreground;
            this.finished = finished;
            this.indeterminate = indeterminate;
        }

        @Override
        protected void doInit() {
            float satOff = disabled ? -0.2f : 0.0f;
            var fillName = colorName(true);
            var bOffsetFill = brightnessOffset(true);
            colorFill = decodeColor(fillName, 0, satOff, bOffsetFill, 0);

            if (indeterminate && foreground) {
                colorsIndeterminate = new Color[]{
                        decodeColor("nimbusSelectionBackground", 0, satOff, 0, 0),
                        decodeColor("nimbusSelectionBackground", 0, satOff, 0.3f, 0),
                };
            }
        }

        @Override
        protected String colorName(boolean fillOrDraw) {
            return foreground ? "nimbusSelectionBackground" : "background";
        }

        @Override
        protected float brightnessOffset(boolean fillOrDraw) {
            return super.brightnessOffset(fillOrDraw) + (fillOrDraw && foreground && disabled ? 0.5f : 0.0f);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            if (indeterminate && foreground) {
                float x = decodeX(0.0f);
                float y = decodeY(0.4f);
                float w = decodeX(3f) - x;
                float h = decodeY(2.6f) - y;
                Rectangle2D.Float r = new Rectangle2D.Float(x, y, w, h);
                var colors2 = colorsIndeterminate;
                var p = new LinearGradientPaint(x, 0, x + w, 0,
                        new float[]{0.1f, 0.5f, 1f}, new Color[]{colors2[1], colors2[0], colors2[1]});
                g.setPaint(p);
                g.fill(r);
            } else {
                float x = decodeX(0.4f);
                float y = decodeY(0.8f);
                float w = decodeX(2.6f) - x;
                float h = decodeY(2.2f) - y;
                float rw = decodeX(3) - decodeX(2);
                float rh = decodeY(3) - decodeY(2);
                RoundRectangle2D.Float r = new RoundRectangle2D.Float(x, y, w, h, rw, rh);
                g.setPaint(colorFill);
                g.fill(r);
            }
        }
    }

    public static class SliderPainter extends ButtonPainter {
        protected boolean arrow;
        public SliderPainter(boolean pressed, boolean focused, boolean disabled, boolean arrow) {
            super(pressed, focused, disabled, false);
            this.arrow = arrow;
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0.4f);
            float y = decodeY(0.4f);
            float w = decodeX(2.6f) - x;
            float h = decodeY(2.6f) - y;
            Ellipse2D.Float r = new Ellipse2D.Float(x, y, w, h);
            g.setPaint(colorFill);
            g.fill(r);
            g.setPaint(colorDraw);
            draw(g, r);
        }
    }

    public static class RadioButtonPainter extends CheckBoxPainter {
        public RadioButtonPainter(boolean pressed, boolean focused, boolean disabled, boolean selected) {
            super(pressed, focused, disabled, selected);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0.4f);
            float y = decodeY(0.4f);
            float w = decodeX(2.6f) - x;
            float h = decodeY(2.6f) - y;
            Ellipse2D.Float r = new Ellipse2D.Float(x, y, w, h);
            g.setPaint(colorFill);
            g.fill(r);
            g.setPaint(colorDraw);
            draw(g, r);

            if (selected) {
                float sx = decodeX(1.15f);
                float sy = decodeY(1.15f);
                float sw = decodeX(1.9f) - sx;
                float sh = decodeY(1.9f) - sy;
                Ellipse2D.Float sr = new Ellipse2D.Float(sx, sy, sw, sh);
                g.setPaint(colorCheck);
                g.fill(sr);
            }
        }
    }

    public static class SplitPainter extends ButtonPainter {
        public SplitPainter(boolean focused) {
            super(false, focused, false, false);
        }

        @Override
        protected float brightnessOffset(boolean fillOrDraw) {
            if (fillOrDraw) {
                return (selected ? -0.3f : 0f);
            } else {
                return (focused ? 0f : -0.4f) + (disabled ? 0.2f : 0.0f);
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0f);
            float y = decodeY(0f);
            float w = decodeX(3f) - x;
            float h = decodeY(3f) - y;
            var r = new Rectangle2D.Float(x, y, w, h);
            g.setPaint(colorFill);
            g.fill(r);
            if (focused) {
                var line1 = new Line2D.Float(x, y + 1f, x + w, y + 1f);
                var line2 = new Line2D.Float(x, y + h - 1f, x + w, y + h - 1f);
                g.setPaint(colorDraw);
                draw(g, line1);
                draw(g, line2);
            }
        }
    }

    public static class TableHeaderPainter extends ButtonPainter {
        protected Color[] colorsGradient;
        public TableHeaderPainter(boolean pressed, boolean focused, boolean disabled, boolean selected) {
            super(pressed, focused, disabled, selected);
        }

        @Override
        protected void doInit() {
            super.doInit();
            colorsGradient = gradientColors();
        }

        @Override
        protected float brightnessOffset(boolean fillOrDraw) {
            if (fillOrDraw) {
                return (pressed ? -0.2f : 0f) + (selected ? -0.03f : 0f);
            } else {
                return (focused ? 0f : -0.4f) + (disabled ? 0.2f : 0.0f);
            }
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            init();
            float x = decodeX(0);
            float y = decodeY(0);
            float w = decodeX(3) - x;
            float h = decodeY(3) - y;
            var r = new Rectangle2D.Float(x, y, w, h);
            var r2 = new Rectangle2D.Float(x + 1, y + 1, w-2, w-2);
            g.setPaint(colorFill);
            g.fill(r);

            var line = new Line2D.Float(x + w, y, x + w, y + h);
            var p = gradientVertical(y, h, colorsGradient);
            g.setPaint(p);
            g.draw(line);

            if (focused) {
                g.setPaint(colorDraw);
                draw(g, r2);
            }
        }
    }

    public static class ArrowPainter extends BasePainter {
        protected Color colorFill;
        @SuppressWarnings("this-escape")
        public ArrowPainter(boolean disabled, boolean selected) {
            super(5, 5, 9, 10);
            colorFill = decodeColor(
                    selected ? "nimbusSelectedText" :
                            disabled ? "nimbusDisabledText" :
                                    "text", 0, 0, 0, 0);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            Path2D.Float path = new Path2D.Float();
            path.moveTo(decodeX(0), decodeY(0.2f));
            path.lineTo(decodeX(2.7f), decodeY(2.1f));
            path.lineTo(decodeX(0), decodeY(3f));
            path.lineTo(decodeX(0), decodeY(0.2f));
            path.closePath();
            g.setPaint(colorFill);
            g.fill(path);
        }
    }
}