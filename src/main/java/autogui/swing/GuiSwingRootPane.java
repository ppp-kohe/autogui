package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.log.GuiSwingLogManager;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GuiSwingRootPane extends JComponent implements GuiSwingPreferences.RootView {
    protected GuiMappingContext context;
    protected GuiSwingView view;
    protected GuiSwingPreferences preferences;
    protected GuiSwingLogManager.GuiSwingLogWindow logWindow;

    protected JComponent viewComponent;
    protected JMenu objectMenu;
    protected WindowMenuBuilder windowMenuBuilder;

    protected GuiSwingPreferences.WindowPreferencesUpdater logPreferencesUpdater;
    protected GuiSwingPreferences.FileDialogPreferencesUpdater fileDialogPreferencesUpdater;

    protected SettingsWindow settingsWindow;
    protected boolean applicationRoot;

    protected GuiSwingKeyBinding keyBinding;

    protected String title;
    protected JMenuBar menuBar;

    protected ShowPreferencesAction showPreferencesAction;
    protected GuiSwingPreferences.PrefsApplyMenu prefsApplyMenu;
    protected WindowCloseAction closeAction;


    public static GuiSwingRootPane createForObject(Object o) {
        return new GuiSwingRootPane(new GuiMappingContext(
                new GuiTypeBuilder().get(o.getClass()), o));
    }

    public static GuiSwingRootPane createForObjectRelaxed(Object o) {
        return new GuiSwingRootPane(new GuiMappingContext(
                new GuiTypeBuilder.GuiTypeBuilderRelaxed().get(o.getClass()), o));
    }

    public GuiSwingRootPane(GuiMappingContext context) {
        setLayout(new BorderLayout());
        this.context = context;
        if (context.getRepresentation() == null) {
            GuiSwingMapperSet.getReprDefaultSet().match(context);
        }
        this.view = (GuiSwingView) GuiSwingMapperSet.getDefaultMapperSet().view(context);
        init();
    }

    public GuiSwingRootPane(GuiMappingContext context, GuiSwingView view) throws HeadlessException {
        this.context = context;
        this.view = view;
        init();
    }

    @Override
    public GuiMappingContext getContext() {
        return context;
    }

    @Override
    public JComponent getViewComponent() {
        return viewComponent;
    }

    protected void init() {
        initTitle();
        initViewComponent();
        initPrefs(); //read context, viewComponent
        initKeyBinding();
        initViewComponentSet();
        initMenu();
        initLog();
        initFileDialogPrefsUpdater();
        initContextUpdate(); //set context sources
        initPrefsLoad();  //may update properties of context sources
        initSettingWindow();
    }

    protected void initTitle() {
        setTitle(context.getDisplayName());
    }

    protected void initViewComponent() {
        viewComponent = view.createView(context, () -> GuiReprValue.NONE);
    }


    protected void initKeyBinding() {
        closeAction = new WindowCloseAction(this);
        showPreferencesAction = new ShowPreferencesAction(GuiSwingRootPane.this::getPreferences, viewComponent);
        prefsApplyMenu = new GuiSwingPreferences.PrefsApplyMenu(preferences);

        getInputMap().put(
                closeAction.getKeyStroke(),
                closeAction);
        getActionMap().put(closeAction, closeAction);

        keyBinding = new GuiSwingKeyBinding();
        keyBinding.bind(viewComponent);
    }

    protected void initViewComponentSet() {
        add(viewComponent);
    }

    protected void initPrefs() {
        preferences = new GuiSwingPreferences(this);
    }


    protected void initMenu() {
        JMenuBar bar = new JMenuBar();

        objectMenu = new JMenu("Object");
        objectMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                setupObjectMenu();
            }

            @Override
            public void menuDeselected(MenuEvent e) { }

            @Override
            public void menuCanceled(MenuEvent e) { }
        });

        bar.add(objectMenu);

        windowMenuBuilder = new WindowMenuBuilder();
        bar.add(windowMenuBuilder.getMenu());

        setMenuBar(bar);
    }

    public void setupObjectMenu() {
        objectMenu.removeAll();
        objectMenu.add(showPreferencesAction);
        objectMenu.add(prefsApplyMenu);
        objectMenu.addSeparator();
        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            ((GuiSwingView.ValuePane) viewComponent).getSwingMenuBuilder()
                    .build(PopupExtension.MENU_FILTER_IDENTITY,
                            new MenuBuilder.MenuAppender(objectMenu));
            objectMenu.addSeparator();
        }
        objectMenu.add(closeAction);
    }


    protected void initLog() {
        logWindow = initSwingLogManager().createWindow();
        logPreferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(logWindow, context, "$logWindow");
        logPreferencesUpdater.setUpdater(preferences.getUpdateRunner());
        logWindow.addComponentListener(logPreferencesUpdater);

        setContentPane(logWindow.getPaneWithStatusBar(getContentPane()));
    }

    protected GuiSwingLogManager initSwingLogManager() {
        GuiSwingLogManager logManager;
        synchronized (GuiLogManager.class) {
            GuiLogManager m = GuiLogManager.get();
            if (m instanceof GuiSwingLogManager) {
                logManager = (GuiSwingLogManager) m;
            } else {
                logManager = new GuiSwingLogManager();
                logManager.setupConsole(
                        GuiSwingLogManager.replaceErr,
                        GuiSwingLogManager.replaceOut,
                        GuiSwingLogManager.replaceExceptionHandler,
                        GuiSwingLogManager.redirectToConsole);
                GuiLogManager.setManager(logManager);
            }
        }
        return logManager;
    }

    protected void initFileDialogPrefsUpdater() {
        fileDialogPreferencesUpdater = new GuiSwingPreferences.FileDialogPreferencesUpdater(
                SettingsWindow.getFileDialogManager(), context);
        fileDialogPreferencesUpdater.setUpdater(preferences.getUpdateRunner());
        fileDialogPreferencesUpdater.addToDialogManager();
    }

    protected void initContextUpdate() {
        context.updateSourceFromRoot();
    }

    protected void initPrefsLoad() {
        loadPreferences(preferences.getLaunchPreferences());
    }

    protected void initSettingWindow() {
        settingsWindow = new SettingsWindow();
        preferences.setSettingsWindow(settingsWindow);
        GuiSwingView.forEach(GuiSwingView.SettingsWindowClient.class, viewComponent,
                c -> c.setSettingsWindow(settingsWindow));
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setMenuBar(JMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void setContentPane(JComponent comp) {
        if (getComponentCount() > 0) {
            remove(0);
        }
        add(comp, BorderLayout.CENTER);
    }

    public JComponent getContentPane() {
        if (getComponentCount() == 0) {
            return null;
        } else {
            return (JComponent) getComponent(0);
        }
    }

    public GuiSwingView getView() {
        return view;
    }

    public GuiSwingLogManager.GuiSwingLogWindow getLogWindow() {
        return logWindow;
    }

    public JMenu getObjectMenu() {
        return objectMenu;
    }

    public WindowMenuBuilder getWindowMenuBuilder() {
        return windowMenuBuilder;
    }

    public GuiSwingPreferences.WindowPreferencesUpdater getLogPreferencesUpdater() {
        return logPreferencesUpdater;
    }

    public GuiSwingPreferences.FileDialogPreferencesUpdater getFileDialogPreferencesUpdater() {
        return fileDialogPreferencesUpdater;
    }

    public SettingsWindow getSettingsWindow() {
        return settingsWindow;
    }

    public GuiSwingKeyBinding getKeyBinding() {
        return keyBinding;
    }

    public boolean isApplicationRoot() {
        return applicationRoot;
    }

    public void setApplicationRoot(boolean applicationRoot) {
        this.applicationRoot = applicationRoot;
    }

    public void cleanUp() {
        context.shutdownTaskRunner();
        GuiSwingView.forEach(GuiSwingView.ValuePane.class, viewComponent,
                GuiSwingView.ValuePane::shutdownSwingView);
        preferences.shutdown();
        logWindow.dispose();
        settingsWindow.dispose();
        keyBinding.unbind();
        fileDialogPreferencesUpdater.removeFromDialogManager();
    }

    public GuiSwingPreferences getPreferences() {
        return preferences;
    }

    @Override
    public void loadPreferences(GuiPreferences prefs) {
        withError(() -> preferences.getPrefsWindowUpdater().apply(prefs));
        withError(() -> logPreferencesUpdater.apply(prefs));
        withError(() -> fileDialogPreferencesUpdater.apply(prefs));

        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            withError(() -> ((GuiSwingView.ValuePane) viewComponent).loadSwingPreferences(prefs));
        }
        GuiSwingView.loadChildren(prefs, viewComponent);
    }

    public void withError(Runnable r) {
        try {
            r.run();
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    @Override
    public void savePreferences(GuiPreferences prefs) {
        withError(() -> preferences.getPrefsWindowUpdater().getPrefs().saveTo(prefs));
        withError(() -> logPreferencesUpdater.getPrefs().saveTo(prefs));
        withError(() -> fileDialogPreferencesUpdater.getPrefs().saveTo(prefs));

        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            withError(() -> ((GuiSwingView.ValuePane) viewComponent).saveSwingPreferences(prefs));
        }
        GuiSwingView.saveChildren(prefs, viewComponent);
    }

    @SuppressWarnings("unchecked")
    public GuiSwingView.ValuePane<Object> getViewValuePane() {
        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            return (GuiSwingView.ValuePane<Object>) viewComponent;
        }
        return null;
    }

    /**
     *
     * @param context the searched context
     * @return a descendant value pane holding the context, or null.
     *            wrappers which have the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByContext(GuiMappingContext context) {
        return GuiSwingView.findChild(getViewComponent(), p ->
                Objects.equals(context, p.getSwingViewContext()));
    }

    /**
     * @param value the searched value
     * @return a descendant value pane holding the value, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByValue(Object value) {
        return GuiSwingView.findChild(getViewComponent(),
                p -> Objects.equals(p.getSwingViewValue(), value));
    }


    /**
     * @param valuePredicate the condition holds the searched value
     * @return a first descendant value pane holding a value matched by the predicate, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getDescendantByValueIf(Predicate<Object> valuePredicate) {
        return GuiSwingView.findChild(getViewComponent(),
                p -> valuePredicate.test(p.getSwingViewValue()));
    }


    /**
     * @param name the searched context name
     * @return a child (or descendant for wrappers) value pane holding the named context, or null.
     *    wrappers holding the same context are avoided.
     */
    public GuiSwingView.ValuePane<Object> getChildByName(String name) {
        return GuiSwingView.getChild(getViewComponent(),
                p -> p.getSwingViewContext() != null &&
                        Objects.equals(p.getSwingViewContext().getName(), name));
    }

    public GuiSwingActionDefault.ExecutionAction getActionByName(String name) {
        GuiSwingView.ValuePane<Object> v = getViewValuePane();
        if (v != null) {
            return v.getActionByName(name);
        } else {
            return null;
        }
    }

    public GuiSwingActionDefault.ExecutionAction getActionByContext(GuiMappingContext context) {
        GuiSwingView.ValuePane<Object> v = getViewValuePane();
        if (v != null) {
            return v.getActionByContext(context);
        } else {
            return null;
        }
    }

    public GuiSwingActionDefault.ExecutionAction getDescendantActionByContext(GuiMappingContext context) {
        return GuiSwingView.findNonNullByFunction(getViewComponent(), p -> p.getActionByContext(context));
    }


    public static class ShowPreferencesAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected Supplier<GuiSwingPreferences> preferences;
        protected JComponent sender;

        public ShowPreferencesAction(Supplier<GuiSwingPreferences> preferences, JComponent sender) {
            putValue(NAME, "Preferences...");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.preferences = preferences;
            this.sender = sender;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            preferences.get().show(sender);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PREFS_WINDOW;
        }
    }

    public static class WindowMenuBuilder {
        protected JMenu menu;

        protected WindowMenuMinimizeAction minimizeAction;
        protected WindowMenuZoomAction zoomAction;

        public WindowMenuBuilder() {
            menu = new JMenu("Window");
            minimizeAction = new WindowMenuMinimizeAction();
            zoomAction = new WindowMenuZoomAction();
            updateMenuItems();
            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    updateMenuItems();
                }
                @Override
                public void menuDeselected(MenuEvent e) { }
                @Override
                public void menuCanceled(MenuEvent e) { }
            });
        }

        public JMenu getMenu() {
            return menu;
        }

        public void updateMenuItems() {
            menu.removeAll();
            menu.add(minimizeAction);
            menu.add(zoomAction);
            menu.addSeparator();
            updateMenuItemsForFrames();
        }

        public void updateMenuItemsForFrames() {
            for (Frame f : Frame.getFrames()) {
                if (isListed(f)) {
                    menu.add(new JCheckBoxMenuItem(new WindowMenuToFromAction(f)));
                }
            }
        }

        public boolean isListed(Frame f) {
            if (f instanceof SettingsWindow.SettingsFrame) {
                return ((SettingsWindow.SettingsFrame) f).isShown();
            } else {
                return f.isDisplayable(); //dialogs are discarded
            }
        }
    }

    public static Frame getActiveFrame() {
        for (Frame f : Frame.getFrames()) {
            if (f.isActive()) {
                return f;
            }
        }
        return null;
    }

    public static class WindowMenuMinimizeAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        public WindowMenuMinimizeAction() {
            putValue(NAME, "Minimize");
            ///putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Frame f = getActiveFrame();
            if (f != null) {
                f.setState(Frame.ICONIFIED);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_WINDOW;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_WINDOW_VIEW;
        }
    }

    public static class WindowMenuZoomAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        public WindowMenuZoomAction() {
            putValue(NAME, "Zoom");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Frame frm = getActiveFrame();
            if (frm != null) {
                int ex = frm.getExtendedState();
                if ((ex & Frame.MAXIMIZED_HORIZ) != 0 || (ex & Frame.MAXIMIZED_VERT) != 0) {
                    frm.setExtendedState(Frame.NORMAL);
                } else {
                    frm.setExtendedState(Frame.MAXIMIZED_BOTH);
                }
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_WINDOW;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_WINDOW_VIEW;
        }
    }

    public static class WindowMenuToFromAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected Frame frame;

        public WindowMenuToFromAction(Frame frame) {
            this.frame = frame;
            putValue(NAME, frame.getTitle());

            putValue(SELECTED_KEY, frame.isActive());
        }

        public boolean isSelected() {
            return (Boolean) getValue(SELECTED_KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.setVisible(true);
            frame.toFront();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_WINDOW;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_WINDOW_SELECT;
        }
    }

    public static class WindowCloseAction extends AbstractAction {
        protected JComponent pane;
        protected KeyStroke keyStroke;

        public WindowCloseAction(JComponent pane) {
            this.pane = pane;
            putValue(NAME, "Close Window");
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            putValue(ACCELERATOR_KEY, keyStroke);
        }

        public KeyStroke getKeyStroke() {
            return keyStroke;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Window w = SwingUtilities.windowForComponent(pane);
            if (w instanceof GuiSwingWindow) {
                ((GuiSwingWindow) w).close();
            } else {
                w.setVisible(false);
                w.dispose();
            }
        }
    }

}
