package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.swing.util.ApplicationIconGenerator;
import org.autogui.swing.util.SettingsWindow;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * a root window wrapping a {@link GuiSwingRootPane}.
 */
public class GuiSwingWindow extends JFrame implements GuiSwingPreferences.RootView {
    @Serial private static final long serialVersionUID = 1L;
    protected GuiSwingRootPane contextRootPane;
    protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;
    /** @since 1.1 */
    protected boolean exitAfterDispose;
    /** @since 1.5 */
    protected Image appIcon;

    public static GuiSwingWindow createForObject(Object o) {
        return creator()
                .createWindow(o);
    }

    public static GuiSwingWindow createForObjectRelaxed(Object o) {
        return creator()
                .withTypeBuilderRelaxed()
                .createWindow(o);
    }

    public static GuiSwingWindowCreator creator() {
        return new GuiSwingWindowCreator();
    }

    public static class GuiSwingWindowCreator extends GuiSwingRootPane.GuiSwingRootPaneCreator
            implements Function<Object, GuiSwingWindow> {

        public GuiSwingWindowCreator() {}

        @Override
        public GuiSwingWindow apply(Object o) {
            return createWindow(o);
        }

        public GuiSwingWindow createWindow(Object o) {
            return new GuiSwingWindow(create(o));
        }

        public GuiSwingWindow createWindowWithContext(GuiMappingContext context) {
            return new GuiSwingWindow(createWithContext(context));
        }

        @Override
        public GuiSwingWindowCreator withKeyBindingWithoutAutomaticBindings() {
            return (GuiSwingWindowCreator) super.withKeyBindingWithoutAutomaticBindings();
        }

        @Override
        public GuiSwingWindowCreator withKeyBinding(GuiSwingKeyBinding keyBinding) {
            return (GuiSwingWindowCreator) super.withKeyBinding(keyBinding);
        }

        @Override
        public GuiSwingWindowCreator withTypeBuilderRelaxed() {
            return (GuiSwingWindowCreator) super.withTypeBuilderRelaxed();
        }

        @Override
        public GuiSwingWindowCreator withTypeBuilder(GuiTypeBuilder typeBuilder) {
            return (GuiSwingWindowCreator) super.withTypeBuilder(typeBuilder);
        }

        @Override
        public GuiSwingWindowCreator withLogStatusDisabled() {
            return (GuiSwingWindowCreator) super.withLogStatusDisabled();
        }

        @Override
        public GuiSwingWindowCreator withLogStatus(boolean enableLog) {
            return (GuiSwingWindowCreator) super.withLogStatus(enableLog);
        }

        @Override
        public GuiSwingWindowCreator withPreferencesOnMemory() {
            return (GuiSwingWindowCreator) super.withPreferencesOnMemory();
        }

        @Override
        public GuiSwingWindowCreator withPreferences(Function<GuiMappingContext, GuiPreferences> prefsCreator) {
            return (GuiSwingWindowCreator) super.withPreferences(prefsCreator);
        }

        @Override
        public GuiSwingWindowCreator withSettingWindow(SettingsWindow settingsWindow) {
            return (GuiSwingWindowCreator) super.withSettingWindow(settingsWindow);
        }

        @Override
        public GuiSwingWindowCreator withView(GuiSwingView view) {
            return (GuiSwingWindowCreator) super.withView(view);
        }

        @Override
        public GuiSwingWindowCreator withPrefsApplyOptions(GuiSwingPreferences.PrefsApplyOptions prefsApplyOptions) {
            return (GuiSwingWindowCreator) super.withPrefsApplyOptions(prefsApplyOptions);
        }
    }

    public GuiSwingWindow(GuiMappingContext context) throws HeadlessException {
        this(new GuiSwingRootPane(context));
    }

    public GuiSwingWindow(GuiMappingContext context, GuiSwingView view, GuiSwingKeyBinding keyBinding,
                          boolean logStatus, SettingsWindow settingsWindow) throws HeadlessException {
        this(new GuiSwingRootPane(context, view, keyBinding, logStatus, settingsWindow));
    }

    @SuppressWarnings("this-escape")
    public GuiSwingWindow(GuiSwingRootPane contextRootPane) {
        this.contextRootPane = contextRootPane;
        init();
    }

    @Override
    public GuiMappingContext getContext() {
        return contextRootPane.getContext();
    }

    public GuiSwingRootPane getContextRootPane() {
        return contextRootPane;
    }

    @Override
    public JComponent getViewComponent() {
        return contextRootPane.getViewComponent();
    }

    protected void init() {
        initTitle();
        initViewComponentSet();
        initMenu();
        initIcon();
        initPrefsUpdater();
        pack();
        initPrefsLoad();  //may update properties of context sources
        initClosing();
    }

    protected void initTitle() {
        setTitle(contextRootPane.getTitle());
    }

    protected void initViewComponentSet() {
        setContentPane(contextRootPane);
    }

