package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.log.GuiSwingLogManager;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GuiSwingWindow extends JFrame {
    protected GuiMappingContext context;
    protected GuiSwingView view;
    protected GuiSwingPreferences preferences;
    protected GuiSwingLogManager.GuiSwingLogWindow logWindow;

    protected JComponent viewComponent;
    protected JMenu objectMenu;
    protected WindowMenuBuilder windowMenuBuilder;

    protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;
    protected GuiSwingPreferences.WindowPreferencesUpdater logPreferencesUpdater;

    protected SettingsWindow settingsWindow;
    protected boolean applicationRoot;

    public static GuiSwingWindow createForObject(Object o) {
        return new GuiSwingWindow(new GuiMappingContext(
            new GuiTypeBuilder().get(o.getClass()), o));
    }

    public static GuiSwingWindow createForObjectRelaxed(Object o) {
        return new GuiSwingWindow(new GuiMappingContext(
                new GuiTypeBuilder.GuiTypeBuilderRelaxed().get(o.getClass()), o));
    }

    public GuiSwingWindow(GuiMappingContext context) {
        this.context = context;
        if (context.getRepresentation() == null) {
            GuiSwingElement.getReprDefaultSet().match(context);
        }
        this.view = (GuiSwingView) GuiSwingElement.getDefaultMapperSet().view(context);
        init();
    }



    public GuiSwingWindow(GuiMappingContext context, GuiSwingView view) throws HeadlessException {
        this.context = context;
        this.view = view;
        init();
    }

    public GuiMappingContext getContext() {
        return context;
    }

    public JComponent getViewComponent() {
        return viewComponent;
    }

    protected void init() {
        initTitle();
        initViewComponent();
        initPrefs(); //read context, viewComponent
        initViewComponentSet();
        initKeyBinding();
        initMenu();
        initLog();
        initIcon();
        initPrefsUpdater();
        pack();
        initContextUpdate(); //set context sources
        initPrefsLoad();  //may update properties of context sources
        initClosing();
        initSettingWindow();
    }

    protected void initTitle() {
        setTitle(context.getDisplayName());
    }

    protected void initViewComponent() {
        viewComponent = view.createView(context);
    }

    protected void initViewComponentSet() {
        setContentPane(viewComponent);
    }

    protected void initPrefs() {
        preferences = new GuiSwingPreferences(this);
    }


    protected void initKeyBinding() {
        new GuiSwingKeyBinding().bind(viewComponent);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.err.println("presed " + e);
            }
        });
//        put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.META_DOWN_MASK),
//                new AbstractAction() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        System.err.println("test");
//                    }
//                }); //DEBUG
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

        setJMenuBar(bar);
    }

    public void setupObjectMenu() {
        objectMenu.removeAll();
        objectMenu.add(new ShowPreferencesAction(GuiSwingWindow.this::getPreferences, viewComponent));
        objectMenu.addSeparator();
        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            ((GuiSwingView.ValuePane) viewComponent).getSwingMenuBuilder()
                    .build(i -> i,
                            new MenuBuilder.MenuAppender(objectMenu));
        }
    }


    protected void initLog() {
        logWindow = initSwingLogManager().createWindow();
        logPreferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(logWindow, context, "$logWindow");
        logPreferencesUpdater.setUpdater(preferences.getUpdateRunner());
        logWindow.addComponentListener(logPreferencesUpdater);
        setContentPane(logWindow.getPaneWithStatusBar(viewComponent));
    }

    protected GuiSwingLogManager initSwingLogManager() {
        GuiSwingLogManager logManager;
        synchronized (GuiLogManager.class) {
            GuiLogManager m = GuiLogManager.get();
            if (m instanceof GuiSwingLogManager) {
                logManager = (GuiSwingLogManager) m;
            } else {
                logManager = new GuiSwingLogManager();
                logManager.setupConsole(true, true, true);
                GuiLogManager.setManager(logManager);
            }
        }
        return logManager;
    }

    protected void initIcon() {
        new ApplicationIconGenerator(256, 256, context.getName())
                .setAppIcon(this);
    }

    protected void initPrefsUpdater() {
        preferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(this, context);
        preferencesUpdater.setUpdater(preferences.getUpdateRunner());
        addComponentListener(preferencesUpdater);
    }

    protected void initContextUpdate() {
        context.updateSourceFromRoot();
    }

    protected void initPrefsLoad() {
        loadPreferences(context.getPreferences());
    }

    protected void initClosing() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isApplicationRoot()) {
                    cleanUp();
                }
            }
        });
    }

    protected void initSettingWindow() {
        settingsWindow = new SettingsWindow();
        preferences.setSettingsWindow(settingsWindow);
        GuiSwingView.forEach(GuiSwingView.SettingsWindowClient.class, viewComponent,
                c -> c.setSettingsWindow(settingsWindow));
    }

    public boolean isApplicationRoot() {
        return true;
    }

    public void setApplicationRoot(boolean applicationRoot) {
        this.applicationRoot = applicationRoot;
    }

    public void cleanUp() {
        context.getTaskRunner().shutdown();
        GuiSwingView.forEach(GuiSwingView.ValuePane.class, viewComponent,
                GuiSwingView.ValuePane::shutdownSwingView);
        preferences.shutdown();
        logWindow.dispose();
        settingsWindow.getWindow().dispose();
        dispose();
    }

    public GuiSwingPreferences getPreferences() {
        return preferences;
    }

    public void loadPreferences(GuiPreferences prefs) {
        try {
            preferencesUpdater.apply(context.getPreferences());
            preferences.getPrefsWindowUpdater().apply(context.getPreferences());
            logPreferencesUpdater.apply(context.getPreferences());

            if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane) viewComponent).loadSwingPreferences(prefs);
            }
            GuiSwingView.loadChildren(prefs, viewComponent);
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    public void savePreferences(GuiPreferences prefs) {
        try {
            preferencesUpdater.getPrefs().saveTo(prefs);
            preferences.getPrefsWindowUpdater().getPrefs().saveTo(prefs);
            logPreferencesUpdater.getPrefs().saveTo(prefs);

            if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane) viewComponent).saveSwingPreferences(prefs);
            }
            GuiSwingView.saveChildren(prefs, viewComponent);
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    public static class ShowPreferencesAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected Supplier<GuiSwingPreferences> preferences;
        protected JComponent sender;

        public ShowPreferencesAction(Supplier<GuiSwingPreferences> preferences, JComponent sender) {
            putValue(NAME, "Preferences...");
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
                return true;
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
}
