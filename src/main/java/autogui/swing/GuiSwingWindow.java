package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.log.GuiSwingLogManager;
import autogui.swing.util.ApplicationIconGenerator;
import autogui.swing.util.MenuBuilder;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

public class GuiSwingWindow extends JFrame {
    protected GuiMappingContext context;
    protected GuiSwingView view;
    protected GuiSwingPreferences preferences;
    protected GuiSwingLogManager logManager;

    protected JComponent viewComponent;
    protected JMenu objectMenu;

    protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;
    protected GuiSwingPreferences.WindowPreferencesUpdater logPreferencesUpdater;

    public static GuiSwingWindow createForObject(Object o) {
        return new GuiSwingWindow(new GuiMappingContext(
            new GuiTypeBuilder().get(o.getClass()), o));
    }

    public GuiSwingWindow(GuiMappingContext context) {
        this.context = context;
        setTitle(context.getDisplayName());
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
        viewComponent = view.createView(context);

        preferences = new GuiSwingPreferences(this);

        setContentPane(viewComponent);
        initMenu();
        initLog();
        initIcon();
        initPrefs();
        pack();
        initPrefsLoad();

        context.updateSourceFromRoot();
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

        setJMenuBar(bar);
    }

    public void setupObjectMenu() {
        objectMenu.removeAll();
        objectMenu.add(new ShowPreferencesAction(GuiSwingWindow.this::getPreferences, viewComponent));
        objectMenu.addSeparator();
        if (viewComponent instanceof GuiSwingView.ValuePane<?>) {
            ((GuiSwingView.ValuePane) viewComponent).getSwingMenuBuilder()
                    .build(() -> viewComponent,
                            new MenuBuilder.MenuAppender(objectMenu));
        }
    }


    protected void initLog() {
        logManager = new GuiSwingLogManager();
        logManager.setupConsole(true, true, true);
        GuiLogManager.setManager(logManager);
        GuiSwingLogManager.GuiSwingLogWindow logWindow = logManager.createWindow();
        logPreferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(logWindow, context, "$logWindow");
        logPreferencesUpdater.setUpdater(preferences.getUpdateRunner());
        logWindow.addComponentListener(logPreferencesUpdater);
        setContentPane(logWindow.getPaneWithStatusBar(viewComponent));
    }

    protected void initIcon() {
        new ApplicationIconGenerator(256, 256, context.getName())
                .setAppIcon(this);
    }

    protected void initPrefs() {
        preferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(this, context);
        preferencesUpdater.setUpdater(preferences.getUpdateRunner());
        addComponentListener(preferencesUpdater);
    }

    protected void initPrefsLoad() {
        loadPreferences(context.getPreferences());
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
                ((GuiSwingView.ValuePane) viewComponent).loadPreferences(prefs);
            } else {
                GuiSwingView.loadChildren(prefs, viewComponent);
            }
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
                ((GuiSwingView.ValuePane) viewComponent).savePreferences(prefs);
            } else {
                GuiSwingView.saveChildren(prefs, viewComponent);
            }
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    public static class ShowPreferencesAction extends AbstractAction {
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
    }
}