    protected void initMenu() {
        initMenuScreen();
        setJMenuBar(contextRootPane.getMenuBar());
    }

    /**
     * setting up the menu property
     * @since 1.2
     */
    protected void initMenuScreen() {
        String macOSMenuName = "apple.laf.useScreenMenuBar";
        if (System.getProperty(macOSMenuName) == null) {
            System.setProperty(macOSMenuName, "true");
        }
    }

    protected void initIcon() {
        int wh = UIManagerUtil.getInstance().getScaledSizeInt(256);
        ApplicationIconGenerator gen = new ApplicationIconGenerator(wh, wh, GuiMappingContext.nameSplit(contextRootPane.getContext().getName(), true));
        appIcon = gen.getImage();
        updateAppIcon();
    }

    protected void initPrefsUpdater() {
        preferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(this, contextRootPane.getContext());
        preferencesUpdater.setUpdater(contextRootPane.getPreferences().getUpdateRunner());
        addComponentListener(preferencesUpdater);
        contextRootPane.getPreferences().setRootView(this);
    }

    protected void initPrefsLoad() {
        loadPreferences(contextRootPane.getPreferences().getLaunchPreferences(),
                contextRootPane.getPrefsApplyOptions());
    }

    protected void initClosing() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    public void close() {
        if (contextRootPane.isApplicationRoot()) {
            cleanUp();
        }
    }

    public boolean isApplicationRoot() {
        //return true;
        return contextRootPane.isApplicationRoot();
    }

    public void setApplicationRoot(boolean applicationRoot) {
        //this.applicationRoot = applicationRoot;
        contextRootPane.setApplicationRoot(applicationRoot);
        updateAppIcon();
    }

    private void updateAppIcon() {
        if (isApplicationRoot() && appIcon != null) {
            ApplicationIconGenerator.setAppIcon(appIcon);
        } else if (appIcon != null) {
            setIconImage(appIcon);
        }
    }

    /**
     * @param exitAfterDispose if true, {@link System#exit(int)} with 0 after disposing the window.
     * @since 1.1
     */
    public void setExitAfterDispose(boolean exitAfterDispose) {
        this.exitAfterDispose = exitAfterDispose;
    }

    /**
     * @return the current flag
     * @since 1.1
     */
    public boolean isExitAfterDispose() {
        return exitAfterDispose;
    }

    public void cleanUp() {
        contextRootPane.cleanUp();
        dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (exitAfterDispose) {
            System.exit(0);
        }
    }

    public GuiSwingPreferences getPreferences() {
        return contextRootPane.getPreferences();
    }

    @Override
    public void loadPreferences(GuiPreferences prefs, GuiSwingPreferences.PrefsApplyOptions options) {
        options.begin(this, prefs, GuiSwingPreferences.PrefsApplyOptionsLoadingTargetType.View);
        try {
            contextRootPane.withError(() -> options.apply(preferencesUpdater, prefs));
            contextRootPane.loadPreferences(prefs, options);
        } finally {
            options.end(this, prefs, GuiSwingPreferences.PrefsApplyOptionsLoadingTargetType.View);
        }
    }

    @Override
    public void savePreferences(GuiPreferences prefs) {
        contextRootPane.withError(() -> preferencesUpdater.getPrefs().saveTo(prefs));
        contextRootPane.savePreferences(prefs);
    }

    public GuiSwingView.ValuePane<Object> getViewValuePane() {
        return contextRootPane.getViewValuePane();
    }

    /**
     *
     * @param context the searched context
     * @return a descendant value pane holding the context, or null.
     *            wrappers which have the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByContext(GuiMappingContext context) {
        return contextRootPane.getDescendantByContext(context);
    }

    /**
     * @param value the searched value
     * @return a descendant value pane holding the value, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByValue(Object value) {
        return contextRootPane.getDescendantByValue(value);
    }


    /**
     * @param valuePredicate the condition holds the searched value
     * @return a first descendant value pane holding a value matched by the predicate, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByValueIf(Predicate<Object> valuePredicate) {
        return contextRootPane.getDescendantByValueIf(valuePredicate);
    }


    /**
     * @param name the searched context name
     * @return a child (or descendant for wrappers) value pane holding the named context, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getChildByName(String name) {
        return contextRootPane.getChildByName(name);
    }

    public GuiSwingActionDefault.ExecutionAction getActionByName(String name) {
        return contextRootPane.getActionByName(name);
    }

    public GuiSwingActionDefault.ExecutionAction getActionByContext(GuiMappingContext context) {
        return contextRootPane.getActionByContext(context);
    }

    public GuiSwingActionDefault.ExecutionAction getDescendantActionByContext(GuiMappingContext context) {
        return contextRootPane.getDescendantActionByContext(context);
    }

    /**
     * the convenient method for refreshing the pane
     * @since 1.5
     */
    public void refreshByContext() {
        contextRootPane.refreshByContext();
    }
}
