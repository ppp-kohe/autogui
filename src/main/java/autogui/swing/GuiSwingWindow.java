package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.util.ApplicationIconGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Predicate;

public class GuiSwingWindow extends JFrame implements GuiSwingPreferences.RootView {
    protected GuiSwingRootPane contextRootPane;
    protected GuiSwingPreferences.WindowPreferencesUpdater preferencesUpdater;

    public static GuiSwingWindow createForObject(Object o) {
        return new GuiSwingWindow(new GuiMappingContext(
            new GuiTypeBuilder().get(o.getClass()), o));
    }

    public static GuiSwingWindow createForObjectRelaxed(Object o) {
        return new GuiSwingWindow(new GuiMappingContext(
                new GuiTypeBuilder.GuiTypeBuilderRelaxed().get(o.getClass()), o));
    }

    public GuiSwingWindow(GuiMappingContext context) {
        contextRootPane = new GuiSwingRootPane(context);
        init();
    }

    public GuiSwingWindow(GuiMappingContext context, GuiSwingView view) throws HeadlessException {
        contextRootPane = new GuiSwingRootPane(context, view);
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
        setJMenuBar(contextRootPane.getMenuBar());
    }

    protected void initIcon() {
        new ApplicationIconGenerator(256, 256, GuiMappingContext.nameSplit(contextRootPane.getContext().getName(), true))
                .setAppIcon(this);
    }

    protected void initPrefsUpdater() {
        preferencesUpdater = new GuiSwingPreferences.WindowPreferencesUpdater(this, contextRootPane.getContext());
        preferencesUpdater.setUpdater(contextRootPane.getPreferences().getUpdateRunner());
        addComponentListener(preferencesUpdater);
        contextRootPane.getPreferences().setRootView(this);
    }

    protected void initPrefsLoad() {
        loadPreferences(contextRootPane.getContext().getPreferences());
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
    }

    public void cleanUp() {
        contextRootPane.cleanUp();
        dispose();
    }

    public GuiSwingPreferences getPreferences() {
        return contextRootPane.getPreferences();
    }

    @Override
    public void loadPreferences(GuiPreferences prefs) {
        contextRootPane.withError(() -> preferencesUpdater.apply(prefs));
        contextRootPane.loadPreferences(prefs);
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
}
