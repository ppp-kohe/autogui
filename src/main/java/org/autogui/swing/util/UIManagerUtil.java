package org.autogui.swing.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** a support class with {@link UIManager}.
 *  the main purpose of the class is providing default settings of UI resources and sizes
 *      in order to support HiDPI environments.
 *  <pre>
 *          int size = UIManager.getInstance().getScaledSizeForInt(12);
 *          ...
 *  </pre>
 *  */
public class UIManagerUtil {
    static UIManagerUtil instance;

    protected Font consoleFont;
    protected int iconSize = -1;
    protected boolean systemLaf;

    /**
     * @since 1.2
     */
    protected OsVersion osVersion = initOsVersion();

    public static UIManagerUtil getInstance() {
        if (instance == null) {
            instance = new UIManagerUtil();
        }
        return instance;
    }

    public Font getConsoleFont() {
        if (consoleFont == null) {
            Font base = UIManager.getFont("List.font");
            int size = (base == null ? 12 : base.getSize());

            Font f = null;

            if (getOsVersion() instanceof OsVersionMac) {
                f = new Font("Menlo", Font.PLAIN, size); //macOS Sierra introduced "SF Mono" font but it seems not available
                //Windows and Linux seems not to support font-fallback
//            } else if (os.contains("windows")) {
//                f = new Font("Consolas", Font.PLAIN, size); //from Vista
//            }
//            if (f == null || f.getFamily().equals(Font.DIALOG)) { //free font
//                f = new Font("DejaVu Sans Mono", Font.PLAIN, size);
            }
            if (f == null || f.getFamily().equals(Font.DIALOG)) { //if the font didn't find, the family becomes "Dialog"
                f = new Font(Font.MONOSPACED, Font.PLAIN, size);
            }
            consoleFont = f;
        }
        return consoleFont;
    }

