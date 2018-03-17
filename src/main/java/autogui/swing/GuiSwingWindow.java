package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.log.GuiSwingLogManager;
import autogui.swing.log.GuiSwingLogStatusBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

public class GuiSwingWindow extends JFrame {
    protected GuiMappingContext context;
    protected GuiSwingView view;
    protected GuiSwingPreferences preferences;
    protected GuiSwingLogManager logManager;

    protected JComponent viewComponent;

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

    protected void init() {
        viewComponent = view.createView(context);

        preferences = new GuiSwingPreferences(context);

        setContentPane(viewComponent);
        initMenu();
        initLog();

        pack();
        context.updateSourceFromRoot();
    }

    protected void initMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu menu = new JMenu("Object");
        menu.add(new ShowPreferencesAction(this::getPreferences, viewComponent));
        bar.add(menu);

        setJMenuBar(bar);
    }

    protected void initLog() {
        logManager = new GuiSwingLogManager();
        logManager.setupConsole(true, true, true);
        GuiLogManager.setManager(logManager);
        setContentPane(logManager.createWindow().getPaneWithStatusBar(viewComponent));
    }

    public GuiSwingPreferences getPreferences() {
        return preferences;
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
