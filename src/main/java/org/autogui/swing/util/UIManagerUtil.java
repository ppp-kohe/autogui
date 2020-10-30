package org.autogui.swing.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     * @param lookAndFeelClass the LAF class name
     *                          or special name <code>"#system"</code> for {@link UIManager#getSystemLookAndFeelClassName()}
     * @since 1.2
     */
    public void setLookAndFeel(String lookAndFeelClass) {
        try {
//            boolean sysLafIsGtk = false;
//            if (UIManager.getSystemLookAndFeelClassName().contains("GTKLookAndFeel")) {
//                sysLafIsGtk = true;
//            }
            String laf = lookAndFeelClass;
            if (laf != null && laf.equals("#system")) {
                laf = UIManager.getSystemLookAndFeelClassName();
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
        if (os.startsWith("mac")) {
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
    }
}