    public Font getLabelFont() {
        Font font = UIManager.getFont("Label.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        return font;
    }

    public Font getEditorPaneFont() {
        Font font = UIManager.getFont("EditorPane.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        return font;
    }


    public Border getTableFocusCellHighlightBorder() {
        Border b = UIManager.getBorder("Table.focusCellHighlightBorder");
        if (b == null) {
            b = BorderFactory.createEmptyBorder();
        }
        return b;
    }

    public Color getLabelForeground() {
        Color color = UIManager.getColor("Label.foreground");
        if (color == null) {
            color = Color.black;
        }
        return color;
    }

    public Color getLabelBackground() {
        Color color = UIManager.getColor("Label.background");
        if (color == null) {
            color = new Color(238, 238, 238);
        }
        return color;
    }


    public Color getLabelDisabledForeground() {
        Color color = UIManager.getColor("Label.disabledForeground");
        if (color == null) {
            color = new Color(128, 128, 128);
        }
        return color;
    }

    public Color getTextPaneSelectionBackground() {
        Color color = UIManager.getColor("TextPane.selectionBackground");
        if (color == null) {
            color = new Color(160, 200, 250);
        }
        return color;
    }

    public Color getTextPaneSelectionForeground() {
        Color color = UIManager.getColor("TextPane.selectionForeground");
        if (color == null) {
            color = Color.white;
        }
        return color;
    }

    /**
     * @return the text pane foreground
     * @since 1.2
     */
    public Color getTextPaneForeground() {
        Color color = UIManager.getColor("TextPane.foreground");
        if (color == null) {
            color = Color.black;
        }
        return color;
    }

    /**
     * @return the text pane background
     * @since 1.2
     */
    public Color getTextPaneBackground() {
        Color color = UIManager.getColor("TextPane.background");
        if (color == null) {
            color = Color.white;
        }
        return color;
    }

    public Color getFocusColor() {
        Color color = UIManager.getColor("Focus.color");
        if (color == null) {
            color = new Color(150, 150, 150);
        }
        return color;
    }

    public Color getMenuDisabledForeground() {
        Color color = UIManager.getColor("MenuItem.disabledForeground");
        if (color == null) {
            color = Color.darkGray;
        }
        return color;
    }

    /**
     * @return Table.alternateRowColor or null
     * @since 1.2
     */
    public Color getTableAlternateRowColor() {
        return UIManager.getColor("Table.alternateRowColor");
    }

    /**
     * @return Table.dropCellBackground or null
     * @since 1.2
     */
    public Color getTableDropCellBackground() {
        return UIManager.getColor("Table.dropCellBackground");
    }

    /**
     * @return Table.dropCellForeground or null
     * @since 1.2
     */
    public Color getTableDropCellForeground() {
        return UIManager.getColor("Table.dropCellForeground");
    }

    /**
     * @return Table.focusCellBackground or null
     * @since 1.2
     */
    public Color getTableFocusCellBackground() {
        return UIManager.getColor("Table.focusCellBackground");
    }

    /**
     * @return Table.background or null
     * @since 1.2
     */
    public Color getTableBackground() {
        return UIManager.getColor("Table.background");
    }

    /**
     * @return Table.focusCellForeground or null
     * @since 1.2
     */
    public Color getTableFocusCellForeground() {
        return UIManager.getColor("Table.focusCellForeground");
    }


    /**
     * @return List.alternateRowColor or null
     * @since 1.2
     */
    public Color getListAlternateRowColor() {
        return UIManager.getColor("List.alternateRowColor");
    }

    /**
     * @return List.dropCellBackground or null
     * @since 1.2
     */
    public Color getListDropCellBackground() {
        return UIManager.getColor("List.dropCellBackground");
    }

    /**
     * @return List.dropCellForeground or null
     * @since 1.2
     */
    public Color getListDropCellForeground() {
        return UIManager.getColor("List.dropCellForeground");
    }

    /**
     * @return List.focusCellBackground or null
     * @since 1.2
     */
    public Color getListFocusCellBackground() {
        return UIManager.getColor("List.focusCellBackground");
    }

    /**
     * @return List.focusCellForeground or null
     * @since 1.2
     */
    public Color getListFocusCellForeground() {
        return UIManager.getColor("List.focusCellForeground");
    }


    /**
     * @return 32 or label font height x2.1
     */
    public int getIconSize() {
        if (iconSize < 0) {
            try {
                BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = img.createGraphics();
                {
                    Font font = getLabelFont();
                    TextLayout l = new TextLayout("M", font, g.getFontRenderContext());
                    float h = l.getAscent() + l.getDescent();
                    iconSize = (int) (h * 2.1f); //in macOS HS Lucida Grande 13pt
                }
                g.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                iconSize = 32;
            }
        }
        return iconSize;
    }

    /**
     * @param iconSize the new icon size: font height x2.1
     * @since 1.2
     */
    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
    }

    public int getScaledSizeInt(int n) {
        //macOS Retina env automatically scales the size:
        //    e.g. with x2 PPI, texts with size 14 Font will be rendered as size 28. border size 5 will be handled as 10.
        // however GTK (and Windows too?) physically increases UI resource sizes, and we need to supply x2 sizes for x2 PPI.
        return (int) getScaledSizeFloat(n);
    }

    public float getScaledSizeFloat(float n) {
        return ((float) getIconSize()) / 32f * n;
    }

    /**
     * <ol>
     *     <li>#prop:p =&gt; {@link #selectLookAndFeelFromProperty(String, boolean)} with (p,true) </li>
     *     <li>#special:v =&gt; {@link #selectLookAndFeelFromSpecialName(String)} with (v) </li>
     *     <li>#default or default =&gt; {@link #selectLookAndFeelDefault(boolean)}</li>
     *     <li>darklaf =&gt; {@link #installLookAndFeelDarkLaf()}</li>
     *     <li>#none =&gt; nothing to do. it also clears the "autogui.laf" property
     *            for preventing changing LAF by the user</li>
     *     <li>null =&gt; nothing to do </li>
     *     <li>otherwise =&gt; treats the value as a LAF class-name </li>
     * </ol>
     * @param lookAndFeelClass the LAF class name or a special configuration name
     *                          starting from <code>#prop:</code>,
     *                          <code>#none</code> or
     *                          <code>#default</code>
     * @since 1.2
     */
    public void setLookAndFeel(String lookAndFeelClass) {
        try {
            String laf = lookAndFeelClass;
            if (laf != null && laf.startsWith(LOOK_AND_FEEL_PROP_HEAD)) {
                laf = selectLookAndFeelFromProperty(laf.substring(LOOK_AND_FEEL_PROP_HEAD.length()), true);
            }
            if (laf != null && laf.startsWith(LOOK_AND_FEEL_SPECIAL_HEAD)) {
                laf = selectLookAndFeelFromSpecialName(laf.substring(LOOK_AND_FEEL_SPECIAL_HEAD.length()));
            }
            if (laf != null && (laf.equals(LOOK_AND_FEEL_DEFAULT) ||
                                laf.equals(LOOK_AND_FEEL_VALUE_DEFAULT) ||
                                laf.equals(LOOK_AND_FEEL_VALUE_DEFAULT_NO_DARKLAF))) {
                laf = selectLookAndFeelDefault(!laf.equals(LOOK_AND_FEEL_VALUE_DEFAULT_NO_DARKLAF));
            }
            if (laf != null && laf.equals(LOOK_AND_FEEL_VALUE_DARKLAF)) {
                installLookAndFeelDarkLaf();
                laf = null;
            }
            if (laf != null && laf.equals(LOOK_AND_FEEL_NONE)) {
                laf = null;
                System.clearProperty(LOOK_AND_FEEL_PROP_DEFAULT);
            }
            if (laf != null) {
                UIManager.setLookAndFeel(laf);
            }
            if (laf != null && Objects.equals(UIManager.getSystemLookAndFeelClassName(), laf)) {
                systemLaf = true;
                setLookAndFeelSystemFix();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  process LAF resolution for "#prop:p".
     *  it reads a LAF class-name from the system property "p".
     *      The value of p can be a special name resolved by {@link #selectLookAndFeelFromSpecialName(String)}
     *        or a concrete LAF class-name .
     *  <p>
     *   The default prop is "autogui.laf".
     *    This means that the system accepts the property with a special name like "-Dautogui.laf=system"
     *      and it can resolve the property with an actual class-name which will be read by the swing system.
     * @param prop a nullable property name
     * @param updateProp if true it updates the prop specified by the laf with the resolved concrete class-name
     *                    For "darklaf", it clears the property
     * @return  a resolved class-name from the property or null
     * @since 1.3
     */
    public String selectLookAndFeelFromProperty(String prop, boolean updateProp) {
        if (prop == null || prop.isEmpty()) {
            return null;
        } else {
            String value = System.getProperty(prop);
            String laf = selectLookAndFeelFromSpecialName(value);
            if (updateProp) {
                if (laf == null) {
                    if (value != null && value.isEmpty()) {
                        System.clearProperty(prop);
                    }
                } else if (isLookAndFeelSpecial(laf)) {
                    System.clearProperty(prop);
                } else {
                    System.setProperty(prop, laf);
                }
            }
            return laf;
        }
    }

    private boolean isLookAndFeelSpecial(String laf) {
        return laf != null &&
                (LOOK_AND_FEEL_VALUE_METAL.equals(laf) ||
                 LOOK_AND_FEEL_VALUE_NIMBUS.equals(laf) ||
                 LOOK_AND_FEEL_VALUE_SYSTEM.equals(laf) ||
                 LOOK_AND_FEEL_VALUE_DEFAULT.equals(laf) ||
                 LOOK_AND_FEEL_VALUE_DARKLAF.equals(laf) ||
                 LOOK_AND_FEEL_VALUE_DEFAULT_NO_DARKLAF.equals(laf));
    }

    /**
     * <ul>
     * <li>"metal" =&gt; <code>MetalLookAndFeel</code> </li>
     * <li> "nimbus" =&gt; <code>NimbusLookAndFeel</code> </li>
     * <li> "system" =&gt; {@link UIManager#getSystemLookAndFeelClassName()} </li>
     * <li> null, "" or "default"  =&gt; "default" for later processes by {@link #selectLookAndFeelDefault(boolean)} </li>
     * <li> "darklaf"  =&gt; "darklaf" for later processes by {@link #installLookAndFeelDarkLaf()} </li>
     * <li> "default-no-darklaf" =&gt; "default-no-darklaf" for later processes. same as "default" except for no darklaf installing </li>
     * <li> otherwise =&gt; name as is </li>
     * </ul>
     * @param name a special name or a class-name
     * @return resolved class name or still special name for some names
     * @since 1.3
     */
    public String selectLookAndFeelFromSpecialName(String name) {
        if (name == null || name.isEmpty()) {
            return LOOK_AND_FEEL_VALUE_DEFAULT;
        } else if (name.equals(LOOK_AND_FEEL_VALUE_METAL)) {
            return "javax.swing.plaf.metal.MetalLookAndFeel";
        } else if (name.equals(LOOK_AND_FEEL_VALUE_NIMBUS)) {
            return "javax.swing.plaf.nimbus.NimbusLookAndFeel";
        } else if (name.equals(LOOK_AND_FEEL_VALUE_SYSTEM)) {
            return UIManager.getSystemLookAndFeelClassName();
        } else if (name.equals(LOOK_AND_FEEL_VALUE_DEFAULT) ||
                    name.equals(LOOK_AND_FEEL_VALUE_DEFAULT_NO_DARKLAF) ||
                    name.equals(LOOK_AND_FEEL_VALUE_DARKLAF)) {
            return name;
        } else {
            return name;
        }
    }

    /**
     * process default behavior for the "#default" configuration
     * <ol>
     *     <li>try to load Darklaf by {@link #installLookAndFeelDarkLaf()}  if the parameter is true</li>
     *     <li>if non-Unix environment, use {@link UIManager#getSystemLookAndFeelClassName()}</li>
     *     <li>if the GDK_SCALE env is set, use "metal" </li>
     *     <li>otherwise, nothing to do</li>
     * </ol>
     * @param tryDarklaf if true, call {@link #installLookAndFeelDarkLaf()}
     * @return a resolved LAF class-name or null
     * @since 1.3
     */
    public String selectLookAndFeelDefault(boolean tryDarklaf) {
        if (tryDarklaf && installLookAndFeelDarkLaf()) {
            return null;
        } else if (getOsVersion().isMacOS() || getOsVersion().isWindows()) {
            return UIManager.getSystemLookAndFeelClassName();
        } else if (System.getenv("GDK_SCALE") != null) { //for Unix GNOME with GDK_SCALE=...
            return selectLookAndFeelFromSpecialName(LOOK_AND_FEEL_VALUE_METAL);
        } else {
            return null;
        }
    }

    /**
     * install com.formdev.flatlaf.FlatDarkLaf or .FlatLaf by reflection.
     * @return true if the darklaf is installed
     * @since 1.3
     */
    public boolean installLookAndFeelDarkLaf() {
        try {
            String pack = "com.formdev.flatlaf";
            Class<?> laf;
            if (getOsVersion().isDarkTheme()) {
                laf = Class.forName(pack + ".FlatDarkLaf");
            } else {
                laf = Class.forName(pack + ".FlatLaf");
            }
            laf.getMethod("install")
                    .invoke(null);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
    
    //currently turned off:      * do <code>com.github.weisj.darklaf.LafManager.install(LafManager.themeForPreferredStyle(LafManager.getPreferredThemeStyle()))</code>
    private boolean installLookAndFeelDarkLafOld() {
        try {
            String pack = "com.github.weisj.darklaf";
            Class<?> lafManager = Class.forName(pack + ".LafManager");
            Object style = lafManager.getMethod("getPreferredThemeStyle")
                    .invoke(null);
            Object theme = lafManager.getMethod("themeForPreferredStyle", Class.forName(pack + ".theme.info.PreferredThemeStyle"))
                    .invoke(null, style);

            lafManager.getMethod("install", Class.forName(pack + ".theme.Theme"))
                    .invoke(null, theme);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_PROP_HEAD = "#prop:";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_SPECIAL_HEAD = "#special:";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_PROP_DEFAULT = "autogui.laf";

    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_METAL = "metal";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_NIMBUS = "nimbus";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_SYSTEM = "system";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_DEFAULT = "default";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_DEFAULT_NO_DARKLAF = "default-no-darklaf";
    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_VALUE_DARKLAF = "darklaf";

    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_DEFAULT = "#default";

    /** @since 1.3 */
    public static final String LOOK_AND_FEEL_NONE = "#none";

    /**
     * @param prop a property name
     * @return "#prop:"+prop
     * @since 1.3
     */
    public static String getLookAndFeelProp(String prop) {
        return LOOK_AND_FEEL_PROP_HEAD + prop;
    }

    /**
     *
     * @param value a special name
     * @return "#special:"+value
     * @since 1.3.1
     */
    public static String getLookAndFeelSpecial(String value) {
        return LOOK_AND_FEEL_SPECIAL_HEAD + value;
    }

    /**
     * called from {@link #setLookAndFeel(String)} and apply some customization
     * @since 1.2
     */
    public void setLookAndFeelSystemFix() {
        if (getOsVersion() instanceof OsVersionMac &&
                ((OsVersionMac) getOsVersion()).isBigSurOrLater()) {
            //fix BigSur tabbed-pane tab title
            UIDefaults defaults = UIManager.getDefaults();
            Color black = UIManager.getColor("TabbedPane.foreground");
            Color clear = new Color(200, 200, 200, 0);
            defaults.put("TabbedPane.selectedTabTitleNormalColor", black);
            defaults.put("TabbedPane.selectedTabTitlePressedColor", black);
            defaults.put("TabbedPane.selectedTabTitleShadowNormalColor", clear);
            defaults.put("TabbedPane.selectedTabTitleShadowDisabledColor", clear);
            defaults.put("Table.focusCellForeground", defaults.getColor("Table.foreground"));
            defaults.put("Table.focusCellBackground", defaults.getColor("Table.background"));
        }
    }

    /**
     * @return if macOS native UI, true for enabling table-view custom highlighting
     * @since 1.2
     */
    public boolean isTableCustomHighlighting() {
        return getOsVersion().isMacOS() &&
                systemLaf;
    }

    /**
     * @return if macOS native UI, true for allowing transparent tab-component
     * @since 1.2
     */
    public boolean isTabbedPaneAllowOpaqueComponent() {
        return isTableCustomHighlighting();
    }

    /**
     * @return a created OS version of the runtime
     * @since 1.2
     */
    public static OsVersion initOsVersion() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.startsWith("linux")) {
            return new OsVersionWin();
        } else if (os.startsWith("windows")) {
            return new OsVersionWin();
        } else if (os.startsWith("mac")) {
            return new OsVersionMac();
        } else {
            return new OsVersion();
        }
    }

    /**
     * @return the OS version of the runtime
     * @since 1.2
     */
    public OsVersion getOsVersion() {
        return osVersion;
    }

    /**
     * representing the OS version number
     * @since 1.2
     */
    public static class OsVersion {
        protected String arch;
        protected String name;
        protected String version;

        public OsVersion(String arch, String name, String version) {
            this.arch = arch;
            this.name = name;
            this.version = version;
        }

        public OsVersion() {
            this(System.getProperty("os.arch", ""),
                    System.getProperty("os.name", ""),
                    System.getProperty("os.version", ""));
        }

        public String getArch() {
            return arch;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public int versionNumber(String n) {
            Matcher m = Pattern.compile("(\\d+)").matcher(n);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "arch='" + arch + '\'' +
                    ", name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        public boolean isMacOS() {
            return getName().toLowerCase().startsWith("mac");
        }

        public boolean isWindows() {
            return getName().toLowerCase().startsWith("windows");
        }

        public boolean isLinux() {
            return getName().toLowerCase().startsWith("linux");
        }

        /**
         * @return true if the current UI theme is dark mode. (not yet fully implemented)
         * @since 1.3.1
         */
        public boolean isDarkTheme() {
            return false;
        }
    }

    /**
     * an OS version subclass for Windows
     * @since 1.3.1
     */
    public static class OsVersionWin extends OsVersion {
        public OsVersionWin(String arch, String name, String version) {
            super(arch, name, version);
        }

        public OsVersionWin() {
        }

        @Override
        public boolean isWindows() {
            return true;
        }

        @Override
        public boolean isDarkTheme() {
            try {
                String data = command("powershell.exe",
                        "Get-ItemProperty",
                        "-Path",
                        "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "-Name",
                        "AppsUseLightTheme");
                return Pattern.compile("AppsUseLightTheme\\s*:\\s*0").matcher(data).find();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * an OS version subclass for Windows
     * @since 1.3.1
     */
    public static class OsVersionLinux extends OsVersion {
        public OsVersionLinux(String arch, String name, String version) {
            super(arch, name, version);
        }

        public OsVersionLinux() {
        }

        @Override
        public boolean isLinux() {
            return true;
        }

        @Override
        public boolean isDarkTheme() {
            try {
                //currently only support Gtk
                return command("gsettings", "get", "org-gnome.desktop.interface", "gtk-theme")
                        .toLowerCase()
                        .contains("dark"); //'Yaru-dark' is the default dark theme?
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * an OS version subclass for macOS
     * @since 1.2
     */
    public static class OsVersionMac extends OsVersion {
        protected int versionTop; //10 : 11.0==10.16
        protected int versionMajor; //15, 16, ...
        protected int versionMinor;
        public OsVersionMac(String arch, String name, String version) {
            super(arch, name, version);
            initMac();
        }

        public OsVersionMac() {
            super();
            initMac();
        }

        protected void initMac() {
            List<String> version = Arrays.asList(getVersion().split("\\.", -1));
            versionTop = (version.size() >= 1 ? versionNumber(version.get(0)) : 10);
            versionMajor = (version.size() >= 2 ? versionNumber(version.get(1)) : 0);
            versionMinor = (version.size() >= 3 ? versionNumber(version.get(2)) : 0);
        }

        public int getVersionTop() {
            return versionTop;
        }

        public int getVersionMajor() {
            return versionMajor;
        }

        public int getVersionMinor() {
            return versionMinor;
        }

        public boolean isBigSurOrLater() {
            return versionTop >= 11 || (versionTop == 10 && versionMajor >= 16);
        }

        @Override
        public boolean isMacOS() {
            return true;
        }

        @Override
        public boolean isDarkTheme() {
            try {
                return command("defaults", "read", "-g", "AppleInterfaceStyle")
                        .toLowerCase().trim()
                        .contains("dark");
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * run a process with the arguments. The timeout for waiting outputs is 10 secs.
     * @param args the command arguments
     * @return the standard output of the process. the method throws an exception if failed
     * @since 1.3.1
     */
    public static String command(String... args) {
        try {
            Process p = new ProcessBuilder()
                    .command(args)
                    .start();
            InputStream in = p.getInputStream();
            CompletableFuture<String> result = new CompletableFuture();
            new Thread() {
                public void run() {
                    try {
                        String str = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(in.readAllBytes()))
                                .toString();
                        result.complete(str);
                    } catch (Exception ex) {
                        result.completeExceptionally(ex);
                    }
                }
            }.start();
            p.waitFor(10, TimeUnit.SECONDS);
            return result.get(1, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
