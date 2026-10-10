package org.autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.AppForegroundEvent;
import java.awt.desktop.AppForegroundListener;
import java.util.function.Function;

/**
 * the polling based auto LAF switcher for {@link NimbusLookAndFeelCustomFlat}
 * <pre>
 *    new LookAndFeelThemeSwitcher(100_000).install()
 * </pre>
 * @since 1.9
 */
public class LookAndFeelThemeSwitcher implements AppForegroundListener {
    protected int pollingMillis;

    protected boolean installed;
    protected boolean background;
    protected Timer pollingTimer;

    protected boolean darkTheme;

    protected Function<Boolean, LookAndFeel> lookAndFeelFunction;

    @SuppressWarnings("this-escape")
    public LookAndFeelThemeSwitcher(int pollingMillis, Function<Boolean, LookAndFeel> lookAndFeelFunction) {
        this.pollingMillis = pollingMillis;
        this.darkTheme = isDarkTheme();
        this.lookAndFeelFunction = lookAndFeelFunction;
    }

    public LookAndFeelThemeSwitcher(int pollingMillis) {
        this(pollingMillis, LookAndFeelThemeSwitcher::lafForTheme);
    }

    public synchronized boolean isInstalled() {
        return installed;
    }

    public synchronized void install() {
        if (Desktop.isDesktopSupported() && !installed) {
            installed = true;
            var desktop = Desktop.getDesktop();
            desktop.addAppEventListener(this);
            darkTheme = isDarkTheme();
            if (pollingMillis > 0) {
                pollingTimer = new Timer(pollingMillis, e -> updateThemeIfChangedAndInstalled());
                pollingTimer.setRepeats(true);
                pollingTimer.start();
            }
        }
    }

    public synchronized void uninstall() {
        if (Desktop.isDesktopSupported() && installed) {
            installed = false;
            var desktop = Desktop.getDesktop();
            desktop.removeAppEventListener(this);
            if (pollingTimer != null) {
                pollingTimer.stop();
                pollingTimer = null;
            }
        }
    }

    protected boolean isDarkTheme() {
        return UIManagerUtil.getInstance().getOsVersion().isDarkTheme();
    }

    public synchronized void updateThemeIfChangedAndInstalled() {
        if (installed) {
            updateThemeIfChanged();
        }
    }

    public synchronized void updateThemeIfChanged() {
        boolean current = isDarkTheme();
        if (current != darkTheme) {
            this.darkTheme = current;
            SwingUtilities.invokeLater(() -> switchTheme(current));
        }
    }

    public void switchTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
        try {
            UIManager.setLookAndFeel(lookAndFeelFunction.apply(darkTheme));
            for (var w : Window.getWindows()) {
                switchTheme(darkTheme, w);
            }
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
    }

    protected void switchTheme(boolean darkTheme, Window w) {
        SwingUtilities.updateComponentTreeUI(w);
        w.revalidate();
    }

    public static LookAndFeel lafForTheme(boolean darkTheme) {
        return darkTheme ?
                new NimbusLookAndFeelCustomFlat.NimbusLookAndFeelCustomFlatDark() :
                new NimbusLookAndFeelCustomFlat.NimbusLookAndFeelCustomFlatLight();
    }

    @Override
    public synchronized void appMovedToBackground(AppForegroundEvent e) {
        background = true;
    }

    @Override
    public synchronized void appRaisedToForeground(AppForegroundEvent e) {
        background = false;
        updateThemeIfChangedAndInstalled();
    }
}
